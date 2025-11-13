package com.sybyl.trace.masterdata;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerticalService {
  private final VerticalRepository repo;

  public List<Vertical> findAll() { return repo.findAll(); }
  public Vertical findById(Long id) { return repo.findById(id).orElseThrow(); }

  @Transactional
  public Vertical create(@Valid Vertical v) {
    if (repo.existsByNameIgnoreCase(v.getName())) {
      throw new IllegalArgumentException("Vertical with this name already exists");
    }
    return repo.save(v);
  }

  @Transactional
  public Vertical update(Long id, @Valid Vertical form) {
    Vertical v = findById(id);
    if (!v.getName().equalsIgnoreCase(form.getName())
        && repo.existsByNameIgnoreCase(form.getName())) {
      throw new IllegalArgumentException("Vertical with this name already exists");
    }
    v.setName(form.getName());
    v.setDescription(form.getDescription());
    v.setActive(form.isActive());
    return repo.save(v);
  }
  
  public Page<Vertical> search(String q, int page, int size){
      Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("name").ascending());
      return (q == null || q.isBlank()) ? repo.findAll(pageable) : repo.search(q.trim(), pageable);
  }

  @Transactional
  public void delete(Long id) { repo.deleteById(id); }
}
