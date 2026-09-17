package com.pvk.cinemas.user.service;

import com.pvk.cinemas.audit.service.AuditLogService;
import com.pvk.cinemas.catalogue.model.Language;
import com.pvk.cinemas.catalogue.repository.LanguageRepository;
import com.pvk.cinemas.common.exceptions.BadRequestException;
import com.pvk.cinemas.common.exceptions.ConflictException;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.organization.dto.AssignManagerRequest;
import com.pvk.cinemas.organization.service.TheatreService;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.dto.AdminUserResponse;
import com.pvk.cinemas.user.dto.CreateUserRequest;
import com.pvk.cinemas.user.dto.ProfileResponse;
import com.pvk.cinemas.user.dto.UpdateProfileRequest;
import com.pvk.cinemas.user.dto.UpdateUserStatusRequest;
import com.pvk.cinemas.user.model.CustomerProfile;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.CustomerProfileRepository;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final LanguageRepository languageRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TheatreService theatreService;

    public UserService(UserRepository userRepository,
                       CustomerProfileRepository customerProfileRepository,
                       EmployeeProfileRepository employeeProfileRepository,
                       UserRoleRepository userRoleRepository,
                       RoleRepository roleRepository,
                       LanguageRepository languageRepository,
                       AuditLogService auditLogService,
                       PasswordEncoder passwordEncoder,
                       @Lazy TheatreService theatreService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.languageRepository = languageRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.theatreService = theatreService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ProfileResponse resp = new ProfileResponse();
        resp.setUserId(user.getUserId());
        resp.setFirstName(user.getFirstName());
        resp.setLastName(user.getLastName());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());

        customerProfileRepository.findByUserId(userId).ifPresent(cp -> {
            resp.setPreferredLanguageId(cp.getPreferredLanguageId() != null ? cp.getPreferredLanguageId().intValue() : null);
            resp.setDateOfBirth(cp.getDateOfBirth());
            if (cp.getPreferredLanguageId() != null) {
                languageRepository.findById(cp.getPreferredLanguageId())
                        .map(Language::getLanguageName)
                        .ifPresent(resp::setPreferredLanguageName);
            }
        });

        employeeProfileRepository.findByUserId(userId).ifPresent(ep -> {
            resp.setEmployeeCode(ep.getEmployeeCode());
        });

        return resp;
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getPhone() != null && !request.getPhone().trim().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone().trim())) {
                throw new ConflictException("Phone number already in use: " + request.getPhone());
            }
            user.setPhone(request.getPhone().trim());
        }

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        if (request.getPreferredLanguageId() != null || request.getDateOfBirth() != null) {
            CustomerProfile cp = customerProfileRepository.findByUserId(userId)
                    .orElseGet(() -> new CustomerProfile(userId, (Long) null, null));

            if (request.getPreferredLanguageId() != null) {
                languageRepository.findById(request.getPreferredLanguageId().longValue())
                        .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + request.getPreferredLanguageId()));
                cp.setPreferredLanguageId(request.getPreferredLanguageId().longValue());
            }
            if (request.getDateOfBirth() != null) {
                cp.setDateOfBirth(request.getDateOfBirth());
            }
            cp.setUpdatedAt(Instant.now());
            customerProfileRepository.save(cp);
        }

        return getProfile(userId);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToAdminUserResponse);
    }

    /**
     * Super Admin provisioning: creates a user with encoded password, assigns role,
     * creates appropriate profile, and optionally assigns theatre if ROLE_THEATRE_MANAGER.
     */
    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request, Long actorUserId, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        user.setAccountStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        // Assign role
        String rawRole = (request.getRoleCode() != null && !request.getRoleCode().isBlank())
                ? request.getRoleCode().trim().toUpperCase()
                : "ROLE_CUSTOMER";
        // Normalize: strip ROLE_ prefix then re-add to ensure consistent lookup
        String roleLookup = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
        Role role = roleRepository.findByRoleCode(roleLookup)
                .orElseGet(() -> roleRepository.findByRoleCode(rawRole).orElseThrow(
                        () -> new ResourceNotFoundException("Role not found: " + rawRole)));
        userRoleRepository.save(new UserRole(user.getUserId(), role.getRoleId(), actorUserId));

        // Create profile
        boolean isManager = roleLookup.contains("MANAGER");
        if (isManager) {
            // Employee profile for manager — use 2-arg constructor (userId, employeeCode)
            EmployeeProfile ep = new EmployeeProfile(user.getUserId(), "MGR-" + user.getUserId());
            employeeProfileRepository.save(ep);

            // Assign to theatre if theatreId provided
            if (request.getTheatreId() != null) {
                AssignManagerRequest amr = new AssignManagerRequest();
                amr.setUserId(user.getUserId());
                theatreService.assignManager(request.getTheatreId(), amr, actorUserId, ipAddress);
            }
        } else {
            // Customer profile — constructor sets createdAt/updatedAt automatically
            CustomerProfile cp = new CustomerProfile(user.getUserId(), (Long) null, null);
            customerProfileRepository.save(cp);
        }

        auditLogService.logAction(actorUserId, "USER_CREATE", "USER", String.valueOf(user.getUserId()),
                "Admin provisioned user " + email + " with role " + roleLookup, ipAddress);

        return mapToAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUser(Long targetUserId, UpdateUserStatusRequest request, Long actorUserId, String ipAddress) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        if (actorUserId != null && actorUserId.equals(targetUserId)) {
            if (request.getAccountStatus() != null && !request.getAccountStatus().trim().equalsIgnoreCase("ACTIVE")) {
                throw new BadRequestException("Administrators cannot suspend or deactivate their own account");
            }
        }

        String oldStatus = user.getAccountStatus();
        if (request.getAccountStatus() != null && !request.getAccountStatus().isBlank()) {
            user.setAccountStatus(request.getAccountStatus().trim().toUpperCase());
        }
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            userRoleRepository.deleteByIdUserId(targetUserId);
            for (String roleCode : request.getRoleCodes()) {
                Role role = roleRepository.findByRoleCode(roleCode.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleCode));
                userRoleRepository.save(new UserRole(targetUserId, role.getRoleId(), actorUserId));
            }
        }

        auditLogService.logAction(
                actorUserId,
                "USER_UPDATE",
                "USER",
                String.valueOf(targetUserId),
                "Status changed from " + oldStatus + " to " + user.getAccountStatus(),
                ipAddress
        );

        return mapToAdminUserResponse(user);
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        AdminUserResponse resp = new AdminUserResponse();
        resp.setUserId(user.getUserId());
        resp.setEmail(user.getEmail());
        resp.setFirstName(user.getFirstName());
        resp.setLastName(user.getLastName());
        resp.setPhone(user.getPhone());
        resp.setAccountStatus(user.getAccountStatus());
        resp.setLastLoginAt(user.getLastLoginAt());
        resp.setCreatedAt(user.getCreatedAt());

        List<UserRole> userRoles = userRoleRepository.findByIdUserId(user.getUserId());
        List<String> roles = new ArrayList<>();
        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getId().getRoleId()).ifPresent(r -> roles.add(r.getRoleCode()));
        }
        resp.setRoles(roles);
        return resp;
    }
}
