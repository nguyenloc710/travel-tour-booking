# CODEBASE GUIDE — TRAVEL TOUR BOOKING

Tài liệu hướng dẫn toàn diện về kiến trúc, mã nguồn, quy tắc nghiệp vụ và các luồng vận hành của dự án **travel-tour-booking**.
Dành cho AI Agent và lập trình viên để nhanh chóng kết nối, đọc hiểu source code và thực thi công việc chính xác mà không vi phạm các ràng buộc hệ thống.

---

## 1. Bối Cảnh Nghiệp Vụ & 4 Nguyên Tắc Bất Biến (Invariants)

Dự án là hệ thống **website bán tour du lịch Việt Nam**, phục vụ hai thị trường (`DK`, `VN`), hỗ trợ hai ngôn ngữ (`da`, `vi`). Gồm website khách, API backend và trang quản trị nội bộ.

### 4 Nguyên tắc cốt lõi — Không bao giờ được vi phạm:

1. **`Market` và `Locale` là hai khái niệm độc lập:**
   - **Market** (`DK`, `VN`): Quyết định khách **mua gì** — catalog sản phẩm, bảng giá, tiền tệ (DKK / VND), cổng thanh toán, pháp lý, chính sách đặt cọc. Market nằm ở **đoạn đường dẫn API** (`/api/v1/dk/...`) và lưu ở **cookie** phía web khách.
   - **Locale** (`da`, `vi`): Chỉ quyết định khách **đọc bằng tiếng gì**. Locale nằm ở **đoạn đầu URL web** (`/da/...`, `/vi/...`) và gửi qua header HTTP `Accept-Language` tới API.
   - *Ví dụ:* Một Việt kiều ở Đan Mạch mua tour tại thị trường Đan Mạch (`DK` - giá DKK bao gồm vé máy bay quốc tế) nhưng đọc giao diện bằng tiếng Việt (`vi`). **Tuyệt đối không bao giờ suy luận Market từ Locale ở backend.**
2. **Tiếng Đan Mạch (`da`) là ngôn ngữ nguồn (Source Language):**
   - Nội dung bán hàng (tour, điểm đến, bài viết, hành trình) bắt buộc viết bằng `da` trước, dịch sang `vi` sau.
   - CSDL có trigger hoãn (`ct_product_source_translation`) chặn lưu sản phẩm nếu thiếu bản dịch `da`.
3. **Nội dung bán hàng TUYỆT ĐỐI KHÔNG FALLBACK:**
   - Nếu một tour, điểm đến hay bài viết chưa có bản dịch tiếng Việt (`vi`), khi khách xem trang tiếng Việt, nội dung đó **bị ẩn hoàn toàn** (không listing, không tìm kiếm, không sitemap, URL trực tiếp trả về `404`). Không bao giờ hiển thị bản `da` thay thế.
   - Điều này được hiện thực tự nhiên ở SQL bằng `INNER JOIN` với bảng `*_translation`.
   - *Ngược lại:* Chuỗi giao diện tĩnh (nhãn nút, placeholder) fallback về `da` nếu thiếu key dịch.
4. **Không quy đổi tỷ giá (No Currency Exchange):**
   - Giá ở mỗi thị trường là giá người nhập riêng biệt, không phải kết quả nhân tỷ giá. Tour bán cho khách Đan bao gồm vé máy bay từ Copenhagen/Billund và tiêu chuẩn dịch vụ riêng; tour bán cho khách Việt là sản phẩm khác.
   - **Tuyệt đối không có cột `exchange_rate` ở bất kỳ bảng nào trong CSDL.**

---

## 2. Bố Cục Thư Mục & Công Nghệ (Repository Map)

Cấu trúc Monorepo (`travel-tour-booking/`):

