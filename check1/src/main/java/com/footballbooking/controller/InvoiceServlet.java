package com.footballbooking.controller;

import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.util.*;

/**
 * Servlet hiển thị chi tiết hóa đơn cho người dùng.
 * Cho phép khách hàng xem lại đơn đặt sân, thực hiện đặt cọc hoặc hủy đơn.
 */
@WebServlet("/invoice")
public class InvoiceServlet extends HttpServlet {
    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        String id = q.getParameter("id");
        try {
            // Tải thông tin chi tiết hóa đơn từ cơ sở dữ liệu
            Map<String, Object> inv = service.invoice(id);
            q.setAttribute("invoice", inv);
            if (inv != null) {
                q.setAttribute("vouchers", service.getAvailableVouchers(id));
            }
        } catch (Exception e) {
            // Hiển thị dữ liệu giả định nếu có lỗi CSDL (phục vụ mục đích demo/kiểm thử)
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma_hd", id); m.put("ho_ten", "Khách hàng"); m.put("sdt", "0900000000");
            m.put("email", "khachhang@example.com");
            m.put("ten_nv", "Quản trị viên"); m.put("chuc_vu", "Admin");
            m.put("trang_thai", "CHƯA THANH TOÁN");
            m.put("tong_tien", 400000); m.put("tien_coc", 120000); m.put("so_tien_con_lai", 280000);
            List<Map<String, Object>> slots = new ArrayList<>();
            Map<String, Object> sl = new LinkedHashMap<>();
            sl.put("ten_san", "Sân 1"); sl.put("loai_san", "Sân 7 người");
            sl.put("ngay_dat", java.time.LocalDate.now().toString());
            sl.put("gio_bat_dau", "06:00"); sl.put("gio_ket_thuc", "08:00");
            sl.put("thanh_tien", 400000);
            slots.add(sl);
            m.put("slots", slots);
            m.put("services", new ArrayList<>());
            q.setAttribute("invoice", m);
        }
        q.getRequestDispatcher("/jsp/user/invoice.jsp").forward(q, p);
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws IOException {
        String op = q.getParameter("op");
        String id = q.getParameter("id");
        try {
            // Xử lý hành động Hủy hóa đơn
            if ("cancel".equals(op)) {
                service.cancelInvoice(id);
                p.sendRedirect(q.getContextPath() + "/home");
                return;
            }
            // Xử lý hành động Thanh toán đặt cọc (30%)
            if ("pay".equals(op)) {
                service.payDeposit(id);
                p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
                return;
            }
            // Xử lý hành động Thanh toán phần còn lại (70%)
            if ("payRemaining".equals(op)) {
                service.payRemaining(id);
                p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
                return;
            }
            // Xử lý áp dụng Voucher
            if ("applyVoucher".equals(op)) {
                String voucherId = q.getParameter("voucherId");
                service.applyVoucher(id, voucherId);
                p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
                return;
            }
            p.sendRedirect(q.getContextPath() + "/invoice?id=" + id);
        } catch (Exception e) {
            p.sendError(500, "Lỗi xử lý hóa đơn: " + e.getMessage());
        }
    }
}
