# -*- coding: utf-8 -*-
"""Sinh tài liệu đặc tả chức năng & nghiệp vụ (.docx) để gửi khách hàng."""

from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = r"D:\CODE\travel-tour-booking\docs\Dac-ta-chuc-nang-nghiep-vu.docx"

INK = RGBColor(0x1A, 0x1A, 0x1A)
ACCENT = RGBColor(0x1F, 0x3A, 0x66)
MUTED = RGBColor(0x5A, 0x5A, 0x5A)
HDR_BG = "1F3A66"
ALT_BG = "EDF1F7"
NOTE_BG = "FBF3E4"

doc = Document()

# ---------------------------------------------------------------- trang & font

for s in doc.sections:
    s.top_margin = Cm(2.2)
    s.bottom_margin = Cm(2.2)
    s.left_margin = Cm(2.5)
    s.right_margin = Cm(2.2)

USABLE_CM = 21 - 2.5 - 2.2


def set_font(style_or_run, name):
    """Đặt font cho cả ascii, hAnsi, eastAsia và complex script."""
    rpr = style_or_run._element.get_or_add_rPr()
    rf = rpr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts')
        rpr.append(rf)
    for a in ('w:ascii', 'w:hAnsi', 'w:eastAsia', 'w:cs'):
        rf.set(qn(a), name)


normal = doc.styles['Normal']
normal.font.name = 'Calibri'
normal.font.size = Pt(11)
normal.font.color.rgb = INK
set_font(normal, 'Calibri')
normal.paragraph_format.space_after = Pt(7)
normal.paragraph_format.line_spacing = 1.18

for lvl, size, col in (('Heading 1', 19, ACCENT), ('Heading 2', 14.5, ACCENT),
                       ('Heading 3', 12, ACCENT)):
    st = doc.styles[lvl]
    st.font.name = 'Calibri Light'
    st.font.size = Pt(size)
    st.font.bold = True
    st.font.color.rgb = col
    set_font(st, 'Calibri Light')
    st.paragraph_format.space_before = Pt(16 if lvl == 'Heading 1' else 12)
    st.paragraph_format.space_after = Pt(6)
    st.paragraph_format.keep_with_next = True

# cập nhật field (mục lục) khi mở file
settings = doc.settings.element
uf = OxmlElement('w:updateFields')
uf.set(qn('w:val'), 'true')
settings.append(uf)

# ---------------------------------------------------------------- tiện ích


def h1(text):
    doc.add_heading(text, level=1)


def h2(text):
    doc.add_heading(text, level=2)


def h3(text):
    doc.add_heading(text, level=3)


def p(text='', bold=False, italic=False, size=None, color=None, align=None,
      space_after=None):
    par = doc.add_paragraph()
    run = par.add_run(text)
    run.bold = bold
    run.italic = italic
    if size:
        run.font.size = Pt(size)
    if color:
        run.font.color.rgb = color
    set_font(run, 'Calibri')
    if align:
        par.alignment = align
    if space_after is not None:
        par.paragraph_format.space_after = Pt(space_after)
    return par


def rich(parts, align=None):
    """parts: danh sách (text, bold) hoặc chuỗi."""
    par = doc.add_paragraph()
    for item in parts:
        text, bold = (item, False) if isinstance(item, str) else item
        run = par.add_run(text)
        run.bold = bold
        set_font(run, 'Calibri')
    if align:
        par.alignment = align
    return par


def bullets(items, style='List Bullet'):
    for it in items:
        par = doc.add_paragraph(style=style)
        if isinstance(it, tuple):
            head, rest = it
            r1 = par.add_run(head)
            r1.bold = True
            set_font(r1, 'Calibri')
            r2 = par.add_run(rest)
            set_font(r2, 'Calibri')
        else:
            run = par.add_run(it)
            set_font(run, 'Calibri')
        par.paragraph_format.space_after = Pt(3)


def shade(cell, hex_color):
    tcpr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), hex_color)
    tcpr.append(shd)


def cell_text(cell, text, bold=False, white=False, size=10):
    cell.text = ''
    par = cell.paragraphs[0]
    par.paragraph_format.space_after = Pt(2)
    par.paragraph_format.space_before = Pt(2)
    for i, line in enumerate(str(text).split('\n')):
        if i:
            par = cell.add_paragraph()
            par.paragraph_format.space_after = Pt(2)
        run = par.add_run(line)
        run.bold = bold
        run.font.size = Pt(size)
        if white:
            run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        set_font(run, 'Calibri')


def table(headers, rows, widths=None, size=10, zebra=True):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = 'Table Grid'
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    for i, htext in enumerate(headers):
        c = t.rows[0].cells[i]
        shade(c, HDR_BG)
        cell_text(c, htext, bold=True, white=True, size=size)
    for ri, row in enumerate(rows):
        cells = t.add_row().cells
        for i, val in enumerate(row):
            bold = val.startswith('**') and val.endswith('**')
            if bold:
                val = val[2:-2]
            cell_text(cells[i], val, bold=bold, size=size)
            if zebra and ri % 2 == 1:
                shade(cells[i], ALT_BG)
    if widths:
        total = sum(widths)
        for row in t.rows:
            for i, w in enumerate(widths):
                row.cells[i].width = Cm(round(USABLE_CM * w / total, 2))
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    return t


def note(title, body):
    t = doc.add_table(rows=1, cols=1)
    t.style = 'Table Grid'
    c = t.rows[0].cells[0]
    shade(c, NOTE_BG)
    c.text = ''
    par = c.paragraphs[0]
    r = par.add_run(title)
    r.bold = True
    r.font.size = Pt(10.5)
    set_font(r, 'Calibri')
    par2 = c.add_paragraph()
    r2 = par2.add_run(body)
    r2.font.size = Pt(10.5)
    set_font(r2, 'Calibri')
    par2.paragraph_format.space_after = Pt(4)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def page_break():
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def add_field(paragraph, instr):
    run = paragraph.add_run()
    f1 = OxmlElement('w:fldChar')
    f1.set(qn('w:fldCharType'), 'begin')
    it = OxmlElement('w:instrText')
    it.set(qn('xml:space'), 'preserve')
    it.text = instr
    f2 = OxmlElement('w:fldChar')
    f2.set(qn('w:fldCharType'), 'separate')
    f3 = OxmlElement('w:fldChar')
    f3.set(qn('w:fldCharType'), 'end')
    run._r.append(f1)
    run._r.append(it)
    run._r.append(f2)
    run._r.append(f3)
    return run


# ---------------------------------------------------------------- trang bìa

for _ in range(4):
    doc.add_paragraph()

p('TÀI LIỆU ĐẶC TẢ', size=13, color=MUTED, align=WD_ALIGN_PARAGRAPH.CENTER,
  space_after=2)
par = doc.add_paragraph()
par.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = par.add_run('Chức năng và nghiệp vụ')
r.bold = True
r.font.size = Pt(30)
r.font.color.rgb = ACCENT
set_font(r, 'Calibri Light')

par = doc.add_paragraph()
par.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = par.add_run('Website bán tour du lịch Việt Nam')
r.font.size = Pt(16)
r.font.color.rgb = INK
set_font(r, 'Calibri Light')

p('Hai thị trường  ·  Đan Mạch và Việt Nam  ·  Hai ngôn ngữ',
  size=11.5, color=MUTED, align=WD_ALIGN_PARAGRAPH.CENTER)

for _ in range(6):
    doc.add_paragraph()

