package com.footballbooking.repository;

import com.footballbooking.config.DBConnection;
import com.footballbooking.model.Entities.Voucher;
import com.footballbooking.model.Entities.VoucherLoai;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO quản lý bảng Voucher.
 * Hỗ trợ CRUD đầy đủ và lọc theo cột loai (1=Hóa đơn, 2=Sân, 3=Dịch vụ).
 */
public class VoucherRepository {

    private static final String COLS =
            "ma_voucher, ma_code, ten_voucher, loai_giam, gia_tri, don_toi_thieu, "
                    + "ngay_bat_dau, ngay_ket_thuc, so_luong, trang_thai, loai";

    static {
        ensureSchema();
    }

    /**
     * Đảm bảo bảng Voucher / cột loai / cột tien_giam tồn tại (an toàn nếu DB đã có).
     */
    private static void ensureSchema() {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("IF OBJECT_ID('Voucher', 'U') IS NULL CREATE TABLE Voucher ("
                    + "ma_voucher VARCHAR(20) PRIMARY KEY, "
                    + "ma_code VARCHAR(50) NOT NULL, "
                    + "ten_voucher NVARCHAR(100) NOT NULL, "
                    + "loai_giam VARCHAR(20) NOT NULL, "
                    + "gia_tri FLOAT NOT NULL DEFAULT 0, "
                    + "don_toi_thieu FLOAT NOT NULL DEFAULT 0, "
                    + "ngay_bat_dau DATE NOT NULL, "
                    + "ngay_ket_thuc DATE NOT NULL, "
                    + "so_luong INT NOT NULL DEFAULT 0, "
                    + "trang_thai NVARCHAR(30) NOT NULL DEFAULT N'HOẠT ĐỘNG', "
                    + "loai INT NOT NULL DEFAULT 1)");

            // Bổ sung cột loai nếu bảng cũ chưa có
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
            s.execute("IF COL_LENGTH('Voucher','ma_voucher') IS NULL AND COL_LENGTH('Voucher','idVoucher') IS NOT NULL "
                    + "EXEC sp_rename 'Voucher.idVoucher', 'ma_voucher', 'COLUMN'");

            s.execute("IF COL_LENGTH('HoaDon','tien_giam') IS NULL ALTER TABLE HoaDon ADD tien_giam DECIMAL(18,2) NOT NULL DEFAULT 0");
            s.execute("IF COL_LENGTH('HoaDon','ma_voucher') IS NULL ALTER TABLE HoaDon ADD ma_voucher VARCHAR(20) NULL");
        } catch (Exception e) {
            System.err.println("VoucherRepository schema: " + e.getMessage());
        }
    }

    /** Lấy tất cả voucher (đầy đủ cột loai). */
    public List<Voucher> getAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM Voucher ORDER BY loai, ma_voucher";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            return mapList(rs);
        }
    }

    /** Lấy voucher theo mã. */
    public Voucher getById(String maVoucher) throws SQLException {
        if (maVoucher == null || maVoucher.isBlank()) return null;
        String sql = "SELECT " + COLS + " FROM Voucher WHERE ma_voucher=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, maVoucher.trim());
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Lấy voucher theo mã code (VD: MORNING10, PROMO50). */
    public Voucher getByCode(String maCode) throws SQLException {
        if (maCode == null || maCode.isBlank()) return null;
        String sql = "SELECT " + COLS + " FROM Voucher WHERE ma_code=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, maCode.trim());
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Lọc voucher theo loại áp dụng (1/2/3...). */
    public List<Voucher> getVoucherByLoai(int loai) throws SQLException {
        String sql = "SELECT " + COLS + " FROM Voucher WHERE loai=? ORDER BY ma_voucher";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, loai);
            try (ResultSet rs = p.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /** Voucher áp dụng cho Hóa đơn (Booking) — loai = 1. */
    public List<Voucher> getVoucherHoaDon() throws SQLException {
        return getVoucherByLoai(VoucherLoai.HOA_DON);
    }

    /** Voucher áp dụng cho Sân — loai = 2. */
    public List<Voucher> getVoucherSan() throws SQLException {
        return getVoucherByLoai(VoucherLoai.SAN);
    }

    /** Voucher áp dụng cho Dịch vụ — loai = 3. */
    public List<Voucher> getVoucherDichVu() throws SQLException {
        return getVoucherByLoai(VoucherLoai.DICH_VU);
    }

    /**
     * Voucher Hóa đơn còn hiệu lực (ngày, số lượng, trạng thái) cho trang Booking.
     */
    public List<Voucher> getAvailableHoaDonVouchers(double donHang) throws SQLException {
        String sql = "SELECT " + COLS + " FROM Voucher "
                + "WHERE loai=? "
                + "AND (trang_thai = 1) "
                + "AND (ngay_bat_dau <= CAST(SYSDATETIME() AS DATE)) "
                + "AND (ngay_ket_thuc >= CAST(SYSDATETIME() AS DATE)) "
                + "AND (so_luong > 0) "
                + "AND (don_toi_thieu <= ?) "
                + "ORDER BY ma_code";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, VoucherLoai.HOA_DON); // 1 = HOA_DON
            p.setDouble(2, donHang);
            try (ResultSet rs = p.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    /** Thêm voucher mới. */
    public int insert(Voucher v) throws SQLException {
        String sql = "INSERT INTO Voucher (" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            bindAll(p, v);
            return p.executeUpdate();
        }
    }

    /** Cập nhật voucher. */
    public int update(Voucher v) throws SQLException {
        String sql = "UPDATE Voucher SET ma_code=?, ten_voucher=?, loai_giam=?, gia_tri=?, don_toi_thieu=?, "
                + "ngay_bat_dau=?, ngay_ket_thuc=?, so_luong=?, trang_thai=?, loai=? "
                + "WHERE ma_voucher=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, v.getMaCode());
            p.setString(2, v.getTenVoucher());
            p.setString(3, v.getLoaiGiam());
            p.setDouble(4, v.getGiaTri());
            p.setDouble(5, v.getDonToiThieu());
            setDate(p, 6, v.getNgayBatDau());
            setDate(p, 7, v.getNgayKetThuc());
            p.setInt(8, v.getSoLuong());
            p.setString(9, v.getTrangThai());
            p.setInt(10, v.getLoai());
            p.setString(11, v.getMaVoucher());
            return p.executeUpdate();
        }
    }

    /** Xóa voucher theo mã. */
    public int delete(String maVoucher) throws SQLException {
        if (maVoucher == null || maVoucher.isBlank()) return 0;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM Voucher WHERE ma_voucher=?")) {
            p.setString(1, maVoucher.trim());
            return p.executeUpdate();
        }
    }

    /** Chuyển entity sang Map (dùng cho JSP / Admin CRUD generic). */
    public Map<String, Object> toMap(Voucher v) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (v == null) return m;
        m.put("ma_voucher", v.getMaVoucher());
        m.put("ma_code", v.getMaCode());
        m.put("ten_voucher", v.getTenVoucher());
        m.put("loai_giam", v.getLoaiGiam());
        m.put("gia_tri", v.getGiaTri());
        m.put("don_toi_thieu", v.getDonToiThieu());
        m.put("ngay_bat_dau", v.getNgayBatDau() != null ? v.getNgayBatDau().toString() : null);
        m.put("ngay_ket_thuc", v.getNgayKetThuc() != null ? v.getNgayKetThuc().toString() : null);
        m.put("so_luong", v.getSoLuong());
        m.put("trang_thai", v.getTrangThai());
        m.put("loai", v.getLoai());
        m.put("ten_loai", v.getTenLoai());
        // Alias tương thích hiển thị cũ trên booking (nếu còn tham chiếu)
        m.put("idVoucher", v.getMaVoucher());
        m.put("mucGiam", v.getGiaTri());
        m.put("loaiGiam", v.getLoaiGiam());
        m.put("maCode", v.getMaCode());
        m.put("tenVoucher", v.getTenVoucher());
        return m;
    }

    public List<Map<String, Object>> toMapList(List<Voucher> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (list != null) {
            for (Voucher v : list) out.add(toMap(v));
        }
        return out;
    }

    private void bindAll(PreparedStatement p, Voucher v) throws SQLException {
        p.setString(1, v.getMaVoucher());
        p.setString(2, v.getMaCode());
        p.setString(3, v.getTenVoucher());
        p.setString(4, v.getLoaiGiam());
        p.setDouble(5, v.getGiaTri());
        p.setDouble(6, v.getDonToiThieu());
        setDate(p, 7, v.getNgayBatDau());
        setDate(p, 8, v.getNgayKetThuc());
        p.setInt(9, v.getSoLuong());
        p.setString(10, v.getTrangThai() != null ? v.getTrangThai() : "HOẠT ĐỘNG");
        p.setInt(11, v.getLoai() > 0 ? v.getLoai() : VoucherLoai.HOA_DON);
    }

    private static void setDate(PreparedStatement p, int idx, LocalDate d) throws SQLException {
        if (d == null) p.setNull(idx, java.sql.Types.DATE);
        else p.setDate(idx, Date.valueOf(d));
    }

    private List<Voucher> mapList(ResultSet rs) throws SQLException {
        List<Voucher> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Voucher mapRow(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setMaVoucher(getStr(rs, "ma_voucher"));
        v.setMaCode(getStr(rs, "ma_code"));
        v.setTenVoucher(getStr(rs, "ten_voucher"));
        v.setLoaiGiam(getStr(rs, "loai_giam"));
        v.setGiaTri(getDbl(rs, "gia_tri"));
        v.setDonToiThieu(getDbl(rs, "don_toi_thieu"));
        Date d1 = rs.getDate("ngay_bat_dau");
        Date d2 = rs.getDate("ngay_ket_thuc");
        v.setNgayBatDau(d1 != null ? d1.toLocalDate() : null);
        v.setNgayKetThuc(d2 != null ? d2.toLocalDate() : null);
        v.setSoLuong(getInt(rs, "so_luong"));
        v.setTrangThai(getStr(rs, "trang_thai"));
        v.setLoai(getInt(rs, "loai"));
        // Fallback schema cũ (nếu cột mới null nhưng còn cột legacy)
        if (v.getMaVoucher() == null) v.setMaVoucher(safeGet(rs, "idVoucher"));
        if (v.getLoaiGiam() == null) {
            String old = safeGet(rs, "loaiGiam");
            if ("GIAM_THEO_PHAN_TRAM".equals(old)) v.setLoaiGiam("PERCENT");
            else if (old != null) v.setLoaiGiam("MONEY");
        }
        if (v.getGiaTri() == 0) v.setGiaTri(safeDbl(rs, "mucGiam"));
        if (v.getDonToiThieu() == 0) v.setDonToiThieu(safeDbl(rs, "mucToiThieu"));
        if (v.getSoLuong() == 0) v.setSoLuong(safeInt(rs, "soLuong"));
        if (v.getNgayBatDau() == null) {
            Date d = safeDate(rs, "ngayBatDau");
            if (d != null) v.setNgayBatDau(d.toLocalDate());
        }
        if (v.getNgayKetThuc() == null) {
            Date d = safeDate(rs, "ngayKetThuc");
            if (d != null) v.setNgayKetThuc(d.toLocalDate());
        }
        if (v.getMaCode() == null || v.getMaCode().isBlank()) v.setMaCode(v.getMaVoucher());
        if (v.getTenVoucher() == null || v.getTenVoucher().isBlank()) v.setTenVoucher(v.getMaCode());
        if (v.getLoai() == 0) v.setLoai(VoucherLoai.HOA_DON);
        if (v.getTrangThai() == null) v.setTrangThai("HOẠT ĐỘNG");
        return v;
    }

    private static String getStr(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private static double getDbl(ResultSet rs, String col) {
        try {
            double d = rs.getDouble(col);
            return rs.wasNull() ? 0 : d;
        } catch (SQLException e) { return 0; }
    }

    private static int getInt(ResultSet rs, String col) {
        try {
            int i = rs.getInt(col);
            return rs.wasNull() ? 0 : i;
        } catch (SQLException e) { return 0; }
    }

    private static String safeGet(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }

    private static double safeDbl(ResultSet rs, String col) {
        try { return rs.getDouble(col); } catch (Exception e) { return 0; }
    }

    private static int safeInt(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (Exception e) { return 0; }
    }

    private static Date safeDate(ResultSet rs, String col) {
        try { return rs.getDate(col); } catch (Exception e) { return null; }
    }
}
