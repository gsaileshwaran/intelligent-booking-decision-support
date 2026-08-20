package com.booking.intelligent.service;

import com.booking.intelligent.dto.ShowRequestDto;
import com.booking.intelligent.dto.ShowResponseDto;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.enums.ShowStatus;
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
public class ShowServiceTest {

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

    private User providerUser;
    private User otherProviderUser;
    private Theatre pvkBranch;
    private Screen screen1;
    private Movie testMovie;

    @BeforeEach
    public void setup() {
        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        providerUser = userRepository.save(User.builder()
                .email("provider_test_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("PVK Owner Test")
                .role(providerRole)
                .build());

        otherProviderUser = userRepository.save(User.builder()
                .email("other_provider_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Other Cinema Owner")
                .role(providerRole)
                .build());

        pvkBranch = theatreRepository.save(Theatre.builder()
                .name("PVK — Test Branch")
                .location("Chennai")
                .address("Test Road")
                .ownerUser(providerUser)
                .build());

        screen1 = screenRepository.save(Screen.builder()
                .name("Screen 1")
                .capacity(100)
                .theatre(pvkBranch)
                .build());

        testMovie = movieRepository.save(Movie.builder()
                .title("Test Movie " + System.currentTimeMillis())
                .duration(120)
                .genre("Action")
                .language("English")
                .releaseDate(LocalDate.now())
                .build());
    }

    @Test
    public void testNonOverlappingShowsAccepted() {
        Show show1 = Show.builder()
                .showDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build();
        showService.createShow(show1, testMovie.getMovieId(), screen1.getScreenId());

        Show show2 = Show.builder()
                .showDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(15, 30))
                .ticketPrice(BigDecimal.valueOf(220.00))
                .build();

        assertDoesNotThrow(() -> showService.createShow(show2, testMovie.getMovieId(), screen1.getScreenId()));
    }

    @Test
    public void testOverlappingShowsRejected() {
        Show show1 = Show.builder()
                .showDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build();
        showService.createShow(show1, testMovie.getMovieId(), screen1.getScreenId());

        // Overlapping show: 12:00 to 14:00 overlaps with 10:00 to 12:30
        Show show2 = Show.builder()
                .showDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(14, 0))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            showService.createShow(show2, testMovie.getMovieId(), screen1.getScreenId());
        });

        assertTrue(exception.getMessage().contains("Showtime conflict"));
    }

    @Test
    public void testInvalidTicketPriceRejected() {
        Show show1 = Show.builder()
                .showDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(-50.00))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            showService.createShow(show1, testMovie.getMovieId(), screen1.getScreenId());
        });

        assertTrue(exception.getMessage().contains("Ticket price must be positive"));
    }

    @Test
    public void testProviderOwnershipAccessControl() {
        Show show1 = Show.builder()
                .showDate(LocalDate.now().plusDays(4))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build();
        Show created = showService.createShow(show1, testMovie.getMovieId(), screen1.getScreenId());

        // Owner provider can fetch show
        assertDoesNotThrow(() -> showService.getShowForOwner(created.getShowId(), providerUser.getUserId()));

        // Other provider attempting to access show triggers AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            showService.getShowForOwner(created.getShowId(), otherProviderUser.getUserId());
        });
    }

    @Test
    public void testCancelShowPreservesRecordsAndUpdatesStatus() {
        Show show1 = Show.builder()
                .showDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 30))
                .ticketPrice(BigDecimal.valueOf(200.00))
                .build();
        Show created = showService.createShow(show1, testMovie.getMovieId(), screen1.getScreenId());

        ShowResponseDto cancelled = showService.cancelShow(created.getShowId(), providerUser.getUserId());

        assertEquals(ShowStatus.CANCELLED, cancelled.getStatus());
        Show reloaded = showRepository.findById(created.getShowId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals(ShowStatus.CANCELLED, reloaded.getStatus());
    }
}
