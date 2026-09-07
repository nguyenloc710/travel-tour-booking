# Sổ trạng thái

```
Trạng thái: Đã duyệt
Cập nhật: 07/09/2026
Phiên bản: 1.2
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
| **Cổng gần nhất đã qua** | G2 — 01/09/2026, **qua có điều kiện**. Còn **1/3** điều kiện treo: mở một PR thật (mục 7) |
| **Phần code của G3** | **Xong.** Tiêu chí ra 2–8 của `40` mục G3 đều đã có code; tiêu chí 1 chỉ chờ nội dung thật |
| **Việc chặn G3 qua cổng** | Nội dung thật của 3 tour đủ hai ngôn ngữ — chặn ở **Q-1**; và `20`, `21`, `05` còn ở `Nháp` |
| **Tài liệu** | 23/26 file. Xong tầng 0, 1, 2, 4 và `30`, `31`, `34` — còn lại `32`, `33`, `35` |
| **Nhánh** | `dung-khung-va-loi-danh-muc` — **75 commit** trước `main`, đã đẩy lên origin, cây làm việc sạch. `main` vẫn chỉ có commit khởi tạo: merge được bằng fast-forward, nhưng **đi qua PR** để lượt CI đầu tiên xảy ra ở đúng chỗ nó phải xảy ra |

---

## 2. Đã có gì

### Tài liệu — 23 file

| Tầng | File | Trạng thái |
|---|---|---|
| 0 | `00-ke-hoach-tai-lieu` | Đã duyệt |
| 0 | `01-dac-ta-san-pham` | Nháp |
| 0 | `02-thi-truong-va-da-ngon-ngu` | Nháp — **cần nâng lên Đã duyệt, xem mục 4** |
| 0 | `03-tu-vung-nghiep-vu` | Nháp |
| 0 | `04-phan-loai-san-pham` | Nháp — **cần nâng lên Đã duyệt** |
| 0 | `05-trang-chi-tiet-san-pham` | Nháp — **cần `Đã duyệt` trước cổng G3** |
| 1 | `10-kien-truc-he-thong` | Nháp |
| 1 | `11-mo-hinh-du-lieu` | Nháp |
| 1 | `12-luoc-do-csdl` | Nháp |
| 1 | `13-hop-dong-api` | Nháp |
| 1 | `14-quy-tac-nghiep-vu` | Nháp |
| 1 | `15-ke-hoach-dung-be` | Nháp |
| 2 | `20-frontend-web` | Nháp — **cần `Đã duyệt` trước cổng G3** |
| 2 | `21-he-thong-thiet-ke` | Nháp — **cần `Đã duyệt` trước cổng G3** |
| 2 | `22-trang-quan-tri` | Nháp — cần `Đã duyệt` trước G4 |
| 2 | `23-luong-dat-tour` | Nháp — cần `Đã duyệt` trước G4 |
| 2 | `24-noi-dung-va-anh` | Nháp — cần `Đã duyệt` trước G5 |
| 3 | `30-thanh-toan` | Nháp |
| 3 | `31-bao-mat-va-du-lieu-ca-nhan` | Nháp — **cần người có chuyên môn pháp lý duyệt** |
| 3 | `34-cicd-va-moi-truong` | Nháp |
| 4 | `40-ke-hoach-thuc-hien` | Đã duyệt |
| 4 | `41-tinh-trang` | Đã duyệt — file này |
| 4 | `42-quy-trinh-tai-lieu` | Đã duyệt |

ADR-001 … ADR-006 và ADR-010: **đã chốt**. ADR-007 (cổng thanh toán), ADR-008
(lưu ảnh) **bị thay thế bởi ADR-011** khi Q-6 chốt ngày 06/09; ADR-009 (phạm vi
`COMBO`) vẫn **đề xuất**, chờ Q-7, và ADR-007 chờ Q-3.
Không còn ADR nào được trỏ tới mà chưa viết.

Ba file `CLAUDE.md`: gốc repo, `api/`, `web/`.

### Ngoài tài liệu

| Thứ | Ở đâu | Ghi chú |
|---|---|---|
| Bản Word gửi khách | `docs/Dac-ta-chuc-nang-nghiep-vu.docx` | 12 chương, sinh lại bằng `docs/tools/build-docx.py` |
| Quy trình cho Claude Code | `.claude/` | Skill, lệnh, hook, agent |
| Bộ kiểm tài liệu | `scripts/docs_check.py` | Không cần thư viện ngoài. Máy đang làm **nay đã có Python 3.12 thật** — mục 6.1 đã lạc hậu |
| Scaffolding backend | `api/` | **Một** module Gradle chia theo feature (ADR-010), 204 file Java, migration `V1`–`V6` |
| Hợp đồng API | `contracts/openapi.yaml` | **v0.16.0** — 54 endpoint |
| Postgres cho dev | `compose.yaml` | Postgres 16, có ICU và contrib |
| Scaffolding frontend | `web/` | pnpm workspace, 2 app Next.js, 3 package dùng chung |
| CI | `.github/workflows/` | `api.yml`, `web.yml`, `tai-lieu.yml` — lọc theo đường dẫn |
| Lõi danh mục | `contracts/openapi.yaml` v0.2 · `api/` · `web/` | Listing + trang chi tiết, hai đầu sinh từ cùng một spec |
| Tài liệu tầng 2 | `docs/20` … `docs/24` | Đủ 5 file, tất cả ở `Nháp`. Theo `40`: `20`, `21` (cùng `05` ở tầng 0) phải `Đã duyệt` trước **G3**; `22`, `23` trước **G4**; `24` trước **G5** |
| Migration `V2` | `12` mục 3.1, 4.6, 4.7, 6.1 · `V2__vai_tro_anh_hanh_khach_slug.sql` | 7 bảng: vai trò, ảnh kèm giấy phép, slug cũ, hành khách. 13 test |
| **Ba màn hình đọc của trang quản trị** | `22` M2, M10, M12 · spec v0.7 · `admin/` | Danh sách sản phẩm, hàng đợi dịch, bảng độ phủ — **12 test** |
| **Đường ghi của trang quản trị** | `22` M3, M4, M5 · spec v0.8 · `admin/` | Tạo/sửa/xoá mềm sản phẩm, gán thị trường, ngày khởi hành, nhân bản lịch, bảng giá, thang giá — **17 test** |
| **Giao diện trang quản trị** | `web/apps/admin` · 5 màn hình | Đăng nhập, bảng điều khiển, danh sách, tạo tour, sửa 4 tab. Vòng mở bán khép kín **từ giao diện** |
| Migration `V5` — `price_from` thành cột trigger sở hữu | `11` mục 12 · `12` mục 6.1 và quy tắc kiểm 5 | Ba trigger; dữ liệu mồi bỏ giá đặt tay, nay suy từ `departure_price` |
| **Luồng báo giá `PRIVATE_TOUR`** | `14` mục 7 · `23` mục 7 · spec v0.13 · `V6` | Khách gửi yêu cầu, tư vấn viên dựng bảng giá rồi gửi. Máy trạng thái thuần, job quét hạn, form trên site khách, hai màn hình M8 — **26 test** |
| **Nội dung khác và người dùng** | `22` M13, M14 · spec v0.14 | Điểm đến, bài viết, buổi thuyết trình sửa được từ giao diện; ADMIN gán được vai trò. Bốn màn hình quản trị — **22 test** |
| **Ba trang còn thiếu của site khách** | `20` R9, R10, R11 | Blog kèm lọc thẻ, sự kiện, liên hệ. Menu chính ở header; sitemap nhận thêm bài viết |
| **Hệ thống thiết kế áp vào site khách** | `21` mục 2–5 · `05` mục 3–5 | Token màu, chữ, khoảng cách, điểm ngắt; trang chủ R1 thật; **trang chi tiết chia tab** kèm thanh dính đáy |
| **Giao diện khách dựng đủ khối** | `21` mục 5.0 · `20` R1 | Đầu trang có bộ đổi ngôn ngữ, khối mở đầu có ảnh, băng lời hứa, lưới hình thức đi, thẻ tour có ảnh và sao, dải dữ kiện, thẻ khách sạn, chân trang bốn cột |
| **Dữ liệu mồi đủ để nhìn** | `12` mục 8.2 | 12 sản phẩm phủ cả sáu loại, 82 ngày lịch trình song ngữ, 10 khách sạn, 10 tham quan, 6 chủ đề, 6 bài viết, 6 buổi thuyết trình. **Đạt 13/19 quy tắc kiểm** ở `12` mục 9 |
| **Ảnh mẫu cho dev** | `24` mục 7.1b · `scripts/tai-anh-mau.py` | 29 ảnh thật từ Unsplash, tải về phục vụ từ máy chủ của mình, kèm sổ giấy phép `NGUON.md` |
| **Đợt thiết kế thứ hai** | `21` mục 2, 3.1, 3.2 | Hai họ chữ (có chân cho tiêu đề), bảng màu ấm, màu kêu gọi tách khỏi màu nhấn, thang chữ co giãn, bỏ viền hộp, khối tràn hết bề ngang |

---

## 3. Chưa có gì

### Tài liệu còn thiếu — 3 file, toàn bộ là tầng 3

`32-phap-ly-nganh-du-lich` · `33-testing` · `35-van-hanh`

`34-cicd-va-moi-truong` viết ngày 02/09/2026 — điều kiện treo số 2 của G2.
`30-thanh-toan` và `31-bao-mat-va-du-lieu-ca-nhan` viết cùng ngày. `30` mục 1
chia rõ ba trong năm mục **không** phụ thuộc Q-3, nên nó vẫn có giá trị kể cả khi
Q-3 trả lời "không". `31` cần **người có chuyên môn pháp lý duyệt** — `40` mục 4
mới chỉ bắt `32` phải có chữ ký đó.

Không còn ADR nào thiếu. ADR-007, ADR-008 và ADR-009 đều ở trạng thái `Đề xuất`
— cả ba không tự chốt được, vì phần còn thiếu của chúng là quyết định chi tiêu,
quyết định phạm vi, và hai tài liệu chưa có (`30`, `31`).

Cộng `docs/tham-chieu/phan-tich-website.md` — chép nguyên từ demo, chưa chép.

### Code

| Phần | Trạng thái |
|---|---|
| `api/` **một** module Gradle chia theo feature — ADR-010, không còn `archTest` | ✔ |
| `api/` migration `V1__khoi_tao.sql` — toàn bộ lược đồ `12` | ✔ **đã chạy trên Postgres 16 thật** |
| `api/` dữ liệu tra cứu `R__` và `scripts/seed-dev.sql` | ✔ đã chạy sạch |
| Cột kiểm toán + xoá mềm: 18 bảng đủ 5 cột, 3 bảng 4 cột, 21 trigger, 15 index duy nhất bộ phận | ✔ |
| Đã commit | ✔ nhánh `dung-khung-va-loi-danh-muc`, 44 commit trước `main`. **Bảy commit mới nhất chưa đẩy** lên origin |
| Test | ✔ **276/276 xanh, 0 lỗi** — `./gradlew test` chạy thật ngày 05/09 trên Postgres 16 qua Testcontainers. 21 lớp: 13 IT và 8 unit thuần không context |
| `contracts/openapi.yaml` **v0.16.0** (54 endpoint) → interface Java | ✔ sinh và biên dịch sạch |
| `contracts/openapi.yaml` → TS client | ✔ sinh và biên dịch sạch. `packages/api-client` chỉ commit `package.json` + `tsconfig`, mã nguồn sinh lúc build — đúng quy tắc 9 của `CLAUDE.md` |
| `web/` pnpm workspace: `site`, `admin`, `i18n`, `ui`, `api-client` | ✔ |
| `pnpm typecheck` · `lint` · `test` · `i18n:check` · `build` | ✔ tất cả xanh, chạy lại 05/09. `i18n:check` **219 khoá**, `vi` 100.0% — nhưng xem lỗ hổng của nó ở mục 6.5. **`typecheck` phải chạy SAU `build`** ở máy local — xem mục 6.3 |
| Chạy thật đầu-cuối: API + site, hai locale, hai market | ✔ |
| `.github/workflows/`: `api.yml`, `web.yml`, `tai-lieu.yml` | ✔ đã dựng |
| CI chạy trên một PR thật | ✗ **chưa mở PR nào** — điều kiện treo số 1 của G2 |
| `Dockerfile` cho `api/`, `site`, `admin` — hai giai đoạn | ✔ có mã, ✗ **chưa build thật lần nào**: VM Docker trên máy đang làm không ra được registry |
| `deploy/` — compose nền + hai file phủ (`ip`, `tenmien`), `Caddyfile`, `.env.prod.example` | ✔ có mã. `compose config` xanh **ở cả hai chế độ**; chưa chạy trên máy chủ nào |
| Chế độ chạy đầu tiên | **IP + mở cổng, không HTTPS** — chưa có tên miền. `34` mục 5.0 ghi rõ đánh đổi: mật khẩu quản trị đi qua mạng dạng chữ thường |
| `.github/workflows/trien-khai.yml` — build → GHCR → SSH → up | ✔ có mã, ✗ chưa chạy lượt nào, chưa điền bí mật nào |
| Branch protection trên `main` (ba check bắt buộc) | ✗ **chưa bật** — thiếu nó thì `trien-khai.yml` sẽ triển khai cả commit đỏ |
| VPS: tên miền, `.env`, sao lưu | ✗ máy chủ đã có, **chưa cấu hình gì** — `34` mục 8 |
| API đọc: `GET /{market}/products` và `/products/{slug}` | ✔ 20 test tích hợp xanh |
| Site khách: trang danh sách + trang chi tiết, ba trạng thái, bộ lọc trong URL | ✔ chạy thật đầu-cuối |
| Tiêu chí ra **2–8** của G3 (`40` mục G3) đã có code | ✔ rà 04/09: không-fallback bằng `INNER JOIN`; `ORDER BY … COLLATE "da-DK-x-icu"` lấy tên collation từ bảng `locale`; `f_unaccent` ở **cả hai vế** khi tìm; sitemap `da` 12 URL / `vi` 9 URL. Tiêu chí **1** chờ nội dung thật — Q-1 |
| Sắp sản phẩm theo ngày khởi hành gần nhất | ✗ `13` mục 12 |
| API điểm đến: `GET /destinations`, `/destinations/{slug}`, lọc sản phẩm theo điểm đến | ✔ spec v0.3, 10 test tích hợp |
| Tầng nội dung biên tập: chủ đề, khách sạn, tham quan, lịch trình, bài viết, thẻ, buổi thuyết trình | ✔ `12` mục 4.8–4.11 và `V3` — 17 bảng, 12 test |
| API cho tầng nội dung đó: `/themes`, `?theme=`, `/itinerary`, `/hotels`, `/departures`, `/posts`, `/lectures` | ✔ spec v0.4, 15 test tích hợp + 11 test domain |
| Giải trạng thái ngày khởi hành ở `domain` (`14` mục 5) | ✔ hàm thuần, 11 test JUnit không context |
| Đăng nhập nhân viên, phiên cookie `HttpOnly`, ma trận quyền `22` mục 2.1 | ✔ 12 test |
| Đường **ghi** đầu tiên: sửa bản dịch sản phẩm qua JPA + MapStruct, cột kiểm toán tự điền | ✔ |
| Engine giá ở `domain`: tám bước cộng dồn, làm tròn từng dòng, bậc giá, giảm đặt sớm, nâng hạng cabin | ✔ **31 test** JUnit thuần — `14` mục 9.1 tick đủ |
| Tính giá, giữ chỗ, đặt tour, tra đơn — `POST /pricing/preview`, `/seat-holds`, `/bookings` | ✔ 19 test, gồm **test hai luồng giành chỗ cuối** |
| Máy trạng thái đơn ở `booking/service` | ✔ 9 test |
| **Gộp bốn module Gradle thành một, chia theo feature** | ✔ ADR-010 — mất `archTest` và 6 test ranh giới |
| Tính bất biến khi gọi lại (`Idempotency-Key`) và job quét hạn có ShedLock | ✔ `V4` |
| Thanh toán thật, webhook, email | ✗ đợt 5 — **Q-3** |
| Khách tự huỷ đơn | ✗ có chủ ý: bậc huỷ và tỷ lệ hoàn chưa chốt (**Q-2**), `23` mục 8 |
| Con số nghiệp vụ thị trường `VN` | ✗ **Q-2** — engine chạy được, chỉ thiếu dữ liệu cấu hình |
| `GET /site-info` | ✗ `13` mục 9.1 mới nói bốn chữ — chưa đủ để dựng bảng, ghi ở `12` mục 10 |
| Trang điểm đến ở `web/` — danh sách R5, chi tiết R6, tìm tour R2 | ✔ cả hai locale; đoạn của locale kia trả 404 |
| Sitemap theo locale và `robots.txt` | ✔ `/sitemap/da.xml` 12 URL · `/sitemap/vi.xml` 9 URL — chênh lệch chính là chính sách không-fallback |
| **Luồng đặt tour trên web khách** — R7 bốn bước, R8 xác nhận | ✔ giữ chỗ ở bước 1, tính giá lại mỗi lần đổi, tạo đơn, tra cứu bằng mã + email |
| Bốn nhóm bảng thiếu: vai trò, bộ ảnh kèm giấy phép, slug cũ, hành khách | ✔ `V2` — chạy thật trên Postgres 16, 13 test |
| Đọc và ghi bốn nhóm bảng đó qua API | **Ba trong bốn đã có** (rà 04/09, dòng cũ ghi ✗ là sai): `staff_user_role` đọc ở `auth/StaffRoleRepository`, `slug_history` đọc qua `GET /redirects`, `booking_passenger` **ghi thật** trong luồng đặt tour. `media_asset` **đã có đường đọc** từ 06/09 (bộ ảnh ở trang chi tiết); đường ghi còn thiếu |
| `product.hero_image` và `map_image` trỏ tới `media_asset` | ✗ đổi phá vỡ tương thích, phải tách hai lần triển khai — `12` mục 10 |
| Chuyển hướng slug cũ đọc `slug_history` | ✔ spec v0.10 `GET /{market}/redirects/{type}/{slug}` + trang chi tiết bắt 404 rồi chuyển hướng **308**. Chỉ chuyển khi đích thật sự xem được — xem `20` mục 6 |
| Danh sách sản phẩm quản trị, hàng đợi dịch, bảng độ phủ | ✔ 12 test — `22` M2, M10, M12 |
| **Vận hành đơn: danh sách (`22` M6) và chi tiết (M7)** | ✔ spec v0.11 · `GET /admin/bookings` + `/admin/bookings/{reference}` · hai màn hình `admin/don` · **17 test**. Mặc định lọc `NEEDS_ACTION` nằm ở **backend**; chi tiết trả phân rã giá đã chụp lại, hành khách kèm hộ chiếu, và **toàn bộ** `booking_event` |
| **Đổi trạng thái đơn từ trang quản trị** | ✔ spec v0.12 · `POST /admin/bookings/{reference}/status` · khối thao tác ở màn hình M7 · **8 test**. Khoá bi quan trên dòng đơn; huỷ **trả chỗ về kho ngay** (`14` mục 6.5); mọi lần đổi ghi một `booking_event`. Nhân viên đặt được **bốn** trạng thái, ba cái còn lại do luồng thanh toán và job quét hạn sinh ra |
| Huỷ chuyến vì thiếu khách — huỷ **hàng loạt** đơn của một ngày khởi hành | ✗ `14` mục 6.6 đòi màn hình riêng. Nay huỷ được **từng đơn một**, chưa huỷ được cả chuyến |
| Hàng đợi dịch cho **điểm đến** và **buổi thuyết trình** | ✗ có chủ ý: bảng dịch của chúng không có `status` lẫn `translated_at` — `12` mục 10. Nhưng **M13 nay hiện trạng thái từng locale**, nên chỗ thiếu bản dịch vẫn nhìn thấy được |
| **Luồng báo giá** — khách gửi yêu cầu, nhân viên dựng giá và gửi | ✔ `14` mục 7 đủ bảy quy tắc; job `QuoteSweeper` cho báo giá quá hạn sang `EXPIRED` |
| Từ báo giá `ACCEPTED` sinh ra `Booking` | ✗ quy tắc 3 của `14` mục 7. Bước sau đó là đặt cọc, mà đặt cọc chờ **Q-3** |
| **M13 nội dung khác** — điểm đến, bài viết, sự kiện | ✔ CRUD, bản dịch, xoá mềm cả chùm; luật quyền theo vai trò **và** locale |
| M13 cho **khách sạn** và **điểm tham quan** | ✗ hai bảng đó gắn vào sản phẩm và chưa có cả đường đọc quản trị. `22` M13 kể tên chúng, ma trận mục 2.1 thì không |
| **M14 người dùng và vai trò** | ✔ chỉ `ADMIN`; `LAST_ADMIN` chặn tự khoá mình ra ngoài |
| Tạo người dùng mới, đặt lại mật khẩu | ✗ cần luồng mời qua email, mà email chưa có gì — đợt 5, **Q-3** |
| Tạo, sửa, xoá mềm sản phẩm; gán thị trường; ngày khởi hành; bảng giá; thang giá | ✔ 17 test, gồm **một bài đi hết bảy bước mở bán rồi kiểm bằng bề mặt khách** |
| Nhân bản lịch khởi hành giữa hai thị trường — **không** chép giá | ✔ `22` M4, ADR-006 |
| `price_from` tự tính từ `departure_price` | ✔ `V5` — trước đó cột này chưa bao giờ được điền |
| Đổi loại sản phẩm sau khi tạo | ✗ **có chủ ý cấm** — `22` mục 7; `productType` là `updatable = false` ở entity |
| Presigned URL và màn tải ảnh ở trang quản trị | ✗ — cần client S3, đường ĐỌC đã xong 06/09 |
| **`pnpm contracts:generate`** | ✔ sửa 05/09 — nó gọi `:web:contractsGenerate`, đường dẫn task chết từ khi ADR-010 gộp module |
| Trang `blog`, `kontakt`, `foredrag` của site khách | ✔ **xong 05/09** — R9 kèm lọc thẻ lặp lại được, R10, R11. Bảng `pathnames` không còn hứa nhiều hơn thứ đang có |
| Giới hạn 10 lượt đăng nhập / 15 phút | ✗ `22` mục 9 mô tả như đã có, **chưa có gì cài** — `31` mục 6.2. Rà lại 04/09: `api/src/main` không có một dòng nào về rate limit |
| Cờ `Secure` cho cookie phiên | ✗ chặn trước lần triển khai `prod` đầu — `34` mục 5.2 |
| **Bộ kiểm nhất quán dữ liệu** — 23 quy tắc của `12` mục 9 | ✔ **xong 05/09** — `api/scripts/kiem-nhat-quan.sql` + `KiemNhatQuanIT`. Quy tắc 13 cảnh báo có chủ ý, chờ **Q-2** |
| Giá phòng đơn bắt buộc ở sản phẩm có lưu trú (quy tắc 23) | ✔ chặn ở **ba** chỗ: lưu bảng giá, bật bán, và đường tính giá — `409 SINGLE_PRICE_MISSING`. Trước đó 36/40 ngày khởi hành thiếu, và khách đi một mình trả giá chia đôi phòng |

---

## 4. Quyết định đang chờ

Số ngày treo tính từ 31/08/2026.

| # | Câu hỏi | Chặn | Ai trả lời | Treo từ |
|---|---|---|---|---|
| Q-1 | **Ai viết nội dung tour tiếng Đan Mạch?** | **G3** | Chủ sản phẩm | 31/08 |
| Q-2 | Sáu con số nghiệp vụ thị trường `VN`: tỷ lệ đặt cọc, phí xử lý, có giảm đặt sớm không, làm tròn tới nghìn đồng không, hạn giữ chỗ, bậc huỷ và tỷ lệ hoàn | **G4** | Chủ sản phẩm | 31/08 |
| Q-3 | GĐ-6 — v1 có thanh toán online thật, hay nhận đặt chỗ rồi gọi điện chốt? Nay chỉ còn chặn **mục 3 và 4** của `30`; ba mục kia và ADR-007 đã viết xong không chờ nó | G5 | Chủ sản phẩm | 31/08 |
| Q-4 | Quy mô dự kiến: bao nhiêu tour, bao nhiêu đơn mỗi tháng? | G2 | Chủ sản phẩm | 31/08 |
| Q-5 | Chạy một hay nhiều instance? | G2 | Kiến trúc sư | 31/08 |
| ~~Q-6~~ | ~~Lưu ảnh ở đâu: VPS, S3, hay CDN?~~ → **đã chốt 06/09: MinIO tự dựng cho cả dev và prod, ADR-011**, thay thế đề xuất "kho có quản" của ADR-008 | — | — | xong 06/09 |
| Q-7 | `COMBO` làm bó cố định do nhân viên soạn, hay tồn kho thời gian thực? → **ADR-009 đã quyết: bó cố định**, chỉ còn xác nhận | **v1.5**, không phải G4 — xem ghi chú dưới bảng | Chủ sản phẩm | 31/08 |
| Q-8 | Nâng `02` và `04` lên `Đã duyệt` — hai file này quyết định lược đồ CSDL, G0 đã qua mà chúng vẫn ở `Nháp` | **G2** | Chủ sản phẩm + Kiến trúc sư | 31/08 |

> **Q-7 trước nay bị gắn nhầm cổng.** `41` ghi nó chặn G4, nhưng tám tiêu chí ra
> của G4 (`40` mục 4) không nhắc `COMBO` một lần nào, và `04` mục 2 cùng `01`
> mục 3.2 đều xếp `COMBO` vào **v1.5**. Nó chặn v1.5, không chặn cổng nào của v1.
> Một câu hỏi treo gắn nhầm vào cổng gần nhất làm hai việc cùng lúc: tạo áp lực
> giả lên cổng đó, và giấu mất việc thật — chưa ai lên lịch cho v1.5.

> **Q-1 là câu hỏi nguy hiểm nhất** vì nó không có vẻ kỹ thuật nên dễ bị đẩy lùi.
> Nội dung tiếng Đan là **ngôn ngữ nguồn** (ADR-004): không có nó thì không có gì
> để dịch, không có gì để hiển thị, và toàn bộ G3 xong xuôi vẫn không ra mắt được.

---

## 5. Việc kế tiếp — theo thứ tự

Ba điều kiện treo của cổng G2 (mục 7) đi trước, rồi tới phần còn lại của G3.

1. **Mở một PR thật** — điều kiện treo số 1 của G2, và là cái **duy nhất** còn
   lại. Cần thấy: sửa `web/` **không** kích hoạt build Gradle, sửa `contracts/`
   kích hoạt **cả hai**, sửa `docs/` chỉ kích hoạt `tai-lieu.yml`.
   Nhánh đã đẩy; `gh` chưa cài trên máy này nên PR phải mở bằng tay.

   PR gộp nhánh vào `main` sẽ chạm cả bốn vùng nên kích hoạt **trọn ba
   workflow** một lượt — đúng thứ cần để xác nhận. Nhưng nó **không** chứng minh
   được bộ lọc *loại trừ*: muốn thấy "sửa `web/` không kích hoạt `api.yml`" thì
   cần một PR nhỏ thứ hai chỉ động vào `web/`.

   Đi kèm việc này: **dựng lại bộ công cụ kiểm trên máy đang làm** (mục 6.1) rồi
   chạy lại `./gradlew test` và `pnpm build`. Mọi con số xanh trong file này
   hiện là **ghi chép lịch sử**, chưa tái kiểm được ở máy hiện tại — và mở PR mà
   không chạy được gì trên máy thì CI đỏ cũng không sửa tại chỗ được
2. ~~Viết `34-cicd-va-moi-truong` bản nháp~~ — **xong** 02/09/2026
3. ~~Sửa câu chữ tiêu chí 5 của `40`~~ — **xong** 02/09/2026
4. **Nội dung thật cho G3**: ít nhất 3 tour đủ hai ngôn ngữ. Đang chặn ở **Q-1**;
   không có người viết bản tiếng Đan thì hệ thống xong mà không có gì để bán
5. Trả lời Q-8, và **duyệt `20`, `21`, `05`** — cổng G3 đòi cả ba ở `Đã duyệt`
6. ~~Phần G3 còn thiếu ở tầng code: trang điểm đến, trang tìm kiếm, sitemap theo
   locale, chuyển hướng 301 đọc `slug_history`~~ — **xong** 02/09/2026. Chuyển
   hướng cài bằng **308** chứ không 301, và chỉ chuyển khi đích thật sự xem được
   (`20` mục 6)
7. ~~Đợt 5b~~ — **xong** 02/09/2026. Tiêu chí ra số 7 của G4 nay có một bài
   test chứng minh: tạo tour qua API quản trị, dịch, gán thị trường, nhập lịch
   và giá, rồi tour đó hiện ra ở `GET /api/v1/dk/products` kèm giá
8. **Chốt ADR-008** — nó đã ở trạng thái `Đề xuất` từ 02/09/2026, và mục 6 của
   nó liệt kê đúng bốn thứ còn thiếu. Media là phần cuối của đợt 5
9. **Trả lời Q-3.** Nó không còn xoá được cả `30` nữa — `30` mục 1 cho thấy ba
   trong năm mục sống sót dù trả lời thế nào. Nhưng nó vẫn quyết mục 3 và 4, và
   nó vẫn là câu hỏi rẻ nhất đang treo
10. ~~**M6 và M7** — danh sách đơn và chi tiết đơn~~ — **xong** 04/09/2026.
    Đây là việc kỹ thuật lớn nhất **không phụ thuộc câu hỏi nào đang treo**
11. ~~**Đường ghi của M7** — nhân viên đổi trạng thái đơn~~ — **xong**
    04/09/2026. Còn thiếu **huỷ hàng loạt** theo ngày khởi hành (`14` mục 6.6)
12. ~~**M13** nội dung khác và **M14** người dùng và vai trò~~ — **xong**
    05/09/2026, cùng với **M8 báo giá** (luồng `Quote` nay đã có) và ba trang
    còn thiếu của site khách
13. **Việc kế tiếp không chờ ai**, theo thứ tự đáng làm trước:
    - **Đảo thứ tự `typecheck` và `build` trong `web.yml`** — mục 6.3. Rẻ nhất,
      và nó quyết định tín hiệu CI của PR đầu tiên có đáng tin không
    - **Giới hạn số lần gọi** (`13` mục 8, `22` mục 9, `31` mục 6.2) — vẫn chưa
      có một dòng nào, dù ba tài liệu đều mô tả như đã có
    - **M9 yêu cầu tư vấn** — cần bảng `lead`, chưa có trong lược đồ
    - **Huỷ hàng loạt** theo ngày khởi hành (`14` mục 6.6)
    - Trang chi tiết theo **tab** như `05` mục 4 mô tả; nay là một trang cuộn phẳng
14. **Đưa bản chạy thật lên máy chủ.** Phần code đã xong — ba `Dockerfile`,
    `deploy/`, `trien-khai.yml`. Năm việc còn lại đều cần **quyền trên GitHub
    hoặc trên VPS**, không phải cần viết thêm code:
    - Bật **branch protection** trên `main`, đặt `api`, `web`, `tai-lieu` là
      check bắt buộc. Làm trước tiên: thiếu nó thì pipeline triển khai sẵn sàng
      đẩy một commit đỏ lên máy chủ
    - Điền bốn **Secrets**: `VPS_HOST`, `VPS_USER`, `VPS_PASSWORD`,
      `VPS_KNOWN_HOSTS`. Máy chủ đăng nhập bằng mật khẩu nên pipeline dùng
      `sshpass`; `VPS_KNOWN_HOSTS` vì thế là **bắt buộc**, không phải tuỳ chọn
      (`34` mục 3.1)
    - Điền năm **Variables**: `PUBLIC_SITE_URL`, `PUBLIC_API_URL`,
      `MEDIA_PROTOCOL`, `MEDIA_HOST`, `MEDIA_PORT` — biến chứ không phải bí
      mật, và chúng bị **nướng vào ảnh lúc build** nên phải khớp với `.env`
      trên máy chủ (`34` mục 5.3). Là địa chỉ đầy đủ chứ không phải tên miền,
      nên cùng một pipeline dùng được cho cả `http://<IP>:3000` lẫn
      `https://vidu.com`. Không có gì cho trang quản trị: nó không có biến
      `NEXT_PUBLIC_*` nào
    - Trỏ bốn bản ghi A về máy chủ, tạo `.env` từ `deploy/.env.prod.example`,
      `chmod 600`
    - Chạy `trien-khai.yml` lần đầu và xem nó hỏng ở đâu. Ba `Dockerfile` chưa
      từng build thật, nên lượt đầu hỏng là chuyện bình thường chứ không phải
      dấu hiệu thiết kế sai

