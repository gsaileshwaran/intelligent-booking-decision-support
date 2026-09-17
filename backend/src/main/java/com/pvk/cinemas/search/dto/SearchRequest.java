package com.pvk.cinemas.search.dto;

public class SearchRequest {
    private String query;
    private Integer cityId;
    private Integer limit = 10;

    public SearchRequest() {}

    public SearchRequest(String query, Integer cityId, Integer limit) {
        this.query = query;
        this.cityId = cityId;
        this.limit = limit != null ? limit : 10;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
