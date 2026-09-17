package com.pvk.cinemas.catalogue.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.catalogue.dto.GenreResponse;
import com.pvk.cinemas.catalogue.dto.LanguageResponse;
import com.pvk.cinemas.catalogue.dto.MovieRequest;
import com.pvk.cinemas.catalogue.dto.MovieResponse;
import com.pvk.cinemas.catalogue.model.*;
import com.pvk.cinemas.catalogue.repository.*;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final LanguageRepository languageRepository;
    private final MovieLanguageRepository movieLanguageRepository;
    private final CertificationRepository certificationRepository;
    private final AuditLogService auditLogService;

    public MovieService(MovieRepository movieRepository,
                        GenreRepository genreRepository,
                        MovieGenreRepository movieGenreRepository,
                        LanguageRepository languageRepository,
                        MovieLanguageRepository movieLanguageRepository,
                        CertificationRepository certificationRepository,
                        AuditLogService auditLogService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.languageRepository = languageRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.certificationRepository = certificationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String status, Integer cityId, Pageable pageable) {
        List<Movie> movies;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        if (cityId != null) {
            movies = movieRepository.findMoviesByCityAndStatus(cityId, cleanStatus);
        } else if (cleanStatus != null) {
            movies = movieRepository.findByMovieStatus(cleanStatus);
        } else {
            movies = movieRepository.findAll();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), movies.size());
        List<MovieResponse> pagedList = (start <= end && start < movies.size()) ?
                movies.subList(start, end).stream().map(this::mapToResponse).collect(Collectors.toList()) :
                List.of();

        return new PageImpl<>(pagedList, pageable, movies.size());
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String status, Pageable pageable) {
        return getMovies(status, null, pageable);
    }

    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> getGenresForMovie(Long movieId) {
        movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));

        List<MovieGenre> mgs = movieGenreRepository.findByIdMovieId(movieId);
        List<GenreResponse> resps = new ArrayList<>();
        for (MovieGenre mg : mgs) {
            genreRepository.findById(mg.getId().getGenreId())
                    .ifPresent(g -> resps.add(new GenreResponse(g.getGenreId(), g.getGenreName(), g.getDescription())));
        }
        return resps;
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> getLanguagesForMovie(Long movieId) {
        movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));

        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(movieId);
        List<LanguageResponse> resps = new ArrayList<>();
        for (MovieLanguage ml : mls) {
            languageRepository.findById(ml.getLanguageId())
                    .ifPresent(l -> {
                        LanguageResponse lr = new LanguageResponse(l.getLanguageId(), l.getLanguageCode(), l.getLanguageName(), ml.getLanguageRole());
                        lr.setMovieLanguageId(ml.getMovieLanguageId());
                        resps.add(lr);
                    });
        }
        return resps;
    }

    @Transactional
    public MovieResponse createMovie(MovieRequest request, Long actorUserId, String ipAddress) {
        certificationRepository.findById(request.getCertificationId())
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found: " + request.getCertificationId()));

        if (request.getRuntimeMinutes() <= 0) {
            throw new BadRequestException("Runtime minutes must be greater than 0");
        }

        Movie movie = new Movie();
        movie.setCertificationId(request.getCertificationId());
        movie.setTitle(request.getTitle().trim());
        movie.setSynopsis(request.getSynopsis());
        movie.setRuntimeMinutes(request.getRuntimeMinutes());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setMovieStatus(request.getMovieStatus() != null ? request.getMovieStatus().toUpperCase() : "UPCOMING");
        movie.setCreatedAt(Instant.now());
        movie.setUpdatedAt(Instant.now());
        final Movie savedMovie = movieRepository.save(movie);

        if (request.getGenreIds() != null) {
            for (Integer gid : request.getGenreIds()) {
                genreRepository.findById(gid).ifPresent(g -> movieGenreRepository.save(new MovieGenre(savedMovie.getMovieId(), gid)));
            }
        }

        if (request.getLanguageIds() != null) {
            for (Integer lid : request.getLanguageIds()) {
                languageRepository.findById(lid).ifPresent(l -> movieLanguageRepository.save(new MovieLanguage(savedMovie.getMovieId(), lid, "ORIGINAL")));
            }
        }

        auditLogService.logAction(actorUserId, "MOVIE_CREATE", "MOVIE", String.valueOf(savedMovie.getMovieId()), "Created movie " + savedMovie.getTitle(), ipAddress);
        return mapToResponse(savedMovie);
    }

    @Transactional
    public MovieResponse updateMovie(Long movieId, MovieRequest request, Long actorUserId, String ipAddress) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));

        if (request.getCertificationId() != null) {
            certificationRepository.findById(request.getCertificationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Certification not found: " + request.getCertificationId()));
            movie.setCertificationId(request.getCertificationId());
        }
        if (request.getTitle() != null) movie.setTitle(request.getTitle().trim());
        if (request.getSynopsis() != null) movie.setSynopsis(request.getSynopsis());
        if (request.getRuntimeMinutes() != null) {
            if (request.getRuntimeMinutes() <= 0) throw new BadRequestException("Runtime must be > 0");
            movie.setRuntimeMinutes(request.getRuntimeMinutes());
        }
        if (request.getReleaseDate() != null) movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());
        if (request.getTrailerUrl() != null) movie.setTrailerUrl(request.getTrailerUrl());
        if (request.getMovieStatus() != null) movie.setMovieStatus(request.getMovieStatus().toUpperCase());
        movie.setUpdatedAt(Instant.now());
        movie = movieRepository.save(movie);

        auditLogService.logAction(actorUserId, "MOVIE_UPDATE", "MOVIE", String.valueOf(movieId), "Updated movie " + movie.getTitle(), ipAddress);
        return mapToResponse(movie);
    }

    private MovieResponse mapToResponse(Movie m) {
        MovieResponse resp = new MovieResponse();
        resp.setMovieId(m.getMovieId());
        resp.setCertificationId(m.getCertificationId());
        certificationRepository.findById(m.getCertificationId()).ifPresent(c -> resp.setCertificationCode(c.getCertificationCode()));
        resp.setTitle(m.getTitle());
        resp.setSynopsis(m.getSynopsis());
        resp.setRuntimeMinutes(m.getRuntimeMinutes());
        resp.setReleaseDate(m.getReleaseDate());
        resp.setPosterUrl(m.getPosterUrl());
        resp.setTrailerUrl(m.getTrailerUrl());
        resp.setMovieStatus(m.getMovieStatus());

        List<String> genres = new ArrayList<>();
        for (MovieGenre mg : movieGenreRepository.findByIdMovieId(m.getMovieId())) {
            genreRepository.findById(mg.getId().getGenreId()).ifPresent(g -> genres.add(g.getGenreName()));
        }
        resp.setGenres(genres);

        List<String> languages = new ArrayList<>();
        for (MovieLanguage ml : movieLanguageRepository.findByMovieId(m.getMovieId())) {
            languageRepository.findById(ml.getLanguageId()).ifPresent(l -> languages.add(l.getLanguageName()));
        }
        resp.setLanguages(languages);
        return resp;
    }
}
