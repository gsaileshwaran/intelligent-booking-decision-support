package com.booking.intelligent.service;

import com.booking.intelligent.dto.ChangePasswordRequestDto;
import com.booking.intelligent.dto.UpdateProfileRequestDto;
import com.booking.intelligent.dto.UserProfileDto;
import com.booking.intelligent.entity.Booking;
import com.booking.intelligent.entity.User;
import com.booking.intelligent.enums.BookingStatus;
import com.booking.intelligent.exception.ResourceNotFoundException;
import com.booking.intelligent.repository.BookingRepository;
import com.booking.intelligent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Booking> userBookings = bookingRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        long totalBookings = userBookings.size();
        long activeHolds = userBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.HELD)
                .count();

        return UserProfileDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getRoleName().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .totalBookings(totalBookings)
                .activeHolds(activeHolds)
                .build();
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        user.setName(request.getName().trim());
        userRepository.save(user);

        return getProfile(userId);
    }

    @Transactional
    public UserProfileDto changePassword(Long userId, ChangePasswordRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            throw new IllegalArgumentException("Current password is required");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return getProfile(userId);
    }
}
