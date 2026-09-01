---
name: ban-giao-khach
description: Sinh lại bản Word đặc tả chức năng và nghiệp vụ gửi khách hàng từ bộ tài liệu docs/. Dùng khi được yêu cầu làm tài liệu gửi khách, xuất file Word, cập nhật bản đặc tả cho khách hàng.
---

# Bản Word gửi khách hàng

Đích: `docs/Dac-ta-chuc-nang-nghiep-vu.docx`
Script sinh: `docs/tools/build-docx.py` (dùng `python-docx`, đã có sẵn)

```bash
python docs/tools/build-docx.py
```

**Không sửa trực tiếp file `.docx`.** Sửa script rồi sinh lại — sửa file Word sẽ
bị ghi đè ở lần sinh kế tiếp mà không ai biết.

## Chuyển văn phong — việc chính của skill này

Tài liệu trong `docs/` viết cho đội kỹ thuật. Bản gửi khách phải bỏ hết tầng kỹ
thuật và giữ nguyên tầng nghiệp vụ.

| Bỏ hoàn toàn | Giữ và diễn đạt lại |
|---|---|
| SQL, DDL, tên bảng, tên cột, index, collation | Quy tắc dữ liệu diễn đạt bằng nghiệp vụ |
| Java, Spring, Gradle, module, ArchUnit | — |
| OpenAPI, endpoint, mã lỗi, header | Chức năng mà endpoint đó phục vụ |
| ADR, bố cục repo, CI/CD | Quyết định và **lý do**, không kèm cơ chế |
| Testcontainers, migration, `f_unaccent` | — |

Ví dụ chuyển:

| Trong `docs/` | Trong bản gửi khách |
|---|---|
| "Khoá bi quan `SELECT … FOR UPDATE` trên dòng departure" | "Hai khách không thể cùng mua chỗ cuối cùng" |
| "`INNER JOIN` chính là chính sách không-fallback" | "Tour chưa dịch xong sẽ ẩn khỏi ngôn ngữ đó" |
| "`amount` là chuỗi trong JSON" | (bỏ — khách không cần biết) |

## Cấu trúc bắt buộc

Trang bìa có bảng kiểm soát tài liệu · Mục lục tự động · 12 chương từ Giới thiệu
đến Phụ lục thuật ngữ.

Hai chương **không được bỏ**, vì chúng là phần khách hàng cần nhất:

- **Chương "Nội dung cần khách hàng xác nhận"** — mọi câu hỏi đang treo ở
  `docs/41` mục 4, kèm mức ảnh hưởng. Đây là chương có giá trị thực tế cao nhất
- **Chương "Phạm vi triển khai"** — gồm cả danh sách **không làm** ở `docs/01`.
  Bỏ mục này là mở đường cho tranh cãi phạm vi về sau

## Trước khi bàn giao

- [ ] Nội dung khớp `docs/` hiện tại — chạy `python scripts/docs_check.py` trước
- [ ] Danh sách câu hỏi khớp `docs/41` mục 4
- [ ] Ba chỗ placeholder đã điền: `[Tên công ty]`, `[Tên đơn vị]`, `[Họ tên]`
- [ ] Mở bằng Word, `Ctrl+A` rồi `F9` để sinh mục lục
- [ ] Kiểm hiển thị `æ ø å` và dấu tiếng Việt
- [ ] Phần pháp lý ghi rõ **chưa qua thẩm định pháp lý** — không được bỏ dòng này

## Sau khi bàn giao

Ghi vào `docs/41` mục 9: đã gửi bản nào, ngày nào, cho ai. Khách trả lời câu hỏi
nào thì cập nhật `docs/41` mục 4 và tài liệu gốc tương ứng — **không** chỉ sửa
trong file Word.
