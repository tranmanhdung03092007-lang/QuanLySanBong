package com.footballbooking.service;

import com.footballbooking.model.Entities.Voucher;
import com.footballbooking.model.Entities.VoucherLoai;
import com.footballbooking.repository.VoucherRepository;

import java.util.List;
import java.util.Map;

/**
 * Tầng Service cho nghiệp vụ Voucher — lọc theo loai, CRUD.
 */
public class VoucherService {
    private final VoucherRepository repo = new VoucherRepository();

    public List<Voucher> getAll() throws Exception {
        return repo.getAll();
    }

    public Voucher getById(String maVoucher) throws Exception {
        return repo.getById(maVoucher);
    }

    public Voucher getByCode(String maCode) throws Exception {
        return repo.getByCode(maCode);
    }

    public List<Voucher> getVoucherByLoai(int loai) throws Exception {
        return repo.getVoucherByLoai(loai);
    }

    public List<Voucher> getVoucherHoaDon() throws Exception {
        return repo.getVoucherHoaDon();
    }

    public List<Voucher> getVoucherSan() throws Exception {
        return repo.getVoucherSan();
    }

    public List<Voucher> getVoucherDichVu() throws Exception {
        return repo.getVoucherDichVu();
    }

    /** Voucher cho trang Booking (chỉ loai = 1). */
    public List<Map<String, Object>> getAvailableBookingVouchers(double donHang) throws Exception {
        return repo.toMapList(repo.getAvailableHoaDonVouchers(donHang));
    }

    public List<Map<String, Object>> getVoucherSanAsMaps() throws Exception {
        return repo.toMapList(repo.getVoucherSan());
    }

    public List<Map<String, Object>> getVoucherDichVuAsMaps() throws Exception {
        return repo.toMapList(repo.getVoucherDichVu());
    }

    public List<Map<String, Object>> getVoucherByLoaiAsMaps(int loai) throws Exception {
        return repo.toMapList(repo.getVoucherByLoai(loai));
    }

    public int insert(Voucher v) throws Exception {
        if (v.getLoai() <= 0) v.setLoai(VoucherLoai.HOA_DON);
        if (v.getTrangThai() == null || v.getTrangThai().isBlank()) v.setTrangThai("HOẠT ĐỘNG");
        return repo.insert(v);
    }

    public int update(Voucher v) throws Exception {
        return repo.update(v);
    }

    public int delete(String maVoucher) throws Exception {
        return repo.delete(maVoucher);
    }

    public Map<String, Object> toMap(Voucher v) {
        return repo.toMap(v);
    }
}
