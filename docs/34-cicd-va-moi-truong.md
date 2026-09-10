# CI/CD và môi trường

```
Trạng thái: Nháp
Cập nhật: 09/09/2026
Nguồn sự thật về: các môi trường chạy được, ba pipeline và điều kiện kích hoạt,
                  biến môi trường và bí mật, cách đóng gói và triển khai, thứ tự
                  chạy migration lúc triển khai, cách lùi một lần phát hành.
Không nói về: quy ước viết file migration và quy tắc đổi phá vỡ tương thích
              (12 mục 8), chiến lược test theo tầng (33), giám sát và sao lưu
              (35), bảo vệ dữ liệu cá nhân (31), quy trình tài liệu (42).
```

Tài liệu này trả lời: **code đi từ máy lập trình viên tới khách bằng đường nào,
và khi có sự cố thì lùi lại bằng cách nào.**

> **Cảnh báo trạng thái.** Mục 2, 3, 4 và 5 mô tả thứ **đã có mã**: ba pipeline
> kiểm, ba `Dockerfile`, ba file compose trong `deploy/`, `deploy/Caddyfile` và
> `trien-khai.yml`.
>
> `trien-khai.yml` **đã chạy thật** và đã đưa được ba ảnh lên VPS. Cái **chưa
> xảy ra** là một lượt triển khai đi tới cuối: `api` chưa khởi động thành công
> lần nào, vì ba điều kiện của CSDL dùng chung ở mục 5.0.2. Chưa có tên miền nào
> trỏ về đâu. Mục 6 vẫn là lý thuyết — kịch bản lùi chưa từng chạy, và chính mục
> 6 nói: một kịch bản chưa chạy bao giờ thì lúc cần cũng hỏng.

---

## 1. Môi trường

| Môi trường | Chạy ở đâu | CSDL | Ai dùng | Trạng thái |
|---|---|---|---|---|
| `dev` | Máy lập trình viên | `compose.yaml` → Postgres 16 cục bộ | Lập trình viên | ✔ chạy được |
| `test` | Runner GitHub Actions | Testcontainers dựng rồi xoá theo từng lượt chạy | CI | ✔ chạy được |
| `staging` | Chưa có | Chưa có | Chưa có | ✗ **Q-9** |
| `prod` | VPS | Container `shared-postgres` **của dự án khác** — mục 5.0.2 | Khách và nhân viên | ✗ chưa dựng |

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

Ứng dụng đọc mười bốn biến, tất cả đều có giá trị mặc định dùng được cho `dev`:

| Biến | Mặc định | Bí mật |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/travel` | Không |
| `DB_USER` | `travel` | Không |
| `DB_PASSWORD` | `travel` | **Có** ở `prod` |
| `PORT` | `8080` | Không |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | Không |
| `STORAGE_PUBLIC_BASE_URL` | `http://localhost:9000/travel-media` | Không |
| `TRAVEL_STORAGE_ENDPOINT_PUBLIC` | `http://localhost:9000` | Không |
| `TRAVEL_STORAGE_ENDPOINT_INTERNAL` | `http://localhost:9000` | Không |
| `TRAVEL_STORAGE_BUCKET` | `travel-media` | Không |
| `TRAVEL_STORAGE_ACCESS_KEY` | `travel` | **Có** ở `prod` |
| `TRAVEL_STORAGE_SECRET_KEY` | `travel-dev-secret` | **Có** ở `prod` |
| `BOOTSTRAP_ADMIN_EMAIL` | rỗng — không tạo gì | Không |
| `BOOTSTRAP_ADMIN_PASSWORD` | rỗng — không tạo gì | **Có** ở `prod` |
| `BOOTSTRAP_ADMIN_NAME` | `Quản trị viên` | Không |

