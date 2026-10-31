package com.securebank.api;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final JdbcTemplate jdbc;
  public AdminController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  @GetMapping("/summary")
  public Map<String, Object> summary() {
    return Map.of(
        "users", jdbc.queryForObject("SELECT count(*) FROM app_users WHERE role='USER'", Integer.class),
        "accounts", jdbc.queryForObject("SELECT count(*) FROM accounts", Integer.class),
        "transactions", jdbc.queryForObject("SELECT count(*) FROM transactions", Integer.class),
        "volume", jdbc.queryForObject("SELECT coalesce(sum(amount),0) FROM transactions", java.math.BigDecimal.class));
  }
  @GetMapping("/users")
  public List<Map<String, Object>> users() {
    return jdbc.queryForList("SELECT id,email,full_name,role,status,created_at FROM app_users ORDER BY created_at DESC LIMIT 100");
  }
  @GetMapping("/audit")
  public List<Map<String, Object>> audit() {
    return jdbc.queryForList("SELECT id,user_id,action,details,created_at FROM audit_events ORDER BY created_at DESC LIMIT 100");
  }
}