```
travel-tour-booking/
├── api/                     # Backend: Java 21, Spring Boot 4, Gradle (1 module chia feature)
│   ├── src/main/java/vn/travel/booking/
│   │   ├── <feature>/       # admin, auth, booking, departure, destination, itinerary,
│   │   │                    # lecture, market, media, post, pricing, product, quote, region, theme
│   │   │   ├── controller/  # REST controller, annotation định tuyến
│   │   │   ├── dto/         # Domain DTOs / Records
│   │   │   ├── entity/      # JPA entities (chỉ có ở feature có đường ghi)
│   │   │   ├── mapper/      # MapStruct mappers
│   │   │   ├── repository/  # Spring Data JPA / JdbcTemplate
│   │   │   └── service/     # Business logic, pure domain engine
│   │   └── common/          # config, exception, money, util, BaseEntity
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/    # Flyway migrations (V1__ -> V9__, R__du_lieu_tra_cuu.sql)
│   ├── src/test/java/       # JUnit 5 thuần cho pure domain, Testcontainers cho integration test
│   └── scripts/             # seed-dev.sql, kiem-nhat-quan.sql
├── web/                     # Frontend: Next.js 16 (App Router), TypeScript, pnpm workspace
│   ├── apps/
│   │   ├── site/            # Website khách: đa ngôn ngữ trên URL, SSR + SSG, proxy.ts
│   │   └── admin/           # Trang quản trị: SPA, quản lý sản phẩm, đơn, báo giá, nội dung
│   └── packages/
│       ├── api-client/      # TS Client sinh tự động từ openapi.yaml (KHÔNG SỬA TAY)
│       ├── i18n/            # Catalog từ điển da/vi, pathnames mapping
│       └── ui/              # Format tiền tệ/ngày tháng dùng chung, Design tokens
├── contracts/
│   └── openapi.yaml         # NGUỒN SỰ THẬT của hợp đồng API (Spec-First)
├── docs/                    # 24 tài liệu đặc tả chi tiết, 13 ADRs, sổ trạng thái 41-tinh-trang.md
├── scripts/                 # Python scripts hỗ trợ (docs_check.py, tai-anh-mau.py, nap-anh-len-kho.py)
├── .claude/                 # Quy trình tự động hoá, hooks, commands, skills
└── compose.yaml             # Hạ tầng dev: PostgreSQL 16 (ICU, unaccent) + MinIO (S3 storage)
```

---

## 3. Kiến Trúc Backend (API — Spring Boot 4 / Java 21)

### 3.1. Bố cục Package theo Feature (ADR-010)
Toàn bộ backend nằm trong **một module Gradle**, phân chia theo domain feature:
`vn.travel.booking.<feature>/{controller, dto, entity, mapper, repository, service}`.

Khi thêm tính năng mới, tạo thư mục feature mới với đầy đủ các tầng con tương ứng, không nhét bừa vào feature đã có.

### 3.2. Hai đường truy cập dữ liệu (CQRS nhẹ — `docs/10`)
- **Đường ĐỌC (Website khách):** Sử dụng **SQL thuần + `JdbcTemplate`** (`<feature>/repository/*Repository`).
  - Lý do: Tối ưu hiệu năng, trực tiếp thực hiện `INNER JOIN` với bảng dịch đa ngôn ngữ, sắp xếp bằng ICU collation (`COLLATE "da-DK-x-icu"` / `"vi-VN-x-icu"`), tìm kiếm không dấu (`f_unaccent`). JPA sinh SQL rất kém ở các truy vấn này.
- **Đường GHI (Trang quản trị / Đặt tour):** Sử dụng **Spring Data JPA + MapStruct** (`<feature>/{entity, mapper, repository}`).
  - Quản lý transaction rõ ràng, kiểm soát trạng thái thực thể.

### 3.3. Ba lõi tính toán thuần túy (Pure Domain Engines)
Nằm ở tầng `service`, **tuyệt đối không phụ thuộc Spring, không đọc CSDL, không đọc đồng hồ hệ thống (`LocalDate.now()`)**:
1. **`pricing/service/PricingEngine`:**
   - Thứ tự cộng dồn cố định: Giá cơ bản theo loại khách $\rightarrow$ Phụ thu phòng đơn $\rightarrow$ Nâng hạng cabin (Cruise) $\rightarrow$ Phụ thu điểm khởi hành $\rightarrow$ Bảo hiểm $\rightarrow$ Khách sạn trước bay $\rightarrow$ Giảm đặt sớm (số âm) $\rightarrow$ Phí xử lý.
   - **Làm tròn ở từng dòng** (`HALF_UP`) rồi mới cộng tổng (để bảng phân rã cộng lại đúng bằng tổng).
   - `deposit` làm tròn xuống (`FLOOR`), `balance = total - deposit` để đảm bảo $deposit + balance = total$ luôn chính xác 100%.
