import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, Edit2, Check, X } from 'lucide-react';
import { managerApi, theatresApi } from '../../api/client';
import type { Screen, Theatre, ScreenRequest } from '../../types/theatre';
import { useToast } from '../../context/ToastContext';

interface ScreenManagementViewProps {
  theatreId: number;
  onNavigate: (view: string, param?: any) => void;
}

export const ScreenManagementView: React.FC<ScreenManagementViewProps> = ({ theatreId, onNavigate }) => {
  const { showToast } = useToast();
  const [screens, setScreens] = useState<Screen[]>([]);
  const [theatre, setTheatre] = useState<Theatre | null>(null);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingScreen, setEditingScreen] = useState<Screen | null>(null);
  const [screenNumber, setScreenNumber] = useState<number>(1);
  const [screenName, setScreenName] = useState<string>('');
  const [screenStatus, setScreenStatus] = useState<string>('ACTIVE');
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [screensData, theatreData] = await Promise.all([
        managerApi.getScreens(theatreId),
        theatresApi.getTheatre(theatreId),
      ]);
      setScreens(screensData || []);
      setTheatre(theatreData);
    } catch (err: any) {
      console.error('Failed to load screens', err);
      showToast(err.message || 'Failed to load screens', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [theatreId]);

  const handleOpenAddModal = () => {
    setEditingScreen(null);
    setScreenNumber((screens.length || 0) + 1);
    setScreenName(`Audi ${screens.length + 1}`);
    setScreenStatus('ACTIVE');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (screen: Screen) => {
    setEditingScreen(screen);
    setScreenNumber(screen.screenNumber);
    setScreenName(screen.screenName);
    setScreenStatus(screen.screenStatus || 'ACTIVE');
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      const payload: ScreenRequest = {
        screenNumber,
        screenName,
        screenStatus,
      };

      if (editingScreen) {
        await managerApi.updateScreen(theatreId, editingScreen.screenId, payload);
        showToast('Screen updated successfully!', 'success');
      } else {
        await managerApi.createScreen(theatreId, payload);
        showToast('New screen added to multiplex!', 'success');
      }

      setIsModalOpen(false);
      await loadData();
    } catch (err: any) {
      console.error('Failed to save screen', err);
      showToast(err.message || 'Failed to save screen', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="screen-management-view">
      <div>
        <button
          onClick={() => onNavigate('manager-dashboard')}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Manager Dashboard
        </button>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Screen Inventory</h1>
          <p>Auditorium screens at {theatre?.theatreName || 'Multiplex'}</p>
        </div>

        <button onClick={handleOpenAddModal} className="btn btn-primary" style={{ gap: '6px' }}>
          <Plus size={16} />
          Add Auditorium Screen
        </button>
      </div>

      {/* Screens List Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none', borderRadius: 0 }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Screen Number</th>
                <th>Screen Name</th>
                <th>Capacity / Seats</th>
                <th>Format Capability</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading screen inventory...
                  </td>
                </tr>
              ) : screens.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No screens configured for this theatre. Click "Add Auditorium Screen" above.
                  </td>
                </tr>
              ) : (
                screens.map((screen) => (
                  <tr key={screen.screenId}>
                    <td><code>Audi #{screen.screenNumber}</code></td>
                    <td style={{ fontWeight: 600 }}>{screen.screenName}</td>
                    <td>{screen.totalSeats || 'Configured'}</td>
                    <td>{screen.screenTypeName || 'Standard Digital'}</td>
                    <td>
                      <span className={`badge ${screen.screenStatus === 'ACTIVE' ? 'badge-emerald' : 'badge-amber'}`}>
                        {screen.screenStatus || 'ACTIVE'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          onClick={() => handleOpenEditModal(screen)}
                          className="btn btn-sm btn-secondary"
                          style={{ padding: '4px 8px', fontSize: '0.8rem', gap: '4px' }}
                        >
                          <Edit2 size={12} />
                          Edit
                        </button>
                        <button
                          onClick={() => onNavigate('manager-seats', theatreId)}
                          className="btn btn-sm btn-outline"
                          style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                        >
                          Configure Seats
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Dialog for Add / Edit Screen */}
      {isModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem' }}>
                {editingScreen ? `Edit ${editingScreen.screenName}` : 'Add New Auditorium Screen'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="btn-icon"
                aria-label="Close dialog"
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="screen-num">Screen Number</label>
                  <input
                    id="screen-num"
                    type="number"
                    min="1"
                    required
                    value={screenNumber}
                    onChange={(e) => setScreenNumber(parseInt(e.target.value, 10))}
                    className="input"
                  />
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="screen-name">Screen / Auditorium Name</label>
                  <input
                    id="screen-name"
                    type="text"
                    required
                    placeholder="e.g. Audi 1 (Dolby Atmos)"
                    value={screenName}
                    onChange={(e) => setScreenName(e.target.value)}
                    className="input"
                  />
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="screen-status">Screen Status</label>
                  <select
                    id="screen-status"
                    value={screenStatus}
                    onChange={(e) => setScreenStatus(e.target.value)}
                    className="select"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="MAINTENANCE">MAINTENANCE</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn btn-primary btn-sm"
                  style={{ gap: '6px' }}
                >
                  <Check size={14} />
                  {submitting ? 'Saving...' : editingScreen ? 'Update Screen' : 'Create Screen'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
