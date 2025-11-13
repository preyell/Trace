package com.sybyl.trace.controller;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sybyl.trace.exception.EmailAlreadyInUseException;
import com.sybyl.trace.exception.UsernameAlreadyInUseException;
import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.user.ActivationService;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;
import com.sybyl.trace.user.CreateUserRequest;
import com.sybyl.trace.user.UserAdminService;
import com.sybyl.trace.user.UserForm;
import com.sybyl.trace.web.SearchForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserPageController {

	private final AppUserRepository users;
	private final VerticalRepository verticals;
	private final UserAdminService userService; // your service with create/update/delete
	private final ActivationService activationService; // for resend

	public AdminUserPageController(AppUserRepository users, VerticalRepository verticals, UserAdminService userService,
			ActivationService activationService) {
		this.users = users;
		this.verticals = verticals;
		this.userService = userService;
		this.activationService = activationService;
	}

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {
		Page<AppUser> result = userService.search(q, page, size);
		model.addAttribute("page", result); // Page<User>
		model.addAttribute("pageTitle", "Master Data · Users");
		model.addAttribute("contentJsp", "admin/users/userlist.jsp");
		model.addAttribute("items", users.findAll());
		return "layout";
	}
	
	@GetMapping(value="/table", produces="text/html")
	public String usersTable(@RequestParam(value="q", required=false) String q,
	                         @RequestParam(value="page", defaultValue="0") int page,
	                         @RequestParam(value="size", defaultValue="10") int size,
	                         Model model) {
	  var result = userService.search(q, page, size);
	  model.addAttribute("page", result);
	  return "admin/users/_table";
	}

	// CREATE FORM
	@GetMapping("/userForm")
	public String newForm(Model model) {
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
			@RequestParam(value = "sendActivation", required = false) String sendActivation, Model model) {
		// basic UI validation
		if (form.getRoles() == null || form.getRoles().isEmpty()) {
			result.rejectValue("roles", "roles.required", "Select at least one role");
		}
		if (result.hasErrors()) {
			return backToForm(model, form, "create");
		}

		try {
			var req = new CreateUserRequest(form.getUsername(), form.getEmail(), form.getFirstName(),
					form.getLastName(), form.getLocations(), form.getRoles(), form.getVerticalIds());
			userService.createUser(req); // may throw IllegalArgumentException
			return "redirect:/admin/users?created=true";
		} catch (UsernameAlreadyInUseException ex) {
			result.rejectValue("username", "username.in.use", "Username already in use");
			return backToForm(model, form, "create"); // <-- stay on page and show the error
		} catch (EmailAlreadyInUseException ex1) {
			result.rejectValue("email", "email.in.use", "Email already in use");
			return backToForm(model, form, "create");
		}
		
	}

	private String backToForm(org.springframework.ui.Model model, UserForm form, String mode) {
		model.addAttribute("pageTitle", mode.equals("edit") ? "Edit User" : "New User");
		model.addAttribute("form", form);
		model.addAttribute("locations", Location.values());
		model.addAttribute("roles", com.sybyl.trace.user.AppRole.values());
		model.addAttribute("verticals", verticals.findAll());
		model.addAttribute("mode", mode);
		// expose BindingResult for JSPs that read ${errors.fieldErrors}
		var binding = (org.springframework.validation.BindingResult) model.asMap()
				.get(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "form");
		model.addAttribute("contentJsp", "admin/users/userForm.jsp");
		return "layout";
	}

	// EDIT FORM
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		AppUser u = users.findById(id).orElseThrow();
		UserForm form = new UserForm();
		form.id = u.getId();
		form.username = u.getUsername();
		form.email = u.getEmail();
		form.firstName = u.getFirstName();
		form.lastName = u.getLastName();
		form.enabled = u.isEnabled();
		// locations can be empty; use noneOf-or-copy
		form.setLocations(
            (u.getLocations() == null || u.getLocations().isEmpty())
                ? EnumSet.noneOf(Location.class)
                : EnumSet.copyOf(u.getLocations())
        );
        // roles can be empty
		form.setRoles(
            (u.getRoles() == null || u.getRoles().isEmpty())
                ? EnumSet.noneOf(AppRole.class)
                : EnumSet.copyOf(u.getRoles())
        );
        // vertical ids from entities
		form.setVerticalIds(
            u.getVerticals().stream()
                .map(v -> v.getId())
                .collect(Collectors.toSet())
        );

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
			Model model) {
		if (form.getRoles() == null || form.getRoles().isEmpty()) {
			result.rejectValue("roles", "roles.required", "Select at least one role");
		}
		if (result.hasErrors())
			return backToForm(model, form, "edit");

		userService.updateUser(id, form);
		return "redirect:/admin/users?updated=true";
	}

	// DELETE
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id) {
		userService.deleteUser(id);
		return "redirect:/admin/users?deleted=true";
	}

	// RESEND ACTIVATION
	@PostMapping("/{id}/resend-activation")
	public String resend(@PathVariable Long id) {
		AppUser u = users.findById(id).orElseThrow();
		if (!u.isEnabled()) {
			var token = activationService.createTokenFor(u);
			userService.publishActivationEmail(u, token.getToken()); // helper to publish event
		}
		return "redirect:/admin/users?activationResent=true";
	}

	// Ensures the JSP Spring form always has a backing bean
	@ModelAttribute("search")
	public SearchForm search(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "size", required = false) Integer size) {
		return new SearchForm(q, size == null ? 10 : size);
	}
}
