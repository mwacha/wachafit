# WachaFit Etapa 4A — EmailService com Notificações HTML

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Centralizar envio de e-mail HTML em um `EmailService` com Thymeleaf, adicionar notificações de boas-vindas, confirmação/cancelamento de booking e lembrete 4h antes da sessão, migrando o `AuthService` para usar o novo serviço.

**Architecture:** Novo pacote `notification/` com `EmailService` (sendHtml genérico) e `ReminderScheduler` (@Scheduled). `BookingService`, `AuthService` e `UserService` injetam `EmailService`. Templates Thymeleaf em `resources/templates/email/`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Mail, Thymeleaf, Mockito, GreenMail (testes), Testcontainers

## Global Constraints

- Projeto: `/Users/marceloferreira/developer/wachafit/`
- Backend Maven: `cd /Users/marceloferreira/developer/wachafit/backend && mvn ...`
- Package root: `com.github.mwacha.wachafit`
- Sem Lombok — getters/setters manuais
- TDD: testes falhos commitados antes da implementação
- `@ActiveProfiles("test")` em testes de integração
- Erros de envio de e-mail devem ser **logados mas não propagados** (não bloquear fluxo principal)
- `spring-boot-starter-mail` já está no `pom.xml`
- `springdoc-openapi` já está no `pom.xml`

---

## Task 1: EmailService + dependências

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/notification/EmailService.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/notification/EmailServiceTest.java`

**Interfaces:**
- Produces: `EmailService.sendHtml(String to, String subject, String templateName, Map<String,Object> vars) → void`

- [ ] **Step 1: Adicionar dependências no pom.xml**

Abrir `backend/pom.xml` e adicionar dentro de `<dependencies>`:

```xml
<!-- Thymeleaf para templates de e-mail HTML -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- GreenMail para testes de envio de e-mail -->
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-junit5</artifactId>
    <version>2.1.2</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Escrever teste unitário falho**

```java
// backend/src/test/java/com/github/mwacha/wachafit/notification/EmailServiceTest.java
package com.github.mwacha.wachafit.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock TemplateEngine templateEngine;
    @InjectMocks EmailService service;

    @Test
    void sendHtml_shouldRenderTemplateAndSend() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(eq("email/welcome"), any(Context.class)))
            .thenReturn("<html><body>Olá João</body></html>");

        service.sendHtml("joao@test.com", "Bem-vindo!", "email/welcome", Map.of("name", "João"));

        verify(templateEngine).process(eq("email/welcome"), any(Context.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendHtml_shouldNotThrow_whenMailSenderFails() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(any(), any())).thenReturn("<html></html>");
        doThrow(new RuntimeException("SMTP indisponível")).when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
            service.sendHtml("joao@test.com", "Assunto", "email/welcome", Map.of()));
    }
}
```

- [ ] **Step 3: Confirmar falha de compilação**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test -Dtest=EmailServiceTest 2>&1 | tail -5
```

Expected: erro de compilação (`EmailService` não existe).

- [ ] **Step 4: Criar EmailService**

```java
// backend/src/main/java/com/github/mwacha/wachafit/notification/EmailService.java
package com.github.mwacha.wachafit.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendHtml(String to, String subject, String templateName, Map<String, Object> vars) {
        try {
            Context ctx = new Context();
            ctx.setVariables(vars);
            String html = templateEngine.process(templateName, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}: {}", to, e.getMessage());
        }
    }
}
```

- [ ] **Step 5: Rodar testes**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test -Dtest=EmailServiceTest 2>&1 | tail -8
```

Expected: `Tests run: 2, Failures: 0, Errors: 0` — `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git -C /Users/marceloferreira/developer/wachafit checkout -b feat/etapa4a-email
git -C /Users/marceloferreira/developer/wachafit add \
  backend/pom.xml \
  backend/src/main/java/com/github/mwacha/wachafit/notification/EmailService.java \
  backend/src/test/java/com/github/mwacha/wachafit/notification/EmailServiceTest.java
git -C /Users/marceloferreira/developer/wachafit commit -m "feat: EmailService centralizado com Thymeleaf HTML"
```

---

## Task 2: Templates HTML de e-mail

