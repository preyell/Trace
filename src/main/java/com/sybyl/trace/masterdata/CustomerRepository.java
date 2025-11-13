package com.sybyl.trace.masterdata;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Customer> findByNameIgnoreCase(String name);
    @Query("""
    	       select c from Customer c
    	       where (:q is null or :q = '' or
    	             lower(c.name)        like lower(concat('%', :q, '%')) or
    	             lower(c.contactName) like lower(concat('%', :q, '%')) or
    	             lower(c.email)       like lower(concat('%', :q, '%')) or
    	             lower(c.phone)       like lower(concat('%', :q, '%')))
    	    """)
    	    Page<Customer> search(@Param("q") String q, Pageable pageable);
}
