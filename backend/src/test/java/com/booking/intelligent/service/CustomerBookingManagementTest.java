package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.PaymentStatus;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.exception.InvalidBookingStateException;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
public class CustomerBookingManagementTest {

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
    private Show upcomingShow;
    private Show pastShow;
    private List<ShowSeat> showSeats;

    @BeforeEach
    public void setup() {
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        User provider = userRepository.save(User.builder()
                .email("cust_mgmt_provider_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("PVK Provider")
                .role(providerRole)
                .build());

        customer1 = userRepository.save(User.builder()
                .email("cust_mgmt1_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Customer Mgmt One")
                .role(customerRole)
                .build());

        customer2 = userRepository.save(User.builder()
                .email("cust_mgmt2_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Customer Mgmt Two")
                .role(customerRole)
                .build());

        Theatre theatre = theatreRepository.save(Theatre.builder()
                .name("PVK — Management Branch")
                .location("Chennai")
                .address("Management Street")
                .ownerUser(provider)
                .build());

        Screen screen = screenRepository.save(Screen.builder()
                .name("Screen 1")
                .capacity(50)
                .theatre(theatre)
                .build());

        Movie movie = movieRepository.save(Movie.builder()
                .title("Management Movie " + System.currentTimeMillis())
                .duration(120)
                .genre("Drama")
                .language("Tamil")
                .releaseDate(LocalDate.now())
                .build());

        upcomingShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 30))
                .ticketPrice(BigDecimal.valueOf(250.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        pastShow = showRepository.save(Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(LocalDate.now().minusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .status(ShowStatus.ACTIVE)
                .build());

        showSeats = showService.getShowSeats(upcomingShow.getShowId());
    }

    @Test
    public void testCustomerCanViewOwnBooking() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(upcomingShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());
        BookingResponse retrieved = bookingService.getBookingById(holdResp.getBookingId(), customer1.getUserId());

        assertNotNull(retrieved);
        assertEquals(holdResp.getBookingRef(), retrieved.getBookingRef());
    }

    @Test
    public void testCustomerCannotViewAnotherCustomerBooking() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(upcomingShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());

        assertThrows(AccessDeniedException.class, () -> {
            bookingService.getBookingById(holdResp.getBookingId(), customer2.getUserId());
        });
    }

    @Test
    public void testCustomerCanCancelEligibleConfirmedBookingAndSimulateRefund() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(upcomingShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());
        BookingResponse confirmedResp = bookingService.confirmBooking(holdResp.getBookingId(), "UPI", customer1.getUserId());

        assertEquals(BookingStatus.CONFIRMED, confirmedResp.getStatus());

        // Cancel confirmed future booking
        BookingResponse cancelledResp = bookingService.cancelBooking(confirmedResp.getBookingId(), customer1.getUserId());

        assertEquals(BookingStatus.CANCELLED, cancelledResp.getStatus());
        assertNotNull(cancelledResp.getRefundRef());
        assertTrue(cancelledResp.getRefundRef().startsWith("REFUND-"));
        assertEquals(confirmedResp.getTotalAmount(), cancelledResp.getRefundedAmount());

        // Verify seats released back to AVAILABLE
        ShowSeat updatedSeat = showSeatRepository.findById(seat1.getShowSeatId()).orElse(null);
        assertNotNull(updatedSeat);
        assertEquals(ShowSeatStatus.AVAILABLE, updatedSeat.getStatus());

        // Verify payment status marked REFUNDED
        Payment payment = paymentRepository.findByBookingBookingId(confirmedResp.getBookingId()).orElse(null);
        assertNotNull(payment);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    public void testCustomerCannotCancelAnotherCustomerBooking() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(upcomingShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());

        assertThrows(AccessDeniedException.class, () -> {
            bookingService.cancelBooking(holdResp.getBookingId(), customer2.getUserId());
        });
    }

    @Test
    public void testCustomerCannotCancelExpiredBooking() {
        ShowSeat seat1 = showSeats.get(0);
        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(upcomingShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer1.getUserId());
        Booking booking = bookingRepository.findById(holdResp.getBookingId()).orElseThrow();
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        assertThrows(InvalidBookingStateException.class, () -> {
            bookingService.cancelBooking(holdResp.getBookingId(), customer1.getUserId());
        });
    }

    @Test
    public void testCustomerCannotCancelPastShowBooking() {
        List<ShowSeat> pastSeats = showService.getShowSeats(pastShow.getShowId());
        ShowSeat pastSeat = pastSeats.get(0);

        Booking booking = bookingRepository.save(Booking.builder()
                .user(customer1)
                .bookingRef("BK-PASTTEST")
                .status(BookingStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(200.00))
                .build());

        BookingItem item = bookingItemRepositorySave(booking, pastSeat);

        assertThrows(InvalidBookingStateException.class, () -> {
            bookingService.cancelBooking(booking.getBookingId(), customer1.getUserId());
        });
    }

    private BookingItem bookingItemRepositorySave(Booking booking, ShowSeat showSeat) {
        BookingItem item = BookingItem.builder()
                .booking(booking)
                .showSeat(showSeat)
                .price(showSeat.getPrice())
                .build();
        booking.getItems().add(item);
        bookingRepository.save(booking);
        return item;
    }
}
