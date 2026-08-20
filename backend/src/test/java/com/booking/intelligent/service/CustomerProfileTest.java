package com.booking.intelligent.service;

import com.booking.intelligent.dto.AuthRequest;
import com.booking.intelligent.dto.AuthResponse;
import com.booking.intelligent.dto.ChangePasswordRequestDto;
import com.booking.intelligent.dto.UpdateProfileRequestDto;
import com.booking.intelligent.dto.UserProfileDto;
import com.booking.intelligent.entity.Role;
import com.booking.intelligent.entity.User;
import com.booking.intelligent.enums.RoleName;
import com.booking.intelligent.repository.RoleRepository;
import com.booking.intelligent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CustomerProfileTest {

    @Autowired
    private CustomerProfileService customerProfileService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer1;
    private User customer2;
    private String rawPassword;

    @BeforeEach
    public void setup() {
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        rawPassword = "password123";

        customer1 = userRepository.save(User.builder()
                .email("profile_cust1_" + System.currentTimeMillis() + "@example.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name("Original Customer One")
                .role(customerRole)
                .build());

        customer2 = userRepository.save(User.builder()
                .email("profile_cust2_" + System.currentTimeMillis() + "@example.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name("Original Customer Two")
                .role(customerRole)
                .build());
    }

    @Test
    public void testCustomerCanRetrieveOwnProfile() {
        UserProfileDto profile = customerProfileService.getProfile(customer1.getUserId());
        assertNotNull(profile);
        assertEquals(customer1.getEmail(), profile.getEmail());
        assertEquals("Original Customer One", profile.getName());
        assertEquals("ROLE_CUSTOMER", profile.getRole());
    }

    @Test
    public void testCustomerCanUpdateOwnProfile() {
        UpdateProfileRequestDto updateReq = UpdateProfileRequestDto.builder()
                .name("Updated Customer Name")
                .build();

        UserProfileDto updatedProfile = customerProfileService.updateProfile(customer1.getUserId(), updateReq);
        assertNotNull(updatedProfile);
        assertEquals("Updated Customer Name", updatedProfile.getName());

        User reloadedUser = userRepository.findById(customer1.getUserId()).orElseThrow();
        assertEquals("Updated Customer Name", reloadedUser.getName());
    }

    @Test
    public void testCorrectCurrentPasswordAllowsPasswordChange() {
        ChangePasswordRequestDto changeReq = ChangePasswordRequestDto.builder()
                .currentPassword(rawPassword)
                .newPassword("newSecurePassword456")
                .confirmPassword("newSecurePassword456")
                .build();

        UserProfileDto result = customerProfileService.changePassword(customer1.getUserId(), changeReq);
        assertNotNull(result);

        User reloadedUser = userRepository.findById(customer1.getUserId()).orElseThrow();
        assertTrue(passwordEncoder.matches("newSecurePassword456", reloadedUser.getPasswordHash()));
        assertFalse(passwordEncoder.matches(rawPassword, reloadedUser.getPasswordHash()));
    }

    @Test
    public void testIncorrectCurrentPasswordIsRejected() {
        ChangePasswordRequestDto changeReq = ChangePasswordRequestDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newSecurePassword456")
                .confirmPassword("newSecurePassword456")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            customerProfileService.changePassword(customer1.getUserId(), changeReq);
        });
        assertTrue(ex.getMessage().contains("Current password is incorrect"));
    }

    @Test
    public void testNewPasswordCannotEqualCurrentPassword() {
        ChangePasswordRequestDto changeReq = ChangePasswordRequestDto.builder()
                .currentPassword(rawPassword)
                .newPassword(rawPassword)
                .confirmPassword(rawPassword)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            customerProfileService.changePassword(customer1.getUserId(), changeReq);
        });
        assertTrue(ex.getMessage().contains("cannot be the same as the current password"));
    }

    @Test
    public void testExistingLoginWorksAfterPasswordChange() {
        ChangePasswordRequestDto changeReq = ChangePasswordRequestDto.builder()
                .currentPassword(rawPassword)
                .newPassword("brandNewPassword99")
                .confirmPassword("brandNewPassword99")
                .build();

        customerProfileService.changePassword(customer1.getUserId(), changeReq);

        AuthRequest loginReq = new AuthRequest(customer1.getEmail(), "brandNewPassword99");
        AuthResponse response = authService.authenticateUser(loginReq);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(customer1.getUserId(), response.getUserId());
    }
}
