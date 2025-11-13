package com.sybyl.trace.masterdata;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/expenses")
public class AdditionalExpenseLabelController {

  private final AdditionalExpenseLabelService svc;
  private final AdditionalExpenseLabelRepository repo;

  @GetMapping
  public String list(@RequestParam(defaultValue="true") boolean showInactive, Model m) {

	  m.addAttribute("labels",
		      showInactive ? repo.findAllForAdmin() : repo.findAllActive());
    m.addAttribute("pageTitle", "Additional Expense Labels");
    m.addAttribute("contentJsp", "admin/expenses.jsp");
    return "layout";
  }

  @PostMapping("/create")
  public String create(@RequestParam String name,
                       @RequestParam(required = false) String description) {
    svc.create(name, description, false);
    return "redirect:/admin/expenses";
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
    svc.deactivate(id);
    ra.addFlashAttribute("message", "Label deactivated.");
    return "redirect:/admin/expenses?showInactive=true";
  }

  @PostMapping("/{id}/reactivate")
  public String reactivate(@PathVariable Long id, RedirectAttributes ra) {
    svc.reactivate(id);
    ra.addFlashAttribute("message", "Label reactivated.");
    return "redirect:/admin/expenses";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id) {
    svc.delete(id);
    return "redirect:/admin/expenses";
  }
}
