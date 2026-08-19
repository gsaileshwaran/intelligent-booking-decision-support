import React, { createContext, useContext, useState } from 'react';

const BookingContext = createContext(null);

export const BookingProvider = ({ children }) => {
  const [selectedShow, setSelectedShow] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [activeBooking, setActiveBooking] = useState(null);

  const toggleSeatSelection = (showSeat) => {
    setSelectedSeats((prev) => {
      const exists = prev.some((s) => s.showSeatId === showSeat.showSeatId);
      if (exists) {
        return prev.filter((s) => s.showSeatId !== showSeat.showSeatId);
      } else {
        return [...prev, showSeat];
      }
    });
  };

  const clearBookingState = () => {
    setSelectedShow(null);
    setSelectedSeats([]);
    setActiveBooking(null);
  };

  return (
    <BookingContext.Provider
      value={{
        selectedShow,
        setSelectedShow,
        selectedSeats,
        setSelectedSeats,
        toggleSeatSelection,
        activeBooking,
        setActiveBooking,
        clearBookingState,
      }}
    >
      {children}
    </BookingContext.Provider>
  );
};

export const useBooking = () => {
  const context = useContext(BookingContext);
  if (!context) {
    throw new Error('useBooking must be used within a BookingProvider');
  }
  return context;
};
