import React, { useState, useEffect } from 'react';
import { providerService } from '../../services/providerService';
import { Ticket, Search, Filter, Calendar, MapPin, CheckCircle2, AlertCircle, X, Eye, CreditCard } from 'lucide-react';

export const ProviderBookingsPage = () => {
  const [bookings, setBookings] = useState([]);
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [selectedBranchId, setSelectedBranchId] = useState('');
  const [selectedDateFilter, setSelectedDateFilter] = useState('ALL');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Selected Booking Modal
  const [selectedBooking, setSelectedBooking] = useState(null);

  useEffect(() => {
    fetchBranches();
  }, []);

  useEffect(() => {
    fetchBookings();
  }, [selectedBranchId, selectedDateFilter, selectedStatusFilter]);

  const fetchBranches = async () => {
    try {
      const res = await providerService.getMyTheatres();
      if (res.success && res.data) {
        setBranches(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch PVK branches:', err);
    }
  };

  const fetchBookings = async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedBranchId) params.branchId = selectedBranchId;
      if (selectedDateFilter !== 'ALL') params.date = selectedDateFilter;
      if (selectedStatusFilter !== 'ALL') params.status = selectedStatusFilter;

      const res = await providerService.getBookings(params);
      if (res.success && res.data) {
        setBookings(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch provider bookings:', err);
    } finally {
      setLoading(false);
    }
  };

  // Client-side search filtering
  const filteredBookings = bookings.filter((b) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      (b.bookingRef && b.bookingRef.toLowerCase().includes(q)) ||
      (b.customerName && b.customerName.toLowerCase().includes(q)) ||
      (b.customerEmail && b.customerEmail.toLowerCase().includes(q)) ||
      (b.movieTitle && b.movieTitle.toLowerCase().includes(q))
    );
  });

  const formatSeats = (seats) => {
    if (!seats || seats.length === 0) return 'None';
    return seats.map((s) => `${s.rowLabel}${s.seatNumber}`).join(', ');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">PVK Operator Portal</span>
          <h1 className="text-3xl font-extrabold text-white">Booking Management</h1>
          <p className="text-xs text-slate-400 mt-1">
            Monitor customer bookings, seat holds, confirmations, and simulated payment transactions across PVK branches
          </p>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="glass-card p-6 mb-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Search */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">Search</label>
          <div className="relative">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Ref, Name, or Email..."
              className="form-input pl-9 text-xs py-2 w-full"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {/* Branch Filter */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">PVK Branch</label>
          <select
            className="form-input text-xs py-2 w-full bg-slate-900 text-white"
            value={selectedBranchId}
            onChange={(e) => setSelectedBranchId(e.target.value)}
          >
            <option value="">All PVK Branches</option>
            {branches.map((b) => (
              <option key={b.theatreId} value={b.theatreId}>
                {b.name}
              </option>
            ))}
          </select>
        </div>

        {/* Date Filter */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">Show Date</label>
          <select
            className="form-input text-xs py-2 w-full bg-slate-900 text-white"
            value={selectedDateFilter}
            onChange={(e) => setSelectedDateFilter(e.target.value)}
          >
            <option value="ALL">All Dates</option>
            <option value="TODAY">Today's Shows</option>
            <option value="UPCOMING">Upcoming Shows</option>
            <option value="PAST">Past Shows</option>
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">Booking Status</label>
          <select
            className="form-input text-xs py-2 w-full bg-slate-900 text-white"
            value={selectedStatusFilter}
            onChange={(e) => setSelectedStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="HELD">HELD</option>
            <option value="PENDING">PENDING</option>
            <option value="CANCELLED">CANCELLED</option>
            <option value="EXPIRED">EXPIRED</option>
          </select>
        </div>
      </div>

      {/* Bookings Table */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading branch bookings...</div>
      ) : filteredBookings.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Ticket className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No Bookings Found</h3>
          <p className="text-xs text-slate-500 mt-1">Adjust your branch, date, or status filters to view records.</p>
        </div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="p-4">Reference</th>
                  <th className="p-4">Customer</th>
                  <th className="p-4">PVK Branch / Screen</th>
                  <th className="p-4">Movie & Showtime</th>
                  <th className="p-4">Seats</th>
                  <th className="p-4">Total (₹)</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredBookings.map((b) => (
                  <tr key={b.bookingId} className="hover:bg-slate-800/40 transition-colors">
                    <td className="p-4 font-bold text-indigo-300">{b.bookingRef}</td>
                    <td className="p-4">
                      <div className="font-semibold text-white">{b.customerName || `User #${b.userId}`}</div>
                      <div className="text-[10px] text-slate-500">{b.customerEmail}</div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-slate-200">{b.theatreName || 'PVK Branch'}</div>
                      <div className="text-[10px] text-slate-400">{b.screenName}</div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-slate-200">{b.movieTitle}</div>
                      <div className="text-[10px] text-indigo-300">
                        {b.showDate} {b.startTime ? `• ${b.startTime.substring(0, 5)}` : ''}
                      </div>
                    </td>
                    <td className="p-4 font-mono text-purple-300">
                      {formatSeats(b.seats)} ({b.seats?.length || 0})
                    </td>
                    <td className="p-4 font-bold text-emerald-400">₹{b.totalAmount?.toFixed(2)}</td>
                    <td className="p-4">
                      <span
                        className={`badge ${
                          b.status === 'CONFIRMED'
                            ? 'badge-confirmed'
                            : b.status === 'HELD' || b.status === 'PENDING'
                            ? 'badge-held'
                            : 'badge-expired'
                        }`}
                      >
                        {b.status}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => setSelectedBooking(b)}
                        className="btn-secondary text-[11px] py-1.5 px-3"
                      >
                        <Eye className="w-3.5 h-3.5" /> Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Booking Details Modal */}
      {selectedBooking && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-lg p-6 sm:p-8 animate-fade-in my-8 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
              <div>
                <span className="badge badge-confirmed mb-1">Booking Detail View</span>
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
              {/* Customer Info */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Customer Information</span>
                <div className="text-white font-bold">{selectedBooking.customerName || `User #${selectedBooking.userId}`}</div>
                <div className="text-slate-400">{selectedBooking.customerEmail}</div>
              </div>

              {/* Show & Venue Info */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-1">Venue & Showtime</span>
                <div className="text-white font-bold">{selectedBooking.theatreName}</div>
                <div className="text-indigo-300">{selectedBooking.screenName} &bull; {selectedBooking.movieTitle}</div>
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
                  <span className="text-white">Total Booking Amount</span>
                  <span className="text-emerald-400">₹{selectedBooking.totalAmount?.toFixed(2)}</span>
                </div>
              </div>

              {/* Payment Details */}
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-2 flex items-center gap-1">
                  <CreditCard className="w-3.5 h-3.5 text-indigo-400" /> Simulated Payment Record
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
                    <span className="font-bold text-emerald-400">{selectedBooking.paymentStatus || 'SUCCESS'}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[10px]">Created Timestamp</span>
                    <span className="text-slate-300">{selectedBooking.createdAt?.replace('T', ' ').substring(0, 19)}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
