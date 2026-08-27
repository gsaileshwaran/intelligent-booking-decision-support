import api from './api';

export const movieService = {
  getAllMovies: async (params = {}) => {
    const response = await api.get('/movies', { params });
    return response.data;
  },

  searchMovies: async (query) => {
    const response = await api.get('/movies/search', { params: { query } });
    return response.data;
  },

  getMovieById: async (movieId) => {
    const response = await api.get(`/movies/${movieId}`);
    return response.data;
  },
};
