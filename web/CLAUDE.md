# web/CLAUDE.md

Frontend: Next.js (App Router), TypeScript, pnpm workspace.
Hai ứng dụng: website khách (`apps/site`) và trang quản trị (`apps/admin`).

Đọc `../CLAUDE.md` trước — bốn điều quan trọng nhất của cả dự án nằm ở đó.

> **Trạng thái: site khách và trang quản trị đã chạy đủ** (05/09/2026). pnpm
> workspace, hai app Next.js, ba package dùng chung, TS client sinh từ spec.
> Site khách có danh sách, chi tiết, điểm đến, tìm tour, luồng đặt tour bốn
> bước, trang xác nhận, form yêu cầu báo giá, blog, sự kiện và liên hệ — chạy
> thật ở cả hai locale và hai thị trường. Trang quản trị có mười một màn hình:
> đăng nhập, bảng điều khiển, sản phẩm (danh sách, tạo, sửa 4 tab), đơn (danh
> sách, chi tiết), báo giá (danh sách, chi tiết), nội dung khác, người dùng.

## 0. Lệnh

```bash
pnpm install               # cần pnpm: `npm i -g pnpm`. Node 25 đã bỏ corepack,
                           # nên `corepack enable pnpm` không còn chạy được
pnpm dev                   # site ở cổng 3000
pnpm dev:admin             # admin ở cổng 3001

pnpm typecheck             # sinh lại TS client rồi kiểm cả 5 package
pnpm lint
pnpm test
pnpm i18n:check            # thiếu khoá dịch là LỖI
pnpm build

pnpm contracts:generate    # sinh CẢ HAI phía: TS client + interface Java
pnpm contracts:check       # tương thích ngược so với nhánh main
```

Site cần API chạy ở `http://localhost:8080` — đổi bằng `NEXT_PUBLIC_API_URL`.

### Bố cục

```
apps/site        website khách — locale ở URL
apps/admin       trang quản trị — KHÔNG có locale trong URL,
                 locale ở đây là thuộc tính của DỮ LIỆU đang sửa
packages/i18n    locale, market, message catalog, bảng đoạn đường dẫn
packages/ui      hàm định dạng tiền/ngày dùng chung, component giá
packages/api-client   SINH RA từ contracts/openapi.yaml — src/ và dist/ không commit
```

Phiên bản thư viện tập trung ở `pnpm-workspace.yaml` mục `catalog:`, giống
`libs.versions.toml` bên `api/`. Đổi ở đó, không đổi rải rác từng `package.json`.

> **Next 16 đổi tên `middleware` thành `proxy`.** File là `apps/site/src/proxy.ts`,
> hàm xuất ra tên `proxy`. Cùng cơ chế, chỉ khác tên.

---

## 1. Locale trong URL, market trong cookie

```
/da/rejser/vietnam-fra-nord-til-syd
/vi/tour/viet-nam-tu-bac-vao-nam
```

**Cả đoạn đường dẫn lẫn slug đều dịch.** Không dùng slug tiếng Đan cho trang
tiếng Việt. Bảng ánh xạ đoạn đường dẫn là dữ liệu tĩnh trong `packages/i18n`,
không nằm trong CSDL. Bảng đầy đủ: `docs/02` mục 5.1.

**Market không nằm trong URL.** Suy từ locale (`da`→`DK`, `vi`→`VN`), ghi đè bằng
cookie `market` khi khách dùng bộ chọn thị trường. Khi market khác mặc định của
locale, hiện **băng thông báo thường trực** — không để khách nhầm giá.

URL canonical chỉ chứa locale, nên không có nội dung trùng lặp với công cụ tìm
kiếm. Lý do đầy đủ: `docs/02` mục 5.3.

Gọi API thì gửi market **tường minh** trong đường dẫn: `/api/v1/dk/tours`, kèm
`Accept-Language`.

---

## 2. Fallback — hai luật ngược nhau

| Loại | Luật |
|---|---|
| **Nội dung bán hàng** (tour, điểm đến, bài viết) | **Không fallback.** Backend đã ẩn sẵn qua `INNER JOIN` — frontend không cần xử lý, và **không được** tự lấp bằng bản `da` |
| **Chuỗi giao diện** (nhãn nút, tiêu đề cột) | Fallback về `da`, ghi log. `pnpm i18n:check` báo lỗi nếu thiếu khoá |

Thư viện i18n nào cũng bật fallback sẵn cho cả hai. Cấu hình lại: chỉ message
catalog mới fallback.

---

## 3. Định dạng — frontend làm, backend không

