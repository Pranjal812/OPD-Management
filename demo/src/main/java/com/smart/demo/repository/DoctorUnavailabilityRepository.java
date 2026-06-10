package com.smart.demo.repository;

import com.smart.demo.model.DoctorUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoctorUnavailabilityRepository extends JpaRepository<DoctorUnavailability, Long> {
    List<DoctorUnavailability> findByDoctorId(Long doctorId);
    List<DoctorUnavailability> findByUnavailableDate(LocalDate date);
    Optional<DoctorUnavailability> findByDoctorIdAndUnavailableDate(Long doctorId, LocalDate date);
    void deleteByDoctorIdAndUnavailableDate(Long doctorId, LocalDate date);
}
