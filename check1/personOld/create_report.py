"""
Script tạo báo cáo dự án "Xây dựng hệ thống quản lý sân bóng Hoa Sen"
Dựa trên template từ báo cáo gốc, nội dung kiểm tra từ project và SQL.
"""
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import os

# ============================================================
# HÀM TIỆN ÍCH
# ============================================================

def set_cell_shading(cell, color):
    """Đặt màu nền cho ô bảng."""
    shading = OxmlElement('w:shd')
    shading.set(qn('w:fill'), color)
    shading.set(qn('w:val'), 'clear')
    cell._tc.get_or_add_tcPr().append(shading)

def add_heading_center(doc, text, size=14, bold=True):
    """Thêm tiêu đề căn giữa."""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(text)
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = 'Times New Roman'
    return p

def add_heading_left(doc, text, size=14, bold=True):
    """Thêm tiêu đề căn trái."""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    run = p.add_run(text)
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = 'Times New Roman'
    return p

def add_paragraph(doc, text, size=13, bold=False, align=WD_ALIGN_PARAGRAPH.JUSTIFY):
    """Thêm đoạn văn bản."""
    p = doc.add_paragraph()
    p.alignment = align
    p.paragraph_format.line_spacing = 1.3
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run(text)
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = 'Times New Roman'
    return p

def add_table(doc, headers, rows, col_widths=None):
    """Thêm bảng với tiêu đề và dữ liệu."""
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Header
    for i, h in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.text = h
        set_cell_shading(cell, 'D9E2F3')
        for paragraph in cell.paragraphs:
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in paragraph.runs:
                run.bold = True
                run.font.size = Pt(12)
                run.font.name = 'Times New Roman'
    
    # Data
    for r_idx, row in enumerate(rows):
        for c_idx, val in enumerate(row):
            cell = table.rows[r_idx + 1].cells[c_idx]
            cell.text = str(val)
            for paragraph in cell.paragraphs:
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for run in paragraph.runs:
                    run.font.size = Pt(12)
                    run.font.name = 'Times New Roman'
    
    if col_widths:
        for i, w in enumerate(col_widths):
            for row in table.rows:
                row.cells[i].width = Cm(w)
    
    return table

def add_page_break(doc):
    """Thêm ngắt trang."""
    doc.add_page_break()

# ============================================================
# TẠO TÀI LIỆU
# ============================================================

doc = Document()

# Cấu hình font mặc định
style = doc.styles['Normal']
font = style.font
font.name = 'Times New Roman'
font.size = Pt(13)

# ============================================================
# TRANG BÌA
# ============================================================

for _ in range(3):
    doc.add_paragraph()

add_heading_center(doc, 'TRƯỜNG CAO ĐẲNG FPT POLYTECHNIC', 16, True)
doc.add_paragraph()
add_heading_center(doc, 'BÁO CÁO DỰ ÁN 1', 18, True)
doc.add_paragraph()
add_heading_center(doc, 'ĐỀ TÀI: XÂY DỰNG HỆ THỐNG QUẢN LÝ SÂN BÓNG HOA SEN', 16, True)

for _ in range(4):
    doc.add_paragraph()

# Thông tin giảng viên và nhóm
info_lines = [
    ('Giảng viên hướng dẫn:', 'Nguyễn Thúy Hằng'),
    ('Chuyên ngành:', 'Phát triển phần mềm'),
    ('Nhóm thực hiện:', 'SD21304 – Nhóm 2'),
]
for label, value in info_lines:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(f'{label} {value}')
    run.font.size = Pt(14)
    run.font.name = 'Times New Roman'

doc.add_paragraph()
add_heading_center(doc, 'Sinh viên thực hiện:', 14, True)

students = [
    ('Phạm Đức Mạnh', 'PH66001'),
    ('Trần Ngọc Thành', 'PH65891'),
    ('Ngô Ngọc Đại', 'PH65874'),
    ('Ngô Ngọc Duy', 'PH65875'),
    ('Trần Thị Thu Phương', 'PH66986'),
]
for name, mssv in students:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(f'{name}               {mssv}')
    run.font.size = Pt(14)
    run.font.name = 'Times New Roman'

for _ in range(3):
    doc.add_paragraph()

add_heading_center(doc, 'Hà Nội – 2026', 14, True)

add_page_break(doc)

# ============================================================
# MỤC LỤC
# ============================================================

add_heading_center(doc, 'MỤC LỤC', 16, True)
doc.add_paragraph()

toc_items = [
    'QUY ƯỚC TÀI LIỆU',
    'BẢNG CHÚ GIẢI THUẬT NGỮ',
    'DANH SÁCH THÀNH VIÊN',
    'GIẢNG VIÊN HƯỚNG DẪN',
    'LỜI MỞ ĐẦU',
    'TÓM TẮT NỘI DUNG DỰ ÁN',
    'CHƯƠNG 1: GIỚI THIỆU',
    '1.1. Bối cảnh - hiện trạng',
    '1.2. Mục tiêu - Phạm vi',
    '1.3. Nguồn lực - Kế hoạch',
    '1.4. Khảo sát',
    'CHƯƠNG 2: PHÂN TÍCH',
    '2.1. Danh sách chức năng',
    '2.2. Quan hệ thực thể',
    'CHƯƠNG 3: THIẾT KẾ',
    '3.1. Cơ sở dữ liệu',
    '3.2. Giao diện người dùng',
    'CHƯƠNG 4: TỔNG KẾT DỰ ÁN',
    'CHƯƠNG 5: HƯỚNG DẪN SỬ DỤNG',
    'PHỤ LỤC A: ĐẶC TẢ BẢNG',
]
for item in toc_items:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    run = p.add_run(item)
    run.font.size = Pt(13)
    run.font.name = 'Times New Roman'

add_page_break(doc)

# ============================================================
# QUY ƯỚC TÀI LIỆU
# ============================================================

add_heading_center(doc, 'QUY ƯỚC TÀI LIỆU', 16, True)
doc.add_paragraph()

add_table(doc, ['Kích thước', 'Tiêu đề', 'Cỡ chữ', 'Kiểu chữ'], [
    ['Tiêu đề chính', 'Cỡ chữ 14', 'In đậm, viết hoa, căn giữa', 'Times New Roman'],
    ['Tiêu đề cấp 2', 'Cỡ chữ 14', 'In đậm, căn trái', 'Times New Roman'],
    ['Tiêu đề cấp 3', 'Cỡ chữ 14', 'In đậm, căn trái', 'Times New Roman'],
    ['Tiêu đề thường', 'Cỡ chữ 14', 'In thường', 'Times New Roman'],
    ['Bảng', 'Tự khớp Window', 'Chính giữa/Sang trái', 'Times New Roman'],
    ['Đoạn văn', 'Căn đều 2 lề', 'Khoảng cách dòng 1.3', 'Times New Roman'],
])

add_page_break(doc)

# ============================================================
# BẢNG CHÚ GIẢI THUẬT NGỮ
# ============================================================

add_heading_center(doc, 'BẢNG CHÚ GIẢI THUẬT NGỮ', 16, True)
doc.add_paragraph()

