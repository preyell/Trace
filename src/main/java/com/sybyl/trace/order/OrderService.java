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
import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Customer;
import com.sybyl.trace.masterdata.CustomerRepository;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.order.events.OrderCreatedEvent;
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

	public OrderService(OrderRepository orders,
	                    CustomerRepository customers,
	                    AppUserRepository users,
	                    VerticalRepository verticals,
	                    ApplicationEventPublisher events,
	                    OrderStatusService orderStatusService,
	                    AppAuditService auditService) {
		this.orders = orders;
		this.customers = customers;
		this.users = users;
		this.verticals = verticals;
		this.events = events;
		this.orderStatusService = orderStatusService;
		this.auditService = auditService;
	}


	public Page<Order> listForUser(AppUser currentUser, Location loc, String q, Pageable pageable) {
		log.info("OrderService.listForUser: userId={}, loc={}, q='{}', page={}",
				currentUser != null ? currentUser.getId() : null, loc, q,
				pageable != null ? pageable.getPageNumber() : null);

		var allowed = currentUser.getLocations();
		boolean hasSearch = (q != null && !q.isBlank());

		Page<Order> page;

		if (loc != null) {
			page = hasSearch
					? orders.searchInLocation(loc, q.trim(), pageable)
					: orders.findByLocation(loc, pageable);
		} else {
			page = hasSearch
					? orders.searchInLocations(allowed, q.trim(), pageable)
					: orders.findByLocationIn(allowed, pageable);
		}

		page.forEach(o -> {
			OrderStatusView status = orderStatusService.computeOrderStatus(o.getId());
			o.setStatusView(status);
		});

		log.debug("OrderService.listForUser: totalElements={}, totalPages={}",
				page.getTotalElements(), page.getTotalPages());

		return page;
	}

	@Transactional(readOnly = true)
	public Order getRequired(Long id) {
		log.debug("OrderService.getRequired: id={}", id);
		return orders.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	}

	@Transactional
	public Order create(AppUser creator, OrderForm form) {
		log.info("OrderService.create: creatorId={}, salesOrderId={}",
				creator != null ? creator.getId() : null,
				form != null ? form.getSalesOrderId() : null);

		validateCreateOrUpdateInput(form);

		// Creator must be allowed for chosen location (unless ADMIN)
		if (!creator.getLocations().contains(form.getLocation()) && !hasAdmin(creator)) {
			log.warn("OrderService.create: access denied for creatorId={} to location={}",
					creator.getId(), form.getLocation());
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

		Set<Vertical> selectedVerticals =
				form.getVerticalIds() == null || form.getVerticalIds().isEmpty()
						? Set.of()
						: verticals.findAllById(form.getVerticalIds())
						.stream()
						.collect(Collectors.toSet());

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
		events.publishEvent(
				new OrderCreatedEvent(
						o.getId(),
						o.getCustomer().getId(),
						o.getCustomer().getName(),
						o.getDescription(),
						o.getLocation().label(),
						o.getSalesManager().getId(),
						o.getSalesManager().getEmail(),
						o.getSalesOrderId()
				)
		);

		

		log.info("OrderService.create: created orderId={}, salesOrderId={}",
				o.getId(), o.getSalesOrderId());

		return o;
	}

	@Transactional
	public Order update(AppUser updater, Long id, OrderForm form) {
		log.info("OrderService.update: updaterId={}, id={}",
				updater != null ? updater.getId() : null, id);

		validateCreateOrUpdateInput(form);

		Order o = getRequired(id);

		if (!updater.getLocations().contains(form.getLocation()) && !hasAdmin(updater)) {
			log.warn("OrderService.update: access denied for updaterId={} to location={}",
					updater.getId(), form.getLocation());
			throw new AccessDeniedException("Not allowed to update to location: " + form.getLocation());
		}

		// Uniqueness guard on update (ignore self)
		String newSoid = form.getSalesOrderId().trim();
		if (!o.getSalesOrderId().equalsIgnoreCase(newSoid)
				&& orders.existsBySalesOrderIdIgnoreCase(newSoid)) {
			log.warn("OrderService.update: duplicate salesOrderId={} for id={}", newSoid, id);
			throw new IllegalArgumentException("Sales Order ID already exists: " + newSoid);
		}

		Customer customer = customers.findById(form.getCustomerId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid customerId: " + form.getCustomerId()));

		AppUser salesManager = users.findById(form.getSalesManagerId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid salesManagerId: " + form.getSalesManagerId()));

		Set<Vertical> selectedVerticals =
				form.getVerticalIds() == null || form.getVerticalIds().isEmpty()
						? Set.of()
						: verticals.findAllById(form.getVerticalIds())
						.stream()
						.collect(Collectors.toSet());

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

		

		log.info("OrderService.update: updated orderId={}, salesOrderId={}",
				saved.getId(), saved.getSalesOrderId());

		return saved;
	}

	@Transactional
	public void delete(Long id) {
		log.warn("OrderService.delete: id={}", id);

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
		if (s == null) return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	@Transactional(readOnly = true)
	public Order getForDetails(Long id) {
		log.debug("OrderService.getForDetails: id={}", id);
		return orders.findForDetails(id)
				.orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	}

	@Transactional(readOnly = true)
	public Order getForEdit(Long id) {
		log.debug("OrderService.getForEdit: id={}", id);
		return orders.findForEdit(id)
				.orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	}
}
