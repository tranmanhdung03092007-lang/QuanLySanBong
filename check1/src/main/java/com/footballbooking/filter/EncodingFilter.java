package com.footballbooking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import java.io.*;

/**
 * Filter thiết lập bảng mã UTF-8 cho toàn bộ yêu cầu (request) và phản hồi (response).
 * Đảm bảo dữ liệu tiếng Việt được hiển thị và xử lý chính xác trên trình duyệt và server.
 */
@WebFilter("/*")
public class EncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest q, ServletResponse p, FilterChain c) throws IOException, ServletException {
        // Thiết lập mã hóa cho request và response
        q.setCharacterEncoding("UTF-8");
        p.setCharacterEncoding("UTF-8");
        
        // Chuyển tiếp cho Filter tiếp theo hoặc Servlet xử lý
        c.doFilter(q, p);
    }
}
