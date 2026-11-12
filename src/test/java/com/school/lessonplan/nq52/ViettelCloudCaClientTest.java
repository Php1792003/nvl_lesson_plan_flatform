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

    @Test
    public void testEnsureBase64Hash_ConvertsHexTo44CharBase64() {
        // Chuỗi SHA-256 rỗng dạng Hex: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String base64 = ViettelCloudCaClient.ensureBase64Hash(hex);

        assertNotNull(base64);
        assertEquals(44, base64.length());
        assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", base64);
    }

    @Test
    public void testEncodeDocumentName_SanitizesAndEncodesBase64() {
        String docName = "Phiếu đánh giá Nghị quyết 52 (Tháng 11/2026)";
        String b64 = ViettelCloudCaClient.encodeDocumentName(docName);

        assertNotNull(b64);
        assertTrue(b64.length() < 100);

        // Giải mã kiểm tra nội dung
        String decoded = new String(java.util.Base64.getDecoder().decode(b64));
        assertFalse(decoded.contains("ế"));
        assertFalse(decoded.contains("á"));
        assertTrue(decoded.contains("Phieu danh gia"));
    }

    @Test
    public void testParseGeneralizedTime_Rfc5280Format() {
        java.time.Instant instant1 = ViettelCloudCaClient.parseGeneralizedTime("20250115000000+0000");
        assertNotNull(instant1);

        java.time.Instant instant2 = ViettelCloudCaClient.parseGeneralizedTime("20280115235959Z");
        assertNotNull(instant2);
    }
}

