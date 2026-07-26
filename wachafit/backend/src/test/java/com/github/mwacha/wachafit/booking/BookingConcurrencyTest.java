package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.groupclass.GroupClassRepository;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BookingConcurrencyTest {

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

    @Autowired BookingService bookingService;
    @Autowired UserRepository userRepository;
    @Autowired GroupClassRepository groupClassRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
    @Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;

    @AfterEach
    void tearDown() {
        com.github.mwacha.wachafit.tenant.TenantContext.clear();
    }

    @Test
    void rn03_onlyOneBookingSucceeds_whenTwoStudentsRaceForLastSlot() throws Exception {
        var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
        // O código abaixo persiste entidades TenantAware fora de uma requisição HTTP autenticada
        // (que é quem normalmente define isso via JwtFilter), então precisa setar o TenantContext
        // manualmente antes de qualquer save() nesta thread de teste.
        com.github.mwacha.wachafit.tenant.TenantContext.set(tenant.getId());

        // Create a trainer
        Account trainerAccount = new Account();
        trainerAccount.setName("Trainer"); trainerAccount.setEmail("trainer-c-" + UUID.randomUUID() + "@test.com");
        trainerAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(trainerAccount);
        User trainer = new User();
        trainer.setAccount(trainerAccount);
        trainer.setRole(Role.TRAINER); trainer.setTenant(tenant);
        trainer = userRepository.save(trainer);

        // Create group class with capacity = 1
        GroupClass gc = new GroupClass();
        gc.setName("Solo Class"); gc.setCapacity(1); gc.setDurationMinutes(60);
        gc.setTrainer(trainer);
        gc = groupClassRepository.save(gc);

        // Create schedule
        Schedule schedule = new Schedule();
        schedule.setGroupClass(gc);
        schedule.setTrainerId(trainer.getId());
        schedule.setType(ScheduleType.CLASS);
        schedule.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        schedule.setEndsAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1));
        schedule = scheduleRepository.save(schedule);
        final UUID scheduleId = schedule.getId();

        // Create 2 students
        Account s1Account = new Account(); s1Account.setName("S1"); s1Account.setEmail("s1-c-" + UUID.randomUUID() + "@test.com");
        s1Account.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(s1Account);
        User s1 = new User();
        s1.setAccount(s1Account); s1.setRole(Role.STUDENT); s1.setTenant(tenant);
        s1 = userRepository.save(s1);

        Account s2Account = new Account(); s2Account.setName("S2"); s2Account.setEmail("s2-c-" + UUID.randomUUID() + "@test.com");
        s2Account.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(s2Account);
        User s2 = new User();
        s2.setAccount(s2Account); s2.setRole(Role.STUDENT); s2.setTenant(tenant);
        s2 = userRepository.save(s2);

        final UUID student1Id = s1.getId();
        final UUID student2Id = s2.getId();

        // Race both students to book the last slot
        ExecutorService exec = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // TenantContext é um ThreadLocal — não se propaga automaticamente para as threads do
        // pool, então cada tarefa precisa setá-lo antes de chamar o service e limpá-lo ao final.
        futures.add(exec.submit(() -> {
            com.github.mwacha.wachafit.tenant.TenantContext.set(tenant.getId());
            try {
                bookingService.createBooking(new CreateBookingRequest(scheduleId), student1Id);
                successCount.incrementAndGet();
                return true;
            } catch (BusinessException e) { return false; }
            finally { com.github.mwacha.wachafit.tenant.TenantContext.clear(); }
        }));
        futures.add(exec.submit(() -> {
            com.github.mwacha.wachafit.tenant.TenantContext.set(tenant.getId());
            try {
                bookingService.createBooking(new CreateBookingRequest(scheduleId), student2Id);
                successCount.incrementAndGet();
                return true;
            } catch (BusinessException e) { return false; }
            finally { com.github.mwacha.wachafit.tenant.TenantContext.clear(); }
        }));

        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);

        // RN-02/RN-03: exactly ONE booking must succeed
        assertThat(successCount.get())
            .as("Exactly one booking should succeed when two race for the last slot")
            .isEqualTo(1);
    }
}
