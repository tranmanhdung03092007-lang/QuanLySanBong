<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/jsp/common/header.jspf" %>
<style>
    body {
        background-repeat: no-repeat;      /* Không lặp */
        background-size: cover;            /* Phủ kín */
        background-position: center;       /* Căn giữa */
        background-attachment: fixed;      /* Cuộn nền cố định */
    }
.hero-section {
    background: linear-gradient(135deg, #1565c0 0%, #0d47a1 100%);
    color: white;
    padding: 60px 20px;
    text-align: center;
    border-radius: 0 0 30px 30px;
    margin-bottom: 40px;
    box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}
.hero-section h1 { font-size: 2.5em; margin-bottom: 10px; }
.hero-section p { font-size: 1.2em; opacity: 0.9; }

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 20px;
    margin-top: -30px;
    padding: 0 20px;
}
.stat-card {
    background: white;
    padding: 20px;
    border-radius: 12px;
    text-align: center;
    box-shadow: 0 4px 10px rgba(0,0,0,0.05);
    transition: transform 0.3s;
}
.stat-card:hover { transform: translateY(-5px); }
.stat-card i { font-size: 24px; display: block; margin-bottom: 10px; }
.stat-card .value { font-size: 20px; font-weight: bold; display: block; }
.stat-card .label { color: #666; font-size: 14px; }

.fields-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 25px;
    padding: 20px;
}
.field-card {
    background: white;
    border-radius: 15px;
    overflow: hidden;
    box-shadow: 0 5px 15px rgba(0,0,0,0.08);
    transition: all 0.3s ease;
    border: 1px solid #eee;
}
.field-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 25px rgba(0,0,0,0.15);
}
.field-card-img {
    height: 150px;
    background: #e3f2fd;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 50px;
}
.field-card-content { padding: 20px;}
.field-card-content h3 { margin: 0 0 10px 0; color: #1565c0; }
.field-card-content p { color: #666; margin: 5px 0; font-size: 14px; }
.field-price { font-size: 18px; color: #d32f2f; font-weight: bold; margin: 15px 0; }
.field-card-btn {
    display: block;
    width: 95%;
    margin: 0 auto;
    text-align: center;
    background: #1565c0;
    color: white;
    padding: 12px;
    text-decoration: none;
    border-radius: 8px;
    font-weight: bold;
    transition: background 0.2s;
}
.field-card-btn:hover { background: #0d47a1; }
</style>
<body>
<div class="hero-section">
    <h1>⚽ Sân Bóng Hoa Sen</h1>
    <p>Hệ thống đặt sân hiện đại - Uy tín - Chất lượng hàng đầu</p>
    <div style="margin-top:25px;">
        <a href="${pageContext.request.contextPath}/booking" class="btn" style="background:#ffca28;color:#333;font-weight:bold;padding:12px 30px;border-radius:25px;text-decoration:none;">ĐẶT LỊCH NGAY</a>
    </div>
</div>

<div class="container">
    <div class="stats-grid">
        <div class="stat-card">
            <span style="font-size:30px;">✅</span>
            <span class="value">${stats.free}</span>
            <span class="label">Sân trống</span>
        </div>
        <div class="stat-card">
            <span style="font-size:30px;">⏳</span>
            <span class="value">${stats.pending}</span>
            <span class="label">Đang chờ</span>
        </div>
        <div class="stat-card">
            <span style="font-size:30px;">🏃</span>
            <span class="value">${stats.booked}</span>
            <span class="label">Đã đặt</span>
        </div>
        <div class="stat-card">
            <span style="font-size:30px;">⛔</span>
            <span class="value">${stats.expired}</span>
            <span class="label">Hết</span>
        </div>
    </div>

</div>
</body>
<%@ include file="/jsp/common/footer.jspf" %>