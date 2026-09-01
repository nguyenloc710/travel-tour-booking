# api/CLAUDE.md

Backend: Java 21, Spring Boot, Gradle multi-module, PostgreSQL.

Đọc `../CLAUDE.md` trước — bốn điều quan trọng nhất của cả dự án nằm ở đó.

> **Trạng thái: đọc xong, ghi vừa bắt đầu** (01/09/2026). Bốn module Gradle,
> migration `V1`–`V3`, đủ danh mục đọc của `docs/13` mục 9.1 trừ `/site-info`,
> và bề mặt quản trị đầu tiên: đăng nhập phiên cookie, ma trận quyền, sửa bản
> dịch sản phẩm. Chưa có tồn kho, giữ chỗ, đặt tour — đó là G4.

## 0b. Đường đọc và đường ghi

Hai đường, hai công nghệ, `docs/10` mục 6 đã chốt:

| Đường | Dùng | Ở đâu |
|---|---|---|
| Đọc cho website khách | SQL thuần + `JdbcTemplate` | `infrastructure/<feature>/Jdbc*QueryAdapter` |
| Ghi cho trang quản trị | Spring Data JPA + MapStruct | `infrastructure/<feature>/{entity,repository,mapper}` |

**Entity chỉ nằm ở `infrastructure`.** `application` khai báo cổng và bản ghi
thuần; `web` không bao giờ thấy entity. ArchUnit canh cả hai chiều.

Lược đồ do Flyway sở hữu, Hibernate chạy `ddl-auto: validate` — không được tạo
hay sửa bảng nào.

## 0. Lệnh

```bash
./gradlew build            # biên dịch + test (gồm cả archTest ở project gốc)
./gradlew archTest         # chỉ kiểm ranh giới module
./gradlew :web:bootRun     # chạy API, cần Postgres ở cổng 5432
./gradlew :web:contractsGenerate   # sinh interface Java từ contracts/openapi.yaml

docker compose up -d       # Postgres 16, có ICU và contrib
psql postgresql://travel:travel@localhost:5432/travel -f scripts/seed-dev.sql
```

**Gradle phải chạy trên JDK 17 trở lên** (Spring Boot 4). Mã nguồn biên dịch
bằng JDK 21 qua toolchain. `JAVA_HOME` còn trỏ JDK cũ thì đặt
`org.gradle.java.home` trong `~/.gradle/gradle.properties`, không đặt trong repo.

**Migration không tự chạy trong test của `domain` và `application`** — hai module
đó không chạm CSDL, đúng như thiết kế.

---

## 1. Ranh giới module — ràng buộc quan trọng nhất

```
web  ──►  application  ──►  domain
              │
infrastructure┘
```

| Module | Được phụ thuộc | Tuyệt đối không |
|---|---|---|
| `domain` | Chỉ thư viện chuẩn Java | Spring, JPA, Jackson, **mọi annotation** |
| `application` | `domain` | JPA, Spring Web |
| `infrastructure` | `application`, `domain` | — |
| `web` | `application`, `domain` | JPA trực tiếp |

**`domain` không được biết Spring tồn tại.** Engine tính giá, giải trạng thái
ngày khởi hành, quy tắc lịch trình nằm ở đây và test bằng JUnit thuần trong vài
mili giây — không dựng context, không CSDL.

Kiểm bằng ArchUnit (`./gradlew archTest`), không dựa vào kỷ luật cá nhân.

### Ngày giờ

`domain` **không đọc đồng hồ hệ thống**. Hàm cần "hôm nay" thì nhận `LocalDate`
làm tham số; `application` truyền vào từ `Clock` được tiêm.

Giảm giá đặt sớm phụ thuộc khoảng cách tới ngày khởi hành. Test đọc đồng hồ thật
sẽ đỏ vào một ngày nào đó trong tương lai mà không ai hiểu vì sao.

---

## 2. Market và locale trong một request

| | Lấy từ đâu |
|---|---|
| Market | Đoạn đường dẫn — `/api/v1/dk/tours` |
| Locale | Header `Accept-Language` |

Phản hồi kèm `Content-Language` và `Vary: Accept-Language`.

