# Sổ trạng thái

```
Trạng thái: Đã duyệt
Cập nhật: 31/08/2026
Phiên bản: 1.0
Chủ sở hữu: Chủ sản phẩm
Người duyệt: Chủ sản phẩm
Nguồn sự thật về: đang ở giai đoạn nào, việc đang làm, quyết định đang chờ,
                  nhật ký cổng, chỗ dễ quên khi làm tiếp.
Không nói về: định nghĩa giai đoạn và tiêu chí ra (40),
              quy trình tài liệu (42), bản đồ tài liệu (00).
```

> **File sống.** Cập nhật ở cuối mỗi phiên làm việc, không đợi hết giai đoạn.
> Đọc file này **trước** khi bắt đầu bất cứ việc gì.

---

## 1. Đang ở đâu

| | |
|---|---|
| **Giai đoạn** | **G3 — Lõi danh mục** |
| **Cổng gần nhất đã qua** | G2 — 01/09/2026, **qua có điều kiện** (3 điều kiện treo, mục 7) |
| **Việc chặn G3 qua cổng** | Nội dung thật của 3 tour đủ hai ngôn ngữ — chặn ở **Q-1**; và `20`, `21`, `05` còn ở `Nháp` |
| **Tài liệu** | 19/25 file. **Xong tầng 0, 1, 2, 4** — còn lại là tầng 3 (`30`–`35`) |

---

## 2. Đã có gì

### Tài liệu — 14 file

| Tầng | File | Trạng thái |
|---|---|---|
| 0 | `00-ke-hoach-tai-lieu` | Đã duyệt |
| 0 | `01-dac-ta-san-pham` | Nháp |
| 0 | `02-thi-truong-va-da-ngon-ngu` | Nháp — **cần nâng lên Đã duyệt, xem mục 4** |
| 0 | `03-tu-vung-nghiep-vu` | Nháp |
| 0 | `04-phan-loai-san-pham` | Nháp — **cần nâng lên Đã duyệt** |
| 0 | `05-trang-chi-tiet-san-pham` | Nháp |
| 1 | `10-kien-truc-he-thong` | Nháp |
| 1 | `11-mo-hinh-du-lieu` | Nháp |
| 1 | `12-luoc-do-csdl` | Nháp |
| 1 | `13-hop-dong-api` | Nháp |
| 1 | `14-quy-tac-nghiep-vu` | Nháp |
| 4 | `40-ke-hoach-thuc-hien` | Đã duyệt |
| 4 | `41-tinh-trang` | Đã duyệt — file này |
| 4 | `42-quy-trinh-tai-lieu` | Đã duyệt |

ADR-001 … ADR-006: đã chốt. ADR-007 (cổng thanh toán), ADR-008 (lưu ảnh),
ADR-009 (phạm vi COMBO): chưa viết.

Ba file `CLAUDE.md`: gốc repo, `api/`, `web/`.

### Ngoài tài liệu

| Thứ | Ở đâu | Ghi chú |
|---|---|---|
| Bản Word gửi khách | `docs/Dac-ta-chuc-nang-nghiep-vu.docx` | 12 chương, sinh lại bằng `docs/tools/build-docx.py` |
| Quy trình cho Claude Code | `.claude/` | Skill, lệnh, hook, agent |
| Bộ kiểm tài liệu | `scripts/docs_check.py` | Chạy được ngay, không cần cài gì |
| Scaffolding backend | `api/` | Bốn module Gradle, migration `V1`, một endpoint đọc sinh từ spec |
| Hợp đồng API | `contracts/openapi.yaml` | v0.1 — một endpoint, đủ để chứng minh spec-first |
| Postgres cho dev | `compose.yaml` | Postgres 16, có ICU và contrib |
| Scaffolding frontend | `web/` | pnpm workspace, 2 app Next.js, 3 package dùng chung |
| CI | `.github/workflows/` | `api.yml`, `web.yml`, `tai-lieu.yml` — lọc theo đường dẫn |
| Lõi danh mục | `contracts/openapi.yaml` v0.2 · `api/` · `web/` | Listing + trang chi tiết, hai đầu sinh từ cùng một spec |
| Tài liệu tầng 2 | `docs/20` … `docs/24` | Đủ 5 file, tất cả ở `Nháp` — `20`, `21`, `24` phải `Đã duyệt` trước cổng G3; `22`, `23` trước G4 |
| Migration `V2` | `12` mục 3.1, 4.6, 4.7, 6.1 · `V2__vai_tro_anh_hanh_khach_slug.sql` | 7 bảng: vai trò, ảnh kèm giấy phép, slug cũ, hành khách. 13 test |

