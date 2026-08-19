package com.booking.intelligent.repository;

import com.booking.intelligent.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {
    List<RecommendationFeedback> findByUserUserId(Long userId);
}