table(['Hạng mục', 'Nội dung'], [
    ['Tên tài liệu', 'Đặc tả chức năng và nghiệp vụ'],
    ['Phiên bản', '1.0'],
    ['Ngày phát hành', '31/08/2026'],
    ['Khách hàng', '[Tên công ty]'],
    ['Đơn vị thực hiện', '[Tên đơn vị]'],
    ['Người soạn', '[Họ tên]'],
    ['Trạng thái', 'Bản thảo — chờ khách hàng xác nhận'],
], widths=[1, 2.4])

doc.add_paragraph()
note('Tài liệu này cần được xác nhận',
     'Mục 11 liệt kê 14 nội dung cần khách hàng quyết định trước khi bắt đầu '
     'lập trình. Những nội dung đó ảnh hưởng trực tiếp tới thiết kế hệ thống; '
     'thay đổi sau khi đã triển khai sẽ phát sinh chi phí đáng kể.')

page_break()

# ---------------------------------------------------------------- mục lục

h1('Mục lục')
par = doc.add_paragraph()
add_field(par, r'TOC \o "1-2" \h \z \u')
p('(Mở file bằng Microsoft Word và nhấn Ctrl+A rồi F9 để cập nhật mục lục.)',
  italic=True, size=10, color=MUTED)

page_break()

# ---------------------------------------------------------------- 1

h1('1. Giới thiệu')

h2('1.1. Mục đích tài liệu')
p('Tài liệu mô tả đầy đủ chức năng và quy tắc nghiệp vụ của website bán tour '
  'du lịch Việt Nam, phục vụ hai thị trường khách hàng là Đan Mạch và Việt Nam. '
  'Tài liệu là cơ sở để hai bên thống nhất phạm vi công việc trước khi bắt đầu '
  'lập trình, và là căn cứ nghiệm thu khi bàn giao.')
p('Tài liệu mô tả hệ thống làm được gì và vận hành theo quy tắc nào. Các nội '
  'dung kỹ thuật như cấu trúc cơ sở dữ liệu, giao diện lập trình và hạ tầng '
  'triển khai được trình bày ở bộ tài liệu kỹ thuật riêng.')

h2('1.2. Phạm vi hệ thống')
p('Hệ thống gồm ba phần:')
table(['Thành phần', 'Người sử dụng', 'Mô tả'], [
    ['Website khách hàng', 'Khách mua tour',
     'Xem tour, xem lịch trình từng ngày, chọn ngày khởi hành, đặt tour và '
     'thanh toán trực tuyến'],
    ['Hệ thống máy chủ', 'Hai phần còn lại',
     'Lưu trữ dữ liệu, xử lý nghiệp vụ, tính giá, quản lý chỗ và đơn hàng'],
    ['Trang quản trị', 'Nhân viên công ty',
     'Quản lý sản phẩm, giá, ngày khởi hành, đơn hàng, nội dung và bản dịch'],
], widths=[1.1, 1, 2.6])

h2('1.3. Cấu trúc phân loại địa lý')
p('Do công ty chỉ bán tour trong lãnh thổ Việt Nam, hệ thống không dùng cấp '
  'phân loại "châu lục — quốc gia" như các công ty lữ hành quốc tế. Thay vào '
  'đó, cấp phân loại là:')
rich([('Miền', True), ' (Bắc / Trung / Nam)  →  ', ('Điểm đến', True),
      ' (Hà Nội, Sa Pa, vịnh Hạ Long, Hội An…)'])
note('Quy tắc đếm số tour theo miền',
     'Số tour của một miền được đếm theo tour, không cộng dồn theo từng điểm '
     'đến. Một tour đi qua Hà Nội, Hạ Long và Ninh Bình chỉ được tính một lần '
     'cho miền Bắc. Cộng dồn sẽ ra con số lớn hơn thực tế và khách hàng phát '
     'hiện ngay khi bấm vào xem danh sách.')

page_break()

# ---------------------------------------------------------------- 2

h1('2. Đối tượng sử dụng')

h2('2.1. Khách hàng Đan Mạch — nhóm khách chính')
p('Khách trung niên và cao tuổi ở Bắc Âu, đi tour trọn gói có trưởng đoàn nói '
  'tiếng Đan Mạch. Đặc điểm của nhóm khách này ảnh hưởng trực tiếp tới thiết '
  'kế giao diện, không phải là mô tả tham khảo:')
bullets([
    'Cỡ chữ nội dung tối thiểu 16px, khoảng cách dòng rộng',
    'Vùng bấm tối thiểu 44×44px trên thiết bị cảm ứng',
    'Độ tương phản màu đạt chuẩn tiếp cận WCAG AA',
    'Không dùng hiệu ứng rê chuột làm cách duy nhất để hiện thông tin',
    'Ngôn từ rõ ràng, không viết tắt',
    'Số điện thoại và giờ làm việc hiển thị thường trực trên đầu trang, vì '
    'nhiều khách gọi điện để chốt thay vì đặt trực tuyến',
])

h2('2.2. Khách hàng Việt Nam')
p('Gồm hai nhóm nhỏ:')
bullets([
    ('Người Việt tại Việt Nam — ', 'mua tour nội địa, giá tính bằng đồng, '
     'không bao gồm vé máy bay quốc tế.'),
    ('Người Việt sinh sống tại Đan Mạch — ', 'mua sản phẩm của thị trường Đan '
     'Mạch nhưng muốn đọc bằng tiếng Việt.'),
])
note('Nhóm thứ hai là lý do hệ thống tách riêng "thị trường" và "ngôn ngữ"',
     'Một khách người Việt sống tại Đan Mạch mua tour có vé bay từ Copenhagen, '
     'thanh toán bằng krone, nhưng đọc nội dung tiếng Việt. Nếu hệ thống gộp '
     'hai khái niệm này làm một, khách buộc phải đọc tiếng Đan hoặc phải mua '
     'sản phẩm không phù hợp. Chi tiết ở mục 3.')

h2('2.3. Nhân viên công ty')
table(['Vai trò', 'Công việc'], [
    ['Nhân viên tư vấn', 'Xem yêu cầu tư vấn, xem đơn hàng, dựng báo giá cho '
     'tour riêng, trả lời khách'],
    ['Biên tập nội dung', 'Viết và chỉnh sửa nội dung tour, điểm đến, bài viết '
     '— bằng ngôn ngữ nguồn'],
    ['Biên dịch', 'Dịch nội dung sang ngôn ngữ thứ hai, xử lý hàng đợi dịch'],
    ['Quản trị viên', 'Quản lý ngày khởi hành, giá, số chỗ, tài khoản người dùng'],
], widths=[1, 3])

page_break()

# ---------------------------------------------------------------- 3

h1('3. Mô hình hai thị trường, hai ngôn ngữ')

h2('3.1. Thị trường và ngôn ngữ là hai khái niệm riêng biệt')
table(['', 'Thị trường', 'Ngôn ngữ'], [
    ['Trả lời câu hỏi', 'Khách **mua** gì, giá bao nhiêu, theo quy định nào',
     'Khách **đọc** bằng tiếng gì'],
    ['Quyết định', 'Danh mục tour, bảng giá, tiền tệ, hình thức thanh toán, tỷ '
     'lệ đặt cọc, quy định pháp lý, chứng từ',
     'Chữ trên giao diện, nội dung tour, đường dẫn, thứ tự sắp xếp, định dạng '
     'ngày tháng'],
    ['Khi thay đổi', 'Giá và danh sách tour thay đổi', 'Chỉ chữ thay đổi, giá '
     'giữ nguyên'],
    ['Giá trị', 'Đan Mạch, Việt Nam', 'Tiếng Đan Mạch, tiếng Việt'],
], widths=[0.9, 1.6, 1.6])

