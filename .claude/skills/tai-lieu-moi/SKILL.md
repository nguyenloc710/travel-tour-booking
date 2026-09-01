---
name: tai-lieu-moi
description: Viết một tài liệu mới trong docs/ đúng chuẩn dự án — khối trạng thái, đánh số, đăng ký vào bản đồ 00, không lấn nguồn sự thật của file khác. Dùng khi được yêu cầu viết docs/NN-*.md, viết tài liệu đợt tiếp theo, hoặc thêm ADR.
---

# Viết tài liệu mới

Quy ước đầy đủ ở `docs/42-quy-trinh-tai-lieu.md`. Skill này là thứ tự thao tác,
không phải bản sao của quy ước — khi lệch thì `42` đúng.

## 1. Trước khi viết dòng nào

Đọc theo đúng thứ tự này, đừng bỏ bước:

1. `docs/00-ke-hoach-tai-lieu.md` — file này đã được đăng ký chưa, chịu trách
   nhiệm về **nguồn sự thật** nào
2. `docs/41-tinh-trang.md` — đang ở giai đoạn nào, có quyết định nào đang chờ mà
   tài liệu này phụ thuộc không
3. `docs/42-quy-trinh-tai-lieu.md` mục 6 — bảng "một sự thật một chỗ"
4. Hai đến ba tài liệu **lân cận cùng tầng** — để bắt được giọng văn, mật độ
   bảng, cách nêu cái bẫy

Nếu tài liệu phụ thuộc một quyết định đang chờ ở `41` mục 4: **viết được phần
không phụ thuộc, để lại phần phụ thuộc dưới dạng mục "chưa chốt" có tên câu
hỏi**. Đừng tự chọn giùm rồi viết như thể đã chốt.

## 2. Khối trạng thái

Mở đầu bằng tiêu đề `#`, rồi khối trong hàng rào ba backtick. Tài liệu mới luôn
bắt đầu ở `Nháp` — không tự đặt `Đã duyệt`, đó là việc của người ký.

```
Trạng thái: Nháp
Cập nhật: <hôm nay, dd/mm/yyyy>
Nguồn sự thật về: <một câu>
Không nói về: <trỏ sang file chịu trách nhiệm phần đó>
```

ADR dùng khối ngắn hơn: `Trạng thái` (`Đề xuất` / `Đã chốt`) và `Ngày`.

## 3. Viết

- **Tiếng Việt.** Định danh code giữ tiếng Anh, trong backtick
- **Bảng thay cho đoạn văn** khi nội dung có cấu trúc
- **Nêu cái bẫy, không chỉ nêu quy tắc.** "Đây là chỗ lập trình viên mặc định làm
  ngược" có giá trị hơn ba đoạn giải thích đúng
- **Ghi cả phương án bị loại và lý do loại**
- **Con số phải có nguồn** — nói rõ là quyết định hay là khảo sát
- Trỏ tài liệu khác bằng `` `NN` `` hoặc `` `NN` mục X ``, không chép nội dung
- Không emoji

Quyết định khó đảo ngược phát sinh khi viết → **tách thành ADR**, không nhét vào
thân tài liệu. Tiêu chí ở `42` mục 5.3.

## 4. Sau khi viết

```bash
python scripts/docs_check.py docs/NN-ten-file.md
```

Rồi làm đủ ba việc, thiếu một việc là tài liệu mồ côi:

1. **Đăng ký vào `docs/00`** — thêm dòng vào bảng của tầng tương ứng
2. **Cập nhật `docs/41`** — bảng ở mục 2 và nhật ký phiên ở mục 9
3. `python scripts/docs_check.py` — kiểm toàn bộ, phải 0 lỗi

## 5. Bẫy thường gặp

| Bẫy | Dấu hiệu | Xử lý |
|---|---|---|
| Lấn nguồn sự thật | Đang giải thích lại thứ file khác đã nói | Xoá, thay bằng một câu và một link |
| Viết như đã chốt | Có con số mà không ai từng quyết | Đưa vào mục "chưa chốt", ghi tên câu hỏi ở `41` |
| Quên đăng ký | `docs_check` báo "chưa đăng ký vào bản đồ 00" | Thêm dòng vào `00` |
| Đặt `Đã duyệt` cho tiện | Bộ kiểm đòi `Phiên bản`, `Chủ sở hữu`, `Người duyệt` | Để `Nháp`. Việc duyệt là của người, không phải của người viết |
