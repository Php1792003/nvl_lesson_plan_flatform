package com.school.lessonplan.audit;

import com.school.lessonplan.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String action, String targetType, Long targetId, String detail, HttpServletRequest request) {
        AuditLog log = new AuditLog();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser currentUser) {
            log.setActorUserId(currentUser.getId());
            log.setActorEmailSnapshot(currentUser.getUsername());
        }

        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);

        if (request != null) {
            log.setIp(getClientIp(request));
        }

        auditLogRepository.save(log);
    }

    @Transactional
    public void log(String action, String targetType, Long targetId, String detail) {
        log(action, targetType, targetId, detail, null);
    }

    /**
     * Ghi audit log khi actor đã bị xoá (dùng snapshot)
     */
    @Transactional
    public void logWithSnapshot(Long actorId, String actorEmail, String action,
                                String targetType, Long targetId, String detail, String ip) {
        AuditLog log = new AuditLog();
        log.setActorUserId(actorId);
        log.setActorEmailSnapshot(actorEmail);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(ip);
        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
