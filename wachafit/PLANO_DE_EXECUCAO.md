# Plano de Execução — Wachafit

*Roteiro de implementação ordenado para o Claude Code.*
*Documentos de referência: `PRD.md` e `DOCUMENTO_TECNICO.md`.*

---

## Como usar este documento

Este é o plano de trabalho. Execute as tarefas **na ordem**, de cima para baixo. Cada tarefa tem:
- um **ID** (`T-XX`);
- **arquivos-alvo** (o que criar/editar);
- **critério de aceite** (como saber que terminou);
- caixas `[ ]` para marcar progresso.

**Regra geral:** só avance para a próxima tarefa quando o critério de aceite da atual estiver satisfeito e o projeto compilar. Ao concluir cada tarefa, marque a caixa e faça um commit com a mensagem sugerida.

---

## Convenções do projeto (ler antes de começar)

- **Linguagem/stack:** Java 21 + Spring Boot 3.3, Vue 3 + TypeScript + Vite, PostgreSQL.
- **Pacote raiz:** `com.github.mwacha.wachafit`. Organização **package-by-feature**.
- **IDs:** todos UUID. Entidades usam:
  ```java
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;
  ```
- **Migrations:** Flyway, uma por feature, nomeadas `V{n}__{descricao}.sql`. Nunca editar migration já aplicada — criar nova.
- **Camadas:** Controller (REST + `@PreAuthorize`) → Service (regras de negócio + `@Transactional`) → Repository (Spring Data). DTOs de entrada/saída separados das entidades.
- **Erros:** lançar exceções de domínio (`BusinessException`, `NotFoundException`) tratadas no `GlobalExceptionHandler`, retornando o JSON de erro padronizado.
- **Nomenclatura de domínio:** aula coletiva = `GroupClass`; sessão individual = schedule com `type = PERSONAL`.
- **Commits:** um por tarefa, prefixo `feat:`, `chore:`, `test:` conforme o caso.
- **Testes:** toda regra de negócio (RN-xx) precisa de teste antes de marcar a tarefa correspondente como concluída.
- **Idioma:** código, nomes e comentários em inglês; mensagens ao usuário final em português.

---

## ETAPA 1 — Base

> Objetivo: projeto roda, banco sobe, usuário cadastra/loga e navega com controle por perfil.

### T-01 — Esqueleto do projeto e infraestrutura local
- [ ] Inicializar projeto Spring Boot (Maven) com `groupId = com.github.mwacha`, `artifactId = wachafit`.
- [ ] Dependências: Web, Data JPA, Validation, Security, Flyway, PostgreSQL Driver, Mail, springdoc-openapi, Lombok (opcional), Testcontainers (test).
- [ ] Criar `docker-compose.yml` com serviços `db` (PostgreSQL 16), `mailhog`.
- [ ] Configurar `application.yml` / `application-dev.yml` lendo variáveis de ambiente.
- **Arquivos:** `backend/pom.xml`, `docker-compose.yml`, `backend/src/main/resources/application*.yml`
- **Aceite:** `docker compose up db` sobe o Postgres; a aplicação inicia conectando no banco sem erro.
- **Commit:** `chore: bootstrap spring boot project and local infra`

### T-02 — Estrutura compartilhada (shared)
- [ ] `GlobalExceptionHandler` com handlers para `BusinessException`, `NotFoundException`, validação e genérico.
- [ ] DTO de erro padronizado (timestamp, status, error, message, path).
- [ ] Classes base de exceção de domínio.
- **Arquivos:** `shared/exception/*`
- **Aceite:** uma exceção lançada num endpoint de teste retorna o JSON de erro no formato do Documento Técnico (seção 5).
- **Commit:** `feat: shared exception handling`

### T-03 — Migration e entidade de usuário
- [ ] `V1__create_users.sql` (tabela `users` com `id uuid`, role, active, etc.).
- [ ] Entidade `User`, enum `Role` (ADMIN, TRAINER, STUDENT), `UserRepository`.
- **Arquivos:** `resources/db/migration/V1__create_users.sql`, `user/User.java`, `user/Role.java`, `user/UserRepository.java`
- **Aceite:** Flyway aplica a migration no boot; tabela `users` existe com PK uuid.
- **Commit:** `feat: user entity and migration`