**Files:**
- Create: `backend/src/main/resources/templates/email/welcome.html`
- Create: `backend/src/main/resources/templates/email/booking-confirmed.html`
- Create: `backend/src/main/resources/templates/email/booking-cancelled.html`
- Create: `backend/src/main/resources/templates/email/session-reminder.html`
- Create: `backend/src/main/resources/templates/email/password-reset.html`

**Interfaces:**
- Consumes: `EmailService.sendHtml(...)` — templateName mapeia para `email/<nome>`
- Variáveis por template:
  - `welcome`: `name`
  - `booking-confirmed`: `name`, `className`, `date`, `time`, `trainerName`
  - `booking-cancelled`: `name`, `className`, `date`, `time`
  - `session-reminder`: `name`, `className`, `date`, `time`, `trainerName`
  - `password-reset`: `name`, `resetLink`

- [ ] **Step 1: Criar welcome.html**

```html
<!-- backend/src/main/resources/templates/email/welcome.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Bem-vindo ao WachaFit</title></head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
  <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
    <div style="background:#1a73e8;padding:24px;text-align:center;">
      <h1 style="color:#fff;margin:0;">WachaFit</h1>
    </div>
    <div style="padding:32px;">
      <h2>Bem-vindo, <span th:text="${name}">Aluno</span>!</h2>
      <p>Sua conta foi criada com sucesso. Acesse a plataforma e comece a agendar suas aulas.</p>
      <p>Bons treinos!</p>
    </div>
    <div style="background:#f4f4f4;padding:16px;text-align:center;color:#666;font-size:12px;">
      <p>© 2026 WachaFit</p>
    </div>
  </div>
</body>
</html>
```

- [ ] **Step 2: Criar booking-confirmed.html**

```html
<!-- backend/src/main/resources/templates/email/booking-confirmed.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Agendamento Confirmado</title></head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
  <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
    <div style="background:#1a73e8;padding:24px;text-align:center;">
      <h1 style="color:#fff;margin:0;">WachaFit</h1>
    </div>
    <div style="padding:32px;">
      <h2>Agendamento confirmado!</h2>
      <p>Olá, <strong th:text="${name}">Aluno</strong>!</p>
      <p>Seu agendamento foi confirmado com sucesso:</p>
      <table style="width:100%;border-collapse:collapse;margin:16px 0;">
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Aula</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${className}">Musculação</td></tr>
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Data</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${date}">2026-07-01</td></tr>
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Horário</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${time}">08:00</td></tr>
        <tr><td style="padding:8px;color:#666;">Profissional</td>
            <td style="padding:8px;" th:text="${trainerName}">Personal</td></tr>
      </table>
      <p>Até lá!</p>
    </div>
    <div style="background:#f4f4f4;padding:16px;text-align:center;color:#666;font-size:12px;">
      <p>© 2026 WachaFit</p>
    </div>
  </div>
</body>
</html>
```

- [ ] **Step 3: Criar booking-cancelled.html**

```html
<!-- backend/src/main/resources/templates/email/booking-cancelled.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Agendamento Cancelado</title></head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
  <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
    <div style="background:#e53935;padding:24px;text-align:center;">
      <h1 style="color:#fff;margin:0;">WachaFit</h1>
    </div>
    <div style="padding:32px;">
      <h2>Agendamento cancelado</h2>
      <p>Olá, <strong th:text="${name}">Aluno</strong>!</p>
      <p>Seu agendamento foi cancelado:</p>
      <table style="width:100%;border-collapse:collapse;margin:16px 0;">
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Aula</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${className}">Musculação</td></tr>
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Data</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${date}">2026-07-01</td></tr>
        <tr><td style="padding:8px;color:#666;">Horário</td>
            <td style="padding:8px;" th:text="${time}">08:00</td></tr>
      </table>
      <p>Para reagendar, acesse a plataforma.</p>
    </div>
    <div style="background:#f4f4f4;padding:16px;text-align:center;color:#666;font-size:12px;">
      <p>© 2026 WachaFit</p>
    </div>
  </div>
</body>
</html>
```

- [ ] **Step 4: Criar session-reminder.html**

