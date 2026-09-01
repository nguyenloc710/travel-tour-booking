# Kiến trúc hệ thống

```
Trạng thái: Nháp
Cập nhật: 31/08/2026
Nguồn sự thật về: sơ đồ hệ thống, bố cục repo, module Gradle, ranh giới phụ thuộc,
                  luồng một request, môi trường, danh mục ADR.
Không nói về: lược đồ CSDL (12), hợp đồng API chi tiết (13),
              pipeline CI/CD chi tiết (34).
```

---

## 1. Sơ đồ

```
                    ┌──────────────────────────────┐
   Khách   ────────►│  web/  Next.js               │
   Nhân viên ──────►│  · site khách (SSG + SSR)    │
                    │  · trang quản trị (SPA)      │
                    │  · message catalog da / vi   │
                    └──────────────┬───────────────┘
                                   │  REST, TS client
                                   │  sinh từ openapi.yaml
                    ┌──────────────▼───────────────┐
                    │  api/  Spring Boot           │
                    │  web → application → domain  │
                    └──────────────┬───────────────┘
                                   │
                    ┌──────────────▼───────────────┐
                    │  PostgreSQL                  │
                    │  ICU collation, unaccent     │
                    └──────────────────────────────┘

   Bên ngoài:  cổng thanh toán (DK, VN) · dịch vụ gửi email · lưu trữ ảnh
```

Một API duy nhất phục vụ cả website khách lẫn trang quản trị. Không tách hai
service ở v1 — quy mô không đủ để trả giá cho chi phí vận hành hai thứ, và tách
sau được nếu cần.

---

## 2. Bố cục repo

Một repo, hai toolchain (ADR-001).

```
travel-tour-booking/
  api/                    Gradle multi-module, Spring Boot
    domain/               Quy tắc nghiệp vụ thuần. KHÔNG phụ thuộc Spring
    application/          Use case, transaction, cổng ra ngoài
    infrastructure/       JPA, cổng thanh toán, email, lưu trữ
    web/                  Controller, ánh xạ DTO
    build.gradle.kts
  web/                    pnpm workspace
    apps/site/            Next.js — website khách
    apps/admin/           Next.js — trang quản trị
    packages/api-client/  TS client SINH RA, không sửa tay
    packages/ui/          Design system dùng chung
    packages/i18n/        Message catalog da / vi
  contracts/
    openapi.yaml          Nguồn sự thật của hợp đồng API
  docs/                   Bộ tài liệu, dùng chung
  .github/workflows/      api.yml · web.yml · tai-lieu.yml — lọc theo đường dẫn
  docker/
  compose.yaml
```

Giữ một repo vì `docs/` và `contracts/` phải dùng chung. Tách repo là lúc hợp
đồng API bắt đầu lệch giữa hai bên mà không ai phát hiện.

CI lọc theo đường dẫn: sửa `web/` không kích hoạt build Gradle, và ngược lại. Sửa
`contracts/` kích hoạt **cả hai**, vì cả hai bên đều sinh code từ đó.

Hai pipeline code, cộng một pipeline thứ ba `tai-lieu.yml` chạy bộ kiểm tài liệu
khi `docs/` đổi. Tách ra chứ không gộp vào hai cái kia, vì cùng một lý do: sửa
tài liệu không phải chờ Gradle lẫn pnpm, mà sửa code cũng không phải chờ bộ kiểm
tài liệu.

---

## 3. Bố cục package — một module, chia theo feature

ADR-010. Một module Gradle; ranh giới nằm ở tầng package:

```
vn.travel.booking.
  <feature>/
    controller/   nhận HTTP, implements interface sinh từ spec
    dto/          bản ghi thuần đi giữa các tầng
    entity/       entity JPA — chỉ feature nào có đường ghi
    mapper/       entity sang DTO, bằng MapStruct
    repository/   truy cập cơ sở dữ liệu
    service/      quy tắc nghiệp vụ và use case
  common/{config, dto, entity, exception, mapper, money, repository, util}
```

