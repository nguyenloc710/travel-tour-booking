---
description: Kết thúc phiên làm việc — chạy kiểm, cập nhật sổ trạng thái, đề xuất commit
allowed-tools: Read, Edit, Grep, Glob, Bash
---

Kết thúc phiên làm việc.

1. `git status --short` và `git diff --stat` — phiên này đã đụng vào những gì
2. Chạy bộ kiểm **liên quan tới thứ vừa sửa**, theo `CLAUDE.md`:
   - sửa `docs/` → `python scripts/docs_check.py`
   - sửa `api/` → `./gradlew build`
   - sửa `web/` → `pnpm typecheck` và `pnpm lint`
   - sửa `contracts/` → **cả hai bên**
3. Cập nhật `docs/41-tinh-trang.md`:
   - mục 9 *Nhật ký phiên*: hôm nay làm gì, để lại gì dở dang
   - mục 5 *Việc kế tiếp*: viết lại nếu thứ tự đã đổi
   - mục 6 *Chỗ dễ quên*: thêm nếu phiên này gặp bẫy mới — đây là mục có giá trị nhất của file
   - mục 4: thêm câu hỏi mới phát sinh cần người quyết
4. Đề xuất câu commit: tiền tố `api:` / `web:` / `contracts:` / `docs:` / `chore:`, nội dung **tiếng Việt**

Đề xuất commit thôi, **không tự commit** trừ khi tôi bảo.
