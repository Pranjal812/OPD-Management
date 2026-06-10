package com.smart.demo.controller;

import com.smart.demo.model.Appointment;
import com.smart.demo.model.Role;
import com.smart.demo.model.User;
import com.smart.demo.service.HealthcareService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    
    private final HealthcareService healthcareService;
    
    public DashboardController(HealthcareService healthcareService) {
        this.healthcareService = healthcareService;
    }

    @GetMapping("/patient")
    public String patientDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || session.getAttribute("userRole") != Role.PATIENT) {
            return "redirect:/auth/login";
        }
        model.addAttribute("user", user);
        return "patient-dashboard";
    }

    @GetMapping("/doctor")
    public String doctorDashboard(HttpSession session, Model model) {
        Long doctorId = (Long) session.getAttribute("userId");
        if (doctorId == null || session.getAttribute("userRole") != Role.DOCTOR) {
            return "redirect:/auth/login";
        }
        
        com.smart.demo.model.Doctor doctor = healthcareService.getDoctorById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        
        Optional<Appointment> nextAppointment = healthcareService.getNextAppointmentForDoctor(doctor.getId());
        List<Appointment> todayAppointments = healthcareService.getTodayAppointmentsForDoctor(doctor.getId());
        List<Appointment> waitingAppointments = todayAppointments.stream()
                .filter(a -> a.getStatus() == com.smart.demo.model.AppointmentStatus.SCHEDULED)
                .toList();

        model.addAttribute("user", doctor);
        model.addAttribute("todayAppointmentsCount", healthcareService.getTodayAppointmentsCountForDoctor(doctor.getId()));
        model.addAttribute("completedTodayCount", healthcareService.getCompletedTodayCountForDoctor(doctor.getId()));
        model.addAttribute("waitingTodayCount", healthcareService.getWaitingTodayCountForDoctor(doctor.getId()));
        model.addAttribute("totalPatientsCount", healthcareService.getTotalPatientsCount());
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("waitingAppointments", waitingAppointments);
        model.addAttribute("nextAppointment", nextAppointment.orElse(null));
        model.addAttribute("allPatients", healthcareService.getAllPatients());
        return "doctor-dashboard";
    }

    @GetMapping("/staff")
    public String staffDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || session.getAttribute("userRole") != Role.STAFF) {
            return "redirect:/auth/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("todayOpdCount", healthcareService.getTodayOpdCount());
        model.addAttribute("patientsCount", healthcareService.getTotalPatientsCount());
        model.addAttribute("doctorsCount", healthcareService.getTotalDoctorsCount());
        model.addAttribute("todayAppointments", healthcareService.getTodayAppointments());
        return "staff-dashboard";
    }
}