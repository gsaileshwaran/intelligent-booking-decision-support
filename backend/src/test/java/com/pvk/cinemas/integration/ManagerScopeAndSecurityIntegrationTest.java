package com.pvk.cinemas.integration;

import com.pvk.cinemas.organization.model.City;
import com.pvk.cinemas.organization.model.EmployeeTheatre;
import com.pvk.cinemas.organization.model.Theatre;
import com.pvk.cinemas.organization.repository.CityRepository;
import com.pvk.cinemas.organization.repository.EmployeeTheatreRepository;
import com.pvk.cinemas.organization.repository.TheatreRepository;
import com.pvk.cinemas.security.TheatreScopeService;
import com.pvk.cinemas.security.UserPrincipal;
import com.pvk.cinemas.security.model.Role;
import com.pvk.cinemas.security.model.UserRole;
import com.pvk.cinemas.security.repository.RoleRepository;
import com.pvk.cinemas.security.repository.UserRoleRepository;
import com.pvk.cinemas.user.model.EmployeeProfile;
import com.pvk.cinemas.user.model.User;
import com.pvk.cinemas.user.repository.EmployeeProfileRepository;
import com.pvk.cinemas.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ManagerScopeAndSecurityIntegrationTest {

    @Autowired private TheatreScopeService theatreScopeService;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeProfileRepository employeeProfileRepository;
    @Autowired private EmployeeTheatreRepository employeeTheatreRepository;
    @Autowired private TheatreRepository theatreRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    private User managerA;
    private User managerB;
    private User superAdmin;
    private User customer;
    private Theatre theatreA;
    private Theatre theatreB;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        City city = cityRepository.findAll().get(0);

        Role managerRole = roleRepository.findByRoleCode("ROLE_THEATRE_MANAGER").orElseThrow();
        Role superAdminRole = roleRepository.findByRoleCode("ROLE_SUPER_ADMIN").orElseThrow();
        Role customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER").orElseThrow();

        // 1. Create Theatres A & B in MySQL
        theatreA = new Theatre();
        theatreA.setCityId(city.getCityId());
        theatreA.setTheatreCode("TH-A-" + uid);
        theatreA.setTheatreName("Multiplex Alpha " + uid);
        theatreA.setAddressLine1("123 Alpha St");
        theatreA.setStatus("ACTIVE");
        theatreA = theatreRepository.save(theatreA);

        theatreB = new Theatre();
        theatreB.setCityId(city.getCityId());
        theatreB.setTheatreCode("TH-B-" + uid);
        theatreB.setTheatreName("Multiplex Beta " + uid);
        theatreB.setAddressLine1("456 Beta Ave");
        theatreB.setStatus("ACTIVE");
        theatreB = theatreRepository.save(theatreB);

        // 2. Create Manager A -> assign Theatre A
        managerA = userRepository.save(new User("Manager", "A", "mgr_a_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), "hash", "ACTIVE"));
        employeeProfileRepository.save(new EmployeeProfile(managerA.getUserId(), "EMP-A-" + uid));
        userRoleRepository.save(new UserRole(managerA.getUserId(), managerRole.getRoleId()));
        employeeTheatreRepository.save(new EmployeeTheatre(managerA.getUserId(), theatreA.getTheatreId()));

        // 3. Create Manager B -> assign Theatre B
        managerB = userRepository.save(new User("Manager", "B", "mgr_b_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), "hash", "ACTIVE"));
        employeeProfileRepository.save(new EmployeeProfile(managerB.getUserId(), "EMP-B-" + uid));
        userRoleRepository.save(new UserRole(managerB.getUserId(), managerRole.getRoleId()));
        employeeTheatreRepository.save(new EmployeeTheatre(managerB.getUserId(), theatreB.getTheatreId()));

        // 4. Create Super Admin
        superAdmin = userRepository.save(new User("Admin", "Super", "admin_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), "hash", "ACTIVE"));
        userRoleRepository.save(new UserRole(superAdmin.getUserId(), superAdminRole.getRoleId()));

        // 5. Create Customer
        customer = userRepository.save(new User("Cust", "User", "cust_" + uid + "@example.com",
                "+919" + (int)(Math.random() * 90000000 + 10000000), "hash", "ACTIVE"));
        userRoleRepository.save(new UserRole(customer.getUserId(), customerRole.getRoleId()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(User user, String role) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        UserPrincipal principal = new UserPrincipal(user.getUserId(), user.getEmail(), "", "ACTIVE", authorities);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Section 19: Manager A -> Theatre A is ALLOWED, Theatre B is REJECTED")
    void testManagerAScope() {
        authenticateUser(managerA, "ROLE_THEATRE_MANAGER");

        assertTrue(theatreScopeService.hasAccessToTheatre(theatreA.getTheatreId()),
                "Manager A must have access to assigned Theatre A");
        assertFalse(theatreScopeService.hasAccessToTheatre(theatreB.getTheatreId()),
                "Manager A must NOT have access to unassigned Theatre B");
    }

    @Test
    @DisplayName("Section 19: Manager B -> Theatre B is ALLOWED, Theatre A is REJECTED")
    void testManagerBScope() {
        authenticateUser(managerB, "ROLE_THEATRE_MANAGER");

        assertTrue(theatreScopeService.hasAccessToTheatre(theatreB.getTheatreId()),
                "Manager B must have access to assigned Theatre B");
        assertFalse(theatreScopeService.hasAccessToTheatre(theatreA.getTheatreId()),
                "Manager B must NOT have access to unassigned Theatre A");
    }

    @Test
    @DisplayName("Super Admin has platform-wide access to all multiplexes")
    void testSuperAdminScope() {
        authenticateUser(superAdmin, "ROLE_SUPER_ADMIN");

        assertTrue(theatreScopeService.hasAccessToTheatre(theatreA.getTheatreId()),
                "Super Admin must have platform-wide access to Theatre A");
        assertTrue(theatreScopeService.hasAccessToTheatre(theatreB.getTheatreId()),
                "Super Admin must have platform-wide access to Theatre B");
    }

    @Test
    @DisplayName("Customer has 0 manager scope access to multiplexes")
    void testCustomerScope() {
        authenticateUser(customer, "ROLE_CUSTOMER");

        assertFalse(theatreScopeService.hasAccessToTheatre(theatreA.getTheatreId()),
                "Customer must have zero manager scope access to Theatre A");
        assertFalse(theatreScopeService.hasAccessToTheatre(theatreB.getTheatreId()),
                "Customer must have zero manager scope access to Theatre B");
    }
}