---

## 3. Chưa có gì

### Tài liệu còn thiếu — 6 file, toàn bộ là tầng 3

`30-thanh-toan` · `31-bao-mat-va-du-lieu-ca-nhan` ·
`32-phap-ly-nganh-du-lich` · `33-testing` · `34-cicd-va-moi-truong` ·
`35-van-hanh`

Thêm ba ADR đã được trỏ tới mà chưa viết: ADR-007, ADR-008 (Q-6), ADR-009 (Q-7).

Cộng `docs/tham-chieu/phan-tich-website.md` — chép nguyên từ demo, chưa chép.

### Code

| Phần | Trạng thái |
|---|---|
| `api/` bốn module Gradle, `archTest` xanh 6/6 | ✔ |
| `api/` migration `V1__khoi_tao.sql` — toàn bộ lược đồ `12` | ✔ **đã chạy trên Postgres 16 thật** |
| `api/` dữ liệu tra cứu `R__` và `scripts/seed-dev.sql` | ✔ đã chạy sạch |
| Cột kiểm toán + xoá mềm: 18 bảng đủ 5 cột, 3 bảng 4 cột, 21 trigger, 15 index duy nhất bộ phận | ✔ |
| **Chưa commit lần nào** — toàn bộ `api/`, `web/`, `docs/`, `.claude/`, `.github/` còn là file chưa theo dõi | ✗ |
| Test: 6 ArchUnit + 5 domain + 43 tích hợp Testcontainers | ✔ 54/54 xanh |
| `contracts/openapi.yaml` v0.2 → interface Java | ✔ sinh và biên dịch sạch |
| `contracts/openapi.yaml` → TS client | ✔ sinh và biên dịch sạch |
| `web/` pnpm workspace: `site`, `admin`, `i18n`, `ui`, `api-client` | ✔ |
| `pnpm typecheck` · `lint` · `test` · `i18n:check` · `build` | ✔ tất cả xanh |
| Chạy thật đầu-cuối: API + site, hai locale, hai market | ✔ |
| `.github/workflows/`: `api.yml`, `web.yml`, `tai-lieu.yml` | ✔ đã dựng |
| CI chạy trên một PR thật | ✗ **chưa đẩy lên GitHub lần nào** |
| API đọc: `GET /{market}/products` và `/products/{slug}` | ✔ 20 test tích hợp xanh |
| Site khách: trang danh sách + trang chi tiết, ba trạng thái, bộ lọc trong URL | ✔ chạy thật đầu-cuối |
| Bộ lọc theo chủ đề (`?theme=`) | ✗ **`12` chưa có bảng `product_theme`** — đã ghi vào `13` mục 12 |
| Sắp theo ngày khởi hành gần nhất | ✗ cần join `departure`, để G4 |
| Trang tìm kiếm riêng, điểm đến, bài viết, sự kiện | ✗ G3 còn tiếp |
| Bốn nhóm bảng thiếu: vai trò, bộ ảnh kèm giấy phép, slug cũ, hành khách | ✔ `V2` — chạy thật trên Postgres 16, 13 test |
| Đọc và ghi bốn nhóm bảng đó qua API | ✗ lược đồ đã có, chưa có endpoint nào chạm tới |
| `product.hero_image` và `map_image` trỏ tới `media_asset` | ✗ đổi phá vỡ tương thích, phải tách hai lần triển khai — `12` mục 10 |
| Chuyển hướng 301 đọc `slug_history` ở tầng web | ✗ bảng đã có dữ liệu, `20` chưa dùng |

---

## 4. Quyết định đang chờ

Số ngày treo tính từ 31/08/2026.

