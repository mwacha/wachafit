# Design — Wachafit Etapa 2: Core

**Data:** 2026-06-25
**Escopo:** T-08 a T-14 (migrations do core, gestão de usuários, turmas, schedules, disponibilidade, bookings, frontend calendário + agendamento + painel admin)
**Critério de aceite:** aluno agenda, admin gerencia; regras de conflito e capacidade funcionando
**Documentos de referência:** PRD.md (RF-02 a RF-08, RN-01 a RN-07), DOCUMENTO_TECNICO.md, PLANO_DE_EXECUCAO.md

---

## Decisões arquiteturais

| Decisão | Escolha | Motivo |
|---------|---------|--------|
| Lock RN-03 (capacidade) | `SELECT FOR UPDATE` pessimista via JPQL | Simples, robusto para o volume do MVP; sem retry logic |
| Calendário frontend | FullCalendar Vue 3 (`@fullcalendar/vue3`) | PrimeVue não tem grade de eventos semana/mês; FullCalendar é o padrão do mercado |
| Recorrência de turmas | Geração em lote no frontend + `POST /api/schedules/batch` | Sem mudança no modelo de dados; frontend calcula datas, backend valida e insere |
| Estrutura de rotas frontend | Separadas por role (`/admin/*`, `/trainer/*`, `/student/*`) | Guards já existem da Etapa 1; separação clara de responsabilidades |

---

## Seção 1: Migrations (T-08)

### V3__create_group_classes.sql

```sql
CREATE TABLE group_classes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(120) NOT NULL,
    description      TEXT,
    capacity         INT NOT NULL,
    duration_minutes INT NOT NULL,
    trainer_id       UUID NOT NULL REFERENCES users(id),
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### V4__create_schedules.sql

```sql
CREATE TABLE schedules (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_class_id UUID REFERENCES group_classes(id),
    trainer_id     UUID NOT NULL REFERENCES users(id),
    type           VARCHAR(20) NOT NULL CHECK (type IN ('CLASS', 'PERSONAL')),
    starts_at      TIMESTAMPTZ NOT NULL,
    ends_at        TIMESTAMPTZ NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'FULL', 'CANCELLED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_schedules_starts_at ON schedules(starts_at);
CREATE INDEX idx_schedules_trainer_starts ON schedules(trainer_id, starts_at);
```

### V5__create_bookings.sql

```sql
CREATE TABLE bookings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES schedules(id),
    student_id  UUID NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    booked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (schedule_id, student_id)
);

CREATE INDEX idx_bookings_student_status ON bookings(student_id, status);
```

### V6__create_trainer_availability.sql

```sql
CREATE TABLE trainer_availability (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL REFERENCES users(id),
    weekday    INT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL
);
```

---

## Seção 2: Gestão de usuários — T-09

### Pacote: `user/`

Adiciona `UserService.java` e `UserController.java` ao pacote já existente.

### Endpoints

Todos com `@PreAuthorize("hasRole('ADMIN')")` exceto GET.

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/users?role=&active=` | Lista usuários com filtros opcionais |
| POST | `/api/users` | Cria TRAINER ou ADMIN (nunca STUDENT via este endpoint) |
| PUT | `/api/users/{id}` | Edita nome, email, role |
| PATCH | `/api/users/{id}/deactivate` | Seta `active=false` (RN-07) |
| PATCH | `/api/users/{id}/activate` | Seta `active=true` |

### DTOs

```java
// Entrada
record CreateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank @Size(min=8) String password,
    @NotNull Role role  // só TRAINER ou ADMIN; validado no service
) {}

record UpdateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotNull Role role
) {}

// Saída
record UserResponse(
    String id, String name, String email,
    String role, boolean active, String createdAt
) {}
```

### Regras

- `POST /api/users`: role deve ser TRAINER ou ADMIN (STUDENT só via `/api/auth/register`); senha com hash BCrypt.
- `PATCH deactivate/activate`: não permite desativar o próprio usuário autenticado.
- `GET /api/users`: suporta `?role=TRAINER`, `?active=true/false`, combinável.

---

## Seção 3: Turmas — T-10

### Pacote: `groupclass/`

