<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="dayNames" value="Thứ 2,Thứ 3,Thứ 4,Thứ 5,Thứ 6,Thứ 7,Chủ nhật"/>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.booking-layout{display:flex;gap:16px;align-items:flex-start;max-width:1600px;margin:0 auto;}
.booking-form-panel{flex:0 0 320px;position:sticky;top:10px;}
.booking-table-panel{flex:1;min-width:0;}
.booking-table table{font-size:13px;border-collapse:collapse;width:100%;}
.booking-table th,.booking-table td{border:1px solid #ccc;padding:8px;text-align:center;vertical-align:top;min-width:90px;}
.booking-table th{background:#f5f5f5;font-weight:bold;}
.booking-table thead tr:first-child th{background:#1565c0;color:#fff;}
.booking-table tbody th{background:#1565c0;color:#fff;}
.booking-table td.free{background:#fff;color:#333;cursor:pointer;transition:all 0.2s ease;}
.booking-table td.free:hover{background:#e3f2fd;transform:scale(1.02);border-color:#2196f3;box-shadow:0 2px 8px rgba(0,0,0,0.1);}
.booking-table td.selected{background:#2196f3;color:#fff;font-weight:bold;box-shadow:0 0 10px rgba(33,150,243,0.5);animation:glow 0.5s ease;}
.booking-table td.holding{background:#ffd54f;color:#333;cursor:not-allowed;font-style:italic;}
.booking-table td.pending{background:#ffd54f;color:#333;cursor:not-allowed;}
.booking-table td.booked{background:#ef5350;color:#fff;cursor:not-allowed;}
.booking-table td.event{background:#ce93d8;color:#333;cursor:not-allowed;}
.booking-table td.expired{background:#9e9e9e;color:#fff;cursor:not-allowed;}
.service-menu-table{width:100%;border-collapse:collapse;margin-top:10px;font-size:12px;}
.service-menu-table th,.service-menu-table td{border:1px solid #eee;padding:6px;text-align:left;}
.service-menu-table th{background:#f9f9f9;}
.invoice-detail-section{margin-top:20px;padding:15px;border:2px dashed #1565c0;background:#fff;}
.invoice-table{width:100%;border-collapse:collapse;margin-top:10px;font-size:13px;}
.invoice-table th,.invoice-table td{padding:8px;border-bottom:1px solid #eee;text-align:left;}
.invoice-table th{color:#1565c0;font-weight:bold;}
.invoice-total{margin-top:10px;text-align:right;font-size:15px;}
.badge-status{padding:2px 6px;border-radius:10px;font-size:10px;font-weight:bold;text-transform:uppercase;}
.status-free{background:#e8f5e9;color:#2e7d32;}
.status-holding{background:#fff8e1;color:#f57f17;}
.status-booked{background:#ffebee;color:#c62828;}
.status-event{background:#f3e5f5;color:#7b1fa2;}
.status-expired{background:#eeeeee;color:#616161;}
@keyframes glow{0%{box-shadow:0 0 0 0 rgba(33,150,243,0.7);}50%{box-shadow:0 0 0 5px rgba(33,150,243,0.3);}100%{box-shadow:0 0 10px rgba(33,150,243,0.5);}}
</style>
<div style="padding:0 12px;">
<p><a class="btn" href="${pageContext.request.contextPath}/home">&larr; Về</a>
<input type="date" id="booking-date" min="${today}" value="${viewDate}" style="width:auto;"></p>
<h3>Đặt lịch</h3>
<c:if test="${not empty bookingError}"><div class="alert">${bookingError}</div><c:remove var="bookingError" scope="session"/></c:if>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<c:if test="${not empty activeInvoiceId}">
<p style="background:#e3f2fd;padding:8px 12px;border-radius:4px;border-left:4px solid #2196f3;">
<b>Đang thao tác trên hóa đơn:</b> ${activeInvoiceId}
</p>
</c:if>

<c:choose>
<c:when test="${empty fields}"><div class="alert">Chưa có dữ liệu sân.</div></c:when>
<c:otherwise>
<div class="booking-layout">
<div class="booking-form-panel">
<div style="border:1px solid #999;padding:12px;background:#fff;">
<h4>Thông tin khách hàng</h4>
<form id="booking-confirm-form" action="${pageContext.request.contextPath}/booking" method="post">
<input type="hidden" name="op" value="confirm">
<input type="hidden" name="date" value="${viewDate}">

<label>Họ và tên <span style="color:red">*</span></label>
<input type="text" name="name" value="${not empty editName ? editName : ''}" placeholder="Họ tên" required style="width:100%;padding:8px;margin:4px 0;">
<label>Số điện thoại <span style="color:red">*</span></label>
<input type="tel" name="phone" value="${not empty editPhone ? editPhone : ''}" placeholder="Số điện thoại" required style="width:100%;padding:8px;margin:4px 0;">
<label>Email</label>
<input type="email" name="email" value="${not empty editEmail ? editEmail : ''}" placeholder="Email (không bắt buộc)" style="width:100%;padding:8px;margin:4px 0;">
</form>

<div class="services-menu" style="margin:15px 0;">
<b>Dịch vụ:</b>
<table class="service-menu-table">
<thead><tr><th>Tên</th><th>Giá</th><th>SL</th><th></th></tr></thead>
<tbody>
<c:forEach items="${services}" var="sv">
<tr>
    <td>${sv.ten_dv}</td>
    <td><fmt:formatNumber value="${sv.don_gia}" type="number"/></td>
    <td style="padding:4px;">
        <input type="number" form="svc_form_${sv.ma_dv}" name="qty" value="1" min="1" max="50" style="width:35px;padding:2px;">
    </td>
    <td>
        <form id="svc_form_${sv.ma_dv}" action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;">
            <input type="hidden" name="op" value="addService">
            <input type="hidden" name="date" value="${viewDate}">
            <input type="hidden" name="serviceId" value="${sv.ma_dv}">
            <button type="submit" style="padding:2px 5px;font-size:10px;">Thêm</button>
        </form>
    </td>
</tr>
</c:forEach>
</tbody>
</table>
</div>

<p style="margin-top:10px;">
    <button type="submit" form="booking-confirm-form" class="btn btn-primary" style="width:100%;padding:10px;">Xác nhận đặt sân</button>
</p>
</div>
</div>

<div class="booking-table-panel">
<div style="overflow-x:auto;">
<table class="booking-table" id="schedule-table">
<thead><tr>
<th>Sân</th>
<c:forEach items="${headers}" var="h">
<th>${fn:substring(h.label,0,2)}h-${fn:substring(h.label,6,8)}h<br><small>Ca ${h.so_ca}</small></th>
</c:forEach>
</tr></thead>
<tbody>
<c:forEach items="${fields}" var="f">
<tr>
<th>${f.ten_san}</th>
<c:set var="rowSlots" value="${schedule[f.ma_san]}"/>
<c:forEach items="${rowSlots}" var="s">
    <c:set var="sc" value="${s.status_code}"/>
    <c:set var="isSelected" value="false"/>
    <c:if test="${not empty invoice.slots}">
        <c:forEach items="${invoice.slots}" var="islot">
            <c:if test="${islot.ma_san == f.ma_san && islot.so_ca == s.so_ca && islot.ngay_dat == viewDate}">
                <c:set var="isSelected" value="true"/>
            </c:if>
        </c:forEach>
    </c:if>
    
    <td class="${isSelected ? 'selected' : (sc==0?'free':sc==1?'pending':sc==2?'booked':sc==3?'expired':'event')}" style="padding:0;">
        <c:choose>
            <c:when test="${sc==0 || isSelected}">
                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;height:100%;">
                    <input type="hidden" name="op" value="selectSlot">
                    <input type="hidden" name="fieldId" value="${f.ma_san}">
                    <input type="hidden" name="slot" value="${s.so_ca}">
                    <input type="hidden" name="date" value="${viewDate}">
                    <button type="submit" style="background:none;border:none;color:inherit;width:100%;height:100%;cursor:pointer;padding:8px;display:block;font-size:inherit;">
                        ${s.nhan_ca}
                        <div style="margin-top:4px;">
                            <c:choose>
                                <c:when test="${isSelected}"><span class="badge-status status-holding" style="background:#fff;color:#2196f3;">Đang chọn</span></c:when>
                                <c:otherwise><span class="badge-status status-free">Trống</span></c:otherwise>
                            </c:choose>
                        </div>
                    </button>
                </form>
            </c:when>
            <c:otherwise>
                <div style="padding:8px;">
                    ${s.nhan_ca}
                    <div style="margin-top:4px;">
                        <c:choose>
                            <c:when test="${sc==1}"><span class="badge-status status-holding">Chờ</span></c:when>
                            <c:when test="${sc==2}"><span class="badge-status status-booked">Hết</span></c:when>
                            <c:when test="${sc==3}"><span class="badge-status status-expired">Hết</span></c:when>
                            <c:otherwise><span class="badge-status status-event">Sự kiện</span></c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </td>
</c:forEach>
</tr>
</c:forEach>
</tbody></table></div>

<div class="invoice-detail-section" id="invoice-detail">
    <h4 style="margin-top:0;color:#1565c0;border-bottom:2px solid #1565c0;padding-bottom:5px;">Chi tiết hóa đơn tạm tính</h4>
    <c:choose>
        <c:when test="${empty invoice.slots && empty invoice.services}">
            <p style="text-align:center;color:#999;">Chưa chọn hạng mục nào.</p>
        </c:when>
        <c:otherwise>
            <table class="invoice-table">
                <thead><tr><th>Hạng mục</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th><th></th></tr></thead>
                <tbody>
                    <c:forEach items="${invoice.slots}" var="s">
                        <tr>
                            <td>Sân ${s.ten_san} - Ca ${s.so_ca} (${s.ngay_dat})</td>
                            <td>1</td>
                            <td><fmt:formatNumber value="${s.thanh_tien}" type="number"/></td>
                            <td style="font-weight:bold;color:#c62828;"><fmt:formatNumber value="${s.thanh_tien}" type="number"/></td>
                            <td>
                                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;">
                                    <input type="hidden" name="op" value="selectSlot">
                                    <input type="hidden" name="fieldId" value="${s.ma_san}">
                                    <input type="hidden" name="slot" value="${s.so_ca}">
                                    <input type="hidden" name="date" value="${s.ngay_dat}">
                                    <button type="submit" style="color:red;border:none;background:none;cursor:pointer;font-size:16px;">&times;</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:forEach items="${invoice.services}" var="sv">
                        <tr>
                            <td>${sv.ten_dv}</td>
                            <td>${sv.so_luong}</td>
                            <td><fmt:formatNumber value="${sv.don_gia}" type="number"/></td>
                            <td><fmt:formatNumber value="${sv.thanh_tien}" type="number"/></td>
                            <td>
                                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin:0;">
                                    <input type="hidden" name="op" value="removeService">
                                    <input type="hidden" name="serviceId" value="${sv.ma_dv}">
                                    <input type="hidden" name="date" value="${viewDate}">
                                    <button type="submit" style="color:red;border:none;background:none;cursor:pointer;font-size:16px;">&times;</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <div class="invoice-total">
                <c:if test="${invoice.discount_amount > 0}">
                    <p>Tiền trước giảm: <fmt:formatNumber value="${invoice.raw_total}" type="number"/> VNĐ</p>
                    <p style="color:green;">Giảm giá: -<fmt:formatNumber value="${invoice.discount_amount}" type="number"/> VNĐ</p>
                </c:if>
                <p>Tổng cộng: <b><fmt:formatNumber value="${invoice.tong_tien}" type="number"/> VNĐ</b></p>
                <p>Tiền cọc (30%): <fmt:formatNumber value="${invoice.tien_coc}" type="number"/> VNĐ</p>
                <p style="color:#c62828;">Còn lại: <b><fmt:formatNumber value="${invoice.so_tien_con_lai}" type="number"/> VNĐ</b></p>
            </div>

            <div style="margin-top: 15px; border-top: 1px solid #eee; padding-top: 10px;">
                <b>Áp dụng Voucher (Sân):</b>
                <form action="${pageContext.request.contextPath}/booking" method="post" style="margin-top:5px;">
                    <input type="hidden" name="op" value="applyVoucher">
                    <input type="hidden" name="date" value="${viewDate}">
                    <select name="voucherId" onchange="this.form.submit()" style="padding:5px; width: 100%;">
                        <option value="">-- Chọn Voucher Sân --</option>
                        <c:forEach items="${vouchers}" var="v">
                            <%-- Chỉ render voucher có loai = 1 (hoặc '1') --%>
                            <c:if test="${v.loai == 1 || v.loai == '1'}">
                                <option value="${v.ma_voucher}" ${invoice.ma_voucher == v.ma_voucher ? 'selected' : ''}>
                                        ${not empty v.ma_code ? v.ma_code : v.ma_voucher}
                                    <c:if test="${not empty v.ten_voucher}"> — ${v.ten_voucher}</c:if>
                                    — Giảm <fmt:formatNumber value="${v.gia_tri}" type="number"/>
                                        ${(v.loai_giam eq 'PERCENT' or v.loai_giam eq 'GIAM_THEO_PHAN_TRAM' or v.loai_giam eq '1') ? '%' : ' VNĐ'}
                                </option>
                            </c:if>
                        </c:forEach>
                    </select>
                </form>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</div>
</div>
</c:otherwise>
</c:choose>
</div>
<script>
(function(){
    var dateInput = document.getElementById('booking-date');
    if (dateInput) {
        dateInput.addEventListener('change', function(){
            window.location.href = '${pageContext.request.contextPath}/booking?date=' + this.value;
        });
    }
})();
</script>