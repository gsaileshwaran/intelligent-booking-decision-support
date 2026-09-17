import React, { useState, useEffect } from 'react';
import { ArrowLeft, RefreshCw } from 'lucide-react';
import { adminApi } from '../../api/client';
import type { AuditLog } from '../../types/admin';
import { useToast } from '../../context/ToastContext';

interface AuditLogsViewProps {
  onNavigate: (view: string) => void;
}

export const AuditLogsView: React.FC<AuditLogsViewProps> = ({ onNavigate }) => {
  const { showToast } = useToast();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [loading, setLoading] = useState(true);

  const loadAuditLogs = async () => {
    try {
      setLoading(true);
      const data = await adminApi.getAuditLogs({ page, size: 15 });
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: any) {
      console.error('Failed to load audit logs', err);
      showToast(err.message || 'Failed to load audit trail', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAuditLogs();
  }, [page]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="audit-logs-view">
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
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Immutable Audit Trail</h1>
          <p>Chronological security and administrative event ledger across all platform domains</p>
        </div>

        <button onClick={loadAuditLogs} disabled={loading} className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Refresh Trail
        </button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none', borderRadius: 0 }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Log ID</th>
                <th>Actor User ID</th>
                <th>Action</th>
                <th>Target Entity</th>
                <th>Entity ID</th>
                <th>Occurred At</th>
                <th>Mutation Payload / Details</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading audit trail logs...
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No audit records logged yet.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.auditLogId}>
                    <td><code>#{log.auditLogId}</code></td>
                    <td>
                      <span className="badge badge-slate">User #{log.actorUserId}</span>
                    </td>
                    <td>
                      <span className="badge badge-crimson">{log.action || log.actionType || 'MUTATION'}</span>
                    </td>
                    <td>{log.entityType}</td>
                    <td><code>#{log.entityId}</code></td>
                    <td style={{ whiteSpace: 'nowrap', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      {log.occurredAt || log.createdAt ? new Date(log.occurredAt || log.createdAt!).toLocaleString() : 'Recent'}
                    </td>
                    <td style={{ maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      <code style={{ fontSize: '0.75rem' }}>
                        {log.newValue || log.details || log.oldValue || 'N/A'}
                      </code>
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
    </div>
  );
};
