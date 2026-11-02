package com.school.lessonplan.nq52;

import com.school.lessonplan.nq52.signature.*;
import com.school.lessonplan.nq52.signature.config.SignatureProperties;
import com.school.lessonplan.nq52.signature.dto.*;
import com.school.lessonplan.nq52.signature.provider.*;
import com.school.lessonplan.role.Role;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class Nq52SignatureServiceTest {

    private Nq52DigitalSignatureRepository signatureRepository;
    private Nq52WorkResultRepository workResultRepository;
    private Nq52EvaluationRepository evaluationRepository;
    private Nq52WorkResultService workResultService;
    private Nq52EvaluationService evaluationService;
    private UserRepository userRepository;
    private VnptSmartCaClient vnptClient;
    private ViettelCloudCaClient viettelClient;
    private VneIdCaClient vneidClient;
    private SignatureProperties signatureProperties;

    private Nq52SignatureService service;
    private User teacherUser;
    private User adminUser;
    private Nq52WorkResult sampleWorkResult;

    @BeforeEach
    public void setup() {
        signatureRepository = Mockito.mock(Nq52DigitalSignatureRepository.class);
        workResultRepository = Mockito.mock(Nq52WorkResultRepository.class);
        evaluationRepository = Mockito.mock(Nq52EvaluationRepository.class);
        workResultService = Mockito.mock(Nq52WorkResultService.class);
        evaluationService = Mockito.mock(Nq52EvaluationService.class);
        userRepository = Mockito.mock(UserRepository.class);
        vnptClient = Mockito.mock(VnptSmartCaClient.class);
        viettelClient = Mockito.mock(ViettelCloudCaClient.class);
        vneidClient = Mockito.mock(VneIdCaClient.class);
        signatureProperties = new SignatureProperties();

        service = new Nq52SignatureService(
                signatureRepository,
                workResultRepository,
                evaluationRepository,
                workResultService,
                evaluationService,
                userRepository,
                vnptClient,
                viettelClient,
                vneidClient,
                signatureProperties
        );

        teacherUser = new User();
        teacherUser.setId(10L);
        teacherUser.setLastName("Nguyễn");
        teacherUser.setGivenName("Văn An");
        teacherUser.setEmail("teacher@school.edu.vn");

        Role teacherRole = new Role();
        teacherRole.setCode("TEACHER");
        teacherRole.setName("Giáo viên");
        teacherUser.setRoles(Set.of(teacherRole));

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setLastName("Ban");
        adminUser.setGivenName("Giám Hiệu");
        adminUser.setEmail("admin@school.edu.vn");
        Role adminRole = new Role();
        adminRole.setCode("ADMIN");
        adminRole.setName("Quản trị viên / Ban Giám Hiệu");
        adminUser.setRoles(Set.of(adminRole));

        sampleWorkResult = new Nq52WorkResult();
        sampleWorkResult.setId(100L);
        sampleWorkResult.setUser(teacherUser);
        sampleWorkResult.setTeacherNameSnapshot("Nguyễn Văn An");
        sampleWorkResult.setEvaluationMonth(11);
        sampleWorkResult.setEvaluationYear(2026);
        sampleWorkResult.setStatus(Nq52Status.DRAFT);
        sampleWorkResult.setTotalAssigned(6);
        sampleWorkResult.setTotalCompleted(6);
        sampleWorkResult.setCompletionRate(BigDecimal.valueOf(100.0));
        sampleWorkResult.setOnTimeRate(BigDecimal.valueOf(100.0));
        sampleWorkResult.setQualityRate(BigDecimal.valueOf(100.0));

        when(workResultRepository.findById(100L)).thenReturn(Optional.of(sampleWorkResult));
        when(userRepository.findById(10L)).thenReturn(Optional.of(teacherUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(signatureRepository.save(any(Nq52DigitalSignature.class))).thenAnswer(invocation -> {
            Nq52DigitalSignature sig = invocation.getArgument(0);
            sig.setId(999L);
            return sig;
        });
    }

    @Test
    public void testPrepareSigning() {
        PrepareSigningResponse res = service.prepareSigning("WORK_RESULT", 100L, teacherUser);
        assertNotNull(res);
        assertEquals("WORK_RESULT", res.getDocumentType());
        assertEquals(100L, res.getDocumentId());
        assertEquals("Nguyễn Văn An", res.getSignerName());
        assertEquals("TEACHER", res.getSignerRole());
        assertEquals("REMOTE_SIGNING", res.getRecommendedSignatureType());
        assertNotNull(res.getDataHashSha256());
    }

    @Test
    public void testInitiateRemoteSigning_VnptSmartCa() {
        when(vnptClient.initiateSign(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new VnptSmartCaClient.ProviderSignResult(true, "VNPT-12345", "WAITING", "Push notification sent"));

        RemoteSignInitiateRequest req = new RemoteSignInitiateRequest();
        req.setDocumentType("WORK_RESULT");
        req.setDocumentId(100L);
        req.setProvider("VNPT_SMARTCA");

        RemoteSignInitiateResponse res = service.initiateRemoteSigning(req, teacherUser);
        assertTrue(res.isSuccess());
        assertNotNull(res.getTransactionId());
        assertEquals(120, res.getExpiresInSeconds());
        assertEquals("VNPT_SMARTCA", res.getProvider());
    }

    @Test
    public void testInitiateRemoteSigning_ViettelCloudCa() {
        when(viettelClient.initiateSign(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new VnptSmartCaClient.ProviderSignResult(true, "VIETTEL-12345", "WAITING", "Viettel push sent"));

        RemoteSignInitiateRequest req = new RemoteSignInitiateRequest();
        req.setDocumentType("WORK_RESULT");
        req.setDocumentId(100L);
        req.setProvider("VIETTEL_CLOUD_CA");

        RemoteSignInitiateResponse res = service.initiateRemoteSigning(req, teacherUser);
        assertTrue(res.isSuccess());
        assertEquals("VIETTEL_CLOUD_CA", res.getProvider());
    }

    @Test
    public void testInitiateRemoteSigning_VneId() {
        when(vneidClient.initiateSign(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new VnptSmartCaClient.ProviderSignResult(true, "VNEID-12345", "WAITING", "VNeID push sent"));

        RemoteSignInitiateRequest req = new RemoteSignInitiateRequest();
        req.setDocumentType("WORK_RESULT");
        req.setDocumentId(100L);
        req.setProvider("VN_EID");

        RemoteSignInitiateResponse res = service.initiateRemoteSigning(req, teacherUser);
        assertTrue(res.isSuccess());
        assertEquals("VN_EID", res.getProvider());
    }

    @Test
    public void testSimulateAppApprovalAndRealtimePolling() {
        // 1. Khởi tạo phiên
        when(vnptClient.initiateSign(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new VnptSmartCaClient.ProviderSignResult(true, "VNPT-TX-1", "WAITING", "Waiting app approval"));

        RemoteSignInitiateRequest initReq = new RemoteSignInitiateRequest();
        initReq.setDocumentType("WORK_RESULT");
        initReq.setDocumentId(100L);
        initReq.setProvider("VNPT_SMARTCA");
        RemoteSignInitiateResponse initRes = service.initiateRemoteSigning(initReq, teacherUser);

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("127.0.0.1");

        // 2. Kiểm tra trạng thái khi đang chờ (Polling check)
        RemoteSignStatusResponse statusBefore = service.checkRemoteSignStatus(initRes.getTransactionId(), httpReq);
        assertFalse(statusBefore.isCompleted());
        assertEquals("WAITING", statusBefore.getStatus());

        // 3. Người dùng xác nhận trên ứng dụng di động (Simulate App Approval)
        RemoteSignStatusResponse simRes = service.simulateAppApproval(initRes.getTransactionId(), teacherUser, httpReq);
        assertTrue(simRes.isCompleted());
        assertEquals("COMPLETED", simRes.getStatus());
        assertEquals("Nguyễn Văn An", simRes.getSignerName());

        // 4. Polling kiểm tra lại -> phải trả về COMPLETED ngay
        RemoteSignStatusResponse statusAfter = service.checkRemoteSignStatus(initRes.getTransactionId(), httpReq);
        assertTrue(statusAfter.isCompleted());
        assertEquals("COMPLETED", statusAfter.getStatus());
    }

    @Test
    public void testConfirmRemoteSigning_WithOtp() {
        when(vnptClient.initiateSign(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new VnptSmartCaClient.ProviderSignResult(true, "VNPT-TX-2", "WAITING", "Waiting OTP"));

        RemoteSignInitiateRequest initReq = new RemoteSignInitiateRequest();
        initReq.setDocumentType("WORK_RESULT");
        initReq.setDocumentId(100L);
        initReq.setProvider("VNPT_SMARTCA");
        RemoteSignInitiateResponse initRes = service.initiateRemoteSigning(initReq, teacherUser);

        RemoteSignConfirmRequest confirmReq = new RemoteSignConfirmRequest();
        confirmReq.setDocumentType("WORK_RESULT");
        confirmReq.setDocumentId(100L);
        confirmReq.setTransactionId(initRes.getTransactionId());
        confirmReq.setOtp("123456");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        Nq52DigitalSignature sig = service.confirmRemoteSigning(confirmReq, teacherUser, httpReq);

        assertNotNull(sig);
        assertEquals(Nq52SignatureType.REMOTE_SIGNING, sig.getSignatureType());
        assertEquals(Nq52SigningRole.TEACHER, sig.getSigningRole());
        assertEquals("Nguyễn Văn An", sig.getSignerNameSnapshot());
        assertEquals(Nq52SignatureStatus.VALID, sig.getStatus());
        assertEquals(Nq52Status.SUBMITTED, sampleWorkResult.getStatus());
    }

    @Test
    public void testSignWithUsbToken_ByAdmin() {
        UsbTokenSignRequest req = new UsbTokenSignRequest();
        req.setDocumentType("WORK_RESULT");
        req.setDocumentId(100L);
        req.setCertSerial("54:02:FA:19:88:AC:33:1B");
        req.setPin("12345678");
        req.setBghNotes("Đồng ý phê duyệt bảng tự đánh giá.");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        Nq52DigitalSignature sig = service.signWithUsbToken(req, adminUser, httpReq);

        assertNotNull(sig);
        assertEquals(Nq52SignatureType.USB_TOKEN, sig.getSignatureType());
        assertEquals(Nq52SigningRole.PRINCIPAL, sig.getSigningRole());
        assertEquals(Nq52Status.REVIEWED, sampleWorkResult.getStatus());
        assertEquals("BAN_CO_YEU_CHINH_PHU", sig.getCaProvider());
    }

    @Test
    public void testVerifySignatureRealtime_Success() {
        Nq52DigitalSignature sig = new Nq52DigitalSignature();
        sig.setId(99L);
        sig.setDocumentType("WORK_RESULT");
        sig.setDocumentId(100L);
        sig.setSigningRole(Nq52SigningRole.TEACHER);
        sig.setSignerNameSnapshot("Nguyễn Văn An");
        sig.setCaProvider("VNPT_SMARTCA");
        sig.setSignatureType(Nq52SignatureType.REMOTE_SIGNING);
        sig.setStatus(Nq52SignatureStatus.VALID);
        sig.setSignedAt(Instant.now());
        sig.setCertValidTo(Instant.now().plus(java.time.Duration.ofDays(365)));

        // Precompute sample hash for work result 100
        when(workResultRepository.findById(100L)).thenReturn(Optional.of(sampleWorkResult));
        
        String computedHash = service.computeWorkResultHash(sampleWorkResult);
        sig.setDataHashSha256(computedHash);

        when(signatureRepository.findById(99L)).thenReturn(Optional.of(sig));
        when(signatureRepository.findByDocumentTypeAndDocumentIdAndStatusOrderBySignedAtAsc("WORK_RESULT", 100L, Nq52SignatureStatus.VALID))
                .thenReturn(Collections.singletonList(sig));

        RealtimeVerificationResultDto result = service.verifySignatureRealtime("WORK_RESULT", 100L, 99L);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.isIntegrityMatched());
        assertEquals("GOOD", result.getOcspStatus());
        assertEquals("NOT_REVOKED", result.getCrlStatus());
        assertEquals("Nguyễn Văn An", result.getSignerName());
        assertEquals("VNPT SmartCA", result.getCaProvider());
    }

    @Test
    public void testDirectSignWithCertificate_Success() {
        CertificateDirectSignRequest req = new CertificateDirectSignRequest();
        req.setDocumentType("WORK_RESULT");
        req.setDocumentId(100L);
        req.setProvider("VNPT_SMARTCA");
        req.setUsername("048089001234");
        req.setPassword("Password@123");

        when(workResultRepository.findById(100L)).thenReturn(Optional.of(sampleWorkResult));
        when(signatureRepository.save(any(Nq52DigitalSignature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(vnptClient.authenticateAndSign(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CaSignResult.success(
                        "CN=Nguyễn Văn An, UID=048089001234, O=Trường THCS Nguyễn Văn Linh, C=VN",
                        "54:VNPT:FA:99:11",
                        "CN=VNPT-CA Cloud HSM Root, O=VNPT Group, C=VN",
                        Instant.now().minus(java.time.Duration.ofDays(30)),
                        Instant.now().plus(java.time.Duration.ofDays(700)),
                        "SIG-VALUE-12345",
                        "TSA-TOKEN-67890"
                ));

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        Nq52DigitalSignature sig = service.directSignWithCertificate(req, teacherUser, httpReq);

        assertNotNull(sig);
        assertEquals("VNPT_SMARTCA", sig.getCaProvider());
        assertEquals(Nq52SigningRole.TEACHER, sig.getSigningRole());
        assertEquals("Nguyễn Văn An", sig.getSignerNameSnapshot());
        assertEquals("54:VNPT:FA:99:11", sig.getCertSerial());
        assertEquals(Nq52Status.SUBMITTED, sampleWorkResult.getStatus());
    }
}

