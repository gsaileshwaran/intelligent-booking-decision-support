package com.booking.intelligent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recommendation_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_item_id")
    private Long recommendationItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "suitability_score", precision = 5, scale = 4, nullable = false)
    private BigDecimal suitabilityScore;

    @Column(name = "reason_data", length = 2000)
    private String reasonData;
}
