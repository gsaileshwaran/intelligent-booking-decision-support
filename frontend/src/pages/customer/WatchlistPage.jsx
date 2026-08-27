import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { convenienceService } from '../../services/convenienceService';
import { Bookmark, Film, Trash2, Ticket, Clock } from 'lucide-react';

export const WatchlistPage = () => {
  const navigate = useNavigate();

  const [favorites, setFavorites] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchFavorites();
  }, []);

  const fetchFavorites = async () => {
    setLoading(true);
    try {
      const res = await convenienceService.getFavorites();
      if (res.success && res.data) {
        setFavorites(res.data);
      }
    } catch (err) {
      console.error('Failed to load favorites:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveFavorite = async (movieId) => {
    try {
      await convenienceService.toggleFavorite(movieId);
      setFavorites((prev) => prev.filter((m) => m.movieId !== movieId));
    } catch (err) {
      console.error('Failed to remove favorite:', err);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="mb-8 flex items-center justify-between">
        <div>
          <span className="badge badge-confirmed mb-1">PERSONAL WATCHLIST</span>
          <h1 className="text-3xl font-extrabold text-white flex items-center gap-2">
            <Bookmark className="w-7 h-7 text-indigo-400 fill-indigo-400" /> Saved Movies
          </h1>
          <p className="text-xs text-slate-400 mt-1">Keep track of movies you want to watch and check live show schedules</p>
        </div>
      </div>

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading your saved watchlist...</div>
      ) : favorites.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400 max-w-md mx-auto">
          <Bookmark className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">Your Watchlist is Empty</h3>
          <p className="text-xs text-slate-500 mt-1 mb-6">Explore movies and click the bookmark icon to save them here.</p>
          <button onClick={() => navigate('/movies')} className="btn-primary text-xs mx-auto">
            Browse Movies
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {favorites.map((movie) => (
            <div key={movie.movieId} className="glass-card overflow-hidden group flex flex-col justify-between">
              <div>
                <div className="aspect-[2/3] bg-slate-800 relative overflow-hidden">
                  {movie.posterUrl ? (
                    <img src={movie.posterUrl} alt={movie.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-slate-600 bg-gradient-to-tr from-slate-900 to-indigo-950">
                      <Film className="w-12 h-12 text-indigo-400/40" />
                    </div>
                  )}
                  <button
                    onClick={() => handleRemoveFavorite(movie.movieId)}
                    className="absolute top-3 right-3 p-2 rounded-full bg-slate-950/80 hover:bg-red-500 text-slate-300 hover:text-white transition-all shadow-lg"
                    title="Remove from Watchlist"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                <div className="p-4">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 text-[10px] font-bold">
                      {movie.censorRating || 'UA'}
                    </span>
                    <span className="text-[10px] text-slate-400">{movie.genre}</span>
                  </div>
                  <h3 className="font-bold text-white text-base truncate">{movie.title}</h3>
                </div>
              </div>

              <div className="p-4 pt-0">
                <button
                  onClick={() => navigate(`/movies/${movie.movieId}`)}
                  className="btn-primary w-full py-2 text-xs font-semibold justify-center"
                >
                  Book Showtimes <Ticket className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
