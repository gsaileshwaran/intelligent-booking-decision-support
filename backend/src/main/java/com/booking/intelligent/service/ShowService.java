package com.booking.intelligent.service;

import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.entity.Screen;
import com.booking.intelligent.entity.Seat;
import com.booking.intelligent.entity.Show;
import com.booking.intelligent.entity.ShowSeat;
import com.booking.intelligent.enums.ShowSeatStatus;
import com.booking.intelligent.enums.ShowStatus;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    public List<Show> getShowsByMovieAndDate(Long movieId, LocalDate date) {
        if (date == null) {
            return showRepository.findByMovieMovieIdAndStatus(movieId, ShowStatus.ACTIVE);
        }
        return showRepository.findByMovieMovieIdAndShowDate(movieId, date);
    }

    public Show getShowById(Long showId) {
        return showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", "id", showId));
    }

    public List<ShowSeat> getShowSeats(Long showId) {
        return showSeatRepository.findByShowShowId(showId);
    }

    @Transactional
    public Show createShow(Show show, Long movieId, Long screenId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        show.setMovie(movie);
        show.setScreen(screen);
        if (show.getStatus() == null) {
            show.setStatus(ShowStatus.ACTIVE);
        }

        Show savedShow = showRepository.save(show);

        // Generate ShowSeat inventory for all physical seats in screen
        List<Seat> physicalSeats = seatRepository.findByScreenScreenId(screenId);
        for (Seat physicalSeat : physicalSeats) {
            BigDecimal seatPrice = calculateSeatPrice(show.getTicketPrice(), physicalSeat.getSeatType().name());
            ShowSeat showSeat = ShowSeat.builder()
                    .show(savedShow)
                    .seat(physicalSeat)
                    .price(seatPrice)
                    .status(ShowSeatStatus.AVAILABLE)
                    .build();
            showSeatRepository.save(showSeat);
        }

        return savedShow;
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, String seatType) {
        if ("BALCONY".equalsIgnoreCase(seatType)) {
            return basePrice.add(BigDecimal.valueOf(5.00));
        } else if ("PREMIUM".equalsIgnoreCase(seatType)) {
            return basePrice.add(BigDecimal.valueOf(3.00));
        }
        return basePrice;
    }
}
