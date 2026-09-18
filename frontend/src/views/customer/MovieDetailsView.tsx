import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Clock, Calendar, MapPin, Tv, ArrowLeft, Filter, Sparkles,
  ShieldCheck, AlertTriangle, Users, DollarSign, Target, Zap,
  TrendingUp, Award, ChevronDown, ChevronUp, Play
} from 'lucide-react';
import { moviesApi, decisionApi } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import type { Movie } from '../../types/movie';
import type { Show } from '../../types/show';
import type { ShowRecommendationResponse, InteractionFriction } from '../../types/booking';
import { ShowtimePill } from '../../components/cinema/ShowtimePill';
import { DisplayOnlyBanner } from '../../components/common/DisplayOnlyBanner';

interface MovieDetailsViewProps {
  movieId: number;
  initialTheatreId?: number;
  initialDate?: string;
  onNavigate: (view: string, param?: any) => void;
}

interface TheatreGroup {
  theatreId: number;
  theatreName: string;
  screens: { screenId: number; screenName: string; shows: Show[] }[];
}

type Priority = 'BEST_PRICE' | 'BEST_SEATS' | 'BEST_TIME' | 'BALANCED';

const PRIORITY_CONFIG: Record<Priority, { label: string; icon: React.ReactNode; color: string; desc: string }> = {
  BEST_PRICE: {
    label: 'Best Price',
    icon: <DollarSign size={14} />,
    color: '#10b981',
    desc: 'Lowest total cost for your party',
  },
  BEST_SEATS: {
    label: 'Best Seats',
    icon: <Award size={14} />,
    color: '#6366f1',
    desc: 'Largest adjacent seat block in a single row',
  },
  BEST_TIME: {
    label: 'Best Time',
    icon: <Clock size={14} />,
    color: '#f59e0b',
    desc: 'Closest match to your preferred time slot',
  },
  BALANCED: {
    label: 'Balanced',
    icon: <Target size={14} />,
    color: '#e50914',
    desc: 'Balanced ranking across seats, price and timing',
  },
};

function getISODate(offsetDays = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().slice(0, 10);
}

