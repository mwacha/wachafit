package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
public class StudentProfile extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(nullable = false, unique = true, length = 14) private String cpf;
    @Column(length = 20) private String rg;
    @Column(name = "birth_date") private LocalDate birthDate;
    @Column(length = 10) private String gender;
    @Column(name = "marital_status", length = 20) private String maritalStatus;
    @Column(length = 100) private String profession;
    @Column(length = 20) private String phone;
    @Column(name = "address_zip", length = 9) private String addressZip;
    @Column(name = "address_line", length = 200) private String addressLine;
    @Column(name = "address_number", length = 10) private String addressNumber;
    @Column(name = "address_complement", length = 100) private String addressComplement;
    @Column(name = "address_neighborhood", length = 100) private String addressNeighborhood;
    @Column(name = "address_city", length = 100) private String addressCity;
    @Column(name = "address_state", length = 2) private String addressState;
    @Column(name = "emergency_contact_name", length = 120) private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 20) private String emergencyContactPhone;
    @Column(name = "emergency_contact_relationship", length = 50) private String emergencyContactRelationship;
    @Column(name = "profile_photo_key", length = 255) private String profilePhotoKey;
    @Column(name = "document_photo_key", length = 255) private String documentPhotoKey;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false) private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { this.cpf = v; }
    public String getRg() { return rg; }
    public void setRg(String v) { this.rg = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { this.birthDate = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String v) { this.maritalStatus = v; }
    public String getProfession() { return profession; }
    public void setProfession(String v) { this.profession = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getAddressZip() { return addressZip; }
    public void setAddressZip(String v) { this.addressZip = v; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String v) { this.addressLine = v; }
    public String getAddressNumber() { return addressNumber; }
    public void setAddressNumber(String v) { this.addressNumber = v; }
    public String getAddressComplement() { return addressComplement; }
    public void setAddressComplement(String v) { this.addressComplement = v; }
    public String getAddressNeighborhood() { return addressNeighborhood; }
    public void setAddressNeighborhood(String v) { this.addressNeighborhood = v; }
    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String v) { this.addressCity = v; }
    public String getAddressState() { return addressState; }
    public void setAddressState(String v) { this.addressState = v; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String v) { this.emergencyContactName = v; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String v) { this.emergencyContactPhone = v; }
    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String v) { this.emergencyContactRelationship = v; }
    public Instant getCreatedAt() { return createdAt; }
}
