package com.sybyl.trace.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/admin/audit-log")
@RequiredArgsConstructor
public class AppAuditController {

    private final AppAuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String showAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            Model model
    ) {
    	
        log.info("Audit log requested: page={}, size={}, query='{}'", page, size, q);

        try {
            Page<AppAuditLog> logsPage = auditService.search(q, page, size);

            log.debug("Audit log page loaded: totalElements={}, totalPages={}, size={}, number={}",
                    logsPage.getTotalElements(), logsPage.getTotalPages(), logsPage.getSize(), logsPage.getNumber()
            );

            model.addAttribute("logs", logsPage.getContent());
            model.addAttribute("page", logsPage.getNumber());
            model.addAttribute("size", logsPage.getSize());
            model.addAttribute("totalPages", logsPage.getTotalPages());
            model.addAttribute("q", q);

            model.addAttribute("pageTitle", "Application Audit Log");
            model.addAttribute("contentJsp", "admin/audit_log.jsp");

            log.info("Audit log response generated successfully for page {}", logsPage.getNumber());
            return "layout";

        } catch (Exception ex) {
            log.error("Failed to load audit log data: page={}, size={}, query='{}'", page, size, q, ex);
            throw ex;
        }
    }
}
