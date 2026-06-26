# WachaFit Etapa 3 — Design Spec
**Data:** 2026-06-26  
**Escopo:** Acompanhamento do aluno — backend (T-14 a T-17) + frontend completo (T-13 + T-18)  
**Critério de aceite:** Profissional avalia, prescreve e acompanha; aluno registra treino e vê evolução

---

## 1. Visão Geral

Etapa 3 adiciona os módulos de acompanhamento longitudinal do aluno:
- **Assessment:** avaliações físicas com medidas corporais
- **Goal:** metas com ciclo de vida (IN_PROGRESS → ACHIEVED/EXPIRED)
- **Exercise:** biblioteca global de exercícios
- **Workout:** fichas de treino prescritas + logs de execução + recordes pessoais automáticos
- **Progress:** fotos de progresso armazenadas no disco local

O frontend cobre tanto o core de Etapa 2 (calendário, agendamento, painel admin) quanto todas as telas de acompanhamento de Etapa 3, em fidelidade funcional completa (navegação real, chamadas à API, validações, feedback de erro).

**Decisões de arquitetura:**
- Armazenamento de fotos: **disco local** (`uploads/photos/{studentId}/`), servido via `StreamingResponseBody`
- Calendário frontend: **PrimeVue** (sem FullCalendar); adicionar FullCalendar só se visão mensal for insatisfatória
- Padrão de implementação: TDD, package-by-feature, `@PreAuthorize`, exceções via classes compartilhadas

---

## 2. Decomposição em Sub-etapas

| Sub-etapa | Conteúdo | Tarefas |
|-----------|----------|---------|
| **3A** | Migrations V7–V15 | T-14 |
| **3B** | Assessment + Goal + Exercise backends | T-15 |
| **3C** | Workout + Progress backends | T-16, T-17 |
| **3D** | Frontend completo (T-13 + T-18) | T-13, T-18 |

---

## 3. Schema do Banco (Migrations V7–V15)

### V7 — `physical_assessments`
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

### V8 — `assessment_measurements`
```sql
CREATE TABLE assessment_measurements (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES physical_assessments(id),
    body_part     VARCHAR(40) NOT NULL,
    value_cm      NUMERIC(5,2) NOT NULL
);
```

### V9 — `student_goals`
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

### V10 — `exercises`
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

### V11 — `workout_plans`
```sql
CREATE TABLE workout_plans (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES users(id),
    trainer_id UUID NOT NULL REFERENCES users(id),
    name       VARCHAR(120) NOT NULL,
    description TEXT,
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_workout_plans_student_active ON workout_plans(student_id, active);
```

### V12 — `workout_plan_items`
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

### V13 — `workout_logs`
```sql
CREATE TABLE workout_logs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id           UUID NOT NULL REFERENCES users(id),
    exercise_id          UUID NOT NULL REFERENCES exercises(id),
    workout_plan_item_id UUID REFERENCES workout_plan_items(id),
    performed_at         DATE NOT NULL,
    sets                 INT,
    reps                 INT,
    load_kg              NUMERIC(6,2),
    notes                VARCHAR(200),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_workout_logs_student_exercise_date ON workout_logs(student_id, exercise_id, performed_at);
```

### V14 — `personal_records`
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

### V15 — `progress_photos`
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

---

## 4. Módulos Backend

### 4.1 `assessment` — `com.github.mwacha.wachafit.assessment`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/students/{studentId}/assessments` | TRAINER, ADMIN |
| GET | `/api/students/{studentId}/assessments` | dono, TRAINER, ADMIN |
| GET | `/api/students/{studentId}/assessments/evolution` | dono, TRAINER, ADMIN |
| PUT | `/api/assessments/{id}` | TRAINER, ADMIN |

**DTOs:**
- `CreateAssessmentRequest(assessedAt, weightKg?, heightCm?, bodyFatPct?, bmi?, notes?, measurements[])` — measurements é lista de `{bodyPart, valueCm}`
- `AssessmentResponse(id, studentId, assessedById, assessedAt, weightKg, heightCm, bodyFatPct, bmi, notes, measurements[], createdAt)`
- `EvolutionPoint(assessedAt, weightKg, bodyFatPct, bmi)` — usado na série temporal

**Regras:**
- RN-08: escrita restrita a TRAINER/ADMIN
- RN-11: leitura restrita ao dono + TRAINER/ADMIN; validar `studentId` no service

### 4.2 `goal` — `com.github.mwacha.wachafit.goal`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/students/{studentId}/goals` | dono, TRAINER, ADMIN |
| GET | `/api/students/{studentId}/goals` | dono, TRAINER, ADMIN |
| PATCH | `/api/goals/{id}/status` | TRAINER, ADMIN |

**DTOs:**
- `CreateGoalRequest(description, metric?, targetValue?, targetDate?)`
- `GoalResponse(id, studentId, createdById, description, metric, targetValue, targetDate, status, createdAt)`
- `UpdateGoalStatusRequest(status)` — só ACHIEVED ou EXPIRED

**Regras:**
- RN-11: visibilidade restrita

### 4.3 `exercise` — `com.github.mwacha.wachafit.exercise`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/exercises` | qualquer autenticado |
| POST | `/api/exercises` | TRAINER, ADMIN |
| PUT | `/api/exercises/{id}` | TRAINER, ADMIN |
| PATCH | `/api/exercises/{id}/deactivate` | ADMIN |

**Query params GET:** `q` (busca no nome), `muscleGroup`

