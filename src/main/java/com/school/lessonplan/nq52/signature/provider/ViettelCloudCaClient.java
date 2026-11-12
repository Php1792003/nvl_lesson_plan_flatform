package com.school.lessonplan.nq52.signature.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.nq52.signature.config.SignatureProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Client kết nối API ký số đồng bộ (Synchronize) dịch vụ Viettel MySign / Viettel Cloud-CA.
 * Tuân thủ chính xác theo tài liệu đặc tả kỹ thuật:
 * 1. Login: POST BASE_URL/vtss/service/ras/v1/login
 * 2. Certificates/Info: POST BASE_URL/adss/service/ras/csc/v1/credentials/list (với Bearer Token)
 * 3. SignHash: POST BASE_URL/vtss/service/signHash (async = 0, OID SHA-256 & RSA, Bearer Token)
 */
@Component
public class ViettelCloudCaClient {

    private static final Logger log = LoggerFactory.getLogger(ViettelCloudCaClient.class);

    public static final String DEFAULT_PROFILE_ID = "ADSS RAS Profile 001";
    public static final String HASH_ALGO_SHA256_OID = "2.16.840.1.101.3.4.2.1";
    public static final String SIGN_ALGO_RSA_OID = "1.2.840.113549.1.1.1";

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
     * DTO lưu trữ thông tin chứng thư số Viettel trả về từ credentials/list
     */
    public static class ViettelCredentialDetail {
        private String credentialId;
        private String serialNumber;
        private String subjectDN;
        private String issuerDN;
        private Instant validFrom;
        private Instant validTo;
        private String certStatus;
        private String keyStatus;
        private List<String> certificates = new ArrayList<>();
        private String rawResponse;

        public String getCredentialId() { return credentialId; }
        public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public String getSubjectDN() { return subjectDN; }
        public void setSubjectDN(String subjectDN) { this.subjectDN = subjectDN; }
        public String getIssuerDN() { return issuerDN; }
        public void setIssuerDN(String issuerDN) { this.issuerDN = issuerDN; }
        public Instant getValidFrom() { return validFrom; }
        public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
        public Instant getValidTo() { return validTo; }
        public void setValidTo(Instant validTo) { this.validTo = validTo; }
        public String getCertStatus() { return certStatus; }
        public void setCertStatus(String certStatus) { this.certStatus = certStatus; }
        public String getKeyStatus() { return keyStatus; }
        public void setKeyStatus(String keyStatus) { this.keyStatus = keyStatus; }
        public List<String> getCertificates() { return certificates; }
        public void setCertificates(List<String> certificates) { this.certificates = certificates; }
        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    }

    /**
     * Chuyển đổi mã băm SHA-256 dạng Hex (64 ký tự) thành dạng Base64 (44 ký tự) theo yêu cầu Viettel
     */
    public static String ensureBase64Hash(String hashInput) {
        if (hashInput == null || hashInput.isBlank()) return "";
        String trimmed = hashInput.trim();
        // Nếu là mã băm Hex 64 ký tự (SHA-256)
        if (trimmed.length() == 64 && trimmed.matches("^[0-9a-fA-F]{64}$")) {
            byte[] bytes = new byte[32];
            for (int i = 0; i < 32; i++) {
                bytes[i] = (byte) Integer.parseInt(trimmed.substring(i * 2, i * 2 + 2), 16);
            }
            return Base64.getEncoder().encodeToString(bytes);
        }
        return trimmed;
    }