---

## 6. Chỗ dễ quên khi làm tiếp

Mục có giá trị nhất của file này. Đây là những thứ đã tốn thời gian một lần rồi.

### 6.1. Máy làm việc — kiểm trước khi tin bất cứ con số xanh nào

Rà đầu ngày 04/09/2026: **cả ba chuỗi kiểm của dự án đều không chạy được** trên
máy đang làm — không phải code hỏng, môi trường đã đổi từ lần chạy 01–02/09.
Dựng lại trong cùng ngày; bảng dưới là trạng thái **sau** khi dựng.

| Cần | Trạng thái cuối ngày 04/09 | Ghi chú |
|---|---|---|
| `pnpm` | ✔ **đã cài** 11.24.0 bằng `npm i -g pnpm` | `corepack` **không có** trên bản Node đang cài (v25.2.1), nên `corepack enable pnpm` như `CLAUDE.md` gốc hướng dẫn không chạy được ở máy này. Trình cài đặt đặt nó ở `%APPDATA%\npm`, thư mục đó có thể **chưa nằm trong PATH** của terminal đang mở — mở lại terminal, hoặc gọi bằng đường dẫn đầy đủ |
| `web/node_modules` | ✔ đã cài | |
| Docker | ✔ đang chạy | Testcontainers dùng được |
| `java` | ✔ 21.0.9 trên PATH | |
| `JAVA_HOME` | **rỗng**, và `~/.gradle/gradle.properties` **không còn** | Gradle rơi về `java` trên PATH — nay là 21 nên vẫn xanh, nhưng cái vá cũ ở mục 6.2 đã biến mất chứ không phải còn đó |
| `python` | ✗ **vẫn chỉ có alias rỗng** của Microsoft Store | `scripts/docs_check.py` không chạy — bộ quy tắc kiểm ở `42` mục 8 vẫn chưa tự thi hành được. Đây là thứ duy nhất còn thiếu |

