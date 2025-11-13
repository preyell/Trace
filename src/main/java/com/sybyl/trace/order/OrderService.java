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

import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.Customer;
import com.sybyl.trace.masterdata.CustomerRepository;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.order.events.OrderCreatedEvent;
import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

@Service
public class OrderService {

	private final OrderRepository orders;
	private final CustomerRepository customers;
	private final AppUserRepository users;
	private final VerticalRepository verticals;
	private final ApplicationEventPublisher events;

	public OrderService(OrderRepository orders, CustomerRepository customers, AppUserRepository users,
			VerticalRepository verticals, ApplicationEventPublisher events) {
		this.orders = orders;
		this.customers = customers;
		this.users = users;
		this.verticals = verticals;
		this.events = events;
	}

	// ---------- Queries ----------

	public Page<Order> listForUser(AppUser currentUser, Location loc, String q, Pageable pageable) {
	    var allowed = currentUser.getLocations();
	    boolean hasSearch = (q != null && !q.isBlank());

	    if (loc != null) {
	      return hasSearch ? orders.searchInLocation(loc, q.trim(), pageable)
	                       : orders.findByLocation(loc, pageable);
	    } else {
	      return hasSearch ? orders.searchInLocations(allowed, q.trim(), pageable)
	                       : orders.findByLocationIn(allowed, pageable);
	    }
	  }

	  @Transactional(readOnly = true)
	  public Order getRequired(Long id) {
	    return orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	  }

	  @Transactional
	  public Order create(AppUser creator, OrderForm form) {
	    validateCreateOrUpdateInput(form);

	    // Creator must be allowed for chosen location (unless ADMIN)
	    if (!creator.getLocations().contains(form.getLocation()) && !hasAdmin(creator)) {
	      throw new AccessDeniedException("Not allowed to create orders for location: " + form.getLocation());
	    }

	    // Uniqueness guard (case-insensitive)
	    if (orders.existsBySalesOrderIdIgnoreCase(form.getSalesOrderId())) {
	      throw new IllegalArgumentException("Sales Order ID already exists: " + form.getSalesOrderId());
	    }

	    Customer customer = customers.findById(form.getCustomerId())
	        .orElseThrow(() -> new IllegalArgumentException("Invalid customerId: " + form.getCustomerId()));

	    AppUser salesManager = users.findById(form.getSalesManagerId())
	        .orElseThrow(() -> new IllegalArgumentException("Invalid salesManagerId: " + form.getSalesManagerId()));

	    Set<Vertical> selectedVerticals = form.getVerticalIds() == null || form.getVerticalIds().isEmpty()
	        ? Set.of()
	        : verticals.findAllById(form.getVerticalIds()).stream().collect(Collectors.toSet());

	    if (form.getVerticalIds() != null && selectedVerticals.size() != form.getVerticalIds().size()) {
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

	    events.publishEvent(
	        new OrderCreatedEvent(
	            o.getId(),
	            o.getCustomer().getId(),
	            o.getCustomer().getName(),
	            o.getDescription(),
	            o.getLocation().label(),
	            o.getSalesManager().getId(),
	            o.getSalesManager().getEmail(),
	            o.getSalesOrderId() // <-- add to event (see event class below)
	        )
	    );

	    return o;
	  }

	  @Transactional
	  public Order update(AppUser updater, Long id, OrderForm form) {
	    validateCreateOrUpdateInput(form);

	    Order o = getRequired(id);

	    if (!updater.getLocations().contains(form.getLocation()) && !hasAdmin(updater)) {
	      throw new AccessDeniedException("Not allowed to update to location: " + form.getLocation());
	    }

	    // Uniqueness guard on update (ignore self)
	    String newSoid = form.getSalesOrderId().trim();
	    if (!o.getSalesOrderId().equalsIgnoreCase(newSoid)
	        && orders.existsBySalesOrderIdIgnoreCase(newSoid)) {
	      throw new IllegalArgumentException("Sales Order ID already exists: " + newSoid);
	    }

	    Customer customer = customers.findById(form.getCustomerId())
	        .orElseThrow(() -> new IllegalArgumentException("Invalid customerId: " + form.getCustomerId()));

	    AppUser salesManager = users.findById(form.getSalesManagerId())
	        .orElseThrow(() -> new IllegalArgumentException("Invalid salesManagerId: " + form.getSalesManagerId()));

	    Set<Vertical> selectedVerticals = form.getVerticalIds() == null || form.getVerticalIds().isEmpty()
	        ? Set.of()
	        : verticals.findAllById(form.getVerticalIds()).stream().collect(Collectors.toSet());

	    if (form.getVerticalIds() != null && selectedVerticals.size() != form.getVerticalIds().size()) {
	      throw new IllegalArgumentException("One or more vertical IDs are invalid");
	    }

	    o.setSalesOrderId(newSoid);
	    o.setCustomer(customer);
	    o.setDescription(trimToNull(form.getDescription()));
	    o.setSalesManager(salesManager);
	    o.setVerticals(selectedVerticals);
	    o.setLocation(form.getLocation());

	    return orders.save(o);
	  }

	  @Transactional
	  public void delete(Long id) { orders.deleteById(id); }

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
	    return orders.findForDetails(id)
	        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	  }

	  @Transactional(readOnly = true)
	  public Order getForEdit(Long id) {
	    return orders.findForEdit(id)
	        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
	  }
}
