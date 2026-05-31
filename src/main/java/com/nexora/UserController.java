package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class UserController {

    @Autowired
    JdbcTemplate db;

    @Autowired
    JavaMailSender mailSender;

    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam String password,
                               Model model) {
        try {
            if (username.contains(" ")) {
                model.addAttribute("error", "Username cannot contain spaces!");
                return "register";
            }
            if (password.length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters!");
                return "register";
            }
            List<Map<String, Object>> existing = db.queryForList(
                "SELECT * FROM users WHERE username = ? OR email = ?", username, email);
            if (!existing.isEmpty()) {
                model.addAttribute("error", "Username or email already exists!");
                return "register";
            }
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            db.update("INSERT INTO users (username, email, password_hash, is_verified) VALUES (?, ?, ?, true)",
                username, email, hashedPassword);
            sendEmail(email, "Welcome to Nexora! 🌌",
                "Hi " + username + "!\n\nWelcome to Nexora — your private social platform!\n\nStart posting and connecting with friends!\n\n🌌 Nexora Team");
            return "redirect:/login?verified=true";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @PostMapping("/register-mobile")
    public String registerMobile(@RequestParam String username,
                                 @RequestParam String mobile,
                                 @RequestParam String password,
                                 Model model) {
        try {
            if (username.contains(" ")) {
                model.addAttribute("error", "Username cannot contain spaces!");
                return "register";
            }
            if (password.length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters!");
                return "register";
            }
            List<Map<String, Object>> existing = db.queryForList(
                "SELECT * FROM users WHERE username = ? OR mobile = ?", username, mobile);
            if (!existing.isEmpty()) {
                model.addAttribute("error", "Username or mobile already exists!");
                return "register";
            }
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            String otp = generateOTP();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
            db.update("INSERT INTO users (username, mobile, password_hash, is_verified, otp, otp_expiry) VALUES (?, ?, ?, false, ?, ?)",
                username, mobile, hashedPassword, otp, expiry);
            return "redirect:/verify-mobile?mobile=" + mobile + "&otp=" + otp;
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/verify-mobile")
    public String verifyMobilePage(@RequestParam String mobile,
                                   @RequestParam String otp,
                                   Model model) {
        model.addAttribute("mobile", mobile);
        model.addAttribute("otp", otp);
        return "verify-mobile";
    }

    @PostMapping("/verify-mobile")
    public String verifyMobile(@RequestParam String mobile,
                               @RequestParam String otp,
                               Model model) {
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE mobile = ? AND otp = ?", mobile, otp);
        if (users.isEmpty()) {
            model.addAttribute("mobile", mobile);
            model.addAttribute("error", "Invalid OTP!");
            return "verify-mobile";
        }
        db.update("UPDATE users SET is_verified = true, otp = null WHERE mobile = ?", mobile);
        return "redirect:/login?verified=true";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String identifier,
                            @RequestParam String password,
                            jakarta.servlet.http.HttpSession session,
                            Model model) {
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE (email = ? OR username = ? OR mobile = ?) AND is_verified = true",
            identifier, identifier, identifier);

        if (users.isEmpty()) {
            model.addAttribute("error", "User not found or not verified!");
            return "login";
        }

        Map<String, Object> user = users.get(0);
        String hashedPassword = (String) user.get("password_hash");

        if (!org.mindrot.jbcrypt.BCrypt.checkpw(password, hashedPassword)) {
            model.addAttribute("error", "Invalid password!");
            return "login";
        }

        session.setAttribute("username", user.get("username"));
        session.setAttribute("user_id", ((Number) user.get("id")).intValue());
        return "redirect:/feed";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE email = ?", email);
        if (users.isEmpty()) {
            model.addAttribute("error", "Email not found!");
            return "forgot-password";
        }
        String otp = generateOTP();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        db.update("UPDATE users SET otp = ?, otp_expiry = ? WHERE email = ?", otp, expiry, email);
        sendEmail(email, "Nexora - Password Reset OTP",
            "Your password reset OTP is: " + otp + "\n\nThis OTP expires in 10 minutes.\n\n🌌 Nexora Team");
        model.addAttribute("success", "OTP sent to " + email);
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String otp,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            model.addAttribute("email", email);
            return "reset-password";
        }
        if (newPassword.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters!");
            model.addAttribute("email", email);
            return "reset-password";
        }
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE email = ? AND otp = ?", email, otp);
        if (users.isEmpty()) {
            model.addAttribute("error", "Invalid OTP!");
            model.addAttribute("email", email);
            return "reset-password";
        }
        Map<String, Object> user = users.get(0);
        LocalDateTime expiry = ((java.sql.Timestamp) user.get("otp_expiry")).toLocalDateTime();
        if (LocalDateTime.now().isAfter(expiry)) {
            model.addAttribute("error", "OTP expired!");
            model.addAttribute("email", email);
            return "reset-password";
        }
        String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
        db.update("UPDATE users SET password_hash = ?, otp = null WHERE email = ?", hashedPassword, email);
        return "redirect:/login?reset=true";
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
