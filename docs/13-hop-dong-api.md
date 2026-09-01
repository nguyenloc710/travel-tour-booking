# Hợp đồng API

```
Trạng thái: Nháp
Cập nhật: 31/08/2026
Nguồn sự thật về: quy trình spec-first, quy ước URL và header, phân trang,
                  mã lỗi, danh mục endpoint, xác thực, tính bất biến khi gọi lại.
Không nói về: công thức tính giá (14), lược đồ CSDL (12),
              cách frontend gọi (web/CLAUDE.md).
```

> **Nguồn sự thật thật sự là `contracts/openapi.yaml`.** Tài liệu này là quy ước
> và lý do đằng sau; khi lệch, file spec đúng.

Với backend Java và frontend TypeScript, hai bên không chia sẻ type. `openapi.yaml`
là **chỗ duy nhất hai bên gặp nhau** — vì thế tài liệu này quan trọng hơn hẳn so
với một dự án thuần TypeScript.

---

## 1. Quy trình spec-first

ADR-002. Thứ tự **bắt buộc**, không được đảo:

```
1. Sửa contracts/openapi.yaml        ← pull request riêng, có người duyệt
2. pnpm contracts:generate           ← sinh interface Java + TS client
3. Controller implements interface mới
4. Sửa frontend
```

- Controller **`implements`** interface sinh ra → đổi spec mà quên sửa controller
  là **lỗi biên dịch**, không phải bug lúc chạy. Đây là toàn bộ lý do chọn
  spec-first.
- Code sinh ra **không commit** — sinh lúc build, nên không ai sửa tay được.
- CI kiểm tương thích ngược so với nhánh chính.
- Đổi phá vỡ tương thích → lên `/api/v2/`.

**Không dùng springdoc để sinh spec từ annotation.** Đó là chiều ngược lại và
ADR-002 đã bác bỏ.

---

## 2. Hai bề mặt API

| | API công khai | API quản trị |
|---|---|---|
| Tiền tố | `/api/v1/{market}/…` | `/api/v1/admin/…` |
| Market | **Trong đường dẫn** | **Không có** — nhân viên làm việc xuyên thị trường |
| Xác thực | Không | Phiên đăng nhập, bốn vai trò |
| Locale | `Accept-Language` | `Accept-Language` cho giao diện; dữ liệu trả **mọi locale** |
| Nội dung chưa xuất bản | Không bao giờ | Có |
| Cache | Có, xem mục 8 | `no-store` |

Khác biệt cuối cùng đáng nhớ: API công khai trả **một** bản dịch (bản của locale
đang xem); API quản trị trả **tất cả** bản dịch của một bản ghi, vì màn hình dịch
song song cần bản `da` và bản `vi` cạnh nhau.

---

## 3. Market và locale

| | Nằm ở đâu | Ví dụ |
|---|---|---|
| Market | Đoạn đường dẫn, **chữ thường** | `/api/v1/dk/products` |
| Locale | Header | `Accept-Language: da` |

Market vào đường dẫn vì nó đổi **tài nguyên**: `/dk/products` và `/vn/products`
là hai tập khác nhau, giá khác nhau, cache riêng được. Locale vào header vì nó
chỉ đổi **cách trình bày** cùng một tài nguyên.

Phản hồi luôn kèm:

```http
Content-Language: da
Vary: Accept-Language
```

**Backend không bao giờ suy market từ locale.** Frontend gửi market tường minh.
Market không tồn tại hoặc `is_active = false` → `404`, không phải `400`.

Locale không hợp lệ hoặc không bật → **`400 UNSUPPORTED_LOCALE`**, không lặng lẽ
lùi về `da`. Lùi lặng lẽ ở tầng API sẽ che mất lỗi cấu hình của frontend.

---

## 4. Quy ước chung

| Mục | Quy ước |
|---|---|
| Đường dẫn | `kebab-case`, danh từ **số nhiều**: `/products`, `/seat-holds` |
| Trường JSON | `camelCase` |
| Ngày | ISO 8601: `"2027-03-14"` |
| Thời điểm | ISO 8601 UTC: `"2026-08-31T09:12:00Z"` |
| Enum | `SCREAMING_SNAKE` |
| Định danh trong URL công khai | **`slug`**, không phải `id` |
| Định danh trong URL quản trị | `id` (UUID) |
| Trường rỗng | **Bỏ hẳn khỏi JSON**, không trả `null` |

