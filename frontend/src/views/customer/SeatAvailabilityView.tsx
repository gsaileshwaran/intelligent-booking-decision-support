import React, { useState, useEffect, useRef } from 'react';
import { ArrowLeft, RefreshCw, Tv, MapPin, Film, Sparkles, Clock, AlertTriangle, CheckCircle, ShieldCheck } from 'lucide-react';
import { showsApi, seatHoldApi, decisionApi } from '../../api/client';
import type { ShowSeatAvailabilityResponse, Show } from '../../types/show';
import type { SeatQualityScore, SeatRecommendations, DecisionGuidance, BookingResponse, InteractionFriction } from '../../types/booking';
import { SeatGrid } from '../../components/cinema/SeatGrid';
import { CheckoutModal } from '../../components/cinema/CheckoutModal';
import { DisplayOnlyBanner } from '../../components/common/DisplayOnlyBanner';

interface SeatAvailabilityViewProps {
  showId: number;
  initialPartySize?: number;
  initialRecommendedSeatIds?: number[];
  initialRecommendedSeatLabels?: string[];
  onNavigate: (view: string, param?: any) => void;
}

export const SeatAvailabilityView: React.FC<SeatAvailabilityViewProps> = ({
  showId,
  initialPartySize,
  initialRecommendedSeatIds,
  initialRecommendedSeatLabels,
  onNavigate,
}) => {
  const [availability, setAvailability] = useState<ShowSeatAvailabilityResponse | null>(null);
  const [show, setShow] = useState<Show | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Seat Selection State
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([]);

  // Live AI Recommendation Handoff States
  const [handoffNotice, setHandoffNotice] = useState<{ type: 'success' | 'stale'; message: string } | null>(null);
  const hasHandledHandoffRef = useRef(false);

  // Intelligent Decision Engine States
  const [seatScores, setSeatScores] = useState<Record<number, SeatQualityScore>>({});
  const [recommendations, setRecommendations] = useState<SeatRecommendations | null>(null);
  const [guidance, setGuidance] = useState<DecisionGuidance | null>(null);
  const [partySize, setPartySize] = useState<number>(initialPartySize && initialPartySize >= 1 && initialPartySize <= 8 ? initialPartySize : 2);
  const [isChangingPartySize, setIsChangingPartySize] = useState(false);
  const [showAiSuggestions, setShowAiSuggestions] = useState(false);
  const [aiSelectionError, setAiSelectionError] = useState<string | null>(null);
  const [seatToggleCount, setSeatToggleCount] = useState<number>(0);
  const [interactionFriction, setInteractionFriction] = useState<InteractionFriction | null>(null);

  // Seat Hold & Checkout States
  const [holdToken, setHoldToken] = useState<string | null>(null);
  const [holdSecondsLeft, setHoldSecondsLeft] = useState<number>(0);
  const [holdTotalAmount, setHoldTotalAmount] = useState<number>(0);
  const [isHolding, setIsHolding] = useState(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [holdMessage, setHoldMessage] = useState<string | null>(null);

  const timerRef = useRef<any>(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [availData, showData] = await Promise.all([
        showsApi.getShowSeats(showId),
        showsApi.getShow(showId),
      ]);
      setAvailability(availData);
      setShow(showData);

      // Live validation of incoming AI recommendation handoff
      if (initialRecommendedSeatIds && initialRecommendedSeatIds.length > 0 && !hasHandledHandoffRef.current) {
        hasHandledHandoffRef.current = true;
        const requestedSeats = initialRecommendedSeatIds;
        const allAvailable = requestedSeats.every((id) => {
          const s = availData.seats.find((seat) => seat.seatId === id);
          return s && s.availabilityStatus === 'AVAILABLE' && s.physicalStatus !== 'BLOCKED';
        });

        if (allAvailable) {
          setSelectedSeatIds(requestedSeats);
          const labelsText = initialRecommendedSeatLabels && initialRecommendedSeatLabels.length > 0
            ? initialRecommendedSeatLabels.join(', ')
            : requestedSeats.join(', ');
          setHandoffNotice({
            type: 'success',
            message: `AI Recommendation applied: Selected optimal seats (${labelsText}) based on your preferences. You can adjust your selection below.`,
          });
        } else {
          // Stale recommendation: One or more seats became HELD or BOOKED in the interim
          try {
            const freshRecs = await decisionApi.getRecommendations(showId, partySize);
            setRecommendations(freshRecs);
            if (freshRecs && freshRecs.recommendations && freshRecs.recommendations.length > 0) {
              const bestRec = freshRecs.recommendations[0];
              const freshIds = bestRec.seatIds;
              const freshLabels = bestRec.seatLabels?.join(', ') || freshIds.join(', ');
              setSelectedSeatIds(freshIds);
              setHandoffNotice({
                type: 'stale',
                message: `One or more originally recommended seats became unavailable (held or booked). We updated your recommendation to seats ${freshLabels} (${bestRec.rationale || 'Optimal viewing sweet spot'}).`,
              });
            } else {
              setHandoffNotice({
                type: 'stale',
                message: 'One or more originally recommended seats are no longer available. Please select your preferred seats from the map below.',
              });
            }
          } catch (e) {
            setHandoffNotice({
              type: 'stale',
              message: 'One or more originally recommended seats are no longer available. Please select your preferred seats from the map below.',
            });
          }
        }
      }

      // Load Intelligent Engine: Seat Scores & Default Recommendations
      try {
        const scores = await decisionApi.getSeatScores(showId);
        const scoreMap: Record<number, SeatQualityScore> = {};
        scores.forEach((s) => {
          scoreMap[s.seatId] = s;
        });
        setSeatScores(scoreMap);

        const recs = await decisionApi.getRecommendations(showId, partySize);
        setRecommendations(recs);
      } catch (decErr) {
        console.warn('Decision engine metadata unavailable', decErr);
      }
    } catch (err: any) {
      console.error('Failed to load seat availability', err);
      setError(err.message || 'Failed to load seat availability');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [showId]);

  // Update recommendations when party size changes
  useEffect(() => {
    const updateRecs = async () => {
      try {
        const recs = await decisionApi.getRecommendations(showId, partySize);
        setRecommendations(recs);
      } catch (e) {
        // ignore
      }
    };
    if (availability) updateRecs();
  }, [partySize, showId]);

  // Update guidance whenever seat selection changes
  useEffect(() => {
    const updateGuidance = async () => {
      if (selectedSeatIds.length === 0) {
        setGuidance(null);
        return;
      }
      try {
        const g = await decisionApi.getGuidance(showId, selectedSeatIds);
        setGuidance(g);
      } catch (e) {
        // ignore
      }
    };
    updateGuidance();
  }, [selectedSeatIds, showId]);

  // Hold Timer Management
  useEffect(() => {
    if (holdSecondsLeft <= 0 && holdToken) {
      // Hold expired!
      setHoldMessage('Your 5-minute seat hold has expired. The seats have been released.');
      setHoldToken(null);
      setIsCheckoutOpen(false);
      setSelectedSeatIds([]);
      loadData();
      if (timerRef.current) clearInterval(timerRef.current);
      return;
    }

    if (holdSecondsLeft > 0) {
      timerRef.current = setInterval(() => {
        setHoldSecondsLeft((prev) => {
          if (prev <= 1) {
            clearInterval(timerRef.current!);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => {
        if (timerRef.current) clearInterval(timerRef.current);
      };
    }
  }, [holdSecondsLeft, holdToken]);

  const handleToggleSeat = (seatId: number) => {
    // If hold is active, don't allow toggling seats
    if (holdToken) return;

    const nextCount = seatToggleCount + 1;
    setSeatToggleCount(nextCount);
    if (nextCount >= 6) {
      decisionApi.evaluateInteractionFriction({ seatToggleCount: nextCount, timeWindowSeconds: 60 })
        .then((res) => {
          if (res && res.detected) setInteractionFriction(res);
        })
        .catch(() => {});
    }

    setSelectedSeatIds((prev) => {
      if (prev.includes(seatId)) {
        return prev.filter((id) => id !== seatId);
      } else {
        if (prev.length >= 8) {
          alert('Maximum 8 seats per booking transaction.');
          return prev;
        }
        return [...prev, seatId];
      }
    });
  };

  const handleApplyRecommendation = async (seatIds: number[]) => {
    if (holdToken) return;
    setAiSelectionError(null);

    try {
      // Revalidate live availability from backend before applying
      const freshAvail = await showsApi.getShowSeats(showId);
      setAvailability(freshAvail);

      const invalidSeats = seatIds.filter((id) => {
        const seat = freshAvail.seats.find((s) => s.seatId === id);
        return !seat || seat.availabilityStatus !== 'AVAILABLE' || seat.physicalStatus === 'BLOCKED';
      });

      if (invalidSeats.length > 0) {
        setAiSelectionError('Selected AI recommendation is no longer available (some seats were booked, held, or blocked). Fresh suggestions have been updated.');
        try {
          const freshRecs = await decisionApi.getRecommendations(showId, partySize);
          setRecommendations(freshRecs);
        } catch (e) {}
        return;
      }

      // Valid: immediately update the authoritative manual seat selection state
      setSelectedSeatIds(seatIds);
    } catch (err: any) {
      // Fallback local check
      const invalid = seatIds.filter((id) => {
        const seat = availability?.seats.find((s) => s.seatId === id);
        return !seat || seat.availabilityStatus !== 'AVAILABLE' || seat.physicalStatus === 'BLOCKED';
      });
      if (invalid.length > 0) {
        setAiSelectionError('Selected seats are currently unavailable.');
      } else {
        setSelectedSeatIds(seatIds);
      }
    }
  };

  const handleHoldSeats = async () => {
    if (selectedSeatIds.length === 0) {
      alert('Please select at least one available seat to proceed.');
      return;
    }
    try {
      setIsHolding(true);
      setError(null);
      setHoldMessage(null);
      const res = await seatHoldApi.holdSeats(showId, selectedSeatIds);
      setHoldToken(res.holdToken);
      setHoldSecondsLeft(res.expiresInSeconds);
      setHoldTotalAmount(res.totalAmount);
      setIsCheckoutOpen(true);
      // Refresh local seats to show as HELD
      await loadData();
    } catch (err: any) {
      console.error('Seat hold failed', err);
      setError(err.message || 'Seat reservation conflict. Seats may already be held by another customer.');
      // Refresh seat availability to reflect current status
      await loadData();
    } finally {
      setIsHolding(false);
    }
  };

  const handlePaymentSuccess = (booking: BookingResponse) => {
    setIsCheckoutOpen(false);
    setHoldToken(null);
    if (timerRef.current) clearInterval(timerRef.current);
    onNavigate('booking-confirmation', booking.bookingReference);
  };

  const handlePaymentFailure = (msg: string) => {
    setIsCheckoutOpen(false);
    setHoldToken(null);
    if (timerRef.current) clearInterval(timerRef.current);
    setError(`Payment failed: ${msg}. Your held seats have been automatically released.`);
    setSelectedSeatIds([]);
    loadData();
  };

  const selectedSeatObjects = availability?.seats.filter((s) => selectedSeatIds.includes(s.seatId)) || [];
  const exactSelectedTotal = selectedSeatObjects.reduce((sum, s) => sum + (s.price != null ? Number(s.price) : 200), 0);

  const formatTimer = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const s = sec % 60;
    return `${mins.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }} data-testid="seat-availability-view">
      {/* Top Controls */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <button
          onClick={() => {
            if (show?.movieId) {
              onNavigate('movie-details', show.movieId);
            } else {
              onNavigate('movies');
            }
          }}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Showtimes
        </button>

        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          {holdToken && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'rgba(245, 158, 11, 0.15)',
              border: '1px solid #f59e0b',
              borderRadius: 'var(--radius-full)',
              padding: '6px 14px',
              color: '#fbbf24',
              fontWeight: 800,
              fontSize: '0.88rem',
            }}>
              <Clock size={16} className="animate-spin" />
              <span>Seats held for {formatTimer(holdSecondsLeft)}</span>
            </div>
          )}

          <button
            onClick={loadData}
            disabled={loading}
            className="btn btn-sm btn-secondary"
            style={{ gap: '6px' }}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      {/* Scope Disclaimer Banner */}
      <DisplayOnlyBanner message="PVK Cinemas is a cinema discovery and decision-support platform with movie search, showtime discovery, seat selection, temporary seat holds, booking, and simulated payment. Payments use dummy/test data only; no real financial transactions are processed." />

      {holdMessage && (
        <div style={{
          background: 'rgba(239, 68, 68, 0.15)',
          border: '1px solid #ef4444',
          borderRadius: 'var(--radius-md)',
          padding: '12px 18px',
          color: '#fca5a5',
          fontSize: '0.9rem',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
        }}>
          <AlertTriangle size={18} />
          <span>{holdMessage}</span>
        </div>
      )}

      {/* AI Recommendation Handoff Alert Banner */}
      {handoffNotice && (
        <div style={{
          background: handoffNotice.type === 'success' ? 'rgba(16, 185, 129, 0.12)' : 'rgba(245, 158, 11, 0.15)',
          border: `1px solid ${handoffNotice.type === 'success' ? '#10b981' : '#f59e0b'}`,
          borderRadius: 'var(--radius-md)',
          padding: '14px 20px',
          color: handoffNotice.type === 'success' ? '#6ee7b7' : '#fbbf24',
          fontSize: '0.92rem',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
        }} data-testid="ai-handoff-banner">
          {handoffNotice.type === 'success' ? (
            <Sparkles size={20} color="#10b981" style={{ flexShrink: 0 }} />
          ) : (
            <AlertTriangle size={20} color="#f59e0b" style={{ flexShrink: 0 }} />
          )}
          <span style={{ fontWeight: 500 }}>{handoffNotice.message}</span>
        </div>
      )}

      {/* Header Screening Info Card */}
      {show && (
        <div className="card" style={{ padding: '24px 32px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                <Film size={16} color="var(--accent-crimson)" />
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>RESERVE SEATS FOR SCREENING</span>
              </div>
              <h1 style={{ fontSize: '2rem', marginBottom: '6px' }}>{show.movieTitle}</h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: 'var(--text-secondary)', fontSize: '0.92rem', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <MapPin size={15} color="var(--accent-crimson)" />
                  <span>{show.theatreName} ({show.cityName || 'Chennai'})</span>
                </div>
                <span>&bull;</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Tv size={15} color="var(--accent-gold)" />
                  <span>{show.screenName}</span>
                </div>
                <span>&bull;</span>
                <span>{new Date(show.startAt).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}</span>
              </div>
            </div>

            {/* Availability Badges */}
            {availability && (
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <div style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '8px 14px',
                  textAlign: 'center',
                }}>
                  <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--status-available)' }}>
                    {availability.availableSeats}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Available</div>
                </div>

                <div style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '8px 14px',
                  textAlign: 'center',
                }}>
                  <div style={{ fontSize: '1.2rem', fontWeight: 800, color: '#f59e0b' }}>
                    {availability.seats.filter(s => s.availabilityStatus === 'HELD').length}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Held</div>
                </div>

                <div style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '8px 14px',
                  textAlign: 'center',
                }}>
                  <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--status-booked)' }}>
                    {availability.bookedSeats}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Booked</div>
                </div>

                <div style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '8px 14px',
                  textAlign: 'center',
                }}>
                  <div style={{ fontSize: '1.2rem', fontWeight: 800, color: '#ffffff' }}>
                    {availability.totalSeats}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Total</div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Optional Intelligent Decision Support & Party Size */}
      {!showAiSuggestions ? (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '14px 22px',
          background: 'rgba(245, 158, 11, 0.04)',
          border: '1px dashed rgba(245, 158, 11, 0.3)',
          borderRadius: 'var(--radius-md)',
          flexWrap: 'wrap',
          gap: '14px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text-secondary)', fontSize: '0.88rem' }}>
            <Sparkles size={18} color="var(--accent-gold)" />
            <span>Need help choosing optimal acoustic or viewing seats?</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '18px', flexWrap: 'wrap' }}>
            {/* Party size indicator with deliberate change action */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Party:</span>
              <span style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff' }}>{partySize} {partySize === 1 ? 'seat' : 'seats'}</span>
              {!isChangingPartySize ? (
                <button
                  type="button"
                  onClick={() => setIsChangingPartySize(true)}
                  className="btn btn-sm btn-ghost"
                  style={{ fontSize: '0.78rem', padding: '2px 8px', height: '24px', textDecoration: 'underline' }}
                >
                  Change
                </button>
              ) : (
                <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                  {[1, 2, 3, 4, 5, 6].map((size) => (
                    <button
                      key={size}
                      type="button"
                      onClick={() => {
                        setPartySize(size);
                        setIsChangingPartySize(false);
                      }}
                      className={`btn btn-sm ${partySize === size ? 'btn-primary' : 'btn-outline'}`}
                      style={{ minWidth: '28px', height: '24px', padding: '0 4px', fontSize: '0.75rem' }}
                    >
                      {size}
                    </button>
                  ))}
                  <button
                    type="button"
                    onClick={() => setIsChangingPartySize(false)}
                    className="btn btn-sm btn-ghost"
                    style={{ fontSize: '0.75rem', padding: '2px 6px', height: '24px' }}
                  >
                    Done
                  </button>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => setShowAiSuggestions(true)}
              className="btn btn-sm btn-secondary"
              style={{ gap: '6px', fontSize: '0.84rem' }}
              data-testid="get-ai-suggestions-btn"
            >
              <Sparkles size={14} color="#f59e0b" />
              Get AI Suggestions
            </button>
          </div>
        </div>
      ) : (
        <div className="card" style={{
          padding: '20px 24px',
          background: 'linear-gradient(135deg, rgba(229, 9, 20, 0.05) 0%, rgba(245, 158, 11, 0.05) 100%)',
          border: '1px solid var(--border-subtle)',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-gold)', marginBottom: '4px' }}>
                <Sparkles size={18} />
                <span style={{ fontSize: '0.85rem', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  AI Seat Recommendations
                </span>
              </div>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                Acoustic sweet spot & optimal viewing angles for party of {partySize}.
              </p>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
              {/* Party size indicator */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Party:</span>
                <span style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff' }}>{partySize} {partySize === 1 ? 'seat' : 'seats'}</span>
                {!isChangingPartySize ? (
                  <button
                    type="button"
                    onClick={() => setIsChangingPartySize(true)}
                    className="btn btn-sm btn-ghost"
                    style={{ fontSize: '0.78rem', padding: '2px 8px', height: '24px', textDecoration: 'underline' }}
                  >
                    Change
                  </button>
                ) : (
                  <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                    {[1, 2, 3, 4, 5, 6].map((size) => (
                      <button
                        key={size}
                        type="button"
                        onClick={() => {
                          setPartySize(size);
                          setIsChangingPartySize(false);
                        }}
                        className={`btn btn-sm ${partySize === size ? 'btn-primary' : 'btn-outline'}`}
                        style={{ minWidth: '28px', height: '24px', padding: '0 4px', fontSize: '0.75rem' }}
                      >
                        {size}
                      </button>
                    ))}
                    <button
                      type="button"
                      onClick={() => setIsChangingPartySize(false)}
                      className="btn btn-sm btn-ghost"
                      style={{ fontSize: '0.75rem', padding: '2px 6px', height: '24px' }}
                    >
                      Done
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => setShowAiSuggestions(false)}
                className="btn btn-sm btn-ghost"
                style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}
              >
                Hide Suggestions
              </button>
            </div>
          </div>

          {aiSelectionError && (
            <div style={{
              marginTop: '12px',
              padding: '10px 14px',
              borderRadius: '6px',
              background: 'rgba(239, 68, 68, 0.15)',
              border: '1px solid #ef4444',
              color: '#fca5a5',
              fontSize: '0.85rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}>
              <AlertTriangle size={16} />
              <span>{aiSelectionError}</span>
            </div>
          )}

          {/* Recommendation Cards */}
          {recommendations && ((recommendations.recommendations || recommendations.options || []).length > 0) ? (
            <div style={{ marginTop: '16px', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
              {(recommendations.recommendations || recommendations.options || []).slice(0, 3).map((opt, idx) => {
                const optSeats = availability?.seats.filter(s => opt.seatIds.includes(s.seatId)) || [];
                const optPrice = opt.totalPrice ?? optSeats.reduce((sum, s) => sum + (s.price != null ? Number(s.price) : 200), 0);
                const isCurrentlySelected = opt.seatIds.every(id => selectedSeatIds.includes(id)) && opt.seatIds.length === selectedSeatIds.length;

                return (
                  <div
                    key={idx}
                    style={{
                      padding: '14px 18px',
                      background: 'var(--bg-elevated)',
                      borderRadius: 'var(--radius-md)',
                      border: isCurrentlySelected ? '1px solid var(--accent-gold)' : '1px solid var(--border-subtle)',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      gap: '12px',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Sparkles size={14} color="#f59e0b" />
                          <span style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff' }}>
                            {opt.title || (idx === 0 ? 'Best Seats' : 'Alternative View')}
                          </span>
                        </div>
                        <span className="badge badge-gold" style={{ fontSize: '0.72rem', padding: '2px 8px' }}>
                          {opt.averageScore}/100
                        </span>
                      </div>
                      <div style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--accent-gold)', marginBottom: '4px' }}>
                        {opt.seatLabels.join(' + ')}
                      </div>
                      <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                        {opt.rationale}
                      </p>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-subtle)', paddingTop: '10px' }}>
                      <div>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Total: </span>
                        <strong style={{ color: '#ffffff', fontSize: '0.95rem' }}>₹{optPrice}</strong>
                      </div>
                      <button
                        type="button"
                        onClick={() => handleApplyRecommendation(opt.seatIds)}
                        className={`btn btn-sm ${isCurrentlySelected ? 'btn-outline' : 'btn-primary'}`}
                        style={{ fontSize: '0.8rem', padding: '6px 14px', fontWeight: 700 }}
                        data-testid={`use-seats-btn-${idx}`}
                      >
                        {isCurrentlySelected ? 'Selected' : 'Use These Seats'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div style={{ marginTop: '14px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              No contiguous blocks found for party size {partySize}. You can select available seats directly from the seat map below.
            </div>
          )}
        </div>
      )}

        {/* Observable Interaction Friction Alert */}
        {interactionFriction && interactionFriction.detected && (
          <div style={{
            marginTop: '16px',
            padding: '12px 18px',
            borderRadius: '8px',
            background: 'rgba(245, 158, 11, 0.12)',
            border: '1px solid rgba(245, 158, 11, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <AlertTriangle size={18} color="#f59e0b" />
              <div>
                <span style={{ fontWeight: 600, fontSize: '0.88rem', color: '#fbbf24' }}>{interactionFriction.title}: </span>
                <span style={{ fontSize: '0.85rem', color: '#cbd5e1' }}>{interactionFriction.description}</span>
              </div>
            </div>
            {recommendations && (recommendations.recommendations || recommendations.options || []).length > 0 && (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => {
                  const rec = (recommendations.recommendations || recommendations.options || [])[0];
                  if (rec) handleApplyRecommendation(rec.seatIds);
                  setInteractionFriction(null);
                }}
                style={{ fontSize: '0.78rem', padding: '4px 12px', whiteSpace: 'nowrap' }}
              >
                Auto-Align Sweet Spot
              </button>
            )}
          </div>
        )}

        {/* Dynamic Decision Guidance & Friction Alerts */}
        {guidance && (
          <div style={{
            marginTop: '16px',
            padding: '14px 18px',
            background: 'var(--bg-elevated)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff' }}>
                Selected Seat Quality: <span style={{ color: 'var(--accent-gold)' }}>
                  {guidance.overallQualityScore ?? guidance.averageQualityScore ?? 90}/100 ({guidance.overallRating ?? guidance.tier ?? 'OPTIMAL'})
                </span>
              </span>
            </div>

            {/* Strengths */}
            {(guidance.keyStrengths || guidance.strengths || []).length > 0 && (
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                <CheckCircle size={14} color="#10b981" />
                <span style={{ fontSize: '0.82rem', color: '#a7f3d0' }}>
                  {(guidance.keyStrengths || guidance.strengths || []).join(' • ')}
                </span>
              </div>
            )}

            {/* Explainable Decision Factors per Seat */}
            {selectedSeatObjects.length > 0 && (
              <div style={{ marginTop: '4px', fontSize: '0.8rem', color: '#94a3b8', borderTop: '1px dashed var(--border-subtle)', paddingTop: '6px' }}>
                <div style={{ fontWeight: 600, color: '#cbd5e1', marginBottom: '2px', fontSize: '0.78rem' }}>Deterministic Scoring Breakdown:</div>
                {selectedSeatObjects.map((s) => {
                  const sc = seatScores[s.seatId];
                  if (!sc || !sc.reasons || sc.reasons.length === 0) return null;
                  return (
                    <div key={s.seatId} style={{ marginTop: '2px', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                      <strong style={{ color: 'var(--accent-gold)' }}>{s.rowLabel}{s.seatNumber} ({sc.score ?? sc.totalScore}/100 {sc.badge ?? sc.tier}):</strong>
                      <span>{sc.reasons.slice(0, 3).join(' • ')}</span>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Friction Alerts */}
            {((guidance.frictionAlerts && guidance.frictionAlerts.length > 0) || (guidance.frictions && guidance.frictions.length > 0)) && (
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                <AlertTriangle size={14} color="#f59e0b" />
                <span style={{ fontSize: '0.82rem', color: '#fde68a' }}>
                  {guidance.frictionAlerts ? guidance.frictionAlerts.map(f => f.message).join(' • ') : (guidance.frictions || []).join(' • ')}
                </span>
              </div>
            )}

            {/* One-Click Recovery Actions */}
            {((guidance.recoveryStrategies && guidance.recoveryStrategies.length > 0) || (guidance.recoveryActions && guidance.recoveryActions.length > 0)) && (
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center', marginTop: '4px' }}>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Smart Adjust:</span>
                {(guidance.recoveryStrategies || guidance.recoveryActions || []).map((act, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => handleApplyRecommendation(act.suggestedSeatIds)}
                    className="btn btn-sm btn-outline"
                    style={{ fontSize: '0.75rem', padding: '2px 8px', height: '26px' }}
                  >
                    {act.description}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

      {/* Seat Matrix Card */}
      <div className="card" style={{ padding: '36px 20px', minHeight: '400px' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
            Loading auditorium seat layout...
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#ef4444' }}>
            <p>{error}</p>
          </div>
        ) : availability && availability.seats ? (
          <SeatGrid
            seats={availability.seats}
            selectedSeatIds={selectedSeatIds}
            onToggleSeat={handleToggleSeat}
            isInteractive={!holdToken}
            isCustomerSelection={true}
            seatScores={seatScores}
          />
        ) : (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
            No seat inventory configured for this auditorium screen.
          </div>
        )}
      </div>

      {/* Bottom Sticky Action Bar when seats are selected */}
      {selectedSeatIds.length > 0 && (
        <div style={{
          position: 'sticky',
          bottom: '24px',
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-medium)',
          borderRadius: 'var(--radius-lg)',
          padding: '18px 28px',
          boxShadow: 'var(--shadow-xl)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px',
          zIndex: 100,
          animation: 'fadeIn 0.2s ease-out',
        }}>
          <div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Selected Seats ({selectedSeatObjects.length})
            </div>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '4px', flexWrap: 'wrap' }}>
              {selectedSeatObjects.map((s) => (
                <span key={s.seatId} className="badge badge-crimson" style={{ fontSize: '0.88rem', fontWeight: 700 }}>
                  {s.rowLabel}{s.seatNumber} {s.price ? `(₹${s.price})` : ''}
                </span>
              ))}
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginLeft: '8px' }}>
                Total: <strong style={{ color: 'var(--accent-gold)', fontSize: '1.05rem' }}>₹{holdTotalAmount || exactSelectedTotal}</strong>
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            {holdToken ? (
              <button
                type="button"
                onClick={() => setIsCheckoutOpen(true)}
                className="btn btn-primary"
                style={{ padding: '12px 28px', fontSize: '1rem', fontWeight: 800 }}
              >
                Resume Checkout ({formatTimer(holdSecondsLeft)})
              </button>
            ) : (
              <button
                type="button"
                disabled={isHolding || selectedSeatIds.length === 0}
                onClick={handleHoldSeats}
                className="btn btn-primary"
                style={{ padding: '12px 28px', fontSize: '1rem', fontWeight: 800, gap: '8px' }}
              >
                <ShieldCheck size={18} />
                {isHolding ? 'Holding Seats...' : 'Hold Seats & Proceed to Pay'}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Express Checkout Modal */}
      {show && (
        <CheckoutModal
          isOpen={isCheckoutOpen}
          onClose={() => setIsCheckoutOpen(false)}
          show={show}
          selectedSeats={selectedSeatObjects}
          holdToken={holdToken || ''}
          expiresInSeconds={holdSecondsLeft}
          totalAmount={holdTotalAmount || exactSelectedTotal}
          onPaymentSuccess={handlePaymentSuccess}
          onPaymentFailure={handlePaymentFailure}
        />
      )}
    </div>
  );
};