```html
<!-- backend/src/main/resources/templates/email/session-reminder.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Lembrete de Sessão</title></head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
  <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
    <div style="background:#f57c00;padding:24px;text-align:center;">
      <h1 style="color:#fff;margin:0;">WachaFit</h1>
    </div>
    <div style="padding:32px;">
      <h2>Sua sessão começa em 4 horas!</h2>
      <p>Olá, <strong th:text="${name}">Aluno</strong>! Este é um lembrete da sua sessão de hoje:</p>
      <table style="width:100%;border-collapse:collapse;margin:16px 0;">
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Aula</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${className}">Musculação</td></tr>
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Data</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${date}">2026-07-01</td></tr>
        <tr><td style="padding:8px;border-bottom:1px solid #eee;color:#666;">Horário</td>
            <td style="padding:8px;border-bottom:1px solid #eee;" th:text="${time}">08:00</td></tr>
        <tr><td style="padding:8px;color:#666;">Profissional</td>
            <td style="padding:8px;" th:text="${trainerName}">Personal</td></tr>
      </table>
      <p>Prepare-se e até logo!</p>
    </div>
    <div style="background:#f4f4f4;padding:16px;text-align:center;color:#666;font-size:12px;">
      <p>© 2026 WachaFit</p>
    </div>
  </div>
</body>
</html>
```

- [ ] **Step 5: Criar password-reset.html**

```html
<!-- backend/src/main/resources/templates/email/password-reset.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>Redefinição de Senha</title></head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
  <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;">
    <div style="background:#1a73e8;padding:24px;text-align:center;">
      <h1 style="color:#fff;margin:0;">WachaFit</h1>
    </div>
    <div style="padding:32px;">
      <h2>Redefinição de senha</h2>
      <p>Olá, <strong th:text="${name}">Usuário</strong>!</p>
      <p>Recebemos uma solicitação de redefinição de senha para sua conta. Clique no botão abaixo:</p>
      <div style="text-align:center;margin:32px 0;">
        <a th:href="${resetLink}" style="background:#1a73e8;color:#fff;padding:14px 28px;border-radius:4px;text-decoration:none;font-weight:bold;">
          Redefinir senha
        </a>
      </div>
      <p style="color:#666;font-size:13px;">Este link expira em 30 minutos. Se você não solicitou a redefinição, ignore este e-mail.</p>
    </div>
    <div style="background:#f4f4f4;padding:16px;text-align:center;color:#666;font-size:12px;">
      <p>© 2026 WachaFit</p>
    </div>
  </div>
</body>
</html>
```

- [ ] **Step 6: Rodar suíte completa para verificar que nada quebrou**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test 2>&1 | tail -8
```

Expected: `BUILD SUCCESS` com a mesma contagem de testes anterior.

- [ ] **Step 7: Commit**

```bash
git -C /Users/marceloferreira/developer/wachafit add \
  backend/src/main/resources/templates/
git -C /Users/marceloferreira/developer/wachafit commit -m "feat: templates HTML Thymeleaf para e-mails"
```

---

## Task 3: Migrar AuthService + e-mail de boas-vindas

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/AuthService.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/UserService.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/auth/AuthServiceTest.java` (se existir) ou `AuthControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/user/UserServiceTest.java`

**Interfaces:**
- Consumes: `EmailService.sendHtml(String, String, String, Map<String,Object>)`
- `AuthService.register()` → envia `email/welcome` após salvar usuário
- `AuthService.forgotPassword()` → envia `email/password-reset` via `EmailService` (substitui `SimpleMailMessage`)
- `UserService.createUser()` → envia `email/welcome` após salvar usuário

- [ ] **Step 1: Atualizar AuthService**

Substituir o conteúdo de `AuthService.java`:

```java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.auth.dto.*;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.UnauthorizedException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        JwtUtil jwtUtil,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            saved.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", saved.getName())
        );
        String token = jwtUtil.generateToken(saved);
        return new LoginResponse(token, saved.getRole().name(), saved.getId().toString());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (!user.isActive()) {
            throw new UnauthorizedException("Usuário inativo");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getRole().name(), user.getId().toString());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            tokenRepository.save(resetToken);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendHtml(
                user.getEmail(),
                "Redefinição de senha — WachaFit",
                "email/password-reset",
                Map.of("name", user.getName(), "resetLink", resetLink)
            );
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
            .orElseThrow(() -> new BusinessException("Token inválido"));
        if (resetToken.isUsed()) {
            throw new BusinessException("Token já utilizado");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expirado");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
    }
}
```

