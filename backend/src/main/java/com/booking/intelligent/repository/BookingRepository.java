package com.booking.intelligent.repository;

import com.booking.intelligent.entity.Booking;
import com.booking.intelligent.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingRef(String bookingRef);

    List<Booking> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatus(BookingStatus status);

    long countByStatus(BookingStatus status);

    @Query("SELECT DISTINCT b FROM Booking b JOIN b.items bi JOIN bi.showSeat ss JOIN ss.show s JOIN s.screen sc WHERE sc.theatre.theatreId = :theatreId ORDER BY b.createdAt DESC")
    List<Booking> findByTheatreId(@Param("theatreId") Long theatreId);

    @Query("SELECT DISTINCT b FROM Booking b JOIN b.items bi JOIN bi.showSeat ss JOIN ss.show s JOIN s.screen sc JOIN sc.theatre t WHERE t.ownerUser.userId = :ownerId ORDER BY b.createdAt DESC")
    List<Booking> findByProviderOwnerId(@Param("ownerId") Long ownerId);
}
