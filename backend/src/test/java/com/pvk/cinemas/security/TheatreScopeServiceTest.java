package com.pvk.cinemas.security;

import com.pvk.cinemas.infrastructure.model.Screen;
import com.pvk.cinemas.infrastructure.repository.ScreenRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.scheduling.repository.ScreenCapabilityRepository;
import com.pvk.cinemas.scheduling.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TheatreScopeServiceTest {

    @Mock
    private EmployeeTheatreRepository employeeTheatreRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ScreenCapabilityRepository screenCapabilityRepository;

    @Mock
    private ShowRepository showRepository;

    @InjectMocks
    private TheatreScopeService theatreScopeService;

    private final Long managerAUserId = 101L;
    private final Long theatreAId = 1L;

    private final Long managerBUserId = 102L;
    private final Long theatreBId = 2L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String email, String role) {
        UserPrincipal principal = new UserPrincipal(userId, email, "hash", "ACTIVE", List.of(new SimpleGrantedAuthority(role)));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Manager A can access Theatre A but is rejected from Theatre B")
    void testManagerScopeIsolation_ManagerA() {
        authenticate(managerAUserId, "managerA@pvk.com", "ROLE_THEATRE_MANAGER");

        when(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(managerAUserId, theatreAId)).thenReturn(true);
        when(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(managerAUserId, theatreBId)).thenReturn(false);

        // Manager A -> Theatre A allowed
        assertTrue(theatreScopeService.hasAccessToTheatre(theatreAId));
        // Manager A -> Theatre B rejected
        assertFalse(theatreScopeService.hasAccessToTheatre(theatreBId));
    }

    @Test
    @DisplayName("Manager B can access Theatre B but is rejected from Theatre A")
    void testManagerScopeIsolation_ManagerB() {
        authenticate(managerBUserId, "managerB@pvk.com", "ROLE_THEATRE_MANAGER");

        when(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(managerBUserId, theatreBId)).thenReturn(true);
        when(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(managerBUserId, theatreAId)).thenReturn(false);

        // Manager B -> Theatre B allowed
        assertTrue(theatreScopeService.hasAccessToTheatre(theatreBId));
        // Manager B -> Theatre A rejected
        assertFalse(theatreScopeService.hasAccessToTheatre(theatreAId));
    }

    @Test
    @DisplayName("Super Admin has global access to all theatres")
    void testSuperAdminGlobalAccess() {
        authenticate(1L, "admin@pvk.com", "ROLE_SUPER_ADMIN");

        assertTrue(theatreScopeService.hasAccessToTheatre(theatreAId));
        assertTrue(theatreScopeService.hasAccessToTheatre(theatreBId));
        assertTrue(theatreScopeService.hasAccessToTheatre(999L));
    }

    @Test
    @DisplayName("Customer is denied access to theatre management")
    void testCustomerDeniedAccess() {
        authenticate(301L, "customer@pvk.com", "ROLE_CUSTOMER");

        assertFalse(theatreScopeService.hasAccessToTheatre(theatreAId));
        assertFalse(theatreScopeService.hasAccessToTheatre(theatreBId));
    }

    @Test
    @DisplayName("Screen access follows Theatre ownership scope")
    void testScreenAccessFollowsTheatreScope() {
        authenticate(managerAUserId, "managerA@pvk.com", "ROLE_THEATRE_MANAGER");

        Screen screen1 = new Screen();
        screen1.setScreenId(10L);
        screen1.setTheatreId(theatreAId);

        Screen screen2 = new Screen();
        screen2.setScreenId(20L);
        screen2.setTheatreId(theatreBId);

        when(screenRepository.findById(10L)).thenReturn(Optional.of(screen1));
        when(screenRepository.findById(20L)).thenReturn(Optional.of(screen2));

        when(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(managerAUserId, theatreAId)).thenReturn(true);
        when(employeeTheatreRepository.existsByIdUserIdAndIdTheatreId(managerAUserId, theatreBId)).thenReturn(false);

        assertTrue(theatreScopeService.hasAccessToScreen(10L));
        assertFalse(theatreScopeService.hasAccessToScreen(20L));
    }
}
