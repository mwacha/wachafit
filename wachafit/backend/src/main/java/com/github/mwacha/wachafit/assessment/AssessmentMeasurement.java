package com.github.mwacha.wachafit.assessment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "assessment_measurements")
public class AssessmentMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private PhysicalAssessment assessment;

    @Column(name = "body_part", nullable = false, length = 40)
    private String bodyPart;

    @Column(name = "value_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal valueCm;

    public UUID getId() { return id; }
    public PhysicalAssessment getAssessment() { return assessment; }
    public void setAssessment(PhysicalAssessment assessment) { this.assessment = assessment; }
    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }
    public BigDecimal getValueCm() { return valueCm; }
    public void setValueCm(BigDecimal valueCm) { this.valueCm = valueCm; }
}
