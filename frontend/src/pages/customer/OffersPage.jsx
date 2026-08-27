import React, { useState, useEffect } from 'react';
import { convenienceService } from '../../services/convenienceService';
import { Tag, Copy, Check, Ticket, Sparkles, ShieldCheck } from 'lucide-react';

export const OffersPage = () => {
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [copiedCode, setCopiedCode] = useState('');

  useEffect(() => {
    fetchOffers();
  }, []);

  const fetchOffers = async () => {
    setLoading(true);
    try {
      const res = await convenienceService.getOffers();
      if (res.success && res.data) {
        setOffers(res.data);
      }
    } catch (err) {
      console.error('Failed to load offers:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCopyCode = (code) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(''), 3000);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-8">
        <span className="badge bg-amber-500/20 text-amber-300 border-amber-500/40 mb-1 font-bold">
          <Sparkles className="w-3.5 h-3.5 mr-1 inline" /> DEMO PROMOTIONS
        </span>
        <h1 className="text-3xl font-extrabold text-white">Offers & Concession Deals</h1>
        <p className="text-xs text-slate-400 mt-1">Exclusive promo vouchers and instant discounts for cinema bookings</p>
      </div>

      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading active promo offers...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {offers.map((offer) => (
            <div
              key={offer.offerId}
              className="glass-card overflow-hidden border border-slate-800 hover:border-amber-500/40 transition-all flex flex-col justify-between"
            >
              <div>
                <div className="h-36 bg-slate-800 relative overflow-hidden">
                  <img
                    src={offer.bannerUrl}
                    alt={offer.title}
                    className="w-full h-full object-cover opacity-80 hover:opacity-100 hover:scale-105 transition-all duration-500"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/40 to-transparent" />
                  <span className="absolute top-3 left-3 px-2.5 py-1 rounded-md bg-amber-500 text-slate-950 text-[10px] font-extrabold uppercase tracking-wider">
                    {offer.discountType === 'PERCENTAGE' ? `${offer.discountValue}% OFF` : `₹${offer.discountValue} OFF`}
                  </span>
                </div>

                <div className="p-6">
                  <h3 className="text-lg font-bold text-white mb-2">{offer.title}</h3>
                  <p className="text-xs text-slate-300 leading-relaxed mb-4">{offer.description}</p>
                  <p className="text-[11px] text-slate-500 flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" /> Valid till: {offer.validTill}
                  </p>
                </div>
              </div>

              <div className="p-6 pt-0">
                <div className="flex items-center justify-between p-3 rounded-xl bg-slate-900/90 border border-slate-800">
                  <div>
                    <span className="text-[10px] text-slate-500 block uppercase font-semibold">Promo Code</span>
                    <span className="font-mono text-xs font-extrabold text-amber-400 tracking-wider">{offer.code}</span>
                  </div>
                  <button
                    onClick={() => handleCopyCode(offer.code)}
                    className="btn-secondary py-1.5 px-3 text-xs flex items-center gap-1.5"
                  >
                    {copiedCode === offer.code ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-400" /> Copied!
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" /> Copy Code
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
