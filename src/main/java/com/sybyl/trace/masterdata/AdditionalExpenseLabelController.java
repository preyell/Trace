package com.sybyl.trace.masterdata;

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
	public String list(@RequestParam(defaultValue = "true") boolean showInactive, Model m) {

		log.info("AdditionalExpense labels list requested: showInactive={}", showInactive);

		m.addAttribute("labels", showInactive ? repo.findAllForAdmin() : repo.findAllActive());
		m.addAttribute("pageTitle", "Additional Expense Labels");
		m.addAttribute("contentJsp", "admin/expenses.jsp");
		return "layout";
	}

	@PostMapping("/create")
	public String create(@RequestParam String name, @RequestParam(required = false) String description,
			@AuthenticationPrincipal MyUserDetails me, HttpServletRequest request, RedirectAttributes ra) {

		log.info("Create AdditionalExpenseLabel requested: name={}", name);

		var label = svc.create(name, description, false);

		AppUser actor = me != null ? me.getUser() : null;
		String actorIp = request != null ? request.getRemoteAddr() : null;

		auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", label.getId(), null, "CREATE",
				"Created additional expense label: " + label.getName(), null, actor, actorIp);

		ra.addFlashAttribute("message", "Label created.");
		log.info("AdditionalExpenseLabel created successfully: id={}, name={}", label.getId(), label.getName());

		return "redirect:/admin/expenses";
	}

	@PostMapping("/{id}/deactivate")
	public String deactivate(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me,
			HttpServletRequest request, RedirectAttributes ra) {

		log.info("Deactivate AdditionalExpenseLabel requested: id={}", id);

		var label = repo.findById(id).orElse(null);
		String labelName = (label != null) ? label.getName() : ("id=" + id);

		svc.deactivate(id);

		AppUser actor = me != null ? me.getUser() : null;
		String actorIp = request != null ? request.getRemoteAddr() : null;

		auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", id, null, "DEACTIVATE",
				"Deactivated additional expense label: " + labelName, null, actor, actorIp);

		ra.addFlashAttribute("message", "Label deactivated.");
		log.info("AdditionalExpenseLabel deactivated: {}", labelName);

		return "redirect:/admin/expenses?showInactive=true";
	}

	@PostMapping("/{id}/reactivate")
	public String reactivate(@PathVariable Long id, @AuthenticationPrincipal MyUserDetails me,
			HttpServletRequest request, RedirectAttributes ra) {

		log.info("Reactivate AdditionalExpenseLabel requested: id={}", id);

		var label = repo.findById(id).orElse(null);
		String labelName = (label != null) ? label.getName() : ("id=" + id);

		svc.reactivate(id);

		AppUser actor = me != null ? me.getUser() : null;
		String actorIp = request != null ? request.getRemoteAddr() : null;

		auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", id, null, "REACTIVATE",
				"Reactivated additional expense label: " + labelName, null, actor, actorIp);

		ra.addFlashAttribute("message", "Label reactivated.");
		log.info("AdditionalExpenseLabel reactivated: {}", labelName);

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
			String actorIp = request != null ? request.getRemoteAddr() : null;

			auditService.logEvent("ADDITIONAL_EXPENSE_LABEL", id, null, "DELETE",
					"Deleted additional expense label: " + labelName, null, actor, actorIp);

			ra.addFlashAttribute("message", "Label deleted.");
			log.info("AdditionalExpenseLabel deleted: {}", labelName);
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		}

		return "redirect:/admin/expenses";
	}
}