| # | Câu hỏi | Chặn | Ai trả lời | Treo từ |
|---|---|---|---|---|
| Q-1 | **Ai viết nội dung tour tiếng Đan Mạch?** | **G3** | Chủ sản phẩm | 31/08 |
| Q-2 | Sáu con số nghiệp vụ thị trường `VN`: tỷ lệ đặt cọc, phí xử lý, có giảm đặt sớm không, làm tròn tới nghìn đồng không, hạn giữ chỗ, bậc huỷ và tỷ lệ hoàn | **G4** | Chủ sản phẩm | 31/08 |
| Q-3 | GĐ-6 — v1 có thanh toán online thật, hay nhận đặt chỗ rồi gọi điện chốt? | G5 | Chủ sản phẩm | 31/08 |
| Q-4 | Quy mô dự kiến: bao nhiêu tour, bao nhiêu đơn mỗi tháng? | G2 | Chủ sản phẩm | 31/08 |
| Q-5 | Chạy một hay nhiều instance? | G2 | Kiến trúc sư | 31/08 |
| Q-6 | Lưu ảnh ở đâu: VPS, S3, hay CDN? → ADR-008 | G3 | Kiến trúc sư | 31/08 |
| Q-7 | `COMBO` v1 làm bó cố định do nhân viên soạn, hay tồn kho thời gian thực? → ADR-009 | G4 | Chủ sản phẩm | 31/08 |
| Q-8 | Nâng `02` và `04` lên `Đã duyệt` — hai file này quyết định lược đồ CSDL, G0 đã qua mà chúng vẫn ở `Nháp` | **G2** | Chủ sản phẩm + Kiến trúc sư | 31/08 |

> **Q-1 là câu hỏi nguy hiểm nhất** vì nó không có vẻ kỹ thuật nên dễ bị đẩy lùi.
> Nội dung tiếng Đan là **ngôn ngữ nguồn** (ADR-004): không có nó thì không có gì
> để dịch, không có gì để hiển thị, và toàn bộ G3 xong xuôi vẫn không ra mắt được.

---

## 5. Việc kế tiếp — theo thứ tự

Ba điều kiện treo của cổng G2 (mục 7) đi trước, rồi tới phần còn lại của G3.

1. **Đẩy lên GitHub và mở một PR thật** — điều kiện treo số 1 của G2. Cần thấy:
   sửa `web/` **không** kích hoạt build Gradle, sửa `contracts/` kích hoạt **cả
   hai**, sửa `docs/` chỉ kích hoạt `tai-lieu.yml`
2. **Viết `34-cicd-va-moi-truong` bản nháp** — điều kiện treo số 2
3. **Sửa câu chữ tiêu chí 5 của `40`** — điều kiện treo số 3
4. **Nội dung thật cho G3**: ít nhất 3 tour đủ hai ngôn ngữ. Đang chặn ở **Q-1**;
   không có người viết bản tiếng Đan thì hệ thống xong mà không có gì để bán
5. Trả lời Q-8, và **duyệt `20`, `21`, `05`** — cổng G3 đòi cả ba ở `Đã duyệt`
6. Phần G3 còn thiếu ở tầng code: trang điểm đến, trang tìm kiếm, sitemap theo
   locale, chuyển hướng 301 đọc `slug_history`

---

## 6. Chỗ dễ quên khi làm tiếp

Mục có giá trị nhất của file này. Đây là những thứ đã tốn thời gian một lần rồi.

- **`unaccent()` là `STABLE`, không phải `IMMUTABLE`** — không index trực tiếp
  được. Phải bọc bằng hàm `f_unaccent` khai báo `IMMUTABLE`. `12` có sẵn, nhưng
  dễ bỏ sót khi viết migration
- **`departure` gấp đôi số dòng** khi một sản phẩm bán ở cả hai thị trường
  (ADR-006). Trang quản trị cần chức năng nhân bản lịch giữa hai thị trường,
  nếu không nhân viên nhập tay hai lần và sẽ lệch
- **`INNER JOIN` chính là chính sách không-fallback.** Đừng thêm `if` ở tầng ứng
  dụng để "xử lý trường hợp thiếu bản dịch" — thêm là hỏng chính sách
- **Không dùng Hibernate `@Filter` cho locale.** Filter hoạt động âm thầm; quên
  bật ở một service là rò rỉ nội dung sai ngôn ngữ mà không có lỗi nào
- **`domain` không được đọc đồng hồ hệ thống.** Hàm cần "hôm nay" thì nhận
  `LocalDate` làm tham số — nếu không, test giảm giá đặt sớm sẽ đỏ vào một ngày
  nào đó trong tương lai mà không ai hiểu vì sao
