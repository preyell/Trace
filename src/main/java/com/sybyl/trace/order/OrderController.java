// com.sybyl.trace.order.OrderController
package com.sybyl.trace.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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

import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.AdditionalExpenseLabelService;
import com.sybyl.trace.masterdata.CustomerRepository;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUserRepository;

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
	@Value("${app.upload.margin-reports.path}")
	private String uploadBasePath;

	public OrderController(OrderService orderService, CustomerRepository customers, AppUserRepository users,
			VerticalRepository verticals, MarginReportService marginReportService, AdditionalExpenseLabelService additionalExpenseLabelService, AdditionalExpenseService additionalExpenseService) {
		this.orderService = orderService;
		this.customers = customers;
		this.users = users;
		this.verticals = verticals;
		this.marginReportService = marginReportService;
		this.additionalExpenseLabelService = additionalExpenseLabelService;
		this.additionalExpenseService = additionalExpenseService;
	}

	// LIST
	@GetMapping("/orders")
	public String list(@RequestParam(value = "q", required = false) String q, @AuthenticationPrincipal MyUserDetails me,
			@RequestParam(name = "loc", required = false) Location loc,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size, Model model) {

		log.info("Orders list requested by userId={}, q='{}', loc={}, page={}, size={}",
				(me != null ? me.getUsername() : null), q, loc, page, size);

		Set<Location> allowed = me.getLocations();
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

		Page<Order> result = orderService.listForUser(me.getUser(), loc, q, pageable);

		log.debug("Orders list result: totalElements={}, totalPages={}, numberOfElements={}", result.getTotalElements(),
				result.getTotalPages(), result.getNumberOfElements());

		model.addAttribute("pageTitle", "Orders");
		model.addAttribute("page", result);
		model.addAttribute("allowedLocations", allowed);
		model.addAttribute("selectedLoc", loc);
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("contentJsp", "orders/orderlist.jsp");
		return "layout";
	}

	@GetMapping("/orders/fragment")
	@PreAuthorize("isAuthenticated()")
	public String listFragment(@AuthenticationPrincipal MyUserDetails me,
			@RequestParam(name = "loc", required = false) Location loc,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size, Model model) {

		log.debug("Orders fragment requested by userId={}, q='{}', loc={}, page={}, size={}",
				(me != null ? me.getUsername() : null), q, loc, page, size);

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Order> result = orderService.listForUser(me.getUser(), loc, q, pageable);

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
	public String create(@AuthenticationPrincipal MyUserDetails me, @Valid @ModelAttribute("form") OrderForm form,
			BindingResult errors, Model model, RedirectAttributes redirectAttrs) {
		log.info("Create order requested by userId={}, salesOrderId={}", (me != null ? me.getUsername() : null),
				form.getSalesOrderId());

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
		} catch (AccessDeniedException ex) {
			log.warn("Create order forbidden for userId={}, location={}", me.getUsername(), form.getLocation());
			errors.rejectValue("location", "order.location.forbidden",
					"You are not allowed to create orders for this location.");
			model.addAttribute("pageTitle", "Create Order");
			supplyFormLookups(model, me);
			model.addAttribute("contentJsp", "orders/orderform.jsp");
			return "layout";
		} catch (Exception ex) {
			log.error("Create order failed for userId={}, salesOrderId={}", me.getUsername(), form.getSalesOrderId(),
					ex);
			throw ex;
		}

		redirectAttrs.addFlashAttribute("message", "Order created successfully.");
		return "redirect:/orders";
	}

	@PostMapping("/orders/{id}")
	public String update(@AuthenticationPrincipal MyUserDetails me, @PathVariable Long id,
			@Valid @ModelAttribute("form") OrderForm form, BindingResult errors, Model model,
			RedirectAttributes redirectAttrs) {
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
		} catch (AccessDeniedException ex) {
			log.warn("Update order forbidden: userId={}, id={}, newLocation={}", me.getUsername(), id,
					form.getLocation());
			errors.rejectValue("location", "order.location.forbidden",
					"You are not allowed to update orders for this location.");
			model.addAttribute("pageTitle", "Update Order");
			supplyFormLookups(model, me);
			model.addAttribute("contentJsp", "orders/form.jsp");
			return "layout";
		} catch (Exception ex) {
			log.error("Update order failed: id={}", id, ex);
			throw ex;
		}
		redirectAttrs.addFlashAttribute("message", "Order updated successfully.");
		return "redirect:/orders";
	}

	@PostMapping("/orders/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
		log.warn("Delete order requested: id={}", id);
		orderService.delete(id);
		redirectAttrs.addFlashAttribute("message", "Order deleted.");
		return "redirect:/orders";
	}

	// -------- helpers --------
	private void supplyFormLookups(Model model, MyUserDetails me) {
		log.debug("Supply lookups for userId={}", (me != null ? me.getUsername() : null));
		model.addAttribute("customers", customers.findAll());
		model.addAttribute("salesManagers", users.findAllSalesManagers());
		model.addAttribute("verticals", verticals.findAll());

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

	@GetMapping("/orders/{id}")
	public String showOrderDetails(@PathVariable Long id,
			@RequestParam(name = "tab", defaultValue = "margin") String tab, Model model) {
		log.info("Order details requested: id={}, tab={}", id, tab);
		Order order = orderService.getForDetails(id);
		model.addAttribute("order", order);
		model.addAttribute("activeTab", tab);

		if ("margin".equals(tab)) {
			model.addAttribute("currencies", java.util.Arrays.asList(com.sybyl.trace.order.CurrencyCode.values()));
			model.addAttribute("verticals", verticals.findAll());
			model.addAttribute("marginReports", marginReportService.listForOrder(id));
			model.addAttribute("expenseLabels", additionalExpenseLabelService.listActive());
			model.addAttribute("additionalExpenses", additionalExpenseService.listForOrder(id));
			model.addAttribute("scriptJsp",  "orders/detail/maargin-script.jsp");
		}

		model.addAttribute("pageTitle", "Order #" + order.getSalesOrderId());
		

		model.addAttribute("contentJsp", "orders/detail/order_detail.jsp");
		

		return "layout";
	}

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

		marginReportService.save(id, me.getUser(), file, label, buyingCurrency, sellingCurrency, buyingPrice,
				sellingPrice, conversionRate, verticalId, comments);

		ra.addFlashAttribute("message", "Margin report uploaded.");
		return "redirect:/orders/" + id + "?tab=margin";
	}

	@GetMapping("/orders/{orderId}/margin-reports/{mrId}/download")
	public void downloadMarginReport(@PathVariable Long orderId, @PathVariable Long mrId, HttpServletResponse response)
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
			log.error("Download failed: orderId={}, mrId={}, path={}", orderId, mrId, filePath, ex);
			throw ex;
		}
	}



	@PostMapping("/orders/{orderId}/margin-reports/{mrId}/delete")
	public String deleteMarginReport(@AuthenticationPrincipal com.sybyl.trace.security.MyUserDetails me,
			@PathVariable Long orderId, @PathVariable Long mrId,
			@RequestParam(value = "deleteFile", defaultValue = "true") boolean deleteFile, RedirectAttributes ra)
			throws IOException {
		marginReportService.delete(orderId, mrId, me.getUser(), deleteFile);
		ra.addFlashAttribute("message", "Margin report deleted.");
		return "redirect:/orders/" + orderId + "?tab=margin";
	}

	@PostMapping("/orders/{orderId}/margin-reports/{mrId}/update")
	public String updateMarginReport(@AuthenticationPrincipal com.sybyl.trace.security.MyUserDetails me,
			@PathVariable Long orderId, @PathVariable Long mrId, @RequestParam BigDecimal buyingPrice,
			@RequestParam CurrencyCode buyingCurrency, @RequestParam BigDecimal sellingPrice,
			@RequestParam CurrencyCode sellingCurrency, @RequestParam BigDecimal conversionRate,
			@RequestParam Long verticalId, @RequestParam(required = false) String comments, RedirectAttributes ra) {
		marginReportService.update(orderId, mrId, me.getUser(), buyingPrice, buyingCurrency, sellingPrice,
				sellingCurrency, conversionRate, verticalId, comments);
		ra.addFlashAttribute("message", "Margin report updated.");
		return "redirect:/orders/" + orderId + "?tab=margin";
	}

}
