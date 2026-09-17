import React, { useState, useEffect } from 'react';
import { CheckCircle2, MapPin, Tv, Calendar, QrCode, ArrowLeft, User } from 'lucide-react';
import type { BookingResponse } from '../../types/booking';
import { bookingApi } from '../../api/client';

interface BookingConfirmationViewProps {
  bookingReference?: string;
  initialBooking?: BookingResponse | null;
  onNavigate: (view: string, param?: any) => void;
}

export const BookingConfirmationView: React.FC<BookingConfirmationViewProps> = ({
  bookingReference,
  initialBooking,
  onNavigate,
}) => {
  const [booking, setBooking] = useState<BookingResponse | null>(initialBooking || null);
  const [loading, setLoading] = useState(!initialBooking && !!bookingReference);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!initialBooking && bookingReference) {
      const loadBooking = async () => {
        try {
          setLoading(true);
          setError(null);
          const data = await bookingApi.getBookingByReference(bookingReference);
          setBooking(data);
        } catch (err: any) {
          console.error('Failed to load booking details', err);
          setError(err.message || 'Failed to find booking');
        } finally {
          setLoading(false);
        }
      };
      loadBooking();
    }
  }, [bookingReference, initialBooking]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
        Retrieving your verified cinema ticket...
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="card" style={{ maxWidth: '600px', margin: '40px auto', textAlign: 'center', padding: '40px' }}>
        <h3 style={{ color: '#ef4444' }}>Booking Not Found</h3>
        <p style={{ marginTop: '10px', color: 'var(--text-secondary)' }}>{error || 'Unable to retrieve ticket details.'}</p>
        <button className="btn btn-primary" onClick={() => onNavigate('movies')} style={{ marginTop: '24px' }}>
          Browse Movies
        </button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Top Banner */}
      <div style={{
        background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.05) 100%)',
        border: '1px solid rgba(16, 185, 129, 0.3)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '10px',
      }}>
        <CheckCircle2 size={48} color="#10B981" />
        <h1 style={{ fontSize: '1.8rem', margin: 0, color: '#ffffff' }}>Booking Confirmed!</h1>
        <p style={{ margin: 0, color: '#A7F3D0', fontSize: '0.95rem' }}>
          Your seats are reserved and your ticket has been persisted to your account.
        </p>
      </div>

      {/* Cinema Ticket Card */}
      <div className="card" style={{
        padding: '0',
        borderRadius: 'var(--radius-lg)',
        overflow: 'hidden',
        border: '1px solid var(--border-medium)',
        background: 'var(--bg-surface)',
        boxShadow: 'var(--shadow-xl)',
        position: 'relative',
      }}>
        {/* Ticket Header */}
        <div style={{
          background: 'linear-gradient(90deg, var(--accent-crimson) 0%, #991b1b 100%)',
          padding: '20px 28px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          color: '#ffffff',
        }}>
          <div>
            <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', opacity: 0.9 }}>
              OFFICIAL PVK E-TICKET
            </div>
            <div style={{ fontSize: '1.2rem', fontWeight: 900, fontFamily: 'var(--font-display)' }}>
              PVK CINEMAS
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em', opacity: 0.85 }}>
              Booking Reference
            </div>
            <div style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: '1.05rem', letterSpacing: '0.05em' }}>
              {booking.bookingReference}
            </div>
          </div>
        </div>

        {/* Ticket Body */}
        <div style={{ padding: '28px' }}>
          <h2 style={{ fontSize: '1.9rem', marginBottom: '14px', color: '#ffffff' }}>
            {booking.movieTitle}
          </h2>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '20px',
            marginBottom: '24px',
          }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: '4px' }}>
                <MapPin size={14} color="var(--accent-crimson)" />
                THEATRE & CITY
              </div>
              <div style={{ fontWeight: 700, fontSize: '1rem', color: '#ffffff' }}>
                {booking.theatreName}
              </div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                {booking.cityName}
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: '4px' }}>
                <Tv size={14} color="var(--accent-gold)" />
                AUDITORIUM
              </div>
              <div style={{ fontWeight: 700, fontSize: '1rem', color: '#ffffff' }}>
                {booking.screenName}
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: '4px' }}>
                <Calendar size={14} color="var(--accent-crimson)" />
                DATE & TIME
              </div>
              <div style={{ fontWeight: 700, fontSize: '1rem', color: '#ffffff' }}>
                {booking.showDate}
              </div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                {booking.showTime}
              </div>
            </div>
          </div>

          {/* Seats Box */}
          <div style={{
            background: 'var(--bg-elevated)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '16px 20px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '24px',
            flexWrap: 'wrap',
            gap: '12px',
          }}>
            <div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>
                Reserved Seats ({booking.seats.length})
              </div>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {booking.seats.map((seat) => (
                  <span key={seat.seatId} className="badge badge-crimson" style={{ fontSize: '0.95rem', fontWeight: 800, padding: '4px 10px' }}>
                    {seat.rowLabel}{seat.seatNumber}
                  </span>
                ))}
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>
                Total Paid
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 900, color: 'var(--accent-gold)' }}>
                ₹{booking.totalAmount.toFixed(2)}
              </div>
            </div>
          </div>

          {/* Payment & QR Code Section */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderTop: '1px dashed var(--border-medium)',
            paddingTop: '20px',
            flexWrap: 'wrap',
            gap: '16px',
          }}>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              <div><strong>Payment Ref:</strong> {booking.paymentReference || 'N/A'}</div>
              <div><strong>Method:</strong> {booking.paymentMethod || 'Simulated Money'}</div>
              <div><strong>Status:</strong> <span style={{ color: '#10B981', fontWeight: 700 }}>{booking.paymentStatus || 'PAID'}</span></div>
            </div>

            {/* Visual QR Code simulation */}
            <div style={{
              background: '#ffffff',
              padding: '8px',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <QrCode size={56} color="#000000" />
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Buttons */}
      <div style={{ display: 'flex', gap: '14px', justifyContent: 'center', flexWrap: 'wrap' }}>
        <button
          onClick={() => onNavigate('profile')}
          className="btn btn-secondary"
          style={{ gap: '8px' }}
        >
          <User size={16} />
          View in My Bookings
        </button>

        <button
          onClick={() => onNavigate('movies')}
          className="btn btn-primary"
          style={{ gap: '8px' }}
        >
          <ArrowLeft size={16} />
          Book Another Movie
        </button>
      </div>
    </div>
  );
};
