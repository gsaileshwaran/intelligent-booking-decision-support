package com.pvk.cinemas.infrastructure.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.pvk.cinemas.infrastructure.dto.ScreenRequest;
import com.pvk.cinemas.infrastructure.dto.ScreenResponse;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final com.pvk.cinemas.infrastructure.repository.SeatRepository seatRepository;
    private final com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository;
    private final ScreenCapabilityService screenCapabilityService;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.pvk.cinemas.security.TheatreScopeService theatreScopeService;

    @org.springframework.beans.factory.annotation.Autowired
    public ScreenService(ScreenRepository screenRepository,
                         TheatreRepository theatreRepository,
                         AuditLogService auditLogService,
                         com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository,
                         @org.springframework.beans.factory.annotation.Autowired(required = false)
                         com.pvk.cinemas.infrastructure.repository.SeatRepository seatRepository,
                         @org.springframework.beans.factory.annotation.Autowired(required = false)
                         ScreenCapabilityService screenCapabilityService) {
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.auditLogService = auditLogService;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.seatRepository = seatRepository;
        this.screenCapabilityService = screenCapabilityService;
    }

    public ScreenService(ScreenRepository screenRepository,
                         TheatreRepository theatreRepository,
                         AuditLogService auditLogService,
                         com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository) {
        this(screenRepository, theatreRepository, auditLogService, screenCapabilityRepository, null, null);
    }

    public void setTheatreScopeService(com.pvk.cinemas.security.TheatreScopeService theatreScopeService) {
        this.theatreScopeService = theatreScopeService;
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheatre(Long theatreId) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
        if (theatreScopeService != null) {
            theatreScopeService.checkTheatreScope(theatreId);
        }
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));
        return screenRepository.findByTheatreIdAndIsActiveTrue(theatreId).stream()
                .sorted(java.util.Comparator.comparing(Screen::getScreenCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheatre(Integer theatreId) {
        return getScreensByTheatre(theatreId != null ? theatreId.longValue() : null);
    }

    @Transactional
    public ScreenResponse createScreen(Long theatreId, ScreenRequest request, Long actorUserId, String ipAddress) {
        if (theatreId == null) {
            throw new ResourceNotFoundException("Theatre not found: null");
        }
        if (theatreScopeService != null) {
            theatreScopeService.checkTheatreScope(theatreId);
        }
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));

        screenRepository.findByTheatreIdAndScreenCode(theatreId, request.getScreenCode().trim())
                .ifPresent(s -> { throw new ConflictException("Screen code already exists in theatre: " + request.getScreenCode()); });

        Screen screen = new Screen();
        screen.setTheatreId(theatreId);
        screen.setScreenCode(request.getScreenCode().trim());
        screen.setScreenName(request.getScreenName().trim());
        screen.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        screen = screenRepository.save(screen);

        auditLogService.logAction(actorUserId, "SCREEN_CREATE", "SCREEN", String.valueOf(screen.getScreenId()), "Created screen " + screen.getScreenName(), ipAddress);
        return mapToResponse(screen);
    }

    @Transactional
    public ScreenResponse createScreen(Integer theatreId, ScreenRequest request, Long actorUserId, String ipAddress) {
        return createScreen(theatreId != null ? theatreId.longValue() : null, request, actorUserId, ipAddress);
    }

    @Transactional
    public ScreenResponse updateScreen(Long theatreId, Long screenId, ScreenRequest request, Long actorUserId, String ipAddress) {
        if (screenId == null) {
            throw new ResourceNotFoundException("Screen not found: null");
        }
        if (theatreScopeService != null) {
            if (theatreId != null) {
                theatreScopeService.checkTheatreScope(theatreId);
            } else {
                theatreScopeService.checkScreenScope(screenId);
            }
        }
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));

        if (theatreId != null && !screen.getTheatreId().equals(theatreId)) {
            throw new AccessDeniedException("Screen does not belong to specified theatre");
        }

        if (request.getScreenCode() != null && !request.getScreenCode().isBlank()) {
            String newCode = request.getScreenCode().trim();
            screenRepository.findByTheatreIdAndScreenCode(screen.getTheatreId(), newCode)
                    .ifPresent(existing -> {
                        if (!existing.getScreenId().equals(screenId)) {
                            throw new ConflictException("Screen code already exists in theatre: " + newCode);
                        }
                    });
            screen.setScreenCode(newCode);
        }

        if (request.getScreenName() != null && !request.getScreenName().isBlank()) screen.setScreenName(request.getScreenName().trim());
        if (request.getIsActive() != null) screen.setIsActive(request.getIsActive());
        screen = screenRepository.save(screen);

        auditLogService.logAction(actorUserId, "SCREEN_UPDATE", "SCREEN", String.valueOf(screenId), "Updated screen " + screen.getScreenName(), ipAddress);
        return mapToResponse(screen);
    }

    @Transactional
    public ScreenResponse updateScreen(Integer theatreId, Integer screenId, ScreenRequest request, Long actorUserId, String ipAddress) {
        return updateScreen(theatreId != null ? theatreId.longValue() : null, screenId != null ? screenId.longValue() : null, request, actorUserId, ipAddress);
    }

    @Transactional
    public ScreenResponse updateScreen(Long screenId, ScreenRequest request, Long actorUserId, String ipAddress) {
        return updateScreen((Long) null, screenId, request, actorUserId, ipAddress);
    }

    @Transactional
    public ScreenResponse updateScreen(Integer screenId, ScreenRequest request, Long actorUserId, String ipAddress) {
        return updateScreen((Long) null, screenId != null ? screenId.longValue() : null, request, actorUserId, ipAddress);
    }

    private ScreenResponse mapToResponse(Screen s) {
        ScreenResponse resp = new ScreenResponse(s.getScreenId(), s.getTheatreId(), s.getScreenCode(), s.getScreenName(), s.getIsActive());
        if (s.getScreenId() != null) {
            if (seatRepository != null) {
                resp.setTotalCapacity((int) seatRepository.findByScreenId(s.getScreenId()).size());
            }
            java.util.List<com.pvk.cinemas.scheduling.model.ScreenCapability> caps = screenCapabilityRepository.findByScreenId(s.getScreenId());
            if (!caps.isEmpty()) {
                resp.setScreenCapabilityId(caps.get(0).getScreenCapabilityId());
                if (screenCapabilityService != null) {
                    resp.setCapabilities(caps.stream().map(screenCapabilityService::mapToResponse).collect(Collectors.toList()));
                }
            }
        }
        return resp;
    }
}
