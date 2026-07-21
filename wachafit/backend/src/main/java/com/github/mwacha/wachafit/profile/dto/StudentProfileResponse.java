package com.github.mwacha.wachafit.profile.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentProfileResponse(
    UUID id, UUID userId,
    String cpf, String rg,
    LocalDate birthDate, String gender, String maritalStatus, String profession,
    String phone,
    String addressZip, String addressLine, String addressNumber,
    String addressComplement, String addressNeighborhood,
    String addressCity, String addressState,
    String emergencyContactName, String emergencyContactPhone, String emergencyContactRelationship,
    Instant createdAt
) {}