### 6.3. `pnpm typecheck` cần một lần `build` đi trước

Ở máy local, `pnpm typecheck` chạy một mình sẽ **đỏ** với mọi đường dẫn mới thêm:
`typedRoutes` của Next đọc bảng đường dẫn sinh ra ở `.next/types/routes.d.ts`, và
bảng đó chỉ được dựng lại lúc `build`. Bảng cũ thì đường dẫn mới chưa có tên
trong đó.

Ở CI thì ngược lại, và tệ hơn: `web.yml` chạy `typecheck` **trước** `build`, mà
lần chạy đầu trên máy sạch thì `.next/` chưa tồn tại — không có bảng nào cả, nên
`typedRoutes` **không kiểm gì hết** và mọi `href` sai đều lọt. Đã thử: xoá
`.next/types` rồi chạy `tsc --noEmit` cho `apps/admin` → exit 0.

Cách sửa là đảo thứ tự hai bước trong `web.yml`, hoặc chạy `next typegen` trước
bước kiểm kiểu. Chưa làm — nó không chặn việc gì hôm nay, nhưng nó làm tín hiệu
xanh của CI mất giá trị đúng ở chỗ dễ tin nhất.

### 6.6. Bộ kiểm dữ liệu của `12` mục 9 — nay đã có mã, và nó đã bắt được bốn lỗi thật

