package com.booking.intelligent.entity;

import com.booking.intelligent.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "movie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "original_title", length = 200)
    private String originalTitle;

    @Column(name = "description", length = 1500)
    private String description;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "language", length = 100)
    private String language;

    @Column(name = "languages", length = 200)
    private String languages;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "censor_rating", length = 20)
    private String censorRating;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "release_date_status", length = 30)
    private String releaseDateStatus; // CONFIRMED, ANNOUNCED, EXPECTED, TBA

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "backdrop_url", length = 500)
    private String backdropUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Column(name = "director", length = 200)
    private String director;

    @Column(name = "movie_cast", length = 500)
    private String cast;

    @Column(name = "studio", length = 200)
    private String studio;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "anticipation_score")
    private Integer anticipationScore; // 0 - 100 score representing audience anticipation

    @Column(name = "anticipation_label", length = 30)
    private String anticipationLabel; // VERY HIGH, HIGH, MEDIUM, LOW

    @Column(name = "is_upcoming")
    private Boolean isUpcoming;

    @Column(name = "is_now_showing")
    private Boolean isNowShowing;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MovieStatus status;

    public Boolean getIsNowShowing() {
        if (isNowShowing != null) return isNowShowing;
        if (releaseDate == null) return false;
        return !releaseDate.isAfter(LocalDate.now());
    }

    public Boolean getIsUpcoming() {
        if (isUpcoming != null) return isUpcoming;
        if (releaseDate == null) return true;
        return releaseDate.isAfter(LocalDate.now());
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (status == null) {
            status = MovieStatus.ACTIVE;
        }
        if (releaseDate != null) {
            boolean upcoming = releaseDate.isAfter(LocalDate.now());
            this.isUpcoming = upcoming;
            this.isNowShowing = !upcoming;
        } else {
            this.isUpcoming = true;
            this.isNowShowing = false;
        }
    }
}
