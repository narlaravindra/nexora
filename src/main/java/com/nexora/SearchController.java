package com.nexora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

    @Autowired
    JdbcTemplate db;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         Model model,
                         jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";

        if (q != null && !q.trim().isEmpty()) {
            List<Map<String, Object>> users = db.queryForList(
                "SELECT username, " +
                "(SELECT COUNT(*) FROM posts WHERE user_id = users.id) as post_count, " +
                "(SELECT COUNT(*) FROM follows WHERE following_id = users.id) as followers " +
                "FROM users WHERE username ILIKE ? AND is_verified = true",
                "%" + q + "%");
            model.addAttribute("users", users);
            model.addAttribute("query", q);
        }

        model.addAttribute("username", session.getAttribute("username"));
        return "search";
    }
}