add_table(doc, ['STT', 'Thuật ngữ', 'Giải thích'], [
    ['1', 'MVC (Model-View-Controller)', 'Mô hình kiến trúc phần mềm tách ứng dụng thành ba thành phần: Model (xử lý dữ liệu), View (giao diện hiển thị) và Controller (điều phối luồng xử lý), giúp mã nguồn dễ bảo trì và mở rộng.'],
    ['2', 'Servlet', 'Thành phần Java chạy trên máy chủ, tiếp nhận và xử lý các yêu cầu (request) từ trình duyệt, sau đó trả về phản hồi (response) cho người dùng.'],
    ['3', 'JSP (JavaServer Pages)', 'Công nghệ cho phép nhúng mã Java vào trang HTML để tạo giao diện web động phía server.'],
    ['4', 'SQL Server', 'Hệ quản trị cơ sở dữ liệu quan hệ (RDBMS) do Microsoft phát triển, được sử dụng để lưu trữ và quản lý dữ liệu của hệ thống Sân Bóng Hoa Sen.'],
    ['5', 'CRUD', 'Viết tắt của bốn thao tác cơ bản trên dữ liệu: Create (thêm), Read (đọc), Update (sửa), Delete (xóa).'],
    ['6', 'Use Case (Ca sử dụng)', 'Mô tả một chức năng cụ thể của hệ thống dưới góc nhìn tương tác giữa người dùng (actor) và hệ thống nhằm đạt được một mục tiêu nhất định.'],
    ['7', 'Actor (Tác nhân)', 'Người dùng hoặc hệ thống bên ngoài tương tác trực tiếp với hệ thống, ví dụ: Khách hàng, Nhân viên, Quản lý.'],
    ['8', 'ERD (Entity Relationship Diagram)', 'Sơ đồ thực thể - quan hệ, mô tả các bảng dữ liệu (thực thể) và mối quan hệ giữa chúng trong cơ sở dữ liệu.'],
    ['9', 'Entity (Thực thể)', 'Đối tượng dữ liệu được lưu trữ trong hệ thống, tương ứng với một bảng trong cơ sở dữ liệu (ví dụ: khách hàng, sân bóng, hóa đơn).'],
    ['10', 'PK (Primary Key - Khóa chính)', 'Cột hoặc tập hợp cột dùng để xác định duy nhất một dòng dữ liệu trong bảng.'],
    ['11', 'FK (Foreign Key - Khóa ngoại)', 'Cột tham chiếu đến khóa chính của một bảng khác, dùng để thiết lập mối quan hệ giữa các bảng.'],
    ['12', 'IDENTITY', 'Thuộc tính của cột trong SQL Server cho phép giá trị tự động tăng dần khi thêm dòng dữ liệu mới, thường dùng cho khóa chính.'],
    ['13', 'Voucher / Phiếu giảm giá', 'Mã ưu đãi cho phép khách hàng được giảm giá khi thanh toán đơn hàng, có thể giới hạn theo thời gian, số lượng hoặc giá trị đơn hàng tối thiểu.'],
    ['14', 'Tồn kho', 'Số lượng sản phẩm còn lại trong kho tại một thời điểm, được cập nhật mỗi khi có giao dịch nhập hoặc xuất hàng.'],
    ['15', 'Database (Cơ sở dữ liệu)', 'Tập hợp dữ liệu có tổ chức, được lưu trữ và quản lý có hệ thống để phục vụ cho việc truy xuất và xử lý của ứng dụng.'],
])

add_page_break(doc)

# ============================================================
# DANH SÁCH THÀNH VIÊN
# ============================================================

add_heading_center(doc, 'DANH SÁCH THÀNH VIÊN', 16, True)
doc.add_paragraph()

add_table(doc, ['STT', 'Họ tên', 'MSSV', 'SĐT', 'Email'], [
    ['1', 'Phạm Đức Mạnh', 'PH66001', '0862715970', 'manhzata2007@gmail.com'],
    ['2', 'Trần Ngọc Thành', 'PH65891', '0358176805', 'ngocthanhtran847@gmail.com'],
    ['3', 'Ngô Ngọc Đại', 'PH65874', '0766454239', 'ngodai131@gmail.com'],
    ['4', 'Ngô Ngọc Duy', 'PH65875', '0906015767', 'phamvananhhp96@gmail.com'],
    ['5', 'Trần Thị Thu Phương', 'PH66986', '0327544823', 'tranphuongngt2207@gmail.com'],
])

doc.add_paragraph()
add_paragraph(doc, 'Nhóm dự án cam kết toàn bộ thông tin, nội dung và kết quả trình bày trong tài liệu này là trung thực, phản ánh đúng quá trình thực hiện của các thành viên trong nhóm. Nhóm đảm bảo không sao chép hoặc vi phạm bản quyền đối với bất kỳ tài liệu, công trình hay sản phẩm nào của cá nhân hoặc tổ chức khác. Nếu phát hiện có hành vi sao chép hoặc vi phạm bản quyền, nhóm xin hoàn toàn chịu trách nhiệm và chấp nhận mọi hình thức xử lý, bao gồm việc hủy kết quả của dự án.')

add_page_break(doc)

# ============================================================
# GIẢNG VIÊN HƯỚNG DẪN
# ============================================================

add_heading_center(doc, 'GIẢNG VIÊN HƯỚNG DẪN', 16, True)
doc.add_paragraph()

add_paragraph(doc, 'Họ và tên: Nguyễn Thúy Hằng', 13, True)
add_paragraph(doc, 'Cơ quan tác vụ: Trường CĐ FPT Polytechnic')
add_paragraph(doc, 'Điện thoại: 0942578685 \t\t Email: hangnt169@fe.edu.vn')
doc.add_paragraph()
add_paragraph(doc, 'Ý kiến nhận xét, đánh giá của cán bộ hướng dẫn:')
doc.add_paragraph()
doc.add_paragraph()
doc.add_paragraph()
add_paragraph(doc, 'Giảng viên hướng dẫn', 13, True, WD_ALIGN_PARAGRAPH.RIGHT)
add_paragraph(doc, '(Ký và ghi rõ họ tên)', 13, False, WD_ALIGN_PARAGRAPH.RIGHT)
doc.add_paragraph()
add_paragraph(doc, 'Nguyễn Thúy Hằng', 13, True, WD_ALIGN_PARAGRAPH.RIGHT)

add_page_break(doc)

# ============================================================
# LỜI MỞ ĐẦU
# ============================================================

add_heading_center(doc, 'LỜI MỞ ĐẦU', 16, True)
doc.add_paragraph()

add_paragraph(doc, 'Trong những năm gần đây, cùng với sự phát triển mạnh mẽ của công nghệ thông tin và xu hướng chuyển đổi số, việc ứng dụng các hệ thống quản lý vào lĩnh vực thể thao và giải trí ngày càng trở nên phổ biến. Đối với các sân bóng đá, việc số hóa quy trình đặt sân trực tuyến, quản lý lịch thi đấu, khách hàng và vận hành hoạt động kinh doanh không chỉ giúp nâng cao hiệu quả quản lý mà còn cải thiện đáng kể trải nghiệm của khách hàng. Người dùng hiện nay có xu hướng ưu tiên những dịch vụ cho phép đặt sân trực tuyến nhanh chóng, tra cứu thông tin lịch trống dễ dàng, được tư vấn chuyên nghiệp và có đa dạng lựa chọn thanh toán ngay từ khi đặt lịch.')

add_paragraph(doc, 'Tuy nhiên, trên thực tế nhiều sân bóng vẫn quản lý bằng phương pháp thủ công hoặc sử dụng nhiều công cụ riêng lẻ, dẫn đến việc dữ liệu không được đồng bộ và khó kiểm soát. Điều này dễ gây ra các vấn đề như quản lý lịch đặt sân không chính xác, khó theo dõi tình trạng sân trống, mất thời gian trong quá trình lập hóa đơn, quản lý danh mục sân, giá cả và chương trình khuyến mãi. Bên cạnh đó, việc quản lý khách hàng, nhân viên và thống kê doanh thu cũng gặp nhiều hạn chế, ảnh hưởng đến hiệu quả vận hành và chất lượng dịch vụ của sân bóng.')

add_paragraph(doc, 'Xuất phát từ những yêu cầu đó, nhóm đã thực hiện đề tài "Xây dựng hệ thống quản lý sân bóng Hoa Sen". Hệ thống được xây dựng nhằm hỗ trợ sân bóng quản lý toàn diện các hoạt động từ đặt sân trực tuyến, tiếp nhận đơn đặt tại quầy, quản lý hóa đơn, danh mục sân, lịch đặt, khách hàng, nhân viên đến các chương trình khuyến mãi và thống kê doanh thu. Bên cạnh đó, hệ thống còn áp dụng cơ chế phân quyền giữa Quản trị viên và Nhân viên, góp phần đảm bảo tính bảo mật, thuận tiện và phù hợp với quy trình vận hành thực tế của sân bóng.')

add_paragraph(doc, 'Đối với quản trị viên và nhân viên: hệ thống hỗ trợ quản lý đơn đặt sân, tiếp nhận khách hàng, lập và quản lý hóa đơn, quản lý lịch sân, danh mục sân, loại sân, dịch vụ đi kèm, khách hàng, nhân viên, phiếu giảm giá, chương trình khuyến mãi và thống kê doanh thu. Việc tập trung dữ liệu trên một hệ thống duy nhất giúp quá trình quản lý trở nên thuận tiện, chính xác và giảm thiểu sai sót trong quá trình vận hành.')

