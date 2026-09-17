package com.pvk.cinemas.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.catalogue.model.Movie;
import com.pvk.cinemas.catalogue.repository.MovieRepository;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.search.dto.SearchCardResponse;
import com.pvk.cinemas.search.dto.SearchIndexStatusResponse;
import com.pvk.cinemas.search.model.SearchIndexDocument;
import com.pvk.cinemas.search.model.SearchQuery;
import com.pvk.cinemas.search.model.SearchResult;
import com.pvk.cinemas.search.repository.SearchIndexDocumentRepository;
import com.pvk.cinemas.search.repository.SearchQueryRepository;
import com.pvk.cinemas.search.repository.SearchResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Search Orchestration Service.
 *
 * Phase 4 Enhancement:
 * Attempts to delegate search to the Python/FastAPI search service (hybrid BM25 + semantic).
 * Falls back to relational SQL matching when the Python service is unavailable.
 *
 * Architecture boundary (ARCH §11, AP-03, BR-009, PLAN §12):
 * - Python service provides ranked candidates (entity_type, entity_id, rank, method)
 * - This service resolves authoritative entity details from MySQL using those candidate IDs
 * - SEARCH_QUERY + SEARCH_RESULT telemetry is always written here (not in Python service)
 *   because Spring Boot is the authenticated boundary (FR-093, FR-094, FR-095)
 *
 * Fallback behavior (PLAN §12):
 * - If Python service unreachable → SQL LIKE matching
 * - retrieval_method = "LEXICAL" for SQL fallback
 * - Response returns normally; caller is unaware of fallback
 */
