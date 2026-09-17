package com.pvk.cinemas.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Integer preferredLanguageId;
    private String preferredLanguageName;
    private LocalDate dateOfBirth;
    private String employeeCode;

    public ProfileResponse() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getPreferredLanguageId() { return preferredLanguageId; }
    public void setPreferredLanguageId(Integer preferredLanguageId) { this.preferredLanguageId = preferredLanguageId; }

    public String getPreferredLanguageName() { return preferredLanguageName; }
    public void setPreferredLanguageName(String preferredLanguageName) { this.preferredLanguageName = preferredLanguageName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
}