**Xong 05/09/2026**: `api/scripts/kiem-nhat-quan.sql` cài cả 23 quy tắc, và
`KiemNhatQuanIT` chạy chúng trong CI trên chính `seed-dev.sql`.

```bash
psql "$DB_URL" -f api/scripts/kiem-nhat-quan.sql     # nhìn bằng mắt
cd api && ./gradlew test --tests "*KiemNhatQuanIT*"  # cổng thật
```

Trước khi có mã, đợt nạp dữ liệu mồi 05/09 phải kiểm bằng SQL viết tại chỗ, và
**ba quy tắc bắt được lỗi thật ngay lần đầu**; lần đóng thành mã bắt thêm cái
thứ tư:

| Quy tắc | Bắt được gì |
|---|---|
| 3 | Ba sản phẩm không có ngày lịch trình nào. Ý định ban đầu là "để trạng thái rỗng có thứ chứng minh" — quy tắc bác, và nó đúng |
| 7 | `CRUISE` chênh giá cabin 15.4% ở bậc đầu (ngưỡng là 20–45%), và một ngày khởi hành chỉ có ba hạng thay vì bốn. Lỗi 15.4% có **từ trước** đợt này |
| 11 | Mô tả ngày lịch trình trung bình 47 ký tự, ngưỡng là 80. Có ngày chỉ vỏn vẹn "Biển." |
| 18 | Điểm đến xoá mềm của bộ dữ liệu mồi không ghi ai xoá — chính cái cột sinh ra để trả lời câu đó |

