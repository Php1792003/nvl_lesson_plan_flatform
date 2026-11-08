package com.school.lessonplan.nq52.signature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.signature")
public class SignatureProperties {

    /**
     * Chế độ hoạt động:
     * - REAL: Bắt buộc gọi trực tiếp tới Cổng API máy chủ Cloud CA thật của nhà phát hành (VNPT, Viettel, VNeID). Không sử dụng mock data.
     */
    private String mode = "REAL";

    private ProviderConfig vnptSmartca = new ProviderConfig("https://rmgateway.vnptit.vn/sca/sp769", "school_nvl_vnpt", "smartca_sec_2026");
    private ProviderConfig viettelCloudCa = new ProviderConfig("https://remotesigning.viettel.vn", "school_nvl_viettel", "viettel_sec_2026");
    private ProviderConfig vneid = new ProviderConfig("https://dvc.dancuquocgia.gov.vn", "school_nvl_vneid", "vneid_sec_2026");

    public SignatureProperties() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public ProviderConfig getVnptSmartca() { return vnptSmartca; }
    public void setVnptSmartca(ProviderConfig vnptSmartca) { this.vnptSmartca = vnptSmartca; }

    public ProviderConfig getViettelCloudCa() { return viettelCloudCa; }
    public void setViettelCloudCa(ProviderConfig viettelCloudCa) { this.viettelCloudCa = viettelCloudCa; }

    public ProviderConfig getVneid() { return vneid; }
    public void setVneid(ProviderConfig vneid) { this.vneid = vneid; }

    public static class ProviderConfig {
        private boolean enabled = true;
        private String baseUrl;
        private String clientId = "";
        private String clientSecret = "";
        private int timeoutSeconds = 15;

        public ProviderConfig() {}

        public ProviderConfig(String defaultBaseUrl) {
            this.baseUrl = defaultBaseUrl;
        }

        public ProviderConfig(String defaultBaseUrl, String defaultClientId, String defaultClientSecret) {
            this.baseUrl = defaultBaseUrl;
            this.clientId = defaultClientId;
            this.clientSecret = defaultClientSecret;
        }

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}

