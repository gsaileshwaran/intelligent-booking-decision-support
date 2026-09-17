import React, { useState, useEffect } from 'react';
import { Search, Film, Sparkles, Tv, ArrowRight } from 'lucide-react';
import { moviesApi, theatresApi } from '../../api/client';
import type { Movie } from '../../types/movie';
import type { Theatre, City } from '../../types/theatre';
import { MovieCard } from '../../components/cinema/MovieCard';
import { TheatreCard } from '../../components/cinema/TheatreCard';
import { useAuth } from '../../context/AuthContext';

interface HomeViewProps {
  onNavigate: (view: string, param?: any) => void;
}

export const HomeView: React.FC<HomeViewProps> = ({ onNavigate }) => {
  const { selectedCityId, setSelectedCityId } = useAuth();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [theatres, setTheatres] = useState<Theatre[]>([]);
  const [cities, setCities] = useState<City[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadHomeData = async () => {
      try {
        setLoading(true);
        const [moviesData, theatresData, citiesData] = await Promise.all([
          moviesApi.getMovies({ status: 'AIRING', cityId: selectedCityId || undefined, page: 0, size: 8 }),
          theatresApi.getTheatres(selectedCityId || undefined),
          theatresApi.getCities(),
        ]);
        setMovies(moviesData.content || []);
        setTheatres(theatresData.slice(0, 4));
        setCities(citiesData);
      } catch (err) {
        console.error('Failed to load home data', err);
      } finally {
        setLoading(false);
      }
    };
    loadHomeData();
  }, [selectedCityId]);

  const handleHeroSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      onNavigate('search', searchQuery.trim());
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '48px' }}>
      {/* Hero Showcase Section */}
      <section style={{
        background: 'linear-gradient(135deg, rgba(229, 9, 20, 0.12) 0%, rgba(18, 24, 38, 0.95) 60%, rgba(10, 13, 20, 1) 100%)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-lg)',
        padding: '56px 40px',
        position: 'relative',
        overflow: 'hidden',
      }}>
        <div style={{ maxWidth: '780px', position: 'relative', zIndex: 2 }}>
          <div className="badge badge-crimson" style={{ marginBottom: '16px', gap: '6px' }}>
            <Sparkles size={14} />
            <span>Intelligent Cinema Discovery</span>
          </div>

          <h1 style={{ fontSize: '2.8rem', lineHeight: 1.15, marginBottom: '18px' }}>
            Experience Cinematic Brilliance Across Top Multiplexes.
          </h1>

          <p style={{ fontSize: '1.15rem', color: 'var(--text-secondary)', marginBottom: '32px', lineHeight: 1.6 }}>
            Browse curated movies, real-time screen configurations, and live auditorium seat availability across India's premier cinema destinations.
          </p>

          {/* Hero Search Box */}
          <form onSubmit={handleHeroSearch} style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '280px', position: 'relative' }}>
              <Search size={20} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                placeholder="Search movies by title, genre, actor, or mood..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="input"
                style={{
                  paddingLeft: '46px',
                  height: '52px',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(10, 13, 20, 0.9)',
                  fontSize: '1.05rem',
                }}
              />
            </div>

            <select
              value={selectedCityId || ''}
              onChange={(e) => setSelectedCityId(e.target.value ? parseInt(e.target.value, 10) : null)}
              className="select"
              aria-label="Filter city"
              style={{
                width: 'auto',
                minWidth: '160px',
                height: '52px',
                background: 'rgba(10, 13, 20, 0.9)',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.95rem',
              }}
            >
              <option value="">All Cities</option>
              {cities.map((city) => (
                <option key={city.cityId} value={city.cityId}>
                  {city.cityName}
                </option>
              ))}
            </select>

            <button type="submit" className="btn btn-primary btn-lg" style={{ height: '52px' }}>
              Search Catalog
            </button>
          </form>
        </div>
      </section>

      {/* Feature Pillars */}
      <section style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
        gap: '20px',
      }}>
        <div className="card" style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
          <div style={{ background: 'var(--accent-crimson-glow)', padding: '12px', borderRadius: '10px' }}>
            <Film size={24} color="var(--accent-crimson)" />
          </div>
          <div>
            <h4 style={{ marginBottom: '6px' }}>Authoritative Catalog</h4>
            <p style={{ fontSize: '0.88rem' }}>Direct synchronization with centralized Spring Boot database &amp; normalized metadata.</p>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
          <div style={{ background: 'var(--accent-gold-glow)', padding: '12px', borderRadius: '10px' }}>
            <Sparkles size={24} color="var(--accent-gold)" />
          </div>
          <div>
            <h4 style={{ marginBottom: '6px' }}>Hybrid Search Engine</h4>
            <p style={{ fontSize: '0.88rem' }}>FastAPI AI service combining BM25 keyword matching and 384-d semantic vectors.</p>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
          <div style={{ background: 'rgba(16, 185, 129, 0.15)', padding: '12px', borderRadius: '10px' }}>
            <Tv size={24} color="#10B981" />
          </div>
          <div>
            <h4 style={{ marginBottom: '6px' }}>Live Seat Visualization</h4>
            <p style={{ fontSize: '0.88rem' }}>Auditorium seat availability preview with high-contrast accessibility compliance.</p>
          </div>
        </div>
      </section>

      {/* Now Showing Movies Section */}
      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h2>Now Showing in Theatres</h2>
            <p>Explore the latest releases and showtimes</p>
          </div>
          <button 
            onClick={() => onNavigate('movies')} 
            className="btn btn-secondary btn-sm"
            style={{ gap: '6px' }}
          >
            <span>View All Movies</span>
            <ArrowRight size={14} />
          </button>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-secondary)' }}>
            Loading cinematic catalog...
          </div>
        ) : movies.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
            <p>No active movies scheduled at this time.</p>
          </div>
        ) : (
          <div className="grid-movies">
            {movies.map((movie) => (
              <MovieCard 
                key={movie.movieId} 
                movie={movie} 
                onSelect={(id) => onNavigate('movie-details', id)} 
              />
            ))}
          </div>
        )}
      </section>

      {/* Featured Multiplex Theatres Section */}
      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h2>Featured Cinema Hubs</h2>
            <p>Multiplex locations and screen facilities</p>
          </div>
          <button 
            onClick={() => onNavigate('theatres')} 
            className="btn btn-secondary btn-sm"
            style={{ gap: '6px' }}
          >
            <span>View All Theatres</span>
            <ArrowRight size={14} />
          </button>
        </div>

        <div className="grid-theatres">
          {theatres.map((theatre) => (
            <TheatreCard 
              key={theatre.theatreId} 
              theatre={theatre} 
              onSelect={(id) => onNavigate('theatre-details', id)} 
            />
          ))}
        </div>
      </section>
    </div>
  );
};
