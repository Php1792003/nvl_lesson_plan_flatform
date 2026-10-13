package com.school.lessonplan.security;

import com.school.lessonplan.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giới hạn số upload đang được tiếp nhận đồng thời trên toàn bộ instance.
 * <p>
 * Mặc định: tối đa 3 upload đồng thời (cấu hình qua app.upload.max-concurrent).
 * Khi không còn slot, từ chối ngay (tryAcquire) thay vì chờ vô hạn.
 * <p>
 * LƯU Ý: Giới hạn này chỉ áp dụng trong bộ nhớ (in-memory) của 1 instance JVM.
 * Nếu chạy nhiều instance, cần dùng cơ chế dùng chung (Redis Semaphore hoặc tương đương).
 * Hiện tại hệ thống chạy đơn instance nên in-memory Semaphore là phù hợp.
 */
@Service
public class UploadThrottleService {

    private static final Logger log = LoggerFactory.getLogger(UploadThrottleService.class);

    private final Semaphore uploadSlots;
    private final int maxConcurrent;

    /** Số upload đang xử lý - dùng cho monitoring */
    private final AtomicInteger activeUploads = new AtomicInteger(0);

    /** Tổng số yêu cầu bị từ chối do hết slot - dùng cho monitoring */
    private final AtomicInteger rejectedCount = new AtomicInteger(0);

    public UploadThrottleService(AppProperties appProperties) {
        this.maxConcurrent = appProperties.getUpload().getMaxConcurrent();
        this.uploadSlots = new Semaphore(maxConcurrent, true); // fair=true
        log.info("UploadThrottleService initialized: maxConcurrent={}", maxConcurrent);
    }

    /**
     * Cố gắng lấy 1 slot upload. Không chờ - trả false ngay nếu hết slot.
     *
     * @return true nếu lấy được slot, false nếu hết slot
     */
    public boolean tryAcquire() {
        boolean acquired = uploadSlots.tryAcquire();
        if (acquired) {
            int active = activeUploads.incrementAndGet();
            log.debug("Upload slot acquired. Active uploads: {}/{}", active, maxConcurrent);
        } else {
            int rejected = rejectedCount.incrementAndGet();
            log.warn("Upload slot unavailable. Active: {}/{}, total rejected: {}",
                activeUploads.get(), maxConcurrent, rejected);
        }
        return acquired;
    }

    /**
     * Giải phóng 1 slot upload. PHẢI được gọi trong finally block
     * sau khi upload hoàn tất (thành công, lỗi, timeout hoặc client ngắt).
     */
    public void release() {
        uploadSlots.release();
        int active = activeUploads.decrementAndGet();
        log.debug("Upload slot released. Active uploads: {}/{}", active, maxConcurrent);
    }

    // ===== Monitoring getters =====

    /** Số upload đang xử lý hiện tại */
    public int getActiveUploads() {
        return activeUploads.get();
    }

    /** Số slot còn trống */
    public int getAvailableSlots() {
        return uploadSlots.availablePermits();
    }

    /** Tổng số yêu cầu bị từ chối */
    public int getRejectedCount() {
        return rejectedCount.get();
    }

    /** Giới hạn tối đa đồng thời */
    public int getMaxConcurrent() {
        return maxConcurrent;
    }
}
