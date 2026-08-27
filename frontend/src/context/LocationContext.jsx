import React, { createContext, useContext, useState, useEffect } from 'react';

const LocationContext = createContext(null);

export const LocationProvider = ({ children }) => {
  const [selectedLocation, setSelectedLocation] = useState(() => {
    const saved = localStorage.getItem('user_location');
    if (saved === 'Bangalore') return 'Bengaluru';
    return saved || 'Chennai';
  });

  const availableLocations = ['Chennai', 'Bengaluru', 'Mumbai', 'Delhi', 'Hyderabad'];

  useEffect(() => {
    localStorage.setItem('user_location', selectedLocation);
  }, [selectedLocation]);

  const changeLocation = (loc) => {
    const normalized = loc === 'Bangalore' ? 'Bengaluru' : loc;
    if (availableLocations.includes(normalized)) {
      setSelectedLocation(normalized);
    }
  };

  return (
    <LocationContext.Provider
      value={{
        selectedLocation,
        changeLocation,
        availableLocations,
      }}
    >
      {children}
    </LocationContext.Provider>
  );
};

export const useLocation = () => {
  const context = useContext(LocationContext);
  if (!context) {
    throw new Error('useLocation must be used within a LocationProvider');
  }
  return context;
};
