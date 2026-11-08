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
        assertEquals("", p.getFullName());
        assertEquals("", p.getGender());
        assertEquals("Chưa liên kết", p.getRegistrationStatus());
        assertEquals("REMOTE SIGNING", p.getSignatureType());
        assertEquals("Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)", p.getCaProvider());
        assertEquals("", p.getCaUsername());
        assertEquals("", p.getCertSerial());
        assertEquals("", p.getCertSubject());
        assertEquals("", p.getOwnerInfo());
        assertTrue(p.getAutoAttachSignature());
        assertEquals("", p.getSignatureImageBase64());

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

        when(vnptClient.getCertificateOnly(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(caRes);
        when(vnptClient.getCertificateOnly(anyString(), anyString(), anyString()))
                .thenReturn(caRes);

        Nq52UserSignatureProfile linked = service.authenticateAndLinkCa(
                teacherUser, "VNPT_SMARTCA", "048085001234", "Pass@123"
        );

        assertEquals("Đã liên kết tài khoản CA", linked.getRegistrationStatus());
        assertEquals("048085001234", linked.getCaUsername());
        assertEquals("54:02:FA:19:88:AC:33:1B", linked.getCertSerial());
        assertEquals("CN=Nguyễn Văn An, O=Trường THCS Nguyễn Văn Linh, C=VN", linked.getCertSubject());
        assertEquals("CN=Nguyễn Văn An, O=Trường THCS Nguyễn Văn Linh, C=VN", linked.getOwnerInfo());

        verify(vnptClient, times(1)).getCertificateOnly(
                eq("048085001234"), eq("Pass@123"), anyString(), anyString()
        );
    }

    @Test
    public void testAuthenticateAndLinkCa_Vnpt_FemaleUser_ExtractsGenderAndCccd() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CaSignResult caRes = new CaSignResult();
        caRes.setSuccess(true);
        // CCCD 066194013360: ký tự thứ 4 là '1' (lẻ) -> Nữ sinh năm 1994
        caRes.setCertSubject("C=VN,ST=Đà Nẵng,L=Phường An Khê,CN=Võ Thị Ngọc Hậu,UID=CCCD:066194013360");
        caRes.setCertSerial("54010101df4e0394862dd9c1c71dc0c9");
        caRes.setCertIssuer("VNPT SmartCA RS");
        caRes.setSignatureValue("SIG_VALUE_OK");
        caRes.setTimestampToken("TSA-12345");

        when(vnptClient.getCertificateOnly(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(caRes);

        Nq52UserSignatureProfile linked = service.authenticateAndLinkCa(
                teacherUser, "VNPT_SMARTCA", "happyangel18194@gmail.com", "Pass@123"
        );

        assertEquals("Đã liên kết tài khoản CA", linked.getRegistrationStatus());
        assertEquals("Võ Thị Ngọc Hậu", linked.getFullName());
        assertEquals("066194013360", linked.getNationalId());
        assertEquals("066194013360", linked.getPersonalId());
        assertEquals("Nữ", linked.getGender());
        assertEquals("54010101df4e0394862dd9c1c71dc0c9", linked.getCertSerial());
    }

    @Test
    public void testAuthenticateAndLinkCa_Vnpt_MaleUser_ExtractsGenderAndCccd() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CaSignResult caRes = new CaSignResult();
        caRes.setSuccess(true);
        // CCCD 048204001304: ký tự thứ 4 là '2' (chẵn) -> Nam sinh năm 2004
        caRes.setCertSubject("C=VN,ST=Đà Nẵng,CN=Trần Đình Quân,UID=CCCD:048204001304");
        caRes.setCertSerial("54010101d083e85cf756c243822f0e6f");
        caRes.setCertIssuer("VNPT SmartCA RS");
        caRes.setSignatureValue("SIG_VALUE_OK");

        when(vnptClient.getCertificateOnly(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(caRes);

        Nq52UserSignatureProfile linked = service.authenticateAndLinkCa(
                teacherUser, "VNPT_SMARTCA", "048204001304", "Pass@123"
        );

        assertEquals("Đã liên kết tài khoản CA", linked.getRegistrationStatus());
        assertEquals("Trần Đình Quân", linked.getFullName());
        assertEquals("048204001304", linked.getNationalId());
        assertEquals("048204001304", linked.getPersonalId());
        assertEquals("Nam", linked.getGender());
    }

    @Test
    public void testAuthenticateAndLinkCa_CaRejects_ThrowsExceptionAndDoesNotSave() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        existing.setRegistrationStatus("Chưa liên kết");
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

        CaSignResult caRes = CaSignResult.error("Tài khoản '048204001304' không tồn tại hoặc chưa được cấp chứng thư số trên Cổng VNPT SmartCA.");
        when(vnptClient.getCertificateOnly(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(caRes);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.authenticateAndLinkCa(teacherUser, "VNPT_SMARTCA", "048204001304", "Pass@123")
        );

        assertTrue(ex.getMessage().contains("không tồn tại"));
        // Profile must NOT be updated or saved!
        verify(profileRepository, never()).save(any());
        assertEquals("Chưa liên kết", existing.getRegistrationStatus());
    }

    @Test
    public void testAuthenticateAndLinkCa_MissingCredentials_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.authenticateAndLinkCa(teacherUser, "VNPT_SMARTCA", "")
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

    @Test
    public void testUnlinkCa_PreservesSignatureImageAndConfig() {
        Nq52UserSignatureProfile existing = new Nq52UserSignatureProfile();
        existing.setUser(teacherUser);
        existing.setFullName("Lê Hoàng Long");
        existing.setRegistrationStatus("Đã liên kết tài khoản CA");
        existing.setCertSerial("54010101d083e85cf756c243822f0e6f");
        existing.setCertSubject("C=VN,CN=Lê Hoàng Long");
        existing.setCaUsername("048204001304");
        existing.setLinkedAt(Instant.now());
        existing.setAutoAttachSignature(true);
        String customImage = "data:image/svg+xml;base64,PHN2Zz5NeVN0YW1wPC9zdmc+";
        existing.setSignatureImageBase64(customImage);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Nq52UserSignatureProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        Nq52UserSignatureProfile result = service.unlinkCa(teacherUser);

        assertEquals("Chưa liên kết", result.getRegistrationStatus());
        assertEquals("", result.getCertSerial());
        assertEquals("", result.getCertSubject());
        assertEquals("", result.getCaUsername());
        assertNull(result.getLinkedAt());
        // Khi hủy liên kết tài khoản thì làm mới toàn bộ thông tin đã nhập
        assertEquals("", result.getSignatureImageBase64());
        assertTrue(result.getAutoAttachSignature());
        assertEquals("", result.getFullName());

        verify(profileRepository, times(1)).save(existing);
    }
}
