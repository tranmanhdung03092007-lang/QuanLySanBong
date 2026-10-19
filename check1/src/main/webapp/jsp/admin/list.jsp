<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
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

<%-- Khu vực thống kê doanh thu cho Hóa đơn --%>
<c:if test="${meta.table=='HoaDon'}">
<div style="margin:15px 0;padding:15px;border:1px solid #ddd;border-radius:8px;background:#f8f9fa;">
    <h3 style="margin:0 0 10px 0;color:#1565c0;">📊 Thống kê doanh thu</h3>
    <form method="get" style="margin:10px 0;display:flex;gap:10px;align-items:center;flex-wrap:wrap;">
        <input type="hidden" name="entity" value="HoaDon">
        <label style="font-weight:bold;">Từ ngày:</label>
        <input type="date" name="fromDate" value="${param.fromDate}" style="padding:6px;border:1px solid #ccc;border-radius:4px;">
        <label style="font-weight:bold;">Đến ngày:</label>
        <input type="date" name="toDate" value="${param.toDate}" style="padding:6px;border:1px solid #ccc;border-radius:4px;">
        <button class="btn" style="background:#1565c0;color:white;">Lọc</button>
        <a class="btn" href="?entity=HoaDon">Xóa lọc</a>
    </form>
    
    <table style="width:100%;border-collapse:collapse;font-size:14px;margin-bottom:15px;">
        <tr>
            <td colspan="2" style="padding:10px;border:1px solid #ddd;background:#e3f2fd;text-align:center;"><b>📋 HÓA ĐƠN</b></td>
            <td colspan="2" style="padding:10px;border:1px solid #ddd;background:#e8f5e9;text-align:center;"><b>💰 DOANH THU</b></td>
        </tr>
        <tr>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Tổng số hóa đơn:</b></td>
            <td style="padding:8px;border:1px solid #ddd;"><b>${revenue.total_invoices}</b></td>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Tổng doanh thu:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#2e7d32;font-weight:bold;"><fmt:formatNumber value="${revenue.total_revenue}" type="number"/>đ</td>
        </tr>
        <tr>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Hóa đơn đã thanh toán:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#2e7d32;font-weight:bold;"><b>${revenue.paid_count}</b> (<fmt:formatNumber value="${revenue.paid_total}" type="number"/>đ)</td>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Tiền còn lại phải thu:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#c62828;font-weight:bold;"><fmt:formatNumber value="${revenue.total_remaining}" type="number"/>đ</td>
        </tr>
        <tr>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Hóa đơn đã hủy:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#c62828;"><b>${revenue.cancelled_count}</b></td>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Tổng cộng:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#1565c0;font-weight:bold;"><fmt:formatNumber value="${revenue.total_revenue + revenue.total_remaining}" type="number"/>đ</td>
        </tr>
        <tr>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Hóa đơn đang chờ:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#f57c00;"><b>${revenue.pending_count}</b></td>
            <td style="padding:8px;border:1px solid #ddd;background:#f5f5f5;"><b>Tiền cọc đã thu:</b></td>
            <td style="padding:8px;border:1px solid #ddd;color:#1565c0;font-weight:bold;"><fmt:formatNumber value="${revenue.total_deposit}" type="number"/>đ</td>
        </tr>
    </table>
    
    <c:if test="${revenue.paid_count > 0}">
    <div style="margin-top:15px;padding:15px;background:#fff;border:1px solid #e0e0e0;border-radius:8px;">
        <h4 style="margin:0 0 10px 0;color:#1565c0;">📊 Chi tiết doanh thu hóa đơn đã thanh toán</h4>
        <table style="width:100%;border-collapse:collapse;font-size:13px;">
            <tr style="background:#f5f5f5;">
                <td style="padding:8px;border:1px solid #ddd;"><b>Tiền cọc (đã thu):</b></td>
                <td style="padding:8px;border:1px solid #ddd;color:#2e7d32;font-weight:bold;"><fmt:formatNumber value="${revenue.paid_deposit}" type="number"/>đ</td>
                <td style="padding:8px;border:1px solid #ddd;"><b>Tiền sân:</b></td>
                <td style="padding:8px;border:1px solid #ddd;color:#1565c0;font-weight:bold;"><fmt:formatNumber value="${revenue.paid_field}" type="number"/>đ</td>
                <td style="padding:8px;border:1px solid #ddd;"><b>Tiền dịch vụ:</b></td>
                <td style="padding:8px;border:1px solid #ddd;color:#c62828;font-weight:bold;"><fmt:formatNumber value="${revenue.paid_service}" type="number"/>đ</td>
                <td style="padding:8px;border:1px solid #ddd;background:#e8f5e9;"><b>Tổng đã thu:</b></td>
                <td style="padding:8px;border:1px solid #ddd;background:#e8f5e9;color:#2e7d32;font-weight:bold;"><fmt:formatNumber value="${revenue.paid_deposit + revenue.paid_field + revenue.paid_service}" type="number"/>đ</td>
            </tr>
        </table>
    </div>
    </c:if>
    
    <c:if test="${not empty dailyRevenue}">
    <div style="margin-top:15px;padding:15px;background:#fff;border:1px solid #e0e0e0;border-radius:8px;">
        <h4 style="margin:0 0 15px 0;color:#1565c0;">📈 Biểu đồ doanh thu theo ngày</h4>
        <canvas id="dailyRevenueChart" style="width:100%;max-height:300px;"></canvas>
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <script>
        document.addEventListener('DOMContentLoaded', function() {
            const labels = [
                <c:forEach items="${dailyRevenue}" var="day" varStatus="st">
                '<fmt:formatDate value="${day.ngay}" pattern="dd/MM"/>'${!st.last?',':''}
                </c:forEach>
            ];
            const doanhThu = [
                <c:forEach items="${dailyRevenue}" var="day" varStatus="st">
                ${day.doanh_thu}${!st.last?',':''}
                </c:forEach>
            ];
            const tienCoc = [
                <c:forEach items="${dailyRevenue}" var="day" varStatus="st">
                ${day.tien_coc}${!st.last?',':''}
                </c:forEach>
            ];
            new Chart(document.getElementById('dailyRevenueChart'), {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Doanh thu',
                            data: doanhThu,
                            backgroundColor: 'rgba(46,125,50,0.7)',
                            borderColor: '#2e7d32',
                            borderWidth: 1
                        },
                        {
                            label: 'Tiền cọc',
                            data: tienCoc,
                            backgroundColor: 'rgba(21,101,192,0.7)',
                            borderColor: '#1565c0',
                            borderWidth: 1
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: { position: 'top' },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return context.dataset.label + ': ' + new Intl.NumberFormat('vi-VN').format(context.parsed.y) + 'đ';
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    if (value >= 1000000) return (value/1000000) + 'M';
                                    if (value >= 1000) return (value/1000) + 'K';
                                    return value;
                                }
                            }
                        }
                    }
                }
            });
        });
        </script>
    </div>
    </c:if>
