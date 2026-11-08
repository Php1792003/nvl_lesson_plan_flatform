package com.school.lessonplan.nq52.signature;

import com.school.lessonplan.nq52.signature.dto.UserSignatureProfileForm;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class Nq52SignatureProfileController {

    private static final Logger log = LoggerFactory.getLogger(Nq52SignatureProfileController.class);

    private final Nq52UserSignatureProfileService profileService;
    private final UserRepository userRepository;

    public Nq52SignatureProfileController(Nq52UserSignatureProfileService profileService,
                                          UserRepository userRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    /**
     * Màn hình chính Quản lý Tài khoản Chữ ký số & Dán chữ ký NQ 52
     */
    @GetMapping("/nq52/digital-signature")
    public String viewSignatureProfilePage(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        User user = resolveUser(currentUser);
        Nq52UserSignatureProfile profile = profileService.getOrCreateProfile(user);

        model.addAttribute("profile", profile);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Quản lý Tài khoản Chữ ký số");
        return "nq52/signature-profile";
    }

    /**
     * API Lưu thông tin nhân sự và cấu hình đăng ký chữ ký số
     */
    @PostMapping("/api/nq52/signature-profile/save")
    @ResponseBody
    public ResponseEntity<?> saveProfile(@AuthenticationPrincipal CurrentUser currentUser,
                                         @RequestBody UserSignatureProfileForm form) {
        try {
            User user = resolveUser(currentUser);
            Nq52UserSignatureProfile updated = profileService.updateProfile(user, form);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "Đã lưu thông tin nhân sự và cấu hình tài khoản chữ ký số thành công!");
            result.put("registrationStatus", updated.getRegistrationStatus());
            result.put("caProvider", updated.getCaProvider());
            result.put("caUsername", updated.getCaUsername());
            result.put("moetId", updated.getMoetId() != null ? updated.getMoetId() : "");
            result.put("fullName", updated.getFullName() != null ? updated.getFullName() : "");
            result.put("dob", updated.getDob() != null ? updated.getDob() : "");
            result.put("nationalId", updated.getNationalId() != null ? updated.getNationalId() : "");
            result.put("gender", updated.getGender() != null ? updated.getGender() : "Nam");
            result.put("personalId", updated.getPersonalId() != null ? updated.getPersonalId() : "");
            result.put("certSerial", updated.getCertSerial());
            result.put("certSubject", updated.getCertSubject());
            result.put("validFrom", updated.getValidFrom());
            result.put("validTo", updated.getValidTo());
            result.put("ownerInfo", updated.getOwnerInfo());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi lưu cấu hình chữ ký số:", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Không thể lưu cấu hình chữ ký số."
            ));
        }
    }

    /**
     * API Đăng nhập và liên kết tài khoản chứng thư số (VNPT-CA, Viettel-CA, VNeID)
     */
    @PostMapping("/api/nq52/signature-profile/link-ca")
    @ResponseBody
    public ResponseEntity<?> linkCaAccount(@AuthenticationPrincipal CurrentUser currentUser,
                                           @RequestBody Map<String, String> request) {
        try {
            User user = resolveUser(currentUser);
            String provider = request.get("provider");
            String username = request.get("username");
            String password = request.get("password");

            Nq52UserSignatureProfile updated = profileService.authenticateAndLinkCa(user, provider, username, password);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("success", true);
            if (updated.getWarningMessage() != null && !updated.getWarningMessage().isBlank()) {
                result.put("message", "Tra cứu và liên kết chứng thư số thành công! (" + updated.getWarningMessage() + ")");
            } else {
                result.put("message", "Tra cứu và liên kết chứng thư số thành công!");
            }
            result.put("registrationStatus", updated.getRegistrationStatus());
            result.put("caProvider", updated.getCaProvider());
            result.put("caUsername", updated.getCaUsername());
            result.put("moetId", updated.getMoetId() != null ? updated.getMoetId() : "");
            result.put("fullName", updated.getFullName() != null ? updated.getFullName() : "");
            result.put("dob", updated.getDob() != null ? updated.getDob() : "");
            result.put("nationalId", updated.getNationalId() != null ? updated.getNationalId() : "");
            result.put("gender", updated.getGender() != null ? updated.getGender() : "Nam");
            result.put("personalId", updated.getPersonalId() != null ? updated.getPersonalId() : "");
            result.put("certSerial", updated.getCertSerial());
            result.put("certSubject", updated.getCertSubject());
            result.put("validFrom", updated.getValidFrom());
            result.put("validTo", updated.getValidTo());
            result.put("ownerInfo", updated.getOwnerInfo());
            result.put("signatureImageBase64", updated.getSignatureImageBase64() != null ? updated.getSignatureImageBase64() : "");
            result.put("lastCaResponse", updated.getLastCaResponse() != null ? updated.getLastCaResponse() : "");
            result.put("linkedAt", updated.getLinkedAtFormatted());
            result.put("linkExpireAt", updated.getLinkExpireAtFormatted());
            result.put("daysRemaining", updated.getDaysRemaining());
            result.put("isLinkValid", updated.isLinkValid());
            return ResponseEntity.ok(result);
        } catch (CaAuthenticationException e) {
            log.warn("Lỗi xác thực chứng thư số: {}", e.getMessage());
            Map<String, Object> errMap = new java.util.LinkedHashMap<>();
            errMap.put("success", false);
            errMap.put("message", e.getMessage());
            errMap.put("lastCaResponse", e.getRawResponse() != null ? e.getRawResponse() : "");
            return ResponseEntity.badRequest().body(errMap);
        } catch (Exception e) {
            log.warn("Lỗi đăng nhập chứng thư số: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Xác thực tài khoản chữ ký số thất bại."
            ));
        }
    }

    /**
     * API Hủy liên kết / Đặt lại tài khoản chứng thư số
     */
    @PostMapping("/api/nq52/signature-profile/unlink")
    @ResponseBody
    public ResponseEntity<?> unlinkCaAccount(@AuthenticationPrincipal CurrentUser currentUser) {
        try {
            User user = resolveUser(currentUser);
            profileService.unlinkCa(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy liên kết tài khoản chứng thư số thành công."
            ));
        } catch (Exception e) {
            log.warn("Lỗi hủy liên kết chứng thư số: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi hủy liên kết: " + e.getMessage()
            ));
        }
    }

    /**
     * API Lưu hình ảnh con dấu / chữ ký tay (Base64)
     */
    @PostMapping("/api/nq52/signature-profile/upload-image")
    @ResponseBody
    public ResponseEntity<?> uploadSignatureImage(@AuthenticationPrincipal CurrentUser currentUser,
                                                  @RequestBody Map<String, String> request) {
        try {
            User user = resolveUser(currentUser);
            String base64Image = request.get("signatureImageBase64");
            Nq52UserSignatureProfile updated = profileService.saveSignatureImage(user, base64Image);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã dán con dấu chữ ký điện tử thành công!",
                    "signatureImageBase64", updated.getSignatureImageBase64()
            ));
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh chữ ký:", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Lỗi khi lưu ảnh chữ ký."
            ));
        }
    }

    private User resolveUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new org.springframework.security.access.AccessDeniedException("Người dùng chưa đăng nhập");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Không tìm thấy thông tin tài khoản"));
    }
}
