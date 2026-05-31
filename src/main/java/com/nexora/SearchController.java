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
                "SELECT * FROM search_users(?)", q);
            List<Map<String, Object>> trending = db.queryForList(
                "SELECT * FROM get_trending_posts()");
            model.addAttribute("users", users);
            model.addAttribute("trending", trending);
            model.addAttribute("query", q);
        }

        model.addAttribute("username", session.getAttribute("username"));
        return "search";
    }

    @GetMapping("/trending")
    public String trending(Model model,
                           jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("username") == null) return "redirect:/login";
        List<Map<String, Object>> trending = db.queryForList(
            "SELECT * FROM get_trending_posts()");
        model.addAttribute("trending", trending);
        model.addAttribute("username", session.getAttribute("username"));
        return "trending";
    }
}
