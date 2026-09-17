package com.pvk.cinemas.security;

import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("theatreScopeService")
public class TheatreScopeService {

    private final EmployeeTheatreRepository employeeTheatreRepository;
    private final ScreenRepository screenRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final ShowRepository showRepository;

    public TheatreScopeService(EmployeeTheatreRepository employeeTheatreRepository,
                               ScreenRepository screenRepository,
                               ScreenCapabilityRepository screenCapabilityRepository,
                               ShowRepository showRepository) {
        this.employeeTheatreRepository = employeeTheatreRepository;
        this.screenRepository = screenRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.showRepository = showRepository;
    }

    public boolean hasAccessToTheatre(Long theatreId) {
        if (theatreId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Super Admin has platform-wide authority
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
        if (isSuperAdmin) {
            return true;
        }

        // Check Theatre Manager scope
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            Long userId = principal.getUserId();
            return employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(userId, theatreId);
        }

        return false;
    }

    public boolean hasAccessToTheatre(Integer theatreId) {
        return theatreId != null && hasAccessToTheatre(theatreId.longValue());
    }

    public boolean hasAccessToScreen(Long screenId) {
        if (screenId == null) {
            return false;
        }
        return screenRepository.findById(screenId)
                .map(Screen::getTheatreId)
                .map(this::hasAccessToTheatre)
                .orElse(false);
    }

    public boolean hasAccessToScreen(Integer screenId) {
        return screenId != null && hasAccessToScreen(screenId.longValue());
    }

    public boolean hasAccessToShow(Long showId) {
        if (showId == null) {
            return false;
        }
        return showRepository.findById(showId)
                .map(Show::getScreenCapabilityId)
                .flatMap(screenCapabilityRepository::findById)
                .map(ScreenCapability::getScreenId)
                .map(this::hasAccessToScreen)
                .orElse(false);
    }

    public boolean isScreenInTheatre(Integer screenId, Integer theatreId) {
        return screenId != null && theatreId != null && isScreenInTheatre(screenId.longValue(), theatreId.longValue());
    }

    public boolean isScreenInTheatre(Long screenId, Long theatreId) {
        if (screenId == null || theatreId == null) {
            return false;
        }
        return screenRepository.findById(screenId)
                .map(Screen::getTheatreId)
                .map(tId -> tId.equals(theatreId))
                .orElse(false);
    }

    public boolean isShowInTheatre(Long showId, Integer theatreId) {
        return showId != null && theatreId != null && isShowInTheatre(showId, theatreId.longValue());
    }

    public boolean isShowInTheatre(Long showId, Long theatreId) {
        if (showId == null || theatreId == null) {
            return false;
        }
        return showRepository.findById(showId)
                .map(Show::getScreenCapabilityId)
                .flatMap(screenCapabilityRepository::findById)
                .map(ScreenCapability::getScreenId)
                .flatMap(screenRepository::findById)
                .map(Screen::getTheatreId)
                .map(tId -> tId.equals(theatreId))
                .orElse(false);
    }
}
