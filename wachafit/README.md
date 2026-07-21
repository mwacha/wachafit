# WachaFit

Sistema de gestão para academias e personal trainers — agendamento, acompanhamento de alunos, financeiro e relatórios.

---

## Funcionalidades

### Gestão de Alunos
- Cadastro completo com wizard de 6 etapas (dados pessoais, endereço, plano, anamnese, PAR-Q, contrato)
- Perfil com foto, dados físicos e histórico
- Avaliações físicas com medidas corporais e composição corporal
- Metas com acompanhamento de status (em andamento, concluída, abandonada)
- Fotos de progresso com visualização cronológica

### Turmas e Agenda
- Cadastro de turmas coletivas com grade de horários semanal
- Matrículas de alunos em turmas (válidas até o plano expirar)
- Agenda de sessões personais com calendário semanal de navegação
- Grade visual de horários por dia da semana
- Reserva de sessões personais com confirmação automática

### Fichas de Treino
- Editor de fichas de treino por aluno (séries, reps, carga, descanso, divisão A/B/C)
- Biblioteca de exercícios por grupo muscular com filtro
- Ativação de ficha vigente
- Log de treinos executados com cargas registradas
- Evolução de carga por exercício com gráfico
- Recordes pessoais automáticos

### Financeiro
- Planos de matrícula com valor e periodicidade
- Geração automática de cobranças mensais
- Registro de pagamentos (múltiplas formas)
- Fluxo de caixa com entradas e saídas
- Gate de inadimplência: aluno com cobrança vencida vê apenas a tela de pagamentos
- Relatório de receita, inadimplentes e comissões de personal trainers

### Usuários e Perfis
- Multi-perfil: Admin, Manager, Cashier, Receptionist, Trainer, Student
- Autenticação JWT com expiração configurável
- Reset de senha via e-mail
- Menus adaptativos por role com grupos colapsáveis

### Comunicação
- E-mails automáticos (boas-vindas, reset de senha, nova ficha de treino)
- Geração de relatórios em PDF

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · Flyway |
| Banco | PostgreSQL 16 |
| Frontend | Vue 3 · TypeScript · Vite · PrimeVue 4 (Aura) · Pinia · Chart.js |
| E-mail | JavaMailSender · Thymeleaf · MailHog (dev) |
| PDF | openhtmltopdf + Thymeleaf |
| Testes | JUnit 5 · Mockito · Testcontainers |

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|--------------|
| Java | 21 |
| Maven | 3.9 |
| Node.js | 20 |
| Docker + Docker Compose | qualquer versão recente |

---

## Rodando em desenvolvimento

### Opção 1 — script único (recomendado)

```bash
chmod +x dev.sh
./dev.sh
```

O script sobe o PostgreSQL e o MailHog via Docker, aguarda o banco ficar pronto, inicia o backend e o frontend automaticamente. `Ctrl+C` encerra tudo.

| Serviço | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend / API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MailHog (e-mails) | http://localhost:8025 |

---

### Opção 2 — passo a passo

#### 1. Infraestrutura (banco + e-mail)

```bash
docker compose up -d
```

Sobe:
- **PostgreSQL 16** em `localhost:5432` (banco `wachafit`, user/senha `wachafit`)
- **MailHog** em `localhost:1025` (SMTP) e `localhost:8025` (UI web)

#### 2. Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## Variáveis de ambiente (produção)

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `DB_URL` | JDBC URL do PostgreSQL | `jdbc:postgresql://db:5432/wachafit` |
| `DB_USER` | Usuário do banco | `wachafit` |
| `DB_PASSWORD` | Senha do banco | `senha-segura` |
| `JWT_SECRET` | Segredo HMAC-SHA256 (mín. 32 chars) | `meu-segredo-de-32-chars-minimo!` |
| `JWT_EXPIRATION` | Expiração do token em segundos | `3600` |
| `MAIL_HOST` | Servidor SMTP | `smtp.sendgrid.net` |
| `MAIL_PORT` | Porta SMTP | `587` |
| `MAIL_USER` | Usuário SMTP | `apikey` |
| `MAIL_PASSWORD` | Senha SMTP | `SG.xxx` |
| `APP_FRONTEND_URL` | URL pública do frontend (CORS) | `https://app.wachafit.com` |
| `APP_UPLOAD_DIR` | Diretório de uploads | `/data/uploads` |
| `PAYMENT_GATEWAY` | Gateway de pagamento | `manual` |
| `PAYMENT_SUSPEND_AFTER_DAYS` | Dias de atraso para suspender acesso | `5` |

