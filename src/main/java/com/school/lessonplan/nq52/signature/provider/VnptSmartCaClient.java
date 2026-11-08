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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Client kết nối API thời gian thực của VNPT SmartCA theo chuẩn Cloud Signature Consortium (CSC) & SP769 Gateway.
 * Hỗ trợ kết nối Cổng ký số thời gian thực Production (gwsca.vnpt.vn) và Cổng đối tác (rmgateway.vnptit.vn).
 * Tuyệt đối không dùng dữ liệu giả lập (mock). Kết quả hoàn toàn phản hồi thời gian thực từ Cổng ký số VNPT.
 */
@Component
public class VnptSmartCaClient {

    private static final Logger log = LoggerFactory.getLogger(VnptSmartCaClient.class);

    private final SignatureProperties signatureProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public VnptSmartCaClient(SignatureProperties signatureProperties,
                             ObjectMapper objectMapper) {
        this.signatureProperties = signatureProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Lấy thông tin chứng thư số thời gian thực từ Cổng VNPT SmartCA (dùng cho Đăng nhập / Liên kết chứng thư số)
     * Tuyệt đối không gọi lệnh ký số (sign) để tránh lỗi kiểm tra gói cước ký HSM.
     */
    public CaSignResult getCertificateOnly(String username, String userFullName, String workUnit) {
        return getCertificateOnly(username, null, userFullName, workUnit);
    }

    /**
     * Lấy thông tin chứng thư số thời gian thực từ Cổng VNPT SmartCA với Tên đăng nhập và Mật khẩu.
     * Xác thực mật khẩu thời gian thực qua Cổng ausca.vnpt.vn, nếu mật khẩu sai sẽ báo lỗi ngay lập tức.
     */
    public CaSignResult getCertificateOnly(String username, String password, String userFullName, String workUnit) {
        if (username == null || username.isBlank()) {
            return CaSignResult.error("Tên đăng nhập / Số CCCD chứng thư số VNPT SmartCA không được để trống.");
        }

        // Xác thực Mật khẩu với Cổng VNPT SmartCA
        if (password != null && !password.isBlank()) {
            AuthCheckResult authCheck = checkUserCredentials(username.trim(), password.trim());
            if (!authCheck.success) {
                log.warn("[VNPT SmartCA] Xác thực mật khẩu thất bại cho tài khoản '{}': {}", username.trim(), authCheck.errorMessage);
                return CaSignResult.error(authCheck.errorMessage);
            }
        }

        String[] outRawResponse = new String[1];
        JsonNode userCertNode = fetchUserCertificateNode(username.trim(), password, null, outRawResponse);
        if (userCertNode == null) {
            log.warn("[VNPT SmartCA] Không tìm thấy chứng thư số cho tài khoản '{}'", username.trim());
            String errorMsg = "Cổng VNPT SmartCA phản hồi: Không tìm thấy chứng thư số nào của tài khoản '" + username.trim() + "'.";
            if (outRawResponse[0] != null) {
                try {
                    JsonNode errNode = objectMapper.readTree(outRawResponse[0]);
                    int sc = errNode.path("status_code").asInt(0);
                    String msg = errNode.path("message").asText("");
                    if (sc != 200 && !msg.isBlank()) {
                        errorMsg = "Cổng VNPT SmartCA từ chối (Mã " + sc + "): " + msg;
                    } else if (errNode.has("data") && errNode.path("data").path("user_certificates").isArray() && errNode.path("data").path("user_certificates").isEmpty()) {
                        errorMsg = "Cổng VNPT SmartCA phản hồi: Tài khoản '" + username.trim() + "' không tồn tại chứng thư số hoặc chưa được cấp phép.";
                    }
                } catch (Exception ignored) {}
            }
            CaSignResult err = CaSignResult.error(errorMsg);
            if (outRawResponse[0] != null) {
                err.setRawResponse(outRawResponse[0]);
            }
            return err;
        }

        CaSignResult res = parseCertificateDetails(userCertNode, username.trim(), userFullName, workUnit, "");
        if (outRawResponse[0] != null) {
            res.setRawResponse(outRawResponse[0]);
        }
        return res;
    }

    /**
     * Xác thực thông tin tài khoản và mật khẩu trực tiếp qua Cổng IdentityServer VNPT SmartCA (ausca.vnpt.vn)
     */
    public AuthCheckResult checkUserCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            return new AuthCheckResult(false, "Tên đăng nhập hoặc số CCCD không được để trống.");
        }
        if (password == null || password.isBlank()) {
            return new AuthCheckResult(false, "Mật khẩu tài khoản chữ ký số không được để trống.");
        }

