package com.sybyl.trace.masterdata;

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
import com.sybyl.trace.web.SearchForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerAdminController {

	private final CustomerService service;
	private final AppAuditService auditService;

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {

		log.info("Customer list requested: q='{}', page={}, size={}", q, page, size);

		var result = service.search(q, page, size);
		model.addAttribute("page", result); // org.springframework.data.domain.Page
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("size", size);
		model.addAttribute("pageTitle", "Master Data · Customers");
		model.addAttribute("contentJsp", "admin/customers/customerlist.jsp");
		model.addAttribute("customer", new Customer());
		return "layout";
	}

	@ModelAttribute("search")
	public SearchForm search(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "size", required = false) Integer size) {
		int effectiveSize = (size == null ? 10 : size);
		log.debug("Customer search model attribute: q='{}', size={}", q, effectiveSize);
		return new SearchForm(q, effectiveSize);
	}

	@GetMapping(value = "/table", produces = "text/html")
	public String table(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {

		log.debug("Customer table fragment requested: q='{}', page={}, size={}", q, page, size);
		var result = service.search(q, page, size);
		model.addAttribute("page", result);
		return "admin/customers/_table";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		log.debug("New customer form requested");
		model.addAttribute("customer", new Customer());
		model.addAttribute("pageTitle", "Master Data · Customers · Create");
		model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
		return "layout";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("customer") Customer customer, BindingResult errors,
			@AuthenticationPrincipal MyUserDetails me, HttpServletRequest request, RedirectAttributes ra, Model model) {

		log.info("Create customer requested: name={}", customer.getName());

		if (errors.hasErrors()) {
			log.debug("Create customer validation errors: {}", errors.getAllErrors());
			model.addAttribute("pageTitle", "Master Data · Customers · Create");
			model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
			return "layout";
		}

		try {
			Customer saved = service.create(customer);

			// audit
			var actor = (me != null) ? me.getUser() : null;

			auditService.logEvent("CUSTOMER", saved.getId(), null, "CREATE", "Created customer: " + saved.getName(),
					null, actor);

			ra.addFlashAttribute("message", "Customer created.");
			log.info("Customer created successfully: id={}, name={}", saved.getId(), saved.getName());
			return "redirect:/admin/customers?created";

		} catch (IllegalArgumentException e) {
			log.warn("Create customer failed (duplicate name?): {}", e.getMessage());
			errors.rejectValue("name", "name.exists", e.getMessage());
			model.addAttribute("pageTitle", "Master Data · Customers · Create");
			model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
			return "layout";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		log.info("Edit customer form requested: id={}", id);
		model.addAttribute("customer", service.get(id));
		model.addAttribute("pageTitle", "Master Data · Customers · Edit");
		model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
		return "layout";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("customer") Customer customer,
			BindingResult errors, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request,
			RedirectAttributes ra, Model model) {

		log.info("Update customer requested: id={}, name={}", id, customer.getName());

		if (errors.hasErrors()) {
			log.debug("Update customer validation errors: {}", errors.getAllErrors());
			model.addAttribute("pageTitle", "Master Data · Customers · Edit");
			model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
			return "layout";
		}

		try {
			Customer updated = service.update(id, customer);

			var actor = (me != null) ? me.getUser() : null;

			auditService.logEvent("CUSTOMER", updated.getId(), null, "UPDATE", "Updated customer: " + updated.getName(),
					null, actor);

			ra.addFlashAttribute("message", "Customer updated.");
			log.info("Customer updated successfully: id={}, name={}", updated.getId(), updated.getName());
			return "redirect:/admin/customers?updated";

		} catch (IllegalArgumentException e) {
			log.warn("Update customer failed (duplicate name?): {}", e.getMessage());
			errors.rejectValue("name", "name.exists", e.getMessage());
			model.addAttribute("pageTitle", "Master Data · Customers · Edit");
			model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
			return "layout";
		}
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request,
			RedirectAttributes ra) {

		Customer existing = null;
		try {
			existing = service.get(id);
		} catch (Exception ignore) {
		}

		try {
			service.delete(id);

			var actor = (me != null) ? me.getUser() : null;
			String customerName = (existing != null ? existing.getName() : "id=" + id);

			auditService.logEvent("CUSTOMER", id, null, "DELETE", "Deleted customer: " + customerName, null, actor
					);

			ra.addFlashAttribute("message", "Customer deleted.");
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}

		return "redirect:/admin/customers";
	}

}
