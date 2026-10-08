package com.school.lessonplan.mail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class EmailOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxProcessor.class);

    private final EmailOutboxRepository emailOutboxRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    private static final int[] BACKOFF_MINUTES = { 1, 5, 15, 60 };
    private static final int BATCH_SIZE = 20;

    public EmailOutboxProcessor(EmailOutboxRepository emailOutboxRepository,
                               JavaMailSender mailSender,
                               TemplateEngine templateEngine,
                               ObjectMapper objectMapper,
                               AppProperties appProperties) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    /**
     * Kích hoạt xử lý ngay lập tức trong background thread sau khi transaction commit
     */
    @Async
    public void triggerImmediateProcessing() {
        processPendingEmails();
    }

    /**
     * Quét định kỳ mỗi 30 giây để xử lý các email retry hoặc email chưa gửi
     */
    @Scheduled(fixedDelay = 30000)
    public void processPendingEmails() {
        List<EmailOutbox> pendingEmails = emailOutboxRepository.findPendingEmails(
                Instant.now(), PageRequest.of(0, BATCH_SIZE));

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.debug("Found {} pending email(s) in outbox to process", pendingEmails.size());

        for (EmailOutbox email : pendingEmails) {
            sendSingleEmail(email.getId());
        }
    }

    /**
     * Xử lý 1 email: Render và gửi mail NGOÀI Transaction DB để không lock DB connection.
     */
    public void sendSingleEmail(Long emailId) {
        EmailOutbox email = emailOutboxRepository.findById(emailId).orElse(null);
        if (email == null || !"PENDING".equals(email.getStatus())) {
            return;
        }

        try {
            Map<String, Object> variables = objectMapper.readValue(
                    email.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
            variables.put("baseUrl", appProperties.getBaseUrl());

            Context context = new Context();
            context.setVariables(variables);

            String htmlBody = templateEngine.process(email.getTemplate(), context);

            // Kiểm tra chế độ Dev Mode / Mock Mode để tối ưu chi phí và tiện lợi khi test local
            boolean isDevMode = appProperties.getMail().isDevMode() || smtpUsername == null || smtpUsername.isBlank();

            if (isDevMode) {
                logMockEmail(email, variables);
            } else {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String fromEmail = appProperties.getMail().getFrom();
                String senderName = appProperties.getMail().getSenderName();
                if (senderName != null && !senderName.isBlank()) {
                    helper.setFrom(fromEmail, senderName);
                } else {
                    helper.setFrom(fromEmail);
                }

                helper.setTo(email.getToEmail());
                helper.setSubject(email.getSubject());
                helper.setText(htmlBody, true);

                mailSender.send(message);
                log.info("Email sent successfully to {}", maskEmail(email.getToEmail()));
            }

            markSuccess(emailId);
        } catch (Exception e) {
            markFailure(emailId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long emailId) {
        emailOutboxRepository.findById(emailId).ifPresent(email -> {
            email.setStatus("SENT");
            email.setSentAt(Instant.now());
            email.setLastError(null);
            emailOutboxRepository.save(email);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(Long emailId, String errorMessage) {
        emailOutboxRepository.findById(emailId).ifPresent(email -> {
            int attempts = email.getAttempts() + 1;
            email.setAttempts(attempts);
            email.setLastError(errorMessage);

            if (attempts >= 5) {
                email.setStatus("FAILED");
                log.error("Email #{} to {} failed permanently after {} attempts: {}",
                        emailId, maskEmail(email.getToEmail()), attempts, errorMessage);
            } else {
                int backoffIdx = Math.min(attempts - 1, BACKOFF_MINUTES.length - 1);
                int delayMinutes = BACKOFF_MINUTES[backoffIdx];
                email.setNextAttemptAt(Instant.now().plus(delayMinutes, ChronoUnit.MINUTES));
                log.warn("Email #{} to {} failed (attempt {}). Retrying in {} minutes: {}",
                        emailId, maskEmail(email.getToEmail()), attempts, delayMinutes, errorMessage);
            }
            emailOutboxRepository.save(email);
        });
    }

    private void logMockEmail(EmailOutbox email, Map<String, Object> variables) {
        Object otpCode = variables.get("code");
        log.info("""
                
                ================================================================================
                [EMAIL DEV MOCK - TIẾT KIỆM CHI PHÍ / TEST OFFLINE]
                -> Đến      : {}
                -> Tiêu đề  : {}
                -> Mã OTP   : {}
                -> Template : {}
                ================================================================================
                """,
                email.getToEmail(),
                email.getSubject(),
                otpCode != null ? "[" + otpCode + "]" : "(Không có OTP)",
                email.getTemplate());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
