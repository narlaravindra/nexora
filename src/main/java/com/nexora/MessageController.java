package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class MessageController {

    @Autowired
    JdbcTemplate db;

    @GetMapping("/messages")
    public String messages(Model model, jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");

        List<Map<String, Object>> conversations = db.queryForList(
            "SELECT DISTINCT ON (other_user) other_user, username, photo, last_message, last_time " +
            "FROM ( " +
            "SELECT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END as other_user, " +
            "content as last_message, created_at as last_time " +
            "FROM messages WHERE sender_id = ? OR receiver_id = ? " +
            "ORDER BY created_at DESC) m " +
            "JOIN users ON users.id = m.other_user " +
            "ORDER BY other_user, last_time DESC", userId, userId, userId);

        model.addAttribute("conversations", conversations);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("userId", userId);
        return "messages";
    }

    @GetMapping("/messages/{username}")
    public String chat(@PathVariable String username, Model model,
                       jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");

        List<Map<String, Object>> otherUser = db.queryForList(
            "SELECT * FROM users WHERE username = ?", username);
        if (otherUser.isEmpty()) return "redirect:/messages";

        int otherId = ((Number) otherUser.get(0).get("id")).intValue();

        List<Map<String, Object>> chats = db.queryForList(
            "SELECT m.*, u.username as sender_name, u.photo as sender_photo " +
            "FROM messages m JOIN users u ON m.sender_id = u.id " +
            "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
            "ORDER BY created_at ASC", userId, otherId, otherId, userId);

        db.update("UPDATE messages SET is_read = true WHERE sender_id = ? AND receiver_id = ?",
            otherId, userId);

        model.addAttribute("chats", chats);
        model.addAttribute("otherUser", otherUser.get(0));
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("userId", userId);
        return "chat";
    }

    @PostMapping("/messages/{username}")
    public String sendMessage(@PathVariable String username,
                              @RequestParam String content,
                              jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");

        List<Map<String, Object>> otherUser = db.queryForList(
            "SELECT * FROM users WHERE username = ?", username);
        if (otherUser.isEmpty()) return "redirect:/messages";

        int otherId = ((Number) otherUser.get(0).get("id")).intValue();
        db.update("INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)",
            userId, otherId, content);

        return "redirect:/messages/" + username;
    }
}
