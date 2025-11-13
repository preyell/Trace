package com.sybyl.trace.user;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sybyl.trace.exception.EmailAlreadyInUseException;
import com.sybyl.trace.exception.UsernameAlreadyInUseException;
import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.VerticalRepository;

@Service
public class UserAdminService {
	private final AppUserRepository users;
	private final VerticalRepository verticals;
	private final ActivationService service;
	private final PasswordEncoder encoder;
	private final ApplicationEventPublisher events;

	public UserAdminService(AppUserRepository users, VerticalRepository verticals, ActivationService service,
			PasswordEncoder encoder, ApplicationEventPublisher events) {
		this.users = users;
		this.verticals = verticals;
		this.encoder = encoder;
		this.service = service;
		this.events = events;
	}

	@Transactional
	public void createUser(CreateUserRequest req) {
		if (users.existsByUsernameIgnoreCase(req.username()))
		    throw new UsernameAlreadyInUseException(); 
		if (users.existsByEmailIgnoreCase(req.email()))
		    throw new EmailAlreadyInUseException();

		AppUser u = new AppUser();
		u.setUsername(req.username().trim());
		u.setEmail(req.email().trim());
		u.setFirstName(req.firstName().trim());
		u.setLastName(req.lastName().trim());
		u.setEnabled(false); // wait until activation
		u.setPassword(encoder.encode(Tokens.urlSafe(24))); // placeholder hash
		Set<Location> locations = (req.locations() == null || req.locations().isEmpty())
			    ? java.util.EnumSet.noneOf(Location.class)
			    : java.util.EnumSet.copyOf(req.locations());
			u.setLocations(locations);

		Set<AppRole> roles = EnumSet.copyOf(req.roles());
		u.setRoles(roles);

		if (req.verticalIds() != null && !req.verticalIds().isEmpty()) {
			var vs = new HashSet<>(verticals.findAllById(req.verticalIds()));
			if (vs.size() != req.verticalIds().size())
				throw new IllegalArgumentException("One or more vertical IDs are invalid");
			u.setVerticals(vs);
		}

		users.save(u);
		var token = service.createTokenFor(u);
		events.publishEvent(
				new UserCreatedEvent(u.getId(), u.getEmail(), u.getUsername(), u.getFirstName(), token.getToken()));
	}

	@Transactional
	public void updateUser(Long id, UserForm form) {
	    AppUser u = users.findById(id).orElseThrow();

	    // Basic fields
	    u.setEmail(form.getEmail());
	    u.setFirstName(form.getFirstName());
	    u.setLastName(form.getLastName());
	    u.setEnabled(form.isEnabled());

	    // Locations (use form -> entity; allow empty = no rows in user_locations)
	    Set<Location> newLocs = (form.getLocations() == null || form.getLocations().isEmpty())
	        ? EnumSet.noneOf(Location.class)
	        : EnumSet.copyOf(form.getLocations());
	    u.setLocations(newLocs);

	    // Roles (null-safe)
	    Set<AppRole> newRoles = (form.getRoles() == null || form.getRoles().isEmpty())
	        ? EnumSet.noneOf(AppRole.class)
	        : EnumSet.copyOf(form.getRoles());
	    u.setRoles(newRoles);

	    // Verticals (null-safe)
	    Set<Long> ids = (form.getVerticalIds() == null) ? java.util.Set.of() : form.getVerticalIds();
	    var vs = new java.util.HashSet<>(verticals.findAllById(ids));
	    u.setVerticals(vs);

	    users.save(u);
	}


	public void deleteUser(Long id) {
		users.deleteById(id);
	}

	public void publishActivationEmail(AppUser u, String token) {
		events.publishEvent(new UserCreatedEvent(u.getId(), u.getEmail(), u.getUsername(), u.getFirstName(), token));
	}
	
	 public Page<AppUser> search(String q, int page, int size) {
	        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("username").ascending());
	        return (q == null || q.isBlank()) ? users.findAll(pageable) : users.search(q.trim(), pageable);
	    }

}