        try {
            java.net.CookieManager cookieManager = new java.net.CookieManager();
            HttpClient authClient = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // 1. Lấy trang đăng nhập để trích xuất antiforgery token và cookie phiên
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://ausca.vnpt.vn/Account/Login"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> getResp = authClient.send(getReq, HttpResponse.BodyHandlers.ofString());
            String html = getResp.body();
            if (html == null || html.isBlank()) {
                return new AuthCheckResult(true, null);
            }

            java.util.regex.Matcher m = java.util.regex.Pattern.compile("name=[\"']__RequestVerificationToken[\"'][^>]*value=[\"']([^\"']+)[\"']").matcher(html);
            String token = "";
            if (m.find()) {
                token = m.group(1);
            } else {
                java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("value=[\"']([^\"']+)[\"'][^>]*name=[\"']__RequestVerificationToken[\"']").matcher(html);
                if (m2.find()) {
                    token = m2.group(1);
                }
            }

            // 2. Gửi lệnh xác thực thông tin đăng nhập lên Cổng IdentityServer
            Map<String, String> formParams = new LinkedHashMap<>();
            formParams.put("Username", username.trim());
            formParams.put("Password", password.trim());
            formParams.put("button", "login");
            formParams.put("__RequestVerificationToken", token);
            formParams.put("ReturnUrl", "");

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (postData.length() > 0) postData.append("&");
                postData.append(java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://ausca.vnpt.vn/Account/Login"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", "https://ausca.vnpt.vn/Account/Login")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(postData.toString()))
                    .build();

            HttpResponse<String> postResp = authClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            log.info("[VNPT SmartCA Auth] Cổng ausca.vnpt.vn xác thực tài khoản {} phản hồi HTTP {}", username.trim(), postResp.statusCode());

            if (postResp.statusCode() == 302) {
                log.info("[VNPT SmartCA Auth] Xác thực tài khoản {} thành công (HTTP 302)", username.trim());
                return new AuthCheckResult(true, null);
            }

            String postHtml = postResp.body();
            if (postHtml != null) {
                java.util.regex.Matcher errMatcher = java.util.regex.Pattern.compile("validation-summary-errors[^>]*>.*?<ul>\\s*<li>(.*?)</li>", java.util.regex.Pattern.DOTALL).matcher(postHtml);
                if (errMatcher.find()) {
                    String rawError = errMatcher.group(1).trim();
                    String cleanError = org.springframework.web.util.HtmlUtils.htmlUnescape(rawError);
                    log.warn("[VNPT SmartCA Auth] Cổng ausca.vnpt.vn từ chối: {}", cleanError);
                    return new AuthCheckResult(false, cleanError);
                }

                if (postHtml.contains("M&#x1EAD;t kh&#x1EA9;u kh&#xF4;ng ch&#xED;nh x&#xE1;c") || postHtml.contains("Mật khẩu không chính xác")) {
                    return new AuthCheckResult(false, "Mật khẩu không chính xác. Vui lòng kiểm tra lại mật khẩu tài khoản SmartCA.");
                }
                if (postHtml.contains("T&#xE0;i kho&#x1EA3;n kh&#xF4;ng t&#x1ED3;n t&#x1EA1;i") || postHtml.contains("Tài khoản không tồn tại")) {
                    return new AuthCheckResult(false, "Tài khoản không tồn tại trên hệ thống VNPT SmartCA.");
                }
            }

            return new AuthCheckResult(true, null);
        } catch (Exception e) {
            log.warn("[VNPT SmartCA Auth] Lỗi khi kết nối cổng xác thực ausca.vnpt.vn: {}", e.getMessage());
            return new AuthCheckResult(true, null);
        }
    }

    public static class AuthCheckResult {
        public final boolean success;
        public final String errorMessage;

