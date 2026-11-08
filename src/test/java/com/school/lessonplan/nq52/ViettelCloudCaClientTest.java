package com.school.lessonplan.nq52;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.nq52.signature.config.SignatureProperties;
import com.school.lessonplan.nq52.signature.provider.CaSignResult;
import com.school.lessonplan.nq52.signature.provider.ViettelCloudCaClient;
import com.school.lessonplan.nq52.signature.provider.VnptSmartCaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ViettelCloudCaClientTest {

    private SignatureProperties signatureProperties;
    private ObjectMapper objectMapper;
    private ViettelCloudCaClient client;

    @BeforeEach
    public void setup() {
        signatureProperties = new SignatureProperties();
        signatureProperties.getViettelCloudCa().setBaseUrl("http://localhost:9998/mock-viettel");
        signatureProperties.getViettelCloudCa().setClientId("school_nvl_viettel");
        signatureProperties.getViettelCloudCa().setClientSecret("viettel_sec_2026");
        objectMapper = new ObjectMapper();

        client = new ViettelCloudCaClient(signatureProperties, objectMapper);
    }

    @Test
    public void testGetCertificateOnly_EmptyUsername_ReturnsError() {
        CaSignResult result = client.getCertificateOnly("", "Password123!", "Nguyễn Văn An", "Trường THCS Nguyễn Văn Linh");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Tên đăng nhập / Số CCCD"));
    }

    @Test
    public void testGetCertificateOnly_EmptyPassword_ReturnsError() {
        CaSignResult result = client.getCertificateOnly("048302001055", "", "Nguyễn Văn An", "Trường THCS Nguyễn Văn Linh");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Mật khẩu / Mã PIN"));
    }

    @Test
    public void testAuthenticateAndSign_EmptyUsername_ReturnsError() {
        CaSignResult result = client.authenticateAndSign(
                "",
                "Password123!",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Nguyễn Văn An",
                "Trường THCS Nguyễn Văn Linh"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Tên đăng nhập / Số CCCD"));
    }

    @Test
    public void testAuthenticateAndSign_UnreachableEndpoint_ReturnsConnectionErrorWithoutMock() {
        CaSignResult result = client.authenticateAndSign(
                "048302001055",
                "WrongPassword999",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Nguyễn Văn An",
                "Trường THCS Nguyễn Văn Linh"
        );

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertNull(result.getSignatureValue());
    }

    @Test
    public void testInitiateSign_UnreachableEndpoint_ReturnsFailure() {
        VnptSmartCaClient.ProviderSignResult result = client.initiateSign(
                "TX-12345",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "048302001055",
                "Nguyễn Văn An",
                "SERIAL-001",
                "Pass123"
        );

        assertFalse(result.success);
        assertNotNull(result.message);
        assertEquals("FAILED", result.status);
    }
}

