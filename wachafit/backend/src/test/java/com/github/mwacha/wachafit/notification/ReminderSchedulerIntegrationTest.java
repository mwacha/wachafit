package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.booking.Booking;
import com.github.mwacha.wachafit.booking.BookingRepository;
import com.github.mwacha.wachafit.booking.BookingStatus;
import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.groupclass.GroupClassRepository;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Regressão: sendReminders() rodava sem @Transactional, e Booking.schedule / Schedule.groupClass
// são @ManyToOne LAZY -- um teste puramente Mockito (ReminderSchedulerTest) nunca detectaria isso,
// já que objetos construídos diretamente em memória nunca são proxies reais do Hibernate. Este
// teste persiste as entidades de verdade contra Postgres real (via Testcontainers) para garantir
// que o método realmente carrega as associações lazy sem lançar LazyInitializationException.
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ReminderSchedulerIntegrationTest {

    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "integration-test-secret-32-chars-ok");
        r.add("jwt.expiration", () -> "3600");
        r.add("app.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired ReminderScheduler scheduler;
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired GroupClassRepository groupClassRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TenantRepository tenantRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sendReminders_loadsLazyAssociations_withoutThrowing() {
        var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
        TenantContext.set(tenant.getId());

        Account trainerAccount = new Account();
        trainerAccount.setName("Trainer Reminder"); trainerAccount.setEmail("trainer-rs-" + UUID.randomUUID() + "@test.com");
        trainerAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(trainerAccount);
        User trainer = new User();
        trainer.setAccount(trainerAccount); trainer.setRole(Role.TRAINER); trainer.setTenant(tenant);
        trainer = userRepository.save(trainer);

        Account studentAccount = new Account();
        studentAccount.setName("Student Reminder"); studentAccount.setEmail("student-rs-" + UUID.randomUUID() + "@test.com");
        studentAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(studentAccount);
        User student = new User();
        student.setAccount(studentAccount); student.setRole(Role.STUDENT); student.setTenant(tenant);
        student = userRepository.save(student);

        GroupClass gc = new GroupClass();
        gc.setName("Yoga"); gc.setCapacity(10); gc.setDurationMinutes(60);
        gc.setTrainer(trainer);
        gc = groupClassRepository.save(gc);

        Schedule schedule = new Schedule();
        schedule.setGroupClass(gc);
        schedule.setTrainerId(trainer.getId());
        schedule.setType(ScheduleType.CLASS);
        schedule.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(4));
        schedule.setEndsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(5));
        schedule = scheduleRepository.save(schedule);

        Booking booking = new Booking();
        booking.setSchedule(schedule);
        booking.setStudentId(student.getId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        UUID bookingId = booking.getId();

        assertThatCode(() -> scheduler.sendReminders()).doesNotThrowAnyException();

        assertThat(bookingRepository.findById(bookingId).orElseThrow().isReminderSent()).isTrue();
    }
}
