package com.footballbooking.config;

import com.footballbooking.repository.BookingRepository;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * Listener theo dõi vòng đời ứng dụng Web.
 * Tự động kiểm tra và xử lý các hóa đơn/lịch đặt sân đã hết hạn định kỳ.
 * Chạy mỗi giờ một lần tại thời điểm phút 0 để:
 *  - Hủy các hóa đơn quá hạn thanh toán
 *  - Đặt trạng thái "HẾT" cho các ca trống có giờ bắt đầu đã qua thời điểm hiện tại
 */
@WebListener
public class ExpiryListener implements ServletContextListener {
    // Service lập lịch chạy ngầm
    private ScheduledExecutorService ex;

    @Override
    public void contextInitialized(ServletContextEvent e) {
        try {
            // Khởi tạo repository khi ứng dụng bắt đầu
            new BookingRepository();
        } catch (Throwable ignored) {
            System.err.println("Lỗi khởi tạo BookingRepository: " + ignored.getMessage());
        }

        // Tạo một luồng chạy ngầm để quét các lịch đặt sân hết hạn
        ex = Executors.newSingleThreadScheduledExecutor();

        // Tính thời gian chờ ban đầu cho đến phút 0 của giờ tiếp theo
        long initialDelay = computeInitialDelayToNextHour();

        // Lên lịch chạy mỗi giờ một lần tại thời điểm phút 0
        ex.scheduleAtFixedRate(() -> {
            try {
                BookingRepository repo = new BookingRepository();
                // Hủy các hóa đơn quá hạn thanh toán và giải phóng ca sân
                repo.expire();
                // Đặt trạng thái "HẾT" cho các ca trống đã qua giờ bắt đầu
                repo.expireSlots();
            } catch (Throwable ignored) {
                System.err.println("Lỗi chạy tác vụ quét hết hạn: " + ignored.getMessage());
            }
        }, initialDelay, 1, TimeUnit.HOURS);
    }

    /**
     * Tính số giây chờ cho đến phút 0 của giờ tiếp theo.
     * Ví dụ: nếu hiện tại là 12:50:41, sẽ chờ 9 phút 19 giây (559 giây) để đến 13:00:00.
     */
    private long computeInitialDelayToNextHour() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy thời điểm phút 0 của giờ tiếp theo
        LocalDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        Duration duration = Duration.between(now, nextHour);
        return Math.max(1, duration.getSeconds());
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        // Hủy bỏ tác vụ chạy ngầm khi ứng dụng dừng
        if (ex != null) {
            ex.shutdownNow();
        }
    }
}