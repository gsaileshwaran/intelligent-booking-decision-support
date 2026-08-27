import React, { useState, useEffect } from 'react';
import { X, Sparkles, Building2, Clock, Ticket, CheckCircle2, ArrowRight, ShieldCheck, Filter, Users, DollarSign, Layers } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { recommendationService } from '../services/recommendationService';

export const AIRecommendationModal = ({ isOpen, onClose, movieId, movieTitle }) => {
  const navigate = useNavigate();

  const [groupSize, setGroupSize] = useState(2);
  const [maxBudget, setMaxBudget] = useState(300);
  const [preferredTime, setPreferredTime] = useState('19:00');
  const [preferredSeatType, setPreferredSeatType] = useState('PREMIUM');

  const [loading, setLoading] = useState(false);
  const [recommendations, setRecommendations] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen && movieId) {
      handleEvaluate();
    }
  }, [isOpen, movieId]);

  const handleEvaluate = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await recommendationService.evaluateRecommendations({
        movieId: movieId ? Number(movieId) : null,
        groupSize: Number(groupSize),
        maxBudget: maxBudget ? Number(maxBudget) : null,
        preferredTime: preferredTime || null,
        preferredSeatType: preferredSeatType || null,
      });

      if (res && res.success && res.data) {
        setRecommendations(res.data);
      } else {
        setError(res?.message || 'Unable to generate recommendations.');
      }
    } catch (err) {
      console.error('Failed to evaluate AI recommendations:', err);
      setError('AI Recommendation Service is temporarily unavailable.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md overflow-y-auto">
      <div className="glass-card w-full max-w-4xl max-h-[92vh] flex flex-col border border-indigo-500/40 p-6 sm:p-8">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-gradient-to-tr from-indigo-600 to-purple-600 text-white shadow-lg shadow-indigo-500/30">
              <Sparkles className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="badge badge-confirm text-[10px] uppercase font-bold tracking-wider">
                  AI DECISION SUPPORT ENGINE
                </span>
                {recommendations?.engineVersion && (
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-indigo-300 border border-slate-700">
                    {recommendations.engineVersion}
                  </span>
                )}
              </div>
              <h2 className="text-xl font-bold text-white mt-0.5">
                Smart Showtimes & Venue Recommendations
              </h2>
              <p className="text-xs text-slate-400">
                Multi-Criteria Decision Model (MCDM) analysis for {movieTitle || 'selected movie'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Preference Filters Controls */}
        <div className="bg-slate-900/90 rounded-2xl p-4 border border-slate-800 mb-6 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
          <div>
            <label className="block text-slate-400 font-semibold mb-1 flex items-center gap-1.5">
              <Users className="w-3.5 h-3.5 text-indigo-400" /> Group Size
            </label>
            <select
              value={groupSize}
              onChange={(e) => setGroupSize(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-white font-medium focus:border-indigo-500 focus:outline-none"
            >
              {[1, 2, 3, 4, 5, 6, 8, 10].map((n) => (
                <option key={n} value={n}>{n} {n === 1 ? 'Ticket' : 'Tickets'}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-semibold mb-1 flex items-center gap-1.5">
              <DollarSign className="w-3.5 h-3.5 text-emerald-400" /> Max Budget / Ticket
            </label>
            <select
              value={maxBudget}
              onChange={(e) => setMaxBudget(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-white font-medium focus:border-indigo-500 focus:outline-none"
            >
              <option value="150">₹150 Max</option>
              <option value="200">₹200 Max</option>
              <option value="300">₹300 Max</option>
              <option value="500">₹500 Max</option>
              <option value="1000">₹1000 Max</option>
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-semibold mb-1 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-amber-400" /> Target Showtime
            </label>
            <select
              value={preferredTime}
              onChange={(e) => setPreferredTime(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-white font-medium focus:border-indigo-500 focus:outline-none"
            >
              <option value="10:00">10:00 AM (Morning)</option>
              <option value="14:00">02:00 PM (Matinee)</option>
              <option value="19:00">07:00 PM (Prime Evening)</option>
              <option value="22:00">10:00 PM (Night)</option>
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-semibold mb-1 flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-purple-400" /> Preferred Category
            </label>
            <select
              value={preferredSeatType}
              onChange={(e) => setPreferredSeatType(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-white font-medium focus:border-indigo-500 focus:outline-none"
            >
              <option value="REGULAR">Regular Tier</option>
              <option value="PREMIUM">Premium Tier</option>
              <option value="BALCONY">Balcony Tier</option>
              <option value="VIP">VIP Lounge</option>
            </select>
          </div>
        </div>

        <div className="flex justify-end mb-6">
          <button
            onClick={handleEvaluate}
            disabled={loading}
            className="btn-primary py-2.5 px-6 text-xs font-bold flex items-center gap-2"
          >
            <Sparkles className="w-4 h-4" />
            {loading ? 'Evaluating MCDM Engine...' : 'Re-Evaluate AI Recommendations'}
          </button>
        </div>

        {/* Results Body */}
        <div className="flex-1 overflow-y-auto pr-2 space-y-6">
          {error && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-12 text-center space-y-3">
              <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p className="text-xs text-slate-400 font-medium">
                Evaluating candidate showtimes against real venue inventory...
              </p>
            </div>
          ) : recommendations && recommendations.rankedResults?.length > 0 ? (
            <div className="space-y-6">
              {recommendations.rankedResults.map((result) => {
                const isTopMatch = result.isBestMatch;
                return (
                  <div
                    key={result.showId}
                    className={`p-6 rounded-2xl border transition-all ${
                      isTopMatch
                        ? 'bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 border-indigo-500/60 shadow-xl shadow-indigo-500/10 ring-1 ring-indigo-500/40'
                        : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800/80 pb-4 mb-4">
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          {isTopMatch ? (
                            <span className="px-3 py-1 rounded-full bg-gradient-to-r from-amber-500 to-amber-600 text-slate-950 font-extrabold text-[11px] flex items-center gap-1.5 shadow-md">
                              <Sparkles className="w-3.5 h-3.5 fill-slate-950" /> #1 BEST MATCH
                            </span>
                          ) : (
                            <span className="px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 text-[11px] font-bold">
                              Rank #{result.rank}
                            </span>
                          )}

                          {result.alternativeNotice && (
                            <span className="px-2.5 py-0.5 rounded-full bg-purple-500/10 text-purple-300 border border-purple-500/30 text-[11px] font-medium">
                              {result.alternativeNotice}
                            </span>
                          )}
                        </div>

                        <h3 className="text-lg font-bold text-white flex items-center gap-2">
                          <Building2 className="w-4 h-4 text-indigo-400" />
                          {result.theatreName}
                        </h3>
                        <p className="text-xs text-slate-400 mt-0.5">
                          Movie: <span className="text-white font-semibold">{result.movieTitle}</span>
                        </p>
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <div className="text-2xl font-black text-emerald-400 font-mono">
                            {result.suitabilityScore?.toFixed(1)}%
                          </div>
                          <div className="text-[10px] text-slate-400 font-medium uppercase tracking-wider">
                            Suitability Score
                          </div>
                        </div>

                        <button
                          onClick={() => {
                            onClose();
                            navigate(`/shows/${result.showId}/seats`);
                          }}
                          className="btn-primary py-2.5 px-5 text-xs font-bold flex items-center gap-2"
                        >
                          Select Seats <ArrowRight className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {/* Show Metrics */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs bg-slate-950/60 p-3 rounded-xl border border-slate-800/80 mb-4">
                      <div>
                        <span className="text-slate-500 block text-[10px]">Showtime</span>
                        <strong className="text-indigo-300 font-mono">{result.startTime}</strong>
                      </div>
                      <div>
                        <span className="text-slate-500 block text-[10px]">Show Date</span>
                        <strong className="text-slate-200">{result.showDate}</strong>
                      </div>
                      <div>
                        <span className="text-slate-500 block text-[10px]">Base Price</span>
                        <strong className="text-emerald-400 font-mono">₹{result.price?.toFixed(2)}</strong>
                      </div>
                      <div>
                        <span className="text-slate-500 block text-[10px]">Available Seats</span>
                        <strong className="text-purple-300">{result.availableSeats} Open Seats</strong>
                      </div>
                    </div>

                    {/* Decision Factors */}
                    {result.explanationFactors && result.explanationFactors.length > 0 && (
                      <div className="space-y-1.5 text-xs">
                        <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                          Key Decision Drivers:
                        </span>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {result.explanationFactors.map((factor, fIdx) => (
                            <div
                              key={fIdx}
                              className="flex items-center gap-2 p-2 rounded-lg bg-slate-900/80 text-slate-300 border border-slate-800/60 text-[11px]"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                              <span>{factor}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="py-12 text-center text-slate-400 text-xs">
              No matching showtimes found for your filter constraints. Try adjusting group size or ticket budget ceiling.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