Quy tắc 11 là cái đáng nhớ nhất: nó không bắt lỗi kỹ thuật mà bắt **nội dung
lấp chỗ trống**, thứ mà đọc code không thấy và mở trang cũng dễ bỏ qua.

Ba điều phải biết khi sửa bộ kiểm:

1. **Nó là MỘT câu lệnh SQL, cố ý.** Tệp chạy ở hai chỗ — `psql` khi làm tay và
   JDBC trong bài test. JDBC không hiểu `\set` hay `\i`, còn tách nhiều câu lệnh
   bằng máy là một chỗ để sai. Đừng thêm lệnh meta của `psql` vào.
2. **Ba quy tắc quét MỌI bảng thay vì liệt kê tên** (16, 17, 18; hai cái đầu và
   cái cuối chạy truy vấn động bằng `query_to_xml`). Lỗi mà chúng bắt chỉ xảy ra
   khi ai đó thêm bảng mới và quên một bước, nên một danh sách tên bảng viết tay
   sẽ bỏ sót đúng cái bảng gây lỗi.
3. **Xanh không chứng minh gì nếu truy vấn gõ sai.** Cách kiểm: phá đúng một dòng
   dữ liệu trong một transaction rồi `ROLLBACK` — cả 23 quy tắc đã được thử như
   thế một lần, và `KiemNhatQuanIT` giữ lại phép thử ấy cho quy tắc 23.

