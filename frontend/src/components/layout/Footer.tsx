import React from 'react';
import { Film, ShieldAlert } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer style={{
      background: 'var(--bg-surface)',
      borderTop: '1px solid var(--border-subtle)',
      padding: '48px 0 32px 0',
      marginTop: 'auto',
    }}>
      <div className="app-container">
        {/* Authoritative Architectural Scope Disclaimer */}
        <div style={{
          background: 'rgba(245, 158, 11, 0.08)',
          border: '1px solid rgba(245, 158, 11, 0.25)',
          borderRadius: 'var(--radius-md)',
          padding: '16px 20px',
          display: 'flex',
          alignItems: 'center',
          gap: '14px',
          marginBottom: '36px',
        }}>
          <ShieldAlert size={22} color="#F59E0B" style={{ flexShrink: 0 }} />
          <p style={{ margin: 0, fontSize: '0.85rem', color: '#FDE68A', lineHeight: 1.5 }}>
            <strong>PVK Cinemas Information Notice:</strong> PVK Cinemas is a cinema discovery and decision-support platform with movie search, 
            showtime discovery, seat selection, temporary seat holds, booking, and simulated payment. 
            <strong> Payments use dummy/test data only; no real financial transactions are processed.</strong>
          </p>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '32px',
          marginBottom: '36px',
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
              <div style={{
                background: 'var(--accent-crimson)',
                color: '#fff',
                borderRadius: '6px',
                padding: '4px 8px',
                fontWeight: 900,
                fontSize: '1rem',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
              }}>
                <Film size={16} />
                PVK
              </div>
              <span style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: '1.1rem' }}>
                CINEMAS
              </span>
            </div>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
              Next-generation intelligent movie catalogue, multiplex schedule discovery, and real-time screen visualization.
            </p>
          </div>

          <div>
            <h4 style={{ fontSize: '0.95rem', marginBottom: '14px', color: 'var(--text-primary)' }}>Top Cinema Hubs</h4>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.88rem', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <li><span style={{ color: 'var(--text-secondary)' }}>Bangalore — Forum Mall & Orion</span></li>
              <li><span style={{ color: 'var(--text-secondary)' }}>Mumbai — Phoenix Palladium</span></li>
              <li><span style={{ color: 'var(--text-secondary)' }}>Hyderabad — Inorbit Hitec City</span></li>
              <li><span style={{ color: 'var(--text-secondary)' }}>Delhi NCR — Ambience Mall</span></li>
            </ul>
          </div>

          <div>
            <h4 style={{ fontSize: '0.95rem', marginBottom: '14px', color: 'var(--text-primary)' }}>System Information</h4>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.88rem', display: 'flex', flexDirection: 'column', gap: '8px', color: 'var(--text-muted)' }}>
              <li>Architecture: Spring Boot REST + Vite React</li>
              <li>Search: Hybrid Lexical-Semantic AI Engine</li>
              <li>Database: MySQL 8.0 (28 Normalized Entities)</li>
              <li>Security: JWT + RBAC Multi-Portal Isolation</li>
            </ul>
          </div>
        </div>

        <div style={{
          borderTop: '1px solid var(--border-subtle)',
          paddingTop: '20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          fontSize: '0.82rem',
          color: 'var(--text-muted)',
        }}>
          <div>&copy; {new Date().getFullYear()} PVK Cinemas Inc. All rights reserved. Intelligent Cinema Discovery & Booking Platform.</div>
          <div>Simulated Payment Active &bull; Test / Academic Demo Mode</div>
        </div>
      </div>
    </footer>
  );
};
