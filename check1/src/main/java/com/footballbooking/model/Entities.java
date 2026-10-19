package com.footballbooking.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Lớp chứa các thực thể (Entities) ánh xạ trực tiếp với các bảng trong cơ sở dữ liệu.
 * Được sử dụng để đóng gói dữ liệu và trao đổi giữa các tầng trong kiến trúc MVC.
 */
public final class Entities {
    private Entities() {}

    /** Thực thể Khách hàng */
    public static class KhachHang {
        public String maKh, hoTen, sdt, email;
    }

    /** Thực thể Nhân viên */
    public static class NhanVien {
        public String maNv, tenNv, sdt, chucVu, matKhau;
    }

    /** Thực thể Sân bóng */
    public static class San {
        public String maSan, tenSan, loaiSan, trangThai;
        public double giaTheoGio;
    }

    /** Thực thể Lịch đặt sân */
    public static class LichSan {
        public String maLich, maSan, buoi, ca, gioBatDau, gioKetThuc, trangThai;
        public LocalDate ngayDat;
        public int soCa;
    }

    /** Thực thể Dịch vụ đi kèm */
    public static class DichVu {
        public String maDv, tenDv;
        public double donGia;
    }

    /**
     * Hằng số loại Voucher — mở rộng thêm loại mới chỉ cần bổ sung hằng số.
     * loai = 1 → Hóa đơn (Booking)
     * loai = 2 → Sân
     * loai = 3 → Dịch vụ
     */
    public static final class VoucherLoai {
        public static final int HOA_DON = 1;
        public static final int SAN = 2;
        public static final int DICH_VU = 3;

        private VoucherLoai() {}

        public static String ten(int loai) {
            return switch (loai) {
                case HOA_DON -> "Hóa đơn";
                case SAN -> "Sân";
                case DICH_VU -> "Dịch vụ";
                default -> "Khác (" + loai + ")";
            };
        }

        public static boolean isValid(int loai) {
            return loai == HOA_DON || loai == SAN || loai == DICH_VU;
        }
    }

    /**
     * Thực thể Voucher — ánh xạ bảng Voucher.
     * loai_giam: MONEY (giảm tiền) hoặc PERCENT (giảm %).
     */
    public static class Voucher {
        private String maVoucher;
        private String maCode;
        private String tenVoucher;
        private String loaiGiam;
        private double giaTri;
        private double donToiThieu;
        private LocalDate ngayBatDau;
        private LocalDate ngayKetThuc;
        private int soLuong;
        private String trangThai;
        private int loai;

        public Voucher() {}

        public Voucher(String maVoucher, String maCode, String tenVoucher, String loaiGiam,
                       double giaTri, double donToiThieu, LocalDate ngayBatDau, LocalDate ngayKetThuc,
                       int soLuong, String trangThai, int loai) {
            this.maVoucher = maVoucher;
            this.maCode = maCode;
            this.tenVoucher = tenVoucher;
            this.loaiGiam = loaiGiam;
            this.giaTri = giaTri;
            this.donToiThieu = donToiThieu;
            this.ngayBatDau = ngayBatDau;
            this.ngayKetThuc = ngayKetThuc;
            this.soLuong = soLuong;
            this.trangThai = trangThai;
            this.loai = loai;
        }

        public String getMaVoucher() { return maVoucher; }
        public void setMaVoucher(String maVoucher) { this.maVoucher = maVoucher; }

        public String getMaCode() { return maCode; }
        public void setMaCode(String maCode) { this.maCode = maCode; }

        public String getTenVoucher() { return tenVoucher; }
        public void setTenVoucher(String tenVoucher) { this.tenVoucher = tenVoucher; }

        public String getLoaiGiam() { return loaiGiam; }
        public void setLoaiGiam(String loaiGiam) { this.loaiGiam = loaiGiam; }

        public double getGiaTri() { return giaTri; }
        public void setGiaTri(double giaTri) { this.giaTri = giaTri; }

        public double getDonToiThieu() { return donToiThieu; }
        public void setDonToiThieu(double donToiThieu) { this.donToiThieu = donToiThieu; }

        public LocalDate getNgayBatDau() { return ngayBatDau; }
        public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }

        public LocalDate getNgayKetThuc() { return ngayKetThuc; }
        public void setNgayKetThuc(LocalDate ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

        public int getSoLuong() { return soLuong; }
        public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

        public String getTrangThai() { return trangThai; }
        public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

        public int getLoai() { return loai; }
        public void setLoai(int loai) { this.loai = loai; }

        /** true nếu giảm theo phần trăm */
        public boolean isPercent() {
            return "PERCENT".equalsIgnoreCase(loaiGiam);
        }

        /** true nếu giảm số tiền cố định */
        public boolean isMoney() {
            return "MONEY".equalsIgnoreCase(loaiGiam);
        }

        /** Nhãn loại áp dụng (Hóa đơn / Sân / Dịch vụ) */
        public String getTenLoai() {
            return VoucherLoai.ten(loai);
        }

        @Override
        public String toString() {
            return "Voucher{" +
                    "maVoucher='" + maVoucher + '\'' +
                    ", maCode='" + maCode + '\'' +
                    ", tenVoucher='" + tenVoucher + '\'' +
                    ", loaiGiam='" + loaiGiam + '\'' +
                    ", giaTri=" + giaTri +
                    ", donToiThieu=" + donToiThieu +
                    ", ngayBatDau=" + ngayBatDau +
                    ", ngayKetThuc=" + ngayKetThuc +
                    ", soLuong=" + soLuong +
                    ", trangThai='" + trangThai + '\'' +
                    ", loai=" + loai +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Voucher voucher = (Voucher) o;
            return Objects.equals(maVoucher, voucher.maVoucher);
        }

        @Override
        public int hashCode() {
            return Objects.hash(maVoucher);
        }
    }

    /** Thực thể Hóa đơn */
    public static class HoaDon {
        public String maHd, maKh, maNv, trangThai, maVoucher;
        public double tongTien, tienCoc, soTienConLai, tienGiam;
    }

    /** Thực thể Chi tiết hóa đơn */
    public static class CTHD {
        public int maCthd;
        public String maHd, maLich, maDv;
        public int soLuong;
        public double donGia, thanhTien;
    }
}
