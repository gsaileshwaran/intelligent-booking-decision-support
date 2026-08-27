import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation as useReactRouterLocation } from 'react-router-dom';
import { movieService } from '../../services/movieService';
import { convenienceService } from '../../services/convenienceService';
import { showService } from '../../services/showService';
import { useLocation } from '../../context/LocationContext';
import { useAuth } from '../../context/AuthContext';
import { MovieCard } from '../../components/MovieCard';
import { MovieSkeletonCard } from '../../components/MovieSkeletonCard';
import { HeroCarousel } from '../../components/HeroCarousel';
import { OptionComparisonModal } from '../../components/OptionComparisonModal';
import {
  Search,
  Film,
  Sparkles,
  Bookmark,
  Building2,
  Tag,
  ArrowRight,
  Star,
  Play,
  Calendar,
  Flame,
  Clapperboard,
  Trophy,
  Ticket,
  Filter
} from 'lucide-react';

export const HomePage = () => {
  const navigate = useNavigate();
  const routerLocation = useReactRouterLocation();
  const { selectedLocation } = useLocation();
  const { isAuthenticated } = useAuth();

  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState('ALL');
  const [selectedGenre, setSelectedGenre] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [nowShowingSort, setNowShowingSort] = useState('RECOMMENDED');
  const [upcomingSort, setUpcomingSort] = useState('ANTICIPATION');

  const [favoriteMovieIds, setFavoriteMovieIds] = useState(new Set());

  // Comparison Modal
  const [isCompareOpen, setIsCompareOpen] = useState(false);
  const [compareShows, setCompareShows] = useState([]);

  useEffect(() => {
    fetchMoviesAndWatchlist();
  }, [routerLocation.pathname, selectedLocation, isAuthenticated, selectedLanguage, selectedGenre, selectedStatus]);

  const fetchMoviesAndWatchlist = async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedLanguage !== 'ALL') params.language = selectedLanguage;
      if (selectedGenre !== 'ALL') params.genre = selectedGenre;
      if (selectedStatus !== 'ALL') params.status = selectedStatus;

      const [movieRes, favRes] = await Promise.all([
        movieService.getAllMovies(params),
        isAuthenticated ? convenienceService.getFavorites().catch(() => ({ success: false, data: [] })) : Promise.resolve({ success: false, data: [] }),
      ]);

      if (movieRes && movieRes.success && Array.isArray(movieRes.data)) {
        setMovies(movieRes.data);
      } else {
        const fallbackRes = await movieService.getAllMovies({});
        if (fallbackRes && fallbackRes.success && Array.isArray(fallbackRes.data)) {
          setMovies(fallbackRes.data);
        }
      }

      if (favRes && favRes.success && Array.isArray(favRes.data)) {
        setFavoriteMovieIds(new Set(favRes.data.map((m) => m.movieId)));
      }
    } catch (err) {
      console.error('Failed to load movie catalogue:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleFavorite = async (movieId, e) => {
    if (e && e.stopPropagation) e.stopPropagation();
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    try {
      const isFav = await convenienceService.toggleFavorite(movieId);
      setFavoriteMovieIds((prev) => {
        const next = new Set(prev);
        if (isFav) next.add(movieId);
        else next.delete(movieId);
        return next;
      });
    } catch (err) {
      console.error('Failed to toggle favorite:', err);
    }
  };

  const handleOpenComparison = async (movieId) => {
    try {
      const res = await showService.getShows(movieId);
      if (res.success && res.data && res.data.length > 0) {
        setCompareShows(res.data);
        setIsCompareOpen(true);
      } else {
        alert('No active showtimes currently scheduled for comparison.');
      }
    } catch (err) {
      console.error('Failed to load comparison showtimes:', err);
    }
  };

  // Client-side search & genre filtering
  const filteredMovies = movies.filter((m) => {
    // 1. Search Query
    if (search.trim()) {
      const q = search.toLowerCase();
      const matchTitle = m.title && m.title.toLowerCase().includes(q);
      const matchCast = m.cast && m.cast.toLowerCase().includes(q);
      const matchDir = m.director && m.director.toLowerCase().includes(q);
      const matchGenre = m.genre && m.genre.toLowerCase().includes(q);
      const matchLang = m.language && m.language.toLowerCase().includes(q);
      if (!matchTitle && !matchCast && !matchDir && !matchGenre && !matchLang) return false;
    }
    // 2. Language Filter
    if (selectedLanguage !== 'ALL') {
      const mLang = (m.language || '').toLowerCase();
      const mLangs = (m.languages || '').toLowerCase();
      const qLang = selectedLanguage.toLowerCase();
      if (!mLang.includes(qLang) && !mLangs.includes(qLang)) return false;
    }
    // 3. Genre Filter
    if (selectedGenre !== 'ALL') {
      const mGenre = (m.genre || '').toLowerCase();
      if (!mGenre.includes(selectedGenre.toLowerCase())) return false;
    }
    return true;
  });

  // Separate NOW SHOWING vs UPCOMING
  const nowShowingMovies = filteredMovies
    .filter((m) => m.isNowShowing === true || (m.releaseDate && new Date(m.releaseDate) <= new Date()))
    .sort((a, b) => {
      if (nowShowingSort === 'RECOMMENDED') {
        const rA = a.rating || 0;
        const rB = b.rating || 0;
        return rB - rA;
      }
      if (nowShowingSort === 'RELEASE_DATE') {
        return new Date(b.releaseDate || 0) - new Date(a.releaseDate || 0);
      }
      if (nowShowingSort === 'TITLE') {
        return (a.title || '').localeCompare(b.title || '');
      }
      return 0;
    });

  const heroMovie = nowShowingMovies.length > 0 ? nowShowingMovies[0] : (filteredMovies.length > 0 ? filteredMovies[0] : null);

  const upcomingMovies = filteredMovies
    .filter((m) => m.isUpcoming === true && (!m.releaseDate || new Date(m.releaseDate) > new Date()))
    .sort((a, b) => {
      if (upcomingSort === 'ANTICIPATION') {
        return (b.anticipationScore || 0) - (a.anticipationScore || 0);
      }
      if (upcomingSort === 'RELEASE_DATE') {
        return new Date(a.releaseDate || 0) - new Date(b.releaseDate || 0);
      }
      if (upcomingSort === 'TITLE') {
        return (a.title || '').localeCompare(b.title || '');
      }
      return 0;
    });

  const languagesList = ['ALL', 'Tamil', 'Hindi', 'Telugu', 'Malayalam', 'Kannada', 'English'];
  const genresList = ['ALL', 'Action', 'Adventure', 'Comedy', 'Drama', 'Thriller', 'Horror', 'Sci-Fi', 'Romance', 'Crime', 'Animation', 'Family', 'Fantasy'];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Featured Spotlight Hero Carousel */}
      <HeroCarousel movies={nowShowingMovies} selectedLocation={selectedLocation} />

      {/* Discovery & Filter Bar */}
      <div id="movies-catalogue" className="glass-card p-6 mb-10 space-y-6">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <h2 className="text-2xl font-black text-white flex items-center gap-2">
              <Clapperboard className="w-6 h-6 text-indigo-400" /> Official Movie Catalogue
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Explore currently playing blockbusters & verified upcoming releases in {selectedLocation}
            </p>
          </div>

          <div className="relative w-full md:w-80">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search title, cast, director, genre..."
              className="form-input w-full pl-9 text-xs py-2.5"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {/* Filter Rows */}
        <div className="space-y-4">
          {/* LANGUAGE ROW */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="text-slate-400 font-bold uppercase text-[10px] w-20 shrink-0 flex items-center gap-1">
              <Filter className="w-3 h-3 text-indigo-400" /> Language:
            </span>
            {languagesList.map((lang) => (
              <button
                key={lang}
                onClick={() => setSelectedLanguage(lang)}
                className={`px-3 py-1.5 rounded-full font-bold transition-all ${
                  selectedLanguage === lang
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                    : 'bg-slate-900/80 text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                {lang}
              </button>
            ))}
          </div>

          {/* GENRE ROW */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="text-slate-400 font-bold uppercase text-[10px] w-20 shrink-0 flex items-center gap-1">
              <Tag className="w-3 h-3 text-cyan-400" /> Genre:
            </span>
            {genresList.map((gen) => (
              <button
                key={gen}
                onClick={() => setSelectedGenre(gen)}
                className={`px-3 py-1 rounded-full font-bold text-[11px] transition-all ${
                  selectedGenre === gen
                    ? 'bg-cyan-600 text-white shadow-md shadow-cyan-600/30'
                    : 'bg-slate-900/80 text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                {gen}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* SECTION 1: CURRENTLY AIRING IN CINEMAS */}
      {(selectedStatus === 'ALL' || selectedStatus === 'NOW_SHOWING') && (
        <section className="mb-14">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-2 gap-4">
            <div className="flex items-center gap-2">
              <Flame className="w-6 h-6 text-emerald-400" />
              <div>
                <h3 className="text-2xl font-black text-white uppercase tracking-tight">CURRENTLY AIRING IN CINEMAS</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  {nowShowingMovies.length} movies currently showing at PVK Cinemas in {selectedLocation}
                </p>
              </div>
            </div>

            {/* Now Showing Sorting */}
            <div className="flex items-center gap-2 text-xs self-end sm:self-auto">
              <span className="text-slate-400 font-semibold">Sort By:</span>
              <select
                className="form-input text-xs bg-slate-900 text-slate-200 py-1.5"
                value={nowShowingSort}
                onChange={(e) => setNowShowingSort(e.target.value)}
              >
                <option value="RECOMMENDED">Recommended</option>
                <option value="RELEASE_DATE">Release Date</option>
                <option value="TITLE">Title (A-Z)</option>
              </select>
            </div>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6 mt-6">
              {[1, 2, 3, 4, 5].map((n) => <MovieSkeletonCard key={n} />)}
            </div>
          ) : nowShowingMovies.length === 0 ? (
            <div className="glass-card p-8 text-center text-slate-400 text-xs mt-6">
              No movies currently playing matching your active filter criteria in {selectedLocation}. Select another genre/language or check upcoming releases below.
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6 mt-6">
              {nowShowingMovies.map((movie) => (
                <MovieCard
                  key={movie.movieId}
                  movie={movie}
                  isFavorite={favoriteMovieIds.has(movie.movieId)}
                  onToggleFavorite={handleToggleFavorite}
                  onOpenComparison={handleOpenComparison}
                />
              ))}
            </div>
          )}
        </section>
      )}

      {/* SECTION 2: UPCOMING MOVIE SLATE — SEPTEMBER 2026 ONWARD */}
      {(selectedStatus === 'ALL' || selectedStatus === 'UPCOMING') && (
        <section className="mb-14 pt-6 border-t border-slate-800">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
            <div className="flex items-center gap-2">
              <Calendar className="w-6 h-6 text-indigo-400" />
              <div>
                <h3 className="text-2xl font-black text-white uppercase tracking-tight">UPCOMING MOVIE SLATE — SEPTEMBER 2026 ONWARD</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  {upcomingMovies.length} upcoming releases announced for 2026 – 2027
                </p>
              </div>
            </div>

            {/* Upcoming Sorting */}
            <div className="flex items-center gap-2 text-xs self-end sm:self-auto">
              <span className="text-slate-400 font-semibold">Sort By:</span>
              <select
                className="form-input text-xs bg-slate-900 text-slate-200 py-1.5"
                value={upcomingSort}
                onChange={(e) => setUpcomingSort(e.target.value)}
              >
                <option value="ANTICIPATION">Most Anticipated</option>
                <option value="RELEASE_DATE">Release Date</option>
                <option value="TITLE">Title (A-Z)</option>
              </select>
            </div>
          </div>

          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
              {[1, 2, 3, 4, 5].map((n) => <MovieSkeletonCard key={n} />)}
            </div>
          ) : upcomingMovies.length === 0 ? (
            <div className="glass-card p-8 text-center text-slate-400 text-xs">
              No upcoming movies match the selected filters.
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
              {upcomingMovies.map((movie) => (
                <MovieCard
                  key={movie.movieId}
                  movie={movie}
                  isFavorite={favoriteMovieIds.has(movie.movieId)}
                  onToggleFavorite={handleToggleFavorite}
                  onOpenComparison={handleOpenComparison}
                />
              ))}
            </div>
          )}
        </section>
      )}

      {/* Quick Access Footers */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-12">
        <div className="glass-card p-8 border border-slate-800 hover:border-indigo-500/40 transition-all flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <Building2 className="w-6 h-6 text-indigo-400" />
              <span className="badge badge-confirmed">PVK NATIONAL NETWORK</span>
            </div>
            <h3 className="text-2xl font-bold text-white mb-2">PVK Cinema Auditoriums</h3>
            <p className="text-xs text-slate-400 leading-relaxed mb-6">
              Discover 5 multi-screen PVK multiplex locations in {selectedLocation} (25 nationwide) offering IMAX 4K Laser & Dolby Atmos sound.
            </p>
          </div>
          <button onClick={() => navigate('/theatres')} className="btn-secondary w-full py-3 text-xs font-bold justify-center">
            Explore All 25 PVK Multiplexes <ArrowRight className="w-4 h-4" />
          </button>
        </div>

        <div className="glass-card p-8 border border-slate-800 hover:border-amber-500/40 transition-all flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <Tag className="w-6 h-6 text-amber-400" />
              <span className="badge bg-amber-500/20 text-amber-300 border-amber-500/40 font-bold">PROMO VOUCHERS</span>
            </div>
            <h3 className="text-2xl font-bold text-white mb-2">Exclusive Ticket Offers</h3>
            <p className="text-xs text-slate-400 leading-relaxed mb-6">
              Save up to 15% on ticket reservations with instant promotional voucher codes.
            </p>
          </div>
          <button onClick={() => navigate('/offers')} className="btn-secondary w-full py-3 text-xs font-bold justify-center">
            View Deals & Vouchers <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Comparison Modal */}
      <OptionComparisonModal
        isOpen={isCompareOpen}
        onClose={() => setIsCompareOpen(false)}
        shows={compareShows}
      />
    </div>
  );
};
