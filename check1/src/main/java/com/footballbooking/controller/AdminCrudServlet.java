package com.footballbooking.controller;

import com.footballbooking.config.DBConnection;
import com.footballbooking.model.Entities.Voucher;
import com.footballbooking.model.Entities.VoucherLoai;
import com.footballbooking.repository.BookingRepository;
import com.footballbooking.service.VoucherService;
import com.footballbooking.utils.Slots;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Servlet xử lý các thao tác CRUD (Thêm, Sửa, Xóa, Liệt kê) cho các thực thể trong hệ thống quản trị.
 * Hỗ trợ các bảng: Sân, Lịch Sân, Khách Hàng, Nhân Viên, Dịch Vụ, Hóa Đơn và Chi Tiết Hóa Đơn.
 */
@WebServlet("/admin/manage")
public class AdminCrudServlet extends HttpServlet {
    // Bản đồ chứa tên hiển thị tiếng Việt cho các bảng và cột trong CSDL
    private static final Map<String, String> L = new HashMap<>();
    static {
        L.put("San", "Sân"); L.put("LichSan", "Lịch Sân"); L.put("KhachHang", "Khách Hàng");
        L.put("NhanVien", "Nhân Viên"); L.put("DichVu", "Dịch Vụ"); L.put("HoaDon", "Hóa Đơn");
        L.put("CTHD", "Chi Tiết Hóa Đơn");
        L.put("ma_san", "Mã Sân"); L.put("ten_san", "Tên Sân"); L.put("loai_san", "Loại Sân");
        L.put("gia_theo_gio", "Giá/Giờ"); L.put("trang_thai", "Trạng Thái");
        L.put("ma_lich", "Mã Lịch"); L.put("ngay_dat", "Ngày Đặt"); L.put("buoi", "Buổi");
        L.put("so_ca", "Số Ca"); L.put("ca", "Ca"); L.put("gio_bat_dau", "Bắt Đầu");
        L.put("gio_ket_thuc", "Kết Thúc");
        L.put("ma_kh", "Mã KH"); L.put("ho_ten", "Họ Tên"); L.put("sdt", "Số Điện Thoại");
        L.put("email", "Email");
        L.put("ma_nv", "Mã NV"); L.put("ten_nv", "Tên NV"); L.put("chuc_vu", "Chức Vụ");
        L.put("ma_dv", "Mã DV"); L.put("ten_dv", "Tên DV"); L.put("don_gia", "Đơn Giá");
        L.put("ma_hd", "Mã HĐ"); L.put("ngay_lap", "Ngày Lập"); L.put("tong_tien", "Tổng Tiền");
        L.put("tien_coc", "Tiền Cọc"); L.put("so_tien_con_lai", "Còn Lại");
        L.put("han_thanh_toan", "Hạn Thanh Toán");
        L.put("da_dat_san", "Đã Nhận Sân");
        L.put("ma_cthd", "Mã CTHD"); L.put("so_luong", "Số Lượng"); L.put("thanh_tien", "Thành Tiền");
        L.put("Voucher", "Voucher");
        L.put("ma_voucher", "Mã Voucher");
        L.put("ma_code", "Mã Code");
        L.put("ten_voucher", "Tên Voucher");
        L.put("loai_giam", "Loại Giảm");
        L.put("gia_tri", "Giá Trị");
        L.put("don_toi_thieu", "Đơn Tối Thiểu");
        L.put("ngay_bat_dau", "Ngày Bắt Đầu");
        L.put("ngay_ket_thuc", "Ngày Kết Thúc");
        L.put("so_luong", "Số Lượng");
        L.put("trang_thai", "Trạng Thái");
        L.put("loai", "Loại Áp Dụng");
        // Alias legacy (nếu form cũ còn tham chiếu)
        L.put("idVoucher", "Mã Voucher");
        L.put("ngayBatDau", "Ngày Bắt Đầu");
        L.put("ngayKetThuc", "Ngày Kết Thúc");
        L.put("soLuong", "Số Lượng");
        L.put("loaiGiam", "Loại Giảm");
        L.put("mucToiThieu", "Mức Tối Thiểu");
        L.put("mucGiam", "Mức Giảm");
        L.put("loaiVoucher", "Loại Voucher");
    }

    // Tập hợp các trạng thái hợp lệ để kiểm tra tính toàn vẹn dữ liệu
    private static final Set<String> INVOICE_STATUS = new HashSet<>(Arrays.asList(
            "ĐÃ ĐẶT", "CHƯA THANH TOÁN", "MỚI THANH TOÁN CỌC", "ĐÃ THANH TOÁN", "ĐÃ HỦY", "ĐÃ XÓA"
    ));
    private static final Set<String> LICH_SAN_STATUS = new HashSet<>(Arrays.asList(
            "CÒN TRỐNG", "ĐANG GIỮ CHỖ", "ĐÃ ĐẶT", "SỰ KIỆN", "HẾT"
    ));
    private static final Set<String> SAN_STATUS = new HashSet<>(Arrays.asList(
            "HOẠT ĐỘNG", "BẢO TRÌ", "ĐÃ XÓA"
    ));

    /**
     * Lớp nội bộ lưu trữ thông tin cấu trúc (Metadata) của một bảng.
     */
    public static final class Meta {
        final String table, pk;
        final String[] cols;
        Meta(String t, String p, String... c) {
            table = t; pk = p; cols = c;
        }
        public String getTable() { return table; }
        public String getPk() { return pk; }
        public String[] getCols() { return cols; }
    }

    // Định nghĩa Metadata cho từng bảng để xử lý tự động trong Servlet
    private static final Map<String, Meta> M = new LinkedHashMap<>();
    static {
        M.put("San", new Meta("San", "ma_san", "ma_san", "ten_san", "loai_san", "gia_theo_gio", "trang_thai"));
        M.put("LichSan", new Meta("LichSan", "ma_lich", "ma_lich", "ma_san", "ngay_dat", "buoi", "so_ca", "ca", "gio_bat_dau", "gio_ket_thuc", "trang_thai"));
        M.put("KhachHang", new Meta("KhachHang", "ma_kh", "ma_kh", "ho_ten", "sdt", "email"));
        M.put("NhanVien", new Meta("NhanVien", "ma_nv", "ma_nv", "ten_nv", "sdt", "chuc_vu"));
        M.put("DichVu", new Meta("DichVu", "ma_dv", "ma_dv", "ten_dv", "don_gia"));
        M.put("HoaDon", new Meta("HoaDon", "ma_hd", "ma_hd", "ma_kh", "ma_nv", "ngay_lap", "tong_tien", "tien_coc", "so_tien_con_lai", "trang_thai", "da_dat_san"));
        M.put("CTHD", new Meta("CTHD", "ma_cthd", "ma_cthd", "ma_hd", "ma_lich", "ma_dv", "so_luong", "don_gia", "thanh_tien"));
        M.put("Voucher", new Meta("Voucher", "ma_voucher",
                "ma_voucher", "ma_code", "ten_voucher", "loai_giam", "gia_tri",
                "don_toi_thieu", "ngay_bat_dau", "ngay_ket_thuc", "so_luong", "trang_thai", "loai"));
    }

