package com.pvk.cinemas.security.dto;

import java.util.List;

public class UpdateRolePermissionsRequest {

    private List<String> permissionCodes;

    public UpdateRolePermissionsRequest() {}

    public List<String> getPermissionCodes() { return permissionCodes; }
    public void setPermissionCodes(List<String> permissionCodes) { this.permissionCodes = permissionCodes; }
}
