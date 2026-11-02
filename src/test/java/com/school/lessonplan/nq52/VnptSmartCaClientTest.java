package com.school.lessonplan.nq52;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.lessonplan.nq52.signature.config.SignatureProperties;
import com.school.lessonplan.nq52.signature.provider.CaSignResult;
import com.school.lessonplan.nq52.signature.provider.VnptSmartCaClient;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class VnptSmartCaClientTest {

    private SignatureProperties signatureProperties;
    private ObjectMapper objectMapper;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private VnptSmartCaClient client;

    @BeforeEach
    public void setup() {
        signatureProperties = new SignatureProperties();
        objectMapper = new ObjectMapper();
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();

        client = new VnptSmartCaClient(signatureProperties, objectMapper, userRepository, passwordEncoder);
    }

    @Test
    public void testAuthenticateAndSign_WithValidPassword_Success() {
        User teacher = new User();
        teacher.setId(10L);
        teacher.setEmail("giaovien@school.edu.vn");
        teacher.setPasswordHash(passwordEncoder.encode("Password123!"));

        when(userRepository.findByEmail("giaovien@school.edu.vn")).thenReturn(Optional.of(teacher));

        CaSignResult result = client.authenticateAndSign(
                "giaovien@school.edu.vn",
                "Password123!",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Nguyễn Văn An",
                "Trường THCS Nguyễn Văn Linh"
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getCertSerial());
        assertTrue(result.getCertSerial().startsWith("54:VNPT:"));
        assertTrue(result.getCertSubject().contains("Nguyễn Văn An"));
        assertNotNull(result.getSignatureValue());
    }

    @Test
    public void testAuthenticateAndSign_WithWrongPassword_Fails() {
        User teacher = new User();
        teacher.setId(10L);
        teacher.setEmail("giaovien@school.edu.vn");
        teacher.setPasswordHash(passwordEncoder.encode("Password123!"));

        when(userRepository.findByEmail("giaovien@school.edu.vn")).thenReturn(Optional.of(teacher));

        CaSignResult result = client.authenticateAndSign(
                "giaovien@school.edu.vn",
                "WrongPassword999",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Nguyễn Văn An",
                "Trường THCS Nguyễn Văn Linh"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Mật khẩu"));
    }
}

