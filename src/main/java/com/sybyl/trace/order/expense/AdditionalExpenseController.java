package com.sybyl.trace.order.expense;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.order.Order;
import com.sybyl.trace.order.margin.MarginReportService;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders/{orderId}/expenses")
public class AdditionalExpenseController {

    private final AdditionalExpenseService service;
    private final MarginReportService marginReportService;
    @Value("${app.upload.additional-expenses.path}")
    private String uploadBasePath;

    /* =========================================================
       CREATE
       ========================================================= */
    @PostMapping
    public String create(@PathVariable Long orderId,
                         @AuthenticationPrincipal MyUserDetails me,
                         @RequestParam Long labelId,
                         @RequestParam BigDecimal amount,
                         @RequestParam CurrencyCode currency,
                         @RequestParam(required = false) BigDecimal conversionRate,
                         @RequestParam Long verticalId,
                         @RequestParam String comments,
                         @RequestParam(required = false) MultipartFile file,
                         HttpServletRequest request,
                         RedirectAttributes ra) throws IOException {

        try {
            service.create(orderId, me.getUser(), labelId, amount, currency,
                    conversionRate, verticalId, comments, file);
            ra.addFlashAttribute("message", "Additional expense added.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/orders/" + orderId + "?tab=margin#additional-expenses";
    }

    
    /* =========================================================
       DELETE
       ========================================================= */
    @PostMapping("/{expId}/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String delete(@PathVariable Long orderId,
                         @PathVariable Long expId,
                         @RequestParam(defaultValue = "false") boolean deleteFile,
                         @AuthenticationPrincipal MyUserDetails me,
                         HttpServletRequest request,
                         RedirectAttributes ra) throws IOException {

        service.delete(orderId, expId, me.getUser(), deleteFile, request);

        ra.addFlashAttribute("message", "Additional expense deleted.");
        return "redirect:/orders/" + orderId + "?tab=margin";
    }

    /* =========================================================
       APPROVALS
       ========================================================= */
    @PostMapping("/{expenseId}/approve/ceo")
    public String approveCeo(@PathVariable Long orderId,
                             @PathVariable Long expenseId,
                             @RequestParam(required = false) String note,
                             @RequestParam(required = true) String comments,
                             @AuthenticationPrincipal MyUserDetails me,
                             HttpServletRequest request,
                             RedirectAttributes ra) {

        if (!(me.getUser().isAdmin() || me.getUser().hasRole(AppRole.CEO))) {
            throw new AccessDeniedException("CEO/Admin only");
        }

        service.ceoApprove(orderId, expenseId, me.getUser(), note, comments);

        ra.addFlashAttribute("message", "Expense CEO-approved.");
        return "redirect:/orders/" + orderId + "?tab=margin";
    }

    @PostMapping("/{expenseId}/approve/cfo")
    public String approveCfo(@PathVariable Long orderId,
                             @PathVariable Long expenseId,
                             @RequestParam(required = false) String note,
                             @RequestParam(required = true) String comments,
                             @AuthenticationPrincipal MyUserDetails me,
                             HttpServletRequest request,
                             RedirectAttributes ra) {

        if (!(me.getUser().isAdmin() || me.getUser().hasRole(AppRole.CFO))) {
            throw new AccessDeniedException("CFO/Admin only");
        }

        service.cfoApprove(orderId, expenseId, me.getUser(), note, comments);

        ra.addFlashAttribute("message", "Expense CFO-approved.");
        return "redirect:/orders/" + orderId + "?tab=margin";
    }

    @PostMapping("/{expenseId}/reject")
    public String reject(@PathVariable Long orderId,
                         @PathVariable Long expenseId,
                         @RequestParam(required = false) String note,
                         @RequestParam(required = true) String comments,
                         @AuthenticationPrincipal MyUserDetails me,
                         HttpServletRequest request,
                         RedirectAttributes ra) {

        if (!(me.getUser().isAdmin()
                || me.getUser().hasRole(AppRole.CEO)
                || me.getUser().hasRole(AppRole.CFO))) {
            throw new AccessDeniedException("CEO/CFO/Admin only");
        }

        service.reject(orderId, expenseId, me.getUser(), comments);

        ra.addFlashAttribute("message", "Expense rejected.");
        return "redirect:/orders/" + orderId + "?tab=margin";
    }

    /* =========================================================
       CONSUME (DISBURSE)
       ========================================================= */
    @PostMapping("/{expenseId}/consume")
    public String consume(@PathVariable Long orderId,
                          @PathVariable Long expenseId,
                          @RequestParam BigDecimal amount,
                          @RequestParam CurrencyCode currency,
                          @RequestParam(required = false) BigDecimal conversionRate,
                          @RequestParam(required = true) String comments,
                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate consumedOn,
                          @AuthenticationPrincipal MyUserDetails me,
                          HttpServletRequest request,
                          RedirectAttributes ra) {


        try {
            service.disburse(orderId, expenseId, amount, currency, conversionRate, me.getUser(), comments, consumedOn);
            ra.addFlashAttribute("message", "Consumption captured successfully.");

        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("expenseError", ex.getMessage());
            ra.addFlashAttribute("openConsumeExpenseId", expenseId);

            ra.addFlashAttribute("consumeAmount", amount);
            ra.addFlashAttribute("consumeCurrency", currency);
            ra.addFlashAttribute("consumeConversionRate", conversionRate);
            ra.addFlashAttribute("consumeNote", comments);
        }

        return "redirect:/orders/" + orderId + "?tab=margin";
    }
    /* =========================================================
       DISBURSEMENTS (AJAX)
       ========================================================= */
    @GetMapping("/{expId}/disbursements")
    public String listDisbursements(@PathVariable Long expId, Model model) {
        model.addAttribute("disbursements", service.listDisbursements(expId));
        return "margin_report/expense_disbursements";
    }

    @PostMapping("/{expId}/disbursements/{id}/delete")
    public String deleteDisbursement(@PathVariable Long orderId,
                                     @PathVariable Long expId,
                                     @PathVariable Long id,
                                     @AuthenticationPrincipal MyUserDetails me,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {

        service.deleteDisbursement(orderId, expId, id, me.getUser());

        ra.addFlashAttribute("message", "Consumption deleted.");
        return "redirect:/orders/" + orderId + "?tab=margin";
    }

    /* =========================================================
       AUDIT (AJAX)
       ========================================================= */
    @GetMapping("/{expenseId}/audits")
    public String audits(@PathVariable Long expenseId, Model model) {
    	AdditionalExpense ex = service.getById(expenseId);
    	model.addAttribute("ex", ex);
        model.addAttribute("audits", service.audits(expenseId));
        return "margin_report/expense_audits";
    }

    /* =========================================================
       EDIT MODAL – FILTERED VERTICALS (AJAX)
       ========================================================= */
    @GetMapping("/{expId}/verticals")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public List<IdNameDto> expenseVerticalsForEdit(@PathVariable Long orderId,
                                                   @PathVariable Long expId) {

        AdditionalExpense ex = service.getById(expId);
        if (!ex.getOrder().getId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // margin-uploaded verticals
        Set<Long> allowedIds = marginReportService.listForOrder(orderId).stream()
                .filter(mr -> mr.getVertical() != null)
                .map(mr -> mr.getVertical().getId())
                .collect(Collectors.toSet());

        // include current expense vertical
        if (ex.getVertical() != null) {
            allowedIds.add(ex.getVertical().getId());
        }

        Order order = ex.getOrder();

        return order.getVerticals().stream()
                .filter(v -> allowedIds.contains(v.getId()))
                .map(v -> new IdNameDto(v.getId(), v.getName()))
                .toList();
    }
    
    @GetMapping("/{id}/download")
    public void downloadAdditionalExpenseFile(@PathVariable Long id,
                                              HttpServletResponse response) throws IOException {

        log.info("Download additional expense file requested: expenseId={}", id);

        // 1) Load expense (and implicitly validate it exists)
        AdditionalExpense ex = service.getById(id);
                
        
        // 2) Check that a file is linked
        if (ex.getStorageKey() == null || ex.getStorageKey().isBlank()) {
            log.warn("No file attached for additional expense id={}", id);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No file is attached for this expense");
            return;
        }

        // 3) Resolve path safely
        Path base = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path filePath = base.resolve(ex.getStorageKey()).normalize();

        if (!filePath.startsWith(base)) {
            log.warn("Blocked path traversal: expenseId={}, resolved={}", id, filePath);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
            return;
        }

        if (!Files.exists(filePath)) {
            log.warn("File not found on disk for expenseId={}, path={}", id, filePath);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        // 4) Content type + safe filename
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        String safeName = (ex.getFileName() == null || ex.getFileName().isBlank())
                ? "attachment"
                : ex.getFileName().replace("\"", "");

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + safeName + "\"");

        // 5) Stream file
        try (var in = Files.newInputStream(filePath);
             var out = response.getOutputStream()) {

            in.transferTo(out);
            out.flush();

            log.info("Additional expense file download completed: expenseId={}, bytes={}",
                    id, Files.size(filePath));
        } catch (IOException exIo) {
            log.error("Failed to stream additional expense file: expenseId={}, path={}",
                    id, filePath, exIo);
            throw exIo;
        }
    }
    

    /* ========================================================= */
    public record IdNameDto(Long id, String name) {}
}