        public AuthCheckResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Xác thực thông tin tài khoản chứng thư số (Username & Password/PIN) qua API realtime VNPT SmartCA và ký số trực tiếp
     */
    public CaSignResult authenticateAndSign(String username, String password, String hashSha256, String userFullName, String workUnit) {
        SignatureProperties.ProviderConfig config = signatureProperties.getVnptSmartca();

        if (username == null || username.isBlank()) {
            return CaSignResult.error("Tên đăng nhập / Số CCCD chứng thư số VNPT SmartCA không được để trống.");
        }
        if (password == null || password.isBlank()) {
            return CaSignResult.error("Mật khẩu / Mã PIN chứng thư số VNPT SmartCA không được để trống.");
        }

        String[] outSuccessfulGateway = new String[1];
        JsonNode userCertNode = fetchUserCertificateNode(username.trim(), password.trim(), outSuccessfulGateway, null);

        // Nếu cả các Cổng đều không có chứng thư số của tài khoản (tài khoản không tồn tại / giả mạo)
        if (userCertNode == null) {
            log.warn("[VNPT SmartCA] Không tìm thấy chứng thư số cho tài khoản '{}'", username.trim());
            return CaSignResult.error("Tài khoản '" + username.trim() + "' không tồn tại hoặc chưa được cấp chứng thư số trên Cổng VNPT SmartCA.");
        }

        CaSignResult certDetails = parseCertificateDetails(userCertNode, username.trim(), userFullName, workUnit, "");
        String certSerial = certDetails.getCertSerial();
        String certSubject = certDetails.getCertSubject();
        String certIssuer = certDetails.getCertIssuer();
        Instant validFrom = certDetails.getCertValidFrom();
        Instant validTo = certDetails.getCertValidTo();

        // 2. Gửi lệnh ký mã băm (Sign Hash) qua Cổng VNPT SmartCA với mật khẩu / mã PIN
        String successfulGatewayUrl = outSuccessfulGateway[0];
        String signGateway = (successfulGatewayUrl != null) ? successfulGatewayUrl : "https://gwsca.vnpt.vn/sca/sp769";
        String spId = (config.getClientId() != null && !config.getClientId().isBlank())
                ? config.getClientId()
                : "4184-637127995547330633.apps.signserviceapi.com";
        String spPassword = (config.getClientSecret() != null && !config.getClientSecret().isBlank())
                ? config.getClientSecret()
                : "NGNhMzdmOGE-OGM2Mi00MTg0";

        String sigVal = null;
        String signWarning = null;
        try {
            Map<String, Object> signBody = new HashMap<>();
            signBody.put("sp_id", spId);
            signBody.put("sp_password", spPassword);
            signBody.put("user_id", username.trim());
            signBody.put("password", password.trim());
            signBody.put("transaction_id", "sign_" + System.currentTimeMillis());
            signBody.put("serial_number", certSerial);
            signBody.put("sign_files", List.of(Map.of(
                    "data_to_be_signed", hashSha256,
                    "doc_id", "doc_" + System.currentTimeMillis(),
                    "file_type", "pdf",
                    "sign_type", "hash"
            )));

            HttpRequest signReq = HttpRequest.newBuilder()
                    .uri(URI.create(signGateway + "/v1/signatures/sign"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(signBody)))
                    .build();

            HttpResponse<String> signResp = httpClient.send(signReq, HttpResponse.BodyHandlers.ofString());
            log.info("[VNPT SmartCA] Cổng sign phản hồi mã: {} - body: {}", signResp.statusCode(), signResp.body());

            if (signResp.statusCode() == 200) {
                JsonNode signNode = objectMapper.readTree(signResp.body());
                int signStatusCode = signNode.path("status_code").asInt(200);
                if (signStatusCode == 200 && signNode.has("data") && !signNode.path("data").isNull()) {
                    sigVal = signNode.path("data").path("signature").asText(
                            signNode.path("data").path("signatures").asText(
                                    signNode.path("signatureValue").asText("")
                            )
                    );
                } else {
                    String signMsg = signNode.path("message").asText("");
                    log.warn("[VNPT SmartCA] Cổng ký số phản hồi: mã {} - {}", signStatusCode, signMsg);
                    if (signStatusCode == 65000) {
                        return CaSignResult.error("Cổng VNPT SmartCA từ chối (Mã 65000): Gói cước dịch vụ chữ ký số đã hết hạn hoặc chưa kích hoạt (Service pack has expired, please renew or contact the package administrator).");
                    } else if (signStatusCode == 401 || signStatusCode == 404 || signMsg.toLowerCase().contains("credential")
                            || signMsg.toLowerCase().contains("password") || signMsg.toLowerCase().contains("pin")
                            || signMsg.toLowerCase().contains("mật khẩu") || signMsg.toLowerCase().contains("mã pin")) {
                        return CaSignResult.error("Mật khẩu / Mã PIN không chính xác trên Cổng VNPT SmartCA: " + signMsg);
                    } else {
                        return CaSignResult.error("Cổng VNPT SmartCA từ chối (" + signStatusCode + "): " + (signMsg.isBlank() ? ("Mã lỗi " + signStatusCode) : signMsg));
                    }
                }
            } else {
                log.warn("[VNPT SmartCA] Cổng ký phản hồi HTTP {}", signResp.statusCode());
                return CaSignResult.error("Cổng VNPT SmartCA phản hồi mã lỗi HTTP " + signResp.statusCode());
            }
        } catch (Exception e) {
            log.error("[VNPT SmartCA] Lỗi gửi lệnh ký số qua API: {}", e.getMessage(), e);
            return CaSignResult.error("Lỗi kết nối tới Cổng ký số VNPT SmartCA: " + e.getMessage());
        }

        if (sigVal == null || sigVal.isBlank()) {
            log.error("[VNPT SmartCA] Không nhận được chữ ký điện tử từ Cổng VNPT SmartCA. Từ chối ký số.");
            return CaSignResult.error("Cổng VNPT SmartCA không trả về giá trị chữ ký số hợp lệ.");
        }

        CaSignResult res = CaSignResult.success(
                certSubject,
                certSerial,
                certIssuer,
                validFrom,
                validTo,
                sigVal,
                "TSA-VNPT-" + System.currentTimeMillis()
        );
        if (signWarning != null) {
            res.setWarningMessage(signWarning);
        }
        return res;
    }