add_page_break(doc)

# ============================================================
# TÓM TẮT NỘI DUNG DỰ ÁN
# ============================================================

add_heading_center(doc, 'TÓM TẮT NỘI DUNG DỰ ÁN', 16, True)
doc.add_paragraph()

add_paragraph(doc, 'Dự án Xây dựng hệ thống quản lý sân bóng Hoa Sen được thực hiện nhằm giải quyết những hạn chế trong công tác quản lý và vận hành của các sân bóng hiện nay. Khi nhu cầu đặt sân trực tuyến, quản lý lịch thi đấu và trải nghiệm dịch vụ ngày càng tăng, việc áp dụng một hệ thống quản lý tập trung sẽ giúp tối ưu quy trình đặt sân, giảm thiểu sai sót và nâng cao chất lượng dịch vụ dành cho khách hàng.')

add_paragraph(doc, 'Mục tiêu của hệ thống là xây dựng một nền tảng quản lý và đặt sân trực tuyến toàn diện, kết nối hiệu quả giữa khách hàng và sân bóng, đồng thời hỗ trợ số hóa các hoạt động quản lý trong quá trình vận hành.')

add_paragraph(doc, 'Về phía quản trị viên (Admin/Nhân viên): Hệ thống hỗ trợ quản lý toàn diện các nghiệp vụ của sân bóng, bao gồm:', 13, True)

add_paragraph(doc, '• Quản lý đặt sân tại quầy và đặt sân trực tuyến.')
add_paragraph(doc, '• Quản lý hóa đơn và chứng từ bán hàng.')
add_paragraph(doc, '• Quản lý lịch sân và tình trạng sân trống.')
add_paragraph(doc, '• Quản lý danh mục sân, loại sân, dịch vụ đi kèm.')
add_paragraph(doc, '• Quản lý khách hàng và nhân viên.')
add_paragraph(doc, '• Quản lý phiếu giảm giá (Voucher).')
add_paragraph(doc, '• Thống kê doanh thu và báo cáo hoạt động kinh doanh.')

add_paragraph(doc, 'Thông qua việc ứng dụng công nghệ vào quy trình quản lý và đặt sân, hệ thống giúp giảm thiểu sai sót trong quá trình phục vụ, nâng cao hiệu quả quản lý, tối ưu thời gian xử lý nghiệp vụ và cải thiện trải nghiệm của khách hàng. Đồng thời, đây cũng là cơ hội để nhóm vận dụng những kiến thức đã học về phân tích, thiết kế, phát triển và kiểm thử phần mềm vào một bài toán quản lý thực tế trong lĩnh vực thể thao.')

add_page_break(doc)

# ============================================================
# CHƯƠNG 1: GIỚI THIỆU
# ============================================================

add_heading_center(doc, 'CHƯƠNG 1: GIỚI THIỆU', 16, True)
doc.add_paragraph()

add_heading_left(doc, '1.1. Bối cảnh - hiện trạng', 14, True)

add_paragraph(doc, 'Trong bối cảnh chuyển đổi số, việc ứng dụng công nghệ vào hoạt động kinh doanh sân bóng ngày càng cần thiết. Đối với các sân bóng, lượng khách thường tập trung vào mùa cao điểm, cuối tuần và dịp lễ, khiến nhu cầu đặt sân trực tuyến và tư vấn dịch vụ tăng cao. Khách hàng không chỉ mong muốn đặt được sân chất lượng mà còn kỳ vọng có thể đặt sân trực tuyến nhanh chóng, tìm kiếm lịch trống phù hợp và được tư vấn chuyên nghiệp.')

add_paragraph(doc, 'Tuy nhiên, nhiều sân bóng vừa và nhỏ vẫn đang quản lý theo phương pháp thủ công hoặc sử dụng nhiều công cụ riêng lẻ, khiến dữ liệu không được đồng bộ và khó kiểm soát. Điều này dẫn đến nhiều hạn chế như:')

add_paragraph(doc, '• Dễ xảy ra tình trạng nhầm lẫn hoặc sai sót trong quá trình đặt sân và cập nhật lịch.')
add_paragraph(doc, '• Khó theo dõi tình trạng sân trống, sân đã đặt và yêu cầu quản lý lịch.')
add_paragraph(doc, '• Việc quản lý hóa đơn, khách hàng, nhân viên và các chương trình khuyến mãi còn mất nhiều thời gian.')
add_paragraph(doc, '• Công tác thống kê doanh thu, phân tích sân được đặt nhiều và theo dõi hoạt động kinh doanh chưa được thực hiện hiệu quả.')

add_paragraph(doc, 'Xuất phát từ những hạn chế trên, dự án Xây dựng hệ thống quản lý sân bóng Hoa Sen được xây dựng nhằm hỗ trợ số hóa quy trình quản lý và nâng cao chất lượng dịch vụ.')

add_paragraph(doc, 'Quản trị viên: Hệ thống hỗ trợ quản lý toàn bộ hoạt động của sân bóng như quản lý đơn đặt sân, tiếp nhận khách hàng, hóa đơn, lịch sân, danh mục sân, loại sân, giá cả, khách hàng, nhân viên, phiếu giảm giá, chương trình khuyến mãi và thống kê doanh thu.')

add_paragraph(doc, 'Nhân viên: Hệ thống hỗ trợ quản lý hoạt động hàng ngày như tiếp nhận khách hàng, xử lý đơn đặt sân, lập hóa đơn, cập nhật tình trạng lịch sân, quản lý danh mục sân, hỗ trợ tìm kiếm và tư vấn dịch vụ cho khách hàng, quản lý phiếu giảm giá, theo dõi chương trình khuyến mãi và thống kê kết quả kinh doanh.')

add_heading_left(doc, '1.2. Mục tiêu - Phạm vi', 14, True)

add_paragraph(doc, 'Mục tiêu của dự án là xây dựng một website đặt sân trực tuyến kết hợp hệ thống quản lý sân bóng, giúp tối ưu hóa quy trình vận hành và nâng cao trải nghiệm của khách hàng. Hệ thống hỗ trợ khách hàng tìm kiếm sân nhanh chóng, tra cứu thông tin lịch trống. Đồng thời, hệ thống cung cấp cho quản trị viên và nhân viên các công cụ quản lý tập trung để điều hành hoạt động của sân bóng một cách hiệu quả.')

add_paragraph(doc, 'Phía quản trị: Đối với quản trị viên và nhân viên, hệ thống cung cấp đầy đủ các chức năng phục vụ công tác quản lý và vận hành sân bóng, bao gồm:')

add_paragraph(doc, '+ Quản lý đặt sân tại quầy: Cho phép tiếp nhận, tạo mới, chỉnh sửa, hủy và tìm kiếm thông tin đơn đặt sân. Hệ thống hỗ trợ theo dõi trạng thái đơn đặt, thời gian đặt, số lượng sân và phân công xử lý phù hợp nhằm hạn chế tình trạng nhầm lẫn hoặc quá tải trong giờ cao điểm.')

add_paragraph(doc, '+ Xác nhận khách hàng: Hỗ trợ xác nhận khách hàng khi đặt sân tại sân bóng, cập nhật trạng thái đơn đặt và chuyển thông tin sang quy trình thanh toán, giúp nhân viên theo dõi chính xác tình trạng đơn đặt.')

add_paragraph(doc, '+ Quản lý lịch sân: Cho phép quản lý danh sách lịch đặt, loại sân, ca đặt và trạng thái sân. Thông tin được cập nhật theo thời gian thực để hỗ trợ việc kiểm soát lịch đặt, quản lý sân và tiếp thị.')

add_paragraph(doc, '+ Quản lý hóa đơn: Hỗ trợ tạo, cập nhật, tra cứu và in hóa đơn. Hệ thống cho phép áp dụng phiếu giảm giá, đợt giảm giá và lưu trữ toàn bộ lịch sử giao dịch phục vụ công tác quản lý.')

add_paragraph(doc, '+ Quản lý danh mục sân: Bao gồm quản lý danh mục sân, loại sân, ca đặt, giá cả và thông tin chi tiết sân. Người quản trị có thể thêm mới, chỉnh sửa, ngừng kinh doanh hoặc cập nhật thông tin sân khi cần thiết.')

