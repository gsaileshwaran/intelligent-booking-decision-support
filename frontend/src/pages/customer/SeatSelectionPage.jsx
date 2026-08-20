import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { showService } from '../../services/showService';
import { bookingService } from '../../services/bookingService';
import { useBooking } from '../../context/BookingContext';
import { Ticket, ArrowLeft, Clock, AlertCircle, CheckCircle, Shield } from 'lucide-react';

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

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading auditorium layout and live seat map...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white mb-6 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

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
              <span className="block text-slate-500 text-[10px]">Base Ticket Price</span>
              <span className="font-bold text-emerald-400 text-sm">₹{show.ticketPrice?.toFixed(2)}</span>
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

      {/* Seat Map Area */}
      <div className="glass-card p-8 mb-8 overflow-x-auto">
        <div className="text-center text-xs text-slate-400 mb-6 font-semibold tracking-widest uppercase">
          Cinema Screen Direction
        </div>

        {/* Screen Visual */}
        <div className="cinema-screen" />

        {/* Category Multiplier Tier Indicator */}
        <div className="flex flex-wrap justify-center gap-3 mb-6 text-[11px] font-semibold">
          <span className="px-3 py-1 rounded-full bg-blue-500/10 text-blue-300 border border-blue-500/20">
            Regular (1.0x Base)
          </span>
          <span className="px-3 py-1 rounded-full bg-purple-500/10 text-purple-300 border border-purple-500/20">
            Premium (1.25x Base)
          </span>
          <span className="px-3 py-1 rounded-full bg-amber-500/10 text-amber-300 border border-amber-500/20">
            Balcony (1.50x Base)
          </span>
        </div>

        {/* Status Legend */}
        <div className="flex flex-wrap justify-center gap-6 mb-10 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-5 h-5 rounded bg-blue-500/20 border border-blue-500/40" />
            <span className="text-slate-300">Available</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-5 h-5 rounded bg-emerald-500 border border-emerald-400 shadow shadow-emerald-500/50" />
            <span className="text-slate-300">Selected</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-5 h-5 rounded bg-amber-500/20 border border-amber-500/50 opacity-70" />
            <span className="text-slate-400">Held</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-5 h-5 rounded bg-slate-800 border border-slate-700 opacity-50" />
            <span className="text-slate-500">Booked</span>
          </div>
        </div>

        {/* Seat Rows Matrix */}
        <div className="flex flex-col gap-3 max-w-2xl mx-auto min-w-[320px]">
          {Object.keys(seatsByRow).map((row) => (
            <div key={row} className="flex items-center justify-center gap-3">
              <span className="w-6 text-xs font-bold text-slate-500 text-center">{row}</span>
              <div className="flex items-center gap-2 flex-wrap justify-center">
                {seatsByRow[row].map((showSeat) => {
                  const isSelected = selectedSeatIds.includes(showSeat.showSeatId);
                  const isAvailable = showSeat.status === 'AVAILABLE';
                  const isHeld = showSeat.status === 'HELD';
                  const isBooked = showSeat.status === 'BOOKED' || showSeat.status === 'CONFIRMED';
                  const seatType = showSeat.seat?.seatType || 'REGULAR';

                  let statusClass = 'available';
                  if (isSelected) statusClass = 'selected';
                  else if (isHeld) statusClass = 'held';
                  else if (isBooked) statusClass = 'booked';

                  let tierColorClass = '';
                  if (isAvailable && !isSelected) {
                    if (seatType === 'PREMIUM') tierColorClass = 'border-purple-500/50 text-purple-300 bg-purple-500/10';
                    else if (seatType === 'BALCONY') tierColorClass = 'border-amber-500/50 text-amber-300 bg-amber-500/10';
                  }

                  return (
                    <button
                      key={showSeat.showSeatId}
                      disabled={!isAvailable}
                      onClick={() => handleSeatClick(showSeat)}
                      className={`seat-item ${statusClass} ${tierColorClass}`}
                      title={`Row ${showSeat.seat?.rowLabel} Seat ${showSeat.seat?.seatNumber} (${seatType}) - ₹${showSeat.price}`}
                    >
                      {showSeat.seat?.seatNumber}
                    </button>
                  );
                })}
              </div>
              <span className="w-6 text-xs font-bold text-slate-500 text-center">{row}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Selected Seats Summary Footer Bar */}
      <div className="glass-card p-6 flex flex-col sm:flex-row items-center justify-between gap-6 sticky bottom-6 z-40">
        <div>
          <span className="block text-xs text-slate-400">Selected Seats:</span>
          {selectedSeatsList.length === 0 ? (
            <span className="text-sm font-semibold text-slate-500">None selected</span>
          ) : (
            <div className="flex flex-wrap gap-2 mt-1">
              {selectedSeatsList.map((ss) => (
                <span
                  key={ss.showSeatId}
                  className="px-2.5 py-1 rounded-md bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-bold"
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
            <span className="text-xl font-extrabold text-white">₹{totalPrice.toFixed(2)}</span>
          </div>

          <button
            disabled={selectedSeatIds.length === 0 || holding}
            onClick={handleHoldReservation}
            className="btn-primary py-3 px-6 text-xs font-bold"
          >
            {holding ? 'Locking Seats...' : `Reserve Seats (${selectedSeatIds.length})`}
          </button>
        </div>
      </div>
    </div>
  );
};
