package com.devsclinic.backend.service;

import com.devsclinic.backend.exception.EmailSendException;
import com.devsclinic.backend.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    @Value("${clinic.notification.email}")
    private String clinicEmail;

    @Value("${clinic.name}")
    private String clinicName;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy (EEEE)");

    public void sendAppointmentNotification(Appointment appointment) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", fromEmail);
            body.put("to", List.of(clinicEmail));
            body.put("subject", String.format(
                    "New Appointment — %s | %s",
                    appointment.getFullName(),
                    appointment.getPreferredDate().format(DATE_FORMAT)
            ));
            body.put("html", buildHtmlEmail(appointment));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    RESEND_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("HTML email sent via Resend for appointment id={}", appointment.getId());
            } else {
                throw new EmailSendException("Resend API returned: " + response.getStatusCode());
            }

        } catch (EmailSendException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Failed to send email via Resend for appointment id={}: {}",
                    appointment.getId(), ex.getMessage(), ex);
            throw new EmailSendException("Resend API call failed", ex);
        }
    }

    private String buildHtmlEmail(Appointment a) {
        String date = a.getPreferredDate().format(DATE_FORMAT);
        String email = (a.getEmailAddress() != null && !a.getEmailAddress().isBlank())
                ? a.getEmailAddress() : "Not provided";
        String notes = (a.getAdditionalNotes() != null && !a.getAdditionalNotes().isBlank())
                ? a.getAdditionalNotes() : "None";

        return "<!DOCTYPE html>" +
            "<html lang='en'><head><meta charset='UTF-8'/>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
            "<title>New Appointment</title></head>" +
            "<body style='margin:0;padding:0;background:#fdf7f9;font-family:Segoe UI,Arial,sans-serif;'>" +

            // Wrapper
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fdf7f9;padding:32px 16px;'>" +
            "<tr><td align='center'>" +
            "<table width='100%' style='max-width:580px;background:#ffffff;border-radius:16px;" +
            "overflow:hidden;box-shadow:0 4px 24px rgba(108,63,196,0.10);'>" +

            // Header
            "<tr><td style='background:linear-gradient(135deg,#6c3fc4 0%,#4e2d96 100%);" +
            "padding:32px 36px;text-align:center;'>" +
            "<p style='margin:0 0 6px 0;color:rgba(255,255,255,0.75);font-size:12px;" +
            "letter-spacing:2px;text-transform:uppercase;font-weight:600;'>New Booking Alert</p>" +
            "<h1 style='margin:0;color:#ffffff;font-size:24px;font-weight:800;line-height:1.2;'>" +
            "✦ " + clinicName + "</h1>" +
            "<p style='margin:10px 0 0 0;color:rgba(255,255,255,0.85);font-size:14px;'>" +
            "A new consultation has been requested</p>" +
            "</td></tr>" +

            // Patient Details Card
            "<tr><td style='padding:28px 36px 0;'>" +
            "<p style='margin:0 0 14px 0;font-size:11px;font-weight:700;color:#6c3fc4;" +
            "letter-spacing:1.5px;text-transform:uppercase;'>Patient Details</p>" +
            "<table width='100%' style='background:#fdf7f9;border-radius:10px;padding:4px;border-collapse:collapse;'>" +
            buildRow("👤 Full Name", a.getFullName()) +
            buildRow("📞 Phone", a.getPhoneNumber()) +
            buildRow("✉️ Email", email) +
            "</table></td></tr>" +

            // Appointment Details Card
            "<tr><td style='padding:20px 36px 0;'>" +
            "<p style='margin:0 0 14px 0;font-size:11px;font-weight:700;color:#6c3fc4;" +
            "letter-spacing:1.5px;text-transform:uppercase;'>Appointment Details</p>" +
            "<table width='100%' style='background:#fdf7f9;border-radius:10px;padding:4px;border-collapse:collapse;'>" +
            buildRow("🩺 Primary Concern", a.getPrimaryConcern()) +
            buildRow("📅 Preferred Date", date) +
            buildRow("🕐 Preferred Time", a.getPreferredTime()) +
            buildRow("📝 Notes", notes) +
            "</table></td></tr>" +

            // Action Banner
            "<tr><td style='padding:24px 36px 0;'>" +
            "<div style='background:#f0ebff;border-left:4px solid #6c3fc4;border-radius:0 8px 8px 0;" +
            "padding:14px 18px;'>" +
            "<p style='margin:0;color:#4e2d96;font-size:14px;font-weight:600;'>" +
            "⏰ Please contact the patient within 2 hours to confirm this appointment.</p>" +
            "</div></td></tr>" +

            // System Info
            "<tr><td style='padding:20px 36px 0;'>" +
            "<p style='margin:0 0 10px 0;font-size:11px;font-weight:700;color:#6c3fc4;" +
            "letter-spacing:1.5px;text-transform:uppercase;'>System Info</p>" +
            "<table width='100%' style='border-collapse:collapse;'>" +
            buildRowMuted("Appointment ID", a.getAppointmentId() != null ? a.getAppointmentId() : a.getId()) +
            buildRowMuted("Status", a.getStatus().name()) +
            buildRowMuted("Booked At", a.getCreatedAt().toString()) +
            "</table></td></tr>" +

            // Footer
            "<tr><td style='padding:28px 36px 32px;text-align:center;'>" +
            "<p style='margin:0;font-size:12px;color:#b8a8b0;'>" +
            "This is an automated notification from <strong style='color:#6c3fc4;'>" +
            clinicName + "</strong> Booking System.</p>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    private String buildRow(String label, String value) {
        return "<tr>" +
            "<td style='padding:10px 16px;font-size:13px;color:#6b6070;width:40%;font-weight:600;" +
            "vertical-align:top;border-bottom:1px solid #e8e0f5;'>" + label + "</td>" +
            "<td style='padding:10px 16px;font-size:13px;color:#1e1e1e;font-weight:500;" +
            "border-bottom:1px solid #e8e0f5;'>" + value + "</td>" +
            "</tr>";
    }

    private String buildRowMuted(String label, String value) {
        return "<tr>" +
            "<td style='padding:6px 0;font-size:12px;color:#b8a8b0;width:40%;font-weight:600;'>" +
            label + "</td>" +
            "<td style='padding:6px 0;font-size:12px;color:#6b6070;'>" + value + "</td>" +
            "</tr>";
    }
}
