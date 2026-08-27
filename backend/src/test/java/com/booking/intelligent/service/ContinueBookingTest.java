package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.dto.ShowSeatResponseDto;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ContinueBookingTest {

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

    @Test
    public void testCustomerA_Hold_Recognized_On_ReLogin_And_Show_Retrieval() {
        User customerA = userRepository.findByEmail("customer@example.com").orElseThrow();
        Movie movie = movieRepository.findAll().stream().findFirst().orElseThrow();
        Screen screen = screenRepository.findAll().stream().findFirst().orElseThrow();

        Show futureShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(21, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        List<ShowSeat> seats = showService.getShowSeats(futureShow.getShowId());
        ShowSeat targetSeat = seats.get(0);

        // 1. Customer A holds target seat B6
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(targetSeat.getShowSeatId()))
                .build();

        BookingResponse holdRes = bookingService.holdSeats(holdReq, customerA.getUserId());
        assertEquals(BookingStatus.HELD, holdRes.getStatus());

        // 2. Customer A logs out, logs back in, and retrieves seat map for the SAME show
        List<ShowSeatResponseDto> showSeatsForCustomerA = showService.getShowSeatsWithUserOwnership(futureShow.getShowId(), customerA.getUserId());

        ShowSeatResponseDto seatForCustomerA = showSeatsForCustomerA.stream()
                .filter(s -> s.getShowSeatId().equals(targetSeat.getShowSeatId()))
                .findFirst()
                .orElseThrow();

        // 3. MUST be recognized as YOUR HOLD belonging to Customer A's active booking
        assertTrue(seatForCustomerA.getHeldByCurrentUser());
        assertEquals(holdRes.getBookingId(), seatForCustomerA.getBookingId());
        assertEquals(ShowSeatStatus.HELD, seatForCustomerA.getStatus());

        // 4. Customer A can continue directly to confirm using existing booking ID
        BookingResponse confirmRes = bookingService.confirmBooking(seatForCustomerA.getBookingId(), "MOCK_CARD", customerA.getUserId());
        assertEquals(BookingStatus.CONFIRMED, confirmRes.getStatus());
    }

    @Test
    public void testCustomerB_Sees_Seat_As_Held_Other_And_Cannot_Confirm() {
        User customerA = userRepository.findByEmail("customer@example.com").orElseThrow();
        User customerB = userRepository.findByEmail("admin@example.com").orElseThrow();

        Movie movie = movieRepository.findAll().stream().findFirst().orElseThrow();
        Screen screen = screenRepository.findAll().stream().findFirst().orElseThrow();

        Show futureShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(8))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        List<ShowSeat> seats = showService.getShowSeats(futureShow.getShowId());
        ShowSeat targetSeat = seats.get(0);

        // 1. Customer A holds target seat
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(targetSeat.getShowSeatId()))
                .build();

        BookingResponse holdResA = bookingService.holdSeats(holdReq, customerA.getUserId());

        // 2. Customer B retrieves seat map for the SAME show
        List<ShowSeatResponseDto> showSeatsForCustomerB = showService.getShowSeatsWithUserOwnership(futureShow.getShowId(), customerB.getUserId());

        ShowSeatResponseDto seatForCustomerB = showSeatsForCustomerB.stream()
                .filter(s -> s.getShowSeatId().equals(targetSeat.getShowSeatId()))
                .findFirst()
                .orElseThrow();

        // 3. MUST NOT be recognized as held by Customer B
        assertFalse(seatForCustomerB.getHeldByCurrentUser());
        assertNull(seatForCustomerB.getBookingId());
        assertEquals(ShowSeatStatus.HELD, seatForCustomerB.getStatus());

        // 4. Customer B cannot confirm Customer A's booking ID
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.confirmBooking(holdResA.getBookingId(), "MOCK_CARD", customerB.getUserId());
        });
    }
}
