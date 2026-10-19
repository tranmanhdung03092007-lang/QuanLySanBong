# 04_ADMIN_DICH VU_CRONJOB.md
## PHẦN 4 — NGƯỜI 4: ADMIN + DỊCH VỤ + CRONJOB

---

## 1. CHỨC NĂNG PHỤ TRÁCH

Người 4 phụ trách toàn bộ chức năng liên quan đến:

- **Admin CRUD** (Quản lý toàn bộ các thực thể)
- **Quản lý Dịch vụ** (CRUD)
- **Quản lý Voucher** (nếu Admin quản lý)
- **Quản lý Hóa đơn** (nếu Admin có chức năng này)
- **CronJob** (Tự động hủy hóa đơn quá hạn, cập nhật trạng thái)
- **ServletContextListener** (Khởi động CronJob khi deploy)

**Lưu ý quan trọng:**
- Không code chức năng đăng nhập, đặt sân, voucher, thanh toán
- Chỉ làm việc trên các file liên quan đến Admin, Dịch vụ, CronJob
- Nếu cần sửa file của người khác, phải đánh dấu "FILE DÙNG CHUNG"

---

## 2. CẤU TRÚC THƯ MỤC

```
src/main/java/com/footballbooking/
├── config/
│   ├── DBConnection.java                [CLASS DÙNG CHUNG]
│   └── ExpiryListener.java              [CLASS DÙNG CHUNG]
├── controller/
│   ├── AdminCrudServlet.java            (CRUD tổng quát)
│   ├── AdminInvoiceApiServlet.java      (API quản lý hóa đơn)
│   ├── InvoiceManagementServlet.java    (Quản lý thanh toán)
│   ├── FieldDashboardServlet.java       (Dashboard sân)
│   └── ... (các Servlet khác do người khác phụ trách)
├── model/
│   └── Entities.java                    [CLASS DÙNG CHUNG]
├── repository/
│   ├── BookingRepository.java           [CLASS DÙNG CHUNG]
│   └── VoucherRepository.java           [CLASS DÙNG CHUNG]
├── service/
│   ├── BookingService.java              [CLASS DÙNG CHUNG]
│   └── VoucherService.java              [CLASS DÙNG CHUNG]
└── utils/
    ├── DbConnection.java                [CLASS DÙNG CHUNG]
    ├── JsonUtil.java                    [CLASS DÙNG CHUNG]
    └── Slots.java                       [CLASS DÙNG CHUNG]

src/main/webapp/jsp/
├── admin/
│   ├── dashboard.jsp                   [CLASS DÙNG CHUNG]
│   ├── login.jsp                       [CLASS DÙNG CHUNG]
│   ├── logout.jsp                      [CLASS DÙNG CHUNG]
│   ├── form.jsp                        [Form thêm/sửa)
│   ├── list.jsp                        (Danh sách)
│   ├── field-dashboard.jsp             (Quản lý sân)
│   └── invoice-payment.jsp             (Thanh toán)
└── common/
    ├── header.jspf                     [CLASS DÙNG CHUNG]
    └── footer.jspf                     [CLASS DÙNG CHUNG]

database/
└── football_booking.sql                [CLASS DÙNG CHUNG]
```

---

## 3. DATABASE LIÊN QUAN

### 3.1 Bảng DichVu (Dịch vụ)
```sql
CREATE TABLE DichVu (
    ma_dv   VARCHAR(10) PRIMARY KEY,
    ten_dv  NVARCHAR(100) NOT NULL,
    don_gia DECIMAL(18,2) NOT NULL CHECK (don_gia >= 0)
);
```

**Dữ liệu mẫu:**
```sql
INSERT INTO DichVu (ma_dv, ten_dv, don_gia) VALUES
('DV001', N'Nước suối', 10000),
('DV002', N'Áo bib', 30000),
('DV003', N'Bóng thi đấu', 50000);
```

### 3.2 Bảng Voucher
```sql
CREATE TABLE Voucher (
    ma_voucher VARCHAR(20) PRIMARY KEY,
    ma_code VARCHAR(50) NOT NULL,
    ten_voucher NVARCHAR(100) NOT NULL,
    loai_giam VARCHAR(20) NOT NULL,
    gia_tri FLOAT NOT NULL DEFAULT 0,
    don_toi_thieu FLOAT NOT NULL DEFAULT 0,
    ngay_bat_dau DATE NOT NULL,
    ngay_ket_thuc DATE NOT NULL,
    so_luong INT NOT NULL DEFAULT 0,
    trang_thai NVARCHAR(30) NOT NULL DEFAULT N'HOẠT ĐỘNG',
    loai INT NOT NULL DEFAULT 1
);
```

### 3.3 Bảng HoaDon
```sql
CREATE TABLE HoaDon (
    ma_hd           VARCHAR(10) PRIMARY KEY,
    ma_kh           VARCHAR(10) NOT NULL REFERENCES KhachHang(ma_kh),
    ma_nv           VARCHAR(10) NULL REFERENCES NhanVien(ma_nv),
    ngay_lap        DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    tong_tien       DECIMAL(18,2) NOT NULL,
    tien_coc        DECIMAL(18,2) NOT NULL DEFAULT 200000,
    so_tien_con_lai DECIMAL(18,2) NOT NULL,
    han_thanh_toan  DATETIME NOT NULL,
    trang_thai      NVARCHAR(30) NOT NULL DEFAULT N'CHƯA THANH TOÁN'
);
```

