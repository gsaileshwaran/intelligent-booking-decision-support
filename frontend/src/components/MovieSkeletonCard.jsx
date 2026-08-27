import React from 'react';

export const MovieSkeletonCard = () => {
  return (
    <div className="glass-card flex flex-col overflow-hidden animate-pulse">
      <div className="aspect-[2/3] bg-slate-800/80 w-full" />
      <div className="p-4 space-y-3">
        <div className="h-4 bg-slate-800 rounded w-3/4" />
        <div className="h-3 bg-slate-800/60 rounded w-1/2" />
        <div className="h-8 bg-slate-800 rounded w-full mt-2" />
      </div>
    </div>
  );
};
