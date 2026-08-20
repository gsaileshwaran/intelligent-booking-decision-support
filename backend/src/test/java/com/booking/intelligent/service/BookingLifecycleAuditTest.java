package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.exception.InvalidBookingStateException;
import com.booking.intelligent.exception.SeatNotAvailableException;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
public class BookingLifecycleAuditTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShowService showService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User customer1;
    private User customer2;
    private Show activeShow;
    private Show cancelledShow;
    private Show pastShow;
    private List<ShowSeat> showSeats;

    @BeforeEach
    public void setup() {
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        User provider = userRepository.save(User.builder()
                .email("audit_provider_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Audit Provider")
                .role(providerRole)
                .build());

        customer1 = userRepository.save(User.builder()
                .email("cust1_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Customer One")
                .role(customerRole)
                .build());

        customer2 = userRepository.save(User.builder()
                .email("cust2_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Customer Two")
                .role(customerRole)
                .build());

        Theatre theatre = theatreRepository.save(Theatre.builder()
                .name("PVK — Audit Branch")
                .location("Chennai")
                .address("Audit Street")
                .ownerUser(provider)
                .build());

        Screen screen = screenRepository.save(Screen.builder()
                .name("Auditorium 1")
                .capacity(50)
                .theatre(theatre)
                .build());

        Movie movie = movieRepository.save(Movie.builder()
                .title("Audit Movie " + System.currentTimeMillis())
                .duration(120)
                .genre("Thriller")
                .language("Tamil")
                .releaseDate(LocalDate.now())
                .build());

        activeShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        cancelledShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());
        showService.cancelShow(cancelledShow.getShowId(), provider.getUserId());

        pastShow = showRepository.save(Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(LocalDate.now().minusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .status(ShowStatus.ACTIVE)
                .build());

        showSeats = showService.getShowSeats(activeShow.getShowId());
    }

    @Test
    public void testSuccessfulBookingLifecycle() {
        ShowSeat seat1 = showSeats.get(0);
        ShowSeat seat2 = showSeats.get(1);

        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(activeShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId(), seat2.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());
        assertNotNull(holdResp);
        assertEquals(BookingStatus.HELD, holdResp.getStatus());
        assertEquals(2, holdResp.getSeats().size());

        // Verify Server-side Price Integrity
        BigDecimal expectedTotal = seat1.getPrice().add(seat2.getPrice());
        assertEquals(expectedTotal, holdResp.getTotalAmount());

        // Confirm Payment
        BookingResponse confirmResp = bookingService.confirmBooking(holdResp.getBookingId(), "UPI", customer1.getUserId());
        assertEquals(BookingStatus.CONFIRMED, confirmResp.getStatus());
        assertEquals("UPI", confirmResp.getPaymentMethod());

        // Verify Seats Marked CONFIRMED
        ShowSeat updatedSeat1 = showSeatRepository.findById(seat1.getShowSeatId()).orElse(null);
        assertNotNull(updatedSeat1);
        assertEquals(ShowSeatStatus.CONFIRMED, updatedSeat1.getStatus());
    }

    @Test
    public void testUnavailableSeatRejection() {
        ShowSeat seat1 = showSeats.get(0);

        SeatHoldRequest holdReq1 = SeatHoldRequest.builder()
                .showId(activeShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        bookingService.holdSeats(holdReq1, customer1.getUserId());

        // Customer 2 attempts to hold the same seat
        SeatHoldRequest holdReq2 = SeatHoldRequest.builder()
                .showId(activeShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        assertThrows(SeatNotAvailableException.class, () -> {
            bookingService.holdSeats(holdReq2, customer2.getUserId());
        });
    }

    @Test
    public void testCancelledShowBookingRejection() {
        List<ShowSeat> cancelledSeats = showService.getShowSeats(cancelledShow.getShowId());
        if (cancelledSeats.isEmpty()) return;

        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(cancelledShow.getShowId())
                .showSeatIds(List.of(cancelledSeats.get(0).getShowSeatId()))
                .build();

        InvalidBookingStateException ex = assertThrows(InvalidBookingStateException.class, () -> {
            bookingService.holdSeats(holdReq, customer1.getUserId());
        });
        assertTrue(ex.getMessage().contains("cancelled show"));
    }

    @Test
    public void testPastShowBookingRejection() {
        List<ShowSeat> pastSeats = showService.getShowSeats(pastShow.getShowId());

        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(pastShow.getShowId())
                .showSeatIds(List.of(pastSeats.get(0).getShowSeatId()))
                .build();

        InvalidBookingStateException ex = assertThrows(InvalidBookingStateException.class, () -> {
            bookingService.holdSeats(holdReq, customer1.getUserId());
        });
        assertTrue(ex.getMessage().contains("already ended"));
    }

    @Test
    public void testCustomerOwnershipSecurity() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(activeShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());

        // Customer 2 attempts to view Customer 1's booking
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.getBookingById(holdResp.getBookingId(), customer2.getUserId());
        });

        // Customer 2 attempts to confirm Customer 1's booking
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.confirmBooking(holdResp.getBookingId(), "MOCK_CARD", customer2.getUserId());
        });
    }

    @Test
    public void testConfirmBookingIdempotency() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(activeShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());
        BookingResponse firstConfirm = bookingService.confirmBooking(holdResp.getBookingId(), "MOCK_CARD", customer1.getUserId());
        assertEquals(BookingStatus.CONFIRMED, firstConfirm.getStatus());

        // Second confirmation call on already confirmed booking returns clean success
        BookingResponse secondConfirm = bookingService.confirmBooking(holdResp.getBookingId(), "MOCK_CARD", customer1.getUserId());
        assertEquals(BookingStatus.CONFIRMED, secondConfirm.getStatus());
        assertEquals(firstConfirm.getBookingRef(), secondConfirm.getBookingRef());
    }

    @Test
    public void testExpiredHoldsReleased() {
        ShowSeat seat = showSeats.get(2);
        seat.setStatus(ShowSeatStatus.HELD);
        seat.setHeldUntil(LocalDateTime.now().minusMinutes(5));
        showSeatRepository.save(seat);

        bookingService.cleanupExpiredHolds();

        ShowSeat reloaded = showSeatRepository.findById(seat.getShowSeatId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals(ShowSeatStatus.AVAILABLE, reloaded.getStatus());
        assertNull(reloaded.getHeldUntil());
    }
}