add_paragraph(doc, '+ Quản lý khách hàng và nhân viên: Hệ thống lưu trữ thông tin khách hàng và nhân viên, hỗ trợ tìm kiếm, cập nhật dữ liệu, phân quyền tài khoản và quản lý lịch làm việc của nhân viên.')

add_paragraph(doc, '+ Quản lý phiếu giảm giá: Cho phép tạo và quản lý các phiếu giảm giá, điều kiện áp dụng, thời gian hiệu lực và số lượng sử dụng, giúp triển khai các chương trình khuyến mãi dành cho khách hàng.')

add_paragraph(doc, '+ Thống kê và báo cáo: Cung cấp các biểu đồ và báo cáo doanh thu theo ngày, tháng, năm cùng các số liệu về hóa đơn, sân được đặt nhiều và hoạt động kinh doanh, hỗ trợ nhà quản lý đưa ra quyết định phù hợp.')

add_heading_left(doc, '1.3. Nguồn lực - Kế hoạch', 14, True)

add_heading_left(doc, '1.3.1. Nguồn lực', 14, True)

add_paragraph(doc, 'Nguồn lực:', 13, True)

add_table(doc, ['STT', 'Họ và tên', 'Nhiệm vụ'], [
    ['1', 'Phạm Đức Mạnh', 'Leader, Developer'],
    ['2', 'Trần Ngọc Thành', 'Developer'],
    ['3', 'Ngô Ngọc Đại', 'Developer'],
    ['4', 'Ngô Ngọc Duy', 'Developer'],
    ['5', 'Trần Thị Thu Phương', 'Developer'],
])

add_heading_left(doc, '1.3.2. Kế hoạch', 14, True)

add_table(doc, ['STT', 'Tên công việc', 'Ngày bắt đầu', 'Ngày kết thúc', 'Người thực hiện', 'Độ hoàn thành'], [
    ['1', 'Chọn đề tài', '30/06/2026', '15/08/2026', 'Cả nhóm', '100%'],
    ['2', 'Lập kế hoạch thực hiện dự án', '30/06/2026', '30/06/2026', 'Cả nhóm', '100%'],
    ['3', 'Khảo sát thực trạng quản lý sân bóng', '30/06/2026', '01/07/2026', 'Phạm Đức Mạnh', '100%'],
    ['4', 'Xác định yêu cầu nghiệp vụ dự án', '01/07/2026', '02/07/2026', 'Cả nhóm', '100%'],
    ['5', 'Phân tích luồng chức năng và Use Case', '02/07/2026', '04/07/2026', 'Trần Ngọc Thành', '100%'],
    ['6', 'Thiết kế sơ đồ ERD', '03/07/2026', '05/07/2026', 'Trần Ngọc Thành', '100%'],
    ['7', 'Thiết kế Database (chuẩn hóa 3NF)', '05/07/2026', '07/07/2026', 'Cả nhóm', '100%'],
    ['8', 'Code chức năng Quản lý sân', '08/07/2026', '14/07/2026', 'Phạm Đức Mạnh', '100%'],
    ['9', 'Code chức năng Quản lý nhân viên', '08/07/2026', '14/07/2026', 'Ngô Ngọc Đại', '100%'],
    ['10', 'Code chức năng Quản lý khách hàng', '08/07/2026', '16/07/2026', 'Ngô Ngọc Duy', '100%'],
    ['11', 'Code chức năng Đặt sân và Quản lý hóa đơn', '08/07/2026', '18/07/2026', 'Phạm Đức Mạnh', '100%'],
    ['12', 'Code chức năng Quản lý phiếu giảm giá', '08/07/2026', '16/07/2026', 'Trần Thị Thu Phương', '100%'],
    ['13', 'Code chức năng Thống kê doanh thu', '16/07/2026', '20/07/2026', 'Ngô Ngọc Đại', '100%'],
    ['14', 'Merge và kiểm tra tích hợp toàn bộ hệ thống', '20/07/2026', '25/07/2026', 'Cả nhóm', '100%'],
    ['15', 'Viết báo cáo dự án', '25/07/2026', '28/07/2026', 'Ngô Ngọc Duy', 'Đang thực hiện'],
])

add_heading_left(doc, '1.4. Khảo sát', 14, True)

add_heading_left(doc, '1.4.1. Khảo sát online tại KiotViet', 14, True)

add_paragraph(doc, 'Địa điểm: Online')
add_paragraph(doc, 'Kết luận khảo sát:')
add_paragraph(doc, '• Đặt sân và Mua sắm: Hỗ trợ đặt sân tại quầy và online, quản lý lịch đặt sân, danh sách chờ, thanh toán trước và ghép đơn.')
add_paragraph(doc, '• Quản lý sân: Đồng bộ danh mục sân, giá bán, quản lý lịch sân và theo dõi trạng thái theo ca, buổi.')
add_paragraph(doc, '• Quản lý hóa đơn: Quản lý hóa đơn từ tạm tính đến thanh toán, hỗ trợ tách hóa đơn, hoàn trả và lưu trữ dữ liệu.')
add_paragraph(doc, '• Khuyến mãi: Hỗ trợ voucher và các chương trình khuyến mãi theo đối tượng khách hàng (VIP, thành viên, mới).')
add_paragraph(doc, '• Thống kê: Tự động thống kê lượt đặt, doanh thu, sân được đặt nhiều nhất và xuất báo cáo trực quan.')

add_heading_left(doc, '1.4.2. Khảo sát online tại Sapo', 14, True)

add_paragraph(doc, 'Địa điểm: Online')
add_paragraph(doc, 'Kết luận khảo sát:')
add_paragraph(doc, '• Đặt sân và Mua sắm: Hỗ trợ đặt sân tại quầy và online, quản lý lịch đặt sân, danh sách chờ, thanh toán trước và ghép đơn.')
add_paragraph(doc, '• Quản lý sân: Đồng bộ danh mục sân, giá bán, quản lý lịch sân và theo dõi trạng thái theo ca, buổi.')
add_paragraph(doc, '• Quản lý hóa đơn: Quản lý hóa đơn từ tạm tính đến thanh toán, hỗ trợ tách hóa đơn, hoàn trả và lưu trữ dữ liệu.')
add_paragraph(doc, '• Khuyến mãi: Hỗ trợ voucher và các chương trình khuyến mãi theo đối tượng khách hàng (VIP, thành viên, mới).')
add_paragraph(doc, '• Thống kê: Tự động thống kê lượt đặt, doanh thu, sân được đặt nhiều nhất và xuất báo cáo trực quan.')

add_page_break(doc)

# ============================================================
# CHƯƠNG 2: PHÂN TÍCH
# ============================================================

add_heading_center(doc, 'CHƯƠNG 2: PHÂN TÍCH', 16, True)
doc.add_paragraph()

add_heading_left(doc, '2.1. Danh sách chức năng', 14, True)

add_table(doc, ['STT', 'Mã UC', 'Tên UC', 'Tác nhân', 'Mô tả'], [
    ['1', 'UC-01', 'Đặt sân trực tuyến', 'Khách hàng', 'Xem lịch sân, chọn sân và ca đặt, thêm dịch vụ, áp dụng voucher, xác nhận đặt sân.'],
    ['2', 'UC-02', 'Quản lý đặt sân tại quầy', 'Quản lý, Nhân viên', 'Tìm kiếm và chọn sân, chọn khách hàng, áp dụng phiếu giảm giá, chọn hình thức thanh toán và lập hóa đơn.'],
    ['3', 'UC-03', 'Quản lý hóa đơn', 'Quản lý, Nhân viên', 'Tìm kiếm, lọc hóa đơn theo mã hoặc số điện thoại khách hàng, theo khoảng ngày và theo trạng thái; xem chi tiết và cập nhật trạng thái.'],
    ['4', 'UC-04', 'Quản lý sân và lịch sân', 'Quản lý', 'Thêm, sửa, xóa, tìm kiếm và lọc sân theo trạng thái, loại sân, giá; quản lý lịch đặt theo ca, buổi.'],
    ['5', 'UC-05', 'Quản lý phiếu giảm giá', 'Quản lý', 'Thêm, sửa, xóa, tìm kiếm phiếu giảm giá theo loại (Hóa đơn, Sân, Dịch vụ), theo trạng thái hoạt động.'],
    ['6', 'UC-06', 'Quản lý nhân viên', 'Quản lý', 'Thêm, sửa, xóa, tìm kiếm tài khoản nhân viên; phân loại theo chức vụ (Quản lý, Quản trị viên, Nhân viên).'],
    ['7', 'UC-07', 'Quản lý khách hàng', 'Quản lý, Nhân viên', 'Thêm, sửa, tìm kiếm khách hàng; quản lý thông tin liên hệ.'],
    ['8', 'UC-08', 'Quản lý dịch vụ', 'Quản lý', 'Thêm, sửa, xóa, tìm kiếm dịch vụ đi kèm (nước uống, áo bib, bóng thi đấu).'],
    ['9', 'UC-09', 'Thống kê doanh thu', 'Quản lý', 'Xem tổng quan doanh thu, số đơn hàng, số khách hàng, số đơn hoàn hủy và biểu đồ doanh thu theo tháng/năm.'],
])

