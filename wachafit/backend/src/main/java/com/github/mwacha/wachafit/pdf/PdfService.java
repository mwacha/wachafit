package com.github.mwacha.wachafit.pdf;

import com.github.mwacha.wachafit.assessment.PhysicalAssessment;
import com.github.mwacha.wachafit.assessment.PhysicalAssessmentRepository;
import com.github.mwacha.wachafit.exercise.ExerciseRepository;
import com.github.mwacha.wachafit.goal.StudentGoalRepository;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.UserRepository;
import com.github.mwacha.wachafit.workout.PersonalRecord;
import com.github.mwacha.wachafit.workout.PersonalRecordRepository;
import com.github.mwacha.wachafit.workout.WorkoutPlanItem;
import com.github.mwacha.wachafit.workout.WorkoutPlanRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PdfService {

    private final TemplateEngine templateEngine;
    private final UserRepository userRepo;
    private final PhysicalAssessmentRepository assessmentRepo;
    private final StudentGoalRepository goalRepo;
    private final PersonalRecordRepository recordRepo;
    private final WorkoutPlanRepository workoutPlanRepo;
    private final ExerciseRepository exerciseRepo;

    public PdfService(TemplateEngine templateEngine, UserRepository userRepo,
                      PhysicalAssessmentRepository assessmentRepo,
                      StudentGoalRepository goalRepo, PersonalRecordRepository recordRepo,
                      WorkoutPlanRepository workoutPlanRepo, ExerciseRepository exerciseRepo) {
        this.templateEngine = templateEngine;
        this.userRepo = userRepo;
        this.assessmentRepo = assessmentRepo;
        this.goalRepo = goalRepo;
        this.recordRepo = recordRepo;
        this.workoutPlanRepo = workoutPlanRepo;
        this.exerciseRepo = exerciseRepo;
    }

    public byte[] generateEvolutionPdf(UUID studentId) {
        String studentName = userRepo.findByIdAndTenantId(studentId, TenantContext.get())
            .map(u -> u.getName())
            .orElse("Aluno");

        List<PhysicalAssessment> assessments = assessmentRepo.findByStudentIdOrderByAssessedAtAsc(studentId);
        PhysicalAssessment last = assessments.isEmpty() ? null : assessments.get(assessments.size() - 1);

        var goals = goalRepo.findByStudentIdOrderByCreatedAtDesc(studentId);

        var records = recordRepo.findByStudentIdOrderByAchievedAtDesc(studentId);
        var exerciseIds = records.stream().map(PersonalRecord::getExerciseId).collect(Collectors.toList());
        var exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
            .collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));

        var recordDtos = records.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("exerciseName", exerciseMap.getOrDefault(r.getExerciseId(), "—"));
            m.put("recordLoadKg", r.getRecordLoadKg());
            m.put("achievedAt", r.getAchievedAt().toString());
            return m;
        }).toList();

        Context ctx = new Context();
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("generatedAt", LocalDate.now().toString());
        ctx.setVariable("lastAssessment", last);
        ctx.setVariable("goals", goals);
        ctx.setVariable("records", recordDtos);

        String html = templateEngine.process("pdf/student-evolution", ctx);
        return renderToPdf(html);
    }

    public byte[] generateWorkoutPdf(UUID studentId) {
        String studentName = userRepo.findByIdAndTenantId(studentId, TenantContext.get())
            .map(u -> u.getName())
            .orElse("Aluno");

        var planOpt = workoutPlanRepo.findByStudentIdAndActiveTrue(studentId);
        if (planOpt.isEmpty()) {
            Context ctx = new Context();
            ctx.setVariable("planName", "Sem ficha ativa");
            ctx.setVariable("studentName", studentName);
            ctx.setVariable("trainerName", "—");
            ctx.setVariable("planDescription", null);
            ctx.setVariable("itemsByDivision", new LinkedHashMap<>());
            return renderToPdf(templateEngine.process("pdf/workout-sheet", ctx));
        }

        var plan = planOpt.get();
        String trainerName = userRepo.findByIdAndTenantId(plan.getTrainerId(), TenantContext.get())
            .map(u -> u.getName())
            .orElse("—");

        var items = plan.getItems().stream()
            .sorted(Comparator.comparingInt(WorkoutPlanItem::getOrderIndex))
            .toList();

        var exerciseIds = items.stream().map(WorkoutPlanItem::getExerciseId).collect(Collectors.toList());
        var exerciseMap = exerciseRepo.findAllById(exerciseIds).stream()
            .collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));

        var itemsByDivision = items.stream().collect(
            Collectors.groupingBy(
                i -> i.getDivision() != null ? i.getDivision() : "",
                LinkedHashMap::new,
                Collectors.mapping(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("exerciseName", exerciseMap.getOrDefault(i.getExerciseId(), "—"));
                    m.put("sets", i.getSets());
                    m.put("reps", i.getReps());
                    m.put("suggestedLoadKg", i.getSuggestedLoadKg());
                    m.put("restSeconds", i.getRestSeconds());
                    m.put("notes", i.getNotes() != null ? i.getNotes() : "");
                    return m;
                }, Collectors.toList())
            )
        );

        Context ctx = new Context();
        ctx.setVariable("planName", plan.getName());
        ctx.setVariable("studentName", studentName);
        ctx.setVariable("trainerName", trainerName);
        ctx.setVariable("planDescription", plan.getDescription());
        ctx.setVariable("itemsByDivision", itemsByDivision);

        return renderToPdf(templateEngine.process("pdf/workout-sheet", ctx));
    }

    private byte[] renderToPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                .withHtmlContent(html, null)
                .toStream(baos)
                .run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }
}