`GroupClass.java`, `GroupClassRepository.java`, `GroupClassService.java`, `GroupClassController.java`, `dto/`.

### Endpoints

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/classes?active=` | Autenticado |
| POST | `/api/classes` | ADMIN |
| PUT | `/api/classes/{id}` | ADMIN |
| PATCH | `/api/classes/{id}/deactivate` | ADMIN |

### DTOs

```java
record GroupClassRequest(
    @NotBlank String name,
    String description,
    @Min(1) int capacity,
    @Min(1) int durationMinutes,
    @NotNull UUID trainerId
) {}

record GroupClassResponse(
    String id, String name, String description,
    int capacity, int durationMinutes,
    String trainerId, String trainerName,
    boolean active, String createdAt
) {}
```

`trainerName` resolvido via `JOIN FETCH` no repositório para evitar N+1.

---

## Seção 4: Schedules + Disponibilidade — T-11

### Pacote: `schedule/`

`Schedule.java`, `ScheduleRepository.java`, `ScheduleService.java`, `ScheduleController.java`, `TrainerAvailability.java`, `TrainerAvailabilityRepository.java`, `dto/`.

### Endpoints de Schedules

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/schedules?from=&to=&trainerId=&type=` | Autenticado |
| POST | `/api/schedules` | ADMIN/TRAINER |
| POST | `/api/schedules/batch` | ADMIN |
| PATCH | `/api/schedules/{id}/cancel` | ADMIN/TRAINER (só o próprio trainer ou admin) |

### `POST /api/schedules/batch`

```java
record BatchScheduleRequest(
    UUID groupClassId,  // null se PERSONAL
    UUID trainerId,
    ScheduleType type,
    List<SlotRequest> slots  // [{startsAt, endsAt}, ...]
) {}
```

O service valida RN-02 para **todos** os slots antes de persistir qualquer um (`@Transactional`). Se qualquer slot conflitar, lança `BusinessException` com a lista de conflitos e faz rollback.

### RN-02 — Conflito do profissional

```java
// ScheduleRepository
@Query("""
  SELECT COUNT(s) FROM Schedule s
  WHERE s.trainerId = :trainerId
    AND s.status != 'CANCELLED'
    AND s.startsAt < :endsAt
    AND s.endsAt > :startsAt
""")
long countOverlaps(UUID trainerId, OffsetDateTime startsAt, OffsetDateTime endsAt);
```

### Endpoints de Disponibilidade

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/trainers/{id}/availability` | Autenticado |
| PUT | `/api/trainers/{id}/availability` | TRAINER (próprio) ou ADMIN |

`PUT` recebe lista completa e faz `DELETE WHERE trainer_id = :id` + `INSERT` em uma única transação (replace strategy).

---

## Seção 5: Bookings — T-12

### Pacote: `booking/`

`Booking.java`, `BookingRepository.java`, `BookingService.java`, `BookingController.java`, `dto/`.

### Endpoints

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/bookings` | STUDENT |
| GET | `/api/bookings/me` | STUDENT |
| PATCH | `/api/bookings/{id}/cancel` | STUDENT/ADMIN |
| PATCH | `/api/bookings/{id}/confirm` | TRAINER/ADMIN |

### Fluxo de `POST /api/bookings` — `@Transactional`

```
1. Buscar schedule por ID → NotFoundException se não existir ou CANCELLED
2. RN-01: verificar overlap do aluno
   SELECT COUNT(b) FROM Booking b
   JOIN Schedule s ON s.id = b.scheduleId
   WHERE b.studentId = :studentId
     AND b.status IN ('PENDING','CONFIRMED')
     AND s.startsAt < :endsAt
     AND s.endsAt > :startsAt
   → BusinessException se > 0

3. SELECT FOR UPDATE: bloquear linha do schedule
   SELECT s FROM Schedule s WHERE s.id = :id FOR UPDATE

4. RN-03: contar confirmados
   SELECT COUNT(b) FROM Booking b
   WHERE b.scheduleId = :id AND b.status = 'CONFIRMED'
   → BusinessException("Turma lotada") se >= groupClass.capacity

5. RN-06: definir status inicial
   type=CLASS && vagas > 0  → CONFIRMED
   type=PERSONAL            → PENDING

6. Se novos confirmados == capacity → atualizar schedule.status = 'FULL'

7. Persistir Booking
```

