# Kế hoạch triển khai tài liệu

```
Trạng thái: Đã duyệt
Cập nhật: 31/08/2026
Phiên bản: 1.1
Chủ sở hữu: Chủ sản phẩm
Người duyệt: Chủ sản phẩm + Kiến trúc sư
Nguồn sự thật về: bản đồ bộ tài liệu và thứ tự viết.
Không nói về: nội dung của từng tài liệu con; vòng đời và quy ước tài liệu (42);
              giai đoạn và cổng nghiệm thu (40); trạng thái hiện tại (41).
```

Dự án: **website bán tour du lịch Việt Nam**, hai thị trường, hai ngôn ngữ, gồm
frontend cho khách, backend Java Spring Boot và trang quản trị.

Soạn ngày **31/08/2026**. Tham chiếu: demo frontend tại `D:\CODE\travel`
(Vite + React, 172 file `src/`, 13 file mock data, 7 tài liệu).

> **Tiến độ: đợt 1 và đợt 2 đã xong** — 19 file (13 + 6), chưa kể chính file này.
> Còn đợt 3 (5 file), đợt 4 (6 file) và tầng 4 (`40`, `41`, tham chiếu). Xem mục 7.

> Tài liệu này là **bản đồ của bộ tài liệu**, không phải đặc tả sản phẩm. Nó
> trả lời: cần viết những tài liệu nào, mỗi tài liệu chịu trách nhiệm về điều
> gì, viết theo thứ tự nào, và làm sao để chúng không lỗi thời.

---

## 1. Kết luận sau khi đọc demo

Demo đã giải quyết xong tầng giao diện và mô hình nội dung tour. Những gì kế
thừa được, và những gì phải viết mới:

| Vùng | Demo có sẵn | Trạng thái với dự án mới |
|---|---|---|
| Mô hình miền → điểm đến → tour → ngày khởi hành | Đầy đủ, đã chạy thật | **Kế thừa**, bổ sung tầng dịch |
| Quy tắc nghiệp vụ giá, trạng thái khởi hành | `lib/pricing.ts`, `lib/status.ts`, có test | **Kế thừa**, bổ sung tồn kho thật |
| Bản đồ trang, 7 tab tour, luồng đặt tour 4 bước | Đã dựng và đã kiểm bằng mắt | **Kế thừa** làm đặc tả UI |
| Khảo sát website mẫu (`phan-tich-website.md`) | 286 dòng, đối chiếu từng khối | **Kế thừa nguyên trạng** |
| Quy ước làm việc (`CLAUDE.md`) | 370 dòng | **Viết lại** — demo chỉ có frontend |
| CSDL, hợp đồng API, phân quyền | Không có | **Viết mới hoàn toàn** |
| Tồn kho chỗ, giữ chỗ, hết hạn giữ | Không có (`seatsBooked` là số tĩnh) | **Viết mới hoàn toàn** |
| Thanh toán, hoàn/huỷ, đối soát | Cố tình giả lập | **Viết mới hoàn toàn** |
| Đa ngôn ngữ | Không có — mỗi chuỗi nằm thẳng trên thực thể | **Viết mới, và định hình mọi thứ khác** |

### 1.1. Ba phát hiện định hình cả bộ tài liệu

**a) Đa ngôn ngữ không phải là dịch chuỗi — nó là hai thị trường.**

Demo đặt `title`, `shortDescription`, `longDescription[]` thẳng trên `Tour`.
Với hai ngôn ngữ trở lên, mọi chuỗi khách nhìn thấy phải rời khỏi thực thể sang
tầng dịch. Nhưng vấn đề lớn hơn nằm ở chỗ khác: **cùng một hành trình bán cho
khách Đan Mạch và khách Việt không phải cùng một sản phẩm.**

| | Khách Đan Mạch | Khách Việt |
|---|---|---|
| Vé máy bay quốc tế | Nằm trong giá | Không có |
| Trưởng đoàn | Nói tiếng Đan Mạch, bay cùng đoàn | Hướng dẫn viên tiếng Việt tại chỗ |
| Tiền tệ | DKK, `24.990 kr.` | VND, `18.900.000 ₫` |
| Đặt cọc | 25% theo thông lệ Bắc Âu | Thông lệ khác, cần khảo sát |
| Ràng buộc pháp lý | Rejsegarantifonden, chỉ thị EU về gói du lịch | Luật Du lịch 2017, ký quỹ lữ hành |
| Chứng từ | Hoá đơn EU, VAT | Hoá đơn điện tử theo NĐ 123/2020 |