2. **`departure/service/DepartureStatuses`:**
   - Giải trạng thái ngày khởi hành theo thứ tự: `PENDING` $\rightarrow$ `SOLD_OUT` $\rightarrow$ Hết chỗ khả dụng $\rightarrow$ `GUARANTEED` (khi đủ `guaranteedThreshold`) $\rightarrow$ `FEW_SEATS` $\rightarrow$ Ghi đè tay $\rightarrow$ `OPEN`.
   - **`GUARANTEED` được ưu tiên xét trước `FEW_SEATS`** và nhân viên không thể ghi đè tay thành `GUARANTEED`.
3. **`booking/service/BookingStatuses`:**
   - Máy trạng thái hữu hạn cho đơn hàng. **Không có bước lùi** (ví dụ: `CANCELLED` không thể quay lại `CONFIRMED`).

### 3.4. Cột do Cơ Sở Dữ Liệu sở hữu (Database-owned Columns)
Trong Java entity, các cột sau bắt buộc phải đánh dấu `insertable = false, updatable = false`:
- `created_at`: CSDL điền bằng `DEFAULT now()`.
- `last_modified_at`: CSDL điền bằng database trigger `tg_*_last_modified`. Java không được phép cập nhật.
- `product_market.price_from`: CSDL vật chất hoá bằng trigger ở migration `V5` dựa trên `MIN(departure_price.amount)`.

### 3.5. Kiểm toán và Xoá mềm (`BaseEntity`)
- `BaseEntity` (trong `common.entity`) chứa 5 cột chuẩn: `created_at`, `created_by`, `last_modified_at`, `last_modified_by`, `soft_delete`.
- `BaseEntity` **không dùng `@LastModifiedDate`** (do trigger CSDL phụ trách). `created_by` và `last_modified_by` do Spring Data JPA `AuditorAware` điền thông qua session của nhân viên.
- Mọi truy vấn đọc của website khách **bắt buộc phải có điều kiện `AND NOT soft_delete`**. Xoá mềm không cascade xuống bảng con.

### 3.6. Bảo Mật & Xác Thực (SecurityConfig)
- **Khách hàng:** Không cần đăng nhập ở phiên bản v1. Đơn hàng được tra cứu qua mã `reference` (ví dụ `VN-2026-8F3K2P`).
- **Nhân viên:** Đăng nhập phiên thông qua cookie session (Spring Security), BCrypt password hashing.
  - 4 vai trò: `ADMIN`, `CONSULTANT`, `EDITOR`, `TRANSLATOR` (ma trận quyền chi tiết tại `docs/22`).
- **CSRF:**
  - Bật cho toàn bộ bề mặt Admin: cấu hình `CookieCsrfTokenRepository.withHttpOnlyFalse()` gửi thẻ `XSRF-TOKEN`.
  - Miễn trừ CSRF cho public API dạng `/api/v1/{market:[a-z]{2}}/**` và đường dẫn đăng nhập `/api/v1/admin/session`.
- **Idempotency (Tính bất biến khi gọi lại):**
  - Khách gửi yêu cầu ghi (giữ chỗ, đặt tour, yêu cầu báo giá) kèm header `Idempotency-Key: <UUID>`.
  - Bảng `idempotency_key` lưu vết kèm `request_hash` (SHA-256 của body) để tránh nuốt nhầm request khác body nhưng cùng key.

