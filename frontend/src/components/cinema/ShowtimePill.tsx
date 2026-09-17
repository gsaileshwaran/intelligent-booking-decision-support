import React from 'react';
import type { Show } from '../../types/show';

interface ShowtimePillProps {
  show: Show;
  onSelect: (showId: number) => void;
}

export const ShowtimePill: React.FC<ShowtimePillProps> = ({ show, onSelect }) => {
  const startTime = new Date(show.startAt);
  const timeFormatted = startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });

  const isCancelled = show.showStatus === 'CANCELLED';
  const availableCount = show.availableSeats;
  const isSoldOut = availableCount !== undefined && availableCount <= 0;
  const isLowAvailability = availableCount !== undefined && availableCount > 0 && availableCount <= 20;

  return (
    <button
      onClick={() => onSelect(show.showId)}
      disabled={isCancelled || isSoldOut}
      className="btn"
      style={{
        display: 'inline-flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        padding: '12px 16px',
        minWidth: '175px',
        borderRadius: 'var(--radius-md)',
        background: isCancelled ? 'rgba(239, 68, 68, 0.08)' : isSoldOut ? 'rgba(255, 255, 255, 0.03)' : 'var(--bg-elevated)',
        border: `1px solid ${isCancelled ? 'rgba(239, 68, 68, 0.25)' : isSoldOut ? 'var(--border-subtle)' : 'var(--border-medium)'}`,
        cursor: isCancelled || isSoldOut ? 'not-allowed' : 'pointer',
        transition: 'all 0.15s ease',
        textAlign: 'left',
        gap: '6px',
      }}
      data-testid={`showtime-pill-${show.showId}`}
      aria-label={`Show at ${timeFormatted} on ${show.screenName}, ${show.languageName}, ${show.presentationFormat || '2D'}, ${availableCount ?? ''} seats available${isCancelled ? ', Cancelled' : ''}`}
    >
      {/* Top Header: Screen & Format */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', gap: '8px' }}>
        <span style={{
          fontSize: '0.72rem',
          fontWeight: 700,
          color: 'var(--text-muted)',
          textTransform: 'uppercase',
          letterSpacing: '0.04em',
        }}>
          {show.screenName?.split('-')[0]?.trim() || 'Screen'}
        </span>
        <span style={{
          fontSize: '0.68rem',
          fontWeight: 700,
          background: show.formatCode === 'IMAX' || show.presentationFormat?.toLowerCase().includes('imax')
            ? 'rgba(245, 158, 11, 0.2)'
            : 'rgba(255, 255, 255, 0.08)',
          color: show.formatCode === 'IMAX' || show.presentationFormat?.toLowerCase().includes('imax')
            ? '#fbbf24'
            : 'var(--text-secondary)',
          padding: '2px 6px',
          borderRadius: '4px',
        }}>
          {show.formatCode || show.presentationFormat || '2D'}
        </span>
      </div>

      {/* Main Showtime */}
      <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
        <span style={{
          fontSize: '1.15rem',
          fontWeight: 800,
          color: isCancelled ? '#ef4444' : isSoldOut ? 'var(--text-muted)' : '#ffffff',
          letterSpacing: '-0.02em',
        }}>
          {timeFormatted}
        </span>
        {show.languageName && (
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 500 }}>
            {show.languageName}
          </span>
        )}
      </div>

      {/* Bottom Metadata: Availability & Pricing */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        width: '100%',
        marginTop: '2px',
        paddingTop: '6px',
        borderTop: '1px solid var(--border-subtle)',
        fontSize: '0.73rem',
        gap: '8px',
      }}>
        {isCancelled ? (
          <span style={{ color: '#ef4444', fontWeight: 600 }}>Cancelled</span>
        ) : isSoldOut ? (
          <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Sold Out</span>
        ) : (
          <span style={{
            color: isLowAvailability ? '#f59e0b' : '#10b981',
            fontWeight: 600,
          }}>
            {availableCount != null ? `${availableCount} seats left` : 'Available'}
          </span>
        )}

        {show.minPrice != null && !isCancelled && (
          <span style={{ color: 'var(--text-secondary)', fontWeight: 600 }}>
            From ₹{Math.round(show.minPrice)}
          </span>
        )}
      </div>
    </button>
  );
};
