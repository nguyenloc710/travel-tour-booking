---
name: them-thi-truong-ngon-ngu
description: Thêm một Market mới (ví dụ SE, NO) hoặc một Locale mới (ví dụ en) vào hệ thống — checklist đầy đủ qua CSDL, API, frontend, nội dung, pháp lý. Dùng khi được yêu cầu thêm thị trường, thêm ngôn ngữ, hoặc mở rộng sang nước khác.
---

# Thêm thị trường hoặc ngôn ngữ

**Đây là hai việc khác nhau.** Nhầm lẫn giữa chúng là nhầm lẫn tốn kém nhất của
dự án — `docs/02-thi-truong-va-da-ngon-ngu.md` mục 1.

| | `Market` | `Locale` |
|---|---|---|
| Quyết định | Khách **mua** gì | Khách **đọc** bằng tiếng gì |
| Kéo theo | Catalog, bảng giá, tiền tệ, cổng thanh toán, luật, loại khách, điểm khởi hành | Bản dịch, message catalog, collation, định dạng ngày |
| Chi phí | **Rất lớn** — gần như một dòng sản phẩm mới | Lớn nhưng cơ học |

Hỏi trước khi làm: người dùng muốn bán ở nơi mới, hay chỉ muốn hiển thị thêm một
thứ tiếng? Trả lời sai câu này thì làm sai toàn bộ.

## Trước tiên

1. Đọc `docs/02` mục 12 — checklist gốc, skill này chỉ là cách thi hành
2. Việc này là **phiếu thay đổi** theo `docs/40` mục 7, cần Chủ sản phẩm duyệt
3. Nếu đang ở giai đoạn G3 trở về trước: cân nhắc **hoãn**. Thêm thị trường khi
   thị trường đầu tiên chưa chạy thật là nhân đôi phần chưa được kiểm chứng

## Thêm một `Locale`

| # | Việc | Ở đâu |
|---|---|---|
| 1 | Thêm dòng vào bảng `locale` | Migration Flyway |
| 2 | Chọn collation ICU đúng cho ngôn ngữ đó | `docs/02` mục 9 |
| 3 | Cấu hình `unaccent` cho ngôn ngữ mới nếu cần | `docs/12` |
| 4 | Thêm message catalog đầy đủ | `web/packages/i18n/` |
| 5 | Kiểm phông chữ có đủ ký tự | Thử chuỗi thật, không tin bảng hỗ trợ |
| 6 | Thêm `hreflang` và mục sitemap | `docs/02` mục 7 |
| 7 | Quyết định: nội dung bán hàng chưa dịch thì **ẩn hoàn toàn** | Chính sách sẵn có, không được nới |
| 8 | `pnpm i18n:check` phải xanh — thiếu khoá là **lỗi** | CI |

`da` vẫn là ngôn ngữ nguồn (ADR-004). Ngôn ngữ mới là **bản dịch từ `da`**,
không phải nguồn thứ hai.

## Thêm một `Market`

Nặng hơn nhiều. Mỗi dòng dưới đây là việc thật, không phải cấu hình.

| # | Việc | Ghi chú |
|---|---|---|
| 1 | Dòng trong bảng `market` + tiền tệ + scale | VND 0 chữ số thập phân, DKK 2 |
| 2 | Bộ `pax_type` riêng | Ngưỡng tuổi trẻ em khác nhau giữa các thị trường |
| 3 | Bộ `departure_origin` riêng | Ở `DK` là phụ thu, ở `VN` là biến thể sản phẩm |
| 4 | Bảng giá riêng cho từng sản phẩm bán ở thị trường đó | **Không quy đổi tỷ giá.** Không có cột nào làm việc đó |
| 5 | `departure` riêng | ADR-006 — mỗi đoàn thuộc đúng một thị trường |
| 6 | Hằng số nghiệp vụ: đặt cọc, phí, giảm đặt sớm, làm tròn, hạn giữ chỗ, bậc huỷ | `docs/14` mục 10. **Sáu con số này phải có trước khi code** |
| 7 | Cổng thanh toán | ADR-007 |
| 8 | Ràng buộc pháp lý, chứng từ, quy tắc hiển thị giá | `docs/32` — cần người có chuyên môn pháp lý |
| 9 | Quyết định catalog: sản phẩm nào bán ở thị trường mới | Qua bảng `product_market` |
| 10 | Nội dung: tour bán cho thị trường mới có thể là **sản phẩm khác** | Vé bay quốc tế, trưởng đoàn, lộ trình |

> **Bước 10 là bước hay bị bỏ qua nhất.** Cùng một hành trình bán cho khách Đan
> và khách Việt không phải cùng một sản phẩm. Thêm thị trường mà chỉ thêm bảng
> giá là đang giả định ngược lại.

## Kiểm sau khi làm

```bash
./gradlew test
pnpm i18n:check
pnpm test
python scripts/docs_check.py
```

Cộng ba phép thử thủ công: giá hiển thị đúng tiền tệ và đúng scale; sắp xếp đúng
collation; sản phẩm không bán ở thị trường mới thì **không** xuất hiện ở đó.

Cuối cùng cập nhật `docs/02`, `docs/12`, `docs/14` và `docs/41`.