h2('3.2. Cùng một hành trình, hai sản phẩm khác nhau')
p('Đây là điểm quan trọng nhất của mô hình kinh doanh. Cùng một lộ trình bán '
  'cho khách Đan Mạch và khách Việt Nam không phải là cùng một sản phẩm:')
table(['Hạng mục', 'Khách Đan Mạch', 'Khách Việt Nam'], [
    ['Vé máy bay quốc tế', 'Nằm trong giá tour', 'Không có'],
    ['Người dẫn đoàn', 'Trưởng đoàn nói tiếng Đan Mạch, bay cùng đoàn',
     'Hướng dẫn viên tiếng Việt tại chỗ'],
    ['Tiền tệ', 'Krone Đan Mạch (DKK)', 'Đồng Việt Nam (VND)'],
    ['Điểm khởi hành', 'Copenhagen, Billund, Aalborg',
     'Hà Nội, TP. Hồ Chí Minh, Đà Nẵng'],
    ['Tỷ lệ đặt cọc', '25% giá trị đơn hàng', 'Cần xác nhận'],
    ['Quy định pháp lý', 'Quỹ bảo đảm du lịch Đan Mạch, chỉ thị EU về gói du lịch',
     'Luật Du lịch 2017, ký quỹ kinh doanh lữ hành'],
    ['Chứng từ', 'Hoá đơn theo quy định EU', 'Hoá đơn điện tử theo NĐ 123/2020'],
], widths=[1.1, 1.5, 1.5])

note('Hệ thống không quy đổi tỷ giá',
     'Giá tại mỗi thị trường là giá do công ty nhập trực tiếp, không phải kết '
     'quả nhân tỷ giá từ một giá gốc. Vì hai thị trường bán hai sản phẩm khác '
     'nhau (một bên có vé bay quốc tế, một bên không), việc quy đổi tỷ giá sẽ '
     'cho ra con số sai.')

h2('3.3. Quy tắc hiển thị nội dung theo ngôn ngữ')
p('Tiếng Đan Mạch là ngôn ngữ gốc: mọi nội dung được viết bằng tiếng Đan Mạch '
  'trước, sau đó dịch sang tiếng Việt. Hệ thống áp dụng hai quy tắc ngược nhau '
  'cho hai loại nội dung:')
table(['Loại nội dung', 'Khi thiếu bản dịch'], [
    ['**Nội dung bán hàng** — tour, điểm đến, bài viết',
     '**Ẩn hoàn toàn** khỏi ngôn ngữ đó: không xuất hiện trong danh sách, không '
     'tìm kiếm được, không có trong sơ đồ trang. Không hiển thị bản gốc thay thế.'],
    ['**Chữ trên giao diện** — nhãn nút, tiêu đề cột',
     'Hiển thị tạm bằng ngôn ngữ gốc và ghi nhận cảnh báo để bổ sung.'],
], widths=[1.2, 2.8])
p('Lý do: khách hàng Đan Mạch vào trang tour mà thấy tiêu đề tiếng Việt sẽ mất '
  'lòng tin và rời đi. Ẩn một tour thì mất một cơ hội bán hàng; hiển thị tour '
  'dịch dở thì mất lòng tin vào cả website.')
note('Hệ quả cần lưu ý',
     'Số lượng tour hiển thị có thể khác nhau giữa hai ngôn ngữ, do những tour '
     'chưa dịch xong bị ẩn khỏi ngôn ngữ thứ hai. Đây là hành vi đúng theo '
     'thiết kế, không phải lỗi.')

h2('3.4. Chuyển đổi ngôn ngữ và thị trường')
bullets([
    'Đường dẫn website chứa mã ngôn ngữ. Cả đoạn đường dẫn lẫn tên rút gọn của '
    'tour đều được dịch, không dùng đường dẫn tiếng Đan Mạch cho trang tiếng Việt.',
    'Thị trường được suy ra từ ngôn ngữ đang xem. Khách có thể đổi thị trường '
    'bằng công cụ chọn riêng trên đầu trang.',
    'Khi thị trường đang xem khác với mặc định của ngôn ngữ, hệ thống hiển thị '
    'thông báo thường trực để khách không nhầm giá.',
])

page_break()

# ---------------------------------------------------------------- 4

h1('4. Phân loại sản phẩm')

h2('4.1. Sáu loại sản phẩm')
table(['Loại sản phẩm', 'Mô tả ngắn', 'Giai đoạn'], [
    ['**Tour đoàn có trưởng đoàn**',
     'Khách lạ đi chung một đoàn theo ngày và lộ trình do công ty ấn định. '
     'Có trưởng đoàn đi suốt tuyến. Quy mô 10–25 khách.', 'Giai đoạn 1'],
    ['**Tour cá nhân**',
     'Lộ trình do công ty soạn sẵn, khách đi riêng nhóm mình, không ghép khách '
     'lạ. Tra được giá ngay.', 'Giai đoạn 1'],
    ['**Tour riêng theo yêu cầu**',
     'Khách đề xuất ngày đi, lộ trình điều chỉnh được, giá theo bậc số khách. '
     'Phải qua bước báo giá.', 'Giai đoạn 1'],
    ['**Du thuyền**',
     'Tour trên tàu, giá theo hạng cabin.', 'Giai đoạn 1'],
    ['**Combo bay + khách sạn**',
     'Gói gồm vé máy bay khứ hồi và một số đêm khách sạn. Không có lịch trình '
     'từng ngày.', 'Giai đoạn 1.5'],
    ['**Tour trong ngày**',
     'Sản phẩm bán độc lập, kéo dài vài giờ, có khung giờ khởi hành.',
     'Giai đoạn 2'],
], widths=[1.2, 2.6, 0.8])

h2('4.2. Năm điểm khác biệt giữa các loại')
p('Sáu loại sản phẩm khác nhau ở đúng năm điểm; mọi khác biệt còn lại đều suy '
  'ra được từ bảng này.')
table(['Loại', 'Ai quyết định ngày', 'Đơn vị tính giá', 'Quản lý số chỗ',
       'Cần báo giá'], [
    ['Tour đoàn', 'Công ty', 'Theo ngày khởi hành', 'Theo chỗ', 'Không'],
    ['Tour cá nhân', 'Công ty', 'Theo ngày và loại phòng', 'Không', 'Không'],
    ['**Tour riêng**', '**Khách đề xuất**', '**Theo bậc số khách**', 'Không',
     '**Có**'],
    ['Du thuyền', 'Công ty', 'Theo hạng cabin', 'Theo cabin từng hạng', 'Không'],
    ['Combo', 'Khách chọn khoảng ngày', 'Theo thành phần', 'Theo suất', 'Không'],
    ['Tour trong ngày', 'Khách chọn ngày và giờ', 'Theo loại khách',
     'Theo khung giờ', 'Không'],
], widths=[1, 1.1, 1.2, 1, 0.7], size=9.5)

page_break()

h2('4.3. Nghiệp vụ từng loại sản phẩm')

h3('4.3.1. Tour đoàn có trưởng đoàn')
p('Sản phẩm chủ lực của thị trường Đan Mạch. Khách lạ đi chung một đoàn, chia '
  'sẻ chi phí xe, hướng dẫn viên và một số bữa ăn.')
