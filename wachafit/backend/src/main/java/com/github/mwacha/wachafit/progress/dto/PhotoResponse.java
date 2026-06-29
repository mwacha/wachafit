package com.github.mwacha.wachafit.progress.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PhotoResponse(UUID id, UUID studentId, UUID uploadedBy, LocalDate takenAt, String notes, String fileUrl, Instant createdAt) {}
