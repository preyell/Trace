package com.sybyl.trace.order.expense;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AdditionalExpenseReportController {

    private final AdditionalExpenseService service;

    public AdditionalExpenseReportController(AdditionalExpenseService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','CFO','CEO')")
    @GetMapping("/reports/expenses")
    public String expenseReport(Model model) {
        log.info("Additional expense report requested");
        List<OrderAdditionalExpenseSummaryRow> rows = service.getOrderExpenseSummary();
        model.addAttribute("rows", rows);
        model.addAttribute("contentJsp", "reports/expense_report.jsp");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','CFO','CEO')")
    @GetMapping("/reports/expenses/{orderId}/details")
    public String expenseDetailsForOrder(@PathVariable Long orderId, Model model) {
        log.debug("Additional expense report details requested: orderId={}", orderId);
        var expenses = service.findByOrderWithExpenses(orderId);
        model.addAttribute("expenses", expenses);
        return "reports/_expense_order_details";
    }
}
