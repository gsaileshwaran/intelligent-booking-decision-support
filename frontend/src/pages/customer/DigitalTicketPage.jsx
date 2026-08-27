import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { bookingService } from '../../services/bookingService';
import { Ticket, Film, MapPin, Clock, Calendar, CheckCircle2, ArrowLeft, Printer, ShieldCheck } from 'lucide-react';

export const DigitalTicketPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchTicketDetails();
  }, [id]);

  const fetchTicketDetails = async () => {
    setLoading(true);
    try {
      const res = await bookingService.getBookingById(id);
      if (res.success && res.data) {
        setBooking(res.data);
      } else {
        setError('Booking ticket not found or access denied.');
      }
    } catch (err) {
      setError('Failed to fetch digital ticket details.');
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading your confirmed digital ticket...</div>;
  }

  if (error || !booking) {
    return (
      <div className="max-w-md mx-auto my-20 p-8 glass-card text-center">
        <p className="text-red-400 text-sm mb-4">{error || 'Ticket not found.'}</p>
        <button onClick={() => navigate('/my-bookings')} className="btn-secondary text-xs">
          Back to Bookings
        </button>
      </div>
    );
  }

  const { bookingId, bookingRef, movieTitle, theatreName, screenName, showDate, startTime, totalAmount, paymentStatus, status, seats = [] } = booking;

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6 print:hidden">
        <button
          onClick={() => navigate('/my-bookings')}
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to My Bookings
        </button>
        <button onClick={handlePrint} className="btn-secondary text-xs py-2 px-4 flex items-center gap-2">
          <Printer className="w-4 h-4" /> Print / Save PDF
        </button>
      </div>

      {/* Main Ticket Card Container */}
      <div className="glass-card overflow-hidden border border-indigo-500/40 shadow-2xl bg-gradient-to-b from-slate-900 to-indigo-950">
        {/* Ticket Top Header */}
        <div className="p-6 bg-indigo-600/30 border-b border-indigo-500/30 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Ticket className="w-8 h-8 text-indigo-400" />
            <div>
              <span className="text-[10px] font-extrabold text-indigo-300 uppercase tracking-widest block">
                PVK CINEMA OFFICIAL E-TICKET
              </span>
              <h2 className="text-xl font-bold text-white">Ref: {bookingRef || `PVK-${bookingId}`}</h2>
            </div>
          </div>
          <span className="px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-xs font-bold flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4" /> {status}
          </span>
        </div>

        {/* Ticket Body Content */}
        <div className="p-8 space-y-6">
          {/* Movie & Venue Info */}
          <div className="border-b border-slate-800 pb-6">
            <span className="text-xs font-bold text-indigo-400 block uppercase mb-1">Movie Title</span>
            <h1 className="text-3xl font-extrabold text-white mb-3">{movieTitle || 'Blockbuster Movie'}</h1>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4 text-indigo-400 shrink-0" />
                <div>
                  <span className="text-slate-500 block text-[10px]">Cinema Branch</span>
                  <strong className="text-white">{theatreName} &bull; {screenName}</strong>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-indigo-400 shrink-0" />
                <div>
                  <span className="text-slate-500 block text-[10px]">Show Date & Showtime</span>
                  <strong className="text-white">{showDate} at {startTime}</strong>
                </div>
              </div>
            </div>
          </div>

          {/* Seat Breakdown */}
          <div className="border-b border-slate-800 pb-6">
            <span className="text-xs font-bold text-slate-400 block uppercase mb-3">Reserved Seat(s)</span>
            <div className="flex flex-wrap gap-3">
              {seats.map((st, idx) => (
                <div key={idx} className="px-4 py-2 rounded-xl bg-indigo-500/10 border border-indigo-500/30 text-xs text-indigo-300 font-bold">
                  Row {st.rowLabel || st.seat?.rowLabel} Seat {st.seatNumber || st.seat?.seatNumber} ({st.seatType || st.seat?.seatType || 'REGULAR'})
                </div>
              ))}
            </div>
          </div>

          {/* QR Code Placeholder & Entry Rules */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-6 bg-slate-900/80 p-6 rounded-2xl border border-slate-800">
            <div className="space-y-1 text-xs">
              <span className="text-slate-400 font-semibold flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-400" /> Digital Entry Code
              </span>
              <p className="text-slate-300 text-[11px]">Show this barcode / QR code at the usher gate for instant entry.</p>
              <p className="text-slate-500 text-[10px] mt-1">Payment Status: <strong className="text-emerald-400 uppercase">{paymentStatus}</strong> &bull; Total Paid: <strong className="text-white">₹{totalAmount?.toFixed(2)}</strong></p>
            </div>

            {/* Generated QR Representation */}
            <div className="w-28 h-28 bg-white p-2 rounded-xl flex items-center justify-center shrink-0 shadow-lg">
              <img
                src={`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=PVK-BOOKING-${bookingRef || bookingId}`}
                alt="Ticket QR Code"
                className="w-full h-full object-contain"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