- [ ] **Step 2: Atualizar UserService — injetar EmailService e enviar boas-vindas**

Substituir o conteúdo de `UserService.java`:

```java
package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UpdateUserRequest;
import com.github.mwacha.wachafit.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String role, Boolean active) {
        return userRepository.findAll().stream()
            .filter(u -> role == null || u.getRole().name().equals(role))
            .filter(u -> active == null || u.isActive() == active)
            .map(this::toResponse)
            .toList();
    }

    public UserResponse createUser(CreateUserRequest req) {
        if (req.role() == Role.STUDENT) {
            throw new BusinessException("Não é permitido criar usuário com role STUDENT por este endpoint");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("E-mail já cadastrado");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        User saved = userRepository.save(user);
        emailService.sendHtml(
            saved.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", saved.getName())
        );
        return toResponse(saved);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = findOrThrow(id);
        user.setName(req.name());
        user.setRole(req.role());
        return toResponse(userRepository.save(user));
    }

    public void deactivateUser(UUID id, UUID currentUserId) {
        User user = findOrThrow(id);
        if (user.getRole() == Role.STUDENT) {
            throw new BusinessException("Cannot deactivate a student user");
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException("Não é possível desativar a própria conta");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(UUID id) {
        User user = findOrThrow(id);
        user.setActive(true);
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
            u.getId().toString(), u.getName(), u.getEmail(),
            u.getRole().name(), u.isActive(),
            u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }
}
```

- [ ] **Step 3: Atualizar UserServiceTest para mockar EmailService**

```java
// backend/src/test/java/com/github/mwacha/wachafit/user/UserServiceTest.java
package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, emailService);
    }

    @Test
    void createUser_shouldCreateTrainer() {
        when(userRepository.existsByEmail("trainer@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(u, UUID.randomUUID()); }
            catch (Exception e) { throw new RuntimeException(e); }
            return u;
        });

        UserResponse result = userService.createUser(
            new CreateUserRequest("João Trainer", "trainer@example.com", "senha123", Role.TRAINER));

        assertThat(result.role()).isEqualTo("TRAINER");
        assertThat(result.active()).isTrue();
        verify(userRepository).save(argThat(u -> u.getRole() == Role.TRAINER));
        verify(emailService).sendHtml(eq("trainer@example.com"), contains("Bem-vindo"), eq("email/welcome"), anyMap());
    }

    @Test
    void createUser_shouldRejectStudentRole() {
        assertThatThrownBy(() -> userService.createUser(
            new CreateUserRequest("Student", "s@example.com", "senha123", Role.STUDENT)))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createUser_shouldThrow_whenEmailExists() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser(
            new CreateUserRequest("Dup", "dup@example.com", "senha123", Role.TRAINER)))
            .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 4: Rodar testes**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test -Dtest=UserServiceTest,AuthControllerIntegrationTest 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git -C /Users/marceloferreira/developer/wachafit add \
  backend/src/main/java/com/github/mwacha/wachafit/auth/AuthService.java \
  backend/src/main/java/com/github/mwacha/wachafit/user/UserService.java \
  backend/src/test/java/com/github/mwacha/wachafit/user/UserServiceTest.java
git -C /Users/marceloferreira/developer/wachafit commit -m "feat: e-mail de boas-vindas + migração AuthService para EmailService"
```

---

