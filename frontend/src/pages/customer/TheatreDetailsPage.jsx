import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { convenienceService } from '../../services/convenienceService';
import { Building2, MapPin, Film, Clock, ArrowLeft, ShieldCheck, Ticket, Sparkles, Phone, Compass, Info } from 'lucide-react';

export const TheatreDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [theatreDetails, setTheatreDetails] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(0); // 0 = today, 1 = tomorrow, etc.

  useEffect(() => {
    fetchTheatreDetails();
  }, [id]);

  const fetchTheatreDetails = async () => {
    setLoading(true);
    try {
      const res = await convenienceService.getTheatreById(id);
      if (res.success && res.data) {
        setTheatreDetails(res.data);
      }
    } catch (err) {
      console.error('Failed to load theatre details:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading cinema branch details and live schedules...</div>;
  }

  if (!theatreDetails) {
    return (
      <div className="max-w-md mx-auto my-20 p-8 glass-card text-center">
        <p className="text-red-400 text-sm mb-4">Theatre branch not found.</p>
        <button onClick={() => navigate('/theatres')} className="btn-secondary text-xs">
          Back to Venues
        </button>
      </div>
    );
  }

  const { name, location, city, locality, address, description, phone, operatingHours, amenities, formats, screens = [], shows = [] } = theatreDetails;

  // Build Date Tabs (Today + Next 5 Days)
  const dateOptions = [];
  const today = new Date();
  for (let i = 0; i < 6; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    const dateStr = d.toISOString().split('T')[0];
    const label = i === 0 ? 'TODAY' : i === 1 ? 'TOMORROW' : d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    dateOptions.push({ index: i, dateStr, label });
  }

  const activeTargetDate = dateOptions[selectedDate]?.dateStr;

  // Filter shows by selected date
  const filteredShows = shows.filter((s) => s.showDate === activeTargetDate);

  // Group shows by Movie
  const showsByMovie = filteredShows.reduce((acc, show) => {
    const movieTitle = show.movie?.title || 'Scheduled Film';
    if (!acc[movieTitle]) {
      acc[movieTitle] = {
        movie: show.movie,
        shows: [],
      };
    }
    acc[movieTitle].shows.push(show);
    return acc;
  }, {});

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate('/theatres')}
        className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Multiplex Directory
      </button>

      {/* Theatre Venue Banner Header */}
      <div className="glass-card p-6 sm:p-8 mb-10 border border-slate-800 relative overflow-hidden">
        <div className="absolute -right-10 -top-10 w-60 h-60 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none"></div>

        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="badge badge-confirmed font-extrabold flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-cyan-400" /> PVK NATIONAL NETWORK
              </span>
              <span className="text-xs text-indigo-400 font-bold">{city || location}</span>
            </div>
            <h1 className="text-3xl font-black text-white mb-2 flex items-center gap-3">
              <Building2 className="w-8 h-8 text-indigo-400" /> {name}
            </h1>
            <p className="text-xs text-slate-400 flex items-center gap-1.5 mb-2">
              <MapPin className="w-4 h-4 text-slate-500" /> {address || `${locality}, ${city || location}`}
            </p>
            {phone && (
              <p className="text-xs text-slate-400 flex items-center gap-1.5">
                <Phone className="w-3.5 h-3.5 text-slate-500" /> Contact: <span className="text-slate-300 font-semibold">{phone}</span> &bull; <Clock className="w-3.5 h-3.5 text-slate-500 ml-2" /> Hours: <span className="text-slate-300 font-semibold">{operatingHours || '09:00 AM - 11:45 PM'}</span>
              </p>
            )}
          </div>

          {/* Formats Pills */}
          {formats && (
            <div className="flex flex-wrap gap-2 text-xs">
              {formats.split(',').map((f, idx) => (
                <span key={idx} className="px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-700 text-cyan-300 font-extrabold shadow-sm">
                  {f.trim()}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Description / About */}
        {description && (
          <div className="mt-6 pt-4 border-t border-slate-800/80 text-xs text-slate-300 leading-relaxed flex items-start gap-2">
            <Info className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
            <p>{description}</p>
          </div>
        )}

        {/* Amenities & Auditoriums */}
        <div className="mt-6 pt-6 border-t border-slate-800 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <span className="text-xs font-bold text-slate-400 block mb-2 uppercase tracking-wider">
              Auditoriums ({screens.length} Screens)
            </span>
            <div className="flex flex-wrap gap-2">
              {screens.map((sc) => (
                <div key={sc.screenId} className="px-3 py-1.5 rounded-lg bg-slate-900/80 border border-slate-800 text-xs">
                  <span className="font-bold text-white block">{sc.name}</span>
                  <span className="text-[10px] text-slate-500">Capacity: {sc.capacity} seats</span>
                </div>
              ))}
            </div>
          </div>

          {amenities && (
            <div>
              <span className="text-xs font-bold text-slate-400 block mb-2 uppercase tracking-wider">
                Venue Amenities
              </span>
              <div className="flex flex-wrap gap-1.5">
                {amenities.split(',').map((am, idx) => (
                  <span key={idx} className="px-2.5 py-1 rounded bg-slate-900/90 text-slate-300 border border-slate-800 text-[11px] font-semibold flex items-center gap-1">
                    <ShieldCheck className="w-3 h-3 text-emerald-400" /> {am.trim()}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Date Selector Bar */}
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-white mb-4">Select Show Date</h2>
        <div className="flex items-center gap-2 overflow-x-auto pb-2">
          {dateOptions.map((opt) => (
            <button
              key={opt.index}
              onClick={() => setSelectedDate(opt.index)}
              className={`px-5 py-2.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all border ${
                selectedDate === opt.index
                  ? 'bg-indigo-600 border-indigo-500 text-white shadow-lg shadow-indigo-500/25 scale-105'
                  : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-white hover:border-slate-700'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* Shows Scheduled at this Theatre for Selected Date */}
      <h2 className="text-xl font-extrabold text-white mb-6">
        Now Showing on {dateOptions[selectedDate]?.label}
      </h2>

      {Object.keys(showsByMovie).length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Film className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-base font-bold text-slate-300">No Showtimes Scheduled</h3>
          <p className="text-xs text-slate-500 mt-1">No showtimes are currently scheduled at {name} for this selected date.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(showsByMovie).map(([movieTitle, { movie, shows: movieShows }]) => (
            <div key={movieTitle} className="glass-card p-6 border border-slate-800 hover:border-indigo-500/30 transition-all">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-800 pb-4 mb-4 gap-2">
                <div>
                  <h3 className="text-lg font-black text-white flex items-center gap-2">
                    <Film className="w-5 h-5 text-indigo-400" />
                    {movieTitle}
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    {movie?.genre} &bull; {movie?.duration} mins &bull; {movie?.censorRating || 'U/A'}
                  </p>
                </div>
                <button
                  onClick={() => navigate(`/movies/${movie?.movieId}`)}
                  className="btn-secondary text-xs py-1.5 px-3 self-start sm:self-auto"
                >
                  Movie Details
                </button>
              </div>

              {/* Showtimes */}
              <div className="flex flex-wrap gap-3">
                {movieShows.map((show) => (
                  <button
                    key={show.showId}
                    onClick={() => navigate(`/shows/${show.showId}/seats`)}
                    className="px-4 py-3 rounded-xl bg-slate-900 hover:bg-indigo-600/40 border border-slate-800 hover:border-indigo-500/60 text-left transition-all group"
                  >
                    <div className="flex items-center gap-1.5 text-xs font-black text-indigo-300 group-hover:text-white">
                      <Clock className="w-3.5 h-3.5" />
                      <span>{show.startTime?.substring(0, 5)}</span>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-1">
                      {show.screen?.name} &bull; <strong className="text-emerald-400">₹{show.ticketPrice?.toFixed(2)}</strong>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
