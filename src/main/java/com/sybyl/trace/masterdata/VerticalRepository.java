package com.sybyl.trace.masterdata;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerticalRepository extends JpaRepository<Vertical, Long> {
  Optional<Vertical> findByNameIgnoreCase(String name);
  boolean existsByNameIgnoreCase(String name);
  @Query("""
	       select v from Vertical v
	       where (:q is null or :q = '' or
	             lower(v.name) like lower(concat('%', :q, '%')) or
	             lower(v.description) like lower(concat('%', :q, '%')))
	    """)
	    Page<Vertical> search(@Param("q") String q, Pageable pageable);
}
