// com.sybyl.trace.order.MarginReportService
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
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

  /* ========= Create / Update ========= */

  @Transactional
  public MarginReport save(Long orderId,
                           AppUser currentUser,
                           MultipartFile file,
                           String labelVerticalNameIfAny, // kept to match your signature; not used here
                           CurrencyCode buyingCur,
                           CurrencyCode sellingCur,
                           BigDecimal buyingPrice,
                           BigDecimal sellingPrice,
                           BigDecimal conversionRate,
                           Long verticalId,
                           String comments) throws IOException {

    log.info("Saving margin report: orderId={}, userId={}, verticalId={}, originalFile='{}'",
        orderId,
        (currentUser != null ? currentUser.getId() : null),
        verticalId,
        (file != null ? file.getOriginalFilename() : null));

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
    mr.setApprovalStatus(ApprovalStatus.FINANCE_PENDING);
    mr.setRejectionReason(null);

    mr.setComments(comments);

    var saved = repo.saveAndFlush(mr);
    audit(saved.getId(), currentUser.getId(), "CREATED", "Created Margin Report");
    log.info("Margin report saved: id={}, orderId={}, storageKey='{}'", saved.getId(), orderId, saved.getStorageKey());
    return saved;
  }

  @Transactional
  public MarginReport update(Long orderId,
                             Long mrId,
                             AppUser user,
                             BigDecimal buyingPrice,
                             CurrencyCode buyingCur,
                             BigDecimal sellingPrice,
                             CurrencyCode sellingCur,
                             BigDecimal conversionRate,
                             Long verticalId,
                             String comments) {

    var mr = repo.findById(mrId).orElseThrow();
    if (!mr.getOrder().getId().equals(orderId))
      throw new IllegalArgumentException("MR not in this Order");

    mr.setBuyingPrice(buyingPrice.setScale(2, RoundingMode.HALF_UP));
    mr.setBuyingCurrency(buyingCur);
    mr.setSellingPrice(sellingPrice.setScale(2, RoundingMode.HALF_UP));
    mr.setSellingCurrency(sellingCur);
    mr.setConversionRate(conversionRate.setScale(6, RoundingMode.HALF_UP));
    if (verticalId != null) {
      mr.setVertical(verticals.findById(verticalId).orElseThrow());
    }
    mr.setComments(comments);

    // Optional policy: editing resets approvals back to FINANCE_PENDING (unless already fully approved)
    if (mr.getApprovalStatus() != ApprovalStatus.FINANCE_PENDING) {
      mr.setApprovalStatus(ApprovalStatus.FINANCE_PENDING);
      // clear prior approvals if any
      mr.setFinanceApprovedBy(null);
      mr.setFinanceApprovedOn(null);
      mr.setCeoApprovedBy(null);
      mr.setCeoApprovedOn(null);
      mr.setRejectionReason(null);
    }

    var saved = repo.saveAndFlush(mr);
    audit(saved.getId(), user.getId(), "UPDATED", null);
    return saved;
  }

  /* ========= Submit / Approvals / Rejection ========= */

  @Transactional
  public void submitForApproval(Long orderId, Long mrId, AppUser currentUser) {
    log.info("Submit for approval: orderId={}, mrId={}, userId={}", orderId, mrId,
        (currentUser != null ? currentUser.getId() : null));

    MarginReport mr = repo.findById(mrId).orElseThrow(() -> {
      log.warn("MarginReport not found: {}", mrId);
      return new IllegalArgumentException("MarginReport not found: " + mrId);
    });
    if (!mr.getOrder().getId().equals(orderId)) {
      log.warn("MarginReport {} does not belong to order {}", mrId, orderId);
      throw new IllegalArgumentException("MarginReport does not belong to order " + orderId);
    }

    // Ensure it goes to the first stage
    mr.setApprovalStatus(ApprovalStatus.FINANCE_PENDING);
    mr.setRejectionReason(null);
    audit(mr.getId(), currentUser.getId(), "SUBMITTED", "Submitted for approval");
  }

  @Transactional
  public void approveFinance(Long orderId, Long mrId, AppUser actor, String note) {
    var mr = repo.findById(mrId).orElseThrow();
    ensureOrder(mr, orderId);
    if (mr.getApprovalStatus() != ApprovalStatus.FINANCE_PENDING) {
      throw new IllegalStateException("Margin Report is not in FINANCE_PENDING");
    }
    assertFinanceApprover(actor);

    mr.setApprovalStatus(ApprovalStatus.CEO_PENDING);
    mr.setFinanceApprovedBy(actor);
    mr.setFinanceApprovedOn(Instant.now());
    mr.setRejectionReason(null);

    repo.saveAndFlush(mr);
    audit(mr.getId(), actor.getId(), "FINANCE_APPROVED", note);
  }

  @Transactional
  public void approveCeo(Long orderId, Long mrId, AppUser actor, String note) {
    var mr = repo.findById(mrId).orElseThrow();
    ensureOrder(mr, orderId);
    if (mr.getApprovalStatus() != ApprovalStatus.CEO_PENDING) {
      throw new IllegalStateException("Margin Report is not in CEO_PENDING");
    }
    assertCeo(actor);

    mr.setApprovalStatus(ApprovalStatus.APPROVED);
    mr.setCeoApprovedBy(actor);
    mr.setCeoApprovedOn(Instant.now());
    mr.setRejectionReason(null);

    repo.saveAndFlush(mr);
    audit(mr.getId(), actor.getId(), "CEO_APPROVED", note);
  }

  @Transactional
  public void reject(Long orderId, Long mrId, AppUser actor, String reason) {
    var mr = repo.findById(mrId).orElseThrow();
    ensureOrder(mr, orderId);

    // who may reject depends on stage
    if (mr.getApprovalStatus() == ApprovalStatus.FINANCE_PENDING) {
      assertFinanceApprover(actor);
    } else if (mr.getApprovalStatus() == ApprovalStatus.CEO_PENDING) {
      assertCeo(actor);
    } else {
      throw new IllegalStateException("Cannot reject in state " + mr.getApprovalStatus());
    }

    mr.setApprovalStatus(ApprovalStatus.REJECTED);
    mr.setRejectionReason(reason);

    repo.saveAndFlush(mr);
    audit(mr.getId(), actor.getId(), "REJECTED", reason);
  }

  /* ========= Delete ========= */

  @Transactional
  public void delete(Long orderId, Long mrId, AppUser user, boolean deleteFile) throws IOException {
    var mr = repo.findById(mrId).orElseThrow();
    ensureOrder(mr, orderId);

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
  }

  /* ========= Helpers ========= */

  private void ensureOrder(MarginReport mr, Long orderId) {
    if (!mr.getOrder().getId().equals(orderId))
      throw new IllegalArgumentException("MR not in this Order");
  }

  private void assertFinanceApprover(AppUser actor) {
	  if (actor == null || !(actor.isAdmin() || actor.hasRole(AppRole.FINANCE_APPROVER))) {
	    throw new org.springframework.security.access.AccessDeniedException("Only Finance Approver or Super Admin can perform this action");
	  }
	}

  private void assertCeo(AppUser actor) {
	  if (actor == null || !(actor.isAdmin() || actor.hasRole(AppRole.CEO))) {
	    throw new org.springframework.security.access.AccessDeniedException("Only CEO or Admin can perform this action");
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
