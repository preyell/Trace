// com.sybyl.trace.order.MarginReportService
package com.sybyl.trace.order.margin;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.notification.NotificationService;
import com.sybyl.trace.notification.NotificationType;
import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.order.OrderRepository;
import com.sybyl.trace.order.expense.AdditionalExpenseRepository;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarginReportService {

	private final MarginReportRepository repo;
	private final OrderRepository orders;
	private final VerticalRepository verticals;
	private final MarginReportAuditRepository auditRepo;
	private final EntityManager em;
	private final NotificationService notificationService;
	private final AppAuditService appAuditService;
	private final AdditionalExpenseRepository additionalExpenseRepository;
	@Value("${app.upload.margin-reports.path}")
	private String uploadPath; // injected base path

	private Path root;

	@PostConstruct
	void init() throws IOException {
		this.root = Paths.get(uploadPath).toAbsolutePath().normalize();
		Files.createDirectories(root);
		log.info("MarginReport uploads root initialized at: {}", root);
	}

	/* ========= Queries ========= */

	@Transactional(readOnly = true)
	public List<MarginReport> listForOrder(Long orderId) {
		log.debug("List margin reports: orderId={}", orderId);
		return repo.findByOrderIdOrderByUploadedOnDesc(orderId);
	}

	@Transactional(readOnly = true)
	public MarginReport getForDownload(Long orderId, Long mrId) {
		log.debug("Get for download: orderId={}, mrId={}", orderId, mrId);
		MarginReport mr = repo.findById(mrId).orElseThrow(() -> {
			log.warn("MarginReport not found: {}", mrId);
			return new IllegalArgumentException("MarginReport not found: " + mrId);
		});
		if (!mr.getOrder().getId().equals(orderId)) {
			log.warn("MarginReport {} does not belong to order {}", mrId, orderId);
			throw new IllegalArgumentException("MarginReport does not belong to order " + orderId);
		}
		return mr;
	}

	@Transactional
	public MarginReport save(Long orderId, AppUser currentUser, MultipartFile file, String labelVerticalNameIfAny,
			CurrencyCode buyingCur, CurrencyCode sellingCur, BigDecimal buyingPrice, BigDecimal sellingPrice,
			BigDecimal conversionRate, Long verticalId, String comments) throws IOException {

		log.info("Saving margin report: orderId={}, userId={}, verticalId={}, originalFile='{}'", orderId,
				(currentUser != null ? currentUser.getId() : null), verticalId,
				(file != null ? file.getOriginalFilename() : null));

		if (repo.existsByOrderIdAndVerticalId(orderId, verticalId)) {
			log.error(
					"A margin report for this vertical already exists for this order. orderId={}, userId={}, verticalId={}, originalFile='{}'",
					orderId, (currentUser != null ? currentUser.getId() : null), verticalId,
					(file != null ? file.getOriginalFilename() : null));
			throw new IllegalStateException("A margin report for this vertical already exists for this order.");
		}

		var order = orders.findById(orderId).orElseThrow(() -> {
			log.warn("Order not found: {}", orderId);
			return new IllegalArgumentException("Order not found: " + orderId);
		});

		Vertical vertical = verticals.findById(verticalId).orElseThrow(() -> {
			log.warn("Vertical not found: {}", verticalId);
			return new IllegalArgumentException("Vertical not found: " + verticalId);
		});

		if (file == null || file.isEmpty()) {
			log.warn("Upload rejected: empty file for orderId={}", orderId);
			throw new IllegalArgumentException("File is required");
		}

		String original = file.getOriginalFilename();
		String safeOriginal = sanitize(original);
		String serverFileName = orderId + "_" + Instant.now().toEpochMilli() + "_" + safeOriginal;

		Path dest = root.resolve(serverFileName).normalize();
		if (!dest.startsWith(root)) {
			log.warn("Blocked path traversal: dest={}", dest);
			throw new SecurityException("Invalid file path");
		}

		// persist file
		try {
			Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
			log.debug("Stored file: {}", dest.toAbsolutePath());
		} catch (IOException ex) {
			log.error("Failed writing file to disk: {}", dest, ex);
			throw ex;
		}

		MarginReport mr = new MarginReport();
		mr.setOrder(order);
		mr.setVertical(vertical);
		mr.setUploadedBy(currentUser);
		mr.setUploadedOn(Instant.now());

		mr.setBuyingPrice(buyingPrice.setScale(2, RoundingMode.HALF_UP));
		mr.setSellingPrice(sellingPrice.setScale(2, RoundingMode.HALF_UP));
		// FX should be 6dp for rates; your UI enforces 6dp – keep DB consistent:
		mr.setConversionRate(conversionRate.setScale(6, RoundingMode.HALF_UP));

		mr.setBuyingCurrency(buyingCur);
		mr.setSellingCurrency(sellingCur);

		mr.setFileName(original);
		mr.setStorageKey(serverFileName);

		// NEW: two-stage approval – new items start at FINANCE_PENDING
		mr.setApprovalStatus(MarginReportApprovalStatus.FINANCE_PENDING);
		mr.setRejectionReason(null);

		mr.setComments(comments);

		var saved = repo.saveAndFlush(mr);

		notificationService.notifyRole(AppRole.FINANCE_APPROVER, NotificationType.MARGIN_CREATED,
				"New margin report submitted",
				"Order " + order.getSalesOrderId() + " has a new margin report awaiting Finance approval.", "MARGIN",
				mr.getId(), "/orders/" + order.getId() + "?tab=margin");
		audit(saved.getId(), currentUser.getId(), "CREATED", "Created Margin Report");

		log.info("Margin report saved: id={}, orderId={}, storageKey='{}'", saved.getId(), orderId,
				saved.getStorageKey());
		return saved;
	}

	@Transactional
	public void update(Long orderId, Long marginReportId, BigDecimal buyingPrice, CurrencyCode buyingCurrency,
			BigDecimal sellingPrice, CurrencyCode sellingCurrency, BigDecimal conversionRate, Long verticalId,
			String comments, MultipartFile file, AppUser actor, String actorIp) {

		MarginReport mr = repo.findById(marginReportId)
				.orElseThrow(() -> new IllegalArgumentException("Margin report not found"));

		if (!mr.getOrder().getId().equals(orderId)) {
			throw new IllegalArgumentException("Invalid order");
		}

		if (mr.getApprovalStatus() == MarginReportApprovalStatus.APPROVED) {
			throw new IllegalStateException("Approved margin report cannot be edited");
		}

		boolean changed = false;

		changed |= updateIfChanged(mr.getBuyingPrice(), buyingPrice, mr::setBuyingPrice);
		changed |= updateIfChanged(mr.getBuyingCurrency(), buyingCurrency, mr::setBuyingCurrency);
		changed |= updateIfChanged(mr.getSellingPrice(), sellingPrice, mr::setSellingPrice);
		changed |= updateIfChanged(mr.getSellingCurrency(), sellingCurrency, mr::setSellingCurrency);
		changed |= updateIfChanged(mr.getConversionRate(), conversionRate, mr::setConversionRate);

		if (verticalId != null && (mr.getVertical() == null || !mr.getVertical().getId().equals(verticalId))) {
			mr.setVertical(verticals.findById(verticalId).orElseThrow());
			changed = true;
		}

		if (comments != null && !comments.equals(mr.getComments())) {
			mr.setComments(comments);
			changed = true;
		}

		if (file != null && !file.isEmpty()) {
			storeFile(mr, file, orderId); // your existing file logic
			changed = true;
		}

		if (!changed) {
			return; // NO approval reset, NO audit, NO save
		}

		mr.setApprovalStatus(MarginReportApprovalStatus.FINANCE_PENDING);
		mr.setFinanceApprovedBy(null);
		mr.setFinanceApprovedOn(null);
		mr.setCeoApprovedBy(null);
		mr.setCeoApprovedOn(null);
		mr.setRejectionReason(null);

		repo.save(mr);

		audit(mr.getId(), actor.getId(), "UPDATED", "Margin report updated");

		appAuditService.logEvent("MARGIN_REPORT", mr.getId(), mr.getOrder().getSalesOrderId(), "UPDATE",
				"Margin report updated and approval reset", null, actor, actorIp);
	}

	private <T> boolean updateIfChanged(T oldVal, T newVal, java.util.function.Consumer<T> setter) {
		if (oldVal == null && newVal == null)
			return false;
		if (oldVal != null && oldVal.equals(newVal))
			return false;
		setter.accept(newVal);
		return true;
	}

	private void storeFile(MarginReport mr, MultipartFile file, Long orderId) {
		if (file == null || file.isEmpty())
			return;

		String orig = file.getOriginalFilename();
		String safe = sanitize(orig); // use your existing sanitize() method
		String serverName = (safe != null) ? (orderId + "_" + Instant.now().toEpochMilli() + "_" + safe) : null;

		if (serverName == null) {
			throw new IllegalArgumentException("Invalid file name");
		}

		// (Optional) delete old stored file if present
		try {
			if (mr.getStorageKey() != null) {
				Path oldPath = root.resolve(mr.getStorageKey()).normalize();
				if (oldPath.startsWith(root)) {
					Files.deleteIfExists(oldPath);
				}
			}
		} catch (IOException ignore) {
			// If delete fails, we still proceed with overwrite strategy below.
			// You may log if you want.
		}

		try {
			Path dest = root.resolve(serverName).normalize();
			if (!dest.startsWith(root)) {
				throw new SecurityException("Invalid file path");
			}

			Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

			mr.setFileName(orig);
			mr.setStorageKey(serverName);

		} catch (IOException e) {
			throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
		}
	}

	@Transactional
	public void approveFinance(Long orderId, Long mrId, AppUser actor, String note) {
		var mr = repo.findById(mrId).orElseThrow();
		ensureOrder(mr, orderId);
		if (mr.getApprovalStatus() != MarginReportApprovalStatus.FINANCE_PENDING) {
			throw new IllegalStateException("Margin Report is not in FINANCE_PENDING");
		}
		assertFinanceApprover(actor);

		mr.setApprovalStatus(MarginReportApprovalStatus.CEO_PENDING);
		mr.setFinanceApprovedBy(actor);
		mr.setFinanceApprovedOn(Instant.now());
		mr.setRejectionReason(null);

		repo.saveAndFlush(mr);
		audit(mrId, actor.getId(), "APPROVE_FINANCE", note);
		notificationService.notifyRole(AppRole.CEO, NotificationType.MARGIN_APPROVED_FINANCE,
				"Margin report approved by Finance",
				"Margin report for order " + mr.getOrder().getSalesOrderId() + " awaits CEO approval.", "MARGIN",
				mr.getId(), "/orders/" + orderId + "?tab=margin");

	}

	@Transactional
	public void approveCeo(Long orderId, Long mrId, AppUser actor, String note) {
		var mr = repo.findById(mrId).orElseThrow();
		ensureOrder(mr, orderId);
		if (mr.getApprovalStatus() != MarginReportApprovalStatus.CEO_PENDING) {
			throw new IllegalStateException("Margin Report is not in CEO_PENDING");
		}
		assertCeo(actor);

		mr.setApprovalStatus(MarginReportApprovalStatus.APPROVED);
		mr.setCeoApprovedBy(actor);
		mr.setCeoApprovedOn(Instant.now());
		mr.setRejectionReason(null);

		repo.saveAndFlush(mr);
		audit(mrId, actor.getId(), "APPROVE_CEO", note);
	}

	@Transactional
	public void reject(Long orderId, Long mrId, AppUser actor, String reason) {
		var mr = repo.findById(mrId).orElseThrow();
		ensureOrder(mr, orderId);

		// who may reject depends on stage
		if (mr.getApprovalStatus() == MarginReportApprovalStatus.FINANCE_PENDING) {
			assertFinanceApprover(actor);
		} else if (mr.getApprovalStatus() == MarginReportApprovalStatus.CEO_PENDING) {
			assertCeo(actor);
		} else {
			throw new IllegalStateException("Cannot reject in state " + mr.getApprovalStatus());
		}

		mr.setApprovalStatus(MarginReportApprovalStatus.REJECTED);
		mr.setRejectionReason(reason);

		repo.saveAndFlush(mr);
		audit(mr.getId(), actor.getId(), "REJECTED", reason);
	}

	/* ========= Delete ========= */

	@Transactional
	public void delete(Long orderId, Long mrId, AppUser user, boolean deleteFile, String actorIp) throws IOException {
		var mr = repo.findById(mrId).orElseThrow();
		ensureOrder(mr, orderId);

		Long verticalId = (mr.getVertical() != null) ? mr.getVertical().getId() : null;
		if (verticalId != null) {
			boolean hasExpenses = additionalExpenseRepository.existsByOrderIdAndVerticalId(orderId, verticalId);
			if (hasExpenses) {
				throw new IllegalStateException(
						"Cannot delete this Margin Report because Additional Expenses exist for this Vertical. "
								+ "Please delete/move the expenses first.");
			}
		}
		String storage = mr.getStorageKey();

		repo.deleteById(mrId); // audits removed via FK cascade if configured
		repo.flush();

		if (deleteFile && storage != null) {
			try {
				// FIX: delete from the configured root, not as an arbitrary path
				Files.deleteIfExists(root.resolve(storage).normalize());
			} catch (Exception ex) {
				log.warn("Failed to delete margin report file '{}': {}", storage, ex.toString());
			}
		}

		appAuditService.logEvent("MARGIN_REPORT", mrId, mr.getOrder().getSalesOrderId(), "DELETE",
				"Deleted margin report " + mrId + " (Vertical=" + (verticalId != null ? verticalId : "N/A") + ")", null,
				user, actorIp);
	}

	/* ========= Helpers ========= */

	private void ensureOrder(MarginReport mr, Long orderId) {
		if (!mr.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("MR not in this Order");
	}

	private void assertFinanceApprover(AppUser actor) {
		if (actor == null || !(actor.isAdmin() || actor.hasRole(AppRole.FINANCE_APPROVER))) {
			throw new org.springframework.security.access.AccessDeniedException(
					"Only Finance Approver or Super Admin can perform this action");
		}
	}

	private void assertCeo(AppUser actor) {
		if (actor == null || !(actor.isAdmin() || actor.hasRole(AppRole.CEO))) {
			throw new org.springframework.security.access.AccessDeniedException(
					"Only CEO or Admin can perform this action");
		}
	}

	private void audit(Long mrId, Long actorUserId, String action, String note) {
		var a = new MarginReportAudit();
		a.setMarginReport(em.getReference(MarginReport.class, mrId));
		a.setActor(em.getReference(AppUser.class, actorUserId));
		a.setAction(action);
		a.setNote(note);
		a.setActedOn(Instant.now());
		auditRepo.save(a);
	}

	private static String sanitize(String name) {
		return (name == null || name.isBlank()) ? "file" : name.replaceAll("[^A-Za-z0-9._\\-]", "_");
	}
}
