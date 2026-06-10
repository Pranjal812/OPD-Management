package com.smart.demo.repository;

import com.smart.demo.model.Appointment;
import com.smart.demo.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId);
    
    List<Appointment> findByAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(LocalDateTime start, LocalDateTime end);

    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
            Long doctorId, LocalDateTime start, LocalDateTime end
    );

    Optional<Appointment> findFirstByDoctorIdAndAppointmentDateTimeAfterAndStatusInOrderByAppointmentDateTimeAsc(
            Long doctorId, LocalDateTime now, Collection<AppointmentStatus> statuses
    );

    long countByDoctorIdAndAppointmentDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    
    long countByAppointmentDateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
            Long doctorId, LocalDateTime start, LocalDateTime end, AppointmentStatus status
    );

    long countByDoctorId(Long doctorId);
}
