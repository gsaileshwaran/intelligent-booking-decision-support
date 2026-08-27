import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { convenienceService } from '../../services/convenienceService';
import { useLocation } from '../../context/LocationContext';
import { Building2, MapPin, Film, ArrowRight, Search, ShieldCheck, Sparkles, Phone, Clock, Compass } from 'lucide-react';

export const TheatresPage = () => {
  const navigate = useNavigate();
  const { selectedLocation, changeLocation, availableLocations } = useLocation();

  const [theatres, setTheatres] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFormat, setSelectedFormat] = useState('ALL');

  useEffect(() => {
    fetchTheatres();
  }, [selectedLocation]);

  const fetchTheatres = async () => {
    setLoading(true);
    try {
      const res = await convenienceService.getTheatres(selectedLocation);
      if (res.success && res.data) {
        setTheatres(res.data);
      }
    } catch (err) {
      console.error('Failed to fetch theatres:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredTheatres = theatres.filter((t) => {
    const matchQuery = !searchQuery || 
      t.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
      (t.address && t.address.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (t.locality && t.locality.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchFormat = selectedFormat === 'ALL' || 
      (t.formats && t.formats.toLowerCase().includes(selectedFormat.toLowerCase()));

    return matchQuery && matchFormat;
  });

  const formatsFilterList = ['ALL', 'IMAX', 'Dolby Atmos', '4DX', 'Recliner', '4K Laser'];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1">NATIONAL CINEMA NETWORK</span>
          <h1 className="text-3xl font-extrabold text-white">PVK Multiplex Directory</h1>
          <p className="text-xs text-slate-400 mt-1">Explore 25 commercial-grade PVK cinemas across 5 major Indian metropolitan hubs</p>
        </div>

        {/* Location Selector */}
        <div className="flex flex-wrap items-center gap-2 bg-slate-900/80 p-2 rounded-xl border border-slate-800">
          <MapPin className="w-4 h-4 text-indigo-400 ml-1" />
          <span className="text-xs font-semibold text-slate-400 mr-1">City:</span>
          {availableLocations.map((loc) => (
            <button
              key={loc}
              onClick={() => changeLocation(loc)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                selectedLocation === loc
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              {loc}
            </button>
          ))}
        </div>
      </div>

      {/* Toolbar: Search + Format Filters */}
      <div className="glass-card p-4 mb-8 flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Search Bar */}
        <div className="flex items-center gap-3 w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-500 shrink-0" />
          <input
            type="text"
            placeholder="Search theatre branch or locality..."
            className="form-input text-xs w-full bg-slate-900/90 py-2"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Format Filter Pills */}
        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
          <span className="text-[10px] text-slate-500 font-bold uppercase mr-1">Format:</span>
          {formatsFilterList.map((fmt) => (
            <button
              key={fmt}
              onClick={() => setSelectedFormat(fmt)}
              className={`px-2.5 py-1 rounded-full text-[11px] font-bold transition-all ${
                selectedFormat === fmt
                  ? 'bg-purple-600 text-white shadow-sm'
                  : 'bg-slate-900/80 text-slate-400 hover:text-white'
              }`}
            >
              {fmt}
            </button>
          ))}
        </div>
      </div>

      {/* Theatres List */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading PVK multiplexes in {selectedLocation}...</div>
      ) : filteredTheatres.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          <Building2 className="w-12 h-12 mx-auto mb-3 text-slate-600" />
          <h3 className="text-lg font-bold text-slate-300">No PVK Branches Found</h3>
          <p className="text-xs text-slate-500 mt-1">No cinema branches currently match your filters in {selectedLocation}.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {filteredTheatres.map((theatre) => (
            <div
              key={theatre.theatreId}
              className="glass-card p-6 border border-slate-800 hover:border-indigo-500/40 transition-all flex flex-col justify-between group shadow-xl"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="px-2.5 py-0.5 rounded-full bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 text-[11px] font-bold flex items-center gap-1">
                    <Sparkles className="w-3 h-3 text-cyan-400" /> PVK NATIONAL NETWORK
                  </span>
                  <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-400">
                    <ShieldCheck className="w-3.5 h-3.5" /> 100% Digital Laser & Sound
                  </span>
                </div>

                <h3 className="text-xl font-black text-white mb-2 group-hover:text-indigo-300 transition-colors">
                  {theatre.name}
                </h3>

                <p className="text-xs text-slate-400 flex items-center gap-1.5 mb-3">
                  <MapPin className="w-4 h-4 text-slate-500 shrink-0" />
                  {theatre.address || `${theatre.locality}, ${theatre.location}`}
                </p>

                {theatre.description && (
                  <p className="text-xs text-slate-400 line-clamp-2 mb-4 leading-relaxed">
                    {theatre.description}
                  </p>
                )}

                {/* Formats Tags */}
                {theatre.formats && (
                  <div className="flex flex-wrap gap-1.5 mb-4">
                    {theatre.formats.split(',').map((f, idx) => (
                      <span key={idx} className="px-2 py-0.5 rounded bg-slate-900 border border-slate-700 text-[10px] font-extrabold text-cyan-300">
                        {f.trim()}
                      </span>
                    ))}
                  </div>
                )}

                {/* Stats Table */}
                <div className="grid grid-cols-2 gap-3 text-xs bg-slate-900/60 p-3 rounded-xl border border-slate-800/80 mb-6">
                  <div>
                    <span className="text-[10px] text-slate-500 block">Screens & Capacity</span>
                    <span className="font-bold text-slate-200">
                      {theatre.totalScreens || 5} Screens &bull; {theatre.totalCapacity || 800} Seats
                    </span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block">Operating Hours</span>
                    <span className="font-bold text-indigo-300 flex items-center gap-1">
                      <Clock className="w-3 h-3 text-indigo-400" /> {theatre.operatingHours || '09:00 AM - 11:45 PM'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={() => navigate(`/theatres/${theatre.theatreId}`)}
                  className="btn-primary w-full py-3 text-xs font-extrabold justify-center group-hover:bg-indigo-600 transition-all"
                >
                  View Showtimes & Screens <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
