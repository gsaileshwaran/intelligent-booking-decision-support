package com.pvk.cinemas.security;

import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.model.Seat;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.infrastructure.repository.SeatRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.scheduling.model.ScreenCapability;
import com.pvk.cinemas.scheduling.model.Show;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("theatreScopeService")
public class TheatreScopeService {

    private final EmployeeTheatreRepository employeeTheatreRepository;
    private final ScreenRepository screenRepository;
    private final ScreenCapabilityRepository screenCapabilityRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public TheatreScopeService(EmployeeTheatreRepository employeeTheatreRepository,
                               ScreenRepository screenRepository,
                               ScreenCapabilityRepository screenCapabilityRepository,
                               ShowRepository showRepository) {
        this(employeeTheatreRepository, screenRepository, screenCapabilityRepository, showRepository, null);
    }

    @Autowired
    public TheatreScopeService(EmployeeTheatreRepository employeeTheatreRepository,
                               ScreenRepository screenRepository,
                               ScreenCapabilityRepository screenCapabilityRepository,
                               ShowRepository showRepository,
                               SeatRepository seatRepository) {
        this.employeeTheatreRepository = employeeTheatreRepository;
        this.screenRepository = screenRepository;
        this.screenCapabilityRepository = screenCapabilityRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    public boolean hasAccessToTheatre(Long theatreId) {
        if (theatreId == null) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }

        // Super Admin has platform-wide authority
        if (isSuperAdmin(auth)) {
            return true;
        }

        // Only Theatre Managers may have theatre-scoped operational authority
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_THEATRE_MANAGER") || a.getAuthority().equals("THEATRE_MANAGER"));
        if (!isManager) {
            return false;
        }

        // Check Theatre Manager assignment in EMPLOYEE_THEATRE
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

    public boolean hasAccessToSeat(Long seatId) {
        if (seatId == null || seatRepository == null) {
            return false;
        }
        return seatRepository.findById(seatId)
                .map(Seat::getScreenId)
                .map(this::hasAccessToScreen)
                .orElse(false);
    }

    public boolean hasAccessToSeat(Integer seatId) {
        return seatId != null && hasAccessToSeat(seatId.longValue());
    }

    public boolean hasAccessToCapability(Long screenCapabilityId) {
        if (screenCapabilityId == null || screenCapabilityRepository == null) {
            return false;
        }
        return screenCapabilityRepository.findById(screenCapabilityId)
                .map(ScreenCapability::getScreenId)
                .map(this::hasAccessToScreen)
                .orElse(false);
    }

    public boolean hasAccessToCapability(Integer screenCapabilityId) {
        return screenCapabilityId != null && hasAccessToCapability(screenCapabilityId.longValue());
    }

    public boolean hasAccessToShow(Long showId) {
        if (showId == null) {
            return false;
        }
        return showRepository.findById(showId)
                .map(show -> {
                    if (show.getScreenId() != null) {
                        return hasAccessToScreen(show.getScreenId());
                    }
                    return screenCapabilityRepository.findById(show.getScreenCapabilityId())
                            .map(ScreenCapability::getScreenId)
                            .map(this::hasAccessToScreen)
                            .orElse(false);
                })
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

    public boolean isSeatInScreen(Long seatId, Long screenId) {
        if (seatId == null || screenId == null || seatRepository == null) {
            return false;
        }
        return seatRepository.findById(seatId)
                .map(Seat::getScreenId)
                .map(sId -> sId.equals(screenId))
                .orElse(false);
    }

    public boolean isSeatInScreen(Long seatId, Integer screenId) {
        return screenId != null && isSeatInScreen(seatId, screenId.longValue());
    }

    public boolean isCapabilityInScreen(Long screenCapabilityId, Long screenId) {
        if (screenCapabilityId == null || screenId == null || screenCapabilityRepository == null) {
            return false;
        }
        return screenCapabilityRepository.findById(screenCapabilityId)
                .map(ScreenCapability::getScreenId)
                .map(sId -> sId.equals(screenId))
                .orElse(false);
    }

    public boolean isCapabilityInScreen(Long screenCapabilityId, Integer screenId) {
        return screenId != null && isCapabilityInScreen(screenCapabilityId, screenId.longValue());
    }

    public boolean isCapabilityInScreen(Integer screenCapabilityId, Integer screenId) {
        return screenCapabilityId != null && screenId != null
                && isCapabilityInScreen(screenCapabilityId.longValue(), screenId.longValue());
    }

    public boolean isShowInTheatre(Long showId, Integer theatreId) {
        return showId != null && theatreId != null && isShowInTheatre(showId, theatreId.longValue());
    }

    public boolean isShowInTheatre(Long showId, Long theatreId) {
        if (showId == null || theatreId == null) {
            return false;
        }
        return showRepository.findById(showId)
                .map(show -> {
                    Long screenId = show.getScreenId();
                    if (screenId != null) {
                        return isScreenInTheatre(screenId, theatreId);
                    }
                    return screenCapabilityRepository.findById(show.getScreenCapabilityId())
                            .map(ScreenCapability::getScreenId)
                            .flatMap(screenRepository::findById)
                            .map(Screen::getTheatreId)
                            .map(tId -> tId.equals(theatreId))
                            .orElse(false);
                })
                .orElse(false);
    }

    // ── Permission & Role Resolution ──────────────────────────────────────────

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        if (isSuperAdmin(auth)) {
            return true;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(permissionCode)
                        || a.getAuthority().equalsIgnoreCase("PERM_" + permissionCode));
    }

    public boolean hasRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        String cleanRole = roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_" + cleanRole)
                        || a.getAuthority().equalsIgnoreCase(cleanRole));
    }

    public boolean hasTheatreScopeAndPermission(Long theatreId, String permissionCode) {
        return hasAccessToTheatre(theatreId) && hasPermission(permissionCode);
    }

    public boolean hasScreenScopeAndPermission(Long screenId, String permissionCode) {
        return hasAccessToScreen(screenId) && hasPermission(permissionCode);
    }

    public boolean hasShowScopeAndPermission(Long showId, String permissionCode) {
        return hasAccessToShow(showId) && hasPermission(permissionCode);
    }

    // ── Domain Guard Methods for Service-Level Authorization ─────────────────

    public void checkTheatreScope(Long theatreId) {
        if (theatreId == null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!hasAccessToTheatre(theatreId)) {
                throw new AccessDeniedException("Access denied: Not authorized for theatre " + theatreId);
            }
        }
    }

    public void checkTheatreScope(Integer theatreId) {
        if (theatreId != null) {
            checkTheatreScope(theatreId.longValue());
        }
    }

    public void checkScreenScope(Long screenId) {
        if (screenId == null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!hasAccessToScreen(screenId)) {
                throw new AccessDeniedException("Access denied: Not authorized for screen " + screenId);
            }
        }
    }

    public void checkScreenScope(Integer screenId) {
        if (screenId != null) {
            checkScreenScope(screenId.longValue());
        }
    }

    public void checkSeatScope(Long seatId) {
        if (seatId == null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!hasAccessToSeat(seatId)) {
                throw new AccessDeniedException("Access denied: Not authorized for seat " + seatId);
            }
        }
    }

    public void checkCapabilityScope(Long screenCapabilityId) {
        if (screenCapabilityId == null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!hasAccessToCapability(screenCapabilityId)) {
                throw new AccessDeniedException("Access denied: Not authorized for screen capability " + screenCapabilityId);
            }
        }
    }

    public void checkCapabilityScope(Integer screenCapabilityId) {
        if (screenCapabilityId != null) {
            checkCapabilityScope(screenCapabilityId.longValue());
        }
    }

    public void checkShowScope(Long showId) {
        if (showId == null) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!hasAccessToShow(showId)) {
                throw new AccessDeniedException("Access denied: Not authorized for show " + showId);
            }
        }
    }

    public void checkShowScope(Long showId, Long theatreId) {
        checkShowScope(showId);
        if (theatreId != null && !isShowInTheatre(showId, theatreId)) {
            throw new AccessDeniedException("Access denied: Show " + showId + " does not belong to theatre " + theatreId);
        }
    }

    public void checkShowScope(Long showId, Integer theatreId) {
        checkShowScope(showId, theatreId != null ? theatreId.longValue() : null);
    }

    public void checkSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!isSuperAdmin(auth)) {
                throw new AccessDeniedException("Access denied: Operation requires SUPER_ADMIN authority");
            }
        }
    }

    public void checkPermission(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (!hasPermission(permissionCode)) {
                throw new AccessDeniedException("Access denied: Missing required permission " + permissionCode);
            }
        }
    }

    private boolean isSuperAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
    }
}
