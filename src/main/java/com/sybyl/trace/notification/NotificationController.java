package com.sybyl.trace.notification;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sybyl.trace.security.MyUserDetails;
import com.sybyl.trace.user.AppUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // HTML page: /notifications
    @GetMapping
    public String list(@AuthenticationPrincipal MyUserDetails me,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(value = "filter", required = false, defaultValue = "all")
                           String filter,
                       Model model) {

        AppUser user = me.getUser();
        boolean unreadOnly = "unread".equalsIgnoreCase(filter);

        Page<Notification> pageResult =
                notificationService.pageForUser(user, page, size, unreadOnly);

        model.addAttribute("notifications", pageResult.getContent());
        model.addAttribute("page", pageResult.getNumber());
        model.addAttribute("size", pageResult.getSize());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("filter", filter);

        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("contentJsp", "notification/list.jsp");

        log.debug("Notifications page: user={}, page={}, size={}, filter={}, totalPages={}",
                user.getUsername(), page, size, filter, pageResult.getTotalPages());

        return "layout";
    }

    // Small JSON for recent (e.g. dropdown)
    @GetMapping("/api/recent")
    @ResponseBody
    public List<NotificationDto> apiRecent(@AuthenticationPrincipal MyUserDetails me) {
        AppUser user = me.getUser();
        return notificationService.listForUser(user).stream()
                .map(NotificationDto::from)
                .toList();
    }

    @GetMapping("api/unread-count")
    @ResponseBody
    public Map<String, Long> unreadCount(@AuthenticationPrincipal MyUserDetails me) {
        long c = notificationService.unreadCount(me.getUser());
        return java.util.Map.of("count", c);
    }

    @GetMapping("/api/list")
    @ResponseBody
    public List<NotificationDto> apiList(@AuthenticationPrincipal MyUserDetails me) {
        AppUser user = me.getUser();
        return notificationService.listForUser(user).stream()
                .map(NotificationDto::from)
                .toList();
    }

    @PostMapping("/api/mark-all-read")
    @ResponseBody
    public void markAllRead(@AuthenticationPrincipal MyUserDetails me) {
        notificationService.markAllAsRead(me.getUser());
    }
}
