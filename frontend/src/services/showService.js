import api from './api';

export const showService = {
  getShows: async (movieId, date) => {
    const params = { movieId };
    if (date) params.date = date;
    const response = await api.get('/shows', { params });
    return response.data;
  },

  getShowById: async (showId) => {
    const response = await api.get(`/shows/${showId}`);
    return response.data;
  },

  getShowSeats: async (showId) => {
    const response = await api.get(`/shows/${showId}/seat-map`);
    return response.data;
  },
};
