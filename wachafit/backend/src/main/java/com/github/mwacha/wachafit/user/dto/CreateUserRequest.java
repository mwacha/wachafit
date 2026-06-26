package com.github.mwacha.wachafit.user.dto;

import com.github.mwacha.wachafit.user.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotNull Role role
) {}