### T-04 — Segurança e JWT
- [ ] `JwtUtil` (gerar/validar token; claims `sub`=uuid, `role`, `exp`).
- [ ] `JwtFilter` populando o `SecurityContext`.
- [ ] `SecurityConfig` (stateless, rotas públicas de auth liberadas, resto autenticado), `BCryptPasswordEncoder`, `CorsConfig`.
- **Arquivos:** `shared/security/*`, `config/SecurityConfig.java`, `config/CorsConfig.java`
- **Aceite:** endpoint protegido retorna 401 sem token e 200 com token válido.
- **Commit:** `feat: jwt security setup`

### T-05 — Autenticação (register/login/reset)
- [ ] `AuthController` + `AuthService`: `register` (cria STUDENT), `login` (retorna JWT), `forgot-password`, `reset-password`.
- [ ] `V2__create_password_reset_tokens.sql` + entidade.
- [ ] Hash de senha com BCrypt; token de reset com expiração e uso único.
- **Arquivos:** `auth/*`, `resources/db/migration/V2__*.sql`
- **Aceite:** fluxo completo de cadastro → login → token; reset gera token e troca a senha. (RF-01)
- **Commit:** `feat: authentication flow`

### T-06 — Frontend: bootstrap
- [ ] Inicializar Vue 3 + Vite + TypeScript; instalar Pinia, Vue Router, Axios, PrimeVue.
- [ ] `services/api.ts` com instância Axios + interceptors (injeta Bearer, trata 401).
- [ ] Estrutura de pastas conforme Documento Técnico (seção 9).
- **Arquivos:** `frontend/*`
- **Aceite:** `npm run dev` sobe o frontend; chamada autenticada à API funciona com o token.
- **Commit:** `chore: frontend bootstrap`

### T-07 — Frontend: auth e guards
- [ ] `stores/auth.store.ts` (login, logout, persistência do token em memória/estado).
- [ ] Telas `LoginView` e `RegisterView`.
- [ ] Guards de rota por role (`meta.roles`) no `router`.
- **Arquivos:** `frontend/src/views/auth/*`, `stores/auth.store.ts`, `router/index.ts`
- **Aceite:** usuário loga, é redirecionado conforme o role, e rotas restritas bloqueiam acesso indevido. (RF-01.6)
- **Commit:** `feat: frontend auth and route guards`

**✅ Fim da Etapa 1:** usuário cadastra, loga e navega com controle por perfil.

---

## ETAPA 2 — Core (agendamento e gestão)

> Objetivo: aluno agenda, admin gerencia, regras de conflito e capacidade funcionando.

### T-08 — Migrations do core
- [ ] `V3__create_group_classes.sql`, `V4__create_schedules.sql`, `V5__create_bookings.sql`, `V6__create_trainer_availability.sql`.
- [ ] FKs em uuid; `UNIQUE (schedule_id, student_id)` em bookings; índices da seção 3.
- **Aceite:** todas as migrations aplicam; relacionamentos batem com o diagrama.
- **Commit:** `feat: core scheduling migrations`

### T-09 — Gestão de usuários (admin)
- [ ] CRUD de profissionais e gestão de alunos; desativação (RN-07).
- [ ] `@PreAuthorize("hasRole('ADMIN')")` nos endpoints de escrita.
- **Arquivos:** `user/UserController.java`, `user/UserService.java`, dtos
- **Aceite:** admin cria/edita/desativa; usuário desativado não autentica nem lista. (RF-02)
- **Commit:** `feat: admin user management`

### T-10 — Turmas (GroupClass)
- [ ] Módulo `groupclass`: entidade, repo, service, controller. CRUD restrito a ADMIN. (RF-03)
- **Arquivos:** `groupclass/*`
- **Aceite:** admin cria/edita/desativa turma com capacidade, duração e profissional.
- **Commit:** `feat: group class management`

### T-11 — Disponibilidade e schedules
- [ ] Módulo `schedule`: criar ocorrência (CLASS/PERSONAL), cancelar, listar por período/filtro.
- [ ] Disponibilidade do profissional (`trainer_availability`).
- [ ] **RN-02**: impedir overlap de horário do profissional.
- **Arquivos:** `schedule/*`
- **Aceite:** criação de schedule respeita conflito do profissional; listagem por `from/to/trainerId/type` funciona. (RF-04, RF-06)
- **Testes:** RN-02.
- **Commit:** `feat: schedules and availability`

### T-12 — Agendamento (bookings)
- [ ] Módulo `booking`: aluno agenda slot, cancela, lista histórico.
- [ ] **RN-01** (conflito do aluno), **RN-03** (capacidade, com tratamento de concorrência — transação + lock), **RN-04** (janela de cancelamento), **RN-06** (PERSONAL=PENDING / CLASS com vaga=CONFIRMED).
- **Arquivos:** `booking/*`
- **Aceite:** aluno agenda respeitando todas as regras; última vaga não é vendida em dobro sob concorrência.
- **Testes:** RN-01, RN-03 (incluindo cenário concorrente), RN-04, RN-06.
- **Commit:** `feat: booking with business rules`

