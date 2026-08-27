import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLocation } from '../../context/LocationContext';
import { convenienceService } from '../../services/convenienceService';
import { Film, LogOut, Ticket, LayoutDashboard, Building2, Calendar, User, Shield, MapPin, Bookmark, Tag, Bell } from 'lucide-react';

export const Navbar = () => {
  const { user, isAuthenticated, isProvider, isAdmin, logout } = useAuth();
  const { selectedLocation, changeLocation, availableLocations } = useLocation();
  const navigate = useNavigate();

  const [unreadNotificationsCount, setUnreadNotificationsCount] = useState(0);
  const [showLocationDropdown, setShowLocationDropdown] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      fetchNotificationsCount();
    }
  }, [isAuthenticated]);

  const fetchNotificationsCount = async () => {
    try {
      const res = await convenienceService.getNotifications();
      if (res.success && res.data) {
        const unread = res.data.filter((n) => !n.isRead).length;
        setUnreadNotificationsCount(unread);
      }
    } catch (err) {
      // Silent catch
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="glass-nav sticky top-0 z-50 px-4 sm:px-6 py-3.5">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
        {/* Left Section: Logo & Location Selector */}
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-purple-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Film className="w-6 h-6 text-white" />
            </div>
            <div>
              <span className="text-xl font-extrabold bg-gradient-to-r from-white via-slate-200 to-indigo-200 bg-clip-text text-transparent">
                PVK Cinema
              </span>
              <span className="block text-[9px] uppercase tracking-widest text-cyan-400 font-bold">
                Booking Platform
              </span>
            </div>
          </Link>

          {/* Location Selector Dropdown */}
          <div className="relative hidden md:block">
            <button
              onClick={() => setShowLocationDropdown(!showLocationDropdown)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-xs font-semibold text-slate-300 hover:text-white hover:border-slate-700 transition-all"
            >
              <MapPin className="w-3.5 h-3.5 text-indigo-400" />
              <span>{selectedLocation}</span>
            </button>

            {showLocationDropdown && (
              <div className="absolute left-0 mt-2 w-40 glass-card p-2 border border-slate-800 shadow-2xl z-50 space-y-1">
                <span className="text-[10px] font-bold text-slate-500 uppercase px-2 py-1 block">Select City</span>
                {availableLocations.map((loc) => (
                  <button
                    key={loc}
                    onClick={() => {
                      changeLocation(loc);
                      setShowLocationDropdown(false);
                    }}
                    className={`w-full text-left px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                      selectedLocation === loc ? 'bg-indigo-600 text-white' : 'text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    {loc}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Center/Right Navigation Links */}
        <div className="flex items-center gap-3 sm:gap-5">
          <Link to="/movies" className="text-xs font-semibold text-slate-300 hover:text-white transition-colors">
            Movies
          </Link>

          <Link to="/theatres" className="text-xs font-semibold text-slate-300 hover:text-white transition-colors flex items-center gap-1">
            <Building2 className="w-3.5 h-3.5 text-indigo-400" />
            Theatres
          </Link>

          <Link to="/offers" className="text-xs font-semibold text-slate-300 hover:text-white transition-colors flex items-center gap-1">
            <Tag className="w-3.5 h-3.5 text-amber-400" />
            Offers
          </Link>

          {isAuthenticated && (
            <>
              <Link to="/watchlist" className="text-xs font-semibold text-slate-300 hover:text-white transition-colors flex items-center gap-1">
                <Bookmark className="w-3.5 h-3.5 text-purple-400" />
                Watchlist
              </Link>

              <Link to="/my-bookings" className="text-xs font-semibold text-slate-300 hover:text-white flex items-center gap-1.5 transition-colors">
                <Ticket className="w-4 h-4 text-indigo-400" />
                My Bookings
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
            </>
          )}

          {/* User Profile & Auth Section */}
          {isAuthenticated ? (
            <div className="flex items-center gap-3 pl-3 border-l border-slate-800">
              <Link to="/customer/profile" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-extrabold text-cyan-300">
                  {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="hidden lg:block">
                  <span className="block text-xs font-bold text-white leading-tight">{user.name}</span>
                  <span className="block text-[9px] text-slate-400 uppercase">{user.role?.replace('ROLE_', '')}</span>
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
            <div className="flex items-center gap-2">
              <Link to="/login" className="btn-secondary text-xs px-3.5 py-1.5">
                Sign In
              </Link>
              <Link to="/register" className="btn-primary text-xs px-3.5 py-1.5">
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};
