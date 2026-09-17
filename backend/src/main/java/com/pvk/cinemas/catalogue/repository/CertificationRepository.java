package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {
    Optional<Certification> findByCertificationCode(String certificationCode);

    default Optional<Certification> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
