package com.sybyl.trace.order.finance;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.web.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/orders/{orderId}/invoices")
@Slf4j
public class OrderInvoiceController {

    private final OrderInvoiceService service;

    public OrderInvoiceController(OrderInvoiceService service) {
        this.service = service;
    }

    @PostMapping
    public String create(@PathVariable Long orderId,
                         @RequestParam String invoiceNumber,
                         @RequestParam BigDecimal amount,
                         @RequestParam CurrencyCode currency,
                         @RequestParam String invoiceDeliveryDate,
                         @RequestParam String expectedPaymentDate,
                         @RequestParam(required = false) String paymentReceivedDate,
                         @RequestParam BigDecimal invoicePercent,
                         @RequestParam(required = false) BigDecimal conversionRate,
                         @RequestParam(required = false, defaultValue = "false") boolean finalInvoice,
                         @RequestParam(required = false) MultipartFile file,
                         @AuthenticationPrincipal MyUserDetails me,
                         RedirectAttributes ra,
                         HttpServletRequest request) {

        String actorIp = IpUtils.getClientIp(request);

        log.info("Invoice create requested: orderId={}, invoiceNumber={}, actor={}, ip={}",
                orderId, invoiceNumber,
                me != null && me.getUser() != null ? me.getUser().getUsername() : "SYSTEM",
                actorIp);

        try {
            service.create(
                    orderId,
                    invoiceNumber,
                    amount,
                    currency,
                    LocalDate.parse(invoiceDeliveryDate),
                    LocalDate.parse(expectedPaymentDate),
                    (paymentReceivedDate == null || paymentReceivedDate.isBlank())
                            ? null : LocalDate.parse(paymentReceivedDate),
                    invoicePercent,
                    conversionRate,
                    finalInvoice,
                    file,
                    me.getUser()
                    
            );
            ra.addFlashAttribute("message", "Invoice added.");
        } catch (Exception e) {
            log.error("Invoice create failed: orderId={}, invoiceNumber={}", orderId, invoiceNumber, e);
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders/" + orderId + "?tab=finance";
    }

    @PostMapping("/{invoiceId}/update")
    public String update(@PathVariable Long orderId,
                         @PathVariable Long invoiceId,
                         @RequestParam String invoiceNumber,
                         @RequestParam BigDecimal amount,
                         @RequestParam CurrencyCode currency,
                         @RequestParam String invoiceDeliveryDate,
                         @RequestParam String expectedPaymentDate,
                         @RequestParam(required = false) String paymentReceivedDate,
                         @RequestParam BigDecimal invoicePercent,
                         @RequestParam(required = false) BigDecimal conversionRate,
                         @RequestParam(required = false, defaultValue = "false") boolean finalInvoice,
                         @RequestParam(required = false) MultipartFile file,
                         @AuthenticationPrincipal MyUserDetails me,
                         RedirectAttributes ra,
                         HttpServletRequest request) {


        log.info("Invoice update requested: orderId={}, invoiceId={}, invoiceNumber={}, actor={}, ip={}",
                orderId, invoiceId, invoiceNumber,
                me != null && me.getUser() != null ? me.getUser().getUsername() : "SYSTEM"
                );

        try {
            service.update(
                    orderId,
                    invoiceId,
                    invoiceNumber,
                    amount,
                    currency,
                    LocalDate.parse(invoiceDeliveryDate),
                    LocalDate.parse(expectedPaymentDate),
                    (paymentReceivedDate == null || paymentReceivedDate.isBlank())
                            ? null : LocalDate.parse(paymentReceivedDate),
                    invoicePercent,
                    conversionRate,
                    finalInvoice,
                    file,
                    me != null ? me.getUser() : null
                    
            );
            ra.addFlashAttribute("message", "Invoice updated.");
        } catch (IOException | RuntimeException e) {
            log.error("Invoice update failed: orderId={}, invoiceId={}", orderId, invoiceId, e);
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders/" + orderId + "?tab=finance";
    }

    @PostMapping("/{invoiceId}/delete")
    public String delete(@PathVariable Long orderId,
                         @PathVariable Long invoiceId,
                         @RequestParam(name = "deleteFile", defaultValue = "false") boolean deleteFile,
                         @AuthenticationPrincipal MyUserDetails me,
                         RedirectAttributes ra,
                         HttpServletRequest request) {


        log.warn("Invoice delete requested: orderId={}, invoiceId={}, deleteFile={}, actor={}, ip={}",
                orderId, invoiceId, deleteFile,
                me != null && me.getUser() != null ? me.getUser().getUsername() : "SYSTEM"
                );

        try {
            service.delete(orderId, invoiceId, deleteFile, me != null ? me.getUser() : null);
            ra.addFlashAttribute("message", "Invoice deleted successfully.");
        } catch (Exception e) {
            log.error("Invoice delete failed: orderId={}, invoiceId={}", orderId, invoiceId, e);
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders/" + orderId + "?tab=finance";
    }
    
    @GetMapping("/{invoiceId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long orderId,
                                             @PathVariable Long invoiceId) {
        return service.download(orderId, invoiceId);
    }
}
