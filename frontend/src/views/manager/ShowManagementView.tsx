import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, AlertTriangle, Check, X } from 'lucide-react';
import { managerApi, moviesApi } from '../../api/client';
import type { Show, ShowRequest } from '../../types/show';
import type { Screen } from '../../types/theatre';
import type { Movie, Language } from '../../types/movie';
import { useToast } from '../../context/ToastContext';

interface ShowManagementViewProps {
  theatreId: number;
  onNavigate: (view: string, param?: any) => void;
}

export const ShowManagementView: React.FC<ShowManagementViewProps> = ({ theatreId, onNavigate }) => {
  const { showToast } = useToast();
  const [shows, setShows] = useState<Show[]>([]);
  const [screens, setScreens] = useState<Screen[]>([]);
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);

  // Scheduling Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedMovieId, setSelectedMovieId] = useState<number | ''>('');
  const [movieLanguages, setMovieLanguages] = useState<Language[]>([]);
  const [selectedLanguageId, setSelectedLanguageId] = useState<number | ''>('');
  const [selectedScreenId, setSelectedScreenId] = useState<number | ''>('');
  const [startAtString, setStartAtString] = useState<string>('');
  const [endAtString, setEndAtString] = useState<string>('');
  const [overlapWarning, setOverlapWarning] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [showsData, screensData, moviesData] = await Promise.all([
        managerApi.getShows(theatreId),
        managerApi.getScreens(theatreId),
        moviesApi.getMovies({ status: 'AIRING', size: 100 }),
      ]);
      setShows(showsData || []);
      setScreens(screensData || []);
      setMovies(moviesData.content || []);
    } catch (err: any) {
      console.error('Failed to load shows data', err);
      showToast(err.message || 'Failed to load scheduling data', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [theatreId]);

  // When movie changes, fetch its languages
  useEffect(() => {
    if (selectedMovieId) {
      moviesApi.getMovieLanguages(Number(selectedMovieId))
        .then((langs) => {
          setMovieLanguages(langs || []);
          if (langs && langs.length > 0) {
            setSelectedLanguageId(langs[0].movieLanguageId || langs[0].languageId);
          }
        })
        .catch((err) => console.error('Failed to load movie languages', err));
    }
  }, [selectedMovieId]);

  // IMP-010: Auto-calculate end time from movie runtime whenever movie or start time changes
  useEffect(() => {
    if (!selectedMovieId || !startAtString) return;
    const selectedMovie = movies.find((m) => m.movieId === Number(selectedMovieId));
    if (!selectedMovie || !selectedMovie.runtimeMinutes) return;
    const startDate = new Date(startAtString);
    if (isNaN(startDate.getTime())) return;
    // End = start + runtime + 30 min buffer for ads/cleaning
    const endDate = new Date(startDate.getTime() + (selectedMovie.runtimeMinutes + 30) * 60 * 1000);
    const endIso = endDate.toISOString().slice(0, 16);
    setEndAtString(endIso);
  }, [selectedMovieId, startAtString, movies]);

  // BR-004 Conflict Overlap Pre-Validation Check
  useEffect(() => {
    if (!selectedScreenId || !startAtString || !endAtString) {
      setOverlapWarning(null);
      return;
    }

    const proposedStart = new Date(startAtString).getTime();
    const proposedEnd = new Date(endAtString).getTime();

    if (proposedEnd <= proposedStart) {
      setOverlapWarning('Show end time must be after the start time.');
      return;
    }

    const conflict = shows.find((s) => {
      if (s.screenId !== Number(selectedScreenId) || s.showStatus === 'CANCELLED') return false;
      const existingStart = new Date(s.startAt).getTime();
      const existingEnd = new Date(s.endAt).getTime();
      // Overlap condition: proposedStart < existingEnd && proposedEnd > existingStart
      return proposedStart < existingEnd && proposedEnd > existingStart;
    });

    if (conflict) {
      setOverlapWarning(
        `Scheduling Conflict Detected (BR-004): Overlaps with existing show #${conflict.showId} (${conflict.movieTitle}) from ${new Date(conflict.startAt).toLocaleTimeString()} to ${new Date(conflict.endAt).toLocaleTimeString()}`
      );
    } else {
      setOverlapWarning(null);
    }
  }, [selectedScreenId, startAtString, endAtString, shows]);

  const handleOpenAdd = () => {
    if (movies.length > 0) setSelectedMovieId(movies[0].movieId);
    if (screens.length > 0) setSelectedScreenId(screens[0].screenId);

    // Default start 2 hours from now, end 4.5 hours from now
    const now = new Date();
    now.setHours(now.getHours() + 2, 0, 0, 0);
    const startIso = now.toISOString().slice(0, 16);
    now.setHours(now.getHours() + 2, 30, 0, 0);
    const endIso = now.toISOString().slice(0, 16);

    setStartAtString(startIso);
    setEndAtString(endIso);
    setIsModalOpen(true);
  };

  const handleScheduleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMovieId || !selectedLanguageId || !selectedScreenId || !startAtString || !endAtString) {
      showToast('Please fill out all required fields.', 'info');
      return;
    }

    try {
      setSubmitting(true);
      const targetScreen = screens.find((s) => s.screenId === Number(selectedScreenId));
      const payload: ShowRequest = {
        movieId: Number(selectedMovieId),
        movieLanguageId: Number(selectedLanguageId),
        screenId: Number(selectedScreenId),
        screenCapabilityId: targetScreen?.screenCapabilityId || 1,
        startAt: new Date(startAtString).toISOString(),
        endAt: new Date(endAtString).toISOString(),
        showStatus: 'SCHEDULED',
      };

      await managerApi.createShow(theatreId, payload);
      showToast('Show scheduled successfully!', 'success');
      setIsModalOpen(false);
      await loadData();
    } catch (err: any) {
      console.error('Failed to schedule show', err);
      showToast(err.message || 'Failed to schedule show (Conflict or Invalid Payload)', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="show-management-view">
      <div>
        <button
          onClick={() => onNavigate('manager-dashboard')}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Manager Dashboard
        </button>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Show Scheduler</h1>
          <p>Schedule screenings with automated BR-004 screen collision detection</p>
        </div>

        <button onClick={handleOpenAdd} className="btn btn-primary" style={{ gap: '6px' }}>
          <Plus size={16} />
          Schedule New Show
        </button>
      </div>

      {/* Shows List Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none', borderRadius: 0 }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Movie Title</th>
                <th>Auditorium</th>
                <th>Language</th>
                <th>Start Time</th>
                <th>End Time</th>
                <th>Status</th>
                <th>Seat Preview</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading scheduled screenings...
                  </td>
                </tr>
              ) : shows.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No shows currently scheduled. Click "Schedule New Show" above.
                  </td>
                </tr>
              ) : (
                shows.map((show) => (
                  <tr key={show.showId}>
                    <td><code>#{show.showId}</code></td>
                    <td style={{ fontWeight: 600 }}>{show.movieTitle}</td>
                    <td>{show.screenName}</td>
                    <td>{show.languageName || 'Standard'}</td>
                    <td>{new Date(show.startAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</td>
                    <td>{new Date(show.endAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</td>
                    <td>
                      <span className={`badge ${show.showStatus === 'SCHEDULED' ? 'badge-emerald' : 'badge-slate'}`}>
                        {show.showStatus}
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => onNavigate('manager-show-seats', show.showId)}
                        className="btn btn-sm btn-secondary"
                        style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                      >
                        Inspect Seats
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Schedule Show Modal */}
      {isModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content" style={{ maxWidth: '640px' }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem' }}>Schedule New Screening</h3>
              <button onClick={() => setIsModalOpen(false)} className="btn-icon" aria-label="Close modal">
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleScheduleSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {overlapWarning && (
                  <div className="banner-warning" style={{ fontSize: '0.88rem' }}>
                    <AlertTriangle size={18} style={{ flexShrink: 0 }} />
                    <span>{overlapWarning}</span>
                  </div>
                )}

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="show-movie">Select Movie</label>
                  <select
                    id="show-movie"
                    required
                    value={selectedMovieId}
                    onChange={(e) => setSelectedMovieId(e.target.value ? parseInt(e.target.value, 10) : '')}
                    className="select"
                  >
                    <option value="">Choose a movie...</option>
                    {movies.map((m) => (
                      <option key={m.movieId} value={m.movieId}>
                        {m.title} ({m.runtimeMinutes}m)
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="show-lang">Audio Language</label>
                    <select
                      id="show-lang"
                      required
                      value={selectedLanguageId}
                      onChange={(e) => setSelectedLanguageId(e.target.value ? parseInt(e.target.value, 10) : '')}
                      className="select"
                    >
                      <option value="">Select language...</option>
                      {movieLanguages.map((l) => (
                        <option key={l.movieLanguageId || l.languageId} value={l.movieLanguageId || l.languageId}>
                          {l.languageName}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="show-screen">Auditorium Screen</label>
                    <select
                      id="show-screen"
                      required
                      value={selectedScreenId}
                      onChange={(e) => setSelectedScreenId(e.target.value ? parseInt(e.target.value, 10) : '')}
                      className="select"
                    >
                      <option value="">Select screen...</option>
                      {screens.map((sc) => (
                        <option key={sc.screenId} value={sc.screenId}>
                          {sc.screenName}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="show-start">Start Time</label>
                    <input
                      id="show-start"
                      type="datetime-local"
                      required
                      value={startAtString}
                      onChange={(e) => setStartAtString(e.target.value)}
                      className="input"
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="show-end">
                      End Time
                      {selectedMovieId && startAtString && (
                        <span style={{ marginLeft: '8px', fontSize: '0.7rem', color: 'var(--accent-gold)', fontWeight: 400 }}>
                          ✨ auto-calculated
                        </span>
                      )}
                    </label>
                    <input
                      id="show-end"
                      type="datetime-local"
                      required
                      value={endAtString}
                      onChange={(e) => setEndAtString(e.target.value)}
                      className="input"
                      style={selectedMovieId && startAtString ? { borderColor: 'var(--accent-gold)', background: 'rgba(234,179,8,0.05)' } : undefined}
                    />
                    {selectedMovieId && startAtString && (() => {
                      const m = movies.find(mv => mv.movieId === Number(selectedMovieId));
                      return m ? (
                        <span style={{ fontSize: '0.73rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
                          {m.runtimeMinutes}m runtime + 30m buffer
                        </span>
                      ) : null;
                    })()}
                  </div>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting || !!overlapWarning}
                  className="btn btn-primary btn-sm"
                  style={{ gap: '6px' }}
                >
                  <Check size={14} />
                  {submitting ? 'Scheduling...' : 'Confirm Schedule'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
