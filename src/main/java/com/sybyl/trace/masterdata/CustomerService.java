package com.sybyl.trace.masterdata;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository repo;

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        log.debug("Fetching all customers");
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        log.debug("Fetching customer by id={}", id);
        return repo.findById(id).orElseThrow();
    }

    public Customer create(Customer c) {
        log.info("Creating customer: name={}", c.getName());

        if (repo.existsByNameIgnoreCase(c.getName())) {
            log.warn("Attempt to create duplicate customer: name={}", c.getName());
            throw new IllegalArgumentException("Customer name already exists");
        }

        Customer saved = repo.save(c);
        log.debug("Customer saved: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Customer update(Long id, Customer upd) {
        log.info("Updating customer: id={}", id);

        Customer c = get(id);

        // Enforce unique name if changed
        if (!c.getName().equalsIgnoreCase(upd.getName())
                && repo.existsByNameIgnoreCase(upd.getName())) {
            log.warn("Attempt to rename customer to existing name: oldName={}, newName={}",
                    c.getName(), upd.getName());
            throw new IllegalArgumentException("Customer name already exists");
        }

        c.setName(upd.getName());
        c.setContactName(upd.getContactName());
        c.setEmail(upd.getEmail());
        c.setPhone(upd.getPhone());
        c.setActive(upd.isActive());

        log.debug("Customer updated (dirty check pending flush): id={}, name={}",
                c.getId(), c.getName());
        return c;
    }

    @Transactional
    public void delete(Long id) {
      try {
        repo.deleteById(id);
        repo.flush(); // force FK error here
      } catch (DataIntegrityViolationException ex) {
        throw new IllegalStateException("Cannot delete this customer because it is already used in Orders.");
      }
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(String q, int page, int size) {
        int pageIndex = Math.max(page, 0);
        int pageSize = Math.max(size, 1);

        log.debug("Customer search: q='{}', page={}, size={}", q, pageIndex, pageSize);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("name").ascending());
        if (q == null || q.isBlank()) {
            return repo.findAll(pageable);
        }
        return repo.search(q.trim(), pageable);
    }
}
