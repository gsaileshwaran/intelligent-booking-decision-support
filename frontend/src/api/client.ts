import type { ApiResponse, Page } from '../types/api';
import type { 
  AuthResponse, 
  LoginRequest, 
  RegisterRequest, 
  User, 
  ProfileResponse, 
  UpdateProfileRequest 
} from '../types/auth';
import type { Movie, Genre, Language, MovieRequest } from '../types/movie';
import type { City, Theatre, Screen, ScreenRequest, Seat, SeatRequest, CreateUserRequest } from '../types/theatre';
import type { Show, ShowRequest, ShowSeatAvailabilityResponse, UpdateSeatStatusRequest } from '../types/show';
import type { SearchCard, SearchIndexStatusResponse } from '../types/search';
import type { AdminUser, AuditLog, RoleInfo } from '../types/admin';
import type {
  SeatHoldResponse,
  CheckoutRequest,
  BookingResponse,
  SeatQualityScore,
  SeatRecommendations,
  DecisionGuidance,
  DecisionNegotiation,
  ShowRecommendationResponse,
  InteractionFriction,
  JourneyGuidance,
} from '../types/booking';

const BASE_URL = '';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('pvk_token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // If unauthorized, clear stale token
    localStorage.removeItem('pvk_token');
    localStorage.removeItem('pvk_user');
  }

  let body: any = null;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    body = await response.json();
  } else {
    body = await response.text();
  }

  if (!response.ok) {
    const errorMessage = (body && typeof body === 'object' && (body.message || body.error)) || response.statusText || 'Request failed';
    throw new Error(errorMessage);
  }

  // Check if wrapped in ApiResponse
  if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
    return (body as ApiResponse<T>).data;
  }

  return body as T;
}

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const payload = {
      email: credentials.email,
      password: credentials.password || credentials.passwordHash,
    };
    return request<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  register: async (payload: RegisterRequest): Promise<AuthResponse> => {
    const body = {
      ...payload,
      password: payload.password || payload.passwordHash,
    };
    return request<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },
  getCurrentUser: async (): Promise<User> => {
    return request<User>('/api/v1/auth/me');
  },
  logout: async (): Promise<void> => {
    try {
      await request<any>('/api/v1/auth/logout', {
        method: 'POST',
      });
    } catch (err) {
      console.warn('Backend logout notification failed, clearing local session', err);
    }
  },
};

