# Devs Hair and Skin Clinic — Backend API

Production-ready Spring Boot backend for the appointment booking system.
Patient books appointment on frontend → saved to MongoDB → clinic notified by email.

---

## Tech Stack

| Layer       | Technology              |
|-------------|--------------------------|
| Language    | Java 17                 |
| Framework   | Spring Boot 3.3.4       |
| Database    | MongoDB                 |
| Email       | Spring Boot Mail (SMTP) |
| Validation  | Jakarta Bean Validation |
| Build Tool  | Maven                   |

---

## Folder Structure

```
devs-clinic-backend/
├── pom.xml
└── src/
    └── main/
        ├── java/com/devsclinic/backend/
        │   ├── DevsClinicBackendApplication.java   # main entry point
        │   ├── controller/
        │   │   ├── AppointmentController.java      # REST APIs
        │   │   └── HealthController.java           # health check
        │   ├── service/
        │   │   ├── AppointmentService.java         # business logic
        │   │   └── EmailService.java               # email sending
        │   ├── repository/
        │   │   └── AppointmentRepository.java      # MongoDB data access
        │   ├── model/
        │   │   └── Appointment.java                # MongoDB document/entity
        │   ├── dto/
        │   │   ├── AppointmentRequest.java          # incoming request + validation
        │   │   ├── AppointmentResponse.java          # outgoing response
        │   │   └── ApiResponse.java                  # standard response envelope
        │   ├── exception/
        │   │   ├── ResourceNotFoundException.java
        │   │   ├── EmailSendException.java
        │   │   └── GlobalExceptionHandler.java       # centralized error handling
        │   └── config/
        │       └── CorsConfig.java                   # CORS setup
        └── resources/
            └── application.properties                # configuration
```

---

## Setup Instructions

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB running locally (`mongodb://localhost:27017`) or a MongoDB Atlas cluster
- A Gmail account with an **App Password** generated (Google Account → Security → 2-Step Verification → App Passwords)

### 2. Configure `application.properties`
Update these values in `src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/devs_clinic_db
spring.mail.username=your-sending-account@gmail.com
spring.mail.password=your-16-char-app-password
clinic.notification.email=vipluved@gmail.com
cors.allowed-origins=http://localhost:3000,https://your-production-domain.com
```

> ⚠️ **Never commit real credentials to git.** In production, use environment
> variables instead (Spring Boot auto-maps `SPRING_MAIL_USERNAME`,
> `SPRING_MAIL_PASSWORD`, `SPRING_DATA_MONGODB_URI`, etc.)

### 3. Run the application
```bash
cd devs-clinic-backend
mvn spring-boot:run
```
Server starts on `http://localhost:8080`

### 4. Verify health
```bash
curl http://localhost:8080/api/health
```

---

## API Reference

Base URL: `http://localhost:8080/api/appointments`

### 1. Book an Appointment
**`POST /api/appointments`**

Request body:
```json
{
  "fullName": "Priya Raman",
  "phoneNumber": "+91 98765 43210",
  "emailAddress": "priya.raman@example.com",
  "primaryConcern": "Acne & Pimples",
  "preferredDate": "2026-07-05",
  "preferredTime": "10:00 AM – 11:00 AM",
  "additionalNotes": "Sensitive skin, currently on no medication."
}
```

Success response — `201 Created`:
```json
{
  "success": true,
  "message": "Appointment booked successfully. Our team will contact you shortly.",
  "data": {
    "id": "6663f1a2b9c8e2451a9f9e21",
    "fullName": "Priya Raman",
    "phoneNumber": "+91 98765 43210",
    "emailAddress": "priya.raman@example.com",
    "primaryConcern": "Acne & Pimples",
    "preferredDate": "2026-07-05",
    "preferredTime": "10:00 AM – 11:00 AM",
    "additionalNotes": "Sensitive skin, currently on no medication.",
    "status": "PENDING",
    "emailNotificationSent": true,
    "createdAt": "2026-06-30T14:22:10.512"
  },
  "timestamp": "2026-06-30T14:22:10.601"
}
```

