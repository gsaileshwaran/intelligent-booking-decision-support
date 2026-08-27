package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.exception.InvalidBookingStateException;
import com.booking.intelligent.exception.SeatNotAvailableException;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CrossSessionHoldTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShowService showService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Test
    public void testHeldBookingSurvivesCustomerLogoutLoginAndCanBeConfirmed() {
        User customerA = userRepository.findByEmail("customer@example.com").orElseThrow();
        User customerB = userRepository.findByEmail("admin@example.com").orElseThrow();

        Movie movie = movieRepository.findAll().stream().findFirst().orElseThrow();
        Screen screen = screenRepository.findAll().stream().findFirst().orElseThrow();

        Show futureShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        List<ShowSeat> seats = showService.getShowSeats(futureShow.getShowId());
        ShowSeat seatA1 = seats.get(0);
        ShowSeat seatB1 = seats.get(1);
        ShowSeat seatB2 = seats.get(2);

        // 1. Customer A holds seat A1
        SeatHoldRequest holdReqA = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(seatA1.getShowSeatId()))
                .build();

        BookingResponse holdResA = bookingService.holdSeats(holdReqA, customerA.getUserId());
        assertNotNull(holdResA.getBookingId());
        assertEquals(BookingStatus.HELD, holdResA.getStatus());

        // 2. Customer B attempts to acquire seat A1 -> MUST FAIL with SeatNotAvailableException
        SeatHoldRequest holdReqB_Conflict = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(seatA1.getShowSeatId()))
                .build();

        assertThrows(SeatNotAvailableException.class, () -> {
            bookingService.holdSeats(holdReqB_Conflict, customerB.getUserId());
        });

        // 3. Customer B buys tickets B1 + B2 -> MUST SUCCEED
        SeatHoldRequest holdReqB_Valid = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(seatB1.getShowSeatId(), seatB2.getShowSeatId()))
                .build();

        BookingResponse holdResB = bookingService.holdSeats(holdReqB_Valid, customerB.getUserId());
        BookingResponse confirmResB = bookingService.confirmBooking(holdResB.getBookingId(), "MOCK_CARD", customerB.getUserId());
        assertEquals(BookingStatus.CONFIRMED, confirmResB.getStatus());

        // 4. Customer B attempts to access Customer A's booking A1 -> MUST FAIL with AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.getBookingById(holdResA.getBookingId(), customerB.getUserId());
        });

        assertThrows(AccessDeniedException.class, () -> {
            bookingService.confirmBooking(holdResA.getBookingId(), "MOCK_CARD", customerB.getUserId());
        });

        // 5. Customer A logs back in (simulated re-auth), fetches booking A1 and confirms -> MUST SUCCEED
        BookingResponse fetchedResA = bookingService.getBookingById(holdResA.getBookingId(), customerA.getUserId());
        assertEquals(BookingStatus.HELD, fetchedResA.getStatus());
        assertNotNull(fetchedResA.getHoldExpiresAt());

        BookingResponse confirmResA = bookingService.confirmBooking(holdResA.getBookingId(), "MOCK_CARD", customerA.getUserId());
        assertEquals(BookingStatus.CONFIRMED, confirmResA.getStatus());
    }

    @Test
    public void testExpiredHoldCannotBeConfirmedAndReleasesSeat() {
        User customerA = userRepository.findByEmail("customer@example.com").orElseThrow();
        Movie movie = movieRepository.findAll().stream().findFirst().orElseThrow();
        Screen screen = screenRepository.findAll().stream().findFirst().orElseThrow();

        Show futureShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(6))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(21, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        List<ShowSeat> seats = showService.getShowSeats(futureShow.getShowId());
        ShowSeat seat = seats.get(0);

        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(seat.getShowSeatId()))
                .build();

        BookingResponse holdRes = bookingService.holdSeats(holdReq, customerA.getUserId());

        // Manually simulate hold expiration in DB
        ShowSeat dbSeat = showSeatRepository.findById(seat.getShowSeatId()).orElseThrow();
        dbSeat.setHeldUntil(LocalDateTime.now().minusMinutes(5));
        showSeatRepository.save(dbSeat);

        // Attempt to confirm expired hold -> MUST FAIL with InvalidBookingStateException
        assertThrows(InvalidBookingStateException.class, () -> {
            bookingService.confirmBooking(holdRes.getBookingId(), "MOCK_CARD", customerA.getUserId());
        });

        // Seat must revert to AVAILABLE
        ShowSeat updatedSeat = showSeatRepository.findById(seat.getShowSeatId()).orElseThrow();
        assertEquals(ShowSeatStatus.AVAILABLE, updatedSeat.getStatus());
        assertNull(updatedSeat.getHeldUntil());
    }
}