table(['Nội dung', 'Quy tắc'], [
    ['Quy mô đoàn', 'Tối thiểu và tối đa trong khoảng 10–25 khách'],
    ['Ngày khởi hành', 'Do công ty ấn định, mỗi ngày một mức giá riêng'],
    ['Đảm bảo khởi hành',
     'Khi số khách đã đặt đạt ngưỡng tối thiểu, chuyến được đánh dấu "Đảm bảo '
     'khởi hành". Trạng thái này do hệ thống tự tính, nhân viên không nhập tay.'],
    ['Còn ít chỗ', 'Khi số chỗ trống còn từ 3 trở xuống'],
    ['Hết chỗ',
     'Hiển thị mờ, không bấm được, không có nút đặt'],
    ['Huỷ do thiếu khách',
     'Đến hạn chốt mà chưa đủ số khách tối thiểu, công ty huỷ chuyến và hoàn '
     '100% cho khách. Hệ thống có màn hình riêng cho nghiệp vụ này.'],
    ['Giữ chỗ', 'Khi khách vào bước thanh toán, hệ thống giữ chỗ trong 20 phút. '
     'Quá hạn, chỗ được trả về kho tự động.'],
], widths=[1, 3])

h3('4.3.2. Tour cá nhân')
p('Lộ trình do công ty soạn sẵn nhưng khách đi riêng nhóm mình, không ghép '
  'khách lạ. Khác tour riêng ở chỗ lộ trình đã cố định và tra được giá ngay.')
table(['Nội dung', 'Quy tắc'], [
    ['Quy mô đoàn', 'Không áp dụng — không có đoàn để giới hạn'],
    ['Số khách tối thiểu', 'Thường 2 người. Đi một mình chịu phụ thu phòng đơn'],
    ['Số chỗ', 'Không quản lý số chỗ, nhưng có thể bị giới hạn bởi phòng khách sạn'],
    ['Xác nhận đơn hàng',
     'Ngày khởi hành chưa chắc còn phòng được đánh dấu "Chờ xác nhận". Khách '
     'vẫn đặt được, và nhân viên xác nhận trong vòng 24 giờ.'],
], widths=[1, 3])
note('Khác biệt quan trọng so với tour đoàn',
     'Đơn hàng tour cá nhân không được xác nhận ngay sau khi thanh toán. Hệ '
     'thống gửi email "đã tiếp nhận, đang xác nhận" thay vì "đặt tour thành '
     'công". Đây là quy trình nghiệp vụ, cần thống nhất với bộ phận vận hành.')

h3('4.3.3. Tour riêng theo yêu cầu')
p('Khách đề xuất ngày đi, lộ trình điều chỉnh được, giá tính theo bậc số khách. '
  'Đây là loại sản phẩm phổ biến của thị trường Việt Nam.')
table(['Nội dung', 'Quy tắc'], [
    ['Bậc giá', 'Chia theo số khách, ví dụ 2 · 3–4 · 5–8 · 9–15 · từ 16 trở '
     'lên. Càng đông giá mỗi người càng giảm.'],
    ['Thời gian báo trước', 'Tối thiểu 7–14 ngày tuỳ sản phẩm. Hệ thống chặn '
     'ngay trong lịch chọn ngày, không để khách gửi rồi mới bị từ chối.'],
    ['Số chỗ', 'Không quản lý số chỗ, không có trạng thái hết chỗ'],
    ['Nút chính trên trang',
     '**"Yêu cầu báo giá"**, không phải "Đặt tour". Đây là loại sản phẩm duy '
     'nhất không chốt đơn tự động được.'],
    ['Hiển thị giá',
     'Giá luôn kèm bậc số khách, ví dụ "từ 4.290.000 ₫/khách khi đi 2 người". '
     'Hiển thị giá trần trụi sẽ gây hiểu nhầm: khách đi một mình thấy giá gấp '
     'đôi ở bước sau và rời đi.'],
    ['Hiệu lực báo giá', 'Thường 7 ngày. Quá hạn, khách phải yêu cầu lại.'],
], widths=[1, 3])
p('Quy trình báo giá:')
bullets([
    'Khách gửi yêu cầu: ngày mong muốn, số khách theo từng loại, ghi chú',
    'Nhân viên tư vấn dựng báo giá — chốt lộ trình và chốt giá, có thể khác giá '
    'niêm yết theo bậc',
    'Gửi báo giá cho khách, kèm thời hạn hiệu lực',
    'Khách chấp nhận báo giá',
    'Khách đặt cọc, hệ thống sinh đơn hàng',
])

h3('4.3.4. Du thuyền')
table(['Nội dung', 'Quy tắc'], [
    ['Hạng cabin', 'Bốn hạng: cabin trong, cabin ngoài, có ban công, hạng cao '
     'cấp. Chênh giá từ hạng thấp nhất tới hạng cao nhất khoảng 20–45%.'],
    ['Bảng ngày khởi hành',
     'Gộp theo ngày, mỗi ngày mở ra bốn dòng theo hạng cabin. Không hiển thị '
     'bốn dòng rời rạc, tránh khách hiểu nhầm là bốn chuyến khác nhau.'],
    ['Hết chỗ',
     '**Tính riêng cho từng hạng cabin.** Hết hạng có ban công không có nghĩa '
     'là hết cabin trong — ba hạng còn lại vẫn đặt được bình thường.'],
    ['Trang chi tiết', 'Thay mục "Khách sạn" bằng mục "Tàu và cabin", vì khách '
     'ngủ trên tàu'],
], widths=[1, 3])

h3('4.3.5. Combo bay + khách sạn')
p('Gói gồm vé máy bay khứ hồi và một số đêm khách sạn, có thể kèm xe đưa đón '
  'sân bay và vé tham quan.')
table(['Nội dung', 'Quy tắc'], [
    ['Lịch trình từng ngày', '**Không có.** Combo không phải là một hành trình '
     'được thiết kế mà là một gói dịch vụ.'],
    ['Điểm khởi hành',
     'Là biến thể sản phẩm riêng, không phải khoản phụ thu. Combo khởi hành từ '
     'Hà Nội và từ TP. Hồ Chí Minh là hai sản phẩm.'],
    ['Chọn khách sạn', 'Khách chọn trong danh sách khách sạn của gói, mỗi lựa '
     'chọn một mức giá'],
    ['Giá', 'Thay đổi theo ngày đi và theo khách sạn khách chọn, cập nhật ngay '
     'trên trang'],
    ['Chính sách huỷ',
     '**Tách riêng theo từng thành phần.** Vé máy bay thường không hoàn được, '
     'phòng khách sạn hoàn được tới sát ngày. Áp một tỷ lệ chung cho cả đơn là '
     'nguồn khiếu nại.'],
], widths=[1, 3])
note('Khuyến nghị về phạm vi combo ở giai đoạn 1',
     'Kết nối trực tiếp với hệ thống đặt vé máy bay và hệ thống phòng khách sạn '
     'theo thời gian thực là khối lượng công việc rất lớn, kèm chi phí hợp đồng '
     'và phí giao dịch với nhà cung cấp. Khuyến nghị giai đoạn đầu triển khai '
     'combo dưới dạng gói cố định do nhân viên soạn sẵn: nhân viên nhập tuyến '
     'bay, giờ bay, danh sách khách sạn và giá trọn gói theo từng khoảng ngày. '
     'Cách này giữ được phần lớn giá trị bán hàng với khối lượng nhỏ hơn nhiều, '
     'và không cản trở việc nâng cấp về sau.')

h3('4.3.6. Tour trong ngày')
table(['Nội dung', 'Quy tắc'], [
    ['Khung giờ', 'Mỗi ngày có nhiều khung giờ, **mỗi khung quản lý số chỗ '
     'riêng.** Khung 9:00 hết chỗ không có nghĩa khung 14:00 hết chỗ.'],
    ['Giá', 'Theo loại khách: người lớn, trẻ em, em bé'],
    ['Đóng bán', 'Trước giờ khởi hành một khoảng thời gian định trước'],
    ['Chính sách huỷ', 'Thường cho huỷ miễn phí trước 24 giờ'],
], widths=[1, 3])

