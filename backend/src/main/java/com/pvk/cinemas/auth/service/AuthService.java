package com.pvk.cinemas.auth.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.auth.dto.AuthResponse;
import com.pvk.cinemas.auth.dto.CurrentUserResponse;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.auth.dto.RegisterRequest;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.security.JwtTokenProvider;
import com.pvk.cinemas.security.TokenRevocationStore;
import com.pvk.cinemas.security.UserPrincipal;
import com.pvk.cinemas.security.model.Permission;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.RolePermission;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.PermissionRepository;
import com.pvk.cinemas.security.repository.RolePermissionRepository;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.CustomerProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.CustomerProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final LanguageRepository languageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationStore tokenRevocationStore;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       CustomerProfileRepository customerProfileRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository,
                       LanguageRepository languageRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenRevocationStore tokenRevocationStore,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.languageRepository = languageRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationStore = tokenRevocationStore;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already registered: " + request.getPhone());
        }

        if (request.getPreferredLanguageId() != null) {
            languageRepository.findById(request.getPreferredLanguageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Preferred language not found with ID: " + request.getPreferredLanguageId()));
        }

        // 1. Create USER
        User user = new User(
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getPhone().trim(),
                passwordEncoder.encode(request.getPassword()),
                "ACTIVE"
        );
        user = userRepository.save(user);

        // 2. Create CUSTOMER_PROFILE
        Long prefLangId = request.getPreferredLanguageId() != null ? request.getPreferredLanguageId().longValue() : null;
        CustomerProfile profile = new CustomerProfile(user.getUserId(), prefLangId, null);
        customerProfileRepository.save(profile);

        // 3. Assign CUSTOMER role
        Role customerRole = roleRepository.findByRoleCode("CUSTOMER")
                .or(() -> roleRepository.findByRoleCode("ROLE_CUSTOMER"))
                .orElseThrow(() -> new ResourceNotFoundException("Role CUSTOMER not found in reference data"));
        UserRole userRole = new UserRole(user.getUserId(), customerRole.getRoleId());
        userRoleRepository.save(userRole);

        // 4. Emit Audit Log
        auditLogService.logAction(user.getUserId(), "AUTH_REGISTER", "USER", user.getUserId(), null, "{\"email\":\"" + user.getEmail() + "\"}");

        // 5. Generate Token
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), List.of("CUSTOMER"));

        return new AuthResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                "CUSTOMER",
                token
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new AccessDeniedException("Account is not active. Status: " + user.getAccountStatus());
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Resolve roles & permissions
        List<UserRole> userRoles = userRoleRepository.findByIdUserId(user.getUserId());
        List<String> roleCodes = new ArrayList<>();
        List<String> permissions = new ArrayList<>();

        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getId().getRoleId()).ifPresent(r -> {
                roleCodes.add(r.getRoleCode());
                List<RolePermission> rps = rolePermissionRepository.findByIdRoleId(r.getRoleId());
                for (RolePermission rp : rps) {
                    permissionRepository.findById(rp.getId().getPermissionId()).ifPresent(p -> {
                        if (!permissions.contains(p.getPermissionCode())) {
                            permissions.add(p.getPermissionCode());
                        }
                    });
                }
            });
        }

        String primaryRole = roleCodes.isEmpty() ? "CUSTOMER" : roleCodes.get(0);
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), roleCodes);

        // Emit Audit Log
        auditLogService.logAction(user.getUserId(), "AUTH_LOGIN", "USER", user.getUserId(), null, "{\"email\":\"" + user.getEmail() + "\"}");

        AuthResponse resp = new AuthResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                primaryRole,
                token
        );
        resp.setPermissions(permissions);
        return resp;
    }

    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken = bearerToken.substring(7);
        }
        if (bearerToken != null && jwtTokenProvider.validateToken(bearerToken)) {
            String jti = jwtTokenProvider.getJti(bearerToken);
            Instant expiry = jwtTokenProvider.getExpiration(bearerToken);
            tokenRevocationStore.revokeToken(jti, expiry);
            try {
                Long userId = jwtTokenProvider.getUserId(bearerToken);
                auditLogService.logAction(userId, "AUTH_LOGOUT", "USER", userId, null, "{\"action\":\"LOGOUT\"}");
            } catch (Exception ignored) {}
        }
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUserId()));

        List<UserRole> userRoles = userRoleRepository.findByIdUserId(user.getUserId());
        List<String> roleCodes = new ArrayList<>();
        List<String> permissions = new ArrayList<>();

        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getId().getRoleId()).ifPresent(r -> {
                roleCodes.add(r.getRoleCode());
                List<RolePermission> rps = rolePermissionRepository.findByIdRoleId(r.getRoleId());
                for (RolePermission rp : rps) {
                    permissionRepository.findById(rp.getId().getPermissionId()).ifPresent(p -> {
                        if (!permissions.contains(p.getPermissionCode())) {
                            permissions.add(p.getPermissionCode());
                        }
                    });
                }
            });
        }

        return new CurrentUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAccountStatus(),
                roleCodes,
                permissions
        );
    }
}
