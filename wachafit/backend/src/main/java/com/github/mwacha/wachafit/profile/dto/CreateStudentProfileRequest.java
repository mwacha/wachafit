package com.github.mwacha.wachafit.profile.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateStudentProfileRequest(
    @NotBlank String cpf,
    String rg,
    LocalDate birthDate,
    String gender,
    String maritalStatus,
    String profession,
    String phone,
    String addressZip,
    String addressLine,
    String addressNumber,
    String addressComplement,
    String addressNeighborhood,
    String addressCity,
    String addressState,
    String emergencyContactName,
    String emergencyContactPhone,
    String emergencyContactRelationship
) {}