### 3.7. Kho Lưu Trữ Media (MinIO / S3)
- Lưu trữ trên MinIO tự dựng (`compose.yaml`), bucket `travel-media`.
- **Khai báo 2 endpoint tách biệt** (`application.yml`):
  - `endpoint-public` (`TRAVEL_STORAGE_ENDPOINT_PUBLIC`): Dùng để sinh URL công khai và ký Presigned URL upload (SigV4 ký kèm host, nếu ký bằng host nội bộ thì trình duyệt upload sẽ dính lỗi 403 Forbidden).
  - `endpoint-internal` (`TRAVEL_STORAGE_ENDPOINT_INTERNAL`): Backend gọi nội bộ kiểm tra file tồn tại.
- `media_asset.path` chỉ lưu **đường dẫn tương đối** (ví dụ `tour/sapa-01.jpg`), không bao giờ lưu URL tuyệt đối vào CSDL.

---

## 4. Kiến Trúc Frontend (Web — Next.js 16 App Router)

### 4.1. Bố cục Workspace
- `apps/site`: Website khách hàng.
  - Hỗ trợ đa ngôn ngữ trực tiếp trên đường dẫn URL: `app/[locale]/[section]/[slug]`.
  - Next.js 16 sử dụng `src/proxy.ts` (thay cho `middleware.ts`).
  - Typography: Playfair Display (tiêu đề) + Be Vietnam Pro (thân bài), bảng màu ấm, thiết kế co giãn (fluid typography).
  - Ràng buộc WCAG AA cho đối tượng khách hàng lớn tuổi Bắc Âu: cỡ chữ $\ge$ 16px, line-height 1.6, touch target $\ge$ 44x44px, không dùng hover làm điều kiện duy nhất để xem thông tin.
- `apps/admin`: Ứng dụng quản trị (SPA).
  - URL **không có locale**: `admin/san-pham`, `admin/don`, `admin/bao-gia`, `admin/noi-dung`, `admin/nguoi-dung`.
  - Locale trong admin là thuộc tính của bản ghi nội dung đang chỉnh sửa.
- `packages/api-client`: Client TypeScript tạo tự động bởi `openapi-generator-cli`. **Không sửa tay thư mục này.**
- `packages/i18n`: Dữ liệu tĩnh song ngữ.
  - Bảng ánh xạ URL path segment `pathnames.ts`:
    - `tour` (vi) $\leftrightarrow$ `rejser` (da)
    - `tim-tour` (vi) $\leftrightarrow$ `rejsefinder` (da)
    - `diem-den` (vi) $\leftrightarrow$ `destinationer` (da)
    - `dat-tour` (vi) $\leftrightarrow$ `booking` (da)
    - `xac-nhan` (vi) $\leftrightarrow$ `bekraeftelse` (da)
- `packages/ui`: Thư viện component và formatters:
  - Format tiền tệ bằng `Intl.NumberFormat` (DKK: 2 số thập phân `24.990 kr.`, VND: 0 số thập phân `18.900.000 ₫`).
  - Tiền từ API trả về là **chuỗi** `amount`, không dùng `parseFloat` để tính toán nhằm tránh lỗi dấu phẩy động JS.

### 4.2. Quy tắc giao diện bắt buộc
1. **Không hardcode số liệu:** "Xem tất cả 12 tour" phải lấy từ `totalItems` mà API trả về theo đúng `(market, locale)`.
2. **Bộ lọc nằm trên URL (`useSearchParams`):** F5 hoặc gửi link cho người khác phải giữ nguyên trạng thái filter.
3. **Luôn có 3 trạng thái:** Đang tải (**Skeleton**, không dùng spinner vòng xoay), Rỗng (Empty state kèm gợi ý), Lỗi (Error state).
4. **Giá luôn kèm chữ "từ" và disclaimer pháp lý:** Nằm sẵn trong UI component hiển thị giá.

---

## 5. Cơ Sở Dữ Liệu & Data Model (PostgreSQL 16)

