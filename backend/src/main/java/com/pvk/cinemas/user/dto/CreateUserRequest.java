package com.pvk.cinemas.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for Super Admin provisioning a new user (customer or theatre manager).
 */
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;

    /**
     * Role code to assign: ROLE_CUSTOMER or ROLE_THEATRE_MANAGER.
     * Defaults to ROLE_CUSTOMER if not provided.
     */
    private String roleCode;

    /**
     * Theatre ID to assign when provisioning a ROLE_THEATRE_MANAGER.
     * Optional — if provided the manager will be associated with this theatre.
     */
    private Integer theatreId;

    public CreateUserRequest() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public Integer getTheatreId() { return theatreId; }
    public void setTheatreId(Integer theatreId) { this.theatreId = theatreId; }
}
