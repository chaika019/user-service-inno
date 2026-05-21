package by.chaika19.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UserRequestDto(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Surname is required")
        String surname,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date should be in the past")
        LocalDate birthDate,

        @NotBlank
        @Email(message = "Invalid email")
        String email,

        @NotNull(message = "Active is required")
        Boolean active
) { }