Nếu mô hình dữ liệu chỉ có "bảng dịch", giá sẽ phải quy đổi tỷ giá — và ra con
số sai. Đề xuất trong tài liệu `02`: **thực thể `Market` là cấp một**, mỗi thị
trường có catalog riêng, bảng giá riêng, ngôn ngữ mặc định riêng, quy tắc pháp
lý riêng. Ngôn ngữ chỉ là một thuộc tính của thị trường, không phải trục chính.

**b) Không được fallback ngôn ngữ một cách âm thầm.**

Với nội dung marketing, trang chưa dịch hiện tiêu đề ngôn ngữ khác là mất khách
ngay lập tức. Quy tắc đề xuất: nội dung bán hàng (tour, điểm đến, bài viết)
**thiếu bản dịch thì ẩn khỏi listing và khỏi sitemap của locale đó**, không hiện
bản gốc. Chuỗi giao diện thì ngược lại — fallback được, vì thiếu một nhãn nút
không phá vỡ lòng tin. Hai loại nội dung, hai chính sách. Phải ghi rõ, vì đây là
chỗ lập trình viên mặc định làm ngược.

**c) Demo có `validate-data` làm hàng rào, tài liệu cũng cần hàng rào.**

`docs/tinh-trang.md` của demo gọi `npm run validate-data` là "hàng rào chính" —
nó bắt được lỗi mà `tsc` bỏ qua. Bộ tài liệu 20+ file sẽ lỗi thời trong ba tháng
nếu không có cơ chế tương tự. Xem mục 5.

---

## 2. Giả định

| # | Giả định | Trạng thái |
|---|---|---|
| GĐ-1 | Ngôn ngữ ra mắt `da` + `vi`, **`da` là ngôn ngữ nguồn**, mở sẵn cho `en` | **Đã chốt** — ADR-004 |
| GĐ-2 | Hai thị trường bán hàng thật, không phải một thị trường xem hai thứ tiếng | **Đã chốt** — `02` |
| GĐ-3 | Một repo chứa `api/` (Gradle) và `web/` (pnpm), CI lọc theo đường dẫn | **Đã chốt** — ADR-001 |
| GĐ-4 | Code viết mới; demo là **tham chiếu đặc tả**, không phải nền tảng sửa tiếp | Đang dùng |
| GĐ-5 | Tài liệu viết bằng **tiếng Việt**, kèm đối chiếu thuật ngữ Đan–Việt–code | Đang dùng — `03` |
| GĐ-6 | Có thanh toán online thật ở v1 | **Chưa duyệt** — nhưng không còn xoá được cả `30` nữa: `30` mục 1 cho thấy ba trong năm mục của nó không phụ thuộc câu trả lời. Nay nó chỉ quyết mục 3 và 4 |

Ba quyết định đã chốt thêm: **spec-first** cho hợp đồng API (ADR-002), **bảng dịch
riêng** thay JSONB (ADR-003), và market **không nằm trong URL** mà suy từ locale
rồi ghi đè bằng cookie (`02` mục 5.3).

**Chưa chốt, tài liệu `10` sẽ quyết bằng ADR:** CSDL, cách dựng trang quản trị,
hạ tầng chạy thật.

---

## 2.1. Stack — backend là Java Spring Boot

Backend đã chốt là **Spring Boot**. Điều này thay đổi bốn thứ trong kế hoạch so
với bản đầu, và mỗi thứ đều kéo theo một quyết định phải ghi vào tài liệu.

### a) Repo: một repo, hai toolchain

Gradle và pnpm không sống chung trong một workspace được. Đề xuất:

```
travel-tour-booking/
  api/          Gradle multi-module — Spring Boot
  web/          pnpm workspace — Next.js (khách) + admin
  contracts/    openapi.yaml — nguồn sự thật của hợp đồng API
  docs/         bộ tài liệu, dùng chung cho cả hai
  .github/      hai pipeline code + bộ kiểm tài liệu, lọc theo đường dẫn
```

