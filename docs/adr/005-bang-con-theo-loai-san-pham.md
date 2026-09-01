# ADR-005 — Mỗi loại sản phẩm một bảng con

```
Trạng thái: Đã chốt
Ngày: 31/08/2026
```

## Bối cảnh

Sáu loại sản phẩm (`04` mục 2) có trường riêng **gần như không giao nhau**:
`min_pax` chỉ có nghĩa với tour đoàn, `price_tiers` chỉ với tour riêng,
`components` chỉ với combo, `time_slots` chỉ với tour trong ngày.

## Phương án đã cân nhắc

| | Một bảng rộng, cột nullable | **Bảng con theo loại** | Cột JSONB |
|---|---|---|---|
| `NOT NULL` cho trường bắt buộc của loại | **Không được** | **Được** | Không |
| `CHECK (min_pax BETWEEN 10 AND 25)` | Phải kèm `OR IS NULL` — mất tác dụng | **Sạch** | Không |
| Đọc lược đồ hiểu loại nào có gì | Không — 40 cột lẫn lộn | **Có** | Không |
| Thêm loại mới | `ALTER TABLE` bảng đang chạy | **Thêm bảng** | Không đổi |
| Truy vấn listing | 1 bảng | **1 bảng `product` là đủ** | 1 bảng |
| Số bảng | 1 | 7 | 1 |

Điểm mấu chốt là dòng áp chót: **truy vấn listing không cần bảng con.** Thẻ sản
phẩm chỉ đọc `product` + `product_translation` + `product_market`; trường riêng
của loại chỉ cần ở trang chi tiết, khi đó đã biết loại nên join đúng một bảng.
Nỗi lo "bảng con làm listing chậm" không có thật.

## Quyết định

Bảng con, khoá chính `product_id`, cưỡng chế đúng loại bằng **khoá ngoại kép**:

```sql
CREATE TABLE product (
  id UUID PRIMARY KEY,
  product_type VARCHAR(24) NOT NULL,
  CONSTRAINT ux_product_id_type UNIQUE (id, product_type)
);

CREATE TABLE product_group_tour (
  product_id   UUID PRIMARY KEY,
  product_type VARCHAR(24) NOT NULL DEFAULT 'GROUP_TOUR'
               CHECK (product_type = 'GROUP_TOUR'),
  min_pax      SMALLINT NOT NULL CHECK (min_pax BETWEEN 10 AND 25),
  FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);
```

## Hệ quả

- Ràng buộc nghiệp vụ của `04` viết được thẳng vào lược đồ, **không kèm
  `OR IS NULL`**. Đây là lợi ích chính
- Không thể gắn dòng `product_group_tour` vào một sản phẩm loại `CRUISE`
- **Khoá ngoại kép không chặn được *thiếu* dòng bảng con** — chỉ chặn dòng sai
  loại. Cần thêm constraint trigger hoãn, cộng một quy tắc trong bộ kiểm dữ liệu
  (`12` mục 9 quy tắc 1). Ghi rõ để không ai tưởng lược đồ đã kín
- JPA ánh xạ bằng `@Inheritance(JOINED)` hoặc bằng thực thể riêng cho từng bảng
  con. Nghiêng về thực thể riêng: tầng `domain` không dùng JPA, nên không cần cây
  thừa kế ở tầng dữ liệu
- Thêm loại thứ bảy là thêm một bảng, không đụng vào bảng đang chạy
