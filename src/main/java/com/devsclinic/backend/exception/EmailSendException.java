package com.devsclinic.backend.exception;

/**
 * Thrown when the clinic notification email fails to send.
 * Note: by design, this does NOT roll back the appointment save —
 * the appointment is still persisted in MongoDB even if email delivery fails,
 * so no booking is ever lost. See AppointmentService for details.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailSendException(String message) {
        super(message);
    }
}
