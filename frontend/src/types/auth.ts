export type Role = 'CUSTOMER' | 'THEATRE_MANAGER' | 'SUPER_ADMIN' | 'SUPPORT_AGENT';

export interface User {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role?: string;
  roles?: string[];
  permissions: string[];
  phone?: string;
  accountStatus?: string;
  assignedTheatreId?: number;
}

export interface AuthResponse {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  permissions: string[];
  token: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password?: string;
  passwordHash?: string;
}

export interface RegisterRequest {
  email: string;
  password?: string;
  passwordHash?: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface ProfileResponse {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  preferredCityId?: number;
  roles: string[];
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  preferredCityId?: number;
}
