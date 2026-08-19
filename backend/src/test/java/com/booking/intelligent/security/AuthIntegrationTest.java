package com.booking.intelligent.security;

import com.booking.intelligent.dto.AuthRequest;
import com.booking.intelligent.dto.AuthResponse;
import com.booking.intelligent.dto.RegisterRequest;
import com.booking.intelligent.entity.Role;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.repository.RoleRepository;
import com.booking.intelligent.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));
        roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));
    }

    @Test
    void testUserRegistrationAndLoginFlow() {
        String testEmail = "testuser_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test Customer");
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword("securePassword123");
        registerRequest.setRole(RoleName.ROLE_CUSTOMER);

        AuthResponse regResponse = authService.registerUser(registerRequest);
        assertNotNull(regResponse.getToken(), "JWT token should be returned upon registration");
        assertEquals(testEmail, regResponse.getEmail());
        assertEquals("ROLE_CUSTOMER", regResponse.getRole());

        // Test Login
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword("securePassword123");

        AuthResponse loginResponse = authService.authenticateUser(loginRequest);
        assertNotNull(loginResponse.getToken(), "JWT token should be returned upon login");
        assertTrue(jwtTokenProvider.validateToken(loginResponse.getToken()), "Returned JWT token should be valid");
        assertEquals(regResponse.getUserId(), jwtTokenProvider.getUserIdFromJWT(loginResponse.getToken()));
    }

    @Test
    void testInvalidPasswordThrowsException() {
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("wrongPassword");

        assertThrows(AuthenticationException.class, () -> {
            authService.authenticateUser(loginRequest);
        });
    }
}