### 5.1. Mô hình Phân cấp Sản phẩm (6 loại tour — ADR-005)
1 bảng gốc `product` chứa thông tin chung + 6 bảng con chuyên biệt:
1. `product_group_tour`: Tour đoàn ghép (`min_pax`, `max_pax`, `guaranteed_threshold`, `tour_leader_language`, `fitness_level`).
2. `product_individual`: Gói tự do linh hoạt (`min_party_size`, `flexible_date_window_days`).
3. `product_private`: Tour thiết kế riêng (`lead_time_days`, `quote_valid_days`). Đi qua luồng báo giá.
4. `product_cruise`: Du thuyền vịnh/sông (`ship_name`, `port_count`, phân hạng cabin).
5. `product_combo`: Gói kết hợp (`nights`, `valid_from`, `valid_to`).
6. `product_day_tour`: Tour trong ngày (`duration_hours`, `cutoff_hours`). Không có `duration_days`.

> **Khoá ngoại kép chống gán sai bảng con:** Bảng gốc có `CONSTRAINT ux_product_id_type UNIQUE (id, product_type)`. Mọi bảng con liên kết bằng `FOREIGN KEY (product_id, product_type) REFERENCES product (id, product_type) ON DELETE CASCADE`.

### 5.2. Đa Ngôn Ngữ & Bảng Dịch
Mọi bảng có nội dung dịch đều có bảng phụ `<entity>_translation` (`product_translation`, `destination_translation`, `itinerary_day_translation`, ...):
- Khóa chính kép: `(entity_id, locale)`.
- Cột `slug` có `CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')`: Cấm ký tự có dấu ở cả hai ngôn ngữ.
- Collation ICU theo locale:
  - `da-DK-x-icu`: Các chữ cái `æ, ø, å` đứng sau `z`.
  - `vi-VN-x-icu`: Phân biệt thứ tự dấu thanh tiếng Việt và chữ `đ`.
- Tìm kiếm không dấu: Sử dụng hàm bọc `f_unaccent(text)` (được đánh dấu `IMMUTABLE`) kết hợp GIN index `pg_trgm`.

### 5.3. Xoá Mềm & Partial Unique Index (Cạm bẫy quan trọng!)
- Một bản ghi bị xoá mềm (`soft_delete = TRUE`) vẫn nằm trong bảng. Nếu dùng `CONSTRAINT ... UNIQUE (locale, slug)` thông thường thì slug đó sẽ bị khoá vĩnh viễn, không thể tái sử dụng.
- **Quy tắc bắt buộc:** Mọi ràng buộc duy nhất trên bảng có `soft_delete` phải là **Partial Unique Index**:
  ```sql
  CREATE UNIQUE INDEX ux_product_translation_slug
    ON product_translation (locale, slug)
    WHERE NOT soft_delete;
  ```
- *Ngoại lệ duy nhất:* `CONSTRAINT ux_product_id_type UNIQUE (id, product_type)` trên `product` bắt buộc giữ nguyên là constraint cứng, vì Foreign Key của PostgreSQL không thể tham chiếu vào Partial Index.

### 5.4. Lịch sử Slug & Redirect 301 (`slug_history`)
Khi biên tập viên đổi slug của sản phẩm, điểm đến, bài viết:
- Database trigger `trg_luu_slug_cu` tự động ghi nhận slug cũ vào bảng `slug_history`.
- Khi khách truy cập slug cũ, ứng dụng tra bảng này và trả về HTTP 301 chuyển hướng sang slug mới, tránh bị link chết SEO.

### 5.5. Tồn kho và Giữ chỗ (Inventory & Seat Hold)
- Áp dụng cho: `GROUP_TOUR`, `CRUISE`, `COMBO`, `DAY_TOUR`.
- Công thức tính chỗ trống khả dụng:
  $$\text{seatsAvailable} = \text{capacity} - \text{seatsBooked} - \sum(\text{seat\_hold còn hạn chưa nhả})$$
- Tạo giữ chỗ: Dùng khoá bi quan `SELECT ... FOR UPDATE` trên dòng `departure`, tạo bản ghi `seat_hold` có thời hạn 20 phút.
- Background Job quét hạn: Chạy định kỳ mỗi phút, giải phóng chỗ quá hạn (`released_at = now()`), sử dụng **ShedLock** (`shedlock` table) để không bị xung đột khi scale nhiều instance.

