import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, Edit2, Check, X } from 'lucide-react';
import { adminApi, moviesApi } from '../../api/client';
import type { Movie, MovieRequest } from '../../types/movie';
import { useToast } from '../../context/ToastContext';

interface MovieManagementViewProps {
  onNavigate: (view: string) => void;
}

export const MovieManagementView: React.FC<MovieManagementViewProps> = ({ onNavigate }) => {
  const { showToast } = useToast();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingMovie, setEditingMovie] = useState<Movie | null>(null);
  const [title, setTitle] = useState('');
  const [synopsis, setSynopsis] = useState('');
  const [runtimeMinutes, setRuntimeMinutes] = useState<number>(120);
  const [releaseDate, setReleaseDate] = useState('');
  const [posterUrl, setPosterUrl] = useState('');
  const [trailerUrl, setTrailerUrl] = useState('');
  const [movieStatus, setMovieStatus] = useState('AIRING');
  const [certificationId, setCertificationId] = useState<number>(2); // Default U/A
  const [submitting, setSubmitting] = useState(false);

  const loadMovies = async () => {
    try {
      setLoading(true);
      const data = await moviesApi.getMovies({ page, size: 10 });
      setMovies(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: any) {
      console.error('Failed to load movies', err);
      showToast(err.message || 'Failed to load movies', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMovies();
  }, [page]);

  const handleOpenAdd = () => {
    setEditingMovie(null);
    setTitle('');
    setSynopsis('');
    setRuntimeMinutes(120);
    setReleaseDate(new Date().toISOString().slice(0, 10));
    setPosterUrl('');
    setTrailerUrl('');
    setMovieStatus('AIRING');
    setCertificationId(2);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (m: Movie) => {
    setEditingMovie(m);
    setTitle(m.title);
    setSynopsis(m.synopsis || '');
    setRuntimeMinutes(m.runtimeMinutes || 120);
    setReleaseDate(m.releaseDate || new Date().toISOString().slice(0, 10));
    setPosterUrl(m.posterUrl || '');
    setTrailerUrl(m.trailerUrl || '');
    setMovieStatus(m.movieStatus || 'AIRING');
    setCertificationId(m.certificationId || 2);
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      const payload: MovieRequest = {
        title,
        synopsis,
        runtimeMinutes: Number(runtimeMinutes),
        releaseDate,
        posterUrl: posterUrl || undefined,
        trailerUrl: trailerUrl || undefined,
        movieStatus,
        certificationId: Number(certificationId),
      };

      if (editingMovie) {
        await adminApi.updateMovie(editingMovie.movieId, payload);
        showToast('Movie catalogue record updated!', 'success');
      } else {
        await adminApi.createMovie(payload);
        showToast('New movie added to catalogue!', 'success');
      }

      setIsModalOpen(false);
      await loadMovies();
    } catch (err: any) {
      console.error('Failed to save movie', err);
      showToast(err.message || 'Failed to save movie', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="movie-management-view">
      <div>
        <button
          onClick={() => onNavigate('admin-dashboard')}
          className="btn btn-sm btn-outline"
          style={{ gap: '6px' }}
        >
          <ArrowLeft size={16} />
          Back to Admin Dashboard
        </button>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>Movie Catalogue CRUD</h1>
          <p>Create and update global movie metadata, runtime, and certification specifications</p>
        </div>

        <button onClick={handleOpenAdd} className="btn btn-primary" style={{ gap: '6px' }}>
          <Plus size={16} />
          Add Movie to Catalogue
        </button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="table-container" style={{ border: 'none', borderRadius: 0 }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Certification</th>
                <th>Runtime</th>
                <th>Release Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Loading catalogue records...
                  </td>
                </tr>
              ) : movies.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    No movies in catalogue. Click "Add Movie" above.
                  </td>
                </tr>
              ) : (
                movies.map((m) => (
                  <tr key={m.movieId}>
                    <td><code>#{m.movieId}</code></td>
                    <td style={{ fontWeight: 600 }}>{m.title}</td>
                    <td>
                      <span className="badge badge-gold">{m.certificationCode || 'U/A'}</span>
                    </td>
                    <td>{m.runtimeMinutes} min</td>
                    <td>{m.releaseDate ? new Date(m.releaseDate).toLocaleDateString() : 'N/A'}</td>
                    <td>
                      <span className={`badge ${m.movieStatus === 'ACTIVE' ? 'badge-emerald' : 'badge-slate'}`}>
                        {m.movieStatus}
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => handleOpenEdit(m)}
                        className="btn btn-sm btn-secondary"
                        style={{ padding: '4px 8px', fontSize: '0.8rem', gap: '4px' }}
                      >
                        <Edit2 size={12} />
                        Edit
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="btn btn-secondary btn-sm"
          >
            Previous
          </button>
          <span style={{ display: 'flex', alignItems: 'center', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="btn btn-secondary btn-sm"
          >
            Next
          </button>
        </div>
      )}

      {/* Add / Edit Movie Modal */}
      {isModalOpen && (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-content" style={{ maxWidth: '640px' }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem' }}>
                {editingMovie ? `Edit ${editingMovie.title}` : 'Add Movie to Global Catalogue'}
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="btn-icon" aria-label="Close dialog">
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="m-title">Movie Title</label>
                  <input
                    id="m-title"
                    type="text"
                    required
                    placeholder="e.g. Inception"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="input"
                  />
                </div>

                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="m-cert">Certification</label>
                    <select
                      id="m-cert"
                      value={certificationId}
                      onChange={(e) => setCertificationId(parseInt(e.target.value, 10))}
                      className="select"
                    >
                      <option value={1}>U (Universal)</option>
                      <option value={2}>U/A (Parental Guidance)</option>
                      <option value={3}>A (Adults Only)</option>
                    </select>
                  </div>

                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="m-runtime">Runtime (Minutes)</label>
                    <input
                      id="m-runtime"
                      type="number"
                      min="1"
                      required
                      value={runtimeMinutes}
                      onChange={(e) => setRuntimeMinutes(parseInt(e.target.value, 10))}
                      className="input"
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="m-release">Release Date</label>
                    <input
                      id="m-release"
                      type="date"
                      required
                      value={releaseDate}
                      onChange={(e) => setReleaseDate(e.target.value)}
                      className="input"
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" htmlFor="m-status">Catalog Status</label>
                    <select
                      id="m-status"
                      value={movieStatus}
                      onChange={(e) => setMovieStatus(e.target.value)}
                      className="select"
                    >
                      <option value="AIRING">AIRING</option>
                      <option value="UPCOMING">UPCOMING</option>
                      <option value="ENDED">ENDED</option>
                      <option value="INACTIVE">INACTIVE</option>
                    </select>
                  </div>
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="m-poster">Poster URL (Optional)</label>
                  <input
                    id="m-poster"
                    type="url"
                    placeholder="https://example.com/poster.jpg"
                    value={posterUrl}
                    onChange={(e) => setPosterUrl(e.target.value)}
                    className="input"
                  />
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label" htmlFor="m-synopsis">Synopsis</label>
                  <textarea
                    id="m-synopsis"
                    rows={3}
                    placeholder="Enter movie narrative overview..."
                    value={synopsis}
                    onChange={(e) => setSynopsis(e.target.value)}
                    className="textarea"
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" onClick={() => setIsModalOpen(false)} className="btn btn-secondary btn-sm">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn btn-primary btn-sm" style={{ gap: '6px' }}>
                  <Check size={14} />
                  {submitting ? 'Saving...' : editingMovie ? 'Update Movie' : 'Create Movie'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
