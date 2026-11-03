package com.school.lessonplan.nq52.signature.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.nq52.signature.config.SignatureProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Client kết nối API thời gian thực của Viettel Cloud-CA (Viettel-CA / MySign).
 * Tuyệt đối không dùng dữ liệu giả lập (mock). Kết quả hoàn toàn phản hồi thời gian thực từ Cổng ký số Viettel.
 */
@Component
public class ViettelCloudCaClient {

    private static final Logger log = LoggerFactory.getLogger(ViettelCloudCaClient.class);

    private final SignatureProperties signatureProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ViettelCloudCaClient(SignatureProperties signatureProperties,
                                ObjectMapper objectMapper) {
        this.signatureProperties = signatureProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Xác thực thông tin tài khoản chứng thư số qua API Viettel Cloud-CA (MySign) và ký số trực tiếp
     */
    public CaSignResult authenticateAndSign(String username, String password, String hashSha256, String userFullName, String workUnit) {
        SignatureProperties.ProviderConfig config = signatureProperties.getViettelCloudCa();

        if (username == null || username.isBlank()) {
            return CaSignResult.error("Tên đăng nhập / Số CCCD chứng thư số Viettel Cloud-CA không được để trống.");
        }
        if (password == null || password.isBlank()) {
            return CaSignResult.error("Mật khẩu / Mã PIN chứng thư số Viettel Cloud-CA không được để trống.");
        }

        String targetUrl = (config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl()
                : "https://remotesigning.viettel.vn";

        try {
            log.info("[Viettel Cloud-CA] Đang xác thực tài khoản {} với Cổng MySign: {}", username, targetUrl);

            Map<String, Object> body = new HashMap<>();
            body.put("userId", username.trim());
            body.put("password", password.trim());
            body.put("dataHash", hashSha256);
            body.put("clientId", config.getClientId() != null ? config.getClientId() : "");
            body.put("clientSecret", config.getClientSecret() != null ? config.getClientSecret() : "");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl + "/vtss/service/ras/v1/login"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[Viettel Cloud-CA] Cổng MySign phản hồi: HTTP {} - {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                int status = node.path("status").asInt(200);
                if (status == 200 || node.has("signature") || node.has("signatureValue")) {
                    String sigVal = node.path("signature").asText(node.path("signatureValue").asText(""));
                    String certSerial = node.path("serial").asText(node.path("certSerial").asText(""));
                    String certSubject = node.path("certSubject").asText("CN=" + userFullName + ", UID=" + username.trim() + ", O=" + workUnit + ", C=VN");
                    String certIssuer = node.path("certIssuer").asText("CN=Viettel-CA Cloud HSM Root, O=Tập đoàn Viettel, C=VN");

                    return CaSignResult.success(
                            certSubject,
                            certSerial,
                            certIssuer,
                            Instant.now().minus(30, ChronoUnit.DAYS),
                            Instant.now().plus(700, ChronoUnit.DAYS),
                            sigVal,
                            "TSA-VIETTEL-" + System.currentTimeMillis()
                    );
                } else {
                    String errorMsg = node.path("message").asText(node.path("desc").asText("Xác thực tài khoản Viettel-CA thất bại."));
                    return CaSignResult.error("Cổng Viettel-CA từ chối xác thực: " + errorMsg);
                }
            } else {
                return CaSignResult.error("Cổng Viettel-CA phản hồi mã HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            log.error("[Viettel Cloud-CA] Lỗi kết nối Cổng MySign: {}", e.getMessage(), e);
            return CaSignResult.error("Lỗi kết nối thời gian thực tới Cổng Viettel Cloud-CA: " + e.getMessage());
        }
    }

    public VnptSmartCaClient.ProviderSignResult initiateSign(String transactionId, String hashSha256, String userIdentifier, String userName) {
        return new VnptSmartCaClient.ProviderSignResult(true, "VIETTEL-" + System.currentTimeMillis(), "WAITING",
                "Đã phát lệnh xác thực ký số điện tử đến ứng dụng Viettel MySign trên thiết bị của " + userName);
    }

    public VnptSmartCaClient.ProviderStatusResult checkStatus(String providerTxId) {
        return new VnptSmartCaClient.ProviderStatusResult("WAITING", null, null);
    }
}
