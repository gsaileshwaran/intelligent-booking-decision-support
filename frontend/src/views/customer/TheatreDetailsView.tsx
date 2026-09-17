import React, { useState, useEffect, useMemo } from 'react';
import { MapPin, Tv, ArrowLeft, Film, Calendar } from 'lucide-react';
import { theatresApi } from '../../api/client';
import type { Theatre } from '../../types/theatre';
import type { Show } from '../../types/show';
import { ShowtimePill } from '../../components/cinema/ShowtimePill';
import { DisplayOnlyBanner } from '../../components/common/DisplayOnlyBanner';

interface TheatreDetailsViewProps {
  theatreId: number;
  onNavigate: (view: string, param?: any) => void;
}

interface MovieShowsGroup {
  movieId: number;
  movieTitle: string;
  posterUrl?: string;
  shows: Show[];
}

export const TheatreDetailsView: React.FC<TheatreDetailsViewProps> = ({ theatreId, onNavigate }) => {
  const [theatre, setTheatre] = useState<Theatre | null>(null);
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        setError(null);
        const [theatreData, showsData] = await Promise.all([
          theatresApi.getTheatre(theatreId),
          theatresApi.getTheatreShows(theatreId),
        ]);
        setTheatre(theatreData);
        setShows(showsData || []);
      } catch (err: any) {
        console.error('Failed to load theatre details', err);
        setError(err.message || 'Failed to load multiplex details');
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [theatreId]);

  // Extract distinct schedule dates
  const availableDates = useMemo(() => {
    const set = new Set<string>();
    shows.forEach((s) => {
      if (s.startAt) {
        set.add(s.startAt.slice(0, 10));
      }
    });
    return Array.from(set).sort();
  }, [shows]);

  const [selectedDate, setSelectedDate] = useState<string>('');

  useEffect(() => {
    if (availableDates.length > 0 && (!selectedDate || !availableDates.includes(selectedDate))) {
      setSelectedDate(availableDates[0]);
    }
  }, [availableDates, selectedDate]);

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

  // Filter shows by active calendar date
  const filteredShows = useMemo(() => {
    if (!selectedDate) return shows;
    return shows.filter((s) => s.startAt && s.startAt.startsWith(selectedDate));
  }, [shows, selectedDate]);

  // Group filtered shows by Movie
  const movieGroups: MovieShowsGroup[] = useMemo(() => {
    const map = new Map<number, { movieTitle: string; posterUrl?: string; shows: Show[] }>();
    filteredShows.forEach((show) => {
      if (!map.has(show.movieId)) {
        map.set(show.movieId, {
          movieTitle: show.movieTitle,
          posterUrl: show.posterUrl,
          shows: [],
        });
      }
      map.get(show.movieId)!.shows.push(show);
    });

    return Array.from(map.entries()).map(([movieId, entry]) => ({
      movieId,
      movieTitle: entry.movieTitle,
      posterUrl: entry.posterUrl,
      shows: entry.shows.sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime()),
    }));
  }, [filteredShows]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
        Loading multiplex details and current schedules...
      </div>
    );
  }

  if (error || !theatre) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
        <h3>Error Loading Multiplex</h3>
        <p style={{ marginTop: '8px', color: '#ef4444' }}>{error || 'Multiplex not found.'}</p>
        <button className="btn btn-secondary btn-sm" onClick={() => onNavigate('theatres')} style={{ marginTop: '20px' }}>
          Back to Theatres
        </button>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="theatre-details-view">
      <div>
        <button
          onClick={() => onNavigate('theatres')}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Theatres
        </button>
      </div>

      {/* Theatre Profile Card */}
      <div className="card" style={{ padding: '32px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <span className="badge badge-crimson">{theatre.theatreCode}</span>
              {theatre.theatreStatus && (
                <span className="badge badge-emerald">{theatre.theatreStatus}</span>
              )}
            </div>
            <h1 style={{ fontSize: '2.4rem', marginBottom: '8px' }}>{theatre.theatreName}</h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)' }}>
              <MapPin size={18} color="var(--accent-crimson)" />
              <span>{theatre.addressLine1}{theatre.addressLine2 ? `, ${theatre.addressLine2}` : ''}</span>
              {theatre.postalCode && <span>&bull; PIN: {theatre.postalCode}</span>}
            </div>
          </div>

          <div style={{
            background: 'var(--bg-elevated)',
            padding: '16px 24px',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-subtle)',
            textAlign: 'center',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '4px' }}>
              <Tv size={18} color="var(--accent-gold)" />
              <span style={{ fontSize: '1.4rem', fontWeight: 800 }}>{theatre.totalScreens != null ? theatre.totalScreens : 0}</span>
            </div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Auditorium Screens
            </span>
          </div>
        </div>
      </div>

      {/* Shows at this Theatre */}
      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
          <div>
            <h2>Current Schedules at {theatre.theatreName}</h2>
            <p>Live movie screenings, auditorium screens, formats, and available seats</p>
          </div>

          {/* Date Selector Tabs */}
          {availableDates.length > 1 && (
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }} data-testid="date-selector-tabs">
              {availableDates.map((dateStr) => {
                const isSelected = selectedDate === dateStr;
                const { prefix, sub } = formatDateLabel(dateStr);
                return (
                  <button
                    key={dateStr}
                    onClick={() => setSelectedDate(dateStr)}
                    className="btn"
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      padding: '8px 16px',
                      borderRadius: 'var(--radius-md)',
                      background: isSelected ? 'var(--accent-crimson)' : 'var(--bg-elevated)',
                      border: isSelected ? '1px solid var(--accent-crimson)' : '1px solid var(--border-subtle)',
                      color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                      transition: 'all 0.15s ease',
                      minWidth: '80px',
                    }}
                    data-testid={`date-tab-${dateStr}`}
                  >
                    <span style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase' }}>{prefix}</span>
                    <span style={{ fontSize: '0.9rem', fontWeight: 800 }}>{sub}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Scope Banner */}
        <DisplayOnlyBanner />

        {movieGroups.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <Calendar size={36} color="var(--text-muted)" style={{ margin: '0 auto 12px' }} />
            <h3>No Scheduled Shows on This Date</h3>
            <p style={{ marginTop: '8px' }}>There are currently no active screenings scheduled for this date at this multiplex.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {movieGroups.map((group) => (
              <div key={group.movieId} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '18px', padding: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '14px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    {group.posterUrl ? (
                      <img
                        src={group.posterUrl}
                        alt={group.movieTitle}
                        style={{
                          width: '44px',
                          height: '62px',
                          objectFit: 'cover',
                          borderRadius: 'var(--radius-sm)',
                          border: '1px solid var(--border-subtle)',
                        }}
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                    ) : (
                      <div style={{
                        width: '44px',
                        height: '62px',
                        background: 'var(--bg-elevated)',
                        borderRadius: 'var(--radius-sm)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}>
                        <Film size={20} color="var(--accent-crimson)" />
                      </div>
                    )}
                    <div>
                      <h3 style={{ fontSize: '1.35rem', fontWeight: 800, letterSpacing: '-0.01em' }}>{group.movieTitle}</h3>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {group.shows.length} {group.shows.length === 1 ? 'screening' : 'screenings'} on this date
                      </span>
                    </div>
                  </div>

                  <button
                    onClick={() => onNavigate('movie-details', group.movieId)}
                    className="btn btn-sm btn-outline"
                  >
                    Movie Details
                  </button>
                </div>

                <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap' }}>
                  {group.shows.map((show) => (
                    <ShowtimePill
                      key={show.showId}
                      show={show}
                      onSelect={(id) => onNavigate('show-seats', id)}
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
