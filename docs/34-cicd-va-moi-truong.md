# CI/CD và môi trường

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: các môi trường chạy được, ba pipeline và điều kiện kích hoạt,
                  biến môi trường và bí mật, cách đóng gói và triển khai, thứ tự
                  chạy migration lúc triển khai, cách lùi một lần phát hành.
Không nói về: quy ước viết file migration và quy tắc đổi phá vỡ tương thích
              (12 mục 8), chiến lược test theo tầng (33), giám sát và sao lưu
              (35), bảo vệ dữ liệu cá nhân (31), quy trình tài liệu (42).
```

Tài liệu này trả lời: **code đi từ máy lập trình viên tới khách bằng đường nào,
và khi có sự cố thì lùi lại bằng cách nào.**

> **Cảnh báo trạng thái.** Mục 2, 3 và 4 mô tả thứ **đã dựng và chạy được**.
> Mục 5, 6 và 7 mô tả thứ **đã thiết kế nhưng chưa dựng** — chưa có `Dockerfile`,
> chưa có máy chủ. Mỗi bảng ghi rõ cột trạng thái; đừng đọc tài liệu này như thể
> hệ thống đã triển khai.

---

## 1. Môi trường

| Môi trường | Chạy ở đâu | CSDL | Ai dùng | Trạng thái |
|---|---|---|---|---|
| `dev` | Máy lập trình viên | `compose.yaml` → Postgres 16 cục bộ | Lập trình viên | ✔ chạy được |
| `test` | Runner GitHub Actions | Testcontainers dựng rồi xoá theo từng lượt chạy | CI | ✔ chạy được |
| `staging` | Chưa có | Chưa có | Chưa có | ✗ **Q-9** |
| `prod` | VPS | Postgres 16 trên cùng máy chủ | Khách và nhân viên | ✗ chưa dựng |

Ba điều cố ý:

1. **`test` không dùng CSDL cài sẵn.** Testcontainers dựng Postgres thật cho mỗi
   lượt chạy. Đây là khoản không mượn của `comic-social-network-be` (`15` mục 3):
   bên đó test cần Postgres ở `localhost:5432`, ở đây test chạy được trên máy
   sạch và trên runner mà không ai phải cài gì trước.
2. **Không dùng H2 ở bất kỳ môi trường nào.** H2 không mô phỏng được ICU
   collation và `unaccent` — đúng hai thứ dễ sai nhất của dự án này (`12` mục 1).
3. **`staging` chưa tồn tại và điều đó là một rủi ro có thật**, không phải một
   khoảng trống vô hại. Xem mục 9.

---

## 2. Ba pipeline

Tách theo **vùng của repo**, không gộp thành một workflow lớn. Sửa một dòng chữ
trong `docs/` không có lý do gì phải dựng Postgres và chạy 167 bài test.

| Workflow | Kích hoạt khi sửa | Làm gì | Thời gian trần |
|---|---|---|---|
| `api.yml` | `api/**` · `contracts/**` | `./gradlew build` (biên dịch + toàn bộ test, gồm Testcontainers) rồi sinh lại interface từ spec | 30 phút |
| `web.yml` | `web/**` · `contracts/**` | Sinh TS client → `typecheck` → `lint` → `test` → `i18n:check` → `build`; thêm job kiểm tương thích ngược của hợp đồng, **chỉ trên pull request** | 20 phút |
| `tai-lieu.yml` | `docs/**` · `scripts/docs_check.py` | `python scripts/docs_check.py` | 5 phút |

Mỗi workflow còn tự kích hoạt khi chính file của nó bị sửa — đổi pipeline mà
pipeline không chạy là cách chắc chắn để đẩy lỗi cấu hình lên `main`.

### 2.1. Vì sao `contracts/` kích hoạt **cả hai**

`openapi.yaml` là nguồn sự thật của hợp đồng (ADR-002), và **hai bên cùng sinh
code từ nó**: phía Java sinh interface mà controller `implements`, phía TS sinh
client mà frontend gọi. Sửa spec là đổi cả hai bên cùng lúc.

Chạy một bên thôi thì lỗi của bên kia chỉ lộ ra ở lần merge sau, và lúc đó không
ai còn nhớ commit nào gây ra.

Đây cũng chính là **tiêu chí ra số 7 của cổng G2** (`40` mục 4), và nó phải được
xác nhận trên một pull request thật, không phải bằng mô phỏng trên máy: sửa
`web/` **không** được kích hoạt build Gradle.

### 2.2. Ba chi tiết đã trả giá để biết

| Chi tiết | Vì sao |
|---|---|
| **GitHub Actions không hỗ trợ neo YAML** (`&`/`*`) | Danh sách `paths` phải viết lặp hai lần, một cho `push` một cho `pull_request`. Sửa một chỗ mà quên chỗ kia là pipeline chạy đúng trên PR rồi im lặng bỏ qua trên `main` |
| **`web.yml` vẫn cần JDK 21** | `openapi-generator-cli` là công cụ Java. Thiếu JDK thì bước sinh TS client chết, dù đây là pipeline frontend |
| **Job kiểm hợp đồng cần `fetch-depth: 0`** | Nó đọc `openapi.yaml` của nhánh đích bằng `git show`; checkout một commit là không có gì để so |

### 2.3. Cái gì làm đỏ, cái gì không

| Kết quả | Làm đỏ CI |
|---|---|
| Test đỏ, biên dịch đỏ, `typecheck` đỏ | Có |
| Thiếu khoá chuỗi giao diện (`i18n:check`) | **Có** — `02` mục 4.3 |
| Hợp đồng phá vỡ tương thích ngược | Có |
| **Lỗi** của bộ kiểm tài liệu | Có |
| **Cảnh báo** của bộ kiểm tài liệu | Không |

Dòng cuối có chủ ý. "Tài liệu chưa viết" là cảnh báo suốt cả dự án; để nó chặn
merge thì cả đội sẽ học cách phớt lờ màu đỏ, và ngày màu đỏ thật xuất hiện sẽ
không ai nhìn.

### 2.4. Cái CI **không** làm

Không tự sinh lại code rồi commit ngược vào repo. Code sinh từ `openapi.yaml`
**không nằm trong git** (`CLAUDE.md` điều 9): nó sinh lại ở mỗi lượt build, ở cả
hai phía. Bot commit vào nhánh là cách nhanh nhất để có hai nguồn sự thật.

---

## 3. Biến môi trường

Ứng dụng đọc bốn biến, tất cả đều có giá trị mặc định dùng được cho `dev`:

| Biến | Mặc định | Bí mật |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/travel` | Không |
| `DB_USER` | `travel` | Không |
| `DB_PASSWORD` | `travel` | **Có** ở `prod` |
| `PORT` | `8080` | Không |

Phía `web/` có thêm hai biến:

| Biến | Mặc định | Dùng ở |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Site khách gọi API từ phía máy chủ |
| `NEXT_PUBLIC_SITE_URL` | `http://localhost:3000` | Sitemap và `robots.txt` — **URL tuyệt đối** |

Mặc định dùng được là có chủ ý: người mới `git clone` rồi `docker compose up -d`
rồi `./gradlew bootRun` là chạy. Bắt phải có file `.env` trước khi khởi động
được lần đầu chỉ làm chậm mọi người, mà không an toàn hơn chút nào — mật khẩu
`travel/travel` cho Postgres cục bộ không phải là bí mật.

**Ở `prod` thì ngược lại: không có mặc định nào được dùng.** Mục 5.2.

### 3.1. Bí mật

| Bí mật | Dùng ở đâu | Trạng thái |
|---|---|---|
| `DB_PASSWORD` | `prod` | ✗ chưa có máy chủ |
| Khoá đăng nhập registry ảnh | Pipeline triển khai | ✗ chưa dựng |
| Khoá SSH triển khai | Pipeline triển khai | ✗ chưa dựng |
| Khoá cổng thanh toán | `30` | ✗ **Q-3** chưa trả lời |
| Khoá lưu trữ ảnh | ADR-008 | ✗ **Q-6** chưa trả lời |

Quy tắc, áp ngay từ bí mật đầu tiên:

- Bí mật nằm trong GitHub Secrets, **không** trong repo, **không** trong ảnh
  Docker, **không** trong biến build
- Bí mật của `prod` không bao giờ có mặt ở `dev` hay `test`
- Không ghi bí mật ra log. `logging.level` đang là `DEBUG` cho
  `vn.travel.booking` — trước khi lên `prod` phải hạ xuống `INFO` (mục 5.2)
- Lộ bí mật thì **xoay khoá**, không phải xoá commit. Commit xoá rồi vẫn còn
  trong bản clone của người khác

---

## 4. Migration

Quy ước viết file migration ở `12` mục 8 — không chép lại ở đây. Phần thuộc về
tài liệu này là **lúc nào nó chạy**.

| | |
|---|---|
| Chạy bằng gì | `spring-boot-flyway`, lúc **ứng dụng khởi động** |
| Không chạy bằng gì | Plugin Flyway của Gradle. Dự án **không** áp plugin đó, nên **không có task `flywayMigrate`** |
| Ở `dev` | `cd api && ./gradlew bootRun` |
| Ở `prod` | Container mới khởi động → Flyway chạy → ứng dụng nhận traffic |

`spring.flyway.baseline-on-migrate` để `false` có chủ ý: gặp một CSDL có sẵn bảng
mà không có bảng lịch sử Flyway thì phải **dừng lại và đỏ**, không được lặng lẽ
coi lược đồ hiện có là điểm xuất phát.

### 4.1. Hệ quả: một instance, cho tới khi có quyết định khác

Migration chạy lúc khởi động nghĩa là **hai container khởi động cùng lúc sẽ cùng
chạy migration**. Flyway có khoá cấp CSDL nên không hỏng dữ liệu — container thứ
hai chờ rồi thấy không còn gì để chạy — nhưng nó biến thứ tự triển khai thành thứ
phụ thuộc vào thời điểm, và đó là loại lỗi chỉ xuất hiện lúc tải cao.

Chạy nhiều instance thì phải tách migration thành **một bước riêng trước khi
triển khai**, không để ứng dụng tự chạy. Việc đó chưa làm vì **Q-5** chưa trả
lời. Ghi ở đây để lúc trả lời Q-5 thì biết phải sửa cái gì.

Ghi chú: ShedLock (`14` mục 6.4) giải bài toán **job nền** chạy trên nhiều
instance, không giải bài toán migration. Hai thứ khác nhau, đừng lẫn.

---

## 5. Đóng gói và triển khai

> **Chưa dựng.** Mục này là thiết kế, kế thừa khuôn đã chạy thật của
> `comic-social-network-be` (`15` mục 2, dòng cuối).

### 5.1. Đường đi

```
đẩy lên main
  → api.yml và web.yml xanh
  → build ảnh Docker hai giai đoạn
  → đẩy lên registry
  → SSH vào VPS: docker compose pull && docker compose up -d
  → container mới khởi động, Flyway chạy, nhận traffic
```

Hai giai đoạn trong `Dockerfile` vì ảnh cuối **không được chứa Gradle, JDK đầy
đủ, mã nguồn hay bí mật build**: giai đoạn một biên dịch, giai đoạn hai chỉ chép
file `jar` sang một ảnh JRE.

### 5.2. Bắt buộc trước lần triển khai `prod` đầu tiên

Danh sách này là **điều kiện chặn**, không phải gợi ý:

- [ ] `DB_PASSWORD` thật, không phải `travel`
- [ ] `logging.level.vn.travel.booking` hạ từ `DEBUG` xuống `INFO`
- [ ] `NEXT_PUBLIC_SITE_URL` trỏ tên miền thật. Sitemap và `robots.txt` dùng URL
      tuyệt đối; để nguyên `localhost` thì công cụ tìm kiếm bỏ qua **toàn bộ**,
      và không có lỗi nào nổ
- [ ] Cookie phiên đặt `Secure` — hiện mới có `HttpOnly` + `SameSite=Lax`
      (`22` mục 9)
- [ ] HTTPS, và HTTP chuyển hướng sang HTTPS
- [ ] Sao lưu CSDL có lịch và **đã thử phục hồi một lần** (`35`)
- [ ] `30`, `31`, `32` ở trạng thái `Đã duyệt` — cổng G5 (`40`)

Dòng thứ năm là dòng hay bị bỏ qua nhất: bản sao lưu chưa từng phục hồi thử thì
chưa phải là bản sao lưu, nó chỉ là một file.

---

## 6. Lùi một lần phát hành

**Lùi code dễ, lùi lược đồ thì không.** Đây là bất đối xứng quan trọng nhất của
mục này.

| Tình huống | Cách lùi |
|---|---|
| Bản mới lỗi, migration **không** đổi lược đồ | Triển khai lại ảnh cũ. Xong |
| Bản mới lỗi, migration **có** đổi lược đồ nhưng tương thích ngược | Triển khai lại ảnh cũ. Cột mới thừa ra, không ai đọc, dọn ở lần sau |
| Bản mới lỗi, migration **phá vỡ** tương thích | Không lùi được. Phải sửa tới |

Dòng thứ ba là lý do `12` mục 8 bắt **tách mọi đổi phá vỡ tương thích thành hai
lần triển khai**. Quy tắc đó trông như thủ tục thừa cho tới đúng lần đầu tiên
phải lùi lúc hai giờ sáng.

**Không rollback ngược bằng migration `U`.** Sửa thì viết migration `V` mới. Một
kịch bản lùi được viết sẵn nhưng chưa bao giờ chạy thì lúc cần dùng nó cũng hỏng.

Ví dụ có thật đang chờ trong dự án này: đổi `product.hero_image` từ `TEXT` sang
khoá ngoại trỏ `media_asset` (`12` mục 10). Đó là đổi phá vỡ tương thích, và nó
**phải** đi theo bốn bước — thêm cột → ghi cả hai chỗ → chuyển dữ liệu → lần
triển khai sau mới bỏ cột cũ.

---

## 7. Cấm

- Triển khai từ máy cá nhân, bỏ qua CI
- Commit code sinh từ `openapi.yaml` vào git
- Bí mật trong repo, trong ảnh Docker, hoặc trong biến build
- `ddl-auto` khác `validate` ở bất kỳ môi trường nào
- Sửa một file migration đã chạy ở bất kỳ môi trường nào
- Sửa lược đồ `prod` bằng `psql` tay
- Bỏ qua CI đỏ bằng cách merge thẳng
- Dùng H2 thay Postgres trong test
- Ảnh Docker gắn nhãn `latest` để triển khai — không truy ngược được bản nào
  đang chạy

---

## 8. Việc còn thiếu để tài liệu này thành sự thật

| # | Việc | Chặn ai |
|---|---|---|
| 1 | Đẩy repo lên GitHub, mở một PR thật, xác nhận bộ lọc đường dẫn chạy đúng trên runner | Điều kiện treo số 1 của cổng G2 (`41` mục 7) |
| 2 | Viết `Dockerfile` hai giai đoạn cho `api/` và cho hai app Next.js | Mục 5 |
| 3 | Dựng VPS, cài Docker, tạo CSDL `prod` | Mục 1, mục 5 |
| 4 | Workflow triển khai và ba bí mật của nó | Mục 3.1 |

---

## 9. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| **Có môi trường `staging` không** — không có thì lần chạy thật đầu tiên của mọi migration là trên dữ liệu khách | Mục 1 | **Q-9**, đề nghị thêm vào `41` mục 4 |
| Một hay nhiều instance | Mục 4.1 | **Q-5** ở `41` mục 4 |
| Registry ảnh: GHCR hay registry riêng trên VPS | Mục 5.1 | Kiến trúc sư |
| Triển khai không gián đoạn, hay chấp nhận vài giây tắt | Mục 5.1 — quyết định này ràng buộc mục 4.1 | **Q-4** (quy mô) |
| Cổng thanh toán cần webhook, tức là cần URL công khai ổn định từ trước | `30` | **Q-3** |