@Service
public class SearchOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(SearchOrchestrationService.class);

    private final SearchQueryRepository searchQueryRepository;
    private final SearchResultRepository searchResultRepository;
    private final SearchIndexDocumentRepository searchIndexDocumentRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ObjectMapper objectMapper;
    private final SearchServiceClient searchServiceClient;

    @Value("${app.search.fallback-enabled:true}")
    private boolean fallbackEnabled;

    public SearchOrchestrationService(SearchQueryRepository searchQueryRepository,
                                      SearchResultRepository searchResultRepository,
                                      SearchIndexDocumentRepository searchIndexDocumentRepository,
                                      MovieRepository movieRepository,
                                      TheatreRepository theatreRepository,
                                      ObjectMapper objectMapper,
                                      SearchServiceClient searchServiceClient) {
        this.searchQueryRepository = searchQueryRepository;
        this.searchResultRepository = searchResultRepository;
        this.searchIndexDocumentRepository = searchIndexDocumentRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.objectMapper = objectMapper;
        this.searchServiceClient = searchServiceClient;
    }

    /**
     * Execute search query via hybrid search service with SQL fallback.
     * Authority: FR-090–FR-095, ARCH §11, PLAN §12.
     */
    @Transactional
    public List<SearchCardResponse> search(String queryText, Integer cityId, Long userId) {
        String cleanQuery = (queryText != null ? queryText.trim() : "");
        List<SearchCardResponse> results;

        // Attempt delegation to Python search service
        if (!cleanQuery.isEmpty()) {
            try {
                results = searchViaService(cleanQuery, cityId);
                // If hybrid + prefix expansion both return 0, fall back to SQL LIKE
                if (results.isEmpty() && fallbackEnabled) {
                    log.info("Python search service returned 0 results for '{}', falling back to SQL LIKE", cleanQuery);
                    results = searchViaSqlFallback(cleanQuery, cityId);
                } else {
                    log.debug("Search delegated to Python service: query='{}' cityId={} results={}", cleanQuery, cityId, results.size());
                }
            } catch (RestClientException e) {
                if (fallbackEnabled) {
                    log.warn("Python search service unavailable ({}), falling back to SQL", e.getMessage());
                    results = searchViaSqlFallback(cleanQuery, cityId);
                } else {
                    log.error("Python search service unavailable and fallback disabled: {}", e.getMessage());
                    results = new ArrayList<>();
                }
            } catch (Exception e) {
                log.error("Unexpected error during search delegation, falling back to SQL: {}", e.getMessage());
                results = fallbackEnabled ? searchViaSqlFallback(cleanQuery, cityId) : new ArrayList<>();
            }
        } else {
            // Empty query — return all movies via SQL (browsing mode)
            results = searchViaSqlFallback(cleanQuery, cityId);
        }

        // Re-assign contiguous rank positions after city filtering
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRankPosition(i + 1);
        }

        // Persist search telemetry (always written by Spring Boot — authenticated boundary)
        // Authority: FR-093, FR-094, FR-095
        SearchQuery sq = new SearchQuery(userId, cleanQuery, results.size());
        sq = searchQueryRepository.save(sq);

        // Persist per-result telemetry (FR-092, AI-006, AI-007)
        for (SearchCardResponse card : results) {
            SearchResult sr = new SearchResult(
                    sq.getSearchQueryId(),
                    card.getEntityType(),
                    card.getEntityId(),
                    card.getRankPosition(),
                    card.getRetrievalMethod()
            );
            searchResultRepository.save(sr);
        }

        return results;
    }

    /**
     * Delegate search to Python service and resolve authoritative entity details from MySQL.
     * Authority: ARCH §11 — search identifies candidates, backend resolves authoritative details.
     */
    private List<SearchCardResponse> searchViaService(String queryText, Integer cityId) {
        List<SearchServiceClient.SearchCandidateDto> candidates = searchServiceClient.search(queryText, 20);
        List<SearchCardResponse> results = new ArrayList<>();

        for (SearchServiceClient.SearchCandidateDto candidate : candidates) {
            SearchCardResponse card = resolveCandidate(candidate, cityId);
            if (card != null) {
                results.add(card);
            }
        }
        return results;
    }

    /**
     * Resolve authoritative entity details from MySQL for a search candidate.
     * Authority: ARCH §11 — authoritative details resolved by backend after search.
     */
    private SearchCardResponse resolveCandidate(SearchServiceClient.SearchCandidateDto candidate, Integer cityId) {
        switch (candidate.getEntityType()) {
            case "MOVIE" -> {
                Optional<Movie> movieOpt = movieRepository.findById(candidate.getEntityId());
                if (movieOpt.isPresent()) {
                    Movie m = movieOpt.get();
                    return new SearchCardResponse(
                            "MOVIE",
                            m.getMovieId(),
                            m.getTitle(),
                            m.getRuntimeMinutes() + " mins",
                            m.getMovieStatus(),
                            candidate.getRankPosition(),
                            candidate.getRetrievalMethod(),
                            m.getPosterUrl()
                    );
                }
            }
            case "THEATRE" -> {
                Optional<Theatre> theatreOpt = theatreRepository.findById((long) candidate.getEntityId().intValue());
                if (theatreOpt.isPresent()) {
                    Theatre t = theatreOpt.get();
                    if (cityId != null && (t.getCityId() == null || !t.getCityId().equals(cityId.longValue()))) {
                        return null; // Exclude theatre outside selected city
                    }
                    return new SearchCardResponse(
                            "THEATRE",
                            (long) t.getTheatreId(),
                            t.getTheatreName(),
                            t.getAddressLine1(),
                            t.getStatus(),
                            candidate.getRankPosition(),
                            candidate.getRetrievalMethod(),
                            null
                    );
                }
            }
        }
        log.warn("Could not resolve candidate entity_type={} entity_id={}", candidate.getEntityType(), candidate.getEntityId());
        return null;
    }

    public List<SearchCardResponse> searchViaSqlFallback(String query) {
        return searchViaSqlFallback(query, null);
    }

    /**
     * SQL-based fallback: keyword LIKE matching on movie titles and theatre names.
     * Used when the Python search service is unavailable.
     * retrieval_method = "LEXICAL" (this is keyword/string matching).
     * Authority: PLAN §12 (Fallback Behavior), SPEC-BASE §I.4.
     */
    public List<SearchCardResponse> searchViaSqlFallback(String query, Integer cityId) {
        List<SearchCardResponse> results = new ArrayList<>();
        String normalizedQuery = query != null ? query.trim().toLowerCase() : "";
        int rank = 1;

        List<Movie> movies = movieRepository.findAll();
        for (Movie m : movies) {
            if (normalizedQuery.isEmpty() || (m.getTitle() != null && m.getTitle().toLowerCase().contains(normalizedQuery))) {
                results.add(new SearchCardResponse(
                        "MOVIE",
                        m.getMovieId(),
                        m.getTitle(),
                        m.getRuntimeMinutes() + " mins",
                        m.getMovieStatus(),
                        rank++,
                        "LEXICAL",
                        m.getPosterUrl()
                ));
            }
        }

        List<Theatre> theatres = theatreRepository.findAll();
        for (Theatre t : theatres) {
            if (cityId != null && (t.getCityId() == null || !t.getCityId().equals(cityId.longValue()))) {
                continue; // Exclude out-of-city theatre
            }
            if (normalizedQuery.isEmpty() || (t.getTheatreName() != null && t.getTheatreName().toLowerCase().contains(normalizedQuery))) {
                results.add(new SearchCardResponse(
                        "THEATRE",
                        (long) t.getTheatreId(),
                        t.getTheatreName(),
                        t.getAddressLine1(),
                        t.getStatus(),
                        rank++,
                        "LEXICAL"
                ));
            }
        }

        return results;
    }

    /**
     * Reindex all documents in MySQL and notify the Python search service.
     * Authority: FR-096, FR-097, FR-098, AI-009.
     */
    @Transactional
    public void reindexAll() {
        // Reindex SEARCH_INDEX_DOCUMENT in MySQL (operational record)
        reindexMoviesInMysql();
        reindexTheatresInMysql();

        // Notify Python service to rebuild its in-process BM25 + vector indexes
        try {
            searchServiceClient.triggerReindex();
            log.info("Python search service reindex triggered successfully");
        } catch (RestClientException e) {
            log.warn("Python search service unavailable during reindex trigger: {}", e.getMessage());
            // Not a fatal error — SEARCH_INDEX_DOCUMENT is still updated in MySQL
        }
    }

    private void reindexMoviesInMysql() {
        List<Movie> movies = movieRepository.findAll();
        for (Movie m : movies) {
            Map<String, Object> payload = Map.of(
                    "title", m.getTitle(),
                    "synopsis", m.getSynopsis() != null ? m.getSynopsis() : "",
                    "runtimeMinutes", m.getRuntimeMinutes(),
                    "status", m.getMovieStatus()
            );
            try {
                String json = objectMapper.writeValueAsString(payload);
                SearchIndexDocument doc = searchIndexDocumentRepository.findByEntityTypeAndEntityId("MOVIE", m.getMovieId())
                        .orElseGet(() -> new SearchIndexDocument("MOVIE", m.getMovieId(), json));
                doc.setDocumentPayload(json);
                doc.setDocumentStatus("ACTIVE");
                doc.setUpdatedAt(Instant.now());
                searchIndexDocumentRepository.save(doc);
            } catch (JsonProcessingException ignored) {}
        }
    }

    private void reindexTheatresInMysql() {
        List<Theatre> theatres = theatreRepository.findAll();
        for (Theatre t : theatres) {
            Map<String, Object> payload = Map.of(
                    "theatreName", t.getTheatreName(),
                    "theatreCode", t.getTheatreCode(),
                    "address", t.getAddressLine1()
            );
            try {
                String json = objectMapper.writeValueAsString(payload);
                SearchIndexDocument doc = searchIndexDocumentRepository.findByEntityTypeAndEntityId("THEATRE", Long.valueOf(t.getTheatreId()))
                        .orElseGet(() -> new SearchIndexDocument("THEATRE", Long.valueOf(t.getTheatreId()), json));
                doc.setDocumentPayload(json);
                doc.setDocumentStatus("ACTIVE");
                doc.setUpdatedAt(Instant.now());
                searchIndexDocumentRepository.save(doc);
            } catch (JsonProcessingException ignored) {}
        }
    }

    @Transactional(readOnly = true)
    public SearchIndexStatusResponse getIndexStatus() {
        List<SearchIndexDocument> docs = searchIndexDocumentRepository.findAll();
        long active = docs.stream().filter(d -> "ACTIVE".equalsIgnoreCase(d.getDocumentStatus())).count();
        long stale = docs.stream().filter(d -> "STALE".equalsIgnoreCase(d.getDocumentStatus())).count();
        Instant lastIndexed = docs.stream().map(SearchIndexDocument::getUpdatedAt).max(Instant::compareTo).orElse(Instant.now());

        return new SearchIndexStatusResponse(docs.size(), active, stale, "v4.0", lastIndexed);
    }
}
