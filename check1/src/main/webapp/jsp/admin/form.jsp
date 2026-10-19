<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<c:set var="isFinalStatus" value="${(meta.table=='HoaDon' && (record['trang_thai']=='ĐÃ THANH TOÁN' || record['trang_thai']=='ĐÃ HỦY')) || (meta.table=='CTHD' && (invoiceStatus=='ĐÃ THANH TOÁN' || invoiceStatus=='ĐÃ HỦY'))}"/>
<c:set var="isInvoice" value="${meta.table=='HoaDon'}"/>
<c:set var="isInvoiceEdit" value="${isInvoice && !isNew}"/>

<c:choose>
<c:when test="${isInvoiceEdit}">
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
.status-badge.booked{background:#7b1fa2;}
.edit-section{margin:15px 0;padding:12px;border:1px solid #ccc;background:#f9f9f9;border-radius:4px;}
.edit-section .edit-title{font-weight:bold;margin-bottom:8px;color:#1565c0;}
.edit-section .edit-list{max-height:200px;overflow-y:auto;}
.edit-section .edit-item{margin:5px 0;display:flex;align-items:center;gap:10px;}
.edit-section .edit-item input[type=checkbox]{width:18px;height:18px;}
.edit-section .edit-item label{margin:0;font-weight:normal;}
.edit-section .edit-item input[type=number]{width:50px;padding:4px;}
</style>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=HoaDon">&larr; Danh sách</a></p>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<c:if test="${not empty param.saved && param.saved=='1'}">
    <div class="alert" style="background:#e8f5e9;color:#2e7d32;border:1px solid #4caf50;">✓ Đã cập nhật hóa đơn thành công!</div>
</c:if>
<c:if test="${not empty param.datsan && param.datsan=='1'}">
    <div class="alert" style="background:#e8f5e9;color:#2e7d32;border:1px solid #4caf50;">✓ Đã xác nhận nhận sân thành công!</div>
</c:if>
<div class="invoice-container">
<form method="post">
<input type="hidden" name="entity" value="HoaDon">
<input type="hidden" name="op" id="invoiceOp" value="update">
<input type="hidden" name="ma_hd" value="${record['ma_hd']}">
<input type="hidden" name="ma_kh" value="${record['ma_kh']}">
<input type="hidden" name="ma_nv" value="${record['ma_nv']}">
<input type="hidden" name="ngay_lap" value="${record['ngay_lap']}">
<input type="hidden" name="tong_tien" value="${record['tong_tien']}">
<input type="hidden" name="tien_coc" value="${record['tien_coc']}">
<input type="hidden" name="so_tien_con_lai" value="${record['so_tien_con_lai']}">
<c:if test="${not empty param.back}"><input type="hidden" name="back" value="${param.back}"></c:if>

<!-- Header -->
<div class="invoice-header">
    <h2>SÂN BÓNG HOA SEN</h2>
    <div class="sub">CHỈNH SỬA HÓA ĐƠN</div>
</div>

<!-- Thông tin hóa đơn -->
<div class="invoice-info">
    <div class="info-item">
        <span class="info-label">Mã hóa đơn</span>
        <span class="info-value highlight">${record['ma_hd']}</span>
    </div>
    <div class="info-item">
        <span class="info-label">Trạng thái</span>
        <span class="info-value">
            <c:choose>
                <c:when test="${isFinalStatus}">
                    <input type="hidden" name="trang_thai" value="${record['trang_thai']}">
                    <c:choose>
                        <c:when test="${record['trang_thai']=='ĐÃ THANH TOÁN'}"><span class="status-badge paid">Đã thanh toán</span></c:when>
                        <c:when test="${record['trang_thai']=='ĐÃ HỦY'}"><span class="status-badge cancelled">Đã hủy</span></c:when>
                        <c:otherwise>${record['trang_thai']}</c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <select name="trang_thai" style="padding:6px;border-radius:4px;border:1px solid #ccc;">
                        <option value="ĐÃ ĐẶT" ${record['trang_thai']=='ĐÃ ĐẶT'?'selected':''}>Đã đặt</option>
                        <option value="CHƯA THANH TOÁN" ${record['trang_thai']=='CHƯA THANH TOÁN'?'selected':''}>Chưa thanh toán</option>
                        <option value="MỚI THANH TOÁN CỌC" ${record['trang_thai']=='MỚI THANH TOÁN CỌC'?'selected':''}>Mới thanh toán cọc</option>
                        <option value="ĐÃ THANH TOÁN" ${record['trang_thai']=='ĐÃ THANH TOÁN'?'selected':''}>Đã thanh toán</option>
                        <option value="ĐÃ HỦY" ${record['trang_thai']=='ĐÃ HỦY'?'selected':''}>Đã hủy</option>
                    </select>
                </c:otherwise>
            </c:choose>
        </span>
    </div>
    <div class="info-item">
        <span class="info-label">Khách hàng</span>
        <span class="info-value">${inv.ho_ten}</span>
    </div>
    <div class="info-item">
        <span class="info-label">Số điện thoại</span>
        <span class="info-value">${inv.sdt}</span>
    </div>
    <c:if test="${not empty inv.email}">
    <div class="info-item">
        <span class="info-label">Email</span>
        <span class="info-value">${inv.email}</span>
    </div>
    </c:if>
    <div class="info-item">
        <span class="info-label">Ngày lập</span>
        <span class="info-value">${inv.ngay_lap}</span>
    </div>
    <c:if test="${not empty inv.ten_nv}">
    <div class="info-item">
        <span class="info-label">Nhân viên phụ trách</span>
        <span class="info-value">${inv.ten_nv} (${inv.chuc_vu})</span>
    </div>
    </c:if>
    <div class="info-item">
        <span class="info-label">Đã nhận sân</span>
        <span class="info-value">
            <c:choose>
                <c:when test="${'' += record['da_dat_san'] eq 'true' || '' += record['da_dat_san'] eq '1'}">
                    <span style="color:green;font-weight:bold;">✓ Đã nhận sân</span>
                </c:when>
                <c:otherwise>
                    <span style="color:#888;">Chưa nhận sân</span>
                    <c:if test="${!isFinalStatus}">
                        <button
                                type="submit"
                                class="btn"
                                style="background:#7b1fa2;color:white;padding:6px 15px;font-size:0.9em;margin-top:5px;"
                                onclick="
                                if (!confirm('Xác nhận đã nhận sân?')) return false;
                                document.getElementById('invoiceOp').value='confirmDatSan';
                                return true;
                            ">
                            ✓ Nhận sân
                        </button>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </span>
    </div>
</div>

<!-- Sân đã đặt -->
<c:set var="fieldTotal" value="0"/>
<div class="invoice-section">
    <h3>Danh sách sân đã đặt</h3>
    <c:if test="${empty inv.slots}">
        <p style="color:#999;">Không có sân nào.</p>
    </c:if>
    <c:if test="${not empty inv.slots}">
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
        <c:forEach items="${inv.slots}" var="sl">
        <c:set var="fieldTotal" value="${fieldTotal + sl.thanh_tien}"/>
        <tr>
            <td><b>${sl.ten_san}</b> <small style="color:#888;">(${sl.loai_san})</small></td>
            <td>${sl.ngay_dat}</td>
            <td>${sl.buoi} - Ca ${sl.so_ca}</td>
            <td>${sl.gio_bat_dau} - ${sl.gio_ket_thuc}</td>
            <td style="text-align:right;"><fmt:formatNumber value="${sl.thanh_tien}" type="number"/>đ</td>
        </tr>
        </c:forEach>
        </tbody>
    </table>
    </c:if>
</div>

<!-- Dịch vụ -->
<c:set var="serviceTotal" value="0"/>
<c:if test="${not empty inv.services}">
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
        <c:forEach items="${inv.services}" var="sv">
        <c:set var="serviceTotal" value="${serviceTotal + sv.thanh_tien}"/>
        <tr>
            <td>${sv.ten_dv}</td>
            <td style="text-align:center;">${sv.so_luong}</td>
            <td style="text-align:right;"><fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</td>
            <td style="text-align:right;"><fmt:formatNumber value="${sv.thanh_tien}" type="number"/>đ</td>
        </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</c:if>

<!-- Chỉnh sửa sân & dịch vụ (chỉ khi chưa thanh toán/hủy) -->
<c:if test="${!isFinalStatus}">
<div class="invoice-section" style="padding-top:0;">
    <h3>Chỉnh sửa sân & dịch vụ</h3>
    
    <!-- Chọn sân/ca -->
    <div class="edit-section">
        <div class="edit-title">Chọn sân/ca trong ngày ${not empty inv.slots ? inv.slots[0].ngay_dat : ''}:</div>
        <div class="edit-list">
            <c:forEach items="${availableSlots}" var="slot">
                <div class="edit-item" style="padding:4px; border-bottom:1px dashed #d0e8ff;">
                    <input type="checkbox" name="add_slot_${slot.ma_san}_${slot.so_ca}" ${slot.checked ? 'checked' : ''}>
                    <label style="cursor:pointer;">
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
    
    <!-- Chọn dịch vụ -->
    <div class="edit-section">
        <div class="edit-title">Dịch vụ đi kèm:</div>
        <div class="edit-list">
            <c:forEach items="${services}" var="sv">
                <c:set var="isSelected" value="false"/>
                <c:if test="${not empty editServices}">
                    <c:forEach items="${editServices}" var="esId">
                        <c:if test="${esId == sv.ma_dv}"><c:set var="isSelected" value="true"/></c:if>
                    </c:forEach>
                </c:if>
                <c:set var="savedQty" value="${not empty editServiceQty ? editServiceQty[sv.ma_dv] : 1}"/>
                <div class="edit-item">
                    <input type="checkbox" name="service_${sv.ma_dv}" ${isSelected ? 'checked' : ''}>
                    <label>${sv.ten_dv} — <fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</label>
                    <input type="number" name="qty_${sv.ma_dv}" value="${savedQty}" min="1" max="10">
                </div>
            </c:forEach>
            <c:if test="${empty services}">
                <p style="font-size: 0.9em; color: #666;">Không có dịch vụ</p>
            </c:if>
        </div>
    </div>
</div>
</c:if>

<!-- Tổng tiền -->
<div class="invoice-total">
    <div class="total-row"><span>Tiền sân:</span> <b><fmt:formatNumber value="${fieldTotal}" type="number"/>đ</b></div>
    <c:if test="${serviceTotal > 0}">
    <div class="total-row"><span>Tiền dịch vụ:</span> <b><fmt:formatNumber value="${serviceTotal}" type="number"/>đ</b></div>
    </c:if>
    <c:if test="${inv.discount_amount > 0}">
    <div class="total-row"><span>Tiền trước giảm:</span> <fmt:formatNumber value="${inv.raw_total}" type="number"/>đ</div>
    <div class="total-row" style="color:green;"><span>Giảm giá:</span> -<fmt:formatNumber value="${inv.discount_amount}" type="number"/>đ</div>
    </c:if>
    <div class="total-row grand"><span>Tổng cộng:</span> <span><fmt:formatNumber value="${record['tong_tien']}" type="number"/>đ</span></div>
    <div class="total-row deposit"><span>Đã thanh toán (Cọc):</span> <span><fmt:formatNumber value="${record['tien_coc']}" type="number"/>đ</span></div>
    <div class="total-row" style="color:#c62828;font-size:16px;font-weight:bold;"><span>SỐ TIỀN CẦN THANH TOÁN:</span> <span><fmt:formatNumber value="${record['so_tien_con_lai']}" type="number"/>đ</span></div>
</div>

<!-- Actions -->
<div class="invoice-actions">
    <c:if test="${!isFinalStatus}">
        <button class="btn" style="background:#1565c0;color:white;padding:10px 25px;font-size:1.1em;" onclick="return confirm('Bạn có chắc muốn lưu hóa đơn này?')">Lưu</button>
        <c:if test="${record.trang_thai=='ĐÃ ĐẶT' || record.trang_thai=='CHƯA THANH TOÁN' || record.trang_thai=='MỚI THANH TOÁN CỌC'}">
            <button type="submit" name="afterSave" value="payment" class="btn" style="background:#2e7d32;color:white;padding:10px 25px;font-size:1.1em;" onclick="return confirm('Hóa đơn sẽ được lưu trước khi chuyển sang thanh toán. Tiếp tục?')">Lưu & Thanh toán</button>
        </c:if>
    </c:if>
    <a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=HoaDon" style="padding:10px 25px;font-size:1.1em;" onclick="return confirm('Bạn có chắc muốn hủy thay đổi?')">Hủy</a>
</div>
</form>
</div>
</div>
</c:when>
<c:otherwise>
<div class="container" style="max-width:500px;">
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
    <c:when test="${col=='da_dat_san'}">
        <c:choose>
            <c:when test="${'' += record[col] eq 'true' || '' += record[col] eq '1'}">
                <input type="checkbox" name="${col}" value="1" checked disabled style="width:18px;height:18px;">
                <span style="color:green;font-weight:bold;">✓ Đã nhận sân</span>
            </c:when>
            <c:otherwise>
                <input type="checkbox" name="${col}" value="1" disabled style="width:18px;height:18px;">
                <span style="color:#888;">Chưa nhận sân</span>
            </c:otherwise>
        </c:choose>
    </c:when>
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
<p><c:if test="${!isFinalStatus}">
    <c:choose>
        <c:when test="${isInvoice && !isNew}">
            <button class="btn" onclick="return confirm('Bạn có chắc muốn lưu hóa đơn này?')">Lưu</button>
        </c:when>
        <c:otherwise>
            <button class="btn">Lưu</button>
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${isInvoice && !isNew && (record.trang_thai=='ĐÃ ĐẶT' || record.trang_thai=='CHƯA THANH TOÁN' || record.trang_thai=='MỚI THANH TOÁN CỌC')}">
    <button type="submit" name="afterSave" value="payment" class="btn" style="background:#2e7d32;color:white;" onclick="return confirm('Hóa đơn sẽ được lưu trước khi chuyển sang thanh toán. Tiếp tục?')">Lưu & Thanh toán</button>
</c:if>
<c:choose>
    <c:when test="${isInvoice && !isNew}">
        <a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}" onclick="return confirm('Bạn có chắc muốn hủy thay đổi?')">Hủy</a>
    </c:when>
    <c:otherwise>
        <a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${meta.table}">Hủy</a>
    </c:otherwise>
</c:choose></p>
</form></div></div>
</c:otherwise>
</c:choose>
<%@ include file="/jsp/common/footer.jspf" %>