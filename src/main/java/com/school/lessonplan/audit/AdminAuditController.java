package com.school.lessonplan.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listAuditLogs(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "actorEmail", required = false) String actorEmail,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        String cleanAction = (action != null && !action.isBlank()) ? action.trim() : null;
        String cleanEmail = (actorEmail != null && !actorEmail.isBlank()) ? actorEmail.trim() : null;

        Page<AuditLog> logPage = auditLogRepository.searchLogs(
            cleanAction, cleanEmail, PageRequest.of(page, size)
        );

        model.addAttribute("logPage", logPage);
        model.addAttribute("action", action);
        model.addAttribute("actorEmail", actorEmail);

        return "admin/audit/list";
    }
}