**DTOs:**
- `CreateExerciseRequest(name, muscleGroup, description?, videoUrl?)`
- `UpdateExerciseRequest(name, muscleGroup, description?, videoUrl?)`
- `ExerciseResponse(id, name, muscleGroup, description, videoUrl, active)`

### 4.4 `workout` — `com.github.mwacha.wachafit.workout`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/students/{id}/workout-plans` | TRAINER, ADMIN |
| GET | `/api/students/{id}/workout-plans` | dono, TRAINER, ADMIN |
| GET | `/api/students/{id}/workout-plans/active` | dono, TRAINER, ADMIN |
| PUT | `/api/workout-plans/{id}` | TRAINER, ADMIN |
| PATCH | `/api/workout-plans/{id}/activate` | TRAINER, ADMIN |
| POST | `/api/students/{id}/workout-logs` | dono (STUDENT) |
| GET | `/api/students/{id}/workout-logs` | dono, TRAINER, ADMIN |
| GET | `/api/students/{id}/records` | dono, TRAINER, ADMIN |
| GET | `/api/students/{id}/exercises/{exerciseId}/progression` | dono, TRAINER, ADMIN |

**Regras críticas:**
- RN-09: `POST /workout-plans` restrito a TRAINER/ADMIN; `POST /workout-logs` restrito ao dono
- RN-10: ao salvar log, verificar se `load_kg > personal_records.record_load_kg` para (studentId, exerciseId); se sim, upsert em `personal_records`. Tudo em uma transação.
- RN-12: `PATCH /activate` seta `active=false` em todos os planos do aluno, depois `active=true` no plano alvo. Transação com lock pessimista.
- RN-11: visibilidade restrita

### 4.5 `progress` — `com.github.mwacha.wachafit.progress`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/students/{id}/photos` | dono, TRAINER |
| GET | `/api/students/{id}/photos` | dono, TRAINER, ADMIN |
| GET | `/api/photos/{id}/file` | dono, TRAINER, ADMIN |
| DELETE | `/api/photos/{id}` | dono, TRAINER, ADMIN |

**Storage:** arquivo gravado em `${app.upload-dir}/photos/{studentId}/{uuid}.{ext}`. `storage_key` salvo no BD é o caminho relativo ao `upload-dir`. Configurável via `application.yml`: `app.upload-dir: uploads`.

**DTOs:**
- `PhotoResponse(id, studentId, uploadedById, takenAt, notes, fileUrl)` — `fileUrl` aponta para `GET /api/photos/{id}/file`

---

## 5. Regras de Negócio

| Regra | Módulo | Implementação |
|-------|--------|---------------|
| RN-08 | assessment, workout | `@PreAuthorize` restringe escrita a TRAINER/ADMIN |
| RN-09 | workout | Log criado só pelo dono; workout-plans bloqueado para STUDENT |
| RN-10 | workout | Upsert em `personal_records` dentro da mesma transação do log |
| RN-11 | todos | Service valida `studentId` vs JWT `sub`; nega se não é dono nem TRAINER/ADMIN |
| RN-12 | workout | `@Transactional` + UPDATE SET active=false WHERE student_id=? antes de ativar novo plano |

---

## 6. Frontend (T-13 + T-18)

### 6.1 Novas rotas

**Admin:**
- `/admin/users` — CRUD de usuários
- `/admin/classes` — gestão de turmas
- `/admin/schedules` — calendário de horários (criar/cancelar)

**Trainer:**
- `/trainer/schedule` — calendário pessoal + criar horários
- `/trainer/students` — lista de alunos
- `/trainer/students/:id/overview` — visão completa do aluno
- `/trainer/students/:id/assessment/new` — nova avaliação
- `/trainer/students/:id/workout-plan/new` — nova ficha
- `/trainer/students/:id/goals` — metas

**Student:**
- `/student/schedule` — calendário + agendar aula
- `/student/bookings` — minhas reservas
- `/student/workout` — ficha ativa + registrar treino
- `/student/records` — recordes pessoais
- `/student/evolution` — gráficos (peso, medidas, carga)
- `/student/goals` — metas
- `/student/photos` — galeria + upload

### 6.2 Stores Pinia

`schedule.store.ts`, `booking.store.ts`, `assessment.store.ts`, `workout.store.ts`, `progress.store.ts`

### 6.3 Componentes novos

`EvolutionChart.vue` (PrimeVue Chart), `WorkoutLogForm.vue`, `PhotoGallery.vue`, `AssessmentForm.vue`, `WorkoutPlanEditor.vue`, `CalendarView.vue`

### 6.4 Calendário

PrimeVue `<DatePicker>` + listagem de slots por data. Sem FullCalendar (YAGNI). Se a visão de calendário mensal mostrar-se insuficiente durante implementação, adicionar FullCalendar somente nesse ponto.

### 6.5 Sidebar

Adicionar links para as novas rotas no `AppLayout.vue` por role (já estruturado em Etapa 1).

---

## 7. Testes

- **Backend:** TDD em todos os módulos. Testes unitários (Mockito) + integração (Testcontainers). Meta: cobertura das regras RN-08 a RN-12.
- **Progress:** teste de integração de upload com `MockMultipartFile`.
- **Frontend:** não há testes automatizados previstos (fora do escopo desta etapa).

---

## 8. Configuração adicional

`application.yml` / `application-dev.yml`:
```yaml
app:
  upload-dir: uploads
```

O diretório `uploads/` deve estar no `.gitignore`.
