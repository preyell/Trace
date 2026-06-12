// com.sybyl.trace.order.OrderService.java
package com.sybyl.trace.order;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.exception.BusinessException;
import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Customer;
import com.sybyl.trace.masterdata.CustomerRepository;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.order.events.OrderCreatedEvent;
import com.sybyl.trace.order.finance.OrderInvoiceRepository;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {

	private final OrderRepository orders;
	private final CustomerRepository customers;
	private final AppUserRepository users;
	private final VerticalRepository verticals;
	private final ApplicationEventPublisher events;
	private final OrderStatusService orderStatusService;
	private final AppAuditService auditService;
	private final OrderInvoiceRepository invRepo;
	
	public OrderService(OrderRepository orders, CustomerRepository customers, AppUserRepository users,
			VerticalRepository verticals, ApplicationEventPublisher events, OrderStatusService orderStatusService,
			AppAuditService auditService, OrderInvoiceRepository invRepo) {
		this.orders = orders;
		this.customers = customers;
		this.users = users;
		this.verticals = verticals;
		this.events = events;
		this.orderStatusService = orderStatusService;
		this.auditService = auditService;
		this.invRepo = invRepo;
	}

	public Page<Order> listForUser(AppUser currentUser, Location loc, String q, Pageable pageable) {
		log.info("OrderService.listForUser: userId={}, loc={}, q='{}', page={}",
				currentUser != null ? currentUser.getId() : null,
				loc, q,
				pageable != null ? pageable.getPageNumber() : null);

		var allowed = currentUser.getLocations();
		boolean hasSearch = (q != null && !q.isBlank());
		String qTrim = hasSearch ? q.trim() : null;

		Page<Order> page;

		// 1) Normal DB search first (for SO ID, customer, desc, createdAt, etc.)
		if (loc != null) {
			page = hasSearch ? orders.searchInLocation(loc, qTrim, pageable)
					: orders.findByLocation(loc, pageable);
		} else {
			page = hasSearch ? orders.searchInLocations(allowed, qTrim, pageable)
					: orders.findByLocationIn(allowed, pageable);
		}

		// 2) Compute transient status for UI
		page.forEach(o -> o.setStatusView(orderStatusService.computeOrderStatus(o.getId())));

		// 3) Fallback: if user searched and DB returned nothing, try status search in-memory
		//    (status is transient, DB cannot search it)
		if (hasSearch && page.getTotalElements() == 0) {
			log.debug("DB search returned 0; trying in-memory status fallback for q='{}'", qTrim);

			Page<Order> allPage = (loc != null)
					? orders.findByLocation(loc, Pageable.unpaged())
					: orders.findByLocationIn(allowed, Pageable.unpaged());

			var all = allPage.getContent();
			all.forEach(o -> o.setStatusView(orderStatusService.computeOrderStatus(o.getId())));

			String needle = normalizeForSearch(qTrim);

			var filtered = all.stream()
					.filter(o -> matchesStatusFlexible(o, needle))
					.toList();

			page = toPage(filtered, pageable);

			log.debug("Status fallback search done: q='{}', filtered={}", qTrim, filtered.size());
		}

		log.debug("OrderService.listForUser: totalElements={}, totalPages={}",
				page.getTotalElements(), page.getTotalPages());

		return page;
	}
	
	private org.springframework.data.domain.Page<Order> toPage(java.util.List<Order> items, Pageable pageable) {
		if (pageable == null || pageable.isUnpaged()) {
			return new org.springframework.data.domain.PageImpl<>(items);
		}

		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), items.size());

		java.util.List<Order> pageContent =
				(start >= items.size()) ? java.util.List.of() : items.subList(start, end);

		return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, items.size());
	}

	private boolean matchesStatusFlexible(Order o, String needleRaw) {
	    if (o == null || needleRaw == null || needleRaw.isBlank()) return false;

	    String needle = normalizeForSearch(needleRaw);

	    String code  = normalizeForSearch(o.getStatusCode());   // e.g. "mr finance pending"
	    String label = normalizeForSearch(o.getStatusLabel());  // e.g. "pending approval from finance"

	    // direct contains
	    if (code.contains(needle) || label.contains(needle)) {
	        return true;
	    }

	    // token match (order-independent): "finance approved" should match "approved finance"
	    String[] tokens = needle.split("\\s+");
	    int nonBlank = 0;

	    for (String t : tokens) {
	        if (t == null || t.isBlank()) continue;
	        nonBlank++;

	        // support common aliases
	        String alias = normalizeStatusAlias(t);

	        boolean tokenMatched =
	                code.contains(t) || label.contains(t) ||
	                (!alias.equals(t) && (code.contains(alias) || label.contains(alias)));

	        if (!tokenMatched) {
	            return false;
	        }
	    }

	    return nonBlank > 0;
	}

	private String normalizeForSearch(String s) {
	    if (s == null) return "";
	    return s.toLowerCase()
	            .replace('_', ' ')
	            .replace('-', ' ')
	            .replace(':', ' ')
	            .replaceAll("\\s+", " ")
	            .trim();
	}

	private String normalizeStatusAlias(String token) {
	    return switch (token) {
	        case "fin" -> "finance";
	        case "exp" -> "expense";
	        case "reject" -> "rejected";
	        case "approve" -> "approved";   // map to final state only
	        default -> token;
	    };
	}
	@Transactional(readOnly = true)
	public Order getRequired(Long id) {
		log.debug("OrderService.getRequired: id={}", id);
		return orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	}

	@Transactional
	public Order create(AppUser creator, OrderForm form) {
		log.info("OrderService.create: creatorId={}, salesOrderId={}", creator != null ? creator.getId() : null,
				form != null ? form.getSalesOrderId() : null);

		validateCreateOrUpdateInput(form);

		// Creator must be allowed for chosen location (unless ADMIN)
		if (!creator.getLocations().contains(form.getLocation()) && !hasAdmin(creator)) {
			log.warn("OrderService.create: access denied for creatorId={} to location={}", creator.getId(),
					form.getLocation());
			throw new AccessDeniedException("Not allowed to create orders for location: " + form.getLocation());
		}

		// Uniqueness guard (case-insensitive)
		if (orders.existsBySalesOrderIdIgnoreCase(form.getSalesOrderId())) {
			log.warn("OrderService.create: duplicate salesOrderId={}", form.getSalesOrderId());
			throw new IllegalArgumentException("Sales Order ID already exists: " + form.getSalesOrderId());
		}

		Customer customer = customers.findById(form.getCustomerId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid customerId: " + form.getCustomerId()));

		AppUser salesManager = users.findById(form.getSalesManagerId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid salesManagerId: " + form.getSalesManagerId()));

		Set<Vertical> selectedVerticals = form.getVerticalIds() == null || form.getVerticalIds().isEmpty() ? Set.of()
				: verticals.findAllById(form.getVerticalIds()).stream().collect(Collectors.toSet());

		if (form.getVerticalIds() != null && selectedVerticals.size() != form.getVerticalIds().size()) {
			log.warn("OrderService.create: invalid vertical IDs in form");
			throw new IllegalArgumentException("One or more vertical IDs are invalid");
		}

		Order o = new Order();
		o.setSalesOrderId(form.getSalesOrderId().trim());
		o.setCustomer(customer);
		o.setDescription(trimToNull(form.getDescription()));
		o.setSalesManager(salesManager);
		o.setVerticals(selectedVerticals);
		o.setLocation(form.getLocation());
		o.setCreatedBy(creator);
		o.setCreatedAt(Instant.now());

		orders.save(o);

		// Publish domain event
		events.publishEvent(new OrderCreatedEvent(o.getId(), o.getCustomer().getId(), o.getCustomer().getName(),
				o.getDescription(), o.getLocation().label(), o.getSalesManager().getId(),
				o.getSalesManager().getEmail(), o.getSalesOrderId()));

		log.info("OrderService.create: created orderId={}, salesOrderId={}", o.getId(), o.getSalesOrderId());

		return o;
	}

	@Transactional
	public Order update(AppUser updater, Long id, OrderForm form) {
		log.info("OrderService.update: updaterId={}, id={}", updater != null ? updater.getId() : null, id);

		validateCreateOrUpdateInput(form);

		Order o = getRequired(id);

		if (!updater.getLocations().contains(form.getLocation()) && !hasAdmin(updater)) {
			log.warn("OrderService.update: access denied for updaterId={} to location={}", updater.getId(),
					form.getLocation());
			throw new AccessDeniedException("Not allowed to update to location: " + form.getLocation());
		}

		// Uniqueness guard on update (ignore self)
		String newSoid = form.getSalesOrderId().trim();
		if (!o.getSalesOrderId().equalsIgnoreCase(newSoid) && orders.existsBySalesOrderIdIgnoreCase(newSoid)) {
			log.warn("OrderService.update: duplicate salesOrderId={} for id={}", newSoid, id);
			throw new IllegalArgumentException("Sales Order ID already exists: " + newSoid);
		}

		Customer customer = customers.findById(form.getCustomerId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid customerId: " + form.getCustomerId()));

		AppUser salesManager = users.findById(form.getSalesManagerId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid salesManagerId: " + form.getSalesManagerId()));

		Set<Vertical> selectedVerticals = form.getVerticalIds() == null || form.getVerticalIds().isEmpty() ? Set.of()
				: verticals.findAllById(form.getVerticalIds()).stream().collect(Collectors.toSet());

		if (form.getVerticalIds() != null && selectedVerticals.size() != form.getVerticalIds().size()) {
			log.warn("OrderService.update: invalid vertical IDs in form for id={}", id);
			throw new IllegalArgumentException("One or more vertical IDs are invalid");
		}

		o.setSalesOrderId(newSoid);
		o.setCustomer(customer);
		o.setDescription(trimToNull(form.getDescription()));
		o.setSalesManager(salesManager);
		o.setVerticals(selectedVerticals);
		o.setLocation(form.getLocation());

		Order saved = orders.save(o);

		log.info("OrderService.update: updated orderId={}, salesOrderId={}", saved.getId(), saved.getSalesOrderId());

		return saved;
	}

	@Transactional
	public void delete(Long id) {
		log.warn("OrderService.delete: id={}", id);

		// ✅ Block delete if invoices exist
		if (hasInvoices(id)) {
			throw new BusinessException(
					"Cannot delete this order because invoice(s) exist. Please delete/void the invoice(s) first.");
		}

		String salesOrderId = null;
		try {
			var o = orders.findById(id).orElse(null);
			if (o != null) {
				salesOrderId = o.getSalesOrderId();
			}
		} catch (Exception ex) {
			log.warn("OrderService.delete: failed to load order before delete id={}", id, ex);
		}

		orders.deleteById(id);

		log.info("OrderService.delete: deleted id={}, salesOrderId={}", id, salesOrderId);
	}

	private void validateCreateOrUpdateInput(OrderForm form) {
		Objects.requireNonNull(form, "form");
		if (form.getSalesOrderId() == null || form.getSalesOrderId().isBlank())
			throw new IllegalArgumentException("salesOrderId is required");
		if (form.getSalesOrderId().length() > 50)
			throw new IllegalArgumentException("salesOrderId too long (max 50)");
		if (!form.getSalesOrderId().matches("^[A-Za-z0-9._\\-]+$"))
			throw new IllegalArgumentException("salesOrderId allows only letters, numbers, dot, dash and underscore");

		if (form.getCustomerId() == null)
			throw new IllegalArgumentException("customerId is required");
		if (form.getSalesManagerId() == null)
			throw new IllegalArgumentException("salesManagerId is required");
		if (form.getLocation() == null)
			throw new IllegalArgumentException("location is required");
		if (form.getDescription() != null && form.getDescription().length() > 500)
			throw new IllegalArgumentException("description too long (max 500)");
	}

	private boolean hasAdmin(AppUser user) {
		return user.getRoles() != null && user.getRoles().contains(AppRole.ADMIN);
	}

	private static String trimToNull(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	@Transactional(readOnly = true)
	public Order getForDetails(Long id) {
		log.debug("OrderService.getForDetails: id={}", id);
		return orders.findForDetails(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	}

	@Transactional(readOnly = true)
	public Order getForEdit(Long id) {
		log.debug("OrderService.getForEdit: id={}", id);
		return orders.findForEdit(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	}

	@Transactional
	public boolean hasInvoices(Long orderId) {
		return invRepo.existsByOrder_Id(orderId);
	}
	
	@Transactional(readOnly = true)
	public Page<Order> listForUserAdvanced(AppUser currentUser, Location loc, String q,
			Long customerId, Long managerId, Long verticalId, String descWords,
			String startDateStr, String endDateStr, Pageable pageable) {

		var allowed = currentUser.getLocations();
		boolean hasBaseSearch = (q != null && !q.isBlank());
		String qTrim = hasBaseSearch ? q.trim() : null;

		// 1. Fetch unpaged dataset based on standard user location context
		Page<Order> rawLookupPage = (loc != null)
				? orders.findByLocation(loc, Pageable.unpaged())
				: orders.findByLocationIn(allowed, Pageable.unpaged());

		var listData = rawLookupPage.getContent();
		
		// Map workflow states instantly
		listData.forEach(o -> o.setStatusView(orderStatusService.computeOrderStatus(o.getId())));

		var stream = listData.stream();

		// Filter A: Handle original top bar search input if active
		if (hasBaseSearch) {
			String needle = normalizeForSearch(qTrim);
			stream = stream.filter(o -> 
				o.getSalesOrderId().toLowerCase().contains(needle) ||
				(o.getCustomer() != null && o.getCustomer().getName().toLowerCase().contains(needle)) ||
				(o.getDescription() != null && o.getDescription().toLowerCase().contains(needle)) ||
				matchesStatusFlexible(o, needle)
			);
		}

		// Filter B: Advanced Option - Customer Selection
		if (customerId != null) {
			stream = stream.filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(customerId));
		}

		// Filter C: Advanced Option - Sales Manager Selection
		if (managerId != null) {
			stream = stream.filter(o -> o.getSalesManager() != null && o.getSalesManager().getId().equals(managerId));
		}

		// Filter D: Advanced Option - Business Vertical Association
		if (verticalId != null) {
			stream = stream.filter(o -> o.getVerticals().stream().anyMatch(v -> v.getId().equals(verticalId)));
		}

		// Filter E: Advanced Option - Targeted Description Keywords
		if (descWords != null && !descWords.isBlank()) {
			String descNeedle = descWords.trim().toLowerCase();
			stream = stream.filter(o -> o.getDescription() != null && o.getDescription().toLowerCase().contains(descNeedle));
		}

		// Filter F: Advanced Option - Boundary Start Date Limit
		if (startDateStr != null && !startDateStr.isBlank()) {
			try {
				Instant startThreshold = java.time.LocalDate.parse(startDateStr).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
				stream = stream.filter(o -> !o.getCreatedAt().isBefore(startThreshold));
			} catch (Exception e) {
				log.warn("Parsing advanced query filter baseline startDate error: {}", startDateStr);
			}
		}

		// Filter G: Advanced Option - Boundary End Date Limit
		if (endDateStr != null && !endDateStr.isBlank()) {
			try {
				Instant endThreshold = java.time.LocalDate.parse(endDateStr).atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant();
				stream = stream.filter(o -> !o.getCreatedAt().isAfter(endThreshold));
			} catch (Exception e) {
				log.warn("Parsing advanced query filter baseline endDate error: {}", endDateStr);
			}
		}

		var finalizedCollection = stream.toList();
		return toPage(finalizedCollection, pageable);
	}
}
