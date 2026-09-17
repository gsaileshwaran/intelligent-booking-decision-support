package com.pvk.cinemas.scheduling.repository;

import com.pvk.cinemas.scheduling.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("SELECT COUNT(s) FROM Show s " +
           "WHERE s.screenCapabilityId IN (SELECT sc.screenCapabilityId FROM ScreenCapability sc WHERE sc.screenId = :screenId) " +
           "  AND s.status IN ('SCHEDULED', 'OPEN') " +
           "  AND (s.startAt < :endAt AND s.endAt > :startAt)")
    long countOverlappingShows(
            @Param("screenId") Long screenId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
    default long countOverlappingShows(Integer screenId, Instant startAt, Instant endAt) {
        return screenId != null ? countOverlappingShows(screenId.longValue(), startAt, endAt) : 0;
    }

    @Query("SELECT COUNT(s) FROM Show s " +
           "WHERE s.screenCapabilityId IN (SELECT sc.screenCapabilityId FROM ScreenCapability sc WHERE sc.screenId = :screenId) " +
           "  AND s.showId != :showId " +
           "  AND s.status IN ('SCHEDULED', 'OPEN') " +
           "  AND (s.startAt < :endAt AND s.endAt > :startAt)")
    long countOverlappingShowsExcluding(
            @Param("screenId") Long screenId,
            @Param("showId") Long showId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
    default long countOverlappingShowsExcluding(Integer screenId, Long showId, Instant startAt, Instant endAt) {
        return screenId != null ? countOverlappingShowsExcluding(screenId.longValue(), showId, startAt, endAt) : 0;
    }

    List<Show> findByMovieLanguageId(Long movieLanguageId);
    List<Show> findByScreenCapabilityId(Long screenCapabilityId);
    default List<Show> findByScreenCapabilityId(Integer screenCapabilityId) {
        return screenCapabilityId != null ? findByScreenCapabilityId(screenCapabilityId.longValue()) : java.util.Collections.emptyList();
    }

    List<Show> findByScreenCapabilityIdIn(List<Long> screenCapabilityIds);
    List<Show> findByScreenCapabilityIdInAndStatusIn(List<Long> screenCapabilityIds, List<String> statuses);
    List<Show> findByMovieLanguageIdIn(List<Long> movieLanguageIds);
    List<Show> findByMovieLanguageIdInAndStatusIn(List<Long> movieLanguageIds, List<String> statuses);
    List<Show> findByStatus(String status);
}
