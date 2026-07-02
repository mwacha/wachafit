package com.github.mwacha.wachafit.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock TemplateEngine templateEngine;
    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender, templateEngine, "noreply@wachafit.com");
    }

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
        when(templateEngine.process(any(String.class), any(IContext.class))).thenReturn("<html></html>");
        doThrow(new RuntimeException("SMTP indisponível")).when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
            service.sendHtml("joao@test.com", "Assunto", "email/welcome", Map.of()));
    }
}
