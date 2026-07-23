package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_enrollments")
public class ClassEnrollment extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private GroupClass groupClass;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "enrolled_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime enrolledAt;

    public UUID getId() { return id; }
    public GroupClass getGroupClass() { return groupClass; }
    public void setGroupClass(GroupClass groupClass) { this.groupClass = groupClass; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getEnrolledAt() { return enrolledAt; }
}
