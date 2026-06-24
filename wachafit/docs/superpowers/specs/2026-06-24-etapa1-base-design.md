# Design — Wachafit Etapa 1: Base

**Data:** 2026-06-24  
**Escopo:** T-01 a T-07 (infra, shared, usuário, segurança/JWT, autenticação, bootstrap frontend, guards por role)  
**Critério de aceite:** usuário cadastra, loga e navega com controle por perfil  
**Documentos de referência:** PRD.md (RF-01, RNF-03/04/05), DOCUMENTO_TECNICO.md, PLANO_DE_EXECUCAO.md

---

## Decisões arquiteturais

| Decisão | Escolha | Motivo |
|---------|---------|--------|
| Estrutura de repo | Monorepo (`backend/` + `frontend/` + `docker-compose.yml` na raiz) | Simplicidade para MVP; um único contexto de CI |
| Token JWT | Access token único, HS256, expiração 1h | Sem refresh token no MVP; 401 → logout |
| Storage do token (frontend) | `localStorage` + Pinia como cache em memória | Persiste entre reloads; risco XSS aceitável em domínio controlado |
| UserDetails | `User implements UserDetails` diretamente | Padrão Spring Security; `isEnabled()` aplica RN-07 imediatamente |
| Subject do JWT | `sub = UUID do usuário` | UUID é mais estável que email; UserDetailsService usa `findById` |
| JwtFilter | Approach B: carrega User do banco via UserDetailsService a cada request | Garante RN-07 em cada chamada; query única por request, negligenciável no volume do MVP |

---

## Seção 1: Layout do projeto e infra local (T-01)

### Estrutura de diretórios

```
wachafit/
├── backend/
│   ├── pom.xml
│   └── src/
├── frontend/
│   ├── package.json
│   └── src/
├── docker-compose.yml
├── PRD.md
├── DOCUMENTO_TECNICO.md
└── PLANO_DE_EXECUCAO.md
```

### docker-compose.yml

Dois serviços apenas (backend e frontend rodam localmente em dev):

| Serviço | Imagem | Porta |
|---------|--------|-------|
| `db` | `postgres:16` | 5432 |
| `mailhog` | `mailhog/mailhog` | SMTP:1025, UI:8025 |

Volume nomeado para persistência do Postgres em dev.

### Configuração Spring Boot

- `application.yml` — lê tudo de variáveis de ambiente (`${DB_URL}`, `${JWT_SECRET}`, etc.)
- `application-dev.yml` — defaults para desenvolvimento local:
  - `DB_URL: jdbc:postgresql://localhost:5432/wachafit`
  - `JWT_SECRET: dev-secret-change-in-production`
  - `JWT_EXPIRATION: 3600` (segundos)
  - `MAIL_HOST: localhost`, `MAIL_PORT: 1025`
  - `APP_FRONTEND_URL: http://localhost:5173`
- Profile `dev` ativado via `SPRING_PROFILES_ACTIVE=dev`

### Dependências Maven (pom.xml)

```
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-starter-mail
flyway-core
postgresql (runtime)
jjwt-api + jjwt-impl + jjwt-jackson
springdoc-openapi-starter-webmvc-ui
lombok (optional)
spring-boot-starter-test + testcontainers (test scope)
```

---

## Seção 2: Shared — exceções e DTOs (T-02)

Pacote: `com.github.mwacha.wachafit.shared.exception`

| Classe | Tipo | HTTP |
|--------|------|------|
| `BusinessException` | `RuntimeException` | 409 Conflict |
| `NotFoundException` | `RuntimeException` | 404 Not Found |
| `ForbiddenException` | `RuntimeException` | 403 Forbidden |
| `ErrorResponse` | record | — |
| `GlobalExceptionHandler` | `@RestControllerAdvice` | — |

### ErrorResponse (record)

```java
record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {}
```

### GlobalExceptionHandler — mapeamentos

| Exceção capturada | Status | `error` |
|-------------------|--------|---------|
| `NotFoundException` | 404 | "Not Found" |
| `BusinessException` | 409 | "Conflict" |
| `ForbiddenException` | 403 | "Forbidden" |
| `MethodArgumentNotValidException` | 400 | "Bad Request" |
| `Exception` (fallback) | 500 | "Internal Server Error" |

Para `MethodArgumentNotValidException`, o campo `message` lista os erros de campo separados por ponto-e-vírgula.

---

## Seção 3: Entidade User e migration (T-03)

### V1__create_users.sql

```sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

### User.java

- `@Entity @Table(name = "users")`
- `@Id @GeneratedValue(strategy = GenerationType.UUID)` — UUID gerado pela aplicação (Hibernate 6+)
- `@Column(columnDefinition = "uuid", updatable = false, nullable = false)`
- Implementa `UserDetails`:
  - `getUsername()` → `email`
  - `getPassword()` → `passwordHash`
  - `isEnabled()` → `active` — **aplica RN-07 a cada request autenticado**
  - `getAuthorities()` → `List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))`
  - `getAccountNonExpired()`, `getAccountNonLocked()`, `isCredentialsNonExpired()` → `true`

### Role.java

```java
public enum Role {
    ADMIN, TRAINER, STUDENT
}
```

Persistido como `@Enumerated(EnumType.STRING)`.

### UserRepository.java

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## Seção 4: Segurança e JWT (T-04)

### JwtUtil (`shared/security/`)

- Biblioteca: `jjwt` (io.jsonwebtoken)
- Algoritmo: HS256
- Claims gerados: `sub` (UUID como String), `role` (nome do enum), `exp` (now + expiration)
- Métodos: `generateToken(User)`, `extractUserId(token): UUID`, `extractRole(token): String`, `isTokenValid(token): boolean`
- Secret e expiração injetados via `@Value`

### JwtFilter (`shared/security/`), extends `OncePerRequestFilter`

Fluxo por request:

```
1. Ler header "Authorization: Bearer <token>"
2. Se ausente ou malformado → chain.doFilter() (Spring Security tratará como anônimo)
3. Validar token via JwtUtil (assinatura + expiração)
4. Extrair userId (UUID) do claim sub
5. Carregar User via UserDetailsService.loadUserById(userId)
6. Criar UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
7. Setar no SecurityContextHolder
8. chain.doFilter()
```

Se o token for inválido (expirado, assinatura incorreta) → `SecurityContextHolder` fica vazio → Spring Security retorna 401 automaticamente para rotas protegidas.

### UserDetailsServiceImpl (`user/`)

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    // loadUserByUsername recebe UUID como string (chamado pelo JwtFilter)
    @Override
    public UserDetails loadUserByUsername(String userId) {
        return userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
```

### SecurityConfig (`config/`)

```
@EnableWebSecurity
@EnableMethodSecurity  // habilita @PreAuthorize

SessionCreationPolicy: STATELESS
Rotas públicas (permitAll):
  POST /api/auth/**
  GET  /swagger-ui/**
  GET  /v3/api-docs/**
Demais rotas: authenticated()

Beans: BCryptPasswordEncoder, AuthenticationManager
```

### CorsConfig (`config/`)

Origem permitida: `${APP_FRONTEND_URL}` (padrão `http://localhost:5173`)  
Métodos: GET, POST, PUT, PATCH, DELETE, OPTIONS  
Headers: Authorization, Content-Type

---

## Seção 5: Fluxo de autenticação (T-05)

### V2__create_password_reset_tokens.sql

```sql
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT false
);
```

### AuthController (`auth/`)

| Método | Rota | DTO entrada | DTO saída |
|--------|------|-------------|-----------|
| POST | `/api/auth/register` | `RegisterRequest` | `LoginResponse` |
| POST | `/api/auth/login` | `LoginRequest` | `LoginResponse` |
| POST | `/api/auth/forgot-password` | `ForgotPasswordRequest` | — (204) |
| POST | `/api/auth/reset-password` | `ResetPasswordRequest` | — (204) |

`LoginResponse`:
```java
record LoginResponse(String token, String role, String userId) {}
```

### AuthService — lógica por endpoint

**register:**
1. Verificar unicidade do email (`existsByEmail`) → `BusinessException` se duplicado
2. Hash da senha com BCrypt
3. Salvar `User` com `role = STUDENT`, `active = true`
4. Gerar JWT e retornar `LoginResponse`

**login:**
1. Buscar User por email → `BusinessException("Credenciais inválidas")` se não encontrado
2. Verificar `active` → `BusinessException("Usuário inativo")` se false (RN-07)
3. Verificar senha com BCrypt → `BusinessException("Credenciais inválidas")` se incorreta
4. Gerar JWT e retornar `LoginResponse`

**forgot-password:**
1. Buscar User por email (silencioso se não encontrado — não revelar existência)
2. Criar `PasswordResetToken` com token UUID aleatório, `expiresAt = now + 30min`
3. Enviar e-mail com link `APP_FRONTEND_URL/reset-password?token=...`