Market vào đường dẫn vì nó đổi **tài nguyên** (`/dk/tours` và `/vn/tours` là hai
tập khác nhau, cache riêng được). Locale vào header vì nó chỉ đổi cách trình bày.

**Không bao giờ suy market từ locale ở backend.** Frontend gửi market tường minh.

---

## 3. Truy vấn nội dung đa ngôn ngữ

`INNER JOIN` với bảng dịch **chính là** chính sách không-fallback. Bản ghi thiếu
bản dịch tự rơi khỏi kết quả — không viết `if` ở tầng ứng dụng:

```sql
SELECT t.id, tt.title, tt.slug, tmp.price_from
FROM tour t
JOIN tour_translation tt
  ON tt.tour_id = t.id
 AND tt.locale  = :locale
 AND tt.status  = 'PUBLISHED'
JOIN tour_market_price tmp
  ON tmp.tour_id = t.id
 AND tmp.market  = :market
ORDER BY tt.title COLLATE "da-DK-x-icu";
```

**Không dùng Hibernate `@Filter`.** Filter hoạt động âm thầm; quên bật ở một
service là rò rỉ nội dung sai ngôn ngữ ra khách. Điều kiện locale phải nhìn thấy
được trong câu truy vấn. ADR-003.

### Hai đường truy cập dữ liệu

| Loại | Dùng |
|---|---|
| CRUD cho trang quản trị | Spring Data JPA |
| Listing cho website khách | jOOQ hoặc SQL thuần |

Join bảng dịch kèm lọc theo market và sắp theo collation là chỗ JPA sinh SQL tệ.

---

## 4. Sắp xếp và tìm kiếm — ở CSDL, không ở Java

`String.compareTo()` sắp theo mã Unicode và sai cho **cả hai** ngôn ngữ.

| Locale | Collation | Dễ sai chỗ nào |
|---|---|---|
| `da` | `da-DK-x-icu` | `æ ø å` đứng **sau** `z` |
| `vi` | `vi-VN-x-icu` | Thứ tự dấu thanh; `đ` là chữ riêng sau `d` |

Tìm không dấu: `unaccent` + `pg_trgm`. `hoi an` phải ra `Hội An`.

**Không test hai thứ này bằng H2** — H2 không mô phỏng được. Dùng Testcontainers
với Postgres thật.

---

## 5. Tiền

| Chỗ | Kiểu |
|---|---|
| Java | `BigDecimal`, scale theo tiền tệ |
| Postgres | `NUMERIC(12,2)` + cột `currency VARCHAR(3)` |
| JSON | Chuỗi: `"24990.00"` |

**Không bao giờ `double` hay `float` cho tiền.** VND có 0 chữ số thập phân, DKK
có 2 — scale lấy từ cấu hình market, không hardcode.

API trả số thô, **không định dạng**. Frontend định dạng.

---

## 6. Lỗi — trả mã, không trả câu

```json
{
  "code": "DEPARTURE_SOLD_OUT",
  "params": { "departureId": "…", "departDate": "2027-03-14" }
}
```

Backend trả `"Afgangen er udsolgt"` là sai: bản dịch sẽ tồn tại ở hai nơi và lệch
nhau. Mã lỗi còn dùng được cho log, thống kê và test; câu chữ thì không.

`MessageSource` chỉ dùng cho thứ backend gửi thẳng tới khách: email xác nhận, PDF
chương trình, hoá đơn.

Danh sách mã lỗi nằm trong `contracts/openapi.yaml`.

---

## 7. Trạng thái tính ra, không lưu

Ba chỗ dùng chung một mô-típ. Đừng thêm cờ vào CSDL cho bất kỳ cái nào:

| Trạng thái | Công thức |
|---|---|
| `GUARANTEED` (ngày khởi hành) | `seatsBooked >= minPax` |
| Số chỗ còn lại (thuyết trình) | `seats − seatsTaken` |
| `OUTDATED` (bản dịch) | `translation(da).lastModifiedAt > translation(vi).translatedAt` |

Kiểu enum **lưu trong CSDL** phải loại trừ `GUARANTEED` để không ai nhập tay được.

---

## 7b. Cột kiểm toán và xoá mềm

