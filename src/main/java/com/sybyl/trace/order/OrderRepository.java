// com.sybyl.trace.order.OrderRepository.java
package com.sybyl.trace.order;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sybyl.trace.location.Location;

public interface OrderRepository extends JpaRepository<Order, Long> {

	boolean existsBySalesOrderIdIgnoreCase(String salesOrderId);

	Optional<Order> findBySalesOrderIdIgnoreCase(String salesOrderId);

	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals" })
	Page<Order> findByLocation(Location location, Pageable pageable);

	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals" })
	Page<Order> findByLocationIn(Set<Location> locations, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals" })
	Page<Order> findAll(Pageable pageable);

	/** Narrower fetch plan for edit form (if you want a lighter query). */
	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals" })
	@Query("select distinct o from Order o where o.id = :id")
	Optional<Order> findForEdit(@Param("id") Long id);

	// ---- Search within one location (now includes salesOrderId) ----
	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals" })
	@Query(value = """
			select distinct o from Order o
			  left join o.customer c
			  left join o.salesManager sm
			  left join o.createdBy cb
			  left join o.verticals v
			where o.location = :loc and (
			       lower(coalesce(o.salesOrderId,''))   like lower(concat('%', :q, '%'))
			    or lower(coalesce(c.name,''))           like lower(concat('%', :q, '%'))
			    or lower(coalesce(o.description,''))    like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(v.name,''))           like lower(concat('%', :q, '%'))
			)
			""", countQuery = """
			select count(distinct o) from Order o
			  left join o.customer c
			  left join o.salesManager sm
			  left join o.createdBy cb
			  left join o.verticals v
			where o.location = :loc and (
			       lower(coalesce(o.salesOrderId,''))   like lower(concat('%', :q, '%'))
			    or lower(coalesce(c.name,''))           like lower(concat('%', :q, '%'))
			    or lower(coalesce(o.description,''))    like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(v.name,''))           like lower(concat('%', :q, '%'))
			)
			""")
	Page<Order> searchInLocation(@Param("loc") Location loc, @Param("q") String q, Pageable pageable);

	// ---- Search across multiple locations (also includes salesOrderId) ----
	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals" })
	@Query(value = """
			select distinct o from Order o
			  left join o.customer c
			  left join o.salesManager sm
			  left join o.createdBy cb
			  left join o.verticals v
			where o.location in :locs and (
			       lower(coalesce(o.salesOrderId,''))   like lower(concat('%', :q, '%'))
			    or lower(coalesce(c.name,''))           like lower(concat('%', :q, '%'))
			    or lower(coalesce(o.description,''))    like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(v.name,''))           like lower(concat('%', :q, '%'))
			)
			""", countQuery = """
			select count(distinct o) from Order o
			  left join o.customer c
			  left join o.salesManager sm
			  left join o.createdBy cb
			  left join o.verticals v
			where o.location in :locs and (
			       lower(coalesce(o.salesOrderId,''))   like lower(concat('%', :q, '%'))
			    or lower(coalesce(c.name,''))           like lower(concat('%', :q, '%'))
			    or lower(coalesce(o.description,''))    like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(sm.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.firstName,''))     like lower(concat('%', :q, '%'))
			    or lower(coalesce(cb.lastName,''))      like lower(concat('%', :q, '%'))
			    or lower(coalesce(v.name,''))           like lower(concat('%', :q, '%'))
			)
			""")
	Page<Order> searchInLocations(@Param("locs") Set<Location> locs, @Param("q") String q, Pageable pageable);

	// OrderRepository.java
	@EntityGraph(attributePaths = { "customer", "salesManager", "createdBy", "verticals", "marginReports",
			"marginReports.vertical", "marginReports.uploadedBy" })
	@Query("select distinct o from Order o where o.id = :id")
	Optional<Order> findForDetails(@Param("id") Long id);

}
