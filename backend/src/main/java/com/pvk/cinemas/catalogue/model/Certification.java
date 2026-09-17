package com.pvk.cinemas.catalogue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "certification")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certification_id")
    private Long certificationId;

    @Column(name = "certification_code", nullable = false, unique = true, length = 20)
    private String certificationCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public Certification() {}

    public Certification(Long certificationId, String certificationCode, String name, String description) {
        this.certificationId = certificationId;
        this.certificationCode = certificationCode;
        this.name = name;
        this.description = description;
    }

    public Long getCertificationId() { return certificationId; }
    public void setCertificationId(Long certificationId) { this.certificationId = certificationId; }
    public void setCertificationId(Integer certificationId) { this.certificationId = certificationId != null ? Long.valueOf(certificationId) : null; }

    public String getCertificationCode() { return certificationCode; }
    public void setCertificationCode(String certificationCode) { this.certificationCode = certificationCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