### 5.6. Bộ Kiểm Tra Nhất Quán Dữ Liệu (`api/scripts/kiem-nhat-quan.sql`)
Gồm **23 quy tắc kiểm tra toàn vẹn dữ liệu** (lịch trình thủng ngày, giá phẳng không biến thiên, thiếu giá phòng đơn, mồ côi ảnh/khách sạn, vi phạm giấy phép ảnh, v.v.).
Chạy bằng lệnh:
```bash
docker exec travel-postgres psql -U travel -d travel -f /tmp/kiem.sql
# Hoặc chạy test CI:
./gradlew test --tests "*KiemNhatQuanIT*"
```

---

## 6. Quy Trình Thay Đổi Hợp Đồng API (Spec-First & ADR-012)

Dự án áp dụng phương pháp **Spec-First** với `contracts/openapi.yaml` là nguồn sự thật duy nhất.

### Thứ tự các bước khi thay đổi API:
1. **Sửa `contracts/openapi.yaml`:** Bổ sung endpoint, tham số, DTO hoặc mã lỗi mới.
2. **Sinh code tự động:**
   ```bash
   pnpm contracts:generate
   ```
   Lệnh này sẽ sinh:
   - TypeScript API Client cho frontend (`web/packages/api-client/`).
   - Java Models / DTOs cho backend (`api/build/generated/openapi/`).
3. **Cập nhật Controller trong Java (`api/`):**
   - Theo **ADR-012**: Controller **không còn `implements` interface sinh ra**, mà tự khai báo annotation `@RestController`, `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestBody`.
   - Cần đảm bảo đường dẫn, kiểu dữ liệu khớp hoàn toàn với `openapi.yaml`.
4. **Kiểm tra tương thích ngược:**
   - Chạy test kiểm tra khớp đường dẫn SpringDoc vs Contract:
     ```bash
     ./gradlew test --tests "*SwaggerIT*"
     ```
   - Chạy kiểm tra OpenAPI breaking change:
     ```bash
     pnpm contracts:check
     ```
5. **Cập nhật code Frontend (`web/`):** Gọi qua client mới sinh trong `packages/api-client`.

---

## 7. Bản Đồ Các Luồng Nghiệp Vụ Chính Trong Code

### 7.1. Luồng Xem Danh Mục & Chi Tiết Tour
- **Client Web:** Khách vào `/vi/tour` hoặc `/da/rejser`.
- **API Endpoint:** `GET /api/v1/{market}/products?region=...&theme=...`
  - `ProductController` $\rightarrow$ `ProductQueryRepository` (SQL thuần).
  - Lọc `product_market.is_published = true`, `INNER JOIN product_translation` theo locale và `status = 'PUBLISHED'`.
- **Chi Tiết Tour:** `GET /api/v1/{market}/products/{slug}`
  - Trả về `ProductDetail` đa hình theo `ProductVariant` (Group, Cruise, Private, ...).
  - Tầng mapper `RefMapper.toProductDetail(...)` sử dụng `switch-case` trên `sealed interface` đảm bảo bao phủ đủ 6 loại tour.
  - Trang chi tiết website khách chia làm 5 tab: Tổng quan, Lịch trình từng ngày, Khách sạn lưu trú, Thông tin thực tế, Ngày khởi hành & Giá.

### 7.2. Luồng Đặt Tour Ghép (`GROUP_TOUR`, `CRUISE`)
1. **Chọn ngày & số khách:** Gọi `GET /api/v1/{market}/products/{slug}/departures` lấy danh sách ngày còn chỗ (trạng thái giải bởi `DepartureStatuses`).
2. **Giữ chỗ tạm thời (Seat Hold):**
   - `POST /api/v1/{market}/seat-holds` kèm `Idempotency-Key`.
   - `BookingService.holdSeats`: Khóa dòng `departure` bằng `Pessimistic Lock`, kiểm tra công thức tồn kho, tạo `seat_hold` 20 phút.
3. **Tính chi tiết giá (Pricing):**
   - `POST /api/v1/{market}/pricing/calculate`
   - `PricingEngine.calculate`: Trả về danh sách từng dòng `PriceLine` và số tiền đặt cọc/còn lại.
