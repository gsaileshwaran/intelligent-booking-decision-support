package com.booking.intelligent.service;

import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.enums.MovieStatus;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Movie> searchMovies(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllActiveMovies();
        }
        return movieRepository.searchMovies(query.trim());
    }

    public List<Movie> filterMovies(String language, String genre, String status, String search, String sort) {
        List<Movie> movies = (search != null && !search.trim().isEmpty())
                ? movieRepository.searchMovies(search.trim())
                : movieRepository.findByStatus(MovieStatus.ACTIVE);

        return movies.stream()
                .filter(m -> {
                    if (language != null && !language.equalsIgnoreCase("ALL") && !language.trim().isEmpty()) {
                        String mLang = m.getLanguage() != null ? m.getLanguage().toLowerCase() : "";
                        String mLangs = m.getLanguages() != null ? m.getLanguages().toLowerCase() : "";
                        String qLang = language.toLowerCase();
                        if (!mLang.contains(qLang) && !mLangs.contains(qLang)) return false;
                    }

                    if (genre != null && !genre.equalsIgnoreCase("ALL") && !genre.trim().isEmpty()) {
                        String mGenre = m.getGenre() != null ? m.getGenre().toLowerCase() : "";
                        if (!mGenre.contains(genre.toLowerCase())) return false;
                    }

                    if (status != null && !status.equalsIgnoreCase("ALL") && !status.trim().isEmpty()) {
                        if ("UPCOMING".equalsIgnoreCase(status) && (m.getIsUpcoming() == null || !m.getIsUpcoming())) return false;
                        if ("NOW_SHOWING".equalsIgnoreCase(status) && (m.getIsNowShowing() == null || !m.getIsNowShowing())) return false;
                    }

                    return true;
                })
                .sorted((m1, m2) -> {
                    if ("ANTICIPATION".equalsIgnoreCase(sort)) {
                        Integer a1 = m1.getAnticipationScore() != null ? m1.getAnticipationScore() : 0;
                        Integer a2 = m2.getAnticipationScore() != null ? m2.getAnticipationScore() : 0;
                        return a2.compareTo(a1);
                    }
                    if ("RELEASE_DATE".equalsIgnoreCase(sort)) {
                        if (m1.getReleaseDate() == null) return 1;
                        if (m2.getReleaseDate() == null) return -1;
                        return m1.getReleaseDate().compareTo(m2.getReleaseDate());
                    }
                    if ("RATING".equalsIgnoreCase(sort)) {
                        Double r1 = m1.getRating() != null ? m1.getRating() : 0.0;
                        Double r2 = m2.getRating() != null ? m2.getRating() : 0.0;
                        return r2.compareTo(r1);
                    }
                    if ("TITLE".equalsIgnoreCase(sort)) {
                        return m1.getTitle().compareToIgnoreCase(m2.getTitle());
                    }
                    // Default: ID desc
                    return m2.getMovieId().compareTo(m1.getMovieId());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Movie createMovie(Movie movie) {
        if (movie.getStatus() == null) {
            movie.setStatus(MovieStatus.ACTIVE);
        }
        return movieRepository.save(movie);
    }
}
