import api from './api';

export const providerService = {
  getTheatres: async () => {
    const response = await api.get('/provider/theatres');
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
