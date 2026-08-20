package com.booking.intelligent.service;

import com.booking.intelligent.dto.ProviderDashboardStats;
import com.booking.intelligent.entity.Booking;
import com.booking.intelligent.entity.Screen;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.Theatre;
import com.booking.intelligent.entity.User;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.TheatreStatus;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.BookingRepository;
import com.booking.intelligent.repository.ScreenRepository;
import com.booking.intelligent.repository.ShowRepository;
import com.booking.intelligent.repository.TheatreRepository;
import com.booking.intelligent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TheatreService {

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    public List<Theatre> getTheatresByOwner(Long ownerUserId) {
        return theatreRepository.findByOwnerUserUserId(ownerUserId);
    }

    public Theatre getTheatreById(Long theatreId) {
        return theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", "id", theatreId));
    }

    public Theatre getTheatreForOwner(Long theatreId, Long ownerUserId) {
        Theatre theatre = getTheatreById(theatreId);
        if (!theatre.getOwnerUser().getUserId().equals(ownerUserId)) {
            throw new AccessDeniedException("Access Denied: You do not own this theatre branch.");
        }
        return theatre;
    }

    @Transactional
    public Theatre createTheatre(Theatre theatre, Long ownerUserId) {
        User ownerUser = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerUserId));

        theatre.setOwnerUser(ownerUser);
        if (theatre.getStatus() == null) {
            theatre.setStatus(TheatreStatus.ACTIVE);
        }
        return theatreRepository.save(theatre);
    }

    public ProviderDashboardStats getProviderDashboardStats(Long ownerUserId) {
        List<Theatre> theatres = getTheatresByOwner(ownerUserId);
        long totalBranches = theatres.size();

        long totalScreens = 0;
        long totalSeats = 0;
        long todaysShows = 0;
        long upcomingShows = 0;

        LocalDate today = LocalDate.now();

        for (Theatre t : theatres) {
            List<Screen> screens = screenRepository.findByTheatreTheatreId(t.getTheatreId());
            totalScreens += screens.size();
            for (Screen sc : screens) {
                totalSeats += sc.getCapacity();
            }

            List<Show> shows = showRepository.findByScreenTheatreTheatreId(t.getTheatreId());
            for (Show sh : shows) {
                if (sh.getShowDate() != null) {
                    if (sh.getShowDate().isEqual(today)) {
                        todaysShows++;
                    } else if (sh.getShowDate().isAfter(today)) {
                        upcomingShows++;
                    }
                }
            }
        }

        List<Booking> bookings = bookingRepository.findByProviderOwnerId(ownerUserId);
        long totalBookings = bookings.size();
        long confirmedBookings = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Booking b : bookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                confirmedBookings++;
                if (b.getTotalAmount() != null) {
                    totalRevenue = totalRevenue.add(b.getTotalAmount());
                }
            }
        }

        return ProviderDashboardStats.builder()
                .totalBranches(totalBranches)
                .totalScreens(totalScreens)
                .totalSeats(totalSeats)
                .todaysShows(todaysShows)
                .upcomingShows(upcomingShows)
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .totalRevenue(totalRevenue)
                .build();
    }

    public List<Screen> getScreensForOwnerTheatre(Long theatreId, Long ownerUserId) {
        getTheatreForOwner(theatreId, ownerUserId);
        return screenRepository.findByTheatreTheatreId(theatreId);
    }

    public List<Show> getShowsForOwnerTheatre(Long theatreId, Long ownerUserId) {
        getTheatreForOwner(theatreId, ownerUserId);
        return showRepository.findByScreenTheatreTheatreId(theatreId);
    }

    public List<Booking> getBookingsForOwnerTheatre(Long theatreId, Long ownerUserId) {
        getTheatreForOwner(theatreId, ownerUserId);
        return bookingRepository.findByTheatreId(theatreId);
    }
}
