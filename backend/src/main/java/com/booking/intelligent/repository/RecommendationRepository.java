package com.booking.intelligent.repository;

import com.booking.intelligent.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