**`CORS_ALLOWED_ORIGINS` phải liệt kê HAI gốc, không phải một** — chú ý rằng
mặc định của `dev` cũng có hai. Gốc thứ hai là trang quản trị, và nó cần thiết
kể cả khi trang quản trị gọi API qua proxy `rewrites()` của Next: request tới
API mang URL nội bộ `http://api:8080`, còn header `Origin` mà Next chuyển tiếp
là địa chỉ công khai của trang quản trị. Hai giá trị đó không bao giờ bằng nhau,
nên Spring xếp lời gọi vào diện CORS và trả `403 Invalid CORS request`. Triệu
chứng: đăng nhập trang quản trị hỏng, còn log API **không ghi một dòng nào** —
bộ lọc CORS chặn trước khi tới controller.

Thêm bốn thuộc tính chuẩn của Spring mà **chỉ `prod` mới đặt**, và cả bốn đều là
cấu hình chứ không phải sửa code:

| Biến | Giá trị ở `prod` | Vì sao |
|---|---|---|
| `LOGGING_LEVEL_VN_TRAVEL_BOOKING` | `INFO` | Hạ từ `DEBUG` — mục 5.2 |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `framework` | Bảo Spring tin `X-Forwarded-*` của proxy. Thiếu nó thì `request.isSecure()` trả `false` vì chặng cuối là HTTP trong mạng nội bộ, và cookie CSRF không được đánh dấu `Secure` dù khách đang dùng HTTPS |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` | Chính là ô "cookie phiên đặt `Secure`" của mục 5.2. Phiên là `JSESSIONID` của servlet, nên cờ này là thuộc tính, không phải mã |
| `SERVER_SERVLET_SESSION_COOKIE_SAME_SITE` | `lax` | Giữ nguyên điều `22` mục 9 đã chốt, nhưng nói ra thay vì dựa vào mặc định |

Phía `web/` có thêm hai biến:

| Biến | Mặc định | Dùng ở |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Site khách gọi API. **Cả từ trình duyệt**: `bookingApi()` chạy phía khách, nên đây phải là tên miền công khai và nó phải có trong `CORS_ALLOWED_ORIGINS` |
| `NEXT_PUBLIC_SITE_URL` | `http://localhost:3000` | Sitemap và `robots.txt` — **URL tuyệt đối** |
| `NEXT_PUBLIC_STORAGE_PROTOCOL` · `_HOST` · `_PORT` | `http` · `localhost` · `9000` | `remotePatterns` của `next/image` — `20` |
| `API_URL` | `http://localhost:8080` | **Chỉ trang quản trị.** Đích của `rewrites()`; là địa chỉ NỘI BỘ giữa hai container, không phải tên miền công khai |

Cả năm biến này **bị nướng vào mã lúc build**, không đọc lúc chạy. Hệ quả ở mục 5.3.

Mặc định dùng được là có chủ ý: người mới `git clone` rồi `docker compose up -d`
rồi `./gradlew bootRun` là chạy. Bắt phải có file `.env` trước khi khởi động
được lần đầu chỉ làm chậm mọi người, mà không an toàn hơn chút nào — mật khẩu
`travel/travel` cho Postgres cục bộ không phải là bí mật.

**Ở `prod` thì ngược lại: không có mặc định nào được dùng.** Mục 5.2.

### 3.1. Bí mật

| Bí mật | Cất ở đâu | Trạng thái |
|---|---|---|
| `DB_PASSWORD` | `.env` trên máy chủ, `chmod 600` | ✔ có chỗ, chờ giá trị thật |
| `MINIO_ROOT_PASSWORD` | `.env` trên máy chủ | ✔ có chỗ, **đang dùng thật** |

> **`MINIO_ROOT_USER` và `MINIO_ROOT_PASSWORD` từ đợt tải ảnh lên đã có tác
> dụng thật.** Trước đó chúng nằm trong `.env` mà không service nào đọc, vì bản
> chạy thật dùng MinIO có sẵn của dự án khác. Nay `api` ký URL tải lên bằng đúng
> cặp khoá này (ADR-013), nên đổi mật khẩu MinIO là phải đổi ở cả hai chỗ.
| Khoá đăng nhập registry | **Không tồn tại** — xem dưới | ✔ không cần |
| `VPS_HOST` · `VPS_USER` · `VPS_PASSWORD` | GitHub Secrets | ✔ pipeline đã đọc, chờ điền |
| Khoá cổng thanh toán | `30` | ✗ **Q-3** chưa trả lời |

