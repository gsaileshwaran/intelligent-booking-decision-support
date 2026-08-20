import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { providerService } from '../../services/providerService';
import { movieService } from '../../services/movieService';
import { Calendar, Search, PlusCircle, Edit3, XCircle, AlertTriangle, X, CheckCircle2, Building2, Film, Tv, DollarSign } from 'lucide-react';

export const ProviderShowsPage = () => {
  const [shows, setShows] = useState([]);
  const [branches, setBranches] = useState([]);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [selectedBranchId, setSelectedBranchId] = useState('');
  const [selectedMovieId, setSelectedMovieId] = useState('');
  const [selectedDateFilter, setSelectedDateFilter] = useState('ALL');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals State
  const [editingShow, setEditingShow] = useState(null);
  const [editDate, setEditDate] = useState('');
  const [editStartTime, setEditStartTime] = useState('');
  const [editEndTime, setEditEndTime] = useState('');
  const [editPrice, setEditPrice] = useState('');
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editError, setEditError] = useState('');

  const [cancellingShow, setCancellingShow] = useState(null);
  const [cancelSubmitting, setCancelSubmitting] = useState(false);
  const [cancelError, setCancelError] = useState('');

  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    fetchMetaData();
  }, []);

  useEffect(() => {
    fetchShows();
  }, [selectedBranchId, selectedMovieId, selectedDateFilter, selectedStatusFilter, searchQuery]);

  const fetchMetaData = async () => {
    try {
      const [branchesRes, moviesRes] = await Promise.all([
        providerService.getMyTheatres(),
        movieService.getAllMovies(),
      ]);

      if (branchesRes.success) setBranches(branchesRes.data || []);
      if (moviesRes.success) setMovies(moviesRes.data || []);
    } catch (err) {
      console.error('Failed to load metadata:', err);
    }
  };

  const fetchShows = async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedBranchId) params.branchId = selectedBranchId;
      if (selectedMovieId) params.movieId = selectedMovieId;
      if (selectedDateFilter !== 'ALL') params.date = selectedDateFilter;
      if (selectedStatusFilter !== 'ALL') params.status = selectedStatusFilter;
      if (searchQuery) params.q = searchQuery;

      const res = await providerService.getShows(params);
      if (res.success && res.data) {
        setShows(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch provider shows:', err);
    } finally {
      setLoading(false);
    }
  };

  const openEditModal = (show) => {
    setEditingShow(show);
    setEditDate(show.showDate || '');
    setEditStartTime(show.startTime ? show.startTime.substring(0, 5) : '');
    setEditEndTime(show.endTime ? show.endTime.substring(0, 5) : '');
    setEditPrice(show.ticketPrice ? show.ticketPrice.toString() : '');
    setEditError('');
  };

  const handleUpdateShow = async (e) => {
    e.preventDefault();
    setEditSubmitting(true);
    setEditError('');

    try {
      const payload = {
        showDate: editDate,
        startTime: editStartTime.length === 5 ? `${editStartTime}:00` : editStartTime,
        endTime: editEndTime.length === 5 ? `${editEndTime}:00` : editEndTime,
        ticketPrice: parseFloat(editPrice),
      };

      const res = await providerService.updateShow(editingShow.showId, payload);
      if (res.success) {
        setSuccessMessage('Show session updated successfully!');
        setEditingShow(null);
        fetchShows();
      }
    } catch (err) {
      setEditError(err.response?.data?.message || err.message || 'Failed to update show session.');
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleCancelShow = async () => {
    if (!cancellingShow) return;
    setCancelSubmitting(true);
    setCancelError('');

    try {
      const res = await providerService.cancelShow(cancellingShow.showId);
      if (res.success) {
        setSuccessMessage(`Show for "${cancellingShow.movieTitle}" cancelled successfully.`);
        setCancellingShow(null);
        fetchShows();
      }
    } catch (err) {
      setCancelError(err.response?.data?.message || err.message || 'Failed to cancel show.');
    } finally {
      setCancelSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">PVK Operator Portal</span>
          <h1 className="text-3xl font-extrabold text-white">Show Management</h1>
          <p className="text-xs text-slate-400 mt-1">
            Manage movie showtimes, auditorium schedules, ticket pricing (₹), and session status
          </p>
        </div>

        <Link to="/provider/shows/new" className="btn-primary text-xs">
          <PlusCircle className="w-4 h-4" /> Schedule New Show
        </Link>
      </div>

      {successMessage && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center justify-between">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage('')} className="text-emerald-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="glass-card p-6 mb-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {/* Search */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">Search</label>
          <div className="relative">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Movie Title or Branch..."
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

        {/* Movie Filter */}
        <div>
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">Movie Filter</label>
          <select
            className="form-input text-xs py-2 w-full bg-slate-900 text-white"
            value={selectedMovieId}
            onChange={(e) => setSelectedMovieId(e.target.value)}
          >
            <option value="">All Movies</option>
            {movies.map((m) => (
              <option key={m.movieId} value={m.movieId}>
                {m.title}
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
          <label className="text-[11px] font-semibold text-slate-400 uppercase block mb-1.5">Show Status</label>
          <select
            className="form-input text-xs py-2 w-full bg-slate-900 text-white"
            value={selectedStatusFilter}
            onChange={(e) => setSelectedStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </div>
      </div>

      {/* Shows Table */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading scheduled show sessions...</div>
      ) : shows.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Calendar className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No Shows Found</h3>
          <p className="text-xs text-slate-500 mt-1">Try adjusting your filters or click "Schedule New Show".</p>
        </div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="p-4">Movie</th>
                  <th className="p-4">Branch & Screen</th>
                  <th className="p-4">Show Date & Time</th>
                  <th className="p-4">Ticket Price</th>
                  <th className="p-4">Seating Availability</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {shows.map((s) => (
                  <tr key={s.showId} className="hover:bg-slate-800/40 transition-colors">
                    <td className="p-4">
                      <div className="font-bold text-white text-sm">{s.movieTitle}</div>
                      <div className="text-[10px] text-slate-400">{s.movieGenre} &bull; {s.movieDuration} mins</div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-indigo-300">{s.theatreName}</div>
                      <div className="text-[10px] text-slate-400">{s.screenName}</div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-slate-200">{s.showDate}</div>
                      <div className="text-[10px] text-emerald-400">
                        {s.startTime?.substring(0, 5)} - {s.endTime?.substring(0, 5)}
                      </div>
                    </td>
                    <td className="p-4 font-bold text-emerald-400 text-sm">
                      ₹{s.ticketPrice?.toFixed(2)}
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-slate-300">
                        {s.availableSeats} / {s.totalCapacity} Available
                      </div>
                      <div className="text-[10px] text-slate-500">
                        {s.confirmedSeats} Confirmed &bull; {s.heldSeats} Held
                      </div>
                    </td>
                    <td className="p-4">
                      <span className={`badge ${s.status === 'ACTIVE' ? 'badge-confirmed' : 'badge-expired'}`}>
                        {s.status}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {s.status === 'ACTIVE' && (
                          <>
                            <button
                              onClick={() => openEditModal(s)}
                              className="btn-secondary text-[11px] py-1 px-2.5"
                              title="Edit Show Session"
                            >
                              <Edit3 className="w-3.5 h-3.5" /> Edit
                            </button>
                            <button
                              onClick={() => {
                                setCancellingShow(s);
                                setCancelError('');
                              }}
                              className="p-1.5 text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                              title="Cancel Show"
                            >
                              <XCircle className="w-4 h-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Edit Show Modal */}
      {editingShow && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 animate-fade-in">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-4">
              <div>
                <span className="badge badge-confirmed mb-1">Edit Show Session</span>
                <h3 className="text-lg font-bold text-white">{editingShow.movieTitle}</h3>
                <p className="text-xs text-slate-400">{editingShow.theatreName} &bull; {editingShow.screenName}</p>
              </div>
              <button
                onClick={() => setEditingShow(null)}
                className="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {editingShow.hasConfirmedBookings && (
              <div className="mb-4 p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <div>
                  <strong>Notice:</strong> This show has active customer bookings. Date and start time cannot be changed, but price can be updated.
                </div>
              </div>
            )}

            {editError && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
                {editError}
              </div>
            )}

            <form onSubmit={handleUpdateShow} className="space-y-4 text-xs">
              <div>
                <label className="form-label">Show Date</label>
                <input
                  type="date"
                  required
                  disabled={editingShow.hasConfirmedBookings}
                  className="form-input text-xs"
                  value={editDate}
                  onChange={(e) => setEditDate(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="form-label">Start Time</label>
                  <input
                    type="time"
                    required
                    disabled={editingShow.hasConfirmedBookings}
                    className="form-input text-xs"
                    value={editStartTime}
                    onChange={(e) => setEditStartTime(e.target.value)}
                  />
                </div>
                <div>
                  <label className="form-label">End Time</label>
                  <input
                    type="time"
                    required
                    className="form-input text-xs"
                    value={editEndTime}
                    onChange={(e) => setEditEndTime(e.target.value)}
                  />
                </div>
              </div>

              <div>
                <label className="form-label">Base Ticket Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  min="1"
                  required
                  placeholder="220.00"
                  className="form-input text-xs"
                  value={editPrice}
                  onChange={(e) => setEditPrice(e.target.value)}
                />
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setEditingShow(null)}
                  className="btn-secondary w-full py-2.5 text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={editSubmitting}
                  className="btn-primary w-full py-2.5 text-xs"
                >
                  {editSubmitting ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Show Confirmation Modal */}
      {cancellingShow && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 animate-fade-in border-red-500/30">
            <div className="flex items-center gap-3 text-red-400 mb-4">
              <div className="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center border border-red-500/30">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">Cancel Show Session</h3>
                <p className="text-xs text-slate-400">{cancellingShow.movieTitle}</p>
              </div>
            </div>

            {cancellingShow.hasConfirmedBookings && (
              <div className="mb-4 p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs">
                <strong>Warning:</strong> This show session has {cancellingShow.confirmedSeats} confirmed customer seat bookings. Cancelling will stop new bookings and seat holds. Historical customer booking records will be preserved.
              </div>
            )}

            {cancelError && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
                {cancelError}
              </div>
            )}

            <p className="text-xs text-slate-300 mb-6">
              Are you sure you want to cancel the show on <strong>{cancellingShow.showDate}</strong> ({cancellingShow.startTime?.substring(0, 5)}) at <strong>{cancellingShow.theatreName} - {cancellingShow.screenName}</strong>?
            </p>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setCancellingShow(null)}
                className="btn-secondary w-full py-2.5 text-xs"
              >
                Keep Active
              </button>
              <button
                type="button"
                disabled={cancelSubmitting}
                onClick={handleCancelShow}
                className="btn-primary bg-red-600 hover:bg-red-500 border-red-500 w-full py-2.5 text-xs"
              >
                {cancelSubmitting ? 'Cancelling...' : 'Confirm Cancel Show'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