page_break()

h2('4.4. Bảng đối chiếu chức năng giữa các loại')
table(['Chức năng', 'Tour\nđoàn', 'Tour\ncá nhân', 'Tour\nriêng', 'Du\nthuyền',
       'Combo', 'Tour\ntrong ngày'], [
    ['Lịch trình từng ngày', 'Có', 'Có', 'Có (mẫu)', 'Có', 'Không', 'Không'],
    ['Bảng ngày khởi hành', 'Có', 'Có', 'Không', 'Có (gộp)', 'Không', 'Không'],
    ['Lịch chọn ngày', 'Không', 'Có', 'Có', 'Không', 'Có', 'Có'],
    ['Đảm bảo khởi hành', 'Có', 'Không', 'Không', 'Có', 'Không', 'Không'],
    ['Trạng thái hết chỗ', 'Có', 'Không', 'Không', 'Theo hạng', 'Có',
     'Theo khung giờ'],
    ['Trạng thái chờ xác nhận', 'Có', 'Chủ đạo', 'Không', 'Có', 'Không', 'Không'],
    ['Giữ chỗ khi thanh toán', 'Có', 'Không', 'Không', 'Có', 'Có', 'Có'],
    ['Phụ thu phòng đơn', 'Có', 'Có', 'Không', 'Có', 'Không', 'Không'],
    ['Giá theo bậc số khách', 'Không', 'Không', 'Có', 'Không', 'Không', 'Không'],
    ['Giảm giá đặt sớm', 'Có', 'Có', 'Không', 'Có', 'Không', 'Không'],
    ['Bước báo giá', 'Không', 'Không', 'Có', 'Không', 'Không', 'Không'],
    ['Đặt và thanh toán online', 'Có', 'Có', 'Không', 'Có', 'Có', 'Có'],
    ['Công ty huỷ vì thiếu khách', 'Có', 'Không', 'Không', 'Có', 'Không',
     'Không'],
], widths=[1.5, 0.6, 0.65, 0.6, 0.65, 0.55, 0.75], size=9)

page_break()

# ---------------------------------------------------------------- 5

h1('5. Chức năng website khách hàng')

h2('5.1. Danh sách màn hình')
table(['Màn hình', 'Chức năng chính'], [
    ['Trang chủ', 'Giới thiệu Việt Nam, ô tìm tour, danh mục theo loại và mùa, '
     'tour nổi bật, thông tin thực tế (thị thực, mùa, tiền tệ, lệch giờ), '
     'đánh giá khách, bài viết, đăng ký nhận bản tin'],
    ['Tìm tour', 'Bộ lọc phân cấp miền → điểm đến, lọc theo loại sản phẩm, chủ '
     'đề, thời lượng, khoảng giá, tháng khởi hành'],
    ['Trang điểm đến', 'Giới thiệu điểm đến, điểm nhấn, mùa đẹp nhất, tour đi '
     'qua, khách sạn, tham quan tuỳ chọn'],
    ['Chi tiết sản phẩm', 'Xem mục 5.3'],
    ['Đặt tour', 'Quy trình 4 bước, xem mục 6'],
    ['Xác nhận đặt tour', 'Hiển thị mã tra cứu đơn hàng, gửi email xác nhận'],
    ['Tra cứu đơn hàng', 'Tra bằng mã đơn và email đã dùng khi đặt'],
    ['Bài viết', 'Danh sách và chi tiết, lọc theo chủ đề'],
    ['Liên hệ', 'Thông tin công ty, danh sách nhân viên tư vấn, biểu mẫu yêu '
     'cầu tư vấn'],
    ['Sự kiện', 'Buổi giới thiệu tour miễn phí, đăng ký giữ chỗ'],
], widths=[1, 3])

h2('5.2. Bốn quy tắc chung cho mọi màn hình')
bullets([
    ('Số liệu hiển thị luôn đếm từ dữ liệu thật. ',
     'Câu "Xem tất cả 92 tour" phải phản ánh đúng số tour đang bán ở thị trường '
     'và ngôn ngữ khách đang xem.'),
    ('Trạng thái bộ lọc nằm trong đường dẫn. ',
     'Tải lại trang vẫn giữ nguyên bộ lọc; gửi đường dẫn cho người khác thì họ '
     'thấy đúng kết quả đó.'),
    ('Mọi màn hình có dữ liệu đều có ba trạng thái: ',
     'đang tải, không có kết quả (kèm hướng dẫn hành động tiếp theo), và lỗi.'),
    ('Giá luôn kèm chữ "từ" và ghi chú điều kiện. ',
     'Đây là yêu cầu pháp lý của ngành lữ hành, không phải lựa chọn thiết kế.'),
])

h2('5.3. Trang chi tiết sản phẩm')
p('Bốn loại tour dài dùng chung một bố cục gồm phần đầu trang, dải thẻ nội '
  'dung, và thanh đặt tour cố định ở đáy màn hình. Số lượng thẻ nội dung khác '
  'nhau theo từng loại:')
table(['Thẻ nội dung', 'Tour đoàn', 'Tour cá nhân', 'Tour riêng', 'Du thuyền'], [
    ['Tổng quan', 'Có', 'Có', 'Có', 'Có'],
    ['Lịch trình', 'Có', 'Có', 'Có (lộ trình mẫu)', 'Có'],
    ['Khách sạn', 'Có', 'Có', 'Có', '**thay bằng Tàu và cabin**'],
    ['Giá và ngày khởi hành', 'Có', 'Có', '**thay bằng Bảng giá theo nhóm**',
     'Có'],
    ['Trưởng đoàn', 'Có', 'Không', 'Có', 'Tuỳ tour'],
    ['Thời tiết', 'Có', 'Có', 'Có', 'Có'],
    ['Thông tin thực tế', 'Có', 'Có', 'Có', 'Có'],
    ['**Tổng số thẻ**', '**7**', '**6**', '**6**', '**6–7**'],
], widths=[1.4, 0.85, 0.9, 1.25, 0.9], size=9.5)
p('Combo và tour trong ngày không dùng bố cục thẻ nội dung. Hai loại này dùng '
  'một trang cuộn liền mạch với khung chọn ngày và số khách đặt cố định bên '
  'phải, do sản phẩm không có lịch trình từng ngày để trình bày.')

h3('Dòng thông tin dưới tên sản phẩm')
p('Đây là dòng giúp khách nhận ra ngay sản phẩm vận hành thế nào, và khác nhau '
  'theo từng loại:')
table(['Loại', 'Nội dung dòng thông tin'], [
    ['Tour đoàn', 'Tour đoàn có trưởng đoàn · 15 ngày · từ 24.990 kr.\n'
     '10–25 khách · 7 ngày khởi hành · mức vận động 2/4'],
    ['Tour cá nhân', 'Tour cá nhân · 14 ngày · từ 19.895 kr.\n'
     'Hà Nội (4 đêm) · Hội An (3) · vịnh Hạ Long (2)'],
    ['Tour riêng', 'Tour riêng · 5 ngày · từ 4.290.000 ₫/khách khi đi 2 người\n'
     'Khởi hành theo yêu cầu · báo trước tối thiểu 7 ngày'],
    ['Du thuyền', 'Du thuyền · 17 ngày · từ 36.990 kr.\n'
     'Tàu Mekong Princess · 8 cảng ghé · 4 hạng cabin'],
], widths=[1, 3])

