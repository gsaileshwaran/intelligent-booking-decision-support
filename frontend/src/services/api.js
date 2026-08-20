import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT Token automatically
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle API Responses and 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Clear token and user session on unauthorized
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user_data');
    }
    return Promise.reject(error);
  }
);

// Helper function to extract user-friendly error messages
export const getErrorMessage = (error) => {
  if (!error) return 'An unexpected error occurred.';
  if (typeof error === 'string') return error;
  if (error.response) {
    const status = error.response.status;
    const data = error.response.data;
    if (data && data.message) return data.message;
    if (status === 401) return 'Invalid email or password. Please try again.';
    if (status === 403) return 'Access denied. You do not have permission for this resource.';
    if (status === 404) return 'The requested resource was not found.';
    if (status === 409) return 'Conflict detected. The requested seat or resource is unavailable.';
    if (status >= 500) return 'Internal server error. Please try again later.';
  }
  if (error.request) return 'Network error: Cannot reach the backend server (http://localhost:8080).';
  return error.message || 'An unexpected error occurred.';
};

export default api;
