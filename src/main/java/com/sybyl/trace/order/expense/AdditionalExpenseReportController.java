package com.sybyl.trace.order.expense;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AdditionalExpenseReportController {

    private final AdditionalExpenseService service;

    public AdditionalExpenseReportController(AdditionalExpenseService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','FINANCE_APPROVER', 'CFO','CEO')")
    @GetMapping("/reports/expenses")
    public String expenseReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String searchOrder,
            @RequestParam(required = false) String searchCustomer,
            Model model) {

        var pageable = PageRequest.of(page, size); 
        
        // 1. Fetch filtered table rows
        var p = service.getOrderExpenseSummary(searchOrder, searchCustomer, pageable);
        model.addAttribute("page", p);
        model.addAttribute("rows", p.getContent());

        // 2. Fetch lists to pre-populate our searchable dropdowns
        List<String> distinctOrders = service.getDistinctSalesOrderIds();
        List<String> distinctCustomers = service.getDistinctCustomerNames();
        
        model.addAttribute("orderOptions", distinctOrders);
        model.addAttribute("customerOptions", distinctCustomers);

        model.addAttribute("contentJsp", "reports/expense_report.jsp");
        return "layout";
    }

    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','FINANCE_APPROVER','CFO','CEO')")
    @GetMapping("/reports/expenses/{orderId}/details")
    public String expenseDetailsForOrder(@PathVariable Long orderId, Model model) {
        log.debug("Additional expense report details requested: orderId={}", orderId);
        var expenses = service.findByOrderWithExpenses(orderId);
        model.addAttribute("expenses", expenses);
        return "reports/_expense_order_details";
    }
    
   
}