---

## 4. CÁC ENTITY

### 4.1 Entity DichVu
```java
public static class DichVu {
    public String maDv, tenDv;
    public double donGia;
}
```

**Giải thích:**
- `maDv`: Mã dịch vụ (VD: DV001)
- `tenDv`: Tên dịch vụ
- `donGia`: Đơn giá

### 4.2 Entity Voucher (đã giải thích trong file 03)

### 4.3 Entity HoaDon (đã giải thích trong file 03)

---

## 5. CÁC SERVLET

### 5.1 AdminCrudServlet.java
**Đường dẫn:** `/admin/manage?entity=...`
**Chức năng:** Servlet tổng quát xử lý CRUD cho tất cả các thực thể

**Lưu ý quan trọng:** Đây là CLASS DÙNG CHUNG. Người 4 cần đọc toàn bộ 1189 dòng.

**Code hoàn chỉnh:** (Đã cung cấp trong file 01_NguoiDung_DangNhap.md)

**Phần Người 4 cần quan tâm:**
- Xử lý `DichVu` (Thêm, sửa, xóa dịch vụ)
- Xử lý `Voucher` (Thêm, sửa, xóa voucher)
- Xử lý `San` (Quản lý sân)
- Xử lý `LichSan` (Quản lý lịch sân)
- Xử lý `HoaDon` (Xem, cập nhật trạng thái)
- Xử lý `CTHD` (Chi tiết hóa đơn)

### 5.2 AdminInvoiceApiServlet.java
**Đường dẫn:** `/api/admin/invoices/*`
**Chức năng:** API JSON cho quản lý hóa đơn (AJAX)

