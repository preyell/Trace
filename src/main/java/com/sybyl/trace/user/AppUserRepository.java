// com.sybyl.trace.user.AppUserRepository.java
package com.sybyl.trace.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsernameIgnoreCase(String username);
  boolean existsByUsernameIgnoreCase(String username);
  boolean existsByEmailIgnoreCase(String email);
  @Query("""
	        select u from AppUser u
	        where (:q is null or :q = '' or
	               lower(u.username)  like lower(concat('%', :q, '%')) or
	               lower(u.firstName) like lower(concat('%', :q, '%')) or
	               lower(u.lastName)  like lower(concat('%', :q, '%')) or
	               lower(u.email)     like lower(concat('%', :q, '%')))
	        """)
	    Page<AppUser> search(@Param("q") String q, Pageable pageable);
  
  @Query("select u from AppUser u join u.roles r where r = com.sybyl.trace.user.AppRole.SALES_MANAGER and u.enabled = true")
  List<AppUser> findAllSalesManagers();
  Optional<AppUser> findByEmailIgnoreCase(String email);
  
  List<AppUser> findByRoles(AppRole role);

  
  @Query("""
	        SELECT (
	            (SELECT COUNT(o) FROM Order o WHERE o.salesManager.id = :userId OR o.createdBy.id = :userId) +
	            (SELECT COUNT(m) FROM MarginReport m WHERE m.uploadedBy.id = :userId OR m.approvedBy.id = :userId OR m.financeApprovedBy.id = :userId OR m.ceoApprovedBy.id = :userId) +
	            (SELECT COUNT(e) FROM AdditionalExpense e WHERE e.uploadedBy.id = :userId OR e.ceoApprovedBy.id = :userId OR e.cfoApprovedBy.id = :userId OR e.rejectedBy.id = :userId)
	        )
	    """)
	    long countAllSystemReferences(@Param("userId") Long userId);
}
