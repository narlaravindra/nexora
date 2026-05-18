package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired
    JdbcTemplate db;

    @GetMapping("/profile/{username}")
    public String profile(@PathVariable String username,
                          Model model,
                          jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";

        String loggedInUser = (String) session.getAttribute("username");
        int loggedInUserId = (int) session.getAttribute("user_id");

        List<Map<String, Object>> users = db.queryForList(
            "SELECT * FROM users WHERE username = ?", username);
        if (users.isEmpty()) return "redirect:/feed";

        Map<String, Object> profileUserData = users.get(0);
        int profileUserId = ((Number) profileUserData.get("id")).intValue();

        List<Map<String, Object>> posts = db.queryForList(
            "SELECT posts.id, posts.content, posts.created_at, COUNT(likes.id) as like_count " +
            "FROM posts LEFT JOIN likes ON likes.post_id = posts.id " +
            "WHERE posts.user_id = ? " +
            "GROUP BY posts.id ORDER BY posts.created_at DESC", profileUserId);

        int followers = db.queryForObject(
            "SELECT COUNT(*) FROM follows WHERE following_id = ?",
            Integer.class, profileUserId);

        int following = db.queryForObject(
            "SELECT COUNT(*) FROM follows WHERE follower_id = ?",
            Integer.class, profileUserId);

        boolean isFollowing = db.queryForObject(
            "SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ?",
            Integer.class, loggedInUserId, profileUserId) > 0;

        model.addAttribute("profileUser", username);
        model.addAttribute("fullName", profileUserData.get("full_name"));
        model.addAttribute("bio", profileUserData.get("bio"));
        model.addAttribute("photo", profileUserData.get("photo"));
        model.addAttribute("postCount", posts.size());
        model.addAttribute("followers", followers);
        model.addAttribute("following", following);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("isOwnProfile", loggedInUser.equals(username));
        model.addAttribute("posts", posts);
        model.addAttribute("username", loggedInUser);
        return "profile";
    }
}
