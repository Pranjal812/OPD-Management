package com.smart.demo.service;

import com.smart.demo.model.User;
import com.smart.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.smart.demo.repository.DoctorRepository doctorRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public com.smart.demo.model.Doctor registerDoctor(com.smart.demo.model.Doctor doctor) {
        doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
        return doctorRepository.save(doctor);
    }

    public Optional<?> loginUser(String emailOrUsername, String password, String role) {
        if ("DOCTOR".equalsIgnoreCase(role)) {
            Optional<com.smart.demo.model.Doctor> doctorOpt = doctorRepository.findByEmail(emailOrUsername);
            if (doctorOpt.isEmpty()) {
                doctorOpt = doctorRepository.findByUsername(emailOrUsername);
            }
            if (doctorOpt.isPresent()) {
                com.smart.demo.model.Doctor doctor = doctorOpt.get();
                if (checkPassword(password, doctor.getPassword(), doctor)) {
                    return Optional.of(doctor);
                }
            }
        } else {
            Optional<User> userOpt = userRepository.findByEmail(emailOrUsername);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(emailOrUsername);
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getRole().name().equalsIgnoreCase(role) && checkPassword(password, user.getPassword(), user)) {
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    private boolean checkPassword(String rawPassword, String storedPassword, Object entity) {
        boolean matches;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            matches = passwordEncoder.matches(rawPassword, storedPassword);
        } else {
            matches = storedPassword != null && storedPassword.equals(rawPassword);
            if (matches) {
                String encoded = passwordEncoder.encode(rawPassword);
                if (entity instanceof User) {
                    ((User) entity).setPassword(encoded);
                    userRepository.save((User) entity);
                } else if (entity instanceof com.smart.demo.model.Doctor) {
                    ((com.smart.demo.model.Doctor) entity).setPassword(encoded);
                    doctorRepository.save((com.smart.demo.model.Doctor) entity);
                }
            }
        }
        return matches;
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email) || doctorRepository.existsByEmail(email);
    }

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username) || doctorRepository.existsByUsername(username);
    }

    public boolean isContactExists(String contact) {
        return userRepository.existsByContactNumber(contact) || doctorRepository.existsByContactNumber(contact);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<com.smart.demo.model.Doctor> getDoctorByEmail(String email) {
        return doctorRepository.findByEmail(email);
    }

    public User updateProfile(Long userId, User updatedData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        applyUserUpdates(user, updatedData);
        return userRepository.save(user);
    }

    public com.smart.demo.model.Doctor updateDoctorProfile(Long doctorId, com.smart.demo.model.Doctor updatedData) {
        com.smart.demo.model.Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        
        applyDoctorUpdates(doctor, updatedData);
        return doctorRepository.save(doctor);
    }

    private void applyUserUpdates(User user, User updatedData) {
        if (updatedData.getFullName() != null && !updatedData.getFullName().isBlank()) {
            user.setFullName(updatedData.getFullName().trim());
        }
        if (updatedData.getContactNumber() != null && !updatedData.getContactNumber().isBlank()) {
            user.setContactNumber(updatedData.getContactNumber().trim());
        }
        if (updatedData.getAddress() != null) {
            user.setAddress(updatedData.getAddress().trim());
        }
        if (updatedData.getAge() != null) {
            user.setAge(updatedData.getAge());
        }
        if (updatedData.getGender() != null) {
            user.setGender(updatedData.getGender());
        }
        if (updatedData.getDateOfBirth() != null) {
            user.setDateOfBirth(updatedData.getDateOfBirth());
        }
    }

    private void applyDoctorUpdates(com.smart.demo.model.Doctor doctor, com.smart.demo.model.Doctor updatedData) {
        if (updatedData.getFullName() != null && !updatedData.getFullName().isBlank()) {
            doctor.setFullName(updatedData.getFullName().trim());
        }
        if (updatedData.getContactNumber() != null && !updatedData.getContactNumber().isBlank()) {
            doctor.setContactNumber(updatedData.getContactNumber().trim());
        }
        if (updatedData.getAddress() != null) {
            doctor.setAddress(updatedData.getAddress().trim());
        }
        if (updatedData.getAge() != null) {
            doctor.setAge(updatedData.getAge());
        }
        if (updatedData.getGender() != null) {
            doctor.setGender(updatedData.getGender());
        }
        if (updatedData.getDateOfBirth() != null) {
            doctor.setDateOfBirth(updatedData.getDateOfBirth());
        }
        if (updatedData.getSpecialization() != null && !updatedData.getSpecialization().isBlank()) {
            doctor.setSpecialization(updatedData.getSpecialization().trim());
        }
    }
}