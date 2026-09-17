import React, { useState, useEffect } from 'react';
import { ArrowLeft, RefreshCw, Eye } from 'lucide-react';
import { managerApi } from '../../api/client';
import type { ShowSeatAvailabilityResponse, SeatAvailabilityDetail } from '../../types/show';
import { useToast } from '../../context/ToastContext';

interface ManagerShowInspectionViewProps {
  showId: number;
  onNavigate: (view: string, param?: any) => void;
}


const zoneBadge = (zone?: string) => {
  if (zone === 'PREMIUM') return { color: 'var(--accent-gold)', label: 'PRE' };
  if (zone === 'VALUE') return { color: 'var(--accent-emerald)', label: 'VAL' };
  return { color: 'var(--text-muted)', label: 'STD' };
};

export const ManagerShowInspectionView: React.FC<ManagerShowInspectionViewProps> = ({ showId, onNavigate }) => {
  const { showToast } = useToast();
  const [data, setData] = useState<ShowSeatAvailabilityResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      setLoading(true);
      const result = await managerApi.getShowSeats(showId);
      setData(result);
    } catch (err: any) {
      console.error('Failed to load show seats', err);
      showToast(err.message || 'Failed to load show seat data', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [showId]);

  const seats = data?.seats || [];
  const rowGroups: Record<string, SeatAvailabilityDetail[]> = {};
  seats.forEach((s) => {
    const row = s.rowLabel || '?';
    if (!rowGroups[row]) rowGroups[row] = [];
    rowGroups[row].push(s);
  });

  const counts = {
    available: seats.filter((s) => s.availabilityStatus?.toUpperCase() === 'AVAILABLE').length,
    held: seats.filter((s) => s.availabilityStatus?.toUpperCase() === 'HELD').length,
    booked: seats.filter((s) => ['BOOKED', 'CONFIRMED'].includes(s.availabilityStatus?.toUpperCase() || '')).length,
    blocked: seats.filter((s) => s.availabilityStatus?.toUpperCase() === 'BLOCKED' || s.physicalStatus === 'BLOCKED').length,
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="manager-show-inspection-view">
      <div>
        <button
          onClick={() => onNavigate('manager-shows', 1)}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Show Scheduler
        </button>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>
            <Eye size={22} style={{ verticalAlign: 'middle', marginRight: '10px', color: 'var(--accent-gold)' }} />
            Show Seat Audit
          </h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Read-only manager inspection — Show #{showId}
            {data?.movieTitle && <span style={{ marginLeft: '8px', fontWeight: 600, color: 'var(--text-primary)' }}>· {data.movieTitle}</span>}
            {data?.screenName && <span style={{ marginLeft: '8px', color: 'var(--text-secondary)' }}>· {data.screenName}</span>}
          </p>
        </div>
        <button onClick={loadData} disabled={loading} className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      {/* Summary Stats */}
      {data && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px' }}>
          {[
            { label: 'Total Seats', value: seats.length, color: 'var(--text-primary)' },
            { label: 'Available', value: counts.available, color: 'var(--accent-emerald)' },
            { label: 'Held', value: counts.held, color: '#f59e0b' },
            { label: 'Booked', value: counts.booked, color: '#ef4444' },
            { label: 'Blocked', value: counts.blocked, color: 'var(--text-muted)' },
          ].map(({ label, value, color }) => (
            <div key={label} className="card" style={{ padding: '16px', textAlign: 'center' }}>
              <div style={{ fontSize: '1.8rem', fontWeight: 700, color }}>{value}</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>{label}</div>
            </div>
          ))}
        </div>
      )}

      {/* Legend */}
      <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Legend:</span>
        {[
          { label: 'Available', cls: 'badge-emerald' },
          { label: 'Held', cls: 'badge-amber' },
          { label: 'Booked', cls: 'badge-crimson' },
          { label: 'Blocked', cls: 'badge-slate' },
        ].map(({ label, cls }) => (
          <span key={label} className={`badge ${cls}`} style={{ fontSize: '0.7rem' }}>{label}</span>
        ))}
        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginLeft: '8px' }}>Zones:</span>
        <span style={{ fontSize: '0.7rem', color: 'var(--accent-gold)', fontWeight: 700 }}>● PRE</span>
        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 700 }}>● STD</span>
        <span style={{ fontSize: '0.7rem', color: 'var(--accent-emerald)', fontWeight: 700 }}>● VAL</span>
      </div>

      {/* Seat Grid — Screen Wise by Row */}
      <div className="card" style={{ padding: '24px' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            Loading seat data...
          </div>
        ) : seats.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            No seat data available for this show.
          </div>
        ) : (
          <>
            {/* Screen indicator */}
            <div style={{
              textAlign: 'center',
              padding: '8px',
              background: 'var(--bg-elevated)',
              borderRadius: '8px',
              marginBottom: '28px',
              color: 'var(--text-muted)',
              fontSize: '0.8rem',
              letterSpacing: '4px',
              textTransform: 'uppercase',
            }}>
              ▬▬▬▬▬▬ SCREEN ▬▬▬▬▬▬
            </div>

            {Object.keys(rowGroups).sort().map((row) => {
              const rowSeats = rowGroups[row].slice().sort((a, b) => {
                const na = parseInt(a.seatNumber?.replace(/\D/g, '') || '0', 10);
                const nb = parseInt(b.seatNumber?.replace(/\D/g, '') || '0', 10);
                return na - nb;
              });

              return (
                <div key={row} style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                  <div style={{
                    width: '28px',
                    textAlign: 'center',
                    fontWeight: 700,
                    fontSize: '0.8rem',
                    color: 'var(--text-muted)',
                    flexShrink: 0,
                  }}>
                    {row}
                  </div>
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {rowSeats.map((seat) => {
                      const status = seat.availabilityStatus?.toUpperCase() || 'AVAILABLE';
                      const isPhysBlocked = seat.physicalStatus === 'BLOCKED';
                      const effectiveStatus = isPhysBlocked ? 'BLOCKED' : status;
                      const zone = zoneBadge(seat.pricingZone);

                      let bg = '#1e293b';
                      let border = '1.5px solid #334155';
                      let textColor = 'var(--text-muted)';
                      if (effectiveStatus === 'AVAILABLE') { bg = 'rgba(16,185,129,0.18)'; border = '1.5px solid #10b981'; textColor = '#10b981'; }
                      if (effectiveStatus === 'HELD') { bg = 'rgba(245,158,11,0.18)'; border = '1.5px solid #f59e0b'; textColor = '#f59e0b'; }
                      if (['BOOKED', 'CONFIRMED'].includes(effectiveStatus)) { bg = 'rgba(239,68,68,0.18)'; border = '1.5px solid #ef4444'; textColor = '#ef4444'; }
                      if (effectiveStatus === 'BLOCKED') { bg = 'rgba(100,116,139,0.18)'; border = '1.5px dashed #64748b'; textColor = '#64748b'; }

                      return (
                        <div
                          key={seat.seatId}
                          title={`${seat.rowLabel}${seat.seatNumber} — ${effectiveStatus}${seat.pricingZone ? ` [${seat.pricingZone}]` : ''}${seat.ticketPrice ? ` ₹${seat.ticketPrice}` : ''}${isPhysBlocked ? ' (Physically Blocked)' : ''}`}
                          style={{
                            width: '36px',
                            height: '32px',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            borderRadius: '4px',
                            background: bg,
                            border,
                            cursor: 'default',
                            fontSize: '0.6rem',
                            gap: '1px',
                          }}
                        >
                          <span style={{ color: zone.color, fontSize: '5px', lineHeight: 1 }}>●</span>
                          <span style={{ color: textColor, fontWeight: 600, fontSize: '0.65rem', lineHeight: 1 }}>
                            {seat.seatNumber}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </>
        )}
      </div>

      {/* ⚠ NO customer booking / hold / checkout buttons below this line */}
      <div style={{
        padding: '12px 16px',
        background: 'rgba(234,179,8,0.08)',
        border: '1px solid rgba(234,179,8,0.3)',
        borderRadius: '8px',
        fontSize: '0.8rem',
        color: 'var(--text-muted)',
      }}>
        ℹ This is a <strong>read-only manager audit view</strong>. To physically block/unblock seats on the screen, 
        use <strong>Seat Management</strong> from the Manager Dashboard. Seat holds and bookings are managed by customers.
      </div>
    </div>
  );
};
