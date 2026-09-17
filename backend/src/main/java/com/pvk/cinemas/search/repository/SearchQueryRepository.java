package com.pvk.cinemas.search.repository;

import com.pvk.cinemas.search.model.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long> {
    Page<SearchQuery> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
