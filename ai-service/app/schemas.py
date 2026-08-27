from typing import List, Optional
from pydantic import BaseModel, Field, field_validator


class CustomerPreferences(BaseModel):
    maxBudgetPerTicket: Optional[float] = Field(None, description="Maximum budget per ticket")
    preferredTimeStart: Optional[str] = Field(None, description="Preferred showtime start boundary (ISO or HH:MM)")
    preferredTimeEnd: Optional[str] = Field(None, description="Preferred showtime end boundary (ISO or HH:MM)")
    preferredSeatCategory: Optional[str] = Field(None, description="Preferred seat category e.g. PREMIUM, BALCONY, REGULAR")
    maxDistanceKm: Optional[float] = Field(None, description="Maximum distance to theatre in KM")

    @field_validator("maxBudgetPerTicket")
    @classmethod
    def validate_budget(cls, v: Optional[float]) -> Optional[float]:
        if v is not None and v < 0:
            raise ValueError("maxBudgetPerTicket cannot be negative")
        return v

    @field_validator("maxDistanceKm")
    @classmethod
    def validate_distance(cls, v: Optional[float]) -> Optional[float]:
        if v is not None and v < 0:
            raise ValueError("maxDistanceKm cannot be negative")
        return v


class ShowCandidate(BaseModel):
    showId: int = Field(..., description="Unique show identifier")
    movieId: int = Field(..., description="Movie identifier")
    theatreId: int = Field(..., description="Theatre identifier")
    theatreName: Optional[str] = Field(None, description="Name of theatre")
    showTime: str = Field(..., description="Show start time ISO or format HH:MM")
    basePrice: float = Field(..., description="Base ticket price")
    availableSeats: int = Field(..., description="Current available seats count")
    totalSeats: Optional[int] = Field(None, description="Total auditorium capacity")
    availableCategories: Optional[List[str]] = Field(default_factory=list, description="Available seat categories")
    distanceKm: Optional[float] = Field(None, description="Distance from customer in KM")

    @field_validator("basePrice")
    @classmethod
    def validate_price(cls, v: float) -> float:
        if v < 0:
            raise ValueError("basePrice cannot be negative")
        return v

    @field_validator("availableSeats")
    @classmethod
    def validate_available_seats(cls, v: int) -> int:
        if v < 0:
            raise ValueError("availableSeats cannot be negative")
        return v


class RecommendationRequest(BaseModel):
    customerId: Optional[int] = Field(None, description="Customer user ID")
    groupSize: int = Field(1, description="Number of tickets requested")
    preferences: Optional[CustomerPreferences] = Field(None, description="User booking preferences")
    candidates: List[ShowCandidate] = Field(default_factory=list, description="List of show candidates to evaluate")

    @field_validator("groupSize")
    @classmethod
    def validate_group_size(cls, v: int) -> int:
        if v <= 0:
            raise ValueError("groupSize must be at least 1")
        return v


class ScoreBreakdown(BaseModel):
    priceFit: float = Field(..., description="Normalized price suitability [0.0, 1.0]")
    timeProximity: float = Field(..., description="Normalized time proximity suitability [0.0, 1.0]")
    seatCategoryMatch: float = Field(..., description="Normalized seat category suitability [0.0, 1.0]")
    distanceProximity: float = Field(..., description="Normalized distance proximity suitability [0.0, 1.0]")


class RankedResult(BaseModel):
    rank: int = Field(..., description="1-based recommendation position")
    showId: int = Field(..., description="Show identifier")
    suitabilityScore: float = Field(..., description="Composite suitability score [0.0, 100.0]")
    scoreBreakdown: ScoreBreakdown = Field(..., description="Breakdown of criterion scores")
    explanation: str = Field(..., description="Human-readable summary explanation")
    explanationFactors: List[str] = Field(..., description="Detailed list of key scoring factors")
    isBestMatch: bool = Field(..., description="True if rank == 1")
    alternativeNotice: Optional[str] = Field(None, description="Tag or message for alternative options when top preference is unavailable or secondary")


class RecommendationResponse(BaseModel):
    recommendationId: str = Field(..., description="Unique recommendation session identifier")
    evaluatedCount: int = Field(..., description="Total candidate shows evaluated")
    rankedResults: List[RankedResult] = Field(default_factory=list, description="Ranked show recommendations")
    modelVersion: str = Field("v1.0-fastapi-mcdm", description="AI Model/Engine version string")
