import React from 'react';
import { Film, Clock } from 'lucide-react';
import type { Movie } from '../../types/movie';

interface MovieCardProps {
  movie: Movie;
  onSelect: (movieId: number) => void;
}

export const MovieCard: React.FC<MovieCardProps> = ({ movie, onSelect }) => {
  const defaultPoster = `https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3`;

  return (
    <article 
      className="card card-interactive" 
      onClick={() => onSelect(movie.movieId)}
      style={{ padding: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column', cursor: 'pointer' }}
      data-testid={`movie-card-${movie.movieId}`}
    >
      {/* Poster Media */}
      <div style={{ position: 'relative', width: '100%', paddingTop: '145%', backgroundColor: '#0f141f' }}>
        <img
          src={movie.posterUrl || defaultPoster}
          alt={`Poster of ${movie.title}`}
          loading="lazy"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
          onError={(e) => {
            (e.target as HTMLImageElement).src = defaultPoster;
          }}
        />

        {/* Certification Badge */}
        {movie.certificationCode && (
          <div style={{
            position: 'absolute',
            top: '12px',
            right: '12px',
            background: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(8px)',
            color: 'var(--accent-gold)',
            border: '1px solid rgba(245, 197, 24, 0.4)',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 700,
          }}>
            {movie.certificationCode}
          </div>
        )}

        {/* Runtime Badge */}
        {movie.runtimeMinutes && (
          <div style={{
            position: 'absolute',
            bottom: '12px',
            left: '12px',
            background: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(8px)',
            color: '#ffffff',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
          }}>
            <Clock size={12} />
            <span>{movie.runtimeMinutes}m</span>
          </div>
        )}
      </div>

      {/* Card Body */}
      <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', flex: 1 }}>
        <h4 style={{ 
          fontSize: '1.05rem', 
          marginBottom: '8px', 
          whiteSpace: 'nowrap', 
          overflow: 'hidden', 
          textOverflow: 'ellipsis' 
        }}>
          {movie.title}
        </h4>

        {/* Languages & Genres */}
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '14px' }}>
          {movie.languages?.slice(0, 2).map((lang) => (
            <span key={lang} className="badge badge-crimson" style={{ fontSize: '0.68rem', padding: '2px 6px' }}>
              {lang}
            </span>
          ))}
          {movie.genres?.slice(0, 2).map((genre) => (
            <span key={genre} className="badge badge-slate" style={{ fontSize: '0.68rem', padding: '2px 6px' }}>
              {genre}
            </span>
          ))}
        </div>

        {/* Action button */}
        <button
          className="btn btn-outline btn-sm"
          style={{ width: '100%', marginTop: 'auto', gap: '6px' }}
          onClick={(e) => {
            e.stopPropagation();
            onSelect(movie.movieId);
          }}
          aria-label={`View showtimes for ${movie.title}`}
        >
          <Film size={14} aria-hidden="true" />
          View Showtimes
        </button>
      </div>
    </article>
  );
};