Giữ một repo vì `docs/` và `contracts/` phải dùng chung; tách repo là lúc hợp
đồng API bắt đầu lệch giữa hai bên mà không ai phát hiện. CI lọc theo đường dẫn
nên sửa frontend không kích hoạt build Gradle.

### b) Không còn chia sẻ type — hợp đồng API lên làm nguồn sự thật

Đây là thay đổi lớn nhất. Với backend TypeScript, frontend import thẳng type từ
backend. Với Java thì không, và **`13-hop-dong-api.md` từ tài liệu tầng 1 bình
thường trở thành trục chính của cả dự án.**

Đề xuất **spec-first**, không phải code-first:

1. `contracts/openapi.yaml` viết tay, review như review code.
2. `openapi-generator` sinh interface Java (Spring) — controller `implements`
   interface đó, nên đổi spec mà quên sửa controller là **lỗi biên dịch**.
3. Cùng spec đó sinh TS client cho `web/`.
4. CI kiểm spec không đổi ngoài ý muốn, và kiểm tương thích ngược.

Cách ngược lại (springdoc sinh spec từ annotation) nhanh hơn lúc đầu nhưng làm
hợp đồng thành hệ quả của code — với dự án dẫn dắt bằng tài liệu thì đó là đi
ngược chiều.

### c) Đa ngôn ngữ trong Spring — ba quy tắc phải ghi rõ

**API trả về mã lỗi, không trả câu tiếng người.** Nếu backend trả
`"Ngày khởi hành đã hết chỗ"` thì bản dịch tồn tại ở hai nơi và sẽ lệch nhau.
Backend trả `DEPARTURE_SOLD_OUT` kèm tham số; frontend dịch. `MessageSource` của
Spring chỉ dùng cho thứ backend thật sự sở hữu: email xác nhận, PDF chương trình,
hoá đơn.

**Bảng dịch riêng, không dùng JSONB.** Mỗi thực thể có nội dung: một bảng
`*_translation` với khoá `(entity_id, locale)`. Lý do chọn bảng thay vì cột
JSONB: trang quản trị cần biết **từng trường** đã dịch hay chưa để hiện bảng
điều khiển dịch thuật, và listing phải `WHERE locale = ?` có index. Truy vấn
dùng join tường minh, không dùng Hibernate `@Filter` — filter là thứ hoạt động
âm thầm, và chỗ này cần nhìn thấy được.

**Market và locale là hai tham số khác nhau.** Xem 1.1.a — thị trường quyết định
catalog, giá và luật; ngôn ngữ chỉ quyết định chữ hiển thị. Một khách Việt ở Đan
Mạch có thể mua ở thị trường DK nhưng đọc tiếng Việt. Đề xuất: market nằm trong
đường dẫn API, locale nằm ở `Accept-Language`. Không gộp làm một.

### d) Bốn cái bẫy của Java/Postgres cho bài toán này

| Bẫy | Xử lý |
|---|---|
| **Tiền tệ** — VND 0 chữ số thập phân, DKK 2 | `BigDecimal` với scale theo từng tiền tệ, **không bao giờ** `double`. Lưu kèm mã tiền tệ. Cân nhắc lưu minor unit dạng `BIGINT` |
| **Sắp xếp chữ** — æ ø å đứng sau z trong tiếng Đan, tiếng Việt lại có thứ tự riêng | Collation ICU theo locale trong Postgres (`da-DK-x-icu`, `vi-VN-x-icu`). Sắp xếp tên tour ở tầng CSDL, đừng sắp ở Java bằng `String.compareTo()` |
| **Tìm kiếm không dấu** — khách gõ "hoi an" phải ra "Hội An" | `unaccent` + `pg_trgm`. Cấu hình theo từng locale |
| **Giữ chỗ khi hai khách bấm cùng lúc** | Khoá bi quan `SELECT ... FOR UPDATE` trên dòng departure trong một transaction ngắn, cộng bảng `seat_hold` có hạn. Job quét hạn dùng `@Scheduled` + **ShedLock** nếu chạy nhiều instance — thiếu ShedLock thì mỗi instance quét một lần |

### e) Đề xuất còn lại

