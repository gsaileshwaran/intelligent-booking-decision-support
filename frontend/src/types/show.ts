export interface Show {
  showId: number;
  movieId: number;
  movieTitle: string;
  movieLanguageId?: number;
  languageName?: string;
  screenId: number;
  screenName: string;
  theatreId: number;
  theatreName: string;
  cityId?: number;
  cityName?: string;
  screenCapabilityId?: number;
  startAt: string;
  endAt: string;
  showStatus: string;
  presentationFormat?: string;
  formatCode?: string;
  availableSeats?: number;
  totalSeats?: number;
  minPrice?: number;
  posterUrl?: string;
  showDate?: string;
  startTime?: string;
  availableSeatCount?: number;
  startingPrice?: number;
  language?: string;
}

export interface ShowRequest {
  movieId: number;
  movieLanguageId: number;
  screenId: number;
  screenCapabilityId: number;
  startAt: string;
  endAt: string;
  showStatus?: string;
}

export interface SeatAvailabilityDetail {
  seatId: number;
  rowLabel: string;
  seatNumber: string;
  seatType: string;
  availabilityStatus: 'AVAILABLE' | 'BOOKED' | 'BLOCKED' | 'HELD';
  pricingZone?: 'VALUE' | 'STANDARD' | 'PREMIUM' | string;
  price?: number;
  ticketPrice?: number;
  gridRowIndex?: number;
  gridColIndex?: number;
  aisleAfter?: boolean;
  /** Physical seat status — BLOCKED means maintenance block that overrides show-level status */
  physicalStatus?: string;
}

export interface ShowSeatAvailabilityResponse {
  showId: number;
  movieId?: number;
  movieTitle?: string;
  theatreId: number;
  theatreName: string;
  screenId: number;
  screenName: string;
  totalSeats: number;
  availableSeats: number;
  bookedSeats: number;
  blockedSeats: number;
  heldSeats?: number;
  seats: SeatAvailabilityDetail[];
}

export interface UpdateSeatStatusRequest {
  seatIds: number[];
  availabilityStatus: 'AVAILABLE' | 'BLOCKED';
}

export interface TheatreGroupedShows {
  theatreId: number;
  theatreName: string;
  screens: {
    screenId: number;
    screenName: string;
    shows: Show[];
  }[];
}
