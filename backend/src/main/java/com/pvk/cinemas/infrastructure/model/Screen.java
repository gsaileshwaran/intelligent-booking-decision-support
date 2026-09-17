package com.pvk.cinemas.infrastructure.model;

import jakarta.persistence.*;

@Entity
@Table(name = "screen", uniqueConstraints = {
    @UniqueConstraint(name = "uq_theatre_screen_code", columnNames = {"theatre_id", "screen_code"})
})
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screen_id")
    private Long screenId;

    @Column(name = "theatre_id", nullable = false)
    private Long theatreId;

    @Column(name = "screen_code", nullable = false, length = 50)
    private String screenCode;

    @Column(name = "screen_name", nullable = false, length = 100)
    private String screenName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public Screen() {}

    public Screen(Long theatreId, String screenCode, String screenName) {
        this.theatreId = theatreId;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.status = "ACTIVE";
    }

    public Screen(Integer theatreId, String screenCode, String screenName) {
        this.theatreId = theatreId != null ? Long.valueOf(theatreId) : null;
        this.screenCode = screenCode;
        this.screenName = screenName;
        this.status = "ACTIVE";
    }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? Long.valueOf(screenId) : null; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? Long.valueOf(theatreId) : null; }

    public String getScreenCode() { return screenCode; }
    public void setScreenCode(String screenCode) { this.screenCode = screenCode; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return "ACTIVE".equalsIgnoreCase(this.status); }
    public void setIsActive(Boolean active) { this.status = (active != null && active) ? "ACTIVE" : "INACTIVE"; }
}
