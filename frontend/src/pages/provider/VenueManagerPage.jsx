import React, { useState, useEffect } from 'react';
import { providerService } from '../../services/providerService';
import { Building2, Plus, MapPin, CheckCircle, AlertCircle, Tv, Calendar, Users, Eye, X } from 'lucide-react';

export const VenueManagerPage = () => {
  const [theatres, setTheatres] = useState([]);
  const [loading, setLoading] = useState(true);

  // Selected Branch Detail State
  const [selectedBranch, setSelectedBranch] = useState(null);
  const [branchScreens, setBranchScreens] = useState([]);
  const [branchShows, setBranchShows] = useState([]);
  const [branchBookings, setBranchBookings] = useState([]);
  const [loadingDetails, setLoadingDetails] = useState(false);

  // Create Modal State
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [address, setAddress] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    fetchTheatres();
  }, []);

  const fetchTheatres = async () => {
    setLoading(true);
    try {
      const res = await providerService.getTheatres();
      if (res.success && res.data) {
        setTheatres(res.data);
      }
    } catch (err) {
      console.error('Failed to load theatres:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectBranch = async (theatre) => {
    setSelectedBranch(theatre);
    setLoadingDetails(true);
    try {
      const [screensRes, showsRes, bookingsRes] = await Promise.all([
        providerService.getTheatreScreens(theatre.theatreId),
        providerService.getTheatreShows(theatre.theatreId),
        providerService.getTheatreBookings(theatre.theatreId),
      ]);

      if (screensRes.success) setBranchScreens(screensRes.data || []);
      if (showsRes.success) setBranchShows(showsRes.data || []);
      if (bookingsRes.success) setBranchBookings(bookingsRes.data || []);
    } catch (err) {
      console.error('Failed to load branch details:', err);
    } finally {
      setLoadingDetails(false);
    }
  };

  const handleCreateTheatre = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setMessage('');

    try {
      const res = await providerService.createTheatre({ name, location, address, status: 'ACTIVE' });
      if (res.success) {
        setMessage('PVK Branch created successfully!');
        setShowModal(false);
        setName('');
        setLocation('');
        setAddress('');
        fetchTheatres();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create branch.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">PVK Chain Management</span>
          <h1 className="text-3xl font-extrabold text-white">PVK Branch & Screen Manager</h1>
          <p className="text-xs text-slate-400 mt-1">Configure PVK cinema branches, auditoriums, seating capacities, and show schedules</p>
        </div>

        <button onClick={() => setShowModal(true)} className="btn-primary text-xs">
          <Plus className="w-4 h-4" /> Add PVK Branch
        </button>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
          {message}
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading PVK branches...</div>
      ) : theatres.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Building2 className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No PVK Branches Registered</h3>
          <p className="text-xs text-slate-500 mt-1">Click "Add PVK Branch" to add cinema branches.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {theatres.map((t) => (
            <div key={t.theatreId} className="glass-card p-6 flex flex-col justify-between border border-slate-800 hover:border-slate-700 transition-all">
              <div>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center">
                      <Building2 className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white">{t.name}</h3>
                      <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                        <MapPin className="w-3.5 h-3.5 text-slate-500" /> {t.location}
                      </p>
                    </div>
                  </div>
                  <span className="badge badge-confirmed">{t.status || 'ACTIVE'}</span>
                </div>

                <div className="grid grid-cols-2 gap-3 mt-4 pt-4 border-t border-slate-800/80 text-xs">
                  <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-800">
                    <span className="block text-[10px] text-slate-500 uppercase">Screens Count</span>
                    <span className="font-bold text-slate-200 text-sm">{t.screens?.length || 0} Auditoriums</span>
                  </div>
                  <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-800">
                    <span className="block text-[10px] text-slate-500 uppercase">Total Seating</span>
                    <span className="font-bold text-indigo-300 text-sm">
                      {t.screens?.reduce((acc, s) => acc + (s.capacity || 0), 0) || 0} Seats
                    </span>
                  </div>
                </div>

                <p className="text-xs text-slate-400 mt-3 text-slate-400">
                  Address: {t.address || 'Address not specified'}
                </p>
              </div>

              <button
                onClick={() => handleSelectBranch(t)}
                className="btn-secondary text-xs justify-center w-full mt-4"
              >
                <Eye className="w-4 h-4" /> Inspect Branch Details & Screens
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Branch Details Modal */}
      {selectedBranch && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="glass-card w-full max-w-3xl p-6 sm:p-8 animate-fade-in my-8 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
              <div>
                <span className="badge badge-confirmed mb-1">PVK Branch Details</span>
                <h2 className="text-2xl font-bold text-white">{selectedBranch.name}</h2>
                <p className="text-xs text-slate-400">{selectedBranch.address}</p>
              </div>
              <button
                onClick={() => setSelectedBranch(null)}
                className="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {loadingDetails ? (
              <div className="py-12 text-center text-slate-400">Loading branch screens and showtimes...</div>
            ) : (
              <div className="space-y-6">
                {/* Branch Auditoriums Section */}
                <div>
                  <h3 className="text-sm font-bold text-slate-200 mb-3 flex items-center gap-2">
                    <Tv className="w-4 h-4 text-indigo-400" />
                    Screens & Auditoriums ({branchScreens.length})
                  </h3>

                  {branchScreens.length === 0 ? (
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400">
                      No screens registered for this branch.
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      {branchScreens.map((s) => (
                        <div key={s.screenId} className="bg-slate-900/80 p-4 rounded-xl border border-slate-800">
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-bold text-purple-300">{s.name}</span>
                            <span className="text-[10px] px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-semibold">
                              Cap: {s.capacity} Seats
                            </span>
                          </div>
                          <p className="text-[11px] text-slate-400">
                            Seat Matrix Layout: Rows A to {s.capacity >= 100 ? 'J' : s.capacity >= 80 ? 'H' : 'F'} (10 seats/row)
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Branch Scheduled Shows */}
                <div>
                  <h3 className="text-sm font-bold text-slate-200 mb-3 flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-emerald-400" />
                    Scheduled Shows ({branchShows.length})
                  </h3>

                  {branchShows.length === 0 ? (
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400">
                      No shows scheduled currently for this branch.
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {branchShows.map((sh) => (
                        <div key={sh.showId} className="bg-slate-900/80 p-4 rounded-xl border border-slate-800 flex items-center justify-between gap-4">
                          <div>
                            <h4 className="text-xs font-bold text-white">{sh.movie?.title || 'Movie'}</h4>
                            <p className="text-[11px] text-slate-400 mt-0.5">
                              {sh.screen?.name} &bull; {sh.showDate} ({sh.startTime?.substring(0, 5)} - {sh.endTime?.substring(0, 5)})
                            </p>
                          </div>
                          <span className="text-xs font-extrabold text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-lg border border-emerald-500/20">
                            ₹{sh.ticketPrice?.toFixed(2)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Branch Bookings */}
                <div>
                  <h3 className="text-sm font-bold text-slate-200 mb-3 flex items-center gap-2">
                    <Users className="w-4 h-4 text-amber-400" />
                    Branch Bookings ({branchBookings.length})
                  </h3>

                  {branchBookings.length === 0 ? (
                    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 text-xs text-slate-400">
                      No bookings recorded yet for this branch.
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {branchBookings.map((b) => (
                        <div key={b.bookingId} className="bg-slate-900/80 p-3 rounded-lg border border-slate-800 flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-indigo-300">{b.bookingRef}</span>
                            <span className="text-slate-400 ml-2">Customer ID: {b.userId}</span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="badge badge-confirmed">{b.status}</span>
                            <span className="font-bold text-emerald-400">₹{b.totalAmount?.toFixed(2)}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Add Theatre Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-4">Add PVK Branch</h2>

            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
                {error}
              </div>
            )}

            <form onSubmit={handleCreateTheatre}>
              <div className="form-group">
                <label className="form-label">Branch Name</label>
                <input
                  type="text"
                  required
                  placeholder="PVK — Adyar"
                  className="form-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">City / Location</label>
                <input
                  type="text"
                  required
                  placeholder="Chennai"
                  className="form-input"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Address</label>
                <input
                  type="text"
                  placeholder="Lattice Bridge Road, Adyar, Chennai"
                  className="form-input"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                />
              </div>

              <div className="flex gap-3 mt-6">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn-secondary w-full text-xs py-2.5"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn-primary w-full text-xs py-2.5"
                >
                  {submitting ? 'Saving...' : 'Create PVK Branch'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
