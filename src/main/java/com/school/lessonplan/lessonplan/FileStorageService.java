package com.school.lessonplan.lessonplan;

import com.school.lessonplan.common.exception.BusinessException;
import com.school.lessonplan.common.exception.FieldValidationException;
import com.school.lessonplan.config.AppProperties;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final AppProperties appProperties;
    private final Tika tika = new Tika();

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "ppt", "pptx");

    private static final Map<String, Set<String>> MIME_BY_EXT = Map.of(
        "pdf", Set.of("application/pdf"),
        "doc", Set.of("application/msword", "application/x-tika-msoffice"),
        "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/x-tika-ooxml"),
        "ppt", Set.of("application/vnd.ms-powerpoint", "application/x-tika-msoffice"),
        "pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/zip", "application/x-tika-ooxml")
    );

    public FileStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        initStorage();
    }

    private void initStorage() {
        try {
            Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
            Path tempPath = Paths.get(appProperties.getStorage().getTempDir()).toAbsolutePath().normalize();
            Files.createDirectories(rootPath);
            Files.createDirectories(tempPath);
        } catch (IOException e) {
            log.error("Could not create storage directories", e);
        }
    }

    public StoredFileResult storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FieldValidationException("file", "Empty", "Vui lòng chọn file tải lên.");
        }

        // 1. Kiểm tra dung lượng (10 MB)
        long maxBytes = (long) appProperties.getUpload().getMaxMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new FieldValidationException("file", "Size", "Tệp vượt quá " + appProperties.getUpload().getMaxMb() + " MB. Hãy nén lại rồi thử lại.");
        }

        // 2. Làm sạch tên file và kiểm tra path traversal
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FieldValidationException("file", "Extension", "Chỉ chấp nhận tệp PDF, Word hoặc PowerPoint.");
        }

        // 3. Kiểm tra MIME khai báo và Magic Bytes với Tika
        try (InputStream is = file.getInputStream()) {
            String detectedMime = tika.detect(is, originalFilename);
            Set<String> validMimes = MIME_BY_EXT.get(extension);
            if (validMimes == null || !validMimes.contains(detectedMime.toLowerCase())) {
                log.warn("MIME detection mismatch: file={}, declared={}, detected={}",
                    originalFilename, file.getContentType(), detectedMime);
                throw new FieldValidationException("file", "Extension", "Nội dung file không khớp với định dạng " + extension + ". Vui lòng kiểm tra lại file.");
            }
        } catch (IOException e) {
            throw new BusinessException("Không thể đọc nội dung file tải lên.");
        }

        // 4. Tính mã băm SHA-256
        String sha256;
        try (InputStream is = file.getInputStream()) {
            sha256 = calculateSha256(is);
        } catch (IOException e) {
            throw new BusinessException("Lỗi trong quá trình tính mã băm file.");
        }

        // 5. Lưu file vào đĩa bằng tên UUID ngẫu nhiên
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
        Path targetPath = rootPath.resolve(storedFilename);

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Không thể lưu file vào hệ thống lưu trữ.");
        }

        return new StoredFileResult(
            originalFilename,
            storedFilename,
            file.getContentType(),
            file.getSize(),
            sha256
        );
    }

    public Resource loadFileAsResource(String storedFilename) {
        try {
            Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(storedFilename).normalize();

            // Chống path traversal
            if (!filePath.startsWith(rootPath)) {
                throw new BusinessException("Đường dẫn file không hợp lệ.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("File không tồn tại trên hệ thống lưu trữ.");
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("Đường dẫn file không hợp lệ.");
        }
    }

    public boolean deletePhysicalFile(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return false;
        }
        try {
            Path rootPath = Paths.get(appProperties.getStorage().getRoot()).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(storedFilename).normalize();
            if (filePath.startsWith(rootPath) && Files.exists(filePath)) {
                return Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", storedFilename, e);
        }
        return false;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "tailieu";
        // Bỏ đường dẫn
        String name = Paths.get(filename).getFileName().toString();
        // Xoá ký tự nguy hiểm
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    private String calculateSha256(InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed", e);
        }
    }

    public record StoredFileResult(
        String originalFilename,
        String storedFilename,
        String contentType,
        long sizeBytes,
        String sha256
    ) {}
}
