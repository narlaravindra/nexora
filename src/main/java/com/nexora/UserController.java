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

    private void sendOTP(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Nexora - Your OTP Code");
        message.setText("Your Nexora OTP is: " + otp + "\n\nThis OTP expires in 10 minutes.\n\n🌌 Nexora Team");
        mailSender.send(message);
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
            List<Map<String, Object>> existing = db.queryForList(
                "SELECT * FROM users WHERE username = ? OR email = ?", username, email);
            if (!existing.isEmpty()) {
                model.addAttribute("error", "Username or email already exists!");
                return "register";
            }

            String otp = generateOTP();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());

            db.update("INSERT INTO users (username, email, password_hash, otp, otp_expiry, is_verified) VALUES (?, ?, ?, ?, ?, false)",
                username, email, hashedPassword, otp, expiry);

            sendOTP(email, otp);
            return "redirect:/verify?email=" + email;

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/verify")
    public String verifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyOTP(@RequestParam String email,
                            @RequestParam String otp,
                            Model model) {
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE email = ? AND otp = ?", email, otp);

        if (users.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Invalid OTP!");
            return "verify";
        }

        Map<String, Object> user = users.get(0);
        LocalDateTime expiry = ((java.sql.Timestamp) user.get("otp_expiry")).toLocalDateTime();

        if (LocalDateTime.now().isAfter(expiry)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "OTP expired! Please register again.");
            return "verify";
        }

        db.update("UPDATE users SET is_verified = true, otp = null WHERE email = ?", email);
        return "redirect:/login?verified=true";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            jakarta.servlet.http.HttpSession session,
                            Model model) {
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE email = ? AND is_verified = true", email);

        if (users.isEmpty()) {
            model.addAttribute("error", "Email not found or not verified!");
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

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
