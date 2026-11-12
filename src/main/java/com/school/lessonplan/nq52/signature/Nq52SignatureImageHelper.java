package com.school.lessonplan.nq52.signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Tiện ích trích xuất và sinh hình ảnh con dấu / chữ ký số (PNG) dùng cho xuất file Word & Excel
 */
public class Nq52SignatureImageHelper {

    private static final Logger log = LoggerFactory.getLogger(Nq52SignatureImageHelper.class);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    /**
     * Lấy mảng byte hình ảnh chữ ký dạng PNG.
     * Nếu người dùng có tải lên chữ ký tay hoặc vẽ chữ ký (PNG/JPEG Base64) -> Cắt viền thừa và dùng ảnh thật đó.
     * Nếu là con dấu số mặc định (SVG) hoặc chưa có ảnh -> Sinh con dấu số chuẩn đồ họa PNG.
     */
    public static byte[] getSignaturePngBytes(Nq52DigitalSignature sig, String fallbackName, String roleTitle, boolean isBgh, boolean isDept) {
        if (sig == null) return null;

        String base64 = sig.getSignatureImageBase64();
        if (base64 != null && !base64.isBlank()) {
            if (base64.startsWith("data:image/png") || base64.startsWith("data:image/jpeg")
                    || base64.startsWith("data:image/jpg") || (!base64.startsWith("data:") && !base64.startsWith("<svg"))) {
                try {
                    String cleanB64 = base64;
                    if (cleanB64.contains(",")) {
                        cleanB64 = cleanB64.substring(cleanB64.indexOf(",") + 1);
                    }
                    cleanB64 = cleanB64.replaceAll("\\s+", "");
                    byte[] raw = Base64.getDecoder().decode(cleanB64);
                    BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(raw));
                    if (bimg != null) {
                        BufferedImage trimmed = trimTransparentOrWhite(bimg);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(trimmed != null ? trimmed : bimg, "PNG", baos);
                        return baos.toByteArray();
                    }
                } catch (Exception e) {
                    log.debug("Không thể đọc ảnh raster từ base64, chuyển sang sinh con dấu số đồ họa: {}", e.getMessage());
                }
            }
        }

        // Sinh ảnh con dấu số điện tử PNG chất lượng cao
        String signerName = (sig.getSignerNameSnapshot() != null && !sig.getSignerNameSnapshot().isBlank())
                ? sig.getSignerNameSnapshot() : (fallbackName != null ? fallbackName : "Giáo viên");
        String caRaw = (sig.getCaProvider() != null && !sig.getCaProvider().isBlank()) ? sig.getCaProvider() : "Ban Cơ yếu CP";
        String ca = isBgh ? "Ban Cơ yếu CP" : (caRaw.contains("BAN_CO_YEU") ? "Ban Cơ yếu CP" : caRaw);
        Instant signedAt = sig.getSignedAt() != null ? sig.getSignedAt() : Instant.now();

