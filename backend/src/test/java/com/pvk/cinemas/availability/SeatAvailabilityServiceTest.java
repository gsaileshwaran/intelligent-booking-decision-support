package com.pvk.cinemas.availability;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.availability.dto.ShowSeatAvailabilityResponse;
import com.pvk.cinemas.availability.model.ShowSeat;
import com.pvk.cinemas.availability.repository.ShowSeatRepository;
import com.pvk.cinemas.availability.service.SeatAvailabilityService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatAvailabilityServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private ScreenCapabilityRepository screenCapabilityRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private TheatreRepository theatreRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.pvk.cinemas.booking.service.SeatPricingService seatPricingService;

    @InjectMocks
    private SeatAvailabilityService seatAvailabilityService;

    private final Long showId = 1001L;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Seat availability API strictly reads SHOW_SEAT states without mutating data")
    void testReadOnlySeatAvailability() {
        Show show = new Show();
        show.setShowId(showId);
        show.setScreenCapabilityId(50L);
        show.setStartAt(Instant.now());
        when(showRepository.findById(showId)).thenReturn(Optional.of(show));

        ScreenCapability sc = new ScreenCapability(5L, 1L, 1L);
        sc.setScreenCapabilityId(50L);
        when(screenCapabilityRepository.findById(50L)).thenReturn(Optional.of(sc));

        Screen screen = new Screen();
        screen.setScreenId(5L);
        screen.setTheatreId(1L);
        screen.setScreenName("Audi 1");
        when(screenRepository.findById(5L)).thenReturn(Optional.of(screen));

        Theatre theatre = new Theatre();
        theatre.setTheatreId(1L);
        theatre.setTheatreName("PVK Prime");
        when(theatreRepository.findById(1L)).thenReturn(Optional.of(theatre));

        ShowSeat ss1 = new ShowSeat(showId, 501L, "AVAILABLE");
        ShowSeat ss2 = new ShowSeat(showId, 502L, "AVAILABLE");
        when(showSeatRepository.findByIdShowId(showId)).thenReturn(List.of(ss1, ss2));

        Seat seat1 = new Seat(5L, 1L, "A", "1");
        seat1.setSeatId(501L);
        Seat seat2 = new Seat(5L, 1L, "A", "2");
        seat2.setSeatId(502L);
        when(seatRepository.findAllById(List.of(501L, 502L))).thenReturn(List.of(seat1, seat2));

        SeatType seatType = new SeatType();
        seatType.setSeatTypeId(1L);
        seatType.setSeatTypeCode("REGULAR");
        seatType.setSeatTypeName("Regular Seat");
        when(seatTypeRepository.findById(1L)).thenReturn(Optional.of(seatType));

        ShowSeatAvailabilityResponse response = seatAvailabilityService.getShowSeatAvailability(showId);

        assertNotNull(response);
        assertEquals(showId, response.getShowId());
        assertEquals(2, response.getTotalSeats());
        assertEquals(2, response.getAvailableSeats());
        assertEquals(2, response.getSeats().size());

        // Verify zero write or update operations were performed
        verify(showSeatRepository, never()).save(any());
        verify(showSeatRepository, never()).saveAll(any());
        verify(showRepository, never()).save(any());
    }
}
