package com.pvk.cinemas.booking.service;

import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.booking.dto.BookingResponse;
import com.pvk.cinemas.booking.dto.CheckoutRequest;
import com.pvk.cinemas.booking.dto.SeatDetail;
import com.pvk.cinemas.booking.model.Booking;
import com.pvk.cinemas.booking.model.BookingSeat;
import com.pvk.cinemas.booking.model.Payment;
import com.pvk.cinemas.booking.model.SeatHold;
import com.pvk.cinemas.booking.repository.BookingRepository;
import com.pvk.cinemas.booking.repository.BookingSeatRepository;
import com.pvk.cinemas.booking.repository.PaymentRepository;
import com.pvk.cinemas.booking.repository.SeatHoldRepository;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieLanguageRepository;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import org.springframework.security.access.AccessDeniedException;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Kolkata"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("Asia/Kolkata"));

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final MovieLanguageRepository movieLanguageRepository;
    private final MovieRepository movieRepository;
    private final LanguageRepository languageRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final PresentationFormatRepository presentationFormatRepository;
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatHoldService seatHoldService;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          PaymentRepository paymentRepository,
                          SeatHoldRepository seatHoldRepository,
                          ShowSeatRepository showSeatRepository,
                          ShowRepository showRepository,
                          MovieLanguageRepository movieLanguageRepository,
                          MovieRepository movieRepository,
                          LanguageRepository languageRepository,
                          ScreenCapabilityRepository screenCapabilityRepository,
                          PresentationFormatRepository presentationFormatRepository,
                          ScreenRepository screenRepository,
                          TheatreRepository theatreRepository,
                          CityRepository cityRepository,
                          SeatRepository seatRepository,
                          SeatTypeRepository seatTypeRepository,
                          SeatHoldService seatHoldService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.movieRepository = movieRepository;
        this.languageRepository = languageRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.presentationFormatRepository = presentationFormatRepository;
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.seatHoldService = seatHoldService;
    }

    /**
     * Executes checkout with simulated payment.
     */
    @Transactional(noRollbackFor = ConflictException.class)
    public BookingResponse checkout(CheckoutRequest request, Long userId) {
        if (request == null || request.getHoldToken() == null || request.getHoldToken().isBlank()) {
            throw new IllegalArgumentException("Hold token is required for checkout");
        }

        List<SeatHold> holds = seatHoldRepository.findByHoldTokenAndStatus(request.getHoldToken(), "ACTIVE");
        if (holds.isEmpty()) {
            throw new ConflictException("Hold has expired or is invalid. Please select seats again.");
        }

        SeatHold firstHold = holds.get(0);
        if (firstHold.isExpired()) {
            // Release holds and fail
            for (SeatHold hold : holds) {
                hold.setStatus("RELEASED");
                showSeatRepository.findByIdShowIdAndIdSeatId(hold.getShowId(), hold.getSeatId()).ifPresent(ss -> {
                    if ("HELD".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                        ss.setAvailabilityStatus("AVAILABLE");
                        showSeatRepository.save(ss);
                    }
                });
            }
            seatHoldRepository.saveAll(holds);
            throw new ConflictException("Hold timer expired. Please reselect your seats.");
        }

        Long showId = firstHold.getShowId();
        List<Long> seatIds = holds.stream().map(SeatHold::getSeatId).toList();

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, BigDecimal> seatPrices = new HashMap<>();
        for (Long seatId : seatIds) {
            BigDecimal price = seatHoldService.determineSeatPrice(showId, seatId);
            seatPrices.put(seatId, price);
            totalAmount = totalAmount.add(price);
        }

        // Check simulated outcome
        if ("FAILED".equalsIgnoreCase(request.getSimulateOutcome())) {
            // Payment failed scenario: release seats and record failed payment
            log.info("Simulating payment failure for holdToken: {}", request.getHoldToken());
            for (SeatHold hold : holds) {
                hold.setStatus("RELEASED");
                showSeatRepository.findByIdShowIdAndIdSeatId(hold.getShowId(), hold.getSeatId()).ifPresent(ss -> {
                    if ("HELD".equalsIgnoreCase(ss.getAvailabilityStatus())) {
                        ss.setAvailabilityStatus("AVAILABLE");
                        showSeatRepository.save(ss);
                    }
                });
            }
            seatHoldRepository.saveAll(holds);

            String failPaymentRef = "PAY-SIM-FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Payment failedPayment = new Payment(failPaymentRef, null, totalAmount, request.getPaymentMethod(), "FAILED");
            paymentRepository.save(failedPayment);

            throw new ConflictException("Simulated payment failed. Seats have been released.");
        }

        // SUCCESS scenario: Confirm booking and transition seats to BOOKED
        String bookingRef = "BK-2026-" + String.format("%04d", (int)(Math.random() * 9000 + 1000)) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Booking booking = new Booking(bookingRef, userId, showId, totalAmount, "CONFIRMED");
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (Long seatId : seatIds) {
            BookingSeat bs = new BookingSeat(savedBooking.getBookingId(), seatId, seatPrices.get(seatId));
            bookingSeats.add(bs);

            // Update show seat status
            showSeatRepository.findByIdShowIdAndIdSeatId(showId, seatId).ifPresent(ss -> {
                ss.setAvailabilityStatus("BOOKED");
                showSeatRepository.save(ss);
            });
        }
        bookingSeatRepository.saveAll(bookingSeats);

        // Convert holds to booking
        for (SeatHold hold : holds) {
            hold.setStatus("CONVERTED_TO_BOOKING");
        }
        seatHoldRepository.saveAll(holds);

        // Record successful simulated payment
        String paymentRef = "PAY-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(paymentRef, savedBooking.getBookingId(), totalAmount, request.getPaymentMethod(), "SUCCESS");
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Successfully created booking {} (ref: {}) for user {} on show {}", savedBooking.getBookingId(), bookingRef, userId, showId);

        return buildBookingResponse(savedBooking, bookingSeats, savedPayment);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String bookingReference, Long requestingUserId, boolean isSuperAdmin) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        if (!isSuperAdmin && !booking.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("You are not authorized to view this booking");
        }

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getBookingId());
        Payment payment = paymentRepository.findByBookingId(booking.getBookingId()).orElse(null);

        return buildBookingResponse(booking, bookingSeats, payment);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<BookingResponse> responses = new ArrayList<>();

        for (Booking b : bookings) {
            List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(b.getBookingId());
            Payment payment = paymentRepository.findByBookingId(b.getBookingId()).orElse(null);
            responses.add(buildBookingResponse(b, bookingSeats, payment));
        }

        return responses;
    }

    private BookingResponse buildBookingResponse(Booking booking, List<BookingSeat> bookingSeats, Payment payment) {
        BookingResponse resp = new BookingResponse();
        resp.setBookingId(booking.getBookingId());
        resp.setBookingReference(booking.getBookingReference());
        resp.setShowId(booking.getShowId());
        resp.setTotalAmount(booking.getTotalAmount());
        resp.setBookingStatus(booking.getBookingStatus());
        resp.setBookedAt(booking.getCreatedAt());

        if (payment != null) {
            resp.setPaymentMethod(payment.getPaymentMethod());
            resp.setPaymentStatus(payment.getPaymentStatus());
            resp.setPaymentReference(payment.getPaymentReference());
        }

        // Populate Show, Movie, Screen, Theatre metadata
        showRepository.findById(booking.getShowId()).ifPresent(show -> {
            resp.setShowDate(DATE_FMT.format(show.getStartAt()));
            resp.setShowTime(TIME_FMT.format(show.getStartAt()));

            movieLanguageRepository.findById(show.getMovieLanguageId()).ifPresent(ml -> {
                movieRepository.findById(ml.getMovieId()).ifPresent(m -> {
                    resp.setMovieTitle(m.getTitle());
                    resp.setPosterUrl(m.getPosterUrl());
                });
                languageRepository.findById(ml.getLanguageId()).ifPresent(l -> {
                    resp.setLanguageName(l.getLanguageName());
                });
            });

            screenCapabilityRepository.findById(show.getScreenCapabilityId()).ifPresent(sc -> {
                presentationFormatRepository.findById(sc.getPresentationFormatId()).ifPresent(pf -> {
                    resp.setFormatName(pf.getName());
                });
                screenRepository.findById(sc.getScreenId()).ifPresent(scr -> {
                    resp.setScreenName(scr.getScreenName());
                    theatreRepository.findById(scr.getTheatreId()).ifPresent(th -> {
                        resp.setTheatreName(th.getTheatreName());
                        if (th.getCityId() != null) {
                            cityRepository.findById(th.getCityId().intValue()).ifPresent(c -> {
                                resp.setCityName(c.getCityName());
                            });
                        }
                    });
                });
            });
        });

        // Populate seat details
        List<SeatDetail> seatDetails = new ArrayList<>();
        for (BookingSeat bs : bookingSeats) {
            SeatDetail sd = new SeatDetail();
            sd.setSeatId(bs.getSeatId());
            sd.setPrice(bs.getPrice());

            seatRepository.findById(bs.getSeatId()).ifPresent(s -> {
                sd.setRowCode(s.getRowLabel());
                try {
                    sd.setSeatNumber(Integer.parseInt(s.getSeatNumber()));
                } catch (Exception ignored) {}
                seatTypeRepository.findById(s.getSeatTypeId()).ifPresent(st -> {
                    sd.setSeatType(st.getName());
                });
            });
            seatDetails.add(sd);
        }
        resp.setSeats(seatDetails);

        return resp;
    }
}