### T-13 — Frontend: calendário e agendamento
- [ ] Componente de calendário (dia/semana/mês) com filtros e cores por status. (RF-06)
- [ ] Fluxo de agendamento do aluno (`BookingModal`, `TimeSlotPicker`).
- [ ] Telas do aluno (dashboard, meus agendamentos) e do profissional (agenda).
- **Arquivos:** `frontend/src/components/scheduling/*`, `views/student/*`, `views/trainer/*`, `stores/schedule.store.ts`, `stores/booking.store.ts`
- **Aceite:** aluno visualiza slots, agenda e vê confirmação; profissional vê a própria agenda.
- **Commit:** `feat: frontend calendar and booking`

### T-14 — Frontend: painel admin
- [ ] Gestão de turmas, profissionais e horários; visão geral e indicadores. (RF-08)
- **Arquivos:** `frontend/src/components/admin/*`, `views/admin/*`
- **Aceite:** admin gerencia tudo pela interface; indicadores básicos exibidos.
- **Commit:** `feat: admin dashboard`

**✅ Fim da Etapa 2:** agendamento ponta a ponta com regras de conflito/capacidade.

---

## ETAPA 3 — Acompanhamento do aluno

> Objetivo: profissional avalia, prescreve e acompanha; aluno registra treino e vê evolução.

### T-15 — Migrations do acompanhamento
- [ ] `V7__create_physical_assessments.sql`, `V8__create_assessment_measurements.sql`, `V9__create_student_goals.sql`, `V10__create_exercises.sql`, `V11__create_workout_plans.sql`, `V12__create_workout_plan_items.sql`, `V13__create_workout_logs.sql`, `V14__create_personal_records.sql`, `V15__create_progress_photos.sql`.
- [ ] Índices da seção 3 (assessments, workout_logs, workout_plans, exercises).
- **Aceite:** todas as 9 tabelas criadas com FKs uuid e relacionamentos do diagrama.
- **Commit:** `feat: progress-tracking migrations`

### T-16 — Avaliações físicas
- [ ] Módulo `assessment`: registrar avaliação + medidas, histórico, série de evolução, edição. Cálculo de IMC. (RF-09)
- [ ] Escrita restrita a TRAINER/ADMIN (RN-08).
- **Arquivos:** `assessment/*`
- **Aceite:** profissional registra avaliação com medidas; endpoint de evolução retorna série temporal por métrica.
- **Testes:** RN-08; cálculo de IMC.
- **Commit:** `feat: physical assessments`

### T-17 — Metas
- [ ] Módulo `goal`: criar, listar, atualizar status (IN_PROGRESS/ACHIEVED/EXPIRED). (RF-10)
- **Arquivos:** `goal/*`
- **Aceite:** metas criadas e acompanhadas; status atualizável.
- **Commit:** `feat: student goals`

### T-18 — Biblioteca de exercícios
- [ ] Módulo `exercise`: CRUD + busca por nome/grupo muscular. Escrita TRAINER/ADMIN. (RF-11)
- **Arquivos:** `exercise/*`
- **Aceite:** busca por `q` e `muscleGroup` funciona; gestão restrita.
- **Commit:** `feat: exercise library`

### T-19 — Fichas de treino
- [ ] Módulo `workout` (parte 1): `WorkoutPlan` + `WorkoutPlanItem`. Criar ficha com itens (séries, reps, carga, descanso, divisão, ordem). (RF-12)
- [ ] **RN-12**: ativar ficha arquiva a anterior na mesma transação.
- [ ] **RN-09**: prescrição só por TRAINER/ADMIN.
- **Arquivos:** `workout/WorkoutPlan*.java`, `workout/WorkoutPlanItem*.java`
- **Aceite:** profissional monta ficha; só uma fica ativa por aluno; aluno vê a ativa.
- **Testes:** RN-09, RN-12.
- **Commit:** `feat: workout plans`

