package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.progress.dto.PhotoResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProgressService {

    private final ProgressPhotoRepository repo;
    private final Path uploadDir;

    public ProgressService(ProgressPhotoRepository repo,
            @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.repo = repo;
        this.uploadDir = Paths.get(uploadDir, "photos");
    }

    public PhotoResponse upload(UUID studentId, MultipartFile file, LocalDate takenAt, String notes, User uploadedBy) {
        assertCanAccessOrUpload(studentId, uploadedBy);
        try {
            Path dir = uploadDir.resolve(studentId.toString());
            Files.createDirectories(dir);
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (ext != null ? "." + ext : "");
            file.transferTo(dir.resolve(filename));
            String storageKey = studentId + "/" + filename;
            ProgressPhoto photo = new ProgressPhoto();
            photo.setStudentId(studentId);
            photo.setUploadedBy(uploadedBy.getId());
            photo.setStorageKey(storageKey);
            photo.setTakenAt(takenAt != null ? takenAt : LocalDate.now());
            photo.setNotes(notes);
            return toResponse(repo.save(photo));
        } catch (IOException e) {
            throw new BusinessException("Failed to store photo: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> list(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findByStudentIdOrderByTakenAtDesc(studentId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Resource loadFile(UUID photoId, User requestingUser) {
        ProgressPhoto photo = repo.findById(photoId)
            .orElseThrow(() -> new NotFoundException("Photo not found"));
        assertCanAccess(photo.getStudentId(), requestingUser);
        Path file = uploadDir.resolve(photo.getStorageKey());
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) throw new NotFoundException("File not found on disk");
        return resource;
    }

    public void delete(UUID photoId, User requestingUser) {
        ProgressPhoto photo = repo.findById(photoId)
            .orElseThrow(() -> new NotFoundException("Photo not found"));
        assertCanAccess(photo.getStudentId(), requestingUser);
        try { Files.deleteIfExists(uploadDir.resolve(photo.getStorageKey())); }
        catch (IOException ignored) {}
        repo.delete(photo);
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void assertCanAccessOrUpload(UUID studentId, User uploadedBy) {
        if (uploadedBy.getRole() == Role.ADMIN) throw new ForbiddenException("ADMIN cannot upload photos");
        if (uploadedBy.getRole() == Role.STUDENT && !studentId.equals(uploadedBy.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private PhotoResponse toResponse(ProgressPhoto p) {
        return new PhotoResponse(p.getId(), p.getStudentId(), p.getUploadedBy(),
            p.getTakenAt(), p.getNotes(), "/api/photos/" + p.getId() + "/file", p.getCreatedAt());
    }
}
