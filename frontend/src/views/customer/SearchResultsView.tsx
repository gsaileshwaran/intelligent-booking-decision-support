import React, { useState, useEffect } from 'react';
import { Search, Sparkles, Film, Building, ArrowRight, Zap, Brain, Hash } from 'lucide-react';
import { searchApi, moviesApi } from '../../api/client';
import type { SearchCard } from '../../types/search';
import { useAuth } from '../../context/AuthContext';

interface SearchResultsViewProps {
  initialQuery: string;
  onNavigate: (view: string, param?: any) => void;
}

const SUGGESTED_QUERIES = [
  { label: 'Action Tamil', query: 'action movie Tamil' },
  { label: 'Sci-Fi IMAX', query: 'science fiction IMAX' },
  { label: 'Drama Oscar', query: 'award winning drama' },
  { label: 'Superhero', query: 'superhero avengers' },
  { label: 'Indian Epic', query: 'Indian epic blockbuster RRR' },
  { label: 'Mind Bending', query: 'mind bending thriller Inception' },
  { label: 'Oppenheimer', query: 'Oppenheimer Manhattan Project' },
  { label: 'Dark Knight', query: 'Batman Joker Gotham' },
];

const MethodBadge: React.FC<{ method: string }> = ({ method }) => {
  const upper = (method || '').toUpperCase();
  let bg: string;
  let icon: React.ReactNode;
  let label: string;
  if (upper.includes('HYBRID')) {
    bg = 'linear-gradient(135deg, #dc2626 0%, #7c3aed 100%)';
    icon = <Zap size={10} />;
    label = 'HYBRID';
  } else if (upper.includes('SEMANTIC') || upper.includes('VECTOR')) {
    bg = 'linear-gradient(135deg, #7c3aed 0%, #2563eb 100%)';
    icon = <Brain size={10} />;
    label = 'SEMANTIC';
  } else {
    bg = 'linear-gradient(135deg, #059669 0%, #0891b2 100%)';
    icon = <Hash size={10} />;
    label = 'LEXICAL';
  }
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '3px',
      padding: '2px 7px', borderRadius: '999px', background: bg,
      color: '#fff', fontSize: '0.65rem', fontWeight: 700, letterSpacing: '0.04em',
    }}>
      {icon}{label}
    </span>
  );
};

