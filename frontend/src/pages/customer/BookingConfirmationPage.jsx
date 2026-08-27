import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { bookingService } from '../../services/bookingService';
import { useBooking } from '../../context/BookingContext';
import { CheckCircle2, Clock, ShieldCheck, CreditCard, Ticket, AlertCircle, Tag, Check } from 'lucide-react';

export const BookingConfirmationPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { activeBooking, clearBookingState } = useBooking();

  const [booking, setBooking] = useState(activeBooking || null);
  const [paymentMethod, setPaymentMethod] = useState('MOCK_CARD');
  const [confirming, setConfirming] = useState(false);
  const [confirmedSuccess, setConfirmedSuccess] = useState(false);
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(600);

  // Promo Code State
  const [promoCodeInput, setPromoCodeInput] = useState('');
  const [appliedPromoCode, setAppliedPromoCode] = useState('');
  const [discountAmount, setDiscountAmount] = useState(0);
  const [promoMessage, setPromoMessage] = useState('');

  useEffect(() => {
    fetchBookingDetails();
  }, [id]);

  useEffect(() => {
    if (!booking || booking.status !== 'HELD') return;

    let initialSeconds = 600;
    if (booking.holdExpiresAt) {
      const expiry = new Date(booking.holdExpiresAt).getTime();
      const now = new Date().getTime();
      initialSeconds = Math.max(0, Math.floor((expiry - now) / 1000));
    } else if (booking.createdAt) {
      const created = new Date(booking.createdAt).getTime();
      const expiry = created + 10 * 60 * 1000;
      const now = new Date().getTime();
      initialSeconds = Math.max(0, Math.floor((expiry - now) / 1000));
    }

    setTimeLeft(initialSeconds);

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [booking?.bookingId, booking?.holdExpiresAt]);

  const fetchBookingDetails = async () => {
    try {
      const res = await bookingService.getBookingById(id);
      if (res.success && res.data) {
        setBooking(res.data);
        if (res.data.status === 'CONFIRMED') {
          setConfirmedSuccess(true);
        }
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Could not retrieve booking details.');
    }
  };

  const handleApplyPromo = async () => {
    if (!promoCodeInput.trim() || !booking) return;
    try {
      const res = await bookingService.validatePromo(promoCodeInput, booking.totalAmount);
      if (res.success && res.data && res.data.isValid) {
        setDiscountAmount(res.data.discountAmount || 0);
        setAppliedPromoCode(res.data.promoCode);
        setPromoMessage(res.data.message);
      } else {
        setPromoMessage(res.data?.message || 'Invalid promo code.');
        setDiscountAmount(0);
        setAppliedPromoCode('');
      }
    } catch (err) {
      setPromoMessage('Invalid promo code or network error.');
    }
  };

  const handlePayment = async () => {
    setConfirming(true);
    setError('');
    try {
      const res = await bookingService.confirmBooking(id, paymentMethod, appliedPromoCode);
      if (res.success) {
        setBooking(res.data);
        setConfirmedSuccess(true);
        clearBookingState();
      } else {
        setError(res.message || 'Payment confirmation failed.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Payment failed or seat hold expired.');
    } finally {
      setConfirming(false);
    }
  };

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  if (!booking) {
    return <div className="py-20 text-center text-slate-400">Loading booking summary...</div>;
  }

  const baseTotal = booking.totalAmount || 0;
  const finalPayable = Math.max(0, baseTotal - discountAmount);

  return (
    <div className="max-w-3xl mx-auto px-4 py-12">
      {confirmedSuccess ? (
        /* Confirmed Ticket View */
        <div className="glass-card p-8 sm:p-12 text-center animate-fade-in border-emerald-500/30">
          <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center mx-auto mb-6 text-emerald-400">
            <CheckCircle2 className="w-10 h-10" />
          </div>
          <span className="badge badge-confirmed mb-2">Booking Confirmed</span>
          <h1 className="text-3xl font-extrabold text-white mb-2">Ticket Reservation Confirmed!</h1>
          <p className="text-sm text-slate-300 mb-8">
            Your booking reference is <strong className="text-indigo-300 font-mono">{booking.bookingRef}</strong>
          </p>

          <div className="glass-card p-6 text-left mb-8 bg-slate-900/80">
            <div className="grid grid-cols-2 gap-4 text-xs border-b border-slate-800 pb-4 mb-4">
              <div>
                <span className="block text-slate-500">Movie</span>
                <span className="font-bold text-white text-sm">{booking.movieTitle}</span>
              </div>
              <div>
                <span className="block text-slate-500">Theatre Venue</span>
                <span className="font-bold text-white text-sm">{booking.theatreName}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div>
                <span className="block text-slate-500">Reserved Seats</span>
                <div className="flex flex-wrap gap-1 mt-1">
                  {booking.seats?.map((s) => (
                    <span key={s.showSeatId} className="px-2 py-0.5 rounded bg-slate-800 text-indigo-300 font-bold">
                      {s.rowLabel}{s.seatNumber}
                    </span>
                  ))}
                </div>
              </div>
              <div>
                <span className="block text-slate-500">Total Paid</span>
                <span className="font-extrabold text-emerald-400 text-base">₹{booking.totalAmount?.toFixed(2)}</span>
              </div>
            </div>
          </div>

          <div className="flex justify-center gap-4">
            <button onClick={() => navigate(`/tickets/${booking.bookingId}`)} className="btn-primary text-xs font-bold">
              <Ticket className="w-4 h-4" /> Open Digital E-Ticket
            </button>
            <button onClick={() => navigate('/my-bookings')} className="btn-secondary text-xs">
              My Bookings
            </button>
          </div>
        </div>
      ) : (
        /* Seat Hold & Payment Confirmation */
        <div className="glass-card p-8 animate-fade-in">
          <div className="flex items-center justify-between border-b border-slate-800 pb-6 mb-6">
            <div>
              <span className="badge badge-held mb-1">Temporary Hold</span>
              <h1 className="text-2xl font-bold text-white">Review & Complete Payment</h1>
            </div>
            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-bold ${
              timeLeft === 0
                ? 'text-red-400 bg-red-500/10 border-red-500/30'
                : 'text-amber-400 bg-amber-500/10 border-amber-500/20'
            }`}>
              <Clock className="w-4 h-4" />
              <span>{timeLeft === 0 ? 'Hold Expired' : `Hold Expires: ${formatTimer(timeLeft)}`}</span>
            </div>
          </div>

          {error && (
            <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-red-400 text-sm">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {timeLeft === 0 && (
            <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-red-400 text-sm">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>Seat hold reservation has expired. Please reselect available seats.</span>
            </div>
          )}

          {/* Booking Summary Box */}
          <div className="bg-slate-900/60 rounded-xl p-6 mb-6 border border-slate-800">
            <div className="flex justify-between items-center mb-4">
              <div>
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Booking Ref</span>
                <span className="block font-mono font-bold text-indigo-300 text-sm">{booking.bookingRef}</span>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Total Payable</span>
                <span className="block font-extrabold text-emerald-400 text-lg">₹{finalPayable.toFixed(2)}</span>
              </div>
            </div>

            <div className="border-t border-slate-800/80 pt-4 text-xs space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-400">Movie Title:</span>
                <span className="font-semibold text-white">{booking.movieTitle || 'Movie'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Theatre / Screen:</span>
                <span className="font-semibold text-white">{booking.theatreName} ({booking.screenName})</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Selected Seats:</span>
                <div className="flex gap-1">
                  {booking.seats?.map((s) => (
                    <span key={s.showSeatId} className="px-1.5 py-0.5 bg-slate-800 text-indigo-300 font-bold rounded">
                      {s.rowLabel}{s.seatNumber} ({s.seatType})
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Subtotal & Promo Discount Row */}
            {discountAmount > 0 && (
              <div className="mt-4 pt-3 border-t border-slate-800 text-xs flex justify-between items-center text-amber-300 bg-amber-500/10 p-2.5 rounded-lg">
                <span className="font-bold flex items-center gap-1">
                  <Tag className="w-3.5 h-3.5" /> Applied Code: {appliedPromoCode}
                </span>
                <span className="font-bold font-mono">-₹{discountAmount.toFixed(2)}</span>
              </div>
            )}
          </div>

          {/* Promo Code Input Box */}
          <div className="mb-8 p-4 rounded-xl bg-slate-900/60 border border-slate-800">
            <label className="form-label text-xs mb-2 block flex items-center gap-1.5">
              <Tag className="w-3.5 h-3.5 text-amber-400" /> Apply Promotional Voucher Code
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Enter promo code (e.g. PVKWEEKEND, FIRSTBOOK)"
                className="form-input text-xs uppercase"
                value={promoCodeInput}
                onChange={(e) => setPromoCodeInput(e.target.value)}
              />
              <button
                type="button"
                onClick={handleApplyPromo}
                className="btn-secondary text-xs px-4 font-bold shrink-0"
              >
                Apply Promo
              </button>
            </div>
            {promoMessage && (
              <p className={`text-[11px] mt-2 font-semibold ${discountAmount > 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                {promoMessage}
              </p>
            )}
          </div>

          {/* Payment Method Selection */}
          <div className="mb-8">
            <label className="form-label mb-3 block text-xs">Select Payment Method</label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <button
                type="button"
                onClick={() => setPaymentMethod('MOCK_CARD')}
                className={`p-4 rounded-xl border text-left flex flex-col gap-2 transition-all ${
                  paymentMethod === 'MOCK_CARD'
                    ? 'bg-indigo-600/20 border-indigo-500 text-white'
                    : 'bg-slate-900/40 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <CreditCard className="w-5 h-5 text-indigo-400" />
                <span className="text-xs font-bold">Credit/Debit Card (Sandbox)</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentMethod('UPI')}
                className={`p-4 rounded-xl border text-left flex flex-col gap-2 transition-all ${
                  paymentMethod === 'UPI'
                    ? 'bg-indigo-600/20 border-indigo-500 text-white'
                    : 'bg-slate-900/40 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <ShieldCheck className="w-5 h-5 text-purple-400" />
                <span className="text-xs font-bold">UPI / Instant QR</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentMethod('NETBANKING')}
                className={`p-4 rounded-xl border text-left flex flex-col gap-2 transition-all ${
                  paymentMethod === 'NETBANKING'
                    ? 'bg-indigo-600/20 border-indigo-500 text-white'
                    : 'bg-slate-900/40 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <Ticket className="w-5 h-5 text-emerald-400" />
                <span className="text-xs font-bold">Netbanking</span>
              </button>
            </div>
          </div>

          <div className="flex gap-4">
            {timeLeft === 0 ? (
              <button
                onClick={() => navigate('/movies')}
                className="btn-secondary w-full py-3 text-xs font-bold justify-center"
              >
                Back to Movies
              </button>
            ) : (
              <button
                disabled={confirming || timeLeft === 0}
                onClick={handlePayment}
                className="btn-primary w-full py-3 text-sm font-bold justify-center"
              >
                {confirming ? 'Processing Transaction...' : `Confirm & Pay ₹${finalPayable.toFixed(2)}`}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
