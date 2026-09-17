package com.pvk.cinemas.organization.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.organization.dto.AssignManagerRequest;
import com.pvk.cinemas.organization.dto.TheatreRequest;
import com.pvk.cinemas.organization.dto.TheatreResponse;
import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final EmployeeTheatreRepository employeeTheatreRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final com.pvk.cinemas.infrastructure.repository.ScreenRepository screenRepository;
    private final AuditLogService auditLogService;

    public TheatreService(TheatreRepository theatreRepository,
                          CityRepository cityRepository,
                          EmployeeTheatreRepository employeeTheatreRepository,
                          EmployeeProfileRepository employeeProfileRepository,
                          com.pvk.cinemas.infrastructure.repository.ScreenRepository screenRepository,
                          AuditLogService auditLogService) {
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.employeeTheatreRepository = employeeTheatreRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.screenRepository = screenRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresByCity(Integer cityId) {
        cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));
        return theatreRepository.findByCityIdAndIsActiveTrue(cityId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getAllTheatres() {
        return theatreRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Integer theatreId) {
        return theatreRepository.findById(theatreId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));
    }

    @Transactional
    public TheatreResponse createTheatre(TheatreRequest request, Long actorUserId, String ipAddress) {
        cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));

        theatreRepository.findByTheatreCode(request.getTheatreCode().trim())
                .ifPresent(t -> { throw new ConflictException("Theatre code already exists: " + request.getTheatreCode()); });

        Theatre theatre = new Theatre();
        theatre.setCityId(request.getCityId());
        theatre.setTheatreCode(request.getTheatreCode().trim());
        theatre.setTheatreName(request.getTheatreName().trim());
        theatre.setAddressLine1(request.getAddressLine1().trim());
        theatre.setAddressLine2(request.getAddressLine2());
        theatre.setPostalCode(request.getPostalCode());
        theatre.setLatitude(request.getLatitude());
        theatre.setLongitude(request.getLongitude());
        theatre.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        theatre.setCreatedAt(Instant.now());
        theatre.setUpdatedAt(Instant.now());

        theatre = theatreRepository.save(theatre);
        auditLogService.logAction(actorUserId, "THEATRE_CREATE", "THEATRE", String.valueOf(theatre.getTheatreId()), "Created theatre " + theatre.getTheatreName(), ipAddress);
        return mapToResponse(theatre);
    }

    @Transactional
    public TheatreResponse updateTheatre(Integer theatreId, TheatreRequest request, Long actorUserId, String ipAddress) {
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));

        if (request.getCityId() != null) {
            cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));
            theatre.setCityId(request.getCityId());
        }
        if (request.getTheatreName() != null) theatre.setTheatreName(request.getTheatreName().trim());
        if (request.getAddressLine1() != null) theatre.setAddressLine1(request.getAddressLine1().trim());
        if (request.getAddressLine2() != null) theatre.setAddressLine2(request.getAddressLine2());
        if (request.getPostalCode() != null) theatre.setPostalCode(request.getPostalCode());
        if (request.getLatitude() != null) theatre.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) theatre.setLongitude(request.getLongitude());
        if (request.getIsActive() != null) theatre.setIsActive(request.getIsActive());
        theatre.setUpdatedAt(Instant.now());

        theatre = theatreRepository.save(theatre);
        auditLogService.logAction(actorUserId, "THEATRE_UPDATE", "THEATRE", String.valueOf(theatreId), "Updated theatre " + theatre.getTheatreName(), ipAddress);
        return mapToResponse(theatre);
    }

    @Transactional
    public void assignManager(Integer theatreId, AssignManagerRequest request, Long actorUserId, String ipAddress) {
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));
        employeeProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + request.getUserId()));

        if (!employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(request.getUserId(), theatreId)) {
            employeeTheatreRepository.save(new EmployeeTheatre(request.getUserId(), theatreId, actorUserId));
            auditLogService.logAction(actorUserId, "MANAGER_ASSIGN", "EMPLOYEE_THEATRE", request.getUserId() + ":" + theatreId, "Assigned manager to theatre", ipAddress);
        }
    }

    @Transactional
    public void revokeManager(Integer theatreId, Long userId, Long actorUserId, String ipAddress) {
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));

        if (employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(userId, theatreId)) {
            employeeTheatreRepository.deleteByIdUserIdAndIdTheatreId(userId, theatreId);
            auditLogService.logAction(actorUserId, "MANAGER_REVOKE", "EMPLOYEE_THEATRE", userId + ":" + theatreId, "Revoked manager from theatre", ipAddress);
        }
    }

    private TheatreResponse mapToResponse(Theatre t) {
        TheatreResponse resp = new TheatreResponse();
        resp.setTheatreId(t.getTheatreId());
        resp.setCityId(t.getCityId());
        cityRepository.findById(t.getCityId()).ifPresent(c -> resp.setCityName(c.getCityName()));
        resp.setTheatreCode(t.getTheatreCode());
        resp.setTheatreName(t.getTheatreName());
        resp.setAddressLine1(t.getAddressLine1());
        resp.setAddressLine2(t.getAddressLine2());
        resp.setPostalCode(t.getPostalCode());
        resp.setLatitude(t.getLatitude());
        resp.setLongitude(t.getLongitude());
        resp.setIsActive(t.getIsActive());
        resp.setTotalScreens((int) screenRepository.countByTheatreId(t.getTheatreId()));
        return resp;
    }
}
