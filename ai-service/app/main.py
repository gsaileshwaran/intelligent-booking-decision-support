from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from app.schemas import RecommendationRequest, RecommendationResponse
from app.engine import evaluate_recommendations, MODEL_VERSION

app = FastAPI(
    title="PVK Cinemas AI Decision Engine",
    description="Multi-Criteria Decision Model (MCDM) recommendation engine for cinema booking optimization",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
@app.get("/api/v1/health")
def health_check():
    return {
        "status": "UP",
        "service": "PVK Cinemas AI Decision Engine",
        "modelVersion": MODEL_VERSION,
    }


@app.post(
    "/api/v1/recommend",
    response_model=RecommendationResponse,
    status_code=status.HTTP_200_OK,
    summary="Evaluate show recommendations using MCDM decision scoring",
)
def get_recommendations(request: RecommendationRequest) -> RecommendationResponse:
    try:
        response = evaluate_recommendations(request)
        return response
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing AI decision recommendation: {str(e)}",
        )
