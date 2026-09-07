---
description: Chạy bộ kiểm tài liệu và sửa những lỗi tìm được
argument-hint: "[đường dẫn file, để trống = kiểm toàn bộ]"
---

Chạy bộ kiểm tài liệu và sửa lỗi.

Mục tiêu kiểm: $ARGUMENTS

Dùng skill `ra-soat-tai-lieu`. Thứ tự:

1. `python scripts/docs_check.py $ARGUMENTS`
2. Sửa **hết** mức LỖI
3. Với mức CẢNH BÁO: xem xét từng cái, cái nào cần người quyết thì hỏi, đừng tự quyết
4. Mức CHƯA VIẾT bỏ qua — đó là việc chưa làm, không phải lỗi
5. Chạy lại bộ kiểm, xác nhận 0 lỗi
6. Cập nhật `docs/41` mục 9 nếu có sửa gì đáng kể

Không đổi ngày `Cập nhật` của một file mà không đọc nội dung file đó — lý do ở `docs/42` mục 8.