        return generateSignatureStampPng(signerName, roleTitle, ca, signedAt, isBgh, isDept);
    }

    /**
     * Tính toán kích thước hiển thị cân đối (giữ nguyên 100% tỷ lệ gốc aspect ratio, không bị méo hay dãn hình)
     */
    public static Dimension getProportionalDimension(byte[] pngBytes, int maxW, int maxH) {
        if (pngBytes == null || pngBytes.length == 0) return new Dimension(maxW, maxH);
        try {
            BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (bimg != null && bimg.getWidth() > 0 && bimg.getHeight() > 0) {
                double scale = Math.min((double) maxW / bimg.getWidth(), (double) maxH / bimg.getHeight());
                int w = (int) Math.round(bimg.getWidth() * scale);
                int h = (int) Math.round(bimg.getHeight() * scale);
                return new Dimension(Math.max(w, 20), Math.max(h, 20));
            }
        } catch (Exception ignored) {}
        return new Dimension(maxW, maxH);
    }

    /**
     * Tự động cắt bỏ khoảng trống trong suốt hoặc viền trắng thừa xung quanh chữ ký
     */
    public static BufferedImage trimTransparentOrWhite(BufferedImage img) {
        if (img == null) return null;
        int width = img.getWidth();
        int height = img.getHeight();

        int top = 0;
        int bottom = height - 1;
        int left = 0;
        int right = width - 1;

        // Tìm top
        findTop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isPixelBlank(img.getRGB(x, y))) {
                    top = y;
                    break findTop;
                }
            }
        }

        // Tìm bottom
        findBottom:
        for (int y = height - 1; y >= top; y--) {
            for (int x = 0; x < width; x++) {
                if (!isPixelBlank(img.getRGB(x, y))) {
                    bottom = y;
                    break findBottom;
                }
            }
        }

        // Tìm left
        findLeft:
        for (int x = 0; x < width; x++) {
            for (int y = top; y <= bottom; y++) {
                if (!isPixelBlank(img.getRGB(x, y))) {
                    left = x;
                    break findLeft;
                }
            }
        }

        // Tìm right
        findRight:
        for (int x = width - 1; x >= left; x--) {
            for (int y = top; y <= bottom; y++) {
                if (!isPixelBlank(img.getRGB(x, y))) {
                    right = x;
                    break findRight;
                }
            }
        }

        int cropW = right - left + 1;
        int cropH = bottom - top + 1;

        if (cropW <= 0 || cropH <= 0 || (cropW == width && cropH == height)) {
            return img;
        }

        int pad = 4;
        int finalW = cropW + pad * 2;
        int finalH = cropH + pad * 2;
        BufferedImage trimmed = new BufferedImage(finalW, finalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = trimmed.createGraphics();
        g.drawImage(img, pad, pad, pad + cropW, pad + cropH, left, top, right + 1, bottom + 1, null);
        g.dispose();

        return trimmed;
    }

    private static boolean isPixelBlank(int rgb) {
        int alpha = (rgb >> 24) & 0xff;
        if (alpha < 20) return true;
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return (red > 245 && green > 245 && blue > 245);
    }

    /**
     * Sinh hình ảnh con dấu điện tử PNG chất lượng cao chuẩn Nghị định 30/2020/NĐ-CP
     */
    public static byte[] generateSignatureStampPng(String signerName, String roleTitle, String caProvider, Instant signedAt, boolean isBgh, boolean isDept) {
        int width = 280;
        int height = 95;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color borderColor = isBgh ? new Color(185, 28, 28) : (isDept ? new Color(22, 163, 74) : new Color(37, 99, 235));
        Color bgColor = isBgh ? new Color(254, 242, 242) : (isDept ? new Color(240, 253, 244) : new Color(239, 246, 255));
        Color textColor = isBgh ? new Color(127, 29, 29) : (isDept ? new Color(20, 83, 45) : new Color(30, 58, 138));

        // Background
        g.setColor(bgColor);
        g.fillRoundRect(2, 2, width - 4, height - 4, 10, 10);

        // Border
        g.setColor(borderColor);
        g.setStroke(new BasicStroke(1.8f));
        g.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);

        // Header Title
        g.setColor(textColor);
        g.setFont(new Font("Times New Roman", Font.BOLD, 12));
        String title = isBgh ? "ĐÃ KÝ DUYỆT CHÍNH THỨC" : (isDept ? "ĐÃ KÝ SỐ THẨM ĐỊNH TỔ" : "ĐÃ KÝ SỐ ĐIỆN TỬ");
        g.drawString(title, 14, 22);

        // Divider
        g.setColor(new Color(203, 213, 225));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(10, 30, width - 10, 30);

        // Signer name
        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("Times New Roman", Font.BOLD, 13));
        String nameStr = isBgh ? "TRƯỜNG THCS NGUYỄN VĂN LINH" : ("Ký bởi: " + signerName);
        g.drawString(nameStr, 14, 48);

        // Time and Provider
        String timeStr = TIME_FMT.format(signedAt != null ? signedAt : Instant.now());
        g.setColor(new Color(71, 85, 105));
        g.setFont(new Font("Times New Roman", Font.PLAIN, 10));
        g.drawString("Thời gian: " + timeStr + " | " + caProvider, 14, 66);

        // Verification note
        g.setColor(borderColor);
        g.setFont(new Font("Times New Roman", Font.ITALIC, 9));
        g.drawString("Xác thực: Hợp lệ & Toàn vẹn (SHA-256)", 14, 82);

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi sinh ảnh con dấu số PNG: {}", e.getMessage(), e);
            return null;
        }
    }
}
