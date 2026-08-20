package com.booking.intelligent.service;

import com.booking.intelligent.dto.ShowRequestDto;
import com.booking.intelligent.dto.ShowResponseDto;
import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.enums.PaymentStatus;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.exception.InvalidBookingStateException;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowService {

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Show> getShowsByMovieAndDate(Long movieId, LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<Show> rawShows = (date == null)
                ? showRepository.findByMovieMovieIdAndStatus(movieId, ShowStatus.ACTIVE)
                : showRepository.findByMovieMovieIdAndShowDate(movieId, date).stream()
                        .filter(s -> s.getStatus() == ShowStatus.ACTIVE)
                        .collect(Collectors.toList());

        return rawShows.stream()
                .filter(s -> {
                    if (s.getShowDate() == null) return false;
                    if (s.getShowDate().isAfter(today)) return true;
                    if (s.getShowDate().isBefore(today)) return false;
                    // If showDate is today, check if endTime has passed
                    return s.getEndTime() == null || s.getEndTime().isAfter(nowTime);
                })
                .collect(Collectors.toList());
    }

    public Show getShowById(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", showId));
    }

    public Show getShowForOwner(Long showId, Long ownerUserId) {
        Show show = getShowById(showId);
        if (!show.getScreen().getTheatre().getOwnerUser().getUserId().equals(ownerUserId)) {
            throw new AccessDeniedException("Access Denied: You do not own the theatre branch for this show.");
        }
        return show;
    }

    public void validateTicketPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ticket price must be positive and greater than ₹0.00.");
        }
    }

    public void validateShowtimeConflict(Long screenId, LocalDate showDate, LocalTime startTime, LocalTime endTime, Long excludeShowId) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Show start time (" + startTime + ") must be earlier than end time (" + endTime + ").");
        }

        List<Show> existingShows = showRepository.findByScreenScreenIdAndShowDateAndStatusNot(screenId, showDate, ShowStatus.CANCELLED);
        for (Show existing : existingShows) {
            if (excludeShowId != null && existing.getShowId().equals(excludeShowId)) {
                continue;
            }

            // Overlap condition: startTime < existing.endTime AND endTime > existing.startTime
            if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
                throw new IllegalArgumentException(
                        "Showtime conflict: Screen already has an active show scheduled between " +
                        existing.getStartTime() + " and " + existing.getEndTime() + " (" + existing.getMovie().getTitle() + ")."
                );
            }
        }
    }

    @Transactional
    public Show createShow(Show show, Long movieId, Long screenId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        validateTicketPrice(show.getTicketPrice());
        validateShowtimeConflict(screenId, show.getShowDate(), show.getStartTime(), show.getEndTime(), null);

        show.setMovie(movie);
        show.setScreen(screen);
        show.setStatus(ShowStatus.ACTIVE);

        Show savedShow = showRepository.save(show);
        generateShowSeatsForShow(savedShow);

        return savedShow;
    }

    @Transactional
    public ShowResponseDto updateShow(Long showId, ShowRequestDto request, Long ownerUserId) {
        Show show = getShowForOwner(showId, ownerUserId);

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new InvalidBookingStateException("Cancelled show cannot be modified.");
        }

        LocalDate targetDate = request.getShowDate() != null ? request.getShowDate() : show.getShowDate();
        LocalTime targetStart = request.getStartTime() != null ? request.getStartTime() : show.getStartTime();
        LocalTime targetEnd = request.getEndTime() != null ? request.getEndTime() : show.getEndTime();

        // Check showtime conflict with other shows on the same screen (excluding this show)
        validateShowtimeConflict(show.getScreen().getScreenId(), targetDate, targetStart, targetEnd, showId);

        if (request.getTicketPrice() != null) {
            validateTicketPrice(request.getTicketPrice());
        }

        if (request.getShowDate() != null) show.setShowDate(request.getShowDate());
        if (request.getStartTime() != null) show.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) show.setEndTime(request.getEndTime());

        List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(showId);
        if (request.getTicketPrice() != null) {
            show.setTicketPrice(request.getTicketPrice());
            for (ShowSeat ss : showSeats) {
                if (ss.getStatus() != ShowSeatStatus.CONFIRMED) {
                    ss.setPrice(calculateSeatPrice(request.getTicketPrice(), ss.getSeat().getSeatType().name()));
                    showSeatRepository.save(ss);
                }
            }
        }

        Show updatedShow = showRepository.save(show);
        return mapToShowResponseDto(updatedShow);
    }

    @Transactional
    public ShowResponseDto cancelShow(Long showId, Long ownerUserId) {
        Show show = getShowForOwner(showId, ownerUserId);

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new InvalidBookingStateException("Show is already cancelled.");
        }

        show.setStatus(ShowStatus.CANCELLED);
        Show cancelledShow = showRepository.save(show);

        List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(showId);
        for (ShowSeat ss : showSeats) {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHeldUntil(null);
            showSeatRepository.save(ss);
        }

        // Cancel bookings and refund payments for this show
        List<Booking> showBookings = bookingRepository.findAll().stream()
                .filter(b -> !b.getItems().isEmpty() && 
                             b.getItems().get(0).getShowSeat() != null && 
                             b.getItems().get(0).getShowSeat().getShow() != null && 
                             b.getItems().get(0).getShowSeat().getShow().getShowId().equals(showId))
                .collect(Collectors.toList());

        for (Booking b : showBookings) {
            if (b.getStatus() == BookingStatus.HELD) {
                b.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(b);
            } else if (b.getStatus() == BookingStatus.CONFIRMED) {
                b.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(b);
                paymentRepository.findByBookingBookingId(b.getBookingId()).ifPresent(p -> {
                    if (p.getStatus() == PaymentStatus.SUCCESS) {
                        p.setStatus(PaymentStatus.REFUNDED);
                        paymentRepository.save(p);
                    }
                });
            }
        }

        return mapToShowResponseDto(cancelledShow);
    }

    public List<ShowResponseDto> getProviderShows(
            Long ownerUserId,
            Long branchId,
            Long screenId,
            Long movieId,
            String dateFilter,
            String statusFilter,
            String searchQuery) {

        List<Show> shows = showRepository.findByScreenTheatreOwnerUserUserId(ownerUserId);
        LocalDate today = LocalDate.now();

        return shows.stream()
                .filter(s -> {
                    if (!s.getScreen().getTheatre().getOwnerUser().getUserId().equals(ownerUserId)) return false;

                    if (branchId != null && !s.getScreen().getTheatre().getTheatreId().equals(branchId)) return false;

                    if (screenId != null && !s.getScreen().getScreenId().equals(screenId)) return false;

                    if (movieId != null && !s.getMovie().getMovieId().equals(movieId)) return false;

                    if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.isEmpty()) {
                        if (!s.getStatus().name().equalsIgnoreCase(statusFilter)) return false;
                    }

                    if (dateFilter != null && !dateFilter.equalsIgnoreCase("ALL") && !dateFilter.isEmpty()) {
                        LocalDate showDate = s.getShowDate();
                        if (showDate != null) {
                            if ("TODAY".equalsIgnoreCase(dateFilter) && !showDate.isEqual(today)) return false;
                            if ("UPCOMING".equalsIgnoreCase(dateFilter) && !showDate.isAfter(today)) return false;
                            if ("PAST".equalsIgnoreCase(dateFilter) && !showDate.isBefore(today)) return false;
                        }
                    }

                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String q = searchQuery.toLowerCase();
                        boolean matchMovie = s.getMovie().getTitle() != null && s.getMovie().getTitle().toLowerCase().contains(q);
                        boolean matchTheatre = s.getScreen().getTheatre().getName() != null && s.getScreen().getTheatre().getName().toLowerCase().contains(q);
                        if (!matchMovie && !matchTheatre) return false;
                    }

                    return true;
                })
                .map(this::mapToShowResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ShowSeat> getShowSeats(Long showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowShowId(showId);
        if (seats.isEmpty()) {
            Show show = getShowById(showId);
            List<Seat> physicalSeats = seatRepository.findByScreenScreenId(show.getScreen().getScreenId());
            if (physicalSeats.isEmpty()) {
                String[] rows = {"A", "B", "C", "D"};
                for (String r : rows) {
                    com.booking.intelligent.enums.SeatType type = r.equals("D") 
                            ? com.booking.intelligent.enums.SeatType.BALCONY 
                            : (r.equals("C") ? com.booking.intelligent.enums.SeatType.PREMIUM : com.booking.intelligent.enums.SeatType.REGULAR);
                    for (int i = 1; i <= 6; i++) {
                        Seat s = seatRepository.save(Seat.builder()
                                .screen(show.getScreen())
                                .rowLabel(r)
                                .seatNumber(i)
                                .seatType(type)
                                .build());
                        physicalSeats.add(s);
                    }
                }
            }

            for (Seat seat : physicalSeats) {
                BigDecimal seatPrice = calculateSeatPrice(show.getTicketPrice(), seat.getSeatType().name());
                ShowSeat showSeat = ShowSeat.builder()
                        .show(show)
                        .seat(seat)
                        .price(seatPrice)
                        .status(ShowSeatStatus.AVAILABLE)
                        .build();
                seats.add(showSeatRepository.save(showSeat));
            }
        }
        return seats;
    }

    private void generateShowSeatsForShow(Show show) {
        getShowSeats(show.getShowId());
    }

    public BigDecimal calculateSeatPrice(BigDecimal basePrice, String seatType) {
        if (basePrice == null) basePrice = BigDecimal.valueOf(200.00);
        if ("BALCONY".equalsIgnoreCase(seatType)) {
            return basePrice.multiply(BigDecimal.valueOf(1.50));
        } else if ("PREMIUM".equalsIgnoreCase(seatType)) {
            return basePrice.multiply(BigDecimal.valueOf(1.25));
        }
        return basePrice;
    }

    public ShowResponseDto mapToShowResponseDto(Show show) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowShowId(show.getShowId());
        int totalSeats = showSeats.size();
        int availableSeats = (int) showSeats.stream().filter(ss -> ss.getStatus() == ShowSeatStatus.AVAILABLE).count();
        int heldSeats = (int) showSeats.stream().filter(ss -> ss.getStatus() == ShowSeatStatus.HELD).count();
        int confirmedSeats = (int) showSeats.stream().filter(ss -> ss.getStatus() == ShowSeatStatus.CONFIRMED).count();

        return ShowResponseDto.builder()
                .showId(show.getShowId())
                .movieId(show.getMovie().getMovieId())
                .movieTitle(show.getMovie().getTitle())
                .movieGenre(show.getMovie().getGenre())
                .movieDuration(show.getMovie().getDuration())
                .theatreId(show.getScreen().getTheatre().getTheatreId())
                .theatreName(show.getScreen().getTheatre().getName())
                .screenId(show.getScreen().getScreenId())
                .screenName(show.getScreen().getName())
                .showDate(show.getShowDate())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .ticketPrice(show.getTicketPrice())
                .status(show.getStatus())
                .totalCapacity(totalSeats)
                .availableSeats(availableSeats)
                .heldSeats(heldSeats)
                .confirmedSeats(confirmedSeats)
                .hasConfirmedBookings(confirmedSeats > 0)
                .build();
    }
}