## Task 4: E-mails de confirmação e cancelamento de booking

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/booking/BookingService.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/booking/BookingServiceTest.java`

**Interfaces:**
- Consumes: `EmailService.sendHtml(...)`, `UserRepository.findById(UUID)`
- `BookingService.createBooking()` → envia `email/booking-confirmed` após salvar booking com status CONFIRMED
- `BookingService.cancelBooking()` → envia `email/booking-cancelled` após cancelar

- [ ] **Step 1: Atualizar BookingService — injetar EmailService e UserRepository**

Substituir o conteúdo de `BookingService.java`:

```java
package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.booking.dto.BookingResponse;
import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleStatus;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository,
                          ScheduleRepository scheduleRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public BookingResponse createBooking(CreateBookingRequest req, UUID studentId) {
        Schedule schedule = scheduleRepository.findById(req.scheduleId())
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado"));

        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessException("Este schedule foi cancelado");
        }
        if (schedule.getStatus() == ScheduleStatus.FULL) {
            throw new BusinessException("Turma lotada");
        }

        long studentOverlap = bookingRepository.countStudentOverlaps(
            studentId, schedule.getStartsAt(), schedule.getEndsAt());
        if (studentOverlap > 0) {
            throw new BusinessException("Você já tem um agendamento neste horário (conflito)");
        }

        Schedule locked = scheduleRepository.findByIdForUpdate(req.scheduleId())
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado"));

        if (locked.getType() == ScheduleType.CLASS) {
            int capacity = locked.getGroupClass().getCapacity();
            long confirmed = bookingRepository.countConfirmedBookings(locked.getId());
            if (confirmed >= capacity) {
                throw new BusinessException("Turma lotada");
            }
            if (confirmed + 1 >= capacity) {
                locked.setStatus(ScheduleStatus.FULL);
                scheduleRepository.save(locked);
            }
        }

        BookingStatus status = locked.getType() == ScheduleType.CLASS
            ? BookingStatus.CONFIRMED
            : BookingStatus.PENDING;

        Booking booking = new Booking();
        booking.setSchedule(locked);
        booking.setStudentId(studentId);
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);

        if (status == BookingStatus.CONFIRMED) {
            sendBookingConfirmedEmail(studentId, locked);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID studentId) {
        return bookingRepository.findByStudentIdOrderByBookedAtDesc(studentId)
            .stream().map(this::toResponse).toList();
    }

    public void cancelBooking(UUID bookingId, UUID requestingUserId, Role requestingRole) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking não encontrado"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking já cancelado");
        }

        if (requestingRole == Role.STUDENT && !booking.getStudentId().equals(requestingUserId)) {
            throw new ForbiddenException("Access denied");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Schedule schedule = booking.getSchedule();
        if (schedule.getStatus() == ScheduleStatus.FULL) {
            schedule.setStatus(ScheduleStatus.OPEN);
            scheduleRepository.save(schedule);
        }
        bookingRepository.save(booking);

        sendBookingCancelledEmail(booking.getStudentId(), schedule);
    }

    private void sendBookingConfirmedEmail(UUID studentId, Schedule schedule) {
        userRepository.findById(studentId).ifPresent(student ->
            userRepository.findById(schedule.getTrainerId()).ifPresent(trainer ->
                emailService.sendHtml(
                    student.getEmail(),
                    "Agendamento confirmado — WachaFit",
                    "email/booking-confirmed",
                    Map.of(
                        "name", student.getName(),
                        "className", schedule.getGroupClass() != null
                            ? schedule.getGroupClass().getName() : "Sessão individual",
                        "date", schedule.getStartsAt().toLocalDate().toString(),
                        "time", schedule.getStartsAt().toLocalTime().toString(),
                        "trainerName", trainer.getName()
                    )
                )
            )
        );
    }

    private void sendBookingCancelledEmail(UUID studentId, Schedule schedule) {
        userRepository.findById(studentId).ifPresent(student ->
            emailService.sendHtml(
                student.getEmail(),
                "Agendamento cancelado — WachaFit",
                "email/booking-cancelled",
                Map.of(
                    "name", student.getName(),
                    "className", schedule.getGroupClass() != null
                        ? schedule.getGroupClass().getName() : "Sessão individual",
                    "date", schedule.getStartsAt().toLocalDate().toString(),
                    "time", schedule.getStartsAt().toLocalTime().toString()
                )
            )
        );
    }

    private BookingResponse toResponse(Booking b) {
        Schedule s = b.getSchedule();
        return new BookingResponse(
            b.getId().toString(),
            s.getId().toString(),
            s.getStartsAt().toString(),
            s.getEndsAt().toString(),
            s.getType().name(),
            b.getStatus().name(),
            s.getGroupClass() != null ? s.getGroupClass().getName() : null,
            null,
            b.getBookedAt() != null ? b.getBookedAt().toString() : null
        );
    }
}
```

- [ ] **Step 2: Atualizar BookingServiceTest — adicionar mocks de UserRepository e EmailService**

Localizar o arquivo existente em `backend/src/test/java/com/github/mwacha/wachafit/booking/BookingServiceTest.java` e adicionar:

```java
// Adicionar no topo dos campos @Mock:
@Mock UserRepository userRepository;
@Mock EmailService emailService;
```

Atualizar a inicialização do service no `@BeforeEach`:

```java
@BeforeEach
void setUp() {
    service = new BookingService(bookingRepository, scheduleRepository, userRepository, emailService);
    // ... resto do setUp existente
}
```

Adicionar teste de verificação de e-mail:

```java
@Test
void createBooking_shouldSendConfirmationEmail_whenClassBooking() {
    // Reusar setup de booking CLASS existente nos testes, adicionar:
    UUID studentId = UUID.randomUUID();
    UUID trainerId = UUID.randomUUID();

    Schedule schedule = buildSchedule(UUID.randomUUID(), ScheduleType.CLASS, ScheduleStatus.OPEN, 10);
    schedule.setTrainerId(trainerId);

    User student = new User(); student.setName("Aluno"); student.setEmail("aluno@test.com");
    User trainer = new User(); trainer.setName("Trainer");

    when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
    when(scheduleRepository.findByIdForUpdate(schedule.getId())).thenReturn(Optional.of(schedule));
    when(bookingRepository.countStudentOverlaps(any(), any(), any())).thenReturn(0L);
    when(bookingRepository.countConfirmedBookings(any())).thenReturn(0L);
    when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

    service.createBooking(new CreateBookingRequest(schedule.getId()), studentId);

    verify(emailService).sendHtml(
        eq("aluno@test.com"),
        contains("confirmado"),
        eq("email/booking-confirmed"),
        anyMap()
    );
}
```

- [ ] **Step 3: Rodar testes de booking**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test -Dtest=BookingServiceTest,BookingConcurrencyTest 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git -C /Users/marceloferreira/developer/wachafit add \
  backend/src/main/java/com/github/mwacha/wachafit/booking/BookingService.java \
  backend/src/test/java/com/github/mwacha/wachafit/booking/BookingServiceTest.java
git -C /Users/marceloferreira/developer/wachafit commit -m "feat: e-mails de confirmação e cancelamento de booking"
```

