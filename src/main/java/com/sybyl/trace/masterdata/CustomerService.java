package com.sybyl.trace.masterdata;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CustomerService {
	private final CustomerRepository repo;

	public CustomerService(CustomerRepository repo) {
		this.repo = repo;
	}

	public List<Customer> findAll() {
		return repo.findAll();
	}

	public Customer get(Long id) {
		return repo.findById(id).orElseThrow();
	}

	public Customer create(Customer c) {
		if (repo.existsByNameIgnoreCase(c.getName())) {
			throw new IllegalArgumentException("Customer name already exists");
		}
		return repo.save(c);
	}

	public Customer update(Long id, Customer upd) {
		Customer c = get(id);

		// Enforce unique name if changed
		if (!c.getName().equalsIgnoreCase(upd.getName()) && repo.existsByNameIgnoreCase(upd.getName())) {
			throw new IllegalArgumentException("Customer name already exists");
		}

		c.setName(upd.getName());
		c.setContactName(upd.getContactName());
		c.setEmail(upd.getEmail());
		c.setPhone(upd.getPhone());
		c.setActive(upd.isActive());
		return c; // dirty-checked
	}

	public void delete(Long id) {
		repo.deleteById(id);
	}
	
	public Page<Customer> search(String q, int page, int size){
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("name").ascending());
        if (q == null || q.isBlank()) return repo.findAll(pageable);
        return repo.search(q.trim(), pageable);
    }
}