    private final VoucherService voucherService = new VoucherService();

    private Meta meta(HttpServletRequest q) throws ServletException {
        Meta m = M.get(q.getParameter("entity"));
        if (m == null) throw new ServletException("Danh mục không hợp lệ");
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            q.setAttribute("L", L);
            Meta m = meta(q);
            String a = q.getParameter("action");

            // Nếu action là 'form', hiển thị giao diện Thêm/Sửa
            if ("form".equals(a)) {
                prepareForm(q, m);
                q.getRequestDispatcher("/jsp/admin/form.jsp").forward(q, p);
                return;
            }
            // Mặc định liệt kê danh sách bản ghi
            list(q, p, m);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Chuẩn bị dữ liệu cho form Thêm/Sửa.
     */
    private void prepareForm(HttpServletRequest q, Meta m) throws Exception {
        String id = q.getParameter("id");
        Map<String, Object> record = find(m, id);
        if (record == null && "Voucher".equals(m.table)) {
            record = new LinkedHashMap<>();
            // Prefill loai khi thêm từ trang Sân / Dịch vụ
            String loaiParam = q.getParameter("loai");
            if (loaiParam != null && !loaiParam.isBlank()) {
                record.put("loai", Integer.parseInt(loaiParam.trim()));
            } else {
                record.put("loai", VoucherLoai.HOA_DON);
            }
            record.put("trang_thai", "HOẠT ĐỘNG");
            record.put("loai_giam", "MONEY");
        }
        q.setAttribute("record", record);
        q.setAttribute("isNew", id == null);
        q.setAttribute("meta", m);

        if ("HoaDon".equals(m.table)) {
            prepareHoaDonForm(q, id);
        } else if ("CTHD".equals(m.table) && id != null) {
            prepareCthdForm(q, record);
        }
    }

    /**
     * Chuẩn bị dữ liệu đặc thù cho form Hóa đơn.
     * Tối ưu hiệu năng: dùng EXISTS để xác định sân đã thuộc hóa đơn.
     * Đảm bảo tất cả sân còn trống trong cùng ngày đặt đều hiển thị.
     */
    private void prepareHoaDonForm(HttpServletRequest q, String id) throws SQLException {
        BookingRepository repo = new BookingRepository();
        q.setAttribute("services", repo.services());

        // Xác định ngày đặt của hóa đơn
        java.sql.Date invoiceDate = null;
        if (id != null) {
            Map<String, Object> inv = repo.invoice(id);
            if (inv != null) {
                q.setAttribute("inv", inv);
                Map<String, Integer> qty = new HashMap<>();
                List<String> sids = new ArrayList<>();
                List<Map<String, Object>> servicesList = (List<Map<String, Object>>) inv.get("services");
                for (Map<String, Object> s : servicesList) {
                    qty.put((String) s.get("ma_dv"), (Integer) s.get("so_luong"));
                    sids.add((String) s.get("ma_dv"));
                }
                q.setAttribute("editServiceQty", qty);
                q.setAttribute("editServices", sids);

                // Lấy ngày đặt từ slot đầu tiên của hóa đơn
                List<Map<String, Object>> invSlots = (List<Map<String, Object>>) inv.get("slots");
                if (invSlots != null && !invSlots.isEmpty()) {
                    Object ngayDat = invSlots.get(0).get("ngay_dat");
                    if (ngayDat instanceof java.sql.Date) {
                        invoiceDate = (java.sql.Date) ngayDat;
                    } else if (ngayDat instanceof java.sql.Timestamp) {
                        invoiceDate = new java.sql.Date(((java.sql.Timestamp) ngayDat).getTime());
                    } else if (ngayDat != null) {
                        try {
                            invoiceDate = java.sql.Date.valueOf(String.valueOf(ngayDat));
                        } catch (Exception ignored) { }
                    }
                }
            }
        }

        // Nếu không có ngày đặt (hóa đơn mới), dùng ngày hôm nay
        if (invoiceDate == null) {
            invoiceDate = java.sql.Date.valueOf(LocalDate.now());
        }

        // Xây dựng bảng tạm chứa 9 ca trong ngày từ Slots.DATA (giống BookingRepository.slots)
        StringBuilder values = new StringBuilder();
        for (String[] x : Slots.DATA) {
            if (values.length() > 0) values.append(',');
            values.append('(').append(x[1]).append(",N'").append(x[0]).append("','")
                    .append(x[2]).append("','").append(x[3]).append("','").append(x[4]).append("')");
        }

        String sql =
                "SELECT " +
                        "s.ma_san, " +
                        "s.ten_san, " +
                        "ls.ma_lich, " +
                        "ls.ca, " +
                        "ls.ngay_dat, " +
                        "x.so_ca, " +
                        "x.gio_bat_dau, " +
                        "x.gio_ket_thuc, " +
                        "CASE WHEN c.ma_lich IS NOT NULL THEN 1 ELSE 0 END checked " +
                        "FROM (VALUES " + values + ") x(so_ca,buoi,gio_bat_dau,gio_ket_thuc,nhan_ca) " +
                        "CROSS JOIN San s " +
                        "LEFT JOIN LichSan ls ON ls.ma_san=s.ma_san AND ls.ngay_dat=? AND ls.so_ca=x.so_ca " +
                        "LEFT JOIN CTHD c ON c.ma_lich=ls.ma_lich AND c.ma_hd=? AND c.ma_dv IS NULL " +
                        "WHERE s.trang_thai=N'HOẠT ĐỘNG' " +
                        "AND ( " +
                        "      ls.ma_lich IS NULL " +
                        "      OR ls.trang_thai=N'CÒN TRỐNG' " +
                        "      OR c.ma_lich IS NOT NULL " +
                        ") " +
                        // Lọc ca đã hết hiệu lực: nếu là ngày hôm nay, chỉ hiển thị ca chưa qua giờ bắt đầu
                        // (trừ các ca đã thuộc hóa đơn này để người dùng vẫn có thể bỏ chọn)
                        "AND ( " +
                        "      ? <> CAST(SYSDATETIME() AS DATE) " +
                        "      OR CAST(x.gio_bat_dau AS TIME) > CAST(SYSDATETIME() AS TIME) " +
                        "      OR c.ma_lich IS NOT NULL " +
                        ") " +
                        "ORDER BY x.so_ca,s.ten_san";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, invoiceDate);
            s.setString(2, id);
            s.setDate(3, invoiceDate);
            List<Map<String, Object>> avail = new ArrayList<>();
            ResultSet r = s.executeQuery();
            while (r.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ma_san", r.getString("ma_san"));
                m.put("ma_lich", r.getString("ma_lich"));
                m.put("ten_san", r.getString("ten_san"));
                m.put("ca", r.getString("ca"));
                m.put("ngay_dat", r.getDate("ngay_dat"));
                m.put("so_ca", r.getInt("so_ca"));
                m.put("gio_bat_dau", r.getString("gio_bat_dau"));
                m.put("gio_ket_thuc", r.getString("gio_ket_thuc"));
                // Đánh dấu đã chọn nếu sân đã thuộc hóa đơn này
                m.put("checked", r.getInt("checked") == 1);
                avail.add(m);
            }
            q.setAttribute("availableSlots", avail);
        }
    }

    /**
     * Chuẩn bị dữ liệu đặc thù cho form Chi tiết hóa đơn.
     */
    private void prepareCthdForm(HttpServletRequest q, Map<String, Object> record) throws SQLException {
        if (record != null && record.get("ma_hd") != null) {
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement s = c.prepareStatement("SELECT trang_thai FROM HoaDon WHERE ma_hd=?")) {
                s.setString(1, String.valueOf(record.get("ma_hd")));
                ResultSet r = s.executeQuery();
                if (r.next()) q.setAttribute("invoiceStatus", r.getString(1));
            }
        }
    }

    /**
     * Liệt kê danh sách các bản ghi có phân trang, tìm kiếm và sắp xếp.
     */
    private void list(HttpServletRequest q, HttpServletResponse p, Meta m) throws Exception {
        String search = val(q, "search");
        String status = val(q, "status");
        String sort = val(q, "sort");
        if (!contains(m.cols, sort)) sort = m.pk;

        // Mặc định hóa đơn mới nhất lên đầu
        String dirParam = q.getParameter("dir");
        if (dirParam == null && "HoaDon".equals(m.table)) dirParam = "desc";
        String dir = "desc".equalsIgnoreCase(dirParam) ? "DESC" : "ASC";

        // Xóa hóa đơn trống khi xem danh sách hóa đơn
        if ("HoaDon".equals(m.table)) {
            new BookingRepository().deleteEmptyInvoices();
        }

        int page = Math.max(1, num(q.getParameter("page"), 1));
        int size = 8;
        int off = (page - 1) * size;
        boolean hasStatus = contains(m.cols, "trang_thai");

        // Xây dựng điều kiện WHERE
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if ("San".equals(m.table) || "HoaDon".equals(m.table)) {
            where.append(" AND trang_thai <> N'ĐÃ XÓA'");
        }
        // Lọc Voucher theo loai (1=HĐ, 2=Sân, 3=DV) nếu có tham số
        String loaiFilter = val(q, "loai");
        List<String> args = new ArrayList<>();
        if ("Voucher".equals(m.table) && !loaiFilter.isEmpty()) {
            where.append(" AND loai=?");
            args.add(loaiFilter);
        }
        buildSearchAndStatusQuery(where, args, m, search, status, hasStatus);

        // Lọc theo khoảng ngày cho Hóa đơn
        String fromDate = val(q, "fromDate");
        String toDate = val(q, "toDate");
        if ("HoaDon".equals(m.table)) {
            if (!fromDate.isEmpty()) {
                where.append(" AND CAST(ngay_lap AS DATE) >= ?");
                args.add(fromDate);
            }
            if (!toDate.isEmpty()) {
                where.append(" AND CAST(ngay_lap AS DATE) <= ?");
                args.add(toDate);
            }
        }

        String base = " FROM " + m.table + ("CTHD".equals(m.table) ? " JOIN HoaDon hd ON CTHD.ma_hd = hd.ma_hd " : "") + where;

        // Đếm tổng và lấy dữ liệu
        int total = countRecords(base, args);
        List<Map<String, Object>> rows = fetchRows(base, args, m, sort, dir, off, size);

        q.setAttribute("meta", m);
        q.setAttribute("rows", rows);
        q.setAttribute("totalPages", Math.max(1, (int) Math.ceil(total / (double) size)));
        q.setAttribute("page", page);
        q.setAttribute("hasStatus", hasStatus);

        // Tính doanh thu cho trang Hóa đơn
        if ("HoaDon".equals(m.table)) {
            q.setAttribute("revenue", calculateRevenue(fromDate, toDate));
            q.setAttribute("dailyRevenue", calculateDailyRevenue(fromDate, toDate));
        }

        // Khu vực "Voucher áp dụng" trên trang Sân / Dịch vụ
        attachEntityVouchers(q, m);

        q.getRequestDispatcher("/jsp/admin/list.jsp").forward(q, p);
    }

    /**
     * Tính tổng doanh thu theo khoảng ngày (mặc định tất cả).
     * Tổng doanh thu bao gồm: tiền sân đã thanh toán + cọc đã thanh toán (từ các đơn đã thanh toán cọc, đã thanh toán, và cả đã hủy)
     */
    private Map<String, Object> calculateRevenue(String fromDate, String toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "COUNT(*) AS total_invoices, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' THEN h.tong_tien ELSE 0 END),0) + " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'MỚI THANH TOÁN CỌC' THEN h.tien_coc ELSE 0 END),0) + " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ HỦY' THEN h.tien_coc ELSE 0 END),0) AS total_revenue, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' OR h.trang_thai=N'MỚI THANH TOÁN CỌC' OR h.trang_thai=N'CHƯA THANH TOÁN' OR h.trang_thai=N'ĐÃ HỦY' THEN h.tien_coc ELSE 0 END),0) AS total_deposit, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai <> N'ĐÃ HỦY' THEN h.tong_tien ELSE 0 END),0) AS total_expected, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai <> N'ĐÃ HỦY' AND h.trang_thai <> N'ĐÃ THANH TOÁN' THEN h.so_tien_con_lai ELSE 0 END),0) AS total_remaining, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ HỦY' THEN 1 ELSE 0 END),0) AS cancelled_count, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'CHƯA THANH TOÁN' OR h.trang_thai=N'MỚI THANH TOÁN CỌC' OR h.trang_thai=N'ĐÃ ĐẶT' THEN 1 ELSE 0 END),0) AS pending_count, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' THEN 1 ELSE 0 END),0) AS paid_count, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' THEN h.tong_tien ELSE 0 END),0) AS paid_total, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' THEN h.tien_coc ELSE 0 END),0) AS paid_deposit, " +
                        /* ✅ ĐÃ SỬA: Lấy dữ liệu trực tiếp từ bảng subquery đã JOIN */
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' THEN ct_summary.paid_field ELSE 0 END),0) AS paid_field, " +
                        "ISNULL(SUM(CASE WHEN h.trang_thai=N'ĐÃ THANH TOÁN' THEN ct_summary.paid_service ELSE 0 END),0) AS paid_service " +
                        "FROM HoaDon h " +
                        /* ✅ LEFT JOIN với Subquery đã gom nhóm sẵn theo từng hóa đơn */
                        "LEFT JOIN (" +
                        "    SELECT " +
                        "        ma_hd, " +
                        "        SUM(CASE WHEN ma_dv IS NULL THEN thanh_tien ELSE 0 END) AS paid_field, " +
                        "        SUM(CASE WHEN ma_dv IS NOT NULL THEN thanh_tien ELSE 0 END) AS paid_service " +
                        "    FROM CTHD " +
                        "    GROUP BY ma_hd" +
                        ") ct_summary ON h.ma_hd = ct_summary.ma_hd " +
                        "WHERE h.trang_thai <> N'ĐÃ XÓA'");

        List<String> args = new ArrayList<>();
        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND CAST(h.ngay_lap AS DATE) >= ?");
            args.add(fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND CAST(h.ngay_lap AS DATE) <= ?");
            args.add(toDate);
        }

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                s.setString(i + 1, args.get(i));
            }
            ResultSet r = s.executeQuery();
            Map<String, Object> rev = new LinkedHashMap<>();
            if (r.next()) {
                rev.put("total_invoices", r.getInt("total_invoices"));
                rev.put("total_revenue", r.getDouble("total_revenue"));
                rev.put("total_expected", r.getDouble("total_expected"));
                rev.put("total_deposit", r.getDouble("total_deposit"));
                rev.put("total_remaining", r.getDouble("total_remaining"));
                rev.put("cancelled_count", r.getInt("cancelled_count"));
                rev.put("pending_count", r.getInt("pending_count"));
                rev.put("paid_count", r.getInt("paid_count"));
                rev.put("paid_total", r.getDouble("paid_total"));
                rev.put("paid_deposit", r.getDouble("paid_deposit"));
                // Bạn có thể put thêm paid_field và paid_service vào Map nếu cần dùng
                rev.put("paid_field", r.getDouble("paid_field"));
                rev.put("paid_service", r.getDouble("paid_service"));
            }
            return rev;
        }
    }

    /**
     * Tính doanh thu theo từng ngày trong khoảng thời gian.
     */
    private List<Map<String, Object>> calculateDailyRevenue(String fromDate, String toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                "CAST(ngay_lap AS DATE) as ngay, " +
                "COUNT(*) as so_hoa_don, " +
                "ISNULL(SUM(CASE WHEN trang_thai=N'ĐÃ THANH TOÁN' THEN tong_tien ELSE 0 END),0) as doanh_thu, " +
                "ISNULL(SUM(CASE WHEN trang_thai=N'ĐÃ THANH TOÁN' OR trang_thai=N'MỚI THANH TOÁN CỌC' OR trang_thai=N'ĐÃ HỦY' THEN tien_coc ELSE 0 END),0) as tien_coc " +
                "FROM HoaDon " +
                "WHERE trang_thai <> N'ĐÃ XÓA'");

        List<String> args = new ArrayList<>();
        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND CAST(ngay_lap AS DATE) >= ?");
            args.add(fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND CAST(ngay_lap AS DATE) <= ?");
            args.add(toDate);
        }
        
        sql.append(" GROUP BY CAST(ngay_lap AS DATE) ORDER BY ngay ASC");

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                s.setString(i + 1, args.get(i));
            }
            ResultSet r = s.executeQuery();
            while (r.next()) {
                Map<String, Object> day = new LinkedHashMap<>();
                day.put("ngay", r.getDate("ngay"));
                day.put("so_hoa_don", r.getInt("so_hoa_don"));
                day.put("doanh_thu", r.getDouble("doanh_thu"));
                day.put("tien_coc", r.getDouble("tien_coc"));
                result.add(day);
            }
        }
        return result;
    }

    /**
     * Gắn danh sách voucher theo loai vào request khi quản lý Sân (loai=2) hoặc Dịch vụ (loai=3).
     */
    private void attachEntityVouchers(HttpServletRequest q, Meta m) {
        try {
            if ("San".equals(m.table)) {
                q.setAttribute("entityVouchers", voucherService.getVoucherSanAsMaps());
                q.setAttribute("voucherLoai", VoucherLoai.SAN);
                q.setAttribute("voucherLoaiLabel", "Sân");
            } else if ("DichVu".equals(m.table)) {
                q.setAttribute("entityVouchers", voucherService.getVoucherDichVuAsMaps());
                q.setAttribute("voucherLoai", VoucherLoai.DICH_VU);
                q.setAttribute("voucherLoaiLabel", "Dịch vụ");
            }
        } catch (Exception e) {
            q.setAttribute("entityVouchers", List.of());
            System.err.println("attachEntityVouchers: " + e.getMessage());
        }
    }

    private void buildSearchAndStatusQuery(StringBuilder where, List<String> args, Meta m, String search, String status, boolean hasStatus) {
        if (!search.isEmpty()) {
            where.append(" AND (");
            for (int i = 0; i < m.cols.length; i++) {
                if (i > 0) where.append(" OR ");
                where.append("CAST(").append(m.cols[i]).append(" AS NVARCHAR(200)) LIKE ?");
                args.add("%" + search + "%");
            }
            where.append(")");
        }
        if (hasStatus && !status.isEmpty()) {
            where.append(" AND trang_thai=?");
            args.add(status);
        }
    }

    private int countRecords(String base, List<String> args) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT COUNT(*)" + base)) {
            bind(s, args);
            ResultSet r = s.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private List<Map<String, Object>> fetchRows(String base, List<String> args, Meta m, String sort, String dir, int off, int size) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String selectCols = "CTHD".equals(m.table) ? "CTHD.*, hd.trang_thai AS trang_thai_hd" : String.join(",", m.cols);
        String sql = "SELECT " + selectCols + base + " ORDER BY " + ("CTHD".equals(m.table) ? "CTHD." + sort : sort) + " " + dir + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, args);
            s.setInt(args.size() + 1, off);
            s.setInt(args.size() + 2, size);
            ResultSet r = s.executeQuery();
            while (r.next()) {
                Map<String, Object> x = new LinkedHashMap<>();
                for (String col : m.cols) x.put(col, r.getObject(col));
                if ("CTHD".equals(m.table)) x.put("trang_thai_hd", r.getObject("trang_thai_hd"));
                rows.add(x);
            }
        }
        return rows;
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse p) throws ServletException, IOException {
        try {
            Meta m = meta(q);
            String op = q.getParameter("op");
            String id = q.getParameter(m.pk);
            
            // Xử lý hành động Xác nhận đặt sân
            if ("confirmDatSan".equals(op) && "HoaDon".equals(m.table)) {
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement s = c.prepareStatement(
                             "UPDATE HoaDon SET da_dat_san=1 WHERE ma_hd=? AND trang_thai <> N'ĐÃ HỦY' AND da_dat_san=0")) {
                    s.setString(1, id);
                    s.executeUpdate();
                }
                // Quay lại form chỉnh sửa hóa đơn để thấy trạng thái đã nhận sân
                p.sendRedirect(q.getContextPath() + "/admin/manage?entity=HoaDon&action=form&id=" + id + "&datsan=1");
                return;
            }
            
            // Xử lý hành động Xóa
            if ("delete".equals(op)) {
                if ("Voucher".equals(m.table)) {
                    voucherService.delete(id);
                    // Quay lại trang Sân/Dịch vụ nếu xóa từ khu vực voucher
                    String back = q.getParameter("back");
                    if (back != null && !back.isBlank()) {
                        p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + back + "&ok=1");
                        return;
                    }
                } else if ("San".equals(m.table)) {
                    // Xóa mềm cho Sân: Cập nhật trạng thái thành 'ĐÃ XÓA'
                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement s = c.prepareStatement("UPDATE San SET trang_thai=N'ĐÃ XÓA' WHERE ma_san=?")) {
                        s.setString(1, id);
                        s.executeUpdate();
                    }
                } else if ("HoaDon".equals(m.table)) {
                    // Xóa mềm cho Hóa đơn: Giải phóng sân và đánh dấu 'ĐÃ XÓA'
                    try (Connection c = DBConnection.getConnection()) {
                        c.setAutoCommit(false);
                        try {
                            // 1. Giải phóng các sân trong LichSan liên quan đến hóa đơn này
                            try (PreparedStatement s = c.prepareStatement(
                                    "UPDATE LichSan SET trang_thai=N'CÒN TRỐNG' WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd=?)")) {
                                s.setString(1, id);
                                s.executeUpdate();
                            }
                            // 2. Đánh dấu hóa đơn là ĐÃ XÓA
                            try (PreparedStatement s = c.prepareStatement("UPDATE HoaDon SET trang_thai=N'ĐÃ XÓA' WHERE ma_hd=?")) {
                                s.setString(1, id);
                                s.executeUpdate();
                            }
                            c.commit();
                        } catch (Exception ex) {
                            c.rollback();
                            throw ex;
                        } finally {
                            c.setAutoCommit(true);
                        }
                    }
                } else {
                    // Xóa vật lý cho các thực thể khác
                    try (Connection c = DBConnection.getConnection();
                         PreparedStatement s = c.prepareStatement("DELETE FROM " + m.table + " WHERE " + m.pk + "=?")) {
                        s.setString(1, id);
                        s.executeUpdate();
                    }
                }
            } else {
                // Xử lý Thêm mới hoặc Cập nhật
                if ("Voucher".equals(m.table)) {
                    saveVoucher(q, "create".equals(op));
                    String back = q.getParameter("back");
                    if (back != null && !back.isBlank()) {
                        p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + back + "&ok=1");
                        return;
                    }
                } else {
                    save(q, m, "create".equals(op));
                }
            }
            // Nếu người dùng bấm "Lưu & Thanh toán", chuyển sang form thanh toán sau khi lưu
            String afterSave = q.getParameter("afterSave");
            if ("payment".equals(afterSave) && "HoaDon".equals(m.table)) {
                String savedId = q.getParameter(m.pk);
                if (savedId != null && !savedId.isBlank()) {
                    p.sendRedirect(q.getContextPath() + "/admin/invoice-management?op=paymentForm&id=" + savedId + "&saved=1");
                    return;
                }
            }
            String redirectLoai = q.getParameter("loai");
            String extra = ("Voucher".equals(m.table) && redirectLoai != null && !redirectLoai.isBlank())
                    ? "&loai=" + redirectLoai : "";
            p.sendRedirect(q.getContextPath() + "/admin/manage?entity=" + m.table + "&ok=1" + extra);
        } catch (Exception e) {
            q.setAttribute("error", friendly(e));
            doGet(q, p);
        }
    }

    /**
     * Lưu Voucher qua VoucherService (đảm bảo map đầy đủ cột loai).
     */
    private void saveVoucher(HttpServletRequest q, boolean create) throws Exception {
        Voucher v = new Voucher();
        v.setMaVoucher(trim(q.getParameter("ma_voucher")));
        v.setMaCode(trim(q.getParameter("ma_code")));
        v.setTenVoucher(trim(q.getParameter("ten_voucher")));
        v.setLoaiGiam(trim(q.getParameter("loai_giam")));
        if (v.getLoaiGiam() == null || v.getLoaiGiam().isBlank()) v.setLoaiGiam("MONEY");
        v.setGiaTri(parseDoubleSafe(q.getParameter("gia_tri")));
        v.setDonToiThieu(parseDoubleSafe(q.getParameter("don_toi_thieu")));
        v.setNgayBatDau(parseDate(q.getParameter("ngay_bat_dau")));
        v.setNgayKetThuc(parseDate(q.getParameter("ngay_ket_thuc")));
        v.setSoLuong(parseIntSafe(q.getParameter("so_luong")));
        String st = trim(q.getParameter("trang_thai"));
        v.setTrangThai(st == null || st.isBlank() ? "HOẠT ĐỘNG" : st);
        int loai = parseIntSafe(q.getParameter("loai"));
        if (!VoucherLoai.isValid(loai)) loai = VoucherLoai.HOA_DON;
        v.setLoai(loai);

        if (create) {
            if (v.getMaVoucher() == null || v.getMaVoucher().isBlank()) {
                throw new IllegalArgumentException("Mã voucher không được trống");
            }
            voucherService.insert(v);
        } else {
            voucherService.update(v);
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (Exception e) { return null; }
    }

    /**
     * Lưu hoặc cập nhật dữ liệu một bản ghi.
     */
    private void save(HttpServletRequest q, Meta m, boolean create) throws SQLException {
        List<String> cols = new ArrayList<>(Arrays.asList(m.cols));
        if (!create || "CTHD".equals(m.table)) cols.remove(m.pk);
        // Loại bỏ da_dat_san khỏi danh sách cột khi lưu (chỉ thay đổi qua nút "Đặt sân")
        cols.remove("da_dat_san");

        Map<String, String> vals = new LinkedHashMap<>();
        for (String col : cols) {
            String v = q.getParameter(col);
            vals.put(col, v == null ? null : v.trim());
        }

        // Xử lý logic nghiệp vụ đặc thù
        if ("HoaDon".equals(m.table)) {
            processHoaDonSave(q, m, vals, create);
        } else if ("CTHD".equals(m.table)) {
            processCthdSave(vals);
        } else if ("LichSan".equals(m.table)) {
            vals.put("trang_thai", sanitizeStatus(vals.get("trang_thai"), LICH_SAN_STATUS, "CÒN TRỐNG"));
        } else if ("San".equals(m.table)) {
            vals.put("trang_thai", sanitizeStatus(vals.get("trang_thai"), SAN_STATUS, "HOẠT ĐỘNG"));
        }

        executeSaveQuery(m, cols, vals, q.getParameter(m.pk), create);
    }

    private void processHoaDonSave(HttpServletRequest q, Meta m, Map<String, String> vals, boolean create) throws SQLException {
        String st = sanitizeStatus(vals.get("trang_thai"), INVOICE_STATUS, "CHƯA THANH TOÁN");
        vals.put("trang_thai", st);

        if (!create) {
            String id = q.getParameter(m.pk);
            if (id != null) {
                updateHoaDonFull(q, id, st, vals);
            }
        } else {
            double tong = parseDoubleSafe(vals.get("tong_tien"));
            double coc = tong * 0.3;
            double remain = "ĐÃ THANH TOÁN".equals(st) ? 0d : Math.max(0d, tong - coc);
            vals.put("tien_coc", toDecimalString(coc));
            vals.put("so_tien_con_lai", toDecimalString(remain));
        }
    }

    private void updateHoaDonFull(HttpServletRequest q, String id, String st, Map<String, String> vals) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Cập nhật sân trước để đảm bảo có sân gắn dịch vụ
                updateHoaDonSlots(q, id, conn);
                // Sau đó mới cập nhật dịch vụ
                updateHoaDonServices(q, id, conn);
                recalculateHoaDonTotals(id, st, vals, conn);
                syncLichSanStatus(id, st, conn);
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw new SQLException(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void updateHoaDonServices(HttpServletRequest q, String id, Connection conn) throws SQLException {
        Map<String, Integer> services = new HashMap<>();
        Enumeration<String> params = q.getParameterNames();
        while (params.hasMoreElements()) {
            String p = params.nextElement();
            if (p.startsWith("service_") && "on".equals(q.getParameter(p))) {
                String svId = p.substring(8);
                int qty = parseIntSafe(q.getParameter("qty_" + svId));
                services.put(svId, Math.max(1, qty));
            }
        }

        String maLich = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT TOP 1 ma_lich FROM CTHD WHERE ma_hd=? AND ma_dv IS NULL")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) maLich = rs.getString(1);
        }

        if (maLich != null) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CTHD WHERE ma_hd=? AND ma_dv IS NOT NULL")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            for (Map.Entry<String, Integer> e : services.entrySet()) {
                double pr = getServicePrice(e.getKey(), conn);
                insertCthdService(id, maLich, e.getKey(), e.getValue(), pr, conn);
            }
        }
    }

    private void updateHoaDonSlots(HttpServletRequest q, String id, Connection conn) throws SQLException {
        // Thu thập danh sách các sân/ca được chọn trong form (dạng add_slot_<ma_san>_<so_ca>)
        Set<String> selectedKeys = new HashSet<>();
        Enumeration<String> params = q.getParameterNames();
        while (params.hasMoreElements()) {
            String p = params.nextElement();
            if (p.startsWith("add_slot_") && "on".equals(q.getParameter(p))) {
                selectedKeys.add(p.substring(9));
            }
        }

        // Lấy ngày đặt của hóa đơn
        java.sql.Date invoiceDate = getInvoiceDate(conn, id);

        // Lấy danh sách sân/ca hiện có trong hóa đơn (dạng ma_san_so_ca)
        Set<String> existingKeys = new HashSet<>();
        Map<String, String> keyToLichId = new HashMap<>();
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT l.ma_lich, l.ma_san, l.so_ca FROM CTHD c " +
                "JOIN LichSan l ON l.ma_lich=c.ma_lich " +
                "WHERE c.ma_hd=? AND c.ma_dv IS NULL")) {
            s.setString(1, id);
            ResultSet r = s.executeQuery();
            while (r.next()) {
                String key = r.getString("ma_san") + "_" + r.getInt("so_ca");
                existingKeys.add(key);
                keyToLichId.put(key, r.getString("ma_lich"));
            }
        }

        // Xóa các sân không còn được chọn trong hóa đơn
        for (String oldKey : existingKeys) {
            if (!selectedKeys.contains(oldKey)) {
                String oldLichId = keyToLichId.get(oldKey);
                // Xóa khỏi CTHD
                try (PreparedStatement s = conn.prepareStatement(
                        "DELETE FROM CTHD WHERE ma_hd=? AND ma_lich=? AND ma_dv IS NULL")) {
                    s.setString(1, id);
                    s.setString(2, oldLichId);
                    s.executeUpdate();
                }
                // Giải phóng sân nếu không còn thuộc hóa đơn nào khác
                try (PreparedStatement s = conn.prepareStatement(
                        "UPDATE LichSan SET trang_thai = N'CÒN TRỐNG' WHERE ma_lich=? AND NOT EXISTS (SELECT 1 FROM CTHD WHERE ma_lich = ? AND ma_dv IS NULL AND ma_hd <> ?)")) {
                    s.setString(1, oldLichId);
                    s.setString(2, oldLichId);
                    s.setString(3, id);
                    s.executeUpdate();
                }
            }
        }

        // Thêm các sân mới được chọn
        for (String key : selectedKeys) {
            if (existingKeys.contains(key)) continue; // Đã có trong hóa đơn
            String[] parts = key.split("_", 2);
            if (parts.length != 2) continue;
            String maSan = parts[0];
            int soCa = parseIntSafe(parts[1]);

            // Tìm hoặc tạo bản ghi LichSan cho sân/ca này
            String lichId = findOrCreateLichSan(conn, maSan, soCa, invoiceDate);

            double price = getFieldPrice(conn, maSan);
            try (PreparedStatement s = conn.prepareStatement("INSERT INTO CTHD(ma_hd, ma_lich, ma_dv, so_luong, don_gia, thanh_tien) VALUES(?,?,NULL,1,?,?)")) {
                s.setString(1, id);
                s.setString(2, lichId);
                s.setDouble(3, price);
                s.setDouble(4, price);
                s.executeUpdate();
            }
            try (PreparedStatement s = conn.prepareStatement("UPDATE LichSan SET trang_thai = N'ĐANG GIỮ CHỖ' WHERE ma_lich = ?")) {
                s.setString(1, lichId);
                s.executeUpdate();
            }
        }
    }

    /**
     * Lấy ngày đặt của hóa đơn (từ slot đầu tiên hoặc ngày hôm nay).
     */
    private java.sql.Date getInvoiceDate(Connection conn, String id) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT TOP 1 l.ngay_dat FROM CTHD c JOIN LichSan l ON l.ma_lich=c.ma_lich WHERE c.ma_hd=? AND c.ma_dv IS NULL")) {
            s.setString(1, id);
            ResultSet r = s.executeQuery();
            if (r.next()) {
                java.sql.Date d = r.getDate(1);
                if (d != null) return d;
            }
        }
        return java.sql.Date.valueOf(LocalDate.now());
    }

    /**
     * Tìm bản ghi LichSan theo sân/ca/ngày, nếu chưa có thì tạo mới.
     */
    private String findOrCreateLichSan(Connection conn, String maSan, int soCa, java.sql.Date ngayDat) throws SQLException {
        // Tìm bản ghi LichSan đã tồn tại
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT ma_lich FROM LichSan WHERE ma_san=? AND ngay_dat=? AND so_ca=?")) {
            s.setString(1, maSan);
            s.setDate(2, ngayDat);
            s.setInt(3, soCa);
            ResultSet r = s.executeQuery();
            if (r.next()) return r.getString(1);
        }

        // Tạo mới bản ghi LichSan
        String lichId = nextLichId(conn);
        String[] sl = Slots.get(soCa);
        LocalTime start = LocalTime.parse(sl[2]);
        LocalTime end = LocalTime.parse(sl[3]);
        LocalDateTime startDt = ngayDat.toLocalDate().atTime(start);
        LocalDateTime endDt = ngayDat.toLocalDate().atTime(end);
        if (!end.isAfter(start)) endDt = endDt.plusDays(1);

        try (PreparedStatement s = conn.prepareStatement(
                "INSERT INTO LichSan(ma_lich,ma_san,ca,gio_bat_dau,gio_ket_thuc,trang_thai,ngay_dat,buoi,so_ca) VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1, lichId);
            s.setString(2, maSan);
            s.setString(3, sl[2] + " - " + sl[3]);
            s.setTimestamp(4, Timestamp.valueOf(startDt));
            s.setTimestamp(5, Timestamp.valueOf(endDt));
            s.setString(6, "CÒN TRỐNG");
            s.setDate(7, ngayDat);
            s.setString(8, sl[0]);
            s.setInt(9, soCa);
            s.executeUpdate();
        }
        return lichId;
    }

    /**
     * Sinh mã lịch sân tự tăng tiếp theo.
     */
    private String nextLichId(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet r = s.executeQuery("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(ma_lich,2,20))),0)+1 FROM LichSan")) {
            r.next();
            return "L" + String.format("%03d", r.getInt(1));
        }
    }

    /**
     * Lấy giá thuê của một sân (giá theo giờ * 2 giờ/ca).
     */
    private double getFieldPrice(Connection conn, String maSan) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement("SELECT gia_theo_gio FROM San WHERE ma_san=?")) {
            s.setString(1, maSan);
            ResultSet r = s.executeQuery();
            if (r.next()) return r.getDouble(1) * 2;
        }
        return 600000;
    }

    private double getServicePrice(String svId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT don_gia FROM DichVu WHERE ma_dv=?")) {
            ps.setString(1, svId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private void insertCthdService(String hdId, String lichId, String svId, int qty, double price, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, hdId);
            ps.setString(2, lichId);
            ps.setString(3, svId);
            ps.setInt(4, qty);
            ps.setDouble(5, price);
            ps.setDouble(6, price * qty);
            ps.executeUpdate();
        }
    }

    private void recalculateHoaDonTotals(String id, String st, Map<String, String> vals, Connection conn) throws SQLException {
        double basePrice = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(thanh_tien) FROM CTHD WHERE ma_hd=? AND ma_dv IS NULL")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) basePrice = rs.getDouble(1);
        }
        double svTotal = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(thanh_tien) FROM CTHD WHERE ma_hd=? AND ma_dv IS NOT NULL")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) svTotal = rs.getDouble(1);
        }
        double tong = basePrice + svTotal;
        
        // Lấy trạng thái cũ để xử lý chuyển đổi trạng thái
        String oldStatus = null;
        double oldCoc = 0;
        double oldRemain = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT trang_thai, tien_coc, so_tien_con_lai FROM HoaDon WHERE ma_hd=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                oldStatus = rs.getString("trang_thai");
                oldCoc = rs.getDouble("tien_coc");
                oldRemain = rs.getDouble("so_tien_con_lai");
            }
        }
        
        double coc = 0;
        double remain = 0;
        
        // Xử lý logic chuyển đổi trạng thái
        if ("ĐÃ THANH TOÁN".equals(st)) {
            // Nếu chuyển sang ĐÃ THANH TOÁN: còn lại = 0, cọc = 30%
            coc = tong * 0.3;
            remain = 0;
        } else if ("MỚI THANH TOÁN CỌC".equals(st)) {
            // Nếu chuyển sang MỚI THANH TOÁN CỌC: còn lại = 70%, cọc = 30%
            coc = tong * 0.3;
            remain = tong * 0.7;
        } else if ("CHƯA THANH TOÁN".equals(st)) {
            // Nếu chuyển sang CHƯA THANH TOÁN từ các trạng thái đã thanh toán
            if ("MỚI THANH TOÁN CỌC".equals(oldStatus) || "ĐÃ THANH TOÁN".equals(oldStatus)) {
                // Từ đã thanh toán cọc/đã thanh toán -> chưa thanh toán: cộng cọc vào còn lại
                remain = tong;
                coc = 0;
            } else {
                // Trường hợp khác: giữ nguyên logic cũ
                remain = tong;
                coc = 0;
            }
        } else if ("ĐÃ HỦY".equals(st)) {
            // Nếu hủy: giữ nguyên số tiền cọc và còn lại như cũ (không thay đổi)
            coc = oldCoc;
            remain = oldRemain;
        } else {
            // Các trạng thái khác (ĐÃ ĐẶT, etc.)
            coc = tong * 0.3;
            remain = tong * 0.7;
        }

        vals.put("tien_coc", toDecimalString(coc));
        vals.put("tong_tien", toDecimalString(tong));
        vals.put("so_tien_con_lai", toDecimalString(remain));
    }

    private void syncLichSanStatus(String id, String st, Connection conn) throws SQLException {
        String lsSt = ("ĐÃ HỦY".equals(st) || "ĐÃ XÓA".equals(st)) ? "CÒN TRỐNG" : ("ĐÃ THANH TOÁN".equals(st) || "MỚI THANH TOÁN CỌC".equals(st) || "ĐÃ ĐẶT".equals(st) ? "ĐÃ ĐẶT" : null);
        if (lsSt != null) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE LichSan SET trang_thai=? WHERE ma_lich IN (SELECT ma_lich FROM CTHD WHERE ma_hd=?)")) {
                ps.setString(1, lsSt);
                ps.setString(2, id);
                ps.executeUpdate();
            }
        }
    }

    private void processCthdSave(Map<String, String> vals) {
        int sl = parseIntSafe(vals.get("so_luong"));
        double donGia = parseDoubleSafe(vals.get("don_gia"));
        vals.put("thanh_tien", toDecimalString(sl * donGia));
    }

    private void executeSaveQuery(Meta m, List<String> cols, Map<String, String> vals, String pkValue, boolean create) throws SQLException {
        StringBuilder sql = new StringBuilder(create ? "INSERT INTO " + m.table + " (" : "UPDATE " + m.table + " SET ");
        if (create) sql.append(String.join(",", cols)).append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append(create ? "?" : cols.get(i) + "=?");
        }
        sql.append(create ? ")" : " WHERE " + m.pk + "=?");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql.toString())) {
            int i = 1;
            for (String col : cols) {
                String v = vals.get(col);
                if (v == null || v.isEmpty()) s.setNull(i++, Types.VARCHAR);
                else s.setString(i++, v);
            }
            if (!create) s.setString(i, pkValue);
            s.executeUpdate();
        }
    }

    /**
     * Tìm kiếm một bản ghi cụ thể theo khóa chính.
     */
    private Map<String, Object> find(Meta m, String id) throws SQLException {
        if (id == null) return null;
        if ("Voucher".equals(m.table)) {
            try {
                Voucher v = voucherService.getById(id);
                return v != null ? voucherService.toMap(v) : null;
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT " + String.join(",", m.cols) + " FROM " + m.table + " WHERE " + m.pk + "=?")) {
            s.setString(1, id);
            ResultSet r = s.executeQuery();
            if (!r.next()) return null;
            Map<String, Object> x = new LinkedHashMap<>();
            for (String col : m.cols) x.put(col, r.getObject(col));
            return x;
        }
    }

    /**
     * Gán các tham số vào PreparedStatement.
     */
    private static void bind(PreparedStatement s, List<String> a) throws SQLException {
        for (int i = 0; i < a.size(); i++) s.setString(i + 1, a.get(i));
    }

    /**
     * Lấy giá trị từ tham số request và loại bỏ khoảng trắng thừa.
     */
    private static String val(HttpServletRequest q, String k) {
        String v = q.getParameter(k);
        return v == null ? "" : v.trim();
    }

    /**
     * Chuyển đổi chuỗi sang số nguyên an toàn.
     */
    private static int num(String v, int d) {
        try { return Integer.parseInt(v); }
        catch (Exception e) { return d; }
    }

    private static int parseIntSafe(String v) {
        return num(v, 0);
    }

    /**
     * Kiểm tra một giá trị có tồn tại trong mảng chuỗi hay không.
     */
    private static boolean contains(String[] a, String x) {
        for (String v : a) if (v.equals(x)) return true;
        return false;
    }

    /**
     * Kiểm tra và chuẩn hóa giá trị trạng thái.
     */
    private static String sanitizeStatus(String v, Set<String> allowed, String def) {
        if (v == null) return def;
        String t = v.trim();
        return allowed.contains(t) ? t : def;
    }

    /**
     * Chuyển đổi chuỗi sang số thực an toàn.
     */
    private static double parseDoubleSafe(String v) {
        try { return v == null || v.trim().isEmpty() ? 0d : Double.parseDouble(v.trim()); }
        catch (Exception e) { return 0d; }
    }

    /**
     * Chuyển đổi số thực sang định dạng chuỗi số thập phân để lưu vào CSDL.
     */
    private static String toDecimalString(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return "0";
        return String.valueOf(x);
    }

    private static String friendly(Exception e) {
        return "Không thể lưu dữ liệu. Kiểm tra mã trùng hoặc khóa liên kết.";
    }
}