**Ba chỗ lệch với khuôn thường gặp, cả ba đều có lý do.**

*Mật khẩu CSDL không nằm trong GitHub Secrets.* Nó nằm trong `.env` trên chính
máy chủ, và pipeline không bao giờ nhìn thấy nó. Đưa nó vào GitHub Secrets nghĩa
là mọi người có quyền sửa workflow đều có đường đọc được mật khẩu `prod` bằng
một dòng `echo` — mà quyền sửa workflow thì rộng hơn quyền vào máy chủ nhiều.
Pipeline không cần biết mật khẩu để triển khai; nó chỉ cần bảo compose khởi động
lại.

*Không có khoá đăng nhập registry.* Pipeline đẩy ảnh bằng `GITHUB_TOKEN` của
chính lượt chạy, và lúc triển khai nó chuyển token đó qua `stdin` của SSH để máy
chủ `docker login` rồi `docker logout` ngay trong cùng lượt. Token hết hạn khi
lượt chạy kết thúc, nên **máy chủ không cất khoá dài hạn nào của registry**. Đây
là thứ tốt hơn thiết kế cũ, không phải thứ bị bỏ sót.

*Đăng nhập máy chủ bằng mật khẩu, không bằng khoá.* Máy chủ của dự án chỉ mở
user/password. Pipeline dùng `appleboy/ssh-action` và `appleboy/scp-action` —
**cùng khuôn với `comic-social-network-be`**, repo mà `15` nói là mượn khuôn đã
chạy thật. Đây là chỗ lệch **yếu hơn** khuôn thường gặp, không mạnh hơn:

- **Không kiểm danh tính máy chủ.** Hai action này không ghim dấu vân tay, nên
  mỗi lượt triển khai gửi mật khẩu VPS cho bất cứ máy nào trả lời ở địa chỉ đó.
  Với khoá thì gặp máy chủ giả chỉ hỏng một lượt; với mật khẩu thì mất luôn mật
  khẩu.
- **Đã thử đường chặt hơn và bỏ.** Bản đầu tự gọi `ssh`/`scp` qua `sshpass` và
  ghim vân tay bằng secret `VPS_KNOWN_HOSTS`. Nó đúng về bảo mật nhưng đắt về
  vận hành: giá trị phải lấy lại mỗi lần đổi IP hoặc cài lại máy, cột đầu phải
  khớp `VPS_HOST` từng ký tự, và khi sai thì OpenSSH chỉ nói
  `Host key verification failed` — câu dùng chung cho cả "host lạ" lẫn "khoá đã
  đổi", nên không đoán được. Ba lượt triển khai hỏng liên tiếp vì nó. Một biện
  pháp bảo mật mà mỗi lần chạm vào là một lượt CI đỏ thì sớm muộn cũng bị gỡ.
- **Cách siết lại KHÔNG phải quay về `known_hosts`** mà là chuyển sang khoá SSH:
  thêm khoá công khai vào `authorized_keys`, đổi `password:` thành `key:` ở hai
  bước. Lúc đó máy chủ giả không lấy được gì, và cũng không cần ghim vân tay nữa
  — một thay đổi giải cả hai vấn đề, thay vì thêm một thứ phải bảo trì. Mật khẩu
  của người vẫn giữ nguyên; hai đường đăng nhập không loại trừ nhau.
- **Hai action này là phụ thuộc mới của repo.** Chúng đã chạy thật ở
  `comic-social-network-be` hai tháng, và cùng một tác giả nên cùng một mô hình
  cấu hình.

Mật khẩu này dùng được ở **mọi nơi** chứ không riêng việc triển khai, và ai sửa
được workflow là có đường đọc nó. Đó là lý do gạch đầu dòng thứ ba đáng làm sớm.

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

