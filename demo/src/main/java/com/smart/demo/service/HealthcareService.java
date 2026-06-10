package com.smart.demo.service;

import com.smart.demo.model.*;
import com.smart.demo.repository.AppointmentRepository;
import com.smart.demo.repository.BillingRecordRepository;
import com.smart.demo.repository.PatientHistoryRepository;
import com.smart.demo.repository.ReportRepository;
import com.smart.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HealthcareService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientHistoryRepository patientHistoryRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final ReportRepository reportRepository;
    private final com.smart.demo.repository.PrescriptionRepository prescriptionRepository;
    private final com.smart.demo.repository.DoctorRepository doctorRepository;
    private final com.smart.demo.repository.DoctorUnavailabilityRepository doctorUnavailabilityRepository;

    public HealthcareService(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PatientHistoryRepository patientHistoryRepository,
            BillingRecordRepository billingRecordRepository,
            ReportRepository reportRepository,
            com.smart.demo.repository.PrescriptionRepository prescriptionRepository,
            com.smart.demo.repository.DoctorRepository doctorRepository,
            com.smart.demo.repository.DoctorUnavailabilityRepository doctorUnavailabilityRepository
    ) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientHistoryRepository = patientHistoryRepository;
        this.billingRecordRepository = billingRecordRepository;
        this.reportRepository = reportRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.doctorRepository = doctorRepository;
        this.doctorUnavailabilityRepository = doctorUnavailabilityRepository;
    }

    public List<com.smart.demo.model.Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Optional<com.smart.demo.model.Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }
    
    public List<User> getAllPatients() {
        return userRepository.findByRole(Role.PATIENT);
    }

    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDateTime slotDateTime, String reason) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        com.smart.demo.model.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        if (patient.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("Selected user is not a patient");
        }

        // Availability check
        if (!isDoctorAvailableOn(doctorId, slotDateTime.toLocalDate())) {
            throw new IllegalArgumentException("Doctor is not available on this date.");
        }

        // Double booking check
        boolean exists = appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
                doctorId,
                slotDateTime.minusSeconds(1),
                slotDateTime.plusSeconds(1)
        ).stream().anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED);

        if (exists) {
            throw new IllegalArgumentException("This time slot is already booked for this doctor.");
        }

        // Check if patient already has an appointment at this time
        boolean patientBusy = appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId)
                .stream()
                .anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED && a.getAppointmentDateTime().equals(slotDateTime));

        if (patientBusy) {
            throw new IllegalArgumentException("You already have another appointment at this time.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(slotDateTime);
        appointment.setReason((reason == null || reason.isBlank()) ? "General Consultation" : reason.trim());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    public List<String> getBookedSlotsForDoctorToday(Long doctorId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
                doctorId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        ).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .map(a -> a.getAppointmentDateTime().toString())
                .toList();
    }

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId);
    }

    public List<PatientHistory> getPatientHistory(Long patientId) {
        return patientHistoryRepository.findByPatientIdOrderByVisitDateTimeDesc(patientId);
    }

    public List<BillingRecord> getPatientBilling(Long patientId) {
        return billingRecordRepository.findByPatientIdOrderByBilledAtDesc(patientId);
    }
    
    public PatientHistory createPatientHistory(
            Long patientId,
            Long doctorId,
            String diagnosis,
            LocalDateTime visitDateTime
    ) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        com.smart.demo.model.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        
        if (patient.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("Selected user is not a patient");
        }
        
        PatientHistory history = new PatientHistory();
        history.setPatient(patient);
        history.setDoctor(doctor);
        history.setDiagnosis(diagnosis == null || diagnosis.isBlank() ? "General Consultation" : diagnosis.trim());
        history.setVisitDateTime(visitDateTime == null ? LocalDateTime.now() : visitDateTime);
        return patientHistoryRepository.save(history);
    }
    
    public BillingRecord createBillingRecord(
            Long patientId,
            Long appointmentId,
            String description,
            java.math.BigDecimal totalAmount,
            java.math.BigDecimal paidAmount
    ) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        if (patient.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException("Selected user is not a patient");
        }
        
        BillingRecord billing = new BillingRecord();
        billing.setPatient(patient);
        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            billing.setAppointment(appointment);
        }
        
        java.math.BigDecimal safeTotal = totalAmount == null ? java.math.BigDecimal.ZERO : totalAmount;
        java.math.BigDecimal safePaid = paidAmount == null ? java.math.BigDecimal.ZERO : paidAmount;
        billing.setDescription(description == null || description.isBlank() ? "Consultation Billing" : description.trim());
        billing.setTotalAmount(safeTotal);
        billing.setPaidAmount(safePaid);
        
        if (safePaid.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            billing.setStatus(BillingStatus.PENDING);
        } else if (safePaid.compareTo(safeTotal) < 0) {
            billing.setStatus(BillingStatus.PARTIALLY_PAID);
            billing.setPaidAt(LocalDateTime.now());
        } else {
            billing.setStatus(BillingStatus.PAID);
            billing.setPaidAt(LocalDateTime.now());
        }
        
        return billingRecordRepository.save(billing);
    }

    public boolean hasScheduledAppointment(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId)
                .stream()
                .anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED);
    }

    public Appointment markAppointmentCompleted(Long doctorId, Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (appt.getDoctor() == null || appt.getDoctor().getId() == null || !appt.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("You can only complete your own appointments");
        }

        if (appt.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Appointment is not in SCHEDULED state");
        }

        appt.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appt);
    }

    public Optional<Appointment> getNextAppointmentForDoctor(Long doctorId) {
        return appointmentRepository.findFirstByDoctorIdAndAppointmentDateTimeAfterAndStatusInOrderByAppointmentDateTimeAsc(
                doctorId,
                LocalDateTime.now(),
                List.of(AppointmentStatus.SCHEDULED)
        );
    }

    public List<Appointment> getTodayAppointmentsForDoctor(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(doctorId, start, end);
    }

    public long getTodayAppointmentsCountForDoctor(Long doctorId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.countByDoctorIdAndAppointmentDateTimeBetween(
                doctorId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    public long getCompletedTodayCountForDoctor(Long doctorId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.countByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                doctorId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                AppointmentStatus.COMPLETED
        );
    }

    public long getWaitingTodayCountForDoctor(Long doctorId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.countByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                doctorId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                AppointmentStatus.SCHEDULED
        );
    }

    public long getTotalPatientsSeenByDoctor(Long doctorId) {
        return appointmentRepository.countByDoctorId(doctorId);
    }
    
    public long getTotalPatientsCount() {
        return userRepository.countByRole(Role.PATIENT);
    }
    
    public long getTotalDoctorsCount() {
        return doctorRepository.count();
    }
    
    public long getTodayOpdCount() {
        LocalDate today = LocalDate.now();
        return appointmentRepository.countByAppointmentDateTimeBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }
    
    public List<Appointment> getTodayAppointments() {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    public void cancelAppointment(Long patientId, Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        
        if (!appt.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("Unauthorized to cancel this appointment");
        }
        
        if (appt.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only scheduled appointments can be cancelled");
        }
        
        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);
    }

    public Report saveReport(Long patientId, String fileName, String filePath, String dataUrl, String fileType, String fileSize) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        
        Report report = new Report();
        report.setPatient(patient);
        report.setPatientName(patient.getFullName());
        report.setFileName(fileName);
        report.setFilePath(filePath);
        report.setDataUrl(dataUrl);
        report.setFileType(fileType);
        report.setFileSize(fileSize);
        return reportRepository.save(report);
    }

    public List<Report> getPatientReports(Long patientId) {
        return reportRepository.findByPatientIdOrderByUploadDateDesc(patientId);
    }

    public void deleteReport(Long patientId, Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        
        if (!report.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("Unauthorized to delete this report");
        }
        
        reportRepository.delete(report);
    }

    public Prescription createPrescription(Long doctorId, Long patientId, String diagnosis, String medicinesJson, String advice) {
        com.smart.demo.model.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        
        Prescription p = new Prescription();
        p.setDoctor(doctor);
        p.setPatient(patient);
        p.setDiagnosis(diagnosis);
        p.setMedicines(medicinesJson);
        p.setAdvice(advice);
        return prescriptionRepository.save(p);
    }

    public List<Prescription> getPatientPrescriptions(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void markDoctorUnavailable(Long doctorId, LocalDate date) {
        if (doctorUnavailabilityRepository.findByDoctorIdAndUnavailableDate(doctorId, date).isPresent()) {
            return;
        }
        com.smart.demo.model.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        
        DoctorUnavailability unavail = new DoctorUnavailability();
        unavail.setDoctor(doctor);
        unavail.setDoctorName(doctor.getFullName());
        unavail.setSpecialization(doctor.getSpecialization() == null ? "General Medicine" : doctor.getSpecialization());
        unavail.setUnavailableDate(date);
        doctorUnavailabilityRepository.save(unavail);
    }

    @org.springframework.transaction.annotation.Transactional
    public void markDoctorAvailable(Long doctorId, LocalDate date) {
        doctorUnavailabilityRepository.deleteByDoctorIdAndUnavailableDate(doctorId, date);
    }

    public boolean isDoctorAvailableOn(Long doctorId, LocalDate date) {
        return doctorUnavailabilityRepository.findByDoctorIdAndUnavailableDate(doctorId, date).isEmpty();
    }

    public List<LocalDate> getUnavailableDatesForDoctor(Long doctorId) {
        return doctorUnavailabilityRepository.findByDoctorId(doctorId).stream()
                .map(DoctorUnavailability::getUnavailableDate)
                .toList();
    }

    public List<DoctorUnavailability> getAllUnavailability() {
        return doctorUnavailabilityRepository.findAll();
    }
}
