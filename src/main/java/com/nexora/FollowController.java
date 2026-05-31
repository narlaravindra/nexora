package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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
            List<Map<String, Object>> targetUser = db.queryForList(
                "SELECT id FROM users WHERE username = ?", username);
            if (targetUser.isEmpty()) return "redirect:/feed";
            int followingId = ((Number) targetUser.get(0).get("id")).intValue();

            db.update("INSERT INTO follows (follower_id, following_id) VALUES (?, ?)",
                followerId, followingId);
            db.update("INSERT INTO notifications (user_id, from_user_id, type) VALUES (?, ?, 'follow')",
                followingId, followerId);
        } catch (Exception e) {
            List<Map<String, Object>> targetUser = db.queryForList(
                "SELECT id FROM users WHERE username = ?", username);
            if (!targetUser.isEmpty()) {
                int followingId = ((Number) targetUser.get(0).get("id")).intValue();
                db.update("DELETE FROM follows WHERE follower_id = ? AND following_id = ?",
                    followerId, followingId);
            }
        }
        return "redirect:/profile/" + username;
    }
}