export const usersApi = {
  getProfile: async (): Promise<ProfileResponse> => {
    return request<ProfileResponse>('/api/v1/me/profile');
  },
  updateProfile: async (data: UpdateProfileRequest): Promise<ProfileResponse> => {
    return request<ProfileResponse>('/api/v1/me/profile', {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
};

export const moviesApi = {
  getMovies: async (params?: { status?: string; cityId?: number; page?: number; size?: number }): Promise<Page<Movie>> => {
    const query = new URLSearchParams();
    if (params?.status) query.set('status', params.status);
    if (params?.cityId !== undefined) query.set('cityId', params.cityId.toString());
    if (params?.page !== undefined) query.set('page', params.page.toString());
    if (params?.size !== undefined) query.set('size', params.size.toString());
    const qs = query.toString() ? `?${query.toString()}` : '';
    return request<Page<Movie>>(`/api/v1/movies${qs}`);
  },
  getMovie: async (movieId: number): Promise<Movie> => {
    return request<Movie>(`/api/v1/movies/${movieId}`);
  },
  getMovieGenres: async (movieId: number): Promise<Genre[]> => {
    return request<Genre[]>(`/api/v1/movies/${movieId}/genres`);
  },
  getMovieLanguages: async (movieId: number): Promise<Language[]> => {
    return request<Language[]>(`/api/v1/movies/${movieId}/languages`);
  },
  getMovieShows: async (movieId: number, cityId?: number): Promise<Show[]> => {
    const qs = cityId ? `?cityId=${cityId}` : '';
    return request<Show[]>(`/api/v1/movies/${movieId}/shows${qs}`);
  },
};

export const theatresApi = {
  getCities: async (): Promise<City[]> => {
    const cities = await request<City[]>('/api/v1/cities');
    return (cities || []).filter(c => c.cityName && !c.cityName.startsWith('City_Upd_'));
  },
  getTheatres: async (cityId?: number): Promise<Theatre[]> => {
    if (cityId) {
      return request<Theatre[]>(`/api/v1/cities/${cityId}/theatres`);
    }
    const cities = await request<City[]>('/api/v1/cities');
    if (!cities || cities.length === 0) return [];
    const allTheatres = await Promise.all(
      cities.map((c) => request<Theatre[]>(`/api/v1/cities/${c.cityId}/theatres`))
    );
    return allTheatres.flat();
  },
  getTheatresByCity: async (cityId: number): Promise<Theatre[]> => {
    return request<Theatre[]>(`/api/v1/cities/${cityId}/theatres`);
  },
  getTheatre: async (theatreId: number): Promise<Theatre> => {
    return request<Theatre>(`/api/v1/theatres/${theatreId}`);
  },
  getTheatreShows: async (theatreId: number): Promise<Show[]> => {
    return request<Show[]>(`/api/v1/theatres/${theatreId}/shows`);
  },
  getTheatreScreens: async (theatreId: number): Promise<Screen[]> => {
    return request<Screen[]>(`/api/v1/theatres/${theatreId}/screens`);
  },
};

export const showsApi = {
  getShow: async (showId: number): Promise<Show> => {
    return request<Show>(`/api/v1/shows/${showId}`);
  },
  getShowSeats: async (showId: number): Promise<ShowSeatAvailabilityResponse> => {
    return request<ShowSeatAvailabilityResponse>(`/api/v1/shows/${showId}/seats`);
  },
};

export const seatHoldApi = {
  holdSeats: async (showId: number, seatIds: number[]): Promise<SeatHoldResponse> => {
    return request<SeatHoldResponse>(`/api/v1/shows/${showId}/seats/hold`, {
      method: 'POST',
      body: JSON.stringify({ seatIds }),
    });
  },
  releaseHold: async (showId: number, holdToken: string): Promise<void> => {
    return request<void>(`/api/v1/shows/${showId}/seats/release`, {
      method: 'POST',
      body: JSON.stringify({ holdToken }),
    });
  },
};

export const bookingApi = {
  checkout: async (data: CheckoutRequest): Promise<BookingResponse> => {
    return request<BookingResponse>('/api/v1/bookings/checkout', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  getMyBookings: async (): Promise<BookingResponse[]> => {
    return request<BookingResponse[]>('/api/v1/me/bookings');
  },
  getBookingByReference: async (reference: string): Promise<BookingResponse> => {
    return request<BookingResponse>(`/api/v1/bookings/reference/${reference}`);
  },
};

export const decisionApi = {
  getSeatScores: async (showId: number): Promise<SeatQualityScore[]> => {
    return request<SeatQualityScore[]>(`/api/v1/decision/shows/${showId}/seat-scores`);
  },
  getRecommendations: async (showId: number, partySize: number = 2, preference: string = 'CENTER_BALANCED'): Promise<SeatRecommendations> => {
    return request<SeatRecommendations>(`/api/v1/decision/shows/${showId}/recommend?partySize=${partySize}&preference=${preference}`);
  },
  getGuidance: async (showId: number, seatIds: number[]): Promise<DecisionGuidance> => {
    return request<DecisionGuidance>(`/api/v1/decision/shows/${showId}/guidance`, {
      method: 'POST',
      body: JSON.stringify({ seatIds }),
    });
  },
  negotiate: async (showId: number, partySize: number = 2): Promise<DecisionNegotiation> => {
    return request<DecisionNegotiation>(`/api/v1/decision/shows/${showId}/negotiate?partySize=${partySize}`);
  },
  getShowRecommendations: async (movieId: number, params?: {
    cityId?: number;
    languageCode?: string;
    dateFrom?: string;   // ISO date e.g. "2026-09-20"
    dateTo?: string;     // ISO date e.g. "2026-09-27"
    formatPreference?: string;
    timePreference?: string;
    priority?: 'BEST_PRICE' | 'BEST_SEATS' | 'BEST_TIME' | 'BALANCED';
    partySize?: number;
    budgetMax?: number;
    budgetMaxTotal?: number; // total budget for whole party
  }): Promise<ShowRecommendationResponse> => {
    const query = new URLSearchParams();
    if (params?.cityId !== undefined) query.set('cityId', params.cityId.toString());
    if (params?.languageCode) query.set('languageCode', params.languageCode);
    if (params?.dateFrom) query.set('dateFrom', params.dateFrom);
    if (params?.dateTo) query.set('dateTo', params.dateTo);
    if (params?.formatPreference) query.set('formatPreference', params.formatPreference);
    if (params?.timePreference) query.set('timePreference', params.timePreference);
    if (params?.priority) query.set('priority', params.priority);
    if (params?.partySize !== undefined) query.set('partySize', params.partySize.toString());
    if (params?.budgetMaxTotal !== undefined) query.set('budgetMaxTotal', params.budgetMaxTotal.toString());
    if (params?.budgetMax !== undefined) query.set('budgetMax', params.budgetMax.toString());
    const qs = query.toString() ? `?${query.toString()}` : '';
    return request<ShowRecommendationResponse>(`/api/v1/decision/movies/${movieId}/recommend-shows${qs}`);
  },
  evaluateInteractionFriction: async (data: { filterChangeCount?: number; theatreChangeCount?: number; timeSelectionCount?: number; seatToggleCount?: number; backNavigationCount?: number; failedHoldAttempts?: number; timeWindowSeconds?: number; recentSelectedTimes?: string[] }): Promise<InteractionFriction> => {
    return request<InteractionFriction>('/api/v1/decision/friction/evaluate', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  getJourneyGuidance: async (data: { currentStage: string; cityId?: number; cityName?: string; movieId?: number; movieTitle?: string; showId?: number; selectedSeatCount?: number; holdSecondsRemaining?: number; detectedFrictionType?: string }): Promise<JourneyGuidance> => {
    return request<JourneyGuidance>('/api/v1/decision/guidance/journey-stage', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

export const searchApi = {
  search: async (q: string, cityId?: number): Promise<SearchCard[]> => {
    const query = new URLSearchParams();
    if (q) query.set('q', q);
    if (cityId) query.set('cityId', cityId.toString());
    return request<SearchCard[]>(`/api/v1/search?${query.toString()}`);
  },
  searchPost: async (query: string, cityId?: number): Promise<SearchCard[]> => {
    return request<SearchCard[]>('/api/v1/search', {
      method: 'POST',
      body: JSON.stringify({ query, cityId }),
    });
  },
};

export const managerApi = {
  getScreens: async (theatreId: number): Promise<Screen[]> => {
    return request<Screen[]>(`/api/v1/manager/theatres/${theatreId}/screens`);
  },
  createScreen: async (theatreId: number, data: ScreenRequest): Promise<Screen> => {
    return request<Screen>(`/api/v1/manager/theatres/${theatreId}/screens`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateScreen: async (theatreId: number, screenId: number, data: ScreenRequest): Promise<Screen> => {
    return request<Screen>(`/api/v1/manager/theatres/${theatreId}/screens/${screenId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
  getSeats: async (screenId: number): Promise<Seat[]> => {
    return request<Seat[]>(`/api/v1/manager/screens/${screenId}/seats`);
  },
  createSeat: async (screenId: number, data: SeatRequest): Promise<Seat> => {
    return request<Seat>(`/api/v1/manager/screens/${screenId}/seats`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateSeat: async (screenId: number, seatId: number, data: SeatRequest): Promise<Seat> => {
    return request<Seat>(`/api/v1/manager/screens/${screenId}/seats/${seatId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
  getShows: async (theatreId: number): Promise<Show[]> => {
    return request<Show[]>(`/api/v1/manager/theatres/${theatreId}/shows`);
  },
  createShow: async (theatreId: number, data: ShowRequest): Promise<Show> => {
    return request<Show>(`/api/v1/manager/theatres/${theatreId}/shows`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateShow: async (theatreId: number, showId: number, data: ShowRequest): Promise<Show> => {
    return request<Show>(`/api/v1/manager/theatres/${theatreId}/shows/${showId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
  getShowSeats: async (showId: number): Promise<ShowSeatAvailabilityResponse> => {
    return request<ShowSeatAvailabilityResponse>(`/api/v1/manager/shows/${showId}/seats`);
  },
  overrideSeatStatus: async (showId: number, data: UpdateSeatStatusRequest): Promise<ShowSeatAvailabilityResponse> => {
    return request<ShowSeatAvailabilityResponse>(`/api/v1/manager/shows/${showId}/seats`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
};

export const adminApi = {
  getUsers: async (params?: { page?: number; size?: number }): Promise<Page<AdminUser>> => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.set('page', params.page.toString());
    if (params?.size !== undefined) query.set('size', params.size.toString());
    const qs = query.toString() ? `?${query.toString()}` : '';
    return request<Page<AdminUser>>(`/api/v1/admin/users${qs}`);
  },
  updateUserStatus: async (userId: number, status: string): Promise<AdminUser> => {
    return request<AdminUser>(`/api/v1/admin/users/${userId}`, {
      method: 'PATCH',
      body: JSON.stringify({ accountStatus: status }),
    });
  },
  createUser: async (data: CreateUserRequest): Promise<AdminUser> => {
    return request<AdminUser>('/api/v1/admin/users', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  getRoles: async (): Promise<RoleInfo[]> => {
    return request<RoleInfo[]>('/api/v1/admin/roles');
  },
  getCities: async (): Promise<City[]> => {
    return request<City[]>('/api/v1/admin/cities');
  },
  createCity: async (data: { cityName: string; state: string; country: string }): Promise<City> => {
    return request<City>('/api/v1/admin/cities', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateCity: async (cityId: number, data: { cityName?: string; state?: string; country?: string }): Promise<City> => {
    return request<City>(`/api/v1/admin/cities/${cityId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
  getTheatres: async (): Promise<Theatre[]> => {
    return request<Theatre[]>('/api/v1/admin/theatres');
  },
  createTheatre: async (data: any): Promise<Theatre> => {
    return request<Theatre>('/api/v1/admin/theatres', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateTheatre: async (theatreId: number, data: any): Promise<Theatre> => {
    return request<Theatre>(`/api/v1/admin/theatres/${theatreId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
  assignManager: async (theatreId: number, userId: number): Promise<void> => {
    return request<void>(`/api/v1/admin/theatres/${theatreId}/managers`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    });
  },
  revokeManager: async (theatreId: number, userId: number): Promise<void> => {
    return request<void>(`/api/v1/admin/theatres/${theatreId}/managers?userId=${userId}`, {
      method: 'PATCH',
    });
  },
  createMovie: async (data: MovieRequest): Promise<Movie> => {
    return request<Movie>('/api/v1/admin/movies', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateMovie: async (movieId: number, data: MovieRequest): Promise<Movie> => {
    return request<Movie>(`/api/v1/admin/movies/${movieId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },
  getAuditLogs: async (params?: { page?: number; size?: number }): Promise<Page<AuditLog>> => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.set('page', params.page.toString());
    if (params?.size !== undefined) query.set('size', params.size.toString());
    const qs = query.toString() ? `?${query.toString()}` : '';
    return request<Page<AuditLog>>(`/api/v1/admin/audit-logs${qs}`);
  },
  getSearchStatus: async (): Promise<SearchIndexStatusResponse> => {
    return request<SearchIndexStatusResponse>('/api/v1/admin/search/status');
  },
  triggerReindex: async (): Promise<string> => {
    return request<string>('/api/v1/admin/search/reindex', {
      method: 'POST',
    });
  },
  getMovies: async (params?: { status?: string; page?: number; size?: number }): Promise<Page<Movie>> => {
    const query = new URLSearchParams();
    if (params?.status) query.set('status', params.status);
    if (params?.page !== undefined) query.set('page', params.page.toString());
    if (params?.size !== undefined) query.set('size', params.size.toString());
    const qs = query.toString() ? `?${query.toString()}` : '';
    return request<Page<Movie>>(`/api/v1/admin/movies${qs}`);
  },
  getPermissions: async (): Promise<any[]> => {
    return request<any[]>('/api/v1/admin/permissions');
  },
  updateRolePermissionsPost: async (roleId: number, permissions: number[]): Promise<any> => {
    return request<any>(`/api/v1/admin/roles/${roleId}`, {
      method: 'POST',
      body: JSON.stringify({ permissions }),
    });
  },
  updateRolePermissionsPatch: async (roleId: number, permissions: number[]): Promise<any> => {
    return request<any>(`/api/v1/admin/roles/${roleId}`, {
      method: 'PATCH',
      body: JSON.stringify({ permissions }),
    });
  },
  updateUserPost: async (userId: number, status: string): Promise<AdminUser> => {
    return request<AdminUser>(`/api/v1/admin/users/${userId}`, {
      method: 'POST',
      body: JSON.stringify({ accountStatus: status }),
    });
  },
};
