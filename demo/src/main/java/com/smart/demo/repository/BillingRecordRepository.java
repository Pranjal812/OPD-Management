package com.smart.demo.repository;

import com.smart.demo.model.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {
    List<BillingRecord> findByPatientIdOrderByBilledAtDesc(Long patientId);
}
