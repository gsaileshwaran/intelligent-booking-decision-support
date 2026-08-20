import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Film, LogOut, Ticket, LayoutDashboard, Building2, Calendar, User, Shield } from 'lucide-react';

export const Navbar = () => {
  const { user, isAuthenticated, isProvider, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="glass-nav sticky top-0 z-50 px-6 py-4">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-purple-500 flex items-center justify-center shadow-lg shadow-indigo-500/30">
            <Film className="w-6 h-6 text-white" />
          </div>
          <div>
            <span className="text-xl font-bold bg-gradient-to-r from-white via-slate-200 to-indigo-200 bg-clip-text text-transparent">
              CineBooking
            </span>
            <span className="block text-[10px] uppercase tracking-wider text-indigo-400 font-semibold">
              Transactional Platform
            </span>
          </div>
        </Link>

        {/* Navigation Links */}
        <div className="flex items-center gap-4 sm:gap-5">
          <Link to="/movies" className="text-xs font-medium text-slate-300 hover:text-white transition-colors">
            Movies
          </Link>
          
          {isAuthenticated && (
            <>
              <Link to="/my-bookings" className="text-xs font-medium text-slate-300 hover:text-white flex items-center gap-1.5 transition-colors">
                <Ticket className="w-4 h-4 text-indigo-400" />
                My Bookings
              </Link>

              <Link to="/customer/profile" className="text-xs font-medium text-slate-300 hover:text-white flex items-center gap-1.5 transition-colors">
                <User className="w-4 h-4 text-purple-400" />
                Profile
              </Link>
            </>
          )}

          {isAdmin && (
            <Link to="/admin/dashboard" className="text-xs font-semibold text-amber-400 hover:text-amber-300 flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-amber-500/10 border border-amber-500/20 transition-all">
              <Shield className="w-3.5 h-3.5" />
              Admin
            </Link>
          )}

          {isProvider && (
            <>
              <Link to="/provider/dashboard" className="text-xs font-semibold text-purple-400 hover:text-purple-300 flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-purple-500/10 border border-purple-500/20 transition-all">
                <LayoutDashboard className="w-3.5 h-3.5" />
                Provider
              </Link>
              <Link to="/provider/theatres" className="text-xs font-semibold text-indigo-300 hover:text-white flex items-center gap-1 transition-colors">
                <Building2 className="w-3.5 h-3.5" />
                Branches
              </Link>
              <Link to="/provider/shows" className="text-xs font-semibold text-purple-300 hover:text-white flex items-center gap-1 transition-colors">
                <Calendar className="w-3.5 h-3.5" />
                Shows
              </Link>
              <Link to="/provider/bookings" className="text-xs font-semibold text-emerald-300 hover:text-white flex items-center gap-1 transition-colors">
                <Ticket className="w-3.5 h-3.5" />
                Bookings
              </Link>
            </>
          )}

          {/* User Section */}
          {isAuthenticated ? (
            <div className="flex items-center gap-4 pl-4 border-l border-slate-700/60">
              <Link to="/customer/profile" className="flex items-center gap-2.5 hover:opacity-80 transition-opacity">
                <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-bold text-indigo-400">
                  {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="hidden sm:block">
                  <span className="block text-xs font-semibold text-white">{user.name}</span>
                  <span className="block text-[10px] text-slate-400 uppercase">{user.role?.replace('ROLE_', '')}</span>
                </div>
              </Link>
              <button
                onClick={handleLogout}
                className="p-2 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                title="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link to="/login" className="btn-secondary text-xs px-4 py-2">
                Sign In
              </Link>
              <Link to="/register" className="btn-primary text-xs px-4 py-2">
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};