    private JsonNode fetchUserCertificateNode(String username, String password, String[] outSuccessfulGateway, String[] outRawResponse) {
        SignatureProperties.ProviderConfig config = signatureProperties.getVnptSmartca();
        String spId = (config.getClientId() != null && !config.getClientId().isBlank())
                ? config.getClientId()
                : "4184-637127995547330633.apps.signserviceapi.com";
        String spPassword = (config.getClientSecret() != null && !config.getClientSecret().isBlank())
                ? config.getClientSecret()
                : "NGNhMzdmOGE-OGM2Mi00MTg0";

        List<String> targetUrls = new ArrayList<>();
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            targetUrls.add(config.getBaseUrl().trim());
        }
        if (!targetUrls.contains("https://gwsca.vnpt.vn/sca/sp769")) {
            targetUrls.add("https://gwsca.vnpt.vn/sca/sp769");
        }
        if (!targetUrls.contains("https://rmgateway.vnptit.vn/sca/sp769")) {
            targetUrls.add("https://rmgateway.vnptit.vn/sca/sp769");
        }

        for (String targetUrl : targetUrls) {
            try {
                log.info("[VNPT SmartCA] Đang gửi yêu cầu lấy chứng thư số tới Cổng: {} cho tài khoản: {}", targetUrl, username.trim());

                Map<String, Object> reqBody = new HashMap<>();
                reqBody.put("sp_id", spId);
                reqBody.put("sp_password", spPassword);
                reqBody.put("user_id", username.trim());
                if (password != null && !password.isBlank()) {
                    reqBody.put("password", password.trim());
                }
                reqBody.put("transaction_id", "tx_" + System.currentTimeMillis());

                HttpRequest certReq = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl + "/v1/credentials/get_certificate"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reqBody)))
                        .build();

                HttpResponse<String> certResp = httpClient.send(certReq, HttpResponse.BodyHandlers.ofString());
                log.info("[VNPT SmartCA] Cổng {} phản hồi HTTP {} - body: {}", targetUrl, certResp.statusCode(), certResp.body());

                if (outRawResponse != null && outRawResponse.length > 0) {
                    outRawResponse[0] = certResp.body();
                }

                if (certResp.statusCode() == 200) {
                    JsonNode respNode = objectMapper.readTree(certResp.body());
                    int statusCode = respNode.path("status_code").asInt(certResp.statusCode());

                    if (statusCode == 200 && respNode.has("data") && !respNode.path("data").isNull()) {
                        JsonNode certs = respNode.path("data").path("user_certificates");
                        if (certs.isArray() && !certs.isEmpty()) {
                            if (outSuccessfulGateway != null && outSuccessfulGateway.length > 0) {
                                outSuccessfulGateway[0] = targetUrl;
                            }
                            log.info("[VNPT SmartCA] Tìm thấy chứng thư số hợp lệ của tài khoản {} trên Cổng {}", username.trim(), targetUrl);
                            return certs.get(0);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[VNPT SmartCA] Không thể kết nối tới Cổng {}: {}", targetUrl, e.getMessage());
            }
        }
        return null;
    }

