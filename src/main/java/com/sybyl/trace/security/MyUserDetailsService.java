package com.sybyl.trace.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sybyl.trace.user.AppUserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MyUserDetailsService implements UserDetailsService {

  private final AppUserRepository repo;

  public MyUserDetailsService(AppUserRepository repo) {
    this.repo = repo;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    if (username == null || username.isBlank()) {
      log.warn("Login attempt with blank username");
      throw new UsernameNotFoundException("No user");
    }

    log.debug("Loading user by username: {}", username);

    return repo.findByUsernameIgnoreCase(username.trim())
      .map(u -> {
        log.debug("User loaded: username={}, enabled={}", u.getUsername(), u.isEnabled());
        return new MyUserDetails(u);
      })
      .orElseThrow(() -> {
        log.warn("User not found for username={}", username);
        return new UsernameNotFoundException("No user " + username);
      });
  }
}
