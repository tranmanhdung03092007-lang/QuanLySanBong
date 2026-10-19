package com.footballbooking.repository;

import com.footballbooking.config.DBConnection;
import com.footballbooking.utils.Slots;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lớp quản lý truy xuất và thao tác dữ liệu liên quan đến đặt sân và hóa đơn.
 * Chứa toàn bộ các câu lệnh SQL và logic nghiệp vụ tương tác với cơ sở dữ liệu.
 */
public class BookingRepository {

    // Định nghĩa các hằng số trạng thái của sân và hóa đơn
    private static final String FREE = "CÒN TRỐNG";
    private static final String HOLD = "ĐANG GIỮ CHỖ";
    private static final String BOOKED = "ĐÃ ĐẶT";
    private static final String EXPIRED = "HẾT";
    private static final String UNPAID = "CHƯA THANH TOÁN";
    private static final String PROCESSING = "ĐANG XỬ LÝ";
    private static final String DEPOSITED = "MỚI THANH TOÁN CỌC";
    private static final String PAID = "ĐÃ THANH TOÁN";
    private static final String CANCEL = "ĐÃ HỦY";
    private static final String EVENT = "SỰ KIỆN";

    static {
        // Thực hiện nâng cấp cấu trúc bảng (Migration) nếu thiếu các cột cần thiết
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // Thêm các cột mới vào bảng HoaDon nếu chưa tồn tại
            s.execute("IF COL_LENGTH('HoaDon','han_thanh_toan') IS NULL ALTER TABLE HoaDon ADD han_thanh_toan DATETIME NULL");
            s.execute("IF COL_LENGTH('HoaDon','tien_coc') IS NULL ALTER TABLE HoaDon ADD tien_coc DECIMAL(18,2) NOT NULL DEFAULT 200000");
            s.execute("IF COL_LENGTH('HoaDon','so_tien_con_lai') IS NULL ALTER TABLE HoaDon ADD so_tien_con_lai DECIMAL(18,2) NOT NULL DEFAULT 0");
            s.execute("IF COL_LENGTH('HoaDon','ma_voucher') IS NULL ALTER TABLE HoaDon ADD ma_voucher VARCHAR(20) NULL");
            s.execute("IF COL_LENGTH('HoaDon','tien_giam') IS NULL ALTER TABLE HoaDon ADD tien_giam DECIMAL(18,2) NOT NULL DEFAULT 0");

            // Bảng Voucher theo cấu trúc: ma_voucher, ma_code, ten_voucher, loai_giam, gia_tri,
            // don_toi_thieu, ngay_bat_dau, ngay_ket_thuc, so_luong, trang_thai, loai
            // loai: 1=Hóa đơn, 2=Sân, 3=Dịch vụ
            s.execute("IF OBJECT_ID('Voucher', 'U') IS NULL CREATE TABLE Voucher (" +
                    "ma_voucher VARCHAR(20) PRIMARY KEY, " +
                    "ma_code VARCHAR(50) NOT NULL, " +
                    "ten_voucher NVARCHAR(100) NOT NULL, " +
                    "loai_giam VARCHAR(20) NOT NULL, " +
                    "gia_tri FLOAT NOT NULL DEFAULT 0, " +
                    "don_toi_thieu FLOAT NOT NULL DEFAULT 0, " +
                    "ngay_bat_dau DATE NOT NULL, " +
                    "ngay_ket_thuc DATE NOT NULL, " +
                    "so_luong INT NOT NULL DEFAULT 0, " +
                    "trang_thai NVARCHAR(30) NOT NULL DEFAULT N'HOẠT ĐỘNG', " +
                    "loai INT NOT NULL DEFAULT 1)");
            s.execute("IF COL_LENGTH('Voucher','loai') IS NULL ALTER TABLE Voucher ADD loai INT NOT NULL DEFAULT 1");
            s.execute("IF COL_LENGTH('Voucher','ma_code') IS NULL ALTER TABLE Voucher ADD ma_code VARCHAR(50) NULL");
            s.execute("IF COL_LENGTH('Voucher','ten_voucher') IS NULL ALTER TABLE Voucher ADD ten_voucher NVARCHAR(100) NULL");
            s.execute("IF COL_LENGTH('Voucher','loai_giam') IS NULL ALTER TABLE Voucher ADD loai_giam VARCHAR(20) NULL");
            s.execute("IF COL_LENGTH('Voucher','gia_tri') IS NULL ALTER TABLE Voucher ADD gia_tri FLOAT NULL");
            s.execute("IF COL_LENGTH('Voucher','don_toi_thieu') IS NULL ALTER TABLE Voucher ADD don_toi_thieu FLOAT NULL");
            s.execute("IF COL_LENGTH('Voucher','ngay_bat_dau') IS NULL ALTER TABLE Voucher ADD ngay_bat_dau DATE NULL");
            s.execute("IF COL_LENGTH('Voucher','ngay_ket_thuc') IS NULL ALTER TABLE Voucher ADD ngay_ket_thuc DATE NULL");
            s.execute("IF COL_LENGTH('Voucher','so_luong') IS NULL ALTER TABLE Voucher ADD so_luong INT NULL");
            s.execute("IF COL_LENGTH('Voucher','trang_thai') IS NULL ALTER TABLE Voucher ADD trang_thai NVARCHAR(30) NULL");
            // Giữ cột legacy (nếu còn) để không phá dữ liệu cũ; map runtime qua VoucherRepository

            s.execute("IF OBJECT_ID('Voucher_San', 'U') IS NULL CREATE TABLE Voucher_San (" +
                    "ma_voucher VARCHAR(20), " +
                    "ma_san VARCHAR(10) REFERENCES San(ma_san), " +
                    "PRIMARY KEY (ma_voucher, ma_san))");

            s.execute("IF OBJECT_ID('Voucher_DichVu', 'U') IS NULL CREATE TABLE Voucher_DichVu (" +
                    "ma_voucher VARCHAR(20), " +
                    "ma_dv VARCHAR(10) REFERENCES DichVu(ma_dv), " +
                    "PRIMARY KEY (ma_voucher, ma_dv))");
            
            // Thêm các cột bổ sung cho bảng LichSan để quản lý ca sân tốt hơn
            s.execute("IF COL_LENGTH('LichSan','so_ca') IS NULL ALTER TABLE LichSan ADD so_ca TINYINT NULL");
            s.execute("IF COL_LENGTH('LichSan','ngay_dat') IS NULL ALTER TABLE LichSan ADD ngay_dat DATE NULL");
            s.execute("IF COL_LENGTH('LichSan','buoi') IS NULL ALTER TABLE LichSan ADD buoi NVARCHAR(20) NULL");
            
            // Đảm bảo bảng NhanVien có cột mật khẩu để đăng nhập admin
            s.execute("IF COL_LENGTH('NhanVien','mat_khau') IS NULL ALTER TABLE NhanVien ADD mat_khau NVARCHAR(100) NULL");

            // Khởi tạo dữ liệu mặc định cho Admin và chuẩn hóa dữ liệu cũ
            s.execute("UPDATE NhanVien SET mat_khau=N'admin123' WHERE mat_khau IS NULL OR mat_khau=''");
            s.execute("IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE ma_nv='NV001' OR sdt='0900000000') " +
                      "INSERT INTO NhanVien (ma_nv, ten_nv, sdt, chuc_vu, mat_khau) VALUES ('NV001', N'Quản trị viên', '0900000000', N'Admin', N'admin123')");
            s.execute("UPDATE LichSan SET ngay_dat=CAST(gio_bat_dau AS DATE) WHERE ngay_dat IS NULL AND gio_bat_dau IS NOT NULL");
            s.execute("UPDATE LichSan SET buoi=ca WHERE buoi IS NULL AND ca IS NOT NULL");
        } catch (Exception e) {
            System.err.println("Lỗi Migration Schema: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách các sân đang hoạt động.
     * @return List chứa thông tin các sân
     */
    public List<Map<String, Object>> fields() throws SQLException {
        return query("SELECT ma_san,ten_san,loai_san,gia_theo_gio,trang_thai FROM San WHERE trang_thai=N'HOẠT ĐỘNG' "
                + "ORDER BY CASE ma_san WHEN 'S01' THEN 1 WHEN 'S02' THEN 2 WHEN 'SVIP' THEN 3 "
                + "WHEN 'S04' THEN 4 WHEN 'S05' THEN 5 WHEN 'S06' THEN 6 WHEN 'S07' THEN 7 WHEN 'S08' THEN 8 ELSE 99 END");
    }

    /**
     * Lấy danh sách tất cả các dịch vụ.
     */
    public List<Map<String, Object>> services() throws SQLException {
        return query("SELECT ma_dv,ten_dv,don_gia FROM DichVu ORDER BY ma_dv");
    }

    /**
     * Lấy trạng thái các ca (slots) của một sân cụ thể trong một ngày nhất định.
     * @param san Mã sân
     * @param d Ngày cần xem lịch
     * @return Danh sách các ca kèm trạng thái (Trống, Đã đặt, Chờ...)
     */
    public List<Map<String, Object>> slots(String san, LocalDate d) throws SQLException {
        StringBuilder values = new StringBuilder();
        // Xây dựng bảng tạm chứa 9 ca trong ngày từ Slots.DATA
        for (String[] x : Slots.DATA) {
            if (values.length() > 0) values.append(',');
            values.append('(').append(x[1]).append(",N'").append(x[0]).append("','")
                    .append(x[2]).append("','").append(x[3]).append("','").append(x[4]).append("')");
        }
        // Sử dụng GROUP BY và MAX(status_code) để tránh lặp lại ca khi có nhiều bản ghi trạng thái
        String sql = "SELECT x.so_ca, x.buoi, x.gio_bat_dau, x.gio_ket_thuc, x.nhan_ca, "
                + "MAX(CASE WHEN l.trang_thai=N'" + BOOKED + "' THEN 2 WHEN l.trang_thai=N'" + EVENT + "' THEN 4 "
                + "WHEN l.trang_thai=N'" + HOLD + "' THEN 1 WHEN l.trang_thai=N'" + EXPIRED + "' THEN 3 ELSE 0 END) as status_code "
                + "FROM (VALUES " + values + ") x(so_ca,buoi,gio_bat_dau,gio_ket_thuc,nhan_ca) "
                + "LEFT JOIN LichSan l ON l.ma_san=? AND l.ngay_dat=? AND l.so_ca=x.so_ca "
                + "GROUP BY x.so_ca, x.buoi, x.gio_bat_dau, x.gio_ket_thuc, x.nhan_ca "
                + "ORDER BY x.so_ca";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, san);
            p.setDate(2, Date.valueOf(d));
            List<Map<String, Object>> list = rows(p.executeQuery());
            for (Map<String, Object> row : list) {
                int sc = ((Number) row.get("status_code")).intValue();
                // Nếu ca chưa có bản ghi trong LichSan (status_code=0) nhưng giờ bắt đầu
                // đã qua thời điểm hiện tại trong ngày hôm nay → coi như "HẾT"
                if (sc == 0 && d.equals(LocalDate.now())) {
                    String startTime = String.valueOf(row.get("gio_bat_dau"));
                    try {
                        LocalTime start = LocalTime.parse(startTime);
                        if (!start.isAfter(LocalTime.now())) {
                            sc = 3;
                            row.put("status_code", sc);
                        }
                    } catch (Exception ignored) { }
                }
                // Map lại trạng thái chuỗi dựa trên mã trạng thái cao nhất tìm thấy
                row.put("trang_thai", sc == 2 ? BOOKED : sc == 4 ? EVENT : sc == 1 ? HOLD : sc == 3 ? EXPIRED : FREE);
            }
            return list;
        }
    }