- **Testcontainers, không H2.** H2 không mô phỏng được ICU collation và
  `unaccent` — đúng hai thứ dễ sai nhất của dự án này
- **Font phải thử cả hai chuỗi cùng lúc**: `Strand og øer på tværs` và
  `Điểm cuối · Hội An · Vịnh Hạ Long`. Demo đã dính bẫy này một lần với Georgia,
  chữ "Điểm cuối" hiện thành "Điểm cuố i"
- **`amount` là chuỗi trong JSON**, không phải số. Và VND có 0 chữ số thập phân,
  DKK có 2 — scale lấy từ cấu hình market

Bốn cái vấp khi dựng scaffolding `api/`, đều đã mất thời gian một lần:

- **Dữ liệu mồi phải chạy trong MỘT transaction.** Trigger
  `ct_product_source_translation` là `DEFERRABLE INITIALLY DEFERRED`, nên
  `product` và bản dịch nguồn của nó phải nằm cùng transaction. Chạy từng câu ở
  chế độ autocommit là đỏ ngay ở `INSERT INTO product` đầu tiên
- **Cấu hình trong `subprojects {}` không áp cho project gốc.** `archTest` nằm ở
  gốc; thiếu `useJUnitPlatform()` riêng cho nó thì Gradle dùng JUnit 4 và **lặng
  lẽ báo xanh với 0 test** — tệ hơn báo đỏ
- **Phải khai `junit-platform-launcher` tường minh.** Không có thì Gradle tự chèn
  bản khác với engine do BOM của Spring Boot quản, và JUnit chết lúc dò test với
  thông báo `OutputDirectoryCreator not available`
- **`libs` không truy cập được bên trong `subprojects {}`.** Bắt giá trị ra biến
  ở đầu build script gốc rồi mới dùng

Năm cái nữa, lộ ra ở lần đầu `V1` gặp Postgres thật — bốn cái đầu **không** phải
lỗi lược đồ, và đó chính là lý do phải chạy thật sớm:

- **`collation` là từ khoá dành riêng của Postgres.** Cột tên `collation` làm
  `CREATE TABLE` đỏ với `syntax error at or near "collation"`. Đã đổi thành
  `collation_name`
- **Spring Boot 4 tách autoconfiguration của Flyway ra module riêng.** Chỉ có
  `flyway-core` trên classpath thì migration **không chạy và cũng không báo gì** —
  triệu chứng là "relation does not exist", rất dễ đổ oan cho DDL. Cần
  `org.springframework.boot:spring-boot-flyway`
- **Máy đặt múi giờ `Asia/Saigon`** thì driver JDBC gửi đúng chuỗi đó sang
  Postgres, và Postgres chỉ biết `Asia/Ho_Chi_Minh` — kết nối bị từ chối ngay.
  Test đã ép `-Duser.timezone=UTC`
- **Một hàm trigger dùng cho hai bảng phải rẽ nhánh theo `TG_TABLE_NAME`.**
  `COALESCE(NEW.id, OLD.product_id)` chết với `record "old" has no field
  "product_id"` vì hai bảng có cấu trúc khác nhau
- **`ON CONFLICT` với index duy nhất bộ phận phải nhắc lại điều kiện của index**:
  `ON CONFLICT (code) WHERE NOT soft_delete DO NOTHING`. Thiếu mệnh đề `WHERE` thì
  Postgres không nhận ra index nào làm trọng tài

Tám cái từ lần dựng `web/`:

- **Múi giờ phải đặt ở ứng dụng, không chỉ ở test.** Vá `-Duser.timezone=UTC`
  cho Gradle test là chưa đủ — `bootRun` vẫn chết. Nay `ApiApplication` đặt
  `TimeZone.setDefault(UTC)` trong khối `static`, đúng cho cả jar lẫn container
- **Chạy DDL bằng tay rồi mới bật ứng dụng thì Flyway từ chối khởi động**:
  *"Found non-empty schema but no schema history table"*. Để Flyway dựng lược đồ,
  đừng chạy `psql -f V1.sql` trước
- **Next 16 đổi quy ước `middleware` thành `proxy`** — file `proxy.ts`, hàm tên
  `proxy`. Dùng tên cũ vẫn chạy nhưng in cảnh báo mỗi lần build
