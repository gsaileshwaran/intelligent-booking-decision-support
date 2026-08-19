import React, { useState, useEffect } from 'react';
import { bookingService } from '../../services/bookingService';
import { Ticket, Calendar, MapPin, AlertCircle, XCircle } from 'lucide-react';

export const BookingHistoryPage = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const res = await bookingService.getMyHistory();
      if (res.success && res.data) {
        setBookings(res.data);
      }
    } catch (err) {
      console.error('Failed to load booking history:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this booking reservation?')) return;
    setCancellingId(bookingId);
    setMessage('');
    try {
      const res = await bookingService.cancelBooking(bookingId);
      if (res.success) {
        setMessage('Booking cancelled successfully and seats released.');
        fetchHistory();
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Cancellation failed.');
    } finally {
      setCancellingId(null);
    }
  };

  const getBadgeClass = (status) => {
    switch (status) {
      case 'CONFIRMED': return 'badge-confirmed';
      case 'HELD': return 'badge-held';
      case 'CANCELLED': return 'badge-cancelled';
      case 'EXPIRED': return 'badge-expired';
      default: return 'badge-available';
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold text-white">My Booking History</h1>
        <p className="text-xs text-slate-400 mt-1">View your movie reservations, seat allocations, and booking statuses</p>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
          {message}
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading your reservation history...</div>
      ) : bookings.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Ticket className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No Bookings Found</h3>
          <p className="text-xs text-slate-500 mt-1">You haven't made any movie ticket reservations yet.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((b) => (
            <div key={b.bookingId} className="glass-card p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <span className={`badge ${getBadgeClass(b.status)}`}>{b.status}</span>
                  <span className="text-xs font-mono text-indigo-300 font-bold">{b.bookingRef}</span>
                </div>

                <h3 className="text-xl font-bold text-white mb-1">
                  {b.movieTitle || 'Movie Reservation'}
                </h3>

                <div className="flex flex-wrap gap-4 text-xs text-slate-400 mt-2">
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-slate-500" /> {b.theatreName} ({b.screenName})
                  </span>
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-slate-500" /> Booked: {new Date(b.createdAt).toLocaleDateString()}
                  </span>
                </div>

                <div className="mt-3 flex items-center gap-2">
                  <span className="text-xs text-slate-500">Seats:</span>
                  {b.seats?.map((s) => (
                    <span key={s.showSeatId} className="px-2 py-0.5 rounded bg-slate-800 text-indigo-300 text-xs font-bold border border-slate-700">
                      {s.rowLabel}{s.seatNumber}
                    </span>
                  ))}
                </div>
              </div>

              <div className="flex flex-col items-end gap-3 shrink-0 w-full md:w-auto border-t md:border-t-0 border-slate-800 pt-4 md:pt-0">
                <div className="text-right">
                  <span className="block text-[10px] text-slate-500 uppercase font-semibold">Total Amount</span>
                  <span className="text-lg font-extrabold text-emerald-400">${b.totalAmount?.toFixed(2)}</span>
                </div>

                {(b.status === 'CONFIRMED' || b.status === 'HELD') && (
                  <button
                    disabled={cancellingId === b.bookingId}
                    onClick={() => handleCancelBooking(b.bookingId)}
                    className="btn-danger text-xs px-4 py-2 flex items-center gap-1.5"
                  >
                    <XCircle className="w-4 h-4" />
                    {cancellingId === b.bookingId ? 'Cancelling...' : 'Cancel Booking'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
