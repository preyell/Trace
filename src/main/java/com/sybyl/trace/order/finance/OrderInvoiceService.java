package com.sybyl.trace.order.finance;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.notification.NotificationService;
import com.sybyl.trace.notification.NotificationType;
import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.order.Order;
import com.sybyl.trace.order.OrderRepository;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderInvoiceService {

    private final OrderInvoiceRepository repo;
    private final OrderRepository orders;

    private final AppAuditService appAuditService;
    private final NotificationService notificationService;

    @Value("${app.upload.invoices.path:data/invoices}")
    private String uploadPath;

    private Path root;

    public OrderInvoiceService(OrderInvoiceRepository repo,
                               OrderRepository orders,
                               AppAuditService appAuditService,
                               NotificationService notificationService) {
        this.repo = repo;
        this.orders = orders;
        this.appAuditService = appAuditService;
        this.notificationService = notificationService;
    }

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Invoice upload root initialized: {}", root);
    }

    @Transactional(readOnly = true)
    public List<OrderInvoice> listByOrder(Long orderId) {
        log.debug("List invoices requested: orderId={}", orderId);
        return repo.findByOrderId(orderId);
    }

    @Transactional
    public OrderInvoice create(Long orderId,
                               String invoiceNumber,
                               BigDecimal amount,
                               CurrencyCode currency,
                               LocalDate invoiceDeliveryDate,
                               LocalDate expectedPaymentDate,
                               LocalDate paymentReceivedDate,
                               BigDecimal invoicePercent,
                               BigDecimal conversionRate,
                               boolean finalInvoice,
                               MultipartFile file,
                               AppUser actor,
                               String actorIp) throws IOException {

        Order order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        log.info("Create invoice: orderId={}, soid={}, invoiceNumber={}, actor={}",
                orderId, order.getSalesOrderId(), invoiceNumber,
                actor != null ? actor.getUsername() : "SYSTEM");

        validate(amount, invoicePercent);

        var inv = new OrderInvoice();
        inv.setOrder(order);
        inv.setInvoiceNumber(invoiceNumber.trim());
        inv.setAmount(amount.setScale(2));
        inv.setCurrency(currency);
        inv.setInvoiceDeliveryDate(invoiceDeliveryDate);
        inv.setExpectedPaymentDate(expectedPaymentDate);
        inv.setPaymentReceivedDate(paymentReceivedDate);
        inv.setInvoicePercent(invoicePercent.setScale(2));

        if (conversionRate != null) inv.setConversionRate(conversionRate.setScale(6));
        inv.setFinalInvoice(finalInvoice);
        inv.setUploadedBy(actor);
        inv.setUploadedOn(Instant.now());

        if (file != null && !file.isEmpty()) {
            storeFile(orderId, file, inv);
        }

        OrderInvoice saved = repo.save(inv);

        // App audit
        appAuditService.logEvent(
                "INVOICE",
                saved.getId(),
                order.getSalesOrderId(),
                "CREATE",
                "Created invoice " + saved.getInvoiceNumber() + " for order " + order.getSalesOrderId(),
                null,
                actor,
                actorIp
        );

        // Notification: finance people should know invoice added/changed
        notificationService.notifyRole(
                AppRole.FINANCE,
                NotificationType.INVOICE_CREATED,
                "Invoice added",
                "Invoice " + saved.getInvoiceNumber() + " added for order " + order.getSalesOrderId(),
                "INVOICE",
                saved.getId(),
                "/orders/" + orderId + "?tab=finance"
        );

        return saved;
    }

    @Transactional
    public OrderInvoice update(Long orderId,
                               Long invoiceId,
                               String invoiceNumber,
                               BigDecimal amount,
                               CurrencyCode currency,
                               LocalDate invoiceDeliveryDate,
                               LocalDate expectedPaymentDate,
                               LocalDate paymentReceivedDate,
                               BigDecimal invoicePercent,
                               BigDecimal conversionRate,
                               boolean finalInvoice,
                               MultipartFile file,
                               AppUser actor,
                               String actorIp) throws IOException {

        var inv = repo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!inv.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Wrong order");
        }

        validate(amount, invoicePercent);

        log.info("Update invoice: orderId={}, invoiceId={}, invoiceNumber={}, actor={}",
                orderId, invoiceId, invoiceNumber, actor != null ? actor.getUsername() : "SYSTEM");

        inv.setInvoiceNumber(invoiceNumber.trim());
        inv.setAmount(amount.setScale(2));
        inv.setCurrency(currency);
        inv.setInvoiceDeliveryDate(invoiceDeliveryDate);
        inv.setExpectedPaymentDate(expectedPaymentDate);
        inv.setPaymentReceivedDate(paymentReceivedDate);
        inv.setInvoicePercent(invoicePercent.setScale(2));
        inv.setConversionRate(conversionRate != null ? conversionRate.setScale(6) : null);
        inv.setFinalInvoice(finalInvoice);

        if (file != null && !file.isEmpty()) {
            // optional: delete old file if exists (kept conservative)
            storeFile(orderId, file, inv);
        }

        OrderInvoice saved = repo.save(inv);

        appAuditService.logEvent(
                "INVOICE",
                invoiceId,
                inv.getOrder().getSalesOrderId(),
                "UPDATE",
                "Updated invoice " + saved.getInvoiceNumber() + " for order " + inv.getOrder().getSalesOrderId(),
                null,
                actor,
                actorIp
        );

        notificationService.notifyRole(
                AppRole.FINANCE,
                NotificationType.INVOICE_UPDATED,
                "Invoice updated",
                "Invoice " + saved.getInvoiceNumber() + " updated for order " + inv.getOrder().getSalesOrderId(),
                "INVOICE",
                saved.getId(),
                "/orders/" + orderId + "?tab=finance"
        );

        return saved;
    }

    @Transactional
    public void delete(Long orderId, Long invoiceId, boolean deleteFile, AppUser actor, String actorIp) throws IOException {
        var inv = repo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!inv.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Wrong order");
        }

        log.warn("Delete invoice: orderId={}, invoiceId={}, deleteFile={}, actor={}",
                orderId, invoiceId, deleteFile, actor != null ? actor.getUsername() : "SYSTEM");

        if (deleteFile && inv.getStorageKey() != null) {
            Path p = root.resolve(inv.getStorageKey()).normalize();
            if (p.startsWith(root)) {
                Files.deleteIfExists(p);
            }
        }

        repo.delete(inv);

        appAuditService.logEvent(
                "INVOICE",
                invoiceId,
                inv.getOrder().getSalesOrderId(),
                "DELETE",
                "Deleted invoice " + inv.getInvoiceNumber() + " for order " + inv.getOrder().getSalesOrderId(),
                null,
                actor,
                actorIp
        );

        // No notification on delete by default (avoids spam). If you want, we can add.
    }

    private static void validate(BigDecimal amount, BigDecimal invoicePercent) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (invoicePercent == null || invoicePercent.signum() < 0) {
            throw new IllegalArgumentException("Invoice % must be >= 0");
        }
    }

    private void storeFile(Long orderId, MultipartFile file, OrderInvoice inv) throws IOException {
        String orig = file.getOriginalFilename();
        String safe = orig == null ? "file" : orig.replaceAll("[^A-Za-z0-9._\\-]", "_");
        String serverName = orderId + "_" + Instant.now().toEpochMilli() + "_" + safe;

        Files.createDirectories(root);
        Path dest = root.resolve(serverName).normalize();
        if (!dest.startsWith(root)) {
            throw new SecurityException("Invalid file path");
        }
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        inv.setFileName(orig);
        inv.setStorageKey(serverName);
    }
}
