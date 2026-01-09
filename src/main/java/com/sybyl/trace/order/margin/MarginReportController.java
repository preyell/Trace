package com.sybyl.trace.order.margin;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/margin-reports")
@RequiredArgsConstructor
@Slf4j
public class MarginReportController {

  private final MarginReportRepository mrRepo;
  private final MarginReportAuditRepository auditRepo;

  @GetMapping("/{id}/audits")
  public String audits(@PathVariable Long id,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

    log.info("MarginReport audits requested: mrId={}, page={}, size={}", id, page, size);

    var mr = mrRepo.findById(id)
            .orElseThrow(() -> {
              log.warn("MarginReport not found for audits: mrId={}", id);
              return new ResponseStatusException(HttpStatus.NOT_FOUND);
            });

    var audits = auditRepo.findByMarginReportIdOrderByActedOnDesc(id, PageRequest.of(page, size));

    model.addAttribute("mr", mr);
    model.addAttribute("audits", audits);


    return "margin_report/audits";
  }
}
