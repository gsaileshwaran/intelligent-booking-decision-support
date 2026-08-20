import api from './api';

export const providerService = {
  getDashboardStats: async () => {
    const response = await api.get('/provider/dashboard');
    return response.data;
  },

  getTheatres: async () => {
    const response = await api.get('/provider/provider/theatres');
    return response.data;
  },

  getMyTheatres: async () => {
    const response = await api.get('/provider/theatres');
    return response.data;
  },

  getTheatreDetails: async (id) => {
    const response = await api.get(`/provider/theatres/${id}`);
    return response.data;
  },

  getTheatreScreens: async (id) => {
    const response = await api.get(`/provider/theatres/${id}/screens`);
    return response.data;
  },

  getTheatreShows: async (id) => {
    const response = await api.get(`/provider/theatres/${id}/shows`);
    return response.data;
  },

  getTheatreBookings: async (id) => {
    const response = await api.get(`/provider/theatres/${id}/bookings`);
    return response.data;
  },

  getBookings: async (params = {}) => {
    const response = await api.get('/provider/bookings', { params });
    return response.data;
  },

  getShows: async (params = {}) => {
    const response = await api.get('/provider/shows', { params });
    return response.data;
  },

  updateShow: async (id, showData) => {
    const response = await api.put(`/provider/shows/${id}`, showData);
    return response.data;
  },

  cancelShow: async (id) => {
    const response = await api.patch(`/provider/shows/${id}/cancel`);
    return response.data;
  },

  createTheatre: async (theatreData) => {
    const response = await api.post('/provider/theatres', theatreData);
    return response.data;
  },

  createMovie: async (movieData) => {
    const response = await api.post('/provider/movies', movieData);
    return response.data;
  },

  createShow: async (showData, movieId, screenId) => {
    const response = await api.post('/provider/shows', showData, {
      params: { movieId, screenId },
    });
    return response.data;
  },
};
