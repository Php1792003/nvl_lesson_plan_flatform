package com.school.lessonplan.nq52.signature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class Nq52UserSignatureProfileService {

    private static final Logger log = LoggerFactory.getLogger(Nq52UserSignatureProfileService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Nq52UserSignatureProfileRepository profileRepository;
    private final VnptSmartCaClient vnptClient;
    private final ViettelCloudCaClient viettelClient;
    private final VneIdCaClient vneidClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            if (p.getAutoAttachSignature() == null) {
                p.setAutoAttachSignature(true);
            }
            // Không tự động điền email/tài khoản hệ thống vào caUsername nếu chưa liên kết chứng thư số
            if ((p.getCertSerial() == null || p.getCertSerial().isBlank())
                    && p.getCaUsername() != null && user.getEmail() != null && p.getCaUsername().equalsIgnoreCase(user.getEmail())) {
                p.setCaUsername("");
            }
            // Chỉ trích xuất thông tin khi chứng thư số đã liên kết hợp lệ
            if (p.getCertSerial() != null && !p.getCertSerial().isBlank() && p.isLinkValid()) {
                if ((p.getGender() == null || p.getGender().isBlank())
                        && (p.getCertSubject() != null && !p.getCertSubject().isBlank() || p.getLastCaResponse() != null && !p.getLastCaResponse().isBlank())) {
                    String detected = extractGender(p.getLastCaResponse(), p.getCertSubject(), p.getCaUsername(), p.getFullName());
                    if (detected != null && !detected.isBlank()) {
                        p.setGender(detected);
                    }
                }
                if ((p.getNationalId() == null || p.getNationalId().isBlank() || p.getPersonalId() == null || p.getPersonalId().isBlank())
                        && (p.getCertSubject() != null && !p.getCertSubject().isBlank() || p.getLastCaResponse() != null && !p.getLastCaResponse().isBlank())) {
                    String cccd = extractCccd(p.getCertSubject(), p.getLastCaResponse(), p.getCaUsername());
                    if (cccd != null && !cccd.isBlank()) {
                        if (p.getNationalId() == null || p.getNationalId().isBlank()) p.setNationalId(cccd);
                        if (p.getPersonalId() == null || p.getPersonalId().isBlank()) p.setPersonalId(cccd);
                    }
                }
            }
            if (p.getCertSerial() != null && !p.getCertSerial().isBlank() && p.isLinkExpired()) {
                p.setRegistrationStatus("Liên kết đã hết hạn (quá 7 ngày)");
                return profileRepository.save(p);
            }
            return p;
        }

        Nq52UserSignatureProfile p = new Nq52UserSignatureProfile();
        p.setUser(user);

        // Thông tin nhân sự ban đầu để trống (làm mới hoàn toàn)
        p.setMoetId("");
        p.setFullName("");
        p.setDob("");
        p.setGender("");
        p.setNationalId("");
        p.setPersonalId("");

        // Thông tin đăng ký tài khoản chữ ký số ban đầu
        p.setRegistrationStatus("Chưa liên kết");
        p.setSignatureType("REMOTE SIGNING");
        p.setCaProvider("Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)");
        p.setCaUsername("");

        // Thông tin chứng thư số ban đầu (khi chưa liên kết Cổng CA thật)
        p.setCertSerial("");
        p.setCertSubject("");
        p.setValidFrom("");
        p.setValidTo("");
        p.setOwnerInfo("");

        p.setAutoAttachSignature(true);
        // Khi chưa liên kết xác thực tài khoản ký số thì không có bất kỳ chữ ký mẫu nào
        p.setSignatureImageBase64("");

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
     * Tra cứu & Liên kết với nhà cung cấp chữ ký số (VNPT-CA / Viettel-CA / VNeID)
     * Đồng thời cập nhật trạng thái đã duyệt và các thông số chứng thư số realtime từ Cổng CA
     */
    public Nq52UserSignatureProfile authenticateAndLinkCa(User user, String provider, String username) {
        return authenticateAndLinkCa(user, provider, username, null);
    }

    public Nq52UserSignatureProfile authenticateAndLinkCa(User user, String provider, String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu nhập tên đăng nhập hoặc số CCCD");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu nhập mật khẩu / Mã PIN tài khoản chữ ký số.");
        }

        Nq52UserSignatureProfile p = getOrCreateProfile(user);
        String prov = (provider != null && !provider.isBlank()) ? provider : "VNPT_SMARTCA";
        String workUnit = "Trường THCS Nguyễn Văn Linh";

        CaSignResult caResult;
        String providerDisplayName;
        if (prov.toUpperCase().contains("VIETTEL") || "VIETTEL_CLOUD_CA".equalsIgnoreCase(prov)) {
            providerDisplayName = "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel-CA)";
            caResult = viettelClient.getCertificateOnly(username.trim(), password.trim(), user.getFullName(), workUnit);
        } else if (prov.toUpperCase().contains("VNEID") || "VN_EID".equalsIgnoreCase(prov)) {
            providerDisplayName = "Cục Cảnh sát QLHC về TTXH - Bộ Công An (VNeID)";
            caResult = vneidClient.getCertificateOnly(username.trim(), password.trim(), user.getFullName(), workUnit);
        } else if (prov.toUpperCase().contains("BAN_CO_YEU")) {
            providerDisplayName = "Ban Cơ yếu Chính phủ";
            caResult = vnptClient.getCertificateOnly(username.trim(), password.trim(), user.getFullName(), workUnit);
        } else {
            providerDisplayName = "Tập đoàn Bưu chính Viễn thông Việt Nam (VNPT-CA)";
            caResult = vnptClient.getCertificateOnly(username.trim(), password.trim(), user.getFullName(), workUnit);
        }

        if (!caResult.isSuccess()) {
            if (caResult.getRawResponse() != null) {
                p.setLastCaResponse(caResult.getRawResponse());
                profileRepository.save(p);
            }
            throw new CaAuthenticationException(
                    caResult.getErrorMessage() != null ? caResult.getErrorMessage() : "Xác thực tài khoản chữ ký số thất bại.",
                    caResult.getRawResponse()
            );
        }

        if (caResult.getRawResponse() != null) {
            p.setLastCaResponse(caResult.getRawResponse());
        }

        // Cập nhật thông tin chứng thư số thành công
        p.setRegistrationStatus("Đã liên kết tài khoản CA");
        p.setCaProvider(providerDisplayName);
        p.setCaUsername(username.trim());
        if (password != null && !password.isBlank()) {
            p.setCaPassword(password.trim());
        }
        if (caResult.getCertSerial() != null) p.setCertSerial(caResult.getCertSerial());
        if (caResult.getCertSubject() != null) p.setCertSubject(caResult.getCertSubject());

        // Đồng bộ thông tin nhân sự từ tài khoản và người dùng
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
            p.setFullName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "");
        }
        if (p.getMoetId() == null) p.setMoetId("");

        // Trích xuất CCCD và Cập nhật CMND/CCCD, Số định danh cá nhân
        String cccd = extractCccd(caResult.getCertSubject(), caResult.getRawResponse(), username);
        if (cccd != null && !cccd.isBlank()) {
            p.setNationalId(cccd);
            p.setPersonalId(cccd);
        } else if (username.trim().matches("\\d{9,12}")) {
            p.setNationalId(username.trim());
            p.setPersonalId(username.trim());
        } else {
            if (p.getNationalId() == null) p.setNationalId("");
            if (p.getPersonalId() == null) p.setPersonalId("");
        }

        // Điền chính xác thông tin Giới tính từ chứng thư số / Cổng CA
        String detectedGender = extractGender(caResult.getRawResponse(), caResult.getCertSubject(), username, p.getFullName());
        p.setGender(detectedGender);

        // Trích xuất ngày sinh nếu có trong JSON phản hồi CA
        String dobFromJson = extractDob(caResult.getRawResponse());
        if (dobFromJson != null && !dobFromJson.isBlank()) {
            p.setDob(dobFromJson);
        } else if (p.getDob() == null) {
            p.setDob("");
        }

        if (caResult.getCertValidFrom() != null) {
            p.setValidFrom(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(java.time.ZoneId.systemDefault()).format(caResult.getCertValidFrom()));
        } else {
            p.setValidFrom("");
        }
        if (caResult.getCertValidTo() != null) {
            p.setValidTo(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(java.time.ZoneId.systemDefault()).format(caResult.getCertValidTo()));
        } else {
            p.setValidTo("");
        }

        String location = "";
        if (caResult.getCertSubject() != null) {
            String s = caResult.getCertSubject();
            if (s.contains("L=") || s.contains("ST=")) {
                String lPart = "";
                String sPart = "";
                java.util.regex.Matcher lm = java.util.regex.Pattern.compile("L=([^,]+)").matcher(s);
                if (lm.find()) lPart = lm.group(1).trim();
                java.util.regex.Matcher sm = java.util.regex.Pattern.compile("ST=([^,]+)").matcher(s);
                if (sm.find()) sPart = sm.group(1).trim();
                if (!lPart.isBlank() && !sPart.isBlank()) {
                    location = lPart + ", " + sPart + ", C=VN";
                } else if (!lPart.isBlank()) {
                    location = lPart + ", C=VN";
                } else {
                    location = s;
                }
            } else {
                location = s;
            }
        }
        p.setOwnerInfo(location);

        p.setRegistrationStatus("Đã liên kết tài khoản CA");
        p.setLinkedAt(Instant.now());
        p.setWarningMessage(caResult.getWarningMessage());

        // Khi liên kết thành công với Cổng CA, tự động tạo con dấu chuẩn tương ứng với CA nếu chưa có
        if (p.getSignatureImageBase64() == null || p.getSignatureImageBase64().isBlank()) {
            String deptName = (user.getDepartment() != null) ? user.getDepartment().getName() : "Trường THCS Nguyễn Văn Linh";
            p.setSignatureImageBase64(generateDefaultStampSvgBase64(p.getFullName(), deptName, providerDisplayName));
        }

        log.info("Đã xác thực và kết nối thành công tài khoản CA cho user id={}, provider={}", user.getId(), prov);
        return profileRepository.save(p);
    }

    /**
     * Hủy liên kết và làm mới toàn bộ thông tin nhân sự và chứng thư số về trạng thái ban đầu
     * Làm mới toàn bộ thông tin đã nhập: Mã định danh, Ngày sinh, Họ tên, CMND/CCCD, Giới tính, Số định danh cá nhân
     * Khi chưa liên kết xác thực tài khoản ký số thì không hiển thị bất kỳ chữ ký mẫu nào
     */
    public Nq52UserSignatureProfile unlinkCa(User user) {
        Nq52UserSignatureProfile p = getOrCreateProfile(user);
        p.setRegistrationStatus("Chưa liên kết");
        p.setLinkedAt(null);
        p.setCertSerial("");
        p.setCertSubject("");
        p.setValidFrom("");
        p.setValidTo("");
        p.setOwnerInfo("");
        p.setCaUsername("");
        p.setCaPassword(null);
        p.setLastCaResponse(null);
        p.setSignatureImageBase64(""); // Khi chưa liên kết thì không hiển thị bất kỳ chữ ký mẫu nào

        // Làm mới toàn bộ thông tin nhân sự
        p.setMoetId("");
        p.setDob("");
        p.setFullName("");
        p.setNationalId("");
        p.setPersonalId("");
        p.setGender("");

        log.info("Đã hủy liên kết và làm mới toàn bộ thông tin hồ sơ cho user id={}", user.getId());
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
        String name = (fullName != null && !fullName.isBlank()) ? fullName.toUpperCase() : "";
        String ca = (caShort != null && !caShort.isBlank()) ? caShort : "VNPT-CA";
        String dateStr = LocalDate.now().format(DATE_FMT);

        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"280\" height=\"90\" viewBox=\"0 0 280 90\">" +
                "<rect x=\"2\" y=\"2\" width=\"276\" height=\"86\" rx=\"6\" fill=\"#f8fafc\" stroke=\"#2563eb\" stroke-width=\"1.5\" stroke-dasharray=\"none\"/>" +
                "<text x=\"14\" y=\"22\" font-family=\"'Times New Roman', serif\" font-size=\"12\" font-weight=\"bold\" fill=\"#1e40af\">CHỮ KÝ SỐ ĐIỆN TỬ HỢP LỆ</text>" +
                "<line x1=\"12\" y1=\"30\" x2=\"268\" y2=\"30\" stroke=\"#cbd5e1\" stroke-width=\"1\"/>" +
                "<text x=\"14\" y=\"46\" font-family=\"'Times New Roman', serif\" font-size=\"12\" font-weight=\"bold\" fill=\"#0f172a\">KÝ BỞI: " + escapeXml(name) + "</text>" +
                "<text x=\"14\" y=\"62\" font-family=\"'Times New Roman', serif\" font-size=\"10.5\" fill=\"#334155\">Đơn vị: THCS Nguyễn Văn Linh (" + escapeXml(deptName) + ")</text>" +
                "<text x=\"14\" y=\"78\" font-family=\"'Times New Roman', serif\" font-size=\"9.5\" fill=\"#64748b\">Ngày ký: " + dateStr + " | CA: " + escapeXml(ca) + "</text>" +
                "</svg>";

        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Trích xuất và xác định giới tính chính xác ("Nam" hoặc "Nữ") từ phản hồi CA (JSON, Cert Subject, CCCD)
     */
    public String extractGender(String rawResponse, String certSubject, String username, String fullName) {
        // 1. Kiểm tra trường tường minh trong JSON phản hồi trực tiếp từ CA
        if (rawResponse != null && !rawResponse.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(rawResponse);
                String fromJson = findGenderInJsonNode(root);
                if (fromJson != null) {
                    return fromJson;
                }
            } catch (Exception ignored) {}
        }

        // 2. Trích xuất mã CCCD 12 số
        String cccd = extractCccd(certSubject, rawResponse, username);

        // 3. Phân tích giới tính theo quy chuẩn CCCD 12 số của Bộ Công An (Nghị định 137/2015/NĐ-CP)
        // Ký tự thứ 4 (index 3) quy định thế kỷ và giới tính:
        // Chẵn (0, 2, 4, 6, 8) -> Nam; Lẻ (1, 3, 5, 7, 9) -> Nữ.
        if (cccd != null && cccd.length() == 12) {
            char g = cccd.charAt(3);
            if (g == '0' || g == '2' || g == '4' || g == '6' || g == '8') {
                return "Nam";
            } else if (g == '1' || g == '3' || g == '5' || g == '7' || g == '9') {
                return "Nữ";
            }
        }

        // 4. Nhận diện qua tên tiếng Việt nếu không có CCCD
        String name = (fullName != null && !fullName.isBlank()) ? fullName : "";
        if (name.isBlank() && certSubject != null) {
            Matcher m = Pattern.compile("CN=([^,]+)").matcher(certSubject);
            if (m.find()) name = m.group(1).trim();
        }
        if (!name.isBlank()) {
            String lower = name.toLowerCase();
            if (lower.matches(".*\\bthị\\b.*") || lower.matches(".*\\bthi\\b.*")) {
                return "Nữ";
            }
            if (lower.matches(".*\\bvăn\\b.*") || lower.matches(".*\\bvan\\b.*")) {
                return "Nam";
            }
        }

        return "Nam";
    }

    /**
     * Trích xuất mã định danh CCCD 12 số từ certSubject, username hoặc rawResponse
     */
    public String extractCccd(String certSubject, String rawResponse, String username) {
        // Ưu tiên tìm trong certSubject: UID=CCCD:066194013360 hoặc UID=066194013360
        if (certSubject != null && !certSubject.isBlank()) {
            Matcher m = Pattern.compile("(?:CCCD[:_\\s]*|UID=(?:CCCD:)?)\\b(\\d{12})\\b").matcher(certSubject);
            if (m.find()) {
                return m.group(1);
            }
            Matcher m2 = Pattern.compile("\\b(\\d{12})\\b").matcher(certSubject);
            if (m2.find()) {
                return m2.group(1);
            }
        }

        // Kiểm tra trong username nếu người dùng nhập số CCCD 12 số
        if (username != null && username.trim().matches("\\d{12}")) {
            return username.trim();
        }

        // Kiểm tra trong rawResponse
        if (rawResponse != null && !rawResponse.isBlank()) {
            Matcher m = Pattern.compile("(?:CCCD[:_\\s]*|UID=(?:CCCD:)?)\\b(\\d{12})\\b").matcher(rawResponse);
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }

    /**
     * Trích xuất ngày sinh nếu Cổng CA có trả về trường ngày sinh
     */
    public String extractDob(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return findDobInJsonNode(root);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String findGenderInJsonNode(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey().toLowerCase();
                JsonNode val = entry.getValue();
                if (key.equals("gender") || key.equals("sex") || key.equals("gioi_tinh") || key.equals("gioitinh") || key.equals("gender_name")) {
                    String text = val.asText("").trim().toLowerCase();
                    if (text.equals("female") || text.equals("f") || text.equals("nữ") || text.equals("nu") || text.equals("1")) {
                        return "Nữ";
                    } else if (text.equals("male") || text.equals("m") || text.equals("nam") || text.equals("0")) {
                        return "Nam";
                    }
                }
                String nested = findGenderInJsonNode(val);
                if (nested != null) return nested;
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                String found = findGenderInJsonNode(item);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String findDobInJsonNode(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey().toLowerCase();
                JsonNode val = entry.getValue();
                if (key.equals("dob") || key.equals("birth_date") || key.equals("birthday") || key.equals("date_of_birth") || key.equals("ngay_sinh")) {
                    String text = val.asText("").trim();
                    if (!text.isBlank()) {
                        if (text.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                            String y = text.substring(0, 4);
                            String m = text.substring(5, 7);
                            String d = text.substring(8, 10);
                            return d + "/" + m + "/" + y;
                        } else if (text.matches("\\d{2}/\\d{2}/\\d{4}")) {
                            return text;
                        }
                    }
                }
                String nested = findDobInJsonNode(val);
                if (nested != null) return nested;
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                String found = findDobInJsonNode(item);
                if (found != null) return found;
            }
        }
        return null;
    }
}

