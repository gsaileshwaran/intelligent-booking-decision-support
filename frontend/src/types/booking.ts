export interface SeatHoldResponse {
  holdToken: string;
  showId: number;
  seatIds: number[];
  heldUntil: string;
  expiresInSeconds: number;
  totalAmount: number;
  message: string;
}

export interface CheckoutRequest {
  holdToken: string;
  paymentMethod: 'DUMMY_CARD' | 'DUMMY_UPI' | 'TEST_WALLET';
  simulateOutcome: 'SUCCESS' | 'FAILED';
}

export interface SeatDetail {
  seatId: number;
  rowLabel: string;
  seatNumber: string;
  seatType: string;
  price: number;
}

export interface BookingResponse {
  bookingId: number;
  bookingReference: string;
  userId: number;
  userEmail?: string;
  showId: number;
  movieTitle: string;
  theatreName: string;
  cityName: string;
  screenName: string;
  showDate: string;
  showTime: string;
  seats: SeatDetail[];
  totalAmount: number;
  bookingStatus: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  paymentReference?: string;
  paymentMethod?: string;
  paymentStatus?: string;
  createdAt: string;
}

export interface SeatQualityScore {
  seatId: number;
  rowLabel: string;
  seatNumber: string;
  seatType: string;
  score?: number;
  totalScore?: number;
  badge?: 'OPTIMAL' | 'PRIME' | 'GOOD' | 'STANDARD' | string;
  tier?: 'OPTIMAL' | 'PRIME' | 'GOOD' | 'STANDARD' | string;
  viewCategory?: string;
  tag?: string;
  distanceFactor?: number;
  lateralFactor?: number;
  acousticFactor?: number;
  price?: number;
  reasons?: string[];
}

export interface SeatBlockOption {
  category?: string;
  title?: string;
  seatIds: number[];
  seatLabels: string[];
  averageScore: number;
  tier?: string;
  totalPrice?: number;
  rationale: string;
}

export interface SeatRecommendations {
  showId: number;
  partySize: number;
  preference?: string;
  recommendations?: SeatBlockOption[];
  options?: SeatBlockOption[];
}

export interface DecisionGuidance {
  selectedSeatIds?: number[];
  overallQualityScore?: number;
  averageQualityScore?: number;
  overallRating?: string;
  tier?: string;
  keyStrengths?: string[];
  strengths?: string[];
  frictionAlerts?: {
    type: string;
    severity: string;
    title: string;
    message: string;
  }[];
  frictions?: string[];
  recoveryStrategies?: {
    title: string;
    description: string;
    actionType: string;
    suggestedSeatIds: number[];
    suggestedSeatLabels?: string[];
  }[];
  recoveryActions?: {
    actionType: string;
    description: string;
    suggestedSeatIds: number[];
  }[];
}

export interface DecisionNegotiation {
  message?: string;
  requestedPartyAvailable?: boolean;
  partySize?: number;
  adjacentSplitOptions?: SeatBlockOption[];
  alternativeShowtimes?: {
    showId: number;
    showTime: string;
    availableSeats: number;
    partySize: number;
    qualityScore: number;
  }[];
  alternativeTheatres?: {
    theatreId: number;
    theatreName: string;
    showId: number;
    showTime: string;
    availableSeats: number;
    qualityScore: number;
  }[];
  alternatives?: {
    type: 'SAME_THEATRE_LATER_SHOW' | 'DIFFERENT_THEATRE_SAME_CITY' | 'SPLIT_ADJACENT_ROWS';
    theatreName?: string;
    showId?: number;
    showTime?: string;
    description: string;
    availableSeatsCount?: number;
  }[];
}

export interface ShowCandidate {
  showId: number;
  theatreId: number;
  theatreName: string;
  screenId: number;
  screenName: string;
  presentationFormat: string;
  startAt: string;
  formattedTime: string;
  showDate?: string;
  language?: string;
  totalCost?: number | string;
  availableSeats: number;
  totalSeats: number;
  availabilityRatio: number;
  ticketPrice: number | string;
  matchScore: number;
  reasons: string[];
  tradeOffs: string[];
  recommendedSeatIds?: number[];
  recommendedSeatLabels?: string[];
  alternativeSeatLabels?: string[];
  seatFitDescription?: string;
  viewingQualityScore?: number;
  seatingTradeoff?: string;
  startTime?: string;
  availableSeatCount?: number;
  startingPrice?: number | string;
}

/** Alias — canonical name used in MovieDetailsView */
export type ShowCandidateItem = ShowCandidate;

export interface ShowRecommendationResponse {
  movieId: number;
  movieTitle: string;
  cityId: number | null;
  cityName: string;
  preferredOption: ShowCandidate | null;
  /** Ranked list of ALL qualifying candidates (sorted best-first). */
  candidates: ShowCandidate[];
  /** Backward compat alias — same as candidates */
  rankedCandidates?: ShowCandidate[];
  conflictAnalysis?: string;
}

export interface InteractionFriction {
  detected: boolean;
  frictionType: string;
  severity: string;
  title: string;
  description: string;
  suggestedAction: string;
  triggerCount?: number;
}

export interface JourneyGuidance {
  currentStage: string;
  guidanceTitle: string;
  guidancePrompt: string;
  nextRecommendedAction: string;
  contextualTips: string[];
}
