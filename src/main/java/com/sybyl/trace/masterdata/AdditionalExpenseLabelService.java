package com.sybyl.trace.masterdata;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalExpenseLabelService {

    private final AdditionalExpenseLabelRepository repo;
    // private final com.sybyl.trace.order.MarginReportCommitmentRepository commitments;

    @Transactional(readOnly = true)
    public List<AdditionalExpenseLabel> listActive() {
        log.debug("Fetching all active AdditionalExpenseLabels");
        return repo.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public AdditionalExpenseLabel create(String name, String description, boolean system) {
        log.info("Creating AdditionalExpenseLabel: name={}, system={}", name, system);

        if (repo.existsByNameIgnoreCase(name)) {
            log.warn("Attempt to create duplicate AdditionalExpenseLabel: name={}", name);
            throw new IllegalArgumentException("Label already exists: " + name);
        }

        var l = new AdditionalExpenseLabel();
        l.setName(name.trim());
        l.setDescription(description);
        l.setSystem(system);
        l.setActive(true);
        l.setCreatedAt(Instant.now());

        AdditionalExpenseLabel saved = repo.save(l);
        log.debug("AdditionalExpenseLabel saved: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public void deactivate(Long id) {
        log.info("Deactivating AdditionalExpenseLabel: id={}", id);

        var l = repo.findById(id).orElseThrow();
        if (l.isSystem()) {
            log.warn("Attempt to deactivate system AdditionalExpenseLabel: id={}", id);
            throw new IllegalStateException("This label cannot be deactivated.");
        }
        l.setActive(false);
        log.debug("AdditionalExpenseLabel deactivated: id={}, name={}", l.getId(), l.getName());
    }

    @Transactional
    public void reactivate(Long id) {
        log.info("Reactivating AdditionalExpenseLabel: id={}", id);

        var l = repo.findById(id).orElseThrow();
        l.setActive(true);
        log.debug("AdditionalExpenseLabel reactivated: id={}, name={}", l.getId(), l.getName());
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting AdditionalExpenseLabel: id={}", id);

        var l = repo.findById(id).orElseThrow();
        if (l.isSystem()) {
            log.warn("Attempt to delete system AdditionalExpenseLabel: id={}", id);
            throw new IllegalStateException("System label cannot be deleted");
        }

        try {
            repo.deleteById(id);
            repo.flush(); // force FK exception here
          } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Cannot delete this label because it is already used in Additional Expenses.");
          }
        log.debug("AdditionalExpenseLabel deleted: id={}, name={}", l.getId(), l.getName());
    }

    /** idempotent: ensure default system label exists */
    @Transactional
    public void ensureDefault() {
        log.debug("Ensuring default AdditionalExpenseLabel exists");

        repo.findByNameIgnoreCase("Design And Implementation Services")
            .orElseGet(() -> {
                log.info("Creating default system AdditionalExpenseLabel: 'Design And Implementation Services'");
                return create("Design And Implementation Services",
                        "Default system label; cannot be deleted", true);
            });
    }
}