### PATCH cancel — RN-04

```java
long hoursUntilStart = ChronoUnit.HOURS.between(Instant.now(), schedule.getStartsAt());
if (hoursUntilStart < cancellationWindowHours) {
    throw new BusinessException("Cancelamento não permitido com menos de X horas de antecedência");
}
```

`cancellationWindowHours` via `@Value("${app.cancellation-window-hours:4}")`.

Ao cancelar: seta booking `CANCELLED`, se schedule estava `FULL` volta para `OPEN`.

### PATCH confirm

Somente bookings com `status=PENDING` e `schedule.type=PERSONAL`. Verifica que o TRAINER autenticado é o `schedule.trainerId`.

### DTO de resposta

```java
record BookingResponse(
    String id, String scheduleId,
    String startsAt, String endsAt,
    String type, String status,
    String groupClassName,  // null se PERSONAL
    String trainerName,
    String bookedAt
) {}
```

### Testes obrigatórios

- RN-01: aluno com booking ativo não consegue criar outro no mesmo horário
- RN-03: dois STUDENTs tentam reservar a última vaga simultaneamente (2 threads, Testcontainers) → só um sucede
- RN-04: cancelamento fora da janela lança BusinessException
- RN-06: CLASS confirma automaticamente; PERSONAL fica PENDING

---

## Seção 6: Frontend — Calendário + Agendamento (T-13)

### Novas dependências

```bash
npm install @fullcalendar/vue3 @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction
```

### Novas rotas

```typescript
// STUDENT
{ path: '/student/calendar',  component: StudentCalendarView,  meta: { requiresAuth: true, roles: ['STUDENT'] } }
{ path: '/student/bookings',  component: StudentBookingsView,  meta: { requiresAuth: true, roles: ['STUDENT'] } }

// TRAINER
{ path: '/trainer/schedule',      component: TrainerScheduleView,      meta: { requiresAuth: true, roles: ['TRAINER'] } }
{ path: '/trainer/availability',  component: TrainerAvailabilityView,  meta: { requiresAuth: true, roles: ['TRAINER'] } }
```

### Stores

**`schedule.store.ts`** — `fetchSchedules(from, to, trainerId?, type?)` → chama `GET /api/schedules`, armazena lista, mapeia para eventos FullCalendar (`{id, title, start, end, color, extendedProps}`).

**`booking.store.ts`** — `fetchMyBookings()`, `createBooking(scheduleId)`, `cancelBooking(bookingId)`.

### Componentes (`components/scheduling/`)

**`WfCalendar.vue`** — wrapper FullCalendar:
- Props: `events[]`, `editable?: boolean`, `selectable?: boolean`
- Emits: `@event-click(eventInfo)`, `@date-select(selectInfo)`
- Plugins: `dayGridPlugin`, `timeGridPlugin`, `interactionPlugin`
- Header: botões dia/semana/mês + navegação prev/next/hoje
- Cores por status: `OPEN=#4175F5`, `FULL=#8890A4`, `CANCELLED=#EF4444`, `CONFIRMED=#22C55E`, `PENDING=#F59E0B`
- Locale: `pt-br`

**`BookingModal.vue`** — modal PrimeVue Dialog:
- Props: `schedule: ScheduleEvent`
- Mostra: horário, nome da turma/profissional, vagas disponíveis, duração
- Botão "Confirmar agendamento" → chama `booking.store.createBooking()`
- Estados: loading, sucesso, erro

### Views

**`StudentCalendarView.vue`**
- Filtros: type (turma/sessão individual), trainerId
- Carrega `GET /api/schedules?from=&to=` ao montar e ao navegar no calendário (evento `datesSet` do FullCalendar)
- Clicar em evento `OPEN` abre `BookingModal`
- Eventos do próprio aluno (`/api/bookings/me`) sobrepostos com cor verde

