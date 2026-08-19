import api from './api';

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    if (response.data.success && response.data.data.token) {
      localStorage.setItem('jwt_token', response.data.data.token);
      localStorage.setItem('user_data', JSON.stringify(response.data.data));
    }
    return response.data;
  },

  register: async (name, email, password, role = 'ROLE_CUSTOMER') => {
    const response = await api.post('/auth/register', { name, email, password, role });
    if (response.data.success && response.data.data.token) {
      localStorage.setItem('jwt_token', response.data.data.token);
      localStorage.setItem('user_data', JSON.stringify(response.data.data));
    }
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_data');
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('user_data');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  getToken: () => localStorage.getItem('jwt_token'),
};
