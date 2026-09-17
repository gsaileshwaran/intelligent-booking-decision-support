package com.pvk.cinemas.scheduling.dto;

import java.time.Instant;

public class ShowResponse {
    private Long showId;
    private Long movieId;
    private String movieTitle;
    private Long movieLanguageId;
    private String languageName;
    private Long screenId;
    private String screenName;
    private Long theatreId;
    private String theatreName;
    private Long cityId;
    private String cityName;
    private Long screenCapabilityId;
    private Instant startAt;
    private Instant endAt;
    private String showStatus;
    private String presentationFormat;
    private String formatCode;
    private Integer availableSeats;
    private Integer totalSeats;
    private java.math.BigDecimal minPrice;
    private String posterUrl;

    public ShowResponse() {}

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public Long getMovieLanguageId() { return movieLanguageId; }
    public void setMovieLanguageId(Long movieLanguageId) { this.movieLanguageId = movieLanguageId; }

    public String getLanguageName() { return languageName; }
    public void setLanguageName(String languageName) { this.languageName = languageName; }

    public Long getScreenId() { return screenId; }
    public void setScreenId(Long screenId) { this.screenId = screenId; }
    public void setScreenId(Integer screenId) { this.screenId = screenId != null ? screenId.longValue() : null; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public Long getTheatreId() { return theatreId; }
    public void setTheatreId(Long theatreId) { this.theatreId = theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId != null ? theatreId.longValue() : null; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId != null ? cityId.longValue() : null; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public Long getScreenCapabilityId() { return screenCapabilityId; }
    public void setScreenCapabilityId(Long screenCapabilityId) { this.screenCapabilityId = screenCapabilityId; }
    public void setScreenCapabilityId(Integer screenCapabilityId) { this.screenCapabilityId = screenCapabilityId != null ? screenCapabilityId.longValue() : null; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public String getShowStatus() { return showStatus; }
    public void setShowStatus(String showStatus) { this.showStatus = showStatus; }

    public String getPresentationFormat() { return presentationFormat; }
    public void setPresentationFormat(String presentationFormat) { this.presentationFormat = presentationFormat; }

    public String getFormatCode() { return formatCode; }
    public void setFormatCode(String formatCode) { this.formatCode = formatCode; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public java.math.BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(java.math.BigDecimal minPrice) { this.minPrice = minPrice; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
}