add_heading_left(doc, '2.2. Quan hệ thực thể', 14, True)

add_heading_left(doc, '2.2.1. Danh sách thực thể', 14, True)

add_table(doc, ['STT', 'Tên thực thể', 'Mô tả'], [
    ['1', 'Khách hàng', 'Cá nhân đặt sân và sử dụng dịch vụ.'],
    ['2', 'Nhân viên', 'Cá nhân làm việc tại sân bóng.'],
    ['3', 'Sân', 'Sân bóng được cho thuê theo giờ.'],
    ['4', 'Lịch sân', 'Lịch đặt sân theo ca, buổi trong ngày.'],
    ['5', 'Dịch vụ', 'Dịch vụ đi kèm khi đặt sân.'],
    ['6', 'Phiếu giảm giá', 'Mã giảm giá cho hóa đơn.'],
    ['7', 'Hóa đơn', 'Tài liệu ghi lại giao dịch đặt sân.'],
    ['8', 'Chi tiết hóa đơn', 'Chi tiết các hạng mục trong hóa đơn.'],
])

add_heading_left(doc, '2.2.2. Sơ đồ quan hệ thực thể', 14, True)

add_paragraph(doc, 'Sơ đồ quan hệ thực thể (ERD) của hệ thống được mô tả trong Phụ lục A. Các thực thể chính bao gồm: San, LichSan, KhachHang, NhanVien, DichVu, HoaDon, CTHD, Voucher. Mối quan hệ giữa các thực thể:')

add_paragraph(doc, '• San (1) - (N) LichSan: Một sân có nhiều lịch đặt.')
add_paragraph(doc, '• KhachHang (1) - (N) HoaDon: Một khách hàng có nhiều hóa đơn.')
add_paragraph(doc, '• NhanVien (1) - (N) HoaDon: Một nhân viên lập nhiều hóa đơn.')
add_paragraph(doc, '• HoaDon (1) - (N) CTHD: Một hóa đơn có nhiều chi tiết hóa đơn.')
add_paragraph(doc, '• LichSan (1) - (N) CTHD: Một lịch sân có thể thuộc nhiều chi tiết hóa đơn.')
add_paragraph(doc, '• DichVu (1) - (N) CTHD: Một dịch vụ có thể xuất hiện trong nhiều chi tiết hóa đơn.')
add_paragraph(doc, '• HoaDon (N) - (1) Voucher: Một hóa đơn có thể áp dụng một voucher.')

add_page_break(doc)

# ============================================================
# CHƯƠNG 3: THIẾT KẾ
# ============================================================

add_heading_center(doc, 'CHƯƠNG 3: THIẾT KẾ', 16, True)
doc.add_paragraph()

add_heading_left(doc, '3.1. Cơ sở dữ liệu', 14, True)

add_heading_left(doc, '3.1.1. Danh sách bảng:', 14, True)

add_table(doc, ['STT', 'Tên bảng', 'Mô tả', 'Phụ thuộc'], [
    ['1', 'San', 'Lưu danh mục sân bóng.', 'không'],
    ['2', 'LichSan', 'Lưu lịch đặt sân theo ca, buổi.', 'San'],
    ['3', 'KhachHang', 'Lưu thông tin khách hàng.', 'không'],
    ['4', 'NhanVien', 'Lưu thông tin nhân viên.', 'không'],
    ['5', 'DichVu', 'Lưu danh mục dịch vụ đi kèm.', 'không'],
    ['6', 'HoaDon', 'Lưu thông tin hóa đơn.', 'KhachHang, NhanVien, Voucher'],
    ['7', 'CTHD', 'Lưu chi tiết hóa đơn.', 'HoaDon, LichSan, DichVu'],
    ['8', 'Voucher', 'Lưu thông tin phiếu giảm giá.', 'không'],
    ['9', 'Voucher_San', 'Lưu voucher áp dụng cho sân.', 'Voucher, San'],
    ['10', 'Voucher_DichVu', 'Lưu voucher áp dụng cho dịch vụ.', 'Voucher, DichVu'],
])

add_heading_left(doc, '3.2. Giao diện người dùng', 14, True)

add_heading_left(doc, '3.2.1. Sơ đồ giao diện', 14, True)

add_table(doc, ['STT', 'Tên màn hình', 'Mô tả', 'Liên kết'], [
    ['1', 'Trang chủ', 'Màn hình giới thiệu sân bóng, hiển thị thống kê sân trống, đã đặt, đang chờ.', 'Đặt lịch'],
    ['2', 'Đặt lịch', 'Màn hình xem lịch sân theo ngày, chọn sân và ca đặt, thêm dịch vụ, áp dụng voucher.', 'Trang chủ, Hóa đơn'],
    ['3', 'Hóa đơn', 'Màn hình xem chi tiết hóa đơn, thanh toán cọc, thanh toán phần còn lại, hủy đơn.', 'Đặt lịch, Trang chủ'],
    ['4', 'Đăng nhập Admin', 'Màn hình đăng nhập cho quản trị viên.', 'Dashboard'],
    ['5', 'Dashboard', 'Màn hình tổng quan thống kê và quản lý các thực thể.', 'Quản lý Sân, Lịch Sân, Khách Hàng, Nhân Viên, Dịch Vụ, Hóa Đơn, CTHD'],
    ['6', 'Quản lý Sân', 'Màn hình quản lý danh sách sân, thêm/sửa/xóa sân.', 'Dashboard'],
    ['7', 'Quản lý Lịch Sân', 'Màn hình quản lý lịch đặt sân theo ca, buổi.', 'Dashboard'],
    ['8', 'Quản lý Khách Hàng', 'Màn hình quản lý thông tin khách hàng.', 'Dashboard'],
    ['9', 'Quản lý Nhân Viên', 'Màn hình quản lý thông tin nhân viên.', 'Dashboard'],
    ['10', 'Quản lý Dịch Vụ', 'Màn hình quản lý dịch vụ đi kèm.', 'Dashboard'],
    ['11', 'Quản lý Hóa Đơn', 'Màn hình quản lý hóa đơn, cập nhật trạng thái, thanh toán.', 'Dashboard'],
    ['12', 'Quản lý Voucher', 'Màn hình quản lý phiếu giảm giá.', 'Dashboard'],
    ['13', 'Dashboard Sân', 'Màn hình chi tiết lịch đặt của từng sân trong ngày.', 'Dashboard'],
])

add_page_break(doc)

# ============================================================
# CHƯƠNG 4: TỔNG KẾT DỰ ÁN
# ============================================================

add_heading_center(doc, 'CHƯƠNG 4: TỔNG KẾT DỰ ÁN', 16, True)
doc.add_paragraph()

add_heading_left(doc, '4.1 Kết quả đạt được so với mục tiêu ban đầu', 14, True)

add_paragraph(doc, 'Sau quá trình nghiên cứu, phân tích, thiết kế và xây dựng hệ thống, nhóm đã hoàn thành các mục tiêu đã đề ra ban đầu của đề tài "Xây dựng hệ thống quản lý sân bóng Hoa Sen". Hệ thống được phát triển trên nền tảng Java Servlet/JSP kết hợp với SQL Server và áp dụng mô hình MVC nhằm đảm bảo tính khoa học trong việc tổ chức mã nguồn, đồng thời tạo điều kiện thuận lợi cho việc bảo trì và phát triển sau này.')

