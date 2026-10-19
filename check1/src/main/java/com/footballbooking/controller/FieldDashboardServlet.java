package com.footballbooking.controller;

import com.footballbooking.repository.BookingRepository;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Servlet hiển thị bảng điều khiển chi tiết cho từng sân bóng.
 * Cung cấp thông tin lịch đặt trong ngày và các hóa đơn liên quan đến một sân cụ thể.
 */
@WebServlet("/admin/field-dashboard")
public class FieldDashboardServlet extends HttpServlet {
    private final BookingRepository repo = new BookingRepository();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fieldId = req.getParameter("field");
        LocalDate date = LocalDate.now();
        
        try {
            // Lấy danh sách toàn bộ các sân để hiển thị trong thanh chọn
            List<Map<String, Object>> fields = repo.fields();
            req.setAttribute("fields", fields);
            
            if (fieldId != null && !fieldId.isEmpty()) {
                // Lấy danh sách các lượt đặt sân của sân đang chọn trong ngày hôm nay
                List<Map<String, Object>> bookings = repo.getBookingsByField(fieldId, date);
                req.setAttribute("bookings", bookings);
                req.setAttribute("selectedField", fieldId);
                
                // Xác định tên sân đang hiển thị
                for (Map<String, Object> f : fields) {
                    if (fieldId.equals(String.valueOf(f.get("ma_san")))) {
                        req.setAttribute("selectedFieldName", f.get("ten_san"));
                        break;
                    }
                }
                
                // Lấy lịch sử các hóa đơn liên quan đến sân này
                List<Map<String, Object>> invoices = repo.getInvoicesByField(fieldId);
                req.setAttribute("invoices", invoices);
            }
        } catch (Exception e) {
            req.setAttribute("error", "Lỗi tải dữ liệu Dashboard sân: " + e.getMessage());
        }
        
        // Chuyển tiếp đến giao diện Dashboard chi tiết sân
        req.getRequestDispatcher("/jsp/admin/field-dashboard.jsp").forward(req, resp);
    }
}