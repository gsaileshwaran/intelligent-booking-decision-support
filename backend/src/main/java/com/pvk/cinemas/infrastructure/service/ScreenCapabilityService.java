package com.pvk.cinemas.infrastructure.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.catalogue.model.AudioFormat;
import com.pvk.cinemas.catalogue.model.PresentationFormat;
import com.pvk.cinemas.catalogue.repository.AudioFormatRepository;
import com.pvk.cinemas.catalogue.repository.PresentationFormatRepository;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.infrastructure.dto.ScreenCapabilityRequest;
import com.pvk.cinemas.infrastructure.dto.ScreenCapabilityResponse;
import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import com.pvk.cinemas.security.TheatreScopeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScreenCapabilityService {

    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final ScreenRepository screenRepository;
    private final PresentationFormatRepository presentationFormatRepository;
    private final AudioFormatRepository audioFormatRepository;
    private final ShowRepository showRepository;
    private final AuditLogService auditLogService;
    private final TheatreScopeService theatreScopeService;

    public ScreenCapabilityService(ScreenCapabilityRepository screenCapabilityRepository,
                                   ScreenRepository screenRepository,
                                   PresentationFormatRepository presentationFormatRepository,
                                   AudioFormatRepository audioFormatRepository,
                                   ShowRepository showRepository,
                                   AuditLogService auditLogService,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false)
                                   TheatreScopeService theatreScopeService) {
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.screenRepository = screenRepository;
        this.presentationFormatRepository = presentationFormatRepository;
        this.audioFormatRepository = audioFormatRepository;
        this.showRepository = showRepository;
        this.auditLogService = auditLogService;
        this.theatreScopeService = theatreScopeService;
    }

    @Transactional(readOnly = true)
    public List<ScreenCapabilityResponse> getCapabilitiesByScreen(Long screenId) {
        if (theatreScopeService != null) {
            theatreScopeService.checkScreenScope(screenId);
        }
        if (screenId == null) {
            throw new ResourceNotFoundException("Screen not found: null");
        }
        screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));

        return screenCapabilityRepository.findByScreenId(screenId).stream()
                .sorted((c1, c2) -> Long.compare(c1.getScreenCapabilityId(), c2.getScreenCapabilityId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenCapabilityResponse> getCapabilitiesByScreen(Integer screenId) {
        return getCapabilitiesByScreen(screenId != null ? screenId.longValue() : null);
    }

    @Transactional
    public ScreenCapabilityResponse addCapabilityToScreen(Long screenId, ScreenCapabilityRequest request, Long actorUserId, String ipAddress) {
        if (theatreScopeService != null) {
            theatreScopeService.checkScreenScope(screenId);
        }
        if (screenId == null) {
            throw new ResourceNotFoundException("Screen not found: null");
        }
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + screenId));

        PresentationFormat pf = presentationFormatRepository.findById(request.getPresentationFormatId())
                .orElseThrow(() -> new ResourceNotFoundException("Presentation format not found: " + request.getPresentationFormatId()));

        AudioFormat af = audioFormatRepository.findById(request.getAudioFormatId())
                .orElseThrow(() -> new ResourceNotFoundException("Audio format not found: " + request.getAudioFormatId()));

        screenCapabilityRepository.findByScreenIdAndPresentationFormatIdAndAudioFormatId(
                screenId, request.getPresentationFormatId(), request.getAudioFormatId())
                .ifPresent(existing -> {
                    throw new ConflictException("Screen capability already exists on screen " + screenId + " for formats "
                            + pf.getFormatCode() + " / " + af.getFormatCode());
                });

        ScreenCapability capability = new ScreenCapability(screenId, request.getPresentationFormatId(), request.getAudioFormatId());
        capability = screenCapabilityRepository.save(capability);

        auditLogService.logAction(actorUserId, "SCREEN_CAPABILITY_CREATE", "SCREEN_CAPABILITY",
                String.valueOf(capability.getScreenCapabilityId()),
                "Added capability [" + pf.getFormatCode() + " + " + af.getFormatCode() + "] to screen " + screen.getScreenCode(),
                ipAddress);

        return mapToResponse(capability);
    }

    @Transactional
    public ScreenCapabilityResponse addCapabilityToScreen(Integer screenId, ScreenCapabilityRequest request, Long actorUserId, String ipAddress) {
        return addCapabilityToScreen(screenId != null ? screenId.longValue() : null, request, actorUserId, ipAddress);
    }

    @Transactional
    public void deleteCapability(Long screenId, Long capabilityId, Long actorUserId, String ipAddress) {
        if (theatreScopeService != null) {
            theatreScopeService.checkScreenScope(screenId);
            theatreScopeService.checkCapabilityScope(capabilityId);
        }
        if (screenId == null || capabilityId == null) {
            throw new ResourceNotFoundException("Screen or Capability identifier cannot be null");
        }
        ScreenCapability capability = screenCapabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen capability not found: " + capabilityId));

        if (!capability.getScreenId().equals(screenId)) {
            throw new AccessDeniedException("Capability " + capabilityId + " does not belong to screen " + screenId);
        }

        // Check if scheduled/active shows depend on this capability
        long showsUsingCapability = showRepository.findAll().stream()
                .filter(s -> capabilityId.equals(s.getScreenCapabilityId()))
                .filter(s -> !"CANCELLED".equalsIgnoreCase(s.getShowStatus()))
                .count();
        if (showsUsingCapability > 0) {
            throw new ConflictException("Cannot delete screen capability: " + showsUsingCapability + " active shows depend on this configuration");
        }

        screenCapabilityRepository.delete(capability);
        auditLogService.logAction(actorUserId, "SCREEN_CAPABILITY_DELETE", "SCREEN_CAPABILITY",
                String.valueOf(capabilityId), "Deleted screen capability " + capabilityId + " from screen " + screenId, ipAddress);
    }

    @Transactional
    public void deleteCapability(Integer screenId, Long capabilityId, Long actorUserId, String ipAddress) {
        deleteCapability(screenId != null ? screenId.longValue() : null, capabilityId, actorUserId, ipAddress);
    }

    public ScreenCapabilityResponse mapToResponse(ScreenCapability cap) {
        ScreenCapabilityResponse resp = new ScreenCapabilityResponse();
        resp.setScreenCapabilityId(cap.getScreenCapabilityId());
        resp.setScreenId(cap.getScreenId());
        resp.setPresentationFormatId(cap.getPresentationFormatId());
        resp.setAudioFormatId(cap.getAudioFormatId());

        if (cap.getPresentationFormatId() != null) {
            presentationFormatRepository.findById(cap.getPresentationFormatId()).ifPresent(pf -> {
                resp.setPresentationFormatCode(pf.getFormatCode());
                resp.setPresentationFormatName(pf.getName());
            });
        }
        if (cap.getAudioFormatId() != null) {
            audioFormatRepository.findById(cap.getAudioFormatId()).ifPresent(af -> {
                resp.setAudioFormatCode(af.getFormatCode());
                resp.setAudioFormatName(af.getName());
            });
        }
        return resp;
    }
}
