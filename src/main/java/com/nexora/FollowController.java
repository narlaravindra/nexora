package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class FollowController {

    @Autowired
    JdbcTemplate db;

    @PostMapping("/follow/{username}")
    public String follow(@PathVariable String username,
                         jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int followerId = (int) session.getAttribute("user_id");
        try {
            db.update(
                "INSERT INTO follows (follower_id, following_id) " +
                "SELECT ?, id FROM users WHERE username = ?",
                followerId, username);
        } catch (Exception e) {
            db.update(
                "DELETE FROM follows WHERE follower_id = ? AND following_id = " +
                "(SELECT id FROM users WHERE username = ?)",
                followerId, username);
        }
        return "redirect:/profile/" + username;
    }
}
