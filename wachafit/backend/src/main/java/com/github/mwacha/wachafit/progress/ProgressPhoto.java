package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "progress_photos")
public class ProgressPhoto extends TenantAwareEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "uploaded_by", nullable = false) private UUID uploadedBy;
    @Column(name = "storage_key", nullable = false, length = 255) private String storageKey;
    @Column(name = "taken_at", nullable = false) private LocalDate takenAt;
    @Column(length = 200) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP") private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID v) { this.uploadedBy = v; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String v) { this.storageKey = v; }
    public LocalDate getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDate v) { this.takenAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public Instant getCreatedAt() { return createdAt; }
}
