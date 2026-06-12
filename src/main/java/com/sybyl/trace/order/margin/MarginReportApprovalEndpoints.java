package com.sybyl.trace.order.margin;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.web.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fixes "approve shows error but actually approved":
 * - The service call commits approval.
 * - Then controller was calling mr.getOrder().getSalesOrderId() outside a session → LazyInitializationException.
 *
 * This class fixes it by fetching MarginReport WITH Order using a fetch-join repository method:
 *   marginReportRepository.findByIdWithOrder(mrId)
 *
 * NOTE: You must add this method to MarginReportRepository:
 *
 *   @Query("""
 *     select mr from MarginReport mr
 *     join fetch mr.order
 *     where mr.id = :id
 *   """)
 *   Optional<MarginReport> findByIdWithOrder(@Param("id") Long id);
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders/{orderId}/margin-reports")
@PreAuthorize("isAuthenticated()")
public class MarginReportApprovalEndpoints {

    private final MarginReportService marginReportService;
    private final MarginReportRepository marginReportRepository;
    private final AppAuditService appAuditService;

    @PostMapping("/{mrId}/approve-finance")
    @PreAuthorize("hasAnyRole('FINANCE','ADMIN')")
    public String approveFinance(@PathVariable Long orderId,
                                 @PathVariable Long mrId,
                                 @RequestParam(required = false) String note,
                                 @RequestParam(required = true) String comments,
                                 @AuthenticationPrincipal MyUserDetails me,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {

        AppUser actor = (me != null) ? me.getUser() : null;

        try {
            marginReportService.approveFinance(orderId, mrId, actor, note, comments);

            // Fetch with order to avoid LazyInitializationException
            String soid = safeSalesOrderId(orderId, mrId);

            // Audit should never break the approval UX; keep it safe.
            try {
                appAuditService.logEvent(
                        "MARGIN_REPORT",
                        mrId,
                        soid,
                        "APPROVE_FINANCE",
                        "Finance approved margin report " + mrId + " for order " + soid,
                        note,
                        actor
                        
                );
            } catch (Exception auditEx) {
                log.error("Audit failed after finance approval: mrId={}, orderId={}", mrId, orderId, auditEx);
            }

            ra.addFlashAttribute("message", "Finance approved. Sent to CEO.");
            return "redirect:/orders/" + orderId + "?tab=margin";

        } catch (Exception ex) {
            log.error("Finance approve failed: mrId={}, orderId={}", mrId, orderId, ex);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/orders/" + orderId + "?tab=margin";
        }
    }

    @PostMapping("/{mrId}/approve-ceo")
    @PreAuthorize("hasAnyRole('CEO','ADMIN')")
    public String approveCeo(@PathVariable Long orderId,
                             @PathVariable Long mrId,
                             @RequestParam(required = false) String note,
                             @RequestParam(required = true) String comments,
                             @AuthenticationPrincipal MyUserDetails me,
                             HttpServletRequest request,
                             RedirectAttributes ra) {

        AppUser actor = (me != null) ? me.getUser() : null;

        try {
            marginReportService.approveCeo(orderId, mrId, actor, note, comments);

            String soid = safeSalesOrderId(orderId, mrId);

            try {
                appAuditService.logEvent(
                        "MARGIN_REPORT",
                        mrId,
                        soid,
                        "APPROVE_CEO",
                        "CEO approved margin report " + mrId + " for order " + soid,
                        null,
                        actor
                        
                );
            } catch (Exception auditEx) {
                log.error("Audit failed after CEO approval: mrId={}, orderId={}", mrId, orderId, auditEx);
            }

            ra.addFlashAttribute("message", "CEO approved.");
            return "redirect:/orders/" + orderId + "?tab=margin";

        } catch (Exception ex) {
            log.error("CEO approve failed: mrId={}, orderId={}", mrId, orderId, ex);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/orders/" + orderId + "?tab=margin";
        }
    }

    @PostMapping("/{mrId}/reject")
    @PreAuthorize("hasAnyRole('FINANCE','CEO','ADMIN')")
    public String reject(@PathVariable Long orderId,
                         @PathVariable Long mrId,
                         @RequestParam(required = true) String comments,
                         @AuthenticationPrincipal MyUserDetails me,
                         HttpServletRequest request,
                         RedirectAttributes ra) {

        AppUser actor = (me != null) ? me.getUser() : null;

        try {
        	String soid = safeSalesOrderId(orderId, mrId);
            marginReportService.reject(orderId, mrId, soid, actor, comments);


            ra.addFlashAttribute("message", "Margin report rejected.");
            return "redirect:/orders/" + orderId + "?tab=margin";

        } catch (Exception ex) {
            log.error("Reject failed: mrId={}, orderId={}", mrId, orderId, ex);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/orders/" + orderId + "?tab=margin";
        }
    }

    /**
     * Fetches MarginReport with Order to safely access salesOrderId.
     * Falls back to orderId if not available.
     */
    private String safeSalesOrderId(Long orderId, Long mrId) {
        try {
            MarginReport mr = marginReportRepository.findByIdWithOrder(mrId).orElse(null);
            if (mr != null && mr.getOrder() != null && mr.getOrder().getSalesOrderId() != null) {
                return mr.getOrder().getSalesOrderId();
            }
        } catch (Exception ex) {
            log.warn("Could not fetch salesOrderId safely: mrId={}, orderId={}", mrId, orderId, ex);
        }
        return String.valueOf(orderId);
    }
}
