# travel-tour-booking

Website bán tour du lịch Việt Nam. Hai thị trường (`DK`, `VN`), hai ngôn ngữ
(`da`, `vi`), ba phần: website khách, API, trang quản trị.

File này trả lời đúng một câu: **làm sao chạy được cái này trên máy mình.**

- Nghiệp vụ, kiến trúc, quyết định — `docs/`, bắt đầu từ
  [`docs/00-ke-hoach-tai-lieu.md`](docs/00-ke-hoach-tai-lieu.md)
- Đang ở đâu, việc kế tiếp, đang chờ ai quyết —
  [`docs/41-tinh-trang.md`](docs/41-tinh-trang.md)
- Quy ước khi sửa mã — `CLAUDE.md` ở gốc, `api/`, `web/`

---

## 1. Cần cài sẵn

| | Bản đang dùng | Ghi chú |
|---|---|---|
| JDK | **21** | Spring Boot 4 cần 17+. `JAVA_HOME` trỏ bản cũ thì xem mục 6 |
| Node | 24 | |
| pnpm | 11 | `npm i -g pnpm`. Node 25 đã bỏ `corepack` nên `corepack enable` không còn dùng được |
| Docker | 28 | Postgres và MinIO chạy trong container |
| Python | 3.12 | Chỉ cho mấy script tiện ích; không cần thư viện ngoài nào |

---

## 2. Chạy lần đầu

Bốn bước, theo đúng thứ tự. Mỗi bước mở một cửa sổ dòng lệnh riêng cho ba lệnh
cuối — chúng chạy liên tục, không tự thoát.

### Bước 1 — hạ tầng

```bash
docker compose up -d
```

Dựng hai container: **Postgres 16** (cổng 5432) và **MinIO** (cổng 9000, bảng
điều khiển 9001). Một container thứ ba chạy một lần rồi thoát — nó tạo bucket
`travel-media` và mở quyền đọc. Thấy `travel-minio-init` ở trạng thái `Exited
(0)` là đúng, không phải lỗi.

### Bước 2 — API

```bash
cd api
./gradlew bootRun
```

