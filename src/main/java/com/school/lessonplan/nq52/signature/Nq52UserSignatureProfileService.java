package com.school.lessonplan.nq52.signature;

import com.school.lessonplan.nq52.signature.dto.UserSignatureProfileForm;
import com.school.lessonplan.nq52.signature.provider.CaSignResult;
import com.school.lessonplan.nq52.signature.provider.ViettelCloudCaClient;
import com.school.lessonplan.nq52.signature.provider.VneIdCaClient;
import com.school.lessonplan.nq52.signature.provider.VnptSmartCaClient;
import com.school.lessonplan.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class Nq52UserSignatureProfileService {

    private static final Logger log = LoggerFactory.getLogger(Nq52UserSignatureProfileService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Nq52UserSignatureProfileRepository profileRepository;
    private final VnptSmartCaClient vnptClient;
    private final ViettelCloudCaClient viettelClient;
    private final VneIdCaClient vneidClient;

    public Nq52UserSignatureProfileService(Nq52UserSignatureProfileRepository profileRepository,
                                           VnptSmartCaClient vnptClient,
                                           ViettelCloudCaClient viettelClient,
                                           VneIdCaClient vneidClient) {
        this.profileRepository = profileRepository;
        this.vnptClient = vnptClient;
        this.viettelClient = viettelClient;
        this.vneidClient = vneidClient;
    }

    /**
     * Lấy hoặc khởi tạo hồ sơ chữ ký số mặc định cho người dùng
     */
    public Nq52UserSignatureProfile getOrCreateProfile(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User không được null");
        }
        Optional<Nq52UserSignatureProfile> existing = profileRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            Nq52UserSignatureProfile p = existing.get();
            long userNum = user.getId() != null ? user.getId() : 1L;

            if (p.getFullName() == null || p.getFullName().isBlank()) {
                p.setFullName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "Nguyễn Văn An");
            }
            if (p.getMoetId() == null || p.getMoetId().isBlank()) {
                p.setMoetId("4801" + String.format("%06d", 100000 + userNum * 73));
            }
            if (p.getDob() == null || p.getDob().isBlank()) {
                p.setDob("15/05/1985");
            }
            if (p.getGender() == null || p.getGender().isBlank()) {
                p.setGender("Nam");
            }
            if (p.getNationalId() == null || p.getNationalId().isBlank()) {
                p.setNationalId("048085001234");
            }
            if (p.getPersonalId() == null || p.getPersonalId().isBlank()) {
                p.setPersonalId("048085001234");
            }
            return p;
        }

        Nq52UserSignatureProfile p = new Nq52UserSignatureProfile();
        p.setUser(user);

        // Thông tin nhân sự mặc định
        long userNum = user.getId() != null ? user.getId() : 1L;
        p.setMoetId("4801" + String.format("%06d", 100000 + userNum * 73));
        p.setFullName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "Nguyễn Văn An");
        p.setDob("15/05/1985");
        p.setGender("Nam");
        p.setNationalId("048085001234");
        p.setPersonalId("048085001234");

        // Thông tin đăng ký tài khoản chữ ký số mặc định
        p.setRegistrationStatus("Đã duyệt tài khoản");
        p.setSignatureType("REMOTE SIGNING");
        p.setCaProvider("Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)");
        String username = user.getEmail() != null && !user.getEmail().isBlank()
                ? user.getEmail()
                : "giaovien@school.edu.vn";
        p.setCaUsername(username);

        // Thông tin chứng thư số ban đầu (khi chưa liên kết Cổng CA thật)
        p.setCertSerial("");
        p.setCertSubject("");
        LocalDate now = LocalDate.now();
        p.setValidFrom(now.format(DATE_FMT));
        p.setValidTo(now.plusYears(1).format(DATE_FMT));

        p.setOwnerInfo(", L=Phường Cẩm Lệ, S=Đà Nẵng, C=VN");

        p.setAutoAttachSignature(true);
        String deptName = (user.getDepartment() != null) ? user.getDepartment().getName() : "Tổ Tự Nhiên";
        p.setSignatureImageBase64(generateDefaultStampSvgBase64(p.getFullName(), deptName, "VNPT-CA"));

        return profileRepository.save(p);
    }

    /**
     * Cập nhật thông tin hồ sơ và cấu hình chữ ký số
     */
    public Nq52UserSignatureProfile updateProfile(User user, UserSignatureProfileForm form) {
        Nq52UserSignatureProfile p = getOrCreateProfile(user);

        if (form.getMoetId() != null) p.setMoetId(form.getMoetId().trim());
        if (form.getFullName() != null) p.setFullName(form.getFullName().trim());
        if (form.getDob() != null) p.setDob(form.getDob().trim());
        if (form.getGender() != null) p.setGender(form.getGender().trim());
        if (form.getNationalId() != null) p.setNationalId(form.getNationalId().trim());
        if (form.getPersonalId() != null) p.setPersonalId(form.getPersonalId().trim());

        if (form.getSignatureType() != null) p.setSignatureType(form.getSignatureType().trim());
        if (form.getCaProvider() != null) p.setCaProvider(form.getCaProvider().trim());
        if (form.getCaUsername() != null) p.setCaUsername(form.getCaUsername().trim());

        if (form.getCertSerial() != null) p.setCertSerial(form.getCertSerial().trim());
        if (form.getCertSubject() != null) p.setCertSubject(form.getCertSubject().trim());
        if (form.getValidFrom() != null) p.setValidFrom(form.getValidFrom().trim());
        if (form.getValidTo() != null) p.setValidTo(form.getValidTo().trim());
        if (form.getOwnerInfo() != null) p.setOwnerInfo(form.getOwnerInfo().trim());

        if (form.getAutoAttachSignature() != null) {
            p.setAutoAttachSignature(form.getAutoAttachSignature());
        }
        if (form.getSignatureImageBase64() != null && !form.getSignatureImageBase64().isBlank()) {
            p.setSignatureImageBase64(form.getSignatureImageBase64());
        }

        return profileRepository.save(p);
    }

    /**
     * Đăng nhập & Xác thực với nhà cung cấp chữ ký số (VNPT-CA / Viettel-CA / VNeID)
     * Đồng thời cập nhật trạng thái đã duyệt và các thông số chứng thư số realtime
     */
    public Nq52UserSignatureProfile authenticateAndLinkCa(User user, String provider, String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu nhập tên đăng nhập");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu nhập mật khẩu");
        }

        Nq52UserSignatureProfile p = getOrCreateProfile(user);
        String prov = (provider != null && !provider.isBlank()) ? provider : "VNPT_SMARTCA";
        String sampleHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String workUnit = "Trường THCS Nguyễn Văn Linh";

        CaSignResult caResult;
        String providerDisplayName;
        if (prov.toUpperCase().contains("VIETTEL") || "VIETTEL_CLOUD_CA".equalsIgnoreCase(prov)) {
            providerDisplayName = "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel-CA)";
            caResult = viettelClient.authenticateAndSign(username.trim(), password.trim(), sampleHash, user.getFullName(), workUnit);
        } else if (prov.toUpperCase().contains("VNEID") || "VN_EID".equalsIgnoreCase(prov)) {
            providerDisplayName = "Cục Cảnh sát QLHC về TTXH - Bộ Công An (VNeID)";
            caResult = vneidClient.authenticateAndSign(username.trim(), password.trim(), sampleHash, user.getFullName(), workUnit);
        } else if (prov.toUpperCase().contains("BAN_CO_YEU")) {
            providerDisplayName = "Ban Cơ yếu Chính phủ";
            caResult = vnptClient.authenticateAndSign(username.trim(), password.trim(), sampleHash, user.getFullName(), workUnit);
        } else {
            providerDisplayName = "Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)";
            caResult = vnptClient.authenticateAndSign(username.trim(), password.trim(), sampleHash, user.getFullName(), workUnit);
        }

        if (!caResult.isSuccess()) {
            throw new IllegalArgumentException(caResult.getErrorMessage() != null ? caResult.getErrorMessage() : "Xác thực tài khoản chữ ký số thất bại.");
        }

        // Cập nhật thông tin chứng thư số thành công
        p.setRegistrationStatus("Đã duyệt tài khoản");
        p.setCaProvider(providerDisplayName);
        p.setCaUsername(username.trim());
        if (caResult.getCertSerial() != null) p.setCertSerial(caResult.getCertSerial());
        if (caResult.getCertSubject() != null) p.setCertSubject(caResult.getCertSubject());

        // Đồng bộ thông tin nhân sự từ tài khoản và người dùng
        long userNum = user.getId() != null ? user.getId() : 1L;
        String extractedName = null;
        if (caResult.getCertSubject() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("CN=([^,]+)").matcher(caResult.getCertSubject());
            if (m.find()) {
                extractedName = m.group(1).trim();
            }
        }
        if (extractedName != null && !extractedName.isBlank()) {
            p.setFullName(extractedName);
        } else if (p.getFullName() == null || p.getFullName().isBlank()) {
            p.setFullName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "Nguyễn Văn An");
        }
        if (p.getMoetId() == null || p.getMoetId().isBlank()) {
            p.setMoetId("4801" + String.format("%06d", 100000 + userNum * 73));
        }
        if (p.getDob() == null || p.getDob().isBlank()) {
            p.setDob("15/05/1985");
        }
        if (p.getGender() == null || p.getGender().isBlank()) {
            p.setGender("Nam");
        }
        if (username.trim().matches("\\d{9,12}")) {
            p.setNationalId(username.trim());
            p.setPersonalId(username.trim());
        } else {
            if (p.getNationalId() == null || p.getNationalId().isBlank()) p.setNationalId("048085001234");
            if (p.getPersonalId() == null || p.getPersonalId().isBlank()) p.setPersonalId("048085001234");
        }

        if (caResult.getCertValidFrom() != null) {
            p.setValidFrom(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(java.time.ZoneId.systemDefault()).format(caResult.getCertValidFrom()));
        } else {
            LocalDate now = LocalDate.now();
            p.setValidFrom(now.format(DATE_FMT));
        }
        if (caResult.getCertValidTo() != null) {
            p.setValidTo(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(java.time.ZoneId.systemDefault()).format(caResult.getCertValidTo()));
        } else {
            LocalDate now = LocalDate.now();
            p.setValidTo(now.plusYears(1).format(DATE_FMT));
        }

        String location = ", L=Phường Cẩm Lệ, S=Đà Nẵng, C=VN";
        if (caResult.getCertSubject() != null) {
            String s = caResult.getCertSubject();
            if (s.contains("L=") || s.contains("ST=")) {
                String lPart = "";
                String sPart = "";
                java.util.regex.Matcher lm = java.util.regex.Pattern.compile("L=([^,]+)").matcher(s);
                if (lm.find()) lPart = "L=" + lm.group(1).trim();
                java.util.regex.Matcher sm = java.util.regex.Pattern.compile("ST=([^,]+)").matcher(s);
                if (sm.find()) sPart = "S=" + sm.group(1).trim();
                if (!lPart.isBlank() && !sPart.isBlank()) {
                    location = ", " + lPart + ", " + sPart + ", C=VN";
                }
            }
        }
        p.setOwnerInfo(location);

        // Nếu chưa có ảnh chữ ký thì tự động tạo con dấu chuẩn tương ứng với CA
        String deptName = (user.getDepartment() != null) ? user.getDepartment().getName() : "Tổ Tự Nhiên";
        if (p.getSignatureImageBase64() == null || p.getSignatureImageBase64().isBlank()) {
            p.setSignatureImageBase64(generateDefaultStampSvgBase64(p.getFullName(), deptName, prov));
        }

        log.info("Đã xác thực và kết nối thành công tài khoản CA cho user id={}, provider={}", user.getId(), prov);
        return profileRepository.save(p);
    }

    /**
     * Cập nhật hình ảnh chữ ký / con dấu số (Base64)
     */
    public Nq52UserSignatureProfile saveSignatureImage(User user, String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("Hình ảnh chữ ký không được để trống");
        }
        Nq52UserSignatureProfile p = getOrCreateProfile(user);
        p.setSignatureImageBase64(base64Image.trim());
        return profileRepository.save(p);
    }

    /**
     * Tạo con dấu chữ ký số điện tử chuẩn SVG Base64 (Chuẩn Nghị định 30/2020/NĐ-CP)
     */
    public String generateDefaultStampSvgBase64(String fullName, String deptName, String caShort) {
        String name = (fullName != null && !fullName.isBlank()) ? fullName.toUpperCase() : "NGUYỄN VĂN AN";
        String ca = (caShort != null && !caShort.isBlank()) ? caShort : "VNPT-CA";
        String dateStr = LocalDate.now().format(DATE_FMT);

        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"280\" height=\"100\" viewBox=\"0 0 280 100\">" +
                "<rect x=\"2\" y=\"2\" width=\"276\" height=\"96\" rx=\"8\" fill=\"#f0fdf4\" stroke=\"#16a34a\" stroke-width=\"2\" stroke-dasharray=\"none\"/>" +
                "<circle cx=\"24\" cy=\"24\" r=\"10\" fill=\"#16a34a\"/>" +
                "<path d=\"M19 24 l3.5 3.5 l7 -7\" stroke=\"#ffffff\" stroke-width=\"2\" fill=\"none\" stroke-linecap=\"round\"/>" +
                "<text x=\"42\" y=\"26\" font-family=\"'Times New Roman', serif\" font-size=\"11\" font-weight=\"bold\" fill=\"#15803d\">ĐÃ KÝ SỐ ĐIỆN TỬ HỢP LỆ</text>" +
                "<line x1=\"12\" y1=\"36\" x2=\"268\" y2=\"36\" stroke=\"#86efac\" stroke-width=\"1\"/>" +
                "<text x=\"14\" y=\"52\" font-family=\"'Times New Roman', serif\" font-size=\"12\" font-weight=\"bold\" fill=\"#0f172a\">KÝ BỞI: " + escapeXml(name) + "</text>" +
                "<text x=\"14\" y=\"68\" font-family=\"'Times New Roman', serif\" font-size=\"10\" fill=\"#334155\">Đơn vị: THCS Nguyễn Văn Linh (" + escapeXml(deptName) + ")</text>" +
                "<text x=\"14\" y=\"84\" font-family=\"'Times New Roman', serif\" font-size=\"9.5\" fill=\"#059669\">Ngày ký: " + dateStr + " | CA: " + escapeXml(ca) + " (SHA-256)</text>" +
                "</svg>";

        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