| Vùng | Đề xuất | Ghi chú |
|---|---|---|
| Java | **21 LTS** | 25 LTS cũng được; 21 có hệ sinh thái thư viện chín hơn. Kiểm phiên bản Spring Boot hiện hành trước khi chốt |
| Build | Gradle Kotlin DSL, multi-module | Tách `domain` / `application` / `web` để quy tắc nghiệp vụ không phụ thuộc Spring và test được bằng JUnit thuần — giống `lib/pricing.ts` của demo là hàm thuần có test |
| CSDL | PostgreSQL 16+ | Cần ICU collation và `unaccent` |
| Migration | **Flyway**, SQL thuần | Chọn Flyway thay Liquibase: file SQL đọc được trong review, hợp với dự án dẫn dắt bằng tài liệu |
| Truy cập dữ liệu | Spring Data JPA cho CRUD, **jOOQ hoặc SQL thuần** cho truy vấn listing đa ngôn ngữ | Truy vấn join bảng dịch kèm lọc và sắp xếp theo locale là chỗ JPA sinh SQL tệ |
| Test | JUnit 5 + **Testcontainers** | Postgres thật, vì collation và `unaccent` không mô phỏng được bằng H2 |
| Frontend | Next.js, App Router | Có sẵn i18n routing và SSG — quan trọng cho SEO hai ngôn ngữ |
| Trang quản trị | React trong `web/`, gọi cùng API | Dùng chung design system với trang khách. Không dùng Thymeleaf — sẽ thành hệ thống giao diện thứ hai phải bảo trì |
| Triển khai | Docker hai giai đoạn → GHCR → VPS | Kế thừa `docs/cicd.md` của demo, thêm bước Flyway migrate |

Đây là **đề xuất**, không phải quyết định. ADR-001 chốt Java/Gradle/Postgres,
ADR-002 chốt spec-first, ADR-003 chốt cách lưu bản dịch.

---

## 3. Bộ tài liệu

25 tài liệu, chia năm tầng. Cột "Nguồn sự thật về" là quan trọng nhất: mỗi sự
thật chỉ được ở **một** chỗ. Demo đã vấp lỗi này — `CLAUDE.md` và
`phan-tich-website.md` mô tả trùng nhau bản đồ URL, và khi đổi đường dẫn ở đợt
V1 thì một trong hai chỗ lạc hậu.

### Tầng 0 — Nền tảng

Không viết xong tầng này thì mọi tài liệu sau đều phải viết lại.

| File | Nguồn sự thật về | Ghi chú |
|---|---|---|
| `CLAUDE.md` ×3 | Quy ước làm việc, lệnh, ranh giới, quy tắc bắt buộc | **Ba file**: gốc repo (chung), `api/CLAUDE.md` (Java, Gradle, JPA), `web/CLAUDE.md` (Next, i18n). Claude Code nạp file theo thư mục đang làm việc — một file 700 dòng lẫn cả Java lẫn React thì cả hai bên đều đọc phần không liên quan |
| `docs/00-ke-hoach-tai-lieu.md` | Bản đồ tài liệu, trạng thái từng file | Chính là file này |
| `docs/01-dac-ta-san-pham.md` | Bán gì cho ai, phạm vi v1, **danh sách không làm** | Mục "không làm" quan trọng ngang mục "làm" |
| `docs/02-thi-truong-va-da-ngon-ngu.md` | Market, locale, tiền tệ, chính sách fallback, URL/slug, hreflang, quy trình dịch | **Tài liệu quan trọng nhất của dự án** |
| `docs/03-tu-vung-nghiep-vu.md` | Thuật ngữ, đối chiếu Việt–Đan–Anh | Team Việt viết code, khách Đan đọc màn hình. Nhầm thuật ngữ ở đây là bug ở kia |
| `docs/04-phan-loai-san-pham.md` | Sáu loại sản phẩm, năm chiều phân biệt, **nghiệp vụ và ràng buộc dữ liệu từng loại**, thẻ sản phẩm, bộ lọc | Khảo sát iVivu, Vietravel, Stjernegaard, Intrepid, Klook. **Quyết định lược đồ CSDL** nên phải xong trước `11` |
| `docs/05-trang-chi-tiet-san-pham.md` | Vào trang chi tiết thấy gì: tab nào có theo loại, dòng phụ, nút chính, thứ tự khối, trạng thái rìa | Ranh giới với `20`: ở đây là **có khối gì cho loại nào**, `20` là route và breakpoint |

