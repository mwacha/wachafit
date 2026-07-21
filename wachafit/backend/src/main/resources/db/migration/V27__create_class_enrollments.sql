CREATE TABLE class_enrollments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id    UUID        NOT NULL REFERENCES group_classes(id),
    student_id  UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (class_id, student_id)
);
