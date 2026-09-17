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
    private final AuditLogService auditLogService;
    private final com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository;

    public ScreenService(ScreenRepository screenRepository,
                         TheatreRepository theatreRepository,
                         AuditLogService auditLogService,
                         com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository screenCapabilityRepository) {
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.auditLogService = auditLogService;
        this.screenCapabilityRepository = screenCapabilityRepository;
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheatre(Integer theatreId) {
        theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found: " + theatreId));
        return screenRepository.findByTheatreIdAndIsActiveTrue(theatreId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ScreenResponse createScreen(Integer theatreId, ScreenRequest request, Long actorUserId, String ipAddress) {
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
    public ScreenResponse updateScreen(Integer theatreId, Integer screenId, ScreenRequest request, Long actorUserId, String ipAddress) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));

        if (theatreId != null && !screen.getTheatreId().equals(theatreId.longValue())) {
            throw new AccessDeniedException("Screen does not belong to specified theatre");
        }

        if (request.getScreenName() != null) screen.setScreenName(request.getScreenName().trim());
        if (request.getIsActive() != null) screen.setIsActive(request.getIsActive());
        screen = screenRepository.save(screen);

        auditLogService.logAction(actorUserId, "SCREEN_UPDATE", "SCREEN", String.valueOf(screenId), "Updated screen " + screen.getScreenName(), ipAddress);
        return mapToResponse(screen);
    }

    @Transactional
    public ScreenResponse updateScreen(Integer screenId, ScreenRequest request, Long actorUserId, String ipAddress) {
        return updateScreen(null, screenId, request, actorUserId, ipAddress);
    }

    private ScreenResponse mapToResponse(Screen s) {
        ScreenResponse resp = new ScreenResponse(s.getScreenId(), s.getTheatreId(), s.getScreenCode(), s.getScreenName(), s.getIsActive());
        if (s.getScreenId() != null) {
            java.util.List<com.pvk.cinemas.scheduling.model.ScreenCapability> caps = screenCapabilityRepository.findByScreenId(s.getScreenId());
            if (!caps.isEmpty()) {
                resp.setScreenCapabilityId(caps.get(0).getScreenCapabilityId());
            }
        }
        return resp;
    }
}