Validation error response — `400 Bad Request`:
```json
{
  "success": false,
  "message": "Validation failed. Please check the highlighted fields.",
  "data": {
    "fullName": "Full name is required",
    "phoneNumber": "Phone number must be a valid format (10-15 digits, may include +, spaces or hyphens)",
    "preferredDate": "Preferred date cannot be in the past"
  },
  "timestamp": "2026-06-30T14:22:10.601"
}
```

---

### 2. Get Appointment by ID
**`GET /api/appointments/{id}`**

```bash
curl http://localhost:8080/api/appointments/6663f1a2b9c8e2451a9f9e21
```

---

### 3. Get All Appointments (Admin/Staff)
**`GET /api/appointments`**

```bash
curl http://localhost:8080/api/appointments
```

---

### 4. Search Appointments by Phone Number
**`GET /api/appointments/search?phone=9876543210`**

```bash
curl "http://localhost:8080/api/appointments/search?phone=9876543210"
```

---

### 5. Update Appointment Status
**`PATCH /api/appointments/{id}/status?status=CONFIRMED`**

Allowed values: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`

```bash
curl -X PATCH "http://localhost:8080/api/appointments/6663f1a2b9c8e2451a9f9e21/status?status=CONFIRMED"
```

---

### 6. Resend Notification Email
**`POST /api/appointments/{id}/resend-email`**

Use if the original email failed to deliver (`emailNotificationSent: false`).

```bash
curl -X POST http://localhost:8080/api/appointments/6663f1a2b9c8e2451a9f9e21/resend-email
```

---

### 7. Delete an Appointment
**`DELETE /api/appointments/{id}`**

```bash
curl -X DELETE http://localhost:8080/api/appointments/6663f1a2b9c8e2451a9f9e21
```

---

## Backend Flow (as implemented)

```
Patient fills appointment form (React frontend)
        │
        ▼
POST /api/appointments  (AppointmentController)
        │
        ▼
@Valid validates AppointmentRequest
        │  (fails → 400 with field errors, nothing saved)
        ▼
AppointmentService.bookAppointment()
        │
        ├──► 1. Build Appointment entity
        ├──► 2. appointmentRepository.save()  →  MongoDB  (ALWAYS happens)
        ├──► 3. emailService.sendAppointmentNotification()
        │         │
        │         ├── success → emailNotificationSent = true, re-save
        │         └── failure → logged, booking still succeeds
        │                       (emailNotificationSent stays false,
        │                        recoverable via /resend-email)
        ▼
AppointmentResponse returned to frontend wrapped in ApiResponse
```

### Why save-before-email?
If the email step ran first and failed, a patient's appointment could be
silently lost. By persisting to MongoDB first, **every booking is
guaranteed to be saved** — email delivery is treated as a best-effort
side-effect with a manual retry path (`/resend-email`), not a blocking
dependency.

---

## Frontend Integration Example (React)

```javascript
const bookAppointment = async (formData) => {
  const response = await fetch("http://localhost:8080/api/appointments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      fullName: formData.name,
      phoneNumber: formData.phone,
      emailAddress: formData.email,
      primaryConcern: formData.concern,
      preferredDate: formData.date,   // "YYYY-MM-DD"
      preferredTime: formData.time,
      additionalNotes: formData.msg,
    }),
  });

  const result = await response.json();

  if (result.success) {
    // show success UI
  } else {
    // show result.data (field errors) or result.message
  }
};
```

---

## Security Notes for Production

- Move SMTP and MongoDB credentials to environment variables / a secrets manager — never hardcode in `application.properties` committed to git.
- Restrict `cors.allowed-origins` to your real production domain only.
- Consider adding rate-limiting on `POST /api/appointments` to prevent spam bookings.
- Add authentication (e.g. Spring Security + JWT) before exposing `GET /api/appointments` (all bookings) and the status/delete endpoints to anyone other than clinic staff.
