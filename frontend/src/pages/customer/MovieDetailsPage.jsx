import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { movieService } from '../../services/movieService';
import { showService } from '../../services/showService';
import { Film, Clock, Calendar, MapPin, Ticket, ArrowLeft } from 'lucide-react';

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

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading showtimes and venue data...</div>;
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
        <div className="w-full md:w-56 aspect-[2/3] rounded-xl overflow-hidden bg-slate-800 shrink-0">
          {movie.posterUrl ? (
            <img src={movie.posterUrl} alt={movie.title} className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-slate-600">
              <Film className="w-16 h-16" />
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

      {/* Available Showtimes Section */}
      <h2 className="text-2xl font-bold mb-6">Available Showtimes</h2>

      {shows.length === 0 ? (
        <div className="glass-card p-8 text-center text-slate-400">
          No showtimes scheduled for this movie currently.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {shows.map((show) => (
            <div key={show.showId} className="glass-card p-6 flex flex-col justify-between gap-4">
              <div>
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-lg font-bold text-white">
                      {show.screen?.theatre?.name || 'Grand Cinema Venue'}
                    </h3>
                    <p className="text-xs text-slate-400 flex items-center gap-1 mt-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-500" />
                      {show.screen?.theatre?.location || 'Downtown Location'}
                    </p>
                  </div>
                  <span className="text-xs font-bold px-2.5 py-1 rounded-md bg-purple-500/10 text-purple-300 border border-purple-500/20">
                    {show.screen?.name || 'Auditorium'}
                  </span>
                </div>

                <div className="flex items-center gap-4 mt-4 pt-4 border-t border-slate-800 text-xs">
                  <div>
                    <span className="block text-slate-500 text-[10px]">Show Date</span>
                    <span className="font-semibold text-slate-200">{show.showDate}</span>
                  </div>
                  <div>
                    <span className="block text-slate-500 text-[10px]">Start Time</span>
                    <span className="font-semibold text-indigo-300">{show.startTime}</span>
                  </div>
                  <div>
                    <span className="block text-slate-500 text-[10px]">Base Price</span>
                    <span className="font-semibold text-emerald-400">${show.ticketPrice?.toFixed(2)}</span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => navigate(`/shows/${show.showId}/seats`)}
                className="btn-primary w-full text-xs justify-center py-2.5 mt-2"
              >
                <Ticket className="w-4 h-4" /> Select Seats & Reserve
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