### Tầng 1 — Kiến trúc và dữ liệu

| File | Nguồn sự thật về |
|---|---|
| `docs/10-kien-truc-he-thong.md` | Sơ đồ hệ thống, ranh giới service, bố cục monorepo, danh mục ADR |
| `docs/11-mo-hinh-du-lieu.md` | ERD, tầng dịch, phiên bản nội dung, vòng đời bản ghi |
| `docs/12-luoc-do-csdl.md` | Bảng, chỉ mục, ràng buộc, migration, dữ liệu mồi |
| `docs/13-hop-dong-api.md` | **Trục chính.** Quy trình spec-first, mã lỗi, phân trang, xác thực, cách truyền market và locale |
| `docs/14-quy-tac-nghiep-vu.md` | Engine giá, trạng thái khởi hành, **tồn kho và giữ chỗ** |
| `docs/15-ke-hoach-dung-be.md` | Thứ tự dựng backend theo đợt; quy ước mượn từ dự án trước và lý do nhận hay loại |

`13` quan trọng hơn hẳn so với dự án thuần TypeScript: hai bên không chia sẻ
type được nữa, nên `contracts/openapi.yaml` là chỗ duy nhất hai bên gặp nhau.
Xem 2.1.b.

`14` kế thừa trực tiếp `lib/pricing.ts` và `lib/status.ts` của demo — thứ tự
cộng dồn tám bước và quy tắc `GUARANTEED` là giá trị tính chứ không lưu vẫn
đúng nguyên. Chuyển sang Java thì hai điều phải ghi thêm: engine giá nằm ở
module `domain`, **không phụ thuộc Spring**, test bằng JUnit thuần đúng như demo
test `pricing.ts`; và mọi phép tính tiền dùng `BigDecimal`, không `double`.
Phần viết mới hoàn toàn là tồn kho: demo không có khái niệm giữ chỗ tạm, hết hạn
giữ, đặt trùng chỗ cuối cùng, hay huỷ trả chỗ về kho.

### Tầng 2 — Ứng dụng

| File | Nguồn sự thật về |
|---|---|
| `docs/20-frontend-web.md` | Bản đồ route theo từng locale, ba trạng thái màn hình, chiến lược render và SEO |
| `docs/21-he-thong-thiet-ke.md` | Token màu, thang chữ, component, tiêu chuẩn tiếp cận |
| `docs/22-trang-quan-tri.md` | Chức năng quản trị, vai trò và quyền, bảng điều khiển dịch thuật |
| `docs/23-luong-dat-tour.md` | Đặt tour đầu-cuối: giữ chỗ → thanh toán → xác nhận → huỷ/hoàn |
| `docs/24-noi-dung-va-anh.md` | Giọng văn hai ngôn ngữ, quy trình chọn ảnh, alt text, bản quyền |

`21` có một ràng buộc dễ quên mà demo đã ghi lại: **font phải dựng sẵn cả æ ø å
lẫn dấu tiếng Việt**. Demo dùng Cambria/Constantia vì có đủ æ ø å; thêm tiếng
Việt thì phải thử lại — chữ "ế", "ữ", "ợ" và "Strand og øer på tværs" trong cùng
một bộ font. Demo đã dính đúng bẫy này một lần: file SVG bản đồ dùng Georgia,
"Điểm cuối" hiện thành "Điểm cuố i".

### Tầng 3 — Vận hành và tuân thủ

| File | Nguồn sự thật về |
|---|---|
| `docs/30-thanh-toan.md` | Cổng thanh toán mỗi thị trường, đặt cọc, hoàn tiền, đối soát |
| `docs/31-bao-mat-va-du-lieu-ca-nhan.md` | GDPR + NĐ 13/2023, dữ liệu cá nhân, thời hạn lưu, cookie consent |
| `docs/32-phap-ly-nganh-du-lich.md` | Rejsegarantifonden, chỉ thị EU về gói du lịch, Luật Du lịch 2017, quy tắc hiển thị giá |
| `docs/33-testing.md` | Chiến lược test theo tầng, dữ liệu test đa ngôn ngữ |
| `docs/34-cicd-va-moi-truong.md` | Môi trường, pipeline, migration khi triển khai, rollback |
| `docs/35-van-hanh.md` | Giám sát, sao lưu, runbook sự cố |