h3('Các tình huống đặc biệt')
table(['Tình huống', 'Hệ thống hiển thị'], [
    ['Tất cả ngày khởi hành đã hết chỗ',
     'Trang vẫn mở đầy đủ. Mục giá và ngày hiển thị thông báo kèm biểu mẫu nhận '
     'thông báo khi mở ngày mới, và ba tour tương tự. Không trả về trang lỗi.'],
    ['Sản phẩm chưa dịch sang ngôn ngữ đang xem',
     'Không truy cập được ở ngôn ngữ đó'],
    ['Sản phẩm chưa được mở bán ở thị trường đang xem',
     'Không truy cập được ở thị trường đó'],
    ['Chưa có ngày khởi hành nào',
     'Hiển thị "Chương trình đang được hoàn thiện", nút chuyển thành "Yêu cầu '
     'tư vấn"'],
    ['Chưa có đánh giá', 'Ẩn hẳn dòng đánh giá, không hiển thị "chưa có đánh giá"'],
], widths=[1.2, 2.8])

page_break()

# ---------------------------------------------------------------- 6

h1('6. Nghiệp vụ đặt tour')

h2('6.1. Quy trình đặt tour trực tuyến')
p('Áp dụng cho tour đoàn, tour cá nhân, du thuyền và combo.')
table(['Bước', 'Nội dung', 'Ghi chú'], [
    ['1', 'Chọn ngày khởi hành', 'Du thuyền chọn thêm hạng cabin'],
    ['2', 'Chọn số khách và loại phòng',
     'Khách đi một mình thấy phụ thu phòng đơn ngay tại bước này'],
    ['3', 'Chọn dịch vụ thêm',
     'Bảo hiểm, đêm khách sạn trước ngày bay, điểm khởi hành khác'],
    ['4', 'Nhập thông tin và thanh toán',
     'Hệ thống giữ chỗ 20 phút kể từ khi vào bước này'],
    ['5', 'Trang xác nhận', 'Hiển thị mã tra cứu, gửi email xác nhận'],
], widths=[0.4, 1.6, 2])
p('Bảng chi tiết giá được hiển thị và cập nhật tức thời ở mọi bước, khách luôn '
  'nhìn thấy tổng tiền trước khi sang bước sau.')

h2('6.2. Quản lý số chỗ và giữ chỗ')
table(['Sự kiện', 'Hệ thống xử lý'], [
    ['Khách vào bước thanh toán', 'Giữ chỗ trong 20 phút'],
    ['Thanh toán thành công', 'Chuyển giữ chỗ thành đơn hàng, trừ chỗ khỏi kho'],
    ['Quá 20 phút chưa thanh toán', 'Trả chỗ về kho tự động'],
    ['Khách huỷ đơn', 'Trả chỗ về kho ngay lập tức, không chờ hoàn tiền xong'],
], widths=[1.2, 2.8])
note('Số chỗ khả dụng đã trừ cả chỗ đang được giữ',
     'Khi tính số chỗ còn lại hiển thị cho khách, hệ thống trừ cả những chỗ mà '
     'khách khác đang giữ nhưng chưa thanh toán xong. Nhờ vậy hai khách không '
     'thể cùng mua được chỗ cuối cùng. Đây là điểm được kiểm thử riêng khi '
     'nghiệm thu.')

h2('6.3. Trạng thái đơn hàng')
table(['Trạng thái', 'Ý nghĩa'], [
    ['Chờ thanh toán', 'Đã tạo đơn, chưa nhận được tiền'],
    ['Chờ xác nhận', 'Đã thanh toán, chờ nhân viên xác nhận trong 24 giờ — chỉ '
     'áp dụng cho tour cá nhân'],
    ['Đã xác nhận', 'Đơn hàng có hiệu lực'],
    ['Hoàn thành', 'Khách đã đi tour'],
    ['Đã huỷ', 'Do khách huỷ hoặc công ty huỷ chuyến'],
    ['Đã hoàn tiền', 'Đã xử lý xong việc hoàn tiền'],
    ['Hết hạn', 'Khách bỏ dở giữa chừng, chỗ đã được trả về kho'],
], widths=[1, 3])
p('Mọi lần thay đổi trạng thái đều được ghi nhật ký kèm người thực hiện và thời '
  'điểm, phục vụ tra cứu khi có khiếu nại.')

page_break()

# ---------------------------------------------------------------- 7

h1('7. Quy tắc tính giá')

h2('7.1. Thứ tự cộng dồn')
p('Thứ tự các khoản được cộng dồn là cố định và ảnh hưởng tới con số cuối cùng, '
  'vì khoản giảm giá đặt sớm được trừ trước khi cộng phí xử lý:')
table(['Thứ tự', 'Khoản mục', 'Áp dụng cho'], [
    ['1', 'Giá cơ bản — số khách theo từng loại nhân đơn giá', 'Mọi loại'],
    ['2', 'Phụ thu phòng đơn', 'Tour đoàn, tour cá nhân, du thuyền'],
    ['3', 'Nâng hạng cabin', 'Du thuyền'],
    ['4', 'Phụ thu điểm khởi hành khác', 'Thị trường Đan Mạch'],
    ['5', 'Bảo hiểm du lịch', 'Tuỳ chọn'],
    ['6', 'Đêm khách sạn trước ngày bay', 'Tuỳ chọn'],
    ['7', '**Trừ giảm giá đặt sớm**', 'Tour đoàn, tour cá nhân, du thuyền'],
    ['8', '**Cộng phí xử lý** (một lần mỗi đơn)', 'Mọi loại trừ tour trong ngày'],
    ['', '**= Tổng cộng.** Đặt cọc = tổng × tỷ lệ đặt cọc của thị trường', ''],
], widths=[0.5, 2.5, 1.2])

h2('7.2. Ví dụ minh hoạ')
p('Tour đoàn, thị trường Đan Mạch, 2 người lớn ở phòng đôi, khởi hành từ '
  'Billund, có mua bảo hiểm, thêm 1 đêm khách sạn trước ngày bay, đặt trước 7 '
  'tháng:')
table(['Khoản mục', 'Số lượng', 'Đơn giá', 'Thành tiền'], [
    ['Giá cơ bản', '2', '24.990', '49.980'],
    ['Phụ thu điểm khởi hành (Billund)', '2', '800', '1.600'],
    ['Bảo hiểm', '2', '895', '1.790'],
    ['Đêm khách sạn trước bay', '1 phòng', '1.095', '1.095'],
    ['Giảm giá đặt sớm (từ 6 tháng)', '2', '−1.000', '**−2.000**'],
    ['Phí xử lý', '1', '295', '295'],
    ['**Tổng cộng**', '', '', '**52.760 kr.**'],
    ['Đặt cọc 25%', '', '', '13.190 kr.'],
    ['Còn lại', '', '', '39.570 kr.'],
], widths=[2.2, 0.8, 0.8, 1])

h2('7.3. Phân loại khách và giá theo loại khách')
p('Hai thị trường phân loại khách khác nhau. Hệ thống cho phép cấu hình riêng '
  'cho từng thị trường, không cố định trong phần mềm:')
table(['Thị trường', 'Phân loại', 'Ghi chú'], [
    ['Đan Mạch', 'Người lớn · Trẻ em', 'Ở phòng đơn là khoản phụ thu, không '
     'phải loại khách riêng'],
    ['Việt Nam', 'Người lớn · Trẻ em 5–11 · Trẻ nhỏ 2–4 · Em bé dưới 2',
     'Tỷ lệ giảm theo từng bậc, cần khách hàng xác nhận'],
], widths=[0.9, 1.9, 1.6])

