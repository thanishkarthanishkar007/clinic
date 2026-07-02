package com.devsclinic.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MongoDB document representing a patient appointment booking
 * for Devs Hair and Skin Clinic.
 *
 * Collection: appointments
 */
@Document(collection = "appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    private String id;

    @Field("appointment_id")
    @Indexed(unique = true)
    private String appointmentId; // e.g. SIVA101, PRIYA102

    @Field("full_name")
    private String fullName;

    @Field("phone_number")
    @Indexed
    private String phoneNumber;

    @Field("email_address")
    private String emailAddress;

    @Field("primary_concern")
    private String primaryConcern;

    @Field("preferred_date")
    private LocalDate preferredDate;

    @Field("preferred_time")
    private String preferredTime;

    @Field("additional_notes")
    private String additionalNotes;

    /**
     * Status of the appointment lifecycle.
     * PENDING -> CONFIRMED -> COMPLETED  (or CANCELLED at any stage)
     */
    @Field("status")
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    /**
     * Tracks whether the notification email to the clinic was sent successfully.
     * Useful for retry/audit purposes if SMTP fails.
     */
    @Field("email_notification_sent")
    @Builder.Default
    private boolean emailNotificationSent = false;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    public enum AppointmentStatus {
        PENDING,
        CONFIRMED,
        COMPLETED,
        CANCELLED
    }
}
