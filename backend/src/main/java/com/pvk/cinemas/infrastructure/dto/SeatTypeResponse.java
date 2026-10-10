package com.pvk.cinemas.infrastructure.dto;

public class SeatTypeResponse {

    private Long seatTypeId;
    private String typeCode;
    private String name;
    private String description;

    public SeatTypeResponse() {}

    public SeatTypeResponse(Long seatTypeId, String typeCode, String name, String description) {
        this.seatTypeId = seatTypeId;
        this.typeCode = typeCode;
        this.name = name;
        this.description = description;
    }

    public Long getSeatTypeId() {
        return seatTypeId;
    }

    public void setSeatTypeId(Long seatTypeId) {
        this.seatTypeId = seatTypeId;
    }

    public void setSeatTypeId(Integer seatTypeId) {
        this.seatTypeId = seatTypeId != null ? seatTypeId.longValue() : null;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
