package com.pvk.cinemas.security;

import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.security.model.Permission;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.RolePermission;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.PermissionRepository;
import com.pvk.cinemas.security.repository.RolePermissionRepository;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeTheatreRepository employeeTheatreRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserRoleRepository userRoleRepository,
                                    RoleRepository roleRepository) {
        this(userRepository, userRoleRepository, roleRepository, null, null, null, null);
    }

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository,
                                    UserRoleRepository userRoleRepository,
                                    RoleRepository roleRepository,
                                    RolePermissionRepository rolePermissionRepository,
                                    PermissionRepository permissionRepository,
                                    EmployeeProfileRepository employeeProfileRepository,
                                    EmployeeTheatreRepository employeeTheatreRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.employeeTheatreRepository = employeeTheatreRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<UserRole> userRoles = userRoleRepository.findByIdUserId(user.getUserId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<String> roleCodes = new ArrayList<>();
        List<String> permissions = new ArrayList<>();

        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getId().getRoleId()).ifPresent(r -> {
                String rawCode = r.getRoleCode();
                String cleanRole = rawCode.startsWith("ROLE_") ? rawCode.substring(5) : rawCode;
                roleCodes.add(cleanRole);
                String roleAuthority = "ROLE_" + cleanRole;
                authorities.add(new SimpleGrantedAuthority(roleAuthority));

                if (rolePermissionRepository != null && permissionRepository != null) {
                    List<RolePermission> rps = rolePermissionRepository.findByIdRoleId(r.getRoleId());
                    for (RolePermission rp : rps) {
                        permissionRepository.findById(rp.getId().getPermissionId()).ifPresent(p -> {
                            String pcode = p.getPermissionCode();
                            if (!permissions.contains(pcode)) {
                                permissions.add(pcode);
                                authorities.add(new SimpleGrantedAuthority(pcode));
                            }
                        });
                    }
                }
            });
        }

        String employeeCode = null;
        if (employeeProfileRepository != null) {
            employeeCode = employeeProfileRepository.findByUserId(user.getUserId())
                    .map(EmployeeProfile::getEmployeeCode)
                    .orElse(null);
        }

        List<Long> assignedTheatreIds = new ArrayList<>();
        if (employeeTheatreRepository != null) {
            assignedTheatreIds = employeeTheatreRepository.findByIdUserId(user.getUserId()).stream()
                    .filter(et -> "ACTIVE".equalsIgnoreCase(et.getStatus()))
                    .map(et -> et.getId().getTheatreId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getAccountStatus(),
                authorities,
                employeeCode,
                roleCodes,
                permissions,
                assignedTheatreIds
        );
    }
}
