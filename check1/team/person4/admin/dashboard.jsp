<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container">
<h2>Bảng điều khiển</h2>
<p>Xin chào, ${sessionScope.adminUser} <a class="btn" href="${pageContext.request.contextPath}/admin/logout">Đăng xuất</a></p>
<div style="display:flex;flex-wrap:wrap;gap:10px;margin:10px 0;">
<c:forEach items="${stats}" var="x"><div class="card" style="flex:1;min-width:100px;text-align:center;"><b>${x.key}</b><br>${x.value}</div></c:forEach>
</div>
<h3>Quản lý</h3>
<div style="display:flex;flex-wrap:wrap;gap:8px;">
<c:forEach items="${['San','LichSan','KhachHang','NhanVien','DichVu','HoaDon','CTHD']}" var="e">
<a class="btn" href="${pageContext.request.contextPath}/admin/manage?entity=${e}">${L[e]}</a>
</c:forEach>
</div>
<h3>Hóa đơn</h3>
<a class="btn" href="${pageContext.request.contextPath}/admin/invoice-management">Quản lý Hóa đơn</a>
</div>
<%@ include file="/jsp/common/footer.jspf" %>