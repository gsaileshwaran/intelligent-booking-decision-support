import React from 'react';
import { X, Clock, MapPin, Ticket, Building2, ShieldCheck, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const OptionComparisonModal = ({ isOpen, onClose, shows = [] }) => {
  const navigate = useNavigate();

  if (!isOpen || !shows || shows.length === 0) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md">
      <div className="glass-card w-full max-w-4xl max-h-[90vh] overflow-y-auto border border-indigo-500/40 p-6 sm:p-8">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
          <div>
            <span className="badge badge-held mb-1">CONVENTIONAL COMPARISON</span>
            <h2 className="text-2xl font-bold text-white">Compare Showtime & Venue Options</h2>
            <p className="text-xs text-slate-400">Evaluate showtimes side-by-side to choose the best option</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Comparison Grid */}
        <div className={`grid grid-cols-1 md:grid-cols-${Math.min(shows.length, 3)} gap-6`}>
          {shows.slice(0, 3).map((show, idx) => (
            <div
              key={show.showId || idx}
              className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 hover:border-indigo-500/40 transition-all flex flex-col justify-between"
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="px-2.5 py-0.5 rounded-full bg-indigo-500/10 text-indigo-300 text-[10px] font-bold">
                    Option {idx + 1}
                  </span>
                  <span className="text-xs font-extrabold text-emerald-400">₹{show.ticketPrice?.toFixed(2)} Base</span>
                </div>

                <div>
                  <h4 className="text-base font-bold text-white flex items-center gap-1.5 mb-1">
                    <Building2 className="w-4 h-4 text-indigo-400" />
                    {show.screen?.theatre?.name || 'PVK Venue'}
                  </h4>
                  <p className="text-xs text-slate-400 flex items-center gap-1">
                    <MapPin className="w-3 h-3 text-slate-500" />
                    {show.screen?.theatre?.location || 'Chennai'}
                  </p>
                </div>

                <div className="space-y-2 text-xs border-t border-slate-800 pt-3">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Screen Auditorium:</span>
                    <strong className="text-slate-200">{show.screen?.name}</strong>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Showtime:</span>
                    <strong className="text-indigo-300 font-mono">{show.startTime} - {show.endTime}</strong>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Show Date:</span>
                    <strong className="text-slate-200">{show.showDate}</strong>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Seat Tiers:</span>
                    <strong className="text-purple-300">Regular / Premium / Balcony</strong>
                  </div>
                </div>
              </div>

              <button
                onClick={() => {
                  onClose();
                  navigate(`/shows/${show.showId}/seats`);
                }}
                className="btn-primary w-full mt-6 py-2.5 text-xs font-bold justify-center"
              >
                Select Seats <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
