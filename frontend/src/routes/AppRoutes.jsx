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
import { CustomerProfilePage } from '../pages/customer/CustomerProfilePage';
import { LoginPage } from '../pages/customer/LoginPage';
import { RegisterPage } from '../pages/customer/RegisterPage';
import { TheatresPage } from '../pages/customer/TheatresPage';
import { TheatreDetailsPage } from '../pages/customer/TheatreDetailsPage';
import { OffersPage } from '../pages/customer/OffersPage';
import { WatchlistPage } from '../pages/customer/WatchlistPage';
import { DigitalTicketPage } from '../pages/customer/DigitalTicketPage';

// Provider Pages
import { ProviderDashboardPage } from '../pages/provider/ProviderDashboardPage';
import { VenueManagerPage } from '../pages/provider/VenueManagerPage';
import { MovieManagerPage } from '../pages/provider/MovieManagerPage';
import { ShowSchedulerPage } from '../pages/provider/ShowSchedulerPage';
import { ProviderBookingsPage } from '../pages/provider/ProviderBookingsPage';
import { ProviderShowsPage } from '../pages/provider/ProviderShowsPage';

// Admin Pages
import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage';

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<Navigate to="/movies" replace />} />
      <Route path="/movies" element={<HomePage />} />
      <Route path="/movies/:id" element={<MovieDetailsPage />} />
      <Route path="/theatres" element={<TheatresPage />} />
      <Route path="/theatres/:id" element={<TheatreDetailsPage />} />
      <Route path="/offers" element={<OffersPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Customer Protected Routes */}
      <Route
        path="/watchlist"
        element={
          <ProtectedRoute>
            <WatchlistPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/tickets/:id"
        element={
          <ProtectedRoute>
            <DigitalTicketPage />
          </ProtectedRoute>
        }
      />
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
      <Route
        path="/customer/profile"
        element={
          <ProtectedRoute>
            <CustomerProfilePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <CustomerProfilePage />
          </ProtectedRoute>
        }
      />

      {/* Admin Protected Routes */}
      <Route
        path="/admin/dashboard"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <AdminDashboardPage />
          </RoleProtectedRoute>
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
      <Route
        path="/provider/shows"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN']}>
            <ProviderShowsPage />
          </RoleProtectedRoute>
        }
      />
      <Route
        path="/provider/bookings"
        element={
          <RoleProtectedRoute allowedRoles={['ROLE_SERVICE_PROVIDER', 'ROLE_ADMIN']}>
            <ProviderBookingsPage />
          </RoleProtectedRoute>
        }
      />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/movies" replace />} />
    </Routes>
  );
};
