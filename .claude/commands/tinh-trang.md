---
description: Tóm tắt dự án đang ở đâu — giai đoạn, việc kế tiếp, quyết định đang chờ
allowed-tools: Read, Grep, Glob, Bash(git status:*), Bash(git log:*), Bash(python scripts/docs_check.py:*)
---

Tóm tắt trạng thái dự án cho tôi.

1. Đọc `docs/41-tinh-trang.md`
2. Chạy `python scripts/docs_check.py` để biết tài liệu có lỗi gì không
3. Chạy `git status --short` và `git log --oneline -5`
4. Đối chiếu: `41` nói đang ở đâu, thực tế thư mục làm việc có khớp không

Trả lời gọn, đúng năm mục:

- **Giai đoạn**: đang ở cổng nào, cổng gần nhất đã qua khi nào
- **Việc kế tiếp**: ba việc đầu ở `41` mục 5
- **Đang chờ ai quyết**: câu hỏi ở `41` mục 4, nêu rõ cái nào chặn giai đoạn hiện tại và đã treo bao nhiêu ngày
- **Tài liệu**: số lỗi, số cảnh báo, file nào quá hạn
- **Lệch**: chỗ nào `41` nói khác thực tế trong repo

Nếu `41` đã cũ hơn thực tế thì nói rõ và đề xuất cập nhật, đừng tự sửa trước khi hỏi.
