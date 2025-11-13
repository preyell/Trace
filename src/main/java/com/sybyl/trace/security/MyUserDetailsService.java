// com.sybyl.trace.security.MyUserDetailsService.java
package com.sybyl.trace.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sybyl.trace.user.AppUserRepository;

@Service
public class MyUserDetailsService implements UserDetailsService {
  private final AppUserRepository repo;
  public MyUserDetailsService(AppUserRepository repo) { this.repo = repo; }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return repo.findByUsernameIgnoreCase(username)
      .map(MyUserDetails::new)
      .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
  }
}