    /**
     * Tạo một đơn đặt sân mới.
     */
    public String createBooking(String[] fields, LocalDate day, int[] cas, String name, String phone, String mode, Map<String, Integer> services) throws Exception {
        validateBookingData(fields, cas);
        checkSlotsAvailability(fields, day, cas);

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String kh = customer(c, name, phone);
                String hd = next(c, "HoaDon", "ma_hd", "HD", 6);
                List<String> lichIds = new ArrayList<>();
                List<Double> slotPrices = new ArrayList<>();

                double baseTotal = processBookingSlots(c, fields, day, cas, mode, null, lichIds, slotPrices);
                double serviceTotal = calculateServiceTotal(c, services);
                double total = baseTotal + serviceTotal;
                double deposit = "direct".equals(mode) ? total : (total * 0.3);
                String status = "direct".equals(mode) ? PAID : UNPAID;

                insertHoaDon(c, hd, kh, total, deposit, status);
                insertCthdForSlots(c, hd, lichIds, slotPrices);
                insertCthdForServices(c, hd, lichIds.get(0), services);

                c.commit();
                return hd;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void validateBookingData(String[] fields, int[] cas) {
        if (fields == null || cas == null || fields.length != cas.length || fields.length == 0)
            throw new IllegalArgumentException("Dữ liệu đặt sân không hợp lệ");
    }

    private void checkSlotsAvailability(String[] fields, LocalDate day, int[] cas) throws SQLException {
        for (int i = 0; i < fields.length; i++) {
            if (!isSlotFree(fields[i], day, cas[i]))
                throw new IllegalStateException("Sân " + fields[i] + " ca " + cas[i] + " đã được đặt hoặc đang giữ chỗ");
        }
    }

    private double processBookingSlots(Connection c, String[] fields, LocalDate day, int[] cas, String mode, String status, List<String> lichIds, List<Double> slotPrices) throws SQLException {
        double baseTotal = 0;
        for (int i = 0; i < fields.length; i++) {
            String san = fields[i];
            int ca = cas[i];
            String lich = next(c, "LichSan", "ma_lich", "L", 3);
            lichIds.add(lich);

            insertLichSanRecord(c, lich, san, ca, day, mode, status);

            double price = getFieldPrice(c, san);
            baseTotal += price;
            slotPrices.add(price);
        }
        return baseTotal;
    }

    private void insertLichSanRecord(Connection c, String lich, String san, int ca, LocalDate day, String mode, String status) throws SQLException {
        String[] sl = Slots.get(ca);
        LocalTime start = LocalTime.parse(sl[2]);
        LocalTime end = LocalTime.parse(sl[3]);
        LocalDateTime startDt = day.atTime(start);
        LocalDateTime endDt = day.atTime(end);
        if (!end.isAfter(start)) endDt = endDt.plusDays(1);

        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO LichSan(ma_lich,ma_san,ca,gio_bat_dau,gio_ket_thuc,trang_thai,ngay_dat,buoi,so_ca) VALUES(?,?,?,?,?,?,?,?,?)")) {
            p.setString(1, lich);
            p.setString(2, san);
            p.setString(3, sl[2] + " - " + sl[3]);
            p.setTimestamp(4, Timestamp.valueOf(startDt));
            p.setTimestamp(5, Timestamp.valueOf(endDt));
            String st = "direct".equals(mode) || PAID.equals(status) ? BOOKED : HOLD;
            p.setString(6, st);
            p.setDate(7, Date.valueOf(day));
            p.setString(8, sl[0]);
            p.setInt(9, ca);
            p.executeUpdate();
        }
    }

