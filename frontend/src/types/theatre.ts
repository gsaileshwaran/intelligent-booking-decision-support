export interface City {
  cityId: number;
  cityName: string;
  state: string;
  country: string;
  theatresCount?: number;
}

export interface Theatre {
  theatreId: number;
  cityId: number;
  cityName?: string;
  theatreName: string;
  theatreCode: string;
  addressLine1: string;
  addressLine2?: string;
  postalCode?: string;
  totalScreens?: number;
  theatreStatus?: string;
  createdAt?: string;
}

export interface Screen {
  screenId: number;
  theatreId: number;
  theatreName?: string;
  screenNumber: number;
  screenName: string;
  totalSeats?: number;
  screenStatus: string;
  screenCapabilityId?: number;
  screenTypeId?: number;
  screenTypeName?: string;
  soundTypeId?: number;
  soundTypeName?: string;
}

export interface ScreenRequest {
  screenNumber: number;
  screenName: string;
  screenStatus?: string;
  screenTypeId?: number;
  soundTypeId?: number;
}

/** Physical seat entity — used by manager screen-wise seat management */
export interface Seat {
  seatId: number;
  screenId: number;
  seatTypeId?: number;
  seatTypeCode?: string;
  rowLabel: string;
  seatNumber: string;
  gridRowIndex?: number;
  gridColIndex?: number;
  isActive?: boolean;
  // Physical domain fields
  status: string;          // 'ACTIVE' | 'BLOCKED'
  pricingZone?: string;    // 'VALUE' | 'STANDARD' | 'PREMIUM'
  aisleAfter?: boolean;
  // Legacy fields (kept for backwards compat)
  seatType?: string;
  seatStatus?: string;
  // Manager warning fields (returned after block operation)
  warningMessage?: string;
  upcomingBookingsCount?: number;
}

export interface SeatRequest {
  rowLabel?: string;
  seatNumber?: string;
  seatTypeId?: number;
  gridRowIndex?: number;
  gridColIndex?: number;
  isActive?: boolean;
  // Physical domain fields
  status?: string;        // 'ACTIVE' | 'BLOCKED'
  blockReason?: string;
}

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
  roleCode?: string;   // 'ROLE_CUSTOMER' | 'ROLE_THEATRE_MANAGER'
  theatreId?: number;
}

