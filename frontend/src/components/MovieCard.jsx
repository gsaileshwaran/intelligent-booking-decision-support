import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bookmark, Star, Calendar, Flame, Eye } from 'lucide-react';

const FALLBACK_POSTER = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80";

export const MovieCard = ({ movie, isFavorite, onToggleFavorite, onOpenComparison }) => {
  const navigate = useNavigate();
  const [imgSrc, setImgSrc] = useState(movie.posterUrl || movie.thumbnailUrl || FALLBACK_POSTER);
  const [imgError, setImgError] = useState(false);

  const handleImgError = () => {
    if (!imgError) {
      setImgError(true);
      setImgSrc(FALLBACK_POSTER);
    }
  };

  const formatReleaseDate = () => {
    if (movie.releaseDateStatus === 'TBA' || !movie.releaseDate) {
      return `${movie.releaseYear || 2027} — DATE TBA`;
    }
    if (movie.releaseDateStatus === 'ANNOUNCED') {
      try {
        const d = new Date(movie.releaseDate);
        return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
      } catch {
        return `ANNOUNCED ${movie.releaseYear || 2027}`;
      }
    }
    try {
      const d = new Date(movie.releaseDate);
      return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return `${movie.releaseDate}`;
    }
  };

  const isUpcoming = movie.isUpcoming === true || (!movie.isNowShowing && movie.releaseDate && new Date(movie.releaseDate) > new Date());
  const isBookable = !isUpcoming;

  return (
    <div className="glass-card flex flex-col overflow-hidden group hover:border-indigo-500/50 transition-all duration-300 shadow-xl hover:shadow-indigo-500/10">
      {/* Poster Container (2:3 Aspect Ratio) */}
      <div className="relative aspect-[2/3] bg-slate-900 overflow-hidden cursor-pointer" onClick={() => navigate(`/movies/${movie.movieId}`)}>
        <img
          src={imgSrc}
          alt={movie.title}
          onError={handleImgError}
          loading="lazy"
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
        />

        {/* Gradient Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/20 to-transparent opacity-80 group-hover:opacity-60 transition-opacity" />

        {/* Top Badges */}
        <div className="absolute top-3 left-3 right-3 flex items-center justify-between pointer-events-none">
          <div className="flex flex-wrap gap-1.5">
            <span className="px-2 py-0.5 bg-slate-950/80 backdrop-blur-md rounded border border-slate-700/60 text-[10px] font-extrabold text-indigo-300 uppercase">
              {movie.censorRating || 'U/A'}
            </span>

            {/* Strictly Separated Rating vs Anticipation Badge */}
            {isUpcoming ? (
              movie.anticipationScore ? (
                <span className="px-2 py-0.5 bg-rose-500/20 backdrop-blur-md rounded border border-rose-500/40 text-[10px] font-extrabold text-rose-300 flex items-center gap-1">
                  <Flame className="w-3 h-3 text-rose-400 fill-rose-400" /> HYPE {movie.anticipationScore}/100
                </span>
              ) : null
            ) : movie.rating ? (
              <span className="px-2 py-0.5 bg-amber-500/20 backdrop-blur-md rounded border border-amber-500/40 text-[10px] font-extrabold text-amber-300 flex items-center gap-1">
                <Star className="w-3 h-3 fill-amber-400 text-amber-400" /> Rating: {movie.rating}
              </span>
            ) : (
              <span className="px-2 py-0.5 bg-slate-800/80 backdrop-blur-md rounded border border-slate-700 text-[10px] font-bold text-slate-400">
                Rating: N/A
              </span>
            )}
          </div>

          {/* Watchlist Toggle Bookmark */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleFavorite(movie.movieId, e);
            }}
            className={`p-2 rounded-full backdrop-blur-md transition-all pointer-events-auto shadow-lg ${
              isFavorite
                ? 'bg-purple-600 text-white border border-purple-400'
                : 'bg-slate-950/70 text-slate-300 hover:text-white hover:bg-slate-900 border border-slate-700/50'
            }`}
            title={isFavorite ? 'Remove from Watchlist' : 'Add to Watchlist'}
          >
            <Bookmark className={`w-3.5 h-3.5 ${isFavorite ? 'fill-white' : ''}`} />
          </button>
        </div>

        {/* Status Indicator Overlay at Bottom of Image */}
        <div className="absolute bottom-3 left-3 right-3">
          <span className={`inline-block px-2.5 py-0.5 rounded-full text-[10px] font-bold backdrop-blur-md border ${
            isBookable
              ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300'
              : 'bg-indigo-500/20 border-indigo-500/40 text-indigo-300'
          }`}>
            {isBookable ? 'NOW SHOWING' : `COMING SOON • ${formatReleaseDate()}`}
          </span>
        </div>
      </div>

      {/* Card Info Body */}
      <div className="p-4 flex flex-col flex-1 bg-slate-950/60">
        <h3
          onClick={() => navigate(`/movies/${movie.movieId}`)}
          className="text-base font-bold text-white group-hover:text-indigo-400 transition-colors line-clamp-1 cursor-pointer"
        >
          {movie.title}
        </h3>

        <div className="flex items-center gap-2 text-[11px] text-slate-400 my-1.5 font-medium">
          <span className="text-cyan-300 font-semibold">{movie.language || 'English'}</span>
          <span>&bull;</span>
          <span className="line-clamp-1">{movie.genre || 'Action'}</span>
        </div>

        {movie.director && (
          <p className="text-[11px] text-slate-400 line-clamp-1">
            <strong className="text-slate-300">Director:</strong> {movie.director}
          </p>
        )}

        {movie.cast && (
          <p className="text-[11px] text-slate-400 line-clamp-1 mb-3">
            <strong className="text-slate-300">Cast:</strong> {movie.cast}
          </p>
        )}

        {/* Quick Action Footer */}
        <div className="flex gap-2 mt-auto pt-2 border-t border-slate-900">
          <Link
            to={`/movies/${movie.movieId}`}
            className={`btn-primary flex-1 text-xs py-2 justify-center font-bold ${
              !isBookable ? 'bg-indigo-600/40 hover:bg-indigo-600' : ''
            }`}
          >
            {isBookable ? 'Book Showtimes' : 'View Release Details'}
          </Link>

          {onOpenComparison && isBookable && (
            <button
              onClick={() => onOpenComparison(movie.movieId)}
              className="btn-secondary p-2 text-xs"
              title="Compare Venues"
            >
              <Eye className="w-4 h-4 text-cyan-400" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
