import React, { useState, useEffect } from 'react';
import { providerService } from '../../services/providerService';
import { Building2, Plus, MapPin, CheckCircle, AlertCircle } from 'lucide-react';

export const VenueManagerPage = () => {
  const [theatres, setTheatres] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form State
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

  const handleCreateTheatre = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setMessage('');

    try {
      const res = await providerService.createTheatre({ name, location, address, status: 'ACTIVE' });
      if (res.success) {
        setMessage('Theatre venue created successfully!');
        setShowModal(false);
        setName('');
        setLocation('');
        setAddress('');
        fetchTheatres();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create theatre.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold text-white">Venue & Theatre Manager</h1>
          <p className="text-xs text-slate-400 mt-1">Configure physical cinema branches and screen auditoriums</p>
        </div>

        <button onClick={() => setShowModal(true)} className="btn-primary text-xs">
          <Plus className="w-4 h-4" /> Add New Theatre Venue
        </button>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
          {message}
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading operated venues...</div>
      ) : theatres.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Building2 className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No Theatre Venues Registered</h3>
          <p className="text-xs text-slate-500 mt-1">Click "Add New Theatre Venue" to add your cinema branches.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {theatres.map((t) => (
            <div key={t.theatreId} className="glass-card p-6 flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center">
                      <Building2 className="w-5 h-5" />
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

                <p className="text-xs text-slate-400 mt-4 border-t border-slate-800 pt-3">
                  Address: {t.address || 'Address not specified'}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Theatre Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-4">Add Theatre Venue</h2>

            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
                {error}
              </div>
            )}

            <form onSubmit={handleCreateTheatre}>
              <div className="form-group">
                <label className="form-label">Theatre Name</label>
                <input
                  type="text"
                  required
                  placeholder="Grand Cinema Downtown"
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
                  placeholder="Metropolis Central"
                  className="form-input"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Address</label>
                <input
                  type="text"
                  placeholder="123 Main Boulevard"
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
                  {submitting ? 'Saving...' : 'Create Venue'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
