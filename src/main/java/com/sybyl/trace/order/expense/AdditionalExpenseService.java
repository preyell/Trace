// com.sybyl.trace.order.expense.AdditionalExpenseService.java
package com.sybyl.trace.order.expense;

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
import com.sybyl.trace.masterdata.AdditionalExpenseLabelRepository;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.notification.NotificationService;
import com.sybyl.trace.notification.NotificationType;
import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.order.OrderRepository;
import com.sybyl.trace.order.margin.MarginReportRepository;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdditionalExpenseService {

	private final AdditionalExpenseRepository repo;
	private final AdditionalExpenseAuditRepository auditRepo;
	private final AdditionalExpenseDisbursementRepository disbRepo;
	private final OrderRepository orders;
	private final AdditionalExpenseLabelRepository labels;
	private final VerticalRepository verticals;
	private final EntityManager em;
	private final MarginReportRepository marginReports;

	private final AppAuditService appAuditService;
	private final NotificationService notificationService;

	@Value("${app.upload.additional-expenses.path}")
	private String uploadPath;

	private Path root;

	@PostConstruct
	void init() throws IOException {
		root = Paths.get(uploadPath).toAbsolutePath().normalize();
		Files.createDirectories(root);
		log.info("AdditionalExpense upload root initialized: {}", root);
	}

	/* ========================= READ ========================= */

	@Transactional(readOnly = true)
	public List<AdditionalExpense> listForOrder(Long orderId) {
		log.debug("listForOrder: orderId={}", orderId);
		return repo.findByOrderIdOrderByUploadedOnDesc(orderId);
	}

	public AdditionalExpense getById(Long id) {
		log.debug("getById: id={}", id);
		return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
	}

	@Transactional(readOnly = true)
	public List<AdditionalExpenseDisbursement> listDisbursements(Long expenseId) {
		log.debug("listDisbursements: expenseId={}", expenseId);
		return disbRepo.findByExpenseIdWithActor(expenseId);
	}

	@Transactional(readOnly = true)
	public List<AdditionalExpenseAudit> audits(Long expenseId) {
		log.debug("audits: expenseId={}", expenseId);
		return auditRepo.findByExpenseIdOrderByActedOnDesc(expenseId);
	}

	@Transactional(readOnly = true)
	public List<OrderAdditionalExpenseSummaryRow> getOrderExpenseSummary() {
		log.debug("getOrderExpenseSummary");
		return repo.findOrderExpenseSummary();
	}

	@Transactional(readOnly = true)
	public List<AdditionalExpense> findByOrderWithExpenses(Long orderId) {
		log.debug("findByOrderWithExpenses: orderId={}", orderId);
		return repo.findByOrderId(orderId);
	}

	/* ========================= CREATE ========================= */

	@Transactional
	public AdditionalExpense create(Long orderId, AppUser user, Long labelId, BigDecimal amount, CurrencyCode currency,
			BigDecimal conversionRate, Long verticalId, String comments, MultipartFile file, String actorIp)
			throws IOException {

		log.info("create additional expense: orderId={}, verticalId={}, actor={}", orderId, verticalId,
				user != null ? user.getUsername() : "SYSTEM");

		if (verticalId == null)
			throw new IllegalArgumentException("Vertical is required");

		if (!marginReports.existsByOrderIdAndVerticalId(orderId, verticalId)) {
			throw new IllegalStateException(
					"Cannot add additional expense for this vertical until a Margin Report is uploaded for the same vertical.");
		}

		var order = orders.findById(orderId).orElseThrow();
		var label = labels.findById(labelId).orElseThrow();
		var vertical = verticals.findById(verticalId).orElseThrow();

		if (comments == null || comments.isBlank())
			throw new IllegalArgumentException("Comments are required");
		if (amount == null || amount.signum() <= 0)
			throw new IllegalArgumentException("Amount must be positive");

		String orig = file != null ? file.getOriginalFilename() : null;
		String safe = sanitize(orig);
		String serverName = (safe != null) ? (orderId + "_" + Instant.now().toEpochMilli() + "_" + safe) : null;

		if (file != null && !file.isEmpty()) {
			var dest = root.resolve(serverName).normalize();
			if (!dest.startsWith(root))
				throw new SecurityException("Invalid file path");
			Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
		}

		var exp = new AdditionalExpense();
		exp.setOrder(order);
		exp.setLabel(label);
		exp.setVertical(vertical);
		exp.setUploadedBy(user);
		exp.setUploadedOn(Instant.now());
		exp.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
		exp.setCurrency(currency);

		if (currency == CurrencyCode.USD) {
			BigDecimal fx = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
			exp.setConversionRate(fx);
			exp.setAmountUsd(amount.setScale(2, RoundingMode.HALF_UP));
		} else {
			if (conversionRate == null || conversionRate.signum() <= 0) {
				throw new IllegalArgumentException(
						"Conversion rate is required and must be positive for non-USD expenses");
			}
			BigDecimal fx = conversionRate.setScale(6, RoundingMode.HALF_UP);
			exp.setConversionRate(fx);
			BigDecimal usd = amount.divide(fx, 6, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
			exp.setAmountUsd(usd);
		}

		exp.setComments(comments.trim());
		exp.setFileName(orig);
		exp.setStorageKey(serverName);
		exp.setApprovalStatus(AdditionalExpenseStatus.WAITING);

		var saved = repo.save(exp);

		audit(saved.getId(), user != null ? user.getId() : null, "CREATED", "Additional expense created");

		appAuditService.logEvent("ADDITIONAL_EXPENSE", saved.getId(), order.getSalesOrderId(), "CREATE",
				"Created additional expense " + saved.getId() + " (" + saved.getAmount() + " " + saved.getCurrency()
						+ ") on order " + order.getSalesOrderId(),
				null, user, actorIp);

		notificationService.notifyRole(AppRole.CEO, NotificationType.EXPENSE_CREATED,
				"Additional expense submitted",
				"Order " + order.getSalesOrderId() + " has a new additional expense to review", "ADDITIONAL_EXPENSE",
				saved.getId(), "/orders/" + orderId + "?tab=margin");

		return saved;
	}

	/* ========================= APPROVALS ========================= */

	@Transactional
	public void ceoApprove(Long orderId, Long expenseId, AppUser actor, String note, String actorIp) {
		log.info("ceoApprove: orderId={}, expenseId={}, actor={}", orderId, expenseId,
				actor != null ? actor.getUsername() : "SYSTEM");

		var e = mustLoad(orderId, expenseId);
		if (e.getApprovalStatus() != AdditionalExpenseStatus.WAITING)
			throw new IllegalStateException("Not awaiting CEO approval");

		e.setApprovalStatus(AdditionalExpenseStatus.CEO_APPROVED);
		e.setCeoApprovedBy(actor);
		e.setCeoApprovedOn(Instant.now());

		audit(e.getId(), actor != null ? actor.getId() : null, "CEO_APPROVED", note);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, e.getOrder().getSalesOrderId(), "CEO_APPROVE",
				"CEO approved additional expense " + expenseId + " on order " + e.getOrder().getSalesOrderId()
						+ (note != null && !note.isBlank() ? (". Note: " + note) : ""),
				null, actor, actorIp);

		// NOTIFICATION: move to CFO
		notificationService.notifyRole(AppRole.CFO, NotificationType.EXPENSE_APPROVED_CEO, "Expense awaiting CFO approval",
				"Order " + e.getOrder().getSalesOrderId() + " expense is approved by CEO and awaits CFO approval",
				"ADDITIONAL_EXPENSE", expenseId, "/orders/" + orderId + "?tab=margin");
	}

	@Transactional
	public void cfoApprove(Long orderId, Long expenseId, AppUser actor, String note, String actorIp) {
		log.info("cfoApprove: orderId={}, expenseId={}, actor={}", orderId, expenseId,
				actor != null ? actor.getUsername() : "SYSTEM");

		var e = mustLoad(orderId, expenseId);
		if (e.getApprovalStatus() != AdditionalExpenseStatus.CEO_APPROVED)
			throw new IllegalStateException("Not awaiting CFO approval");

		e.setApprovalStatus(AdditionalExpenseStatus.CFO_APPROVED);
		e.setCfoApprovedBy(actor);
		e.setCfoApprovedOn(Instant.now());

		audit(e.getId(), actor != null ? actor.getId() : null, "CFO_APPROVED", note);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, e.getOrder().getSalesOrderId(), "CFO_APPROVE",
				"CFO approved additional expense " + expenseId + " on order " + e.getOrder().getSalesOrderId()
						+ (note != null && !note.isBlank() ? (". Note: " + note) : ""),
				null, actor, actorIp);


	}

	@Transactional
	public void reject(Long orderId, Long expenseId, AppUser actor, String reason, String actorIp) {
		log.warn("reject: orderId={}, expenseId={}, actor={}, reason={}", orderId, expenseId,
				actor != null ? actor.getUsername() : "SYSTEM", reason);

		var e = mustLoad(orderId, expenseId);

		e.setApprovalStatus(AdditionalExpenseStatus.REJECTED);
		e.setRejectedBy(actor);
		e.setRejectedOn(Instant.now());
		e.setRejectionReason(reason);

		audit(e.getId(), actor != null ? actor.getId() : null, "REJECTED", reason);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, e.getOrder().getSalesOrderId(), "REJECT",
				"Rejected additional expense " + expenseId + " on order " + e.getOrder().getSalesOrderId()
						+ ". Reason: " + reason,
				null, actor, actorIp);

		// NOTIFICATION: tell uploader
		if (e.getUploadedBy() != null) {
			notificationService.notifyUser(e.getUploadedBy(), NotificationType.EXPENSE_REJECTED, "Expense rejected",
					reason, "ADDITIONAL_EXPENSE", expenseId, "/orders/" + orderId + "?tab=margin");
		}
	}

	/* ========================= DISBURSEMENT ========================= */

	@Transactional
	public AdditionalExpenseDisbursement disburse(Long orderId, Long expenseId, BigDecimal amount,
			CurrencyCode currency, BigDecimal conversionRate, String note, AppUser actor, String actorIp) {

		log.info("disburse: orderId={}, expenseId={}, amount={} {}, actor={}", orderId, expenseId, amount, currency,
				actor != null ? actor.getUsername() : "SYSTEM");

		var exp = repo.findById(expenseId).orElseThrow(() -> new IllegalArgumentException("Expense not found"));

		if (!exp.getOrder().getId().equals(orderId)) {
			throw new IllegalArgumentException("Wrong order");
		}

		if (exp.getApprovalStatus() != AdditionalExpenseStatus.CFO_APPROVED) {
			throw new IllegalStateException("Consumption not allowed until CFO approval.");
		}

		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("Consumption amount must be positive");
		}

		// Convert disbursement to USD
		BigDecimal usd;
		BigDecimal fxForThisDisb;

		if (currency == CurrencyCode.USD) {
			fxForThisDisb = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
			usd = amount.setScale(2, RoundingMode.HALF_UP);
		} else {
			if (conversionRate == null || conversionRate.signum() <= 0) {
				throw new IllegalArgumentException(
						"Conversion rate is required and must be positive for non-USD consumptions");
			}
			fxForThisDisb = conversionRate.setScale(6, RoundingMode.HALF_UP);
			usd = amount.divide(fxForThisDisb, 6, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal already = disbRepo.sumUsdByExpense(expenseId);
		if (already == null)
			already = BigDecimal.ZERO;

		BigDecimal cap = exp.getAmountUsd();

		if (already.add(usd).compareTo(cap) > 0) {
			BigDecimal remaining = cap.subtract(already);
			throw new IllegalStateException(
					"Total consumed would exceed approved amount. Remaining USD " + remaining.toPlainString());
		}

		var d = new AdditionalExpenseDisbursement();
		d.setExpense(em.getReference(AdditionalExpense.class, expenseId));
		d.setActor(actor != null ? em.getReference(AppUser.class, actor.getId()) : null);
		d.setAmount(amount);
		d.setCurrency(currency);
		d.setAmountUsd(usd);
		d.setDisbursedOn(Instant.now());
		d.setNote(note);
		d.setConversionRate(fxForThisDisb);

		var saved = disbRepo.save(d);

		audit(expenseId, actor != null ? actor.getId() : null, "CONSUMED",
				"Amount " + amount + " " + currency + " (~" + usd + " USD)");

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, exp.getOrder().getSalesOrderId(), "CONSUMED",
				"Disbursed " + amount + " " + currency + " (~" + usd + " USD) for additional expense " + expenseId
						+ " on order " + exp.getOrder().getSalesOrderId(),
				null, actor, actorIp);

		return saved;
	}

	@Transactional
	public void deleteDisbursement(Long orderId, Long expenseId, Long disbId, AppUser actor, String actorIp) {
		log.warn("deleteDisbursement: orderId={}, expenseId={}, disbId={}, actor={}",
				orderId, expenseId, disbId, actor != null ? actor.getUsername() : "SYSTEM");

		AdditionalExpense ex = repo.findById(expenseId)
				.orElseThrow(() -> new IllegalArgumentException("Expense not found"));

		if (!ex.getOrder().getId().equals(orderId)) {
			throw new IllegalArgumentException("Expense does not belong to this order");
		}

		disbRepo.deleteByIdAndExpenseId(disbId, expenseId);

		audit(expenseId, actor != null ? actor.getId() : null, "CONSUMPTION_DELETED", "ID " + disbId);

		appAuditService.logEvent(
				"ADDITIONAL_EXPENSE",
				expenseId,
				ex.getOrder().getSalesOrderId(),
				"CONSUMPTION_DELETED",
				"Deleted consumption " + disbId + " for additional expense " + expenseId
						+ " on order " + ex.getOrder().getSalesOrderId(),
				null,
				actor,
				actorIp
		);

	}

	/* ========================= UPDATE ========================= */

	@Transactional
	public void update(Long orderId, Long expId, AppUser actor, Long labelId, BigDecimal amount, CurrencyCode currency,
			BigDecimal rate, Long verticalId, String comments, String actorIp, MultipartFile file) {

		log.info("update expense: orderId={}, expId={}, actor={}", orderId, expId,
				actor != null ? actor.getUsername() : "SYSTEM");

		var ex = repo.findById(expId).orElseThrow();
		assertOrder(ex, orderId);

		if (verticalId == null)
			throw new IllegalArgumentException("Vertical is required");

		if (!marginReports.existsByOrderIdAndVerticalId(orderId, verticalId)) {
			throw new IllegalStateException(
					"Cannot update additional expense to this vertical until a Margin Report is uploaded for the same vertical.");
		}

		if (ex.getApprovalStatus() == AdditionalExpenseStatus.CFO_APPROVED) {
			throw new IllegalStateException("Cannot edit after CFO approval");
		}

		if (amount == null || amount.signum() <= 0)
			throw new IllegalArgumentException("Amount must be positive");
		if (comments == null || comments.isBlank())
			throw new IllegalArgumentException("Comments are required");

		// capture old values first (for correct needsReset logic)
		Long oldLabelId = ex.getLabel() != null ? ex.getLabel().getId() : null;
		BigDecimal oldAmount = ex.getAmount();
		CurrencyCode oldCurrency = ex.getCurrency();
		BigDecimal oldRate = ex.getConversionRate();
		Long oldVerticalId = ex.getVertical() != null ? ex.getVertical().getId() : null;

		boolean needsReset = (oldLabelId != null && !oldLabelId.equals(labelId))
				|| (oldAmount != null && oldAmount.compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0)
				|| (oldCurrency != currency) || (oldVerticalId != null && !oldVerticalId.equals(verticalId))
				|| (currency != CurrencyCode.USD && rate != null && oldRate != null
						&& oldRate.compareTo(rate.setScale(6, RoundingMode.HALF_UP)) != 0);

		ex.setLabel(labels.findById(labelId).orElseThrow());
		ex.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
		ex.setCurrency(currency);

		if (currency == CurrencyCode.USD) {
			BigDecimal fx = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
			ex.setConversionRate(fx);
			ex.setAmountUsd(amount.setScale(2, RoundingMode.HALF_UP));
		} else {
			if (rate == null || rate.signum() <= 0) {
				throw new IllegalArgumentException(
						"Conversion rate is required and must be positive for non-USD expenses");
			}

			BigDecimal fx = rate.setScale(6, RoundingMode.HALF_UP);
			ex.setConversionRate(fx);

			BigDecimal usd = amount.divide(fx, 6, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
			ex.setAmountUsd(usd);
		}

		ex.setVertical(verticals.findById(verticalId).orElseThrow());
		ex.setComments(comments.trim());

		if (needsReset) {
			log.debug("update expense causes approval reset: expId={}", expId);
			ex.setApprovalStatus(AdditionalExpenseStatus.WAITING);
			ex.setCeoApprovedBy(null);
			ex.setCeoApprovedOn(null);
			ex.setCfoApprovedBy(null);
			ex.setCfoApprovedOn(null);
			ex.setRejectedBy(null);
			ex.setRejectedOn(null);
			ex.setRejectionReason(null);
		}

		if (file != null && !file.isEmpty()) {
			String orig = file.getOriginalFilename();
			String safe = sanitize(orig);
			String serverName = (safe != null) ? (orderId + "_" + Instant.now().toEpochMilli() + "_" + safe) : null;

			var dest = root.resolve(serverName).normalize();
			if (!dest.startsWith(root))
				throw new SecurityException("Invalid file path");

			try {
				Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException("Failed to store file", e);
			}

			ex.setFileName(orig);
			ex.setStorageKey(serverName);
		}

		repo.saveAndFlush(ex);

		audit(expId, actor != null ? actor.getId() : null, "UPDATED", "Expense updated");

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expId, ex.getOrder().getSalesOrderId(), "UPDATE",
				"Updated additional expense " + expId + " on order " + ex.getOrder().getSalesOrderId(), null, actor,
				actorIp);

		// NOTIFICATION: only if reset happened (meaning others must re-approve)
		if (needsReset) {
			notificationService.notifyRole(AppRole.CEO, NotificationType.EXPENSE_UPDATED,
					"Expense updated (re-approval needed)",
					"Order " + ex.getOrder().getSalesOrderId() + " expense updated and needs re-approval",
					"ADDITIONAL_EXPENSE", expId, "/orders/" + orderId + "?tab=margin");
		}
	}

	/* ========================= DELETE ========================= */

	@Transactional
	public void delete(Long orderId, Long expId, AppUser actor, boolean deleteFile, String actorIp,
			HttpServletRequest request) throws IOException {

		log.warn("delete expense: orderId={}, expId={}, actor={}, deleteFile={}", orderId, expId,
				actor != null ? actor.getUsername() : "SYSTEM", deleteFile);

		var ex = repo.findById(expId).orElseThrow();
		if (!ex.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("Not in this order");

		if (ex.getApprovalStatus() == AdditionalExpenseStatus.CFO_APPROVED && !request.isUserInRole("ROLE_ADMIN")) {
			throw new IllegalStateException("Only Admin can delete CFO-approved expenses.");
		}

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expId, ex.getOrder().getSalesOrderId(), "DELETE",
				"Deleted additional expense " + expId + " (" + ex.getAmount() + " " + ex.getCurrency() + ") on order "
						+ ex.getOrder().getSalesOrderId(),
				null, actor, actorIp);

		audit(expId, actor != null ? actor.getId() : null, "DELETED", "Expense deleted");

		String storage = ex.getStorageKey();
		repo.delete(ex);

		// FIX: delete actual stored file under root, not Paths.get(storage)
		if (deleteFile && storage != null) {
			try {
				Path filePath = root.resolve(storage).normalize();
				if (!filePath.startsWith(root)) {
					log.warn("Blocked delete due to invalid path: {}", filePath);
				} else {
					Files.deleteIfExists(filePath);
				}
			} catch (Exception e) {
				log.warn("Failed to delete file {}: {}", storage, e.toString());
			}
		}
	}

	/* ========================= INTERNAL HELPERS ========================= */

	private AdditionalExpense mustLoad(Long orderId, Long expenseId) {
		var e = repo.findById(expenseId).orElseThrow();
		if (!e.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("Expense not in this Order");
		return e;
	}

	private void audit(Long expId, Long actorUserId, String action, String note) {
		var a = new AdditionalExpenseAudit();
		a.setExpense(em.getReference(AdditionalExpense.class, expId));
		a.setActor(actorUserId != null ? em.getReference(AppUser.class, actorUserId) : null);
		a.setAction(action);
		a.setNote(note);
		a.setActedOn(Instant.now());
		auditRepo.save(a);
	}

	private static String sanitize(String name) {
		if (name == null || name.isBlank())
			return null;
		return name.replaceAll("[^A-Za-z0-9._\\-]", "_");
	}

	private static void assertOrder(AdditionalExpense ex, Long orderId) {
		if (!ex.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("Expense not in this order");
	}
}
