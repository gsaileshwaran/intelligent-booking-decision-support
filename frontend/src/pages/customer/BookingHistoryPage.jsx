import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookingService } from '../../services/bookingService';
import { Ticket, Calendar, MapPin, Search, Filter, Eye, XCircle, AlertTriangle, CreditCard, ShieldCheck, X } from 'lucide-react';

export const BookingHistoryPage = () => {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filters & Search
  const [selectedFilter, setSelectedFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Selected Booking Modal State
  const [selectedBooking, setSelectedBooking] = useState(null);

  // Cancellation Modal State
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [cancellingSubmitting, setCancellingSubmitting] = useState(false);
  const [cancelError, setCancelError] = useState('');

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

  const handleCancelBooking = async () => {
    if (!cancellingBooking) return;
    setCancellingSubmitting(true);
    setCancelError('');
    try {
      const res = await bookingService.cancelBooking(cancellingBooking.bookingId);
      if (res.success) {
        setMessage(`Booking ${cancellingBooking.bookingRef} cancelled. Simulated refund of ₹${cancellingBooking.totalAmount?.toFixed(2)} processed.`);
        setCancellingBooking(null);
        setSelectedBooking(null);
        fetchHistory();
      }
    } catch (err) {
      setCancelError(err.response?.data?.message || err.message || 'Cancellation failed.');
    } finally {
      setCancellingSubmitting(false);
    }
  };

  const isHeldActive = (b) => {
    if (b.status !== 'HELD' && b.status !== 'PENDING') return false;
    if (b.holdExpiresAt) {
      return new Date(b.holdExpiresAt).getTime() > new Date().getTime();
    }
    if (b.createdAt) {
      return (new Date(b.createdAt).getTime() + 600000) > new Date().getTime();
    }
    return true;
  };

  const formatSeats = (seats) => {
    if (!seats || seats.length === 0) return 'None';
    return seats.map((s) => `${s.rowLabel}${s.seatNumber}`).join(', ');
  };

  const today = new Date().toISOString().split('T')[0];

  const filteredBookings = bookings.filter((b) => {
    // Search query filter
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const matchRef = b.bookingRef && b.bookingRef.toLowerCase().includes(q);
      const matchMovie = b.movieTitle && b.movieTitle.toLowerCase().includes(q);
      const matchBranch = b.theatreName && b.theatreName.toLowerCase().includes(q);
      if (!matchRef && !matchMovie && !matchBranch) return false;
    }

    // Category filter
    if (selectedFilter === 'UPCOMING') {
      return b.status === 'CONFIRMED' && b.showDate && b.showDate >= today;
    }
    if (selectedFilter === 'PAST') {
      return b.status === 'CONFIRMED' && b.showDate && b.showDate < today;
    }
    if (selectedFilter === 'CANCELLED') {
      return b.status === 'CANCELLED' || b.status === 'EXPIRED';
    }

    return true;
  });

  const getBadgeClass = (status) => {
    switch (status) {
      case 'CONFIRMED': return 'badge-confirmed';
      case 'HELD': return 'badge-held';
      case 'PENDING': return 'badge-held';
      case 'CANCELLED': return 'badge-cancelled';
      case 'EXPIRED': return 'badge-expired';
      default: return 'badge-available';
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">Customer Account</span>
          <h1 className="text-3xl font-extrabold text-white">My Booking History</h1>
          <p className="text-xs text-slate-400 mt-1">Manage your movie reservations, seat allocations, payment statuses, and refunds</p>
        </div>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center justify-between">
          <span>{message}</span>
          <button onClick={() => setMessage('')} className="text-emerald-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Filter & Search Bar */}
      <div className="glass-card p-4 mb-8 flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Search */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search ref, movie, or branch..."
            className="form-input pl-9 text-xs py-2 w-full"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Filter Pills */}
        <div className="flex flex-wrap gap-2 w-full sm:w-auto">
          {['ALL', 'UPCOMING', 'PAST', 'CANCELLED'].map((filter) => (
            <button
              key={filter}
              onClick={() => setSelectedFilter(filter)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                selectedFilter === filter
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/20'
                  : 'bg-slate-900/60 text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              {filter === 'ALL' ? 'All Bookings' : filter.charAt(0) + filter.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Bookings List */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading your reservation history...</div>
      ) : filteredBookings.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Ticket className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No Bookings Found</h3>
          <p className="text-xs text-slate-500 mt-1">No ticket reservations match your current search or filter criteria.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredBookings.map((b) => (
            <div key={b.bookingId} className="glass-card p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-6 border border-slate-800 hover:border-slate-700 transition-all">
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
                    <MapPin className="w-3.5 h-3.5 text-slate-500" /> {b.theatreName || 'PVK Branch'} ({b.screenName})
                  </span>
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-slate-500" /> Show: {b.showDate || 'TBD'} ({b.startTime ? b.startTime.substring(0, 5) : 'TBD'})
                  </span>
                </div>

                <div className="mt-3 flex items-center gap-2">
                  <span className="text-xs text-slate-500">Seats ({b.seats?.length || 0}):</span>
                  <span className="font-mono font-bold text-purple-300 text-xs">{formatSeats(b.seats)}</span>
                </div>
              </div>

              <div className="flex flex-col items-end gap-3 shrink-0 w-full md:w-auto border-t md:border-t-0 border-slate-800 pt-4 md:pt-0">
                <div className="text-right">
                  <span className="block text-[10px] text-slate-500 uppercase font-semibold">Total Amount</span>
                  <span className="text-lg font-extrabold text-emerald-400">₹{b.totalAmount?.toFixed(2)}</span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setSelectedBooking(b)}
                    className="btn-secondary text-xs px-3 py-1.5"
                  >
                    <Eye className="w-3.5 h-3.5" /> Details
                  </button>

                  {b.status === 'CONFIRMED' && (
                    <button
                      onClick={() => navigate(`/tickets/${b.bookingId}`)}
                      className="btn-primary bg-indigo-600 hover:bg-indigo-500 text-xs px-3 py-1.5 flex items-center gap-1"
                    >
                      <Ticket className="w-3.5 h-3.5" /> Digital Ticket
                    </button>
                  )}

                  {(b.status === 'HELD' || b.status === 'PENDING') && isHeldActive(b) && (
                    <button
                      onClick={() => navigate(`/bookings/${b.bookingId}/confirm`)}
                      className="btn-primary text-xs px-3.5 py-1.5 font-bold shadow-lg shadow-indigo-500/20"
                    >
                      <CreditCard className="w-3.5 h-3.5" /> Resume & Pay
                    </button>
                  )}

                  {b.status === 'CONFIRMED' && b.showDate >= today && (
                    <button
                      onClick={() => {
                        setCancellingBooking(b);
                        setCancelError('');
                      }}
                      className="btn-secondary text-xs text-red-400 hover:text-red-300 hover:bg-red-500/10 border-red-500/30 px-3 py-1.5"
                    >
                      <XCircle className="w-3.5 h-3.5" /> Cancel
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Booking Details Modal */}
      {selectedBooking && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 sm:p-8 animate-fade-in my-8 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
              <div>
                <span className="badge badge-confirmed mb-1">Booking Confirmation Detail</span>
                <h2 className="text-xl font-bold text-indigo-300">{selectedBooking.bookingRef}</h2>
              </div>
              <button
                onClick={() => setSelectedBooking(null)}
                className="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 text-xs">
              {/* Show Info */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Movie & Showtime</span>
                <div className="text-white font-bold text-sm">{selectedBooking.movieTitle}</div>
                <div className="text-indigo-300 font-semibold">{selectedBooking.theatreName} &bull; {selectedBooking.screenName}</div>
                <div className="text-slate-400 mt-1">
                  Show Date: {selectedBooking.showDate} | Time: {selectedBooking.startTime?.substring(0, 5)}
                </div>
              </div>

              {/* Seats Breakdown */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-2">Reserved Seat Breakdown</span>
                <div className="space-y-2">
                  {selectedBooking.seats?.map((st, idx) => (
                    <div key={idx} className="flex justify-between items-center bg-slate-800/80 p-2 rounded-lg">
                      <span className="font-bold text-purple-300">Seat {st.rowLabel}{st.seatNumber} ({st.seatType})</span>
                      <span className="font-semibold text-emerald-400">₹{st.price?.toFixed(2)}</span>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between items-center mt-3 pt-3 border-t border-slate-800 text-sm font-bold">
                  <span className="text-white">Total Amount</span>
                  <span className="text-emerald-400">₹{selectedBooking.totalAmount?.toFixed(2)}</span>
                </div>
              </div>

              {/* Payment Details */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-2 flex items-center gap-1">
                  <CreditCard className="w-3.5 h-3.5 text-indigo-400" /> Simulated Payment Info
                </span>
                <div className="grid grid-cols-2 gap-2 text-[11px]">
                  <div>
                    <span className="text-slate-500 block text-[10px]">Payment Method</span>
                    <span className="font-semibold text-slate-200">{selectedBooking.paymentMethod || 'MOCK_CARD'}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Transaction Ref</span>
                    <span className="font-mono text-indigo-300">{selectedBooking.transactionRef || 'N/A'}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Payment Status</span>
                    <span className="font-bold text-emerald-400">{selectedBooking.paymentStatus || selectedBooking.status}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Booking Date</span>
                    <span className="text-slate-300">{selectedBooking.createdAt?.replace('T', ' ').substring(0, 10)}</span>
                  </div>
                </div>

                {selectedBooking.status === 'CANCELLED' && (
                  <div className="mt-3 pt-3 border-t border-slate-800 bg-red-500/10 p-3 rounded-lg border border-red-500/20 text-red-300">
                    <span className="block font-bold text-[11px] mb-1">Refund Simulated</span>
                    <div className="flex justify-between text-[10px]">
                      <span>Refund Reference:</span>
                      <span className="font-mono font-bold">{selectedBooking.refundRef || 'REFUND-MOCK'}</span>
                    </div>
                    <div className="flex justify-between text-[10px] mt-0.5">
                      <span>Refunded Amount:</span>
                      <span className="font-bold text-emerald-400">₹{selectedBooking.refundedAmount?.toFixed(2) || selectedBooking.totalAmount?.toFixed(2)}</span>
                    </div>
                  </div>
                )}
              </div>

              {selectedBooking.status === 'CONFIRMED' && (
                <button
                  onClick={() => navigate(`/tickets/${selectedBooking.bookingId}`)}
                  className="btn-primary w-full py-2.5 text-xs font-bold"
                >
                  <Ticket className="w-4 h-4" /> Open Digital E-Ticket
                </button>
              )}

              {(selectedBooking.status === 'HELD' || selectedBooking.status === 'PENDING') && isHeldActive(selectedBooking) && (
                <button
                  onClick={() => navigate(`/bookings/${selectedBooking.bookingId}/confirm`)}
                  className="btn-primary w-full py-2.5 text-xs font-bold"
                >
                  <CreditCard className="w-4 h-4" /> Resume & Complete Payment
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {cancellingBooking && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 animate-fade-in border-red-500/30">
            <div className="flex items-center gap-3 text-red-400 mb-4">
              <div className="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center border border-red-500/30">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">Cancel Ticket Booking</h3>
                <p className="text-xs text-slate-400">{cancellingBooking.bookingRef}</p>
              </div>
            </div>

            <div className="mb-4 p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-xs">
              <strong>Simulated Refund Notice:</strong> Cancelling this booking will release your reserved seats ({formatSeats(cancellingBooking.seats)}) and generate a simulated refund reference for ₹{cancellingBooking.totalAmount?.toFixed(2)}.
            </div>

            {cancelError && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
                {cancelError}
              </div>
            )}

            <p className="text-xs text-slate-300 mb-6">
              Are you sure you want to cancel your reservation for <strong>{cancellingBooking.movieTitle}</strong> at <strong>{cancellingBooking.theatreName}</strong> on {cancellingBooking.showDate}?
            </p>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setCancellingBooking(null)}
                className="btn-secondary w-full py-2.5 text-xs"
              >
                Keep Booking
              </button>
              <button
                type="button"
                disabled={cancellingSubmitting}
                onClick={handleCancelBooking}
                className="btn-primary bg-red-600 hover:bg-red-500 border-red-500 w-full py-2.5 text-xs"
              >
                {cancellingSubmitting ? 'Cancelling...' : 'Confirm Cancellation'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
