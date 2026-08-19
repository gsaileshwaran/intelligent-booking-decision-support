import React, { useState, useEffect } from 'react';
import { providerService } from '../../services/providerService';
import { movieService } from '../../services/movieService';
import { Film, Plus, Clock, Calendar, CheckCircle } from 'lucide-react';

export const MovieManagerPage = () => {
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form state
  const [showModal, setShowModal] = useState(false);
  const [title, setTitle] = useState('');
  const [genre, setGenre] = useState('');
  const [language, setLanguage] = useState('English');
  const [duration, setDuration] = useState(120);
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
      if (res.success && res.data) {
        setMovies(res.data);
      }
    } catch (err) {
      console.error('Failed to load movies:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateMovie = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setMessage('');

    try {
      const res = await providerService.createMovie({ title, genre, language, duration: Number(duration), status: 'ACTIVE' });
      if (res.success) {
        setMessage('Movie added to catalogue successfully!');
        setShowModal(false);
        setTitle('');
        setGenre('');
        fetchMovies();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add movie.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-extrabold text-white">Movie Catalogue Builder</h1>
          <p className="text-xs text-slate-400 mt-1">Manage film releases, durations, languages, and censor classifications</p>
        </div>

        <button onClick={() => setShowModal(true)} className="btn-primary text-xs">
          <Plus className="w-4 h-4" /> Add New Movie
        </button>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
          {message}
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading catalogue...</div>
      ) : movies.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Film className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No Movies in Catalogue</h3>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {movies.map((m) => (
            <div key={m.movieId} className="glass-card p-5 flex flex-col justify-between">
              <div>
                <span className="badge badge-available mb-2">{m.status || 'ACTIVE'}</span>
                <h3 className="text-lg font-bold text-white mb-2">{m.title}</h3>
                <div className="text-xs text-slate-400 space-y-1">
                  <p>Genre: <span className="text-slate-200">{m.genre || 'General'}</span></p>
                  <p>Language: <span className="text-slate-200">{m.language || 'English'}</span></p>
                  <p>Duration: <span className="text-slate-200">{m.duration} mins</span></p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Movie Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="glass-card w-full max-w-md p-6 animate-fade-in">
            <h2 className="text-xl font-bold text-white mb-4">Add Movie to Catalogue</h2>

            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
                {error}
              </div>
            )}

            <form onSubmit={handleCreateMovie}>
              <div className="form-group">
                <label className="form-label">Movie Title</label>
                <input
                  type="text"
                  required
                  placeholder="Cyber Odyssey 2099"
                  className="form-input"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Genre</label>
                <input
                  type="text"
                  required
                  placeholder="Sci-Fi / Action"
                  className="form-input"
                  value={genre}
                  onChange={(e) => setGenre(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="form-group">
                  <label className="form-label">Language</label>
                  <input
                    type="text"
                    required
                    className="form-input"
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Duration (Mins)</label>
                  <input
                    type="number"
                    required
                    className="form-input"
                    value={duration}
                    onChange={(e) => setDuration(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex gap-3 mt-6">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="btn-secondary w-full text-xs py-2.5"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn-primary w-full text-xs py-2.5"
                >
                  {submitting ? 'Saving...' : 'Add Movie'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