**Flyway chạy lúc ứng dụng khởi động**, không phải lúc `docker compose up`. Nên
lần đầu tiên bảng chỉ xuất hiện sau bước này. API nghe ở
[localhost:8080](http://localhost:8080).

### Bước 3 — dữ liệu mồi

Chạy khi API đã tạo xong bảng.

Không cần cài `psql` trên máy — dùng cái có sẵn trong container:

```bash
docker cp api/scripts/seed-dev.sql travel-postgres:/tmp/seed.sql
docker exec travel-postgres psql -U travel -d travel -f /tmp/seed.sql

python scripts/tai-anh-mau.py      # tải 29 ảnh mẫu, cần mạng một lần
python scripts/nap-anh-len-kho.py  # đẩy chúng lên MinIO
```

Có `psql` rồi thì gọi thẳng cũng được:

```bash
psql postgresql://travel:travel@localhost:5432/travel -f api/scripts/seed-dev.sql
```

> **Git Bash trên Windows đổi `/tmp/seed.sql` thành đường dẫn Windows** trước
> khi Docker nhìn thấy, và `psql` báo không tìm thấy tệp. Thêm
> `MSYS_NO_PATHCONV=1` trước lệnh `docker exec`. PowerShell và `cmd` không dính.

Dữ liệu mồi nạp đủ để **nhìn** chứ không chỉ để test: 12 sản phẩm phủ cả sáu
loại, 82 ngày lịch trình song ngữ, khách sạn, tham quan, bài viết, sự kiện, và
61 ảnh gắn vào sản phẩm. Chạy lại được nhiều lần.

### Bước 4 — hai giao diện

```bash
cd web
pnpm install
pnpm dev          # website khách  → localhost:3000
pnpm dev:admin    # trang quản trị → localhost:3001
```

---

## 3. Mở gì để xem

| | Địa chỉ |
|---|---|
| Website khách, tiếng Việt | [localhost:3000/vi](http://localhost:3000/vi) |
| Website khách, tiếng Đan | [localhost:3000/da](http://localhost:3000/da) |
| Trang quản trị | [localhost:3001](http://localhost:3001) |
| Bảng điều khiển MinIO | [localhost:9001](http://localhost:9001) — `travel` / `travel-dev-secret` |

Sáu tài khoản quản trị mồi nằm trong `api/scripts/seed-dev.sql`, bảng
`staff_user`: bốn vai trò của `docs/22` cộng hai trường hợp rìa (một người mang
hai vai trò, một người đã nghỉ việc).

> **Mật khẩu gốc của chúng không được ghi ở đâu trong repo** — seed chỉ có chuỗi
> băm, và test tự sinh băm riêng nên không dùng tới. Muốn đăng nhập bằng tay thì
> tự đặt một mật khẩu: sinh băm bcrypt rồi `UPDATE staff_user SET password_hash
> = '<băm>' WHERE email = 'quan-tri@example.test'`. Đây là tài khoản của môi
> trường dev trên máy mình, không phải của bản chạy thật.

**Ba template trang chi tiết** (`docs/05` mục 1.1) — dữ liệu mồi đặt sẵn để mở
là thấy cả ba:

| Template | Mở ở đâu |
|---|---|
| `co-dien` | [/vi/tour/viet-nam-tu-bac-vao-nam](http://localhost:3000/vi/tour/viet-nam-tu-bac-vao-nam) |
| `tap-chi` | [/vi/tour/mien-bac-va-sa-pa](http://localhost:3000/vi/tour/mien-bac-va-sa-pa) |
| `ke-chuyen` | [/vi/tour/mien-nam-va-song-cuu-long](http://localhost:3000/vi/tour/mien-nam-va-song-cuu-long) |

> **Slug khác nhau theo ngôn ngữ, và đó không phải lỗi.** `/vi/` dùng slug tiếng
> Việt, `/da/` dùng slug tiếng Đan. Mở slug tiếng Đan ở `/vi/` trả **404** đúng
> theo thiết kế — nội dung bán hàng không fallback ngôn ngữ (`docs/02`).

---

## 4. Kiểm trước khi commit

Chạy phần liên quan tới thứ mình sửa. Sửa `contracts/` thì phải chạy **cả hai
bên**.

```bash
# api/
./gradlew test          # 293 test, JUnit + Testcontainers (cần Docker)

# web/
pnpm typecheck
pnpm lint
pnpm test
pnpm i18n:check         # thiếu khoá dịch là LỖI, không phải cảnh báo
pnpm build

# gốc repo
python scripts/docs_check.py    # link chết, khối trạng thái, thuật ngữ
```

**Sửa dữ liệu mồi thì chạy bộ kiểm nhất quán** — nó bắt loại lỗi mà `psql` báo
xanh: lịch trình thủng ngày, giá phẳng, ngày khởi hành thiếu giá phòng đơn.

```bash
docker cp api/scripts/kiem-nhat-quan.sql travel-postgres:/tmp/kiem.sql
docker exec travel-postgres psql -U travel -d travel -f /tmp/kiem.sql
```

Không có dòng nào ở mức `LOI` là sạch. Mức `CANH_BAO` **không** làm đỏ: quy tắc
13 đang cảnh báo có chủ ý cho tới khi câu hỏi Q-2 có người trả lời.

---

## 5. Đổi hợp đồng API

Spec-first, và thứ tự này bắt buộc (ADR-002):

```
1. Sửa contracts/openapi.yaml
2. pnpm contracts:generate      ← sinh CẢ interface Java LẪN TS client
3. Sửa controller cho khớp      ← quên bước này là lỗi biên dịch
4. Sửa frontend
```

Đi ngược — sửa controller trước rồi cập nhật spec sau — là code-first, và
ADR-002 đã bác bỏ. Code sinh ra ở `web/packages/api-client/` và
`api/build/generated/openapi` **không sửa tay**; có hook chặn.

---

## 6. Khi hỏng

### `JAVA_HOME` trỏ JDK cũ

Đặt trong `~/.gradle/gradle.properties` — file này thuộc về máy cá nhân, không
commit:

```properties
org.gradle.java.home=C:/Program Files/Java/jdk-21
```

### Trang trả 500, log Next có `Jest worker ... exceeding retry limit`

Gần như luôn là **hết bộ nhớ**, không phải lỗi mã. Thường có dòng
`write EPIPE` ngay trước đó: cửa sổ chạy `pnpm dev` bị đóng hoặc bị hệ thống
giết, ống ghi log đứt, rồi Next sập theo.

Đóng bớt thứ đang ăn RAM, rồi khởi động lại `pnpm dev`. `pnpm dev` báo cổng
3000 đã có người dùng thì tiến trình cũ vẫn sống dù đã hỏng — dừng hẳn nó bằng
PID mà chính thông báo đó in ra.

Gradle cũng giữ daemon nền sau khi chạy test. `./gradlew --status` liệt kê, và
một daemon `IDLE` treo lại có thể chiếm hơn 1 GB.

### `next build` báo `EBUSY: resource busy or locked`

Có tệp đang mở nằm trong `.next/`, mà `next build` xoá sạch thư mục đó. Thường
là do ai đó ghi log vào trong ấy. Để log ra ngoài `.next/`.

### Ảnh không hiện

Ảnh hero phục vụ từ `web/apps/site/public/img/`, **ảnh bộ phục vụ từ MinIO** —
hai nguồn khác nhau trong giai đoạn này. Bộ ảnh trắng thì kiểm theo thứ tự:

```bash
docker compose ps                                        # MinIO còn chạy không
curl -I http://localhost:9000/travel-media/tour/p01-bac-nam.jpg   # tệp có trong kho không
python scripts/nap-anh-len-kho.py                        # nạp lại nếu thiếu
```

Ảnh **không có `alt` ở ngôn ngữ đang xem thì biến mất khỏi ngôn ngữ đó** —
đúng thiết kế, không phải lỗi (`docs/24` mục 6).

### Seed chạy lần hai thì đứt

Mọi `INSERT` trong `seed-dev.sql` phải có `ON CONFLICT`. Thiếu một cái là lần
nạp thứ hai đứt, và đó đúng là cách người ta dùng nó: sửa vài dòng rồi nạp lại.
`KiemNhatQuanIT` chạy tệp mồi **hai lần** chính để bắt chuyện này.

---

## 7. Dọn dẹp

```bash
docker compose down          # dừng, giữ dữ liệu
docker compose down -v       # dừng và XOÁ SẠCH cả CSDL lẫn kho ảnh
```

`-v` xoá cả hai volume. Sau đó phải chạy lại toàn bộ mục 2 từ bước 1.
