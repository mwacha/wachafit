ALTER TABLE student_profiles
    ADD COLUMN IF NOT EXISTS rg                              VARCHAR(20),
    ADD COLUMN IF NOT EXISTS gender                          VARCHAR(10),
    ADD COLUMN IF NOT EXISTS marital_status                  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS profession                      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_number                  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS address_complement              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address_neighborhood            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_relationship  VARCHAR(50);
