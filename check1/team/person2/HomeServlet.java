package com.footballbooking.controller;
import com.footballbooking.service.BookingService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try { q.setAttribute("fields", new BookingService().fields()); }
        catch (Exception e) {
            List<Map<String, Object>> f = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("ma_san", "S0" + i); r.put("ten_san", "Sân " + i);
                r.put("loai_san", "Sân 7 người"); r.put("gia_theo_gio", 400000 + i * 50000); f.add(r);
            }
            q.setAttribute("fields", f);
        }
        q.getRequestDispatcher("/jsp/user/home.jsp").forward(q, p);
    }
}