**Code hoàn chỉnh:**
```java
package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import com.footballbooking.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

/**
 * API Servlet cung cấp các điểm cuối (endpoints) JSON để quản lý hóa đơn.
 * Sử dụng chủ yếu cho các yêu cầu AJAX từ giao diện quản trị.
 */
@WebServlet("/api/admin/invoices/*")
public class AdminInvoiceApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Lấy các tham số tìm kiếm và lọc
        String search = req.getParameter("search") != null ? req.getParameter("search") : "";
        String status = req.getParameter("status") != null ? req.getParameter("status") : "";
        String date = req.getParameter("date") != null ? req.getParameter("date") : "";
        
        try {
            // Lấy danh sách hóa đơn dựa trên tiêu chí lọc
            Map<String, Object> result = getInvoices(search, status, date);
            PrintWriter out = resp.getWriter();
            out.print(JsonUtil.stringify(result));
            out.flush();
        } catch (SQLException e) {
            resp.setStatus(500);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            PrintWriter out = resp.getWriter();
            out.print(JsonUtil.stringify(error));
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        try {
            // Xử lý các hành động cập nhật trạng thái hóa đơn qua URL
            if (pathInfo != null && pathInfo.endsWith("/approve")) {
                // Phê duyệt hóa đơn (Xác nhận đã thanh toán)
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/approve", "");
                approveInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã xác nhận thanh toán thành công");
            } else if (pathInfo != null && pathInfo.endsWith("/reject")) {
                // Từ chối hóa đơn (Hủy hóa đơn)
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/reject", "");
                rejectInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã từ chối thanh toán");
            } else if (pathInfo != null && pathInfo.endsWith("/processing")) {
                // Chuyển sang trạng thái đang xử lý
                String invoiceId = pathInfo.replace("/invoices/", "").replace("/processing", "");
                setProcessingInvoice(invoiceId);
                sendSuccessResponse(resp, "Đã chuyển sang trạng thái đang xử lý");
            } else {
                resp.setStatus(400);
                sendErrorResponse(resp, "Yêu cầu không hợp lệ");
            }
        } catch (SQLException e) {
            resp.setStatus(500);
            sendErrorResponse(resp, e.getMessage());
        }
    }

    /**
     * Truy vấn danh sách hóa đơn kèm theo thống kê số lượng theo trạng thái.
     */
    private Map<String, Object> getInvoices(String search, String status, String date) throws SQLException {
        List<Map<String, Object>> invoices = new ArrayList<>();
        Map<String, Integer> stats = new HashMap<>();
        
        // Truy vấn danh sách hóa đơn chi tiết
        StringBuilder sql = new StringBuilder(
            "SELECT hd.ma_hd, hd.ma_kh, kh.ho_ten, kh.sdt, s.ten_san, " +
            "ls.ngay_dat, hd.tong_tien, hd.tien_coc, hd.trang_thai, hd.ngay_lap " +
            "FROM HoaDon hd " +
            "JOIN KhachHang kh ON hd.ma_kh = kh.ma_kh " +
            "JOIN CTHD c ON c.ma_hd = hd.ma_hd " +
            "JOIN LichSan ls ON ls.ma_lich = c.ma_lich " +
            "JOIN San s ON ls.ma_san = s.ma_san " +
            "WHERE 1=1"
        );

        List<String> params = new ArrayList<>();

        if (!search.isEmpty()) {
            sql.append(" AND (hd.ma_hd LIKE ? OR kh.ho_ten LIKE ? OR kh.sdt LIKE ?)");
            String searchParam = "%" + search + "%";
            params.add(searchParam);
            params.add(searchParam);
            params.add(searchParam);
        }

        if (!status.isEmpty()) {
            sql.append(" AND hd.trang_thai = ?");
            params.add(status);
        }

        if (!date.isEmpty()) {
            sql.append(" AND CAST(hd.ngay_lap AS DATE) = ?");
            params.add(date);
        }

        sql.append(" ORDER BY hd.ngay_lap DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> invoice = new LinkedHashMap<>();
                invoice.put("ma_hd", rs.getString("ma_hd"));
                invoice.put("ma_kh", rs.getString("ma_kh"));
                invoice.put("ho_ten", rs.getString("ho_ten"));
                invoice.put("sdt", rs.getString("sdt"));
                invoice.put("ten_san", rs.getString("ten_san"));
                invoice.put("ngay_dat", rs.getDate("ngay_dat"));
                invoice.put("tong_tien", rs.getInt("tong_tien"));
                invoice.put("tien_coc", rs.getInt("tien_coc"));
                invoice.put("trang_thai", rs.getString("trang_thai"));
                invoice.put("ngay_lap", rs.getDate("ngay_lap"));
                invoices.add(invoice);
            }
        }

        // Truy vấn thống kê tổng hợp số lượng hóa đơn theo từng trạng thái
        String statsSql = "SELECT trang_thai, COUNT(*) as count FROM HoaDon GROUP BY trang_thai";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(statsSql)) {

            int total = 0;
            int pending = 0;
            int processing = 0;
            int paid = 0;

            while (rs.next()) {
                String state = rs.getString("trang_thai");
                int count = rs.getInt("count");
                total += count;

                if ("CHƯA THANH TOÁN".equals(state)) {
                    pending = count;
                } else if ("ĐANG XỬ LÝ".equals(state)) {
                    processing = count;
                } else if ("ĐÃ THANH TOÁN".equals(state)) {
                    paid = count;
                }
            }

            stats.put("total", total);
            stats.put("pending", pending);
            stats.put("processing", processing);
            stats.put("paid", paid);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("invoices", invoices);
        result.put("stats", stats);

        return result;
    }

    /**
     * Phê duyệt hóa đơn và cập nhật trạng thái lịch sân tương ứng thành 'ĐÃ ĐẶT'.
     */
    private void approveInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE HoaDon SET trang_thai = 'ĐÃ THANH TOÁN' WHERE ma_hd = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, invoiceId);
                    stmt.executeUpdate();
                }

                // Đồng bộ trạng thái cho các lịch sân liên quan
                String updateScheduleSql = "UPDATE LichSan SET trang_thai = 'ĐÃ ĐẶT' WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd = ?)";
                try (PreparedStatement scheduleStmt = conn.prepareStatement(updateScheduleSql)) {
                    scheduleStmt.setString(1, invoiceId);
                    scheduleStmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Đánh dấu hóa đơn đang trong quá trình xử lý.
     */
    private void setProcessingInvoice(String invoiceId) throws SQLException {
        String sql = "UPDATE HoaDon SET trang_thai = 'ĐANG XỬ LÝ' WHERE ma_hd = ? AND trang_thai = 'CHƯA THANH TOÁN'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, invoiceId);
            stmt.executeUpdate();
        }
    }

    /**
     * Từ chối hóa đơn (Hủy bỏ) và giải phóng lại các ca sân về trạng thái 'CÒN TRỐNG'.
     */
    private void rejectInvoice(String invoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE HoaDon SET trang_thai = 'ĐÃ HỦY' WHERE ma_hd = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, invoiceId);
                    stmt.executeUpdate();
                }

                // Giải phóng các sân liên quan về trạng thái trống
                String updateScheduleSql = "UPDATE LichSan SET trang_thai = 'CÒN TRỐNG' WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd = ?)";
                try (PreparedStatement scheduleStmt = conn.prepareStatement(updateScheduleSql)) {
                    scheduleStmt.setString(1, invoiceId);
                    scheduleStmt.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Gửi phản hồi JSON thành công về client.
     */
    private void sendSuccessResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", message);
        
        PrintWriter out = resp.getWriter();
        out.print(JsonUtil.stringify(response));
        out.flush();
    }

    /**
     * Gửi phản hồi JSON lỗi về client.
     */
    private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("success", "false");
        response.put("error", message);
        
        PrintWriter out = resp.getWriter();
        out.print(JsonUtil.stringify(response));
        out.flush();
    }
}
```

### 5.3 FieldDashboardServlet.java
**Đường dẫn:** `/admin/field-dashboard`
**Chức năng:** Hiển thị dashboard chi tiết cho từng sân

**Code hoàn chỉnh:**
```java
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
```

---

## 6. CÁC CLASS DÙNG CHUNG

### 6.1 AdminCrudServlet.java (1189 dòng)
**Đường dẫn:** `src/main/java/com/footballbooking/controller/AdminCrudServlet.java`

**Lý do cần thiết:** Lớp này xử lý CRUD cho tất cả các thực thể.

**Được sử dụng bởi:**
- Người 1 (KhachHang, NhanVien)
- Người 2 (San, LichSan)
- Người 3 (Voucher, HoaDon, CTHD)
- Người 4 (DichVu, San, LichSan, HoaDon, CTHD)

**Code hoàn chỉnh:** (Xem trong file 01_NguoiDung_DangNhap.md)

