package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trainer_profiles")
public class TrainerProfile extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(length = 20) private String cref;
    private String specialties;
    private String bio;
    @Column(name = "profile_photo_key", length = 255) private String profilePhotoKey;
    @Column(name = "contract_type", length = 20) private String contractType;
    @Column(name = "admission_date") private LocalDate admissionDate;
    @Column(name = "commission_type", length = 20) private String commissionType;
    @Column(name = "commission_value", precision = 8, scale = 2) private BigDecimal commissionValue;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP") private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getCref() { return cref; }
    public void setCref(String v) { this.cref = v; }
    public String getSpecialties() { return specialties; }
    public void setSpecialties(String v) { this.specialties = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public String getContractType() { return contractType; }
    public void setContractType(String v) { this.contractType = v; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate v) { this.admissionDate = v; }
    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String v) { this.commissionType = v; }
    public BigDecimal getCommissionValue() { return commissionValue; }
    public void setCommissionValue(BigDecimal v) { this.commissionValue = v; }
    public Instant getCreatedAt() { return createdAt; }
}
