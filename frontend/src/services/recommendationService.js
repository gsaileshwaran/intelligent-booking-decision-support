import api from './api';

export const recommendationService = {
  evaluateRecommendations: async (params) => {
    const response = await api.post('/recommendations/evaluate', params);
    return response.data;
  },
};
