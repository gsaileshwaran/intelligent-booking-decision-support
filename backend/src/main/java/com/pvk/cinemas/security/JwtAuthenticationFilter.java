package com.pvk.cinemas.security;

import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenRevocationStore revocationStore;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    @Autowired(required = false)
    private CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenRevocationStore revocationStore) {
        this(tokenProvider, revocationStore, null, null);
    }

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   TokenRevocationStore revocationStore,
                                   UserRepository userRepository,
                                   EmployeeProfileRepository employeeProfileRepository) {
        this.tokenProvider = tokenProvider;
        this.revocationStore = revocationStore;
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
    }

    public void setCustomUserDetailsService(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            String jti = tokenProvider.getJti(token);
            if (!revocationStore.isRevoked(jti)) {
                Claims claims = tokenProvider.getClaims(token);
                Long userId = Long.parseLong(claims.getSubject());
                String email = claims.get("email", String.class);

                UserPrincipal principal;
                Collection<? extends GrantedAuthority> authorities;

                if (customUserDetailsService != null && email != null) {
                    try {
                        principal = (UserPrincipal) customUserDetailsService.loadUserByUsername(email);
                    } catch (Exception ex) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (!"ACTIVE".equalsIgnoreCase(principal.getAccountStatus())) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    authorities = principal.getAuthorities();
                } else {
                    String accountStatus = "ACTIVE";
                    String employeeCode = null;

                    if (userRepository != null) {
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty() || !"ACTIVE".equalsIgnoreCase(userOpt.get().getAccountStatus())) {
                            SecurityContextHolder.clearContext();
                            filterChain.doFilter(request, response);
                            return;
                        }
                        accountStatus = userOpt.get().getAccountStatus();
                    }

                    if (employeeProfileRepository != null) {
                        employeeCode = employeeProfileRepository.findByUserId(userId)
                                .map(EmployeeProfile::getEmployeeCode)
                                .orElse(null);
                    }

                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles");
                    List<String> permissions = tokenProvider.getPermissions(token);
                    List<Long> theatreIds = tokenProvider.getAssignedTheatreIds(token);

                    List<SimpleGrantedAuthority> authList = new ArrayList<>();
                    if (roles != null) {
                        for (String r : roles) {
                            String clean = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                            authList.add(new SimpleGrantedAuthority(clean));
                        }
                    }
                    if (permissions != null) {
                        for (String p : permissions) {
                            authList.add(new SimpleGrantedAuthority(p));
                        }
                    }
                    authorities = authList;

                    principal = new UserPrincipal(
                            userId,
                            email,
                            "",
                            accountStatus,
                            authorities,
                            employeeCode,
                            roles != null ? roles : Collections.emptyList(),
                            permissions != null ? permissions : Collections.emptyList(),
                            theatreIds != null ? theatreIds : Collections.emptyList()
                    );
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, token, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("pvk_token".equalsIgnoreCase(cookie.getName())
                        || "jwt".equalsIgnoreCase(cookie.getName())
                        || "token".equalsIgnoreCase(cookie.getName())) {
                    if (StringUtils.hasText(cookie.getValue())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }
}
