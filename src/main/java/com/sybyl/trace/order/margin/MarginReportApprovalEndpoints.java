// com.sybyl.trace.order.OrderController (or a dedicated MarginReportController)
package com.sybyl.trace.order;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUser;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MarginReportApprovalEndpoints {

  private final MarginReportService marginReportService;

  private AppUser userOf(MyUserDetails me) { return (me != null ? me.getUser() : null); }

  @PostMapping("/orders/{orderId}/margin-reports/{mrId}/submit")
  public String submitForApproval(@PathVariable Long orderId,
                                  @PathVariable Long mrId,
                                  @AuthenticationPrincipal MyUserDetails me,
                                  RedirectAttributes ra) {
    marginReportService.submitForApproval(orderId, mrId, userOf(me));
    ra.addFlashAttribute("message", "Margin report submitted for approval.");
    return "redirect:/orders/" + orderId + "?tab=margin";
  }

  @PostMapping("/orders/{orderId}/margin-reports/{mrId}/approve-finance")
  public String approveFinance(@PathVariable Long orderId,
                               @PathVariable Long mrId,
                               @AuthenticationPrincipal MyUserDetails me,
                               @RequestParam(required = false) String note,
                               RedirectAttributes ra) {
    marginReportService.approveFinance(orderId, mrId, userOf(me), note);
    ra.addFlashAttribute("message", "Finance approved. Sent to CEO.");
    return "redirect:/orders/" + orderId + "?tab=margin";
  }

  @PostMapping("/orders/{orderId}/margin-reports/{mrId}/approve-ceo")
  public String approveCeo(@PathVariable Long orderId,
                           @PathVariable Long mrId,
                           @AuthenticationPrincipal MyUserDetails me,
                           @RequestParam(required = false) String note,
                           RedirectAttributes ra) {
    marginReportService.approveCeo(orderId, mrId, userOf(me), note);
    ra.addFlashAttribute("message", "CEO approved. Margin report is fully approved.");
    return "redirect:/orders/" + orderId + "?tab=margin";
  }

  @PostMapping("/orders/{orderId}/margin-reports/{mrId}/reject")
  public String reject(@PathVariable Long orderId,
                       @PathVariable Long mrId,
                       @AuthenticationPrincipal MyUserDetails me,
                       @RequestParam("reason") String reason,
                       RedirectAttributes ra) {
    marginReportService.reject(orderId, mrId, userOf(me), reason);
    ra.addFlashAttribute("message", "Margin report rejected: " + reason);
    return "redirect:/orders/" + orderId + "?tab=margin";
  }
}