Feature hiện có: `admin` · `auth` · `booking` · `departure` · `destination` ·
`lecture` · `market` · `post` · `pricing` · `product` · `region` · `theme`.

**Thêm tính năng mới thì tạo feature mới theo đúng sáu thư mục con**, không nhét
vào feature sẵn có.

Ba lõi tính toán — `pricing/service/PricingEngine`,
`departure/service/DepartureStatuses`, `booking/service/BookingStatuses` — là
**hàm thuần**: không nhận dependency, không đọc đồng hồ, không chạm cơ sở dữ
liệu, test bằng JUnit thuần trong vài mili giây. Đây là điều bản demo đã làm đúng
ở `lib/pricing.ts`, và là tính chất phải giữ.

> **Không còn `archTest`.** Trước đây bốn module Gradle cưỡng chế ranh giới này
> bằng ArchUnit; ADR-010 đổi sang một module, nên ranh giới nay là **quy ước đọc
> được**, không phải hàng rào. Cái mất được ghi thẳng trong ADR đó.

### 3.1. Ngày giờ

`domain` **không đọc đồng hồ hệ thống**. Hàm nào cần "hôm nay" thì nhận `LocalDate`
làm tham số. Tầng `application` truyền vào từ `Clock` được tiêm, và test truyền
ngày cố định. Giảm giá đặt sớm phụ thuộc khoảng cách tới ngày khởi hành — test
đọc đồng hồ thật sẽ đỏ vào một ngày nào đó trong tương lai mà không ai hiểu vì sao.

---

## 4. Hợp đồng API — spec-first

Quyết định ở ADR-002.

```
contracts/openapi.yaml   ← viết tay, review như review code
        │
        ├──► openapi-generator ──► interface Java  ──► api/web implements
        │
        └──► openapi-generator ──► TS client       ──► web/packages/api-client
```

Controller **`implements`** interface sinh ra. Hệ quả: đổi spec mà quên sửa
controller là **lỗi biên dịch**, không phải bug lúc chạy. Đây là lý do duy nhất
để chọn spec-first thay vì code-first.

Quy tắc:

1. `openapi.yaml` sửa trong pull request riêng, có người duyệt.
2. Code sinh ra **không commit** — sinh lúc build. Không ai sửa tay được.
3. CI kiểm tương thích ngược so với nhánh chính.
4. Đổi phá vỡ tương thích thì lên phiên bản đường dẫn `/api/v2/`.

---

## 5. Luồng một request

```
GET /api/v1/dk/tours?region=nord
Accept-Language: da

  web/TourController          ← lấy market từ đường dẫn, locale từ header
    → application/ListToursUseCase
      → domain: lọc, giải trạng thái, tính giá "từ"
      → infrastructure/TourQueryRepository
        → SQL: JOIN tour_translation ON locale = 'da' AND status = 'PUBLISHED'
               JOIN tour_market_price ON market = 'DK'
               ORDER BY title COLLATE "da-DK-x-icu"
    ← DTO: số + mã tiền tệ, KHÔNG định dạng
  ← 200, Content-Language: da, Vary: Accept-Language
```

Bốn điều đọc ra từ luồng này, đều là quy tắc:

- **Market ở đường dẫn, locale ở header** — `02` mục 7.
- **`INNER JOIN` chính là chính sách không-fallback.** Bản ghi thiếu bản dịch tự
  rơi khỏi kết quả, không cần điều kiện `if` ở tầng ứng dụng.
- **Sắp xếp ở CSDL bằng collation ICU**, không sắp trong Java.
- **API không định dạng tiền.** Trả `{"amount":"24990.00","currency":"DKK"}`,
  frontend định dạng bằng `Intl.NumberFormat`.

---

## 6. Truy cập dữ liệu — hai đường