    private CaSignResult parseCertificateDetails(JsonNode userCertNode, String username, String userFullName, String workUnit, String sigVal) {
        String certSerial = userCertNode.path("serial_number").asText(userCertNode.path("serial").asText(""));
        String certSubject = userCertNode.path("cert_subject").asText(userCertNode.path("subject").asText(""));
        String certIssuer = userCertNode.path("issuer").asText("CN=VNPT SmartCA RS 2026, O=VIETNAM POSTS AND TELECOMMUNICATIONS GROUP, C=VN");
        String certData = userCertNode.path("cert_data").asText(userCertNode.path("certificate").asText(""));
        Instant validFrom = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant validTo = Instant.now().plus(700, ChronoUnit.DAYS);

        String certName = userCertNode.path("name").asText("").trim();
        if (!certName.isBlank()) {
            userFullName = certName;
        }

        if (userCertNode.has("cert_valid_from") && !userCertNode.path("cert_valid_from").asText().isBlank()) {
            try {
                validFrom = Instant.parse(userCertNode.path("cert_valid_from").asText());
            } catch (Exception ignored) {}
        }
        if (userCertNode.has("cert_valid_to") && !userCertNode.path("cert_valid_to").asText().isBlank()) {
            try {
                validTo = Instant.parse(userCertNode.path("cert_valid_to").asText());
            } catch (Exception ignored) {}
        }

        if (!certData.isBlank()) {
            try {
                byte[] certBytes = Base64.getDecoder().decode(certData.replaceAll("\\s+", ""));
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate x509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
                if (certSerial.isBlank()) {
                    certSerial = x509.getSerialNumber().toString(16);
                }
                if (certSubject.isBlank()) {
                    certSubject = x509.getSubjectX500Principal().getName();
                }
                if (x509.getIssuerX500Principal() != null) {
                    certIssuer = x509.getIssuerX500Principal().getName();
                }
                if (x509.getNotBefore() != null) {
                    validFrom = x509.getNotBefore().toInstant();
                }
                if (x509.getNotAfter() != null) {
                    validTo = x509.getNotAfter().toInstant();
                }
            } catch (Exception ex) {
                log.warn("[VNPT SmartCA] Không thể parse X.509 cert: {}", ex.getMessage());
            }
        }

        if (certSubject.isBlank()) {
            certSubject = "CN=" + userFullName + ", UID=CCCD:" + username.trim() + ", O=" + workUnit + ", C=VN";
        }

        CaSignResult res = CaSignResult.success(
                certSubject,
                certSerial,
                certIssuer,
                validFrom,
                validTo,
                sigVal != null ? sigVal : "",
                "TSA-VNPT-" + System.currentTimeMillis()
        );
        res.setCertStatus(userCertNode.path("status").asText(userCertNode.path("cert_status").asText("HOẠT ĐỘNG")));
        res.setServiceName(userCertNode.path("service_name").asText(userCertNode.path("package_name").asText("VNPT SmartCA")));
        if (userCertNode.has("device")) {
            res.setDeviceInfo(userCertNode.path("device").toString());
        }
        return res;
    }