add_paragraph(doc, 'Đối với quản trị viên và nhân viên, hệ thống hỗ trợ các chức năng quản trị như quản lý sân, lịch sân, khách hàng, nhân viên, dịch vụ, hóa đơn, chi tiết hóa đơn và phiếu giảm giá. Ngoài ra, việc lưu trữ lịch sử hóa đơn và lịch sử thanh toán giúp theo dõi toàn bộ quá trình xử lý đơn đặt sân, hỗ trợ việc kiểm tra và quản lý dữ liệu một cách chính xác. Có thể thấy rằng các mục tiêu đặt ra ban đầu đều đã được hoàn thành, tạo nên một hệ thống có khả năng đáp ứng tốt các yêu cầu quản lý và đặt sân của sân bóng Hoa Sen.')

add_heading_left(doc, '4.2 Đánh giá hiệu quả và giá trị mang lại:', 14, True)

add_paragraph(doc, 'Hệ thống đặt sân của sân bóng Hoa Sen đã góp phần nâng cao hiệu quả trong công tác quản lý đặt sân và quản lý thông tin của sân bóng. Thay vì thực hiện các thao tác thủ công như ghi chép hóa đơn hoặc quản lý lịch sân bằng sổ sách, toàn bộ dữ liệu về sân (loại sân, giá theo giờ), lịch đặt, khách hàng, hóa đơn và phiếu giảm giá được lưu trữ và quản lý tập trung trên hệ thống. Điều này giúp giảm thiểu sai sót trong quá trình nhập liệu, tiết kiệm thời gian và nâng cao hiệu quả làm việc của nhân viên.')

add_paragraph(doc, 'Đối với nhân viên, hệ thống hỗ trợ thực hiện các nghiệp vụ đặt sân một cách nhanh chóng như tìm kiếm sân, lựa chọn ca đặt, tạo hóa đơn, áp dụng phiếu giảm giá và thanh toán. Các thao tác được thực hiện trên cùng một giao diện giúp rút ngắn thời gian phục vụ khách hàng và hạn chế các sai sót trong quá trình đặt sân.')

add_paragraph(doc, 'Đối với sân bóng, hệ thống hỗ trợ quản lý sân, khách hàng, nhân viên, hóa đơn và các chương trình giảm giá một cách khoa học. Việc lưu trữ lịch sử hóa đơn và lịch sử thanh toán giúp dễ dàng kiểm tra, đối chiếu và theo dõi các giao dịch đã phát sinh. Ngoài ra, hệ thống còn hỗ trợ quản lý số lượng ca đặt theo từng sân, giúp sân bóng kiểm soát tình hình kinh doanh và đưa ra các quyết định quản lý phù hợp.')

add_paragraph(doc, 'Bên cạnh giá trị thực tiễn, đề tài còn giúp nhóm vận dụng các kiến thức đã học về Java Servlet/JSP, SQL Server, mô hình MVC và thiết kế cơ sở dữ liệu để xây dựng một hệ thống đặt sân hoàn chỉnh. Quá trình thực hiện cũng giúp các thành viên nâng cao kỹ năng lập trình, làm việc nhóm và giải quyết các vấn đề phát sinh trong quá trình phát triển phần mềm.')

add_heading_left(doc, '4.3 Bài học kinh nghiệm và hướng phát triển tương lai:', 14, True)

add_paragraph(doc, 'Trong quá trình thực hiện đề tài, nhóm đã tích lũy được nhiều kinh nghiệm trong việc phân tích yêu cầu, thiết kế cơ sở dữ liệu và xây dựng hệ thống đặt sân theo mô hình MVC. Nhóm nhận thấy rằng việc phân tích nghiệp vụ và thiết kế cơ sở dữ liệu ngay từ đầu (đặc biệt với các thuộc tính đa dạng của sân như loại sân, giá theo giờ, ca đặt) có ý nghĩa rất quan trọng, giúp hạn chế việc chỉnh sửa trong quá trình phát triển và đảm bảo tính liên kết giữa các bảng dữ liệu. Đồng thời, việc phân chia rõ ràng các tầng xử lý trong mô hình MVC giúp hệ thống dễ bảo trì, dễ mở rộng và thuận tiện khi bổ sung các chức năng mới.')

add_paragraph(doc, 'Ngoài kiến thức chuyên môn, đề tài còn giúp các thành viên rèn luyện kỹ năng làm việc nhóm, lập kế hoạch, phân chia công việc và quản lý tiến độ thực hiện. Việc sử dụng Git để quản lý mã nguồn giúp các thành viên dễ dàng theo dõi sự thay đổi của dự án và phối hợp hiệu quả trong quá trình phát triển. Đây là những kinh nghiệm quan trọng để nhóm có thể thực hiện tốt hơn các dự án phần mềm trong tương lai.')

add_paragraph(doc, 'Trong thời gian tới, hệ thống có thể được phát triển thêm nhiều chức năng nhằm đáp ứng tốt hơn nhu cầu quản lý của sân bóng. Một số hướng phát triển như bổ sung chức năng quản lý kho và cảnh báo khi số lượng dịch vụ sắp hết, thống kê doanh thu theo ngày, tháng và năm bằng biểu đồ trực quan, gợi ý sân dựa trên lịch sử đặt sân của khách, xuất hóa đơn và báo cáo dưới dạng PDF hoặc Excel. Ngoài ra, hệ thống cũng có thể được nâng cấp giao diện, tối ưu hiệu năng trong tương lai, góp phần nâng cao hiệu quả quản lý và đáp ứng tốt hơn nhu cầu thực tế của sân bóng Hoa Sen.')

add_page_break(doc)

# ============================================================
# CHƯƠNG 5: HƯỚNG DẪN SỬ DỤNG
# ============================================================

add_heading_center(doc, 'CHƯƠNG 5: HƯỚNG DẪN SỬ DỤNG', 16, True)
doc.add_paragraph()

add_heading_left(doc, '5.1: Vai trò nhân viên', 14, True)

