// com.sybyl.trace.order.AdditionalExpenseController.java
package com.sybyl.trace.order;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.security.MyUserDetails; // your existing class
import com.sybyl.trace.user.AppRole;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders/{orderId}/expenses")
public class AdditionalExpenseController {

	private final AdditionalExpenseService service;

	@PostMapping
	public String create(@PathVariable Long orderId, @AuthenticationPrincipal MyUserDetails me,
			@RequestParam Long labelId, @RequestParam BigDecimal amount, @RequestParam CurrencyCode currency,
			@RequestParam BigDecimal conversionRate, @RequestParam Long verticalId, @RequestParam String comments,
			@RequestParam(required = false) MultipartFile file, RedirectAttributes ra) throws IOException {

		service.create(orderId, me.getUser(), labelId, amount, currency, conversionRate, verticalId, comments, file);
		ra.addFlashAttribute("message", "Additional expense added.");
		return "redirect:/orders/" + orderId + "?tab=margin#additional-expenses";
	}

	@PostMapping("/{expenseId}/approve/ceo")
	public String ceoApprove(@PathVariable Long orderId, @PathVariable Long expenseId,
			@AuthenticationPrincipal MyUserDetails me, @RequestParam(required = false) String note,
			RedirectAttributes ra) {
		if (!(me.getUser().isAdmin() || me.getUser().hasRole(AppRole.CEO)))
			throw new AccessDeniedException("CEO/Admin only");
		service.ceoApprove(orderId, expenseId, me.getUser(), note);
		ra.addFlashAttribute("message", "Expense CEO-approved.");
		return "redirect:/orders/" + orderId + "?tab=margin#additional-expenses";
	}

	@PostMapping("/{expenseId}/approve/cfo")
	public String cfoApprove(@PathVariable Long orderId, @PathVariable Long expenseId,
			@AuthenticationPrincipal MyUserDetails me, @RequestParam(required = false) String note,
			RedirectAttributes ra) {
		if (!(me.getUser().isAdmin() || me.getUser().hasRole(AppRole.CFO)))
			throw new AccessDeniedException("CFO/Admin only");
		service.cfoApprove(orderId, expenseId, me.getUser(), note);
		ra.addFlashAttribute("message", "Expense CFO-approved.");
		return "redirect:/orders/" + orderId + "?tab=margin#additional-expenses";
	}

	@PostMapping("/{expenseId}/reject")
	public String reject(@PathVariable Long orderId, @PathVariable Long expenseId,
			@AuthenticationPrincipal MyUserDetails me, @RequestParam String reason, RedirectAttributes ra) {
		if (!(me.getUser().isAdmin() || me.getUser().hasRole(AppRole.CEO) || me.getUser().hasRole(AppRole.CFO)))
			throw new AccessDeniedException("CEO/CFO/Admin only");
		service.reject(orderId, expenseId, me.getUser(), reason);
		ra.addFlashAttribute("message", "Expense rejected.");
		return "redirect:/orders/" + orderId + "?tab=margin#additional-expenses";
	}

	// open disbursement modal table (AJAX loads partial)
	@GetMapping("/{expId}/disbursements")
	public String listDisbursements(@PathVariable Long orderId, @PathVariable Long expId, Model m) {
	  var list = service.listDisbursements(expId);
	  m.addAttribute("disbursements", list);
	  return "margin_report/expense_disbursements"; // fragment
	}

	@PostMapping("/{expId}/disburse")
	public String disburse(@AuthenticationPrincipal MyUserDetails me,
	                       @PathVariable Long orderId, @PathVariable Long expId,
	                       @RequestParam BigDecimal amount,
	                       @RequestParam CurrencyCode currency,
	                       @RequestParam String comments,
	                       RedirectAttributes ra) {
	  service.disburse(orderId, expId, amount, currency, comments, me.getUser());
	  ra.addFlashAttribute("message", "Disbursed.");
	  return "redirect:/orders/" + orderId + "?tab=margin";
	}

	@PostMapping("/{expId}/disbursements/{id}/delete")
	public String deleteDisb(@AuthenticationPrincipal MyUserDetails me,
	                         @PathVariable Long orderId, @PathVariable Long expId, @PathVariable Long id,
	                         RedirectAttributes ra) {
	  service.deleteDisbursement(orderId, expId, id, me.getUser());
	  ra.addFlashAttribute("message", "Disbursement deleted.");
	  return "redirect:/orders/" + orderId + "?tab=margin";
	}


	// (Optional) audit modal content
	@GetMapping("/{expenseId}/audits")
	public String audits(@PathVariable Long expenseId, Model m) {
		m.addAttribute("audits", service.audits(expenseId));
		return "margin_report/expense_audits";
	}

//OrderController (snippets)

	@PostMapping("/{expId}/update")
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_CFO','ROLE_CEO')")
	public String updateExpense(@PathVariable Long orderId, @PathVariable Long expId, @RequestParam Long labelId,
			@RequestParam BigDecimal amount, @RequestParam CurrencyCode currency,
			@RequestParam BigDecimal conversionRate, @RequestParam Long verticalId, @RequestParam String comments,
			@AuthenticationPrincipal MyUserDetails me, RedirectAttributes ra) {
		service.update(orderId, expId, me.getUser(), labelId, amount, currency, conversionRate,
				verticalId, comments);
		ra.addFlashAttribute("message", "Additional expense updated.");
		return "redirect:/orders/" + orderId + "?tab=margin";
	}

	@PostMapping("/{expId}/delete")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public String deleteExpense(@PathVariable Long orderId, @PathVariable Long expId,
			@RequestParam(required = false, defaultValue = "false") boolean deleteFile,
			@AuthenticationPrincipal MyUserDetails me, RedirectAttributes ra) throws IOException {
		service.delete(orderId, expId, me.getUser(), deleteFile);
		ra.addFlashAttribute("message", "Additional expense deleted.");
		return "redirect:/orders/" + orderId + "?tab=margin";
	}

}