- **`eslint-config-next` 16 xuất thẳng mảng flat config.** Bọc qua `FlatCompat`
  làm ESLint chết với lỗi tham chiếu vòng, không phải lỗi cấu hình rõ ràng
- **Package TypeScript nguồn dùng chung không được import kèm đuôi `.js`.**
  `from './pathnames.js'` biên dịch được nhưng bundler của Next không giải được
  — chỉ lộ ra lúc `pnpm build`, không lộ ra lúc `pnpm typecheck`
- **Code sinh ra không chịu được `strict` đầy đủ.** Sửa nó là sửa file sinh ra,
  điều bị cấm; nên `api-client` build ra `dist/` với `.d.ts` và các package khác
  kiểm kiểu dựa vào `.d.ts` (được `skipLibCheck` bỏ qua) chứ không đọc mã nguồn
- **`spawnSync` trên Windows không phân giải tên chương trình theo `cwd`.**
  `spawnSync('gradlew.bat', [...], { cwd: 'api', shell: true })` báo *"'gradlew.bat'
  is not recognized"* vì cmd.exe tìm theo thư mục của tiến trình **cha**. Cách
  chắc ăn: một chuỗi lệnh duy nhất chứa **đường dẫn tuyệt đối** — cũng tránh
  luôn cảnh báo DEP0190 của Node
- **`JAVA_HOME` của máy này trỏ `jdk-11`**, nên Gradle chết ở bước cấu hình:
  *"Dependency requires at least JVM runtime version 17"*. Đã đặt
  `org.gradle.java.home=C:/Program Files/Java/jdk-21` trong
  `~/.gradle/gradle.properties` — **không** đặt trong repo, vì đó là đường dẫn
  của máy cá nhân (`api/gradle.properties` đã ghi rõ điều này)

Ba cái từ lần thêm `V2`:

- **Bảng nối có `soft_delete` thì khoá chính không được là khoá kép tự nhiên.**
  `staff_user_role` với khoá `(staff_user_id, role_code)` làm việc **cấp lại một
  vai trò đã thu** trở thành bất khả thi — dòng cũ vẫn chiếm khoá. Khoá thay thế
  cộng index bộ phận `WHERE NOT soft_delete` mới đúng
- **Trigger lưu slug cũ phải dọn dòng nghịch đảo.** Đổi A → B → A mà không xoá
  dòng cũ thì sinh vòng lặp chuyển hướng 301
- **`AFTER UPDATE OF slug`** chứ không phải `AFTER UPDATE`: sửa tiêu đề không
  được sinh dòng lịch sử slug

Hai cái từ lần dựng CI:

- **GitHub Actions không hỗ trợ neo YAML** (`&loc` / `*loc`). Danh sách đường dẫn
  phải viết lặp ở cả `push` lẫn `pull_request` — sửa một chỗ phải nhớ sửa chỗ kia
- **Pipeline `web` vẫn cần JDK.** `openapi-generator-cli` là công cụ Java: thiếu
  JDK thì bước sinh TS client chết, dù đây là pipeline frontend

Năm cái từ lần làm lõi danh mục:

- **Trường phân loại (`discriminator`) phải là `string` trần trong spec.** Trỏ
  `$ref` tới enum thì generator sinh interface cha trả `String` còn lớp con trả
  enum — 12 lỗi biên dịch Java, và không sửa được vì đó là code sinh ra
- **Enum của spec có giá trị khác tên hằng thì Spring không tự đọc được.**
  `ProductSort` mang giá trị `title,asc`; thiếu bean `Converter` thì **mọi** yêu
  cầu trả 400 `VALIDATION_FAILED` — kể cả yêu cầu không truyền `sort`, vì spec có
  giá trị mặc định. Triệu chứng đánh lừa hoàn toàn: tham số nhìn thì đúng hết
- **Ràng buộc `@Min`/`@Max` trên tham số controller nổ bằng
  `ConstraintViolationException`**, không phải `HandlerMethodValidationException`.
  Thiếu nó trong `@ExceptionHandler` thì `?size=999` trả **500** thay vì 400
