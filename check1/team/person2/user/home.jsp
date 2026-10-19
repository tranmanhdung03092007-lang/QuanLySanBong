<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/jsp/common/header.jspf" %>
<div class="container">
<h1>Sân Bóng Hoa Sen</h1>
<p>Đặt sân nhanh chóng, chọn khung giờ phù hợp.</p>
<p><a class="btn" href="${pageContext.request.contextPath}/booking">Đặt lịch ngay</a></p>
<h2>Sân đang phục vụ</h2>
<div style="display:flex;flex-wrap:wrap;gap:10px;">
<c:forEach items="${fields}" var="f" varStatus="st">
<c:if test="${st.index < 4}">
<div class="card" style="flex:1;min-width:150px;max-width:200px;">
<b>${f.ten_san}</b><br><small>${f.loai_san}</small><br>
${f.gia_theo_gio} VNĐ/giờ<br>
<a class="btn" href="${pageContext.request.contextPath}/booking">Đặt sân</a>
</div>
</c:if>
</c:forEach>
</div>
</div>
<%@ include file="/jsp/common/footer.jspf" %>