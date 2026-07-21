# WachaFit Etapa 4A — EmailService com Notificações HTML
**Data:** 2026-06-30
**Escopo:** EmailService centralizado, templates Thymeleaf, lembrete agendado (4h antes), migração do reset de senha

---

## 1. Visão Geral

Centralizar todo envio de e-mail em um `EmailService` no pacote `notification/`, substituindo o `SimpleMailMessage` do `AuthService` e adicionando notificações para os eventos de booking e cadastro de usuário.

---

## 2. Estrutura de Pacotes

```
com.github.mwacha.wachafit.notification/
├── EmailService.java
├── ReminderScheduler.java
└── (templates em resources)

src/main/resources/templates/email/
├── welcome.html
├── booking-confirmed.html
├── booking-cancelled.html
├── session-reminder.html
└── password-reset.html
```

---

## 3. EmailService

**Interface pública:**
```java
void sendHtml(String to, String subject, String templateName, Map<String, Object> vars);
```

- Usa `JavaMailSender` (já no `pom.xml` via `spring-boot-starter-mail`)
- Usa `TemplateEngine` do Thymeleaf para renderizar o HTML
- Envia com `MimeMessage` para suportar HTML
- Erros de envio logados mas não propagados (não bloquear fluxo principal)

---

## 4. Eventos e Gatilhos

| Evento | Gatilho | Template |
|--------|---------|----------|
| Boas-vindas | `UserService.register()` após salvar usuário | `welcome.html` |
| Confirmação de booking | `BookingService.create()` após salvar booking | `booking-confirmed.html` |
| Cancelamento de booking | `BookingService.cancel()` após cancelar | `booking-cancelled.html` |
| Lembrete de sessão | `ReminderScheduler` (job agendado) | `session-reminder.html` |
| Redefinição de senha | `AuthService.forgotPassword()` — migrado | `password-reset.html` |

---

## 5. ReminderScheduler

```java
@Component
@EnableScheduling
public class ReminderScheduler {
    @Scheduled(fixedDelay = 3_600_000) // executa a cada hora
    void sendReminders() {
        Instant from = Instant.now().plus(3, HOURS);
        Instant to   = Instant.now().plus(5, HOURS);
        // busca bookings com status CONFIRMED e startTime entre from e to
        // envia session-reminder.html para cada aluno
    }
}
```

- Janela 3h–5h: garante disparo único ~4h antes da sessão
- `@EnableScheduling` em `WachafitApplication`
- Busca via `BookingRepository.findConfirmedBetween(from, to)`

---

## 6. Templates HTML

Layout base compartilhado: cabeçalho com cor primária + "WachaFit", corpo centralizado (max-width 600px), rodapé com texto simples. Sem imagens externas — compatível com clientes de e-mail restritivos.

| Template | Variáveis Thymeleaf |
|----------|---------------------|
| `welcome.html` | `name` |
| `booking-confirmed.html` | `name`, `className`, `date`, `time`, `trainerName` |
| `booking-cancelled.html` | `name`, `className`, `date`, `time` |
| `session-reminder.html` | `name`, `className`, `date`, `time`, `trainerName` |
| `password-reset.html` | `name`, `resetLink` |

---

## 7. Migração do AuthService

`AuthService.forgotPassword()` atualmente usa `SimpleMailMessage` (texto simples). Será refatorado para:
```java
emailService.sendHtml(
    user.getEmail(),
    "Redefinição de senha — Wachafit",
    "email/password-reset",
    Map.of("name", user.getName(), "resetLink", link)
);
```
Mesmo comportamento, visual HTML.

---

## 8. Configuração

`application.properties` (prod) — variáveis de ambiente:
```properties
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

`application-test.yml` — já tem configuração de mock (host: localhost, port: 1025). Testes de integração usam Mailpit ou GreenMail.

---

## 9. Testes

- **Unitário** (`EmailServiceTest`): mock de `JavaMailSender` e `TemplateEngine`, verifica chamadas corretas
- **Unitário** (`ReminderSchedulerTest`): mock de `BookingRepository` e `EmailService`, verifica que apenas bookings na janela correta recebem e-mail
- **Integração** (`BookingEmailIntegrationTest`): Testcontainers + GreenMail, cria booking e verifica que e-mail foi enviado

---

## 10. Dependências Adicionais no pom.xml

```xml
<!-- Thymeleaf para templates de e-mail -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- GreenMail para testes de e-mail -->
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-junit5</artifactId>
    <version>2.1.2</version>
    <scope>test</scope>
</dependency>
```

> `spring-boot-starter-mail` já está no `pom.xml`.
