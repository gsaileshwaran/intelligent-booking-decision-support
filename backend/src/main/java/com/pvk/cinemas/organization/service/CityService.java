package com.pvk.cinemas.organization.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.organization.dto.CityRequest;
import com.pvk.cinemas.organization.dto.CityResponse;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final CityRepository cityRepository;
    private final AuditLogService auditLogService;

    public CityService(CityRepository cityRepository, AuditLogService auditLogService) {
        this.cityRepository = cityRepository;
        this.auditLogService = auditLogService;
    }

    private static final List<String> CANONICAL_CITIES = List.of(
            "Chennai", "Bengaluru", "Hyderabad", "Mumbai", "Delhi"
    );

    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        return cityRepository.findAll().stream()
                .filter(c -> c.getCityName() != null && !c.getCityName().startsWith("City_Upd_"))
                .sorted((c1, c2) -> {
                    int i1 = CANONICAL_CITIES.indexOf(c1.getCityName());
                    int i2 = CANONICAL_CITIES.indexOf(c2.getCityName());
                    if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
                    if (i1 != -1) return -1;
                    if (i2 != -1) return 1;
                    return c1.getCityName().compareToIgnoreCase(c2.getCityName());
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CityResponse getCityById(Integer cityId) {
        return cityRepository.findById(cityId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));
    }

    @Transactional
    public CityResponse createCity(CityRequest request, Long actorUserId, String ipAddress) {
        cityRepository.findByCityNameAndStateName(request.getCityName().trim(), request.getStateName().trim())
                .ifPresent(c -> { throw new ConflictException("City already exists: " + request.getCityName()); });

        City city = new City(request.getCityName().trim(), request.getStateName().trim(), request.getCountryCode());
        city = cityRepository.save(city);

        auditLogService.logAction(actorUserId, "CITY_CREATE", "CITY", String.valueOf(city.getCityId()), "Created city " + city.getCityName(), ipAddress);
        return mapToResponse(city);
    }

    @Transactional
    public CityResponse updateCity(Integer cityId, CityRequest request, Long actorUserId, String ipAddress) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));

        if (request.getCityName() != null) city.setCityName(request.getCityName().trim());
        if (request.getStateName() != null) city.setStateName(request.getStateName().trim());
        if (request.getCountryCode() != null) city.setCountryCode(request.getCountryCode().trim());
        city = cityRepository.save(city);

        auditLogService.logAction(actorUserId, "CITY_UPDATE", "CITY", String.valueOf(cityId), "Updated city " + city.getCityName(), ipAddress);
        return mapToResponse(city);
    }

    private CityResponse mapToResponse(City c) {
        return new CityResponse(c.getCityId(), c.getCityName(), c.getStateName(), c.getCountryCode());
    }
}
