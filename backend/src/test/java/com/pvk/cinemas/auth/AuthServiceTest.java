package com.pvk.cinemas.auth;

import com.pvk.cinemas.auth.dto.AuthResponse;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.auth.dto.RegisterRequest;
import com.pvk.cinemas.auth.service.AuthService;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.security.JwtTokenProvider;
import com.pvk.cinemas.security.TokenRevocationStore;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.PermissionRepository;
import com.pvk.cinemas.security.repository.RolePermissionRepository;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.CustomerProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.CustomerProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRevocationStore tokenRevocationStore;

    @Mock
    private com.pvk.cinemas.audit.service.AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@customer.com");
        registerRequest.setPassword("SecretPassword123!");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPhone("+919876543210");
    }

    @Test
    @DisplayName("Customer registration transactionally creates USER, CUSTOMER_PROFILE, and ROLE_CUSTOMER")
    void testSuccessfulRegistration() {
        when(userRepository.existsByEmail("test@customer.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919876543210")).thenReturn(false);
        when(passwordEncoder.encode("SecretPassword123!")).thenReturn("$2a$10$hashedPassword");

        User savedUser = new User("John", "Doe", "test@customer.com", "+919876543210", "$2a$10$hashedPassword", "ACTIVE");
        savedUser.setUserId(500L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Role customerRole = new Role();
        customerRole.setRoleId(1);
        customerRole.setRoleCode("CUSTOMER");
        customerRole.setRoleName("Customer");
        when(roleRepository.findByRoleCode("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(jwtTokenProvider.generateToken(eq(500L), eq("test@customer.com"), anyList())).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("test@customer.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("CUSTOMER", response.getRole());

        // Verify entities were persisted
        verify(userRepository).save(any(User.class));
        verify(customerProfileRepository).save(any(CustomerProfile.class));
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Registration rejects duplicate email with 409 Conflict")
    void testDuplicateEmailRegistration() {
        when(userRepository.existsByEmail("test@customer.com")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                authService.register(registerRequest));

        verify(userRepository, never()).save(any());
        verify(customerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration rejects duplicate phone with 409 Conflict")
    void testDuplicatePhoneRegistration() {
        when(userRepository.existsByEmail("test@customer.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919876543210")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                authService.register(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login with valid credentials returns JWT")
    void testLoginValidCredentials() {
        User user = new User("John", "Doe", "test@customer.com", "+919876543210", "$2a$10$hashedPassword", "ACTIVE");
        user.setUserId(500L);

        when(userRepository.findByEmail("test@customer.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecretPassword123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(userRoleRepository.findByIdUserId(500L)).thenReturn(List.of());
        when(jwtTokenProvider.generateToken(eq(500L), eq("test@customer.com"), anyList())).thenReturn("valid.jwt.token");

        LoginRequest loginRequest = new LoginRequest("test@customer.com", "SecretPassword123!");
        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("valid.jwt.token", response.getToken());
    }

    @Test
    @DisplayName("Login with invalid password throws BadCredentialsException")
    void testLoginInvalidPassword() {
        User user = new User("John", "Doe", "test@customer.com", "+919876543210", "$2a$10$hashedPassword", "ACTIVE");
        user.setUserId(500L);

        when(userRepository.findByEmail("test@customer.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        LoginRequest loginRequest = new LoginRequest("test@customer.com", "WrongPassword");
        assertThrows(BadCredentialsException.class, () ->
                authService.login(loginRequest));
    }

    @Test
    @DisplayName("Logout invalidates token in revocation store")
    void testLogoutRevokesToken() {
        String authHeader = "Bearer sample.jwt.token";
        when(jwtTokenProvider.validateToken("sample.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.getJti("sample.jwt.token")).thenReturn("uuid-jti-123");
        Instant expiry = Instant.now().plusSeconds(3600);
        when(jwtTokenProvider.getExpiration("sample.jwt.token")).thenReturn(expiry);

        authService.logout(authHeader);

        verify(tokenRevocationStore).revokeToken("uuid-jti-123", expiry);
    }
}
