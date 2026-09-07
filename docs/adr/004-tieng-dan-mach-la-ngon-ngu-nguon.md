# ADR-004 — Tiếng Đan Mạch là ngôn ngữ nguồn

```
Trạng thái: Đã chốt
Ngày: 31/08/2026
```

## Bối cảnh

Website phục vụ hai thị trường. Thị trường Đan Mạch là thị trường chính. Đội ngũ
phát triển nói tiếng Việt, nên trực giác mặc định sẽ là viết nội dung tiếng Việt
trước rồi dịch sang tiếng Đan.

Nội dung tour là văn bản thuyết phục, không phải văn bản kỹ thuật. Bản dịch của
một văn bản thuyết phục luôn nhạt hơn bản gốc.

## Quyết định

**`da` là ngôn ngữ nguồn.** Mọi nội dung viết bằng tiếng Đan trước, dịch sang
tiếng Việt sau. Không có chiều ngược lại.

Ràng buộc kỹ thuật đi kèm:

- Bản ghi nội dung **bắt buộc** có bản `da`. Thiếu là lỗi dữ liệu, chặn ở ràng
  buộc CSDL
- Bản `vi` là tuỳ chọn; thiếu thì ẩn bản ghi khỏi locale `vi`
- Chuỗi giao diện thiếu locale thì fallback về `da`
- `OUTDATED` tính từ mốc `da`: bản `vi` lỗi thời khi bản `da` được sửa sau

## Hệ quả

- Nhóm khách lớn nhất luôn đọc bản viết gốc, không đọc bản dịch
- Cần người viết nội dung tiếng Đan. **Đây là ràng buộc nhân sự thật, và là rủi
  ro tiến độ lớn nhất của phần nội dung** — ghi ở `01` mục 7
- Đội ngũ người Việt phải làm ngược trực giác. Nói rõ ở cả ba file `CLAUDE.md`
- Thêm `en` về sau thì dịch từ `da`, không dịch từ `vi` — tránh dịch qua hai lớp
