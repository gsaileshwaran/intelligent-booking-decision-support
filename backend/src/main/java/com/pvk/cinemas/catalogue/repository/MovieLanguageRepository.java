package com.pvk.cinemas.catalogue.repository;

import com.pvk.cinemas.catalogue.model.MovieLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieLanguageRepository extends JpaRepository<MovieLanguage, Long> {
    List<MovieLanguage> findByMovieId(Long movieId);
    Optional<MovieLanguage> findByMovieIdAndLanguageIdAndLanguageRole(Long movieId, Integer languageId, String languageRole);
}
