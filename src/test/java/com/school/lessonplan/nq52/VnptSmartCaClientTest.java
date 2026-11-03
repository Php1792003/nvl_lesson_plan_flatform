package com.school.lessonplan.nq52;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.nq52.signature.config.SignatureProperties;
import com.school.lessonplan.nq52.signature.provider.CaSignResult;
import com.school.lessonplan.nq52.signature.provider.VnptSmartCaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VnptSmartCaClientTest {

    private SignatureProperties signatureProperties;
    private ObjectMapper objectMapper;
    private VnptSmartCaClient client;

    @BeforeEach
    public void setup() {
        signatureProperties = new SignatureProperties();
        signatureProperties.getVnptSmartca().setBaseUrl("http://localhost:9999/mock-vnpt");
        objectMapper = new ObjectMapper();

        client = new VnptSmartCaClient(signatureProperties, objectMapper);
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
        assertTrue(result.getErrorMessage().contains("Tên đăng nhập"));
    }

    @Test
    public void testAuthenticateAndSign_EmptyPassword_ReturnsError() {
        CaSignResult result = client.authenticateAndSign(
                "giaovien@school.edu.vn",
                "",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Nguyễn Văn An",
                "Trường THCS Nguyễn Văn Linh"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Mật khẩu"));
    }

    @Test
    public void testAuthenticateAndSign_UnreachableEndpoint_ReturnsConnectionErrorWithoutMock() {
        // When server is unreachable (or wrong credentials), it must return error and NOT fallback to mock success!
        CaSignResult result = client.authenticateAndSign(
                "giaovien@school.edu.vn",
                "WrongPassword999",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Nguyễn Văn An",
                "Trường THCS Nguyễn Văn Linh"
        );

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertNull(result.getSignatureValue());
    }
}
