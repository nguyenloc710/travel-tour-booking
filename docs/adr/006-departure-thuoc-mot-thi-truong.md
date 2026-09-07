# ADR-006 — Ngày khởi hành thuộc về một thị trường

```
Trạng thái: Đã chốt
Ngày: 31/08/2026
```

## Bối cảnh

Một sản phẩm bán ở cả `DK` lẫn `VN`. Ngày khởi hành 14/03/2027 xuất hiện ở cả hai
thị trường. Câu hỏi: một bản ghi `departure` dùng chung, hay hai bản ghi riêng.

## Phương án đã cân nhắc

| | `departure` dùng chung, giá theo market | **`departure` thuộc một market** |
|---|---|---|
| Số dòng cho ngày 14/03 | 1 | 2 |
| Số chỗ | Phải chia giữa hai thị trường — **không mô hình hoá được** | Mỗi đoàn số chỗ riêng |
| Trưởng đoàn | Một người nói cả tiếng Đan lẫn tiếng Việt? | Mỗi đoàn một trưởng đoàn |
| Điểm khởi hành | Copenhagen và Hà Nội trên cùng một dòng | Mỗi dòng một điểm |
| `price_from` từng thị trường | Tính qua bảng giá | Tính trực tiếp |

## Quyết định

`departure.market` là cột bắt buộc, khoá ngoại tới `market`.

Khoá duy nhất: `(product_id, market, depart_date, COALESCE(cabin_category, ''))`.

## Lý do thật sự

Không phải lý do kỹ thuật mà là lý do thực tế: **đoàn khách Đan Mạch bay từ
Copenhagen với trưởng đoàn nói tiếng Đan, và đoàn khách Việt khởi hành từ Hà Nội
với hướng dẫn viên tiếng Việt, là hai chuyến đi khác nhau** — chúng chỉ trùng lộ
trình. Ép vào một bản ghi rồi chia số chỗ là mô hình sai với thực tế đang diễn ra.

Đây là hệ quả trực tiếp của `02` mục 1: `Market` quyết định khách **mua** gì, và
cái khách mua ở đây là một chỗ trên một đoàn cụ thể.

## Hệ quả

- `price_from` nằm ở `product_market`, tính từ `departure` của đúng thị trường —
  hai thị trường hai giá "từ" khác nhau, đúng như `04` mục 4.1 yêu cầu
- Nhập ngày khởi hành cho hai thị trường là hai thao tác. Trang quản trị nên có
  chức năng nhân bản lịch từ thị trường này sang thị trường kia
- Số dòng `departure` gấp đôi khi một sản phẩm bán ở cả hai thị trường. Không
  đáng kể ở quy mô này
- Bộ kiểm dữ liệu phải kiểm giá dao động **trong phạm vi từng thị trường**, không
  kiểm chéo hai thị trường
