import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleProtectedRoute } from './RoleProtectedRoute';

// Customer Pages
import { HomePage } from '../pages/customer/HomePage';
import { MovieDetailsPage } from '../pages/customer/MovieDetailsPage';
import { SeatSelectionPage } from '../pages/customer/SeatSelectionPage';
import { BookingConfirmationPage } from '../pages/customer/BookingConfirmationPage';
import { BookingHistoryPage } from '../pages/customer/BookingHistoryPage';
import { LoginPage } from '../pages/customer/LoginPage';
import { RegisterPage } from '../pages/customer/RegisterPage';

// Provider Pages
import { ProviderDashboardPage } from '../pages/provider/ProviderDashboardPage';
import { VenueManagerPage } from '../pages/provider/VenueManagerPage';
import { MovieManagerPage } from '../pages/provider/MovieManagerPage';
import { ShowSchedulerPage } from '../pages/provider/ShowSchedulerPage';

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<Navigate to="/movies" replace />} />
      <Route path="/movies" element={<HomePage />} />
      <Route path="/movies/:id" element={<MovieDetailsPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Customer Protected Routes */}
      <Route
        path="/shows/:id/seats"
        element={
          <ProtectedRoute>
            <SeatSelectionPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/bookings/:id/confirm"
        element={
          <ProtectedRoute>
            <BookingConfirmationPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-bookings"
        element={
          <ProtectedRoute>
            <BookingHistoryPage />
          </ProtectedRoute>
        }
      />

      {/* Provider / Admin Protected Routes */}
      <Route
        path="/provider/dashboard"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN']}>
            <ProviderDashboardPage />
          </RoleProtectedRoute>
        }
      />
      <Route
        path="/provider/theatres"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN']}>
            <VenueManagerPage />
          </RoleProtectedRoute>
        }
      />
      <Route
        path="/provider/movies/new"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN']}>
            <MovieManagerPage />
          </RoleProtectedRoute>
        }
      />
      <Route
        path="/provider/shows/new"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN']}>
            <ShowSchedulerPage />
          </RoleProtectedRoute>
        }
      />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/movies" replace />} />
    </Routes>
  );
};
