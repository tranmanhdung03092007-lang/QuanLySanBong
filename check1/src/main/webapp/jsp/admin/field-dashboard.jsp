<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.field-layout{display:flex;gap:20px;align-items:flex-start;}
.field-sidebar{flex:0 0 200px;}
.field-list{width:100%;}
.field-item{display:block;padding:12px 15px;margin:5px 0;background:#fff;border:1px solid #ccc;border-radius:4px;text-decoration:none;color:#000;cursor:pointer;transition:all .2s;}
.field-item:hover{background:#e3f2fd;border-color:#2196f3;box-shadow:0 2px 8px rgba(33,150,243,.15);}
.field-item.active{background:#1565c0;color:#fff;border-color:#1565c0;box-shadow:0 2px 8px rgba(21,101,192,.3);font-weight:bold;}
.field-item .field-badge{display:inline-block;padding:2px 8px;border-radius:10px;font-size:11px;margin-top:4px;background:#e0e0e0;}
.field-item.active .field-badge{background:rgba(255,255,255,.2);color:#fff;}
.customer-table{width:100%;border-collapse:collapse;}
.customer-table th,.customer-table td{border:1px solid #ccc;padding:8px;text-align:center;}
.customer-table th{background:#f5f5f5;white-space:nowrap;}
.customer-table tr:hover{background:#f0f7ff;}
.status-badge{display:inline-block;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:bold;color:#fff;}
.status-badge.hold{background:#ff9800;}
.status-badge.booked{background:#f44336;}
.status-badge.free{background:#4caf50;}
.status-badge.expired{background:#9e9e9e;}
.status-badge.event{background:#9c27b0;}
.slot-chip{display:inline-block;padding:3px 10px;border-radius:12px;background:#e8f5e9;color:#2e7d32;font-size:12px;font-weight:bold;white-space:nowrap;}
.session-label{font-weight:bold;color:#1565c0;}

/* Nổi bật cột Sân - toàn bộ ô có nền xanh gradient */
td.field-col{background:linear-gradient(135deg,#1565c0,#1976d2) !important;color:#fff;font-weight:bold;font-size:13px;letter-spacing:.3px;}
td.field-col span{color:#fff;}

/* Nổi bật cột Ca giờ - toàn bộ ô có nền cam gradient */
td.slot-col{background:linear-gradient(135deg,#e65100,#ef6c00) !important;color:#fff;font-weight:bold;}
td.slot-col .session-label,td.slot-col .slot-chip{color:#fff;background:rgba(255,255,255,.2);}
</style>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a></p>
<h2>Quản lý Sân</h2>

<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>

<div class="field-layout">
<!-- Sidebar - Field List -->
<div class="field-sidebar">
<h3>Danh sách sân</h3>
<div class="field-list">
<c:forEach items="${fields}" var="f">
<a href="?field=${f.ma_san}" class="field-item ${f.ma_san == selectedField ? 'active' : ''}">
<strong>${f.ten_san}</strong>
<div style="font-size:12px;color:${f.ma_san == selectedField ? '#fff' : '#666'};margin-top:3px;">
<span class="field-badge">${f.loai_san}</span>
<c:if test="${not empty f.gia_theo_gio}">
<span style="margin-left:5px;"><fmt:formatNumber value="${f.gia_theo_gio}" type="number"/><small>đ/giờ</small></span>
</c:if>
</div>
</a>
</c:forEach>
</div>
</div>

<!-- Main Content - Customer Info Table -->
<div style="flex:1;">
<c:if test="${not empty selectedField}">
<h3>Khách hàng đã đặt sân ${selectedFieldName}</h3>
<c:if test="${empty bookings}">
<p>Chưa có lịch đặt nào cho sân này.</p>
</c:if>
<c:if test="${not empty bookings}">
<table class="customer-table">
<thead>
<tr>
<th>Mã đặt</th>
<th>Sân</th>
<th>Khách hàng</th>
<th>Số điện thoại</th>
<th>Ngày đặt</th>
<th>Ca</th>
<th>Trạng thái</th>
</tr>
</thead>
<tbody>
<c:forEach items="${bookings}" var="b">
<tr>
<td><strong>${b.ma_lich}</strong></td>
<td class="field-col">
<c:if test="${not empty b.ten_san}">
${b.ten_san}
</c:if>
</td>
<td>${b.ten_khach}</td>
<td>${b.sdt}</td>
<td>${b.ngay_dat}</td>
<td class="slot-col">
<c:set var="slotLabel" value="Ca ${b.so_ca}"/>
<c:if test="${not empty b.buoi}">
<span>${b.buoi}</span>&nbsp;
</c:if>
<c:if test="${not empty b.ca}">
<span>${b.ca}</span>
</c:if>
</td>
<td>
<c:choose>
<c:when test="${b.status_code == 1}"><span class="status-badge hold">Đang giữ</span></c:when>
<c:when test="${b.status_code == 2}"><span class="status-badge booked">Đã đặt</span></c:when>
<c:when test="${b.status_code == 3}"><span class="status-badge expired">Hết</span></c:when>
<c:when test="${b.status_code == 4}"><span class="status-badge event">Sự kiện</span></c:when>
<c:otherwise><span class="status-badge free">Trống</span></c:otherwise>
</c:choose>
</td>
</tr>
</c:forEach>
</tbody>
</table>

<h3 style="margin-top:30px;">Hóa đơn của sân ${selectedFieldName}</h3>
<c:if test="${empty invoices}">
<p>Chưa có hóa đơn nào.</p>
</c:if>
<c:if test="${not empty invoices}">
<table class="customer-table">
<thead>
<tr>
<th>Mã HD</th>
<th>Khách hàng</th>
<th>Tổng tiền</th>
<th>Ngày xuất</th>
<th>Trạng thái</th>
</tr>
</thead>
<tbody>
<c:forEach items="${invoices}" var="i">
<tr>
<td>${i.ma_hd}</td>
<td>${i.ten_khach}</td>
<td><fmt:formatNumber value="${i.tong_tien}" type="number"/>đ</td>
<td>${i.ngay_xuat}</td>
<td>
<span style="color:${i.da_thu ? '#4caf50' : '#f44336'}">
${i.da_thu ? 'Đã thu' : 'Chưa thu'}
</span>
</td>
</tr>
</c:forEach>
</tbody>
</table>
</c:if>
</c:if>
</c:if>

<c:if test="${empty selectedField}">
<p style="color:#666;">Vui lòng chọn sân từ danh sách bên trái để xem thông tin khách hàng.</p>
</c:if>
</div>
</div>
</div>
<%@ include file="/jsp/common/footer.jspf" %>