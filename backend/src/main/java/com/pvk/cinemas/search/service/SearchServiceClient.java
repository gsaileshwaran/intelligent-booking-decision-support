package com.pvk.cinemas.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvk.cinemas.search.dto.SearchCardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for delegating search queries to the Python/FastAPI search service.
 *
 * Architecture boundary (ARCH §11, AP-03, BR-009):
 * - Spring Boot calls the Python service's internal API
 * - The Python service returns ranked candidates (entity_type, entity_id, rank, method, score)
 * - Spring Boot resolves authoritative entity details from MySQL AFTER receiving candidates
 * - If the Python service is unreachable, this client throws RestClientException
 *   and the caller (SearchOrchestrationService) falls back to relational SQL retrieval
 *
 * Authority: PLAN §6 (Internal service contract), PLAN §12 (Fallback behavior).
 */
@Component
public class SearchServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.search.service-url:http://127.0.0.1:8001}")
    private String searchServiceUrl;

    public SearchServiceClient(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Call POST /internal/v1/search on the Python search service.
     * Returns a list of CandidateResult-style maps with entity_type, entity_id, rank_position,
     * retrieval_method, relevance_score.
     *
     * @throws RestClientException if the service is unreachable or returns an error
     */
    public List<SearchCandidateDto> search(String queryText, int maxResults) {
        String url = searchServiceUrl + "/internal/v1/search";

        Map<String, Object> requestBody = Map.of(
                "query", queryText,
                "max_results", maxResults
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RestClientException("Search service returned non-2xx status: " + response.getStatusCode());
        }

        return parseCandidates(response.getBody());
    }

    /**
     * Call POST /internal/v1/index to trigger a full reindex on the Python service.
     *
     * @throws RestClientException if the service is unreachable
     */
    public void triggerReindex() {
        String url = searchServiceUrl + "/internal/v1/index";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        restTemplate.postForEntity(url, entity, String.class);
        log.info("SearchServiceClient.triggerReindex() → {}", url);
    }

    /**
     * Check if the Python search service is available.
     */
    public boolean isAvailable() {
        try {
            String url = searchServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    private List<SearchCandidateDto> parseCandidates(String json) {
        List<SearchCandidateDto> candidates = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return candidates;
            }
            for (JsonNode node : results) {
                SearchCandidateDto dto = new SearchCandidateDto();
                dto.setEntityType(node.path("entity_type").asText("MOVIE"));
                dto.setEntityId(node.path("entity_id").asLong(0L));
                dto.setRankPosition(node.path("rank_position").asInt(1));
                dto.setRetrievalMethod(node.path("retrieval_method").asText("HYBRID"));
                JsonNode scoreNode = node.get("relevance_score");
                dto.setRelevanceScore(scoreNode != null && !scoreNode.isNull() ? scoreNode.doubleValue() : null);
                dto.setDisplayTitle(node.path("display_title").asText(null));
                candidates.add(dto);
            }
        } catch (Exception e) {
            log.error("Failed to parse search service response: {}", e.getMessage());
        }
        return candidates;
    }

    /**
     * Data transfer object for a candidate result from the Python service.
     */
    public static class SearchCandidateDto {
        private String entityType;
        private Long entityId;
        private int rankPosition;
        private String retrievalMethod;
        private Double relevanceScore;
        private String displayTitle;

        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }

        public Long getEntityId() { return entityId; }
        public void setEntityId(Long entityId) { this.entityId = entityId; }

        public int getRankPosition() { return rankPosition; }
        public void setRankPosition(int rankPosition) { this.rankPosition = rankPosition; }

        public String getRetrievalMethod() { return retrievalMethod; }
        public void setRetrievalMethod(String retrievalMethod) { this.retrievalMethod = retrievalMethod; }

        public Double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }

        public String getDisplayTitle() { return displayTitle; }
        public void setDisplayTitle(String displayTitle) { this.displayTitle = displayTitle; }
    }
}
