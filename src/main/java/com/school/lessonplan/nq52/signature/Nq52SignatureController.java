package com.school.lessonplan.nq52.signature;

import com.school.lessonplan.nq52.signature.dto.*;
import com.school.lessonplan.security.CurrentUser;
import com.school.lessonplan.user.User;
import com.school.lessonplan.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nq52/signatures")
public class Nq52SignatureController {

    private final Nq52SignatureService signatureService;
    private final UserRepository userRepository;

    public Nq52SignatureController(Nq52SignatureService signatureService,
                                   UserRepository userRepository) {
        this.signatureService = signatureService;
        this.userRepository = userRepository;
    }

    /**
     * Chuẩn bị thông tin và mã băm SHA-256 trước khi ký
     */
    @GetMapping("/prepare")
    public ResponseEntity<PrepareSigningResponse> prepareSigning(
            @RequestParam("docType") String docType,
            @RequestParam("docId") Long docId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        PrepareSigningResponse res = signatureService.prepareSigning(docType, docId, user);
        return ResponseEntity.ok(res);
    }

    /**
     * Ký số trực tiếp bằng Username & Password của tài khoản chứng thư số (VNPT SmartCA, Viettel Cloud-CA, VNeID)
     */
    @PostMapping("/direct-sign")
    public ResponseEntity<?> directSignWithCertificate(
            @RequestBody CertificateDirectSignRequest request,
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest) {
        User user = getUser(currentUser);
        try {
            Nq52DigitalSignature sig = signatureService.directSignWithCertificate(request, user, httpRequest);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("signatureId", sig.getId());
            res.put("signerName", sig.getSignerNameSnapshot());
            res.put("caProvider", sig.getCaProvider());
            res.put("certSerial", sig.getCertSerial());
            res.put("certSubject", sig.getCertSubject());
            res.put("signedAt", sig.getSignedAt().toString());
            res.put("message", "Xác thực tài khoản thành công! Đã ký số điện tử qua cổng API " + sig.getCaProvider() + ".");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Lỗi xử lý ký số: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Khởi tạo giao dịch ký số từ xa (Remote Signing - VNPT SmartCA / Viettel Cloud-CA / VNeID)
     */
    @PostMapping("/remote/initiate")
    public ResponseEntity<RemoteSignInitiateResponse> initiateRemoteSigning(
            @RequestBody RemoteSignInitiateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        User user = getUser(currentUser);
        RemoteSignInitiateResponse res = signatureService.initiateRemoteSigning(request, user);
        return ResponseEntity.ok(res);
    }

    /**
     * Kiểm tra trạng thái ký số từ xa Realtime (Polling từ Web App)
     */
    @GetMapping("/remote/status/{txId}")
    public ResponseEntity<RemoteSignStatusResponse> getRemoteSignStatus(
            @PathVariable("txId") String txId,
            HttpServletRequest httpRequest) {
        RemoteSignStatusResponse res = signatureService.checkRemoteSignStatus(txId, httpRequest);
        return ResponseEntity.ok(res);
    }

    /**
     * Mô phỏng xác thực trên ứng dụng di động (Realtime App Approval qua FaceID / Vân tay)
     */
    @PostMapping("/remote/simulate-app-approval/{txId}")
    public ResponseEntity<RemoteSignStatusResponse> simulateAppApproval(
            @PathVariable("txId") String txId,
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest) {
        User user = getUser(currentUser);
        RemoteSignStatusResponse res = signatureService.simulateAppApproval(txId, user, httpRequest);
        return ResponseEntity.ok(res);
    }

    /**
     * Xác nhận ký số từ xa bằng mã OTP hoặc xác nhận qua App
     */
    @PostMapping("/remote/confirm")
    public ResponseEntity<?> confirmRemoteSigning(
            @RequestBody RemoteSignConfirmRequest request,
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest) {
        User user = getUser(currentUser);
        try {
            Nq52DigitalSignature sig = signatureService.confirmRemoteSigning(request, user, httpRequest);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("signatureId", sig.getId());
            res.put("signerName", sig.getSignerNameSnapshot());
            res.put("signedAt", sig.getSignedAt().toString());
            res.put("message", "Ký số từ xa (" + sig.getCaProvider() + ") thành công!");
            return ResponseEntity.ok(res);
        } catch (IllegalStateException | AccessDeniedException | IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Lỗi trong quá trình ký số: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Ký số bằng USB Token phần cứng (Dành cho Ban Giám Hiệu)
     */
    @PostMapping("/usb-token/sign")
    public ResponseEntity<?> signWithUsbToken(
            @RequestBody UsbTokenSignRequest request,
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest httpRequest) {
        User user = getUser(currentUser);
        try {
            Nq52DigitalSignature sig = signatureService.signWithUsbToken(request, user, httpRequest);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("signatureId", sig.getId());
            res.put("signerName", sig.getSignerNameSnapshot());
            res.put("signedAt", sig.getSignedAt().toString());
            res.put("message", "Ký số USB Token phần cứng Ban Giám Hiệu thành công!");
            return ResponseEntity.ok(res);
        } catch (IllegalStateException | AccessDeniedException | IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Lỗi trong quá trình ký số USB Token: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /**
     * Xác thực chữ ký số thời gian thực (Realtime Validation)
     */
    @GetMapping("/verify-realtime")
    public ResponseEntity<RealtimeVerificationResultDto> verifyRealtime(
            @RequestParam("docType") String docType,
            @RequestParam("docId") Long docId,
            @RequestParam(value = "sigId", required = false) Long sigId) {
        RealtimeVerificationResultDto res = signatureService.verifySignatureRealtime(docType, docId, sigId);
        return ResponseEntity.ok(res);
    }

    /**
     * Tra cứu danh sách chữ ký số của tài liệu
     */
    @GetMapping("/document/{docType}/{docId}")
    public ResponseEntity<List<SignatureItemDto>> getSignatures(
            @PathVariable("docType") String docType,
            @PathVariable("docId") Long docId) {
        List<SignatureItemDto> list = signatureService.getSignaturesForDocument(docType, docId);
        return ResponseEntity.ok(list);
    }

    private User getUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Vui lòng đăng nhập để tiếp tục.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin tài khoản."));
    }
}

