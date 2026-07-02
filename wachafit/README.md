# WachaFit

Sistema de gestão para academias e personal trainers — agendamento, acompanhamento de alunos, financeiro e relatórios.

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · Flyway |
| Banco | PostgreSQL 16 |
| Frontend | Vue 3 · TypeScript · Vite · PrimeVue · Pinia |
| Email | JavaMailSender · Thymeleaf · MailHog (dev) |
| PDF | openhtmltopdf + Thymeleaf |
| Testes | JUnit 5 · Mockito · Testcontainers |

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Node.js 20+
- Docker + Docker Compose

## Rodando em desenvolvimento

### 1. Subir infraestrutura local

```bash
docker compose up -d
```

Isso sobe:
- **PostgreSQL 16** em `localhost:5432` (banco `wachafit`, user/senha `wachafit`)
- **MailHog** em `localhost:1025` (SMTP) e `localhost:8025` (UI web)

### 2. Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

A API ficará em `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

O frontend ficará em `http://localhost:5173`.

## Variáveis de ambiente (produção)

Defina as seguintes variáveis antes de subir em produção:

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
| `APP_UPLOAD_DIR` | Diretório de uploads locais | `/data/uploads` |
| `PAYMENT_GATEWAY` | Gateway de pagamento (`manual`) | `manual` |
| `PAYMENT_SUSPEND_AFTER_DAYS` | Dias de atraso para suspender matrícula | `5` |

## Deploy em produção (Docker)

### Build e subida completa

```bash
# 1. Copiar e preencher variáveis de ambiente
cp .env.example .env
# edite .env com os valores reais

# 2. Subir todos os serviços
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

A stack completa sobe 3 containers:

| Serviço | Porta | Função |
|---------|-------|--------|
| `frontend` | `${PORT:-80}` | nginx serve os assets Vue + proxy `/api/` → backend |
| `backend` | interno | Spring Boot 3 |
| `db` | interno | PostgreSQL 16 |

O frontend fica acessível em `http://seu-servidor:80`.  
O Swagger UI também fica disponível via `http://seu-servidor/swagger-ui.html` (proxy nginx).

### Volumes persistentes

| Volume | Conteúdo |
|--------|----------|
| `postgres_data` | Dados do banco |
| `uploads` | Fotos de progresso e anexos |

### Reverse proxy (recomendado)

Para HTTPS, coloque um reverse proxy (Nginx/Caddy/Traefik) na frente do container `frontend` na porta 80.  
Exemplo com Caddy (`Caddyfile`):

```
app.wachafit.com {
    reverse_proxy localhost:80
}
```

## Rodando os testes

```bash
cd backend
mvn test
```

Os testes de integração sobem o banco via **Testcontainers** (requer Docker em execução).  
Atualmente a suíte tem **177 testes** cobrindo todas as regras de negócio (RN-01 a RN-12).

## Arquitetura

```
backend/
├── src/main/java/com/github/mwacha/wachafit/
│   ├── assessment/       # Avaliações físicas
│   ├── auth/             # Login, registro, reset de senha
│   ├── billing/          # Cobranças e gateway de pagamento
│   ├── booking/          # Agendamentos (RN-01..06)
│   ├── config/           # Security, CORS, OpenAPI
│   ├── exercise/         # Biblioteca de exercícios
│   ├── goal/             # Metas dos alunos
│   ├── groupclass/       # Turmas coletivas
│   ├── membership/       # Planos e matrículas
│   ├── notification/     # Email assíncrono + eventos
│   ├── pdf/              # Geração de relatórios PDF
│   ├── profile/          # Perfil de alunos
│   ├── progress/         # Fotos de progresso
│   ├── report/           # Relatórios gerenciais
│   ├── schedule/         # Horários (CLASS / PERSONAL)
│   ├── shared/           # Exceções, segurança JWT
│   ├── user/             # Gestão de usuários
│   └── workout/          # Fichas de treino e logs
└── src/main/resources/
    ├── db/migration/     # Migrações Flyway (V1..V23)
    └── templates/        # Templates Thymeleaf (email + PDF)

frontend/
└── src/
    ├── components/       # AppLayout
    ├── router/           # Rotas com guards por role
    ├── services/         # Clientes HTTP (Axios)
    ├── stores/           # Estado global (Pinia)
    ├── types/            # Tipos TypeScript da API
    └── views/            # Views por role (admin/cashier/receptionist/trainer/student)
```

## Roles e acessos

| Role | Acesso principal |
|------|-----------------|
| `ADMIN` | Tudo |
| `MANAGER` | Planos, relatórios financeiros |
| `CASHIER` | Cobranças, fluxo de caixa |
| `RECEPTIONIST` | Matrículas, cobranças |
| `TRAINER` | Agenda, alunos, fichas de treino |
| `STUDENT` | Agenda, treino, evolução, financeiro próprio |

## API

Todos os endpoints protegidos exigem `Authorization: Bearer <token>`.  
Obtenha o token em `POST /api/auth/login`.

Documentação interativa completa: `http://localhost:8080/swagger-ui.html`
