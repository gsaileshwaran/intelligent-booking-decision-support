import React, { useState, useEffect } from 'react';
import { ArrowLeft, MapPin, Tv, Eye } from 'lucide-react';
import { showsApi } from '../../api/client';
import type { Show } from '../../types/show';
import { DisplayOnlyBanner } from '../../components/common/DisplayOnlyBanner';

interface ShowDetailsViewProps {
  showId: number;
  onNavigate: (view: string, param?: any) => void;
}

export const ShowDetailsView: React.FC<ShowDetailsViewProps> = ({ showId, onNavigate }) => {
  const [show, setShow] = useState<Show | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadShow = async () => {
      try {
        setLoading(true);
        const data = await showsApi.getShow(showId);
        setShow(data);
      } catch (err: any) {
        console.error('Failed to load show details', err);
        setError(err.message || 'Failed to load show');
      } finally {
        setLoading(false);
      }
    };
    loadShow();
  }, [showId]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
        Loading show details...
      </div>
    );
  }

  if (error || !show) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
        <h3>Error Loading Show</h3>
        <p style={{ marginTop: '8px', color: '#ef4444' }}>{error || 'Show not found.'}</p>
        <button className="btn btn-secondary btn-sm" onClick={() => onNavigate('movies')} style={{ marginTop: '20px' }}>
          Back to Movies
        </button>
      </div>
    );
  }

  const startTime = new Date(show.startAt);
  const endTime = new Date(show.endAt);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="show-details-view">
      <div>
        <button
          onClick={() => onNavigate('movie-details', show.movieId)}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Movie Shows
        </button>
      </div>

      <DisplayOnlyBanner />

      <div className="card" style={{ padding: '36px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span className="badge badge-crimson">SHOW ID #{show.showId}</span>
              <span className={`badge ${show.showStatus === 'SCHEDULED' ? 'badge-emerald' : 'badge-slate'}`}>
                {show.showStatus}
              </span>
            </div>
            <h1 style={{ fontSize: '2.4rem', marginBottom: '8px' }}>{show.movieTitle}</h1>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <MapPin size={16} color="var(--accent-crimson)" />
                <span>{show.theatreName}</span>
              </div>
              <span>&bull;</span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Tv size={16} color="var(--accent-gold)" />
                <span>{show.screenName}</span>
              </div>
            </div>
          </div>

          <button
            onClick={() => onNavigate('show-seats', show.showId)}
            className="btn btn-primary btn-lg"
            style={{ gap: '8px' }}
          >
            <Eye size={18} />
            View Seat Availability
          </button>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '20px',
          background: 'var(--bg-elevated)',
          padding: '20px',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-subtle)',
        }}>
          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>DATE &amp; TIME</span>
            <div style={{ fontWeight: 600, fontSize: '1.05rem', marginTop: '4px' }}>
              {startTime.toLocaleDateString(undefined, { dateStyle: 'full' })}
            </div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '2px' }}>
              {startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} &ndash; {endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>AUDIO &amp; LANGUAGE</span>
            <div style={{ fontWeight: 600, fontSize: '1.05rem', marginTop: '4px' }}>
              {show.languageName || 'Original Audio'}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>SCREEN FORMAT</span>
            <div style={{ fontWeight: 600, fontSize: '1.05rem', marginTop: '4px' }}>
              Standard Digital
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
