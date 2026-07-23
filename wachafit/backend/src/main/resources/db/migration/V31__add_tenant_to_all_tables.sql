DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    tables TEXT[] := ARRAY[
        'group_classes', 'class_enrollments',
        'schedules', 'bookings',
        'physical_assessments', 'assessment_measurements',
        'exercises',
        'workout_plans', 'workout_plan_items', 'workout_logs', 'personal_records',
        'progress_photos',
        'student_goals',
        'student_profiles', 'student_health',
        'trainer_profiles',
        'trainer_availability',
        'membership_plans', 'member_subscriptions', 'payment_charges'
    ];
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id)', tbl);
        EXECUTE format('UPDATE %I SET tenant_id = $1 WHERE tenant_id IS NULL', tbl)
            USING default_tenant;
        EXECUTE format('ALTER TABLE %I ALTER COLUMN tenant_id SET NOT NULL', tbl);
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I(tenant_id)', 'idx_' || tbl || '_tenant', tbl);
    END LOOP;
END $$;
