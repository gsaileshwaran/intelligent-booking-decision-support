import React, { useState, useEffect } from 'react';
import { ArrowLeft, ShieldAlert, ShieldCheck, RefreshCw, AlertTriangle } from 'lucide-react';
import { managerApi } from '../../api/client';
import type { Screen, Seat } from '../../types/theatre';
import { useToast } from '../../context/ToastContext';

interface SeatManagementViewProps {
  theatreId: number;
  onNavigate: (view: string, param?: any) => void;
}

const zoneBg = (zone?: string) => {
  if (zone === 'PREMIUM') return 'rgba(234,179,8,0.15)';
  if (zone === 'VALUE') return 'rgba(16,185,129,0.12)';
  return 'rgba(148,163,184,0.08)';
};
const zoneBorder = (zone?: string) => {
  if (zone === 'PREMIUM') return '1.5px solid rgba(234,179,8,0.6)';
  if (zone === 'VALUE') return '1.5px solid rgba(16,185,129,0.5)';
  return '1.5px solid rgba(148,163,184,0.25)';
};

export const SeatManagementView: React.FC<SeatManagementViewProps> = ({ theatreId, onNavigate }) => {
  const { showToast } = useToast();
  const [screens, setScreens] = useState<Screen[]>([]);
  const [selectedScreenId, setSelectedScreenId] = useState<number | null>(null);
  const [seats, setSeats] = useState<Seat[]>([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([]);
  const [loadingScreens, setLoadingScreens] = useState(true);
  const [loadingSeats, setLoadingSeats] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [blockReason, setBlockReason] = useState('');
  const [showBlockModal, setShowBlockModal] = useState(false);
  const [blockAction, setBlockAction] = useState<'ACTIVE' | 'BLOCKED'>('BLOCKED');
  const [warningMessage, setWarningMessage] = useState<string | null>(null);

  useEffect(() => {
    managerApi.getScreens(theatreId)
      .then((data) => {
        setScreens(data || []);
        if (data && data.length > 0) {
          setSelectedScreenId(data[0].screenId);
        }
      })
      .catch((err) => showToast(err.message || 'Failed to load screens', 'error'))
      .finally(() => setLoadingScreens(false));
  }, [theatreId]);

  const loadSeats = async (screenId: number) => {
    try {
      setLoadingSeats(true);
      setSelectedSeatIds([]);
      setWarningMessage(null);
      const data = await managerApi.getSeats(screenId);
      setSeats(data || []);
    } catch (err: any) {
      showToast(err.message || 'Failed to load seats', 'error');
    } finally {
      setLoadingSeats(false);
    }
  };

  useEffect(() => {
    if (selectedScreenId) loadSeats(selectedScreenId);
  }, [selectedScreenId]);

  const handleToggleSeat = (seatId: number) => {
    setSelectedSeatIds((prev) =>
      prev.includes(seatId) ? prev.filter((id) => id !== seatId) : [...prev, seatId]
    );
  };

  const handleSelectAll = (status: 'ACTIVE' | 'BLOCKED') => {
    const filtered = seats.filter((s) => s.status === status).map((s) => s.seatId);
    setSelectedSeatIds(filtered);
  };

  const openActionModal = (action: 'ACTIVE' | 'BLOCKED') => {
    if (selectedSeatIds.length === 0) {
      showToast('Select at least one seat first.', 'info');
      return;
    }
    setBlockAction(action);
    setBlockReason('');
    setWarningMessage(null);
    setShowBlockModal(true);
  };

  const handleConfirmAction = async () => {
    if (!selectedScreenId || selectedSeatIds.length === 0) return;
    try {
      setUpdating(true);
      setWarningMessage(null);
      let lastWarning: string | null = null;
      // Update each seat individually (the API works seat by seat)
      for (const seatId of selectedSeatIds) {
        const result = await managerApi.updateSeat(selectedScreenId, seatId, {
          status: blockAction,
          blockReason: blockReason.trim() || undefined,
        });
        if (result.warningMessage) {
          lastWarning = result.warningMessage;
        }
      }
      showToast(
        `${selectedSeatIds.length} seat(s) ${blockAction === 'BLOCKED' ? 'blocked' : 'unblocked'} successfully.`,
        'success'
      );
      setShowBlockModal(false);
      setSelectedSeatIds([]);
      if (lastWarning) setWarningMessage(lastWarning);
      await loadSeats(selectedScreenId);
    } catch (err: any) {
      showToast(err.message || 'Failed to update seats', 'error');
    } finally {
      setUpdating(false);
    }
  };

  // Group seats by row
  const rowGroups: Record<string, Seat[]> = {};
  seats.forEach((s) => {
    const row = s.rowLabel || '?';
    if (!rowGroups[row]) rowGroups[row] = [];
    rowGroups[row].push(s);
  });

  const activeSeatCount = seats.filter((s) => s.status === 'ACTIVE').length;
  const blockedSeatCount = seats.filter((s) => s.status === 'BLOCKED').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="seat-management-view">
      <div>
        <button onClick={() => onNavigate('manager-dashboard')} className="btn btn-sm btn-outline" style={{ gap: '6px' }}>
          <ArrowLeft size={16} />
          Back to Manager Dashboard
        </button>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Seat Management &amp; Blocking</h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Screen-wise physical seat control — block/unblock seats for maintenance across all shows on a screen
          </p>
        </div>
      </div>

      {/* Screen Selector */}
      <div className="card" style={{ padding: '20px' }}>
        <label className="form-label" htmlFor="screen-select" style={{ marginBottom: '8px', display: 'block' }}>
          Select Auditorium Screen
        </label>
        {loadingScreens ? (
          <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Loading screens...</div>
        ) : screens.length === 0 ? (
          <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No screens found for this theatre.</div>
        ) : (
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {screens.map((sc) => (
              <button
                key={sc.screenId}
                id={`screen-btn-${sc.screenId}`}
                onClick={() => setSelectedScreenId(sc.screenId)}
                className={`btn btn-sm ${selectedScreenId === sc.screenId ? 'btn-primary' : 'btn-secondary'}`}
                style={{ fontWeight: selectedScreenId === sc.screenId ? 700 : 400 }}
              >
                {sc.screenName}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Seat Stats + Actions */}
      {selectedScreenId && (
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
          <div className="card" style={{ padding: '12px 20px', display: 'flex', gap: '24px' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--accent-emerald)' }}>{activeSeatCount}</div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Active</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-muted)' }}>{blockedSeatCount}</div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Blocked</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--accent-gold)' }}>{selectedSeatIds.length}</div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Selected</div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            <button className="btn btn-sm btn-outline" onClick={() => handleSelectAll('ACTIVE')} style={{ fontSize: '0.8rem' }}>
              Select All Active
            </button>
            <button className="btn btn-sm btn-outline" onClick={() => handleSelectAll('BLOCKED')} style={{ fontSize: '0.8rem' }}>
              Select All Blocked
            </button>
            <button className="btn btn-sm btn-outline" onClick={() => setSelectedSeatIds([])} style={{ fontSize: '0.8rem' }}>
              Clear Selection
            </button>
          </div>

          <div style={{ display: 'flex', gap: '8px', marginLeft: 'auto' }}>
            <button
              className="btn btn-sm btn-danger"
              onClick={() => openActionModal('BLOCKED')}
              disabled={selectedSeatIds.length === 0}
              style={{ gap: '6px' }}
            >
              <ShieldAlert size={14} />
              Block Selected
            </button>
            <button
              className="btn btn-sm btn-secondary"
              onClick={() => openActionModal('ACTIVE')}
              disabled={selectedSeatIds.length === 0}
              style={{ gap: '6px', color: 'var(--accent-emerald)' }}
            >
              <ShieldCheck size={14} />
              Unblock Selected
            </button>
            <button
              className="btn btn-sm btn-outline"
              onClick={() => selectedScreenId && loadSeats(selectedScreenId)}
              disabled={loadingSeats}
              style={{ gap: '6px' }}
            >
              <RefreshCw size={14} className={loadingSeats ? 'animate-spin' : ''} />
              Refresh
            </button>
          </div>
        </div>
      )}

      {/* Warning Banner */}
      {warningMessage && (
        <div style={{
          padding: '14px 18px',
          background: 'rgba(239,68,68,0.1)',
          border: '1px solid rgba(239,68,68,0.4)',
          borderRadius: '8px',
          display: 'flex',
          gap: '10px',
          alignItems: 'flex-start',
        }}>
          <AlertTriangle size={18} style={{ color: '#ef4444', flexShrink: 0, marginTop: '2px' }} />
          <span style={{ fontSize: '0.85rem', color: 'var(--text-primary)' }}>{warningMessage}</span>
          <button onClick={() => setWarningMessage(null)} className="btn-icon" style={{ marginLeft: 'auto' }}>×</button>
        </div>
      )}

      {/* Physical Seat Grid */}
      {selectedScreenId && (
        <div className="card" style={{ padding: '24px' }}>
          <div style={{ marginBottom: '16px', display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Click seat to select/deselect. Zones:</span>
            <span style={{ fontSize: '0.7rem', color: 'var(--accent-gold)' }}>■ PREMIUM</span>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>■ STANDARD</span>
            <span style={{ fontSize: '0.7rem', color: 'var(--accent-emerald)' }}>■ VALUE</span>
            <span style={{ fontSize: '0.7rem', color: '#64748b' }}>░ BLOCKED</span>
          </div>

          {/* Screen Edge */}
          <div style={{
            textAlign: 'center',
            padding: '7px',
            background: 'var(--bg-elevated)',
            borderRadius: '8px',
            marginBottom: '24px',
            color: 'var(--text-muted)',
            fontSize: '0.75rem',
            letterSpacing: '4px',
          }}>
            ▬▬▬▬▬▬▬ SCREEN ▬▬▬▬▬▬▬
          </div>

          {loadingSeats ? (
            <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading seat grid...</div>
          ) : seats.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
              No seats found for this screen.
            </div>
          ) : (
            Object.keys(rowGroups).sort().map((row) => {
              const rowSeats = rowGroups[row].slice().sort((a, b) => {
                const na = parseInt(a.seatNumber?.replace(/\D/g, '') || '0', 10);
                const nb = parseInt(b.seatNumber?.replace(/\D/g, '') || '0', 10);
                return na - nb;
              });
              return (
                <div key={row} style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                  <div style={{ width: '28px', textAlign: 'center', fontWeight: 700, fontSize: '0.8rem', color: 'var(--text-muted)', flexShrink: 0 }}>
                    {row}
                  </div>
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {rowSeats.map((seat, idx) => {
                      const isBlocked = seat.status !== 'ACTIVE';
                      const isSelected = selectedSeatIds.includes(seat.seatId);

                      return (
                        <React.Fragment key={seat.seatId}>
                          {seat.aisleAfter && idx > 0 && <div style={{ width: '12px' }} />}
                          <button
                            onClick={() => handleToggleSeat(seat.seatId)}
                            title={`${seat.rowLabel}${seat.seatNumber} — ${seat.status}${seat.pricingZone ? ` [${seat.pricingZone}]` : ''}`}
                            style={{
                              width: '36px',
                              height: '32px',
                              borderRadius: '4px',
                              border: isSelected
                                ? '2px solid var(--accent-primary)'
                                : isBlocked
                                ? '1.5px dashed #64748b'
                                : zoneBorder(seat.pricingZone),
                              background: isSelected
                                ? 'rgba(99,102,241,0.35)'
                                : isBlocked
                                ? 'rgba(100,116,139,0.1)'
                                : zoneBg(seat.pricingZone),
                              cursor: 'pointer',
                              fontSize: '0.62rem',
                              fontWeight: isSelected ? 700 : 400,
                              color: isSelected
                                ? 'var(--accent-primary)'
                                : isBlocked
                                ? '#64748b'
                                : 'var(--text-primary)',
                              position: 'relative',
                              transition: 'all 0.12s ease',
                              opacity: isBlocked ? 0.6 : 1,
                            }}
                          >
                            {isBlocked && (
                              <span style={{ position: 'absolute', top: '2px', right: '2px', fontSize: '6px', color: '#64748b' }}>✕</span>
                            )}
                            {seat.seatNumber}
                          </button>
                        </React.Fragment>
                      );
                    })}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Block/Unblock Confirmation Modal */}
      {showBlockModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                {blockAction === 'BLOCKED'
                  ? <><ShieldAlert size={18} style={{ color: '#ef4444' }} /> Block {selectedSeatIds.length} Seat(s)</>
                  : <><ShieldCheck size={18} style={{ color: 'var(--accent-emerald)' }} /> Unblock {selectedSeatIds.length} Seat(s)</>}
              </h3>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {blockAction === 'BLOCKED' && (
                <div>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '12px' }}>
                    These seats will be <strong style={{ color: '#ef4444' }}>physically blocked</strong> across <em>all shows</em> on this screen.
                    Customers will not be able to hold or book them until unblocked.
                  </p>
                  <label className="form-label" htmlFor="block-reason">Block Reason (optional)</label>
                  <input
                    id="block-reason"
                    type="text"
                    className="input"
                    placeholder="e.g. Seat cushion damage, VIP reserve, etc."
                    value={blockReason}
                    onChange={(e) => setBlockReason(e.target.value)}
                    maxLength={200}
                  />
                </div>
              )}
              {blockAction === 'ACTIVE' && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  These seats will be <strong style={{ color: 'var(--accent-emerald)' }}>reactivated</strong> and become available for booking in all upcoming shows.
                </p>
              )}
            </div>
            <div className="modal-footer">
              <button type="button" onClick={() => setShowBlockModal(false)} className="btn btn-secondary btn-sm">Cancel</button>
              <button
                type="button"
                onClick={handleConfirmAction}
                disabled={updating}
                className={`btn btn-sm ${blockAction === 'BLOCKED' ? 'btn-danger' : 'btn-primary'}`}
                style={{ gap: '6px' }}
              >
                {updating ? 'Processing...' : blockAction === 'BLOCKED' ? 'Confirm Block' : 'Confirm Unblock'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