### 6.2 BookingRepository.java (1189 dòng)
**Đường dẫn:** `src/main/java/com/footballbooking/repository/BookingRepository.java`

**Lý do cần thiết:** Repository chính xử lý tất cả các thao tác với DB.

**Được sử dụng bởi:**
- Người 2 (Đặt sân)
- Người 3 (Thanh toán, hóa đơn)
- Người 4 (Dashboard, quản lý)

**Code hoàn chỉnh:** (Xem trong file 02_QuanLySan_DatSan.md)

### 6.3 Entities.java (196 dòng)
**Đường dẫn:** `src/main/java/com/footballbooking/model/Entities.java`

**Lý do cần thiết:** Chứa tất cả các Entity.

**Code hoàn chỉnh:** (Xem trong file 01_NguoiDung_DangNhap.md)

---

## 7. CÁC JSP

### 7.1 form.jsp (Form thêm/sửa)
**Đường dẫn:** `/jsp/admin/form.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container" style="max-width:500px;">
<c:set var="isFinalStatus" value="${(meta.table=='HoaDon' && (record['trang_thai']=='ĐÃ THANH TOÁN' || record['trang_thai']=='ĐÃ HỦY')) || (meta.table=='CTHD' && (invoiceStatus=='ĐÃ THANH TOÁN' || invoiceStatus=='ĐÃ HỦY'))}"/>
<c:set var="isInvoice" value="${meta.table=='HoaDon'}"/>
<p><a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">&larr; Danh sách</a></p>
<h2>${isNew?'Thêm':'Sửa'} ${L[meta.table]}</h2>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<c:if test="${not empty param.saved && param.saved=='1'}">
    <div class="alert" style="background:#e8f5e9;color:#2e7d32;border:1px solid #4caf50;">✓ Đã cập nhật hóa đơn thành công!</div>
</c:if>
<div class="card">
<form method="post">
<input type="hidden" name="entity" value="${meta.table}">
<input type="hidden" name="op" value="${isNew?'create':'update'}">
<c:if test="${not empty param.back}"><input type="hidden" name="back" value="${param.back}"></c:if>
<c:if test="${not empty param.loai && meta.table=='Voucher'}"><input type="hidden" name="loai_pref" value="${param.loai}"></c:if>
<c:forEach items="${meta.cols}" var="col">
<label>${L[col]!=null?L[col]:col}</label>
<c:choose>
    <c:when test="${!isNew&&col==meta.pk}"><input name="${col}" value="${record[col]}" readonly style="background:#eee"></c:when>
    <c:when test="${col=='trang_thai'}">
    <c:choose>
      <c:when test="${isInvoice}">
        <c:choose>
          <c:when test="${record[col]=='ĐÃ THANH TOÁN' || record[col]=='ĐÃ HỦY'}">
            <input name="${col}" value="${record[col]}" readonly style="background:#eee">
          </c:when>
          <c:otherwise>
            <select name="${col}">
              <option value="ĐÃ ĐẶT" ${record[col]=='ĐÃ ĐẶT'?'selected':''}>Đã đặt</option>
              <option value="CHƯA THANH TOÁN" ${record[col]=='CHƯA THANH TOÁN'?'selected':''}>Chưa thanh toán</option>
              <option value="MỚI THANH TOÁN CỌC" ${record[col]=='MỚI THANH TOÁN CỌC'?'selected':''}>Mới thanh toán cọc</option>
              <option value="ĐÃ THANH TOÁN" ${record[col]=='ĐÃ THANH TOÁN'?'selected':''}>Đã thanh toán</option>
              <option value="ĐÃ HỦY" ${record[col]=='ĐÃ HỦY'?'selected':''}>Đã hủy</option>
            </select>
          </c:otherwise>
        </c:choose>
      </c:when>
      <c:when test="${meta.table=='LichSan'}">
        <select name="${col}">
          <option value="CÒN TRỐNG" ${record[col]=='CÒN TRỐNG'?'selected':''}>Còn trống</option>
          <option value="ĐANG GIỮ CHỖ" ${record[col]=='ĐANG GIỮ CHỖ'?'selected':''}>Đang giữ chỗ</option>
          <option value="ĐÃ ĐẶT" ${record[col]=='ĐÃ ĐẶT'?'selected':''}>Đã đặt</option>
          <option value="SỰ KIỆN" ${record[col]=='SỰ KIỆN'?'selected':''}>Sự kiện</option>
        </select>
      </c:when>
      <c:when test="${meta.table=='San'}">
        <select name="${col}">
          <option value="HOẠT ĐỘNG" ${record[col]=='HOẠT ĐỘNG'?'selected':''}>Hoạt động</option>
          <option value="BẢO TRÌ" ${record[col]=='BẢO TRÌ'?'selected':''}>Bảo trì</option>
        </select>
      </c:when>
      <c:otherwise>
        <input name="${col}" value="${record[col]}" ${isNew?'required':''}>
      </c:otherwise>
    </c:choose>
    </c:when>
    <c:when test="${col=='tong_tien' || col=='so_tien_con_lai'}">
        <c:choose>
            <c:when test="${meta.table=='HoaDon'}">
                <c:choose>
                    <c:when test="${isFinalStatus}">
                        <div style="margin-bottom: 5px;">
                            <label style="display:inline-block;width:120px;">${L[col]}:</label>
                            <b><fmt:formatNumber value="${record[col]}" type="number"/>đ</b>
                        </div>
                        <c:if test="${col=='so_tien_con_lai'}">
                            <div style="margin-top:20px;">
                                <h4>Danh sách sân/ca</h4>
                                <table class="table" style="width:100%; border-collapse: collapse;" border="1">
                                    <thead style="background:#eee;">
                                        <tr><th>Tên sân</th><th>Ca</th><th>Đơn giá</th><th>Thành tiền</th></tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${inv.slots}" var="sl">
                                            <tr>
                                                <td>${sl.ten_san}</td>
                                                <td>${sl.buoi} ${sl.ca} (${sl.ngay_dat})</td>
                                                <td><fmt:formatNumber value="${sl.thanh_tien}" type="number"/>đ</td>
                                                <td><fmt:formatNumber value="${sl.thanh_tien}" type="number"/>đ</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>

                                <h4 style="margin-top:20px;">Danh sách dịch vụ</h4>
                                <table class="table" style="width:100%; border-collapse: collapse;" border="1">
                                    <thead style="background:#eee;">
                                        <tr><th>Tên dịch vụ</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${inv.services}" var="sv">
                                            <tr>
                                                <td>${sv.ten_dv}</td>
                                                <td>${sv.so_luong}</td>
                                                <td><fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</td>
                                                <td><fmt:formatNumber value="${sv.thanh_tien}" type="number"/>đ</td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty inv.services}">
                                            <tr><td colspan="4" style="text-align:center;">Không có dịch vụ</td></tr>
                                        </c:if>
                                    </tbody>
                                </table>
                                <div style="margin-top:15px; text-align:right; border-top:2px solid #333; padding-top:10px;">
                                    <p>Tổng tiền: <b><fmt:formatNumber value="${record['tong_tien']}" type="number"/>đ</b></p>
                                    <p>Đã thanh toán: <b><fmt:formatNumber value="${record['tong_tien'] - record['so_tien_con_lai']}" type="number"/>đ</b></p>
                                    <p>Còn lại: <b style="color:red;"><fmt:formatNumber value="${record['so_tien_con_lai']}" type="number"/>đ</b></p>
                                </div>
                            </div>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <input name="${col}" value="${record[col]}" readonly style="background:#eee">
                        <c:if test="${col=='so_tien_con_lai'}">
                            <div style="margin: 15px 0; padding: 10px; border: 1px solid #ccc; background: #f9f9f9;">
                                <b>Dịch vụ đi kèm:</b>
                                <div style="max-height: 180px; overflow-y: auto; margin-top: 5px;">
                                <c:forEach items="${services}" var="sv">
                                    <c:set var="isSelected" value="false"/>
                                    <c:if test="${not empty editServices}">
                                        <c:forEach items="${editServices}" var="esId">
                                            <c:if test="${esId == sv.ma_dv}"><c:set var="isSelected" value="true"/></c:if>
                                        </c:forEach>
                                    </c:if>
                                    <c:set var="savedQty" value="${not empty editServiceQty ? editServiceQty[sv.ma_dv] : 1}"/>
                                    <div style="margin: 5px 0; display: flex; align-items: center; gap: 10px;">
                                        <input type="checkbox" name="service_${sv.ma_dv}" ${isSelected ? 'checked' : ''} style="width:18px;height:18px;">
                                        <label style="margin:0;font-weight:normal;">${sv.ten_dv} — <fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</label>
                                        <input type="number" name="qty_${sv.ma_dv}" value="${savedQty}" min="1" max="10" style="width:50px;padding:4px;">
                                    </div>
                                </c:forEach>
                                <c:if test="${empty services}">
                                    <p style="font-size: 0.9em; color: #666;">Không có dịch vụ</p>
                                </c:if>
                                </div>
                            </div>
                            
                            <div style="margin: 15px 0; padding: 10px; border: 1px solid #cce5ff; background: #e7f3ff;">
                                <b>Chọn sân/ca trong ngày ${not empty inv.slots ? inv.slots[0].ngay_dat : ''}:</b>
                                <div style="max-height: 220px; overflow-y: auto; margin-top: 5px;">
                                    <c:forEach items="${availableSlots}" var="slot">
                                        <div style="margin: 5px 0; display: flex; align-items: center; gap: 10px; padding: 4px; border-bottom: 1px dashed #d0e8ff;">
                                            <input type="checkbox" name="add_slot_${slot.ma_san}_${slot.so_ca}" ${slot.checked ? 'checked' : ''} style="width:18px;height:18px;">
                                            <label style="margin:0;font-weight:normal;cursor:pointer;">
                                                <b>${slot.ten_san}</b> - Ca ${slot.so_ca} 
                                                (${slot.gio_bat_dau} - ${slot.gio_ket_thuc})
                                                <c:if test="${slot.checked}"><span style="color:#2e7d32;font-weight:bold;margin-left:5px;">✓</span></c:if>
                                            </label>
                                        </div>
                                    </c:forEach>
                                    <c:if test="${empty availableSlots}">
                                        <p style="font-size: 0.9em; color: #666;">Không có sân/ca trống nào trong ngày này.</p>
                                    </c:if>
                                </div>
                            </div>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <input name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="${col=='email'}"><input type="email" name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}></c:when>
    <c:when test="${col=='ngay_dat' || col=='ngayBatDau' || col=='ngayKetThuc' || col=='ngay_bat_dau' || col=='ngay_ket_thuc'}"><input type="date" name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}></c:when>
    <c:when test="${meta.table=='Voucher' && col=='loai_giam'}">
        <select name="${col}">
            <option value="MONEY" ${record[col]=='MONEY' || record[col]=='GIAM_TRUC_TIEP'?'selected':''}>Giảm tiền (MONEY)</option>
            <option value="PERCENT" ${record[col]=='PERCENT' || record[col]=='GIAM_THEO_PHAN_TRAM'?'selected':''}>Giảm % (PERCENT)</option>
        </select>
    </c:when>
    <c:when test="${meta.table=='Voucher' && col=='loai'}">
        <select name="${col}">
            <option value="1" ${record[col]==1 || record[col]=='1'?'selected':''}>1 — Hóa đơn (Booking)</option>
            <option value="2" ${record[col]==2 || record[col]=='2'?'selected':''}>2 — Sân</option>
            <option value="3" ${record[col]==3 || record[col]=='3'?'selected':''}>3 — Dịch vụ</option>
        </select>
    </c:when>
    <c:when test="${meta.table=='Voucher' && col=='trang_thai'}">
        <select name="${col}">
            <option value="HOẠT ĐỘNG" ${record[col]=='HOẠT ĐỘNG'?'selected':''}>HOẠT ĐỘNG</option>
            <option value="NGỪNG" ${record[col]=='NGỪNG'?'selected':''}>NGỪNG</option>
        </select>
    </c:when>
    <c:when test="${meta.table=='Voucher' && (col=='gia_tri' || col=='don_toi_thieu' || col=='so_luong')}">
        <input type="number" name="${col}" value="${record[col]}" step="any" ${isNew?'required':''}>
    </c:when>
    <c:otherwise><input name="${col}" value="${record[col]}" ${isNew?'required':''} ${(isInvoice || isFinalStatus)?'readonly style="background:#eee"':''}></c:otherwise>
    </c:choose>
</c:forEach>
<p><c:if test="${!isFinalStatus}"><button class="btn">Lưu</button></c:if>
<c:if test="${isInvoice && !isNew && (record.trang_thai=='ĐÃ ĐẶT' || record.trang_thai=='CHƯA THANH TOÁN' || record.trang_thai=='MỚI THANH TOÁN CỌC')}">
    <button type="submit" name="afterSave" value="payment" class="btn" style="background:#2e7d32;color:white;" onclick="return confirm('Hóa đơn sẽ được lưu trước khi chuyển sang thanh toán. Tiếp tục?')">Lưu & Thanh toán</button>
</c:if>
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">Hủy</a></p>
</form></div></div>
<%@ include file="/jsp/common/footer.jspf" %>
```