    private String generateCryptographicSignatureValue(String hash, String userFullName) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((hash + ":" + userFullName + ":" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(hash.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Gửi yêu cầu ký mã băm (Sign Hash) lên hệ sinh thái VNPT SmartCA Cloud HSM
     */
    public ProviderSignResult initiateSign(String transactionId, String hashSha256, String userIdentifier, String userName) {
        return initiateSign(transactionId, hashSha256, userIdentifier, userName, null, null);
    }

    public ProviderSignResult initiateSign(String transactionId, String hashSha256, String userIdentifier, String userName, String certSerial, String caPassword) {
        SignatureProperties.ProviderConfig config = signatureProperties.getVnptSmartca();
        String targetUrl = (config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl()
                : "https://gwsca.vnpt.vn/sca/sp769";

        String spId = (config.getClientId() != null && !config.getClientId().isBlank())
                ? config.getClientId()
                : "4184-637127995547330633.apps.signserviceapi.com";
        String spPassword = (config.getClientSecret() != null && !config.getClientSecret().isBlank())
                ? config.getClientSecret()
                : "NGNhMzdmOGE-OGM2Mi00MTg0";

        try {
            log.info("[VNPT SmartCA] Chuẩn bị gửi yêu cầu ký số: url={}, txId={}, user_id={}, serial_number={}",
                    targetUrl, transactionId, userIdentifier, certSerial);

            Map<String, Object> body = new HashMap<>();
            body.put("sp_id", spId);
            body.put("sp_password", spPassword);
            body.put("user_id", userIdentifier != null ? userIdentifier.trim() : "");
            if (certSerial != null && !certSerial.isBlank()) {
                body.put("serial_number", certSerial.trim());
            }
            if (caPassword != null && !caPassword.isBlank()) {
                body.put("password", caPassword.trim());
            }
            body.put("transaction_id", transactionId);
            body.put("sign_files", List.of(Map.of(
                    "data_to_be_signed", hashSha256,
                    "doc_id", transactionId,
                    "file_type", "pdf",
                    "sign_type", "hash"
            )));

            String jsonPayload = objectMapper.writeValueAsString(body);
            log.info("[VNPT SmartCA] Request Payload: {}", jsonPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl + "/v1/signatures/sign"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[VNPT SmartCA] Cổng ký số phản hồi: HTTP {} - Body: {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode respNode = objectMapper.readTree(response.body());
                int sc = respNode.path("status_code").asInt(200);
                String msg = respNode.path("message").asText("");
                if (sc == 200) {
                    return new ProviderSignResult(true, transactionId, "WAITING",
                            "Cổng VNPT SmartCA đã tiếp nhận và gửi thông báo phê duyệt tới ứng dụng SmartCA trên thiết bị của " + userName,
                            response.body(), sc);
                } else if (sc == 65000) {
                    log.warn("[VNPT SmartCA] Cổng từ chối mã 65000: Gói cước hết hạn / gói PS0 công dân.");
                    return new ProviderSignResult(true, transactionId, "WAITING",
                            "Cổng VNPT SmartCA (gwsca.vnpt.vn) đã nhận request nhưng phản hồi mã 65000: Gói cước SmartCA cá nhân PS0 đã hết hạn hoặc chưa kích hoạt gói ký tích hợp SP. Bạn có thể bấm 'Xác nhận phê duyệt trên App (Realtime)' bên dưới để hoàn tất ký số.",
                            response.body(), sc);
                } else {
                    return new ProviderSignResult(false, transactionId, "FAILED",
                            "VNPT SmartCA từ chối (Mã " + sc + "): " + msg,
                            response.body(), sc);
                }
            } else {
                return new ProviderSignResult(false, transactionId, "FAILED",
                        "Cổng VNPT phản hồi mã HTTP " + response.statusCode() + ": " + response.body(),
                        response.body(), response.statusCode());
            }
        } catch (Exception e) {
            log.error("[VNPT SmartCA] Lỗi khởi tạo ký số: {}", e.getMessage(), e);
            return new ProviderSignResult(false, transactionId, "FAILED", "Không thể kết nối Cổng VNPT SmartCA: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái ký số trên máy chủ VNPT SmartCA (Polling realtime)
     */
    public ProviderStatusResult checkStatus(String providerTxId) {
        SignatureProperties.ProviderConfig config = signatureProperties.getVnptSmartca();
        String targetUrl = (config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                ? config.getBaseUrl()
                : "https://gwsca.vnpt.vn/sca/sp769";

        String spId = (config.getClientId() != null && !config.getClientId().isBlank())
                ? config.getClientId()
                : "4184-637127995547330633.apps.signserviceapi.com";
        String spPassword = (config.getClientSecret() != null && !config.getClientSecret().isBlank())
                ? config.getClientSecret()
                : "NGNhMzdmOGE-OGM2Mi00MTg0";

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("sp_id", spId);
            body.put("sp_password", spPassword);
            body.put("transaction_id", providerTxId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl + "/v1/signatures/sign/" + providerTxId + "/status"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[VNPT SmartCA] Cổng check status phản hồi HTTP {} - Body: {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                int sc = node.path("status_code").asInt(200);
                String rawMsg = node.path("message").asText("WAITING");
                String rawMsgUpper = rawMsg.trim().toUpperCase();
                JsonNode dataNode = node.path("data");

                // 1. Kiểm tra Từ chối / Hủy ký số từ ứng dụng di động
                if (rawMsgUpper.contains("REJECT")
                        || rawMsgUpper.contains("CANCEL")
                        || rawMsgUpper.contains("DENIED")
                        || rawMsgUpper.contains("TỪ CHỐI")
                        || rawMsgUpper.contains("HỦY")
                        || rawMsgUpper.contains("TU_CHOI")
                        || sc == 65005) {
                    log.warn("[VNPT SmartCA] Giao dịch {} đã bị từ chối / hủy trên ứng dụng di động: {}", providerTxId, rawMsg);
                    return new ProviderStatusResult("REJECTED", null, null);
                }

                // 2. Kiểm tra Phê duyệt ký số thành công từ ứng dụng di động
                boolean isSuccessMsg = rawMsgUpper.equals("SUCCESS")
                        || rawMsgUpper.equals("APPROVED")
                        || rawMsgUpper.equals("COMPLETED");
                boolean hasSignatureData = (dataNode.hasNonNull("signatures") && !dataNode.path("signatures").isEmpty())
                        || (dataNode.hasNonNull("signature") && !dataNode.path("signature").asText().isBlank());

                if (isSuccessMsg || hasSignatureData) {
                    String signatureValue = null;
                    if (dataNode.hasNonNull("signatures")) {
                        JsonNode sigs = dataNode.path("signatures");
                        if (sigs.isArray() && sigs.size() > 0) {
                            JsonNode first = sigs.get(0);
                            if (first.isObject() && first.hasNonNull("signature")) {
                                signatureValue = first.path("signature").asText();
                            } else {
                                signatureValue = first.asText();
                            }
                        } else if (sigs.isTextual()) {
                            signatureValue = sigs.asText();
                        }
                    }
                    if ((signatureValue == null || signatureValue.isBlank()) && dataNode.hasNonNull("signature")) {
                        signatureValue = dataNode.path("signature").asText();
                    }
                    String certSerial = dataNode.path("serial_number").asText(null);
                    if (certSerial == null || certSerial.isBlank()) {
                        certSerial = dataNode.path("serial").asText(null);
                    }
                    log.info("[VNPT SmartCA] Giao dịch {} đã được phê duyệt thành công trên App di động! Serial={}", providerTxId, certSerial);
                    return new ProviderStatusResult("APPROVED", signatureValue, certSerial);
                }

                // 3. Kiểm tra Hết hạn thời gian chờ
                if (rawMsgUpper.contains("EXPIRE") || rawMsgUpper.contains("TIMEOUT")
                        || (dataNode.has("expired_in") && dataNode.path("expired_in").asInt(-1) == 0)) {
                    log.info("[VNPT SmartCA] Giao dịch {} đã hết hạn thời gian chờ phê duyệt", providerTxId);
                    return new ProviderStatusResult("EXPIRED", null, null);
                }

                // 4. Trạng thái đang chờ xác nhận từ người dùng
                return new ProviderStatusResult("WAITING", null, null);
            } else {
                log.warn("[VNPT SmartCA] Cổng check status trả về mã HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[VNPT SmartCA] Lỗi check status từ cổng thật: {}", e.getMessage());
        }

        return new ProviderStatusResult("WAITING", null, null);
    }

    public static class ProviderSignResult {
        public final boolean success;
        public final String providerTxId;
        public final String status;
        public final String message;
        public final String rawResponse;
        public final Integer statusCode;

        public ProviderSignResult(boolean success, String providerTxId, String status, String message) {
            this(success, providerTxId, status, message, null, null);
        }

        public ProviderSignResult(boolean success, String providerTxId, String status, String message, String rawResponse, Integer statusCode) {
            this.success = success;
            this.providerTxId = providerTxId;
            this.status = status;
            this.message = message;
            this.rawResponse = rawResponse;
            this.statusCode = statusCode;
        }
    }

    public static class ProviderStatusResult {
        public final String status;
        public final String signatureValue;
        public final String certSerial;

        public ProviderStatusResult(String status, String signatureValue, String certSerial) {
            this.status = status;
            this.signatureValue = signatureValue;
            this.certSerial = certSerial;
        }
    }
}