Năm cột chuẩn ở mọi bảng thực thể: `created_at` · `created_by` ·
`last_modified_at` · `last_modified_by` · `soft_delete`. Bảng nào có, bảng nào
cố tình không có: `docs/11` mục 11.2.

| Cột | Ai ghi |
|---|---|
| `created_at`, `last_modified_at` | **CSDL** — `DEFAULT now()` và trigger `tg_*_last_modified` |
| `created_by`, `last_modified_by` | **Ứng dụng** — CSDL không biết ai đang thao tác |
| `soft_delete` | Ứng dụng |

`BaseEntity` ở `infrastructure/shared` mang bốn cột kiểm toán và cờ xoá mềm.
Nó **cố tình không có `@CreatedDate` lẫn `@LastModifiedDate`**: hai cột thời gian
do CSDL sở hữu, hai cột "ai" do `AuditorAware` điền.

**Đừng để ứng dụng đặt `last_modified_at`.** Sẽ có chỗ quên, và cột "sửa lần
cuối" sai còn tệ hơn không có. Nếu dùng JPA auditing thì chỉ bật
`@CreatedBy`/`@LastModifiedBy`, không bật `@LastModifiedDate`.

**Mọi truy vấn đọc phải tự lọc `AND NOT soft_delete`.** Xoá mềm hoạt động âm
thầm — cùng lý do dự án đã từ chối Hibernate `@Filter` (ADR-003). Quên một chỗ là
rò dữ liệu đã xoá ra khách mà không có lỗi nào nổ.

**Xoá mềm không lan xuống dưới.** `ON DELETE CASCADE` chỉ chạy khi xoá cứng. Đặt
`product.soft_delete = TRUE` không đụng gì tới `product_translation` hay
`departure` của nó — use case phải xoá mềm cả chùm trong một transaction.

---

## 8. Đổi API

Spec-first. **Không sửa controller trước.**

```
1. Sửa contracts/openapi.yaml
2. pnpm contracts:generate
3. Controller implements interface mới — quên là lỗi biên dịch
```

Interface sinh ra **không commit**, sinh lúc build — vào
`web/build/generated/openapi`. Hook `.claude/hooks/chan-file-sinh-ra.py` chặn
sửa tay chỗ đó.

Ví dụ đang chạy: `RegionController implements RegionsApi`. Đổi
`contracts/openapi.yaml` rồi chạy `./gradlew build` mà quên sửa controller thì
build đỏ ngay ở bước biên dịch.

---

## 9. Test

| Loại | Công cụ | Chạy ở |
|---|---|---|
| Quy tắc nghiệp vụ | JUnit 5 thuần | `domain` — nhanh, không context |
| Ranh giới module | ArchUnit | `./gradlew archTest` |
| Truy cập dữ liệu | Testcontainers + Postgres thật | `infrastructure` |
| Controller | `@WebMvcTest` | `web` |

Engine giá và giải trạng thái **phải có test ở `domain`**, không phải test qua
controller. Bản demo có 24 test cho riêng engine giá — giữ mức đó.

---

## 10. Quy ước đặt tên

| Chỗ | Quy ước | Ví dụ |
|---|---|---|
| Bảng | `snake_case`, số ít | `tour`, `tour_translation` |
| Lớp | `PascalCase` | `TourTranslation` |
| Endpoint | `kebab-case`, số nhiều | `/api/v1/dk/tours` |
| Enum | `SCREAMING_SNAKE` | `SOLD_OUT` |
| Market trong code | Chữ hoa | `DK` |
| Market trong URL | Chữ thường | `/dk/` |

Chú thích viết **tiếng Việt**. Bảng thuật ngữ đầy đủ: `docs/03`.

---

## 11. Cấm

- `double` / `float` cho tiền
- `@Filter` của Hibernate cho locale
- Trả câu tiếng người từ API
- Đọc `LocalDate.now()` trong `domain`
- H2 trong test
- Sửa code sinh từ `openapi.yaml`
- Cột `exchange_rate` — không tồn tại trong hệ thống này
- Ứng dụng tự đặt `last_modified_at` — đó là việc của trigger
- Truy vấn đọc thiếu `AND NOT soft_delete`
- `CONSTRAINT ... UNIQUE` trên bảng có `soft_delete` — phải là index bộ phận
