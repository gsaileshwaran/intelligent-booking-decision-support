import React from 'react';
import { AlertTriangle } from 'lucide-react';

interface DisplayOnlyBannerProps {
  message?: string;
}

export const DisplayOnlyBanner: React.FC<DisplayOnlyBannerProps> = ({
  message = "PVK Cinemas is a cinema discovery and decision-support platform with movie search, showtime discovery, seat selection, temporary seat holds, booking, and simulated payment. Payments use dummy/test data only; no real financial transactions are processed.",
}) => {
  return (
    <div 
      className="banner-display-only" 
      role="alert" 
      aria-label="Display Only Notice"
      data-testid="display-only-banner"
    >
      <AlertTriangle size={20} color="#F59E0B" style={{ flexShrink: 0 }} />
      <span>{message}</span>
    </div>
  );
};
