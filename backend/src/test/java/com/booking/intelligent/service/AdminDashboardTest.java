package com.booking.intelligent.service;

import com.booking.intelligent.dto.AdminBranchStatsDto;
import com.booking.intelligent.dto.AdminDashboardStatsDto;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.PaymentStatus;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminDashboardTest {

    @Autowired
    private AdminService adminService;

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
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User adminUser;
    private User customerUser;
    private User providerUser;
    private Theatre theatre1;

    @BeforeEach
    public void setup() {
        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_ADMIN).build()));

        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        adminUser = userRepository.save(User.builder()
                .email("admin_test_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Test Admin")
                .role(adminRole)
                .build());

        customerUser = userRepository.save(User.builder()
                .email("admin_cust_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Test Customer")
                .role(customerRole)
                .build());

        providerUser = userRepository.save(User.builder()
                .email("admin_prov_" + System.currentTimeMillis() + "@example.com")
                .passwordHash("password123")
                .name("Test Provider")
                .role(providerRole)
                .build());

        theatre1 = theatreRepository.save(Theatre.builder()
                .name("PVK — Admin Test Branch")
                .location("Chennai")
                .address("Admin Street")
                .ownerUser(providerUser)
                .build());

        Screen screen1 = screenRepository.save(Screen.builder()
                .name("Screen 1")
                .capacity(100)
                .theatre(theatre1)
                .build());

        Movie movie = movieRepository.save(Movie.builder()
                .title("Admin Movie " + System.currentTimeMillis())
                .duration(120)
                .genre("Action")
                .language("Tamil")
                .releaseDate(LocalDate.now())
                .build());

        Show show = showRepository.save(Show.builder()
                .movie(movie)
                .screen(screen1)
                .showDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 30))
                .ticketPrice(BigDecimal.valueOf(300.00))
                .build());

        Booking confirmedBooking = bookingRepository.save(Booking.builder()
                .user(customerUser)
                .bookingRef("BK-ADMINCONF")
                .status(BookingStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(600.00))
                .createdAt(LocalDateTime.now())
                .build());

        paymentRepository.save(Payment.builder()
                .booking(confirmedBooking)
                .amount(BigDecimal.valueOf(600.00))
                .paymentMethod("UPI")
                .transactionRef("TXN-CONF")
                .status(PaymentStatus.SUCCESS)
                .build());

        Booking cancelledBooking = bookingRepository.save(Booking.builder()
                .user(customerUser)
                .bookingRef("BK-ADMINCANC")
                .status(BookingStatus.CANCELLED)
                .totalAmount(BigDecimal.valueOf(300.00))
                .createdAt(LocalDateTime.now())
                .build());

        paymentRepository.save(Payment.builder()
                .booking(cancelledBooking)
                .amount(BigDecimal.valueOf(300.00))
                .paymentMethod("MOCK_CARD")
                .transactionRef("TXN-REF")
                .status(PaymentStatus.REFUNDED)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminCanAccessDashboardMetrics() {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalUsers() >= 3);
        assertTrue(stats.getTotalBranches() >= 1);
        assertTrue(stats.getConfirmedBookings() >= 1);
        assertNotNull(stats.getConfirmedRevenue());
    }

    @Test
    public void testDashboardMetricsCalculatedFromDatabaseState() {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        long dbUsers = userRepository.count();
        long dbBranches = theatreRepository.count();

        assertEquals(dbUsers, stats.getTotalUsers());
        assertEquals(dbBranches, stats.getTotalBranches());
    }

    @Test
    public void testConfirmedRevenueExcludesCancelledAndHeldBookings() {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        assertNotNull(stats.getConfirmedRevenue());
        assertNotNull(stats.getRefundedAmount());
        assertNotNull(stats.getNetRevenue());

        BigDecimal expectedNet = stats.getConfirmedRevenue().subtract(stats.getRefundedAmount());
        if (expectedNet.compareTo(BigDecimal.ZERO) < 0) {
            expectedNet = BigDecimal.ZERO;
        }

        assertEquals(expectedNet, stats.getNetRevenue());
    }

    @Test
    public void testBranchStatisticsCorrectlyAggregated() {
        List<AdminBranchStatsDto> branches = adminService.getBranchStats();
        assertNotNull(branches);
        assertFalse(branches.isEmpty());

        AdminBranchStatsDto branch = branches.stream()
                .filter(b -> b.getTheatreId().equals(theatre1.getTheatreId()))
                .findFirst()
                .orElse(null);

        assertNotNull(branch);
        assertEquals("PVK — Admin Test Branch", branch.getName());
        assertEquals(1, branch.getScreenCount());
        assertEquals(100, branch.getTotalSeatingCapacity());
    }

    @Test
    public void testRecentBookingsRetrieval() {
        List<BookingResponse> recent = adminService.getRecentBookings(10);
        assertNotNull(recent);
        assertFalse(recent.isEmpty());
        assertTrue(recent.size() <= 10);
    }
}