| Loại | Dùng gì | Vì sao |
|---|---|---|
| CRUD cho trang quản trị | Spring Data JPA | Đơn giản, đủ dùng, có transaction |
| Truy vấn listing cho website khách | jOOQ hoặc SQL thuần | Join bảng dịch kèm lọc theo market, sắp theo collation — JPA sinh SQL tệ ở đây |

Không dùng Hibernate `@Filter` cho locale. Lý do ở `02` mục 6.4: filter hoạt động
âm thầm, quên bật ở một service là rò rỉ nội dung sai ngôn ngữ.

---

## 7. Tiền và số

| Chỗ | Kiểu |
|---|---|
| Java | `BigDecimal`, scale theo tiền tệ. **Không bao giờ** `double` hay `float` |
| Postgres | `NUMERIC(12,2)` kèm cột `currency VARCHAR(3)` |
| JSON | Chuỗi: `"24990.00"` |

`amount` là chuỗi trong JSON vì số dấu phẩy động của JavaScript làm hỏng tiền.
VND có 0 chữ số thập phân, DKK có 2 — scale lấy từ cấu hình market, không hardcode.

---

## 8. Xác thực

| Ai | Cách |
|---|---|
| Khách | **Không đăng nhập ở v1.** Đơn tra cứu bằng mã, không có tài khoản |
| Nhân viên | Spring Security, phiên đăng nhập, bốn vai trò |

Không đăng nhập cho khách là quyết định có chủ đích: cổng khách hàng nằm ở v2
(`01` mục 3.2), và không có tài khoản khách thì phạm vi dữ liệu cá nhân phải bảo
vệ nhỏ hơn hẳn.

---

## 9. Môi trường

| Môi trường | Dùng để | Dữ liệu |
|---|---|---|
| Máy dev | Phát triển | Dữ liệu mồi, Docker Compose |
| Staging | Duyệt nội dung, thử thanh toán | Bản sao đã ẩn danh |
| Production | Chạy thật | Thật |

Máy dev chạy `docker compose up` để có Postgres đúng phiên bản, đúng extension —
`unaccent` và `pg_trgm` phải bật sẵn, không cài tay.

Test dùng **Testcontainers** với Postgres thật. H2 không mô phỏng được ICU
collation và `unaccent`, mà đó chính là hai thứ dễ sai nhất của dự án này.

---

## 10. Danh mục ADR

| ADR | Quyết định | Trạng thái |
|---|---|---|
| 001 | Một repo hai toolchain; Java + Spring Boot + Postgres | Đã chốt |
| 002 | Hợp đồng API spec-first | Đã chốt |
| 003 | Bảng dịch riêng, không JSONB | Đã chốt |
| 004 | `da` là ngôn ngữ nguồn | Đã chốt |
| 005 | Mỗi loại sản phẩm một bảng con | Đã chốt |
| 006 | Ngày khởi hành thuộc về một thị trường | Đã chốt |
| 007 | Cổng thanh toán từng thị trường | Chưa viết, chặn bởi `30` |
| 008 | Lưu trữ ảnh | Chưa viết |
| 009 | `COMBO` bó cố định hay tồn kho thời gian thực | Chưa viết, xem `04` mục 10 |

Một file một quyết định. Đổi ý thì viết ADR mới thay thế, **không sửa ADR cũ**.

---

## 11. Việc còn để ngỏ

| Việc | Chặn cái gì | Ghi chú |
|---|---|---|
| Quy mô dự kiến (số tour, số đơn/tháng) | Chọn hạ tầng, có cần cache không | Hỏi nghiệp vụ — `01` mục 7 |
| Lưu ảnh ở đâu: VPS, S3, hay CDN | ADR-007, `24` | Ảnh hero nặng, nhóm khách lớn tuổi hay dùng mạng chậm |
| Có cần cache tầng ứng dụng ở v1 không | `35` | Nghiêng về **không** — Postgres đủ nhanh ở quy mô này, và cache làm chính sách locale khó suy luận hơn |
| Chạy một hay nhiều instance | Cần ShedLock hay không cho job quét hạn giữ chỗ | `14`, `35` |
