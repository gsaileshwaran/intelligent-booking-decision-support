import api from './api';

export const adminService = {
  getDashboardStats: async () => {
    const response = await api.get('/admin/dashboard');
    return response.data;
  },

  getBranchStats: async () => {
    const response = await api.get('/admin/branches');
    return response.data;
  },

  getRecentBookings: async (limit = 10) => {
    const response = await api.get('/admin/recent-bookings', {
      params: { limit },
    });
    return response.data;
  },
};
