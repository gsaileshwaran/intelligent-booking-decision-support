package com.booking.intelligent.service;

import com.booking.intelligent.config.DataInitializer;
import com.booking.intelligent.entity.Movie;
import com.booking.intelligent.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MovieCatalogueTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private DataInitializer dataInitializer;

    @Test
    public void testSeededCatalogueIntegrity() {
        List<Movie> allMovies = movieService.getAllActiveMovies();
        assertTrue(allMovies.size() >= 10, "Expected a substantial movie catalogue seeded.");

        Movie ramayana = movieRepository.findByTitle("Ramayana: Part 1").orElse(null);
        assertNotNull(ramayana);
        assertEquals("Hindi", ramayana.getLanguage());
        assertNotNull(ramayana.getPosterUrl());
        assertNotNull(ramayana.getBackdropUrl());
        assertEquals("CONFIRMED", ramayana.getReleaseDateStatus());
        assertNull(ramayana.getRating(), "Upcoming movies must have null rating (Not Rated Yet)");
        assertNotNull(ramayana.getAnticipationScore(), "Anticipation score must be set");

        Movie jailer2 = movieRepository.findByTitle("Jailer 2").orElse(null);
        assertNotNull(jailer2);
        assertEquals("Tamil", jailer2.getLanguage());
        assertTrue(jailer2.getIsUpcoming());
        assertNull(jailer2.getRating(), "Upcoming movie must not have fake rating");

        Movie king = movieRepository.findByTitle("King").orElse(null);
        assertNotNull(king);
        assertEquals(2026, king.getReleaseYear());
        assertTrue(king.getIsUpcoming());
    }

    @Test
    public void testIdempotentDataInitializerExecution() throws Exception {
        long initialCount = movieRepository.count();
        dataInitializer.run();
        long secondCount = movieRepository.count();

        assertEquals(initialCount, secondCount, "DataInitializer must be idempotent and not create duplicate movie records.");
    }

    @Test
    public void testMovieSearchByTitleCastDirectorGenreLanguage() {
        List<Movie> rajiniMovies = movieService.searchMovies("Rajinikanth");
        assertFalse(rajiniMovies.isEmpty());
        assertTrue(rajiniMovies.stream().anyMatch(m -> m.getTitle().equals("Jailer 2")));

        List<Movie> niteshMovies = movieService.searchMovies("Nitesh Tiwari");
        assertFalse(niteshMovies.isEmpty());
        assertTrue(niteshMovies.stream().anyMatch(m -> m.getTitle().equals("Ramayana: Part 1")));

        List<Movie> tamilMovies = movieService.filterMovies("Tamil", "ALL", "ALL", null, null);
        assertFalse(tamilMovies.isEmpty());
        assertTrue(tamilMovies.stream().allMatch(m ->
            (m.getLanguage() != null && m.getLanguage().equalsIgnoreCase("Tamil")) ||
            (m.getLanguages() != null && m.getLanguages().toLowerCase().contains("tamil"))
        ));
    }

    @Test
    public void testAnticipationScoreSorting() {
        List<Movie> sortedByAnticipation = movieService.filterMovies(null, null, null, null, "ANTICIPATION");
        assertFalse(sortedByAnticipation.isEmpty());
        for (int i = 0; i < sortedByAnticipation.size() - 1; i++) {
            Integer score1 = sortedByAnticipation.get(i).getAnticipationScore() != null ? sortedByAnticipation.get(i).getAnticipationScore() : 0;
            Integer score2 = sortedByAnticipation.get(i + 1).getAnticipationScore() != null ? sortedByAnticipation.get(i + 1).getAnticipationScore() : 0;
            assertTrue(score1 >= score2, "Movies should be sorted by anticipation score descending");
        }
    }
}
