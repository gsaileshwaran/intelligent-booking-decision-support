package com.pvk.cinemas.user.dto;

import java.util.List;

public class UpdateUserStatusRequest {

    private String accountStatus; // ACTIVE, INACTIVE, SUSPENDED
    private List<String> roleCodes; // Optional replacement of roles

    public UpdateUserStatusRequest() {}

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
}