`34` kế thừa `docs/cicd.md` của demo (428 dòng, đã chạy thật: Docker hai giai
đoạn → GHCR → SSH vào VPS). Phần thêm: CSDL, migration, biến môi trường bí mật
của cổng thanh toán.

`32` phải được **luật sư hoặc người có chuyên môn duyệt**, không phải lập trình
viên. Quy tắc "giá luôn kèm chữ *từ* và disclaimer" mà demo ghi trong `CLAUDE.md`
là yêu cầu pháp lý ngành, không phải lựa chọn thiết kế — và Việt Nam có bộ quy
tắc riêng, không giống EU.

### Tầng 4 — Điều hành

| File | Nguồn sự thật về |
|---|---|
| `docs/40-ke-hoach-thuc-hien.md` | Bảy giai đoạn, cổng nghiệm thu, tiêu chí vào/ra, RACI, truy vết, kiểm soát thay đổi |
| `docs/41-tinh-trang.md` | Sổ trạng thái sống — đang ở đâu, chờ quyết định gì, chỗ dễ quên |
| `docs/42-quy-trinh-tai-lieu.md` | Vòng đời tài liệu, khối trạng thái, đánh số, quyền sở hữu, bộ kiểm tự động |
| `docs/adr/NNN-*.md` | Từng quyết định khó đảo ngược, một file một quyết định |
| `docs/tham-chieu/phan-tich-website.md` | Khảo sát website mẫu — chép nguyên từ demo |

`40` bắt chước `ke-hoach-claude-code.md` của demo: mỗi phiên có prompt sẵn, tiêu
chí hoàn thành đo được, kết thúc bằng một commit. Cách này đã chạy qua 12 + 6
phiên trên demo và giữ được chất lượng.

---

## 4. Thứ tự viết — bốn đợt

Không viết cả 24 file rồi mới code. Tài liệu đi trước code quá xa thì đoán mò,
và đoán sai thì viết lại. Mỗi đợt viết đủ để mở khoá đợt code kế tiếp.

### Đợt 1 — Chốt hướng ✔ **ĐÃ XONG 31/08/2026**

`CLAUDE.md` ×3 · `01-dac-ta-san-pham` · `02-thi-truong-va-da-ngon-ngu` ·
`03-tu-vung-nghiep-vu` · `10-kien-truc-he-thong` (kèm ADR-001 stack, ADR-002
spec-first, ADR-003 cách lưu bản dịch)

Đây là đợt duy nhất **bắt buộc xong trước khi viết dòng code đầu tiên**. Lý do:
quyết định thị trường/ngôn ngữ ở `02` quyết định lược đồ CSDL, mà đổi lược đồ
sau khi có dữ liệu thật là việc đắt nhất trong cả dự án.

*Xong khi:* trả lời được "một tour hiển thị cho khách Đan và khách Việt khác
nhau ở những trường nào, và giá lấy từ đâu" mà không phải suy đoán.

### Đợt 2 — Nền dữ liệu ✔ **ĐÃ XONG 31/08/2026**

`11-mo-hinh-du-lieu` · `12-luoc-do-csdl` · `13-hop-dong-api` · `14-quy-tac-nghiep-vu`

Mở khoá: dựng CSDL, migration Flyway, module `domain`, API đọc.

*Xong khi:* vẽ được ERD đầy đủ; `contracts/openapi.yaml` sinh được cả interface
Java lẫn TS client và cả hai bên biên dịch sạch; quy tắc tồn kho trả lời được
"hai khách cùng bấm đặt chỗ cuối cùng thì chuyện gì xảy ra".

### Đợt 3 — Ứng dụng (5 file)

`20-frontend-web` · `21-he-thong-thiet-ke` · `22-trang-quan-tri` ·
`23-luong-dat-tour` · `24-noi-dung-va-anh`

Mở khoá: dựng giao diện khách và trang quản trị.

*Xong khi:* mỗi màn hình có đặc tả ba trạng thái (loading / rỗng / lỗi) và bản
dịch của mọi chuỗi trên đó.

### Đợt 4 — Sẵn sàng chạy thật (6 file)

