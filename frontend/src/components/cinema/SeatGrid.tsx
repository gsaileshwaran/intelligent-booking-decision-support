import React, { useMemo } from 'react';
import type { SeatAvailabilityDetail } from '../../types/show';
import type { SeatQualityScore } from '../../types/booking';
import { Check, X, ShieldAlert, Clock } from 'lucide-react';

interface SeatGridProps {
  seats: SeatAvailabilityDetail[];
  selectedSeatIds?: number[];
  onToggleSeat?: (seatId: number) => void;
  isInteractive?: boolean; // True for manager override or customer selection
  isCustomerSelection?: boolean; // True for customer interactive selection
  seatScores?: Record<number, SeatQualityScore>;
}

export const SeatGrid: React.FC<SeatGridProps> = ({
  seats,
  selectedSeatIds = [],
  onToggleSeat,
  isInteractive = false,
  isCustomerSelection = false,
  seatScores = {},
}) => {
  // Group seats by rowLabel and sort
  const rows = useMemo(() => {
    const map = new Map<string, SeatAvailabilityDetail[]>();
    seats.forEach((seat) => {
      const row = seat.rowLabel || 'A';
      if (!map.has(row)) {
        map.set(row, []);
      }
      map.get(row)!.push(seat);
    });

    const sortedRowKeys = Array.from(map.keys()).sort();
    return sortedRowKeys.map((rowKey) => {
      const rowSeats = map.get(rowKey)!;
      rowSeats.sort((a, b) => parseInt(a.seatNumber, 10) - parseInt(b.seatNumber, 10));
      return {
        rowLabel: rowKey,
        seats: rowSeats,
      };
    });
  }, [seats]);

  const canInteractWithSeat = (seat: SeatAvailabilityDetail) => {
    if (!isInteractive) return false;
    if (isCustomerSelection) {
      // Customers can select AVAILABLE seats, or deselect already selected seats
      return seat.availabilityStatus === 'AVAILABLE' || selectedSeatIds.includes(seat.seatId);
    }
    // Managers can toggle any seat
    return true;
  };

  return (
    <div style={{ width: '100%', overflowX: 'auto', padding: '24px 0' }} data-testid="seat-grid-container">
      {/* Screen Graphic */}
      <div className="seat-screen-indicator" aria-hidden="true" />

      {/* Auditorium Seat Matrix */}
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        alignItems: 'center',
        minWidth: '640px',
      }}>
        {rows.map(({ rowLabel, seats: rowSeats }) => (
          <div key={rowLabel} style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            {/* Row Letter Left */}
            <span style={{
              width: '24px',
              textAlign: 'center',
              fontWeight: 700,
              fontSize: '0.85rem',
              color: 'var(--text-muted)',
            }}>
              {rowLabel}
            </span>

            {/* Seats in Row */}
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              {rowSeats.map((seat) => {
                const isSelected = selectedSeatIds.includes(seat.seatId);
                const isAvailable = seat.availabilityStatus === 'AVAILABLE';
                const hasAisle = Boolean(seat.aisleAfter);
                const zone = (seat.pricingZone || 'STANDARD').toUpperCase();

                let statusClass = `seat-${seat.availabilityStatus.toLowerCase()}`;
                if (isSelected) {
                  statusClass = 'seat-selected';
                }

                const score = seatScores[seat.seatId];
                const tier = score?.tier || score?.badge;
                const scoreValue = score?.totalScore ?? score?.score;

                const interactable = canInteractWithSeat(seat);

                let title = `Row ${seat.rowLabel} Seat ${seat.seatNumber} — ${seat.seatType}`;
                if (seat.pricingZone) {
                  title += ` • ${zone} Zone`;
                }
                if (seat.price != null) {
                  title += ` • ₹${seat.price}`;
                }
                title += ` (${seat.availabilityStatus})`;
                if (score) {
                  const sweetSpotDesc = (tier === 'OPTIMAL' || tier === 'EXCELLENT')
                    ? ' • Viewing Sweet Spot (preferred viewing area)'
                    : (tier === 'PRIME' || tier === 'VERY_GOOD')
                    ? ' • Prime Viewing Area'
                    : '';
                  title += `\nQuality Score: ${scoreValue}/100 (${tier}${sweetSpotDesc})\nFeatures: ${score.tag || score.viewCategory || 'Center Screen Sightline'}`;
                }

                // Zone styling accents for available seats
                let zoneAccentStyle: React.CSSProperties = {};
                if (isAvailable && !isSelected) {
                  if (zone === 'PREMIUM') {
                    zoneAccentStyle = { borderColor: 'rgba(245, 158, 11, 0.5)', borderWidth: '1.5px' };
                  } else if (zone === 'VALUE') {
                    zoneAccentStyle = { borderColor: 'rgba(56, 189, 248, 0.4)', borderWidth: '1.5px' };
                  }
                }

                const isSweetSpotTier = tier === 'OPTIMAL' || tier === 'EXCELLENT';
                const isPrimeTier = tier === 'PRIME' || tier === 'VERY_GOOD';

                return (
                  <React.Fragment key={seat.seatId}>
                    <div
                      onClick={() => {
                        if (interactable && onToggleSeat) {
                          onToggleSeat(seat.seatId);
                        }
                      }}
                      className={`seat-node ${statusClass}`}
                      style={{
                        cursor: interactable ? 'pointer' : 'default',
                        position: 'relative',
                        outline: isSelected ? '2px solid #ffffff' : 'none',
                        outlineOffset: '2px',
                        transform: isSelected ? 'scale(1.15)' : 'none',
                        boxShadow: isSelected
                          ? '0 0 12px rgba(229, 9, 20, 0.7)'
                          : isSweetSpotTier && isAvailable
                          ? '0 0 6px rgba(245, 158, 11, 0.4)'
                          : 'none',
                        transition: 'all 0.15s ease',
                        ...zoneAccentStyle,
                      }}
                      title={title}
                      aria-label={`Seat ${seat.rowLabel}-${seat.seatNumber}, ${zone} zone, ${seat.price ? '₹' + seat.price : ''}, ${seat.availabilityStatus}`}
                      role={interactable ? 'checkbox' : 'img'}
                      aria-checked={interactable ? isSelected : undefined}
                      data-testid={`seat-node-${seat.seatId}`}
                    >
                      <span>{seat.seatNumber}</span>

                      {/* Quality Tier Indicator Dot */}
                      {isSweetSpotTier && isAvailable && !isSelected && (
                        <span style={{
                          position: 'absolute',
                          top: '-3px',
                          right: '-3px',
                          width: '7px',
                          height: '7px',
                          borderRadius: '50%',
                          background: '#f59e0b',
                          boxShadow: '0 0 4px #f59e0b',
                        }} />
                      )}
                      {isPrimeTier && isAvailable && !isSelected && (
                        <span style={{
                          position: 'absolute',
                          top: '-3px',
                          right: '-3px',
                          width: '6px',
                          height: '6px',
                          borderRadius: '50%',
                          background: '#10b981',
                        }} />
                      )}
                    </div>

                    {/* Physical Aisle Walkway Spacer */}
                    {hasAisle && (
                      <div
                        style={{
                          width: '22px',
                          height: '100%',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                        aria-hidden="true"
                        title="Auditorium Walkway Aisle"
                      >
                        <div style={{
                          width: '1px',
                          height: '18px',
                          background: 'rgba(255, 255, 255, 0.08)',
                        }} />
                      </div>
                    )}
                  </React.Fragment>
                );
              })}
            </div>

            {/* Row Letter Right */}
            <span style={{
              width: '24px',
              textAlign: 'center',
              fontWeight: 700,
              fontSize: '0.85rem',
              color: 'var(--text-muted)',
            }}>
              {rowLabel}
            </span>
          </div>
        ))}
      </div>

      {/* Accessible Status Legend */}
      <div className="seat-legend" style={{ justifyContent: 'center', marginTop: '36px', flexWrap: 'wrap', gap: '20px' }}>
        <div className="seat-legend-item">
          <div className="seat-node seat-available" aria-hidden="true">
            <Check size={12} />
          </div>
          <span>Available</span>
        </div>

        {isCustomerSelection && (
          <div className="seat-legend-item">
            <div className="seat-node seat-selected" style={{ background: 'var(--accent-crimson)', color: '#fff', border: '1.5px solid #fff' }} aria-hidden="true">
              &bull;
            </div>
            <span>Your Selected Seats</span>
          </div>
        )}

        <div className="seat-legend-item">
          <div className="seat-node seat-held" aria-hidden="true" style={{ background: 'rgba(245, 158, 11, 0.2)', border: '1.5px solid #f59e0b', color: '#fbbf24' }}>
            <Clock size={11} />
          </div>
          <span>Temporarily Held (5-Min Lock)</span>
        </div>

        <div className="seat-legend-item">
          <div className="seat-node seat-booked" aria-hidden="true">
            <X size={12} />
          </div>
          <span>Booked (Occupied)</span>
        </div>

        <div className="seat-legend-item">
          <div className="seat-node seat-blocked" aria-hidden="true">
            <ShieldAlert size={12} />
          </div>
          <span>Blocked (Administrative)</span>
        </div>

        <div className="seat-legend-item">
          <div className="seat-node seat-available" style={{ borderColor: 'rgba(245, 158, 11, 0.6)', borderWidth: '1.5px' }} aria-hidden="true">
            <span style={{ fontSize: '9px', fontWeight: 700, color: '#f59e0b' }}>P</span>
          </div>
          <span>Premium Zone</span>
        </div>

        <div className="seat-legend-item">
          <div className="seat-node seat-available" aria-hidden="true">
            <span style={{ fontSize: '9px', fontWeight: 700, color: 'var(--text-muted)' }}>S</span>
          </div>
          <span>Standard Zone</span>
        </div>

        <div className="seat-legend-item">
          <div className="seat-node seat-available" style={{ borderColor: 'rgba(56, 189, 248, 0.6)', borderWidth: '1.5px' }} aria-hidden="true">
            <span style={{ fontSize: '9px', fontWeight: 700, color: '#38bdf8' }}>V</span>
          </div>
          <span>Value Zone</span>
        </div>

        <div className="seat-legend-item">
          <div style={{ width: '12px', height: '16px', borderLeft: '2px dashed rgba(255,255,255,0.2)', margin: '0 4px' }} aria-hidden="true" />
          <span>Walkway Aisle</span>
        </div>

        {Object.keys(seatScores).length > 0 && (
          <div className="seat-legend-item" style={{ maxWidth: '460px', alignItems: 'center' }}>
            <span style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              background: '#f59e0b',
              boxShadow: '0 0 6px #f59e0b',
              display: 'inline-block',
              flexShrink: 0,
            }} />
            <span style={{ color: '#fbbf24', fontSize: '0.8rem', fontWeight: 500 }}>
              <strong>Viewing sweet spot</strong> — seats in the auditorium's preferred viewing area.
            </span>
          </div>
        )}
      </div>
    </div>
  );
};