---

## Task 5: ReminderScheduler

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/booking/BookingRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/notification/ReminderScheduler.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/WachafitApplication.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/notification/ReminderSchedulerTest.java`

**Interfaces:**
- Consumes: `BookingRepository.findConfirmedBetween(OffsetDateTime, OffsetDateTime)`, `UserRepository.findById(UUID)`, `EmailService.sendHtml(...)`
- `ReminderScheduler.sendReminders()` → executa a cada hora, envia `email/session-reminder` para bookings CONFIRMED com `startsAt` entre now+3h e now+5h

- [ ] **Step 1: Adicionar query ao BookingRepository**

```java
// Adicionar em BookingRepository.java:

@Query("""
    SELECT b FROM Booking b
    WHERE b.status = com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED
      AND b.schedule.startsAt >= :from
      AND b.schedule.startsAt <= :to
""")
List<Booking> findConfirmedBetween(@Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to);
```

- [ ] **Step 2: Escrever teste unitário falho para ReminderScheduler**

```java
// backend/src/test/java/com/github/mwacha/wachafit/notification/ReminderSchedulerTest.java
package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.booking.Booking;
import com.github.mwacha.wachafit.booking.BookingRepository;
import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @InjectMocks ReminderScheduler scheduler;

    @Test
    void sendReminders_shouldSendEmail_forBookingsIn4hWindow() {
        UUID studentId = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();

        GroupClass gc = new GroupClass();
        gc.setName("Yoga");

        Schedule schedule = new Schedule();
        schedule.setType(ScheduleType.CLASS);
        schedule.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(4));
        schedule.setEndsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(5));
        schedule.setTrainerId(trainerId);
        schedule.setGroupClass(gc);

        Booking booking = new Booking();
        booking.setStudentId(studentId);
        booking.setSchedule(schedule);

        User student = new User();
        student.setName("Maria");
        student.setEmail("maria@test.com");

        User trainer = new User();
        trainer.setName("João Personal");

        when(bookingRepository.findConfirmedBetween(any(), any())).thenReturn(List.of(booking));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        scheduler.sendReminders();

        verify(emailService).sendHtml(
            eq("maria@test.com"),
            contains("Lembrete"),
            eq("email/session-reminder"),
            anyMap()
        );
    }

    @Test
    void sendReminders_shouldNotSendEmail_whenNoBookingsInWindow() {
        when(bookingRepository.findConfirmedBetween(any(), any())).thenReturn(List.of());

        scheduler.sendReminders();

        verifyNoInteractions(emailService);
    }
}
```

- [ ] **Step 3: Confirmar falha**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test -Dtest=ReminderSchedulerTest 2>&1 | tail -5
```

