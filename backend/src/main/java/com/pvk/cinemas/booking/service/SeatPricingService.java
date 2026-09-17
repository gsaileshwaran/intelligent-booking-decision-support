package com.pvk.cinemas.booking.service;

import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative Unified Seat Pricing Engine.
 *
 * SeatPrice = BaseVenuePrice(Theatre + Offset) + FormatDelta + ZoneDelta + SeatTypeHardwareDelta
 *
 * Baseline Venue Price: ₹200.00 +/- Theatre Offset (-₹10 to +₹30)
 * Format Deltas:
 *   - 2D: +₹0.00
 *   - 3D: +₹40.00
 *   - Dolby / Atmos: +₹60.00
 *   - IMAX: +₹120.00
 *   - 4DX: +₹180.00
 * Zone Deltas:
 *   - VALUE: -₹40.00
 *   - STANDARD: +₹0.00
 *   - PREMIUM: +₹60.00
 * Hardware Seat Type Deltas:
 *   - STANDARD: +₹0.00
 *   - ACCESSIBLE: +₹0.00
 *   - PREMIUM: +₹20.00
 *   - RECLINER: +₹80.00
 */
@Service
public class SeatPricingService {

    private static final BigDecimal DEFAULT_VENUE_BASE = new BigDecimal("200.00");

    private final ShowRepository showRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final PresentationFormatRepository presentationFormatRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;

    private final Map<Long, ShowPricingContext> showContextCache = new ConcurrentHashMap<>();
    private final Map<Long, SeatType> seatTypeCache = new ConcurrentHashMap<>();

    public SeatPricingService(ShowRepository showRepository,
                              ScreenCapabilityRepository screenCapabilityRepository,
                              ScreenRepository screenRepository,
                              TheatreRepository theatreRepository,
                              PresentationFormatRepository presentationFormatRepository,
                              SeatRepository seatRepository,
                              SeatTypeRepository seatTypeRepository) {
        this.showRepository = showRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.presentationFormatRepository = presentationFormatRepository;
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
    }

    public static class ShowPricingContext {
        public final BigDecimal venueBasePrice;
        public final BigDecimal formatDelta;
        public final String formatName;

        public ShowPricingContext(BigDecimal venueBasePrice, BigDecimal formatDelta, String formatName) {
            this.venueBasePrice = venueBasePrice;
            this.formatDelta = formatDelta;
            this.formatName = formatName;
        }

        public BigDecimal getVenueBasePrice() { return venueBasePrice; }
        public BigDecimal getFormatDelta() { return formatDelta; }
        public String getFormatName() { return formatName; }
        public BigDecimal getBasePrice() { return venueBasePrice.add(formatDelta); }
    }

    public ShowPricingContext getShowPricingContext(Long showId) {
        if (showId == null) {
            return new ShowPricingContext(DEFAULT_VENUE_BASE, BigDecimal.ZERO, "2D");
        }
        return showContextCache.computeIfAbsent(showId, id -> {
            Show show = showRepository.findById(id).orElse(null);
            if (show == null) {
                return new ShowPricingContext(DEFAULT_VENUE_BASE, BigDecimal.ZERO, "2D");
            }

            ScreenCapability sc = screenCapabilityRepository.findById(show.getScreenCapabilityId()).orElse(null);
            if (sc == null) {
                return new ShowPricingContext(DEFAULT_VENUE_BASE, BigDecimal.ZERO, "2D");
            }

            Screen screen = screenRepository.findById(sc.getScreenId()).orElse(null);
            BigDecimal venueBase = DEFAULT_VENUE_BASE;
            if (screen != null) {
                Theatre theatre = theatreRepository.findById(screen.getTheatreId()).orElse(null);
                if (theatre != null && theatre.getBasePriceOffset() != null) {
                    venueBase = DEFAULT_VENUE_BASE.add(theatre.getBasePriceOffset());
                }
            }

            BigDecimal formatDelta = BigDecimal.ZERO;
            String formatName = "2D";
            PresentationFormat pf = presentationFormatRepository.findById(sc.getPresentationFormatId()).orElse(null);
            if (pf != null && pf.getName() != null) {
                formatName = pf.getName();
                String norm = formatName.toUpperCase();
                if (norm.contains("4DX")) {
                    formatDelta = new BigDecimal("180.00");
                } else if (norm.contains("IMAX")) {
                    formatDelta = new BigDecimal("120.00");
                } else if (norm.contains("DOLBY") || norm.contains("ATMOS")) {
                    formatDelta = new BigDecimal("60.00");
                } else if (norm.contains("3D")) {
                    formatDelta = new BigDecimal("40.00");
                }
            }

            return new ShowPricingContext(venueBase, formatDelta, formatName);
        });
    }

    /**
     * Authoritative calculation of a single seat price for a specific show.
     */
    public BigDecimal determineSeatPrice(Long showId, Long seatId) {
        ShowPricingContext showCtx = getShowPricingContext(showId);
        Seat seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null) {
            return showCtx.venueBasePrice.add(showCtx.formatDelta);
        }
        return calculateSeatPrice(seat, showCtx);
    }

    /**
     * Fast in-memory calculation using already loaded Seat and ShowPricingContext.
     */
    public BigDecimal calculateSeatPrice(Seat seat, ShowPricingContext showCtx) {
        BigDecimal base = showCtx != null ? showCtx.venueBasePrice : DEFAULT_VENUE_BASE;
        BigDecimal formatDelta = showCtx != null ? showCtx.formatDelta : BigDecimal.ZERO;

        // Zone Delta
        String zone = seat.getPricingZone() != null ? seat.getPricingZone().toUpperCase() : "STANDARD";
        BigDecimal zoneDelta = switch (zone) {
            case "VALUE" -> new BigDecimal("-40.00");
            case "PREMIUM" -> new BigDecimal("60.00");
            default -> BigDecimal.ZERO; // STANDARD
        };

        // Hardware Seat Type Delta
        BigDecimal hardwareDelta = BigDecimal.ZERO;
        if (seat.getSeatTypeId() != null) {
            SeatType st = seatTypeCache.computeIfAbsent(seat.getSeatTypeId(), id -> seatTypeRepository.findById(id).orElse(null));
            if (st != null && st.getTypeCode() != null) {
                String code = st.getTypeCode().toUpperCase();
                if ("RECLINER".equals(code)) {
                    hardwareDelta = new BigDecimal("80.00");
                } else if ("PREMIUM".equals(code)) {
                    hardwareDelta = new BigDecimal("20.00");
                }
            }
        }

        BigDecimal finalPrice = base.add(formatDelta).add(zoneDelta).add(hardwareDelta);
        return finalPrice.max(new BigDecimal("100.00")); // Hard floor protection
    }

    /**
     * Fallback seat price lookup when show context is not available.
     */
    public BigDecimal determineSeatPrice(Long seatId) {
        return determineSeatPrice(null, seatId);
    }

    /**
     * Baseline estimate for a show (standard zone, standard seat).
     */
    public BigDecimal determineShowBasePrice(Long showId) {
        ShowPricingContext showCtx = getShowPricingContext(showId);
        return showCtx.venueBasePrice.add(showCtx.formatDelta);
    }
}
