package com.sybyl.trace.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/margin-reports")
@RequiredArgsConstructor
public class MarginReportController {
  private final MarginReportRepository mrRepo;
  private final MarginReportAuditRepository auditRepo;

  @GetMapping("/{id}/audits")
  public String audits(@PathVariable Long id,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
    var mr = mrRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var audits = auditRepo.findByMarginReportIdOrderByActedOnDesc(id, PageRequest.of(page, size));
    model.addAttribute("mr", mr);
    model.addAttribute("audits", audits);
    return "margin_report/audits";
  }
}
