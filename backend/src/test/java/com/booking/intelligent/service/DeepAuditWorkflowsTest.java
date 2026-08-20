package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.dto.ShowRequestDto;
import com.booking.intelligent.dto.ShowResponseDto;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.PaymentStatus;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DeepAuditWorkflowsTest {

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

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

    private User provider;
    private User customer;
    private Theatre theatre;
    private Screen screen;
    private Movie movie;
    private Show futureShow;
    private Show pastShow;

    @BeforeEach
    public void setup() {
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        provider = userRepository.save(User.builder()
                .email("audit_prov_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Audit Provider")
                .role(providerRole)
                .build());

        customer = userRepository.save(User.builder()
                .email("audit_cust_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Audit Customer")
                .role(customerRole)
                .build());

        theatre = theatreRepository.save(Theatre.builder()
                .name("PVK — Deep Audit Branch")
                .location("Chennai")
                .address("Audit Road")
                .ownerUser(provider)
                .build());

        screen = screenRepository.save(Screen.builder()
                .name("Screen 1")
                .capacity(50)
                .theatre(theatre)
                .build());

        movie = movieRepository.save(Movie.builder()
                .title("Audit Blockbuster " + System.currentTimeMillis())
                .duration(120)
                .genre("Sci-Fi")
                .language("Tamil")
                .releaseDate(LocalDate.now())
                .build());

        futureShow = showService.createShow(Show.builder()
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
    }

    @Test
    public void testPastShowtimesHiddenFromCustomerListing() {
        List<Show> customerShows = showService.getShowsByMovieAndDate(movie.getMovieId(), null);
        assertNotNull(customerShows);

        // Verify future show is included and past show is excluded
        boolean hasFutureShow = customerShows.stream().anyMatch(s -> s.getShowId().equals(futureShow.getShowId()));
        boolean hasPastShow = customerShows.stream().anyMatch(s -> s.getShowId().equals(pastShow.getShowId()));

        assertTrue(hasFutureShow);
        assertFalse(hasPastShow);
    }

    @Test
    public void testProviderCancelShowAutomaticallyCancelsBookingsAndRefunds() {
        List<ShowSeat> seats = showService.getShowSeats(futureShow.getShowId());
        ShowSeat seat1 = seats.get(0);

        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(seat1.getShowSeatId()))
                .build();

        BookingResponse holdResp = bookingService.holdSeats(holdReq, customer.getUserId());
        BookingResponse confirmResp = bookingService.confirmBooking(holdResp.getBookingId(), "UPI", customer.getUserId());
        assertEquals(BookingStatus.CONFIRMED, confirmResp.getStatus());

        // Provider cancels show
        ShowResponseDto cancelResp = showService.cancelShow(futureShow.getShowId(), provider.getUserId());
        assertEquals(ShowStatus.CANCELLED, cancelResp.getStatus());

        // Verify seats released
        ShowSeat updatedSeat = showSeatRepository.findById(seat1.getShowSeatId()).orElseThrow();
        assertEquals(ShowSeatStatus.AVAILABLE, updatedSeat.getStatus());

        // Verify booking cancelled & payment refunded
        Booking updatedBooking = bookingRepository.findById(confirmResp.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());

        Payment updatedPayment = paymentRepository.findByBookingBookingId(confirmResp.getBookingId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, updatedPayment.getStatus());
    }

    @Test
    public void testShowEditValidatesOverlaps() {
        Show show2 = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(21, 0))
                .endTime(LocalTime.of(23, 30))
                .ticketPrice(BigDecimal.valueOf(250.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        // Attempt to edit show2 start time to overlap with futureShow (18:00 to 20:30)
        ShowRequestDto overlapEditReq = ShowRequestDto.builder()
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(21, 30))
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            showService.updateShow(show2.getShowId(), overlapEditReq, provider.getUserId());
        });
    }
}