### 7.2 list.jsp (Danh sách)
**Đường dẫn:** `/jsp/admin/list.jsp`

**Code hoàn chỉnh:**
```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a>
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}&action=form">+ Thêm</a></p>
<h2>Quản lý ${L[meta.table]}</h2>
<p>
<c:forEach items="${['San','LichSan','KhachHang','NhanVien','DichVu','HoaDon','CTHD','Voucher']}" var="e">
<a class="btn" href="?entity=${e}">${L[e]}</a>
</c:forEach></p>
<form style="margin:10px 0;"><input type="hidden" name="entity" value="${meta.table}">
<input type="text" name="search" value="${param.search}" placeholder="Tìm..." style="width:200px;display:inline;">
<button class="btn">Tìm</button></form>
<div style="overflow-x:auto;">
<table><thead><tr>
<c:forEach items="${meta.cols}" var="col"><th>${L[col]!=null?L[col]:col}</th></c:forEach>
<th>Thao tác</th></tr></thead>
<tbody>
<c:forEach items="${rows}" var="row">
<tr><c:forEach items="${meta.cols}" var="col"><td>${row[col]}</td></c:forEach>
<td>
<c:choose>
  <c:when test="${(meta.table=='HoaDon' && (row.trang_thai=='ĐÃ THANH TOÁN' || row.trang_thai=='ĐÃ HỦY')) || (meta.table=='CTHD' && (row.trang_thai_hd=='ĐÃ THANH TOÁN' || row.trang_thai_hd=='ĐÃ HỦY'))}">
    <a class="btn" href="?entity=${meta.table}&action=form&id=${row[meta.pk]}">Xem</a>
  </c:when>
  <c:otherwise>
    <a class="btn" href="?entity=${meta.table}&action=form&id=${row[meta.pk]}">Sửa</a>
  </c:otherwise>
</c:choose>
<c:if test="${meta.table=='HoaDon'}">
    <c:if test="${row.trang_thai=='ĐÃ ĐẶT' || row.trang_thai=='CHƯA THANH TOÁN' || row.trang_thai=='MỚI THANH TOÁN CỌC'}">
        <a class="btn" href="${pageContext.request.contextPath}/admin/invoice-management?op=paymentForm&id=${row.ma_hd}" style="background:#2e7d32;color:white;">Thanh toán</a>
    </c:if>
</c:if>
<form method="post" style="display:inline;" onsubmit="return confirm('Xóa?')">
<input type="hidden" name="entity" value="${meta.table}">
<input type="hidden" name="op" value="delete"><input type="hidden" name="${meta.pk}" value="${row[meta.pk]}">
<button class="btn">Xóa</button></form></td></tr>
</c:forEach>
<c:if test="${empty rows}"><tr><td colspan="20" style="text-align:center;padding:20px;">Chưa có dữ liệu</td></tr>
</c:forEach>
</tbody></table></div>
<p><c:forEach begin="1" end="${totalPages}" var="n">
<a class="btn" href="?entity=${meta.table}&page=${n}&search=${param.search}">${n}</a>
</c:forEach></p>

<%-- Khu vực Voucher áp dụng trên trang Quản lý Sân (loai=2) / Dịch vụ (loai=3) --%>
<c:if test="${meta.table=='San' || meta.table=='DichVu'}">
<hr style="margin:30px 0;border:none;border-top:2px solid #e0e0e0;">
<div class="voucher-section" style="margin-top:10px;">
    <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;">
        <h3 style="margin:0;">Voucher áp dụng — ${voucherLoaiLabel}</h3>
        <div>
            <a class="btn" href="?entity=Voucher&loai=${voucherLoai}">Xem tất cả</a>
            <a class="btn" href="?entity=Voucher&action=form&loai=${voucherLoai}&back=${meta.table}" style="background:#1565c0;color:#fff;">+ Thêm Voucher</a>
        </div>
    </div>
    <p style="color:#666;font-size:13px;margin:8px 0 12px;">
        Chỉ hiển thị Voucher có <b>loai = ${voucherLoai}</b>
        (<c:choose>
            <c:when test="${voucherLoai==2}">Sân</c:when>
            <c:when test="${voucherLoai==3}">Dịch vụ</c:when>
            <c:otherwise>Hóa đơn</c:otherwise>
        </c:choose>).
        Không hiển thị Voucher của các loại khác.
    </p>
    <div style="overflow-x:auto;">
    <table>
        <thead>
        <tr>
            <th>Mã Voucher</th>
            <th>Mã Code</th>
            <th>Tên Voucher</th>
            <th>Loại giảm</th>
            <th>Giá trị</th>
            <th>Đơn tối thiểu</th>
            <th>Bắt đầu</th>
            <th>Kết thúc</th>
            <th>Số lượng</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${entityVouchers}" var="v">
            <tr>
                <td>${v.ma_voucher}</td>
                <td><strong>${v.ma_code}</strong></td>
                <td>${v.ten_voucher}</td>
                <td>
                    <c:choose>
                        <c:when test="${v.loai_giam=='PERCENT' || v.loai_giam=='GIAM_THEO_PHAN_TRAM'}">%</c:when>
                        <c:otherwise>Tiền</c:otherwise>
                    </c:choose>
                </td>
                <td>${v.gia_tri}<c:if test="${v.loai_giam=='PERCENT' || v.loai_giam=='GIAM_THEO_PHAN_TRAM'}">%</c:if></td>
                <td>${v.don_toi_thieu}</td>
                <td>${v.ngay_bat_dau}</td>
                <td>${v.ngay_ket_thuc}</td>
                <td>${v.so_luong}</td>
                <td>${v.trang_thai}</td>
                <td>
                    <a class="btn" href="?entity=Voucher&action=form&id=${v.ma_voucher}&back=${meta.table}">Sửa</a>
                    <form method="post" style="display:inline;" onsubmit="return confirm('Xóa voucher ${v.ma_code}?')">
                        <input type="hidden" name="entity" value="Voucher">
                        <input type="hidden" name="op" value="delete">
                        <input type="hidden" name="ma_voucher" value="${v.ma_voucher}">
                        <input type="hidden" name="back" value="${meta.table}">
                        <button class="btn" type="submit">Xóa</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty entityVouchers}">
            <tr><td colspan="11" style="text-align:center;padding:16px;color:#888;">Chưa có voucher áp dụng cho ${voucherLoaiLabel}</td></tr>
        </c:if>
        </tbody>
    </table>
    </div>
</div>
</c:if>

</div>
<%@ include file="/jsp/common/footer.jspf" %>
```