Dùng `slug` ở API công khai vì nó là thứ đã có trong URL trang web, và tránh lộ
id nội bộ. `slug` phụ thuộc locale, nên `/dk/products/{slug}` được hiểu là "slug
này trong locale ở `Accept-Language`".

### 4.1. Tiền — luôn là một đối tượng, `amount` luôn là chuỗi

```json
{ "amount": "24990.00", "currency": "DKK" }
```

`amount` là **chuỗi** vì số dấu phẩy động của JavaScript làm hỏng tiền.
**API không định dạng** — không có `"24.990 kr."` ở bất kỳ đâu trong phản hồi.
Frontend định dạng bằng `Intl.NumberFormat`.

Không bao giờ trả tiền dạng số trần: `"total": 24990` là sai vì mất mã tiền tệ,
và một phản hồi có thể chứa hai loại tiền (giá tour và phí xử lý ở thị trường
khác nhau trong màn hình quản trị).

---

## 5. Lỗi — trả mã, không trả câu tiếng người

```json
{
  "code": "DEPARTURE_SOLD_OUT",
  "params": { "departureId": "…", "departDate": "2027-03-14" },
  "traceId": "01J9X…"
}
```

Backend trả `"Afgangen er udsolgt"` là sai: bản dịch sẽ tồn tại ở hai nơi và lệch
nhau. Mã lỗi còn dùng được cho log, thống kê và test; câu chữ thì không.

Lỗi kiểm tra dữ liệu vào có thêm `fields`:

```json
{
  "code": "VALIDATION_FAILED",
  "fields": [
    { "path": "party.adults", "code": "MIN", "params": { "min": 1 } }
  ]
}
```

### 5.1. Danh mục mã lỗi

| Mã | HTTP | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Dữ liệu vào sai |
| `UNSUPPORTED_LOCALE` | 400 | Locale không bật |
| `UNAUTHENTICATED` | 401 | Chưa đăng nhập, API quản trị |
| `FORBIDDEN` | 403 | Sai vai trò |
| `NOT_FOUND` | 404 | Không có, hoặc **chưa dịch**, hoặc **chưa gán thị trường** |
| `DEPARTURE_SOLD_OUT` | 409 | Không đủ chỗ khả dụng |
| `SEAT_HOLD_EXPIRED` | 409 | Giữ chỗ đã hết hạn |
| `DEPARTURE_CLOSED` | 409 | Đã đóng bán |
| `QUOTE_EXPIRED` | 409 | Báo giá quá hạn |
| `QUOTE_NOT_ACCEPTABLE` | 409 | Báo giá không ở trạng thái nhận được |
| `LEAD_TIME_NOT_MET` | 422 | Ngày yêu cầu sớm hơn `leadTimeDays` |
| `PRODUCT_NOT_BOOKABLE` | 422 | Loại sản phẩm không đặt trực tiếp được (`PRIVATE_TOUR`) |
| `PARTY_SIZE_OUT_OF_RANGE` | 422 | Số khách ngoài bậc giá hoặc ngoài `minPax`/`maxPax` |
| `PAYMENT_FAILED` | 402 | Cổng thanh toán từ chối |
| `RATE_LIMITED` | 429 | Quá nhiều yêu cầu |
| `INTERNAL_ERROR` | 500 | Kèm `traceId` |

> **`404` gộp ba tình huống có chủ ý**: không tồn tại, chưa dịch cho locale này,
> chưa gán vào thị trường này. Phân biệt ba cái ra ngoài là rò rỉ thông tin về
> sản phẩm chưa mở bán. Log nội bộ vẫn phân biệt.

---

## 6. Phân trang, lọc, sắp xếp

```
GET /api/v1/dk/products?region=nord&productType=GROUP_TOUR&page=0&size=24
```

```json
{
  "items": [ … ],
  "page": 0,
  "size": 24,
  "totalItems": 47,
  "totalPages": 2
}
```

