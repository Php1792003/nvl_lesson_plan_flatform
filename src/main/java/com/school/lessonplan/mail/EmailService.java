package com.school.lessonplan.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final ObjectMapper objectMapper;
    private final EmailOutboxProcessor emailOutboxProcessor;

    public EmailService(EmailOutboxRepository emailOutboxRepository,
            ObjectMapper objectMapper,
            @Lazy EmailOutboxProcessor emailOutboxProcessor) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.objectMapper = objectMapper;
        this.emailOutboxProcessor = emailOutboxProcessor;
    }

    /**
     * Đưa email vào outbox và kích hoạt gửi ngay sau khi transaction commit
     */
    @Transactional
    public void queueEmail(String toEmail, String subject, String template, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            EmailOutbox outbox = new EmailOutbox();
            outbox.setToEmail(toEmail);
            outbox.setSubject(subject);
            outbox.setTemplate(template);
            outbox.setPayloadJson(payloadJson);
            outbox.setStatus("PENDING");
            emailOutboxRepository.save(outbox);

            // Gửi ngay lập tức sau khi transaction DB commit thành công (không cần chờ
            // scheduled 30s)
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailOutboxProcessor.triggerImmediateProcessing();
                    }
                });
            } else {
                emailOutboxProcessor.triggerImmediateProcessing();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize email payload", e);
        }
    }

    /**
     * Tiện ích: Gửi mã OTP xác thực email khi đăng ký
     */
    public void sendOtpVerification(String toEmail, String fullName, String code) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fullName);
        payload.put("code", code);
        queueEmail(toEmail, "Mã xác thực email — THCS Nguyễn Văn Linh", "mail/verify-email", payload);
    }

    /**
     * Tiện ích: Gửi mã OTP quên mật khẩu
     */
    public void sendPasswordResetOtp(String toEmail, String fullName, String code) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fullName);
        payload.put("code", code);
        queueEmail(toEmail, "Mã đặt lại mật khẩu — THCS Nguyễn Văn Linh", "mail/reset-password", payload);
    }

    /**
     * Tiện ích: Thông báo BGH duyệt tài khoản giáo viên
     */
    public void sendAccountApproved(String toEmail, String fullName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fullName);
        queueEmail(toEmail, "Tài khoản của bạn đã được kích hoạt — THCS Nguyễn Văn Linh", "mail/account-approved",
                payload);
    }

    /**
     * Tiện ích: Thông báo giáo án được chia sẻ
     */
    public void sendPlanShared(String toEmail, String recipientName, String planTitle, String granterName, Long planId,
            String expiresAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientName", recipientName);
        payload.put("planTitle", planTitle);
        payload.put("granterName", granterName);
        payload.put("sharerName", granterName);
        payload.put("planId", planId);
        payload.put("expiresAt", expiresAt);
        queueEmail(toEmail, "Giáo án được chia sẻ: " + planTitle, "mail/plan-shared", payload);
    }

    /**
     * Tiện ích: Thông báo nhận xét mới từ BGH
     */
    public void sendNewEvaluation(String toEmail, String ownerName, String planTitle, String reviewerName, Long planId,
            String content, int versionNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ownerName", ownerName);
        payload.put("reviewerName", reviewerName);
        payload.put("planTitle", planTitle);
        payload.put("versionNo", versionNo);
        payload.put("content", content);
        payload.put("planId", planId);
        queueEmail(toEmail, "Nhận xét mới cho giáo án: " + planTitle, "mail/new-evaluation", payload);
    }

    /**
     * Tiện ích: Thông báo giáo viên phản hồi nhận xét
     */
    public void sendEvaluationReply(String toEmail, String reviewerName, String planTitle, String replierName,
            Long planId, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reviewerName", reviewerName);
        payload.put("authorName", replierName);
        payload.put("replierName", replierName);
        payload.put("planTitle", planTitle);
        payload.put("content", content);
        payload.put("planId", planId);
        queueEmail(toEmail, "Phản hồi nhận xét giáo án: " + planTitle, "mail/evaluation-reply", payload);
    }
}