Quy tắc **13 vẫn CẢNH BÁO có chủ ý** (48 ngày khởi hành thiếu giá `CHILD` và
`INFANT`) — nó chờ **Q-2**. Nâng lên `LOI` khi Q-2 xong; bài test sẽ đỏ nếu nó tự
nhiên sạch, để không ai lặng lẽ mất một quy tắc.

### 6.5. `pnpm i18n:check` không bắt được khoá thiếu ở CẢ HAI ngôn ngữ

Bộ kiểm đo **độ phủ của `vi` so với `da`**, tức là nó trả lời "bản dịch có theo
kịp bản nguồn không". Câu hỏi đó không phải câu "mọi khoá code đang gọi đều có
thật không" — một khoá mới gõ trong component mà quên thêm vào **cả hai** catalog
thì độ phủ vẫn 100%, và trang hiện ra nguyên khoá thô.

Đã xảy ra thật ở đợt thiết kế 05/09: bảng ngày khởi hành hiện
`departureStatus.OPEN` giữa cột Status. Chỉ lộ ra khi mở trang bằng mắt.

Cách chặn: bộ kiểm quét mọi lời gọi `t(locale, '…')` với khoá tĩnh và đối chiếu
với catalog `da`. Chưa làm — khoá dựng động (`` t(locale, `detail.tab.${k}`) ``)
làm phép quét đó phức tạp hơn nó đáng, nên tạm thời **mở trang bằng mắt vẫn là
lưới cuối cùng**.

### 6.4. Ngày trên tờ lịch: client dựng nửa đêm ĐỊA PHƯƠNG

Client sinh từ spec dựng trường `format: date` thành nửa đêm **giờ địa phương**
(xem `parseDate` trong runtime của nó). Định dạng lại bằng `timeZone: 'UTC'` là
đổi hệ quy chiếu giữa chừng, và ở phía đông UTC — tức là ở Việt Nam — thì nó lùi
**một ngày**.

Lỗi này đã có thật ở `formatDate` của `packages/ui` và ở hai hàm `ngay()` của
trang quản trị, và nó lặng lẽ: ngày khởi hành trên danh sách đơn lệch một ngày so
với vé. Sửa 05/09, kèm hai test ở `packages/ui` canh cho nó không quay lại.

> Bài học lặp lại của điều kiện treo số 1 (mục 7): **thứ chưa chạy được lúc này
> thì chưa được coi là xanh.** Ba pipeline YAML trông hợp lý mà chưa chạy lần
> nào, và một dòng "tất cả xanh" ghi từ hai ngày trước, là cùng một loại niềm
> tin. Đây cũng là lý do việc số 1 của mục 5 gắn liền với việc dựng lại bộ công
> cụ: mở PR mà máy không chạy được gì thì CI đỏ cũng không sửa tại chỗ được.

### 6.2. Những cái đã vấp

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
  của máy cá nhân (`api/gradle.properties` đã ghi rõ điều này). **Cập nhật
  04/09:** file đó không còn trên máy đang làm và `JAVA_HOME` rỗng; Gradle rơi về
  `java` trên PATH, tình cờ đúng 21 nên vẫn chạy. Máy nào có `JAVA_HOME` trỏ JDK
  cũ thì phải đặt lại — xem mục 6.1

Ba cái từ lần thêm `V2`:

- **Bảng nối có `soft_delete` thì khoá chính không được là khoá kép tự nhiên.**
  `staff_user_role` với khoá `(staff_user_id, role_code)` làm việc **cấp lại một
  vai trò đã thu** trở thành bất khả thi — dòng cũ vẫn chiếm khoá. Khoá thay thế
  cộng index bộ phận `WHERE NOT soft_delete` mới đúng
- **Trigger lưu slug cũ phải dọn dòng nghịch đảo.** Đổi A → B → A mà không xoá
  dòng cũ thì sinh vòng lặp chuyển hướng 301
- **`AFTER UPDATE OF slug`** chứ không phải `AFTER UPDATE`: sửa tiêu đề không
  được sinh dòng lịch sử slug

