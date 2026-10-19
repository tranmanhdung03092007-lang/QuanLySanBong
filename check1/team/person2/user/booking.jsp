<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
.booking-layout{display:flex;gap:16px;align-items:flex-start;max-width:1600px;margin:0 auto;}
.booking-form-panel{flex:0 0 30%;max-width:320px;position:sticky;top:10px;}
.booking-table-panel{flex:1;min-width:0;}
.booking-table table{font-size:13px;border-collapse:collapse;width:100%;}
.booking-table th,.booking-table td{border:1px solid #ccc;padding:8px;text-align:center;vertical-align:top;min-width:90px;}
.booking-table th{background:#f5f5f5;font-weight:bold;}
.booking-table td.free{background:#fff;color:#333;cursor:pointer;}
.booking-table td.pending{background:#ffd54f;color:#333;}
.booking-table td.booked{background:#ef5350;color:#fff;}
.booking-table td.event{background:#ce93d8;color:#333;}
.services-list{margin:10px 0;padding:10px;border:1px solid #ddd;background:#fafafa;}
.service-item{margin:6px 0;display:flex;align-items:center;gap:6px;}
.service-item input[type="checkbox"]{width:18px;height:18px;}
.service-qty{width:50px;padding:4px;text-align:center;}
.date-nav{display:flex;align-items:center;gap:10px;margin-bottom:10px;flex-wrap:wrap;}
.date-search{border:2px solid #007bff;padding:10px 14px;font-size:18px;border-radius:6px;width:200px;}
.btn-nav{background:#007bff;color:#fff;border:none;padding:10px 18px;border-radius:4px;cursor:pointer;font-size:15px;}
.btn-nav:hover{background:#0056b3;}
.calendar-week{display:flex;gap:8px;margin:10px 0;}
.day-item{flex:1;text-align:center;padding:12px 8px;border:2px solid #ddd;border-radius:6px;cursor:pointer;background:#fff;transition:all 0.2s;min-width:60px;}
.day-item:hover{border-color:#007bff;transform:scale(1.05);}
.day-item.selected{border-color:#007bff;border-width:3px;background:#e3f2fd;transform:scale(1.15);font-weight:bold;box-shadow:0 2px 8px rgba(0,123,255,0.3);}
.day-name{font-size:13px;color:#666;margin-bottom:4px;}
.day-number{font-size:18px;}
.day-item.selected .day-number{font-size:24px;color:#007bff;}
.slot-block{padding:4px;margin:2px 0;border-radius:3px;font-size:12px;text-align:center;}
.slot-block.selected{outline:2px solid #2196f3;}
</style>
<div style="padding:0 12px;">
<p><a class="btn" href="${pageContext.request.contextPath}/home">&larr; Về</a></p>
<div class="date-nav">
<button type="button" class="btn-nav" id="prev-week">&larr; Tuần trước</button>
<input type="date" id="booking-date" class="date-search" min="${today}" value="${viewDate}" style="width:auto;">
<button type="button" class="btn-nav" id="next-week">Tuần sau &rarr;</button>
<div style="margin-left:auto;font-size:18px;font-weight:bold;color:#007bff;" id="selected-day-display"></div>
</div>
<div class="calendar-week" id="week-calendar"></div>
<h3>Đặt lịch</h3>
<c:if test="${not empty error}"><div class="alert">${error}</div></c:if>
<c:choose>
<c:when test="${empty fields}"><div class="alert">Chưa có dữ liệu sân.</div></c:when>
<c:otherwise>
<div class="booking-layout">
<div class="booking-form-panel">
<div style="border:1px solid #999;padding:12px;background:#fff;">
<h4>Thông tin đặt sân</h4>
<p><b>Sân:</b> <span id="info-field">--</span></p>
<p><b>Ngày:</b> <span id="info-date">${viewDate}</span></p>
<p><b>Giờ:</b> <span id="info-time">--</span></p>
<div class="services-list">
<b>Dịch vụ đi kèm:</b>
<c:forEach items="${services}" var="sv">
<div class="service-item">
<input type="checkbox" class="service-check" data-name="${sv.ten_dv}" data-price="${sv.don_gia}" id="sv_${sv.ma_dv}">
<label for="sv_${sv.ma_dv}" style="margin:0;font-weight:normal;">${sv.ten_dv} — <fmt:formatNumber value="${sv.don_gia}" type="number"/>đ</label>
<input type="number" class="service-qty" data-service="${sv.ma_dv}" value="1" min="1" max="10">
</div>
</c:forEach>
</div>
<div id="form-error" class="alert" style="display:none;"></div>
<form id="booking-form">
<input type="hidden" id="form-field" name="field">
<input type="hidden" id="form-date" name="date" value="${viewDate}">
<input type="hidden" id="form-slot" name="slot">
<label>Họ và tên <span style="color:red">*</span></label>
<input type="text" id="form-name" name="name" placeholder="Họ tên" required style="width:100%;padding:8px;margin:4px 0;">
<label>Số điện thoại <span style="color:red">*</span></label>
<input type="tel" id="form-phone" name="phone" placeholder="Số điện thoại" required style="width:100%;padding:8px;margin:4px 0;">
<label>Tổng tiền:</label>
<p><b id="total-price">0</b> VNĐ</p>
<p><button type="submit" class="btn btn-primary" id="btn-confirm" disabled style="padding:10px 20px;">Xác nhận đặt</button>
<button type="button" class="btn" id="btn-direct" style="padding:10px 20px;">Đặt trực tiếp</button></p>
</form>
</div>
</div>
<div class="booking-table-panel">
<div style="overflow-x:auto;">
<table class="booking-table" id="schedule-table">
<thead><tr><th>Sân</th>
<c:forEach items="${days}" var="d">
<th>${dayAbbr[d]}<br><small>${fn:substring(d,8,10)}/${fn:substring(d,5,7)}</small></th>
</c:forEach>
</tr></thead>
<tbody>
<c:forEach items="${fields}" var="f">
<tr>
<th>${f.ten_san}</th>
<c:forEach items="${days}" var="day">
<td>
<c:forEach items="${weeklyGrid[day]}" var="s">
<c:if test="${s.ma_san == f.ma_san}">
<c:set var="sc" value="${s.status_code}"/>
<div class="slot-block ${sc==0?'free':sc==1?'pending':sc==2?'booked':'event'}"
style="cursor:${sc==0?'pointer':'default'};"
data-field="${f.ma_san}" data-field-name="${f.ten_san}"
data-slot="${s.so_ca}" data-time="${s.nhan_ca}" data-status="${sc}"
data-price="${f.gia_theo_gio}" data-date="${day}">
${s.gio_bat_dau}-${s.gio_ket_thuc}
<c:if test="${sc==1}"> (Giữ)</c:if>
<c:if test="${sc==2}"> (Đã đặt)</c:if>
</div>
</c:if>
</c:forEach>
</td>
</c:forEach>
</tr>
</c:forEach>
</tbody></table></div>
<p><span style="display:inline-block;width:12px;height:12px;border:1px solid #999;background:#fff"></span> Trống
<span style="display:inline-block;width:12px;height:12px;background:#ffd54f"></span> Giữ
<span style="display:inline-block;width:12px;height:12px;background:#ef5350;color:#fff"></span> Đã đặt
<span style="display:inline-block;width:12px;height:12px;background:#ce93d8"></span> Sự kiện</p>
</div>
</div>
</c:otherwise>
</c:choose>
</div>
<script>
window.BOOKING_CTX='${pageContext.request.contextPath}';
window.BOOKING_DATE='${viewDate}';
window.DAY_ABBR=<c:out value="${dayAbbrJson}" escapeXml="false"/>;
</script>
<%@ include file="/jsp/common/footer.jspf" %>
<script src="${pageContext.request.contextPath}/js/booking.js"></script>
<script>
(function(){
    var days = ['CN','T2','T3','T4','T5','T6','T7'];
    var dateInput = document.getElementById('booking-date');
    var weekDiv = document.getElementById('week-calendar');
    var selectedDisplay = document.getElementById('selected-day-display');
    if (!dateInput || !weekDiv) return;
    function renderWeek() {
        var val = dateInput.value;
        if (!val) return;
        var base = new Date(val + 'T00:00:00');
        var dow = base.getDay();
        var monday = new Date(base);
        monday.setDate(base.getDate() - (dow === 0 ? 6 : dow - 1));
        weekDiv.innerHTML = '';
        for (var i = 0; i < 7; i++) {
            var d = new Date(monday);
            d.setDate(monday.getDate() + i);
            var iso = d.toISOString().slice(0,10);
            var div = document.createElement('div');
            div.className = 'day-item' + (iso === val ? ' selected' : '');
            div.innerHTML = '<div class="day-name">' + days[d.getDay()] + '</div>' +
                            '<div class="day-number">' + d.getDate() + '/' + (d.getMonth()+1) + '</div>';
            div.onclick = function(){ dateInput.value = iso; dateInput.dispatchEvent(new Event('change')); };
            weekDiv.appendChild(div);
        }
        if (selectedDisplay) selectedDisplay.textContent = days[dow] + ' ' + base.getDate() + '/' + (base.getMonth()+1);
    }
    dateInput.addEventListener('change', function(){
        document.getElementById('info-date').textContent = this.value || '';
        renderWeek();
    });
    document.getElementById('prev-week').addEventListener('click', function(){
        var d = new Date(dateInput.value);
        d.setDate(d.getDate() - 7);
        dateInput.value = d.toISOString().slice(0,10);
        dateInput.dispatchEvent(new Event('change'));
    });
    document.getElementById('next-week').addEventListener('click', function(){
        var d = new Date(dateInput.value);
        d.setDate(d.getDate() + 7);
        dateInput.value = d.toISOString().slice(0,10);
        dateInput.dispatchEvent(new Event('change'));
    });
    renderWeek();
})();
</script>