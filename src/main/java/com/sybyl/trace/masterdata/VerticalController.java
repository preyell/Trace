package com.sybyl.trace.masterdata;

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
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.web.SearchForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/verticals")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class VerticalController {

	private final VerticalService service;
	private final AppAuditService auditService;

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {

		log.info("Vertical list requested: q='{}', page={}, size={}", q, page, size);

		var result = service.search(q, page, size);
		model.addAttribute("page", result);
		model.addAttribute("pageTitle", "Master Data · Verticals");
		model.addAttribute("contentJsp", "admin/verticals/verticallist.jsp");
		model.addAttribute("items", service.findAll());
		model.addAttribute("search", new SearchForm(q, size));
		return "layout";
	}

	@GetMapping(value = "/table", produces = "text/html")
	public String table(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {

		log.debug("Vertical table fragment requested: q='{}', page={}, size={}", q, page, size);

		var result = service.search(q, page, size);
		model.addAttribute("page", result);
		return "admin/verticals/_table";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		log.debug("New vertical form requested");

		model.addAttribute("pageTitle", "New Vertical");
		model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
		model.addAttribute("vertical", new Vertical());
		model.addAttribute("mode", "create");
		return "layout";
	}

	@PostMapping
	public String create(@ModelAttribute("vertical") @Valid Vertical vertical, BindingResult br, Model model,
			@AuthenticationPrincipal MyUserDetails me, HttpServletRequest request, RedirectAttributes ra) {

		log.info("Create vertical requested: name={}", vertical.getName());

		if (br.hasErrors()) {
			log.debug("Create vertical validation errors: {}", br.getAllErrors());
			model.addAttribute("pageTitle", "New Vertical");
			model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
			model.addAttribute("mode", "create");
			return "layout";
		}
		try {
			Vertical saved = service.create(vertical);

			AppUser actor = (me != null) ? me.getUser() : null;

			auditService.logEvent("VERTICAL", saved.getId(), null, "CREATE", "Created vertical: " + saved.getName(),
					null, actor);

			ra.addFlashAttribute("message", "Vertical created.");
			log.info("Vertical created successfully: id={}, name={}", saved.getId(), saved.getName());
			return "redirect:/admin/verticals";

		} catch (IllegalArgumentException ex) {
			log.warn("Create vertical failed (duplicate?): {}", ex.getMessage());
			br.rejectValue("name", "duplicate", ex.getMessage());
			model.addAttribute("pageTitle", "New Vertical");
			model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
			model.addAttribute("mode", "create");
			ra.addFlashAttribute("error", ex.getMessage());
			return "layout";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		log.info("Edit vertical form requested: id={}", id);

		model.addAttribute("pageTitle", "Edit Vertical");
		model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
		model.addAttribute("vertical", service.findById(id));
		model.addAttribute("mode", "edit");
		return "layout";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @ModelAttribute("vertical") @Valid Vertical form, BindingResult br,
			Model model, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request, RedirectAttributes ra) {

		log.info("Update vertical requested: id={}, name={}", id, form.getName());

		if (br.hasErrors()) {
			log.debug("Update vertical validation errors: {}", br.getAllErrors());
			model.addAttribute("pageTitle", "Edit Vertical");
			model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
			model.addAttribute("mode", "edit");
			return "layout";
		}
		try {
			Vertical updated = service.update(id, form);

			AppUser actor = (me != null) ? me.getUser() : null;

			auditService.logEvent("VERTICAL", updated.getId(), null, "UPDATE", "Updated vertical: " + updated.getName(),
					null, actor);

			ra.addFlashAttribute("message", "Vertical updated.");
			log.info("Vertical updated successfully: id={}, name={}", updated.getId(), updated.getName());
			return "redirect:/admin/verticals";

		} catch (IllegalArgumentException ex) {
		    log.warn("Update vertical failed (duplicate?): {}", ex.getMessage());

		    br.rejectValue("name", "duplicate", ex.getMessage());

		    model.addAttribute("pageTitle", "Edit Vertical");
		    model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
		    model.addAttribute("mode", "edit");

		    // IMPORTANT: keep the entered values on screen
		    model.addAttribute("vertical", form);

		    return "layout";
		}
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request,
			RedirectAttributes ra) {

		log.info("Delete vertical requested: id={}", id);

		Vertical existing = null;
		try {
			existing = service.findById(id);
		} catch (Exception ex) {
			log.warn("Delete vertical: vertical not found for id={}", id);
		}

		try {
			service.delete(id);

			AppUser actor = (me != null) ? me.getUser() : null;
			String verticalName = (existing != null ? existing.getName() : "id=" + id);

			auditService.logEvent("VERTICAL", id, null, "DELETE", "Deleted vertical: " + verticalName, null, actor);

			ra.addFlashAttribute("message", "Vertical deleted.");
			log.info("Vertical deleted: {}", verticalName);
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/verticals";
	}
}
