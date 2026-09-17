package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findByLanguageCode(String languageCode);

    default Optional<Language> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
