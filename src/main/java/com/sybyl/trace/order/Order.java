// com.sybyl.trace.order.Order.java
package com.sybyl.trace.order;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Customer;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.user.AppUser;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders", uniqueConstraints = {
		@UniqueConstraint(name = "uk_orders_sales_order_id", columnNames = { "sales_order_id" }) })
@Getter
@Setter
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // internal, not shown in UI

	/** Human-readable identifier shown in UI (e.g., SO-UG-2025-00123) */
	@Column(name = "sales_order_id", nullable = false, length = 50)
	private String salesOrderId;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Customer customer;

	@Column(length = 500)
	private String description;

	/** Sales manager responsible for the order */
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_manager_id")
	private AppUser salesManager;

	/** Many orders can map to multiple verticals */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "order_vertical", joinColumns = @JoinColumn(name = "order_id"), inverseJoinColumns = @JoinColumn(name = "vertical_id"))
	private Set<Vertical> verticals = new HashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Location location;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_id")
	private AppUser createdBy;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("uploadedOn desc")
	private java.util.Set<MarginReport> marginReports = new java.util.LinkedHashSet<>();

	public java.util.Set<MarginReport> getMarginReports() {
		return marginReports;
	}

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null)
			createdAt = Instant.now();
	}

	public java.util.Date getCreatedAtDate() {
		return java.util.Date.from(createdAt);
	}
}
