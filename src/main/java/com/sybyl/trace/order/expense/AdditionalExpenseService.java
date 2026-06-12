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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	public List<AdditionalExpense> findByOrderWithExpenses(Long orderId) {
		log.debug("findByOrderWithExpenses: orderId={}", orderId);
		return repo.findByOrderId(orderId).stream()
		        .filter(e -> e.getLabel() != null && 
		                     "Design And Implementation Services".equals(e.getLabel().getName()))
		        .collect(java.util.stream.Collectors.toList());
	}

	/* ========================= CREATE ========================= */

	@Transactional
	public AdditionalExpense create(Long orderId, AppUser user, Long labelId, BigDecimal amount, CurrencyCode currency,
			BigDecimal conversionRate, Long verticalId, String comments, MultipartFile file)
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

		audit(saved.getId(), user != null ? user.getId() : null, "CREATED", "Additional expense created", comments);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", saved.getId(), order.getSalesOrderId(), "CREATE",
				"Additional expense: " + saved.getLabel().getName() + " (" + saved.getAmount() + " " + saved.getCurrency()
						+ ") with comments " + comments,
				null, user);

		notificationService.notifyRole(AppRole.CEO, NotificationType.EXPENSE_CREATED,
				"Additional expense submitted",
				"Order " + order.getSalesOrderId() + " has a new additional expense to review", "ADDITIONAL_EXPENSE",
				saved.getId(), "/orders/" + orderId + "?tab=margin");

		return saved;
	}

	/* ========================= APPROVALS ========================= */

	@Transactional
	public void ceoApprove(Long orderId, Long expenseId, AppUser actor, String note, String comments) {
		log.info("ceoApprove: orderId={}, expenseId={}, actor={}", orderId, expenseId,
				actor != null ? actor.getUsername() : "SYSTEM");

		var e = mustLoad(orderId, expenseId);
		if (e.getApprovalStatus() != AdditionalExpenseStatus.WAITING)
			throw new IllegalStateException("Not awaiting CEO approval");

		e.setApprovalStatus(AdditionalExpenseStatus.CEO_APPROVED);
		e.setCeoApprovedBy(actor);
		e.setCeoApprovedOn(Instant.now());

		audit(e.getId(), actor != null ? actor.getId() : null, "CEO_APPROVED", note, comments);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, e.getOrder().getSalesOrderId(), "CEO_APPROVE",
				comments,
				null, actor);

		// NOTIFICATION: move to CFO
		notificationService.notifyRole(AppRole.CFO, NotificationType.EXPENSE_APPROVED_CEO, "Expense awaiting CFO approval",
				"Order " + e.getOrder().getSalesOrderId() + " expense is approved by CEO and awaits CFO approval",
				"ADDITIONAL_EXPENSE", expenseId, "/orders/" + orderId + "?tab=margin");
	}

	@Transactional
	public void cfoApprove(Long orderId, Long expenseId, AppUser actor, String note, String comments) {
		log.info("cfoApprove: orderId={}, expenseId={}, actor={}", orderId, expenseId,
				actor != null ? actor.getUsername() : "SYSTEM");

		var e = mustLoad(orderId, expenseId);
		if (e.getApprovalStatus() != AdditionalExpenseStatus.CEO_APPROVED)
			throw new IllegalStateException("Not awaiting CFO approval");

		e.setApprovalStatus(AdditionalExpenseStatus.CFO_APPROVED);
		e.setCfoApprovedBy(actor);
		e.setCfoApprovedOn(Instant.now());

		audit(e.getId(), actor != null ? actor.getId() : null, "CFO_APPROVED", note, comments);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, e.getOrder().getSalesOrderId(), "CFO_APPROVE",
				comments,
				null, actor);


	}

	@Transactional
	public void reject(Long orderId, Long expenseId, AppUser actor, String comments) {
		log.warn("reject: orderId={}, expenseId={}, actor={}, reason={}", orderId, expenseId,
				actor != null ? actor.getUsername() : "SYSTEM", comments);

		var e = mustLoad(orderId, expenseId);

		e.setApprovalStatus(AdditionalExpenseStatus.REJECTED);
		e.setRejectedBy(actor);
		e.setRejectedOn(Instant.now());
		e.setRejectionReason(comments);

		audit(e.getId(), actor != null ? actor.getId() : null, "REJECTED", "Rejected additional expense of " + e.getAmount(), comments);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, e.getOrder().getSalesOrderId(), "REJECT",
				"Rejected additional expense of " + e.getAmount() + " on order " + e.getOrder().getSalesOrderId()
						+ ". Reason: " + comments,
				null, actor);

		// NOTIFICATION: tell uploader
		if (e.getUploadedBy() != null) {
			notificationService.notifyUser(e.getUploadedBy(), NotificationType.EXPENSE_REJECTED, "Expense rejected",
					comments, "ADDITIONAL_EXPENSE", expenseId, "/orders/" + orderId + "?tab=margin");
		}
	}

	/* ========================= DISBURSEMENT ========================= */

	@Transactional
	public AdditionalExpenseDisbursement disburse(Long orderId, Long expenseId, BigDecimal amount,
			CurrencyCode currency, BigDecimal conversionRate, AppUser actor, String comments, LocalDate consumedOn) {

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

		    // NEW: validate conversionRate won't overflow DB numeric(18,6)
		    assertFitsNumeric18_6(conversionRate, "Conversion rate");

		    fxForThisDisb = conversionRate.setScale(6, RoundingMode.HALF_UP);

		    // Your formula: USD = amount / rate
		    usd = amount.divide(fxForThisDisb, 6, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);

		    // NEW: If your amountUsd column is numeric(18,6) or similar, validate too.
		    // If your amountUsd is numeric(18,2) then change validation accordingly.
		    assertFitsNumeric18_6(usd, "Converted USD amount");
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
		d.setNote(comments);
		d.setConversionRate(fxForThisDisb);
		d.setDisbursedOn(consumedOn.atStartOfDay(ZoneId.of("Africa/Nairobi")).toInstant());
		var saved = disbRepo.save(d);

		audit(expenseId, actor != null ? actor.getId() : null, "CONSUMED",
				"Amount " + amount + " " + currency + " (~" + usd + " USD)", comments);

		appAuditService.logEvent("ADDITIONAL_EXPENSE", expenseId, exp.getOrder().getSalesOrderId(), "CONSUMED",
				"Consumed " + amount + " " + currency + " (~" + usd + " USD) for additional expense " + exp.getLabel().getName()
						,
				null, actor);

		return saved;
	}

	private static final BigDecimal MAX_NUMERIC_18_6 = new BigDecimal("999999999999.999999"); // 12 digits + 6 decimals

	private static void assertFitsNumeric18_6(BigDecimal v, String fieldName) {
	    if (v == null) return;
	    // after rounding to scale 6, the absolute value must be < 10^12
	    BigDecimal scaled = v.setScale(6, RoundingMode.HALF_UP).abs();
	    if (scaled.compareTo(MAX_NUMERIC_18_6) > 0) {
	        throw new IllegalArgumentException(fieldName + " is too large. Please check the value.");
	    }
	}
	
	@Transactional
	public void deleteDisbursement(Long orderId, Long expenseId, Long disbId, AppUser actor) {
		log.warn("deleteDisbursement: orderId={}, expenseId={}, disbId={}, actor={}",
				orderId, expenseId, disbId, actor != null ? actor.getUsername() : "SYSTEM");

		AdditionalExpense ex = repo.findById(expenseId)
				.orElseThrow(() -> new IllegalArgumentException("Expense not found"));

		if (!ex.getOrder().getId().equals(orderId)) {
			throw new IllegalArgumentException("Expense does not belong to this order");
		}
		
		var disb = disbRepo.findById(disbId)
				.orElseThrow(() -> new IllegalArgumentException("Disbursement not found"));

		// 2. Extract the amounts (Adjust the getter methods to match your entity)
		var amount = disb.getAmount(); 
		var amountUsd = disb.getAmountUsd(); 

		// 3. Format the new audit note
		String auditNote = String.format("Deleted consumption of %s (%s USD)", amount, amountUsd);

		disbRepo.deleteByIdAndExpenseId(disbId, expenseId);

		audit(expenseId, actor != null ? actor.getId() : null, "CONSUMPTION_DELETED", auditNote, null);

		appAuditService.logEvent(
				"ADDITIONAL_EXPENSE",
				expenseId,
				ex.getOrder().getSalesOrderId(),
				"CONSUMPTION_DELETED",
				auditNote  + " for " +  ex.getLabel().getName()
						+ " on order " + ex.getOrder().getSalesOrderId(),
				null,
				actor
		);

	}

	
	@Transactional
	public void delete(Long orderId, Long expId, AppUser actor, boolean deleteFile, 
	        HttpServletRequest request) throws IOException {

	    log.warn("Executing definitive bulk-query deletion for additional expense ID: {}", expId);

	    // 1. Fetch the entity once to run security and validation rules
	    var ex = repo.findById(expId).orElseThrow(() -> new IllegalArgumentException("Expense not found"));
	    if (!ex.getOrder().getId().equals(orderId)) {
	        throw new IllegalArgumentException("Expense does not belong to this order.");
	    }

	    if (ex.getApprovalStatus() == AdditionalExpenseStatus.CFO_APPROVED && !request.isUserInRole("ROLE_ADMIN")) {
	        throw new IllegalStateException("Only System Administrators can delete CFO-approved expenses.");
	    }

	    String storage = ex.getStorageKey();
	    String salesOrderId = ex.getOrder().getSalesOrderId();
	    BigDecimal amount = ex.getAmount();
	    CurrencyCode currency = ex.getCurrency();

	    // 🎯 STEP A: Log the global action audit row first while it's safe
	    appAuditService.logEvent("ADDITIONAL_EXPENSE", expId, salesOrderId, "DELETE",
	            "Deleted additional expense: " + ex.getLabel().getName() + " (" + amount + " " + currency + ")",
	            null, actor);

	    // 🎯 STEP B: Insert the specialized tracking history log row
	    audit(expId, actor != null ? actor.getId() : null, "DELETED", "Expense permanently removed.", null);

	    // 🎯 STEP C: Force Hibernate to write the audit insert to PostgreSQL right now
	    repo.flush(); 

	    // 🎯 STEP D: THE DEFINITIVE FIX - Execute a direct Bulk Deletion Query
	    // This tells the database to drop row 'expId' directly.
	    // PostgreSQL immediately intercepts this and handles its own ON DELETE CASCADE,
	    // bypassing Hibernate's memory graph tracking completely!
	    repo.deleteExpenseByIdQuery(expId);

	    // 2. Clear static attachment files from your storage path safely
	    if (deleteFile && storage != null) {
	        try {
	            Path filePath = root.resolve(storage).normalize();
	            if (filePath.startsWith(root)) {
	                Files.deleteIfExists(filePath);
	            }
	        } catch (Exception e) {
	            log.warn("Failed to delete attachment file '{}': {}", storage, e.toString());
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

	private void audit(Long expId, Long actorUserId, String action, String note, String comments) {
		var a = new AdditionalExpenseAudit();
		a.setExpense(em.getReference(AdditionalExpense.class, expId));
		a.setActor(actorUserId != null ? em.getReference(AppUser.class, actorUserId) : null);
		a.setAction(action);
		a.setNote(note);
		a.setComments(comments);
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
	
	public Page<OrderAdditionalExpenseSummaryRow> getOrderExpenseSummary(String searchOrder, String searchCustomer, Pageable pageable) {
		log.debug("Fetching order expense summary report with filters - searchOrder: {}, searchCustomer: {}", searchOrder, searchCustomer);
		
		// Handle empty text inputs arriving from the frontend form gracefully by converting to null
		String orderParam = (searchOrder != null && !searchOrder.isBlank()) ? searchOrder.trim() : null;
		String customerParam = (searchCustomer != null && !searchCustomer.isBlank()) ? searchCustomer.trim() : null;

		return repo.findOrderExpenseSummary(orderParam, customerParam, pageable);
	}
	
	@Transactional(readOnly = true)
    public List<String> getDistinctSalesOrderIds() {
        return repo.findDistinctSalesOrderIds();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctCustomerNames() {
        return repo.findDistinctCustomerNames();
    }
}