- **`RestClient.uri(String)` mã hoá phần trăm lần thứ hai.** Nối tay
  `"?q=hoi%20an"` thì máy chủ nhận đúng chuỗi `hoi%20an` — tìm kiếm ra 0 kết quả
  mà nhìn URL thì thấy đúng. Dùng `uri(builder -> …)` với `queryParam`
- **Hai constraint trigger của `product` là `DEFERRABLE INITIALLY DEFERRED`**, nên
  sản phẩm và bản dịch nguồn phải nằm trong **cùng một transaction**. Mỗi
  `jdbc.update` là một transaction riêng: dữ liệu thử phải chạy bằng một câu
  `jdbc.execute` nhiều lệnh, không phải nhiều câu `update`

---

## 7. Nhật ký cổng

| Ngày | Cổng | Kết quả | Điều kiện treo |
|---|---|---|---|
| 31/08/2026 | G0 | Qua | `02` và `04` còn ở `Nháp` → Q-8 |
| 31/08/2026 | G1 | Qua | DDL chưa chạy qua Postgres lần nào → **đã xử lý**: chạy thật ngày 31/08, phát hiện và sửa hai lỗi trong `12` |
| 01/09/2026 | G2 | **Qua có điều kiện** — 6/7 tiêu chí đạt | Ba điều kiện treo, xem biên bản ngay dưới |

### Biên bản cổng G2 — 01/09/2026

Tiêu chí lấy nguyên văn từ `40` mục 4. Mỗi dòng là một lệnh đã chạy thật.

| # | Tiêu chí | Bằng chứng | Kết luận |
|---|---|---|---|
| 1 | `docker compose up -d` cho Postgres có sẵn `unaccent` và `pg_trgm` | Container `travel-postgres` chạy; `SELECT extname FROM pg_extension` → `pg_trgm`, `plpgsql`, `unaccent` | **ĐẠT** |
| 2 | `./gradlew build` xanh, `archTest` xác nhận `domain` không dính Spring | `BUILD SUCCESSFUL`; `archTest --rerun-tasks` → 6/6 PASSED, gồm `domain_khong_biet_spring_ton_tai` | **ĐẠT** |
| 3 | `pnpm build` và `pnpm typecheck` xanh cả `site` lẫn `admin` | `typecheck`: 5/5 package Done; `build`: `apps/site` và `apps/admin` Done | **ĐẠT** |
| 4 | `pnpm contracts:generate` sinh **cả** interface Java **và** TS client; controller `implements` interface đó, biên dịch sạch | Sinh cả hai phía; `compileJava --rerun-tasks` → `BUILD SUCCESSFUL`; `RegionController implements RegionsApi`, `ProductController implements ProductsApi` | **ĐẠT** |
| 5 | Migration dựng xong lược đồ của `12`, dữ liệu mồi nạp được | CSDL **trắng** `travel_cong`: `V1` + `V2` + `R__` đều `success = t`, 38 bảng; `seed-dev.sql` nạp sạch → 3 sản phẩm, 5 bản dịch, 9 ngày khởi hành, 4 vai trò; API đọc ra đúng 3 sản phẩm | **ĐẠT** — xem ghi chú dưới bảng |
| 6 | `python scripts/docs_check.py` → 0 lỗi | `28 file · 0 lỗi · 13 cảnh báo` (cảnh báo đều là ADR-007/008/009 chưa viết) | **ĐẠT** |
| 7 | CI chạy trên một PR thật: sửa `web/` **không** kích hoạt build Gradle | `git log` chỉ có `8f2ec0c Initial commit`; `git ls-files .github/` rỗng — ba workflow chưa từng được đẩy lên, chưa PR nào chạy. Bộ lọc đường dẫn mới chỉ kiểm bằng mô phỏng trên máy | **CHƯA ĐẠT** |

**Tài liệu phải `Đã duyệt`:** `40` ✔ `Đã duyệt` v1.0 · `42` ✔ `Đã duyệt` v1.0.

**Ghi chú tiêu chí 5.** `40` viết lệnh là `./gradlew flywayMigrate`, nhưng dự án
không áp plugin Flyway của Gradle: migration chạy lúc **ứng dụng khởi động**, qua
`spring-boot-flyway` (`api/CLAUDE.md` mục 0). Nội dung tiêu chí — dựng xong lược
đồ trên CSDL sạch và nạp được dữ liệu mồi — đã kiểm đầy đủ bằng cơ chế thật, trên
một cơ sở dữ liệu mới tạo rồi xoá đi. Câu chữ của `40` cần sửa cho khớp; đó là
điều kiện treo số 3.

