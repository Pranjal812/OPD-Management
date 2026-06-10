package com.smart.demo.controller;

import com.smart.demo.model.*;
import com.smart.demo.service.HealthcareService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OpdApiController {

    private final HealthcareService healthcareService;
    private final com.smart.demo.service.UserService userService;
    private final com.smart.demo.repository.DoctorRepository doctorRepository;

    public OpdApiController(HealthcareService healthcareService, com.smart.demo.service.UserService userService, com.smart.demo.repository.DoctorRepository doctorRepository) {
        this.healthcareService = healthcareService;
        this.userService = userService;
        this.doctorRepository = doctorRepository;
    }

    private Long getAuthenticatedUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    private Role getAuthenticatedUserRole(HttpSession session) {
        return (Role) session.getAttribute("userRole");
    }

    private void checkRole(HttpSession session, Role requiredRole) {
        Long userId = getAuthenticatedUserId(session);
        Role userRole = getAuthenticatedUserRole(session);
        
        if (userId == null || userRole == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (userRole != requiredRole) {
            throw new IllegalArgumentException("Unauthorized: " + requiredRole + " role required");
        }
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = ex.getMessage().contains("required") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        if (ex.getMessage().contains("Unauthorized")) status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<?> handleDateTimeParse(java.time.format.DateTimeParseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid date/time format"));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        String msg = ex.getMessage() == null ? "Server error" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
    }

    @PostMapping("/patient/profile")
    public ResponseEntity<?> updateProfile(HttpSession session, @RequestBody User updatedData) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);
        User updated = userService.updateProfile(userId, updatedData);
        session.setAttribute("loggedInUser", updated);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PostMapping("/doctor/profile")
    public ResponseEntity<?> updateDoctorProfile(HttpSession session, @RequestBody com.smart.demo.model.Doctor updatedData) {
        checkRole(session, Role.DOCTOR);
        Long doctorId = getAuthenticatedUserId(session);
        com.smart.demo.model.Doctor updated = userService.updateDoctorProfile(doctorId, updatedData);
        session.setAttribute("loggedInUser", updated);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @GetMapping("/patient/doctors")
    public ResponseEntity<?> getDoctors(HttpSession session) {
        checkRole(session, Role.PATIENT);

        List<DoctorResponse> doctors = healthcareService.getAllDoctors().stream()
                .map(doctor -> new DoctorResponse(
                        doctor.getId(),
                        doctor.getFullName(),
                        doctor.getSpecialization() == null ? "General Medicine" : doctor.getSpecialization(),
                        doctor.getUsername(),
                        doctor.getEmail(),
                        doctor.getContactNumber(),
                        doctor.getAge(),
                        doctor.getGender() == null ? null : doctor.getGender().name(),
                        doctor.getAddress(),
                        doctor.getDateOfBirth(),
                        healthcareService.getWaitingTodayCountForDoctor(doctor.getId()),
                        healthcareService.getBookedSlotsForDoctorToday(doctor.getId()),
                        healthcareService.getUnavailableDatesForDoctor(doctor.getId()).stream().map(Object::toString).toList()
                ))
                .toList();

        return ResponseEntity.ok(doctors);
    }

    @PostMapping("/patient/appointments")
    public ResponseEntity<?> bookAppointment(
            HttpSession session,
            @RequestBody BookAppointmentRequest request
    ) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);

        if (request.doctorId() == null || request.appointmentDateTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "doctorId and appointmentDateTime are required"));
        }

        Appointment appointment = healthcareService.bookAppointment(
                userId,
                request.doctorId(),
                LocalDateTime.parse(request.appointmentDateTime()),
                request.reason()
        );
        return ResponseEntity.ok(Map.of(
                "message", "Appointment booked successfully",
                "appointmentId", appointment.getId()
        ));
    }

    @PostMapping("/patient/appointments/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(HttpSession session, @PathVariable Long id) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);
        healthcareService.cancelAppointment(userId, id);
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully"));
    }

    @GetMapping("/patient/appointments")
    public ResponseEntity<?> getPatientAppointments(HttpSession session) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);

        List<AppointmentResponse> data = healthcareService.getPatientAppointments(userId).stream()
                .map(a -> new AppointmentResponse(
                        a.getId(),
                        a.getDoctor().getFullName(),
                        a.getDoctor().getSpecialization() == null ? "General Medicine" : a.getDoctor().getSpecialization(),
                        a.getAppointmentDateTime().toString(),
                        a.getStatus().name(),
                        a.getReason()
                ))
                .toList();

        return ResponseEntity.ok(data);
    }

    @GetMapping("/patient/reports")
    public ResponseEntity<?> getPatientReports(HttpSession session) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);
        List<ReportResponse> reports = healthcareService.getPatientReports(userId).stream()
                .map(r -> new ReportResponse(r.getId(), r.getFileName(), r.getFilePath(), r.getDataUrl(), r.getFileType(), r.getFileSize()))
                .toList();
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/patient/reports")
    public ResponseEntity<?> uploadReport(HttpSession session, @RequestBody ReportUploadRequest request) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);
        healthcareService.saveReport(userId, request.fileName(), request.filePath(), request.dataUrl(), request.fileType(), request.fileSize());
        return ResponseEntity.ok(Map.of("message", "Report record saved successfully"));
    }

    @PostMapping("/staff/reports")
    public ResponseEntity<?> uploadReportByStaff(HttpSession session, @RequestBody ReportUploadRequest request) {
        checkRole(session, Role.STAFF);
        if (request.patientId() == null) throw new IllegalArgumentException("patientId is required");
        healthcareService.saveReport(request.patientId(), request.fileName(), request.filePath(), request.dataUrl(), request.fileType(), request.fileSize());
        return ResponseEntity.ok(Map.of("message", "Report uploaded for patient successfully"));
    }

    @DeleteMapping("/patient/reports/{id}")
    public ResponseEntity<?> deleteReport(HttpSession session, @PathVariable Long id) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);
        healthcareService.deleteReport(userId, id);
        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }

    @GetMapping("/doctor/patients/{patientId}/reports")
    public ResponseEntity<?> getPatientReportsForDoctor(HttpSession session, @PathVariable Long patientId) {
        checkRole(session, Role.DOCTOR);
        List<ReportResponse> reports = healthcareService.getPatientReports(patientId).stream()
                .map(r -> new ReportResponse(r.getId(), r.getFileName(), r.getFilePath(), r.getDataUrl(), r.getFileType(), r.getFileSize()))
                .toList();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/doctor/patients/{patientId}/history")
    public ResponseEntity<?> getPatientHistoryForDoctor(HttpSession session, @PathVariable Long patientId) {
        checkRole(session, Role.DOCTOR);
        List<HistoryResponse> data = healthcareService.getPatientHistory(patientId).stream()
                .map(h -> new HistoryResponse(
                        h.getId(),
                        h.getDoctor().getFullName(),
                        h.getVisitDateTime().toString(),
                        h.getDiagnosis()
                ))
                .toList();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/doctor/prescriptions")
    public ResponseEntity<?> savePrescription(HttpSession session, @RequestBody CreatePrescriptionRequest request) {
        checkRole(session, Role.DOCTOR);
        Long doctorId = getAuthenticatedUserId(session);
        Prescription p = healthcareService.createPrescription(
                doctorId,
                request.patientId(),
                request.diagnosis(),
                request.medicinesJson(),
                request.advice()
        );
        return ResponseEntity.ok(Map.of("message", "Prescription saved successfully", "id", p.getId()));
    }

    @PostMapping("/staff/prescriptions")
    public ResponseEntity<?> savePrescriptionByStaff(HttpSession session, @RequestBody CreatePrescriptionRequest request) {
        checkRole(session, Role.STAFF);
        // For staff, we use the doctorId provided in the request
        if (request.doctorId() == null) throw new IllegalArgumentException("doctorId is required");
        Prescription p = healthcareService.createPrescription(
                request.doctorId(),
                request.patientId(),
                request.diagnosis(),
                request.medicinesJson(),
                request.advice()
        );
        return ResponseEntity.ok(Map.of("message", "Prescription saved successfully", "id", p.getId()));
    }

    @GetMapping("/patient/prescriptions")
    public ResponseEntity<?> getPatientPrescriptions(HttpSession session) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);
        List<PrescriptionResponse> list = healthcareService.getPatientPrescriptions(userId).stream()
                .map(p -> new PrescriptionResponse(
                        p.getId(),
                        p.getDoctor().getFullName(),
                        p.getDiagnosis(),
                        p.getMedicines(),
                        p.getAdvice(),
                        p.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/staff/patients/{patientId}/reports")
    public ResponseEntity<?> getPatientReportsForStaff(HttpSession session, @PathVariable Long patientId) {
        checkRole(session, Role.STAFF);
        List<ReportResponse> reports = healthcareService.getPatientReports(patientId).stream()
                .map(r -> new ReportResponse(r.getId(), r.getFileName(), r.getFilePath(), r.getDataUrl(), r.getFileType(), r.getFileSize()))
                .toList();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/staff/unavailability")
    public ResponseEntity<?> getAllUnavailability(HttpSession session) {
        checkRole(session, Role.STAFF);
        List<DoctorUnavailabilityResponse> data = healthcareService.getAllUnavailability().stream()
                .map(u -> new DoctorUnavailabilityResponse(u.getDoctor().getId(), u.getDoctor().getFullName(), u.getUnavailableDate().toString()))
                .toList();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/staff/unavailability")
    public ResponseEntity<?> markUnavailable(HttpSession session, @RequestBody UnavailabilityRequest request) {
        checkRole(session, Role.STAFF);
        healthcareService.markDoctorUnavailable(request.doctorId(), java.time.LocalDate.parse(request.date()));
        return ResponseEntity.ok(Map.of("message", "Doctor marked as unavailable"));
    }

    @DeleteMapping("/staff/unavailability/{doctorId}/{date}")
    public ResponseEntity<?> markAvailable(HttpSession session, @PathVariable Long doctorId, @PathVariable String date) {
        checkRole(session, Role.STAFF);
        healthcareService.markDoctorAvailable(doctorId, java.time.LocalDate.parse(date));
        return ResponseEntity.ok(Map.of("message", "Doctor marked as available"));
    }

    public record ReportUploadRequest(Long patientId, String fileName, String filePath, String dataUrl, String fileType, String fileSize) {}
    public record ReportResponse(Long id, String fileName, String filePath, String dataUrl, String fileType, String fileSize) {}
    public record DoctorUnavailabilityResponse(Long doctorId, String doctorName, String date) {}
    public record UnavailabilityRequest(Long doctorId, String date) {}
    public record CreatePrescriptionRequest(Long patientId, Long doctorId, String diagnosis, String medicinesJson, String advice) {}
    public record PrescriptionResponse(Long id, String doctorName, String diagnosis, String medicinesJson, String advice, String createdAt) {}

    @GetMapping("/patient/history")
    public ResponseEntity<?> getPatientHistory(HttpSession session) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);

        List<HistoryResponse> data = healthcareService.getPatientHistory(userId).stream()
                .map(h -> new HistoryResponse(
                        h.getId(),
                        h.getDoctor().getFullName(),
                        h.getVisitDateTime().toString(),
                        h.getDiagnosis()
                ))
                .toList();

        return ResponseEntity.ok(data);
    }

    @GetMapping("/patient/billing")
    public ResponseEntity<?> getPatientBilling(HttpSession session) {
        checkRole(session, Role.PATIENT);
        Long userId = getAuthenticatedUserId(session);

        List<BillingResponse> data = healthcareService.getPatientBilling(userId).stream()
                .map(b -> new BillingResponse(
                        b.getId(),
                        b.getDescription(),
                        b.getTotalAmount(),
                        b.getPaidAmount(),
                        b.getStatus().name(),
                        b.getBilledAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(data);
    }
    
    @GetMapping("/staff/patients")
    public ResponseEntity<?> getPatients(HttpSession session) {
        checkRole(session, Role.STAFF);
        
        List<SimpleUserResponse> patients = healthcareService.getAllPatients().stream()
                .map(p -> new SimpleUserResponse(
                        p.getId(),
                        p.getFullName(),
                        p.getEmail(),
                        p.getUsername(),
                        p.getContactNumber(),
                        p.getAge(),
                        p.getGender() == null ? null : p.getGender().name(),
                        p.getAddress(),
                        p.getDateOfBirth()
                ))
                .toList();
        return ResponseEntity.ok(patients);
    }
    
    @GetMapping("/staff/doctors")
    public ResponseEntity<?> getDoctorsForStaff(HttpSession session) {
        checkRole(session, Role.STAFF);
        
        List<DoctorResponse> doctors = healthcareService.getAllDoctors().stream()
                .map(d -> new DoctorResponse(
                        d.getId(),
                        d.getFullName(),
                        d.getSpecialization() == null ? "General Medicine" : d.getSpecialization(),
                        d.getUsername(),
                        d.getEmail(),
                        d.getContactNumber(),
                        d.getAge(),
                        d.getGender() == null ? null : d.getGender().name(),
                        d.getAddress(),
                        d.getDateOfBirth(),
                        healthcareService.getWaitingTodayCountForDoctor(d.getId()),
                        healthcareService.getBookedSlotsForDoctorToday(d.getId()),
                        healthcareService.getUnavailableDatesForDoctor(d.getId()).stream().map(Object::toString).toList()
                ))
                .toList();
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/staff/appointments/today")
    public ResponseEntity<?> getTodaysAppointmentsForStaff(HttpSession session) {
        checkRole(session, Role.STAFF);

        List<TodaysAppointmentResponse> data = healthcareService.getTodayAppointments().stream()
                .map(a -> new TodaysAppointmentResponse(
                        a.getId(),
                        a.getAppointmentDateTime().toString(),
                        a.getPatient().getFullName(),
                        a.getDoctor().getFullName(),
                        a.getReason(),
                        a.getStatus().name()
                ))
                .toList();

        return ResponseEntity.ok(data);
    }
    
    @PostMapping("/staff/history")
    public ResponseEntity<?> createHistory(HttpSession session, @RequestBody CreateHistoryRequest request) {
        checkRole(session, Role.STAFF);
        if (request.patientId() == null || request.doctorId() == null || request.diagnosis() == null || request.diagnosis().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "patientId, doctorId and diagnosis are required"));
        }
        
        PatientHistory history = healthcareService.createPatientHistory(
                request.patientId(),
                request.doctorId(),
                request.diagnosis(),
                request.visitDateTime() == null || request.visitDateTime().isBlank() ? null : LocalDateTime.parse(request.visitDateTime())
        );
        return ResponseEntity.ok(Map.of("message", "History record created", "historyId", history.getId()));
    }
    
    @PostMapping("/staff/billing")
    public ResponseEntity<?> createBilling(HttpSession session, @RequestBody CreateBillingRequest request) {
        checkRole(session, Role.STAFF);
        if (request.patientId() == null || request.totalAmount() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "patientId and totalAmount are required"));
        }
        
        BillingRecord billing = healthcareService.createBillingRecord(
                request.patientId(),
                request.appointmentId(),
                request.description(),
                request.totalAmount(),
                request.paidAmount()
        );
        return ResponseEntity.ok(Map.of("message", "Billing record created", "billingId", billing.getId()));
    }

    public record BookAppointmentRequest(Long doctorId, String appointmentDateTime, String reason) {
    }

    public record DoctorResponse(
            Long id,
            String name,
            String specialization,
            String username,
            String email,
            String contactNumber,
            Integer age,
            String gender,
            String address,
            java.time.LocalDate dateOfBirth,
            Long waitingCount,
            List<String> bookedSlots,
            List<String> unavailableDates
    ) {}

    public record AppointmentResponse(
            Long id,
            String doctorName,
            String specialization,
            String appointmentDateTime,
            String status,
            String reason
    ) {}

    public record HistoryResponse(
            Long id,
            String doctorName,
            String visitDateTime,
            String diagnosis
    ) {}

    public record BillingResponse(
            Long id,
            String description,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            String status,
            String billedAt
    ) {}
    
    public record SimpleUserResponse(
            Long id,
            String name,
            String email,
            String username,
            String contactNumber,
            Integer age,
            String gender,
            String address,
            java.time.LocalDate dateOfBirth
    ) {}
    
    public record CreateHistoryRequest(
            Long patientId,
            Long doctorId,
            String diagnosis,
            String visitDateTime
    ) {}
    
    public record CreateBillingRequest(
            Long patientId,
            Long appointmentId,
            String description,
            BigDecimal totalAmount,
            BigDecimal paidAmount
    ) {}

    @PostMapping("/doctor/appointments/{appointmentId}/complete")
    public ResponseEntity<?> completeAppointment(
            HttpSession session,
            @PathVariable Long appointmentId
    ) {
        checkRole(session, Role.DOCTOR);
        Long doctorId = getAuthenticatedUserId(session);

        Appointment updated = healthcareService.markAppointmentCompleted(doctorId, appointmentId);
        return ResponseEntity.ok(Map.of("message", "Appointment marked COMPLETED", "appointmentId", updated.getId()));
    }

    public record TodaysAppointmentResponse(
            Long id,
            String appointmentDateTime,
            String patientName,
            String doctorName,
            String reason,
            String status
    ) {}
}