**`StudentBookingsView.vue`**
- Lista de bookings do aluno em cards
- Badge de status (PENDING/CONFIRMED/CANCELLED)
- Botão cancelar (só para futuros dentro da janela)

**`TrainerScheduleView.vue`**
- `WfCalendar` read-only com os próprios schedules
- Filtro por semana/mês

**`TrainerAvailabilityView.vue`**
- Grade: linhas = dias da semana (Seg-Dom), colunas = períodos (manhã/tarde/noite)
- Toggle por período ou time pickers para início/fim exatos
- Salva via `PUT /api/trainers/{id}/availability`

### Sidebar — atualização

**STUDENT:** ícone Calendar → `/student/calendar`; ícone Bookings (list) → `/student/bookings`
**TRAINER:** ícone Calendar → `/trainer/schedule`; ícone Clock → `/trainer/availability`

---

## Seção 7: Frontend — Painel Admin (T-14)

### Novas rotas

```typescript
{ path: '/admin/users',     component: AdminUsersView,     meta: { requiresAuth: true, roles: ['ADMIN'] } }
{ path: '/admin/classes',   component: AdminClassesView,   meta: { requiresAuth: true, roles: ['ADMIN'] } }
{ path: '/admin/schedules', component: AdminSchedulesView, meta: { requiresAuth: true, roles: ['ADMIN'] } }
```

### Views

**`AdminUsersView.vue`**
- Tabela PrimeVue DataTable com colunas: nome, email, role, status (badge), criado em
- Filtros: role (TRAINER/STUDENT/ADMIN), active (sim/não)
- Botão "Novo profissional" → modal `CreateUserModal` com `CreateUserRequest`
- Ações inline: editar (modal), ativar/desativar

**`AdminClassesView.vue`**
- Tabela de turmas: nome, capacidade, duração, profissional, status
- Botão "Nova turma" → modal com `GroupClassRequest`
- Dropdown de profissionais ativos (`GET /api/users?role=TRAINER&active=true`)
- Ações: editar, desativar

**`AdminSchedulesView.vue`**
- `WfCalendar` mostrando todos os schedules com filtros por turma e profissional
- Botão "Gerar recorrência" → `RecurrenceModal.vue`:
  - Seleciona turma (dropdown `GroupClass`)
  - Dias da semana (checkboxes: Seg/Ter/Qua/Qui/Sex/Sáb/Dom)
  - Horário início + fim
  - Período: data inicial e data final
  - Preview: lista de datas que serão geradas
  - Confirmar → `POST /api/schedules/batch`

**`AdminDashboard.vue` — KPIs reais**
- Total alunos ativos: `GET /api/users?role=STUDENT&active=true` → count
- Total profissionais: `GET /api/users?role=TRAINER&active=true` → count
- Aulas hoje: `GET /api/schedules?from={hoje 00:00}&to={hoje 23:59}` → count

### Sidebar admin atualizada

| Ícone | Label | Rota |
|-------|-------|------|
| home | Dashboard | /admin |
| users | Usuários | /admin/users |
| calendar | Agenda | /admin/schedules |
| list | Turmas | /admin/classes |
| chart-bar | Relatórios | (Etapa 4) |

---

## Regras de negócio cobertas

| Regra | Onde |
|-------|------|
| RN-01: conflito de horário do aluno | `BookingService.createBooking()` |
| RN-02: conflito do profissional | `ScheduleService.create()` e `createBatch()` |
| RN-03: capacidade máxima (com lock) | `BookingService.createBooking()` via `SELECT FOR UPDATE` |
| RN-04: janela de cancelamento | `BookingService.cancelBooking()` |
| RN-05: só ADMIN cria/edita turmas | `@PreAuthorize` em `GroupClassController` |
| RN-06: CLASS→CONFIRMED, PERSONAL→PENDING | `BookingService.createBooking()` |
| RN-07: usuário desativado bloqueado | `UserController` (deactivate) + `JwtFilter.isEnabled()` (já implementado) |

---

## Fora do escopo desta etapa

- Notificações por e-mail (Etapa 4 — T-23)
- Avaliações, fichas de treino, metas (Etapa 3)
- Relatórios PDF (Etapa 4)
