package com.github.mwacha.wachafit.profile.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateStudentProfileRequest(
    @NotBlank String cpf,
    LocalDate birthDate,
    String phone,
    String addressLine,
    String addressCity,
    String addressState,
    String addressZip,
    String emergencyContactName,
    String emergencyContactPhone
) {}
