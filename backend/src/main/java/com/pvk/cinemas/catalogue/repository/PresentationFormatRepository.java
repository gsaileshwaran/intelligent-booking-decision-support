package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.PresentationFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresentationFormatRepository extends JpaRepository<PresentationFormat, Long> {
    Optional<PresentationFormat> findByFormatCode(String formatCode);

    default Optional<PresentationFormat> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