export const SearchResultsView: React.FC<SearchResultsViewProps> = ({ initialQuery, onNavigate }) => {
  const { selectedCityId } = useAuth();
  const [query, setQuery] = useState(initialQuery);
  const [results, setResults] = useState<SearchCard[]>([]);
  const [moviePosters, setMoviePosters] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    // Cache movie posters for search result card thumbnails
    moviesApi.getMovies({ size: 100 })
      .then((data) => {
        const posterMap: Record<number, string> = {};
        (data.content || []).forEach((m) => {
          if (m.movieId && m.posterUrl) {
            posterMap[m.movieId] = m.posterUrl;
          }
        });
        setMoviePosters(posterMap);
      })
      .catch((err) => console.warn('Could not preload movie posters for search', err));
  }, []);

  const performSearch = async (searchTerm: string) => {
    if (!searchTerm.trim()) return;
    try {
      setLoading(true);
      setSearched(true);
      setQuery(searchTerm);
      const data = await searchApi.search(searchTerm, selectedCityId || undefined);
      setResults(data || []);
    } catch (err) {
      console.error('Search query failed', err);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (initialQuery) {
      setQuery(initialQuery);
      performSearch(initialQuery);
    }
  }, [initialQuery, selectedCityId]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    performSearch(query);
  };

  const handleCardClick = (card: SearchCard) => {
    if (card.entityType.toUpperCase() === 'MOVIE') {
      onNavigate('movie-details', card.entityId);
    } else if (card.entityType.toUpperCase() === 'THEATRE') {
      onNavigate('theatre-details', card.entityId);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }} data-testid="search-results-view">
      <div>
        <h1 style={{ marginBottom: '6px' }}>AI-Powered Cinema Search</h1>
        <p style={{ marginBottom: '16px', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          Hybrid BM25 lexical + MiniLM semantic vector retrieval across the PVK Cinemas catalogue
        </p>

        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '12px', maxWidth: '680px' }}>
          <div style={{ flex: 1, position: 'relative' }}>
            <Search size={18} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' }} />
            <input
              type="search"
              aria-label="Search query input"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by title, genre, director, or cinema location..."
              className="input"
              style={{ paddingLeft: '44px', height: '48px', fontSize: '1rem' }}
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ height: '48px', padding: '0 24px', gap: '8px' }}>
            <Sparkles size={16} />
            Search
          </button>
        </form>

        <div style={{ marginTop: '12px', display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}>
          <span style={{ fontSize: '0.73rem', color: 'var(--text-muted)', marginRight: '4px' }}>Try:</span>
          {SUGGESTED_QUERIES.map((sq) => (
            <button
              key={sq.query}
              type="button"
              onClick={() => performSearch(sq.query)}
              style={{
                padding: '5px 12px', borderRadius: '999px',
                border: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)',
                color: 'var(--text-secondary)', fontSize: '0.78rem', cursor: 'pointer',
                fontFamily: 'inherit', transition: 'all 0.18s',
              }}
              onMouseOver={(e) => {
                (e.currentTarget as HTMLButtonElement).style.background = 'rgba(220,38,38,0.1)';
                (e.currentTarget as HTMLButtonElement).style.borderColor = '#dc2626';
                (e.currentTarget as HTMLButtonElement).style.color = '#dc2626';
              }}
              onMouseOut={(e) => {
                (e.currentTarget as HTMLButtonElement).style.background = 'var(--bg-elevated)';
                (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--border-subtle)';
                (e.currentTarget as HTMLButtonElement).style.color = 'var(--text-secondary)';
              }}
            >
              {sq.label}
            </button>
          ))}
        </div>
      </div>

      {searched && !loading && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text-secondary)', flexWrap: 'wrap' }}>
          <Sparkles size={16} color="var(--accent-gold)" />
          <span>Found <strong>{results.length}</strong> matching candidate{results.length !== 1 ? 's' : ''} for "<em>{query}</em>"</span>
          {results.length > 0 && (
            <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>· BM25 + Semantic MiniLM hybrid ranking</span>
          )}
        </div>
      )}

      {loading && (
        <div style={{ textAlign: 'center', padding: '80px 20px' }}>
          <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'center', gap: '14px' }}>
            <div style={{ width: '36px', height: '36px', border: '3px solid var(--border-subtle)', borderTopColor: '#dc2626', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Running hybrid AI search...</span>
            <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>BM25 + MiniLM-L6-v2 vectors</span>
          </div>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      )}

      {!loading && searched && results.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <Film size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
          <h3>No Matching Candidates</h3>
          <p style={{ marginTop: '8px', color: 'var(--text-secondary)' }}>
            Try a different query or click one of the suggested searches above.
          </p>
        </div>
      )}

      {!loading && results.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {results.map((card, idx) => {
            const isMovie = card.entityType.toUpperCase() === 'MOVIE';
            return (
              <div
                key={`${card.entityType}-${card.entityId}-${idx}`}
                className="card card-interactive"
                onClick={() => handleCardClick(card)}
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 20px', gap: '16px', cursor: 'pointer' }}
                data-testid={`search-card-${card.entityId}`}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: 0 }}>
                  {isMovie && (card.posterUrl || moviePosters[card.entityId]) ? (
                    <div style={{
                      flexShrink: 0, width: '52px', height: '76px',
                      borderRadius: 'var(--radius-sm)',
                      overflow: 'hidden',
                      boxShadow: '0 2px 6px rgba(0,0,0,0.35)',
                      border: '1px solid var(--border-subtle)',
                      background: 'var(--bg-elevated)',
                    }}>
                      <img
                        src={card.posterUrl || moviePosters[card.entityId]}
                        alt={card.title}
                        style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                        onError={(e) => {
                          // Hide image and show fallback icon
                          (e.currentTarget as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    </div>
                  ) : (
                    <div style={{
                      flexShrink: 0, width: '52px', height: '52px',
                      background: isMovie ? 'rgba(220,38,38,0.12)' : 'rgba(234,179,8,0.12)',
                      color: isMovie ? '#dc2626' : '#ca8a04',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      {isMovie ? <Film size={22} /> : <Building size={22} />}
                    </div>
                  )}

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px', flexWrap: 'wrap' }}>
                      <span className={`badge ${isMovie ? 'badge-crimson' : 'badge-gold'}`}>{card.entityType}</span>
                      {card.badge && <span className="badge badge-slate">{card.badge}</span>}
                      {card.retrievalMethod && <MethodBadge method={card.retrievalMethod} />}
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Rank #{card.rankPosition || idx + 1}</span>
                    </div>
                    <div style={{ fontWeight: 600, fontSize: '1.05rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{card.title}</div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{card.subtitle}</div>
                  </div>
                </div>

                <button
                  className="btn btn-sm btn-secondary"
                  style={{ flexShrink: 0, gap: '5px' }}
                  onClick={(e) => { e.stopPropagation(); handleCardClick(card); }}
                >
                  View <ArrowRight size={13} />
                </button>
              </div>
            );
          })}
        </div>
      )}

      {searched && !loading && results.length > 0 && (
        <div style={{
          padding: '10px 14px', borderRadius: 'var(--radius-md)',
          background: 'var(--bg-elevated)', border: '1px solid var(--border-subtle)',
          display: 'flex', alignItems: 'center', gap: '10px',
          color: 'var(--text-muted)', fontSize: '0.76rem',
        }}>
          <Brain size={13} style={{ flexShrink: 0, color: '#7c3aed' }} />
          <span>
            <strong style={{ color: 'var(--text-secondary)' }}>PVK AI Engine:</strong>{' '}
            Results ranked by hybrid score combining BM25 term frequency and{' '}
            <em>sentence-transformers/all-MiniLM-L6-v2</em> 384-dim cosine similarity.
          </span>
        </div>
      )}
    </div>
  );
};