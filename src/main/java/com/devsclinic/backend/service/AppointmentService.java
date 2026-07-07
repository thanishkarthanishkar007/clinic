package com.devsclinic.backend.service;

import com.devsclinic.backend.dto.AppointmentRequest;
import com.devsclinic.backend.dto.AppointmentResponse;
import com.devsclinic.backend.exception.EmailSendException;
import com.devsclinic.backend.exception.ResourceNotFoundException;
import com.devsclinic.backend.model.Appointment;
import com.devsclinic.backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer encapsulating all business logic for appointment booking.
 *
 * Flow for booking a new appointment:
 *   1. Map the incoming DTO to an Appointment entity
 *   2. Persist it to MongoDB  (this ALWAYS happens first and is never skipped)
 *   3. Attempt to email the clinic with the appointment details
 *   4. Update the appointment's emailNotificationSent flag based on outcome
 *   5. Return the response DTO to the controller
 *
 * Design decision: the appointment is saved to the database BEFORE the email
 * is sent, and a failure to send email does NOT cause the booking to fail.
 * This guarantees patient bookings are never lost due to a transient SMTP issue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    /**
     * Generates a human-readable appointment ID like SIVA101, PRIYA102.
     * Format: first 4 chars of name (uppercase) + 3-digit sequence number.
     */
    private String generateAppointmentId(String fullName) {
        // Extract first name, take up to 5 chars, uppercase, letters only
        String raw = fullName.trim().split("\\s+")[0]
                .toUpperCase()
                .replaceAll("[^A-Z]", "");
        final String prefix = raw.length() > 5 ? raw.substring(0, 5) : raw;

        // Count existing appointments with same prefix to get next sequence
        long count = appointmentRepository.findAll()
                .stream()
                .filter(a -> a.getAppointmentId() != null &&
                             a.getAppointmentId().startsWith(prefix))
                .count();

        int sequence = (int)(count + 1) + 100; // starts at 101
        return prefix + sequence;
    }

    /**
     * Books a new appointment: persists to MongoDB, then notifies the clinic via email.
     */
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        log.info("Booking new appointment for patient: {}", request.getFullName());

        Appointment appointment = Appointment.builder()
                .appointmentId(generateAppointmentId(request.getFullName()))
                .fullName(request.getFullName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .emailAddress(request.getEmailAddress() != null ? request.getEmailAddress().trim() : null)
                .primaryConcern(request.getPrimaryConcern().trim())
                .preferredDate(request.getPreferredDate())
                .preferredTime(request.getPreferredTime().trim())
                .additionalNotes(request.getAdditionalNotes() != null ? request.getAdditionalNotes().trim() : null)
                .status(Appointment.AppointmentStatus.PENDING)
                .emailNotificationSent(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Step 1: Save to MongoDB FIRST — booking must never be lost
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment saved to MongoDB with id: {}", saved.getId());

        // Step 2: Attempt to notify the clinic via email
        try {
            emailService.sendAppointmentNotification(saved);
            saved.setEmailNotificationSent(true);
            saved = appointmentRepository.save(saved); // persist updated flag
        } catch (EmailSendException ex) {
            // Do not fail the booking — log and continue.
            // emailNotificationSent remains false; can be retried later via resendNotification().
            log.warn("Appointment {} saved successfully, but email notification failed: {}",
                    saved.getId(), ex.getMessage());
        }

        return AppointmentResponse.fromEntity(saved);
    }

    /**
     * Retrieves a single appointment by its id.
     */
    public AppointmentResponse getAppointmentById(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return AppointmentResponse.fromEntity(appointment);
    }

    /**
     * Retrieves all appointments, most recent first.
     */
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(AppointmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all appointments associated with a given phone number.
     * Useful for patients checking their own booking history.
     */
    public List<AppointmentResponse> getAppointmentsByPhone(String phoneNumber) {
        return appointmentRepository.findByPhoneNumber(phoneNumber)
                .stream()
                .map(AppointmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of an appointment (e.g. clinic staff confirming a booking).
     */
    @Transactional
    public AppointmentResponse updateAppointmentStatus(String id, Appointment.AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(status);
        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment {} status updated to {}", id, status);

        return AppointmentResponse.fromEntity(updated);
    }

    /**
     * Re-attempts sending the clinic notification email for an appointment
     * whose original email delivery failed. Allows manual recovery without
     * requiring the patient to re-book.
     */
    public AppointmentResponse resendNotificationEmail(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        emailService.sendAppointmentNotification(appointment);
        appointment.setEmailNotificationSent(true);
        Appointment updated = appointmentRepository.save(appointment);

        return AppointmentResponse.fromEntity(updated);
    }

    /**
     * Deletes an appointment by id (e.g. patient cancellation, admin cleanup).
     */
    public void deleteAppointment(String id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Appointment not found with id: " + id);
        }
        appointmentRepository.deleteById(id);
        log.info("Appointment {} deleted", id);
    }
}