`30-thanh-toan` · `31-bao-mat-va-du-lieu-ca-nhan` · `32-phap-ly-nganh-du-lich` ·
`33-testing` · `34-cicd-va-moi-truong` · `35-van-hanh`

Mở khoá: nhận tiền thật của khách thật.

*Xong khi:* `32` được người có chuyên môn pháp lý duyệt; `35` có runbook cho
tình huống "cổng thanh toán trả về lỗi sau khi đã trừ tiền khách".

`00-ke-hoach-tai-lieu` và `41-tinh-trang` là hai file sống, cập nhật liên tục
qua cả bốn đợt.

Bốn đợt ở trên là nhịp **viết tài liệu**. Nhịp **thực hiện dự án** là bảy giai
đoạn G0–G6 ở `40-ke-hoach-thuc-hien.md`, và hai nhịp này không trùng nhau: đợt 3
tài liệu nằm trong giai đoạn G3, đợt 4 trải qua G4 và G5. Khi cần biết "được
phép bắt đầu code phần nào rồi", đọc `40`, không đọc mục này.

---

## 5. Quy ước để tài liệu không lỗi thời

Toàn bộ quy ước — vòng đời trạng thái, khối trạng thái, đánh số, quyền sở hữu,
quy trình sửa, bộ kiểm tự động, rà soát 90 ngày — nằm ở
`42-quy-trinh-tai-lieu.md`. Không chép lại ở đây.

Bốn điều rút ra từ demo, là lý do `42` tồn tại:

1. **Tài liệu chết vì trùng lặp.** Demo có bản đồ URL ở hai file; đổi đường dẫn
   một lần là một trong hai chỗ lạc hậu vĩnh viễn, và không ai biết chỗ nào
2. **Tài liệu chết vì không ai sở hữu.** File không có chủ thì không ai dám sửa
3. **Quyết định nằm trong chat là quyết định mất.** Đi vào ADR, một file một
   quyết định
4. **Hàng rào phải tự động.** `docs/tinh-trang.md` của demo gọi
   `npm run validate-data` là "hàng rào chính" vì nó bắt được lỗi mà `tsc` bỏ
   qua. Bộ tài liệu cần thứ tương đương: `python scripts/docs_check.py`

---

## 6. Rủi ro

| Rủi ro | Mức | Cách giảm |
|---|---|---|
| Mô hình đa ngôn ngữ chốt sai, phát hiện sau khi đã có dữ liệu thật | **Cao** | Đợt 1 phải xong và được duyệt trước khi code. Dựng thử một tour đủ hai thị trường trên giấy trước khi viết lược đồ |
| Nội dung tour phải viết hai lần, không phải dịch máy | **Cao** | `24` quy định rõ: nội dung bán hàng do người viết theo từng thị trường; chỉ chuỗi giao diện mới dịch theo khoá |
| Viết 24 tài liệu rồi mới code, tài liệu lạc hậu ngay khi code chạm vào | **Cao** | Chia bốn đợt như mục 4. Mỗi đợt chỉ viết đủ để mở khoá đợt code kế tiếp |
| Yêu cầu pháp lý hai nước mâu thuẫn nhau | Trung bình | `32` viết sớm ở dạng nháp, người có chuyên môn duyệt trước đợt 4 |
| **Hợp đồng API lệch giữa Java và TypeScript** mà không ai phát hiện | **Cao** | Spec-first: controller `implements` interface sinh từ spec, nên lệch là lỗi biên dịch. CI kiểm tương thích ngược. Xem 2.1.b |
| Font không đủ ký tự cho cả hai ngôn ngữ | Trung bình | `21` bắt buộc thử chuỗi có đủ æ ø å và dấu tiếng Việt trước khi chốt font |
| Tồn kho chỗ: hai khách đặt trùng chỗ cuối | Trung bình | `14` phải đặc tả giữ chỗ có hạn và khoá ở tầng CSDL, không khoá ở tầng ứng dụng. Nhiều instance thì cần ShedLock cho job quét hạn |
| Sai số tiền tệ do dùng `double`, hoặc sai scale giữa VND và DKK | Trung bình | `14` quy định `BigDecimal` và scale theo từng tiền tệ. Test đối chiếu cả hai thị trường |
| Sắp xếp và tìm kiếm sai theo ngôn ngữ (æ ø å, dấu tiếng Việt) | Trung bình | Collation ICU và `unaccent` ở tầng Postgres; test bằng Testcontainers với Postgres thật, không dùng H2 |
| Bộ tài liệu phình quá sức đội ngũ nhỏ | Trung bình | Gộp file khi thấy mỏng. 24 là trần, không phải chỉ tiêu |

