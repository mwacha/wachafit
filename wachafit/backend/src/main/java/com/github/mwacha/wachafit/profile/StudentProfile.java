package com.github.mwacha.wachafit.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(nullable = false, unique = true, length = 14) private String cpf;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(length = 20) private String phone;
    @Column(name = "address_line", length = 200) private String addressLine;
    @Column(name = "address_city", length = 100) private String addressCity;
    @Column(name = "address_state", length = 2) private String addressState;
    @Column(name = "address_zip", length = 9) private String addressZip;
    @Column(name = "emergency_contact_name", length = 120) private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 20) private String emergencyContactPhone;
    @Column(name = "profile_photo_key", length = 255) private String profilePhotoKey;
    @Column(name = "document_photo_key", length = 255) private String documentPhotoKey;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false) private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { this.cpf = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { this.birthDate = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String v) { this.addressLine = v; }
    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String v) { this.addressCity = v; }
    public String getAddressState() { return addressState; }
    public void setAddressState(String v) { this.addressState = v; }
    public String getAddressZip() { return addressZip; }
    public void setAddressZip(String v) { this.addressZip = v; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String v) { this.emergencyContactName = v; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String v) { this.emergencyContactPhone = v; }
    public Instant getCreatedAt() { return createdAt; }
}
