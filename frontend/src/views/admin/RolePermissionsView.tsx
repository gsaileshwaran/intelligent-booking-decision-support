import React, { useState, useEffect } from 'react';
import { ArrowLeft, Key, Check } from 'lucide-react';
import { adminApi } from '../../api/client';
import type { RoleInfo } from '../../types/admin';

interface RolePermissionsViewProps {
  onNavigate: (view: string) => void;
}

export const RolePermissionsView: React.FC<RolePermissionsViewProps> = ({ onNavigate }) => {
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.getRoles()
      .then((data) => setRoles(data || []))
      .catch((err) => {
        console.error('Failed to load roles', err);
        // Fallback standard roles for display
        setRoles([
          {
            roleId: 1,
            roleName: 'ROLE_SUPER_ADMIN',
            description: 'Full platform administrative privileges, theatre provisioning, global audits, and catalogue CRUD.',
            permissions: ['ALL_PERMISSIONS', 'USER_MANAGEMENT', 'THEATRE_PROVISION', 'AUDIT_VIEW', 'SEARCH_REINDEX'],
          },
          {
            roleId: 2,
            roleName: 'ROLE_THEATRE_MANAGER',
            description: 'Scoped strictly to assigned multiplexes. Can manage screens, seats, and schedule showtimes.',
            permissions: ['SCREEN_MANAGE', 'SHOW_SCHEDULE', 'SEAT_OVERRIDE'],
          },
          {
            roleId: 3,
            roleName: 'ROLE_CUSTOMER',
            description: 'Standard end-user. Movie catalogue browsing, search, show discovery, and seat preview.',
            permissions: ['CATALOGUE_READ', 'SHOWS_READ', 'SEATS_PREVIEW'],
          },
          {
            roleId: 4,
            roleName: 'ROLE_SUPPORT_AGENT',
            description: 'Operational assistance and read-only inspection for customer support queries.',
            permissions: ['CATALOGUE_READ', 'THEATRE_READ'],
          },
        ]);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
        Loading roles and permissions matrix...
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="role-permissions-view">
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

      <div>
        <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Roles &amp; Permissions Matrix</h1>
        <p>Security role hierarchy, resource boundaries, and privilege allocations</p>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
        gap: '24px',
      }}>
        {roles.map((r) => (
          <div key={r.roleId} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Key size={20} color="var(--accent-gold)" />
                <h3 style={{ fontSize: '1.25rem' }}>{r.roleName.replace('ROLE_', '')}</h3>
              </div>
              <span className="badge badge-crimson">Role ID #{r.roleId}</span>
            </div>

            <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
              {r.description || 'Standard platform role definition.'}
            </p>

            <div style={{
              borderTop: '1px solid var(--border-subtle)',
              paddingTop: '16px',
              marginTop: 'auto',
            }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Granted Permissions
              </span>
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '8px' }}>
                {r.permissions && r.permissions.length > 0 ? (
                  r.permissions.map((p) => (
                    <span key={p} className="badge badge-slate" style={{ fontSize: '0.72rem' }}>
                      <Check size={10} color="#10B981" />
                      {p}
                    </span>
                  ))
                ) : (
                  <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Inherited standard privileges</span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
