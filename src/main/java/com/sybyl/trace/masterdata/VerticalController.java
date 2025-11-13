package com.sybyl.trace.masterdata;

import org.springframework.security.access.prepost.PreAuthorize;
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
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/verticals")
@PreAuthorize("hasRole('ADMIN')")
public class VerticalController {

  private final VerticalService service;

  @GetMapping
  public String list(@RequestParam(value="q", required=false) String q,
                     @RequestParam(value="page", defaultValue="0") int page,
                     @RequestParam(value="size", defaultValue="10") int size,
                     Model model) {
      var result = service.search(q, page, size);
      model.addAttribute("page", result);
      model.addAttribute("pageTitle", "Master Data · Verticals");
      model.addAttribute("contentJsp", "admin/verticals/verticallist.jsp");
      model.addAttribute("items", service.findAll());
      model.addAttribute("search", new SearchForm(q, size));
      return "layout";
  }
  
  @GetMapping(value="/table", produces="text/html")
  public String table(@RequestParam(value="q", required=false) String q,
                      @RequestParam(value="page", defaultValue="0") int page,
                      @RequestParam(value="size", defaultValue="10") int size,
                      Model model) {
    var result = service.search(q, page, size);
    model.addAttribute("page", result);
    return "admin/verticals/_table";
  }

  @GetMapping("/new")
  public String createForm(Model model) {
    model.addAttribute("pageTitle", "New Vertical");
    model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
    model.addAttribute("vertical", new Vertical());
    model.addAttribute("mode", "create");
    return "layout";
  }

  @PostMapping
  public String create(@ModelAttribute("vertical") @Valid Vertical vertical,
                       BindingResult br, Model model) {
    if (br.hasErrors()) {
      model.addAttribute("pageTitle", "New Vertical");
      model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
      model.addAttribute("mode", "create");
      return "layout";
    }
    try {
      service.create(vertical);
      return "redirect:/admin/verticals?created=true";
    } catch (IllegalArgumentException ex) {
      br.rejectValue("name", "duplicate", ex.getMessage());
      model.addAttribute("pageTitle", "New Vertical");
      model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
      model.addAttribute("mode", "create");
      return "layout";
    }
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    model.addAttribute("pageTitle", "Edit Vertical");
    model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
    model.addAttribute("vertical", service.findById(id));
    model.addAttribute("mode", "edit");
    return "layout";
  }

  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @ModelAttribute("vertical") @Valid Vertical form,
                       BindingResult br, Model model) {
    if (br.hasErrors()) {
      model.addAttribute("pageTitle", "Edit Vertical");
      model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
      model.addAttribute("mode", "edit");
      return "layout";
    }
    try {
      service.update(id, form);
      return "redirect:/admin/verticals?updated=true";
    } catch (IllegalArgumentException ex) {
      br.rejectValue("name", "duplicate", ex.getMessage());
      model.addAttribute("pageTitle", "Edit Vertical");
      model.addAttribute("contentJsp", "admin/verticals/verticalform.jsp");
      model.addAttribute("mode", "edit");
      return "layout";
    }
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id) {
    service.delete(id);
    return "redirect:/admin/verticals?deleted=true";
  }
}
