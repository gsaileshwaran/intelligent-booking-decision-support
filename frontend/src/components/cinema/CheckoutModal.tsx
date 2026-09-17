import React, { useState } from 'react';
import { X, CreditCard, Smartphone, Wallet, ShieldCheck, AlertCircle, Clock } from 'lucide-react';
import type { Show } from '../../types/show';
import type { BookingResponse } from '../../types/booking';
import { bookingApi } from '../../api/client';

interface CheckoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  show: Show;
  selectedSeats: { seatId: number; rowLabel: string; seatNumber: string; seatType: string }[];
  holdToken: string;
  expiresInSeconds: number;
  totalAmount: number;
  onPaymentSuccess: (booking: BookingResponse) => void;
  onPaymentFailure: (errorMessage: string) => void;
}

export const CheckoutModal: React.FC<CheckoutModalProps> = ({
  isOpen,
  onClose,
  show,
  selectedSeats,
  holdToken,
  expiresInSeconds,
  totalAmount,
  onPaymentSuccess,
  onPaymentFailure,
}) => {
  const [paymentMethod, setPaymentMethod] = useState<'DUMMY_CARD' | 'DUMMY_UPI' | 'TEST_WALLET'>('DUMMY_CARD');
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleCheckout = async (outcome: 'SUCCESS' | 'FAILED') => {
    try {
      setIsProcessing(true);
      setError(null);
      const booking = await bookingApi.checkout({
        holdToken,
        paymentMethod,
        simulateOutcome: outcome,
      });
      onPaymentSuccess(booking);
    } catch (err: any) {
      console.error('Checkout failed', err);
      const msg = err.message || 'Payment processing failed';
      setError(msg);
      if (outcome === 'FAILED') {
        onPaymentFailure(msg);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="modal-overlay" style={{ zIndex: 1100 }}>
      <div className="card" style={{
        maxWidth: '540px',
        width: '100%',
        padding: '32px',
        maxHeight: '90vh',
        overflowY: 'auto',
        position: 'relative',
        animation: 'scaleIn 0.18s ease-out',
      }}>
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isProcessing}
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
          }}
          aria-label="Close"
        >
          <X size={20} />
        </button>

        {/* Modal Header */}
        <div style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent-crimson)', marginBottom: '4px' }}>
            <ShieldCheck size={18} />
            <span style={{ fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Simulated Express Checkout
            </span>
          </div>
          <h2 style={{ fontSize: '1.6rem', margin: 0 }}>Confirm & Pay</h2>
        </div>

        {/* Live Hold Timer Pill */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(245, 158, 11, 0.12)',
          border: '1px solid rgba(245, 158, 11, 0.3)',
          borderRadius: 'var(--radius-md)',
          padding: '10px 16px',
          marginBottom: '20px',
          color: '#fbbf24',
          fontSize: '0.9rem',
          fontWeight: 600,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={16} className="animate-spin" />
            <span>Temporary Seat Hold Active</span>
          </div>
          <span style={{ fontFamily: 'monospace', fontSize: '1.05rem', fontWeight: 800 }}>
            {formatTimer(expiresInSeconds)}
          </span>
        </div>

        {/* Order Summary Box */}
        <div style={{
          background: 'var(--bg-elevated)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          padding: '16px',
          marginBottom: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
        }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: '1.1rem', color: '#ffffff' }}>{show.movieTitle}</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              {show.theatreName} &bull; {show.screenName}
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {new Date(show.startAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}
            </div>
          </div>

          <div style={{ borderTop: '1px dashed var(--border-subtle)', paddingTop: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
              Seats ({selectedSeats.length}):
            </span>
            <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
              {selectedSeats.map((s) => (
                <span key={s.seatId} className="badge badge-crimson" style={{ fontWeight: 700 }}>
                  {s.rowLabel}{s.seatNumber}
                </span>
              ))}
            </div>
          </div>

          <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 600, fontSize: '1rem' }}>Total Amount</span>
            <span style={{ fontSize: '1.4rem', fontWeight: 900, color: 'var(--accent-gold)' }}>
              ₹{totalAmount.toFixed(2)}
            </span>
          </div>
        </div>

        {/* Simulated Payment Notice */}
        <div style={{
          background: 'rgba(59, 130, 246, 0.08)',
          border: '1px solid rgba(59, 130, 246, 0.25)',
          borderRadius: 'var(--radius-md)',
          padding: '10px 14px',
          marginBottom: '20px',
          fontSize: '0.82rem',
          color: '#93c5fd',
          lineHeight: 1.4,
        }}>
          <strong>Demo Mode:</strong> No real bank transactions. Choose an option below to test deterministic success or failure outcomes.
        </div>

        {/* Payment Method Selector */}
        <div style={{ marginBottom: '24px' }}>
          <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '10px' }}>
            Select Dummy Payment Method
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '10px' }}>
            <button
              type="button"
              onClick={() => setPaymentMethod('DUMMY_CARD')}
              className={`btn btn-sm ${paymentMethod === 'DUMMY_CARD' ? 'btn-primary' : 'btn-outline'}`}
              style={{ flexDirection: 'column', gap: '6px', padding: '12px 6px', height: 'auto' }}
            >
              <CreditCard size={18} />
              <span style={{ fontSize: '0.75rem' }}>Dummy Card</span>
            </button>

            <button
              type="button"
              onClick={() => setPaymentMethod('DUMMY_UPI')}
              className={`btn btn-sm ${paymentMethod === 'DUMMY_UPI' ? 'btn-primary' : 'btn-outline'}`}
              style={{ flexDirection: 'column', gap: '6px', padding: '12px 6px', height: 'auto' }}
            >
              <Smartphone size={18} />
              <span style={{ fontSize: '0.75rem' }}>Dummy UPI</span>
            </button>

            <button
              type="button"
              onClick={() => setPaymentMethod('TEST_WALLET')}
              className={`btn btn-sm ${paymentMethod === 'TEST_WALLET' ? 'btn-primary' : 'btn-outline'}`}
              style={{ flexDirection: 'column', gap: '6px', padding: '12px 6px', height: 'auto' }}
            >
              <Wallet size={18} />
              <span style={{ fontSize: '0.75rem' }}>Test Wallet</span>
            </button>
          </div>
        </div>

        {error && (
          <div style={{
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: 'var(--radius-md)',
            padding: '12px',
            marginBottom: '18px',
            color: '#ef4444',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}>
            <AlertCircle size={16} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {/* Action Buttons */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <button
            type="button"
            disabled={isProcessing || expiresInSeconds <= 0}
            onClick={() => handleCheckout('SUCCESS')}
            className="btn btn-primary"
            style={{ width: '100%', padding: '14px', fontSize: '1rem', fontWeight: 800 }}
          >
            {isProcessing ? 'Processing Payment...' : `Pay ₹${totalAmount.toFixed(2)} (Simulate Success)`}
          </button>

          <button
            type="button"
            disabled={isProcessing || expiresInSeconds <= 0}
            onClick={() => handleCheckout('FAILED')}
            className="btn btn-secondary"
            style={{ width: '100%', padding: '10px', fontSize: '0.85rem', color: '#f87171' }}
          >
            Simulate Payment Failure (Test Rollback)
          </button>
        </div>
      </div>
    </div>
  );
};
