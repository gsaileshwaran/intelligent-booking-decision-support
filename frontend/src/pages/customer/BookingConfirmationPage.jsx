import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { bookingService } from '../../services/bookingService';
import { useBooking } from '../../context/BookingContext';
import { CheckCircle2, Clock, ShieldCheck, CreditCard, Ticket, AlertCircle } from 'lucide-react';

export const BookingConfirmationPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { activeBooking, clearBookingState } = useBooking();

  const [booking, setBooking] = useState(activeBooking || null);
  const [paymentMethod, setPaymentMethod] = useState('MOCK_CARD');
  const [confirming, setConfirming] = useState(false);
  const [confirmedSuccess, setConfirmedSuccess] = useState(false);
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(600); // 10 minutes hold timer in seconds

  useEffect(() => {
    if (!booking) {
      // Fetch booking from history/details if direct URL visit
      fetchBookingDetails();
    }
  }, [id]);

  useEffect(() => {
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
  }, []);

  const fetchBookingDetails = async () => {
    try {
      const res = await bookingService.getMyHistory();
      if (res.success && res.data) {
        const found = res.data.find((b) => b.bookingId === Number(id));
        if (found) setBooking(found);
      }
    } catch (err) {
      setError('Could not retrieve booking details.');
    }
  };

  const handlePayment = async () => {
    setConfirming(true);
    setError('');
    try {
      const res = await bookingService.confirmBooking(id, paymentMethod);
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
                <span className="font-extrabold text-emerald-400 text-base">${booking.totalAmount?.toFixed(2)}</span>
              </div>
            </div>
          </div>

          <div className="flex justify-center gap-4">
            <button onClick={() => navigate('/my-bookings')} className="btn-primary text-xs">
              <Ticket className="w-4 h-4" /> View My Bookings History
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
            <div className="flex items-center gap-2 text-amber-400 bg-amber-500/10 px-3 py-1.5 rounded-lg border border-amber-500/20 text-xs font-bold">
              <Clock className="w-4 h-4" />
              <span>Hold Expires: {formatTimer(timeLeft)}</span>
            </div>
          </div>

          {error && (
            <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-red-400 text-sm">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Booking Summary Box */}
          <div className="bg-slate-900/60 rounded-xl p-6 mb-8 border border-slate-800">
            <div className="flex justify-between items-center mb-4">
              <div>
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Booking Ref</span>
                <span className="block font-mono font-bold text-indigo-300 text-sm">{booking.bookingRef}</span>
              </div>
              <div className="text-right">
                <span className="text-[10px] text-slate-500 uppercase font-semibold">Total Amount</span>
                <span className="block font-extrabold text-emerald-400 text-lg">${booking.totalAmount?.toFixed(2)}</span>
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
                      {s.rowLabel}{s.seatNumber}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Payment Method Selection */}
          <div className="mb-8">
            <label className="form-label mb-3 block">Select Payment Integration</label>
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

          <button
            disabled={confirming || timeLeft === 0}
            onClick={handlePayment}
            className="btn-primary w-full py-3 text-sm font-bold justify-center"
          >
            {confirming ? 'Processing Transaction...' : `Confirm & Pay $${booking.totalAmount?.toFixed(2)}`}
          </button>
        </div>
      )}
    </div>
  );
};