**Ba điều kiện treo**

| # | Điều kiện | Ai chịu trách nhiệm | Hạn |
|---|---|---|---|
| 1 | Đẩy repo lên GitHub, mở một PR thật, xác nhận bộ lọc đường dẫn trên runner | KTS (đẩy) + VH (`A` của dòng CI/CD ở `40` mục 5) | Trước khi chạy cổng G3 |
| 2 | Viết `34-cicd-va-moi-truong` bản nháp — nằm trong danh sách **Bàn giao** của G2 mà chưa có file | VH | Trước khi chạy cổng G3 |
| 3 | Sửa câu chữ tiêu chí 5 của `40` cho khớp cơ chế thật (`40` mục 7 dòng 1: làm rõ câu chữ, không cần PTĐ) | KTS | Trước khi chạy cổng G3 |

**Chưa ai ký.** `40` mục 5: cổng G2 cần **CSH** (bắt buộc, mọi cổng) và **KTS**.
Biên bản này chỉ chuẩn bị bằng chứng, không thay chữ ký.

---

## 8. Nhật ký phiếu thay đổi

Chưa có phiếu nào. Mẫu ở `40` mục 7.

---

## 9. Nhật ký phiên làm việc

Ghi ngắn: làm gì, để lại gì dở dang.

| Ngày | Làm gì | Để lại gì |
|---|---|---|
| 31/08/2026 | Đợt tài liệu 1 và 2 — 20 file | Toàn bộ ở `Nháp` trừ `00` |
| 31/08/2026 | Bản Word gửi khách, 12 chương | Ba chỗ placeholder chưa điền tên công ty |
| 31/08/2026 | `40`, `41`, `42`, `.claude/`, `scripts/docs_check.py` | Scaffolding `api/` và `web/` vẫn chưa dựng |
| 31/08/2026 | Scaffolding `api/`: 4 module, `V1`, `openapi.yaml` v0.1, `compose.yaml` | `:web:test` chưa chạy — máy chưa bật được Docker. `web/` và CI chưa có |
| 31/08/2026 | Cột kiểm toán + xoá mềm vào `11`, `12`, `V1`, seed, adapter, test | `web/` và CI vẫn chưa có |
| 31/08/2026 | Chạy `V1` + `R__` + `seed-dev` trên Postgres 16 thật, sửa 2 lỗi DDL của `12` | 22/22 test xanh |
| 31/08/2026 | Scaffolding `web/`: workspace, 2 app, 3 package, TS client, bộ kiểm i18n và tương thích hợp đồng | Chỉ còn CI là chặn cổng G2 |
| 01/09/2026 | Sửa `contracts-generate.mjs` (đường dẫn tuyệt đối cho `gradlew.bat`), đặt `org.gradle.java.home` = JDK 21; dựng `.github/workflows/` ba file | Bộ lọc đường dẫn mới kiểm bằng mô phỏng — chưa chạy PR thật |
| 01/09/2026 | Lõi danh mục: spec v0.2, listing + chi tiết ở cả hai phía, 20 test tích hợp, trang khách hai locale | Chưa có bảng `product_theme`; `20` và `21` chưa viết |
| 01/09/2026 | Viết `20-frontend-web` và `21-he-thong-thiet-ke` | Cả hai ở `Nháp`; bảng màu và font thương hiệu còn để ngỏ |
| 01/09/2026 | Viết `22-trang-quan-tri` và `23-luong-dat-tour` | Phát hiện `12` thiếu hai bảng: `role`/`staff_user_role` và `booking_passenger` |
| 01/09/2026 | Viết `24-noi-dung-va-anh` — xong tầng 2 | Phát hiện `12` thiếu bảng bộ ảnh và chỗ lưu slug cũ; Q-1 vẫn chặn toàn bộ nội dung |
| 01/09/2026 | Bổ sung `12` mục 3.1, 4.6, 4.7, 6.1 và viết migration `V2` — 7 bảng, 1 trigger lưu slug cũ, 3 quy tắc kiểm mới | Chưa có endpoint nào dùng bốn nhóm bảng này |
