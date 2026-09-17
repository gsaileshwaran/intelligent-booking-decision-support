package com.pvk.cinemas.scheduling.repository;

import com.pvk.cinemas.scheduling.model.ScreenCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenCapabilityRepository extends JpaRepository<ScreenCapability, Long> {
    List<ScreenCapability> findByScreenId(Long screenId);
    default List<ScreenCapability> findByScreenId(Integer screenId) {
        return screenId != null ? findByScreenId(screenId.longValue()) : java.util.Collections.emptyList();
    }

    List<ScreenCapability> findByScreenIdIn(List<Long> screenIds);

    default Optional<ScreenCapability> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }

    Optional<ScreenCapability> findByScreenIdAndPresentationFormatIdAndAudioFormatId(Long screenId, Long presentationFormatId, Long audioFormatId);
    default Optional<ScreenCapability> findByScreenIdAndPresentationFormatIdAndAudioFormatId(Integer screenId, Integer presentationFormatId, Integer audioFormatId) {
        if (screenId == null || presentationFormatId == null || audioFormatId == null) return Optional.empty();
        return findByScreenIdAndPresentationFormatIdAndAudioFormatId(screenId.longValue(), presentationFormatId.longValue(), audioFormatId.longValue());
    }
}
