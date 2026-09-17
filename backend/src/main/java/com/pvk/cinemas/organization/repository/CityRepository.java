package com.pvk.cinemas.organization.repository;

import com.pvk.cinemas.organization.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByCityNameAndStateName(String cityName, String stateName);

    default Optional<City> findById(Integer id) {
        return id != null ? findById(id.longValue()) : Optional.empty();
    }
}
