CREATE DATABASE IF NOT EXISTS opd_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE opd_management;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    age INT NULL,
    gender VARCHAR(30) NULL,
    contact_number VARCHAR(30) NOT NULL UNIQUE,
    address VARCHAR(500) NULL,
    date_of_birth DATE NULL,
    registration_date DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    age INT NULL,
    gender VARCHAR(30) NULL,
    contact_number VARCHAR(30) NOT NULL UNIQUE,
    address VARCHAR(500) NULL,
    date_of_birth DATE NULL,
    specialization VARCHAR(255) NULL,
    years_of_experience VARCHAR(255) NULL,
    qualifications VARCHAR(255) NULL,
    focus_areas VARCHAR(255) NULL,
    registration_date DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date_time DATETIME NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES users(id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE IF NOT EXISTS patient_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    visit_date_time DATETIME NOT NULL,
    diagnosis VARCHAR(255) NOT NULL,
    CONSTRAINT fk_history_patient FOREIGN KEY (patient_id) REFERENCES users(id),
    CONSTRAINT fk_history_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    diagnosis TEXT NOT NULL,
    medicines JSON NOT NULL,
    advice TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_presc_patient FOREIGN KEY (patient_id) REFERENCES users(id),
    CONSTRAINT fk_presc_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE IF NOT EXISTS billing_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    billed_at DATETIME NOT NULL,
    paid_at DATETIME NULL,
    CONSTRAINT fk_billing_patient FOREIGN KEY (patient_id) REFERENCES users(id),
    CONSTRAINT fk_billing_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

-- Stores uploaded report files linked to patients
CREATE TABLE IF NOT EXISTS medical_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    patient_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    data_url LONGTEXT NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    file_size VARCHAR(255) NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_patient FOREIGN KEY (patient_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS doctor_unavailability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    doctor_name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255) NOT NULL,
    unavailable_date DATE NOT NULL,
    CONSTRAINT fk_unavail_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);
