import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Clock, Calendar, MapPin, Tv, ArrowLeft, Filter, Sparkles,
  ShieldCheck, AlertTriangle, Users, DollarSign, Target, Zap,
  TrendingUp, Award, ChevronDown, ChevronUp, Play
} from 'lucide-react';
import { moviesApi, decisionApi, systemApi } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import type { Movie } from '../../types/movie';
import type { Show } from '../../types/show';
import type { ShowRecommendationResponse } from '../../types/booking';
import { ShowtimePill } from '../../components/cinema/ShowtimePill';
import { DisplayOnlyBanner } from '../../components/common/DisplayOnlyBanner';

interface MovieDetailsViewProps {
  movieId: number;
  initialTheatreId?: number;
  initialDate?: string;
  initialCityId?: number;
  onNavigate: (view: string, param?: any) => void;
}

interface TheatreGroup {
  theatreId: number;
  theatreName: string;
  shows: Show[];
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
  initialCityId,
  onNavigate,
}) => {
  const { selectedCityId, setSelectedCityId } = useAuth();

  useEffect(() => {
    if (initialCityId && initialCityId !== selectedCityId) {
      setSelectedCityId(initialCityId);
    }
  }, [initialCityId]);

  const [movie, setMovie] = useState<Movie | null>(null);
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Multiplex selection
  const [selectedTheatreId, setSelectedTheatreId] = useState<number | null>(initialTheatreId ?? null);

  // Authoritative business date from backend provider (7-day rolling window)
  const [businessDate, setBusinessDate] = useState<string>('2026-10-11');
  const [windowEndDate, setWindowEndDate] = useState<string>('2026-10-17');
  const [demoDates, setDemoDates] = useState<string[]>([]);

  useEffect(() => {
    systemApi.getBusinessDate()
      .then(info => {
        if (info.businessDate) setBusinessDate(info.businessDate);
        if (info.windowEndDate) setWindowEndDate(info.windowEndDate);
        if (info.demoDates) setDemoDates(info.demoDates);
      })
      .catch(err => console.warn('Could not fetch business date:', err));
  }, []);

  // Available schedule dates derived from actual shows within the 7-day rolling booking window
  const availableDates = useMemo(() => {
    const set = new Set<string>();
    shows.forEach((s) => {
      const d = s.showDate || (s.startAt ? s.startAt.slice(0, 10) : '');
      if (d) {
        if (d >= businessDate && (!windowEndDate || d <= windowEndDate)) {
          set.add(d);
        }
      }
    });
    // Ensure all demo dates that have shows are present
    demoDates.forEach(d => {
      if (shows.some(s => (s.showDate === d) || (s.startAt && s.startAt.startsWith(d)))) {
        set.add(d);
      }
    });
    return Array.from(set).sort();
  }, [shows, demoDates, businessDate, windowEndDate]);

  const [selectedDate, setSelectedDate] = useState<string>(initialDate || '');

  useEffect(() => {
    if (availableDates.length > 0 && (!selectedDate || !availableDates.includes(selectedDate))) {
      if (initialDate && availableDates.includes(initialDate)) {
        setSelectedDate(initialDate);
      } else {
        const todayOrFuture = availableDates.find(d => d >= businessDate);
        setSelectedDate(todayOrFuture || availableDates[0]);
      }
    }
  }, [availableDates, selectedDate, initialDate, businessDate]);

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

  // Date formatting helpers — "Today" is strictly derived from authoritative business date
  const formatDateLabel = (dateStr: string) => {
    const dateObj = new Date(dateStr + 'T00:00:00');
    const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' });
    const monthName = dateObj.toLocaleDateString('en-US', { month: 'short' });
    const dayNum = dateObj.getDate();

    if (dateStr === businessDate) {
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
  const [recError, setRecError] = useState<string | null>(null);
  const [showAllCandidates, setShowAllCandidates] = useState(false);

  // === Decision Panel Inputs ===
  const [priority, setPriority] = useState<Priority>('BALANCED');
  const [partySize, setPartySize] = useState(2);
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([]);
  const [selectedFormats, setSelectedFormats] = useState<string[]>([]);
  const [selectedTimes, setSelectedTimes] = useState<string[]>([]);
  const [dateFrom, setDateFrom] = useState(initialDate || getISODate(0));
  const [dateTo, setDateTo] = useState(initialDate || getISODate(6));
  const [budgetPerPerson, setBudgetPerPerson] = useState('');

  // Sync dateFrom and dateTo when active calendar date changes
  useEffect(() => {
    if (selectedDate) {
      setDateFrom(selectedDate);
      setDateTo(selectedDate);
    }
  }, [selectedDate]);

  // Live calculation of maximum party budget
  const calculatedPartyBudget = useMemo(() => {
    const val = parseFloat(budgetPerPerson);
    if (!isNaN(val) && val > 0) {
      return val * partySize;
    }
    return null;
  }, [budgetPerPerson, partySize]);

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
    setRecError(null);
    setShowRec(null);
    try {
      const bPerPerson = budgetPerPerson ? parseFloat(budgetPerPerson) : undefined;
      const rec = await decisionApi.getShowRecommendations(movieId, {
        cityId: selectedCityId || undefined,
        theatreId: selectedTheatreId || undefined,
        partySize,
        priority,
        formats: selectedFormats.length > 0 ? selectedFormats : undefined,
        languages: selectedLanguages.length > 0 ? selectedLanguages : undefined,
        times: selectedTimes.length > 0 ? selectedTimes : undefined,
        budgetPerPerson: bPerPerson && !isNaN(bPerPerson) && bPerPerson > 0 ? bPerPerson : undefined,
        dateFrom,
        dateTo,
      });
      setShowRec(rec);
    } catch (err: any) {
      console.warn('Decision engine error:', err);
      setRecError(err?.message || 'Recommendation service encountered an issue. Please retry.');
    } finally {
      setRecLoading(false);
    }
  }, [movieId, selectedCityId, selectedTheatreId, priority, partySize, selectedLanguages, selectedFormats, selectedTimes, dateFrom, dateTo, budgetPerPerson]);

  // Browse-mode grouping: partitioned by active date and theatre (no multi-day duplicates)
  const { theatreGroups, totalScreeningsOnDate } = useMemo(() => {
    let filtered = selectedCityId ? shows.filter((s) => s.cityId === selectedCityId) : shows;
    if (selectedTheatreId != null) {
      filtered = filtered.filter((s) => s.theatreId === selectedTheatreId);
    }
    if (selectedDate) {
      filtered = filtered.filter((s) => (s.showDate ? s.showDate === selectedDate : (s.startAt && s.startAt.startsWith(selectedDate))));
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

    // Group directly by theatre (no nested screen containers) and sort chronologically
    const map = new Map<number, { theatreName: string; showsMap: Map<number, Show> }>();
    filtered.forEach((show) => {
      if (!map.has(show.theatreId)) {
        map.set(show.theatreId, { theatreName: show.theatreName, showsMap: new Map() });
      }
      const te = map.get(show.theatreId)!;
      if (!te.showsMap.has(show.showId)) {
        te.showsMap.set(show.showId, show);
      }
    });

    const groups: TheatreGroup[] = Array.from(map.entries()).map(([theatreId, entry]) => ({
      theatreId,
      theatreName: entry.theatreName,
      shows: Array.from(entry.showsMap.values()).sort(
        (a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime()
      ),
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
          {/* Top Row: Date, Party Size, Budget, Multiplex */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginBottom: '20px' }}>

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
                max={getISODate(6)}
                onChange={e => setDateFrom(e.target.value)}
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
                max={getISODate(6)}
                onChange={e => setDateTo(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>

            {/* Seats (Party Size) */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <Users size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Party Size (Persons)
              </label>
              <input
                id="input-party-size"
                type="number"
                className="select"
                value={partySize}
                min={1}
                max={10}
                onChange={e => setPartySize(Math.max(1, parseInt(e.target.value, 10) || 1))}
                style={{ width: '100%' }}
              />
            </div>

            {/* Maximum Budget Per Person */}
            <div>
              <label style={{ fontSize: '0.82rem', color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                <DollarSign size={13} style={{ verticalAlign: 'middle', marginRight: '4px' }} />Maximum budget per person (₹)
              </label>
              <input
                id="input-budget-per-person"
                type="number"
                className="select"
                placeholder="e.g. 250 (optional)"
                value={budgetPerPerson}
                min={0}
                onChange={e => setBudgetPerPerson(e.target.value)}
                style={{ width: '100%' }}
              />
              {calculatedPartyBudget !== null ? (
                <div style={{ fontSize: '0.78rem', color: '#10b981', marginTop: '6px', fontWeight: 600 }}>
                  Party Budget: ₹{calculatedPartyBudget.toLocaleString('en-IN')} (₹{budgetPerPerson} × {partySize} persons)
                </div>
              ) : (
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '6px' }}>
                  Optional · Estimates base ticket price with taxes
                </div>
              )}
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

          {/* Multi-Select Group 1: Preferred Time Slots */}
          <div style={{ marginTop: '16px', padding: '16px', background: 'var(--bg-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <label style={{ fontSize: '0.84rem', fontWeight: 600, color: '#e2e8f0' }}>
                <Clock size={14} style={{ verticalAlign: 'middle', marginRight: '6px', color: '#f59e0b' }} />
                Preferred Showtimes (Multi-Select)
              </label>
              <span style={{ fontSize: '0.75rem', color: selectedTimes.length > 0 ? '#10b981' : 'var(--text-muted)' }}>
                {selectedTimes.length > 0 ? `${selectedTimes.length} selected (OR matching)` : 'Any time permitted'}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {[
                { id: 'MORNING', label: 'Morning', sub: '< 12 PM' },
                { id: 'AFTERNOON', label: 'Afternoon', sub: '12 – 4 PM' },
                { id: 'EVENING', label: 'Evening', sub: '4 – 8 PM' },
                { id: 'NIGHT', label: 'Night', sub: '> 8 PM' },
              ].map(t => {
                const isSelected = selectedTimes.includes(t.id);
                return (
                  <button
                    key={t.id}
                    type="button"
                    id={`time-chip-${t.id.toLowerCase()}`}
                    onClick={() => {
                      setSelectedTimes(prev =>
                        prev.includes(t.id) ? prev.filter(x => x !== t.id) : [...prev, t.id]
                      );
                    }}
                    style={{
                      padding: '6px 14px',
                      borderRadius: '20px',
                      border: isSelected ? '1px solid #f59e0b' : '1px solid var(--border-subtle)',
                      background: isSelected ? 'rgba(245,158,11,0.18)' : 'var(--bg-surface)',
                      color: isSelected ? '#fbbf24' : 'var(--text-secondary)',
                      fontSize: '0.82rem',
                      fontWeight: isSelected ? 600 : 400,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <span>{t.label}</span>
                    <span style={{ fontSize: '0.72rem', opacity: 0.75 }}>({t.sub})</span>
                  </button>
                );
              })}
              {selectedTimes.length > 0 && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => setSelectedTimes([])}
                  style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                >
                  Clear Time
                </button>
              )}
            </div>
          </div>

          {/* Multi-Select Group 2: Presentation Formats & Audio Capabilities */}
          <div style={{ marginTop: '16px', padding: '16px', background: 'var(--bg-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <label style={{ fontSize: '0.84rem', fontWeight: 600, color: '#e2e8f0' }}>
                <Tv size={14} style={{ verticalAlign: 'middle', marginRight: '6px', color: '#60a5fa' }} />
                Presentation Format & Audio (Multi-Select)
              </label>
              <span style={{ fontSize: '0.75rem', color: selectedFormats.length > 0 ? '#10b981' : 'var(--text-muted)' }}>
                {selectedFormats.length > 0 ? `${selectedFormats.length} selected (OR matching)` : 'Any format permitted'}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {[
                { id: '2D', label: 'Standard 2D', kind: 'Visual' },
                { id: '3D', label: 'RealD 3D', kind: 'Visual' },
                { id: 'IMAX', label: 'IMAX Laser', kind: 'Visual' },
                { id: '4DX', label: '4DX Motion', kind: 'Visual' },
                { id: 'DOLBY_ATMOS', label: 'Dolby Atmos', kind: 'Spatial Audio' },
              ].map(f => {
                const isSelected = selectedFormats.includes(f.id);
                return (
                  <button
                    key={f.id}
                    type="button"
                    id={`format-chip-${f.id.toLowerCase()}`}
                    onClick={() => {
                      setSelectedFormats(prev =>
                        prev.includes(f.id) ? prev.filter(x => x !== f.id) : [...prev, f.id]
                      );
                    }}
                    style={{
                      padding: '6px 14px',
                      borderRadius: '20px',
                      border: isSelected ? '1px solid #60a5fa' : '1px solid var(--border-subtle)',
                      background: isSelected ? 'rgba(96,165,250,0.18)' : 'var(--bg-surface)',
                      color: isSelected ? '#93c5fd' : 'var(--text-secondary)',
                      fontSize: '0.82rem',
                      fontWeight: isSelected ? 600 : 400,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <span>{f.label}</span>
                    <span style={{ fontSize: '0.7rem', opacity: 0.75 }}>[{f.kind}]</span>
                  </button>
                );
              })}
              {selectedFormats.length > 0 && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => setSelectedFormats([])}
                  style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                >
                  Clear Formats
                </button>
              )}
            </div>
          </div>

          {/* Multi-Select Group 3: Languages */}
          <div style={{ marginTop: '16px', padding: '16px', background: 'var(--bg-elevated)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <label style={{ fontSize: '0.84rem', fontWeight: 600, color: '#e2e8f0' }}>
                <Filter size={14} style={{ verticalAlign: 'middle', marginRight: '6px', color: '#c084fc' }} />
                Language (Multi-Select)
              </label>
              <span style={{ fontSize: '0.75rem', color: selectedLanguages.length > 0 ? '#10b981' : 'var(--text-muted)' }}>
                {selectedLanguages.length > 0 ? `${selectedLanguages.length} selected (OR matching)` : 'Any language permitted'}
              </span>
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {[
                { code: 'ta', label: 'Tamil' },
                { code: 'en', label: 'English' },
                { code: 'hi', label: 'Hindi' },
                { code: 'te', label: 'Telugu' },
                { code: 'ml', label: 'Malayalam' },
                { code: 'kn', label: 'Kannada' },
              ].map(lang => {
                const isSelected = selectedLanguages.includes(lang.code);
                return (
                  <button
                    key={lang.code}
                    type="button"
                    id={`language-chip-${lang.code}`}
                    onClick={() => {
                      setSelectedLanguages(prev =>
                        prev.includes(lang.code) ? prev.filter(x => x !== lang.code) : [...prev, lang.code]
                      );
                    }}
                    style={{
                      padding: '6px 14px',
                      borderRadius: '20px',
                      border: isSelected ? '1px solid #c084fc' : '1px solid var(--border-subtle)',
                      background: isSelected ? 'rgba(192,132,252,0.18)' : 'var(--bg-surface)',
                      color: isSelected ? '#e9d5ff' : 'var(--text-secondary)',
                      fontSize: '0.82rem',
                      fontWeight: isSelected ? 600 : 400,
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    {lang.label}
                  </button>
                );
              })}
              {selectedLanguages.length > 0 && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => setSelectedLanguages([])}
                  style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                >
                  Clear Languages
                </button>
              )}
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
                    type="button"
                    onClick={() => setPriority(p)}
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

          {/* Action Buttons */}
          <div style={{ marginTop: '20px', display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
            <button
              id="btn-find-best-shows"
              className="btn btn-primary"
              onClick={runDecisionEngine}
              disabled={recLoading}
              style={{ gap: '8px', fontWeight: 700 }}
            >
              <Zap size={16} />
              {recLoading ? 'Evaluating Candidates...' : 'Find Best Shows'}
            </button>
            <button
              type="button"
              className="btn btn-sm btn-outline"
              onClick={() => {
                setPriority('BALANCED');
                setPartySize(2);
                setSelectedTheatreId(initialTheatreId ?? null);
                setSelectedLanguages([]);
                setSelectedFormats([]);
                setSelectedTimes([]);
                setDateFrom(initialDate || getISODate(0));
                setDateTo(initialDate || getISODate(6));
                setBudgetPerPerson('');
                setShowRec(null);
                setRecError(null);
              }}
            >
              Reset Constraints
            </button>
          </div>
        </div>

        {/* API Error State */}
        {recError && (
          <div style={{
            padding: '16px 20px', marginBottom: '20px', borderRadius: '8px',
            background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <AlertTriangle size={20} color="#ef4444" />
              <div>
                <span style={{ fontWeight: 600, fontSize: '0.88rem', color: '#f87171' }}>Recommendation Engine Error: </span>
                <span style={{ fontSize: '0.85rem', color: '#cbd5e1' }}>{recError}</span>
              </div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={runDecisionEngine} style={{ fontSize: '0.78rem' }}>
              Retry
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
        {!recLoading && !showRec && !recError && (
          <div style={{
            textAlign: 'center', padding: '36px 20px',
            background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)',
            border: '1px dashed var(--border-subtle)',
          }}>
            <Sparkles size={24} color="var(--text-muted)" style={{ marginBottom: '10px' }} />
            <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
              Configure your preferences above, then click <strong style={{ color: 'var(--accent-crimson)' }}>Find Best Shows</strong> to evaluate and rank eligible shows.
            </p>
          </div>
        )}

        {/* Candidate Results */}
        {!recLoading && showRec && showRec.preferredOption && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {/* Conflict Analysis & Diagnostics Banner */}
            {showRec.conflictAnalysis && (
              <div style={{
                padding: '12px 18px', borderRadius: '8px',
                background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.25)',
                fontSize: '0.85rem', color: '#a5b4fc',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  <TrendingUp size={15} />
                  <span style={{ fontWeight: 600 }}>Evaluation Summary:</span>
                  <span>{showRec.conflictAnalysis}</span>
                </div>
                {showRec.totalShowsEvaluated != null && (
                  <div style={{ fontSize: '0.78rem', color: '#cbd5e1', marginTop: '4px' }}>
                    Evaluated {showRec.totalShowsEvaluated} candidate shows in window · {showRec.eligibleCandidatesCount ?? showRec.candidates.length} eligible options ranked
                  </div>
                )}
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

                      {/* Reasons grounded in real evaluated data */}
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

        {/* Truthful Empty Results State */}
        {!recLoading && showRec && !showRec.preferredOption && (
          <div className="card" style={{ padding: '32px', textAlign: 'center', border: '1px solid rgba(245,158,11,0.25)' }}>
            <AlertTriangle size={32} color="#f59e0b" style={{ marginBottom: '12px' }} />
            <h3 style={{ marginBottom: '8px', fontSize: '1.15rem' }}>No Shows Match Your Constraints</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '18px', fontSize: '0.9rem', maxWidth: '640px', margin: '0 auto 18px auto', lineHeight: 1.5 }}>
              {showRec.conflictAnalysis || 'None of the scheduled shows satisfied all selected criteria.'}
            </p>

            {/* Accurate Diagnostics Breakdown Table / Chips */}
            {showRec.totalShowsEvaluated != null && (
              <div style={{
                background: 'var(--bg-elevated)', padding: '16px', borderRadius: '8px',
                maxWidth: '680px', margin: '0 auto 20px auto', border: '1px solid var(--border-subtle)',
                textAlign: 'left'
              }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '10px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Candidate Evaluation Diagnostics
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '10px', fontSize: '0.82rem' }}>
                  <div style={{ padding: '8px', background: 'var(--bg-surface)', borderRadius: '6px' }}>
                    <div style={{ color: 'var(--text-muted)' }}>Evaluated Shows</div>
                    <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#e2e8f0' }}>{showRec.totalShowsEvaluated}</div>
                  </div>
                  {showRec.excludedSeats ? (
                    <div style={{ padding: '8px', background: 'var(--bg-surface)', borderRadius: '6px' }}>
                      <div style={{ color: '#f87171' }}>Insufficient Seats</div>
                      <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#f87171' }}>{showRec.excludedSeats}</div>
                    </div>
                  ) : null}
                  {showRec.excludedFormat ? (
                    <div style={{ padding: '8px', background: 'var(--bg-surface)', borderRadius: '6px' }}>
                      <div style={{ color: '#60a5fa' }}>Format Filter</div>
                      <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#60a5fa' }}>{showRec.excludedFormat}</div>
                    </div>
                  ) : null}
                  {showRec.excludedLanguage ? (
                    <div style={{ padding: '8px', background: 'var(--bg-surface)', borderRadius: '6px' }}>
                      <div style={{ color: '#c084fc' }}>Language Filter</div>
                      <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#c084fc' }}>{showRec.excludedLanguage}</div>
                    </div>
                  ) : null}
                  {showRec.excludedTime ? (
                    <div style={{ padding: '8px', background: 'var(--bg-surface)', borderRadius: '6px' }}>
                      <div style={{ color: '#fbbf24' }}>Time Filter</div>
                      <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#fbbf24' }}>{showRec.excludedTime}</div>
                    </div>
                  ) : null}
                  {showRec.excludedBudget ? (
                    <div style={{ padding: '8px', background: 'var(--bg-surface)', borderRadius: '6px' }}>
                      <div style={{ color: '#34d399' }}>Budget Limit</div>
                      <div style={{ fontWeight: 700, fontSize: '1.1rem', color: '#34d399' }}>{showRec.excludedBudget}</div>
                    </div>
                  ) : null}
                </div>
              </div>
            )}

            {/* Actionable Relaxation Buttons */}
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
              {selectedFormats.length > 0 && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => { setSelectedFormats([]); }}
                >
                  Clear Formats ({selectedFormats.length})
                </button>
              )}
              {selectedLanguages.length > 0 && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => { setSelectedLanguages([]); }}
                >
                  Clear Languages ({selectedLanguages.length})
                </button>
              )}
              {selectedTimes.length > 0 && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => { setSelectedTimes([]); }}
                >
                  Clear Time Slots ({selectedTimes.length})
                </button>
              )}
              {budgetPerPerson && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => { setBudgetPerPerson(''); }}
                >
                  Clear Budget Limit
                </button>
              )}
              <button
                type="button"
                className="btn btn-sm btn-primary"
                onClick={() => {
                  setSelectedFormats([]);
                  setSelectedLanguages([]);
                  setSelectedTimes([]);
                  setBudgetPerPerson('');
                  setPartySize(2);
                }}
              >
                Reset All Active Constraints
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
                <option value="">{availableTheatres.length > 0 ? `All Multiplexes (${availableTheatres.length})` : 'All Multiplexes'}</option>
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

        {/* Date Selector Ribbon Header & Jump Input */}
        {availableDates.length > 0 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px', flexWrap: 'wrap', gap: '8px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              {availableDates.length} scheduled dates available (7-day booking window)
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Calendar size={14} color="var(--accent-crimson)" />
              <span style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>Jump to Date:</span>
              <input
                id="input-jump-to-date"
                type="date"
                className="input"
                style={{ width: '150px', height: '32px', fontSize: '0.82rem', padding: '2px 8px' }}
                value={selectedDate}
                min={businessDate}
                max={windowEndDate || availableDates[availableDates.length - 1]}
                onChange={e => {
                  if (e.target.value) {
                    setSelectedDate(e.target.value);
                  }
                }}
              />
            </div>
          </div>
        )}

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
            {movie?.movieStatus === 'ENDED' || movie?.lifecycleStage === 'ENDED' ? (
              <>
                <Calendar size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
                <h3>Theatrical Run Completed</h3>
                <p style={{ marginTop: '8px', color: 'var(--text-secondary)' }}>
                  This title's exhibition run ended on {movie.exhibitionEndDate ? formatFullDate(movie.exhibitionEndDate) : 'an earlier date'}. No further showtimes are scheduled.
                </p>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => onNavigate('movies')}
                  style={{ marginTop: '16px' }}
                >
                  Browse Now Showing Movies
                </button>
              </>
            ) : shows.length === 0 ? (
              <>
                <Calendar size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
                <h3>No Showtimes Scheduled</h3>
                <p style={{ marginTop: '8px', color: 'var(--text-secondary)' }}>
                  No screenings scheduled for this title within the active 7-day booking window ({businessDate} to {windowEndDate}){selectedCityId ? ' in the selected city' : ''}.
                </p>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => onNavigate('movies')}
                  style={{ marginTop: '16px' }}
                >
                  Explore Other Movies
                </button>
              </>
            ) : (
              <>
                <Calendar size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
                <h3>No Showtimes Match Filters</h3>
                <p style={{ marginTop: '8px', color: 'var(--text-secondary)' }}>
                  No showtimes match your current filters on this date. Try adjusting your multiplex, format, or timing selection.
                </p>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => {
                    setSelectedTheatreId(null);
                    setBrowseFormat('ALL');
                    setBrowseTime('ALL');
                  }}
                  style={{ marginTop: '16px' }}
                >
                  Reset Showtimes Filters
                </button>
              </>
            )}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {theatreGroups.map(group => (
              <div
                key={group.theatreId}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '16px',
                  padding: '24px',
                }}
                data-testid={`theatre-showtime-group-${group.theatreId}`}
              >
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  borderBottom: '1px solid var(--border-subtle)',
                  paddingBottom: '12px',
                  flexWrap: 'wrap',
                  gap: '10px',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <MapPin size={20} color="var(--accent-crimson)" />
                    <h3 style={{ fontSize: '1.25rem', margin: 0 }}>{group.theatreName}</h3>
                    <span className="badge badge-slate" style={{ fontSize: '0.75rem' }}>
                      {group.shows.length} {group.shows.length === 1 ? 'Show' : 'Shows'}
                    </span>
                  </div>
                  <button onClick={() => onNavigate('theatre-details', group.theatreId)} className="btn btn-sm btn-outline">
                    Multiplex Details
                  </button>
                </div>

                {/* Horizontal responsive showtime cards (desktop: 5 across, mobile: responsive wrap) */}
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(185px, 1fr))',
                    gap: '14px',
                    width: '100%',
                    alignItems: 'stretch',
                  }}
                  data-testid={`theatre-showtimes-grid-${group.theatreId}`}
                >
                  {group.shows.map(show => (
                    <ShowtimePill
                      key={show.showId}
                      show={show}
                      onSelect={id => onNavigate('show-seats', { showId: id, partySize })}
                    />
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
