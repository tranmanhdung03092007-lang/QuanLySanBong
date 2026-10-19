package com.footballbooking.controller;

import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Servlet xử lý quy trình đặt sân của người dùng.
 * Quản lý việc chọn ca sân, thêm dịch vụ và xác nhận đơn đặt sân thông qua Session.
 */
@WebServlet("/booking")
public class BookingServlet extends HttpServlet {

    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        LocalDate today = LocalDate.now();
        req.setAttribute("today", today);

        // Xóa các hóa đơn trống để làm sạch CSDL
        try { service.deleteEmptyInvoices(); } catch (Exception ignored) {}

        // Xác định ngày người dùng muốn xem lịch đặt sân
        String dateParam = req.getParameter("date");
        LocalDate viewDate = today;
        if (dateParam != null && !dateParam.isBlank()) {
            try { viewDate = LocalDate.parse(dateParam); }
            catch (Exception ignored) { }
        }
        req.setAttribute("viewDate", viewDate);

        // Lấy thông tin hóa đơn đang thao tác từ Session
        String hdId = (String) session.getAttribute("activeInvoiceId");
        String editId = req.getParameter("edit");
        if (editId != null && !editId.isBlank()) {
            hdId = editId;
            session.setAttribute("activeInvoiceId", hdId);
        }

        // Nếu đang có hóa đơn hoạt động, tải thông tin chi tiết để hiển thị
        if (hdId != null) {
            try {
                Map<String, Object> inv = service.invoice(hdId);
                if (inv != null) {
                    req.setAttribute("invoice", inv);
                    req.setAttribute("editName", inv.get("ho_ten"));
                    req.setAttribute("editPhone", inv.get("sdt"));
                    req.setAttribute("editEmail", inv.get("email"));
                    req.setAttribute("activeInvoiceId", hdId);
                    req.setAttribute("vouchers", service.getAvailableVouchers(hdId));
                } else {
                    // Nếu hóa đơn không tồn tại (đã bị xóa hoặc sai ID), dọn dẹp session
                    session.removeAttribute("activeInvoiceId");
                    hdId = null;
                }
            } catch (Exception e) {
                req.setAttribute("error", "Lỗi tải hóa đơn: " + e.getMessage());
            }
        }

        // Tải dữ liệu lịch sân và dịch vụ để hiển thị lên giao diện đặt sân
        try {
            Map<String, Object> data = service.fullSchedule(viewDate);
            req.setAttribute("fields", data.get("fields"));
            req.setAttribute("schedule", data.get("schedule"));
            req.setAttribute("headers", data.get("headers"));
            req.setAttribute("services", service.services());
        } catch (Exception e) {
            req.setAttribute("error", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
        req.getRequestDispatcher("/jsp/user/booking.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String op = req.getParameter("op");
        String dateStr = req.getParameter("date");
        String hdId = (String) session.getAttribute("activeInvoiceId");

        try {
            hdId = handleOperation(op, req, session, hdId);
            if (hdId != null && !"confirm".equals(op)) {
                session.setAttribute("activeInvoiceId", hdId);
            }
            if ("confirm".equals(op)) {
                resp.sendRedirect(req.getContextPath() + "/invoice?id=" + hdId);
                return;
            }
        } catch (Exception e) {
            session.setAttribute("bookingError", e.getMessage());
        }

        String redirectUrl = req.getContextPath() + "/booking" + (dateStr != null ? "?date=" + dateStr : "");
        resp.sendRedirect(redirectUrl);
    }

    private String handleOperation(String op, HttpServletRequest req, HttpSession session, String hdId) throws Exception {
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");

        if ("selectSlot".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone, email);
            String fieldId = req.getParameter("fieldId");
            int slot = Integer.parseInt(req.getParameter("slot"));
            LocalDate date = LocalDate.parse(req.getParameter("date"));
            service.toggleSlot(hdId, fieldId, date, slot);
        } else if ("addService".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone, email);
            String svId = req.getParameter("serviceId");
            int qty = Integer.parseInt(req.getParameter("qty"));
            service.updateService(hdId, svId, qty);
        } else if ("removeService".equals(op)) {
            if (hdId != null) {
                service.updateService(hdId, req.getParameter("serviceId"), 0);
            }
        } else if ("applyVoucher".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone, email);
            String vId = req.getParameter("voucherId");
            service.applyVoucher(hdId, vId);
        } else if ("confirm".equals(op)) {
            hdId = service.ensureInvoice(hdId, name, phone, email);
            session.removeAttribute("activeInvoiceId");
        }
        return hdId;
    }
}