> **Đã build và đã triển khai thật; `api` chưa lên được.** Tính tới 09/09/2026,
> ba ảnh đã build trên runner, đẩy lên GHCR và pull được về VPS — phần đó của
> tám file dưới đây đã chứng minh. Cái chưa xong là `api` khởi động thành công:
> nó đổ ở CSDL, ba lần, ba nguyên nhân khác nhau. Cả ba nay nằm ở mục 5.0.2.

| File | Vai trò |
|---|---|
| `api/Dockerfile` | Ảnh backend — JDK biên dịch, JRE chạy |
| `web/apps/site/Dockerfile` | Ảnh website khách |
| `web/apps/admin/Dockerfile` | Ảnh trang quản trị |
| `deploy/compose.prod.yaml` | Phần dùng chung: sáu service, không publish cổng nào, không proxy. **Tự nó không chạy được** |
| `deploy/compose.ip.yaml` | Phủ cho chế độ **chưa có tên miền**: mở cổng thẳng, không HTTPS |
| `deploy/compose.tenmien.yaml` | Phủ cho chế độ **đã có tên miền**: thêm Caddy, chỉ mở 80 và 443 |
| `deploy/Caddyfile` | HTTPS và bốn tên miền. Chỉ dùng ở chế độ thứ hai |
| `.github/workflows/trien-khai.yml` | Pipeline: build → đẩy → SSH → up |

### 5.0. Hai hình dạng chạy, một dòng quyết định

Máy chủ chạy theo hình dạng nào là **thuộc tính của máy chủ**, không phải của
pipeline. Nó nằm ở đúng một dòng trong `.env` cạnh các file compose:

```
COMPOSE_FILE=compose.prod.yaml:compose.ip.yaml       ← chưa có tên miền
COMPOSE_FILE=compose.prod.yaml:compose.tenmien.yaml  ← đã có tên miền
```

Compose tự đọc biến này từ `.env`, nên pipeline chạy `docker compose pull` trần,
không truyền `-f`. Đổi hình dạng là sửa một dòng rồi chạy lại workflow.

| | Chế độ IP | Chế độ tên miền |
|---|---|---|
| Định tuyến theo | **Cổng** — `:4000` site, `:4001` quản trị, `:4002` API, `:4003` ảnh | **Host header**, qua Caddy |
| HTTPS | Không có | Let's Encrypt, tự gia hạn |
| Cổng mở ra ngoài | 4000–4003; bảng điều khiển MinIO ở 4004 chỉ nghe loopback | 80, 443 |
| `COOKIE_SECURE` | `false` | `true` |
| `FORWARD_HEADERS` | `none` | `framework` |

Hai dòng cuối là chỗ hai chế độ **không được nhầm**. Đặt `COOKIE_SECURE=true`
khi chưa có HTTPS thì trình duyệt lặng lẽ vứt cookie phiên: đăng nhập trang quản
trị trả 204 thành công, rồi màn hình kế tiếp vẫn đá ngược về trang đăng nhập, và
không có lỗi nào ở bất cứ đâu — không ở log API, không ở console trình duyệt.

**Chế độ IP không có mã hoá đường truyền.** Mật khẩu đăng nhập trang quản trị và
cookie phiên đi qua mạng dưới dạng chữ thường. Chấp nhận được khi đang dựng và
thử; không chấp nhận được khi đã có khách thật và nhân viên thật đăng nhập.

### 5.0.1. Máy chủ này không trống

VPS đang chạy ba dự án khác. Hai hệ quả đã đi vào cấu hình, và cả hai đều là
loại lỗi chỉ lộ ra sau khi đã hỏng:

- **Cổng.** 8080, 9000, 9001, 8091, 5173, 5179, 3309, 16379 và 5432 đã có chủ.
  Dải 4000–4004 chọn vì nó trống. Mọi cổng đều lấy từ `.env`, không đặt cứng —
  kiểm bằng `ss -tlnp` trước khi chạy lần đầu.
- **Tên compose project là `travel-booking`, không phải `travel`.** Máy chủ đã
  có sẵn một container tên `travel-web` của dự án khác. Trùng tên project thì
  `docker compose up -d --remove-orphans` coi container đó là orphan và **xoá
  nó** — mất một dịch vụ đang chạy, do một lệnh triển khai của dự án khác.

