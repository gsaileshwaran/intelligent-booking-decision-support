package com.pvk.cinemas.availability.dto;

import java.util.List;

public class ShowSeatAvailabilityResponse {
    private Long showId;
    private Long theatreId;
    private String theatreName;
    private Long screenId;
    private String screenName;
    private int totalSeats;
    private int availableSeats;
    private int bookedSeats;
    private int blockedSeats;
    private List<SeatAvailabilityDetail> seats;

    public ShowSeatAvailabilityResponse() {}

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? theatreId.longValue() : null; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? screenId.longValue() : null; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public int getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }

    public int getBlockedSeats() { return blockedSeats; }
    public void setBlockedSeats(int blockedSeats) { this.blockedSeats = blockedSeats; }

    public List<SeatAvailabilityDetail> getSeats() { return seats; }
    public void setSeats(List<SeatAvailabilityDetail> seats) { this.seats = seats; }

    public static class SeatAvailabilityDetail {
        private Long seatId;
        private String rowLabel;
        private String seatNumber;
        private String seatType;
        private String availabilityStatus;
        private String pricingZone;
        private java.math.BigDecimal price;
        private Integer gridRowIndex;
        private Integer gridColIndex;
        private Boolean aisleAfter;

        public SeatAvailabilityDetail() {}

        public SeatAvailabilityDetail(Long seatId, String rowLabel, String seatNumber, String seatType, String availabilityStatus) {
            this.seatId = seatId;
            this.rowLabel = rowLabel;
            this.seatNumber = seatNumber;
            this.seatType = seatType;
            this.availabilityStatus = availabilityStatus;
            this.pricingZone = "STANDARD";
            this.aisleAfter = false;
        }

        public SeatAvailabilityDetail(Long seatId, String rowLabel, String seatNumber, String seatType,
                                      String availabilityStatus, String pricingZone, java.math.BigDecimal price,
                                      Integer gridRowIndex, Integer gridColIndex, Boolean aisleAfter) {
            this.seatId = seatId;
            this.rowLabel = rowLabel;
            this.seatNumber = seatNumber;
            this.seatType = seatType;
            this.availabilityStatus = availabilityStatus;
            this.pricingZone = pricingZone != null ? pricingZone : "STANDARD";
            this.price = price;
            this.gridRowIndex = gridRowIndex;
            this.gridColIndex = gridColIndex;
            this.aisleAfter = aisleAfter != null && aisleAfter;
        }

        public Long getSeatId() { return seatId; }
        public void setSeatId(Long seatId) { this.seatId = seatId; }

        public String getRowLabel() { return rowLabel; }
        public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

        public String getSeatType() { return seatType; }
        public void setSeatType(String seatType) { this.seatType = seatType; }

        public String getAvailabilityStatus() { return availabilityStatus; }
        public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

        public String getPricingZone() { return pricingZone; }
        public void setPricingZone(String pricingZone) { this.pricingZone = pricingZone; }

        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }

        public Integer getGridRowIndex() { return gridRowIndex; }
        public void setGridRowIndex(Integer gridRowIndex) { this.gridRowIndex = gridRowIndex; }

        public Integer getGridColIndex() { return gridColIndex; }
        public void setGridColIndex(Integer gridColIndex) { this.gridColIndex = gridColIndex; }

        public Boolean getAisleAfter() { return aisleAfter; }
        public void setAisleAfter(Boolean aisleAfter) { this.aisleAfter = aisleAfter; }
    }
}
