// com.sybyl.trace.user.AppUser.java
package com.sybyl.trace.user;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Vertical;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "app_user")
public class AppUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;
	@Column(nullable = false)
	private String password;
	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "first_name", nullable = false)
	private String firstName;
	@Column(name = "last_name", nullable = false)
	private String lastName;
	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email address")
	@Size(max = 255)
	@Column(nullable = false, unique = true)
	private String email;

	@ElementCollection(fetch = FetchType.EAGER, targetClass = Location.class)
	@CollectionTable(name = "user_locations", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "location", nullable = false)
	private Set<Location> locations = EnumSet.noneOf(Location.class);;
	
	/** Store roles as strings in a separate table via ElementCollection */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "role_name", nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	private Set<AppRole> roles = EnumSet.noneOf(AppRole.class);

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_vertical", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "vertical_id"))
	private Set<Vertical> verticals = new HashSet<>();
	
	public boolean hasRole(AppRole role) {
	    return roles != null && roles.contains(role);
	}

	public boolean hasRole(String roleName) {
	    if (roles == null) return false;
	    try {
	        AppRole role = AppRole.valueOf(roleName);
	        return roles.contains(role);
	    } catch (IllegalArgumentException e) {
	        return false;
	    }
	}

	public boolean isAdmin() { return hasRole(AppRole.ADMIN); }
	public void setEmail(String email) {
	    this.email = (email == null) ? null : email.trim();
	}

}
