package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class EditProfileController {

    @Autowired
    JdbcTemplate db;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @GetMapping("/edit-profile")
    public String editPage(Model model,
                           jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        String username = (String) session.getAttribute("username");
        Map<String, Object> user = db.queryForMap(
            "SELECT * FROM users WHERE username = ?", username);
        model.addAttribute("user", user);
        model.addAttribute("username", username);
        return "edit-profile";
    }

    @PostMapping("/edit-profile")
    public String saveProfile(@RequestParam String full_name,
                              @RequestParam String bio,
                              @RequestParam(required = false) MultipartFile photo,
                              jakarta.servlet.http.HttpSession session) throws Exception {
        if (session.getAttribute("username") == null) return "redirect:/login";
        String username = (String) session.getAttribute("username");

        if (photo != null && !photo.isEmpty()) {
            String ext = photo.getOriginalFilename()
                .substring(photo.getOriginalFilename().lastIndexOf("."));
            String filename = username + "_" + System.currentTimeMillis() + ext;
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.write(path, photo.getBytes());
            db.update("UPDATE users SET full_name = ?, bio = ?, photo = ? WHERE username = ?",
                      full_name, bio, "/uploads/" + filename, username);
        } else {
            db.update("UPDATE users SET full_name = ?, bio = ? WHERE username = ?",
                      full_name, bio, username);
        }
        return "redirect:/profile/" + username;
    }
}
