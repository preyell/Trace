// com.sybyl.trace.order.AdditionalExpenseService.java
package com.sybyl.trace.order;

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

import com.sybyl.trace.masterdata.AdditionalExpenseLabelRepository;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.user.AppUser;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
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

	@Value("${app.upload.additional-expenses.path}")
	private String uploadPath;
	private Path root;

	@PostConstruct
	void init() throws IOException {
		root = Paths.get(uploadPath).toAbsolutePath().normalize();
		Files.createDirectories(root);
	}

	@Transactional(readOnly = true)
	public List<AdditionalExpense> listForOrder(Long orderId) {
		return repo.findByOrderIdOrderByUploadedOnDesc(orderId);
	}

	@Transactional
	public AdditionalExpense create(Long orderId, AppUser user, Long labelId, BigDecimal amount, CurrencyCode currency,
			BigDecimal conversionRate, Long verticalId, String comments, MultipartFile file) throws IOException {
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
		var fx = (conversionRate == null ? BigDecimal.ONE : conversionRate).setScale(6, RoundingMode.HALF_UP);
		exp.setConversionRate(fx);
		exp.setAmountUsd(amount.multiply(fx).setScale(2, RoundingMode.HALF_UP));
		exp.setComments(comments.trim());
		exp.setFileName(orig);
		exp.setStorageKey(serverName);
		exp.setApprovalStatus(AdditionalExpenseStatus.WAITING);

		var saved = repo.save(exp);
		audit(saved.getId(), user.getId(), "CREATED", "Additional expense created");
		return saved;
	}

	@Transactional
	public void ceoApprove(Long orderId, Long expenseId, AppUser actor, String note) {
		var e = mustLoad(orderId, expenseId);
		if (e.getApprovalStatus() != AdditionalExpenseStatus.WAITING)
			throw new IllegalStateException("Not awaiting CEO approval");
		e.setApprovalStatus(AdditionalExpenseStatus.CEO_APPROVED);
		e.setCeoApprovedBy(actor);
		e.setCeoApprovedOn(Instant.now());
		audit(e.getId(), actor.getId(), "CEO_APPROVED", note);
	}

	@Transactional
	public void cfoApprove(Long orderId, Long expenseId, AppUser actor, String note) {
		var e = mustLoad(orderId, expenseId);
		if (e.getApprovalStatus() != AdditionalExpenseStatus.CEO_APPROVED)
			throw new IllegalStateException("Not awaiting CFO approval");
		e.setApprovalStatus(AdditionalExpenseStatus.CFO_APPROVED);
		e.setCfoApprovedBy(actor);
		e.setCfoApprovedOn(Instant.now());
		audit(e.getId(), actor.getId(), "CFO_APPROVED", note);
	}

	@Transactional
	public void reject(Long orderId, Long expenseId, AppUser actor, String reason) {
		var e = mustLoad(orderId, expenseId);
		e.setApprovalStatus(AdditionalExpenseStatus.REJECTED);
		e.setRejectedBy(actor);
		e.setRejectedOn(Instant.now());
		e.setRejectionReason(reason);
		audit(e.getId(), actor.getId(), "REJECTED", reason);
	}

	@Transactional
	public AdditionalExpenseDisbursement disburse(Long orderId, Long expenseId, BigDecimal amount,
			CurrencyCode currency, String note, AppUser actor) {
		var exp = repo.findById(expenseId).orElseThrow(() -> new IllegalArgumentException("Expense not found"));
		if (!exp.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("Wrong order");

		if (exp.getApprovalStatus() != AdditionalExpenseStatus.CFO_APPROVED) {
			throw new IllegalStateException("Disbursement not allowed until CFO approval.");
		}

		// Enforce currency rule (simple): disbursement must match expense currency
		if (currency != exp.getCurrency()) {
			throw new IllegalArgumentException(
					"Disbursement currency must match the approved expense currency (" + exp.getCurrency() + ").");
		}

		// Convert to USD using the expense conversionRate used at approval time
		BigDecimal usd = amount.multiply(exp.getConversionRate()).setScale(2, RoundingMode.HALF_UP);

		BigDecimal already = disbRepo.sumUsdByExpense(expenseId);
		BigDecimal cap = exp.getAmountUsd();
		if (already.add(usd).compareTo(cap) > 0) {
			throw new IllegalStateException("Total disbursed would exceed approved amount. Remaining USD "
					+ cap.subtract(already).toPlainString());
		}

		var d = new AdditionalExpenseDisbursement();
		d.setExpense(em.getReference(AdditionalExpense.class, expenseId));
		d.setActor(actor != null ? em.getReference(AppUser.class, actor.getId()) : null);
		d.setAmount(amount);
		d.setCurrency(currency);
		d.setAmountUsd(usd);
		d.setDisbursedOn(Instant.now());
		d.setNote(note);

		var saved = disbRepo.save(d);

		// audit trail
		audit(expenseId, actor != null ? actor.getId() : null, "DISBURSED",
				"Amount " + amount + " " + currency + " (~" + usd + " USD)");

		return saved;
	}

	@Transactional(readOnly = true)
	public List<AdditionalExpenseDisbursement> listDisbursements(Long expenseId) {
		return disbRepo.findByExpenseIdOrderByDisbursedOnDesc(expenseId);
	}

	@Transactional
	public void deleteDisbursement(Long orderId, Long expenseId, Long disbId, AppUser actor) {
		// optional business rule: block delete if expense is fully approved/closed
		disbRepo.deleteByIdAndExpenseId(disbId, expenseId);
		audit(expenseId, actor != null ? actor.getId() : null, "DISBURSEMENT_DELETED", "ID " + disbId);
	}

	@Transactional(readOnly = true)
	public List<AdditionalExpenseAudit> audits(Long expenseId) {
		return auditRepo.findByExpenseIdOrderByActedOnDesc(expenseId);
	}

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

	@Transactional
	public void update(Long orderId, Long expId, AppUser actor, Long labelId, BigDecimal amount, CurrencyCode currency,
			BigDecimal rate, Long verticalId, String comments) {

		var ex = repo.findById(expId).orElseThrow();
		assertOrder(ex, orderId);

		// Block updates once CFO approved
		if (ex.getApprovalStatus() == AdditionalExpenseStatus.CFO_APPROVED) {
			throw new IllegalStateException("Cannot edit after CFO approval");
		}

		ex.setLabel(labels.findById(labelId).orElseThrow());
		ex.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
		ex.setCurrency(currency);
		ex.setConversionRate(rate.setScale(6, RoundingMode.HALF_UP));
		ex.setVertical(verticals.findById(verticalId).orElseThrow());
		ex.setComments(comments);

		// recompute USD
		ex.setAmountUsd(ex.getAmount().multiply(ex.getConversionRate()).setScale(2, RoundingMode.HALF_UP));

		repo.saveAndFlush(ex);
		audit(expId, actor.getId(), "UPDATED", "Expense updated");
	}

	@Transactional
	public void delete(Long orderId, Long expId, AppUser actor, boolean deleteFile) throws IOException {
		var ex = repo.findById(expId).orElseThrow();
		if (!ex.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("Not in this order");
		if (ex.getApprovalStatus() == AdditionalExpenseStatus.CFO_APPROVED) {
			throw new IllegalStateException("Cannot delete after CFO approval");
		}

		audit(expId, actor != null ? actor.getId() : null, "DELETED", "Expense deleted");

		String storage = ex.getStorageKey();

		repo.delete(ex);
		// repo.flush(); // optional; ok to keep

		if (deleteFile && storage != null) {
			try {
				Files.deleteIfExists(Paths.get(storage));
			} catch (Exception e) {
				log.warn("Failed to delete file {}: {}", storage, e.toString());
			}
		}
	}

	private static void assertOrder(AdditionalExpense ex, Long orderId) {
		if (!ex.getOrder().getId().equals(orderId))
			throw new IllegalArgumentException("Expense not in this order");
	}

}