### T-20 — Registro de treino e recordes
- [ ] Módulo `workout` (parte 2): `WorkoutLog` (registro de carga) + `PersonalRecord`. (RF-13)
- [ ] **RN-10**: ao salvar log que supera o melhor valor, fazer upsert do PR.
- [ ] Endpoints de histórico de cargas e progressão por exercício.
- **Arquivos:** `workout/WorkoutLog*.java`, `workout/PersonalRecord*.java`
- **Aceite:** aluno registra carga; PR atualiza automaticamente; progressão retorna série por exercício.
- **Testes:** RN-10.
- **Commit:** `feat: workout logs and personal records`

### T-21 — Fotos de progresso e storage
- [ ] `StorageService` (interface) + implementação local (dev) e S3-compatible (prod), selecionadas por profile.
- [ ] Módulo `progress`: upload (multipart), listagem com URL assinada, remoção. Validar tipo/tamanho. (RF-14)
- [ ] **RN-11**: visibilidade restrita ao dono + profissional/admin.
- **Arquivos:** `progress/*`
- **Aceite:** upload grava no storage e só a `storage_key` no banco; aluno não acessa fotos de outro aluno.
- **Testes:** RN-11.
- **Commit:** `feat: progress photos and storage`

### T-22 — Frontend: acompanhamento
- [ ] Avaliações + gráficos de evolução; metas; biblioteca; editor de ficha de treino; registro de cargas; galeria de fotos.
- **Arquivos:** `frontend/src/views/student/*`, `views/trainer/*`, componentes de gráfico
- **Aceite:** profissional avalia/prescreve pela interface; aluno registra treino e vê evolução em gráficos.
- **Commit:** `feat: frontend progress tracking`

**✅ Fim da Etapa 3:** ciclo completo de avaliação → prescrição → registro → evolução.

---

## ETAPA 4 — Finalização

> Objetivo: notificações, relatórios PDF, polish, documentação e deploy.

### T-23 — Notificações por e-mail
- [ ] `EmailService` com envio assíncrono (`@Async`) disparado em `@TransactionalEventListener(AFTER_COMMIT)`.
- [ ] Templates: confirmação de agendamento, solicitação ao profissional, cancelamento. (RF-07)
- **Arquivos:** `notification/*`
- **Aceite:** e-mails aparecem no MailHog após os eventos correspondentes; nada é enviado se a transação falha.
- **Commit:** `feat: email notifications`

### T-24 — Relatórios PDF
- [ ] Módulo `report`: relatório de evolução do aluno e exportação da ficha, via Thymeleaf + openhtmltopdf. (RF-15)
- [ ] Gráficos embutidos (PNG/SVG); identificação da academia/profissional.
- **Arquivos:** `report/*`, `report/templates/*`
- **Aceite:** endpoints retornam `application/pdf` válido com os dados do aluno.
- **Commit:** `feat: pdf reports`

### T-25 — Responsividade e polish de UI ✅
- [x] Revisar todas as telas mobile-first (320px+). (RNF-01)
- [x] Estados de loading/erro/vazio; feedback de ações.
- **Aceite:** navegação fluida de 320px ao desktop sem quebra.
- **Commit:** `feat: responsive polish`

### T-26 — Documentação e testes finais ✅
- [x] OpenAPI/Swagger habilitado e revisado.
- [x] `README.md` com setup, variáveis de ambiente e como rodar via Docker.
- [x] Suíte de testes cobrindo as RNs; testes de integração com Testcontainers.
- **Aceite:** Swagger acessível; README permite a um terceiro subir o projeto; testes passam. (179 testes, BUILD SUCCESS)
- **Commit:** `docs: api docs, readme and tests`

### T-27 — Deploy
- [ ] Dockerfiles de backend e frontend; build de produção.
- [ ] Deploy do backend (PaaS/VPS), banco PostgreSQL gerenciado, frontend estático, storage S3-compatible.
- **Aceite:** ambiente de produção acessível e funcional ponta a ponta.
- **Commit:** `chore: production deploy`

**✅ Fim da Etapa 4:** sistema completo, documentado e em produção.

---

## Definition of Done (cada tarefa)
1. Critério de aceite satisfeito.
2. Projeto compila e sobe sem erro.
3. Testes da tarefa (quando houver RN associada) passam.
4. Caixa marcada e commit feito.

## Mapa rápido tarefa → requisitos
| Etapa | Tarefas | Requisitos cobertos |
|-------|---------|---------------------|
| 1 | T-01 a T-07 | RF-01, RNF-03/04/05 |
| 2 | T-08 a T-14 | RF-02 a RF-08, RN-01 a RN-07 |
| 3 | T-15 a T-22 | RF-09 a RF-14, RN-08 a RN-12 |
| 4 | T-23 a T-27 | RF-07, RF-15, RNF-01/06 |
