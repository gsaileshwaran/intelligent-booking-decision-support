package com.booking.intelligent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private Long totalBookings;
    private Long activeHolds;
}
