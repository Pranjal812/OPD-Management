package com.smart.demo.repository;

import com.smart.demo.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByPatientIdOrderByUploadDateDesc(Long patientId);
}
