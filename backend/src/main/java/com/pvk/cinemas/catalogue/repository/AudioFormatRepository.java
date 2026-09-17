package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.AudioFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AudioFormatRepository extends JpaRepository<AudioFormat, Long> {
    Optional<AudioFormat> findByFormatCode(String formatCode);

    default Optional<AudioFormat> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