    private double calculateServiceTotal(Connection c, Map<String, Integer> services) throws SQLException {
        double serviceTotal = 0;
        if (services != null) {
            for (Map.Entry<String, Integer> entry : services.entrySet()) {
                serviceTotal += getServicePrice(c, entry.getKey()) * entry.getValue();
            }
        }
        return serviceTotal;
    }

    private void insertHoaDon(Connection c, String hd, String kh, double total, double deposit, String status) throws SQLException {
        double remaining = Math.max(0, total - deposit);
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO HoaDon(ma_hd,ma_kh,ma_nv,ngay_lap,tong_tien,tien_coc,so_tien_con_lai,han_thanh_toan,trang_thai) "
                        + "SELECT ?,?,(SELECT TOP 1 ma_nv FROM NhanVien),SYSDATETIME(),?,?,?,DATEADD(MINUTE,60,SYSDATETIME()),?")) {
            p.setString(1, hd);
            p.setString(2, kh);
            p.setDouble(3, total);
            p.setDouble(4, deposit);
            p.setDouble(5, remaining);
            p.setString(6, status);
            p.executeUpdate();
        }
    }

    private void insertCthdForSlots(Connection c, String hd, List<String> lichIds, List<Double> slotPrices) throws SQLException {
        for (int i = 0; i < lichIds.size(); i++) {
            double price = slotPrices.get(i);
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,NULL,1,?,?)")) {
                p.setString(1, hd);
                p.setString(2, lichIds.get(i));
                p.setDouble(3, price);
                p.setDouble(4, price);
                p.executeUpdate();
            }
        }
    }

    private void insertCthdForServices(Connection c, String hd, String firstLich, Map<String, Integer> services) throws SQLException {
        if (services != null && !services.isEmpty()) {
            for (Map.Entry<String, Integer> entry : services.entrySet()) {
                String svId = entry.getKey();
                int qty = entry.getValue();
                double price = getServicePrice(c, svId);
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)")) {
                    p.setString(1, hd);
                    p.setString(2, firstLich);
                    p.setString(3, svId);
                    p.setInt(4, qty);
                    p.setDouble(5, price);
                    p.setDouble(6, price * qty);
                    p.executeUpdate();
                }
            }
        }
    }

    /**
     * Lấy giá thuê của một sân.
     */
    private double getFieldPrice(Connection c, String fieldId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT gia_theo_gio FROM San WHERE ma_san=?")) {
            p.setString(1, fieldId);
            ResultSet r = p.executeQuery();
            if (r.next()) return r.getDouble(1) * 2;
        }
        return 600000;
    }

    /**
     * Lấy đơn giá của một dịch vụ.
     */
    private double getServicePrice(Connection c, String serviceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT don_gia FROM DichVu WHERE ma_dv=?")) {
            p.setString(1, serviceId);
            ResultSet r = p.executeQuery();
            if (r.next()) return r.getDouble(1);
        }
        return 0;
    }

    /**
     * Kiểm tra xem một ca sân tại ngày cụ thể có trống hay không.
     * Đảm bảo không có bất kỳ bản ghi nào đang ở trạng thái 'ĐANG GIỮ CHỖ', 'ĐÃ ĐẶT', 'SỰ KIỆN' hoặc 'HẾT'.
     * Đồng thời chặn đặt các ca đã qua giờ bắt đầu trong ngày hôm nay.
     */
    private boolean isSlotFree(String san, LocalDate day, int ca) throws SQLException {
        // Chặn đặt ca đã qua giờ bắt đầu trong ngày hôm nay
        if (day.equals(LocalDate.now())) {
            try {
                String[] sl = Slots.get(ca);
                LocalTime start = LocalTime.parse(sl[2]);
                if (!start.isAfter(LocalTime.now())) return false;
            } catch (Exception ignored) { }
        }
        String sql = "SELECT COUNT(*) FROM LichSan WHERE ma_san=? AND ngay_dat=? AND so_ca=? AND trang_thai NOT IN (N'" + FREE + "', N'" + CANCEL + "')";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, san);
            p.setDate(2, Date.valueOf(day));
            p.setInt(3, ca);
            ResultSet r = p.executeQuery();
            return r.next() && r.getInt(1) == 0;
        }
    }

    /**
     * Lấy mã khách hàng dựa trên số điện thoại, hoặc tạo mới nếu chưa tồn tại.
     */
    private String customer(Connection c, String name, String phone) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT ma_kh FROM KhachHang WHERE sdt=?")) {
            p.setString(1, phone);
            ResultSet r = p.executeQuery();
            if (r.next()) return r.getString(1);
        }
        String id = next(c, "KhachHang", "ma_kh", "KH", 3);
        try (PreparedStatement p = c.prepareStatement("INSERT INTO KhachHang(ma_kh,ho_ten,sdt) VALUES(?,?,?)")) {
            p.setString(1, id);
            p.setString(2, name);
            p.setString(3, phone);
            p.executeUpdate();
        }
        return id;
    }

    /**
     * Sinh mã tự tăng tiếp theo cho một bảng.
     */
    private String next(Connection c, String table, String col, String prefix, int len) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(" + col + "," + (prefix.length() + 1) + ",20))),0)+1 FROM " + table)) {
            r.next();
            return prefix + String.format("%0" + len + "d", r.getInt(1));
        }
    }

    /**
     * Lấy chi tiết thông tin hóa đơn bao gồm thông tin khách hàng, các ca sân và dịch vụ.
     * @param id Mã hóa đơn
     * @return Map chứa toàn bộ thông tin chi tiết hóa đơn
     */
    public Map<String, Object> invoice(String id) throws SQLException {
        String sql = "SELECT h.*,k.ho_ten,k.sdt," +
                "v.ma_code,v.ten_voucher,v.loai_giam,v.gia_tri,v.don_toi_thieu,v.loai AS voucher_loai " +
                "FROM HoaDon h " +
                "JOIN KhachHang k ON k.ma_kh=h.ma_kh " +
                "LEFT JOIN Voucher v ON v.ma_voucher=h.ma_voucher " +
                "WHERE h.ma_hd=? AND h.trang_thai <> N'ĐÃ XÓA'";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            List<Map<String, Object>> list = rows(p.executeQuery());
            if (!list.isEmpty()) {
                Map<String, Object> inv = list.get(0);
                
                // Tính toán tiền giảm giá để hiển thị
                double rawTotal = 0;
                try (PreparedStatement pSum = c.prepareStatement("SELECT SUM(thanh_tien) FROM CTHD WHERE ma_hd=?")) {
                    pSum.setString(1, id);
                    ResultSet rsSum = pSum.executeQuery();
                    if (rsSum.next()) rawTotal = rsSum.getDouble(1);
                }
                inv.put("raw_total", rawTotal);
                Object tg = inv.get("tien_giam");
                double discount = tg != null ? ((Number) tg).doubleValue()
                        : rawTotal - ((Number) inv.get("tong_tien")).doubleValue();
                inv.put("discount_amount", discount);
                inv.put("tien_giam", discount);
                
                // Lấy danh sách các ca sân trong chi tiết hóa đơn
                String sqlSlots = "SELECT s.ma_san, s.ten_san,s.loai_san,l.ngay_dat,l.buoi,l.ca,l.so_ca, "
                        + "CONVERT(VARCHAR(5),l.gio_bat_dau,108) gio_bat_dau, "
                        + "CONVERT(VARCHAR(5),l.gio_ket_thuc,108) gio_ket_thuc, d.thanh_tien "
                        + "FROM CTHD d JOIN LichSan l ON l.ma_lich=d.ma_lich "
                        + "JOIN San s ON s.ma_san=l.ma_san WHERE d.ma_hd=? AND d.ma_dv IS NULL";
                try (PreparedStatement ps = c.prepareStatement(sqlSlots)) {
                    ps.setString(1, id);
                    inv.put("slots", rows(ps.executeQuery()));
                }

                // Lấy danh sách các dịch vụ kèm theo
                List<Map<String, Object>> svcs = new ArrayList<>();
                String sql2 = "SELECT c.ma_dv, dv.ten_dv, c.so_luong, c.don_gia, c.thanh_tien "
                            + "FROM CTHD c LEFT JOIN DichVu dv ON dv.ma_dv=c.ma_dv "
                            + "WHERE c.ma_hd=? AND c.ma_dv IS NOT NULL";
                try (PreparedStatement p2 = c.prepareStatement(sql2)) {
                    p2.setString(1, id);
                    ResultSet rs2 = p2.executeQuery();
                    while (rs2.next()) {
                        Map<String, Object> sv = new LinkedHashMap<>();
                        sv.put("ma_dv", rs2.getString("ma_dv"));
                        sv.put("ten_dv", rs2.getString("ten_dv"));
                        sv.put("so_luong", rs2.getInt("so_luong"));
                        sv.put("don_gia", rs2.getDouble("don_gia"));
                        sv.put("thanh_tien", rs2.getDouble("thanh_tien"));
                        svcs.add(sv);
                    }
                }
                inv.put("services", svcs);
                return inv;
            }
            return null;
        }
    }

    /**
     * Xử lý thanh toán tiền cọc cho hóa đơn.
     * Chuyển trạng thái hóa đơn sang "MỚI THANH TOÁN CỌC" và cập nhật lịch sân sang "ĐÃ ĐẶT".
     */
    public boolean pay(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE HoaDon SET trang_thai=?, so_tien_con_lai=tong_tien-tien_coc WHERE ma_hd=? AND trang_thai=? AND han_thanh_toan>SYSDATETIME()")) {
                    p.setString(1, DEPOSITED);
                    p.setString(2, id);
                    p.setString(3, UNPAID);
                    if (p.executeUpdate() != 1) {
                        c.rollback();
                        return false;
                    }
                }
                
                // Giảm số lượng voucher khi sử dụng thành công (từ UNPAID sang DEPOSITED)
                useVoucher(c, id);

                // Cập nhật trạng thái các ca sân liên quan
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE l SET trang_thai=? FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich WHERE d.ma_hd=?")) {
                    p.setString(1, BOOKED);
                    p.setString(2, id);
                    p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Xử lý thanh toán phần tiền còn lại của hóa đơn.
     */
    public boolean payRemaining(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String oldStatus = "";
                try (PreparedStatement p = c.prepareStatement("SELECT trang_thai FROM HoaDon WHERE ma_hd=?")) {
                    p.setString(1, id);
                    ResultSet rs = p.executeQuery();
                    if (rs.next()) oldStatus = rs.getString(1);
                }

                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE HoaDon SET trang_thai=?, so_tien_con_lai=0 WHERE ma_hd=? AND (trang_thai=? OR trang_thai=? OR trang_thai=?)")) {
                    p.setString(1, PAID);
                    p.setString(2, id);
                    p.setString(3, UNPAID);
                    p.setString(4, DEPOSITED);
                    p.setString(5, BOOKED);
                    int n = p.executeUpdate();
                    if (n == 0) { c.rollback(); return false; }
                }

                // Nếu từ UNPAID lên thẳng PAID, giảm số lượng voucher
                if (UNPAID.equals(oldStatus)) {
                    useVoucher(c, id);
                }

                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE l SET trang_thai=? FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich WHERE d.ma_hd=?")) {
                    p.setString(1, BOOKED);
                    p.setString(2, id);
                    p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void useVoucher(Connection c, String hdId) throws SQLException {
        // Ưu tiên schema mới (so_luong / ma_voucher); fallback schema cũ nếu còn
        try (PreparedStatement p = c.prepareStatement(
                "UPDATE Voucher SET so_luong = so_luong - 1 " +
                "WHERE ma_voucher = (SELECT ma_voucher FROM HoaDon WHERE ma_hd=?) AND so_luong > 0")) {
            p.setString(1, hdId);
            if (p.executeUpdate() > 0) return;
        } catch (SQLException ignored) { }
        try (PreparedStatement p = c.prepareStatement(
                "UPDATE Voucher SET soLuong = soLuong - 1 " +
                "WHERE idVoucher = (SELECT ma_voucher FROM HoaDon WHERE ma_hd=?)")) {
            p.setString(1, hdId);
            p.executeUpdate();
        } catch (SQLException ignored) { }
    }

    /**
     * Hủy bỏ hóa đơn và giải phóng các ca sân đã chọn.
     */
    public boolean cancelInvoice(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("UPDATE HoaDon SET trang_thai=? WHERE ma_hd=?")) {
                p.setString(1, CANCEL);
                p.setString(2, id);
                p.executeUpdate();
            }
            // Giải phóng các ca sân về trạng thái "CÒN TRỐNG"
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE l SET trang_thai=? FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich WHERE d.ma_hd=?")) {
                p.setString(1, FREE);
                p.setString(2, id);
                p.executeUpdate();
            }
            c.commit();
            return true;
        }
    }

    /**
     * Lấy dữ liệu hóa đơn để phục vụ chức năng chỉnh sửa.
     */
    public Map<String, Object> invoiceForEdit(String id) throws SQLException {
        String sql = "SELECT h.ma_hd, h.tong_tien, h.tien_coc, h.so_tien_con_lai, h.trang_thai, " +
                "k.ma_kh, k.ho_ten, k.sdt, s.ma_san, l.ma_lich, l.so_ca, l.ngay_dat " +
                "FROM HoaDon h JOIN KhachHang k ON k.ma_kh=h.ma_kh " +
                "JOIN CTHD d ON d.ma_hd=h.ma_hd " +
                "JOIN LichSan l ON l.ma_lich=d.ma_lich " +
                "JOIN San s ON s.ma_san=l.ma_san WHERE h.ma_hd=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, id);
            ResultSet r = p.executeQuery();
            if (!r.next()) return null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ma_hd", r.getString("ma_hd"));
            m.put("ho_ten", r.getString("ho_ten"));
            m.put("sdt", r.getString("sdt"));
            m.put("ma_san", r.getString("ma_san"));
            m.put("so_ca", r.getInt("so_ca"));
            m.put("ngay_dat", r.getDate("ngay_dat"));

            // Lấy danh sách số lượng dịch vụ đã chọn trong hóa đơn
            Map<String, Integer> qty = new LinkedHashMap<>();
            try (PreparedStatement p2 = c.prepareStatement("SELECT ma_dv, so_luong FROM CTHD WHERE ma_hd=? AND ma_dv IS NOT NULL")) {
                p2.setString(1, id);
                ResultSet r2 = p2.executeQuery();
                while (r2.next()) qty.put(r2.getString(1), r2.getInt(2));
            }
            m.put("serviceQty", qty);
            m.put("serviceIds", new ArrayList<>(qty.keySet()));
            return m;
        }
    }

    /**
     * Cập nhật thông tin một hóa đơn đã tồn tại.
     */
    public void updateInvoice(String invoiceId, String[] fields, LocalDate date, int[] cas, String name, String phone, Map<String, Integer> services) throws Exception {
        validateBookingData(fields, cas);

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String, Object> currentInfo = getInvoiceSummaryForUpdate(c, invoiceId);
                String maKh = (String) currentInfo.get("ma_kh");
                String status = (String) currentInfo.get("trang_thai");
                double tienCoc = (double) currentInfo.get("tien_coc");

                updateCustomerInfo(c, maKh, name, phone);
                clearOldInvoiceDetails(c, invoiceId);

                List<String> newLichIds = new ArrayList<>();
                List<Double> slotPrices = new ArrayList<>();
                double baseTotal = processBookingSlots(c, fields, date, cas, null, status, newLichIds, slotPrices);

                double serviceTotal = calculateServiceTotal(c, services);
                insertCthdForSlots(c, invoiceId, newLichIds, slotPrices);
                insertCthdForServices(c, invoiceId, newLichIds.get(0), services);

                double total = baseTotal + serviceTotal;
                updateHoaDonTotals(c, invoiceId, total, status, tienCoc);

                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private Map<String, Object> getInvoiceSummaryForUpdate(Connection c, String invoiceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT ma_kh, trang_thai, tien_coc FROM HoaDon WHERE ma_hd=?")) {
            p.setString(1, invoiceId);
            ResultSet r = p.executeQuery();
            if (!r.next()) throw new SQLException("Không tìm thấy hóa đơn");
            Map<String, Object> info = new HashMap<>();
            info.put("ma_kh", r.getString(1));
            info.put("trang_thai", r.getString(2));
            info.put("tien_coc", r.getDouble(3));
            return info;
        }
    }

    private void updateCustomerInfo(Connection c, String maKh, String name, String phone) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("UPDATE KhachHang SET ho_ten=?, sdt=? WHERE ma_kh=?")) {
            p.setString(1, name);
            p.setString(2, phone);
            p.setString(3, maKh);
            p.executeUpdate();
        }
    }

    private void clearOldInvoiceDetails(Connection c, String invoiceId) throws SQLException {
        List<String> oldLichIds = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT DISTINCT ma_lich FROM CTHD WHERE ma_hd=? AND ma_lich IS NOT NULL")) {
            p.setString(1, invoiceId);
            ResultSet r = p.executeQuery();
            while (r.next()) oldLichIds.add(r.getString(1));
        }

        try (PreparedStatement p = c.prepareStatement("DELETE FROM CTHD WHERE ma_hd=?")) {
            p.setString(1, invoiceId);
            p.executeUpdate();
        }

        for (String lid : oldLichIds) {
            try (PreparedStatement p = c.prepareStatement("DELETE FROM LichSan WHERE ma_lich=?")) {
                p.setString(1, lid);
                p.executeUpdate();
            }
        }
    }

    private void updateHoaDonTotals(Connection c, String invoiceId, double total, String status, double tienCoc) throws SQLException {
        double remain = PAID.equals(status) ? 0 : Math.max(0, total - tienCoc);
        try (PreparedStatement p = c.prepareStatement("UPDATE HoaDon SET tong_tien=?, so_tien_con_lai=? WHERE ma_hd=?")) {
            p.setDouble(1, total);
            p.setDouble(2, remain);
            p.setString(3, invoiceId);
            p.executeUpdate();
        }
    }

    /**
     * Cập nhật trạng thái hóa đơn sang "ĐANG XỬ LÝ".
     */
    public boolean setProcessing(String id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE HoaDon SET trang_thai=? WHERE ma_hd=? AND trang_thai=?")) {
                p.setString(1, PROCESSING);
                p.setString(2, id);
                p.setString(3, UNPAID);
                return p.executeUpdate() > 0;
            }
        }
    }

    /**
     * Lấy danh sách các lịch đặt sân của một sân trong ngày để hiển thị bảng lịch.
     */
    public List<Map<String, Object>> getBookingsByField(String fieldId, LocalDate date) throws SQLException {
        String sql = "SELECT l.ma_lich, l.ma_san, s.ten_san, l.trang_thai, l.ngay_dat, l.so_ca, "
                + "l.buoi, l.ca, "
                + "k.ho_ten ten_khach, k.sdt, "
                + "CASE WHEN l.trang_thai=N'"+HOLD+"' THEN 1 WHEN l.trang_thai=N'"+BOOKED+"' THEN 2 "
                + "WHEN l.trang_thai=N'"+EXPIRED+"' THEN 3 WHEN l.trang_thai=N'"+EVENT+"' THEN 4 ELSE 0 END status_code "
                + "FROM LichSan l JOIN San s ON s.ma_san=l.ma_san "
                + "JOIN CTHD c ON c.ma_lich=l.ma_lich "
                + "JOIN HoaDon h ON h.ma_hd=c.ma_hd "
                + "JOIN KhachHang k ON k.ma_kh=h.ma_kh "
                + "WHERE l.ma_san=? AND l.ngay_dat=? "
                + "ORDER BY l.so_ca";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, fieldId);
            p.setDate(2, Date.valueOf(date));
            return rows(p.executeQuery());
        }
    }

    /**
     * Lấy danh sách các hóa đơn liên quan đến một sân cụ thể.
     */
    public List<Map<String, Object>> getInvoicesByField(String fieldId) throws SQLException {
        String sql = "SELECT h.ma_hd, h.ma_kh, h.tong_tien, h.ngay_lap ngay_xuat, h.da_thu, "
                + "k.ho_ten ten_khach "
                + "FROM HoaDon h JOIN CTHD d ON d.ma_hd=h.ma_hd "
                + "JOIN LichSan l ON l.ma_lich=d.ma_lich "
                + "JOIN KhachHang k ON k.ma_kh=h.ma_kh "
                + "WHERE l.ma_san=? "
                + "ORDER BY h.ngay_lap DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, fieldId);
            return rows(p.executeQuery());
        }
    }

    /**
     * Thống kê số lượng ca đã đặt, đang chờ, hết và còn trống trong ngày hôm nay của tất cả các sân.
     */
    public Map<String, Integer> fieldStats() throws SQLException {
        List<Map<String, Object>> fields = fields();
        LocalDate today = LocalDate.now();
        int totalSlots = 0;
        int bookedCount = 0;
        int pendingCount = 0;
        int expiredCount = 0;
        for (Map<String, Object> f : fields) {
            List<Map<String, Object>> slotList = slots(String.valueOf(f.get("ma_san")), today);
            for (Map<String, Object> s : slotList) {
                totalSlots++;
                int code = ((Number) s.get("status_code")).intValue();
                if (code == 1) pendingCount++;
                else if (code == 2) bookedCount++;
                else if (code == 3) expiredCount++;
            }
        }
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("booked", bookedCount);
        stats.put("pending", pendingCount);
        stats.put("expired", expiredCount);
        stats.put("free", totalSlots - bookedCount - pendingCount - expiredCount);
        return stats;
    }

    /**
     * Tự động hủy các hóa đơn quá hạn thanh toán và giải phóng các ca sân tương ứng.
     */
    public void expire() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // Hủy hóa đơn chưa thanh toán quá hạn
            s.execute("UPDATE h SET trang_thai=N'" + CANCEL + "' FROM HoaDon h WHERE h.trang_thai=N'" + UNPAID + "' AND h.han_thanh_toan<SYSDATETIME()");
            // Giải phóng các lịch sân thuộc các hóa đơn đã bị hủy
            s.execute("UPDATE l SET trang_thai=N'" + FREE + "' FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich JOIN HoaDon h ON h.ma_hd=d.ma_hd WHERE h.trang_thai=N'" + CANCEL + "'");
        }
    }

    /**
     * Tự động đặt trạng thái "HẾT" cho các ca trống có giờ bắt đầu đã qua thời điểm hiện tại.
     * Chỉ xử lý các ca có trạng thái "CÒN TRỐNG" hoặc "ĐÃ HỦY" và giờ bắt đầu <= thời điểm hiện tại.
     */
    public void expireSlots() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // Đặt trạng thái "HẾT" cho các ca trống có giờ bắt đầu đã qua thời điểm hiện tại
            s.execute("UPDATE LichSan SET trang_thai=N'" + EXPIRED + "' "
                    + "WHERE trang_thai IN (N'" + FREE + "', N'" + CANCEL + "') "
                    + "AND gio_bat_dau <= SYSDATETIME()");
        }
    }

    /**
     * Xóa các hóa đơn trống (không có bất kỳ chi tiết nào trong CTHD).
     */
    public void deleteEmptyInvoices() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM HoaDon WHERE ma_hd NOT IN (SELECT DISTINCT ma_hd FROM CTHD)");
        }
    }

    /**
     * Đảm bảo luôn có một hóa đơn hoạt động để lưu trữ các lựa chọn của người dùng.
     * Nếu đã có ID hóa đơn, cập nhật thông tin khách hàng. Nếu chưa, tạo hóa đơn mới.
     */
    public String ensureInvoice(String hdId, String name, String phone) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (hdId != null && !hdId.isBlank()) {
                    try (PreparedStatement p = c.prepareStatement("SELECT ma_hd FROM HoaDon WHERE ma_hd=? AND trang_thai <> N'ĐÃ XÓA'")) {
                        p.setString(1, hdId);
                        if (p.executeQuery().next()) {
                            // Nếu có thông tin khách hàng mới, cập nhật vào hóa đơn hiện tại
                            if (name != null && !name.isBlank() && phone != null && !phone.isBlank()) {
                                String maKh = customer(c, name, phone);
                                try (PreparedStatement p2 = c.prepareStatement("UPDATE HoaDon SET ma_kh=? WHERE ma_hd=?")) {
                                    p2.setString(1, maKh);
                                    p2.setString(2, hdId);
                                    p2.executeUpdate();
                                }
                            }
                            c.commit();
                            return hdId;
                        }
                    }
                }
                // Tạo mới khách hàng "vãng lai" nếu chưa có thông tin
                String kh = customer(c, (name == null || name.isBlank()) ? "Khách" : name,
                        (phone == null || phone.isBlank()) ? "0000000000" : phone);
                String hd = next(c, "HoaDon", "ma_hd", "HD", 6);
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO HoaDon(ma_hd,ma_kh,ma_nv,ngay_lap,tong_tien,tien_coc,so_tien_con_lai,han_thanh_toan,trang_thai) "
                                + "SELECT ?,?,(SELECT TOP 1 ma_nv FROM NhanVien),SYSDATETIME(),0,0,0,DATEADD(MINUTE,60,SYSDATETIME()),?")) {
                    p.setString(1, hd);
                    p.setString(2, kh);
                    p.setString(3, UNPAID);
                    p.executeUpdate();
                }
                c.commit();
                return hd;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Bật/Tắt lựa chọn một ca sân trong hóa đơn nháp.
     */
    public void toggleSlot(String hdId, String fieldId, LocalDate date, int ca) throws Exception {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String lichId = findLichId(c, hdId, fieldId, date, ca);

                if (lichId != null) {
                    removeSlot(c, hdId, lichId);
                } else {
                    addSlot(c, hdId, fieldId, date, ca);
                }
                c.commit();
                updateTotals(hdId);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private String findLichId(Connection c, String hdId, String fieldId, LocalDate date, int ca) throws SQLException {
        String sql = "SELECT l.ma_lich FROM LichSan l JOIN CTHD d ON d.ma_lich=l.ma_lich " +
                "WHERE d.ma_hd=? AND l.ma_san=? AND l.ngay_dat=? AND l.so_ca=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, hdId);
            p.setString(2, fieldId);
            p.setDate(3, Date.valueOf(date));
            p.setInt(4, ca);
            ResultSet r = p.executeQuery();
            return r.next() ? r.getString(1) : null;
        }
    }

    private void removeSlot(Connection c, String hdId, String lichId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM CTHD WHERE ma_hd=? AND ma_lich=?")) {
            p.setString(1, hdId);
            p.setString(2, lichId);
            p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement("DELETE FROM LichSan WHERE ma_lich=?")) {
            p.setString(1, lichId);
            p.executeUpdate();
        }
    }

    private void addSlot(Connection c, String hdId, String fieldId, LocalDate date, int ca) throws Exception {
        if (!isSlotFree(fieldId, date, ca)) throw new Exception("Sân đã được đặt hoặc đang chờ");

        String newLich = next(c, "LichSan", "ma_lich", "L", 3);
        insertLichSanRecord(c, newLich, fieldId, ca, date, null, HOLD);

        double pr = getFieldPrice(c, fieldId);
        try (PreparedStatement p = c.prepareStatement("INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,NULL,1,?,?)")) {
            p.setString(1, hdId);
            p.setString(2, newLich);
            p.setDouble(3, pr);
            p.setDouble(4, pr);
            p.executeUpdate();
        }
    }

    /**
     * Cập nhật số lượng hoặc thêm mới dịch vụ vào hóa đơn.
     * Nếu dịch vụ đã tồn tại, cộng thêm số lượng mới vào số lượng hiện có.
     * Nếu qty <= 0, xóa dịch vụ khỏi hóa đơn.
     */
    public void updateService(String hdId, String serviceId, int qty) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String lichId = findAnyLichIdForInvoice(c, hdId);
                if (lichId == null) throw new SQLException("Vui lòng chọn sân trước khi thêm dịch vụ");

                int existingQty = getExistingServiceQty(c, hdId, serviceId);
                if (existingQty > 0) {
                    if (qty <= 0) {
                        // Xóa dịch vụ nếu số lượng mới <= 0
                        removeServiceFromInvoice(c, hdId, serviceId);
                    } else {
                        // Cộng thêm số lượng mới vào số lượng hiện có
                        int newQty = existingQty + qty;
                        double price = getServicePrice(c, serviceId);
                        try (PreparedStatement p = c.prepareStatement(
                                "UPDATE CTHD SET so_luong=?, thanh_tien=? WHERE ma_hd=? AND ma_dv=?")) {
                            p.setInt(1, newQty);
                            p.setDouble(2, price * newQty);
                            p.setString(3, hdId);
                            p.setString(4, serviceId);
                            p.executeUpdate();
                        }
                    }
                } else if (qty > 0) {
                    // Thêm mới dịch vụ
                    addServiceToInvoice(c, hdId, lichId, serviceId, qty);
                }
                c.commit();
                updateTotals(hdId);
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Lấy số lượng dịch vụ hiện có trong hóa đơn.
     */
    private int getExistingServiceQty(Connection c, String hdId, String serviceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT so_luong FROM CTHD WHERE ma_hd=? AND ma_dv=?")) {
            p.setString(1, hdId);
            p.setString(2, serviceId);
            ResultSet r = p.executeQuery();
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private String findAnyLichIdForInvoice(Connection c, String hdId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT TOP 1 ma_lich FROM CTHD WHERE ma_hd=? AND ma_lich IS NOT NULL")) {
            p.setString(1, hdId);
            ResultSet r = p.executeQuery();
            return r.next() ? r.getString(1) : null;
        }
    }

    private void removeServiceFromInvoice(Connection c, String hdId, String serviceId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("DELETE FROM CTHD WHERE ma_hd=? AND ma_dv=?")) {
            p.setString(1, hdId);
            p.setString(2, serviceId);
            p.executeUpdate();
        }
    }

    private void addServiceToInvoice(Connection c, String hdId, String lichId, String serviceId, int qty) throws SQLException {
        double price = getServicePrice(c, serviceId);
        try (PreparedStatement p = c.prepareStatement("INSERT INTO CTHD(ma_hd,ma_lich,ma_dv,so_luong,don_gia,thanh_tien) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, hdId);
            p.setString(2, lichId);
            p.setString(3, serviceId);
            p.setInt(4, qty);
            p.setDouble(5, price);
            p.setDouble(6, price * qty);
            p.executeUpdate();
        }
    }

    /**
     * Lấy danh sách Voucher khả dụng cho Booking (Hóa đơn).
     * Chỉ trả về voucher loai = 1 (không lấy voucher Sân/Dịch vụ).
     */
    public List<Map<String, Object>> getAvailableVouchers(String hdId) throws SQLException {
        double totalBeforeVoucher = 0;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT ISNULL(SUM(thanh_tien),0) FROM CTHD WHERE ma_hd=?")) {
            p.setString(1, hdId);
            ResultSet r = p.executeQuery();
            if (r.next()) totalBeforeVoucher = r.getDouble(1);
        }
        VoucherRepository voucherRepo = new VoucherRepository();
        return voucherRepo.toMapList(voucherRepo.getAvailableHoaDonVouchers(totalBeforeVoucher));
    }

    /**
     * Áp dụng Voucher cho hóa đơn (chỉ chấp nhận loai = 1).
     */
    public void applyVoucher(String hdId, String voucherId) throws Exception {
        if (voucherId == null || voucherId.isBlank()) {
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement p = c.prepareStatement(
                         "UPDATE HoaDon SET ma_voucher=NULL, tien_giam=0 WHERE ma_hd=?")) {
                p.setString(1, hdId);
                p.executeUpdate();
            }
            updateTotals(hdId);
            return;
        }
        VoucherRepository voucherRepo = new VoucherRepository();
        com.footballbooking.model.Entities.Voucher v = voucherRepo.getById(voucherId);
        if (v == null) v = voucherRepo.getByCode(voucherId);
        if (v == null) throw new IllegalArgumentException("Voucher không tồn tại");
        if (v.getLoai() != com.footballbooking.model.Entities.VoucherLoai.HOA_DON) {
            throw new IllegalArgumentException("Voucher này không áp dụng cho Hóa đơn (Booking)");
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE HoaDon SET ma_voucher=? WHERE ma_hd=?")) {
            p.setString(1, v.getMaVoucher());
            p.setString(2, hdId);
            p.executeUpdate();
        }
        updateTotals(hdId);
    }

    private boolean isVoucherValid(Connection c, String voucherId, String hdId, double rawTotal) throws SQLException {
        VoucherRepository voucherRepo = new VoucherRepository();
        com.footballbooking.model.Entities.Voucher v = voucherRepo.getById(voucherId);
        if (v == null) return false;

        // Chỉ voucher Hóa đơn (loai = 1) mới áp dụng trên Booking
        if (v.getLoai() != com.footballbooking.model.Entities.VoucherLoai.HOA_DON) return false;

        LocalDate today = LocalDate.now();
        if (v.getNgayBatDau() != null && today.isBefore(v.getNgayBatDau())) return false;
        if (v.getNgayKetThuc() != null && today.isAfter(v.getNgayKetThuc())) return false;
        if (v.getSoLuong() <= 0) return false;
        if (rawTotal < v.getDonToiThieu()) return false;

        String st = v.getTrangThai();
        if (st != null && !st.isBlank()) {
            String up = st.trim().toUpperCase();
            if (up.contains("NGỪNG") || up.contains("KHÓA") || "0".equals(up) || "INACTIVE".equals(up) || "FALSE".equals(up)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tính toán lại tổng tiền, tiền cọc, tiền giảm và số tiền còn lại cho hóa đơn.
     */
    private void updateTotals(String hdId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            double rawTotal = 0;
            try (PreparedStatement p = c.prepareStatement("SELECT ISNULL(SUM(thanh_tien),0) FROM CTHD WHERE ma_hd=?")) {
                p.setString(1, hdId);
                ResultSet r = p.executeQuery();
                if (r.next()) rawTotal = r.getDouble(1);
            }

            String voucherId = null;
            try (PreparedStatement p = c.prepareStatement("SELECT ma_voucher FROM HoaDon WHERE ma_hd=?")) {
                p.setString(1, hdId);
                ResultSet r = p.executeQuery();
                if (r.next()) voucherId = r.getString(1);
            }

            double discount = 0;
            if (voucherId != null && !voucherId.isBlank()) {
                if (isVoucherValid(c, voucherId, hdId, rawTotal)) {
                    VoucherRepository voucherRepo = new VoucherRepository();
                    com.footballbooking.model.Entities.Voucher v = voucherRepo.getById(voucherId);
                    if (v != null) {
                        String loaiGiam = v.getLoaiGiam() != null ? v.getLoaiGiam().trim().toUpperCase() : "MONEY";
                        double giaTri = v.getGiaTri();
                        if ("PERCENT".equals(loaiGiam) || "GIAM_THEO_PHAN_TRAM".equals(loaiGiam)) {
                            discount = rawTotal * (giaTri / 100.0);
                        } else {
                            // MONEY / GIAM_TRUC_TIEP
                            discount = giaTri;
                        }
                        if (discount > rawTotal) discount = rawTotal;
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE HoaDon SET ma_voucher=NULL, tien_giam=0 WHERE ma_hd=?")) {
                        p.setString(1, hdId);
                        p.executeUpdate();
                    }
                    voucherId = null;
                }
            }

            double finalTotal = Math.max(0, rawTotal - discount);
            double deposit = finalTotal * 0.3;
            double remain = finalTotal - deposit;

            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE HoaDon SET tong_tien=?, tien_coc=?, so_tien_con_lai=?, tien_giam=? WHERE ma_hd=?")) {
                p.setDouble(1, finalTotal);
                p.setDouble(2, deposit);
                p.setDouble(3, remain);
                p.setDouble(4, discount);
                p.setString(5, hdId);
                p.executeUpdate();
            }
        }
    }

    /**
     * Thực thi một câu lệnh truy vấn SQL trả về danh sách các bản ghi.
     */
    private List<Map<String, Object>> query(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            return rows(s.executeQuery(sql));
        }
    }

    /**
     * Chuyển đổi ResultSet từ SQL sang danh sách các Map (dạng key-value).
     */
    private List<Map<String, Object>> rows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }
}