4. **Xác nhận đặt tour (Create Booking):**
   - `POST /api/v1/{market}/bookings` kèm `holdId`, thông tin người liên hệ và danh sách hành khách.
   - Chuyển `seat_hold` thành `booking` trong 1 transaction duy nhất: tăng `seats_booked` trên `departure`, chèn `booking`, `booking_line`, `booking_passenger`, `booking_event`.

### 7.3. Luồng Báo Giá Tour Riêng (`PRIVATE_TOUR` — `docs/14` mục 7)
1. **Khách gửi yêu cầu báo giá:** Khách điền form tại trang chi tiết tour riêng $\rightarrow$ `POST /api/v1/{market}/quote-requests` $\rightarrow$ Tạo bản ghi `quote` ở trạng thái `DRAFT`.
2. **Tư vấn viên (Consultant) dựng giá trên Admin:**
   - Vào màn hình Báo giá (`/admin/bao-gia/[id]`), thêm các dòng chi phí `quote_line` theo bảng giá hoặc thỏa thuận riêng.
   - Nhấn "Gửi báo giá" $\rightarrow$ `POST /api/v1/admin/quotes/{id}/send`. Hệ thống tính `valid_until = sent_at + quote_valid_days`, chuyển trạng thái sang `SENT`.
3. **Khách duyệt báo giá:**
   - Khách nhận link xem báo giá $\rightarrow$ Bấm chấp nhận $\rightarrow$ `POST /api/v1/{market}/quotes/{reference}/accept`.
   - Chuyển trạng thái sang `ACCEPTED`, sinh đơn hàng `booking` tương ứng.
4. **Hết hạn báo giá:** `QuoteSweeper` chạy nền kiểm tra các `quote` có `status = 'SENT'` và `valid_until < now()`, chuyển thành `EXPIRED` (không tự động gia hạn).

---

## 8. Bảng Tổng Hợp "Cạm Bẫy" Cần Tránh (Pitfalls & Do's / Don'ts)

| Vấn đề | Tuyệt đối KHÔNG | Bắt buộc PHẢI |
|---|---|---|
| **Tiền tệ** | Không dùng `double`, `float`. Không format tiền ở backend. | Dùng `BigDecimal` (scale theo cấu hình market). Trả JSON string `"amount": "24990.00"`. Frontend format bằng `Intl.NumberFormat`. |
| **Tỷ giá** | Không quy đổi ngoại tệ, không tạo cột `exchange_rate`. | Mỗi thị trường có bảng giá người nhập độc lập. |
| **Đa ngôn ngữ** | Không dùng Hibernate `@Filter`. Không fallback nội dung tour sang `da`. | Dùng `INNER JOIN` với bảng `*_translation`. Thiếu bản dịch thì ẩn khỏi kết quả (404). |
| **Collation** | Không sắp xếp chuỗi bằng `String.compareTo()` hoặc Java stream. | Sắp xếp ở CSDL với ICU: `ORDER BY title COLLATE "da-DK-x-icu"` hoặc `"vi-VN-x-icu"`. |
| **Tìm kiếm** | Không index trực tiếp trên `unaccent(col)` (do không immutable). | Bọc qua hàm `f_unaccent(col)` rồi mới tạo GIN index `pg_trgm`. |
| **Xoá mềm** | Không dùng `CONSTRAINT ... UNIQUE` thông thường trên bảng xoá mềm. | Dùng Partial Unique Index: `CREATE UNIQUE INDEX ... WHERE NOT soft_delete`. |
| **Kiểm toán** | Không để Java ghi `last_modified_at`. Không dùng `@LastModifiedDate`. | Để trigger CSDL `tg_*_last_modified` tự cập nhật. |
| **Ngày giờ lõi** | Không gọi `LocalDate.now()` trong `PricingEngine`, `DepartureStatuses`. | Nhận ngày hiện tại từ tham số truyền vào (được tiêm từ Spring `Clock`). |
| **Contract API** | Không sửa tay code trong `packages/api-client` hoặc `build/generated`. | Sửa `contracts/openapi.yaml` rồi chạy `pnpm contracts:generate`. |
| **Testing** | Không dùng H2 Database cho test tích hợp. | Dùng `Testcontainers` với PostgreSQL 16 thật. |
| **Frontend Layout** | Không dùng `100vw` cho khối tràn viền (vì dính thanh cuộn). | Dùng lớp `.tran` và đặt `.khung` 72rem bên trong theo `docs/21`. |

