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
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            db.update("INSERT INTO users (username, email, password_hash, is_verified) VALUES (?, ?, ?, true)",
                username, email, hashedPassword);
            return "redirect:/login?verified=true";
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
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            jakarta.servlet.http.HttpSession session,
                            Model model) {
        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE email = ? AND is_verified = true", email);

        if (users.isEmpty()) {
            model.addAttribute("error", "Email not found!");
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