h2('7.4. Quy tắc làm tròn')
bullets([
    'Số chữ số thập phân theo tiền tệ của thị trường: krone Đan Mạch có 2 chữ '
    'số, đồng Việt Nam không có chữ số thập phân.',
    'Làm tròn tại từng dòng của bảng chi tiết giá, sau đó mới cộng lại. Nhờ vậy '
    'các dòng cộng lại luôn bằng đúng tổng hiển thị cho khách.',
    'Tiền đặt cọc làm tròn xuống, phần còn lại lấy bằng tổng trừ đặt cọc, đảm '
    'bảo đặt cọc cộng phần còn lại luôn bằng tổng.',
])

page_break()

# ---------------------------------------------------------------- 8

h1('8. Trang quản trị')

h2('8.1. Vai trò và quyền')
table(['Vai trò', 'Được làm gì'], [
    ['Nhân viên tư vấn', 'Xem yêu cầu tư vấn, xem đơn hàng, dựng và gửi báo giá '
     'tour riêng'],
    ['Biên tập nội dung', 'Tạo và sửa nội dung bằng ngôn ngữ gốc, gửi duyệt'],
    ['Biên dịch', 'Dịch nội dung sang ngôn ngữ thứ hai, xử lý hàng đợi dịch'],
    ['Quản trị viên', 'Toàn quyền, bao gồm giá, ngày khởi hành, số chỗ, mở bán '
     'sản phẩm và quản lý tài khoản'],
], widths=[1, 3])

h2('8.2. Nhóm chức năng')
table(['Nhóm', 'Chức năng'], [
    ['Sản phẩm', 'Tạo và sửa sản phẩm, lịch trình từng ngày, chặng khách sạn, '
     'lộ trình, bộ ảnh'],
    ['Mở bán theo thị trường', 'Gán sản phẩm vào từng thị trường và nhập giá '
     'riêng cho từng thị trường'],
    ['Ngày khởi hành', 'Tạo, sửa, đóng bán, nhập giá theo từng ngày và từng '
     'loại khách'],
    ['Bậc giá', 'Nhập bậc giá theo số khách cho tour riêng'],
    ['Đơn hàng', 'Xem, đổi trạng thái, xử lý huỷ và hoàn tiền'],
    ['Báo giá', 'Dựng báo giá, gửi khách, theo dõi hiệu lực'],
    ['Yêu cầu tư vấn', 'Xem và phân công'],
    ['Nội dung', 'Điểm đến, khách sạn, tham quan tuỳ chọn, bài viết, sự kiện, '
     'nhân viên tư vấn'],
    ['Dịch thuật', 'Ba màn hình riêng — xem mục 8.3'],
    ['Người dùng', 'Tạo tài khoản nhân viên, gán vai trò'],
], widths=[1, 3])

h2('8.3. Quy trình biên tập và dịch nội dung')
p('Nội dung được viết bằng ngôn ngữ gốc trước, dịch sau. Quy trình:')
bullets([
    'Biên tập viên viết nội dung bằng ngôn ngữ gốc và gửi duyệt',
    'Nội dung được duyệt sẽ tự động vào hàng đợi dịch',
    'Biên dịch viên dịch sang ngôn ngữ thứ hai',
    'Bản dịch được duyệt và xuất bản lên website',
    'Khi bản gốc được sửa, bản dịch tự động chuyển sang trạng thái "cần dịch '
    'lại" và quay lại hàng đợi',
])
p('Trang quản trị có ba màn hình phục vụ quy trình này:')
table(['Màn hình', 'Chức năng'], [
    ['Hàng đợi dịch', 'Danh sách nội dung đã có bản gốc nhưng thiếu bản dịch '
     'hoặc bản dịch đã lỗi thời, sắp theo mức ưu tiên'],
    ['Màn hình dịch song song', 'Bản gốc bên trái, ô nhập bản dịch bên phải, '
     'theo từng trường nội dung'],
    ['Bảng độ phủ bản dịch', 'Tỷ lệ phần trăm đã dịch của từng loại nội dung'],
], widths=[1.1, 2.9])
note('Không dùng dịch máy tự động cho nội dung bán hàng',
     'Nội dung tour là văn bản thuyết phục, có chi tiết địa danh và ẩm thực. '
     'Bản dịch máy đọc được nhưng không bán được hàng. Chữ trên giao diện thì '
     'có thể dịch máy rồi người rà soát lại.')

page_break()

# ---------------------------------------------------------------- 9

h1('9. Phạm vi triển khai')

h2('9.1. Giai đoạn 1')
table(['Nhóm', 'Nội dung'], [
    ['Sản phẩm', 'Bốn loại: tour đoàn, tour cá nhân, tour riêng, du thuyền'],
    ['Thị trường', 'Đan Mạch và Việt Nam'],
    ['Ngôn ngữ', 'Tiếng Đan Mạch và tiếng Việt'],
    ['Website khách', 'Toàn bộ màn hình ở mục 5.1'],
    ['Đặt tour', 'Quy trình 4 bước, giữ chỗ, thanh toán trực tuyến'],
    ['Báo giá', 'Quy trình báo giá đầy đủ cho tour riêng'],
    ['Trang quản trị', 'Toàn bộ nhóm chức năng ở mục 8.2'],
], widths=[1, 3])

h2('9.2. Giai đoạn sau')
bullets([
    'Combo bay + khách sạn (giai đoạn 1.5)',
    'Tour trong ngày bán độc lập',
    'Tài khoản khách hàng và cổng tra cứu đơn hàng',
    'Ngôn ngữ thứ ba (tiếng Anh)',
    'Tour liên quốc gia',
    'Bản đồ tương tác',
    'Trò chuyện trực tiếp',
    'Ứng dụng di động',
    'Khách hàng tự gửi đánh giá',
])

h2('9.3. Nội dung không nằm trong phạm vi')
table(['Nội dung', 'Lý do'], [
    ['Đặt vé máy bay lẻ', 'Không thuộc nghiệp vụ của công ty'],
    ['So sánh giá với đối thủ', 'Không thuộc nghiệp vụ'],
    ['Tính năng mạng xã hội', 'Không phù hợp nhóm khách hàng mục tiêu'],
    ['Gợi ý cá nhân hoá bằng trí tuệ nhân tạo',
     'Quy mô dữ liệu không đủ; nhóm khách lớn tuổi phản ứng không tốt với nội '
     'dung thay đổi liên tục'],
    ['Quy đổi tỷ giá tự động', 'Giá mỗi thị trường do công ty nhập trực tiếp — '
     'xem mục 3.2'],
], widths=[1.2, 2.8])

page_break()

# ---------------------------------------------------------------- 10

h1('10. Yêu cầu ngoài chức năng')

h2('10.1. Khả năng tiếp cận')
p('Website phục vụ nhóm khách trung niên và cao tuổi, nên các yêu cầu sau là '
  'bắt buộc chứ không phải khuyến nghị:')
bullets([
    'Đạt chuẩn WCAG mức AA về độ tương phản màu',
    'Cỡ chữ nội dung tối thiểu 16px',
    'Vùng bấm tối thiểu 44×44px',
    'Điều hướng được hoàn toàn bằng bàn phím',
    'Không dùng hiệu ứng rê chuột làm cách duy nhất để hiện thông tin',
    'Các dải nội dung không tự động chạy',
])

h2('10.2. Hiển thị đa thiết bị')
p('Giao diện được kiểm tra thực tế ở ba bề ngang màn hình: 375px (điện thoại), '
  '768px (máy tính bảng) và 1440px (máy tính). Không màn hình nào được cuộn '
  'ngang toàn trang; bảng và biểu đồ rộng cuộn trong khung riêng.')