Redis và các Postgres khác của những dự án kia bind vào `127.0.0.1`, còn dự án
này không publish cổng CSDL nào ra ngoài — mọi kết nối đi qua mạng nội bộ của
Docker, xem mục kế tiếp.

### 5.0.2. CSDL của `prod` là Postgres dùng chung — ba việc phải làm trước

`prod` **không dùng Postgres do `compose.prod.yaml` dựng lên**. Nó nối vào một
container đã có sẵn trên VPS:

| | Giá trị hiện tại | Lấy bằng |
|---|---|---|
| Container | `shared-postgres`, PostgreSQL 16.14 | `docker ps --format '{{.Names}}'` |
| Mạng docker | `shared-net` | `docker inspect shared-postgres --format '{{json .NetworkSettings.Networks}}'` |
| Database | `travel`, encoding `UTF8` | `psql -U postgres -c '\l travel'` |

Container đó **thuộc dự án khác**. Dự án này là khách: không dựng nó, không cấu
hình nó, không được xoá nó. Ba việc dưới đây là hệ quả trực tiếp, và cả ba đều
đã làm đổ một lượt triển khai thật trước khi được viết ra đây.

**1. `api` phải được nối vào mạng của container đó.**

DNS nội bộ của Docker chỉ phân giải tên trong **những mạng mà container được nối
vào**. Compose project `travel-booking` mặc định chỉ có mạng `default` của riêng
nó, nên cái tên `shared-postgres` đơn giản là không tồn tại đối với `api`.

`compose.prod.yaml` khai mạng đó `external: true` — nối vào, không tạo ra, và
`docker compose down` ở đây không đụng tới nó. Hai biến trong `.env`:

```
DB_HOST=shared-postgres     ← TÊN CONTAINER, không phải IP: IP đổi mỗi lần dựng lại
DB_NETWORK=shared-net
```

Thiếu thì: `java.net.UnknownHostException: shared-postgres`, ngay lúc Flyway mở
kết nối. Câu đó nằm ở dòng `Caused by` **cuối cùng** của một stack trace dài;
phía trên nó là ba tầng Spring và một `SQL State: 08001` không hé lộ gì về DNS.

**2. Database phải THUỘC VỀ user `travel`, không chỉ được cấp quyền.**

Từ PostgreSQL 15, quyền `CREATE` trên schema `public` bị thu hồi khỏi role
`PUBLIC`. Schema `public` thuộc `pg_database_owner`, nên **chỉ chủ sở hữu
database** mới tạo được bảng trong đó. Mọi hướng dẫn viết trước PG15 đều sai ở
đúng điểm này, và chúng vẫn là phần lớn những gì tìm thấy trên mạng.

Cấp `CONNECT` và `CREATE` ở mức database là **chưa đủ**: `CREATE` ở mức database
là quyền tạo *schema mới*, không phải quyền tạo *bảng trong* `public`. Một
database có `travel=CTc/postgres` trông như đã đủ quyền, mà vẫn hỏng.

```sql
ALTER DATABASE travel OWNER TO travel;
```

Thiếu thì: `SQL State: 42501 — ERROR: permission denied for schema public`, chết
đúng lúc Flyway tạo bảng `flyway_schema_history`, tức là **trước khi** migration
đầu tiên kịp chạy một dòng nào.

**3. Hai collation ICU phải tồn tại — kiểm TRƯỚC lần migrate đầu tiên.**

`V1__khoi_tao.sql` tạo hai index dùng `COLLATE "da-DK-x-icu"` và
`"vi-VN-x-icu"` (`12` mục 1). Hai collation đó có mặt hay không phụ thuộc vào
việc container kia có được biên dịch kèm ICU — bản `alpine` cắt bớt ICU, và hình
dạng của container dùng chung không do dự án này quyết định.

