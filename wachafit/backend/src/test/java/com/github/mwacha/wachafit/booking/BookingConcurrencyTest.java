package com.github.mwacha.wachafit.booking;

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
        r.add("app.cancellation-window-hours", () -> "4");
    }

    @Autowired BookingService bookingService;
    @Autowired UserRepository userRepository;
    @Autowired GroupClassRepository groupClassRepository;
    @Autowired ScheduleRepository scheduleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void rn03_onlyOneBookingSucceeds_whenTwoStudentsRaceForLastSlot() throws Exception {
        // Create a trainer
        User trainer = new User();
        trainer.setName("Trainer"); trainer.setEmail("trainer-c-" + UUID.randomUUID() + "@test.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass")); trainer.setRole(Role.TRAINER);
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
        User s1 = new User(); s1.setName("S1"); s1.setEmail("s1-c-" + UUID.randomUUID() + "@test.com");
        s1.setPasswordHash(passwordEncoder.encode("pass")); s1.setRole(Role.STUDENT);
        s1 = userRepository.save(s1);

        User s2 = new User(); s2.setName("S2"); s2.setEmail("s2-c-" + UUID.randomUUID() + "@test.com");
        s2.setPasswordHash(passwordEncoder.encode("pass")); s2.setRole(Role.STUDENT);
        s2 = userRepository.save(s2);

        final UUID student1Id = s1.getId();
        final UUID student2Id = s2.getId();

        // Race both students to book the last slot
        ExecutorService exec = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        futures.add(exec.submit(() -> {
            try {
                bookingService.createBooking(new CreateBookingRequest(scheduleId), student1Id);
                successCount.incrementAndGet();
                return true;
            } catch (BusinessException e) { return false; }
        }));
        futures.add(exec.submit(() -> {
            try {
                bookingService.createBooking(new CreateBookingRequest(scheduleId), student2Id);
                successCount.incrementAndGet();
                return true;
            } catch (BusinessException e) { return false; }
        }));

        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);

        // RN-02/RN-03: exactly ONE booking must succeed
        assertThat(successCount.get())
            .as("Exactly one booking should succeed when two race for the last slot")
            .isEqualTo(1);
    }
}
