import api from './api';

export const bookingService = {
  holdSeats: async (showId, showSeatIds) => {
    const response = await api.post('/bookings/hold', { showId, showSeatIds });
    return response.data;
  },

  getBookingById: async (bookingId) => {
    const response = await api.get(`/bookings/${bookingId}`);
    return response.data;
  },

  confirmBooking: async (bookingId, paymentMethod = 'MOCK_CARD', promoCode = '') => {
    const response = await api.post(`/bookings/${bookingId}/confirm`, null, {
      params: { paymentMethod, promoCode },
    });
    return response.data;
  },

  validatePromo: async (promoCode, amount) => {
    const response = await api.post('/bookings/validate-promo', null, {
      params: { promoCode, amount },
    });
    return response.data;
  },

  cancelBooking: async (bookingId) => {
    const response = await api.post(`/bookings/${bookingId}/cancel`);
    return response.data;
  },

  getMyHistory: async () => {
    const response = await api.get('/bookings/my-history');
    return response.data;
  },
};