API trả `{"amount":"24990.00","currency":"DKK"}`. `amount` là **chuỗi** — số dấu
phẩy động của JavaScript làm hỏng tiền. Đừng `parseFloat` rồi tính; chỉ định dạng.

| | `da` / DKK | `vi` / VND |
|---|---|---|
| Giá | `24.990 kr.` | `18.900.000 ₫` |
| Chữ số thập phân | 2 | 0 |
| Ngày | `14. marts 2027` | `14/03/2027` |

Dùng `Intl.NumberFormat` và `Intl.DateTimeFormat`. **Một hàm định dạng dùng
chung** trong `packages/ui`, không định dạng rải rác trong component.

**Giá luôn kèm chữ "từ" và disclaimer** — yêu cầu pháp lý, không phải lựa chọn
thiết kế. Nằm sẵn trong component giá; đừng hiện số trần.

---

## 4. Lỗi — dịch mã, không hiện mã

API trả `{"code":"DEPARTURE_SOLD_OUT","params":{…}}`. Frontend tra message
catalog. Mã chưa có bản dịch thì hiện câu lỗi chung, **không hiện mã ra khách**,
và ghi log để bổ sung.

---

## 5. Bốn quy tắc giao diện

1. **Không hardcode số liệu.** "Xem tất cả 92 tour" đếm từ dữ liệu API trả về —
   và số đó khác nhau giữa hai locale, vì bản ghi chưa dịch bị ẩn.
2. **Trạng thái bộ lọc trong URL** (`useSearchParams`), không trong `useState`.
   F5 giữ nguyên bộ lọc; dán link ra đúng kết quả đó.
3. **Ba trạng thái cho mọi màn hình có dữ liệu**: đang tải (**skeleton**, không
   phải vòng xoay), rỗng (kèm hướng dẫn hành động), lỗi.
4. **Không tự chạy.** Dải cuộn ngang không auto-play, không dùng chấm tròn —
   dùng chữ đọc được: `Đang xem 1–3 trên 8 chuyến đi`.

---

## 6. Khách là người lớn tuổi Bắc Âu

Không phải mô tả cho vui — đây là ràng buộc thiết kế:

- Cỡ chữ thân bài tối thiểu **16px**, dòng giãn **1.6**
- Vùng chạm tối thiểu **44×44px**
- Tương phản đạt **WCAG AA**
- **Không dùng hover làm điều kiện duy nhất để lộ thông tin**
- Ngôn từ rõ ràng, không viết tắt
- Số điện thoại và giờ mở cửa hiện thường trực trên header

---

## 7. Font phải phục vụ cả hai ngôn ngữ

Mọi font dùng cho tiêu đề và thân bài phải dựng sẵn **cả `æ ø å` lẫn dấu tiếng
Việt**. Thử bằng đúng hai dòng này trước khi chốt font — và thử lại mỗi lần đổi:

```
Strand og øer på tværs
Điểm cuối · Hội An · Vịnh Hạ Long
```

Bản demo đã dính bẫy này: file SVG bản đồ dùng Georgia, "Điểm cuối" hiện thành
"Điểm cuố i". Ràng buộc áp dụng cho cả **file SVG** và ảnh có chữ, không chỉ CSS.

---

## 8. Client API — sinh ra, không sửa tay

`packages/api-client` sinh từ `contracts/openapi.yaml` lúc build. Cần thêm trường
thì sửa spec rồi `pnpm contracts:generate`, đừng sửa file sinh ra — nó bị ghi đè.

Không viết `fetch` tay tới API. Mọi truy vấn đi qua client sinh ra.

---

## 9. Slug

Không dùng ký tự có dấu ở cả hai ngôn ngữ:

- `da`: `bekraeftelse`, không phải `bekræftelse`
- `vi`: `viet-nam-tu-bac-vao-nam`, không phải `việt-nam-từ-bắc-vào-nam`

Lý do: gõ được trên mọi bàn phím, dán được vào mọi ứng dụng chat mà không bị mã
hoá phần trăm.

---

## 10. Trước khi báo hoàn thành

```bash
pnpm typecheck
pnpm lint
pnpm test
pnpm i18n:check     # thiếu khoá dịch là LỖI, không phải cảnh báo
pnpm build
```

---

## 11. Cấm

- `parseFloat` trên trường tiền
- Định dạng tiền hoặc ngày rải rác trong component
- Tự lấp nội dung thiếu bản dịch bằng bản `da`
- Hiện mã lỗi ra khách
- Trạng thái bộ lọc trong `useState`
- Vòng xoay thay cho skeleton
- Hiện giá không kèm chữ "từ"
- Sửa file trong `packages/api-client`
