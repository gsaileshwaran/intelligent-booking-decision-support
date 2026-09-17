package com.pvk.cinemas.user.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee_profile")
public class EmployeeProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate = LocalDate.now();

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Transient
    private String designation;

    @Transient
    private String department;

    @Transient
    private String emergencyContactPhone;

    public EmployeeProfile() {}

    public EmployeeProfile(Long userId, String employeeCode) {
        this.userId = userId;
        this.employeeCode = employeeCode;
        this.joiningDate = LocalDate.now();
        this.status = "ACTIVE";
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public LocalDate getDateOfJoining() { return joiningDate; }
    public void setDateOfJoining(LocalDate dateOfJoining) { this.joiningDate = dateOfJoining; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
}