- `size` mặc định 24, **tối đa 60**.
- Phân trang theo offset, không dùng con trỏ. Ở quy mô này (hàng chục tới hàng
  trăm sản phẩm) offset đủ, và nó cho phép nhảy tới trang bất kỳ — thứ mà con trỏ
  không làm được. Xem lại nếu catalog vượt vài nghìn bản ghi.
- `totalItems` đếm **trong phạm vi `(market, locale)`**, nên hai locale ra hai
  con số khác nhau. Đúng như thiết kế — `02` mục 4.1.

**Bộ lọc nhiều giá trị dùng tham số lặp lại**, không dùng dấu phẩy:

```
?theme=trekking&theme=du-thuyen-song
```

Dấu phẩy vỡ khi giá trị có chứa dấu phẩy; tham số lặp lại thì không.

Sắp xếp: `?sort=priceFrom,asc` · `title,asc` · `durationDays,asc` ·
`departureDate,asc`. `title` sắp theo collation của locale đang xem, ở tầng CSDL.

---

## 7. Tính bất biến khi gọi lại

Mọi endpoint **tạo ra hệ quả tính tiền** yêu cầu header:

```http
Idempotency-Key: <uuid do client sinh>
```

Áp dụng cho: `POST /bookings`, `POST /seat-holds`, `POST /quote-requests`.

Gọi lại cùng khoá trong **24 giờ** trả về **cùng kết quả cũ**, không tạo bản ghi
thứ hai. Đây là chống hai thứ có thật: khách bấm nút hai lần, và mạng di động
gửi lại yêu cầu sau khi máy chủ đã xử lý xong nhưng phản hồi chưa về.

Không có header này trên endpoint bắt buộc → `400 VALIDATION_FAILED`.

---

## 8. Cache

| Loại | Header |
|---|---|
| Danh mục, nội dung tĩnh (`/regions`, `/site-info`, `/posts`) | `Cache-Control: public, max-age=300` |
| Listing sản phẩm | `public, max-age=60` |
| Chi tiết sản phẩm | `public, max-age=60` |
| **Ngày khởi hành và giá** | `no-store` |
| Mọi thứ có tính tiền, giữ chỗ, đơn đặt | `no-store` |
| API quản trị | `no-store` |

**Ngày khởi hành không bao giờ được cache.** Chỗ còn thay đổi từng phút; hiện số
chỗ cũ là dẫn khách vào một giao dịch sẽ thất bại ở bước cuối.

Mọi phản hồi phụ thuộc locale phải có `Vary: Accept-Language`, nếu không CDN sẽ
phục vụ bản tiếng Đan cho khách Việt.

---

## 9. Danh mục endpoint

### 9.1. Công khai — đọc

| Endpoint | Ghi chú |
|---|---|
| `GET /{market}/site-info` | Thị thực, mùa, tiền tệ, lệch giờ |
| `GET /{market}/regions` | Kèm số sản phẩm, **đếm theo sản phẩm** không cộng dồn điểm đến |
| `GET /{market}/destinations` · `/{slug}` | |
| `GET /{market}/themes` | |
| `GET /{market}/products` | Bộ lọc ở mục 6 |
| `GET /{market}/products/{slug}` | Trả **cả bảng con** theo `productType` |
| `GET /{market}/products/{slug}/itinerary` | 404 với `COMBO`, `DAY_TOUR` |
| `GET /{market}/products/{slug}/hotels` | 404 với `CRUISE` |
| `GET /{market}/products/{slug}/ship` | Chỉ `CRUISE` |
| `GET /{market}/products/{slug}/departures` | `CRUISE` trả **phẳng**, gộp theo ngày là việc của frontend |
| `GET /{market}/products/{slug}/price-tiers` | Chỉ `PRIVATE_TOUR` |
| `GET /{market}/posts` · `/{slug}` | Lọc `?tag=` lặp lại |
| `GET /{market}/lectures` | Chỉ buổi **chưa diễn ra** — lọc ở truy vấn |
| `GET /{market}/consultants` | |

`GET /products/{slug}` trả một đối tượng có trường `productType` và **đúng một**
đối tượng con tương ứng. Trong OpenAPI dùng `discriminator` trên `productType`,
nhờ đó generator sinh ra kiểu tổng (sealed interface Java / union TypeScript) —
frontend buộc phải xử lý đủ mọi loại, quên một loại là lỗi biên dịch.

