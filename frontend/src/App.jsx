import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { BookingProvider } from './context/BookingContext';
import { LocationProvider } from './context/LocationContext';
import { Navbar } from './components/layout/Navbar';
import { Footer } from './components/layout/Footer';
import { AppRoutes } from './routes/AppRoutes';

export default function App() {
  return (
    <Router>
      <AuthProvider>
        <BookingProvider>
          <LocationProvider>
            <div className="min-h-screen flex flex-col justify-between">
              <Navbar />
              <main className="flex-1">
                <AppRoutes />
              </main>
              <Footer />
            </div>
          </LocationProvider>
        </BookingProvider>
      </AuthProvider>
    </Router>
  );
}
