package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.notification.event.WorkoutPlanAssignedEvent;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class WorkoutNotificationListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public WorkoutNotificationListener(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkoutPlanAssigned(WorkoutPlanAssignedEvent event) {
        userRepository.findById(event.studentId()).ifPresent(student ->
            userRepository.findById(event.trainerId()).ifPresent(trainer ->
                emailService.sendHtml(
                    student.getEmail(),
                    "Nova ficha de treino — WachaFit",
                    "email/workout-plan-assigned",
                    Map.of(
                        "studentName", student.getName(),
                        "trainerName", trainer.getName(),
                        "planName",    event.planName()
                    )
                )
            )
        );
    }
}
