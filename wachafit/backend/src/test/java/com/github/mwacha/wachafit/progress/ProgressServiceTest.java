package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock ProgressPhotoRepository repo;
    ProgressService service;

    private User student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new User(); student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }
        service = new ProgressService(repo, System.getProperty("java.io.tmpdir") + "/wachafit-test");
    }

    @Test
    void loadFile_shouldThrowNotFound_whenPhotoMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadFile(UUID.randomUUID(), student))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void upload_shouldThrowBusinessException_whenExtensionNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "malicious.exe", "application/octet-stream", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.upload(studentId, file, LocalDate.now(), null, student))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("File type not allowed");
    }

    @Test
    void upload_shouldThrowBusinessException_whenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> service.upload(studentId, file, LocalDate.now(), null, student))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void delete_shouldThrowForbidden_whenStudentDeletesOthersPhoto() {
        ProgressPhoto photo = new ProgressPhoto();
        photo.setStudentId(UUID.randomUUID()); // different student
        photo.setStorageKey("other/file.jpg");
        when(repo.findById(any())).thenReturn(Optional.of(photo));
        User other = new User(); other.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(other, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), other))
            .isInstanceOf(ForbiddenException.class);
    }
}
