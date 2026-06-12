package com.sybyl.trace.order;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.order.expense.AdditionalExpense;
import com.sybyl.trace.order.expense.AdditionalExpenseService;
import com.sybyl.trace.order.expense.AdditionalExpenseStatus;
import com.sybyl.trace.order.margin.MarginReport;
import com.sybyl.trace.order.margin.MarginReportApprovalStatus;
import com.sybyl.trace.order.margin.MarginReportService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderStatusService {

    private final MarginReportService marginReportService;
    private final AdditionalExpenseService additionalExpenseService;
    private final AppAuditService auditService;

    public OrderStatusService(MarginReportService marginReportService,
                              AdditionalExpenseService additionalExpenseService,
                              AppAuditService auditService) {
        this.marginReportService = marginReportService;
        this.additionalExpenseService = additionalExpenseService;
        this.auditService = auditService;
    }

    public OrderStatusView computeOrderStatus(Long orderId) {

        log.debug("OrderStatusService.computeOrderStatus requested for orderId={}", orderId);

        // 1) Margin Reports
        List<MarginReport> reports = marginReportService.listForOrder(orderId);
        MarginReportApprovalStatus mrStatus = computeOverallMarginStatus(reports);

        // ---- NO MARGIN CASE ----
        if (mrStatus == null) {
            log.debug("OrderStatusService: orderId={} has NO margin reports", orderId);

            return OrderStatusView.NO_MARGIN;
        }

        // Margin decision
        switch (mrStatus) {
            case FINANCE_PENDING:
                return logAndReturn(orderId, "MR_FINANCE_PENDING", OrderStatusView.MR_FINANCE_PENDING);

            case CEO_PENDING:
                return logAndReturn(orderId, "MR_CEO_PENDING", OrderStatusView.MR_CEO_PENDING);

            case REJECTED:
                return logAndReturn(orderId, "MR_REJECTED", OrderStatusView.MR_REJECTED);

            case APPROVED:
                log.debug("OrderStatusService: orderId={} margin APPROVED", orderId);
                break;

            default:
                return logAndReturn(orderId, "NO_MARGIN", OrderStatusView.NO_MARGIN);
        }

        // 2) Additional Expenses (only when margin approved)
        List<AdditionalExpense> exps = additionalExpenseService.listForOrder(orderId);
        AdditionalExpenseStatus expStatus = computeOverallExpenseStatus(exps);

        if (expStatus == null) {
            return logAndReturn(orderId, "MR_APPROVED_NO_EXPENSE", OrderStatusView.MR_APPROVED_NO_EXPENSE);
        }

        switch (expStatus) {
            case WAITING:
                return logAndReturn(orderId, "EXP_CEO_PENDING", OrderStatusView.EXP_CEO_PENDING);

            case CEO_APPROVED:
                return logAndReturn(orderId, "EXP_CFO_PENDING", OrderStatusView.EXP_CFO_PENDING);

            case CFO_APPROVED:
                return logAndReturn(orderId, "EXP_CFO_APPROVED", OrderStatusView.EXP_CFO_APPROVED);

            case REJECTED:
                return logAndReturn(orderId, "EXP_REJECTED", OrderStatusView.EXP_REJECTED);

            default:
                return logAndReturn(orderId, "MR_APPROVED_NO_EXPENSE", OrderStatusView.MR_APPROVED_NO_EXPENSE);
        }
    }

    private OrderStatusView logAndReturn(Long orderId, String statusLabel, OrderStatusView view) {
        log.debug("OrderStatusService: orderId={} → {}", orderId, statusLabel);
        return view;
    }

    // ---------- Aggregation helpers ----------

    private MarginReportApprovalStatus computeOverallMarginStatus(List<MarginReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return null;
        }

        boolean anyFinancePending = reports.stream()
                .anyMatch(r -> r.getApprovalStatus() == MarginReportApprovalStatus.FINANCE_PENDING);
        if (anyFinancePending) return MarginReportApprovalStatus.FINANCE_PENDING;

        boolean anyCeoPending = reports.stream()
                .anyMatch(r -> r.getApprovalStatus() == MarginReportApprovalStatus.CEO_PENDING);
        if (anyCeoPending) return MarginReportApprovalStatus.CEO_PENDING;

        boolean anyRejected = reports.stream()
                .anyMatch(r -> r.getApprovalStatus() == MarginReportApprovalStatus.REJECTED);
        if (anyRejected) return MarginReportApprovalStatus.REJECTED;

        return MarginReportApprovalStatus.APPROVED;
    }

    private AdditionalExpenseStatus computeOverallExpenseStatus(List<AdditionalExpense> exps) {
        if (exps == null || exps.isEmpty()) {
            return null;
        }

        boolean anyWaiting = exps.stream()
                .anyMatch(e -> e.getApprovalStatus() == AdditionalExpenseStatus.WAITING);
        if (anyWaiting) return AdditionalExpenseStatus.WAITING;

        boolean anyCeoApproved = exps.stream()
                .anyMatch(e -> e.getApprovalStatus() == AdditionalExpenseStatus.CEO_APPROVED);
        if (anyCeoApproved) return AdditionalExpenseStatus.CEO_APPROVED;

        boolean anyRejected = exps.stream()
                .anyMatch(e -> e.getApprovalStatus() == AdditionalExpenseStatus.REJECTED);
        if (anyRejected) return AdditionalExpenseStatus.REJECTED;

        return AdditionalExpenseStatus.CFO_APPROVED;
    }
}
