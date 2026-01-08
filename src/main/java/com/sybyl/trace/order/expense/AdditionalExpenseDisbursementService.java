package com.sybyl.trace.order.expense;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalExpenseDisbursementService {

    private final AdditionalExpenseDisbursementRepository repo;

    @Transactional(readOnly = true)
    public List<AdditionalExpenseDisbursement> listForExpense(Long expenseId) {
        log.debug("List disbursements requested: expenseId={}", expenseId);
        return repo.findByExpenseIdWithActor(expenseId);
    }
}
