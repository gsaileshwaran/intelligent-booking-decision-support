import api from './api';

export const convenienceService = {
  // Favorites / Watchlist
  toggleFavorite: async (movieId) => {
    const response = await api.post(`/customer/favorites/${movieId}`);
    return response.data;
  },

  getFavorites: async () => {
    const response = await api.get('/customer/favorites');
    return response.data;
  },

  checkFavorite: async (movieId) => {
    const response = await api.get(`/customer/favorites/check/${movieId}`);
    return response.data;
  },

  // Notifications
  getNotifications: async () => {
    const response = await api.get('/customer/notifications');
    return response.data;
  },

  markNotificationRead: async (notificationId) => {
    const response = await api.put(`/customer/notifications/${notificationId}/read`);
    return response.data;
  },

  // Preferences
  getPreferences: async () => {
    const response = await api.get('/customer/preferences');
    return response.data;
  },

  updatePreferences: async (params) => {
    const response = await api.put('/customer/preferences', null, { params });
    return response.data;
  },

  // Offers
  getOffers: async () => {
    const response = await api.get('/offers');
    return response.data;
  },

  // Theatres / Venues
  getTheatres: async (location) => {
    const response = await api.get('/theatres', { params: { location } });
    return response.data;
  },

  getTheatreById: async (id) => {
    const response = await api.get(`/theatres/${id}`);
    return response.data;
  },
};
