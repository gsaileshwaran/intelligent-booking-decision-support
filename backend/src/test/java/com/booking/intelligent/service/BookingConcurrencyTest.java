package com.booking.intelligent.service;

import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.dto.SeatHoldRequest;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.enums.SeatType;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.enums.UserStatus;
import com.booking.intelligent.exception.SeatNotAvailableException;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    private User userA;
    private User userB;
    private Show targetShow;
    private ShowSeat targetShowSeat;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        User owner = userRepository.save(User.builder()
                .name("Provider Owner")
                .email("owner_" + System.currentTimeMillis() + "@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(providerRole)
                .status(UserStatus.ACTIVE)
                .build());

        userA = userRepository.save(User.builder()
                .name("User A")
                .email("usera_" + System.currentTimeMillis() + "@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(customerRole)
                .status(UserStatus.ACTIVE)
                .build());

        userB = userRepository.save(User.builder()
                .name("User B")
                .email("userb_" + System.currentTimeMillis() + "@example.com")
                .passwordHash(passwordEncoder.encode("password"))
                .role(customerRole)
                .status(UserStatus.ACTIVE)
                .build());

        Theatre theatre = theatreRepository.save(Theatre.builder()
                .ownerUser(owner)
                .name("Concurrency Cinema")
                .location("Downtown")
                .build());

        Screen screen = screenRepository.save(Screen.builder()
                .theatre(theatre)
                .name("Screen 1")
                .capacity(10)
                .build());

        Seat seat = seatRepository.save(Seat.builder()
                .screen(screen)
                .rowLabel("A")
                .seatNumber(1)
                .seatType(SeatType.REGULAR)
                .build());

        Movie movie = movieRepository.save(Movie.builder()
                .title("Concurrency Movie")
                .duration(120)
                .build());

        targetShow = showRepository.save(Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(LocalDate.now())
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .ticketPrice(BigDecimal.valueOf(15.00))
                .status(ShowStatus.ACTIVE)
                .build());

        targetShowSeat = showSeatRepository.save(ShowSeat.builder()
                .show(targetShow)
                .seat(seat)
                .status(ShowSeatStatus.AVAILABLE)
                .price(BigDecimal.valueOf(15.00))
                .build());
    }

    @Test
    void testConcurrentHoldOnSameSeatPreventsDoubleBooking() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        SeatHoldRequest request = new SeatHoldRequest();
        request.setShowId(targetShow.getShowId());
        request.setShowSeatIds(Collections.singletonList(targetShowSeat.getShowSeatId()));

        // Thread 1: User A
        executor.submit(() -> {
            try {
                startLatch.await();
                BookingResponse response = bookingService.holdSeats(request, userA.getUserId());
                if (response != null && response.getBookingId() != null) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread 2: User B
        executor.submit(() -> {
            try {
                startLatch.await();
                BookingResponse response = bookingService.holdSeats(request, userB.getUserId());
                if (response != null && response.getBookingId() != null) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        // Trigger simultaneous execution
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one user should succeed in holding the seat");
        assertEquals(1, failureCount.get(), "Competing user request should fail due to seat conflict lock");

        // Verify database state
        ShowSeat finalSeatState = showSeatRepository.findById(targetShowSeat.getShowSeatId()).orElseThrow();
        assertEquals(ShowSeatStatus.HELD, finalSeatState.getStatus(), "Final database status of seat must be HELD");
        assertNotNull(finalSeatState.getHeldUntil(), "Hold expiration timestamp must be set");
    }
}
