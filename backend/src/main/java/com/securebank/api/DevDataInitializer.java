package com.securebank.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DevDataInitializer {
  @Bean CommandLineRunner seedAdmin(JdbcTemplate jdbc, PasswordEncoder encoder,
      @Value("${app.admin-email}") String email, @Value("${app.admin-password}") String password) {
    return args -> {
      Integer count = jdbc.queryForObject("SELECT count(*) FROM app_users WHERE email=?", Integer.class, email.toLowerCase());
      if (count != null && count == 0) {
        jdbc.update("INSERT INTO app_users(email,password_hash,full_name,role) VALUES(?,?,?,'ADMIN')",
            email.toLowerCase(), encoder.encode(password), "SecureBank Administrator");
      }
    };
  }
}
