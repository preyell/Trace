// com.sybyl.trace.order.NetMarginReportService.java
package com.sybyl.trace.order;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sybyl.trace.order.expense.AdditionalExpense;
import com.sybyl.trace.order.expense.AdditionalExpenseService;
import com.sybyl.trace.order.finance.OrderInvoice;
import com.sybyl.trace.order.finance.OrderInvoiceService;
import com.sybyl.trace.order.margin.MarginReport;
import com.sybyl.trace.order.margin.MarginReportService;

@Service
public class NetMarginReportService {

    private final OrderService orderService;
    private final AdditionalExpenseService additionalExpenseService;
    private final OrderInvoiceService orderInvoiceService;
    private final VelocityEngine velocityEngine;
    private final DecimalFormat moneyFmt;
    private final DateTimeFormatter dtFmt;

    public NetMarginReportService(OrderService orderService,
                                  MarginReportService marginReportService,
                                  AdditionalExpenseService additionalExpenseService,
                                  OrderInvoiceService orderInvoiceService,
                                  VelocityEngine velocityEngine) {
        this.orderService = orderService;
        this.additionalExpenseService = additionalExpenseService;
        this.orderInvoiceService = orderInvoiceService;
        this.velocityEngine = velocityEngine;
        this.moneyFmt = new DecimalFormat("0.00");

        // 🔹 Human-friendly date/time
        this.dtFmt = DateTimeFormatter
                .ofPattern("dd-MMM-yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        
    }

    /**
     * Top-level method called by controller.
     * Everything (DB reads + Velocity render) runs inside one read-only transaction.
     */
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long orderId) {
        String html = buildHtml(orderId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate Net Margin PDF for order " + orderId, ex);
        }
    }

    // If you also want a preview endpoint, you can call this from there too.
    @Transactional(readOnly = true)
    public String generateHtmlForPreview(Long orderId) {
        return buildHtml(orderId);
    }

    /**
     * Private helper to assemble data + render Velocity.
     * This is executed within the transactional methods above.
     */
    private String buildHtml(Long orderId) {
        // Use the same session for everything
        Order order = orderService.getForDetails(orderId);

        List<MarginReport> marginReports =
                new ArrayList<>(order.getMarginReports());

        List<AdditionalExpense> exps =
                additionalExpenseService.listForOrder(orderId);


        VelocityContext ctx = new VelocityContext();
        ctx.put("order", order);
        ctx.put("marginReports", marginReports);
        ctx.put("additionalExpenses", exps);
        ctx.put("fmtMoney", moneyFmt);   
        ctx.put("fmtDate", dtFmt);
        StringWriter writer = new StringWriter();
        velocityEngine.mergeTemplate("templates/net-margin-report.vm",
                                     "UTF-8", ctx, writer);
        return writer.toString();
    }
}
