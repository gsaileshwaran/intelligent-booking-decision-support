package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.entity.Screen;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.Theatre;
import com.booking.intelligent.repository.ScreenRepository;
import com.booking.intelligent.repository.ShowRepository;
import com.booking.intelligent.service.TheatreService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {

    @Autowired
    private TheatreService theatreService;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private ShowRepository showRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Theatre>>> getAllTheatres(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String city) {
        List<Theatre> theatres = theatreService.getAllTheatres();
        String targetCity = (city != null && !city.trim().isEmpty()) ? city : location;
        if (targetCity != null && !targetCity.trim().isEmpty() && !targetCity.equalsIgnoreCase("ALL")) {
            theatres = theatres.stream()
                    .filter(t -> (t.getCity() != null && t.getCity().equalsIgnoreCase(targetCity)) ||
                                 (t.getLocation() != null && t.getLocation().equalsIgnoreCase(targetCity)))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success("Theatres retrieved successfully", theatres));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TheatreDetailsDto>> getTheatreDetails(@PathVariable Long id) {
        Theatre theatre = theatreService.getTheatreById(id);
        List<Screen> screens = screenRepository.findByTheatreTheatreId(id);
        List<Show> rawShows = showRepository.findByScreenTheatreTheatreId(id);

        LocalDate today = LocalDate.now();
        List<Show> activeUpcomingShows = rawShows.stream()
                .filter(s -> s.getShowDate() != null && (s.getShowDate().isEqual(today) || s.getShowDate().isAfter(today)))
                .collect(Collectors.toList());

        TheatreDetailsDto details = TheatreDetailsDto.builder()
                .theatreId(theatre.getTheatreId())
                .name(theatre.getName())
                .location(theatre.getLocation())
                .city(theatre.getCity())
                .locality(theatre.getLocality())
                .address(theatre.getAddress())
                .latitude(theatre.getLatitude())
                .longitude(theatre.getLongitude())
                .description(theatre.getDescription())
                .phone(theatre.getPhone())
                .operatingHours(theatre.getOperatingHours())
                .amenities(theatre.getAmenities())
                .formats(theatre.getFormats())
                .totalScreens(theatre.getTotalScreens() != null ? theatre.getTotalScreens() : screens.size())
                .totalCapacity(theatre.getTotalCapacity() != null ? theatre.getTotalCapacity() : screens.stream().mapToInt(Screen::getCapacity).sum())
                .status(theatre.getStatus().name())
                .screens(screens)
                .shows(activeUpcomingShows)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Theatre details retrieved", details));
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TheatreDetailsDto {
        private Long theatreId;
        private String name;
        private String location;
        private String city;
        private String locality;
        private String address;
        private Double latitude;
        private Double longitude;
        private String description;
        private String phone;
        private String operatingHours;
        private String amenities;
        private String formats;
        private Integer totalScreens;
        private Integer totalCapacity;
        private String status;
        private List<Screen> screens;
        private List<Show> shows;
    }
}
