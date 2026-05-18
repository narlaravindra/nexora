package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class CommentController {

    @Autowired
    JdbcTemplate db;

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable int id,
                           Model model,
                           jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";

        Map<String, Object> post = db.queryForMap(
            "SELECT posts.id, users.username, posts.content, posts.created_at, " +
            "COUNT(likes.id) as like_count " +
            "FROM posts JOIN users ON posts.user_id = users.id " +
            "LEFT JOIN likes ON likes.post_id = posts.id " +
            "WHERE posts.id = ? " +
            "GROUP BY posts.id, users.username, posts.content, posts.created_at", id);

        List<Map<String, Object>> comments = db.queryForList(
            "SELECT comments.id, users.username, comments.content, comments.created_at " +
            "FROM comments JOIN users ON comments.user_id = users.id " +
            "WHERE comments.post_id = ? " +
            "ORDER BY comments.created_at ASC", id);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("username", session.getAttribute("username"));
        return "comments";
    }

    @PostMapping("/comment/{postId}")
    public String addComment(@PathVariable int postId,
                             @RequestParam String content,
                             jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");
        db.update("INSERT INTO comments (user_id, post_id, content) VALUES (?, ?, ?)",
                  userId, postId, content);
        return "redirect:/post/" + postId;
    }

    @PostMapping("/comment/delete/{commentId}/{postId}")
    public String deleteComment(@PathVariable int commentId,
                                @PathVariable int postId,
                                jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");
        db.update("DELETE FROM comments WHERE id = ? AND user_id = ?", commentId, userId);
        return "redirect:/post/" + postId;
    }
}
