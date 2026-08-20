import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { providerService } from '../../services/providerService';
import { movieService } from '../../services/movieService';
import { Calendar, Clock, DollarSign, Film, Building2, CheckCircle2, AlertCircle } from 'lucide-react';

export const ShowSchedulerPage = () => {
  const navigate = useNavigate();

  const [movies, setMovies] = useState([]);
  const [selectedMovieId, setSelectedMovieId] = useState('');
  const [screenId, setScreenId] = useState('1'); // Default Auditorium 1
  const [showDate, setShowDate] = useState(new Date().toISOString().split('T')[0]);
  const [startTime, setStartTime] = useState('18:00');
  const [endTime, setEndTime] = useState('20:30');
  const [ticketPrice, setTicketPrice] = useState('15.00');

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    fetchMovies();
  }, []);

  const fetchMovies = async () => {
    setLoading(true);
    try {
      const res = await movieService.getAllMovies();
      if (res.success && res.data && res.data.length > 0) {
        setMovies(res.data);
        setSelectedMovieId(res.data[0].movieId.toString());
      }
    } catch (err) {
      console.error('Failed to load movies:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleScheduleShow = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setMessage('');

    try {
      const showData = {
        showDate,
        startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
        endTime: endTime.length === 5 ? `${endTime}:00` : endTime,
        ticketPrice: parseFloat(ticketPrice),
        status: 'ACTIVE',
      };

      const res = await providerService.createShow(showData, selectedMovieId, screenId);
      if (res.success) {
        setMessage('Show scheduled successfully! Seat inventory generated.');
        setTimeout(() => navigate('/provider/dashboard'), 1500);
      } else {
        setError(res.message || 'Failed to schedule show.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to schedule showtime.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <div className="glass-card p-8 animate-fade-in">
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-indigo-600 flex items-center justify-center mx-auto mb-3 shadow-lg shadow-emerald-500/30">
            <Calendar className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">Schedule Show Session</h1>
          <p className="text-xs text-slate-400 mt-1">
            Create showtimes and auto-generate seat inventory for customer bookings
          </p>
        </div>

        {message && (
          <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center gap-3 text-emerald-400 text-sm font-semibold">
            <CheckCircle2 className="w-5 h-5 shrink-0" />
            <span>{message}</span>
          </div>
        )}

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/30 flex items-center gap-3 text-red-400 text-sm">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <div className="py-12 text-center text-slate-400">Loading catalogue data...</div>
        ) : (
          <form onSubmit={handleScheduleShow}>
            <div className="form-group">
              <label className="form-label">Select Movie</label>
              <select
                className="form-input w-full bg-slate-900 text-white"
                value={selectedMovieId}
                onChange={(e) => setSelectedMovieId(e.target.value)}
              >
                {movies.map((m) => (
                  <option key={m.movieId} value={m.movieId}>
                    {m.title} ({m.language} - {m.duration} mins)
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Auditorium / Screen ID</label>
              <select
                className="form-input w-full bg-slate-900 text-white"
                value={screenId}
                onChange={(e) => setScreenId(e.target.value)}
              >
                <option value="1">Auditorium 1 (Downtown - 30 Seats)</option>
                <option value="2">Auditorium 2 (VIP - 20 Seats)</option>
                <option value="3">IMAX Hall A (Westside - 40 Seats)</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Show Date</label>
              <input
                type="date"
                required
                className="form-input"
                value={showDate}
                onChange={(e) => setShowDate(e.target.value)}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="form-group">
                <label className="form-label">Start Time</label>
                <input
                  type="time"
                  required
                  className="form-input"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">End Time</label>
                <input
                  type="time"
                  required
                  className="form-input"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Base Ticket Price (₹)</label>
              <input
                type="number"
                step="0.50"
                required
                className="form-input"
                value={ticketPrice}
                onChange={(e) => setTicketPrice(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-primary w-full py-3 mt-4 text-sm font-bold justify-center"
            >
              {submitting ? 'Generating Show Seats...' : 'Schedule & Generate Seat Inventory'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