---

## 8. CRONJOB

### 8.1 ExpiryListener.java
**Đường dẫn:** `src/main/java/com/footballbooking/config/ExpiryListener.java`
**Chức năng:** Tự động hủy hóa đơn quá hạn và cập nhật trạng thái sân

**Code hoàn chỉnh:**
```java
package com.footballbooking.config;

import com.footballbooking.repository.BookingRepository;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listener khởi động CronJob khi deploy ứng dụng.
 * Tự động hủy hóa đơn quá hạn và cập nhật trạng thái ca sân đã qua giờ.
 */
@WebListener
public class ExpiryListener implements ServletContextListener {
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Chạy mỗi phút: hủy hóa đơn quá hạn và cập nhật trạng thái ca sân
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BookingRepository repo = new BookingRepository();
                repo.expire(); // Hủy hóa đơn quá hạn
                repo.expireSlots(); // Đánh dấu ca đã qua giờ là "HẾT"
            } catch (Exception e) {
                System.err.println("Lỗi CronJob: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
        
        System.out.println("✓ CronJob đã khởi động - Kiểm tra hóa đơn quá hạn mỗi phút");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
```

**Giải thích:**
- `contextInitialized()`: Được gọi khi deploy ứng dụng
- `scheduleAtFixedRate()`: Chạy lặp lại mỗi 1 phút
- `expire()`: Hủy hóa đơn quá hạn thanh toán
- `expireSlots()`: Đánh dấu ca đã qua giờ là "HẾT"

