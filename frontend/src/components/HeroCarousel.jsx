import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Sparkles, Star, Play, Ticket, ArrowRight, Pause, Flame } from 'lucide-react';

const FALLBACK_BACKDROP = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80";

export const HeroCarousel = ({ movies = [], selectedLocation = 'CHENNAI' }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isHovered, setIsHovered] = useState(false);

  // Bounds protection when movies array changes
  useEffect(() => {
    if (currentIndex >= movies.length && movies.length > 0) {
      setCurrentIndex(0);
    }
  }, [movies.length, currentIndex]);

  // Autoplay interval (5 seconds)
  useEffect(() => {
    if (!movies || movies.length <= 1 || isHovered) return;

    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % movies.length);
    }, 5000);

    return () => clearInterval(timer);
  }, [movies, isHovered]);

  if (!movies || movies.length === 0) return null;

  const currentMovie = movies[currentIndex] || movies[0];
  const backdropImage = currentMovie.backdropUrl || currentMovie.posterUrl || FALLBACK_BACKDROP;

  const handlePrev = (e) => {
    if (e && e.stopPropagation) e.stopPropagation();
    setCurrentIndex((prev) => (prev - 1 + movies.length) % movies.length);
  };

  const handleNext = (e) => {
    if (e && e.stopPropagation) e.stopPropagation();
    setCurrentIndex((prev) => (prev + 1) % movies.length);
  };

  return (
    <div
      className="relative rounded-3xl overflow-hidden mb-12 bg-slate-950 border border-indigo-500/30 shadow-2xl min-h-[460px] flex flex-col justify-end group transition-all duration-500"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Background Image Slide with Smooth Crossfade */}
      <div className="absolute inset-0 bg-slate-950 overflow-hidden">
        {movies.map((m, idx) => (
          <img
            key={m.movieId || idx}
            src={m.backdropUrl || m.posterUrl || FALLBACK_BACKDROP}
            alt={m.title}
            className={`absolute inset-0 w-full h-full object-cover transition-all duration-1000 ease-out transform ${
              idx === currentIndex
                ? 'opacity-40 scale-100 blur-[0.5px] z-0'
                : 'opacity-0 scale-105 pointer-events-none'
            }`}
          />
        ))}
      </div>

      {/* Dark Gradient Overlay Structure for Legibility */}
      <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/80 to-transparent z-10 pointer-events-none" />
      <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/70 to-transparent z-10 pointer-events-none" />

      {/* Slide Content Details */}
      <div className="relative z-20 p-8 sm:p-12 max-w-2xl">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-bold mb-4 backdrop-blur-md shadow-sm">
          <Sparkles className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
          NOW SHOWING IN PVK CINEMAS ({selectedLocation.toUpperCase()})
          {isHovered && movies.length > 1 && (
            <span className="ml-2 text-[10px] text-amber-300 bg-amber-500/20 px-2 py-0.5 rounded-full flex items-center gap-1 border border-amber-500/30">
              <Pause className="w-2.5 h-2.5" /> Paused
            </span>
          )}
        </div>

        <h1 className="text-4xl sm:text-6xl font-black tracking-tight text-white mb-3 leading-none drop-shadow-lg transition-all duration-300">
          {currentMovie.title}
        </h1>

        <div className="flex flex-wrap items-center gap-3 text-xs text-slate-300 mb-4 font-semibold">
          <span className="px-2.5 py-0.5 rounded bg-slate-900/90 text-indigo-300 font-extrabold border border-slate-700">
            {currentMovie.censorRating || 'U/A'}
          </span>

          {/* Strictly Verified Rating or Rating: N/A (NO HYPE SCORES) */}
          {currentMovie.rating ? (
            <span className="px-2.5 py-0.5 rounded bg-amber-500/20 text-amber-300 font-extrabold border border-amber-500/40 flex items-center gap-1">
              <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" /> Rating: {currentMovie.rating} / 10
            </span>
          ) : (
            <span className="px-2.5 py-0.5 rounded bg-slate-800/80 text-slate-400 font-bold border border-slate-700">
              Rating: N/A
            </span>
          )}

          <span>{currentMovie.genre}</span>
          <span>&bull;</span>
          <span className="text-cyan-300 font-bold">{currentMovie.language || 'English'}</span>
          {currentMovie.duration && <span>&bull; {currentMovie.duration} mins</span>}
        </div>

        <p className="text-slate-300 text-sm mb-8 leading-relaxed line-clamp-3 max-w-xl">
          {currentMovie.description}
        </p>

        {/* Action Buttons */}
        <div className="flex flex-wrap gap-4">
          <Link
            to={`/movies/${currentMovie.movieId}`}
            className="btn-primary py-3.5 px-8 text-xs font-extrabold shadow-lg shadow-indigo-600/30 flex items-center gap-2 hover:scale-105 transition-all"
          >
            <Ticket className="w-4 h-4" /> Book Tickets Now <ArrowRight className="w-4 h-4" />
          </Link>

          {currentMovie.trailerUrl && (
            <a
              href={currentMovie.trailerUrl}
              target="_blank"
              rel="noreferrer"
              className="btn-secondary py-3.5 px-6 text-xs font-bold flex items-center gap-2 hover:bg-slate-800 transition-all"
            >
              <Play className="w-4 h-4 text-rose-400 fill-rose-400" /> Watch Teaser
            </a>
          )}
        </div>
      </div>

      {/* Navigation Arrows (Rendered if > 1 movies) */}
      {movies.length > 1 && (
        <>
          <button
            onClick={handlePrev}
            className="absolute left-4 top-1/2 -translate-y-1/2 z-30 p-3 rounded-full bg-slate-950/60 hover:bg-indigo-600 text-slate-300 hover:text-white border border-slate-700/60 hover:border-indigo-400 backdrop-blur-md transition-all shadow-xl opacity-80 hover:opacity-100"
            title="Previous Movie"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>

          <button
            onClick={handleNext}
            className="absolute right-4 top-1/2 -translate-y-1/2 z-30 p-3 rounded-full bg-slate-950/60 hover:bg-indigo-600 text-slate-300 hover:text-white border border-slate-700/60 hover:border-indigo-400 backdrop-blur-md transition-all shadow-xl opacity-80 hover:opacity-100"
            title="Next Movie"
          >
            <ChevronRight className="w-6 h-6" />
          </button>
        </>
      )}

      {/* Pagination Indicator Dots (Rendered if > 1 movies) */}
      {movies.length > 1 && (
        <div className="absolute bottom-6 right-8 z-30 flex items-center gap-2 bg-slate-950/60 px-4 py-2 rounded-full border border-slate-800/80 backdrop-blur-md">
          {movies.map((m, idx) => (
            <button
              key={m.movieId || idx}
              onClick={() => setCurrentIndex(idx)}
              className={`h-2 rounded-full transition-all duration-300 ${
                idx === currentIndex
                  ? 'w-8 bg-indigo-500 shadow-sm shadow-indigo-500/50'
                  : 'w-2 bg-slate-600 hover:bg-slate-400'
              }`}
              title={`Go to slide ${idx + 1}: ${m.title}`}
            />
          ))}
        </div>
      )}
    </div>
  );
};
