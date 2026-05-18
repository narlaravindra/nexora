package com.nexora;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Controller
public class EditProfileController {

    @Autowired
    JdbcTemplate db;

    @Autowired
    Cloudinary cloudinary;

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
            Map uploadResult = cloudinary.uploader().upload(
                photo.getBytes(),
                ObjectUtils.asMap("folder", "nexora/profiles")
            );
            String photoUrl = (String) uploadResult.get("secure_url");
            db.update("UPDATE users SET full_name = ?, bio = ?, photo = ? WHERE username = ?",
                      full_name, bio, photoUrl, username);
        } else {
            db.update("UPDATE users SET full_name = ?, bio = ? WHERE username = ?",
                      full_name, bio, username);
        }
        return "redirect:/profile/" + username;
    }
}
