package com.sybyl.trace.masterdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdditionalExpenseLabelService {

  private final AdditionalExpenseLabelRepository repo;
//  private final com.sybyl.trace.order.MarginReportCommitmentRepository commitments;

  @Transactional(readOnly = true)
  public List<AdditionalExpenseLabel> listActive() {
    return repo.findAllByActiveTrueOrderByNameAsc();
  }

  @Transactional
  public AdditionalExpenseLabel create(String name, String description, boolean system) {
    if (repo.existsByNameIgnoreCase(name)) {
      throw new IllegalArgumentException("Label already exists: " + name);
    }
    var l = new AdditionalExpenseLabel();
    l.setName(name.trim());
    l.setDescription(description);
    l.setSystem(system);
    l.setActive(true);
    l.setCreatedAt(Instant.now());
    return repo.save(l);
  }

  @Transactional
  public void deactivate(Long id) {
    var l = repo.findById(id).orElseThrow();
    if (l.isSystem()) throw new IllegalStateException("This label cannot be deactivated.");
    l.setActive(false);
  }

  @Transactional
  public void reactivate(Long id) {
    var l = repo.findById(id).orElseThrow();
    l.setActive(true);
  }


  @Transactional
  public void delete(Long id) {
    var l = repo.findById(id).orElseThrow();
    if (l.isSystem()) {
      throw new IllegalStateException("System label cannot be deleted");
    }
	/*
	 * if (commitments.countByLabelId(id) > 0) { throw new
	 * IllegalStateException("Label is in use by commitments"); }
	 */
    repo.deleteById(id);
  }

  /** idempotent: ensure default system label exists */
  @Transactional
  public void ensureDefault() {
    repo.findByNameIgnoreCase("Design And Implementation Services")
        .orElseGet(() -> create("Design And Implementation Services",
          "Default system label; cannot be deleted", true));
  }
}
