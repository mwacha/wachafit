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
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS tenant_id UUID', tbl);
        EXECUTE format('UPDATE %I SET tenant_id = $1 WHERE tenant_id IS NULL', tbl)
            USING default_tenant;
        EXECUTE format('ALTER TABLE %I ALTER COLUMN tenant_id SET NOT NULL', tbl);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_tenant ON %I(tenant_id)', tbl, tbl);
    END LOOP;
END $$;
