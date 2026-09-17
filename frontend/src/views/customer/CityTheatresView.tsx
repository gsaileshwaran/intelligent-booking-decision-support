import React, { useState, useEffect } from 'react';
import { theatresApi } from '../../api/client';
import type { City, Theatre } from '../../types/theatre';
import { TheatreCard } from '../../components/cinema/TheatreCard';
import { useAuth } from '../../context/AuthContext';

interface CityTheatresViewProps {
  onNavigate: (view: string, param?: any) => void;
}

export const CityTheatresView: React.FC<CityTheatresViewProps> = ({ onNavigate }) => {
  const { selectedCityId, setSelectedCityId } = useAuth();
  const [cities, setCities] = useState<City[]>([]);
  const [theatres, setTheatres] = useState<Theatre[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    theatresApi.getCities()
      .then((data) => setCities(data))
      .catch((err) => console.error('Failed to load cities', err));
  }, []);

  useEffect(() => {
    const fetchTheatres = async () => {
      try {
        setLoading(true);
        const data = await theatresApi.getTheatres(selectedCityId || undefined);
        setTheatres(data);
      } catch (err) {
        console.error('Failed to load theatres', err);
      } finally {
        setLoading(false);
      }
    };
    fetchTheatres();
  }, [selectedCityId]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="city-theatres-view">
      <div>
        <h1 style={{ marginBottom: '8px' }}>Cinema Multiplexes</h1>
        <p>Explore PVK Cinemas destinations, auditoriums, and facilities by city</p>
      </div>

      {/* City Chips */}
      <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
        <button
          onClick={() => setSelectedCityId(null)}
          className={`btn btn-sm ${selectedCityId === null ? 'btn-primary' : 'btn-secondary'}`}
        >
          All Cities
        </button>
        {cities.map((city) => (
          <button
            key={city.cityId}
            onClick={() => setSelectedCityId(city.cityId)}
            className={`btn btn-sm ${selectedCityId === city.cityId ? 'btn-primary' : 'btn-secondary'}`}
          >
            {city.cityName}
          </button>
        ))}
      </div>

      {/* Theatres Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
          Loading multiplex destinations...
        </div>
      ) : theatres.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px' }}>
          <h3>No Theatres Found in this Location</h3>
          <p style={{ marginTop: '8px' }}>Select another city to view available multiplex locations.</p>
        </div>
      ) : (
        <div className="grid-theatres">
          {theatres.map((theatre) => (
            <TheatreCard
              key={theatre.theatreId}
              theatre={theatre}
              onSelect={(id) => onNavigate('theatre-details', id)}
            />
          ))}
        </div>
      )}
    </div>
  );
};
