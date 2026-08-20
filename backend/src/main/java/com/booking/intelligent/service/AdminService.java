package com.booking.intelligent.service;

import com.booking.intelligent.dto.AdminBranchStatsDto;
import com.booking.intelligent.dto.AdminDashboardStatsDto;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.*;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminService {

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
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingService bookingService;

    public AdminDashboardStatsDto getDashboardStats() {
        // User Metrics
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRoleRoleName(RoleName.ROLE_CUSTOMER);
        long totalProviders = userRepository.countByRoleRoleName(RoleName.ROLE_SERVICE_PROVIDER);
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);

        // Cinema Metrics
        long totalBranches = theatreRepository.count();
        long totalScreens = screenRepository.count();
        long totalPhysicalSeats = seatRepository.count();
        long totalScheduledShows = showRepository.count();
        long activeShows = showRepository.countByStatus(ShowStatus.ACTIVE);
        long cancelledShows = showRepository.countByStatus(ShowStatus.CANCELLED);

        // Booking Metrics
        long totalBookings = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long heldBookings = bookingRepository.countByStatus(BookingStatus.HELD);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long expiredBookings = bookingRepository.countByStatus(BookingStatus.EXPIRED);

        // Revenue Metrics
        BigDecimal confirmedRevenue = bookingRepository.findByStatus(BookingStatus.CONFIRMED).stream()
                .map(Booking::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundedAmount = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netRevenue = confirmedRevenue.subtract(refundedAmount);
        if (netRevenue.compareTo(BigDecimal.ZERO) < 0) {
            netRevenue = BigDecimal.ZERO;
        }

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalProviders(totalProviders)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .totalBranches(totalBranches)
                .totalScreens(totalScreens)
                .totalPhysicalSeats(totalPhysicalSeats)
                .totalScheduledShows(totalScheduledShows)
                .activeShows(activeShows)
                .cancelledShows(cancelledShows)
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .pendingBookings(pendingBookings)
                .heldBookings(heldBookings)
                .cancelledBookings(cancelledBookings)
                .expiredBookings(expiredBookings)
                .confirmedRevenue(confirmedRevenue)
                .refundedAmount(refundedAmount)
                .netRevenue(netRevenue)
                .build();
    }

    public List<AdminBranchStatsDto> getBranchStats() {
        List<Theatre> theatres = theatreRepository.findAll();
        List<AdminBranchStatsDto> branchStats = new ArrayList<>();

        for (Theatre theatre : theatres) {
            List<Screen> screens = screenRepository.findByTheatreTheatreId(theatre.getTheatreId());
            int screenCount = screens.size();
            int totalCapacity = screens.stream().mapToInt(Screen::getCapacity).sum();

            List<Show> shows = showRepository.findByScreenTheatreTheatreId(theatre.getTheatreId());
            long scheduledShowsCount = shows.size();

            List<Booking> branchBookings = bookingRepository.findByTheatreId(theatre.getTheatreId());
            long confirmedBookingsCount = branchBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .count();

            BigDecimal confirmedRevenue = branchBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .map(Booking::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            branchStats.add(AdminBranchStatsDto.builder()
                    .theatreId(theatre.getTheatreId())
                    .name(theatre.getName())
                    .location(theatre.getLocation())
                    .address(theatre.getAddress())
                    .screenCount(screenCount)
                    .totalSeatingCapacity(totalCapacity)
                    .scheduledShowsCount(scheduledShowsCount)
                    .confirmedBookingsCount(confirmedBookingsCount)
                    .confirmedRevenue(confirmedRevenue)
                    .build());
        }

        return branchStats;
    }

    public List<BookingResponse> getRecentBookings(int limit) {
        List<Booking> recent = bookingRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        return recent.stream().map(b -> {
            Show show = b.getItems().isEmpty() ? null : b.getItems().get(0).getShowSeat().getShow();
            return bookingService.mapToBookingResponse(b, show, null);
        }).collect(Collectors.toList());
    }
}
