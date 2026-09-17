import React, { useState, useEffect } from 'react';
import { Filter, RotateCcw } from 'lucide-react';
import { moviesApi } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import type { Movie } from '../../types/movie';
import { MovieCard } from '../../components/cinema/MovieCard';

interface MovieListingViewProps {
  onNavigate: (view: string, param?: any) => void;
}

export const MovieListingView: React.FC<MovieListingViewProps> = ({ onNavigate }) => {
  const { selectedCityId } = useAuth();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('AIRING');
  const [selectedGenre, setSelectedGenre] = useState<string>('');
  const [selectedLanguage, setSelectedLanguage] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    setPage(0);
  }, [selectedCityId]);

  useEffect(() => {
    const fetchMovies = async () => {
      try {
        setLoading(true);
        const data = await moviesApi.getMovies({
          status: statusFilter || undefined,
          cityId: selectedCityId || undefined,
          page,
          size: 12,
        });
        setMovies(data.content || []);
        setTotalPages(data.totalPages || 1);
      } catch (err) {
        console.error('Failed to fetch movies', err);
      } finally {
        setLoading(false);
      }
    };
    fetchMovies();
  }, [statusFilter, page, selectedCityId]);

  // Client-side filtering by genre / language if needed
  const filteredMovies = movies.filter((m) => {
    const matchesGenre = selectedGenre ? m.genres?.includes(selectedGenre) : true;
    const matchesLang = selectedLanguage ? m.languages?.includes(selectedLanguage) : true;
    return matchesGenre && matchesLang;
  });

  const allGenres = Array.from(new Set(movies.flatMap((m) => m.genres || [])));
  const allLanguages = Array.from(new Set(movies.flatMap((m) => m.languages || [])));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="movie-listing-view">
      {/* Title and Filter Ribbon */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        flexWrap: 'wrap',
        gap: '20px',
      }}>
        <div>
          <h1>Movie Catalogue</h1>
          <p>Explore current releases, upcoming titles, and format details</p>
        </div>

        {/* Status Tabs */}
        <div className="tabs" style={{ marginBottom: 0 }}>
          <button
            onClick={() => { setStatusFilter('AIRING'); setPage(0); }}
            className={`tab-btn ${statusFilter === 'AIRING' ? 'active' : ''}`}
          >
            Now Showing
          </button>
          <button
            onClick={() => { setStatusFilter(''); setPage(0); }}
            className={`tab-btn ${statusFilter === '' ? 'active' : ''}`}
          >
            All Movies
          </button>
        </div>
      </div>

      {/* Filter Control Bar */}
      <div className="card" style={{ padding: '16px 20px', display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)' }}>
          <Filter size={18} />
          <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Filter Catalog:</span>
        </div>

        <select
          value={selectedGenre}
          onChange={(e) => setSelectedGenre(e.target.value)}
          className="select"
          aria-label="Filter by genre"
          style={{ width: 'auto', minWidth: '150px' }}
        >
          <option value="">All Genres</option>
          {allGenres.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>

        <select
          value={selectedLanguage}
          onChange={(e) => setSelectedLanguage(e.target.value)}
          className="select"
          aria-label="Filter by language"
          style={{ width: 'auto', minWidth: '150px' }}
        >
          <option value="">All Languages</option>
          {allLanguages.map((l) => (
            <option key={l} value={l}>{l}</option>
          ))}
        </select>

        {(selectedGenre || selectedLanguage) && (
          <button
            onClick={() => { setSelectedGenre(''); setSelectedLanguage(''); }}
            className="btn btn-sm btn-outline"
            style={{ gap: '6px' }}
          >
            <RotateCcw size={13} />
            Reset Filters
          </button>
        )}
      </div>

      {/* Movie Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
          Loading movies catalogue...
        </div>
      ) : filteredMovies.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <h3>No Movies Found</h3>
          <p style={{ marginTop: '8px' }}>Try selecting a different genre, language, or status filter.</p>
        </div>
      ) : (
        <div className="grid-movies">
          {filteredMovies.map((movie) => (
            <MovieCard
              key={movie.movieId}
              movie={movie}
              onSelect={(id) => onNavigate('movie-details', id)}
            />
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '16px' }}>
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
    </div>
  );
};