</div>
</c:if>
<div style="overflow-x:auto;">
<table><thead><tr>
<c:forEach items="${meta.cols}" var="col"><th>${L[col]!=null?L[col]:col}</th></c:forEach>
<th>Thao tác</th></tr></thead>
<tbody>
<c:forEach items="${rows}" var="row">
<tr>
<c:forEach items="${meta.cols}" var="col">
<td>
    <c:choose>
        <c:when test="${meta.table == 'HoaDon' && col == 'da_dat_san'}">
            <c:choose>
                <c:when test="${row[col]}">
                <span style="color:#2e7d32;font-weight:bold;">
                    ✓ Đã đặt sân
                </span>
                </c:when>

                <c:otherwise>
                <span style="color:#f57c00;">
                    Chưa đặt sân
                </span>
                </c:otherwise>
            </c:choose>
        </c:when>

        <c:otherwise>
            ${row[col]}
        </c:otherwise>
    </c:choose>
</td>
</c:forEach>
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
    <c:if test="${row.trang_thai!='ĐÃ HỦY' && row.da_dat_san != true}">
        <form method="post" style="display:inline;" onsubmit="return confirm('Xác nhận nhận sân cho hóa đơn ${row.ma_hd}?')">
            <input type="hidden" name="entity" value="HoaDon">
            <input type="hidden" name="op" value="confirmDatSan">
            <input type="hidden" name="ma_hd" value="${row.ma_hd}">
            <button class="btn" style="background:#1565c0;color:white;">Nhận sân</button>
        </form>
    </c:if>
    <c:if test="${row.da_dat_san == true}">
        <span style="color:green;font-weight:bold;">✓ Đã nhận sân</span>
    </c:if>
</c:if>
<c:if test="${meta.table!='HoaDon'}">
<form method="post" style="display:inline;" onsubmit="return confirm('Xóa?')">
<input type="hidden" name="entity" value="${meta.table}">
<input type="hidden" name="op" value="delete"><input type="hidden" name="${meta.pk}" value="${row[meta.pk]}">
<button class="btn">Xóa</button></form>
</c:if></td></tr>
</c:forEach>
<c:if test="${empty rows}"><tr><td colspan="20" style="text-align:center;padding:20px;">Chưa có dữ liệu</td></tr></c:if>
</tbody></table></div>
<p><c:forEach begin="1" end="${totalPages}" var="n">
<a class="btn" href="?entity=${meta.table}&page=${n}&search=${param.search}&fromDate=${param.fromDate}&toDate=${param.toDate}">${n}</a>
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