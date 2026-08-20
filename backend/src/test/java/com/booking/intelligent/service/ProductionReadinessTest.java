package com.booking.intelligent.service;

import com.booking.intelligent.config.DataInitializer;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.entity.Screen;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.ShowSeat;
import com.booking.intelligent.entity.User;
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
public class ProductionReadinessTest {

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    public void testSeatPricingMultipliers() {
        BigDecimal basePrice = BigDecimal.valueOf(200.00);

        BigDecimal regularPrice = showService.calculateSeatPrice(basePrice, "REGULAR");
        BigDecimal premiumPrice = showService.calculateSeatPrice(basePrice, "PREMIUM");
        BigDecimal balconyPrice = showService.calculateSeatPrice(basePrice, "BALCONY");

        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(regularPrice));
        assertEquals(0, BigDecimal.valueOf(250.00).compareTo(premiumPrice)); // 200 * 1.25
        assertEquals(0, BigDecimal.valueOf(300.00).compareTo(balconyPrice)); // 200 * 1.50
    }

    @Test
    public void testIdempotentSeedInitialization() throws Exception {
        long initialUsers = userRepository.count();
        long initialTheatres = theatreRepository.count();
        long initialScreens = screenRepository.count();
        long initialSeats = seatRepository.count();

        // Re-run initializer
        dataInitializer.run();

        assertEquals(initialUsers, userRepository.count());
        assertEquals(initialTheatres, theatreRepository.count());
        assertEquals(initialScreens, screenRepository.count());
        assertEquals(initialSeats, seatRepository.count());
    }

    @Test
    public void testCrossUserBookingAccessDenied() {
        User customer1 = userRepository.findByEmail("customer@example.com").orElseThrow();
        User adminUser = userRepository.findByEmail("admin@example.com").orElseThrow();

        Movie movie = movieRepository.findAll().stream().findFirst().orElseThrow();
        Screen screen = screenRepository.findAll().stream().findFirst().orElseThrow();

        Show futureShow = showService.createShow(Show.builder()
                .showDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(21, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build(), movie.getMovieId(), screen.getScreenId());

        List<ShowSeat> seats = showService.getShowSeats(futureShow.getShowId());

        SeatHoldRequest holdReq = SeatHoldRequest.builder()
                .showId(futureShow.getShowId())
                .showSeatIds(List.of(seats.get(0).getShowSeatId()))
                .build();

        BookingResponse b1 = bookingService.holdSeats(holdReq, customer1.getUserId());

        // Admin (or another user) attempting to access customer1's booking via customer getBookingById throws AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.getBookingById(b1.getBookingId(), adminUser.getUserId());
        });
    }

    @Test
    public void testCrossProviderShowAccessDenied() {
        User customerUser = userRepository.findByEmail("customer@example.com").orElseThrow();
        Show show = showRepository.findAll().stream().findFirst().orElseThrow();

        // Non-owner attempting provider show management action throws AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            showService.getShowForOwner(show.getShowId(), customerUser.getUserId());
        });
    }
}
