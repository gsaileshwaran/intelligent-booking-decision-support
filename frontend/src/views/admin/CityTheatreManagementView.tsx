import React, { useState, useEffect } from 'react';
import { ArrowLeft, MapPin, Plus, UserCheck, X, Check } from 'lucide-react';
import { adminApi, theatresApi } from '../../api/client';
import type { City, Theatre } from '../../types/theatre';
import type { AdminUser } from '../../types/admin';
import { useToast } from '../../context/ToastContext';

interface CityTheatreManagementViewProps {
  onNavigate: (view: string) => void;
}

export const CityTheatreManagementView: React.FC<CityTheatreManagementViewProps> = ({ onNavigate }) => {
  const { showToast } = useToast();
  const [cities, setCities] = useState<City[]>([]);
  const [theatres, setTheatres] = useState<Theatre[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [isCityModalOpen, setIsCityModalOpen] = useState(false);
  const [cityName, setCityName] = useState('');
  const [cityState, setCityState] = useState('');
  const [cityCountry, setCityCountry] = useState('India');

  const [isTheatreModalOpen, setIsTheatreModalOpen] = useState(false);
  const [theatreName, setTheatreName] = useState('');
  const [theatreCode, setTheatreCode] = useState('');
  const [theatreCityId, setTheatreCityId] = useState<number | ''>('');
  const [addressLine1, setAddressLine1] = useState('');
  const [totalScreens, setTotalScreens] = useState<number>(3);

  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [assignTheatreId, setAssignTheatreId] = useState<number | null>(null);
  const [assignUserId, setAssignUserId] = useState<number | ''>('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [citiesData, theatresData, usersPage] = await Promise.all([
        theatresApi.getCities(),
        adminApi.getTheatres(),
        adminApi.getUsers({ page: 0, size: 50 }),
      ]);
      setCities(citiesData || []);
      setTheatres(theatresData || []);
      setUsers(usersPage.content || []);
    } catch (err: any) {
      console.error('Failed to load organization data', err);
      showToast(err.message || 'Failed to load data', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateCity = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      await adminApi.createCity({
        cityName,
        state: cityState,
        country: cityCountry,
      });
      showToast(`City "${cityName}" created successfully!`, 'success');
      setIsCityModalOpen(false);
      setCityName('');
      setCityState('');
      await loadData();
    } catch (err: any) {
      console.error('Failed to create city', err);
      showToast(err.message || 'Failed to create city', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateTheatre = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!theatreCityId) return;
    try {
      setSubmitting(true);
      await adminApi.createTheatre({
        cityId: Number(theatreCityId),
        theatreName,
        theatreCode,
        addressLine1,
        totalScreens: Number(totalScreens),
      });
      showToast(`Multiplex "${theatreName}" created successfully!`, 'success');
      setIsTheatreModalOpen(false);
      setTheatreName('');
      setTheatreCode('');
      setAddressLine1('');
      await loadData();
    } catch (err: any) {
      console.error('Failed to create theatre', err);
      showToast(err.message || 'Failed to create theatre', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAssignManager = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignTheatreId || !assignUserId) return;
    try {
      setSubmitting(true);
      await adminApi.assignManager(assignTheatreId, Number(assignUserId));
      showToast('Manager successfully assigned to theatre!', 'success');
      setIsAssignModalOpen(false);
      setAssignTheatreId(null);
      setAssignUserId('');
    } catch (err: any) {
      console.error('Failed to assign manager', err);
      showToast(err.message || 'Failed to assign manager', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '36px' }} data-testid="city-theatre-management-view">
      <div>
        <button
          onClick={() => onNavigate('admin-dashboard')}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Admin Dashboard
        </button>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Cities &amp; Theatres Provisioning</h1>
          <p>Configure geographic locations, provision multiplex properties, and delegate managers</p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button onClick={() => setIsCityModalOpen(true)} className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
            <Plus size={14} />
            Add City
          </button>
          <button onClick={() => {
            if (cities.length > 0) setTheatreCityId(cities[0].cityId);
            setIsTheatreModalOpen(true);
          }} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
            <Plus size={14} />
            Provision Multiplex
          </button>
        </div>
      </div>

      {/* Cities Section */}
      <section>
        <h2 style={{ fontSize: '1.3rem', marginBottom: '16px' }}>Operational Cities ({cities.length})</h2>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '16px',
        }}>
          {cities.map((c) => (
            <div key={c.cityId} className="card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ background: 'var(--accent-crimson-glow)', color: 'var(--accent-crimson)', padding: '10px', borderRadius: '8px' }}>
                <MapPin size={20} />
              </div>
              <div>
                <h4 style={{ fontSize: '1.05rem', marginBottom: '2px' }}>{c.cityName}</h4>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{c.state}, {c.country}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Theatres Section Table */}
      <section>
        <h2 style={{ fontSize: '1.3rem', marginBottom: '16px' }}>Multiplex Inventory ({theatres.length})</h2>
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <div className="table-container" style={{ border: 'none', borderRadius: 0 }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Theatre Name</th>
                  <th>City</th>
                  <th>Address</th>
                  <th>Screens</th>
                  <th>Status</th>
                  <th>Manager Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                      Loading multiplexes...
                    </td>
                  </tr>
                ) : theatres.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                      No theatres provisioned yet.
                    </td>
                  </tr>
                ) : (
                  theatres.map((t) => (
                    <tr key={t.theatreId}>
                      <td><code>{t.theatreCode}</code></td>
                      <td style={{ fontWeight: 600 }}>{t.theatreName}</td>
                      <td>{t.cityName || `City #${t.cityId}`}</td>
                      <td>{t.addressLine1}</td>
                      <td>{t.totalScreens ?? 0}</td>
                      <td>
                        <span className="badge badge-emerald">{t.theatreStatus || 'ACTIVE'}</span>
                      </td>
                      <td>
                        <button
                          onClick={() => {
                            setAssignTheatreId(t.theatreId);
                            setIsAssignModalOpen(true);
                          }}
                          className="btn btn-sm btn-secondary"
                          style={{ padding: '4px 8px', fontSize: '0.8rem', gap: '4px' }}
                        >
                          <UserCheck size={12} />
                          Assign Manager
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      {/* Add City Modal */}
      {isCityModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem' }}>Add Operational City</h3>
              <button onClick={() => setIsCityModalOpen(false)} className="btn-icon" aria-label="Close dialog">
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleCreateCity}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="city-name">City Name</label>
                  <input
                    id="city-name"
                    type="text"
                    required
                    placeholder="e.g. Pune"
                    value={cityName}
                    onChange={(e) => setCityName(e.target.value)}
                    className="input"
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="city-state">State / Province</label>
                  <input
                    id="city-state"
                    type="text"
                    required
                    placeholder="e.g. Maharashtra"
                    value={cityState}
                    onChange={(e) => setCityState(e.target.value)}
                    className="input"
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="city-country">Country</label>
                  <input
                    id="city-country"
                    type="text"
                    required
                    value={cityCountry}
                    onChange={(e) => setCityCountry(e.target.value)}
                    className="input"
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setIsCityModalOpen(false)} className="btn btn-secondary btn-sm">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
                  <Check size={14} />
                  {submitting ? 'Creating...' : 'Create City'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Provision Theatre Modal */}
      {isTheatreModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem' }}>Provision Multiplex Theatre</h3>
              <button onClick={() => setIsTheatreModalOpen(false)} className="btn-icon" aria-label="Close dialog">
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleCreateTheatre}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="theatre-city">City Location</label>
                  <select
                    id="theatre-city"
                    required
                    value={theatreCityId}
                    onChange={(e) => setTheatreCityId(parseInt(e.target.value, 10))}
                    className="select"
                  >
                    {cities.map((c) => (
                      <option key={c.cityId} value={c.cityId}>{c.cityName}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="theatre-name">Theatre Name</label>
                  <input
                    id="theatre-name"
                    type="text"
                    required
                    placeholder="e.g. PVK Cinemas Phoenix Mall"
                    value={theatreName}
                    onChange={(e) => setTheatreName(e.target.value)}
                    className="input"
                  />
                </div>
                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="theatre-code">Theatre Code</label>
                    <input
                      id="theatre-code"
                      type="text"
                      required
                      placeholder="e.g. PVK-PUN-01"
                      value={theatreCode}
                      onChange={(e) => setTheatreCode(e.target.value)}
                      className="input"
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="theatre-screens">Screen Count</label>
                    <input
                      id="theatre-screens"
                      type="number"
                      min="1"
                      required
                      value={totalScreens}
                      onChange={(e) => setTotalScreens(parseInt(e.target.value, 10))}
                      className="input"
                    />
                  </div>
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="theatre-address">Address Line</label>
                  <input
                    id="theatre-address"
                    type="text"
                    required
                    placeholder="e.g. Viman Nagar Road, Phoenix Marketcity"
                    value={addressLine1}
                    onChange={(e) => setAddressLine1(e.target.value)}
                    className="input"
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setIsTheatreModalOpen(false)} className="btn btn-secondary btn-sm">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
                  <Check size={14} />
                  {submitting ? 'Provisioning...' : 'Provision Multiplex'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Assign Manager Modal */}
      {isAssignModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem' }}>Assign Theatre Manager</h3>
              <button onClick={() => setIsAssignModalOpen(false)} className="btn-icon" aria-label="Close dialog">
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleAssignManager}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                  Assigning a user as manager grants operational access to manage screens, layouts, and schedules for this theatre.
                </p>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="assign-user">Select User</label>
                  <select
                    id="assign-user"
                    required
                    value={assignUserId}
                    onChange={(e) => setAssignUserId(parseInt(e.target.value, 10))}
                    className="select"
                  >
                    <option value="">Select a user...</option>
                    {users.map((u) => (
                      <option key={u.userId} value={u.userId}>
                        {u.firstName} {u.lastName} ({u.email})
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setIsAssignModalOpen(false)} className="btn btn-secondary btn-sm">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
                  <Check size={14} />
                  {submitting ? 'Assigning...' : 'Confirm Assignment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
