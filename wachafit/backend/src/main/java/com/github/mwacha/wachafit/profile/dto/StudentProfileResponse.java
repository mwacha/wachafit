package com.github.mwacha.wachafit.profile.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentProfileResponse(
    UUID id, UUID userId, String cpf, LocalDate birthDate, String phone,
    String addressLine, String addressCity, String addressState, String addressZip,
    String emergencyContactName, String emergencyContactPhone, Instant createdAt
) {}
