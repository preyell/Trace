package com.sybyl.trace.user;

import java.util.EnumSet;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.exception.EmailAlreadyInUseException;
import com.sybyl.trace.exception.UsernameAlreadyInUseException;
import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.web.SearchForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserPageController {

	private final AppUserRepository users;
	private final VerticalRepository verticals;
	private final UserAdminService userService;
	private final ActivationService activationService;
	private final AppAuditService auditService;

	public AdminUserPageController(AppUserRepository users, VerticalRepository verticals, UserAdminService userService,
			ActivationService activationService, AppAuditService auditService) {
		this.users = users;
		this.verticals = verticals;
		this.userService = userService;
		this.activationService = activationService;
		this.auditService = auditService;
	}

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {

		log.info("Admin requested user list: page={}, size={}, q='{}'", page, size, q);

		Page<AppUser> result = userService.search(q, page, size);

		log.debug("User search result: totalElements={}, totalPages={}", result.getTotalElements(),
				result.getTotalPages());

		model.addAttribute("page", result); // Page<User>
		model.addAttribute("pageTitle", "Master Data · Users");
		model.addAttribute("contentJsp", "admin/users/userlist.jsp");
		model.addAttribute("items", users.findAll());
		return "layout";
	}

	@GetMapping(value = "/table", produces = "text/html")
	public String usersTable(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {

		log.debug("Loading users table fragment: page={}, size={}, q='{}'", page, size, q);

		var result = userService.search(q, page, size);
		model.addAttribute("page", result);
		return "admin/users/_table";
	}

	// CREATE FORM
	@GetMapping("/userForm")
	public String newForm(Model model) {
		log.debug("Rendering new user form");

		UserForm form = new UserForm();
		model.addAttribute("pageTitle", "Master Data · New User");
		model.addAttribute("contentJsp", "admin/users/userForm.jsp");
		model.addAttribute("form", form);
		model.addAttribute("locations", Location.values());
		model.addAttribute("roles", AppRole.values());
		model.addAttribute("verticals", verticals.findAll());
		model.addAttribute("mode", "create");
		return "layout";
	}

	// CREATE SUBMIT
	@PostMapping
	public String create(@ModelAttribute("form") @Valid UserForm form, BindingResult result,
			@RequestParam(value = "sendActivation", required = false) String sendActivation,
			@AuthenticationPrincipal MyUserDetails me, HttpServletRequest request, Model model) {

		log.info("Create user requested: username={}, email={}", form.getUsername(), form.getEmail());

		// basic UI validation
		if (form.getRoles() == null || form.getRoles().isEmpty()) {
			result.rejectValue("roles", "roles.required", "Select at least one role");
		}
		if (result.hasErrors()) {
			log.debug("Create user validation errors: {}", result.getAllErrors());
			return backToForm(model, form, "create");
		}

		try {
			var req = new CreateUserRequest(form.getUsername(), form.getEmail(), form.getFirstName(),
					form.getLastName(), form.getLocations(), form.getRoles(), form.getVerticalIds());

			userService.createUser(req); // may throw exceptions

			// audit log
			AppUser actor = (me != null) ? me.getUser() : null;
			String actorIp = request != null ? request.getRemoteAddr() : null;

			auditService.logEvent("USER", null, // we don't have the new ID here; repository/service can be enhanced
												// later
					null, "CREATE", "Created user with username=" + form.getUsername() + ", email=" + form.getEmail(),
					null, actor, actorIp);

			log.info("User created successfully: username={}", form.getUsername());
			return "redirect:/admin/users?created=true";

		} catch (UsernameAlreadyInUseException ex) {
			log.warn("Create user failed - username already in use: {}", form.getUsername());
			result.rejectValue("username", "username.in.use", "Username already in use");
			return backToForm(model, form, "create");

		} catch (EmailAlreadyInUseException ex1) {
			log.warn("Create user failed - email already in use: {}", form.getEmail());
			result.rejectValue("email", "email.in.use", "Email already in use");
			return backToForm(model, form, "create");
		}
	}

	private String backToForm(Model model, UserForm form, String mode) {
		log.debug("Returning to user form, mode={}, username={}", mode, form.getUsername());

		model.addAttribute("pageTitle", mode.equals("edit") ? "Edit User" : "New User");
		model.addAttribute("form", form);
		model.addAttribute("locations", Location.values());
		model.addAttribute("roles", AppRole.values());
		model.addAttribute("verticals", verticals.findAll());
		model.addAttribute("mode", mode);
		model.addAttribute("contentJsp", "admin/users/userForm.jsp");
		return "layout";
	}

	// EDIT FORM
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		log.info("Edit user form requested for id={}", id);

		AppUser u = users.findById(id).orElseThrow();
		UserForm form = new UserForm();
		form.id = u.getId();
		form.username = u.getUsername();
		form.email = u.getEmail();
		form.firstName = u.getFirstName();
		form.lastName = u.getLastName();
		form.enabled = u.isEnabled();

		// locations can be empty; use noneOf-or-copy
		form.setLocations((u.getLocations() == null || u.getLocations().isEmpty()) ? EnumSet.noneOf(Location.class)
				: EnumSet.copyOf(u.getLocations()));
		// roles can be empty
		form.setRoles((u.getRoles() == null || u.getRoles().isEmpty()) ? EnumSet.noneOf(AppRole.class)
				: EnumSet.copyOf(u.getRoles()));
		// vertical ids from entities
		form.setVerticalIds(u.getVerticals().stream().map(v -> v.getId()).collect(Collectors.toSet()));

		model.addAttribute("form", form);
		model.addAttribute("pageTitle", "Master Data · Edit User");
		model.addAttribute("contentJsp", "admin/users/userForm.jsp");
		model.addAttribute("locations", Location.values());
		model.addAttribute("roles", AppRole.values());
		model.addAttribute("verticals", verticals.findAll());
		model.addAttribute("mode", "edit");
		return "layout";
	}

	// EDIT SUBMIT
	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @ModelAttribute("form") @Valid UserForm form, BindingResult result,
			@AuthenticationPrincipal MyUserDetails me, HttpServletRequest request, Model model) {

		log.info("Update user requested: id={}, username={}", id, form.getUsername());

		if (form.getRoles() == null || form.getRoles().isEmpty()) {
			result.rejectValue("roles", "roles.required", "Select at least one role");
		}
		if (result.hasErrors()) {
			log.debug("Update user validation errors for id={}: {}", id, result.getAllErrors());
			return backToForm(model, form, "edit");
		}

		userService.updateUser(id, form);

		AppUser actor = (me != null) ? me.getUser() : null;
		String actorIp = request != null ? request.getRemoteAddr() : null;

		auditService.logEvent("USER", id, null, "UPDATE", "Updated user with username=" + form.getUsername(), null,
				actor, actorIp);

		log.info("User updated successfully: id={}, username={}", id, form.getUsername());
		return "redirect:/admin/users?updated=true";
	}

	// DELETE
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request,
			RedirectAttributes ra) {

		log.info("Delete user requested: id={}", id);

		AppUser target = users.findById(id).orElse(null);
		String targetUsername = (target != null) ? target.getUsername() : ("id=" + id);
		try {
			userService.deleteUser(id);
			ra.addFlashAttribute("message", "User deleted.");
			AppUser actor = (me != null) ? me.getUser() : null;
			String actorIp = request != null ? request.getRemoteAddr() : null;

			auditService.logEvent("USER", id, null, "DELETE", "Deleted user " + targetUsername, null, actor, actorIp);

			log.info("User deleted successfully: {}", targetUsername);
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}

		return "redirect:/admin/users?deleted=true";
	}

	// RESEND ACTIVATION
	@PostMapping("/{id}/resend-activation")
	public String resend(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request) {

		log.info("Resend activation requested for user id={}", id);

		AppUser u = users.findById(id).orElseThrow();
		if (!u.isEnabled()) {
			var token = activationService.createTokenFor(u);
			userService.publishActivationEmail(u, token.getToken()); // helper to publish event

			AppUser actor = (me != null) ? me.getUser() : null;
			String actorIp = request != null ? request.getRemoteAddr() : null;

			auditService.logEvent("USER", id, null, "RESEND_ACTIVATION",
					"Resent activation email for user " + u.getUsername(), null, actor, actorIp);

			log.info("Activation email resent for user id={}, username={}", id, u.getUsername());
		} else {
			log.debug("Activation not resent for user id={} because user is already enabled", id);
		}

		return "redirect:/admin/users?activationResent=true";
	}

	// Ensures the JSP Spring form always has a backing bean
	@ModelAttribute("search")
	public SearchForm search(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "size", required = false) Integer size) {
		int effectiveSize = (size == null ? 10 : size);
		log.debug("Search form model attribute created: q='{}', size={}", q, effectiveSize);
		return new SearchForm(q, effectiveSize);
	}
}
