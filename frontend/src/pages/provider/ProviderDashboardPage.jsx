import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { providerService } from '../../services/providerService';
import { movieService } from '../../services/movieService';
import { Film, Building2, Calendar, TrendingUp, Users, PlusCircle, Tv, Ticket } from 'lucide-react';

export const ProviderDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [statsRes, moviesRes] = await Promise.all([
        providerService.getDashboardStats(),
        movieService.getAllMovies(),
      ]);

      if (statsRes.success) setStats(statsRes.data);
      if (moviesRes.success) setMovies(moviesRes.data);
    } catch (err) {
      console.error('Error loading provider dashboard stats:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">PVK Operator Portal</span>
          <h1 className="text-3xl font-extrabold text-white">Cinema Chain Dashboard</h1>
          <p className="text-xs text-slate-400 mt-1">Manage PVK branches, auditoriums, seating layouts, shows, bookings, and revenue</p>
        </div>

        <div className="flex gap-3">
          <Link to="/provider/bookings" className="btn-secondary text-xs">
            <Ticket className="w-4 h-4" /> Bookings
          </Link>
          <Link to="/provider/shows/new" className="btn-primary text-xs">
            <PlusCircle className="w-4 h-4" /> Schedule Show
          </Link>
        </div>
      </div>

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading live chain metrics...</div>
      ) : (
        <>
          {/* Analytics KPI Stat Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
            <div className="glass-card p-6 flex items-center gap-4 border-indigo-500/30">
              <div className="w-12 h-12 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
                <Building2 className="w-6 h-6" />
              </div>
              <div>
                <span className="block text-xs font-semibold text-slate-400 uppercase">PVK Branches</span>
                <span className="text-2xl font-extrabold text-white">{stats?.totalBranches || 0}</span>
                <span className="block text-[10px] text-slate-500 mt-0.5">{stats?.totalScreens || 0} Screens | {stats?.totalSeats || 0} Seats</span>
              </div>
            </div>

            <div className="glass-card p-6 flex items-center gap-4 border-purple-500/30">
              <div className="w-12 h-12 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center">
                <Calendar className="w-6 h-6" />
              </div>
              <div>
                <span className="block text-xs font-semibold text-slate-400 uppercase">Today's Shows</span>
                <span className="text-2xl font-extrabold text-purple-300">{stats?.todaysShows || 0}</span>
                <span className="block text-[10px] text-slate-500 mt-0.5">{stats?.upcomingShows || 0} Upcoming Shows</span>
              </div>
            </div>

            <div className="glass-card p-6 flex items-center gap-4 border-emerald-500/30">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                <TrendingUp className="w-6 h-6" />
              </div>
              <div>
                <span className="block text-xs font-semibold text-slate-400 uppercase">Confirmed Revenue</span>
                <span className="text-2xl font-extrabold text-emerald-400">₹{(stats?.totalRevenue || 0).toFixed(2)}</span>
                <span className="block text-[10px] text-slate-500 mt-0.5">{stats?.confirmedBookings || 0} Confirmed Bookings</span>
              </div>
            </div>

            <div className="glass-card p-6 flex items-center gap-4 border-amber-500/30">
              <div className="w-12 h-12 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center">
                <Ticket className="w-6 h-6" />
              </div>
              <div>
                <span className="block text-xs font-semibold text-slate-400 uppercase">Total Bookings</span>
                <span className="text-2xl font-extrabold text-amber-300">{stats?.totalBookings || 0}</span>
                <span className="block text-[10px] text-slate-500 mt-0.5">{stats?.confirmedBookings || 0} Confirmed</span>
              </div>
            </div>
          </div>

          {/* Operative Modules Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="glass-card p-6 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-indigo-500/10 text-indigo-400 flex items-center justify-center mb-4 border border-indigo-500/20">
                  <Building2 className="w-5 h-5" />
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Branch Manager</h3>
                <p className="text-xs text-slate-400 mb-6">
                  Inspect PVK branches in Anna Nagar, OMR, Velachery, and T. Nagar. View auditoriums and seat layouts.
                </p>
              </div>
              <Link to="/provider/theatres" className="btn-secondary text-xs justify-center w-full">
                Manage Branches
              </Link>
            </div>

            <div className="glass-card p-6 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center mb-4 border border-amber-500/20">
                  <Ticket className="w-5 h-5" />
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Booking Management</h3>
                <p className="text-xs text-slate-400 mb-6">
                  Track real-time bookings across PVK branches. Filter by branch, show date, or booking status.
                </p>
              </div>
              <Link to="/provider/bookings" className="btn-secondary text-xs justify-center w-full">
                View All Bookings
              </Link>
            </div>

            <div className="glass-card p-6 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center mb-4 border border-purple-500/20">
                  <Film className="w-5 h-5" />
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Movie Catalogue</h3>
                <p className="text-xs text-slate-400 mb-6">
                  Add upcoming film releases, genres, language, duration, and censor ratings.
                </p>
              </div>
              <Link to="/provider/movies/new" className="btn-secondary text-xs justify-center w-full">
                Add Movie
              </Link>
            </div>

            <div className="glass-card p-6 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-4 border border-emerald-500/20">
                  <Calendar className="w-5 h-5" />
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Show Scheduler</h3>
                <p className="text-xs text-slate-400 mb-6">
                  Schedule showtimes across PVK auditoriums, set ticket prices (₹), and auto-generate seat maps.
                </p>
              </div>
              <Link to="/provider/shows/new" className="btn-primary text-xs justify-center w-full">
                Schedule Show
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
