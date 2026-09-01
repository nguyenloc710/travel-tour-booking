# ADR-003 — Bản dịch lưu ở bảng riêng, không dùng JSONB

```
Trạng thái: Đã chốt
Ngày: 31/08/2026
```

## Bối cảnh

Mọi thực thể có nội dung (tour, điểm đến, khách sạn, bài viết…) cần lưu chuỗi cho
nhiều locale. Trang quản trị cần biết **từng trường** đã dịch hay chưa để dựng
hàng đợi dịch và bảng độ phủ (`02` mục 8).

## Phương án đã cân nhắc

| | Bảng `*_translation` | Cột JSONB |
|---|---|---|
| Bắt buộc có bản `da` | Ràng buộc CSDL | Kiểm ở ứng dụng |
| Slug duy nhất theo locale | Index UNIQUE thường | Index biểu thức |
| Lọc theo locale | `JOIN … ON locale = ?`, có index | Toán tử JSONB, kế hoạch truy vấn kém |
| Sắp theo collation ICU | Cột `text`, dùng `COLLATE` | Phải ép kiểu, mất index |
| Biết từng trường đã dịch chưa | Đọc thẳng cột | So khoá thủ công |
| Thêm locale | Thêm dòng | Thêm khoá, không kiểm được |
| Số bảng | Gấp đôi | Không đổi |

## Quyết định

Bảng riêng, khoá chính `(entity_id, locale)`, index UNIQUE trên `(locale, slug)`.

Truy vấn bằng **`INNER JOIN` tường minh**, không dùng Hibernate `@Filter`.

## Hệ quả

- **`INNER JOIN` chính là chính sách không-fallback của `02` mục 4.1.** Bản ghi
  thiếu bản dịch tự rơi khỏi kết quả, không cần điều kiện `if` ở tầng ứng dụng.
  Đây là lợi ích lớn nhất, và là lý do thật sự chọn phương án này
- Số bảng gấp đôi. Chấp nhận
- Mọi truy vấn listing đều có join — phải có index trên `(tour_id, locale)`
- Không dùng `@Filter`: điều kiện locale phải nhìn thấy được trong câu truy vấn,
  vì quên bật filter ở một service là rò rỉ nội dung sai ngôn ngữ ra khách
