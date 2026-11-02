package com.school.lessonplan.nq52;

import com.school.lessonplan.department.Department;
import com.school.lessonplan.nq52.signature.*;
import com.school.lessonplan.nq52.signature.dto.UserSignatureProfileForm;
import com.school.lessonplan.nq52.signature.provider.CaSignResult;
import com.school.lessonplan.nq52.signature.provider.ViettelCloudCaClient;
import com.school.lessonplan.nq52.signature.provider.VneIdCaClient;
import com.school.lessonplan.nq52.signature.provider.VnptSmartCaClient;
import com.school.lessonplan.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Nq52UserSignatureProfileServiceTest {

    private Nq52UserSignatureProfileRepository profileRepository;
    private VnptSmartCaClient vnptClient;
    private ViettelCloudCaClient viettelClient;
    private VneIdCaClient vneidClient;

    private Nq52UserSignatureProfileService service;
    private User teacherUser;

    @BeforeEach
    public void setup() {
        profileRepository = Mockito.mock(Nq52UserSignatureProfileRepository.class);
        vnptClient = Mockito.mock(VnptSmartCaClient.class);
        viettelClient = Mockito.mock(ViettelCloudCaClient.class);
        vneidClient = Mockito.mock(VneIdCaClient.class);

        service = new Nq52UserSignatureProfileService(profileRepository, vnptClient, viettelClient, vneidClient);

        teacherUser = new User();
        teacherUser.setId(10L);
        teacherUser.setLastName("Nguyễn");
        teacherUser.setGivenName("Văn An");
        teacherUser.setEmail("giaovien@school.edu.vn");

        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Tổ Toán - Tin");
        teacherUser.setDepartment(dept);
    }

    @Test
    public void testGetOrCreateProfile_WhenNotExists_ShouldCreateDefault() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        Nq52UserSignatureProfile p = service.getOrCreateProfile(teacherUser);

        assertNotNull(p);
        assertEquals(teacherUser, p.getUser());
        assertEquals("Nguyễn Văn An", p.getFullName());
        assertEquals("Nam", p.getGender());
        assertEquals("Đã duyệt tài khoản", p.getRegistrationStatus());
        assertEquals("REMOTE SIGNING", p.getSignatureType());
        assertEquals("Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)", p.getCaProvider());
        assertEquals("giaovien@school.edu.vn", p.getCaUsername());
        assertEquals("", p.getCertSerial());
        assertEquals("", p.getCertSubject());
        assertTrue(p.getOwnerInfo().contains("Phường Cẩm Lệ"));
        assertTrue(p.getOwnerInfo().contains("Đà Nẵng"));
        assertTrue(p.getAutoAttachSignature());
        assertNotNull(p.getSignatureImageBase64());
        assertTrue(p.getSignatureImageBase64().startsWith("data:image/svg+xml;base64,"));

        verify(profileRepository, times(1)).save(any(Nq52UserSignatureProfile.class));
    }

    @Test
    public void testGetOrCreateProfile_WhenExists_ShouldReturnExisting() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setId(1L);
        existing.setUser(teacherUser);
        existing.setFullName("Nguyễn Văn An");

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

        Nq52UserSignatureProfile p = service.getOrCreateProfile(teacherUser);

        assertEquals(1L, p.getId());
        assertEquals("Nguyễn Văn An", p.getFullName());
        verify(profileRepository, never()).save(any());
    }

    @Test
    public void testUpdateProfile_Success() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSignatureProfileForm form = new UserSignatureProfileForm();
        form.setMoetId("4801999999");
        form.setFullName("Nguyễn Văn An (Cập nhật)");
        form.setDob("20/10/1988");
        form.setGender("Nữ");
        form.setNationalId("048088009999");
        form.setPersonalId("048088009999");
        form.setCaProvider("Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel-CA)");
        form.setCaUsername("048088009999");
        form.setAutoAttachSignature(true);

        Nq52UserSignatureProfile updated = service.updateProfile(teacherUser, form);

        assertEquals("4801999999", updated.getMoetId());
        assertEquals("Nguyễn Văn An (Cập nhật)", updated.getFullName());
        assertEquals("20/10/1988", updated.getDob());
        assertEquals("Nữ", updated.getGender());
        assertEquals("048088009999", updated.getNationalId());
        assertEquals("048088009999", updated.getPersonalId());
        assertEquals("Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel-CA)", updated.getCaProvider());
        assertEquals("048088009999", updated.getCaUsername());
        assertTrue(updated.getAutoAttachSignature());

        verify(profileRepository, times(1)).save(existing);
    }

    @Test
    public void testAuthenticateAndLinkCa_Vnpt_Success() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CaSignResult caRes = new CaSignResult();
        caRes.setSuccess(true);
        caRes.setCertSubject("CN=Nguyễn Văn An, O=Trường THCS Nguyễn Văn Linh, C=VN");
        caRes.setCertSerial("54:02:FA:19:88:AC:33:1B");
        caRes.setCertIssuer("VNPT-CA");
        caRes.setSignatureValue("SIG_VALUE_OK");
        caRes.setTimestampToken("TSA-12345");

        when(vnptClient.authenticateAndSign(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(caRes);

        Nq52UserSignatureProfile linked = service.authenticateAndLinkCa(
                teacherUser, "VNPT_SMARTCA", "048085001234", "Secret123!"
        );

        assertEquals("Đã duyệt tài khoản", linked.getRegistrationStatus());
        assertEquals("048085001234", linked.getCaUsername());
        assertEquals("54:02:FA:19:88:AC:33:1B", linked.getCertSerial());
        assertEquals("CN=Nguyễn Văn An, O=Trường THCS Nguyễn Văn Linh, C=VN", linked.getCertSubject());
        assertTrue(linked.getOwnerInfo().contains("Phường Cẩm Lệ"));
        assertTrue(linked.getOwnerInfo().contains("Đà Nẵng"));

        verify(vnptClient, times(1)).authenticateAndSign(
                eq("048085001234"), eq("Secret123!"), anyString(), anyString(), anyString()
        );
    }

    @Test
    public void testAuthenticateAndLinkCa_MissingCredentials_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.authenticateAndLinkCa(teacherUser, "VNPT_SMARTCA", "", "Password123!")
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.authenticateAndLinkCa(teacherUser, "VNPT_SMARTCA", "048085001234", "")
        );
    }

    @Test
    public void testSaveSignatureImage_Success() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        String testImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        Nq52UserSignatureProfile res = service.saveSignatureImage(teacherUser, testImage);

        assertEquals(testImage, res.getSignatureImageBase64());
        verify(profileRepository, times(1)).save(existing);
    }
}
