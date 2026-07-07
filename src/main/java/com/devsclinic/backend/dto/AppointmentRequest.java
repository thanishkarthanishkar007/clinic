package com.devsclinic.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload sent by the frontend when a patient books an appointment.
 * All required fields are validated using Bean Validation (JSR-380).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[+]?[0-9\\s-]{10,15}$",
            message = "Phone number must be a valid format (10-15 digits, may include +, spaces or hyphens)"
    )
    private String phoneNumber;

    @Email(message = "Email address must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String emailAddress; // optional field — not @NotBlank

    @NotBlank(message = "Primary concern is required")
    private String primaryConcern;

    @NotNull(message = "Preferred date is required")
    @FutureOrPresent(message = "Preferred date cannot be in the past")
    private LocalDate preferredDate;

    @NotBlank(message = "Preferred time slot is required")
    private String preferredTime;

    @Size(max = 1000, message = "Additional notes must not exceed 1000 characters")
    private String additionalNotes;
}
