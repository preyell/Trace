package com.sybyl.trace.masterdata;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerticalService {

	private final VerticalRepository repo;

	@Transactional(readOnly = true)
	public List<Vertical> findAll() {
		log.debug("Fetching all verticals");
		return repo.findAll();
	}

	@Transactional(readOnly = true)
	public Vertical findById(Long id) {
		log.debug("Fetching vertical by id={}", id);
		return repo.findById(id).orElseThrow();
	}

	@Transactional
	public Vertical create(@Valid Vertical v) {
		log.info("Creating vertical: name={}", v.getName());

		if (repo.existsByNameIgnoreCase(v.getName())) {
			log.warn("Attempt to create duplicate vertical: name={}", v.getName());
			throw new IllegalArgumentException("Vertical with this name already exists");
		}
		Vertical saved = repo.save(v);
		log.debug("Vertical saved: id={}, name={}", saved.getId(), saved.getName());
		return saved;
	}

	@Transactional
	public Vertical update(Long id, @Valid Vertical form) {
		log.info("Updating vertical: id={}", id);

		Vertical v = findById(id);
		if (!v.getName().equalsIgnoreCase(form.getName()) && repo.existsByNameIgnoreCase(form.getName())) {
			log.warn("Attempt to rename vertical to existing name: oldName={}, newName={}", v.getName(),
					form.getName());
			throw new IllegalArgumentException("Vertical with this name already exists");
		}

		v.setName(form.getName());
		v.setDescription(form.getDescription());
		v.setActive(form.isActive());

		Vertical saved = repo.save(v);
		log.debug("Vertical updated: id={}, name={}", saved.getId(), saved.getName());
		return saved;
	}

	@Transactional(readOnly = true)
	public Page<Vertical> search(String q, int page, int size) {
		int pageIndex = Math.max(page, 0);
		int pageSize = Math.max(size, 1);

		log.debug("Vertical search: q='{}', page={}, size={}", q, pageIndex, pageSize);

		Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("name").ascending());
		return (q == null || q.isBlank()) ? repo.findAll(pageable) : repo.search(q.trim(), pageable);
	}

	@Transactional
	public void delete(Long id) {
		log.info("Deleting vertical: id={}", id);
		try {
			repo.deleteById(id);
			repo.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new IllegalStateException(
					"Cannot delete this Vertical because it is already used in Expenses/Margin Reports.");
		}
	}
}