---

## 7. Việc tiếp theo

### Đợt 1 đã giao — 13 file

| File | Nội dung đáng nhớ nhất |
|---|---|
| `02-thi-truong-va-da-ngon-ngu.md` | Market ≠ Locale; hai chính sách fallback ngược nhau; URL và hreflang; khuôn mẫu bảng dịch; quy trình dịch |
| `03-tu-vung-nghiep-vu.md` | Đối chiếu Việt–Đan–định danh code; các bẫy nghiệp vụ; danh sách thuật ngữ bị cấm |
| `04-phan-loai-san-pham.md` | Sáu loại sản phẩm; **năm chiều phân biệt**; nghiệp vụ giá, tồn kho, huỷ đổi và cạm bẫy từng loại; bảng đối chiếu 17 chức năng |
| `05-trang-chi-tiet-san-pham.md` | Tab nào có theo loại; dòng phụ dưới H1; nút chính; ba màn hình mẫu; **trạng thái rìa** |
| `01-dac-ta-san-pham.md` | Phạm vi v1, danh sách **không làm**, tám quy tắc sản phẩm, tiêu chí ra mắt |
| `10-kien-truc-he-thong.md` | Bố cục repo, bốn module Gradle, luồng một request, danh mục ADR |
| `adr/001` … `adr/004` | Một repo hai toolchain · spec-first · bảng dịch riêng · `da` là ngôn ngữ nguồn |
| `CLAUDE.md` ×3 | Gốc repo, `api/`, `web/` — mỗi bên chỉ đọc phần của mình |

### Đợt 2 đã giao — 6 file

| File | Nội dung đáng nhớ nhất |
|---|---|
| `11-mo-hinh-du-lieu.md` | Bảy quyết định mô hình hoá; ERD; bảng con theo loại; `product_market` là cổng chặn; đơn đặt **chụp lại** giá; bảy giá trị tính ra không lưu |
| `12-luoc-do-csdl.md` | DDL thật; khoá ngoại kép cưỡng chế đúng loại; `f_unaccent` để index được; index bộ phận theo locale; **15 quy tắc kiểm dữ liệu** |
| `13-hop-dong-api.md` | Hai bề mặt API; market ở path, locale ở header; 16 mã lỗi; `Idempotency-Key`; ngày khởi hành **không bao giờ cache** |
| `14-quy-tac-nghiep-vu.md` | Engine giá 8 bước; **quy tắc làm tròn từng dòng**; giải trạng thái 7 bước; khoá bi quan cho giữ chỗ; danh sách test bắt buộc |
| `adr/005` | Mỗi loại sản phẩm một bảng con |
| `adr/006` | Ngày khởi hành thuộc về một thị trường |

### Việc kế tiếp

1. **Trả lời GĐ-6**: v1 có thanh toán online thật, hay chỉ nhận đặt chỗ rồi gọi
   điện chốt? Quyết định mục 3 và mục 4 của `30` — phần còn lại của tài liệu đó
   đứng vững dù trả lời thế nào.
2. **Sáu con số nghiệp vụ đang chặn thị trường `VN`** — gom ở `14` mục 10: tỷ lệ
   đặt cọc, phí xử lý, có giảm đặt sớm không, có làm tròn tới nghìn đồng không,
   hạn giữ chỗ, bậc thời gian huỷ và tỷ lệ hoàn. Engine giá viết xong rồi vì nó
   **nhận hằng số làm tham số**; chỉ thiếu dữ liệu cấu hình.
3. **Dựng scaffolding `api/` và `web/`** để ba file `CLAUDE.md` hết trạng thái
   "lệnh chưa chạy được", rồi viết `contracts/openapi.yaml` và migration đầu tiên.
4. **Đợt 3** — `20-frontend-web`, `21-he-thong-thiet-ke`, `22-trang-quan-tri`,
   `23-luong-dat-tour`, `24-noi-dung-va-anh`. Viết được ngay, không chờ câu nào.
