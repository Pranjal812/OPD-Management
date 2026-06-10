package com.smart.demo.repository;

import com.smart.demo.model.PatientHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientHistoryRepository extends JpaRepository<PatientHistory, Long> {
    List<PatientHistory> findByPatientIdOrderByVisitDateTimeDesc(Long patientId);
}