---

## 9. Sổ Tay Lệnh Vận Hành Nhanh (Operations Cheat-sheet)

### Hạ tầng và Dữ liệu Dev
```bash
# 1. Khởi động Docker (Postgres 16 + MinIO)
docker compose up -d

# 2. Nạp dữ liệu mồi (khi API đã khởi động và tạo bảng qua Flyway)
docker cp api/scripts/seed-dev.sql travel-postgres:/tmp/seed.sql
docker exec travel-postgres psql -U travel -d travel -f /tmp/seed.sql

# 3. Tải và nạp ảnh mẫu lên MinIO
python scripts/tai-anh-mau.py
python scripts/nap-anh-len-kho.py

# 4. Chạy bộ kiểm nhất quán dữ liệu 23 quy tắc
docker cp api/scripts/kiem-nhat-quan.sql travel-postgres:/tmp/kiem.sql
docker exec travel-postgres psql -U travel -d travel -f /tmp/kiem.sql
```

### Backend (Java / Spring Boot)
```bash
cd api
./gradlew bootRun             # Chạy API server (cổng 8080, Swagger UI: /swagger-ui.html)
./gradlew test                # Chạy toàn bộ unit test & integration test (cần Docker)
./gradlew test --tests "*PricingEngineTest*"       # Test JUnit thuần không dựng context
./gradlew test --tests "*KiemNhatQuanIT*"          # Test kiểm tra nhất quán dữ liệu mồi
```

### Frontend (Next.js / pnpm workspace)
```bash
cd web
pnpm install                  # Cài đặt dependencies
pnpm dev                      # Chạy website khách (http://localhost:3000/vi hoặc /da)
pnpm dev:admin                # Chạy trang quản trị (http://localhost:3001)
pnpm typecheck                # Sinh API client và kiểm tra TypeScript toàn bộ 5 packages
pnpm lint                     # Kiểm tra linter
pnpm test                     # Chạy unit tests frontend
pnpm i18n:check               # Kiểm tra độ phủ khóa dịch i18n (thiếu là lỗi)
pnpm contracts:generate       # Sinh lại TS Client và Java Models từ OpenAPI
```

### Kiểm Tra Tài Liệu
```bash
python scripts/docs_check.py            # Kiểm tra link chết, cấu trúc metadata
python scripts/docs_check.py --truy-vet # Ma trận truy vết yêu cầu (YC / QT / RB)
```

---

## 10. Tài Liệu Tham Khảo Trọng Tâm

Khi cần đi sâu vào chi tiết kỹ thuật hoặc quyết định thiết kế:
- **`docs/41-tinh-trang.md`**: Sổ trạng thái cập nhật liên tục — việc đang làm, việc kế tiếp, câu hỏi đang chờ quyết định (ĐỌC FILE NÀY ĐẦU TIÊN KHI BẮT ĐẦU PHIÊN MỚI).
- **`docs/02-thi-truong-va-da-ngon-ngu.md`**: Chi tiết phân biệt Market vs Locale, cấu trúc URL, bảng dịch, fallback.
- **`docs/04-phan-loai-san-pham.md`**: Chi tiết nghiệp vụ 6 loại sản phẩm du lịch.
- **`docs/12-luoc-do-csdl.md`**: Toàn bộ DDL, index, trigger, constraint và 23 quy tắc kiểm tra nhất quán.
- **`docs/14-quy-tac-nghiep-vu.md`**: Chi tiết công thức tính giá, làm tròn, giải trạng thái và giữ chỗ tồn kho.
- **`docs/22-trang-quan-tri.md`**: Đặc tả chi tiết 11 màn hình quản trị và ma trận phân quyền nhân viên.
- **`docs/adr/`**: Tập hợp 13 bản ghi quyết định kiến trúc (ADR-001 đến ADR-013).