add_heading_left(doc, '5.1.1. Chức năng đặt sân trực tuyến', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình đặt sân trực tuyến cho phép khách hàng thực hiện thao tác đặt sân trực tiếp trên website. Đây là màn hình trung tâm hỗ trợ toàn bộ quy trình từ chọn sân, quản lý giỏ đặt đến thanh toán đơn hàng. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Xem lịch sân: Cho phép khách hàng xem lịch trống của tất cả các sân theo ngày, theo ca (9 ca/ngày, mỗi ca 2 giờ).')
add_paragraph(doc, '• Chọn sân và ca đặt: Cho phép chọn sân và ca đặt phù hợp, sân đang chọn được đánh dấu màu xanh.')
add_paragraph(doc, '• Thêm dịch vụ: Hỗ trợ thêm các dịch vụ đi kèm như nước suối, áo bib, bóng thi đấu.')
add_paragraph(doc, '• Áp dụng phiếu giảm giá: Hỗ trợ chọn mã phiếu giảm giá còn hiệu lực để áp dụng chiết khấu cho đơn hàng.')
add_paragraph(doc, '• Tổng kết đơn hàng: Hệ thống tự động hiển thị Tổng tiền, Số tiền giảm, Tiền cọc (30%) và Số tiền còn lại để khách hàng xác nhận trước khi thanh toán.')
add_paragraph(doc, '• Thao tác hoàn tất: Cung cấp nút Xác nhận đặt sân để hoàn tất đơn đặt.')

add_heading_left(doc, '5.1.2. Chức năng quản lý hóa đơn', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình quản lý hóa đơn cho phép quản lý theo dõi toàn bộ các đơn đặt sân đã được tạo trong hệ thống, từ khi khách hàng đặt sân cho đến khi hoàn tất thanh toán. Đây là màn hình trung tâm hỗ trợ tra cứu, kiểm soát trạng thái và xử lý hóa đơn. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Tìm kiếm và lọc hóa đơn: Cho phép tìm kiếm theo mã hóa đơn hoặc số điện thoại khách hàng, kết hợp lọc theo khoảng thời gian và trạng thái.')
add_paragraph(doc, '• Phân loại theo trạng thái: Hiển thị hóa đơn theo các nhóm trạng thái riêng biệt (Chưa thanh toán, Mới thanh toán cọc, Đã thanh toán, Đã hủy).')
add_paragraph(doc, '• Xem chi tiết hóa đơn: Cho phép mở xem thông tin đầy đủ của từng hóa đơn, bao gồm danh sách sân, ca đặt, dịch vụ, số lượng, đơn giá.')
add_paragraph(doc, '• Cập nhật trạng thái: Cho phép cập nhật trạng thái hóa đơn (phê duyệt, từ chối, đang xử lý).')

add_heading_left(doc, '5.1.3. Chức năng quản lý khách hàng', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình quản lý khách hàng cho phép quản lý theo dõi và duy trì thông tin của toàn bộ khách hàng đã từng giao dịch với sân bóng. Đây là màn hình hỗ trợ xây dựng cơ sở dữ liệu khách hàng phục vụ chăm sóc và phân tích hành vi đặt sân. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Tìm kiếm và lọc khách hàng: Cho phép tìm kiếm theo mã, tên hoặc số điện thoại khách hàng.')
add_paragraph(doc, '• Danh sách thông tin khách hàng: Hiển thị đầy đủ mã khách hàng, họ tên, email, số điện thoại.')
add_paragraph(doc, '• Thêm khách hàng mới: Hỗ trợ tạo hồ sơ khách hàng mới trực tiếp trên hệ thống.')
add_paragraph(doc, '• Chỉnh sửa và xem chi tiết: Cho phép cập nhật thông tin khách hàng hoặc xem chi tiết lịch sử giao dịch.')

add_heading_left(doc, '5.2: Vai trò quản lý (Admin)', 14, True)

add_heading_left(doc, '5.2.1. Chức năng quản lý nhân viên', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình quản lý tài khoản nhân viên cho phép quản trị viên theo dõi, phân quyền và duy trì thông tin của toàn bộ nhân viên trong hệ thống. Đây là màn hình hỗ trợ kiểm soát nhân sự và phân công chức vụ làm việc. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Tìm kiếm và lọc nhân viên: Cho phép tìm kiếm theo mã, tên hoặc email nhân viên, kết hợp lọc theo chức vụ và trạng thái hoạt động.')
add_paragraph(doc, '• Danh sách thông tin nhân viên: Hiển thị đầy đủ mã nhân viên, họ tên, email, số điện thoại, chức vụ và trạng thái tài khoản.')
add_paragraph(doc, '• Thêm nhân viên mới: Hỗ trợ tạo tài khoản nhân viên mới, gán chức vụ (Quản lý, Quản trị viên, Nhân viên).')
add_paragraph(doc, '• Chỉnh sửa, xem và xóa: Cho phép cập nhật thông tin, xem chi tiết hoặc xóa tài khoản nhân viên khi không còn sử dụng.')

add_heading_left(doc, '5.2.2. Chức năng quản lý phiếu giảm giá', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình quản lý phiếu giảm giá cho phép quản lý theo dõi và duy trì các chương trình khuyến mãi, ưu đãi áp dụng cho khách hàng khi đặt sân. Đây là màn hình hỗ trợ xây dựng chiến lược kích cầu và chăm sóc khách hàng. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Tìm kiếm và lọc phiếu giảm giá: Cho phép tìm kiếm theo từ khóa, kết hợp lọc theo loại phiếu giảm giá (Hóa đơn, Sân, Dịch vụ), trạng thái và ngày tạo.')
add_paragraph(doc, '• Phân loại đa dạng: Hỗ trợ nhiều loại ưu đãi khác nhau (Phiếu giảm giá, Voucher, Mã khuyến mãi) được phân biệt bằng nhãn màu trực quan.')
add_paragraph(doc, '• Danh sách chi tiết: Hiển thị mã phiếu, tên chương trình, mức chiết khấu, ngày bắt đầu, ngày kết thúc và trạng thái áp dụng (Hoạt động/Không hoạt động).')
add_paragraph(doc, '• Thêm phiếu mới: Cho phép tạo chương trình giảm giá mới với các thông số tùy chỉnh.')
add_paragraph(doc, '• Chỉnh sửa, xem và xóa: Cho phép cập nhật, xem chi tiết hoặc xóa các phiếu giảm giá đã hết hiệu lực.')

add_heading_left(doc, '5.2.3. Chức năng thống kê', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình thống kê cung cấp cái nhìn tổng quan về tình hình hoạt động kinh doanh của sân bóng thông qua các chỉ số quan trọng và biểu đồ trực quan. Đây là màn hình hỗ trợ quản lý ra quyết định dựa trên dữ liệu thực tế. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Bảng chỉ số tổng quan: Hiển thị nhanh các số liệu quan trọng gồm doanh thu, số đơn hàng, số khách hàng, số đơn hoàn/hủy và số sân hiện có.')
add_paragraph(doc, '• Biểu đồ doanh thu: Trực quan hóa doanh thu theo từng tháng trong năm, hỗ trợ chuyển đổi xem theo Tháng hoặc Năm.')
add_paragraph(doc, '• Danh sách sân được đặt nhiều: Hiển thị các sân có mức tiêu thụ hoặc biến động giá đáng chú ý, bao gồm mã sân, tên sân, số lượng ca đặt, giá bán, tổng tiền và trạng thái.')

add_heading_left(doc, '5.2.4. Chức năng quản lý sân', 14, True)

add_paragraph(doc, 'Giới thiệu chung: Màn hình quản lý sân cho phép quản lý theo dõi và duy trì toàn bộ danh mục sân bóng được kinh doanh tại sân bóng. Đây là màn hình hỗ trợ kiểm soát thông tin sân với nhiều thuộc tính đặc thù. Các chức năng chính bao gồm:')

add_paragraph(doc, '• Tìm kiếm và lọc nâng cao: Cho phép tìm kiếm theo mã hoặc tên sân, kết hợp lọc theo trạng thái, loại sân, khoảng giá.')
add_paragraph(doc, '• Danh sách sân: Hiển thị mã sân, tên sân, loại sân, giá theo giờ, ngày tạo và trạng thái hoạt động.')
add_paragraph(doc, '• Thêm sân mới: Hỗ trợ tạo sân mới với đầy đủ các thuộc tính phân loại.')
add_paragraph(doc, '• Xem chi tiết sân: Cho phép xem lịch đặt của từng sân theo ngày, theo ca.')

add_page_break(doc)

# ============================================================
# PHỤ LỤC A: ĐẶC TẢ BẢNG
# ============================================================

add_heading_center(doc, 'PHỤ LỤC A: ĐẶC TẢ BẢNG', 16, True)
doc.add_paragraph()

# Bảng San
add_heading_left(doc, 'Bảng A.1: Bảng sân', 14, True)
add_paragraph(doc, 'San', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_san', 'VARCHAR(10)', 'Khóa chính của sân', 'PK, NOT NULL'],
    ['2', 'ten_san', 'NVARCHAR(100)', 'Tên sân', 'NOT NULL'],
    ['3', 'loai_san', 'NVARCHAR(30)', 'Loại sân (5 người, 7 người, 11 người)', 'NOT NULL'],
    ['4', 'gia_theo_gio', 'DECIMAL(18,2)', 'Giá thuê theo giờ', 'NOT NULL, CHECK >= 0'],
    ['5', 'trang_thai', 'NVARCHAR(20)', 'Trạng thái sân', 'DEFAULT N\'HOẠT ĐỘNG\''],
])

# Bảng LichSan
add_heading_left(doc, 'Bảng A.2: Bảng lịch sân', 14, True)
add_paragraph(doc, 'LichSan', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_lich', 'VARCHAR(10)', 'Khóa chính lịch sân', 'PK, NOT NULL'],
    ['2', 'ma_san', 'VARCHAR(10)', 'Mã sân', 'FK -> San(ma_san)'],
    ['3', 'ngay_dat', 'DATE', 'Ngày đặt sân', 'NOT NULL'],
    ['4', 'buoi', 'NVARCHAR(20)', 'Buổi (Sáng/Chiều/Tối)', 'NOT NULL'],
    ['5', 'ca', 'NVARCHAR(30)', 'Ca đặt', 'NOT NULL'],
    ['6', 'so_ca', 'TINYINT', 'Số ca (1-9)', 'NOT NULL, CHECK 1-9'],
    ['7', 'gio_bat_dau', 'DATETIME', 'Giờ bắt đầu', 'NOT NULL'],
    ['8', 'gio_ket_thuc', 'DATETIME', 'Giờ kết thúc', 'NOT NULL'],
    ['9', 'trang_thai', 'NVARCHAR(20)', 'Trạng thái lịch', 'DEFAULT N\'CÒN TRỐNG\''],
])

# Bảng KhachHang
add_heading_left(doc, 'Bảng A.3: Bảng khách hàng', 14, True)
add_paragraph(doc, 'KhachHang', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_kh', 'VARCHAR(10)', 'Khóa chính khách hàng', 'PK, NOT NULL'],
    ['2', 'ho_ten', 'NVARCHAR(100)', 'Họ và tên khách hàng', 'NOT NULL'],
    ['3', 'sdt', 'VARCHAR(15)', 'Số điện thoại', 'UNIQUE, NOT NULL'],
    ['4', 'email', 'VARCHAR(100)', 'Email khách hàng', 'NULL'],
])

# Bảng NhanVien
add_heading_left(doc, 'Bảng A.4: Bảng nhân viên', 14, True)
add_paragraph(doc, 'NhanVien', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_nv', 'VARCHAR(10)', 'Khóa chính nhân viên', 'PK, NOT NULL'],
    ['2', 'ten_nv', 'NVARCHAR(100)', 'Họ và tên nhân viên', 'NOT NULL'],
    ['3', 'sdt', 'VARCHAR(15)', 'Số điện thoại liên hệ', 'UNIQUE, NOT NULL'],
    ['4', 'chuc_vu', 'NVARCHAR(50)', 'Chức vụ / vai trò', 'NOT NULL'],
    ['5', 'mat_khau', 'NVARCHAR(100)', 'Mật khẩu tài khoản', 'DEFAULT N\'admin123\''],
])

# Bảng DichVu
add_heading_left(doc, 'Bảng A.5: Bảng dịch vụ', 14, True)
add_paragraph(doc, 'DichVu', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_dv', 'VARCHAR(10)', 'Khóa chính dịch vụ', 'PK, NOT NULL'],
    ['2', 'ten_dv', 'NVARCHAR(100)', 'Tên dịch vụ', 'NOT NULL'],
    ['3', 'don_gia', 'DECIMAL(18,2)', 'Đơn giá dịch vụ', 'NOT NULL, CHECK >= 0'],
])

# Bảng HoaDon
add_heading_left(doc, 'Bảng A.6: Bảng hóa đơn', 14, True)
add_paragraph(doc, 'HoaDon', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_hd', 'VARCHAR(10)', 'Khóa chính hóa đơn', 'PK, NOT NULL'],
    ['2', 'ma_kh', 'VARCHAR(10)', 'Mã khách hàng', 'FK -> KhachHang(ma_kh)'],
    ['3', 'ma_nv', 'VARCHAR(10)', 'Mã nhân viên lập hóa đơn', 'FK -> NhanVien(ma_nv)'],
    ['4', 'ngay_lap', 'DATETIME2', 'Ngày giờ lập hóa đơn', 'DEFAULT SYSDATETIME()'],
    ['5', 'tong_tien', 'DECIMAL(18,2)', 'Tổng tiền hóa đơn', 'NOT NULL'],
    ['6', 'tien_coc', 'DECIMAL(18,2)', 'Tiền cọc (30%)', 'DEFAULT 200000'],
    ['7', 'so_tien_con_lai', 'DECIMAL(18,2)', 'Số tiền còn lại', 'NOT NULL'],
    ['8', 'han_thanh_toan', 'DATETIME', 'Hạn thanh toán', 'NOT NULL'],
    ['9', 'trang_thai', 'NVARCHAR(30)', 'Trạng thái hóa đơn', 'DEFAULT N\'CHƯA THANH TOÁN\''],
    ['10', 'ma_voucher', 'VARCHAR(20)', 'Mã voucher áp dụng', 'FK -> Voucher(ma_voucher)'],
    ['11', 'tien_giam', 'DECIMAL(18,2)', 'Số tiền giảm giá', 'DEFAULT 0'],
])

