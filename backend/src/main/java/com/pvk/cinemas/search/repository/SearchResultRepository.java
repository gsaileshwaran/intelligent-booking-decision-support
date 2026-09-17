package com.pvk.cinemas.search.repository;

import com.pvk.cinemas.search.model.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchResultRepository extends JpaRepository<SearchResult, Long> {
    List<SearchResult> findBySearchQueryIdOrderByRankPositionAsc(Long searchQueryId);
}
