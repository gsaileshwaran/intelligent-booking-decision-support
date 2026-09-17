import React, { useState, useEffect } from 'react';
import { ArrowLeft, CheckCircle, Ban, RefreshCw, UserPlus, X } from 'lucide-react';
import { adminApi } from '../../api/client';
import type { AdminUser } from '../../types/admin';
import type { Theatre } from '../../types/theatre';
import type { CreateUserRequest } from '../../types/theatre';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';

interface UserManagementViewProps {
  onNavigate: (view: string) => void;
}

export const UserManagementView: React.FC<UserManagementViewProps> = ({ onNavigate }) => {
  const { showToast } = useToast();
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [loading, setLoading] = useState<boolean>(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  // Provision modal state
  const [showProvisionModal, setShowProvisionModal] = useState(false);
  const [theatres, setTheatres] = useState<Theatre[]>([]);
  const [provisioning, setProvisioning] = useState(false);
  const [provisionForm, setProvisionForm] = useState<CreateUserRequest>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phone: '',
    roleCode: 'ROLE_CUSTOMER',
    theatreId: undefined,
  });

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await adminApi.getUsers({ page, size: 10 });
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: any) {
      console.error('Failed to load users', err);
      showToast(err.message || 'Failed to load user accounts', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [page]);

  const loadTheatres = async () => {
    try {
      const data = await adminApi.getTheatres();
      setTheatres(data || []);
    } catch (err) {
      console.error('Failed to load theatres', err);
    }
  };

  const handleToggleStatus = async (user: AdminUser) => {
    if (currentUser?.userId === user.userId && user.accountStatus === 'ACTIVE') {
      showToast('Administrators cannot suspend their own account', 'error');
      return;
    }
    if (user.accountStatus === 'ACTIVE') {
      const confirmed = window.confirm(`Are you sure you want to suspend user ${user.firstName} ${user.lastName} (${user.email})?`);
      if (!confirmed) {
        return;
      }
    }
    const nextStatus = user.accountStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      setUpdatingId(user.userId);
      await adminApi.updateUserStatus(user.userId, nextStatus);
      showToast(`User ${user.email} marked as ${nextStatus}!`, 'success');
      await loadUsers();
    } catch (err: any) {
      console.error('Failed to update status', err);
      showToast(err.message || 'Failed to update user status', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleOpenProvision = async () => {
    setProvisionForm({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      phone: '',
      roleCode: 'ROLE_CUSTOMER',
      theatreId: undefined,
    });
    await loadTheatres();
    setShowProvisionModal(true);
  };

  const handleProvisionSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!provisionForm.firstName || !provisionForm.lastName || !provisionForm.email || !provisionForm.password) {
      showToast('Please fill all required fields.', 'error');
      return;
    }
    try {
      setProvisioning(true);
      const payload: CreateUserRequest = {
        ...provisionForm,
        theatreId: provisionForm.roleCode === 'ROLE_THEATRE_MANAGER' ? provisionForm.theatreId : undefined,
      };
      await adminApi.createUser(payload);
      showToast(`User ${provisionForm.email} provisioned successfully!`, 'success');
      setShowProvisionModal(false);
      await loadUsers();
    } catch (err: any) {
      console.error('Failed to provision user', err);
      showToast(err.message || 'Failed to provision user', 'error');
    } finally {
      setProvisioning(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="user-management-view">
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
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>User Management</h1>
          <p>Supervise user accounts, role allocations, and security statuses</p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={handleOpenProvision} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
            <UserPlus size={14} />
            Provision User
          </button>
          <button onClick={loadUsers} disabled={loading} className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            Refresh List
          </button>
        </div>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none', borderRadius: 0 }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>User ID</th>
                <th>Full Name</th>
                <th>Email Address</th>
                <th>Assigned Roles</th>
                <th>Account Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading user accounts...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No users found in this page.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.userId}>
                    <td><code>#{u.userId}</code></td>
                    <td style={{ fontWeight: 600 }}>{u.firstName} {u.lastName}</td>
                    <td>{u.email}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                        {u.roles?.map((r) => (
                          <span key={r} className="badge badge-slate" style={{ fontSize: '0.7rem' }}>
                            {r.replace('ROLE_', '')}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${u.accountStatus === 'ACTIVE' ? 'badge-emerald' : 'badge-crimson'}`}>
                        {u.accountStatus}
                      </span>
                    </td>
                    <td>
                      {currentUser?.userId === u.userId ? (
                        <span className="badge badge-slate" title="Current Administrator (Self-suspension disabled)" style={{ fontSize: '0.75rem', opacity: 0.8 }}>
                          Current User
                        </span>
                      ) : (
                        <button
                          onClick={() => handleToggleStatus(u)}
                          disabled={updatingId === u.userId}
                          className={`btn btn-sm ${u.accountStatus === 'ACTIVE' ? 'btn-danger' : 'btn-secondary'}`}
                          style={{ padding: '4px 10px', fontSize: '0.8rem', gap: '4px' }}
                        >
                          {u.accountStatus === 'ACTIVE' ? (
                            <>
                              <Ban size={12} />
                              Suspend
                            </>
                          ) : (
                            <>
                              <CheckCircle size={12} />
                              Activate
                            </>
                          )}
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="btn btn-secondary btn-sm"
          >
            Previous
          </button>
          <span style={{ display: 'flex', alignItems: 'center', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="btn btn-secondary btn-sm"
          >
            Next
          </button>
        </div>
      )}

      {/* Provision User / Manager Modal */}
      {showProvisionModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content" style={{ maxWidth: '560px' }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <UserPlus size={18} style={{ color: 'var(--accent-primary)' }} />
                Provision New User
              </h3>
              <button onClick={() => setShowProvisionModal(false)} className="btn-icon" aria-label="Close modal">
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleProvisionSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="prov-firstname">First Name *</label>
                    <input
                      id="prov-firstname"
                      type="text"
                      required
                      className="input"
                      value={provisionForm.firstName}
                      onChange={(e) => setProvisionForm((f) => ({ ...f, firstName: e.target.value }))}
                      placeholder="First name"
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="prov-lastname">Last Name *</label>
                    <input
                      id="prov-lastname"
                      type="text"
                      required
                      className="input"
                      value={provisionForm.lastName}
                      onChange={(e) => setProvisionForm((f) => ({ ...f, lastName: e.target.value }))}
                      placeholder="Last name"
                    />
                  </div>
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="prov-email">Email Address *</label>
                  <input
                    id="prov-email"
                    type="email"
                    required
                    className="input"
                    value={provisionForm.email}
                    onChange={(e) => setProvisionForm((f) => ({ ...f, email: e.target.value }))}
                    placeholder="user@example.com"
                  />
                </div>

                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="prov-password">Password *</label>
                    <input
                      id="prov-password"
                      type="password"
                      required
                      minLength={6}
                      className="input"
                      value={provisionForm.password}
                      onChange={(e) => setProvisionForm((f) => ({ ...f, password: e.target.value }))}
                      placeholder="Min. 6 characters"
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="prov-phone">Phone (optional)</label>
                    <input
                      id="prov-phone"
                      type="tel"
                      className="input"
                      value={provisionForm.phone}
                      onChange={(e) => setProvisionForm((f) => ({ ...f, phone: e.target.value }))}
                      placeholder="+91 98765 43210"
                    />
                  </div>
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="prov-role">Assign Role *</label>
                  <select
                    id="prov-role"
                    required
                    className="select"
                    value={provisionForm.roleCode}
                    onChange={(e) => setProvisionForm((f) => ({ ...f, roleCode: e.target.value, theatreId: undefined }))}
                  >
                    <option value="ROLE_CUSTOMER">Customer</option>
                    <option value="ROLE_THEATRE_MANAGER">Theatre Manager</option>
                  </select>
                </div>

                {provisionForm.roleCode === 'ROLE_THEATRE_MANAGER' && (
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="prov-theatre">Assign Theatre (optional)</label>
                    <select
                      id="prov-theatre"
                      className="select"
                      value={provisionForm.theatreId ?? ''}
                      onChange={(e) => setProvisionForm((f) => ({ ...f, theatreId: e.target.value ? Number(e.target.value) : undefined }))}
                    >
                      <option value="">— No theatre assignment —</option>
                      {theatres.map((t) => (
                        <option key={t.theatreId} value={t.theatreId}>
                          {t.theatreName} ({t.cityName || `City ${t.cityId}`})
                        </option>
                      ))}
                    </select>
                    <small style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>
                      If selected, this manager will be assigned to the theatre immediately.
                    </small>
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button type="button" onClick={() => setShowProvisionModal(false)} className="btn btn-secondary btn-sm">
                  Cancel
                </button>
                <button type="submit" disabled={provisioning} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
                  <UserPlus size={14} />
                  {provisioning ? 'Provisioning...' : 'Provision User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