```sql
SELECT collname FROM pg_collation
 WHERE collname IN ('da-DK-x-icu','vi-VN-x-icu');   -- phải ra đúng 2 dòng
```

**Kiểm trước, đừng kiểm sau.** `V1` chạy hơn năm trăm dòng rồi mới tới hai index
đó. Chết ở giữa thì `V1` nằm lại trong `flyway_schema_history` ở trạng thái
failed, và lần khởi động sau **không tự sửa được** — phải dọn tay rồi mới thử
lại. Đây là khoảng cách giữa một câu `SELECT` mười giây và nửa giờ dọn dẹp.

Không đủ hai dòng thì thử tạo tay:

```sql
CREATE COLLATION "da-DK-x-icu" (provider = icu, locale = 'da-DK');
CREATE COLLATION "vi-VN-x-icu" (provider = icu, locale = 'vi-VN');
```

Báo `ICU is not supported in this build` thì hết đường ở mức cấu hình, và quyết
định phải lên mức khác: đổi ảnh của container dùng chung — đụng vào dự án kia —
hay dựng Postgres riêng cho `travel`. Chưa gặp, chưa chốt; gặp thì đây là quyết
định của kiến trúc sư, không phải của người triển khai.

Ngoài ba việc trên, `V1` còn cần `unaccent` và `pg_trgm`. Cả hai là extension
*trusted* từ PG13 nên chủ database tự tạo được sau khi việc 2 xong; tạo sẵn bằng
`postgres` cũng không hại gì, lệnh trong `V1` có `IF NOT EXISTS`.

Cả ba việc, chạy một lượt trên máy chủ:

```bash
docker exec shared-postgres psql -U postgres \
  -c "ALTER DATABASE travel OWNER TO travel;"
docker exec shared-postgres psql -U postgres -d travel \
  -c "CREATE EXTENSION IF NOT EXISTS unaccent; CREATE EXTENSION IF NOT EXISTS pg_trgm;"
docker exec shared-postgres psql -U postgres -d travel \
  -c "SELECT collname FROM pg_collation WHERE collname IN ('da-DK-x-icu','vi-VN-x-icu');"
```

**Một hệ quả rơi sang mục khác:** CSDL của `prod` nằm trong container của dự án
khác, nên lịch sao lưu và bài thử phục hồi (`35`) không nằm trọn trong tay dự án
này. Ô sao lưu chưa tích ở mục 5.2 vì thế khó hơn vẻ ngoài của nó.

### 5.1. Đường đi

```
đẩy lên main
  → branch protection đã bắt api.yml và web.yml phải xanh mới vào được main
  → trien-khai.yml: build ba ảnh Docker hai giai đoạn, song song
  → đẩy lên GHCR, thẻ = commit SHA
  → SSH vào VPS: docker compose pull && docker compose up -d
  → container mới khởi động, Flyway chạy, nhận traffic
```

Hai giai đoạn trong `Dockerfile` vì ảnh cuối **không được chứa Gradle, JDK đầy
đủ, mã nguồn hay bí mật build**: giai đoạn một biên dịch, giai đoạn hai chỉ chép
file `jar` sang một ảnh JRE.

`trien-khai.yml` **không chạy lại test**. Câu "không bỏ qua CI" của mục 7 được
giữ bởi **branch protection**, không bởi pipeline: ba check `api`, `web`,
`tai-lieu` phải được đặt bắt buộc trên nhánh `main`. Chưa bật cài đặt đó thì
pipeline vẫn chạy đúng, nhưng nó sẽ vui vẻ triển khai một commit đỏ.

**Bối cảnh build của cả ba ảnh là gốc repo**, không phải thư mục của từng ảnh.
Cả ba đều cần `contracts/openapi.yaml` để sinh code (ADR-002), mà file đó nằm
ngoài `api/` lẫn `web/`. Lấy thư mục con làm bối cảnh thì build chết ở bước sinh
code với một thông báo không nhắc gì tới bối cảnh.

### 5.3. Biến bị nướng vào ảnh lúc build

Đây là chỗ dễ mất buổi chiều nhất của cả tài liệu này.

