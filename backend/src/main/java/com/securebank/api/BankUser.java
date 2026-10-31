package com.securebank.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record BankUser(UUID id, String email, String passwordHash, String fullName, String role,
                       String status, boolean emailVerified) implements UserDetails {
  @Override public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }
  @Override public String getPassword() { return passwordHash; }
  @Override public String getUsername() { return email; }
  @Override public boolean isAccountNonLocked() { return "ACTIVE".equals(status); }
  @Override public boolean isEnabled() { return "ACTIVE".equals(status) && emailVerified; }
  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
}