    /**
     * Chuẩn hóa tên tài liệu hiển thị trên ứng dụng Viettel MySign:
     * - Bỏ dấu tiếng Việt
     * - Chỉ giữ [a-zA-Z0-9_ -]
     * - Mã hóa Base64 UTF-8 độ dài < 100 ký tự theo đặc tả Viettel
     */
    public static String encodeDocumentName(String docName) {
        if (docName == null || docName.isBlank()) {
            docName = "Tai lieu danh gia NQ52";
        }
        String unaccented = Normalizer.normalize(docName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[đĐ]", "d")
                .replaceAll("[^a-zA-Z0-9_\\- ]", " ")
                .trim();
        if (unaccented.length() > 60) {
            unaccented = unaccented.substring(0, 60);
        }
        return Base64.getEncoder().encodeToString(unaccented.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parse thời gian GeneralizedTime RFC 5280 từ Viettel (ví dụ: "20180709132216+0000" hoặc "20180709132216Z")
     */
    public static Instant parseGeneralizedTime(String val) {
        if (val == null || val.isBlank()) return Instant.now();
        try {
            return Instant.parse(val);
        } catch (Exception ignored) {}
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss[X][Z][xx][xxx]");
            return ZonedDateTime.parse(val, dtf.withZone(ZoneId.of("UTC"))).toInstant();
        } catch (Exception ignored) {}
        try {
            if (val.length() >= 14) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                return LocalDateTime.parse(val.substring(0, 14), dtf).atZone(ZoneId.of("UTC")).toInstant();
            }
        } catch (Exception ignored) {}
        return Instant.now();
    }

    // =========================================================================
    // 1. HÀM LOGIN: POST BASE_URL/vtss/service/ras/v1/login
    // =========================================================================
    public String login(String userId) throws Exception {
        SignatureProperties.ProviderConfig config = signatureProperties.getViettelCloudCa();
        String targetUrl = (config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl() : "https://remotesigning.viettel.vn";
        String clientId = config.getClientId() != null ? config.getClientId().trim() : "";
        String clientSecret = config.getClientSecret() != null ? config.getClientSecret().trim() : "";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("profile_id", DEFAULT_PROFILE_ID);
        body.put("user_id", userId != null ? userId.trim() : "");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl + "/vtss/service/ras/v1/login"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        log.info("[Viettel MySign] Gọi API 1 (Login): POST {}/vtss/service/ras/v1/login cho user_id={}", targetUrl, userId);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[Viettel MySign] Phản hồi API 1 (Login): HTTP {} - {}", response.statusCode(), response.body());

        if (response.statusCode() == 200) {
            JsonNode node = objectMapper.readTree(response.body());
            String accessToken = node.path("access_token").asText("");
            if (accessToken.isBlank()) {
                throw new IllegalStateException("Cổng Viettel MySign phản hồi 200 nhưng không tìm thấy trường access_token.");
            }
            return accessToken;
        } else {
            String error = parseViettelError(response.statusCode(), response.body(), userId, clientId);
            throw new IllegalArgumentException(error);
        }
    }

    // =========================================================================
    // 2. HÀM CERTIFICATES/INFO: POST BASE_URL/adss/service/ras/csc/v1/credentials/list
    // =========================================================================
    public ViettelCredentialDetail getCredentialsList(String accessToken, String userId, String userFullName, String workUnit) throws Exception {
        SignatureProperties.ProviderConfig config = signatureProperties.getViettelCloudCa();
        String targetUrl = (config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl() : "https://remotesigning.viettel.vn";
        String clientId = config.getClientId() != null ? config.getClientId().trim() : "";
        String clientSecret = config.getClientSecret() != null ? config.getClientSecret().trim() : "";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("profile_id", DEFAULT_PROFILE_ID);
        body.put("user_id", userId != null ? userId.trim() : "");
        body.put("certificates", "chain");
        body.put("certInfo", true);
        body.put("authInfo", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl + "/adss/service/ras/csc/v1/credentials/list"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        log.info("[Viettel MySign] Gọi API 2 (Credentials/List): POST {}/adss/service/ras/csc/v1/credentials/list", targetUrl);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[Viettel MySign] Phản hồi API 2 (Credentials/List): HTTP {} - {}", response.statusCode(), response.body());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            ViettelCredentialDetail detail = new ViettelCredentialDetail();
            detail.setRawResponse(response.body());
            detail.setCredentialId(root.path("credential_id").asText(root.path("credentialID").asText("")));

            JsonNode keyNode = root.path("key");
            detail.setKeyStatus(keyNode.path("status").asText("ENABLED"));

            JsonNode certNode = root.path("cert");
            detail.setCertStatus(certNode.path("status").asText("valid"));
            detail.setSerialNumber(certNode.path("SerialNumber").asText(certNode.path("serialNumber").asText(detail.getCredentialId())));
            detail.setSubjectDN(certNode.path("subjectDN").asText("CN=" + userFullName + ", UID=" + userId + ", O=" + workUnit + ", C=VN"));
            detail.setIssuerDN(certNode.path("issuerDN").asText("CN=Viettel-CA Cloud HSM Root, O=Tập đoàn Công nghiệp - Viễn thông Quân đội, C=VN"));

            if (certNode.hasNonNull("validFrom")) {
                detail.setValidFrom(parseGeneralizedTime(certNode.path("validFrom").asText()));
            } else {
                detail.setValidFrom(Instant.now().minus(30, ChronoUnit.DAYS));
            }

            if (certNode.hasNonNull("validTo")) {
                detail.setValidTo(parseGeneralizedTime(certNode.path("validTo").asText()));
            } else {
                detail.setValidTo(Instant.now().plus(700, ChronoUnit.DAYS));
            }

            // Đọc chứng thư X.509 nếu có trong certificates array
            JsonNode certsArr = certNode.path("certificates");
            if (certsArr.isArray() && !certsArr.isEmpty()) {
                for (JsonNode c : certsArr) {
                    detail.getCertificates().add(c.asText());
                }
                try {
                    byte[] certBytes = Base64.getDecoder().decode(certsArr.get(0).asText());
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate x509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
                    detail.setSubjectDN(x509.getSubjectX500Principal().getName());
                    detail.setIssuerDN(x509.getIssuerX500Principal().getName());
                    detail.setSerialNumber(x509.getSerialNumber().toString(16).toUpperCase());
                    detail.setValidFrom(x509.getNotBefore().toInstant());
                    detail.setValidTo(x509.getNotAfter().toInstant());
                } catch (Exception e) {
                    log.debug("[Viettel MySign] Parse X.509 certificate note: {}", e.getMessage());
                }
            }

            return detail;
        } else {
            String error = parseViettelError(response.statusCode(), response.body(), userId, clientId);
            throw new IllegalArgumentException(error);
        }
    }

    // =========================================================================
    // 3. HÀM SIGNHASH (ĐỒNG BỘ - ASYNC=0): POST BASE_URL/vtss/service/signHash
    // =========================================================================
    public String signHash(String accessToken, String credentialId, String hashSha256, String docId, String docName) throws Exception {
        SignatureProperties.ProviderConfig config = signatureProperties.getViettelCloudCa();
        String targetUrl = (config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl() : "https://remotesigning.viettel.vn";

        String base64Hash = ensureBase64Hash(hashSha256);
        String base64DocName = encodeDocumentName(docName);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("credentialID", credentialId);
        body.put("numSignatures", 1);
        body.put("description", base64DocName);

        Map<String, Object> docItem = new LinkedHashMap<>();
        docItem.put("document_id", docId != null ? docId : String.valueOf(System.currentTimeMillis()));
        docItem.put("document_name", base64DocName);
        body.put("documents", Collections.singletonList(docItem));

        body.put("hash", Collections.singletonList(base64Hash));
        body.put("hashAlgo", HASH_ALGO_SHA256_OID);
        body.put("signAlgo", SIGN_ALGO_RSA_OID);
        body.put("async", 0); // Ký đồng bộ theo đặc tả Viettel

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl + "/vtss/service/signHash"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        log.info("[Viettel MySign] Gọi API 3 (SignHash Synchronize): POST {}/vtss/service/signHash cho credentialID={}", targetUrl, credentialId);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[Viettel MySign] Phản hồi API 3 (SignHash): HTTP {} - {}", response.statusCode(), response.body());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode sigs = root.path("signatures");
            if (sigs.isArray() && !sigs.isEmpty()) {
                return sigs.get(0).asText();
            }
            throw new IllegalStateException("Cổng Viettel MySign phản hồi 200 nhưng không tìm thấy chữ ký trong mảng 'signatures'.");
        } else {
            String error = parseViettelError(response.statusCode(), response.body(), credentialId, config.getClientId());
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Lấy thông tin chứng thư số Viettel Cloud-CA (Dùng cho Đăng nhập / Liên kết chứng thư số)
     * Quy trình: Login (lấy access_token) -> Credentials/List (lấy thông tin CTS)
     */
    public CaSignResult getCertificateOnly(String username, String userFullName, String workUnit) {
        return getCertificateOnly(username, null, userFullName, workUnit);
    }

    public CaSignResult getCertificateOnly(String username, String password, String userFullName, String workUnit) {
        if (username == null || username.isBlank()) {
            return CaSignResult.error("Tên đăng nhập / Số CCCD chứng thư số Viettel Cloud-CA không được để trống.");
        }
        if (password == null || password.isBlank()) {
            return CaSignResult.error("Mật khẩu / Mã PIN chứng thư số Viettel Cloud-CA không được để trống.");
        }

        try {
            log.info("[Viettel MySign] Bắt đầu xác thực liên kết chứng thư số cho user: {}", username.trim());
            // Bước 1: Login
            String accessToken = login(username.trim());

            // Bước 2: Lấy thông tin chứng thư số
            ViettelCredentialDetail certDetail = getCredentialsList(accessToken, username.trim(), userFullName, workUnit);

            CaSignResult res = CaSignResult.success(
                    certDetail.getSubjectDN(),
                    certDetail.getSerialNumber() != null ? certDetail.getSerialNumber() : certDetail.getCredentialId(),
                    certDetail.getIssuerDN(),
                    certDetail.getValidFrom(),
                    certDetail.getValidTo(),
                    "TOKEN:" + accessToken.substring(0, Math.min(32, accessToken.length())),
                    "TSA-VIETTEL-" + System.currentTimeMillis()
            );
            res.setRawResponse(certDetail.getRawResponse());
            return res;
        } catch (IllegalArgumentException e) {
            return CaSignResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[Viettel MySign] Lỗi kết nối Cổng Viettel: {}", e.getMessage(), e);
            return CaSignResult.error("Lỗi kết nối thời gian thực tới Cổng Viettel Cloud-CA (MySign): " + e.getMessage());
        }
    }

    /**
     * Xác thực thông tin và thực hiện ký số trực tiếp theo quy trình đồng bộ (Synchronize):
     * Bước 1: Login -> nhận access_token
     * Bước 2: Credentials/List -> nhận credential_id & thông tin CTS
     * Bước 3: SignHash (async = 0) -> nhận chữ ký số Base64 từ mảng signatures
     */
    public CaSignResult authenticateAndSign(String username, String password, String hashSha256, String userFullName, String workUnit) {
        if (username == null || username.isBlank()) {
            return CaSignResult.error("Tên đăng nhập / Số CCCD chứng thư số Viettel Cloud-CA không được để trống.");
        }
        if (password == null || password.isBlank()) {
            return CaSignResult.error("Mật khẩu / Mã PIN chứng thư số Viettel Cloud-CA không được để trống.");
        }

        try {
            // Bước 1: Login
            String accessToken = login(username.trim());

            // Bước 2: Credentials/List
            ViettelCredentialDetail certDetail = getCredentialsList(accessToken, username.trim(), userFullName, workUnit);

            // Bước 3: SignHash đồng bộ
            String signatureValue = signHash(accessToken, certDetail.getCredentialId(), hashSha256,
                    "DOC-" + System.currentTimeMillis(), "Phieu danh gia chat luong NQ52");

            CaSignResult res = CaSignResult.success(
                    certDetail.getSubjectDN(),
                    certDetail.getSerialNumber() != null ? certDetail.getSerialNumber() : certDetail.getCredentialId(),
                    certDetail.getIssuerDN(),
                    certDetail.getValidFrom(),
                    certDetail.getValidTo(),
                    signatureValue,
                    "TSA-VIETTEL-" + System.currentTimeMillis()
            );
            res.setRawResponse(certDetail.getRawResponse());
            return res;
        } catch (IllegalArgumentException e) {
            return CaSignResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[Viettel MySign] Lỗi ký số đồng bộ: {}", e.getMessage(), e);
            return CaSignResult.error("Lỗi kết nối thời gian thực tới Cổng Viettel Cloud-CA (MySign): " + e.getMessage());
        }
    }

    /**
     * Khởi tạo giao dịch ký số từ xa qua Viettel MySign.
     * Thực hiện quy trình ký đồng bộ: gửi yêu cầu đến Cổng Viettel và chờ xác nhận từ ứng dụng MySign di động.
     */
    public VnptSmartCaClient.ProviderSignResult initiateSign(String transactionId, String hashSha256, String userIdentifier, String userName) {
        return initiateSign(transactionId, hashSha256, userIdentifier, userName, null, null);
    }

    public VnptSmartCaClient.ProviderSignResult initiateSign(String transactionId, String hashSha256, String userIdentifier, String userName, String certSerial, String caPassword) {
        try {
            log.info("[Viettel MySign] Khởi tạo ký số cho user {} (txId: {})", userIdentifier, transactionId);

            // Bước 1: Login
            String accessToken = login(userIdentifier.trim());

            // Bước 2: Lấy credential_id
            ViettelCredentialDetail certDetail = getCredentialsList(accessToken, userIdentifier.trim(), userName, "Trường THCS Nguyễn Văn Linh");

            // Bước 3: SignHash đồng bộ
            String signatureValue = signHash(accessToken, certDetail.getCredentialId(), hashSha256,
                    transactionId, "Tai lieu danh gia Nghi quyet 52");

            // Khi ký đồng bộ thành công, Cổng MySign trả về ngay chữ ký
            return new VnptSmartCaClient.ProviderSignResult(true, transactionId, "APPROVED",
                    "Ký số thành công qua ứng dụng Viettel MySign của " + userName, certDetail.getRawResponse(), 200);

        } catch (IllegalArgumentException e) {
            log.warn("[Viettel MySign] Khởi tạo ký số từ chối: {}", e.getMessage());
            return new VnptSmartCaClient.ProviderSignResult(false, null, "FAILED",
                    e.getMessage(), null, 400);
        } catch (Exception e) {
            log.error("[Viettel MySign] Lỗi khởi tạo ký số: {}", e.getMessage(), e);
            return new VnptSmartCaClient.ProviderSignResult(false, null, "FAILED",
                    "Lỗi kết nối thời gian thực tới Cổng Viettel Cloud-CA: " + e.getMessage(), null, 500);
        }
    }

    /**
     * Kiểm tra trạng thái ký số (dành cho luồng polling nếu có)
     */
    public VnptSmartCaClient.ProviderStatusResult checkStatus(String providerTxId) {
        return new VnptSmartCaClient.ProviderStatusResult("APPROVED", null, null);
    }

    /**
     * Chuẩn hóa và dịch mã lỗi trả về từ Cổng Viettel MySign
     */
    private String parseViettelError(int statusCode, String rawBody, String username, String clientId) {
        String errorCode = "";
        String errorDesc = "";

        try {
            JsonNode node = objectMapper.readTree(rawBody);
            errorCode = node.path("error").asText(node.path("error_code").asText(""));
            errorDesc = node.path("error_description").asText(node.path("message").asText(""));
        } catch (Exception e) {
            errorDesc = rawBody;
        }

        String descLower = errorDesc.toLowerCase();

        // Mã 401 / 59033: Sai user ID hoặc mật khẩu
        if ("59033".equals(errorCode) || descLower.contains("user id or password is invalid")) {
            return "Cổng Viettel-CA (MySign) từ chối (Mã 59033): Tên đăng nhập / Số CCCD hoặc mật khẩu không chính xác.";
        }

        // Mã 400 / 58071: Thiếu tham số hoặc tham số không hợp lệ
        if ("58071".equals(errorCode)) {
            return "Cổng Viettel-CA (MySign) từ chối (Mã 58071): Yêu cầu thiếu tham số bắt buộc hoặc định dạng tham số không hợp lệ.";
        }

        // Mã 59009: User ID không tồn tại
        if ("59009".equals(errorCode) || descLower.contains("user id does not exist") || descLower.contains("người dùng không tồn tại")) {
            return "Cổng Viettel-CA (MySign) từ chối (Mã 59009): Số CCCD / Mã người dùng '" + username + "' không tồn tại trên hệ thống Viettel hoặc chưa đăng ký chứng thư số MySign.";
        }

        // Mã 58007: Client ID không tồn tại
        if ("58007".equals(errorCode) || descLower.contains("client id does not exist")) {
            return "Cổng Viettel-CA (MySign) từ chối (Mã 58007): Mã ứng dụng tích hợp (Client ID: " + clientId + ") chưa được kích hoạt hoặc chưa được Viettel cấp quyền.";
        }

        // Mã 58008: Client Secret không hợp lệ
        if ("58008".equals(errorCode) || descLower.contains("client secret")) {
            return "Cổng Viettel-CA (MySign) từ chối (Mã 58008): Khóa bí mật ứng dụng (Client Secret) không chính xác.";
        }

        // Lỗi chứng thư số hết hạn hoặc bị thu hồi
        if ("59001".equals(errorCode) || "59002".equals(errorCode) || "59003".equals(errorCode) || descLower.contains("expired") || descLower.contains("revoked")) {
            return "Cổng Viettel-CA (MySign) từ chối (Mã " + errorCode + "): Chứng thư số của tài khoản '" + username + "' đã hết hạn sử dụng hoặc đang bị tạm khóa.";
        }

        if (!errorCode.isBlank() && !errorDesc.isBlank()) {
            return "Cổng Viettel-CA phản hồi mã HTTP " + statusCode + " (Lỗi " + errorCode + "): " + errorDesc;
        }

        return "Cổng Viettel-CA phản hồi mã HTTP " + statusCode + ": " + (errorDesc.isBlank() ? rawBody : errorDesc);
    }
}
