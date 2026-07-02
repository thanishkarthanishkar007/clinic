package com.devsclinic.backend.dto;

import com.devsclinic.backend.model.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload returned to the frontend after an appointment
 * is successfully created (or fetched).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private String id;
    private String appointmentId;
    private String fullName;
    private String phoneNumber;
    private String emailAddress;
    private String primaryConcern;
    private LocalDate preferredDate;
    private String preferredTime;
    private String additionalNotes;
    private String status;
    private boolean emailNotificationSent;
    private LocalDateTime createdAt;

    public static AppointmentResponse fromEntity(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentId(appointment.getAppointmentId())
                .fullName(appointment.getFullName())
                .phoneNumber(appointment.getPhoneNumber())
                .emailAddress(appointment.getEmailAddress())
                .primaryConcern(appointment.getPrimaryConcern())
                .preferredDate(appointment.getPreferredDate())
                .preferredTime(appointment.getPreferredTime())
                .additionalNotes(appointment.getAdditionalNotes())
                .status(appointment.getStatus().name())
                .emailNotificationSent(appointment.isEmailNotificationSent())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}
