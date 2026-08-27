import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { showService } from '../../services/showService';
import { bookingService } from '../../services/bookingService';
import { useBooking } from '../../context/BookingContext';
import { ArrowLeft, AlertCircle, Monitor, ShieldCheck, Clock, CreditCard } from 'lucide-react';

export const SeatSelectionPage = () => {
  const { id } = useParams(); // showId
  const navigate = useNavigate();
  const { setActiveBooking } = useBooking();

  const [show, setShow] = useState(null);
  const [showSeats, setShowSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [holding, setHolding] = useState(false);
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(600);

  useEffect(() => {
    fetchShowData();
  }, [id]);

  const fetchShowData = async () => {
    setLoading(true);
    setError('');
    try {
      const [showRes, seatsRes] = await Promise.all([
        showService.getShowById(id),
        showService.getShowSeats(id),
      ]);

      if (showRes.success) setShow(showRes.data);
      if (seatsRes.success) setShowSeats(seatsRes.data);
    } catch (err) {
      setError('Failed to load show seat map or showtime details.');
    } finally {
      setLoading(false);
    }
  };

  const myHeldSeats = showSeats.filter((ss) => ss.heldByCurrentUser === true);
  const existingBookingId = myHeldSeats.length > 0 ? myHeldSeats[0].bookingId : null;
  const serverHeldUntil = myHeldSeats.length > 0 ? myHeldSeats[0].heldUntil : null;
  const myHeldSeatLabels = myHeldSeats.map((s) => `${s.seat?.rowLabel || ''}${s.seat?.seatNumber || ''}`).join(', ');
  const myHeldTotalPrice = myHeldSeats.reduce((acc, ss) => acc + (ss.price || 0), 0);

  // Timer countdown for active hold
  useEffect(() => {
    if (myHeldSeats.length === 0 || !serverHeldUntil) return;

    const expiry = new Date(serverHeldUntil).getTime();
    const initialSeconds = Math.max(0, Math.floor((expiry - new Date().getTime()) / 1000));
    setTimeLeft(initialSeconds);

    const timer = setInterval(() => {
      const remaining = Math.max(0, Math.floor((expiry - new Date().getTime()) / 1000));
      setTimeLeft(remaining);
      if (remaining <= 0) {
        clearInterval(timer);
        fetchShowData();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [serverHeldUntil, myHeldSeats.length]);

  const handleSeatClick = (seat) => {
    if (seat.status !== 'AVAILABLE') return;

    setSelectedSeatIds((prev) => {
      if (prev.includes(seat.showSeatId)) {
        return prev.filter((id) => id !== seat.showSeatId);
      } else {
        return [...prev, seat.showSeatId];
      }
    });
  };

  const selectedSeatsList = showSeats.filter((ss) => selectedSeatIds.includes(ss.showSeatId));
  const totalPrice = selectedSeatsList.reduce((acc, ss) => acc + (ss.price || 0), 0);

  const handleHoldReservation = async () => {
    if (selectedSeatIds.length === 0) return;
    setHolding(true);
    setError('');
    try {
      const res = await bookingService.holdSeats(id, selectedSeatIds);
      if (res.success && res.data) {
        setActiveBooking(res.data);
        navigate(`/bookings/${res.data.bookingId}/confirm`);
      } else {
        setError(res.message || 'Seat hold reservation failed.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'One or more selected seats were locked by another user. Please reselect available seats.');
      fetchShowData();
    } finally {
      setHolding(false);
    }
  };

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  // Group seats by Row Label (A, B, C...)
  const seatsByRow = showSeats.reduce((acc, seat) => {
    const row = seat.seat?.rowLabel || 'A';
    if (!acc[row]) acc[row] = [];
    acc[row].push(seat);
    return acc;
  }, {});

  // Sort seats within each row by seat number
  Object.keys(seatsByRow).forEach((row) => {
    seatsByRow[row].sort((a, b) => (a.seat?.seatNumber || 0) - (b.seat?.seatNumber || 0));
  });

  // Group rows into 3 distinct sections: REGULAR, PREMIUM, BALCONY
  const sectionDefs = [
    {
      type: 'REGULAR',
      title: 'REGULAR — 1.0× BASE',
      multiplier: '1.0× Base Price',
      badgeClass: 'border-blue-500/40 text-blue-300 bg-blue-500/10',
      dividerClass: 'border-blue-500/30',
      rows: []
    },
    {
      type: 'PREMIUM',
      title: 'PREMIUM — 1.25× BASE',
      multiplier: '1.25× Base Price',
      badgeClass: 'border-purple-500/40 text-purple-300 bg-purple-500/10',
      dividerClass: 'border-purple-500/30',
      rows: []
    },
    {
      type: 'BALCONY',
      title: 'BALCONY — 1.50× BASE',
      multiplier: '1.50× Base Price',
      badgeClass: 'border-amber-500/40 text-amber-300 bg-amber-500/10',
      dividerClass: 'border-amber-500/30',
      rows: []
    }
  ];

  Object.keys(seatsByRow).forEach((row) => {
    const firstSeat = seatsByRow[row][0];
    const type = firstSeat?.seat?.seatType || 'REGULAR';
    const targetSection = sectionDefs.find((s) => s.type === type) || sectionDefs[0];
    targetSection.rows.push(row);
  });

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading auditorium layout and live seat map...</div>;
  }

  const getSeatTierStyle = (seatType, status, isSelected, heldByCurrentUser) => {
    if (heldByCurrentUser) {
      return 'bg-cyan-500/30 border-2 border-cyan-400 text-cyan-200 shadow-md shadow-cyan-500/40 animate-pulse font-extrabold cursor-pointer';
    }

    if (isSelected) {
      if (seatType === 'PREMIUM') return 'bg-emerald-500 border-2 border-purple-400 text-white shadow-lg shadow-emerald-500/50 scale-105';
      if (seatType === 'BALCONY') return 'bg-emerald-500 border-2 border-amber-400 text-white shadow-lg shadow-emerald-500/50 scale-105';
      return 'bg-emerald-500 border-2 border-blue-400 text-white shadow-lg shadow-emerald-500/50 scale-105';
    }

    if (status === 'HELD') {
      if (seatType === 'PREMIUM') return 'bg-amber-500/20 border-2 border-purple-500/40 text-amber-300 opacity-70 cursor-not-allowed';
      if (seatType === 'BALCONY') return 'bg-amber-500/20 border-2 border-amber-500/40 text-amber-300 opacity-70 cursor-not-allowed';
      return 'bg-amber-500/20 border-2 border-blue-500/40 text-amber-300 opacity-70 cursor-not-allowed';
    }

    if (status === 'BOOKED' || status === 'CONFIRMED') {
      if (seatType === 'PREMIUM') return 'bg-slate-900 border border-purple-900/60 text-slate-600 opacity-50 cursor-not-allowed';
      if (seatType === 'BALCONY') return 'bg-slate-900 border border-amber-900/60 text-slate-600 opacity-50 cursor-not-allowed';
      return 'bg-slate-900 border border-blue-900/60 text-slate-600 opacity-50 cursor-not-allowed';
    }

    // AVAILABLE states with tier identity
    if (seatType === 'PREMIUM') {
      return 'bg-purple-500/15 border border-purple-500/50 text-purple-200 hover:bg-purple-500/35 hover:scale-110 shadow-sm shadow-purple-500/20';
    }
    if (seatType === 'BALCONY') {
      return 'bg-amber-500/15 border border-amber-500/50 text-amber-200 hover:bg-amber-500/35 hover:scale-110 shadow-sm shadow-amber-500/20';
    }
    return 'bg-blue-500/15 border border-blue-500/50 text-blue-200 hover:bg-blue-500/35 hover:scale-110 shadow-sm shadow-blue-500/20';
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

      {/* Active Hold Continuation Banner */}
      {myHeldSeats.length > 0 && (
        <div className="mb-8 p-6 rounded-2xl bg-gradient-to-r from-cyan-950/80 via-slate-900/90 to-indigo-950/80 border border-cyan-500/50 shadow-2xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="flex items-center gap-4 text-cyan-300">
            <div className="w-12 h-12 rounded-xl bg-cyan-500/20 border border-cyan-400 flex items-center justify-center text-cyan-300 shrink-0">
              <ShieldCheck className="w-7 h-7" />
            </div>
            <div>
              <span className="badge bg-cyan-500/20 text-cyan-300 border-cyan-500/40 mb-1 font-bold">
                YOUR RESERVATION IN PROGRESS
              </span>
              <h2 className="text-lg font-extrabold text-white">
                You have an active hold on seat(s): <span className="text-cyan-300 font-mono">{myHeldSeatLabels}</span>
              </h2>
              <p className="text-xs text-slate-300 mt-1 flex items-center gap-2">
                <Clock className="w-3.5 h-3.5 text-amber-400" />
                Hold Expiry: <strong className="text-amber-400 font-mono">{formatTimer(timeLeft)}</strong>
              </p>
            </div>
          </div>

          <button
            onClick={() => navigate(`/bookings/${existingBookingId}/confirm`)}
            className="btn-primary bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 border-cyan-400 py-3.5 px-8 text-xs font-extrabold shadow-lg shadow-cyan-500/30 shrink-0 w-full md:w-auto"
          >
            CONTINUE TO PAYMENT (₹{myHeldTotalPrice.toFixed(2)})
          </button>
        </div>
      )}

      {/* Showtime Overview Bar */}
      {show && (
        <div className="glass-card p-6 mb-8 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider">
              {show.movie?.title}
            </span>
            <h1 className="text-2xl font-bold text-white mt-1">
              {show.screen?.theatre?.name} &bull; {show.screen?.name}
            </h1>
            <p className="text-xs text-slate-400 mt-1">
              {show.showDate} at <span className="text-indigo-300 font-semibold">{show.startTime}</span>
            </p>
          </div>

          <div className="flex items-center gap-4 text-xs">
            <div className="text-right">
              <span className="block text-slate-500 text-[10px] uppercase font-semibold">Base Ticket Price</span>
              <span className="font-extrabold text-emerald-400 text-base">₹{show.ticketPrice?.toFixed(2)}</span>
            </div>
          </div>
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-red-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Seat Map Main Auditorium Card */}
      <div className="glass-card p-8 mb-8 overflow-x-auto">
        {/* Screen Orientation Header */}
        <div className="flex flex-col items-center justify-center mb-6">
          <div className="flex items-center gap-2 text-xs font-extrabold text-indigo-300 tracking-widest uppercase mb-3">
            <Monitor className="w-4 h-4 text-indigo-400" /> SCREEN THIS WAY
          </div>
          {/* Screen Visual Curved Bar */}
          <div className="cinema-screen" />
        </div>

        {/* Explanatory Tier & Status Legends */}
        <div className="flex flex-col md:flex-row items-center justify-center gap-6 mb-8 p-4 rounded-xl bg-slate-900/60 border border-slate-800/80">
          {/* Category Tier Legend */}
          <div className="flex flex-wrap items-center justify-center gap-4 text-xs">
            <span className="text-slate-400 font-semibold text-[11px] uppercase">Seating Tiers:</span>
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-blue-500/10 border border-blue-500/30 text-blue-300 font-bold">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-400" />
              <span>REGULAR (1.0× Base)</span>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-purple-500/10 border border-purple-500/30 text-purple-300 font-bold">
              <span className="w-2.5 h-2.5 rounded-full bg-purple-400" />
              <span>PREMIUM (1.25× Base)</span>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-amber-500/10 border border-amber-500/30 text-amber-300 font-bold">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-400" />
              <span>BALCONY (1.50× Base)</span>
            </div>
          </div>

          <div className="hidden md:block w-px h-6 bg-slate-800" />

          {/* Status Legend */}
          <div className="flex flex-wrap items-center justify-center gap-5 text-xs">
            <span className="text-slate-400 font-semibold text-[11px] uppercase">Status:</span>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-slate-800 border border-blue-400/50" />
              <span className="text-slate-300">Available</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-cyan-500/40 border-2 border-cyan-400" />
              <span className="text-cyan-300 font-bold">YOUR HOLD</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-emerald-500 border border-emerald-400 shadow shadow-emerald-500/50" />
              <span className="text-slate-300">Selected</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-amber-500/20 border border-amber-500/50 opacity-70" />
              <span className="text-slate-400">Held (Other)</span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="w-4 h-4 rounded bg-slate-900 border border-slate-700 opacity-50" />
              <span className="text-slate-500">Booked</span>
            </div>
          </div>
        </div>

        {/* Physically Separated Seating Sections */}
        <div className="space-y-8 max-w-3xl mx-auto min-w-[320px]">
          {sectionDefs.map((section) => {
            if (section.rows.length === 0) return null;
            return (
              <div key={section.type} className="flex flex-col gap-4">
                {/* Section Divider Title */}
                <div className="relative flex items-center justify-center my-2">
                  <div className="absolute inset-0 flex items-center" aria-hidden="true">
                    <div className={`w-full border-t ${section.dividerClass}`} />
                  </div>
                  <div className="relative flex justify-center">
                    <span className={`px-4 py-1 rounded-full text-xs font-extrabold uppercase tracking-widest border backdrop-blur-md ${section.badgeClass}`}>
                      ───── {section.title} ─────
                    </span>
                  </div>
                </div>

                {/* Section Rows */}
                <div className="flex flex-col gap-3">
                  {section.rows.map((row) => (
                    <div key={row} className="flex items-center justify-center gap-3">
                      <span className="w-6 text-xs font-bold text-slate-400 text-center">{row}</span>
                      <div className="flex items-center gap-2 flex-wrap justify-center">
                        {seatsByRow[row].map((showSeat) => {
                          const isSelected = selectedSeatIds.includes(showSeat.showSeatId);
                          const isAvailable = showSeat.status === 'AVAILABLE';
                          const isHeld = showSeat.status === 'HELD';
                          const isHeldByCurrent = showSeat.heldByCurrentUser === true;
                          const isBooked = showSeat.status === 'BOOKED' || showSeat.status === 'CONFIRMED';
                          const seatType = showSeat.seat?.seatType || section.type;

                          const styleClass = getSeatTierStyle(seatType, showSeat.status, isSelected, isHeldByCurrent);

                          return (
                            <button
                              key={showSeat.showSeatId}
                              disabled={!isAvailable && !isHeldByCurrent}
                              onClick={() => {
                                if (isHeldByCurrent && existingBookingId) {
                                  navigate(`/bookings/${existingBookingId}/confirm`);
                                } else {
                                  handleSeatClick(showSeat);
                                }
                              }}
                              className={`seat-item ${styleClass}`}
                              title={
                                isHeldByCurrent
                                  ? `Row ${showSeat.seat?.rowLabel} Seat ${showSeat.seat?.seatNumber} (YOUR HOLD - Reserved by You)`
                                  : `Row ${showSeat.seat?.rowLabel} Seat ${showSeat.seat?.seatNumber} (${seatType}) - ₹${showSeat.price}`
                              }
                            >
                              {showSeat.seat?.seatNumber}
                            </button>
                          );
                        })}
                      </div>
                      <span className="w-6 text-xs font-bold text-slate-400 text-center">{row}</span>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Selected Seats Summary Footer Bar */}
      <div className="glass-card p-6 flex flex-col sm:flex-row items-center justify-between gap-6 sticky bottom-6 z-40">
        <div>
          <span className="block text-xs text-slate-400">Selected Seats:</span>
          {selectedSeatsList.length === 0 ? (
            myHeldSeats.length > 0 ? (
              <span className="text-sm font-semibold text-cyan-300 font-mono">
                {myHeldSeatLabels} (Your Active Hold)
              </span>
            ) : (
              <span className="text-sm font-semibold text-slate-500">None selected</span>
            )
          ) : (
            <div className="flex flex-wrap gap-2 mt-1">
              {selectedSeatsList.map((ss) => (
                <span
                  key={ss.showSeatId}
                  className={`px-2.5 py-1 rounded-md text-xs font-bold border ${
                    ss.seat?.seatType === 'PREMIUM'
                      ? 'bg-purple-500/20 border-purple-500/40 text-purple-300'
                      : ss.seat?.seatType === 'BALCONY'
                      ? 'bg-amber-500/20 border-amber-500/40 text-amber-300'
                      : 'bg-blue-500/20 border-blue-500/40 text-blue-300'
                  }`}
                >
                  {ss.seat?.rowLabel}{ss.seat?.seatNumber} ({ss.seat?.seatType}) - ₹{ss.price}
                </span>
              ))}
            </div>
          )}
        </div>

        <div className="flex items-center gap-6 w-full sm:w-auto justify-between sm:justify-end border-t sm:border-t-0 border-slate-800 pt-4 sm:pt-0">
          <div className="text-right">
            <span className="block text-[10px] text-slate-400 uppercase font-semibold">Total Price</span>
            <span className="text-xl font-extrabold text-white">
              ₹{(selectedSeatsList.length > 0 ? totalPrice : myHeldTotalPrice).toFixed(2)}
            </span>
          </div>

          {myHeldSeats.length > 0 ? (
            <button
              onClick={() => navigate(`/bookings/${existingBookingId}/confirm`)}
              className="btn-primary bg-gradient-to-r from-cyan-500 to-indigo-600 border-cyan-400 py-3 px-6 text-xs font-extrabold"
            >
              CONTINUE TO PAYMENT
            </button>
          ) : (
            <button
              disabled={selectedSeatIds.length === 0 || holding}
              onClick={handleHoldReservation}
              className="btn-primary py-3 px-6 text-xs font-bold"
            >
              {holding ? 'Locking Seats...' : `Reserve Seats (${selectedSeatIds.length})`}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
