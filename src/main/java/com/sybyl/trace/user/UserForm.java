package com.sybyl.trace.user;

import java.util.Set;

import com.sybyl.trace.location.Location;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {
  public Long id;                 // null on create
  public String username;

  @Pattern(
		  regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
		  message = "Enter a valid email address"
		)
  public String email;
  public String firstName;
  public String lastName;
  public boolean enabled = false; // create -> false; edit shows real value
  @jakarta.validation.constraints.NotEmpty(message = "Select at least one location")
  private Set<Location> locations;

  public Set<AppRole> roles;
  public Set<Long> verticalIds;
  
  public void setEmail(String email) {
	  this.email = (email == null) ? null : email.trim();
	}

}
