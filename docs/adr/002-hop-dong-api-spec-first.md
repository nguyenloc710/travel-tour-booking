# ADR-002 — Hợp đồng API viết trước, sinh code từ spec

```
Trạng thái: Đã chốt
Ngày: 31/08/2026
Sửa một phần bởi: ADR-012 (09/09/2026)
```

> **ADR-012 đã bỏ cơ chế bắt buộc mô tả ở đây.** Controller không còn
> `implements` interface sinh ra, nên câu "đổi spec mà quên sửa controller là lỗi
> biên dịch" **không còn đúng**. `contracts/openapi.yaml` vẫn là nguồn sự thật;
> cái mất là ràng buộc máy. Đọc ADR-012 trước khi dựa vào mục nào bên dưới.

## Bối cảnh

Backend Java, frontend TypeScript. Hai bên **không chia sẻ type** được như khi
cả hai cùng là TypeScript. Phải có một chỗ duy nhất định nghĩa hợp đồng, và một
cơ chế bắt được lúc hai bên lệch nhau.

Dự án này dẫn dắt bằng tài liệu: quyết định được ghi ra và duyệt trước khi code.

## Phương án đã cân nhắc

| | Ưu | Nhược |
|---|---|---|
| **A. Spec-first** — viết `openapi.yaml`, sinh interface Java và TS client | Đổi spec mà quên sửa controller là **lỗi biên dịch**. Hợp đồng review được độc lập với code | Viết YAML tay chậm hơn lúc đầu. Cần cấu hình generator |
| B. Code-first — springdoc sinh spec từ annotation | Nhanh nhất lúc đầu | Hợp đồng thành **hệ quả** của code. Đổi tên một trường Java là đổi API mà không ai duyệt. Frontend luôn chạy sau |
| C. Viết tay cả hai bên | Không cần công cụ | Lệch nhau là chuyện chắc chắn xảy ra, chỉ là khi nào |

## Quyết định

Chọn **A**.

1. `contracts/openapi.yaml` viết tay, sửa trong pull request riêng, có người duyệt
2. `openapi-generator` sinh interface Java; controller **`implements`** interface đó
3. Cùng spec sinh TS client vào `web/packages/api-client`
4. Code sinh ra **không commit** — sinh lúc build, nên không ai sửa tay được
5. CI kiểm tương thích ngược so với nhánh chính
6. Đổi phá vỡ tương thích thì lên `/api/v2/`

## Hệ quả

- Bước "sửa spec" thành bắt buộc trong mọi thay đổi API — cố ý làm nó có ma sát
- Frontend làm song song được ngay khi spec duyệt xong, không chờ backend
- Spec là tài liệu API luôn đúng, khỏi viết tài liệu API riêng
- Phải chấp nhận một số hạn chế biểu đạt của OpenAPI so với type Java tự do
