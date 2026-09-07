# CLAUDE.md

Dự án: **website bán tour du lịch Việt Nam**, hai thị trường, hai ngôn ngữ.
Gồm website khách, API và trang quản trị.

> **Trạng thái: G2 xong phần code, phần code của G3 và phần lớn G4 đã chạy.**
> Khách xem được danh sách tour, trang chi tiết chia tab, blog, sự kiện và liên
> hệ; đặt được tour và gửi được yêu cầu báo giá. Trang quản trị có đủ mười một
> màn hình: sản phẩm, đơn, báo giá, nội dung khác, người dùng. Còn thiếu một PR
> thật để xác nhận CI. Mọi lệnh bên dưới chạy được thật.
>
> Dữ liệu mồi nạp đủ để **nhìn** chứ không chỉ để test: 12 sản phẩm phủ cả sáu
> loại, 82 ngày lịch trình song ngữ, khách sạn, tham quan, bài viết, sự kiện —
> `psql "$DB_URL" -f api/scripts/seed-dev.sql`, chạy lại được nhiều lần. Ảnh mẫu
> tải bằng `python scripts/tai-anh-mau.py` (cần mạng một lần; giấy phép ghi ở
> `web/apps/site/public/img/NGUON.md`).
>
> **Sửa dữ liệu mồi thì chạy bộ kiểm nhất quán** — `api/scripts/kiem-nhat-quan.sql`,
> 23 quy tắc của `docs/12` mục 9. Nó bắt loại lỗi mà `psql` báo xanh: lịch trình
> thủng ngày, giá phẳng, ngày khởi hành thiếu giá phòng đơn.
>
> **Cần JDK 21 và pnpm.** `JAVA_HOME` trỏ JDK cũ thì đặt `org.gradle.java.home`
> trong `~/.gradle/gradle.properties`; pnpm bật bằng `corepack enable pnpm`.
>
> Đang ở đâu, việc kế tiếp là gì, đang chờ ai quyết: `docs/41-tinh-trang.md`.
> Đọc file đó **trước** khi bắt đầu bất cứ việc gì.

Ba file `CLAUDE.md`. Đọc file ở thư mục đang làm việc, không đọc cả ba:

| File | Cho ai |
|---|---|
| `CLAUDE.md` (file này) | Quy tắc chung cho cả dự án |
| `api/CLAUDE.md` | Java, Gradle, Spring, JPA |
| `web/CLAUDE.md` | Next.js, i18n, giao diện |

---

## Điều quan trọng nhất phải nhớ

**1. `Market` và `Locale` là hai thứ khác nhau. Không gộp.**

`Market` (`DK`, `VN`) quyết định khách **mua** gì: catalog, giá, tiền tệ, cổng
thanh toán, luật. `Locale` (`da`, `vi`) chỉ quyết định khách **đọc** bằng tiếng
gì. Một khách Việt sống ở Đan Mạch mua ở `DK` nhưng đọc `vi`.

Đây là nhầm lẫn tốn kém nhất của dự án. Chi tiết: `docs/02`.

**2. Tiếng Đan Mạch là ngôn ngữ nguồn.**

Nội dung viết bằng `da` trước, dịch sang `vi` sau. Không có chiều ngược lại. Đội
ngũ nói tiếng Việt nên đây là điều làm ngược trực giác — ADR-004.

**3. Không fallback ngôn ngữ cho nội dung bán hàng.**

Tour, điểm đến, bài viết thiếu bản dịch thì **ẩn hoàn toàn** khỏi locale đó:
không listing, không tìm kiếm, không sitemap, URL trả 404. Không hiện bản `da`
thay thế.

Chuỗi giao diện thì ngược lại — fallback về `da`, ghi log, CI báo lỗi.

**4. Không quy đổi tỷ giá.**

Giá ở mỗi thị trường là giá người nhập, không phải kết quả nhân tỷ giá. Tour bán
cho khách Đan gồm vé bay quốc tế, bán cho khách Việt thì không — hai sản phẩm
khác nhau. **Không có cột `exchange_rate` ở đâu trong hệ thống này.**

