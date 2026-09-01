---
name: doi-hop-dong-api
description: Quy trình spec-first cho mọi thay đổi chạm tới contracts/openapi.yaml — thêm/sửa endpoint, đổi DTO, thêm mã lỗi, đổi tham số market hoặc locale. Dùng bất cứ khi nào công việc động tới hợp đồng giữa api/ và web/.
---

# Đổi hợp đồng API

ADR-002 chốt **spec-first**. Chi tiết hợp đồng ở `docs/13-hop-dong-api.md`.

## Thứ tự bắt buộc — không được đảo

```
1. Sửa contracts/openapi.yaml        ← pull request riêng, có người duyệt
2. pnpm contracts:generate
3. Sửa controller cho khớp interface mới   ← quên bước này là lỗi biên dịch
4. Sửa frontend
```

Sửa controller trước rồi cập nhật spec sau là **code-first**, và ADR-002 đã bác
bỏ. Gặp code đang đi ngược thì dừng và báo, đừng sửa tiếp theo chiều sai.

Code sinh ra **không sửa tay** — hook `chan-file-sinh-ra.py` sẽ chặn.

## Trước khi sửa spec

Đọc `docs/13`, kiểm ba thứ dưới đây trên phần sắp sửa:

| Kiểm | Quy tắc |
|---|---|
| Market và locale | Market ở **đường dẫn** (chữ thường), locale ở **`Accept-Language`**. Không gộp, không đảo |
| Tiền | `{"amount":"24990.00","currency":"DKK"}` — `amount` là **chuỗi**. API **không** định dạng tiền |
| Lỗi | Trả **mã lỗi kèm tham số**, không trả câu tiếng người. Mã mới phải thêm vào danh mục ở `13` |

Ba lỗi này chiếm gần hết số lần hợp đồng bị sửa lại.

## Tương thích ngược

| Loại thay đổi | Có phá vỡ không | Việc phải làm |
|---|---|---|
| Thêm trường tuỳ chọn vào response | Không | Sửa spec, sinh lại |
| Thêm endpoint mới | Không | Như trên |
| Thêm tham số **bắt buộc** vào request | **Có** | Lên `/api/v2/`, hoặc cho tham số mặc định |
| Xoá hoặc đổi tên trường | **Có** | Lên `/api/v2/` |
| Đổi kiểu dữ liệu của trường | **Có** | Lên `/api/v2/` |
| Thêm giá trị enum | **Có** với client cũ | Cân nhắc, ghi rõ trong PR |

`pnpm contracts:check` kiểm tương thích ngược so với nhánh chính. Chạy trước khi
mở PR, đừng để CI phát hiện.

## Sau khi sửa

Sửa `contracts/` thì phải chạy **cả hai bên**, không chỉ bên mình đang làm:

```bash
pnpm contracts:generate
./gradlew build            # interface Java phải khớp controller
pnpm typecheck             # TS client phải khớp chỗ gọi
pnpm contracts:check
```

Rồi cập nhật `docs/13` nếu thay đổi chạm tới: danh mục endpoint, danh sách mã
lỗi, quy tắc phân trang, quy tắc cache, hoặc cách truyền market/locale. Đổi spec
mà `13` không đổi là bắt đầu quá trình lệch giữa tài liệu và hiện thực.

## Đổi lớn cần gì thêm

Đổi cách hai hệ thống nói chuyện với nhau — ví dụ chuyển market từ đường dẫn
sang header, hay đổi cách biểu diễn tiền — là **quyết định khó đảo ngược**: viết
ADR mới, không sửa ADR-002. Tiêu chí ở `docs/42` mục 5.3.
