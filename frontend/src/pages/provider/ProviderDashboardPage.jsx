import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { providerService } from '../../services/providerService';
import { movieService } from '../../services/movieService';
import { LayoutDashboard, Film, Building2, Calendar, TrendingUp, Users, PlusCircle } from 'lucide-react';

export const ProviderDashboardPage = () => {
  const [theatres, setTheatres] = useState([]);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [theatresRes, moviesRes] = await Promise.all([
        providerService.getTheatres(),
        movieService.getAllMovies(),
      ]);

      if (theatresRes.success) setTheatres(theatresRes.data);
      if (moviesRes.success) setMovies(moviesRes.data);
    } catch (err) {
      console.error('Error loading provider dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">Service Provider Portal</span>
          <h1 className="text-3xl font-extrabold text-white">Operator Dashboard</h1>
          <p className="text-xs text-slate-400 mt-1">Manage venues, screens, movie catalogue, show schedules, and inventory</p>
        </div>

        <div className="flex gap-3">
          <Link to="/provider/theatres" className="btn-secondary text-xs">
            <Building2 className="w-4 h-4" /> Venues
          </Link>
          <Link to="/provider/shows/new" className="btn-primary text-xs">
            <PlusCircle className="w-4 h-4" /> Schedule Show
          </Link>
        </div>
      </div>

      {/* Analytics KPI Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
        <div className="glass-card p-6 flex items-center gap-4 border-indigo-500/30">
          <div className="w-12 h-12 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
            <Building2 className="w-6 h-6" />
          </div>
          <div>
            <span className="block text-xs font-semibold text-slate-400 uppercase">Theatres Managed</span>
            <span className="text-2xl font-extrabold text-white">{theatres.length}</span>
          </div>
        </div>

        <div className="glass-card p-6 flex items-center gap-4 border-purple-500/30">
          <div className="w-12 h-12 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center">
            <Film className="w-6 h-6" />
          </div>
          <div>
            <span className="block text-xs font-semibold text-slate-400 uppercase">Movies Catalogue</span>
            <span className="text-2xl font-extrabold text-white">{movies.length}</span>
          </div>
        </div>

        <div className="glass-card p-6 flex items-center gap-4 border-emerald-500/30">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <span className="block text-xs font-semibold text-slate-400 uppercase">Average Occupancy</span>
            <span className="text-2xl font-extrabold text-emerald-400">78.5%</span>
          </div>
        </div>

        <div className="glass-card p-6 flex items-center gap-4 border-amber-500/30">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <span className="block text-xs font-semibold text-slate-400 uppercase">Active Holds</span>
            <span className="text-2xl font-extrabold text-amber-300">Live</span>
          </div>
        </div>
      </div>

      {/* Operative Modules Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="glass-card p-6 flex flex-col justify-between">
          <div>
            <div className="w-10 h-10 rounded-lg bg-indigo-500/10 text-indigo-400 flex items-center justify-center mb-4 border border-indigo-500/20">
              <Building2 className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Venue & Screen Management</h3>
            <p className="text-xs text-slate-400 mb-6">
              Add new cinema branches, configure screens, auditoriums, and physical seat layouts.
            </p>
          </div>
          <Link to="/provider/theatres" className="btn-secondary text-xs justify-center w-full">
            Manage Venues
          </Link>
        </div>

        <div className="glass-card p-6 flex flex-col justify-between">
          <div>
            <div className="w-10 h-10 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center mb-4 border border-purple-500/20">
              <Film className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Movie Catalogue Builder</h3>
            <p className="text-xs text-slate-400 mb-6">
              Add upcoming film releases, metadata, genres, duration, and censor ratings.
            </p>
          </div>
          <Link to="/provider/movies/new" className="btn-secondary text-xs justify-center w-full">
            Add New Movie
          </Link>
        </div>

        <div className="glass-card p-6 flex flex-col justify-between">
          <div>
            <div className="w-10 h-10 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-4 border border-emerald-500/20">
              <Calendar className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Showtime Scheduler</h3>
            <p className="text-xs text-slate-400 mb-6">
              Schedule movie showtimes, set base ticket prices, and auto-generate seat inventory.
            </p>
          </div>
          <Link to="/provider/shows/new" className="btn-primary text-xs justify-center w-full">
            Schedule Show
          </Link>
        </div>
      </div>
    </div>
  );
};
