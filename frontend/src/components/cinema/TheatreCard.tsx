import React from 'react';
import { MapPin, Tv, ArrowRight } from 'lucide-react';
import type { Theatre } from '../../types/theatre';

interface TheatreCardProps {
  theatre: Theatre;
  onSelect: (theatreId: number) => void;
}

export const TheatreCard: React.FC<TheatreCardProps> = ({ theatre, onSelect }) => {
  return (
    <article
      className="card card-interactive"
      onClick={() => onSelect(theatre.theatreId)}
      data-testid={`theatre-card-${theatre.theatreId}`}
      style={{ display: 'flex', flexDirection: 'column', gap: '14px', cursor: 'pointer' }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '10px' }}>
        <div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '4px' }}>{theatre.theatreName}</h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            <MapPin size={14} color="var(--accent-crimson)" />
            <span>{theatre.addressLine1}{theatre.addressLine2 ? `, ${theatre.addressLine2}` : ''}</span>
          </div>
        </div>
        {theatre.cityName && (
          <span className="badge badge-crimson">{theatre.cityName}</span>
        )}
      </div>

      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingTop: '12px',
        borderTop: '1px solid var(--border-subtle)',
        marginTop: 'auto',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
          <Tv size={15} color="var(--accent-gold)" />
          <span>{theatre.totalScreens != null ? theatre.totalScreens : 0} Screens</span>
        </div>

        <button
          className="btn btn-sm btn-secondary"
          onClick={(e) => {
            e.stopPropagation();
            onSelect(theatre.theatreId);
          }}
          style={{ gap: '6px' }}
          aria-label={`Explore shows at ${theatre.theatreName}`}
        >
          <span>Explore Shows</span>
          <ArrowRight size={14} aria-hidden="true" />
        </button>
      </div>
    </article>
  );
};
