package com.securebank.api;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BankUserService implements UserDetailsService {
  private final JdbcTemplate jdbc;
  public BankUserService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  @Override public BankUser loadUserByUsername(String email) {
    return jdbc.query("SELECT id,email,password_hash,full_name,role,status,email_verified FROM app_users WHERE email=?",
        (rs, row) -> new BankUser(rs.getObject("id", UUID.class), rs.getString("email"),
            rs.getString("password_hash"), rs.getString("full_name"), rs.getString("role"),
            rs.getString("status"), rs.getBoolean("email_verified")), email.toLowerCase()).stream().findFirst()
        .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }
}
