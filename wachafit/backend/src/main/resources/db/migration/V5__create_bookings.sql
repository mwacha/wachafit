CREATE TABLE bookings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES schedules(id),
    student_id  UUID NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    booked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (schedule_id, student_id)
);

CREATE INDEX idx_bookings_student_status ON bookings(student_id, status);
