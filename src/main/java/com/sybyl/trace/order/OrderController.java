package com.sybyl.trace.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.exception.BusinessException;
import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.AdditionalExpenseLabelService;
import com.sybyl.trace.masterdata.CustomerRepository;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.order.expense.AdditionalExpenseService;
import com.sybyl.trace.order.finance.OrderInvoiceService;
import com.sybyl.trace.order.margin.MarginReportApprovalStatus;
import com.sybyl.trace.order.margin.MarginReportService;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUserRepository;
import com.sybyl.trace.web.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PreAuthorize("isAuthenticated()")
@Controller
public class OrderController {

	private final OrderService orderService;
	private final CustomerRepository customers;
	private final AppUserRepository users;
	private final VerticalRepository verticals;
	private final MarginReportService marginReportService;
	private final AdditionalExpenseLabelService additionalExpenseLabelService;
	private final AdditionalExpenseService additionalExpenseService;
	private final OrderInvoiceService orderInvoiceService;
	private final NetMarginReportService netMarginReportService;
	private final AppAuditService auditService;

	@Value("${app.upload.margin-reports.path}")
	private String uploadBasePath;

	public OrderController(OrderService orderService, CustomerRepository customers, AppUserRepository users,
			VerticalRepository verticals, MarginReportService marginReportService,
			AdditionalExpenseLabelService additionalExpenseLabelService,
			AdditionalExpenseService additionalExpenseService, OrderInvoiceService orderInvoiceService,
			NetMarginReportService netMarginReportService, AppAuditService auditService) {

		this.orderService = orderService;
		this.customers = customers;
		this.users = users;
		this.verticals = verticals;
		this.marginReportService = marginReportService;
		this.additionalExpenseLabelService = additionalExpenseLabelService;
		this.additionalExpenseService = additionalExpenseService;
		this.orderInvoiceService = orderInvoiceService;
		this.netMarginReportService = netMarginReportService;
		this.auditService = auditService;
	}

	@GetMapping("/orders")
	public String list(
			@RequestParam(value = "q", required = false) String q, 
			@AuthenticationPrincipal MyUserDetails me,
			@RequestParam(name = "loc", required = false) Location loc,
			@RequestParam(name = "advCustomer", required = false) Long advCustomer,
			@RequestParam(name = "advManager", required = false) Long advManager,
			@RequestParam(name = "advVertical", required = false) Long advVertical,
			@RequestParam(name = "advDesc", required = false) String advDesc,
			@RequestParam(name = "advStartDate", required = false) String advStartDate,
			@RequestParam(name = "advEndDate", required = false) String advEndDate,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size, Model model) {

		Set<Location> allowed = me.getLocations();
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		// Load custom list incorporating structural variables
		Page<Order> result = orderService.listForUserAdvanced(me.getUser(), loc, q, 
				advCustomer, advManager, advVertical, advDesc, advStartDate, advEndDate, pageable);

		// Provide full reference lookups for dropdown inputs mapping
		model.addAttribute("customersLookup", customers.findAll());
		model.addAttribute("managersLookup", users.findAllSalesManagers());
		model.addAttribute("verticalsLookup", verticals.findAll());

		model.addAttribute("page", result);
		model.addAttribute("allowedLocations", allowed);
		model.addAttribute("selectedLoc", loc);
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("contentJsp", "orders/orderlist.jsp");
		return "layout";
	}