---

## Deploy em produção (Docker)

```bash
# 1. Copiar e preencher variáveis de ambiente
cp .env.example .env
# edite .env com os valores reais

# 2. Subir todos os serviços
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

| Serviço | Porta | Função |
|---------|-------|--------|
| `frontend` | `80` | nginx serve assets Vue + proxy `/api/` → backend |
| `backend` | interno | Spring Boot 3 |
| `db` | interno | PostgreSQL 16 |

**Volumes persistentes:**

| Volume | Conteúdo |
|--------|----------|
| `postgres_data` | Dados do banco |
| `uploads` | Fotos de progresso e anexos |

**HTTPS (recomendado):** coloque um reverse proxy na frente. Exemplo com Caddy:

```
app.wachafit.com {
    reverse_proxy localhost:80
}
```

---

## Testes

```bash
cd backend
mvn test
```

Os testes de integração sobem o banco automaticamente via **Testcontainers** (requer Docker em execução).

---

## Arquitetura

```
backend/
├── src/main/java/com/github/mwacha/wachafit/
│   ├── assessment/       # Avaliações físicas e medidas corporais
│   ├── auth/             # Login, registro, reset de senha (JWT)
│   ├── billing/          # Cobranças, pagamentos e gateway
│   ├── booking/          # Reservas de sessões personais
│   ├── config/           # Security, CORS, OpenAPI
│   ├── exercise/         # Biblioteca de exercícios
│   ├── goal/             # Metas dos alunos
│   ├── groupclass/       # Turmas coletivas e matrículas
│   ├── membership/       # Planos e assinaturas
│   ├── notification/     # E-mails assíncronos via eventos
│   ├── pdf/              # Geração de relatórios PDF
│   ├── profile/          # Perfil e foto de alunos
│   ├── progress/         # Fotos de progresso físico
│   ├── report/           # Relatórios gerenciais
│   ├── schedule/         # Horários CLASS e PERSONAL
│   ├── shared/           # Exceções globais, utilitários JWT
│   ├── user/             # Gestão de usuários e roles
│   └── workout/          # Fichas de treino, logs e recordes
└── src/main/resources/
    ├── db/migration/     # 28 migrações Flyway (V1..V28)
    └── templates/        # Templates Thymeleaf (e-mail + PDF)

frontend/
└── src/
    ├── components/       # AppLayout (sidebar com grupos colapsáveis)
    ├── router/           # Rotas com guards por role
    ├── services/         # Clientes HTTP (Axios)
    ├── stores/           # Estado global (Pinia)
    ├── types/            # Tipos TypeScript da API
    └── views/
        ├── admin/        # Dashboard, usuários, turmas, agenda, planos, relatórios
        ├── cashier/      # Cobranças e fluxo de caixa
        ├── exercises/    # Biblioteca de exercícios
        ├── receptionist/ # Matrículas e cobranças
        ├── student/      # Aulas, reservas, treino, evolução, cobranças
        └── trainer/      # Agenda, alunos, fichas de treino
```

---

## Perfis de acesso

| Role | Acesso |
|------|--------|
| `ADMIN` | Tudo — cadastros, financeiro, relatórios, usuários |
| `MANAGER` | Planos, receita, inadimplentes, comissões |
| `CASHIER` | Cobranças e fluxo de caixa |
| `RECEPTIONIST` | Matrículas e cobranças |
| `TRAINER` | Agenda própria, alunos e fichas de treino |
| `STUDENT` | Aulas, reservas, treino, evolução e cobranças próprias |

---

## API

Todos os endpoints protegidos exigem o header:

```
Authorization: Bearer <token>
```

Obtenha o token em:

```
POST /api/auth/login
{ "email": "...", "password": "..." }
```

Documentação interativa completa: http://localhost:8080/swagger-ui.html
