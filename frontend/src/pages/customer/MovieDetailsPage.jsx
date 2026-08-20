import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { movieService } from '../../services/movieService';
import { showService } from '../../services/showService';
import { Film, Clock, MapPin, Ticket, ArrowLeft, Building2 } from 'lucide-react';

export const MovieDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [movie, setMovie] = useState(null);
  const [shows, setShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchMovieAndShows();
  }, [id]);

  const fetchMovieAndShows = async () => {
    setLoading(true);
    try {
      const [movieRes, showsRes] = await Promise.all([
        movieService.getMovieById(id),
        showService.getShows(id),
      ]);

      if (movieRes.success) setMovie(movieRes.data);
      if (showsRes.success) setShows(showsRes.data);
    } catch (err) {
      setError('Failed to fetch movie details or available showtimes.');
    } finally {
      setLoading(false);
    }
  };

  // Group shows by Theatre
  const showsByTheatre = shows.reduce((acc, show) => {
    const theatreName = show.screen?.theatre?.name || 'Grand Cinema Venue';
    if (!acc[theatreName]) {
      acc[theatreName] = {
        theatre: show.screen?.theatre,
        shows: [],
      };
    }
    acc[theatreName].shows.push(show);
    return acc;
  }, {});

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading showtimes and multi-venue schedule...</div>;
  }

  if (error || !movie) {
    return (
      <div className="max-w-md mx-auto my-20 p-8 glass-card text-center">
        <p className="text-red-400 text-sm mb-4">{error || 'Movie not found.'}</p>
        <button onClick={() => navigate('/movies')} className="btn-secondary text-xs">
          Back to Movies
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate('/movies')}
        className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Movies
      </button>

      {/* Header Banner */}
      <div className="glass-card p-6 sm:p-8 mb-10 flex flex-col md:flex-row gap-8">
        <div className="w-full md:w-56 aspect-[2/3] rounded-xl overflow-hidden bg-slate-800 shrink-0 shadow-xl border border-slate-700/50">
          {movie.posterUrl ? (
            <img src={movie.posterUrl} alt={movie.title} className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-slate-600 bg-gradient-to-tr from-slate-900 to-indigo-950">
              <Film className="w-16 h-16 text-indigo-400/40" />
            </div>
          )}
        </div>

        <div className="flex-1 flex flex-col justify-center">
          <div className="flex items-center gap-3 mb-2">
            <span className="px-2.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 text-xs font-bold border border-indigo-500/30">
              {movie.censorRating || 'UA'}
            </span>
            <span className="text-xs text-slate-400">{movie.language || 'English'}</span>
          </div>

          <h1 className="text-3xl sm:text-4xl font-extrabold text-white mb-3">{movie.title}</h1>

          <div className="flex flex-wrap gap-4 text-xs text-slate-300 mb-4">
            <span className="flex items-center gap-1.5">
              <Film className="w-4 h-4 text-indigo-400" /> {movie.genre}
            </span>
            <span className="flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-indigo-400" /> {movie.duration} minutes
            </span>
          </div>

          <p className="text-slate-300 text-sm leading-relaxed mb-6">
            {movie.description || 'No synopsis available.'}
          </p>
        </div>
      </div>

      {/* Available Showtimes Section - Grouped by Theatre */}
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-white">Available Venues & Showtimes</h2>
        <span className="text-xs text-slate-400 font-medium">Select a time slot to reserve seats</span>
      </div>

      {shows.length === 0 ? (
        <div className="glass-card p-8 text-center text-slate-400">
          No showtimes scheduled for this movie currently.
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(showsByTheatre).map(([theatreName, { theatre, shows: venueShows }]) => (
            <div key={theatreName} className="glass-card p-6 border border-slate-800 hover:border-slate-700 transition-all">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-800 pb-4 mb-4 gap-2">
                <div>
                  <h3 className="text-lg font-bold text-white flex items-center gap-2">
                    <Building2 className="w-5 h-5 text-indigo-400" />
                    {theatreName}
                  </h3>
                  <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                    <MapPin className="w-3.5 h-3.5 text-slate-500" />
                    {theatre?.address || theatre?.location || 'Chennai'}
                  </p>
                </div>
                <span className="text-xs px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 font-semibold self-start sm:self-auto">
                  {venueShows.length} Showtimes Available
                </span>
              </div>

              {/* Group showtimes by Screen Name */}
              <div className="space-y-4">
                {Object.entries(
                  venueShows.reduce((acc, show) => {
                    const screenName = show.screen?.name || 'Standard Screen';
                    if (!acc[screenName]) acc[screenName] = [];
                    acc[screenName].push(show);
                    return acc;
                  }, {})
                ).map(([screenName, screenShows]) => (
                  <div key={screenName} className="flex flex-col md:flex-row items-start md:items-center gap-4 bg-slate-900/60 p-4 rounded-xl border border-slate-800/80">
                    <div className="w-48 shrink-0">
                      <span className="text-xs font-bold text-purple-300 block">{screenName}</span>
                      <span className="text-[10px] text-slate-500">Cap: {screenShows[0]?.screen?.capacity || 60} seats</span>
                    </div>

                    <div className="flex flex-wrap gap-3 flex-1">
                      {screenShows.map((show) => (
                        <button
                          key={show.showId}
                          onClick={() => navigate(`/shows/${show.showId}/seats`)}
                          className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-indigo-600/30 border border-slate-700 hover:border-indigo-500/50 text-left transition-all group"
                        >
                          <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-300 group-hover:text-white">
                            <Clock className="w-3.5 h-3.5" />
                            <span>{show.startTime?.substring(0, 5)}</span>
                          </div>
                          <div className="text-[11px] text-emerald-400 font-semibold mt-0.5">
                            ₹{show.ticketPrice?.toFixed(2)}
                          </div>
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
