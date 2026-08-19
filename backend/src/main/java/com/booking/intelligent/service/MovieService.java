package com.booking.intelligent.service;

import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.enums.MovieStatus;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    public List<Movie> getAllActiveMovies() {
        return movieRepository.findByStatus(MovieStatus.ACTIVE);
    }

    public Movie getMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", "id", movieId));
    }

    @Transactional
    public Movie createMovie(Movie movie) {
        if (movie.getStatus() == null) {
            movie.setStatus(MovieStatus.ACTIVE);
        }
        return movieRepository.save(movie);
    }
}
