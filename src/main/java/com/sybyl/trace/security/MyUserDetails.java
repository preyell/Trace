// com.sybyl.trace.security.MyUserDetails.java
package com.sybyl.trace.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sybyl.trace.location.Location;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyUserDetails implements UserDetails {
	private final AppUser user;

	public MyUserDetails(AppUser user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return user.getRoles().stream().map(AppRole::asAuthority) // 
				.map(SimpleGrantedAuthority::new).toList();
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return user.isEnabled();
	}

	// Extra getters for JSP or controllers
	public String getFirstName() {
		return user.getFirstName();
	}

	public String getLastName() {
		return user.getLastName();
	}

	public Set<Location> getLocations() {
		return user.getLocations();
	}

	public String getEmail() {
		return user.getEmail();
	}

	public Set getVerticals() {
		return user.getVerticals();
	}
}
