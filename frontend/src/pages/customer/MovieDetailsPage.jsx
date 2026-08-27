import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { movieService } from '../../services/movieService';
import { showService } from '../../services/showService';
import { bookingService } from '../../services/bookingService';
import { convenienceService } from '../../services/convenienceService';
import { useAuth } from '../../context/AuthContext';
import { AIRecommendationModal } from '../../components/AIRecommendationModal';
import {
  Film,
  Clock,
  MapPin,
  Ticket,
  ArrowLeft,
  Building2,
  ShieldCheck,
  Star,
  Bookmark,
  Share2,
  Play,
  Calendar,
  UserCheck,
  Video,
  Clapperboard,
  Flame,
  Sun,
  Moon,
  Sunset,
  Sunrise,
  Sparkles
} from 'lucide-react';

const FALLBACK_POSTER = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80";

export const MovieDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { selectedLocation } = useLocation();

  const [movie, setMovie] = useState(null);
  const [shows, setShows] = useState([]);
  const [userBookings, setUserBookings] = useState([]);
  const [isFavorite, setIsFavorite] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [selectedDateIndex, setSelectedDateIndex] = useState(0); // 0 = Today, 1 = Tomorrow, 2 = Day After
  const [posterSrc, setPosterSrc] = useState(FALLBACK_POSTER);
  const [isAIModalOpen, setIsAIModalOpen] = useState(false);

  useEffect(() => {
    fetchMovieAndShows();
  }, [id, isAuthenticated]);

  const fetchMovieAndShows = async () => {
    setLoading(true);
    setError('');
    try {
      // 1. Fetch movie details FIRST
      const movieRes = await movieService.getMovieById(id);
      if (!movieRes || !movieRes.success || !movieRes.data) {
        setError('Movie details not found.');
        setLoading(false);
        return;
      }

      const m = movieRes.data;
      setMovie(m);
      setPosterSrc(m.posterUrl || m.thumbnailUrl || FALLBACK_POSTER);

      // 2. Fetch shows separately (never crash movie profile if 0 shows)
      try {
        const showsRes = await showService.getShows(id);
        if (showsRes && showsRes.success && Array.isArray(showsRes.data)) {
          setShows(showsRes.data);
        } else {
          setShows([]);
        }
      } catch (sErr) {
        console.warn('No showtimes scheduled or error fetching shows:', sErr);
        setShows([]);
      }

      // 3. User bookings and watchlist (authenticated)
      if (isAuthenticated) {
        try {
          const [bRes, fRes] = await Promise.all([
            bookingService.getMyHistory().catch(() => ({ success: false, data: [] })),
            convenienceService.getFavorites().catch(() => ({ success: false, data: [] }))
          ]);
          if (bRes && bRes.success && Array.isArray(bRes.data)) {
            setUserBookings(bRes.data);
          }
          if (fRes && fRes.success && Array.isArray(fRes.data)) {
            setIsFavorite(fRes.data.some((f) => f.movieId === Number(id)));
          }
        } catch (authErr) {
          console.warn('Auth data load warning:', authErr);
        }
      }
    } catch (err) {
      console.error('Failed to fetch movie details:', err);
      setError('Failed to fetch movie details.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleFavorite = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    try {
      const fav = await convenienceService.toggleFavorite(movie.movieId);
      setIsFavorite(fav);
    } catch (err) {
      console.error('Failed to toggle favorite:', err);
    }
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: movie.title,
        text: `Check out ${movie.title} on Intelligent Booking!`,
        url: window.location.href,
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(window.location.href);
      alert('Movie link copied to clipboard!');
    }
  };

  const getActiveHoldForShow = (showId) => {
    if (!isAuthenticated || !userBookings.length) return null;
    const now = new Date().getTime();
    return userBookings.find((b) => {
      if (b.status !== 'HELD' && b.status !== 'PENDING') return false;
      if (b.showId !== showId) return false;
      if (b.holdExpiresAt) {
        return new Date(b.holdExpiresAt).getTime() > now;
      }
      if (b.createdAt) {
        return (new Date(b.createdAt).getTime() + 600000) > now;
      }
      return true;
    });
  };

  const formatReleaseDate = () => {
    if (!movie) return '';
    if (movie.releaseDateStatus === 'TBA' || !movie.releaseDate) {
      return `${movie.releaseYear || 2027} — DATE TO BE ANNOUNCED`;
    }
    if (movie.releaseDateStatus === 'ANNOUNCED') {
      try {
        const d = new Date(movie.releaseDate);
        return d.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
      } catch {
        return `ANNOUNCED ${movie.releaseYear || 2027}`;
      }
    }
    try {
      const d = new Date(movie.releaseDate);
      return d.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    } catch {
      return `${movie.releaseDate}`;
    }
  };

  // Generate Date Buttons (Today, Tomorrow, Day After)
  const dateOptions = [0, 1, 2].map((offset) => {
    const d = new Date();
    d.setDate(d.getDate() + offset);
    const isoDate = d.toISOString().split('T')[0];
    const label = offset === 0 ? 'Today' : (offset === 1 ? 'Tomorrow' : d.toLocaleDateString('en-IN', { weekday: 'short' }));
    const formatted = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
    return { offset, isoDate, label, formatted };
  });

  const selectedDateObj = dateOptions[selectedDateIndex];

  // Filter shows by city and date
  const cityFilteredShows = shows.filter((show) => {
    if (!selectedLocation || selectedLocation === 'ALL') return true;
    const loc = show.screen?.theatre?.city || show.screen?.theatre?.location;
    if (!loc) return true;
    return loc.toLowerCase() === selectedLocation.toLowerCase();
  });

  const dateFilteredShows = cityFilteredShows.filter((show) => {
    if (!show.showDate) return true;
    return show.showDate === selectedDateObj.isoDate;
  });

  // Group shows by Theatre
  const showsByTheatre = dateFilteredShows.reduce((acc, show) => {
    const theatreName = show.screen?.theatre?.name || 'PVK Cinema Venue';
    if (!acc[theatreName]) {
      acc[theatreName] = {
        theatre: show.screen?.theatre,
        shows: [],
      };
    }
    acc[theatreName].shows.push(show);
    return acc;
  }, {});

  // Format 24h time string (e.g. "14:15:00") into 12h AM/PM (e.g. "02:15 PM")
  const format12Hour = (timeStr) => {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    let h = parseInt(parts[0], 10);
    const m = parts[1] || '00';
    const ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12;
    if (h === 0) h = 12;
    const hDisplay = h < 10 ? `0${h}` : `${h}`;
    return `${hDisplay}:${m} ${ampm}`;
  };

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading movie profile and multi-screen showtimes...</div>;
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

  const isBookable = (movie.isNowShowing === true || (movie.releaseDate && new Date(movie.releaseDate) <= new Date())) && cityFilteredShows.length > 0;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate('/movies')}
        className="inline-flex items-center gap-2 text-xs font-bold text-slate-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Movies Catalogue
      </button>

      {/* Hero Movie Details Banner */}
      <div className="relative rounded-3xl overflow-hidden mb-12 bg-slate-950 border border-slate-800 shadow-2xl p-6 sm:p-10">
        {movie.backdropUrl && (
          <img
            src={movie.backdropUrl}
            alt={movie.title}
            className="absolute inset-0 w-full h-full object-cover opacity-25 blur-[2px]"
          />
        )}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/90 to-slate-950/40" />

        <div className="relative z-10 flex flex-col lg:flex-row gap-8 items-start">
          {/* Poster (2:3 Aspect Ratio) */}
          <div className="w-full lg:w-64 aspect-[2/3] rounded-2xl overflow-hidden bg-slate-900 shadow-2xl border border-slate-700 shrink-0 relative">
            <img
              src={posterSrc}
              alt={movie.title}
              onError={() => setPosterSrc(FALLBACK_POSTER)}
              className="w-full h-full object-cover"
            />
          </div>

          {/* Details Content */}
          <div className="flex-1 flex flex-col justify-center space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              <span className="px-3 py-1 rounded bg-indigo-500/20 text-indigo-300 text-xs font-extrabold border border-indigo-500/40">
                {movie.censorRating || 'U/A'}
              </span>

              {movie.isUpcoming === true || (!movie.isNowShowing && movie.releaseDate && new Date(movie.releaseDate) > new Date()) ? (
                movie.anticipationScore ? (
                  <span className="px-3 py-1 rounded bg-rose-500/20 text-rose-300 text-xs font-extrabold border border-rose-500/40 flex items-center gap-1">
                    <Flame className="w-3.5 h-3.5 text-rose-400 fill-rose-400" /> HYPE {movie.anticipationScore} / 100
                  </span>
                ) : null
              ) : movie.rating ? (
                <span className="px-3 py-1 rounded bg-amber-500/20 text-amber-300 text-xs font-extrabold border border-amber-500/40 flex items-center gap-1">
                  <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" /> Rating: {movie.rating} / 10
                </span>
              ) : (
                <span className="px-3 py-1 rounded bg-slate-800 text-slate-400 text-xs font-bold border border-slate-700">
                  Rating: N/A
                </span>
              )}

              <span className="text-xs font-bold text-cyan-300">{movie.languages || movie.language || 'English'}</span>
              {movie.duration && <span className="text-xs text-slate-400 font-semibold">&bull; {movie.duration} mins</span>}
            </div>

            <h1 className="text-4xl sm:text-5xl font-black text-white leading-tight">
              {movie.title}
            </h1>

            {movie.originalTitle && movie.originalTitle !== movie.title && (
              <p className="text-xs text-indigo-300 font-semibold uppercase tracking-wider">
                Original Title: {movie.originalTitle}
              </p>
            )}

            <div className="flex flex-wrap gap-4 text-xs text-slate-300">
              <span className="flex items-center gap-1.5 font-semibold text-slate-300">
                <Clapperboard className="w-4 h-4 text-indigo-400" /> {movie.genre}
              </span>
              <span className="flex items-center gap-1.5 font-semibold text-slate-300">
                <Calendar className="w-4 h-4 text-indigo-400" /> RELEASE DATE: {formatReleaseDate()}
              </span>
              {movie.studio && (
                <span className="flex items-center gap-1.5 font-semibold text-slate-300">
                  <Building2 className="w-4 h-4 text-indigo-400" /> Studio: {movie.studio}
                </span>
              )}
            </div>

            <p className="text-slate-300 text-sm leading-relaxed max-w-3xl pt-2">
              {movie.description}
            </p>

            {/* Cast & Director Badges */}
            <div className="pt-2 border-t border-slate-800/80 space-y-2 text-xs">
              {movie.director && (
                <div>
                  <strong className="text-slate-400">Director: </strong>
                  <span className="text-white font-bold">{movie.director}</span>
                </div>
              )}
              {movie.cast && (
                <div>
                  <strong className="text-slate-400">Starring Cast: </strong>
                  <span className="text-slate-300">{movie.cast}</span>
                </div>
              )}
            </div>

            {/* CTA Action Buttons */}
            <div className="flex flex-wrap gap-4 pt-4">
              {isBookable ? (
                <>
                  <a href="#showtimes-section" className="btn-primary py-3.5 px-8 text-xs font-extrabold shadow-lg shadow-indigo-600/30">
                    <Ticket className="w-4 h-4" /> Select Showtime & Seats
                  </a>
                  <button
                    onClick={() => setIsAIModalOpen(true)}
                    className="px-6 py-3.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-xs font-extrabold flex items-center gap-2 shadow-lg shadow-purple-500/20 transition-all border border-purple-400/30"
                  >
                    <Sparkles className="w-4 h-4 text-amber-300" /> AI Smart Recommendation
                  </button>
                </>
              ) : (
                <div className="px-6 py-3.5 rounded-xl bg-indigo-500/20 border border-indigo-500/40 text-indigo-300 text-xs font-extrabold flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-indigo-400" /> COMING SOON TO PVK MULTIPLEXES
                </div>
              )}

              <button
                onClick={handleToggleFavorite}
                className={`py-3.5 px-5 rounded-xl border text-xs font-bold flex items-center gap-2 transition-all ${
                  isFavorite
                    ? 'bg-purple-600/30 border-purple-500 text-purple-300'
                    : 'bg-slate-900 border-slate-700 text-slate-300 hover:text-white'
                }`}
              >
                <Bookmark className={`w-4 h-4 ${isFavorite ? 'fill-purple-300' : ''}`} />
                {isFavorite ? 'Saved in Watchlist' : 'Add to Watchlist'}
              </button>

              {movie.trailerUrl && (
                <a
                  href={movie.trailerUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="btn-secondary py-3.5 px-5 text-xs font-bold flex items-center gap-2"
                >
                  <Play className="w-4 h-4 text-rose-400 fill-rose-400" /> Watch Teaser
                </a>
              )}

              <button onClick={handleShare} className="btn-secondary py-3.5 px-4 text-xs font-bold">
                <Share2 className="w-4 h-4 text-slate-400" /> Share
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* SHOWTIMES & THEATRE SELECTION SECTION */}
      <div id="showtimes-section" className="scroll-mt-8 space-y-8">
        {/* Date Selector Header */}
        <div className="glass-card p-6 border border-slate-800 flex flex-col md:flex-row items-center justify-between gap-6">
          <div>
            <h2 className="text-2xl font-bold text-white flex items-center gap-2">
              <Building2 className="w-6 h-6 text-indigo-400" /> PVK Multiplexes in {selectedLocation}
            </h2>
            <p className="text-xs text-slate-400 mt-1">Multi-screen auditoriums scheduled for {movie.title}</p>
          </div>

          {/* Date Selector Pills */}
          <div className="flex items-center gap-3 bg-slate-900 p-1.5 rounded-2xl border border-slate-800">
            {dateOptions.map((opt, idx) => (
              <button
                key={opt.offset}
                onClick={() => setSelectedDateIndex(idx)}
                className={`px-5 py-2.5 rounded-xl font-bold text-xs flex flex-col items-center transition-all ${
                  selectedDateIndex === idx
                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/80'
                }`}
              >
                <span>{opt.label}</span>
                <span className="text-[10px] opacity-80">{opt.formatted}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Theatre & Showtime List */}
        {Object.keys(showsByTheatre).length === 0 ? (
          <div className="glass-card p-10 text-center text-slate-400">
            <Calendar className="w-10 h-10 text-indigo-400 mx-auto mb-3" />
            <h3 className="text-lg font-bold text-white">
              {movie.isUpcoming ? 'THEATRICAL RELEASE COMING SOON' : `No Active Showtimes for ${selectedDateObj.label}`}
            </h3>
            <p className="text-xs text-slate-400 mt-2 max-w-md mx-auto">
              {movie.isUpcoming
                ? `Showtimes and ticket reservations will open across PVK multiplexes in ${selectedLocation} upon approaching release on ${formatReleaseDate()}.`
                : `No PVK showtimes are scheduled for this film on ${selectedDateObj.label} (${selectedDateObj.formatted}) in ${selectedLocation}. Try selecting another date.`}
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {Object.entries(showsByTheatre).map(([theatreName, { theatre, shows: venueShows }]) => {
              const sortedShows = [...venueShows].sort((a, b) => {
                if (a.startTime < b.startTime) return -1;
                if (a.startTime > b.startTime) return 1;
                const screenA = a.screen?.name || '';
                const screenB = b.screen?.name || '';
                return screenA.localeCompare(screenB);
              });

              return (
                <div key={theatreName} className="glass-card p-6 border border-slate-800 hover:border-slate-700 transition-all">
                  {/* Theatre Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-800/80 pb-4 mb-5 gap-2">
                    <div>
                      <h3 className="text-xl font-extrabold text-white flex items-center gap-2">
                        <Building2 className="w-5 h-5 text-indigo-400" />
                        {theatreName}
                      </h3>
                      <p className="text-xs text-slate-400 flex items-center gap-1.5 mt-1">
                        <MapPin className="w-3.5 h-3.5 text-slate-500" />
                        {theatre?.address || `${theatre?.locality || ''}, ${theatre?.location || selectedLocation}`}
                      </p>
                    </div>
                    <span className="text-xs px-3.5 py-1.5 rounded-full bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 font-bold self-start sm:self-auto">
                      {sortedShows.length} {sortedShows.length === 1 ? 'Show' : 'Shows'} Available ({selectedDateObj.label})
                    </span>
                  </div>

                  {/* Showtime Cards Grid — Theatre/Branch Wise */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5">
                    {sortedShows.map((show) => {
                      const activeHold = getActiveHoldForShow(show.showId);
                      const screenName = show.screen?.name || 'Screen 1';
                      const timeFormatted = format12Hour(show.startTime);

                      if (activeHold) {
                        return (
                          <button
                            key={show.showId}
                            onClick={() => navigate(`/shows/${show.showId}/seats`)}
                            className="p-3.5 rounded-2xl bg-cyan-950/90 hover:bg-cyan-900 border border-cyan-400/60 text-left transition-all shadow-lg shadow-cyan-500/20 flex flex-col justify-between"
                          >
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-[10px] uppercase font-black text-cyan-400 tracking-wider bg-cyan-900/60 px-2 py-0.5 rounded">
                                {screenName}
                              </span>
                              <ShieldCheck className="w-4 h-4 text-cyan-400" />
                            </div>
                            <div className="text-sm font-black text-cyan-200 my-1">
                              {timeFormatted}
                            </div>
                            <div className="flex items-center justify-between text-xs pt-1.5 border-t border-cyan-800/50">
                              <span className="text-emerald-400 font-extrabold">₹{show.ticketPrice?.toFixed(2)}</span>
                              <span className="text-[10px] text-amber-400 font-bold">CONTINUE</span>
                            </div>
                          </button>
                        );
                      }

                      return (
                        <button
                          key={show.showId}
                          onClick={() => navigate(`/shows/${show.showId}/seats`)}
                          className="p-3.5 rounded-2xl bg-slate-900/90 hover:bg-indigo-950/60 border border-slate-800 hover:border-indigo-500/50 text-left transition-all group shadow-md hover:shadow-indigo-500/10 flex flex-col justify-between"
                        >
                          <div className="flex items-center justify-between mb-1.5">
                            <span className="text-[11px] font-bold text-slate-400 group-hover:text-indigo-300">
                              {screenName}
                            </span>
                            <span className="text-xs font-black text-emerald-400">
                              ₹{show.ticketPrice ? Math.round(show.ticketPrice) : '220'}
                            </span>
                          </div>
                          <div className="flex items-center gap-1.5 text-sm font-black text-white group-hover:text-indigo-200 my-1">
                            <Clock className="w-4 h-4 text-indigo-400 shrink-0" />
                            <span>{timeFormatted}</span>
                          </div>
                          <div className="pt-2 border-t border-slate-800/80 mt-1 flex items-center justify-between">
                            <span className="text-[10px] text-slate-400 font-medium group-hover:text-slate-300">Available</span>
                            <span className="text-xs font-extrabold text-indigo-400 group-hover:text-indigo-300 group-hover:translate-x-0.5 transition-transform flex items-center gap-0.5">
                              Book &rarr;
                            </span>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <AIRecommendationModal
        isOpen={isAIModalOpen}
        onClose={() => setIsAIModalOpen(false)}
        movieId={movie?.movieId}
        movieTitle={movie?.title}
      />
    </div>
  );
};