### 9.2. Công khai — ghi

| Endpoint | Ghi chú |
|---|---|
| `POST /{market}/pricing/preview` | Tính giá, **không lưu gì**. Trả bảng phân rã từng dòng |
| `POST /{market}/seat-holds` | Trả `expiresAt`. Idempotent |
| `DELETE /{market}/seat-holds/{id}` | Khách quay lại bước trước |
| `POST /{market}/bookings` | Idempotent. Cần `seatHoldId` với loại có tồn kho |
| `GET /{market}/bookings/{reference}` | Cần thêm `?email=` khớp — không có đăng nhập ở v1 |
| `POST /{market}/quote-requests` | Chỉ `PRIVATE_TOUR`. Idempotent |
| `POST /{market}/leads` | |
| `POST /{market}/newsletter-subscriptions` | |
| `POST /{market}/lectures/{id}/signups` | |

`POST /pricing/preview` tách khỏi `POST /bookings` là có chủ ý: màn hình đặt tour
gọi nó mỗi lần khách đổi lựa chọn, và nó phải **không có tác dụng phụ nào**.

### 9.3. Quản trị

Nhóm theo tài nguyên, không nhóm theo màn hình:

```
/admin/products            CRUD, mọi trạng thái, mọi locale
/admin/products/{id}/translations/{locale}
/admin/departures
/admin/departures/{id}/prices
/admin/price-tiers
/admin/bookings            xem, đổi trạng thái
/admin/quotes              dựng báo giá, gửi
/admin/leads
/admin/translations/queue  hàng đợi dịch — bản da PUBLISHED mà vi thiếu/OUTDATED
/admin/translations/coverage   bảng độ phủ
/admin/content/{type}      điểm đến, khách sạn, tham quan, bài viết, sự kiện
/admin/users
```

Ba endpoint dịch thuật phục vụ đúng ba màn hình của `02` mục 8.

---

## 10. Xác thực

| Ai | Cách |
|---|---|
| Khách | **Không đăng nhập ở v1.** Tra đơn bằng `reference` + `email` |
| Nhân viên | Spring Security, phiên đăng nhập, cookie `HttpOnly` + `SameSite=Lax` |

Bốn vai trò — chi tiết quyền ở `22`:

```
CONSULTANT   xem lead, xem booking, dựng quote
EDITOR       viết nội dung bản da
TRANSLATOR   dịch da → vi
ADMIN        toàn quyền, gồm giá và người dùng
```

Tra đơn bằng `reference` + `email` phải có **giới hạn tần suất** — nếu không nó
là kênh dò mã đơn.

---

## 11. Giới hạn tần suất

| Nhóm | Giới hạn |
|---|---|
| Đọc công khai | 120 lượt/phút mỗi IP |
| `POST /leads`, `/newsletter-subscriptions`, `/lectures/*/signups` | 5 lượt/giờ mỗi IP |
| `GET /bookings/{reference}` | 10 lượt/giờ mỗi IP |
| `POST /seat-holds` | 20 lượt/giờ mỗi IP |
| Đăng nhập quản trị | 10 lượt/15 phút mỗi tài khoản |

Vượt → `429 RATE_LIMITED` kèm `Retry-After`.

---

## 12. Việc còn để ngỏ

| Việc | Chặn cái gì |
|---|---|
| Webhook của cổng thanh toán trả về đường dẫn nào, ký thế nào | `30`, endpoint xác nhận thanh toán |
| Có cần API cho ứng dụng di động ở v2 không | Có ảnh hưởng tới quyết định phân trang offset |
| Định dạng `reference` chốt chưa | `12`, mẫu sinh mã |
| Giới hạn tần suất theo IP có đủ không, hay cần theo phiên | `35` |
| **`12` chưa có bảng `product_theme`** nên bộ lọc `?theme=` và endpoint `/themes` ở mục 9.1 chưa hiện thực được | Bộ lọc theo chủ đề ở listing |
| Sắp xếp `?sort=departureDate,asc` cần join `departure`, chưa làm | Sắp theo ngày khởi hành gần nhất |
