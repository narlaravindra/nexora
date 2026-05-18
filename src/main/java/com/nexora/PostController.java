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
import java.util.List;
import java.util.Map;

@Controller
public class PostController {

    @Autowired
    JdbcTemplate db;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @GetMapping("/feed")
    public String feed(Model model, jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        String username = (String) session.getAttribute("username");
        int userId = (int) session.getAttribute("user_id");

        List<Map<String, Object>> posts = db.queryForList(
            "SELECT posts.id, users.username, users.photo as user_photo, " +
            "posts.content, posts.image, posts.created_at, " +
            "COUNT(likes.id) as like_count, " +
            "MAX(CASE WHEN likes.user_id = ? THEN 1 ELSE 0 END) as liked " +
            "FROM posts JOIN users ON posts.user_id = users.id " +
            "LEFT JOIN likes ON likes.post_id = posts.id " +
            "GROUP BY posts.id, users.username, users.photo, posts.content, posts.image, posts.created_at " +
            "ORDER BY posts.created_at DESC", userId);

        model.addAttribute("posts", posts);
        model.addAttribute("username", username);
        return "feed";
    }

    @GetMapping("/post")
    public String postPage(jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        return "post";
    }

    @PostMapping("/post")
    public String createPost(@RequestParam(required = false) String content,
                             @RequestParam(required = false) MultipartFile image,
                             jakarta.servlet.http.HttpSession session) throws Exception {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");
        String username = (String) session.getAttribute("username");

        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            String ext = image.getOriginalFilename()
                .substring(image.getOriginalFilename().lastIndexOf("."));
            String filename = "post_" + username + "_" + System.currentTimeMillis() + ext;
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.write(path, image.getBytes());
            imagePath = "/uploads/" + filename;
        }

        db.update("INSERT INTO posts (user_id, content, image) VALUES (?, ?, ?)",
                  userId, content, imagePath);
        return "redirect:/feed";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable int id,
                             jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");
        db.update("DELETE FROM posts WHERE id = ? AND user_id = ?", id, userId);
        return "redirect:/feed";
    }

    @PostMapping("/like/{id}")
    public String likePost(@PathVariable int id,
                           jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");
        try {
            db.update("INSERT INTO likes (user_id, post_id) VALUES (?, ?)", userId, id);
        } catch (Exception e) {
            db.update("DELETE FROM likes WHERE user_id = ? AND post_id = ?", userId, id);
        }
        return "redirect:/feed";
    }
}
