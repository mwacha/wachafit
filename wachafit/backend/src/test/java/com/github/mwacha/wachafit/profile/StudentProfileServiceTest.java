package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.*;
import com.github.mwacha.wachafit.shared.exception.*;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

    @Mock StudentProfileRepository profileRepo;
    @Mock StudentHealthRepository healthRepo;
    @Mock UserRepository userRepo;
    @InjectMocks StudentProfileService service;

    private User admin;
    private User student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new User(); student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }
        admin = new User(); admin.setRole(Role.ADMIN);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(admin, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createProfile_shouldPersist_whenStudentExists() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(student));
        when(profileRepo.findByUserId(studentId)).thenReturn(Optional.empty());
        when(profileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateStudentProfileRequest req = new CreateStudentProfileRequest(
            "123.456.789-00", null, null, null, null, null, "11999999999",
            null, null, null, null, null, null, null, null, null, null
        );
        StudentProfileResponse res = service.createProfile(studentId, req, admin.getId());
        assertThat(res.cpf()).isEqualTo("123.456.789-00");
    }

    @Test
    void createProfile_shouldThrowBusiness_whenCpfAlreadyExists() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(student));
        StudentProfile existing = new StudentProfile();
        when(profileRepo.findByUserId(studentId)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.createProfile(studentId,
            new CreateStudentProfileRequest("123.456.789-00", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null),
            admin.getId()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void getProfile_shouldThrowForbidden_whenStudentAccessesOther() {
        User otherStudent = new User(); otherStudent.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(otherStudent, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        assertThatThrownBy(() -> service.getProfile(studentId, otherStudent))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProfile_shouldSucceed_whenStudentAccessesOwn() {
        when(profileRepo.findByUserId(studentId)).thenReturn(Optional.empty());
        assertThatNoException().isThrownBy(() -> service.getProfile(studentId, student));
    }
}