---

## Bố cục repo

```
api/                    Gradle multi-module, Spring Boot
  domain/               Quy tắc nghiệp vụ thuần. KHÔNG phụ thuộc Spring
  application/          Use case, transaction
  infrastructure/       JPA, thanh toán, email, lưu trữ
  web/                  Controller, ánh xạ DTO
web/                    pnpm workspace
  apps/site/            Next.js — website khách
  apps/admin/           Next.js — trang quản trị
  packages/api-client/  TS client SINH RA — không sửa tay
  packages/ui/          Design system dùng chung
  packages/i18n/        Message catalog da / vi
contracts/openapi.yaml  Nguồn sự thật của hợp đồng API
docs/                   Bộ tài liệu
```

---

## Lệnh thường dùng

```bash
docker compose up -d              # Postgres, có sẵn unaccent và pg_trgm

psql "$DB_URL" -f api/scripts/seed-dev.sql          # dữ liệu mồi, chạy lại được
psql "$DB_URL" -f api/scripts/kiem-nhat-quan.sql    # 23 quy tắc của docs/12 mục 9

# api/
./gradlew build                   # biên dịch + test
./gradlew test                    # JUnit + Testcontainers
./gradlew :web:bootRun            # migration Flyway chạy lúc khởi động ứng dụng

# web/
pnpm dev
pnpm build
pnpm typecheck
pnpm lint
pnpm test
pnpm i18n:check                   # độ phủ chuỗi giao diện — thiếu khoá là LỖI

# contracts/
pnpm contracts:generate           # sinh interface Java + TS client
pnpm contracts:check              # kiểm tương thích ngược
pnpm docs:check                   # link chết, khối trạng thái, thuật ngữ

# chạy được ngay, không cần cài gì
python scripts/docs_check.py                 # bộ kiểm tài liệu
python scripts/docs_check.py --truy-vet      # ma trận YC / QT / RB
python docs/tools/build-docx.py              # sinh lại bản Word gửi khách
```

Trước khi báo hoàn thành, chạy phần liên quan tới thứ mình sửa. Sửa
`contracts/` thì phải chạy **cả hai bên**.

---

## Quy tắc bắt buộc

1. **Không hardcode số liệu hiển thị.** "Xem tất cả 92 tour" phải đếm từ dữ liệu,
   đếm trong phạm vi `(market, locale)` đang xem.
2. **Trạng thái bộ lọc nằm trong URL.** F5 phải giữ nguyên bộ lọc.
3. **Ba trạng thái cho mọi màn hình có dữ liệu**: đang tải (skeleton, không phải
   vòng xoay), rỗng (kèm hướng dẫn hành động), lỗi.
4. **Giá luôn kèm chữ "từ" và disclaimer.** Yêu cầu pháp lý, không phải lựa chọn
   thiết kế.
5. **API không trả câu tiếng người.** Trả mã lỗi kèm tham số; frontend dịch.
6. **API không định dạng tiền.** Trả `{"amount":"24990.00","currency":"DKK"}`.
   `amount` là **chuỗi**, không phải số JSON.
7. **Slug không dùng ký tự có dấu** ở cả hai ngôn ngữ: `bekraeftelse` chứ không
   `bekræftelse`; `viet-nam-tu-bac-vao-nam` chứ không có dấu.
8. **Không tự ý cài thêm dependency.** Hỏi trước.
9. **Không sửa file sinh ra.** `packages/api-client/` và interface Java sinh từ
   `openapi.yaml` — sửa spec, đừng sửa code sinh ra.

---

## Đổi hợp đồng API

Spec-first (ADR-002). Thứ tự bắt buộc:

```
1. Sửa contracts/openapi.yaml     ← pull request riêng, có người duyệt
2. pnpm contracts:generate
3. Sửa controller cho khớp interface mới  ← quên bước này là lỗi biên dịch
4. Sửa frontend
```

Không bao giờ đi ngược: sửa controller trước rồi cập nhật spec sau là code-first,
và ADR-002 đã bác bỏ.