**reset-password:**
1. Buscar token → `BusinessException` se não encontrado
2. Verificar `used == false` → `BusinessException("Token já utilizado")`
3. Verificar `expiresAt > now()` → `BusinessException("Token expirado")`
4. Atualizar senha do usuário com novo hash BCrypt
5. Marcar token como `used = true`

### DTOs com Bean Validation

```java
record RegisterRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @Size(min = 8) String password
) {}

record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
record ForgotPasswordRequest(@Email @NotBlank String email) {}
record ResetPasswordRequest(@NotBlank String token, @Size(min = 8) String newPassword) {}
```

---

## Seção 6: Frontend bootstrap (T-06)

### Estrutura de diretórios

```
frontend/src/
├── components/
├── views/
│   └── auth/
│       ├── LoginView.vue
│       └── RegisterView.vue
├── stores/
│   └── auth.store.ts
├── services/
│   └── api.ts
├── router/
│   └── index.ts
├── composables/
├── types/
│   └── api.ts          ← interfaces alinhadas com o contrato da API
└── App.vue
    main.ts
```

### services/api.ts

```typescript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

// Request: injeta Bearer token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response: 401 → logout
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)
```

### Pacotes npm

```
pinia
vue-router@4
axios
primevue@4
@primevue/themes
primeicons
```

### main.ts

Registra PrimeVue (tema Aura), Pinia, Router em `createApp(App)`.

---

## Seção 7: Auth store e guards (T-07)

### stores/auth.store.ts (Pinia)

**Estado:**

```typescript
interface AuthState {
  token: string | null
  userId: string | null
  role: 'ADMIN' | 'TRAINER' | 'STUDENT' | null
}
```

**Hidratação:** no `$patch` inicial, lê `localStorage.getItem('token')`. Decodifica o payload JWT (base64) para extrair `sub` (userId) e `role` — sem dependência de lib extra.

**Ações:**

| Ação | Comportamento |
|------|---------------|
| `login(credentials)` | Chama `POST /api/auth/login`; salva token + userId + role em localStorage e estado |
| `logout()` | Limpa localStorage e estado; redireciona para `/login` |
| `register(data)` | Chama `POST /api/auth/register`; comportamento igual ao login após cadastro |

**Getter:** `isAuthenticated: boolean`, `userRole: Role | null`

### router/index.ts — guards

`beforeEach`:
1. Rota tem `meta.requiresAuth = true` e store não tem token → redireciona para `/login`
2. Rota tem `meta.roles: Role[]` e role do usuário não está na lista → redireciona para `/unauthorized`
3. Usuário autenticado acessa `/login` ou `/register` → redireciona ao dashboard do seu role

**Redirecionamento pós-login por role:**

| Role | Rota |
|------|------|
| ADMIN | `/admin` |
| TRAINER | `/trainer` |
| STUDENT | `/student` |

### Telas criadas em T-07

**LoginView** (`views/auth/LoginView.vue`):
- Formulário: email + senha
- Validação de campos no frontend (não vazio, formato email)
- Exibe mensagem de erro em caso de credenciais inválidas (sem revelar qual campo está errado)
- Link para `/register` e `/forgot-password`

**RegisterView** (`views/auth/RegisterView.vue`):
- Formulário: nome + email + senha + confirmação de senha
- Após cadastro bem-sucedido, auto-login e redireciona ao dashboard do STUDENT

**UnauthorizedView** (`views/UnauthorizedView.vue`):
- Página 403 simples com link de volta ao dashboard do usuário

**AppLayout.vue** (`components/AppLayout.vue`):
- Navbar com nome do usuário + botão logout
- `<RouterView />` como slot principal
- Placeholder: navegação completa vem nas Etapas 2–3

---

## Regras de negócio cobertas nesta etapa

| Regra | Onde aplicada |
|-------|---------------|
| RN-07: usuário desativado não autentica | `AuthService.login()` + `User.isEnabled()` via UserDetailsService a cada request |
| RF-01.3: register público só cria STUDENT | `AuthService.register()` — role fixo |
| RF-01.6: controle de acesso por rota | Router guards + `@PreAuthorize` (estrutura pronta para Etapa 2) |

---

## O que está fora do escopo desta etapa

- CRUD de usuários pelo admin (T-09, Etapa 2)
- Gestão de TRAINER/ADMIN pelo admin (T-09)
- Qualquer funcionalidade de agendamento, turmas ou acompanhamento (Etapas 2–3)
- Notificações por e-mail além do reset de senha (Etapa 4)
