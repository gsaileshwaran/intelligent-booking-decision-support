import api from './api';

export const customerService = {
  getProfile: async () => {
    const response = await api.get('/customer/profile');
    return response.data;
  },

  updateProfile: async (name) => {
    const response = await api.put('/customer/profile', { name });
    return response.data;
  },

  changePassword: async (currentPassword, newPassword, confirmPassword) => {
    const response = await api.put('/customer/password', {
      currentPassword,
      newPassword,
      confirmPassword,
    });
    return response.data;
  },
};
