package com.sybyl.trace.masterdata;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/expenses")
@Slf4j
public class AdditionalExpenseLabelController {

	private final AdditionalExpenseLabelService svc;
	private final AdditionalExpenseLabelRepository repo;
	private final AppAuditService auditService;
	

	@GetMapping
	public String list(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "0") int page, Model m) {
		m.addAttribute("page", svc.search(q, PageRequest.of(page, size)));
		m.addAttribute("pageTitle", "Additional Expense Labels");
		m.addAttribute("contentJsp", "admin/expenses/list.jsp");
		return "layout";
	}

	@GetMapping("/table")
	public String table(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "0") int page, Model m) {
		m.addAttribute("page", svc.search(q, PageRequest.of(page, size)));
		return "admin/expenses/_table"; 
	}

	@GetMapping("/new")
	public String newForm(Model m) {
		m.addAttribute("label", new AdditionalExpenseLabel()); // empty obj for the form
		m.addAttribute("mode", "create");
		m.addAttribute("pageTitle", "New Additional Expense Label");
		m.addAttribute("contentJsp", "admin/expenses/form.jsp");
		return "layout";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model m) {
		m.addAttribute("label", repo.findById(id).orElseThrow());
		m.addAttribute("mode", "edit");
		m.addAttribute("pageTitle", "Edit Additional Expense Label");
		m.addAttribute("contentJsp", "admin/expenses/form.jsp");
		return "layout";
	}

	@PostMapping
	public String saveCreate(@RequestParam String name, @RequestParam(required = false) String description,
			@AuthenticationPrincipal MyUserDetails me, RedirectAttributes ra) {
		try {
			var label = svc.create(name, description, false);
			auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", label.getId(), null, "CREATE",
					"Created label: " + label.getName(), null, me != null ? me.getUser() : null);
			ra.addFlashAttribute("message", "Label created successfully.");
		} catch (Exception ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/expenses";
	}

	@PostMapping("/{id}")
	public String saveEdit(@PathVariable Long id, @RequestParam String name,
			@RequestParam(required = false) String description,
			@RequestParam(defaultValue = "false") boolean active,
			@AuthenticationPrincipal MyUserDetails me, RedirectAttributes ra) {
		try {
			var label = svc.update(id, name, description, active);
			auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", label.getId(), null, "EDIT",
					"Edited label: " + label.getName(), null, me != null ? me.getUser() : null);
			ra.addFlashAttribute("message", "Label updated successfully.");
		} catch (Exception ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/expenses";
	}


	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me, HttpServletRequest request,
			RedirectAttributes ra) {

		log.info("Delete AdditionalExpenseLabel requested: id={}", id);

		var label = repo.findById(id).orElse(null);
		String labelName = (label != null) ? label.getName() : ("id=" + id);

		try {
			svc.delete(id);
			AppUser actor = me != null ? me.getUser() : null;

			auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", id, null, "DELETE",
					"Deleted additional expense label: " + labelName, null, actor);

			ra.addFlashAttribute("message", "Label deleted.");
			log.info("AdditionalExpenseLabel deleted: {}", labelName);
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}

		return "redirect:/admin/expenses";
	}
}
