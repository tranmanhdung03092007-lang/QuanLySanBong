<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container">
<p><a class="btn" href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a></p>
<h2>Quản lý Hóa đơn</h2>
<form method="GET" style="margin:10px 0;">
<input type="text" name="search" placeholder="Tìm mã HĐ, tên KH..." value="${param.search}" style="width:200px;display:inline;">
<select name="status" style="width:150px;display:inline;">
<option value="">Tất cả</option>
<option value="CHƯA THANH TOÁN" ${param.status=='CHƯA THANH TOÁN'?'selected':''}>Chưa thanh toán</option>
<option value="ĐANG XỬ LÝ" ${param.status=='ĐANG XỬ LÝ'?'selected':''}>Đang xử lý</option>
<option value="ĐÃ THANH TOÁN" ${param.status=='ĐÃ THANH TOÁN'?'selected':''}>Đã thanh toán</option>
<option value="ĐÃ HỦY" ${param.status=='ĐÃ HỦY'?'selected':''}>Đã hủy</option>
</select>
<button class="btn">Lọc</button>
</form>
<div style="overflow-x:auto;">
<table><thead><tr><th>Mã HĐ</th><th>Khách</th><th>SĐT</th><th>Sân</th><th>Ngày</th><th>Giờ</th><th>Tiền</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
<tbody>
<c:forEach items="${rows}" var="row">
<tr>
<td><b>${row.ma_hd}</b></td>
<td>${row.ho_ten}</td>
<td>${row.sdt}</td>
<td>${row.ten_san}</td>
<td>${row.ngay_dat}</td>
<td><fmt:formatDate value="${row.gio_bat_dau}" pattern="HH:mm"/> - <fmt:formatDate value="${row.gio_ket_thuc}" pattern="HH:mm"/></td>
<td><fmt:formatNumber value="${row.tong_tien}" type="number"/></td>
<td>${row.trang_thai}</td>
<td>
<c:if test="${row.trang_thai=='CHƯA THANH TOÁN'||row.trang_thai=='ĐANG XỬ LÝ'}">
<form method="post" style="display:inline;"><input type="hidden" name="op" value="setProcessing"><input type="hidden" name="id" value="${row.ma_hd}"><button class="btn">Xử lý</button></form>
</c:if>
<c:if test="${row.trang_thai=='ĐANG XỬ LÝ'}">
<form method="post" style="display:inline;"><input type="hidden" name="op" value="pay"><input type="hidden" name="id" value="${row.ma_hd}"><button class="btn">Xác nhận</button></form>
</c:if>
<c:if test="${row.trang_thai!='ĐÃ HỦY'&&row.trang_thai!='ĐÃ THANH TOÁN'}">
<form method="post" style="display:inline;" onsubmit="return confirm('Hủy?')"><input type="hidden" name="op" value="cancel"><input type="hidden" name="id" value="${row.ma_hd}"><button class="btn">Hủy</button></form>
</c:if>
</td>
</tr>
</c:forEach>
<c:if test="${empty rows}"><tr><td colspan="9" style="text-align:center;padding:20px;">Chưa có hóa đơn</td></tr></c:if>
</tbody></table></div>
</div>
<%@ include file="/jsp/common/footer.jspf" %>