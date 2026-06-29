CREATE TABLE student_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id),
    cpf                     VARCHAR(14) NOT NULL UNIQUE,
    birth_date              DATE,
    phone                   VARCHAR(20),
    address_line            VARCHAR(200),
    address_city            VARCHAR(100),
    address_state           CHAR(2),
    address_zip             VARCHAR(9),
    emergency_contact_name  VARCHAR(120),
    emergency_contact_phone VARCHAR(20),
    profile_photo_key       VARCHAR(255),
    document_photo_key      VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
