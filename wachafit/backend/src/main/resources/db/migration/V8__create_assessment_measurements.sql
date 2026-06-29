CREATE TABLE assessment_measurements (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES physical_assessments(id),
    body_part     VARCHAR(40) NOT NULL,
    value_cm      NUMERIC(5,2) NOT NULL
);
