# ADR-012 — Controller tự mang annotation, thêm Swagger

```
Trạng thái: Đã chốt
Ngày: 09/09/2026
```

Sửa một phần **ADR-002**. ADR-002 vẫn đúng ở chỗ `contracts/openapi.yaml` là
nguồn sự thật của hợp đồng; cái bị bỏ là **cách bắt buộc code phải theo nó**.

## Bối cảnh

ADR-002 chốt spec-first và cài nó bằng một cơ chế duy nhất: sinh interface Java
từ `openapi.yaml`, controller `implements` interface đó. Annotation định tuyến
(`@RequestMapping`, `@PathVariable`, `@RequestParam`) nằm trên interface sinh ra,
controller chỉ có `@RestController` và các `@Override`.

Cơ chế đó đắt ở chỗ đọc: muốn biết một endpoint nhận tham số gì phải mở file
trong `build/generated/`, và file đó không có trong repo. Nó cũng khiến trang tài
liệu API tương tác không có sẵn — không ai chạy thử được endpoint mà không dựng
frontend hoặc gõ `curl`.

## Quyết định

1. Controller **không** `implements` interface sinh ra nữa. Annotation định
   tuyến chuyển thẳng lên controller.
2. `openapi.yaml` chỉ còn sinh **model** (DTO) cho Java và TS client.
3. Thêm **springdoc-openapi 3.1.1** — Swagger UI ở `/swagger-ui.html`, spec sinh
   từ code ở `/v3/api-docs`.

## Cái mất, nói thẳng

**Trình biên dịch không còn bắt được việc code và spec lệch nhau.** Trước đây đổi
`openapi.yaml` mà quên sửa controller là build đỏ ngay. Giờ hai bên trôi khỏi
nhau trong im lặng, và cái sai chỉ lộ ra khi frontend gọi một endpoint không tồn
tại — ở môi trường chạy thật, không phải lúc biên dịch.

Đây là cái mất thật, không phải cái giá tưởng tượng. `docs/34` mục 2.1 và câu
"quên bước này là lỗi biên dịch" của `api/CLAUDE.md` mục 8 đều dựa vào chính cơ
chế vừa bỏ.

**Từ nay có hai bản mô tả API.** File trong `contracts/` và spec springdoc sinh
ra. Khi lệch, **bản đúng là bản trong `contracts/`** — frontend sinh client từ
đó, và `pnpm contracts:check` kiểm tương thích ngược trên đó.

## Cái giữ

`SwaggerIT.springdocPathsMatchContract` so tập **đường dẫn** springdoc đọc được
từ code với tập đường dẫn khai trong `contracts/openapi.yaml`, và đỏ khi lệch.
Nó thế chỗ một phần cho phép kiểm đã mất.

Nó yếu hơn hẳn lỗi biên dịch, và cần biết yếu ở đâu: **nó chỉ so đường dẫn.**
Thêm endpoint mà quên khai trong spec thì đỏ; đổi kiểu một tham số, đổi tên một
trường trong thân yêu cầu, hay đổi mã trạng thái trả về thì **không ai bắt**.

## Hệ quả

- Sửa endpoint vẫn phải cập nhật `contracts/openapi.yaml` bằng tay. Việc này nay
  là kỷ luật của người viết và người rà soát mã, không còn là ràng buộc máy.
- Swagger UI **phải tắt ở bản chạy thật**: nó liệt kê đủ cả bề mặt quản trị, và
  chế độ chạy bằng IP hiện chưa có HTTPS. Hai biến
  `SPRINGDOC_API_DOCS_ENABLED=false` và `SPRINGDOC_SWAGGER_UI_ENABLED=false` —
  `docs/34` mục 5.2.
- Thêm một dependency chạy thật (`springdoc-openapi-starter-webmvc-ui`). Nhánh
  3.x là nhánh cho Spring Boot 4; 2.x chỉ chạy với Boot 3.
