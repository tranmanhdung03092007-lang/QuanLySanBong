package com.footballbooking.controller;
import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

/**
 * Servlet hiển thị trang chủ của người dùng.
 * Liệt kê danh sách các sân bóng hiện có và thống kê trạng thái các ca trong ngày.
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        BookingService sv = new BookingService();
        try {
            // Lấy danh sách các sân và thống kê đặt sân trong ngày hôm nay
            q.setAttribute("fields", sv.fields());
            q.setAttribute("stats", sv.fieldStats());
        } catch (Exception e) {
            // Xử lý fallback khi có lỗi kết nối CSDL hoặc dữ liệu trống
            List<Map<String, Object>> f = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("ma_san", "S0" + i);
                r.put("ten_san", "Sân " + i);
                r.put("loai_san", "Sân 7 người");
                r.put("gia_theo_gio", 400000 + i * 50000);
                f.add(r);
            }
            q.setAttribute("fields", f);
            q.setAttribute("stats", Map.of("booked", 0, "pending", 0, "expired", 0, "free", 0));
        }
        // Chuyển tiếp đến trang giao diện người dùng
        q.getRequestDispatcher("/jsp/user/home.jsp").forward(q, p);
    }
}
