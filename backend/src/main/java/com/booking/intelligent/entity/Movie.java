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

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MovieStatus status;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = MovieStatus.ACTIVE;
        }
    }
}
