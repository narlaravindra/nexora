package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class NotificationController {

    @Autowired
    JdbcTemplate db;

    @GetMapping("/notifications")
    public String notifications(Model model, jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        int userId = (int) session.getAttribute("user_id");

        List<Map<String, Object>> notifications = db.queryForList(
            "SELECT n.*, u.username as from_username, u.photo as from_photo " +
            "FROM notifications n JOIN users u ON n.from_user_id = u.id " +
            "WHERE n.user_id = ? ORDER BY n.created_at DESC LIMIT 50", userId);

        db.update("UPDATE notifications SET is_read = true WHERE user_id = ?", userId);

        model.addAttribute("notifications", notifications);
        model.addAttribute("username", session.getAttribute("username"));
        return "notifications";
    }

    @GetMapping("/notifications/count")
    @ResponseBody
    public int notificationCount(jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return 0;
        int userId = (int) session.getAttribute("user_id");
        return db.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false",
            Integer.class, userId);
    }
}