Ba cái từ đợt 4:

- **CSRF chặn cả đường ghi CÔNG KHAI.** Khách không đăng nhập, không có cookie
  phiên, nên CSRF không bảo vệ gì ở đó — nhưng bật mặc định thì mọi lời gọi giữ
  chỗ và đặt tour trả **401**. Chống gọi lại ở đường công khai là việc của
  `Idempotency-Key`, không phải của CSRF
- **`findAndAddModules()` của Jackson im lặng không tìm thấy gì.** Lỗi chỉ lộ ra
  lúc chạy, ở đúng dòng có kiểu ngày giờ đầu tiên. Đăng ký thẳng `JavaTimeModule`
- **Spring Boot 4 không còn bean `ObjectMapper` để tiêm.** Lớp cần JSON riêng thì
  tự dựng — và với chỗ băm vân tay yêu cầu thì tự dựng còn **đúng hơn**: dùng
  chung cấu hình với tầng web nghĩa là đổi cách trả JSON sẽ làm mọi vân tay cũ
  hết khớp, và mọi khoá đang sống mất tác dụng

Ba cái từ đợt 2:

- **CSRF chặn lời gọi của người CHƯA đăng nhập thì ra 401, không phải 403.**
  Spring dịch `AccessDeniedException` của người ẩn danh thành entry point. Triệu
  chứng: đăng nhập đúng mật khẩu vẫn 401, và log xác thực sạch trơn vì yêu cầu
  chưa bao giờ tới controller
- **`changeSessionId()` ném lỗi khi chưa có phiên nào** — mà lời gọi đăng nhập
  đầu tiên chính là lúc đó. Phải `getSession(true)` nếu chưa có
- **`@CreatedDate` trên `OffsetDateTime` nổ** với *"Cannot convert unsupported
  date type LocalDateTime"*. Không vá bằng `DateTimeProvider` mà bỏ hẳn: cả
  `created_at` lẫn `last_modified_at` đều để **cơ sở dữ liệu** sở hữu, đúng như
  `11` mục 11.1 đã ghi

Hai cái từ đợt 1b:

- **`GUARANTEED` xét TRƯỚC `FEW_SEATS`** (`14` mục 5) — và chính tôi đặt kỳ vọng
  sai trong test đầu tiên: một chuyến 18/20 khách với ngưỡng đảm bảo 12 ra
  `GUARANTEED`, không phải `FEW_SEATS`. Đảo hai bước này không làm gãy gì, chỉ
  làm mất doanh thu âm thầm
- **Lọc theo thẻ hoặc chủ đề phải dùng `EXISTS`, không `JOIN`.** Với `JOIN` thì
  bản ghi mang hai thẻ đang lọc xuất hiện **hai lần** và `totalItems` đếm sai
  theo. Có test riêng cho đúng trường hợp đó

Ba cái từ đường ghi của M7:

- **Thân yêu cầu sai kiểu trả 500, không phải 400** — và chuyện này đúng với
  **mọi** endpoint nhận JSON, không riêng cái vừa làm. Jackson ném
  `HttpMessageNotReadableException` **trước khi** controller chạy, nên `@Valid`
  không bao giờ thấy nó và nó rơi xuống bộ bắt cuối. Lộ ra nhờ một bài test gửi
  `toStatus: "EXPIRED"` — giá trị enum ngoài spec. Đã thêm vào bộ bắt
  `VALIDATION_FAILED`
- **`seats_booked` cộng theo tổng bản đồ `pax`, nhưng trừ theo số dòng
  `booking_passenger`.** Hai con số này đáng lẽ luôn bằng nhau, mà **chưa có
  ràng buộc nào bắt buộc thế**: `BookingService` không kiểm
  `passengers.size() == tongSoKhach()`. Lệch một lần là bộ đếm tồn kho lệch mãi.
  Câu `UPDATE` đã bọc `GREATEST(…, 0)` để không ra số âm, nhưng đó là lưới an
  toàn chứ không phải cách sửa — chỗ sửa thật là thêm phép kiểm lúc đặt
- **Khoá bi quan là thứ duy nhất chặn hai nhân viên cùng bấm một nút.** Thiếu
  `FOR UPDATE` trên dòng `booking` thì cả hai cùng đọc `CONFIRMED`, cùng thấy
  bước chuyển hợp lệ, và một lần huỷ trừ chỗ **hai lần**. Máy trạng thái không
  cứu được: nó là hàm thuần, nó chỉ biết cái nó được cho xem

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
điều kiện treo số 3 — **đã sửa ngày 02/09/2026**.

**Ba điều kiện treo**

| # | Điều kiện | Ai chịu trách nhiệm | Trạng thái |
|---|---|---|---|
| 1 | Đẩy repo lên GitHub, mở một PR thật, xác nhận bộ lọc đường dẫn trên runner | KTS (đẩy) + VH (`A` của dòng CI/CD ở `40` mục 5) | **Còn treo.** Repo đã đẩy 01/09; PR chưa mở — `gh` chưa cài trên máy đang làm |
| 2 | Viết `34-cicd-va-moi-truong` bản nháp — nằm trong danh sách **Bàn giao** của G2 mà chưa có file | VH | **Xong** 02/09/2026 |
| 3 | Sửa câu chữ tiêu chí 5 của `40` cho khớp cơ chế thật (`40` mục 7 dòng 1: làm rõ câu chữ, không cần PTĐ) | KTS | **Xong** 02/09/2026 |

