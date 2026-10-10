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
    private final com.pvk.cinemas.infrastructure.repository.SeatRepository seatRepository;
    private final com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public TheatreService(TheatreRepository theatreRepository,
                          CityRepository cityRepository,
                          EmployeeTheatreRepository employeeTheatreRepository,
                          EmployeeProfileRepository employeeProfileRepository,
                          com.pvk.cinemas.infrastructure.repository.ScreenRepository screenRepository,
                          @org.springframework.beans.factory.annotation.Autowired(required = false)
                          com.pvk.cinemas.infrastructure.repository.SeatRepository seatRepository,
                          @org.springframework.beans.factory.annotation.Autowired(required = false)
                          com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository,
                          AuditLogService auditLogService) {
        this.theatreRepository = theatreRepository;
        this.cityRepository = cityRepository;
        this.employeeTheatreRepository = employeeTheatreRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.auditLogService = auditLogService;
    }

    public TheatreService(TheatreRepository theatreRepository,
                          CityRepository cityRepository,
                          EmployeeTheatreRepository employeeTheatreRepository,
                          EmployeeProfileRepository employeeProfileRepository,
                          com.pvk.cinemas.infrastructure.repository.ScreenRepository screenRepository,
                          AuditLogService auditLogService) {
        this(theatreRepository, cityRepository, employeeTheatreRepository, employeeProfileRepository, screenRepository, null, null, auditLogService);
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresByCity(Long cityId) {
        if (cityId == null) {
            throw new ResourceNotFoundException("City not found: null");
        }
        cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));
        return theatreRepository.findByCityIdAndIsActiveTrue(cityId).stream()
                .sorted(java.util.Comparator.comparing(Theatre::getTheatreName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresByCity(Integer cityId) {
        return getTheatresByCity(cityId != null ? cityId.longValue() : null);
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getAllTheatres() {
        return theatreRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Theatre::getTheatreName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Long theatreId) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
        return theatreRepository.findById(theatreId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Integer theatreId) {
        return getTheatreById(theatreId != null ? theatreId.longValue() : null);
    }

    @Transactional(readOnly = true)
    public List<com.pvk.cinemas.infrastructure.dto.ScreenResponse> getScreensByTheatre(Long theatreId) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));
        return screenRepository.findByTheatreIdAndIsActiveTrue(theatreId).stream()
                .sorted(java.util.Comparator.comparing(com.pvk.cinemas.infrastructure.model.Screen::getScreenCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapToScreenResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<com.pvk.cinemas.infrastructure.dto.ScreenResponse> getScreensByTheatre(Integer theatreId) {
        return getScreensByTheatre(theatreId != null ? theatreId.longValue() : null);
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
    public TheatreResponse updateTheatre(Long theatreId, TheatreRequest request, Long actorUserId, String ipAddress) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));

        if (request.getCityId() != null) {
            cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));
            theatre.setCityId(request.getCityId());
        }
        if (request.getTheatreName() != null && !request.getTheatreName().isBlank()) theatre.setTheatreName(request.getTheatreName().trim());
        if (request.getAddressLine1() != null && !request.getAddressLine1().isBlank()) theatre.setAddressLine1(request.getAddressLine1().trim());
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
    public TheatreResponse updateTheatre(Integer theatreId, TheatreRequest request, Long actorUserId, String ipAddress) {
        return updateTheatre(theatreId != null ? theatreId.longValue() : null, request, actorUserId, ipAddress);
    }

    @Transactional
    public void assignManager(Long theatreId, AssignManagerRequest request, Long actorUserId, String ipAddress) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
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
    public void assignManager(Integer theatreId, AssignManagerRequest request, Long actorUserId, String ipAddress) {
        assignManager(theatreId != null ? theatreId.longValue() : null, request, actorUserId, ipAddress);
    }

    @Transactional
    public void revokeManager(Long theatreId, Long userId, Long actorUserId, String ipAddress) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));

        if (employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(userId, theatreId)) {
            employeeTheatreRepository.deleteByIdUserIdAndIdTheatreId(userId, theatreId);
            auditLogService.logAction(actorUserId, "MANAGER_REVOKE", "EMPLOYEE_THEATRE", userId + ":" + theatreId, "Revoked manager from theatre", ipAddress);
        }
    }

    @Transactional
    public void revokeManager(Integer theatreId, Long userId, Long actorUserId, String ipAddress) {
        revokeManager(theatreId != null ? theatreId.longValue() : null, userId, actorUserId, ipAddress);
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

    private com.pvk.cinemas.infrastructure.dto.ScreenResponse mapToScreenResponse(com.pvk.cinemas.infrastructure.model.Screen s) {
        com.pvk.cinemas.infrastructure.dto.ScreenResponse resp =
                new com.pvk.cinemas.infrastructure.dto.ScreenResponse(s.getScreenId(), s.getTheatreId(), s.getScreenCode(), s.getScreenName(), s.getIsActive());
        if (seatRepository != null && s.getScreenId() != null) {
            resp.setTotalCapacity((int) seatRepository.findByScreenId(s.getScreenId()).size());
        }
        if (screenCapabilityRepository != null && s.getScreenId() != null) {
            var caps = screenCapabilityRepository.findByScreenId(s.getScreenId());
            if (!caps.isEmpty()) {
                resp.setScreenCapabilityId(caps.get(0).getScreenCapabilityId());
            }
        }
        return resp;
    }
}
