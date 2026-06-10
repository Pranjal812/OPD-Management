package com.smart.demo.controller;

import com.smart.demo.model.Gender;
import com.smart.demo.model.Role;
import com.smart.demo.model.User;
import com.smart.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        model.addAttribute("genders", Gender.values());
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam("fullName") String fullName,
                               @RequestParam("username") String username,
                               @RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               @RequestParam("role") String roleStr,
                               @RequestParam("age") Integer age,
                               @RequestParam("gender") String genderStr,
                               @RequestParam("contactNumber") String contactNumber,
                               @RequestParam("address") String address,
                               @RequestParam(value = "dateOfBirth", required = false) LocalDate dateOfBirth,
                               @RequestParam(value = "specialization", required = false) String specialization,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        // Manual validation
        if (fullName == null || fullName.trim().isEmpty()) {
            model.addAttribute("error", "Full name is required!");
            return "signup";
        }

        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Username is required!");
            return "signup";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Email is required!");
            return "signup";
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters!");
            return "signup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "signup";
        }

        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            model.addAttribute("error", "Contact number is required!");
            return "signup";
        }

        // Validate contact number length and format
        String cleanContact = contactNumber.trim().replaceAll("\\D", "");
        if (cleanContact.length() != 10) {
            model.addAttribute("error", "Contact number must be exactly 10 digits!");
            return "signup";
        }

        // Convert role string to enum
        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid role selected!");
            return "signup";
        }

        // Convert gender string to enum
        Gender gender;
        try {
            gender = Gender.valueOf(genderStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid gender selected!");
            return "signup";
        }

        // Check if username exists
        if (userService.isUsernameExists(username)) {
            model.addAttribute("error", "Username already taken!");
            return "signup";
        }

        // Check if email exists
        if (userService.isEmailExists(email)) {
            model.addAttribute("error", "Email already registered!");
            return "signup";
        }

        // Check if contact exists
        if (userService.isContactExists(contactNumber)) {
            model.addAttribute("error", "Contact number already registered!");
            return "signup";
        }

        try {
            if (role == Role.DOCTOR) {
                com.smart.demo.model.Doctor doctor = new com.smart.demo.model.Doctor();
                doctor.setFullName(fullName);
                doctor.setUsername(username);
                doctor.setEmail(email);
                doctor.setPassword(password);
                doctor.setAge(age);
                doctor.setGender(gender);
                doctor.setContactNumber(contactNumber);
                doctor.setAddress(address);
                doctor.setDateOfBirth(dateOfBirth);
                doctor.setSpecialization((specialization == null || specialization.isBlank()) ? "General Medicine" : specialization.trim());
                doctor.setRegistrationDate(LocalDateTime.now());
                userService.registerDoctor(doctor);
            } else {
                User user = new User();
                user.setFullName(fullName);
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(password);
                user.setRole(role);
                user.setAge(age);
                user.setGender(gender);
                user.setContactNumber(contactNumber);
                user.setAddress(address);
                user.setDateOfBirth(dateOfBirth);
                // specialization is not set for non-doctor roles as it's not in the User model anymore
                user.setRegistrationDate(LocalDateTime.now());
                userService.registerUser(user);
            }
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") != null) {
            Role role = (Role) session.getAttribute("userRole");
            return getRedirectBasedOnRole(role);
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String emailOrUsername,
                            @RequestParam("password") String password,
                            @RequestParam("role") String role,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            model.addAttribute("error", "Email/Username is required!");
            return "login";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Password is required!");
            return "login";
        }

        Optional<?> userOptional = userService.loginUser(emailOrUsername, password, role);

        if (userOptional.isPresent()) {
            Object user = userOptional.get();
            session.setAttribute("loggedInUser", user);
            
            if (user instanceof User) {
                User u = (User) user;
                session.setAttribute("userId", u.getId());
                session.setAttribute("userRole", u.getRole());
                return getRedirectBasedOnRole(u.getRole());
            } else if (user instanceof com.smart.demo.model.Doctor) {
                com.smart.demo.model.Doctor d = (com.smart.demo.model.Doctor) user;
                session.setAttribute("userId", d.getId());
                session.setAttribute("userRole", Role.DOCTOR);
                return getRedirectBasedOnRole(Role.DOCTOR);
            }
        }
        
        model.addAttribute("error", "Invalid credentials or role mismatch!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private String getRedirectBasedOnRole(Role role) {
        switch (role) {
            case PATIENT:
                return "redirect:/dashboard/patient";
            case DOCTOR:
                return "redirect:/dashboard/doctor";
            case STAFF:
                return "redirect:/dashboard/staff";
            default:
                return "redirect:/";
        }
    }
}