> Điều kiện 1 hoá ra còn đắt hơn tưởng: `api.yml` vẫn gọi `./gradlew archTest` và
> `./gradlew :web:contractsGenerate`, hai thứ ADR-010 đã xoá. Nghĩa là pipeline
> backend **sẽ đỏ ngay bước đầu** nếu chạy thật. Đã sửa ngày 02/09, nhưng nó nhắc
> đúng điều mà tiêu chí này tồn tại để nhắc: CI chưa chạy lần nào thì CI chưa
> tồn tại, dù ba file YAML trông rất hợp lý.

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
| 01/09/2026 | Chạy cổng G2 — qua có điều kiện, 6/7. Commit 6 lần, đẩy nhánh `dung-khung-va-loi-danh-muc` | Chưa mở PR: máy chưa cài `gh` |
| 01/09/2026 | Viết `15-ke-hoach-dung-be` sau khi đối chiếu repo `comic-social-network-be`; làm đợt 1a: API điểm đến, spec v0.3 | Đợt 1b chặn ở `12` thiếu bảng nội dung |
| 01/09/2026 | Bổ sung `12` mục 4.8–4.11 và viết `V3`: 17 bảng nội dung biên tập, 12 test | `site_info` vẫn để ngỏ; chưa endpoint nào dùng 17 bảng này |
| 01/09/2026 | Đợt 1b: spec v0.4, 7 endpoint đọc còn thiếu, giải trạng thái ngày khởi hành ở `domain` | Xong danh mục đọc của `13` mục 9.1 trừ `/site-info`. Tổng 102 test |
| 01/09/2026 | Đợt 2: spec v0.5, Spring Security phiên cookie, entity JPA đầu tiên, MapStruct, `AuditorAware` | Tổng 114 test. Ba dependency mới, đều do tài liệu đã chốt |
| 01/09/2026 | Đợt 3: engine giá thuần `domain` — 31 test, không context, không CSDL, không đồng hồ | Tổng 145 test. Chưa có endpoint tính giá; `VN` vẫn chờ Q-2 |
| 01/09/2026 | Đợt 4: spec v0.6, `V4`, khoá bi quan, `Idempotency-Key`, máy trạng thái đơn, job quét có ShedLock | Tổng 173 test. `docs/14` mục 9.3 tick đủ |
| 01/09/2026 | ADR-010: gộp bốn module thành một, chia theo feature như `comic-social-network-be` | 167 test (mất 6 test ranh giới cùng `archTest`) |
| 02/09/2026 | Viết ADR-007, ADR-008, ADR-009 và hai tài liệu `30`, `31` | Cả ba ADR ở `Đề xuất` — không tự chốt được; `31` cần luật sư duyệt |
| 02/09/2026 | Vá lỗ hổng CSRF ở bề mặt quản trị — mẫu `*` khớp cả `admin` | Đường ghi công khai vẫn miễn CSRF theo chủ ý (mục 6.2) |
| 02/09/2026 | Đợt 5a: ba màn hình đọc của trang quản trị, viết `34-cicd-va-moi-truong`, sửa câu chữ tiêu chí 5 của `40` | Gỡ được điều kiện treo 2 và 3 của G2; điều kiện 1 (PR) còn nguyên |
| 02/09/2026 | Đợt 5b: đường ghi của trang quản trị, migration `V5` cho `price_from`, tài khoản mồi bốn vai trò | Tiêu chí ra số 7 của G4 có test chứng minh; media vẫn chờ Q-6 |
| 02/09/2026 | Giao diện trang quản trị: đăng nhập, bảng điều khiển, danh sách, tạo tour, sửa 4 tab | Vòng mở bán khép kín từ giao diện, không cần lập trình viên |
| 02/09/2026 | Web khách: trang điểm đến, trang tìm tour, sitemap theo locale + `robots.txt`, chuyển hướng slug cũ **308** | Gạch xong việc số 6 của mục 5 |
| 03/09/2026 | Web khách: luồng đặt tour bốn bước và trang xác nhận | Phần code của G3 xong; còn chờ nội dung thật (Q-1) |
| 04/09/2026 | Rà soát toàn repo đối chiếu với file này; sửa 4 chỗ lệch: số test, số file tài liệu, việc số 6 của mục 5, nhật ký phiên | Phát hiện **bộ công cụ kiểm trên máy đã hỏng** — thêm mục 6.1. Chưa tái kiểm được test nào |
| 04/09/2026 | Dựng lại bộ công cụ: cài `pnpm` 11.24.0, `pnpm install`, bật Docker | Chỉ còn Python là thiếu |
| 04/09/2026 | **M6 + M7 — vận hành đơn.** Spec v0.11, hai endpoint đọc, `AdminBookingRepository/Service`, hai màn hình `admin/don`, 17 test mới | Lỗ hổng "nhận được đơn mà không vận hành được đơn" đã khép ở phần **xem**. Phần **đổi trạng thái** vẫn chưa có — xem mục 3 |
| 04/09/2026 | **Đường ghi của M7.** Spec v0.12, `POST /admin/bookings/{reference}/status`, khoá bi quan, trả chỗ về kho, khối thao tác có ô xác nhận nói rõ hậu quả. 8 test mới, tổng **228** | Lộ ra hai lỗi có sẵn: thân JSON sai kiểu trả 500 ở **mọi** endpoint (đã sửa), và `seats_booked` cộng/trừ theo hai nguồn khác nhau (chưa sửa — mục 6.2). Còn thiếu huỷ hàng loạt theo chuyến (`14` mục 6.6) |
| 06/09/2026 | **Q-6 chốt — ADR-011: MinIO tự dựng cho cả dev và prod**, thay thế đề xuất "kho có quản" của ADR-008. Cập nhật `10` mục 10 và 11 | Chưa viết dòng code nào. Bảy hệ quả ở ADR-011 mục 6 chưa làm; nặng nhất là **sao lưu ảnh** (`35` chưa viết) |
| 06/09/2026 | **Bộ ảnh — đường ĐỌC end-to-end.** MinIO vào `compose.yaml`; spec v0.15.0 thêm `gallery` và `layout`; `V7` thêm `product.layout`; 29 `media_asset` + 61 `product_image` vào dữ liệu mồi; khối bộ ảnh ở trang chi tiết. 4 test mới, tổng **290** | Đường GHI chưa có: chưa xin được dependency S3, nên biên tập viên chưa tự thêm ảnh — ảnh do `scripts/nap-anh-len-kho.py` nạp. Ô chọn template ở trang quản trị và các template chưa dựng |
| 07/09/2026 | **Guard `known_hosts` hoá ra không guard gì.** `test -s` cho qua khi secret chưa khai, vì `printf '%s
' ""` vẫn ghi một dòng trắng = 1 byte. Thay bằng `grep -q '[^[:space:]]'`, và thêm `ssh-keygen -F` để xác nhận có dòng khớp ĐÚNG host đang gọi | Bài học lặp lại lần thứ hai trong cùng một ngày: guard viết xong phải chạy thử ca hỏng, không chỉ ca chạy. Lần trước là `eval "v=$b"` |
| 07/09/2026 | **Lượt `trien-khai` đầu tiên — đỏ.** Variables chưa đặt nên build-arg rỗng, và build-arg rỗng ghi đè mặc định của `ARG`; `next build` chết với `Expected 'http' | 'https', received ''`. Thêm bước kiểm năm Variables ở đầu job `anh`, và đổi `??` sang `||` ở ba chỗ đọc biến bị nướng vào ảnh | Guard bản đầu dùng `eval "v=$b"` — gán TÊN biến chứ không gán giá trị, nên nó luôn xanh. Đã thay bằng năm dòng thẳng và thử cả ba ca |
| 07/09/2026 | **Đóng gói và triển khai.** Ba `Dockerfile` hai giai đoạn; `output: 'standalone'` + `outputFileTracingRoot` cho hai app Next; `deploy/compose.prod.yaml`, `Caddyfile`, `.env.prod.example`; `.github/workflows/trien-khai.yml`. Cookie `Secure` và HTTPS sau proxy hoá ra chỉ là bốn thuộc tính Spring, không phải sửa code | **Chưa build ảnh nào**: VM Docker trên máy không ra được registry. Chưa chạy pipeline, chưa điền bí mật. PR gộp vào `main` **vẫn chưa mở** — thử gọi API GitHub bằng credential đã lưu thì bị chặn, `gh` thì chưa cài |
| 06/09/2026 | **Ảnh minh hoạ cho điểm đến.** Spec v0.16.0 thêm `Destination.image`; `V8` thêm bảng `destination_image` cùng khuôn với `product_image`; 10 điểm đến có ảnh. Đọc bằng `LEFT JOIN LATERAL` để không sinh N+1. 3 test mới, tổng **293** | Không thêm cột ảnh dạng chữ như `hotel.image` — `12` mục 10 đã ghi khuôn đó vào danh sách nợ. Trang chi tiết điểm đến chưa dùng ảnh; màn quản lý ảnh vẫn chờ dependency S3 |
