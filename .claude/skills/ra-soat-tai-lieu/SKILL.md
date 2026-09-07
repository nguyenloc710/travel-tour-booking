---
name: ra-soat-tai-lieu
description: Rà soát bộ tài liệu — chạy bộ kiểm, xử lý file quá hạn 90 ngày, tìm chỗ tài liệu lệch với code, tìm nội dung trùng lặp giữa hai file. Dùng khi được yêu cầu kiểm tài liệu, dọn tài liệu, hoặc sau khi code đã đi trước tài liệu một quãng.
---

# Rà soát tài liệu

Quy trình ở `docs/42-quy-trinh-tai-lieu.md` mục 7 và 8.

## Bước 1 — Chạy bộ kiểm

```bash
python scripts/docs_check.py
```

Xử lý theo mức, không xử lý lẫn lộn:

| Mức | Nghĩa | Việc |
|---|---|---|
| **LỖI** | Vi phạm quy ước | Sửa hết. Không được để tồn |
| **CẢNH BÁO** | Quá hạn rà soát, ADR chưa viết, nguồn sự thật trùng | Xem xét từng cái, có thể để lại kèm lý do |
| **CHƯA VIẾT** | Tài liệu đã đăng ký mà chưa có | Không phải lỗi. Là việc chưa làm |

## Bước 2 — File quá hạn 90 ngày

Với mỗi file bộ kiểm báo quá hạn, **đọc file rồi kết luận một trong ba**, không
được chỉ đổi ngày cho hết cảnh báo:

| Kết luận | Việc |
|---|---|
| Còn đúng | `python scripts/docs_check.py --sua <file>` |
| Đúng nhưng thiếu | Bổ sung, tăng `Phiên bản` phần `minor`, đổi ngày |
| Không còn đúng | Chuyển `Lỗi thời` hoặc viết lại, tăng `major` |

> **Đổi ngày mà không đọc file là làm hỏng cơ chế.** Cả bộ tài liệu sẽ "tươi"
> trên giấy trong khi nội dung mục ruỗng — đúng thứ quy trình này sinh ra để
> ngăn.

## Bước 3 — Tài liệu lệch với hiện thực

Chỉ làm khi đã có code. Đối chiếu bốn cặp hay lệch nhất:

| Tài liệu | Đối chiếu với | Lệch thì sửa bên nào |
|---|---|---|
| `docs/12` lược đồ CSDL | Migration Flyway thật | **Sửa tài liệu** — CSDL là hiện thực |
| `docs/13` danh mục endpoint | `contracts/openapi.yaml` | **Sửa tài liệu** — spec là hợp đồng |
| `docs/14` hằng số nghiệp vụ | Cấu hình market trong code | Hỏi: con số nào mới là con số đã quyết |
| `CLAUDE.md` mục lệnh | Lệnh chạy được thật | **Sửa `CLAUDE.md`** |

Lệch ở `docs/02` hoặc `docs/04` thì ngược lại: **sửa code**, vì đó là quyết định
nghiệp vụ, không phải mô tả hiện thực.

## Bước 4 — Trùng lặp

Bảng "một sự thật một chỗ" ở `docs/42` mục 6. Với mỗi sự thật trong bảng, tìm
xem có file nào khác đang mô tả lại không:

```bash
grep -rn "<cụm từ đặc trưng>" docs/ CLAUDE.md api/CLAUDE.md web/CLAUDE.md
```

Tìm thấy thì giữ bản ở file chủ sở hữu, chỗ còn lại thay bằng **một câu và một
link**. Ba file `CLAUDE.md` được phép tóm tắt nhưng phải ghi rõ nguồn.

## Bước 5 — Báo cáo

Báo ngắn: đã sửa gì, còn gì cần người quyết, file nào đề nghị chuyển `Lỗi thời`.
Cập nhật `docs/41` mục 9.

**Không tự nâng trạng thái tài liệu lên `Đã duyệt`.** Đó là thẩm quyền của người
duyệt ghi ở `docs/40` mục 5.
