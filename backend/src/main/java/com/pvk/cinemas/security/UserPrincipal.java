package com.pvk.cinemas.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final String accountStatus;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String employeeCode;
    private final List<String> roles;
    private final List<String> permissions;
    private final List<Long> assignedTheatreIds;

    public UserPrincipal(Long userId, String email, String password, String accountStatus, Collection<? extends GrantedAuthority> authorities) {
        this(userId, email, password, accountStatus, authorities, null, extractRoles(authorities), extractPermissions(authorities), List.of());
    }

    public UserPrincipal(Long userId, String email, String password, String accountStatus,
                         Collection<? extends GrantedAuthority> authorities,
                         String employeeCode,
                         List<String> roles,
                         List<String> permissions,
                         List<Long> assignedTheatreIds) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.accountStatus = accountStatus;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.employeeCode = employeeCode;
        this.roles = roles != null ? List.copyOf(roles) : extractRoles(authorities);
        this.permissions = permissions != null ? List.copyOf(permissions) : extractPermissions(authorities);
        this.assignedTheatreIds = assignedTheatreIds != null ? List.copyOf(assignedTheatreIds) : Collections.emptyList();
    }

    private static List<String> extractRoles(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return Collections.emptyList();
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority ga : authorities) {
            String auth = ga.getAuthority();
            if (auth.startsWith("ROLE_")) {
                roles.add(auth.substring(5));
            }
        }
        return Collections.unmodifiableList(roles);
    }

    private static List<String> extractPermissions(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return Collections.emptyList();
        List<String> perms = new ArrayList<>();
        for (GrantedAuthority ga : authorities) {
            String auth = ga.getAuthority();
            if (!auth.startsWith("ROLE_")) {
                perms.add(auth);
            }
        }
        return Collections.unmodifiableList(perms);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<Long> getAssignedTheatreIds() {
        return assignedTheatreIds;
    }

    public boolean hasRole(String role) {
        if (role == null) return false;
        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase(cleanRole));
    }

    public boolean hasPermission(String permission) {
        if (permission == null) return false;
        if (hasRole("SUPER_ADMIN")) return true;
        return permissions.stream().anyMatch(p -> p.equalsIgnoreCase(permission));
    }

    public boolean hasTheatreScope(Long theatreId) {
        if (theatreId == null) return false;
        if (hasRole("SUPER_ADMIN")) return true;
        return assignedTheatreIds.contains(theatreId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equalsIgnoreCase(accountStatus);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(accountStatus);
    }
}
