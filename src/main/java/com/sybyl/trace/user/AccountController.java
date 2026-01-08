package com.sybyl.trace.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.web.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AppUserRepository users,
                             PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/account/change-password")
    public String changePasswordForm(@AuthenticationPrincipal MyUserDetails me,
                                     Model model) {

        log.info("Change password form requested by user={}", me.getUsername());

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ChangePasswordForm());
        }

        model.addAttribute("pageTitle", "Change Password");
        model.addAttribute("contentJsp", "account/change-password.jsp");
        return "layout";
    }

    @PostMapping("/account/change-password")
    public String changePassword(@AuthenticationPrincipal MyUserDetails me,
                                 @Valid @ModelAttribute("form") ChangePasswordForm form,
                                 BindingResult errors,
                                 Model model,
                                 RedirectAttributes ra,
                                 HttpServletRequest request) {

        String actorIp = IpUtils.getClientIp(request);

        log.info("Change password submitted by user={}, ip={}", me.getUsername(), actorIp);

        if (errors.hasErrors()) {
            model.addAttribute("pageTitle", "Change Password");
            model.addAttribute("contentJsp", "account/change-password.jsp");
            return "layout";
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch",
                    "New password and confirmation do not match.");
        }

        AppUser user = users.findById(me.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            errors.rejectValue("currentPassword", "password.invalid",
                    "Current password is incorrect.");
        }

        if (errors.hasErrors()) {
            model.addAttribute("pageTitle", "Change Password");
            model.addAttribute("contentJsp", "account/change-password.jsp");
            return "layout";
        }

        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        users.save(user);

       

        log.info("Password changed for user={}, ip={}", me.getUsername(), actorIp);
        ra.addFlashAttribute("message", "Your password has been changed successfully.");
        return "redirect:/account/change-password";
    }
}