---

## Tài liệu — đọc cái nào khi nào

| Cần biết | Đọc |
|---|---|
| Market, locale, fallback, URL, bảng dịch | `docs/02-thi-truong-va-da-ngon-ngu.md` |
| Thuật ngữ, đối chiếu Việt–Đan–code | `docs/03-tu-vung-nghiep-vu.md` |
| Nghiệp vụ và ràng buộc từng loại sản phẩm | `docs/04-phan-loai-san-pham.md` |
| Vào trang chi tiết thì thấy gì, theo từng loại | `docs/05-trang-chi-tiet-san-pham.md` |
| Phạm vi v1, cái gì không làm | `docs/01-dac-ta-san-pham.md` |
| Kiến trúc, module, ranh giới phụ thuộc | `docs/10-kien-truc-he-thong.md` |
| Vì sao một quyết định lại như vậy | `docs/adr/` |
| Bản đồ toàn bộ tài liệu | `docs/00-ke-hoach-tai-lieu.md` |
| Giai đoạn, cổng nghiệm thu, RACI, kiểm soát thay đổi | `docs/40-ke-hoach-thuc-hien.md` |
| **Đang ở đâu, việc kế tiếp, chỗ dễ quên** | `docs/41-tinh-trang.md` |
| Vòng đời tài liệu, khối trạng thái, bộ kiểm | `docs/42-quy-trinh-tai-lieu.md` |

**Một sự thật, một chỗ.** File này được phép tóm tắt, nhưng khi lệch thì tài liệu
trong `docs/` đúng, không phải file này.

---

## Quy trình — `.claude/`

Quy ước ở `docs/40` và `docs/42` đã được đóng thành thứ chạy được. Chi tiết:
`.claude/README.md`.

| Lệnh | Làm gì |
|---|---|
| `/tinh-trang` | Đang ở đâu, việc kế tiếp, đang chờ ai quyết |
| `/kiem-tai-lieu` | Chạy bộ kiểm, sửa lỗi |
| `/cong G2` | Chạy cổng nghiệm thu một giai đoạn |
| `/tai-lieu 20-frontend-web` | Viết tài liệu mới đúng chuẩn |
| `/ket-phien` | Chạy kiểm, cập nhật `41`, đề xuất commit |

Sáu skill gọi theo tên khi việc khớp: `tai-lieu-moi` · `ra-soat-tai-lieu` ·
`cong-nghiem-thu` · `doi-hop-dong-api` · `them-thi-truong-ngon-ngu` ·
`ban-giao-khach`.

Hai hook chạy tự động: chặn sửa tay code sinh từ `openapi.yaml`, và kiểm mọi file
`docs/` ngay sau khi sửa.

---

## Quy ước commit

Tiền tố theo vùng, vì lịch sử git lẫn hai loại thay đổi:

```
api:        thay đổi backend
web:        thay đổi frontend
contracts:  thay đổi hợp đồng API
docs:       thay đổi tài liệu
chore:      hạ tầng, cấu hình
```

Nội dung commit viết **tiếng Việt**.

---

## Ngôn ngữ của từng thứ

| Thứ | Ngôn ngữ |
|---|---|
| Chuỗi khách nhìn thấy | `da` và `vi` — trong message catalog và bảng dịch |
| Nội dung trong CSDL | `da` là nguồn, `vi` là bản dịch |
| Định danh trong code | Tiếng Anh — xem `docs/03` mục 7 |
| Chú thích trong code | **Tiếng Việt** |
| Tài liệu trong `docs/` | **Tiếng Việt** |
| Commit | **Tiếng Việt** |

---

## Những gì cố tình chưa làm ở v1

Đăng nhập cho khách, cổng khách hàng, tiếng Anh, tour liên quốc gia, bản đồ tương
tác, trò chuyện trực tiếp, ứng dụng di động. Danh sách đầy đủ và lý do: `docs/01`
mục 3.2 và 3.3.

Gặp mấy phần này thì **đừng tự làm** — hỏi trước.
