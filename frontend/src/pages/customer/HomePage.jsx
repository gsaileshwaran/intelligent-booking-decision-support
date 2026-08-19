import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { movieService } from '../../services/movieService';
import { Search, Film, Calendar, Clock, Sparkles } from 'lucide-react';

export const HomePage = () => {
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [genreFilter, setGenreFilter] = useState('ALL');

  useEffect(() => {
    fetchMovies();
  }, []);

  const fetchMovies = async () => {
    setLoading(true);
    try {
      const res = await movieService.getAllMovies();
      if (res.success && res.data) {
        setMovies(res.data);
      }
    } catch (err) {
      console.error('Failed to load movies:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredMovies = movies.filter((m) => {
    const matchesSearch = m.title.toLowerCase().includes(search.toLowerCase());
    const matchesGenre = genreFilter === 'ALL' || (m.genre && m.genre.includes(genreFilter));
    return matchesSearch && matchesGenre;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Hero Section */}
      <div className="relative rounded-3xl overflow-hidden mb-12 p-8 sm:p-12 bg-gradient-to-r from-slate-900 via-indigo-950/60 to-purple-950/40 border border-slate-800 shadow-2xl">
        <div className="max-w-2xl relative z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-xs font-semibold mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            AI Decision Engine PoC Domain
          </div>
          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight text-white mb-4 leading-tight">
            Seamless Cinema Booking & Instant Seat Reservations
          </h1>
          <p className="text-slate-300 text-base sm:text-lg mb-8 leading-relaxed">
            Discover active showtimes, explore auditorium seat maps, reserve temporary seat holds, and execute verified transactions.
          </p>
          <div className="flex flex-wrap gap-4">
            <a href="#movies-section" className="btn-primary">
              Browse Movies & Shows
            </a>
          </div>
        </div>
      </div>

      {/* Filter & Search Toolbar */}
      <div id="movies-section" className="flex flex-col sm:flex-row gap-4 justify-between items-center mb-8">
        <div>
          <h2 className="text-2xl font-bold">Now Showing Movies</h2>
          <p className="text-xs text-slate-400">Select a movie to explore venue showtimes and available seats</p>
        </div>

        <div className="flex flex-wrap gap-3 w-full sm:w-auto">
          {/* Search Box */}
          <div className="relative flex-1 sm:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by title..."
              className="form-input w-full pl-9 text-xs"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {/* Genre Dropdown */}
          <select
            className="form-input text-xs bg-slate-900 text-slate-200"
            value={genreFilter}
            onChange={(e) => setGenreFilter(e.target.value)}
          >
            <option value="ALL">All Genres</option>
            <option value="Sci-Fi">Sci-Fi</option>
            <option value="Mystery">Mystery</option>
            <option value="Drama">Drama</option>
          </select>
        </div>
      </div>

      {/* Movies Grid */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading cinema catalogue...</div>
      ) : filteredMovies.length === 0 ? (
        <div className="py-16 text-center glass-card p-8">
          <Film className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-slate-300">No Movies Found</h3>
          <p className="text-xs text-slate-400 mt-1">Try adjusting your search criteria or check back later.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {filteredMovies.map((movie) => (
            <div key={movie.movieId} className="glass-card flex flex-col overflow-hidden group">
              <div className="relative aspect-[2/3] bg-slate-800 overflow-hidden">
                {movie.posterUrl ? (
                  <img
                    src={movie.posterUrl}
                    alt={movie.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-slate-900 text-slate-600">
                    <Film className="w-16 h-16" />
                  </div>
                )}
                <div className="absolute top-3 right-3 px-2 py-1 bg-slate-950/80 backdrop-blur-md rounded-md border border-slate-700/50 text-[10px] font-bold text-indigo-300">
                  {movie.censorRating || 'UA'}
                </div>
              </div>

              <div className="p-5 flex flex-col flex-1">
                <h3 className="text-lg font-bold text-white group-hover:text-indigo-400 transition-colors line-clamp-1">
                  {movie.title}
                </h3>
                
                <div className="flex items-center gap-3 text-xs text-slate-400 my-2">
                  <span className="flex items-center gap-1">
                    <Film className="w-3.5 h-3.5 text-slate-500" />
                    {movie.genre || 'Cinema'}
                  </span>
                  <span>&bull;</span>
                  <span className="flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-slate-500" />
                    {movie.duration ? `${movie.duration}m` : '120m'}
                  </span>
                </div>

                <p className="text-xs text-slate-400 line-clamp-2 mb-4 flex-1">
                  {movie.description || 'Experience this feature film in theatres.'}
                </p>

                <Link
                  to={`/movies/${movie.movieId}`}
                  className="btn-primary w-full text-xs py-2.5 justify-center mt-auto"
                >
                  View Shows & Book
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
