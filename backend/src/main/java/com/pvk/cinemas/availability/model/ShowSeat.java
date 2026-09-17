package com.pvk.cinemas.availability.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "show_seat")
public class ShowSeat {

    @EmbeddedId
    private ShowSeatId id;

    @Column(name = "availability_status", nullable = false, length = 20)
    private String availabilityStatus = "AVAILABLE";

    public ShowSeat() {}

    public ShowSeat(Long showId, Long seatId, String availabilityStatus) {
        this.id = new ShowSeatId(showId, seatId);
        this.availabilityStatus = availabilityStatus;
    }

    public ShowSeatId getId() { return id; }
    public void setId(ShowSeatId id) { this.id = id; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public Instant getUpdatedAt() { return Instant.now(); }
    public void setUpdatedAt(Instant updatedAt) { /* no-op: column does not exist in schema */ }

    @Embeddable
    public static class ShowSeatId implements Serializable {
        @Column(name = "show_id")
        private Long showId;

        @Column(name = "seat_id")
        private Long seatId;

        public ShowSeatId() {}
        public ShowSeatId(Long showId, Long seatId) {
            this.showId = showId;
            this.seatId = seatId;
        }

        public Long getShowId() { return showId; }
        public void setShowId(Long showId) { this.showId = showId; }

        public Long getSeatId() { return seatId; }
        public void setSeatId(Long seatId) { this.seatId = seatId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ShowSeatId that)) return false;
            return Objects.equals(showId, that.showId) && Objects.equals(seatId, that.seatId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(showId, seatId);
        }
    }
}
