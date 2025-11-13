package com.sybyl.trace.masterdata;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sybyl.trace.web.SearchForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/customers")
public class CustomerAdminController {
	private final CustomerService service;

	public CustomerAdminController(CustomerService service) {
		this.service = service;
	}

	@GetMapping
	public String list(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size, Model model) {
		var result = service.search(q, page, size);
		model.addAttribute("page", result); // org.springframework.data.domain.Page
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("size", size);
		model.addAttribute("pageTitle", "Master Data · Users");
		model.addAttribute("contentJsp", "admin/customers/customerlist.jsp");
		model.addAttribute("customer", new Customer());
		return "layout";
	}

	@ModelAttribute("search")
	public SearchForm search(@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "size", required = false) Integer size) {
		return new SearchForm(q, size == null ? 10 : size);
	}
	

	  @GetMapping(value="/table", produces="text/html")
	  public String table(@RequestParam(value="q", required=false) String q,
	                      @RequestParam(value="page", defaultValue="0") int page,
	                      @RequestParam(value="size", defaultValue="10") int size,
	                      Model model) {
	    var result = service.search(q, page, size);
	    model.addAttribute("page", result);
	    return "admin/customers/_table";
	  }

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("customer", new Customer());
		model.addAttribute("pageTitle", "Master Data · Users · Create");
		model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
		return "layout";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("customer") Customer customer, BindingResult errors) {
		if (errors.hasErrors())
			return "admin/customers/customerform";
		try {
			service.create(customer);
		} catch (IllegalArgumentException e) {
			errors.rejectValue("name", "name.exists", e.getMessage());
			return "admin/customers/customerform";
		}
		return "redirect:/admin/customers?created";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("customer", service.get(id));
		model.addAttribute("pageTitle", "Master Data · Users · Edit");
		model.addAttribute("contentJsp", "admin/customers/customerform.jsp");
		return "layout";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Valid @ModelAttribute("customer") Customer customer,
			BindingResult errors) {
		if (errors.hasErrors())
			return "admin/customers/customerform";
		try {
			service.update(id, customer);
		} catch (IllegalArgumentException e) {
			errors.rejectValue("name", "name.exists", e.getMessage());
			return "admin/customers/form";
		}
		return "redirect:/admin/customers?updated";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id) {
		service.delete(id);
		return "redirect:/admin/customers?deleted";
	}
}
