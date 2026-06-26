package com.github.mwacha.wachafit.user.dto;

import com.github.mwacha.wachafit.user.Role;
import jakarta.validation.constraints.*;

public record UpdateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotNull Role role
) {}