export const MovieDetailsView: React.FC<MovieDetailsViewProps> = ({
  movieId,
  initialTheatreId,
  initialDate,
  onNavigate,
}) => {
  const { selectedCityId } = useAuth();

  const [movie, setMovie] = useState<Movie | null>(null);
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Multiplex selection
  const [selectedTheatreId, setSelectedTheatreId] = useState<number | null>(initialTheatreId ?? null);

  // Available schedule dates derived from actual shows
  const availableDates = useMemo(() => {
    const set = new Set<string>();
    shows.forEach((s) => {
      if (s.startAt) {
        set.add(s.startAt.slice(0, 10));
      }
    });
    return Array.from(set).sort();
  }, [shows]);

  const [selectedDate, setSelectedDate] = useState<string>(initialDate || '');

  useEffect(() => {
    if (availableDates.length > 0 && (!selectedDate || !availableDates.includes(selectedDate))) {
      setSelectedDate(initialDate && availableDates.includes(initialDate) ? initialDate : availableDates[0]);
    }
  }, [availableDates, selectedDate, initialDate]);

  // Available multiplexes derived from shows
  const availableTheatres = useMemo(() => {
    const map = new Map<number, string>();
    shows.forEach((s) => {
      if (s.theatreId && s.theatreName) {
        map.set(s.theatreId, s.theatreName);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [shows]);

  const selectedTheatreName = useMemo(() => {
    if (selectedTheatreId == null) return null;
    return availableTheatres.find((t) => t.id === selectedTheatreId)?.name || null;
  }, [selectedTheatreId, availableTheatres]);

  // Date formatting helpers
  const formatDateLabel = (dateStr: string) => {
    const todayStr = new Date().toISOString().slice(0, 10);
    const dateObj = new Date(dateStr + 'T00:00:00');
    const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' });
    const monthName = dateObj.toLocaleDateString('en-US', { month: 'short' });
    const dayNum = dateObj.getDate();

    if (dateStr === todayStr) {
      return { prefix: 'Today', sub: `${dayNum} ${monthName}` };
    }
    return { prefix: dayName, sub: `${dayNum} ${monthName}` };
  };

  const formatFullDate = (dateStr: string) => {
    if (!dateStr) return '';
    const dateObj = new Date(dateStr + 'T00:00:00');
    return dateObj.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });
  };

  // Decision engine state
  const [showRec, setShowRec] = useState<ShowRecommendationResponse | null>(null);
  const [recLoading, setRecLoading] = useState(false);
  const [frictionAlert, setFrictionAlert] = useState<InteractionFriction | null>(null);
  const [filterChangeCount, setFilterChangeCount] = useState(0);
  const [showAllCandidates, setShowAllCandidates] = useState(false);

  // === Decision Panel Inputs ===
  const [priority, setPriority] = useState<Priority>('BALANCED');
  const [partySize, setPartySize] = useState(2);
  const [languageCode, setLanguageCode] = useState('');
  const [formatFilter, setFormatFilter] = useState('ALL');
  const [timeFilter, setTimeFilter] = useState('ALL');
  const [dateFrom, setDateFrom] = useState(initialDate || getISODate(0));
  const [dateTo, setDateTo] = useState(initialDate || getISODate(7));
  const [budgetTotal, setBudgetTotal] = useState('');

  // Sync dateFrom and dateTo when active calendar date changes
  useEffect(() => {
    if (selectedDate) {
      setDateFrom(selectedDate);
      setDateTo(selectedDate);
    }
  }, [selectedDate]);

  // Simple showtimes panel filter (for the manual browse list below the AI panel)
  const [browseFormat, setBrowseFormat] = useState('ALL');
  const [browseTime, setBrowseTime] = useState('ALL');

  // Load base movie data once
  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [movieData, showsData] = await Promise.all([
          moviesApi.getMovie(movieId),
          moviesApi.getMovieShows(movieId, selectedCityId || undefined),
        ]);
        setMovie(movieData);
        setShows(showsData || []);
      } catch (err: any) {
        setError(err.message || 'Failed to load movie details');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [movieId, selectedCityId]);

  // Run recommendation engine
  const runDecisionEngine = useCallback(async () => {
    setRecLoading(true);
    setShowRec(null);
    try {
      const params: any = {
        cityId: selectedCityId || undefined,
        theatreId: selectedTheatreId || undefined,
        partySize,
        priority,
      };
      if (timeFilter !== 'ALL') params.timePreference = timeFilter;
      if (languageCode) params.languageCode = languageCode;
      if (formatFilter !== 'ALL') params.formatPreference = formatFilter;
      if (dateFrom) params.dateFrom = dateFrom;
      if (dateTo) params.dateTo = dateTo;
      if (budgetTotal) params.budgetMaxTotal = parseFloat(budgetTotal);

      const rec = await decisionApi.getShowRecommendations(movieId, params);
      setShowRec(rec);
    } catch (err) {
      console.warn('Decision engine error:', err);
    } finally {
      setRecLoading(false);
    }
  }, [movieId, selectedCityId, selectedTheatreId, priority, partySize, languageCode, formatFilter, timeFilter, dateFrom, dateTo, budgetTotal]);

  const trackFilterChange = () => {
    const count = filterChangeCount + 1;
    setFilterChangeCount(count);
    if (count >= 4) {
      decisionApi.evaluateInteractionFriction({ filterChangeCount: count, timeWindowSeconds: 90 })
        .then(res => { if (res?.detected) setFrictionAlert(res); })
        .catch(() => {});
    }
  };

  // Browse-mode grouping: partitioned by active date and theatre (no multi-day duplicates)
  const { theatreGroups, totalScreeningsOnDate } = useMemo(() => {
    let filtered = selectedCityId ? shows.filter((s) => s.cityId === selectedCityId) : shows;
    if (selectedTheatreId != null) {
      filtered = filtered.filter((s) => s.theatreId === selectedTheatreId);
    }
    if (selectedDate) {
      filtered = filtered.filter((s) => s.startAt && s.startAt.startsWith(selectedDate));
    }
    if (browseFormat !== 'ALL') {
      filtered = filtered.filter((s) => {
        const n = ((s.presentationFormat || '') + ' ' + (s.screenName || '')).toUpperCase();
        if (browseFormat === 'IMAX') return n.includes('IMAX');
        if (browseFormat === '4DX') return n.includes('4DX');
        if (browseFormat === 'DOLBY') return n.includes('DOLBY') || n.includes('ATMOS');
        if (browseFormat === '3D') return n.includes('3D');
        if (browseFormat === '2D') return !n.includes('IMAX') && !n.includes('4DX');
        return true;
      });
    }
    if (browseTime !== 'ALL') {
      filtered = filtered.filter((s) => {
        const h = new Date(s.startAt).getHours();
        if (browseTime === 'MORNING') return h < 12;
        if (browseTime === 'AFTERNOON') return h >= 12 && h < 16;
        if (browseTime === 'EVENING') return h >= 16 && h < 20;
        if (browseTime === 'NIGHT') return h >= 20;
        return true;
      });
    }

    // Authoritative distinct screening count: distinct showIds on this date
    const distinctShowIds = new Set(filtered.map((s) => s.showId));

    const map = new Map<number, { theatreName: string; screenMap: Map<number, { screenName: string; shows: Show[] }> }>();
    filtered.forEach((show) => {
      if (!map.has(show.theatreId)) map.set(show.theatreId, { theatreName: show.theatreName, screenMap: new Map() });
      const te = map.get(show.theatreId)!;
      if (!te.screenMap.has(show.screenId)) te.screenMap.set(show.screenId, { screenName: show.screenName, shows: [] });
      te.screenMap.get(show.screenId)!.shows.push(show);
    });

    const groups: TheatreGroup[] = Array.from(map.entries()).map(([theatreId, entry]) => ({
      theatreId,
      theatreName: entry.theatreName,
      screens: Array.from(entry.screenMap.entries()).map(([screenId, se]) => ({
        screenId,
        screenName: se.screenName,
        shows: se.shows.sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime()),
      })),
    }));

    return { theatreGroups: groups, totalScreeningsOnDate: distinctShowIds.size };
  }, [shows, selectedCityId, selectedTheatreId, selectedDate, browseFormat, browseTime]);

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>Loading movie details...</div>;
  }
  if (error || !movie) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
        <h3>Error Loading Movie</h3>
        <p style={{ marginTop: '8px', color: '#ef4444' }}>{error || 'Movie not found.'}</p>
        <button className="btn btn-secondary btn-sm" onClick={() => onNavigate('movies')} style={{ marginTop: '20px' }}>
          Back to Movies
        </button>
      </div>
    );
  }

  const defaultPoster = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '40px' }} data-testid="movie-details-view">
      {/* Back */}
      <div>
        <button onClick={() => onNavigate('movies')} className="btn btn-sm btn-outline" style={{ gap: '6px' }}>
          <ArrowLeft size={16} /> Back to Catalogue
        </button>
      </div>

      {/* Movie Hero */}
      <div className="card" style={{ padding: '36px', display: 'grid', gridTemplateColumns: 'minmax(200px, 260px) 1fr', gap: '36px', alignItems: 'start' }}>
        <div style={{ borderRadius: 'var(--radius-md)', overflow: 'hidden', boxShadow: 'var(--shadow-lg)' }}>
          <img
            src={movie.posterUrl || defaultPoster}
            alt={movie.title}
            style={{ width: '100%', height: 'auto', display: 'block', objectFit: 'cover' }}
            onError={e => { (e.target as HTMLImageElement).src = defaultPoster; }}
          />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              {movie.certificationCode && <span className="badge badge-gold">{movie.certificationCode}</span>}
              {movie.movieStatus && (
                <span className={`badge ${movie.movieStatus === 'ACTIVE' ? 'badge-emerald' : 'badge-slate'}`}>{movie.movieStatus}</span>
              )}
            </div>
            <h1 style={{ fontSize: '2.4rem', marginBottom: '12px' }}>{movie.title}</h1>
          </div>
          <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            {movie.runtimeMinutes && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Clock size={16} color="var(--accent-crimson)" /><span>{movie.runtimeMinutes} Minutes</span>
              </div>
            )}
            {movie.releaseDate && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Calendar size={16} color="var(--accent-crimson)" />
                <span>{new Date(movie.releaseDate).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
              </div>
            )}
          </div>
          {movie.languages && movie.languages.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Languages:</span>
              {movie.languages.map(lang => <span key={lang} className="badge badge-crimson">{lang}</span>)}
            </div>
          )}
          {movie.genres && movie.genres.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Genres:</span>
              {movie.genres.map(g => <span key={g} className="badge badge-slate">{g}</span>)}
            </div>
          )}
          <div style={{ marginTop: '12px' }}>
            <h3 style={{ fontSize: '1.05rem', marginBottom: '8px' }}>Synopsis</h3>
            <p style={{ lineHeight: 1.6, color: 'var(--text-secondary)' }}>
              {movie.synopsis || 'No synopsis provided for this cinematic title.'}
            </p>
          </div>
        </div>
      </div>

      {/* ============================================================ */}
      {/* PVK INTELLIGENT DECISION PANEL                               */}
      {/* ============================================================ */}
      <section data-testid="decision-support-panel">
        <div style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <Sparkles size={22} color="#e50914" />
            <h2 style={{ margin: 0 }}>Find Your Best Show</h2>
          </div>
          <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.9rem' }}>
            Set your constraints below. PVK will rank all valid shows by your priority, with explainable reasons.
          </p>
        </div>

        {/* === Decision Input Form === */}
        <div className="card" style={{ padding: '28px', marginBottom: '24px', border: '1px solid rgba(229,9,20,0.2)' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>

            {/* Date From */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Calendar size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />From Date
              </label>
              <input
                type="date"
                className="select"
                value={dateFrom}
                min={getISODate(0)}
                onChange={e => { setDateFrom(e.target.value); trackFilterChange(); }}
                style={{ width: '100%' }}
              />
            </div>

            {/* Date To */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Calendar size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />To Date
              </label>
              <input
                type="date"
                className="select"
                value={dateTo}
                min={dateFrom || getISODate(0)}
                onChange={e => { setDateTo(e.target.value); trackFilterChange(); }}
                style={{ width: '100%' }}
              />
            </div>

            {/* Seats */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Users size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Seats (Party Size)
              </label>
              <input
                type="number"
                className="select"
                value={partySize}
                min={1}
                onChange={e => { setPartySize(Math.max(1, parseInt(e.target.value) || 1)); trackFilterChange(); }}
                style={{ width: '100%' }}
              />
            </div>

            {/* Language */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Filter size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Language (Hard Filter)
              </label>
              <select
                className="select"
                value={languageCode}
                onChange={e => { setLanguageCode(e.target.value); trackFilterChange(); }}
                style={{ width: '100%' }}
              >
                <option value="">Any Language</option>
                {movie.languages?.map(lang => (
                  <option key={lang} value={lang.toLowerCase().slice(0, 2)}>
                    {lang}
                  </option>
                ))}
                <option value="en">English</option>
                <option value="ta">Tamil</option>
                <option value="hi">Hindi</option>
                <option value="te">Telugu</option>
                <option value="ml">Malayalam</option>
                <option value="kn">Kannada</option>
              </select>
            </div>

            {/* Format */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Tv size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Format (Hard Filter)
              </label>
              <select
                className="select"
                value={formatFilter}
                onChange={e => { setFormatFilter(e.target.value); trackFilterChange(); }}
                style={{ width: '100%' }}
              >
                <option value="ALL">Any Format</option>
                <option value="IMAX">IMAX Laser</option>
                <option value="4DX">4DX Motion</option>
                <option value="DOLBY">Dolby Atmos</option>
                <option value="3D">RealD 3D</option>
                <option value="2D">Standard 2D</option>
              </select>
            </div>

            {/* Preferred Time */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Clock size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Preferred Time
              </label>
              <select
                className="select"
                value={timeFilter}
                onChange={e => { setTimeFilter(e.target.value); trackFilterChange(); }}
                style={{ width: '100%' }}
              >
                <option value="ALL">Any Time</option>
                <option value="MORNING">Morning (&lt;12 PM)</option>
                <option value="AFTERNOON">Afternoon (12–4 PM)</option>
                <option value="EVENING">Evening (4–8 PM)</option>
                <option value="NIGHT">Night (&gt;8 PM)</option>
              </select>
            </div>

            {/* Budget */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <DollarSign size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Max Budget (₹ total, optional)
              </label>
              <input
                type="number"
                className="select"
                placeholder="e.g. 800"
                value={budgetTotal}
                min={0}
                onChange={e => { setBudgetTotal(e.target.value); trackFilterChange(); }}
                style={{ width: '100%' }}
              />
            </div>

            {/* Multiplex / Theatre Filter */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <MapPin size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Multiplex (Optional)
              </label>
              <select
                id="select-decision-theatre"
                className="select"
                value={selectedTheatreId !== null ? String(selectedTheatreId) : ''}
                onChange={e => {
                  const val = e.target.value ? parseInt(e.target.value, 10) : null;
                  setSelectedTheatreId(val);
                  trackFilterChange();
                }}
                style={{ width: '100%' }}
              >
                <option value="">All Multiplexes in City</option>
                {availableTheatres.map(t => (
                  <option key={t.id} value={String(t.id)}>{t.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Priority Selector */}
          <div style={{ marginTop: '24px' }}>
            <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '10px' }}>
              <Target size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Ranking Priority
            </label>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              {(Object.keys(PRIORITY_CONFIG) as Priority[]).map(p => {
                const cfg = PRIORITY_CONFIG[p];
                const active = priority === p;
                return (
                  <button
                    key={p}
                    id={`priority-${p.toLowerCase()}`}
                    onClick={() => { setPriority(p); trackFilterChange(); }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '8px 16px',
                      borderRadius: '8px',
                      border: active ? `2px solid ${cfg.color}` : '2px solid var(--border-subtle)',
                      background: active ? `${cfg.color}22` : 'var(--bg-surface)',
                      color: active ? cfg.color : 'var(--text-secondary)',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      fontWeight: active ? 700 : 400,
                      transition: 'all 0.15s ease',
                    }}
                    title={cfg.desc}
                  >
                    {cfg.icon}
                    {cfg.label}
                  </button>
                );
              })}
            </div>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '6px' }}>
              {PRIORITY_CONFIG[priority].desc}
            </p>
          </div>

          {/* Action Button */}
          <div style={{ marginTop: '20px', display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
            <button
              id="btn-find-best-shows"
              className="btn btn-primary"
              onClick={runDecisionEngine}
              disabled={recLoading}
              style={{ gap: '8px', fontWeight: 700 }}
            >
              <Zap size={16} />
              {recLoading ? 'Finding Best Options...' : 'Find Best Shows'}
            </button>
            <button
              className="btn btn-sm btn-outline"
              onClick={() => {
                setPriority('BALANCED');
                setPartySize(2);
                setSelectedTheatreId(initialTheatreId ?? null);
                setLanguageCode('');
                setFormatFilter('ALL');
                setTimeFilter('ALL');
                setDateFrom(initialDate || getISODate(0));
                setDateTo(initialDate || getISODate(7));
                setBudgetTotal('');
                setFrictionAlert(null);
                setFilterChangeCount(0);
                setShowRec(null);
              }}
            >
              Reset Constraints
            </button>
          </div>
        </div>

        {/* Interaction Friction Alert */}
        {frictionAlert?.detected && (
          <div style={{
            padding: '12px 18px', marginBottom: '20px', borderRadius: '8px',
            background: 'rgba(245,158,11,0.12)', border: '1px solid rgba(245,158,11,0.3)',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <AlertTriangle size={18} color="#f59e0b" />
              <div>
                <span style={{ fontWeight: 600, fontSize: '0.88rem', color: '#fbbf24' }}>{frictionAlert.title}: </span>
                <span style={{ fontSize: '0.85rem', color: '#cbd5e1' }}>{frictionAlert.description}</span>
              </div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => setFrictionAlert(null)} style={{ fontSize: '0.78rem' }}>
              Dismiss
            </button>
          </div>
        )}

        {/* === Decision Results === */}
        {recLoading && (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>
            <Sparkles size={28} color="#e50914" style={{ marginBottom: '12px' }} />
            <div>Evaluating all shows with {PRIORITY_CONFIG[priority].label} ranking...</div>
          </div>
        )}

        {/* Idle state — user has not yet clicked Find Best Shows */}
        {!recLoading && !showRec && (
          <div style={{
            textAlign: 'center', padding: '36px 20px',
            background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)',
            border: '1px dashed var(--border-subtle)',
          }}>
            <Sparkles size={24} color="var(--text-muted)" style={{ marginBottom: '10px' }} />
            <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
              Configure your preferences above, then click <strong style={{ color: 'var(--accent-crimson)' }}>Find Best Shows</strong> to get AI-ranked recommendations.
            </p>
          </div>
        )}

        {!recLoading && showRec && showRec.preferredOption && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {/* Conflict Analysis Banner */}
            {showRec.conflictAnalysis && (
              <div style={{
                padding: '10px 16px', borderRadius: '8px',
                background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.25)',
                fontSize: '0.83rem', color: '#a5b4fc',
              }}>
                <TrendingUp size={14} style={{ verticalAlign: 'middle', marginRight: '6px' }} />
                {showRec.conflictAnalysis}
              </div>
            )}

            {/* Ranked Candidate Cards */}
            {(showAllCandidates ? showRec.candidates : showRec.candidates.slice(0, 3)).map((c, idx) => {
              const isTop = idx === 0;
              return (
                <div
                  key={c.showId}
                  className="card"
                  style={{
                    padding: '20px 24px',
                    border: isTop ? '2px solid rgba(229,9,20,0.5)' : '1px solid var(--border-subtle)',
                    background: isTop
                      ? 'linear-gradient(135deg, rgba(229,9,20,0.07) 0%, rgba(20,20,30,0.97) 100%)'
                      : 'var(--bg-card)',
                    position: 'relative',
                    transition: 'border-color 0.2s',
                  }}
                  data-testid={`recommendation-candidate-${idx}`}
                >
                  {isTop && (
                    <div style={{
                      position: 'absolute', top: '-1px', left: '20px',
                      background: '#e50914', color: '#fff',
                      fontSize: '0.72rem', fontWeight: 700, padding: '2px 10px',
                      borderRadius: '0 0 6px 6px', letterSpacing: '0.05em',
                    }}>
                      ★ TOP PICK
                    </div>
                  )}

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '16px', alignItems: 'start', marginTop: isTop ? '8px' : 0 }}>
                    <div>
                      {/* Theatre + Format + Time */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap', marginBottom: '8px' }}>
                        <span style={{ fontWeight: 700, fontSize: '1.05rem' }}>{c.theatreName}</span>
                        <span className="badge badge-gold" style={{ fontSize: '0.78rem' }}>{c.presentationFormat}</span>
                        {c.language && <span className="badge" style={{ fontSize: '0.78rem', background: 'rgba(99,102,241,0.15)', color: '#818cf8' }}>{c.language}</span>}
                        <span style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>{c.screenName}</span>
                      </div>
                      <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '10px', fontSize: '0.88rem', color: '#e2e8f0' }}>
                        {c.showDate && (
                          <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#93c5fd' }}>
                            <Calendar size={14} /> {c.showDate}
                          </span>
                        )}
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Clock size={14} color="#f59e0b" /> {c.formattedTime}
                        </span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#10b981' }}>
                          <ShieldCheck size={14} /> {c.availableSeats} / {c.totalSeats} seats ({Math.round(c.availabilityRatio * 100)}% free)
                        </span>
                        {c.totalCost ? (
                          <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#10b981', fontWeight: 600 }}>
                            <DollarSign size={14} /> ₹{c.ticketPrice} × {partySize} = ₹{c.totalCost} total
                          </span>
                        ) : c.ticketPrice && (
                          <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#10b981' }}>
                            <DollarSign size={14} /> ₹{c.ticketPrice} × {partySize} = ₹{(parseFloat(c.ticketPrice as any) * partySize).toFixed(0)} total
                          </span>
                        )}
                      </div>

                      {/* Reasons */}
                      {c.reasons && c.reasons.length > 0 && (
                        <ul style={{ margin: '0 0 6px 0', paddingLeft: '18px', fontSize: '0.8rem', color: '#94a3b8', lineHeight: 1.55 }}>
                          {c.reasons.map((r, i) => <li key={i} style={{ color: '#e2e8f0' }}>{r}</li>)}
                        </ul>
                      )}

                      {/* Trade-offs */}
                      {c.tradeOffs && c.tradeOffs.length > 0 && (
                        <div style={{ marginTop: '6px', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                          {c.tradeOffs.map((t, i) => (
                            <span key={i} style={{
                              fontSize: '0.75rem', background: 'rgba(245,158,11,0.1)',
                              color: '#fbbf24', padding: '2px 8px', borderRadius: '4px',
                              border: '1px solid rgba(245,158,11,0.2)',
                            }}>⚠ {t}</span>
                          ))}
                        </div>
                      )}

                      {/* Recommended Seats & Group Fit */}
                      {c.recommendedSeatLabels && c.recommendedSeatLabels.length > 0 && (
                        <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.82rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                            <span style={{ color: 'var(--text-muted)' }}>Recommended Seats:</span>
                            <span className="badge badge-emerald" style={{ fontWeight: 700, letterSpacing: '0.02em' }}>
                              {c.recommendedSeatLabels.join(', ')}
                            </span>
                            {c.viewingQualityScore != null && (
                              <span className="badge badge-gold" style={{ fontSize: '0.72rem' }}>
                                Viewing: {c.viewingQualityScore}/100
                              </span>
                            )}
                            {c.alternativeSeatLabels && c.alternativeSeatLabels.length > 0 && (
                              <span style={{ color: 'var(--text-muted)', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                <span>· Alt:</span>
                                <span className="badge badge-slate">{c.alternativeSeatLabels.join(', ')}</span>
                              </span>
                            )}
                          </div>
                          {c.seatFitDescription && (
                            <div style={{ fontSize: '0.78rem', color: '#94a3b8' }}>
                              Arrangement: <span style={{ color: '#e2e8f0' }}>{c.seatFitDescription}</span>
                            </div>
                          )}
                          {c.seatingTradeoff && (
                            <div style={{ fontSize: '0.75rem', color: '#fbbf24', display: 'flex', alignItems: 'center', gap: '4px' }}>
                              <span>ℹ</span>
                              <span>{c.seatingTradeoff}</span>
                            </div>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Score + CTA */}
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '10px', minWidth: '120px' }}>
                      <div style={{ textAlign: 'center' }}>
                        <div style={{
                          fontSize: '2rem', fontWeight: 800, lineHeight: 1,
                          color: c.matchScore >= 80 ? '#10b981' : c.matchScore >= 60 ? '#f59e0b' : '#94a3b8',
                        }}>
                          {c.matchScore}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>/ 100</div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>match score</div>
                      </div>
                      <button
                        id={`btn-select-show-${c.showId}`}
                        className={`btn btn-sm ${isTop ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => onNavigate('show-seats', {
                          showId: c.showId,
                          partySize,
                          recommendedSeatIds: c.recommendedSeatIds,
                          recommendedSeatLabels: c.recommendedSeatLabels
                        })}
                        style={{ width: '100%', fontWeight: 600 }}
                      >
                        <Play size={13} /> Select
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}

            {/* Show/Hide more candidates */}
            {showRec.candidates.length > 3 && (
              <button
                className="btn btn-sm btn-outline"
                onClick={() => setShowAllCandidates(v => !v)}
                style={{ alignSelf: 'center', gap: '6px' }}
              >
                {showAllCandidates ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                {showAllCandidates
                  ? 'Show Fewer Options'
                  : `Show All ${showRec.candidates.length} Ranked Options`}
              </button>
            )}
          </div>
        )}

        {/* No results from engine */}
        {!recLoading && showRec && !showRec.preferredOption && (
          <div className="card" style={{ padding: '32px', textAlign: 'center', border: '1px solid rgba(245,158,11,0.25)' }}>
            <AlertTriangle size={28} color="#f59e0b" style={{ marginBottom: '12px' }} />
            <h3 style={{ marginBottom: '8px' }}>No Shows Match Your Constraints</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '16px', fontSize: '0.9rem' }}>
              {showRec.conflictAnalysis || 'Try relaxing one or more constraints.'}
            </p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
              <button className="btn btn-sm btn-outline" onClick={() => { setFormatFilter('ALL'); setLanguageCode(''); setBudgetTotal(''); runDecisionEngine(); }}>
                Remove Format & Language Filters
              </button>
              <button className="btn btn-sm btn-outline" onClick={() => { setDateFrom(getISODate(0)); setDateTo(getISODate(30)); runDecisionEngine(); }}>
                Expand to 30-Day Window
              </button>
            </div>
          </div>
        )}
      </section>

      {/* ============================================================ */}
      {/* BROWSE SHOWTIMES (manual, all shows)                         */}
      {/* ============================================================ */}
      <section data-testid="theatre-grouped-showtimes-section">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
              <h2 style={{ margin: 0 }}>Browse All Showtimes</h2>
              {selectedDate && (
                <span className="badge badge-slate" style={{ fontSize: '0.82rem', padding: '4px 10px', fontWeight: 600 }}>
                  {totalScreeningsOnDate} {totalScreeningsOnDate === 1 ? 'screening' : 'screenings'}{selectedTheatreName ? ` at ${selectedTheatreName}` : ''} on {formatFullDate(selectedDate)}
                </span>
              )}
            </div>
            <p style={{ margin: '6px 0 0 0', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
              Explore all scheduled shows by theatre — click a showtime to choose your seats
            </p>
          </div>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
            {/* Multiplex Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <MapPin size={14} color="var(--accent-crimson)" />
              <span style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>Multiplex:</span>
              <select
                id="select-browse-theatre"
                className="select"
                value={selectedTheatreId !== null ? String(selectedTheatreId) : ''}
                onChange={e => {
                  const val = e.target.value ? parseInt(e.target.value, 10) : null;
                  setSelectedTheatreId(val);
                }}
                style={{ fontSize: '0.85rem', height: '34px', padding: '6px 12px' }}
              >
                <option value="">All Multiplexes ({availableTheatres.length})</option>
                {availableTheatres.map(t => (
                  <option key={t.id} value={String(t.id)}>{t.name}</option>
                ))}
              </select>
            </div>
            {/* Format Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Filter size={14} color="var(--accent-crimson)" />
              <span style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>Format:</span>
              <select className="select" value={browseFormat} onChange={e => setBrowseFormat(e.target.value)} style={{ fontSize: '0.85rem', height: '34px', padding: '6px 12px' }}>
                <option value="ALL">All</option>
                <option value="IMAX">IMAX</option>
                <option value="4DX">4DX</option>
                <option value="DOLBY">Dolby</option>
                <option value="3D">3D</option>
                <option value="2D">2D</option>
              </select>
            </div>
            {/* Timing Selector */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Clock size={14} color="var(--accent-gold)" />
              <span style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>Timing:</span>
              <select className="select" value={browseTime} onChange={e => setBrowseTime(e.target.value)} style={{ fontSize: '0.85rem', height: '34px', padding: '6px 12px' }}>
                <option value="ALL">All</option>
                <option value="MORNING">Morning</option>
                <option value="AFTERNOON">Afternoon</option>
                <option value="EVENING">Evening</option>
                <option value="NIGHT">Night</option>
              </select>
            </div>
          </div>
        </div>

        {/* Date Selector Ribbon */}
        {availableDates.length > 0 && (
          <div
            style={{
              display: 'flex',
              gap: '10px',
              overflowX: 'auto',
              paddingBottom: '12px',
              marginBottom: '20px',
              scrollbarWidth: 'thin',
            }}
            data-testid="date-selector-ribbon"
          >
            {availableDates.map(dStr => {
              const label = formatDateLabel(dStr);
              const isSelected = dStr === selectedDate;
              return (
                <button
                  key={dStr}
                  onClick={() => setSelectedDate(dStr)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    minWidth: '100px',
                    padding: '8px 16px',
                    borderRadius: '8px',
                    border: isSelected ? '2px solid var(--accent-crimson)' : '1px solid var(--border-medium)',
                    background: isSelected ? 'rgba(229, 9, 20, 0.15)' : 'var(--bg-elevated)',
                    color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                  data-testid={`date-pill-${dStr}`}
                >
                  <span style={{ fontSize: '0.74rem', textTransform: 'uppercase', fontWeight: 700, color: isSelected ? 'var(--accent-crimson)' : 'var(--text-muted)' }}>
                    {label.prefix}
                  </span>
                  <span style={{ fontSize: '0.95rem', fontWeight: 800, marginTop: '2px' }}>
                    {label.sub}
                  </span>
                </button>
              );
            })}
          </div>
        )}

        <DisplayOnlyBanner />

        {theatreGroups.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <h3>No Showtimes Match Filters</h3>
            <p style={{ marginTop: '8px' }}>Try adjusting your format or timing filters above.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {theatreGroups.map(group => (
              <div key={group.theatreId} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }} data-testid={`theatre-showtime-group-${group.theatreId}`}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '14px', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <MapPin size={20} color="var(--accent-crimson)" />
                    <h3 style={{ fontSize: '1.25rem' }}>{group.theatreName}</h3>
                  </div>
                  <button onClick={() => onNavigate('theatre-details', group.theatreId)} className="btn btn-sm btn-outline">
                    Multiplex Details
                  </button>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
                  {group.screens.map(screen => (
                    <div key={screen.screenId} style={{ background: 'var(--bg-surface)', borderRadius: 'var(--radius-md)', padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Tv size={16} color="var(--accent-gold)" />
                        <span style={{ fontWeight: 600, fontSize: '0.95rem' }}>{screen.screenName}</span>
                      </div>
                      <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                        {screen.shows.map(show => (
                          <ShowtimePill key={show.showId} show={show} onSelect={id => onNavigate('show-seats', { showId: id, partySize })} />
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
