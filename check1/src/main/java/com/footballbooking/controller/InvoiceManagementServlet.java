package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Servlet xử lý các chức năng thanh toán và hủy hóa đơn từ phía quản trị.
 * Hiện tại giao diện quản lý danh sách đã bị loại bỏ, chỉ giữ lại logic xử lý thanh toán.
 */
@WebServlet("/admin/invoice-management")
public class InvoiceManagementServlet extends HttpServlet {
    private final BookingService service = new BookingService();

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            String op = q.getParameter("op");
            String id = q.getParameter("id");

            // Hiển thị form thanh toán cho một hóa đơn cụ thể
            if ("paymentForm".equals(op)) {
                Map<String, Object> invoice = service.invoice(id);
                if (invoice == null) {
                    p.sendRedirect(q.getContextPath() + "/admin");
                    return;
                }
                q.setAttribute("invoice", invoice);
                q.getRequestDispatcher("/jsp/admin/invoice-payment.jsp").forward(q, p);
                return;
            }

            // Các yêu cầu khác đều quay về Dashboard
            p.sendRedirect(q.getContextPath() + "/admin");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            String op = q.getParameter("op");
            String id = q.getParameter("id");
            
            // Xử lý các hành động thanh toán hoặc hủy hóa đơn
            if ("cancel".equals(op)) {
                service.cancelInvoice(id);
            } else if ("pay".equals(op)) {
                service.payDeposit(id); // Thanh toán tiền cọc
            } else if ("payRemaining".equals(op)) {
                service.payRemaining(id); // Thanh toán phần còn lại
            } else if ("confirmDatSan".equals(op)) {
                service.confirmDatSan(id); // Xác nhận đặt sân
            }
            p.sendRedirect(q.getContextPath() + "/admin");
        } catch (Exception e) { 
            throw new ServletException(e); 
        }
    }
}
