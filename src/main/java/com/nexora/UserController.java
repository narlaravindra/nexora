package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class UserController {

    @Autowired
    JdbcTemplate db;

    private boolean isMobile(String identifier) {
        return identifier.matches("\\d{10}");
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register-auto")
    public String registerAuto(@RequestParam String username,
                               @RequestParam String identifier,
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
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());

            if (isMobile(identifier)) {
                List<Map<String, Object>> existing = db.queryForList(
                    "SELECT * FROM users WHERE username = ? OR mobile = ?", username, identifier);
                if (!existing.isEmpty()) {
                    model.addAttribute("error", "Username or mobile already exists!");
                    return "register";
                }
                String dummyEmail = username + "_" + identifier + "@nexora.mobile";
                db.update("INSERT INTO users (username, email, mobile, password_hash, is_verified) VALUES (?, ?, ?, ?, true)",
                    username, dummyEmail, identifier, hashedPassword);
            } else {
                List<Map<String, Object>> existing = db.queryForList(
                    "SELECT * FROM users WHERE username = ? OR email = ?", username, identifier);
                if (!existing.isEmpty()) {
                    model.addAttribute("error", "Username or email already exists!");
                    return "register";
                }
                db.update("INSERT INTO users (username, email, password_hash, is_verified) VALUES (?, ?, ?, true)",
                    username, identifier, hashedPassword);
            }
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
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
            model.addAttribute("error", "User not found!");
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
        model.addAttribute("error", "Password reset coming soon!");
        return "forgot-password";
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