	@GetMapping("/orders/fragment")
	@PreAuthorize("isAuthenticated()")
	public String listFragment(
			@AuthenticationPrincipal MyUserDetails me,
			@RequestParam(name = "loc", required = false) Location loc,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "advCustomer", required = false) Long advCustomer,
			@RequestParam(name = "advManager", required = false) Long advManager,
			@RequestParam(name = "advVertical", required = false) Long advVertical,
			@RequestParam(name = "advDesc", required = false) String advDesc,
			@RequestParam(name = "advStartDate", required = false) String advStartDate,
			@RequestParam(name = "advEndDate", required = false) String advEndDate,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size, Model model) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Order> result = orderService.listForUserAdvanced(me.getUser(), loc, q, 
				advCustomer, advManager, advVertical, advDesc, advStartDate, advEndDate, pageable);

		model.addAttribute("page", result);
		model.addAttribute("selectedLoc", loc);
		model.addAttribute("q", q == null ? "" : q);
		return "orders/_listContent";
	}
	// CREATE (form)
	@GetMapping("/orders/new")
	public String createForm(@AuthenticationPrincipal MyUserDetails me, Model model) {
		log.debug("Open create order form by userId={}", (me != null ? me.getUsername() : null));
		model.addAttribute("pageTitle", "Create Order");
		model.addAttribute("form", new OrderForm());
		supplyFormLookups(model, me);
		model.addAttribute("contentJsp", "orders/orderform.jsp");
		return "layout";
	}

	@PostMapping("/orders")
	public String create(@AuthenticationPrincipal MyUserDetails me,
	                     @Valid @ModelAttribute("form") OrderForm form,
	                     BindingResult errors,
	                     Model model,
	                     RedirectAttributes redirectAttrs,
	                     HttpServletRequest request) {

	    log.info("Create order requested by userId={}, salesOrderId={}",
	            (me != null ? me.getUsername() : null), form.getSalesOrderId());

	    if (errors.hasErrors()) {
	        log.warn("Create order validation failed: {}", errors.getAllErrors());
	        model.addAttribute("pageTitle", "Create Order");
	        supplyFormLookups(model, me);
	        model.addAttribute("contentJsp", "orders/orderform.jsp");
	        return "layout";
	    }

	    try {
	        Order created = orderService.create(me.getUser(), form);
	        log.info("Order created: id={}, salesOrderId={}", created.getId(), created.getSalesOrderId());

	        auditService.logEvent("ORDER", created.getId(), created.getSalesOrderId(), "CREATE",
	                "Created order " + created.getSalesOrderId(),
	                null, me != null ? me.getUser() : null);

	        redirectAttrs.addFlashAttribute("message", "Order created successfully.");
	        return "redirect:/orders";

	    } catch (AccessDeniedException ex) {
	        log.warn("Create order forbidden for userId={}, location={}",
	                me != null ? me.getUsername() : null, form.getLocation());

	        errors.rejectValue("location", "order.location.forbidden",
	                "You are not allowed to create orders for this location.");

	    } catch (IllegalArgumentException ex) {
	        log.warn("Create order business validation failed for userId={}, salesOrderId={}, msg={}",
	                me != null ? me.getUsername() : null, form.getSalesOrderId(), ex.getMessage());

	        String msg = ex.getMessage() != null ? ex.getMessage() : "Invalid input.";

	        // map duplicate SO ID to field error
	        if (msg.toLowerCase().contains("sales order id already exists")) {
	            errors.rejectValue("salesOrderId", "order.salesOrderId.duplicate", msg);
	        } else if (msg.toLowerCase().contains("customerid")) {
	            errors.rejectValue("customerId", "order.customer.invalid", msg);
	        } else if (msg.toLowerCase().contains("salesmanagerid")) {
	            errors.rejectValue("salesManagerId", "order.salesManager.invalid", msg);
	        } else {
	            errors.reject("order.create.failed", msg);
	        }

	    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
	        // safety net for race conditions / DB unique constraint
	        log.warn("Create order DB constraint violation for salesOrderId={}", form.getSalesOrderId(), ex);

	        errors.rejectValue("salesOrderId", "order.salesOrderId.duplicate",
	                "Sales Order ID already exists: " + form.getSalesOrderId());

	    } catch (Exception ex) {
	        log.error("Create order failed for userId={}, salesOrderId={}",
	                me != null ? me.getUsername() : null, form.getSalesOrderId(), ex);

	        errors.reject("order.create.failed", "Failed to create order. Please try again.");
	    }

	    // return form with errors
	    model.addAttribute("pageTitle", "Create Order");
	    supplyFormLookups(model, me);
	    model.addAttribute("contentJsp", "orders/orderform.jsp");
	    return "layout";
	}

	@PostMapping("/orders/{id}")
	public String update(@AuthenticationPrincipal MyUserDetails me, @PathVariable Long id,
			@Valid @ModelAttribute("form") OrderForm form, BindingResult errors, Model model,
			RedirectAttributes redirectAttrs, HttpServletRequest request) {

		log.info("Update order requested by userId={}, id={}, salesOrderId={}", me.getUsername(), id,
				form.getSalesOrderId());

		if (errors.hasErrors()) {
			log.warn("Update order validation failed: id={}, errors={}", id, errors.getAllErrors());
			model.addAttribute("pageTitle", "Edit Order #" + id);
			supplyFormLookups(model, me);
			model.addAttribute("contentJsp", "orders/orderform.jsp");
			return "layout";
		}

		try {
			orderService.update(me.getUser(), id, form);
			log.info("Order updated: id={}", id);

			Order updated = orderService.getForDetails(id);


			auditService.logEvent("ORDER", updated.getId(), updated.getSalesOrderId(), "UPDATE",
					"Updated order " + updated.getSalesOrderId(), null, me != null ? me.getUser() : null);

		} catch (AccessDeniedException ex) {
			log.warn("Update order forbidden: userId={}, id={}, newLocation={}", me.getUsername(), id,
					form.getLocation());
			errors.rejectValue("location", "order.location.forbidden",
					"You are not allowed to update orders for this location.");
			model.addAttribute("pageTitle", "Update Order");
			supplyFormLookups(model, me);
			model.addAttribute("contentJsp", "orders/orderform.jsp");
			return "layout";
		} catch (Exception ex) {
			log.error("Update order failed: id={}", id, ex);
			throw ex;
		}

		redirectAttrs.addFlashAttribute("message", "Order updated successfully.");
		return "redirect:/orders";
	}

	@PostMapping("/orders/{id}/delete")
	public String delete(@AuthenticationPrincipal MyUserDetails me,
	                     @PathVariable Long id,
	                     RedirectAttributes redirectAttrs,
	                     HttpServletRequest request) {

	    log.warn("Delete order requested: id={}, userId={}", id, (me != null ? me.getUsername() : null));

	    Order order = null;
	    try {
	        order = orderService.getForDetails(id);
	    } catch (Exception ex) {
	        log.warn("Delete order: order not found for id={}", id);
	    }

	    String salesOrderId = (order != null ? order.getSalesOrderId() : null);

	    try {
	        // ✅ this will throw BusinessException if invoices exist
	        orderService.delete(id);

	        // ✅ audit only on success
	        auditService.logEvent("ORDER", id, salesOrderId, "DELETE",
	                "Deleted order " + (salesOrderId != null ? salesOrderId : ("id=" + id)),
	                null, me != null ? me.getUser() : null);

	        redirectAttrs.addFlashAttribute("message", "Order deleted.");
	        log.info("Order deleted: id={}, salesOrderId={}", id, salesOrderId);

	    } catch (BusinessException ex) {
	        redirectAttrs.addFlashAttribute("error", ex.getMessage());
	        log.warn("Order delete blocked: id={}, reason={}", id, ex.getMessage());
	    }
	    return "redirect:/orders";
	}

	// -------- helpers --------
	private void supplyFormLookups(Model model, MyUserDetails me) {
		log.debug("Supply lookups for userId={}", (me != null ? me.getUsername() : null));
		model.addAttribute("customers", customers.findByActiveTrue());
		model.addAttribute("salesManagers", users.findAllSalesManagers());
		model.addAttribute("verticals", verticals.findByActiveTrue());

		var all = java.util.EnumSet.allOf(Location.class);
		var allowed = me.getUser().getRoles().contains(com.sybyl.trace.user.AppRole.ADMIN) ? all
				: (me.getLocations() == null || me.getLocations().isEmpty() ? java.util.EnumSet.noneOf(Location.class)
						: java.util.EnumSet.copyOf(me.getLocations()));
		model.addAttribute("locations", allowed);
	}

	private OrderForm toForm(Order o) {
		OrderForm f = new OrderForm();
		f.setSalesOrderId(o.getSalesOrderId());
		f.setId(o.getId());
		f.setCustomerId(o.getCustomer().getId());
		f.setDescription(o.getDescription());
		f.setSalesManagerId(o.getSalesManager().getId());
		f.setLocation(o.getLocation());
		f.setVerticalIds(o.getVerticals().stream().map(Vertical::getId).collect(java.util.stream.Collectors.toSet()));
		return f;
	}

	// SHOW order details
	@GetMapping("/orders/{id}")
	public String showOrderDetails(@PathVariable Long id,
			@RequestParam(name = "tab", defaultValue = "margin") String tab, Model model) {

		log.info("Order details requested: id={}, tab={}", id, tab);
		Order order = orderService.getForDetails(id);

		boolean allMarginsApproved = !order.getMarginReports().isEmpty() && order.getMarginReports().stream()
				.allMatch(mr -> mr.getApprovalStatus() == MarginReportApprovalStatus.APPROVED);

		model.addAttribute("order", order);
		model.addAttribute("allMarginsApproved", allMarginsApproved);
		model.addAttribute("activeTab", tab);
		model.addAttribute("currencies", java.util.Arrays.asList(com.sybyl.trace.order.CurrencyCode.values()));

		if ("finance".equals(tab)) {
			model.addAttribute("invoices", orderInvoiceService.listByOrder(id));
		} else {

			var marginReports = marginReportService.listForOrder(id);
			var orderVerticals = order.getVerticals();

			Set<Long> mrVerticalIds = marginReports.stream().filter(mr -> mr.getVertical() != null)
					.map(mr -> mr.getVertical().getId()).collect(Collectors.toSet());

			var expenseVerticals = orderVerticals.stream().filter(v -> mrVerticalIds.contains(v.getId()))
					.collect(Collectors.toList());

			model.addAttribute("verticals", orderVerticals);
			model.addAttribute("marginReports", marginReports);
			model.addAttribute("mrVerticalIds", mrVerticalIds);
			model.addAttribute("expenseVerticals", expenseVerticals);

			model.addAttribute("expenseLabels", additionalExpenseLabelService.listActive());
			model.addAttribute("additionalExpenses", additionalExpenseService.listForOrder(id));

			model.addAttribute("scriptJsp", "orders/detail/margin-script.jsp");
		}

		model.addAttribute("pageTitle", "Order #" + order.getSalesOrderId());
		model.addAttribute("contentJsp", "orders/detail/order_detail.jsp");
		return "layout";
	}

	// EDIT form
	@GetMapping("/orders/{id}/edit")
	public String editForm(@AuthenticationPrincipal MyUserDetails me, @PathVariable Long id, Model model) {
		log.info("Edit order form requested: id={}, userId={}", id, (me != null ? me.getUsername() : null));
		Order o = orderService.getForEdit(id);
		OrderForm form = toForm(o);
		model.addAttribute("pageTitle", "Edit Order #" + id);
		model.addAttribute("form", form);
		supplyFormLookups(model, me);
		model.addAttribute("contentJsp", "orders/orderform.jsp");
		return "layout";
	}

	// --------------------
	// MARGIN REPORT UPLOAD
	// --------------------
	@PostMapping("/orders/{id}/margin-reports")
	public String uploadMarginReport(@AuthenticationPrincipal MyUserDetails me, @PathVariable Long id,
			@RequestParam("file") MultipartFile file, @RequestParam("buyingPrice") BigDecimal buyingPrice,
			@RequestParam("buyingCurrency") CurrencyCode buyingCurrency,
			@RequestParam("sellingPrice") BigDecimal sellingPrice,
			@RequestParam("sellingCurrency") CurrencyCode sellingCurrency,
			@RequestParam("conversionRate") BigDecimal conversionRate, @RequestParam("verticalId") Long verticalId,
			@RequestParam(value = "label", required = false) String label,
			@RequestParam(value = "comments", required = false) String comments, RedirectAttributes ra)
			throws IOException {

		log.info("Upload margin report: orderId={}, userId={}, verticalId={}, file='{}'", id,
				(me != null ? me.getUsername() : null), verticalId, (file != null ? file.getOriginalFilename() : null));

		try {
			marginReportService.save(id, me.getUser(), file, label, buyingCurrency, sellingCurrency, buyingPrice,
					sellingPrice, conversionRate, verticalId, comments);

			ra.addFlashAttribute("message", "Margin report uploaded.");
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("marginError", ex.getMessage());
			ra.addFlashAttribute("showMarginModal", true);
		} catch (Exception ex) {
			ra.addFlashAttribute("marginError", "Failed to upload margin report: " + ex.getMessage());
			ra.addFlashAttribute("showMarginModal", true);
		}

		return "redirect:/orders/" + id + "?tab=margin";
	}

	@GetMapping("/orders/{orderId}/margin-reports/{mrId}/download")
	public void downloadMarginReport(@PathVariable Long orderId, @PathVariable Long mrId, HttpServletResponse response, RedirectAttributes ra)
			throws IOException {

		log.info("Download margin report requested: orderId={}, mrId={}", orderId, mrId);

		var mr = marginReportService.getForDownload(orderId, mrId);

		Path base = Paths.get(uploadBasePath).toAbsolutePath().normalize();
		Path filePath = base.resolve(mr.getStorageKey()).normalize();

		if (!filePath.startsWith(base)) {
			log.warn("Blocked path traversal: mrId={}, resolved={}", mrId, filePath);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid file path");
			return;
		}

		if (!Files.exists(filePath)) {
			log.warn("File not found: mrId={}, path={}", mrId, filePath);
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
			return;
		}

		String contentType = Files.probeContentType(filePath);
		if (contentType == null)
			contentType = "application/octet-stream";

		String safeName = (mr.getFileName() == null ? "file" : mr.getFileName()).replace("\"", "");
		response.setContentType(contentType);
		response.setHeader("Content-Disposition", "attachment; filename=\"" + safeName + "\"");

		try (var in = Files.newInputStream(filePath); var out = response.getOutputStream()) {
			in.transferTo(out);
			out.flush();
			log.info("Download completed: orderId={}, mrId={}, bytes={}", orderId, mrId, Files.size(filePath));
		} catch (IOException ex) {
			ra.addFlashAttribute("error", "Failed to upload margin report: " + ex.getMessage());
			log.error("Download failed: orderId={}, mrId={}, path={}", orderId, mrId, filePath, ex);
			throw ex;
		}
	}

	@PostMapping("/orders/{orderId}/margin-reports/{mrId}/delete")
	public String deleteMarginReport(@AuthenticationPrincipal MyUserDetails me, @PathVariable Long orderId,
			@PathVariable Long mrId, @RequestParam(value = "deleteFile", defaultValue = "true") boolean deleteFile,
			RedirectAttributes ra, HttpServletRequest request) throws IOException {

		try {
			marginReportService.delete(orderId, mrId, me.getUser(), deleteFile);
			ra.addFlashAttribute("message", "Margin report deleted.");
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage());
		} catch (Exception ex) {
			ra.addFlashAttribute("error", "Failed to delete margin report: " + ex.getMessage());
		}

		return "redirect:/orders/" + orderId + "?tab=margin";
	}

	@PostMapping("/orders/{orderId}/margin-reports/{mrId}/update")
	public String updateMarginReport(@AuthenticationPrincipal MyUserDetails me, @PathVariable Long orderId,
			@PathVariable Long mrId, @RequestParam BigDecimal buyingPrice, @RequestParam CurrencyCode buyingCurrency,
			@RequestParam BigDecimal sellingPrice, @RequestParam CurrencyCode sellingCurrency,
			@RequestParam BigDecimal conversionRate, @RequestParam Long verticalId,
			@RequestParam(required = false) String comments, @RequestParam(required = false) MultipartFile file,
			RedirectAttributes ra, HttpServletRequest request) {

		try {
			marginReportService.update(orderId, mrId, buyingPrice, buyingCurrency, sellingPrice, sellingCurrency,
					conversionRate, verticalId, comments, file, me.getUser());

			ra.addFlashAttribute("message", "Margin report updated.");
		} catch (IllegalStateException ex) {
			ra.addFlashAttribute("error", ex.getMessage()); 
		}
		return "redirect:/orders/" + orderId + "?tab=margin";
	}

	@GetMapping("/orders/{id}/net-margin-report")
	public void downloadNetMarginReport(@PathVariable Long id, HttpServletResponse response) throws IOException {

		Order order = orderService.getForDetails(id);

		byte[] pdfBytes = netMarginReportService.generatePdf(id);
		String fileName = "Net-Margin-Report-" + order.getSalesOrderId() + ".pdf";

		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName.replace("\"", "") + "\"");
		response.setContentLength(pdfBytes.length);

		response.getOutputStream().write(pdfBytes);
		response.getOutputStream().flush();
	}

	@GetMapping("/orders/{id}/net-margin-report/preview")
	public void previewNetMarginReport(@PathVariable Long id, HttpServletResponse response) throws IOException {

		String html = netMarginReportService.generateHtmlForPreview(id);
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write(html);
	}
}
