<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.invoice-container{max-width:800px;margin:0 auto;background:#fff;border:1px solid #ddd;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,0.08);}
.invoice-header{background:linear-gradient(135deg,#1565c0,#1976d2);color:#fff;padding:20px 25px;border-radius:8px 8px 0 0;}
.invoice-header h2{margin:0;font-size:22px;}
.invoice-header .sub{opacity:0.9;font-size:13px;margin-top:4px;}
.invoice-info{display:grid;grid-template-columns:1fr 1fr;gap:15px;padding:20px 25px;border-bottom:2px solid #eee;}
.invoice-info .info-item{display:flex;flex-direction:column;}
.invoice-info .info-label{font-size:11px;text-transform:uppercase;color:#888;font-weight:bold;letter-spacing:0.5px;}
.invoice-info .info-value{font-size:15px;font-weight:600;color:#333;margin-top:3px;}
.invoice-info .info-value.highlight{color:#1565c0;font-size:18px;}
.invoice-section{padding:20px 25px;}
.invoice-section h3{color:#1565c0;border-bottom:2px solid #1565c0;padding-bottom:8px;margin-top:0;font-size:16px;}
.invoice-table{width:100%;border-collapse:collapse;font-size:13px;}
.invoice-table th{background:#f5f5f5;padding:10px;text-align:left;border-bottom:2px solid #ddd;font-weight:bold;}
.invoice-table td{padding:10px;border-bottom:1px solid #eee;}
.invoice-table tr:hover{background:#f8faff;}
.invoice-total{background:#f9f9f9;padding:20px 25px;border-top:2px solid #333;border-radius:0 0 8px 8px;}
.invoice-total .total-row{display:flex;justify-content:space-between;padding:6px 0;font-size:14px;}
.invoice-total .total-row.grand{font-size:20px;font-weight:bold;color:#c62828;border-top:2px solid #333;padding-top:12px;margin-top:8px;}
.invoice-total .total-row.deposit{color:#2e7d32;font-weight:bold;}
.invoice-actions{padding:15px 25px;display:flex;gap:10px;flex-wrap:wrap;border-top:1px solid #eee;}
.status-badge{display:inline-block;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold;color:#fff;}
.status-badge.unpaid{background:#f57c00;}
.status-badge.deposited{background:#1976d2;}
.status-badge.paid{background:#2e7d32;}
.status-badge.cancelled{background:#c62828;}
</style>
<div class="container">
<c:choose>
<c:when test="${empty invoice}"><div class="alert">Không tìm thấy hóa đơn.</div></c:when>
<c:otherwise>
<c:if test="${param.paid=='1'}"><div class="alert">Thanh toán thành công!</div></c:if>
<div class="invoice-container">
    <!-- Header -->
    <div class="invoice-header">
        <h2>SÂN BÓNG HOA SEN</h2>
        <div class="sub">Hóa đơn đặt sân</div>
    </div>

    <!-- Thông tin hóa đơn -->
    <div class="invoice-info">
        <div class="info-item">
            <span class="info-label">Mã hóa đơn</span>
            <span class="info-value highlight">${invoice.ma_hd}</span>
        </div>
        <div class="info-item">
            <span class="info-label">Trạng thái</span>
            <span class="info-value">
                <c:choose>
                    <c:when test="${invoice.trang_thai=='CHƯA THANH TOÁN'}"><span class="status-badge unpaid">Cọc</span></c:when>
                    <c:when test="${invoice.trang_thai=='MỚI THANH TOÁN CỌC'}"><span class="status-badge deposited">Mới thanh toán cọc</span></c:when>
                    <c:when test="${invoice.trang_thai=='ĐÃ THANH TOÁN'}"><span class="status-badge paid">Đã thanh toán</span></c:when>
                    <c:when test="${invoice.trang_thai=='ĐÃ HỦY'}"><span class="status-badge cancelled">Đã hủy</span></c:when>
                    <c:otherwise>${invoice.trang_thai}</c:otherwise>
                </c:choose>
            </span>
        </div>
        <div class="info-item">
            <span class="info-label">Khách hàng</span>
            <span class="info-value">${invoice.ho_ten}</span>
        </div>
        <div class="info-item">
            <span class="info-label">Số điện thoại</span>
            <span class="info-value">${invoice.sdt}</span>
        </div>
        <c:if test="${not empty invoice.email}">
        <div class="info-item">
            <span class="info-label">Email</span>
            <span class="info-value">${invoice.email}</span>
        </div>
        </c:if>
        <div class="info-item">
            <span class="info-label">Ngày lập</span>
            <span class="info-value">${invoice.ngay_lap}</span>
        </div>
        <c:if test="${not empty invoice.ten_nv}">
        <div class="info-item">
            <span class="info-label">Nhân viên phụ trách</span>
            <span class="info-value">${invoice.ten_nv} (${invoice.chuc_vu})</span>
        </div>
        </c:if>
    </div>

    <!-- Sân đã đặt -->
    <div class="invoice-section">
        <h3>Danh sách sân đã đặt</h3>
        <c:if test="${empty invoice.slots}">
            <p style="color:#999;">Không có sân nào.</p>
        </c:if>
        <c:if test="${not empty invoice.slots}">
        <table class="invoice-table">
            <thead>
                <tr>
                    <th>Sân</th>
                    <th>Ngày</th>
                    <th>Ca</th>
                    <th>Thời gian</th>
                    <th style="text-align:right;">Thành tiền</th>
                </tr>
            </thead>
            <tbody>
            <c:set var="fieldTotal" value="0"/>
            <c:forEach items="${invoice.slots}" var="sl">
            <c:set var="fieldTotal" value="${fieldTotal + sl.thanh_tien}"/>
            <tr>
                <td><b>${sl.ten_san}</b> <small style="color:#888;">(${sl.loai_san})</small></td>
                <td>${sl.ngay_dat}</td>
                <td>${sl.buoi} - Ca ${sl.so_ca}</td>
                <td>${sl.gio_bat_dau} - ${sl.gio_ket_thuc}</td>
                <td style="text-align:right;"><fmt:formatNumber value="${sl.thanh_tien}" type="number"/> VNĐ</td>
            </tr>
            </c:forEach>
            </tbody>
        </table>
        </c:if>
    </div>

    <!-- Dịch vụ -->
    <c:if test="${not empty invoice.services}">
    <div class="invoice-section" style="padding-top:0;">
        <h3>Dịch vụ đi kèm</h3>
        <table class="invoice-table">
            <thead>
                <tr>
                    <th>Dịch vụ</th>
                    <th style="text-align:center;">SL</th>
                    <th style="text-align:right;">Đơn giá</th>
                    <th style="text-align:right;">Thành tiền</th>
                </tr>
            </thead>
            <tbody>
            <c:set var="serviceTotal" value="0"/>
            <c:forEach items="${invoice.services}" var="sv">
            <c:set var="serviceTotal" value="${serviceTotal + sv.thanh_tien}"/>
            <tr>
                <td>${sv.ten_dv}</td>
                <td style="text-align:center;">${sv.so_luong}</td>
                <td style="text-align:right;"><fmt:formatNumber value="${sv.don_gia}" type="number"/> VNĐ</td>
                <td style="text-align:right;"><fmt:formatNumber value="${sv.thanh_tien}" type="number"/> VNĐ</td>
            </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    </c:if>

    <!-- Tổng tiền -->
    <div class="invoice-total">
        <div class="total-row"><span>Tiền sân:</span> <b><fmt:formatNumber value="${fieldTotal}" type="number"/> VNĐ</b></div>
        <c:if test="${serviceTotal > 0}">
        <div class="total-row"><span>Tiền dịch vụ:</span> <b><fmt:formatNumber value="${serviceTotal}" type="number"/> VNĐ</b></div>
        </c:if>
        <c:if test="${invoice.discount_amount > 0}">
        <div class="total-row"><span>Tiền trước giảm:</span> <fmt:formatNumber value="${invoice.raw_total}" type="number"/> VNĐ</div>
        <div class="total-row" style="color:green;"><span>Giảm giá:</span> -<fmt:formatNumber value="${invoice.discount_amount}" type="number"/> VNĐ</div>
        </c:if>
        <div class="total-row grand"><span>Tổng cộng:</span> <span><fmt:formatNumber value="${invoice.tong_tien}" type="number"/> VNĐ</span></div>
        
        <c:if test="${invoice.trang_thai=='CHƯA THANH TOÁN'}">
        <div class="total-row deposit"><span>Tiền cọc (30%):</span> <span><fmt:formatNumber value="${invoice.tien_coc}" type="number"/> VNĐ</span></div>
        <div class="total-row" style="color:#c62828;font-size:16px;font-weight:bold;"><span>Còn lại:</span> <span><fmt:formatNumber value="${invoice.tong_tien}" type="number"/> VNĐ</span></div>
        </c:if>
        
        <c:if test="${invoice.trang_thai=='MỚI THANH TOÁN CỌC'}">
        <div class="total-row deposit"><span>Đã thanh toán (Cọc):</span> <span><fmt:formatNumber value="${invoice.tien_coc}" type="number"/> VNĐ</span></div>
        <div class="total-row" style="color:#c62828;font-size:16px;font-weight:bold;"><span>Còn lại:</span> <span><fmt:formatNumber value="${invoice.so_tien_con_lai}" type="number"/> VNĐ</span></div>
        </c:if>
        
        <c:if test="${invoice.trang_thai=='ĐÃ THANH TOÁN'}">
        <div class="total-row deposit"><span>Đã thanh toán:</span> <span><fmt:formatNumber value="${invoice.tong_tien}" type="number"/> VNĐ</span></div>
        <div class="total-row" style="color:#2e7d32;font-size:16px;font-weight:bold;"><span>Còn lại:</span> <span>0 VNĐ</span></div>
        </c:if>
    </div>

    <!-- Voucher -->
    <c:if test="${invoice.trang_thai=='CHƯA THANH TOÁN'}">
    <div style="padding:15px 25px;border-top:1px solid #eee;">
        <p><b>Áp dụng Voucher (Hóa đơn):</b></p>
        <form method="post" action="${pageContext.request.contextPath}/invoice">
            <input type="hidden" name="id" value="${invoice.ma_hd}">
            <input type="hidden" name="op" value="applyVoucher">
            <select name="voucherId" onchange="this.form.submit()" style="padding:5px; width: 100%;">
                <option value="">-- Chọn Voucher --</option>
                <c:forEach items="${vouchers}" var="v">
                    <option value="${v.ma_voucher}" ${invoice.ma_voucher == v.ma_voucher ? 'selected' : ''}>
                        ${not empty v.ma_code ? v.ma_code : v.ma_voucher}
                        <c:if test="${not empty v.ten_voucher}"> — ${v.ten_voucher}</c:if>
                        — Giảm <fmt:formatNumber value="${v.gia_tri}" type="number"/>
                        ${v.loai_giam == 'PERCENT' || v.loai_giam == 'GIAM_THEO_PHAN_TRAM' ? '%' : ' VNĐ'}
                    </option>
                </c:forEach>
            </select>
        </form>
    </div>
    </c:if>

    <!-- Actions -->
    <div class="invoice-actions">
        <c:if test="${invoice.trang_thai=='CHƯA THANH TOÁN'}">
        <form method="post" action="${pageContext.request.contextPath}/invoice" style="display:inline;">
        <input type="hidden" name="id" value="${invoice.ma_hd}">
        <input type="hidden" name="op" value="pay">
        <button class="btn btn-primary">Thanh toán cọc <fmt:formatNumber value="${invoice.tien_coc}" type="number"/> VNĐ</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/invoice" style="display:inline;" onsubmit="return confirm('Bạn có chắc muốn thanh toán toàn bộ hóa đơn?')">
        <input type="hidden" name="id" value="${invoice.ma_hd}">
        <input type="hidden" name="op" value="payRemaining">
        <button class="btn btn-primary" style="background:#2e7d32;">Thanh toán toàn bộ <fmt:formatNumber value="${invoice.tong_tien}" type="number"/> VNĐ</button>
        </form>
        <form method="post" onsubmit="return confirm('Bạn có chắc muốn hủy sân?')" action="${pageContext.request.contextPath}/invoice" style="display:inline;">
        <input type="hidden" name="id" value="${invoice.ma_hd}">
        <input type="hidden" name="op" value="cancel">
        <button class="btn" style="background:#d32f2f;color:white;">Hủy sân</button>
        </form>
        </c:if>

        <c:if test="${invoice.trang_thai=='MỚI THANH TOÁN CỌC'}">
        <form method="post" action="${pageContext.request.contextPath}/invoice" style="display:inline;">
        <input type="hidden" name="id" value="${invoice.ma_hd}">
        <input type="hidden" name="op" value="payRemaining">
        <button class="btn btn-primary">Thanh toán <fmt:formatNumber value="${invoice.so_tien_con_lai}" type="number"/> VNĐ</button>
        </form>
        <form method="post" onsubmit="return confirm('Bạn có chắc muốn hủy sân?')" action="${pageContext.request.contextPath}/invoice" style="display:inline;">
        <input type="hidden" name="id" value="${invoice.ma_hd}">
        <input type="hidden" name="op" value="cancel">
        <button class="btn" style="background:#d32f2f;color:white;">Hủy sân</button>
        </form>
        </c:if>

        <c:if test="${invoice.trang_thai=='ĐÃ THANH TOÁN'}">
        <p style="color:green;font-weight:bold;margin:0;">✓ Đã thanh toán đầy đủ</p>
        </c:if>

        <c:if test="${invoice.trang_thai=='ĐÃ HỦY'}">
        <p style="color:red;font-weight:bold;margin:0;">✗ Đơn hàng đã bị hủy</p>
        </c:if>

        <a class="btn" href="${pageContext.request.contextPath}/booking">Đặt thêm</a>
        <a class="btn" href="${pageContext.request.contextPath}/home">Trang chủ</a>
    </div>
</div>
</c:otherwise>
</c:choose>
</div>
<%@ include file="/jsp/common/footer.jspf" %>