# Bảng CTHD
add_heading_left(doc, 'Bảng A.7: Bảng chi tiết hóa đơn', 14, True)
add_paragraph(doc, 'CTHD', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_cthd', 'INT IDENTITY', 'Khóa chính chi tiết hóa đơn', 'PK, IDENTITY'],
    ['2', 'ma_hd', 'VARCHAR(10)', 'Mã hóa đơn', 'FK -> HoaDon(ma_hd)'],
    ['3', 'ma_lich', 'VARCHAR(10)', 'Mã lịch sân', 'FK -> LichSan(ma_lich)'],
    ['4', 'ma_dv', 'VARCHAR(10)', 'Mã dịch vụ', 'FK -> DichVu(ma_dv)'],
    ['5', 'so_luong', 'INT', 'Số lượng', 'DEFAULT 1'],
    ['6', 'don_gia', 'DECIMAL(18,2)', 'Đơn giá', 'NOT NULL'],
    ['7', 'thanh_tien', 'DECIMAL(18,2)', 'Thành tiền', 'NOT NULL'],
])

# Bảng Voucher
add_heading_left(doc, 'Bảng A.8: Bảng phiếu giảm giá', 14, True)
add_paragraph(doc, 'Voucher', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_voucher', 'VARCHAR(20)', 'Khóa chính voucher', 'PK, NOT NULL'],
    ['2', 'ma_code', 'VARCHAR(50)', 'Mã code voucher', 'NOT NULL'],
    ['3', 'ten_voucher', 'NVARCHAR(100)', 'Tên chương trình giảm giá', 'NOT NULL'],
    ['4', 'loai_giam', 'VARCHAR(20)', 'Loại giảm (MONEY/PERCENT)', 'NOT NULL'],
    ['5', 'gia_tri', 'FLOAT', 'Giá trị giảm', 'DEFAULT 0'],
    ['6', 'don_toi_thieu', 'FLOAT', 'Đơn tối thiểu áp dụng', 'DEFAULT 0'],
    ['7', 'ngay_bat_dau', 'DATE', 'Ngày bắt đầu', 'NOT NULL'],
    ['8', 'ngay_ket_thuc', 'DATE', 'Ngày kết thúc', 'NOT NULL'],
    ['9', 'so_luong', 'INT', 'Số lượng phát hành', 'DEFAULT 0'],
    ['10', 'trang_thai', 'NVARCHAR(30)', 'Trạng thái áp dụng', 'DEFAULT N\'HOẠT ĐỘNG\''],
    ['11', 'loai', 'INT', 'Loại áp dụng (1=HĐ, 2=Sân, 3=DV)', 'DEFAULT 1'],
])

# Bảng Voucher_San
add_heading_left(doc, 'Bảng A.9: Bảng voucher sân', 14, True)
add_paragraph(doc, 'Voucher_San', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_voucher', 'VARCHAR(20)', 'Mã voucher', 'PK, FK -> Voucher(ma_voucher)'],
    ['2', 'ma_san', 'VARCHAR(10)', 'Mã sân', 'PK, FK -> San(ma_san)'],
])

# Bảng Voucher_DichVu
add_heading_left(doc, 'Bảng A.10: Bảng voucher dịch vụ', 14, True)
add_paragraph(doc, 'Voucher_DichVu', 13, True)
add_table(doc, ['STT', 'Tên trường', 'Kiểu dữ liệu', 'Mô tả', 'Ràng buộc'], [
    ['1', 'ma_voucher', 'VARCHAR(20)', 'Mã voucher', 'PK, FK -> Voucher(ma_voucher)'],
    ['2', 'ma_dv', 'VARCHAR(10)', 'Mã dịch vụ', 'PK, FK -> DichVu(ma_dv)'],
])

# ============================================================
# LƯU TÀI LIỆU
# ============================================================

output_path = 'Bao_cao_du_an_San_Bong_Hoa_Sen.docx'
doc.save(output_path)
print(f'Đã tạo báo cáo thành công: {output_path}')