Next.js thay `process.env.NEXT_PUBLIC_*` bằng **giá trị chuỗi** lúc build, và
bản `output: 'standalone'` còn đóng băng cả `next.config.ts` đã giải trị vào
`.next/required-server-files.json` — `rewrites()` của trang quản trị nằm trong
đó. Hệ quả:

- Đặt `NEXT_PUBLIC_API_URL` hay `API_URL` trong `compose.prod.yaml` là **vô tác
  dụng**. Chúng phải là `--build-arg` của `docker build`, và `trien-khai.yml`
  truyền chúng từ **GitHub Variables** — biến, không phải bí mật: địa chỉ công
  khai thì không bí mật gì. Năm biến: `PUBLIC_SITE_URL`, `PUBLIC_API_URL`,
  `MEDIA_PROTOCOL`, `MEDIA_HOST`, `MEDIA_PORT`. Chúng là **địa chỉ đầy đủ** chứ
  không phải tên miền, để cùng một pipeline phục vụ được cả `http://<IP>:3000`
  lẫn `https://vidu.com`.
- **Ảnh gắn liền với một môi trường.** Không build một ảnh rồi đem cùng ảnh đó
  chạy ở staging và prod. Ngày trả lời **Q-9** mà câu trả lời là "có staging"
  thì phải build hai lượt, hai bộ thẻ.
- **Build-arg rỗng KHÔNG rơi về mặc định của `ARG`.** Truyền
  `--build-arg X=` là ghi đè bằng chuỗi rỗng, và `ARG X=https` trong
  Dockerfile không cứu được. Lần đầu chạy pipeline đã đỏ vì đúng chuyện này:
  Variables chưa đặt → build-arg rỗng → `next build` chết giữa chừng với
  `Expected http | https, received `, một câu không nhắc gì tới GitHub
  Variables. Nay có hai lớp chặn: job `anh` kiểm năm Variables ở **bước đầu
  tiên**, trước cả checkout, và `next.config.ts` dùng `||` chứ không `??` —
  `??` chỉ bắt `undefined`, không bắt chuỗi rỗng.
- Tên miền vì thế xuất hiện **hai chỗ**: GitHub Variables (lúc build) và `.env`
  trên máy chủ (lúc chạy). Lệch nhau thì site gọi sang API sai địa chỉ và trình
  duyệt báo lỗi CORS. Đổi tên miền là đổi **cả hai chỗ**.

### 5.2. Bắt buộc trước lần triển khai `prod` đầu tiên

Danh sách này là **điều kiện chặn**, không phải gợi ý:

Bốn ô đầu nay **đã có chỗ để làm** — cột bên phải nói chỗ đó ở đâu. Chúng vẫn là
ô chưa tích, vì có chỗ điền không phải là đã điền.

| | Việc | Làm ở đâu |
|---|---|---|
| [ ] | `DB_PASSWORD` thật, không phải `travel` | `.env` trên máy chủ, `openssl rand -base64 32` |
| [ ] | `MINIO_ROOT_PASSWORD` thật | cùng file |
| [ ] | `BOOTSTRAP_ADMIN_EMAIL` và `BOOTSTRAP_ADMIN_PASSWORD` | `.env` trên máy chủ. **Không có nó thì không ai đăng nhập được vào trang quản trị** — hợp đồng API không có đường tạo người dùng, `22` mục 9.1 |
| [ ] | Tắt Swagger: `SPRINGDOC_API_DOCS_ENABLED=false` và `SPRINGDOC_SWAGGER_UI_ENABLED=false` | `.env` trên máy chủ. Trang đó liệt kê **đủ cả bề mặt quản trị** — đường dẫn, tham số, thân yêu cầu — và chế độ IP chưa có HTTPS. ADR-012 |
| [ ] | `logging.level.vn.travel.booking` hạ xuống `INFO` | `LOGGING_LEVEL_VN_TRAVEL_BOOKING` — đã đặt sẵn trong `compose.prod.yaml` |
| [ ] | `NEXT_PUBLIC_SITE_URL` trỏ địa chỉ thật | GitHub Variable `PUBLIC_SITE_URL` — **lúc build**, xem mục 5.3 |
| [ ] | Cookie phiên đặt `Secure` | `SERVER_SERVLET_SESSION_COOKIE_SECURE` + `SERVER_FORWARD_HEADERS_STRATEGY`, cả hai đã đặt sẵn trong `compose.prod.yaml`. Không phải sửa code |
| [ ] | HTTPS, và HTTP chuyển hướng sang HTTPS | `deploy/Caddyfile` — nhưng **chỉ ở chế độ tên miền**. Chế độ IP hiện tại không có HTTPS, và không thể có: Let's Encrypt không cấp chứng chỉ cho địa chỉ IP |
| [ ] | Sao lưu CSDL có lịch và **đã thử phục hồi một lần** (`35`) | **Chưa có gì cả** |
| [ ] | `30`, `31`, `32` ở trạng thái `Đã duyệt` — cổng G5 (`40`) | **Chưa có gì cả** |

