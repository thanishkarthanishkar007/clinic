package com.devsclinic.backend.controller;

import com.devsclinic.backend.dto.ApiResponse;
import com.devsclinic.backend.dto.AppointmentRequest;
import com.devsclinic.backend.dto.AppointmentResponse;
import com.devsclinic.backend.model.Appointment;
import com.devsclinic.backend.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing all appointment-related endpoints
 * for Devs Hair and Skin Clinic.
 *
 * Base path: /api/appointments
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Books a new appointment.
     * This is the primary endpoint called by the frontend's "Book Consultation" form.
     *
     * Flow:
     *   Patient fills form -> POST /api/appointments
     *     -> Validate request body
     *     -> Save to MongoDB
     *     -> Send email to clinic
     *     -> Return success response
     *
     * POST /api/appointments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @Valid @RequestBody AppointmentRequest request) {

        AppointmentResponse response = appointmentService.bookAppointment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Appointment booked successfully. Our team will contact you shortly.", response)
        );
    }

    /**
     * Fetches a single appointment by its MongoDB id.
     * GET /api/appointments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable String id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(ApiResponse.success("Appointment retrieved successfully", response));
    }

    /**
     * Fetches all appointments (intended for clinic admin/staff dashboard).
     * GET /api/appointments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllAppointments() {
        List<AppointmentResponse> response = appointmentService.getAllAppointments();
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved successfully", response));
    }

    /**
     * Fetches all appointments booked under a given phone number.
     * GET /api/appointments/search?phone=9840055678
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsByPhone(
            @RequestParam String phone) {
        List<AppointmentResponse> response = appointmentService.getAppointmentsByPhone(phone);
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved successfully", response));
    }

    /**
     * Updates the status of an appointment (clinic staff use, e.g. confirming a booking).
     * PATCH /api/appointments/{id}/status?status=CONFIRMED
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam Appointment.AppointmentStatus status) {

        AppointmentResponse response = appointmentService.updateAppointmentStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Appointment status updated successfully", response));
    }

    /**
     * Manually re-triggers the clinic notification email for an appointment
     * whose original email failed to send (recovery endpoint for clinic staff).
     * POST /api/appointments/{id}/resend-email
     */
    @PostMapping("/{id}/resend-email")
    public ResponseEntity<ApiResponse<AppointmentResponse>> resendEmail(@PathVariable String id) {
        AppointmentResponse response = appointmentService.resendNotificationEmail(id);
        return ResponseEntity.ok(ApiResponse.success("Notification email resent successfully", response));
    }

    /**
     * Deletes an appointment by id.
     * DELETE /api/appointments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable String id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok(ApiResponse.success("Appointment deleted successfully", null));
    }
}
