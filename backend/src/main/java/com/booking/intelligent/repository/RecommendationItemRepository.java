package com.booking.intelligent.repository;

import com.booking.intelligent.entity.RecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {
    List<RecommendationItem> findByRecommendationRecommendationIdOrderByRankAsc(Long recommendationId);
}
