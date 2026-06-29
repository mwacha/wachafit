# WachaFit Etapa 3A — Migrations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create Flyway migrations V7–V15 for all Etapa 3 tables and verify all 62 existing tests still pass.

**Architecture:** Nine SQL migration files added sequentially. Testcontainers applies all migrations on a fresh Postgres 16 container during `mvn test`.

**Tech Stack:** PostgreSQL 16, Flyway, Spring Boot 3, Testcontainers, JUnit 5

## Global Constraints

- Migration files: `backend/src/main/resources/db/migration/V{N}__{table_name}.sql`
- All UUIDs: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- All timestamps: `TIMESTAMPTZ NOT NULL DEFAULT now()`
- Foreign keys reference `users(id)` for any trainer/student/assessed_by/uploaded_by column
- Worktree root: `/Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/`
- Git: `git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit`
- Maven: `cd backend && mvn ...`

---

## Task 1: V7 + V8 — physical_assessments + assessment_measurements (T-14)

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__create_physical_assessments.sql`
- Create: `backend/src/main/resources/db/migration/V8__create_assessment_measurements.sql`

- [ ] **Step 1: Create V7__create_physical_assessments.sql**

```sql
CREATE TABLE physical_assessments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES users(id),
    assessed_by  UUID NOT NULL REFERENCES users(id),
    assessed_at  DATE NOT NULL,
    weight_kg    NUMERIC(5,2),
    height_cm    NUMERIC(5,2),
    body_fat_pct NUMERIC(4,1),
    bmi          NUMERIC(4,1),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_assessments_student_date ON physical_assessments(student_id, assessed_at);
```

- [ ] **Step 2: Create V8__create_assessment_measurements.sql**

```sql
CREATE TABLE assessment_measurements (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES physical_assessments(id),
    body_part     VARCHAR(40) NOT NULL,
    value_cm      NUMERIC(5,2) NOT NULL
);
```

- [ ] **Step 3: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/main/resources/db/migration/V7__create_physical_assessments.sql backend/src/main/resources/db/migration/V8__create_assessment_measurements.sql
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: V7+V8 physical_assessments and assessment_measurements migrations"
```

---

## Task 2: V9 — student_goals (T-14)

**Files:**
- Create: `backend/src/main/resources/db/migration/V9__create_student_goals.sql`

- [ ] **Step 1: Create V9__create_student_goals.sql**

```sql
CREATE TABLE student_goals (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES users(id),
    created_by   UUID NOT NULL REFERENCES users(id),
    description  VARCHAR(200) NOT NULL,
    metric       VARCHAR(40),
    target_value NUMERIC(8,2),
    target_date  DATE,
    status       VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                     CHECK (status IN ('IN_PROGRESS','ACHIEVED','EXPIRED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 2: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/main/resources/db/migration/V9__create_student_goals.sql
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: V9 student_goals migration"
```

---

## Task 3: V10 — exercises (T-14)

**Files:**
- Create: `backend/src/main/resources/db/migration/V10__create_exercises.sql`

- [ ] **Step 1: Create V10__create_exercises.sql**

```sql
CREATE TABLE exercises (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(120) NOT NULL,
    muscle_group VARCHAR(60) NOT NULL,
    description  TEXT,
    video_url    VARCHAR(255),
    active       BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_exercises_muscle_group ON exercises(muscle_group);
```

- [ ] **Step 2: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/main/resources/db/migration/V10__create_exercises.sql
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: V10 exercises migration"
```

---

## Task 4: V11 + V12 — workout_plans + workout_plan_items (T-14)

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__create_workout_plans.sql`
- Create: `backend/src/main/resources/db/migration/V12__create_workout_plan_items.sql`

- [ ] **Step 1: Create V11__create_workout_plans.sql**

```sql
CREATE TABLE workout_plans (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID NOT NULL REFERENCES users(id),
    trainer_id  UUID NOT NULL REFERENCES users(id),
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workout_plans_student_active ON workout_plans(student_id, active);
```

- [ ] **Step 2: Create V12__create_workout_plan_items.sql**

```sql
CREATE TABLE workout_plan_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_plan_id   UUID NOT NULL REFERENCES workout_plans(id),
    exercise_id       UUID NOT NULL REFERENCES exercises(id),
    division          VARCHAR(10),
    sets              INT NOT NULL,
    reps              VARCHAR(20) NOT NULL,
    suggested_load_kg NUMERIC(6,2),
    rest_seconds      INT,
    order_index       INT NOT NULL,
    notes             VARCHAR(200)
);
```

- [ ] **Step 3: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/main/resources/db/migration/V11__create_workout_plans.sql backend/src/main/resources/db/migration/V12__create_workout_plan_items.sql
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: V11+V12 workout_plans and workout_plan_items migrations"
```

---

## Task 5: V13 + V14 — workout_logs + personal_records (T-14)

**Files:**
- Create: `backend/src/main/resources/db/migration/V13__create_workout_logs.sql`
- Create: `backend/src/main/resources/db/migration/V14__create_personal_records.sql`

- [ ] **Step 1: Create V13__create_workout_logs.sql**

```sql
CREATE TABLE workout_logs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id            UUID NOT NULL REFERENCES users(id),
    exercise_id           UUID NOT NULL REFERENCES exercises(id),
    workout_plan_item_id  UUID REFERENCES workout_plan_items(id),
    performed_at          DATE NOT NULL,
    sets                  INT,
    reps                  INT,
    load_kg               NUMERIC(6,2),
    notes                 VARCHAR(200),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workout_logs_student_exercise_date ON workout_logs(student_id, exercise_id, performed_at);
```

- [ ] **Step 2: Create V14__create_personal_records.sql**

```sql
CREATE TABLE personal_records (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES users(id),
    exercise_id    UUID NOT NULL REFERENCES exercises(id),
    record_load_kg NUMERIC(6,2) NOT NULL,
    achieved_at    DATE NOT NULL,
    UNIQUE (student_id, exercise_id)
);
```

- [ ] **Step 3: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/main/resources/db/migration/V13__create_workout_logs.sql backend/src/main/resources/db/migration/V14__create_personal_records.sql
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: V13+V14 workout_logs and personal_records migrations"
```

---

## Task 6: V15 — progress_photos + full verification (T-14)

**Files:**
- Create: `backend/src/main/resources/db/migration/V15__create_progress_photos.sql`

- [ ] **Step 1: Create V15__create_progress_photos.sql**

```sql
CREATE TABLE progress_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID NOT NULL REFERENCES users(id),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    storage_key VARCHAR(255) NOT NULL,
    taken_at    DATE NOT NULL,
    notes       VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 2: Run all backend tests**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test 2>&1 | tail -10
```

Expected: `Tests run: 62, Failures: 0, Errors: 0` — `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/main/resources/db/migration/V15__create_progress_photos.sql
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Etapa 3 migrations V7-V15 (assessments, goals, exercises, workout, photos)"
```
