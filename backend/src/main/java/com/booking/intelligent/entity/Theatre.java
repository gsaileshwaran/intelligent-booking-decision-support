package com.booking.intelligent.entity;

import com.booking.intelligent.enums.TheatreStatus;
import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "theatre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theatre_id")
    private Long theatreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "location", length = 150, nullable = false)
    private String location;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "locality", length = 150)
    private String locality;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "operating_hours", length = 100)
    private String operatingHours;

    @Column(name = "amenities", length = 500)
    private String amenities;

    @Column(name = "formats", length = 200)
    private String formats;

    @Column(name = "total_screens")
    private Integer totalScreens;

    @Column(name = "total_capacity")
    private Integer totalCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TheatreStatus status;

    public String getCity() {
        if (city != null && !city.trim().isEmpty()) {
            return city;
        }
        return location;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = TheatreStatus.ACTIVE;
        }
        if (city == null && location != null) {
            city = location;
        }
    }
}
