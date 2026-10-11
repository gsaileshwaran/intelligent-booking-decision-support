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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final MovieLifecycleService movieLifecycleService;

    public MovieService(MovieRepository movieRepository,
                        GenreRepository genreRepository,
                        MovieGenreRepository movieGenreRepository,
                        LanguageRepository languageRepository,
                        MovieLanguageRepository movieLanguageRepository,
                        CertificationRepository certificationRepository,
                        AuditLogService auditLogService,
                        MovieLifecycleService movieLifecycleService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.languageRepository = languageRepository;
        this.movieLanguageRepository = movieLanguageRepository;
        this.certificationRepository = certificationRepository;
        this.auditLogService = auditLogService;
        this.movieLifecycleService = movieLifecycleService;
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String q, String status, Integer cityId, String language, Long languageId, Long genreId, Long certificationId, String demandTier, String sort, Pageable pageable) {
        List<Movie> movies;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        if (cityId != null) {
            movies = movieRepository.findMoviesByCityAndStatus(cityId, cleanStatus);
        } else if (cleanStatus != null && ("UPCOMING".equals(cleanStatus) || "AIRING".equals(cleanStatus) || "ENDED".equals(cleanStatus))) {
            movies = movieRepository.findByMovieStatus(cleanStatus);
        } else {
            movies = movieRepository.findAllByOrderByMovieIdAsc();
        }

        // Apply text query filter q if provided
        if (q != null && !q.isBlank()) {
            String qClean = q.trim().toLowerCase();
            movies = movies.stream()
                    .filter(m -> (m.getTitle() != null && m.getTitle().toLowerCase().contains(qClean))
                            || (m.getSynopsis() != null && m.getSynopsis().toLowerCase().contains(qClean)))
                    .collect(Collectors.toList());
        }

        // Apply lifecycle status filter if not already filtered
        if (cleanStatus != null) {
            movies = movies.stream()
                    .filter(m -> {
                        String effective = movieLifecycleService.determineEffectiveStatus(m);
                        String stage = movieLifecycleService.calculateLifecycleStage(m);
                        String dbStatus = m.getMovieStatus() != null ? m.getMovieStatus().toUpperCase() : "";
                        return cleanStatus.equalsIgnoreCase(effective)
                                || cleanStatus.equalsIgnoreCase(stage)
                                || cleanStatus.equalsIgnoreCase(dbStatus);
                    })
                    .collect(Collectors.toList());
        }

        // Apply demand tier filter if provided
        if (demandTier != null && !demandTier.isBlank()) {
            String cleanTier = demandTier.trim().toUpperCase();
            movies = movies.stream()
                    .filter(m -> cleanTier.equalsIgnoreCase(m.getDemandTier()))
                    .collect(Collectors.toList());
        }

        // Apply certificationId filter if provided
        if (certificationId != null) {
            movies = movies.stream()
                    .filter(m -> m.getCertificationId() != null && m.getCertificationId().equals(certificationId))
                    .collect(Collectors.toList());
        }

        // Apply genreId filter if provided
        if (genreId != null) {
            Set<Long> movieIdsWithGenre = movieGenreRepository.findByIdGenreId(genreId).stream()
                    .map(mg -> mg.getId().getMovieId())
                    .collect(Collectors.toSet());
            movies = movies.stream()
                    .filter(m -> movieIdsWithGenre.contains(m.getMovieId()))
                    .collect(Collectors.toList());
        }

        // Apply languageId filter if provided
        if (languageId != null) {
            Set<Long> movieIdsWithLangId = movieLanguageRepository.findByLanguageId(languageId).stream()
                    .map(MovieLanguage::getMovieId)
                    .collect(Collectors.toSet());
            movies = movies.stream()
                    .filter(m -> movieIdsWithLangId.contains(m.getMovieId()))
                    .collect(Collectors.toList());
        }

        // Apply language filter if provided (matches language_code like 'ta' or name like 'Tamil')
        if (language != null && !language.isBlank()) {
            String cleanLang = language.trim();
            List<Language> matchedLangs = languageRepository.findAll().stream()
                    .filter(l -> cleanLang.equalsIgnoreCase(l.getLanguageCode()) || cleanLang.equalsIgnoreCase(l.getName()))
                    .collect(Collectors.toList());
            Set<Long> matchedLangIds = matchedLangs.stream().map(Language::getLanguageId).collect(Collectors.toSet());
            movies = movies.stream()
                    .filter(m -> {
                        List<MovieLanguage> mls = movieLanguageRepository.findByMovieId(m.getMovieId());
                        return mls.stream().anyMatch(ml -> matchedLangIds.contains(ml.getLanguageId()));
                    })
                    .collect(Collectors.toList());
        }

        // Prevent duplicate movies
        movies = movies.stream().distinct().collect(Collectors.toList());

        // Apply sorting if specified; default to deterministic movieId ASC
        if (sort != null && !sort.isBlank()) {
            String s = sort.trim().toLowerCase();
            if (s.startsWith("title")) {
                java.util.Comparator<Movie> comp = java.util.Comparator.comparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Movie::getMovieId);
                if (s.endsWith("desc")) {
                    comp = java.util.Comparator.comparing(Movie::getTitle, String.CASE_INSENSITIVE_ORDER).reversed()
                            .thenComparing(Movie::getMovieId);
                }
                movies.sort(comp);
            } else if (s.startsWith("releasedate") || s.startsWith("release_date")) {
                java.util.Comparator<Movie> comp = java.util.Comparator.comparing(Movie::getReleaseDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(Movie::getMovieId);
                if (s.endsWith("desc")) {
                    comp = java.util.Comparator.comparing(Movie::getReleaseDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed()
                            .thenComparing(Movie::getMovieId);
                }
                movies.sort(comp);
            } else if (s.startsWith("popularityscore") || s.startsWith("popularity_score")) {
                java.util.Comparator<Movie> comp = java.util.Comparator.comparing(Movie::getPopularityScore, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(Movie::getMovieId);
                if (s.endsWith("desc")) {
                    comp = java.util.Comparator.comparing(Movie::getPopularityScore, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed()
                            .thenComparing(Movie::getMovieId);
                }
                movies.sort(comp);
            } else {
                // Unsupported sort parameter: deterministic fallback to default ordering (movieId ASC)
                movies.sort(java.util.Comparator.comparing(Movie::getMovieId));
            }
        } else {
            // Default ordering: explicit and deterministic by movieId ASC
            movies.sort(java.util.Comparator.comparing(Movie::getMovieId));
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), movies.size());
        List<MovieResponse> pagedList = (start <= end && start < movies.size()) ?
                movies.subList(start, end).stream().map(this::mapToResponse).collect(Collectors.toList()) :
                List.of();

        return new PageImpl<>(pagedList, pageable, movies.size());
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String status, Integer cityId, String language, Long languageId, Long genreId, Long certificationId, String demandTier, Pageable pageable) {
        return getMovies(null, status, cityId, language, languageId, genreId, certificationId, demandTier, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String status, Integer cityId, String language, String demandTier, Pageable pageable) {
        return getMovies(status, cityId, language, null, null, null, demandTier, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String status, Integer cityId, Pageable pageable) {
        return getMovies(status, cityId, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> getMovies(String status, Pageable pageable) {
        return getMovies(status, null, null, null, null, null, null, pageable);
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

        movieLifecycleService.validateMovieMetadata(
                request.getDemandTier(),
                request.getPopularityScore(),
                request.getExhibitionStartDate(),
                request.getExhibitionEndDate()
        );

        // Validate genreIds upfront before persisting anything
        List<Genre> validatedGenres = new ArrayList<>();
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            for (Integer gid : request.getGenreIds()) {
                if (gid == null) {
                    throw new BadRequestException("Genre ID cannot be null");
                }
                Genre genre = genreRepository.findById(gid)
                        .orElseThrow(() -> new ResourceNotFoundException("Genre not found: " + gid));
                validatedGenres.add(genre);
            }
        }

        // Validate languageIds upfront before persisting anything
        List<Language> validatedLanguages = new ArrayList<>();
        if (request.getLanguageIds() != null && !request.getLanguageIds().isEmpty()) {
            for (Integer lid : request.getLanguageIds()) {
                if (lid == null) {
                    throw new BadRequestException("Language ID cannot be null");
                }
                Language language = languageRepository.findById(lid)
                        .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + lid));
                validatedLanguages.add(language);
            }
        }

        Movie movie = new Movie();
        movie.setCertificationId(request.getCertificationId());
        movie.setTitle(request.getTitle().trim());
        movie.setSynopsis(request.getSynopsis());
        movie.setRuntimeMinutes(request.getRuntimeMinutes());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setDemandTier(request.getDemandTier() != null ? request.getDemandTier().trim().toUpperCase() : null);
        movie.setPopularityScore(request.getPopularityScore());
        movie.setExhibitionStartDate(request.getExhibitionStartDate());
        movie.setExhibitionEndDate(request.getExhibitionEndDate());

        if (request.getMovieStatus() != null && !request.getMovieStatus().isBlank()) {
            movie.setMovieStatus(request.getMovieStatus().toUpperCase());
        } else if (movie.getExhibitionStartDate() != null && movie.getExhibitionEndDate() != null) {
            movie.setMovieStatus(movieLifecycleService.determineEffectiveStatus(movie));
        } else {
            movie.setMovieStatus("UPCOMING");
        }

        movie.setCreatedAt(Instant.now());
        movie.setUpdatedAt(Instant.now());
        final Movie savedMovie = movieRepository.save(movie);

        // Save validated genres (deduplicating to prevent unique/composite key collisions)
        Set<Long> savedGenreIds = new HashSet<>();
        for (Genre g : validatedGenres) {
            if (savedGenreIds.add(g.getGenreId())) {
                movieGenreRepository.save(new MovieGenre(savedMovie.getMovieId(), g.getGenreId()));
            }
        }

        // Save validated languages (deduplicating to prevent unique constraint collisions)
        Set<Long> savedLangIds = new HashSet<>();
        for (Language l : validatedLanguages) {
            if (savedLangIds.add(l.getLanguageId())) {
                movieLanguageRepository.save(new MovieLanguage(savedMovie.getMovieId(), l.getLanguageId(), "ORIGINAL"));
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

        String targetTier = request.getDemandTier() != null ? request.getDemandTier().trim().toUpperCase() : movie.getDemandTier();
        java.math.BigDecimal targetScore = request.getPopularityScore() != null ? request.getPopularityScore() : movie.getPopularityScore();
        LocalDate targetStart = request.getExhibitionStartDate() != null ? request.getExhibitionStartDate() : movie.getExhibitionStartDate();
        LocalDate targetEnd = request.getExhibitionEndDate() != null ? request.getExhibitionEndDate() : movie.getExhibitionEndDate();

        movieLifecycleService.validateMovieMetadata(targetTier, targetScore, targetStart, targetEnd);

        if (request.getDemandTier() != null) movie.setDemandTier(targetTier);
        if (request.getPopularityScore() != null) movie.setPopularityScore(targetScore);
        if (request.getExhibitionStartDate() != null) movie.setExhibitionStartDate(targetStart);
        if (request.getExhibitionEndDate() != null) movie.setExhibitionEndDate(targetEnd);

        if (request.getMovieStatus() != null) {
            movie.setMovieStatus(request.getMovieStatus().toUpperCase());
        } else if (movie.getExhibitionStartDate() != null && movie.getExhibitionEndDate() != null) {
            movie.setMovieStatus(movieLifecycleService.determineEffectiveStatus(movie));
        }

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

        String effectiveStatus = movieLifecycleService.determineEffectiveStatus(m);
        resp.setMovieStatus(effectiveStatus);
        resp.setLifecycleStage(movieLifecycleService.calculateLifecycleStage(m));

        resp.setDemandTier(m.getDemandTier());
        resp.setPopularityScore(m.getPopularityScore());
        resp.setExhibitionStartDate(m.getExhibitionStartDate());
        resp.setExhibitionEndDate(m.getExhibitionEndDate());

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