h2('10.3. Phông chữ')
p('Toàn bộ phông chữ dùng cho tiêu đề và nội dung phải hỗ trợ đầy đủ ký tự '
  'tiếng Đan Mạch (æ, ø, å) và dấu tiếng Việt. Đây là ràng buộc kỹ thuật thực '
  'tế: nhiều phông chữ phổ biến thiếu một trong hai bộ ký tự, gây lỗi hiển thị '
  'chữ bị vỡ hoặc thiếu dấu.')

h2('10.4. Yêu cầu pháp lý cần tuân thủ')
table(['Thị trường', 'Quy định'], [
    ['Đan Mạch / EU', 'Chỉ thị EU về gói du lịch; đăng ký Quỹ bảo đảm du lịch; '
     'Quy định bảo vệ dữ liệu cá nhân GDPR; quy tắc hiển thị giá của ngành lữ hành'],
    ['Việt Nam', 'Luật Du lịch 2017; giấy phép kinh doanh lữ hành và ký quỹ; '
     'Nghị định 13/2023 về bảo vệ dữ liệu cá nhân; hoá đơn điện tử theo Nghị '
     'định 123/2020'],
], widths=[1, 3])
note('Cần thẩm định pháp lý',
     'Nội dung mục này do đơn vị triển khai tổng hợp và cần được luật sư hoặc '
     'bộ phận pháp chế của khách hàng thẩm định trước khi hệ thống nhận thanh '
     'toán thật.')

h2('10.5. Bảo mật và dữ liệu cá nhân')
bullets([
    'Toàn bộ dữ liệu truyền qua kết nối mã hoá',
    'Thông tin hộ chiếu chỉ thu thập khi thực sự cần và được mã hoá khi lưu trữ',
    'Có cơ chế giới hạn số lần tra cứu đơn hàng để tránh dò mã',
    'Nhật ký thay đổi trạng thái đơn hàng được lưu đầy đủ',
    'Thời hạn lưu trữ dữ liệu cá nhân cần được khách hàng xác nhận',
])

page_break()

# ---------------------------------------------------------------- 11

h1('11. Nội dung cần khách hàng xác nhận')
p('Các nội dung dưới đây ảnh hưởng trực tiếp tới thiết kế hệ thống. Thay đổi '
  'sau khi đã triển khai sẽ phát sinh chi phí đáng kể, đặc biệt là các mục '
  'đánh dấu mức ảnh hưởng Cao.')
table(['#', 'Nội dung cần xác nhận', 'Ảnh hưởng'], [
    ['1', 'Giai đoạn 1 có thanh toán trực tuyến thật, hay chỉ nhận đặt chỗ rồi '
     'nhân viên gọi điện chốt?', '**Cao**'],
    ['2', 'Thị trường Việt Nam bán những tour nào — danh mục sản phẩm cụ thể',
     '**Cao**'],
    ['3', 'Tỷ lệ đặt cọc áp dụng cho thị trường Việt Nam', 'Trung bình'],
    ['4', 'Phí xử lý đơn hàng ở thị trường Việt Nam', 'Trung bình'],
    ['5', 'Thị trường Việt Nam có áp dụng giảm giá đặt sớm không, mức bao nhiêu',
     'Trung bình'],
    ['6', 'Giá bằng đồng Việt Nam có làm tròn tới nghìn đồng không',
     'Thấp'],
    ['7', 'Tỷ lệ giảm giá theo từng bậc tuổi trẻ em, và ngưỡng tuổi của từng bậc',
     'Trung bình'],
    ['8', 'Thời gian giữ chỗ 20 phút có phù hợp không', 'Thấp'],
    ['9', 'Bậc thời gian huỷ tour và tỷ lệ hoàn tiền tương ứng, cho từng thị '
     'trường', '**Cao**'],
    ['10', 'Thời hạn công ty chốt huỷ chuyến khi không đủ số khách tối thiểu',
     'Trung bình'],
    ['11', 'Bậc giá theo số khách của tour riêng chia thế nào', 'Trung bình'],
    ['12', 'Thời gian xác nhận đơn tour cá nhân có đúng 24 giờ không',
     'Trung bình'],
    ['13', 'Ai chịu trách nhiệm viết nội dung bằng ngôn ngữ gốc', '**Cao**'],
    ['14', 'Thời hạn lưu trữ dữ liệu cá nhân của khách hàng', 'Trung bình'],
], widths=[0.35, 3.2, 0.8])

note('Mục 1, 2, 9 và 13 nên được trả lời trước khi bắt đầu lập trình',
     'Mục 1 quyết định có triển khai tích hợp cổng thanh toán ở giai đoạn 1 hay '
     'không. Mục 2 quyết định khối lượng nhập liệu. Mục 9 ảnh hưởng tới thiết '
     'kế nghiệp vụ hoàn tiền. Mục 13 là ràng buộc nhân sự: hệ thống hoàn thiện '
     'mà không có nội dung thì vẫn chưa thể ra mắt.')

page_break()

# ---------------------------------------------------------------- 12

h1('12. Phụ lục — Bảng thuật ngữ')
table(['Thuật ngữ', 'Giải thích'], [
    ['Miền', 'Cấp phân loại địa lý cao nhất: Bắc, Trung, Nam'],
    ['Điểm đến', 'Một địa danh cụ thể trong nước: Hà Nội, Sa Pa, vịnh Hạ Long…'],
    ['Thị trường', 'Nhóm khách hàng theo quốc gia, quyết định danh mục sản '
     'phẩm, giá, tiền tệ và quy định pháp lý áp dụng'],
    ['Ngôn ngữ', 'Ngôn ngữ hiển thị của giao diện và nội dung'],
    ['Ngôn ngữ gốc', 'Ngôn ngữ nội dung được viết đầu tiên, trước khi dịch'],
    ['Ngày khởi hành', 'Một ngày cụ thể của một tour, có giá và số chỗ riêng'],
    ['Đảm bảo khởi hành', 'Chuyến đã đủ số khách tối thiểu, chắc chắn chạy'],
    ['Còn ít chỗ', 'Số chỗ trống còn từ 3 trở xuống'],
    ['Giữ chỗ', 'Việc khoá tạm số chỗ trong lúc khách hoàn tất thanh toán'],
    ['Phụ thu phòng đơn', 'Khoản chênh khi khách ở một mình một phòng'],
    ['Giảm giá đặt sớm', 'Ưu đãi cho khách đặt trước ngày khởi hành đủ lâu'],
    ['Bậc giá theo số khách', 'Bảng giá của tour riêng, giá mỗi người giảm dần '
     'khi nhóm đông hơn'],
    ['Báo giá', 'Bản chào giá do nhân viên dựng riêng cho một yêu cầu tour '
     'riêng, có thời hạn hiệu lực'],
    ['Yêu cầu tư vấn', 'Thông tin liên hệ khách để lại khi chưa sẵn sàng đặt tour'],
    ['Tham quan tuỳ chọn', 'Buổi tham quan khách mua thêm tại chỗ, không nằm '
     'trong giá tour'],
    ['Chặng khách sạn', 'Số đêm tour nghỉ tại một khách sạn cụ thể'],
], widths=[1, 3])

# ---------------------------------------------------------------- footer

footer = doc.sections[0].footer
fp = footer.paragraphs[0]
fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = fp.add_run('Đặc tả chức năng và nghiệp vụ  ·  Website bán tour du lịch '
               'Việt Nam  ·  Trang ')
r.font.size = Pt(8.5)
r.font.color.rgb = MUTED
set_font(r, 'Calibri')
pr = add_field(fp, 'PAGE')
pr.font.size = Pt(8.5)
pr.font.color.rgb = MUTED
set_font(pr, 'Calibri')

doc.save(OUT)
print('Đã tạo:', OUT)