Hai dòng cuối là hai dòng chưa có chỗ nào để tích, và dòng sao lưu là dòng hay
bị bỏ qua nhất: bản sao lưu chưa từng phục hồi thử thì chưa phải là bản sao lưu,
nó chỉ là một file.

`NEXT_PUBLIC_SITE_URL` cũng đáng dừng lại một nhịp. Nó sai thì **không có lỗi
nào nổ ra**: site chạy bình thường, chỉ có sitemap và `robots.txt` mang địa chỉ
`localhost`, và công cụ tìm kiếm bỏ qua toàn bộ site. Không ai phát hiện ra
trong nhiều tuần.

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

| # | Việc | Trạng thái |
|---|---|---|
| 1 | Mở một PR thật, xác nhận bộ lọc đường dẫn chạy đúng trên runner | **Còn lại** — điều kiện treo số 1 của cổng G2 (`41` mục 7) |
| 2 | `Dockerfile` hai giai đoạn cho `api/` và hai app Next.js | ✔ có mã — nhưng **chưa build thật lần nào**, xem cảnh báo đầu mục 5 |
| 3 | Workflow triển khai | ✔ xong — `trien-khai.yml` |
| 4 | Bật branch protection trên `main`, đặt ba check là bắt buộc | **Còn lại** — không có nó thì mục 7 "không bỏ qua CI đỏ" chỉ là lời hứa |
| 5 | Điền bốn bí mật SSH và ba biến tên miền vào GitHub | **Còn lại** — mục 3.1 và 5.3 |
| 6 | Trỏ bốn bản ghi A về máy chủ, tạo `.env` từ `deploy/.env.prod.example` | **Còn lại** — của bạn, không của pipeline |
| 7 | Chạy `trien-khai.yml` lần đầu và xem nó hỏng ở đâu | **Còn lại** — nó sẽ hỏng ở đâu đó, mọi pipeline đều thế |
| 8 | Sao lưu CSDL có lịch, và thử phục hồi một lần (`35`) | **Còn lại** — chưa bắt đầu |

---

## 9. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| **Có môi trường `staging` không** — không có thì lần chạy thật đầu tiên của mọi migration là trên dữ liệu khách | Mục 1 | **Q-9**, đề nghị thêm vào `41` mục 4 |
| Một hay nhiều instance | Mục 4.1 | **Q-5** ở `41` mục 4 |
| Registry ảnh — pipeline **đang dùng GHCR** làm mặc định, vì nó không phải dựng thêm gì và không phải cất khoá dài hạn nào trên máy chủ. Đổi sang registry riêng là đổi một biến `REGISTRY` | Mục 5.1 | Kiến trúc sư — quyết định vẫn mở, nhưng không còn chặn việc gì |
| Triển khai không gián đoạn, hay chấp nhận vài giây tắt | Mục 5.1 — quyết định này ràng buộc mục 4.1 | **Q-4** (quy mô) |
| Cổng thanh toán cần webhook, tức là cần URL công khai ổn định từ trước | `30` | **Q-3** |
