import React from 'react';

export const Footer = () => {
  return (
    <footer className="border-t border-slate-800 bg-slate-950/80 mt-20 py-8 px-6 text-center text-xs text-slate-400">
      <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <span className="font-semibold text-slate-200">AI Decision Engine for Intelligent Booking</span>
          <span className="block text-[11px] text-slate-400">Movie Theatre Booking Transactional Platform</span>
        </div>
        <div className="text-slate-400">
          Backend: Spring Boot 3.2.5 (Java 21) &bull; Frontend: React.js &bull; RDBMS: MySQL 8.0
        </div>
      </div>
    </footer>
  );
};