---

## 9. LUỒNG XỬ LÝ

### 9.1 Luồng quản lý Dịch vụ

```
Admin truy cập /admin/manage?entity=DichVu
        ↓
AdminAuthFilter (kiểm tra đăng nhập)
        ↓
AdminCrudServlet.doGet()
        ↓
Lấy danh sách dịch vụ từ DB
        ↓
Hiển thị list.jsp
        ↓
Admin nhấn "Thêm mới"
        ↓
Truy cập /admin/manage?entity=DichVu&action=form
        ↓
Hiển thị form.jsp (trống)
        ↓
Admin điền thông tin và nhấn "Lưu"
        ↓
Gửi POST đến /admin/manage
        ↓
AdminCrudServlet.doPost()
        ↓
Thực hiện INSERT vào bảng DichVu
        ↓
Chuyển về danh sách
```

### 9.2 Luồng quản lý Voucher

```
Admin truy cập /admin/manage?entity=Voucher
        ↓
AdminCrudServlet.doGet()
        ↓
Lấy danh sách voucher từ DB
        ↓
Hiển thị list.jsp với khu vực voucher theo loai
        ↓
Admin có thể:
- Thêm voucher mới
- Sửa voucher
- Xóa voucher
```

### 9.3 Luồng CronJob

```
Deploy ứng dụng
        ↓
ExpiryListener.contextInitialized()
        ↓
Khởi động ScheduledExecutorService
        ↓
Mỗi 1 phút chạy:
        ↓
BookingRepository.expire()
        ↓
Hủy hóa đơn CHƯA THANH TOÁN quá hạn
        ↓
Giải phóng ca sân về CÒN TRỐNG
        ↓
BookingRepository.expireSlots()
        ↓
Đánh dấu ca đã qua giờ là HẾT
```