Expected: erro de compilação (`ReminderScheduler` não existe).

- [ ] **Step 4: Criar ReminderScheduler**

```java
// backend/src/main/java/com/github/mwacha/wachafit/notification/ReminderScheduler.java
package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.booking.Booking;
import com.github.mwacha.wachafit.booking.BookingRepository;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
public class ReminderScheduler {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ReminderScheduler(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void sendReminders() {
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).plusHours(3);
        OffsetDateTime to   = OffsetDateTime.now(ZoneOffset.UTC).plusHours(5);
        List<Booking> bookings = bookingRepository.findConfirmedBetween(from, to);
        for (Booking booking : bookings) {
            userRepository.findById(booking.getStudentId()).ifPresent(student ->
                userRepository.findById(booking.getSchedule().getTrainerId()).ifPresent(trainer ->
                    emailService.sendHtml(
                        student.getEmail(),
                        "Lembrete de sessão — WachaFit",
                        "email/session-reminder",
                        Map.of(
                            "name", student.getName(),
                            "className", booking.getSchedule().getGroupClass() != null
                                ? booking.getSchedule().getGroupClass().getName()
                                : "Sessão individual",
                            "date", booking.getSchedule().getStartsAt().toLocalDate().toString(),
                            "time", booking.getSchedule().getStartsAt().toLocalTime().toString(),
                            "trainerName", trainer.getName()
                        )
                    )
                )
            );
        }
    }
}
```

- [ ] **Step 5: Adicionar @EnableScheduling em WachafitApplication**

```java
// backend/src/main/java/com/github/mwacha/wachafit/WachafitApplication.java
package com.github.mwacha.wachafit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WachafitApplication {
    public static void main(String[] args) {
        SpringApplication.run(WachafitApplication.class, args);
    }
}
```

- [ ] **Step 6: Rodar testes do scheduler**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test -Dtest=ReminderSchedulerTest 2>&1 | tail -8
```

Expected: `Tests run: 2, Failures: 0, Errors: 0` — `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git -C /Users/marceloferreira/developer/wachafit add \
  backend/src/main/java/com/github/mwacha/wachafit/booking/BookingRepository.java \
  backend/src/main/java/com/github/mwacha/wachafit/notification/ReminderScheduler.java \
  backend/src/main/java/com/github/mwacha/wachafit/WachafitApplication.java \
  backend/src/test/java/com/github/mwacha/wachafit/notification/ReminderSchedulerTest.java
git -C /Users/marceloferreira/developer/wachafit commit -m "feat: ReminderScheduler — lembrete de sessão 4h antes"
```

---

## Task 6: Suíte completa + merge

**Files:** nenhum novo arquivo

- [ ] **Step 1: Rodar suíte completa**

```bash
cd /Users/marceloferreira/developer/wachafit/backend
mvn test 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Contagem esperada ≈ 130+ testes (125 anteriores + novos).

- [ ] **Step 2: Merge da branch para main e push**

```bash
git -C /Users/marceloferreira/developer/wachafit checkout main
git -C /Users/marceloferreira/developer/wachafit merge feat/etapa4a-email --no-ff -m "feat: Etapa 4A — EmailService com notificações HTML"
git -C /Users/marceloferreira/developer/wachafit push origin main
```
