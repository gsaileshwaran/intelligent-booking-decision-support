package com.pvk.cinemas.infrastructure.model;

import jakarta.persistence.*;

@Entity
@Table(name = "seat_type")
public class SeatType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_type_id")
    private Long seatTypeId;

    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public SeatType() {}

    public SeatType(Long seatTypeId, String typeCode, String name, String description) {
        this.seatTypeId = seatTypeId;
        this.typeCode = typeCode;
        this.name = name;
        this.description = description;
    }

    public SeatType(Integer seatTypeId, String typeCode, String name, String description) {
        this.seatTypeId = seatTypeId != null ? Long.valueOf(seatTypeId) : null;
        this.typeCode = typeCode;
        this.name = name;
        this.description = description;
    }

    public Long getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(Long seatTypeId) { this.seatTypeId = seatTypeId; }
    public void setSeatTypeId(Integer seatTypeId) { this.seatTypeId = seatTypeId != null ? Long.valueOf(seatTypeId) : null; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public String getSeatTypeCode() { return typeCode; }
    public void setSeatTypeCode(String seatTypeCode) { this.typeCode = seatTypeCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSeatTypeName() { return name; }
    public void setSeatTypeName(String seatTypeName) { this.name = seatTypeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