---

## 10. CÁC LỖI THƯỜNG GẶP

### 10.1 Lỗi dịch vụ không hiển thị

**Triệu chứng:** Danh sách dịch vụ trống

**Nguyên nhân:**
- Bảng DichVu chưa có dữ liệu
- Lỗi kết nối DB

**Cách sửa:**
```sql
-- Kiểm tra dịch vụ
SELECT * FROM DichVu;

-- Thêm dịch vụ mẫu
INSERT INTO DichVu (ma_dv, ten_dv, don_gia) VALUES
('DV001', N'Nước suối', 10000),
('DV002', N'Áo bib', 30000),
('DV003', N'Bóng thi đấu', 50000);
```

### 10.2 Lỗi CronJob không chạy

**Triệu chứng:** Hóa đơn quá hạn không bị hủy tự động

**Nguyên nhân:**
- ExpiryListener chưa được đăng ký
- ScheduledExecutorService chưa khởi động

**Cách sửa:**
```java
// Kiểm tra annotation @WebListener
// Đảm bảo ExpiryListener được đăng ký trong web.xml hoặc qua annotation
```

### 10.3 Lỗi Dashboard không hiển thị

**Triệu chứng:** FieldDashboardServlet không hiển thị dữ liệu

**Nguyên nhân:**
- BookingRepository chưa có method getBookingsByField()
- Lỗi kết nối DB

**Cách sửa:**
```java
// Kiểm tra method trong BookingRepository
List<Map<String, Object>> bookings = repo.getBookingsByField(fieldId, date);
```

---

## 11. LƯU Ý QUAN TRỌNG

### 11.1 CLASS DÙNG CHUNG

1. **AdminCrudServlet.java** (1189 dòng)
   - Người 4 cần đọc toàn bộ để hiểu cách quản lý
   - Chỉ sửa phần liên quan đến DichVu, San, LichSan, HoaDon, CTHD

2. **BookingRepository.java** (1189 dòng)
   - Người 4 cần đọc phần `expire()`, `expireSlots()`, `getBookingsByField()`, `getInvoicesByField()`
   - Không sửa code liên quan đến đặt sân của Người 2

3. **DBConnection.java** (41 dòng)
   - Tất cả đều dùng
   - Không sửa

### 11.2 QUY TẮC CODE

1. **Luôn kiểm tra quyền Admin trước khi thao tác:**
```java
// AdminAuthFilter đã kiểm tra
// Nhưng nên kiểm tra lại trong nghiệp vụ
```

2. **Sử dụng transaction khi cập nhật nhiều bảng:**
```java
c.setAutoCommit(false);
try {
    // Cập nhật HoaDon
    // Cập nhật LichSan
    c.commit();
} catch (Exception e) {
    c.rollback();
    throw e;
}
```

3. **Không xóa hóa đơn đã thanh toán:**
```java
if ("ĐÃ THANH TOÁN".equals(trangThai)) {
    throw new IllegalStateException("Không thể xóa hóa đơn đã thanh toán");
}
```

---

## 12. TÀI LIỆU THAM KHẢO

- **Java Servlet:** https://docs.oracle.com/javaee/8/tutorial/servlets.htm
- **ServletContextListener:** https://docs.oracle.com/javaee/8/tutorial/servlets014.htm
- **ScheduledExecutorService:** https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html
- **JDBC:** https://docs.oracle.com/javase/tutorial/jdbc/

---

**NGƯỜI PHỤ TRÁCH:** Người 4  
**FILE ĐẦU RA:** 04_Admin_DichVu_CronJob.md  
**TRẠNG THÁI:** Hoàn thành