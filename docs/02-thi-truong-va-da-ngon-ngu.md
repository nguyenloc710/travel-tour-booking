# Thị trường và đa ngôn ngữ

```
Trạng thái: Nháp
Cập nhật: 31/08/2026
Nguồn sự thật về: khái niệm Market và Locale, danh sách thị trường và ngôn ngữ,
                  chính sách fallback, cấu trúc URL, cách lưu bản dịch,
                  quy trình dịch, định dạng tiền tệ và sắp xếp theo ngôn ngữ.
Không nói về: lược đồ CSDL chi tiết (12), hợp đồng API chi tiết (13),
              engine giá (14), giọng văn nội dung (24).
```

Đây là tài liệu chi phối cả dự án. Đọc trước khi thiết kế bảng, trước khi viết
endpoint, trước khi dựng route.

---

## 1. Hai khái niệm không được gộp

| | **Market** (thị trường) | **Locale** (ngôn ngữ) |
|---|---|---|
| Trả lời câu hỏi | Khách **mua** cái gì, giá bao nhiêu, theo luật nào | Khách **đọc** bằng tiếng gì |
| Quyết định | Catalog tour, bảng giá, tiền tệ, cổng thanh toán, tỷ lệ đặt cọc, ràng buộc pháp lý, chứng từ | Chuỗi giao diện, nội dung tour, slug, thứ tự sắp xếp, định dạng ngày |
| Đổi thì | Giá và danh sách tour đổi | Chỉ chữ đổi, giá giữ nguyên |
| Mã | `DK`, `VN` | `da`, `vi` |

**Vì sao phải tách:** một khách Việt sống ở Đan Mạch mua tour ở thị trường `DK`
(có vé máy bay từ Copenhagen, thanh toán bằng DKK, được Rejsegarantifonden bảo
vệ) nhưng muốn đọc bằng tiếng Việt. Nếu gộp hai khái niệm, người này buộc phải
đọc tiếng Đan hoặc phải mua sản phẩm sai.

Đây không phải trường hợp hiếm — Đan Mạch có cộng đồng người Việt đáng kể, và họ
chính là nhóm khách quan tâm tour Việt Nam nhất.

---

## 2. Danh sách ở v1

### 2.1. Market

| Mã | Tên | Tiền tệ | Locale mặc định | Trạng thái v1 |
|---|---|---|---|---|
| `DK` | Đan Mạch | `DKK` | `da` | **Thị trường chính** |
| `VN` | Việt Nam | `VND` | `vi` | Có, catalog hẹp hơn |

### 2.2. Locale

| Mã | Ngôn ngữ | Vai trò |
|---|---|---|
| `da` | Tiếng Đan Mạch | **Ngôn ngữ nguồn.** Mọi nội dung viết bằng `da` trước |
| `vi` | Tiếng Việt | Ngôn ngữ dịch |
| `en` | Tiếng Anh | Chưa bật ở v1. Kiến trúc phải mở sẵn — xem mục 12 |

**`da` là ngôn ngữ nguồn** (ADR-004). Hệ quả cụ thể, phải nhớ:

- Bản ghi nội dung **bắt buộc** có bản `da`; thiếu `da` là lỗi dữ liệu, chặn ở
  ràng buộc CSDL chứ không chỉ ở tầng ứng dụng.
- Bản `vi` là tuỳ chọn. Thiếu thì trang tiếng Việt **ẩn bản ghi đó**, không hiện
  tiếng Đan thay thế — xem mục 4.
- Người viết nội dung viết `da` trước, `vi` dịch sau. Không có chiều ngược lại.
  Điều này khác trực giác của một đội ngũ người Việt, nên phải nói rõ.

### 2.3. Ghép mặc định giữa locale và market

| Locale | Market mặc định |
|---|---|
| `da` | `DK` |
| `vi` | `VN` |

Khách đổi được market bằng bộ chọn riêng trên giao diện. Xem mục 5.3.

---

## 3. Market sở hữu những gì

Một `Market` không phải là một cái nhãn. Nó sở hữu dữ liệu thật:

| Thuộc tính | `DK` | `VN` |
|---|---|---|
| Tiền tệ | DKK, 2 chữ số thập phân | VND, 0 chữ số thập phân |
| Định dạng giá | `24.990 kr.` | `18.900.000 ₫` |
| Vé máy bay quốc tế | Nằm trong giá tour | Không có |
| Người dẫn đoàn | Trưởng đoàn nói tiếng Đan, bay cùng | Hướng dẫn viên tiếng Việt tại chỗ |
| Sân bay khởi hành | Copenhagen mặc định; Billund, Aalborg có phụ thu | Không áp dụng |
| Tỷ lệ đặt cọc | 25% | Cần khảo sát — xem `01` mục rủi ro |
| Phí xử lý | 295 kr / đơn | Cần khảo sát |
| Cổng thanh toán | Thẻ quốc tế, MobilePay | VNPay, MoMo, chuyển khoản |
| Ràng buộc pháp lý | Rejsegarantifonden, chỉ thị EU về gói du lịch | Luật Du lịch 2017, ký quỹ lữ hành |
| Chứng từ | Hoá đơn EU, VAT | Hoá đơn điện tử theo NĐ 123/2020 |
| Bảo vệ dữ liệu | GDPR | NĐ 13/2023 |

**Một tour không tự động có mặt ở mọi market.** Quan hệ `tour ↔ market` là bảng
nối, có bảng giá riêng cho từng cặp. Tour chưa được gán vào market nào thì không
hiện ở đó — kể cả khi đã dịch xong.

> **Không quy đổi tỷ giá.** Giá ở mỗi market là giá do người nhập, không phải kết
> quả nhân tỷ giá. Tour 16 ngày bán cho khách Đan gồm vé bay quốc tế; bán cho
> khách Việt thì không. Đây là hai sản phẩm khác nhau, không phải một sản phẩm
> quy đổi. Không có cột `exchange_rate` ở đâu trong hệ thống này.

---

## 4. Chính sách fallback — hai loại, hai luật ngược nhau

Đây là chỗ lập trình viên mặc định làm sai, vì thư viện i18n nào cũng bật
fallback sẵn.

### 4.1. Nội dung bán hàng — KHÔNG fallback

Áp dụng cho: tour, điểm đến, khách sạn, tham quan tuỳ chọn, bài viết, buổi
thuyết trình, nhân viên tư vấn.

**Thiếu bản dịch cho locale đang xem thì bản ghi bị ẩn hoàn toàn** khỏi locale
đó: không có trong listing, không có trong tìm kiếm, không có trong mega menu,
không có trong sitemap, và truy cập thẳng URL thì trả 404.

Lý do: khách Đan Mạch vào trang tour thấy tiêu đề tiếng Việt sẽ đóng tab. Ẩn một
tour thì mất một cơ hội bán; hiện tour nửa dịch thì mất lòng tin vào cả website.

Hệ quả phải chấp nhận: **số tour hiển thị khác nhau giữa hai locale.** Mọi con số
đếm trên giao diện phải đếm trong phạm vi `(market, locale)` đang xem, không đếm
toàn bảng. Đây là mở rộng của quy tắc "không hardcode số liệu hiển thị".

### 4.2. Chuỗi giao diện — CÓ fallback

Áp dụng cho: nhãn nút, tiêu đề cột, thông báo lỗi, nhãn form — thứ nằm trong file
message catalog của frontend.

Thiếu khoá cho locale đang xem thì **fallback về `da`**, ghi log cảnh báo, và CI
báo lỗi ở lần build kế tiếp. Lý do: thiếu một nhãn nút không phá vỡ điều gì, còn
để trống thì phá vỡ giao diện.

Kiểm độ phủ chuỗi giao diện là bắt buộc trong CI — xem `00` mục 5.

---

## 5. URL và SEO

### 5.1. Cấu trúc

Locale nằm ở đoạn đầu đường dẫn. **Market không nằm trong URL** — xem 5.3.

```
/da/rejser/vietnam-fra-nord-til-syd
/vi/tour/viet-nam-tu-bac-vao-nam
```

**Cả đoạn đường dẫn lẫn slug đều được dịch.** Không dùng slug tiếng Đan cho
trang tiếng Việt. Bảng ánh xạ đoạn đường dẫn (Next.js gọi là `pathnames`) là dữ
liệu tĩnh trong `web/`, không nằm trong CSDL:

| Trang | `da` | `vi` |
|---|---|---|
| Trang chủ | `/da` | `/vi` |
| Tìm tour | `/da/rejsefinder` | `/vi/tim-tour` |
| Điểm đến | `/da/destinationer/{slug}` | `/vi/diem-den/{slug}` |
| Chi tiết tour | `/da/rejser/{slug}` | `/vi/tour/{slug}` |
| Đặt tour | `/da/booking/{tourId}` | `/vi/dat-tour/{tourId}` |
| Xác nhận | `/da/booking/bekraeftelse/{ref}` | `/vi/dat-tour/xac-nhan/{ref}` |
| Blog | `/da/blog/{slug}` | `/vi/blog/{slug}` |
| Liên hệ | `/da/kontakt` | `/vi/lien-he` |
| Sự kiện | `/da/foredrag` | `/vi/su-kien` |

Tab của trang chi tiết cũng dịch đoạn đường dẫn. Số tab và slug từng tab **khác
nhau theo loại sản phẩm** — bảng đầy đủ ở `05` mục 2.

**Slug không dùng ký tự có dấu.** Slug `da` không dùng `æ ø å`
(`bekraeftelse`, không phải `bekræftelse`); slug `vi` không dùng dấu tiếng Việt
(`viet-nam-tu-bac-vao-nam`). Lý do là gõ được trên mọi bàn phím và dán được vào
mọi ứng dụng chat mà không bị mã hoá phần trăm.

Slug là **cột trong bảng dịch**, không phải cột trên thực thể — xem mục 6.

### 5.2. hreflang

Mọi trang có bản dịch phải khai báo chéo, cộng một `x-default` trỏ về `da`:

```html
<link rel="alternate" hreflang="da" href="https://…/da/rejser/{slug-da}">
<link rel="alternate" hreflang="vi" href="https://…/vi/tour/{slug-vi}">
<link rel="alternate" hreflang="x-default" href="https://…/da/rejser/{slug-da}">
```

Trang chưa có bản `vi` thì **không** khai báo `hreflang="vi"`, và không nằm trong
`sitemap-vi.xml`. Sinh sitemap riêng cho từng locale.

### 5.3. Vì sao market không nằm trong URL

Cân nhắc ba phương án:

| Phương án | Bỏ vì |
|---|---|
| Tên miền riêng mỗi market | Nhân đôi chi phí SEO và vận hành, cho hai thị trường mà một cái còn chưa chắc quy mô |
| Market trong đường dẫn `/dk/da/...` | URL có hai mã trông giống nhau, khách không hiểu; và 90% trường hợp market suy ra được từ locale |
| **Market suy từ locale, ghi đè bằng cookie** ✔ | Chọn cái này |

Cách hoạt động:

1. Market mặc định suy từ locale theo bảng 2.3.
2. Khách đổi được bằng bộ chọn thị trường trên header; lựa chọn lưu vào cookie
   `market`, tồn tại 1 năm.
3. Khi market khác mặc định của locale, giao diện **hiện băng thông báo thường
   trực**: *"Bạn đang xem giá cho thị trường Đan Mạch"* — không để khách nhầm giá.
4. URL canonical chỉ chứa locale. Cookie không tạo ra URL mới, nên không có nội
   dung trùng lặp với công cụ tìm kiếm.
5. Trang render phía máy chủ đọc cookie; trang tĩnh hoá thì phần giá nạp phía
   client. Chi tiết ở `20`.

**Đánh đổi đã biết:** không chia sẻ được đường link kèm market. Chấp nhận ở v1;
nếu cần thì thêm tham số truy vấn `?market=dk` chỉ để ghi cookie rồi chuyển
hướng về URL sạch.

---

## 6. Lưu bản dịch — bảng riêng, không JSONB

Quyết định ở ADR-003.

### 6.1. Khuôn mẫu

Mỗi thực thể có nội dung được tách làm hai bảng: bảng gốc giữ dữ liệu **không
phụ thuộc ngôn ngữ**, bảng dịch giữ mọi chuỗi khách nhìn thấy.

```sql
-- Bảng gốc: chỉ thứ không đổi theo ngôn ngữ
CREATE TABLE tour (
  id                      UUID PRIMARY KEY,
  tour_type               VARCHAR(16)  NOT NULL,
  duration_days           SMALLINT     NOT NULL,
  primary_destination_id  UUID         NOT NULL REFERENCES destination(id),
  min_pax                 SMALLINT,
  max_pax                 SMALLINT,
  hero_image              TEXT         NOT NULL,
  is_new                  BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Bảng dịch: mọi chuỗi khách đọc
CREATE TABLE tour_translation (
  tour_id            UUID         NOT NULL REFERENCES tour(id) ON DELETE CASCADE,
  locale             VARCHAR(8)   NOT NULL REFERENCES locale(code),
  slug               VARCHAR(160) NOT NULL,
  title              VARCHAR(200) NOT NULL,
  short_description  TEXT         NOT NULL,
  long_description   TEXT[]       NOT NULL,
  why_choose_this    TEXT[]       NOT NULL,
  status             VARCHAR(16)  NOT NULL,  -- xem 6.3
  translated_at      TIMESTAMPTZ,
  PRIMARY KEY (tour_id, locale)
);

CREATE UNIQUE INDEX ux_tour_translation_slug
  ON tour_translation (locale, slug);
```

Áp dụng cùng khuôn mẫu cho: `destination`, `region`, `theme`, `hotel`,
`excursion`, `post`, `lecture`, `consultant`, `itinerary_day`.

### 6.2. Vì sao không JSONB

| | Bảng riêng | Cột JSONB |
|---|---|---|
| Ràng buộc `NOT NULL` cho `da` | Có | Không, phải kiểm ở ứng dụng |
| Slug duy nhất theo locale | Index UNIQUE bình thường | Index biểu thức, phức tạp |
| Listing lọc theo locale | Join có index | Toán tử JSONB, kế hoạch truy vấn kém |
| Sắp xếp theo collation của locale | Cột `text` thường, dùng `COLLATE` được | Phải ép kiểu, mất index |
| Biết **từng trường** đã dịch chưa | Đọc thẳng cột | Phải so khoá thủ công |
| Thêm ngôn ngữ mới | Thêm dòng | Thêm khoá, không kiểm được |

Điều quyết định là dòng áp chót: trang quản trị cần bảng điều khiển dịch thuật
biết `title` đã dịch nhưng `long_description` thì chưa. JSONB làm được nhưng mọi
truy vấn đều thành thủ công.

### 6.3. Trạng thái bản dịch

```
DRAFT       Đang viết, không hiện ra ngoài dù có đủ trường
TRANSLATED  Đã dịch xong, chờ duyệt
PUBLISHED   Hiện trên website
OUTDATED    Bản da đã sửa sau khi bản này được dịch — cần dịch lại
```

`OUTDATED` là trạng thái **tính ra**, không nhập tay: bản `vi` là `OUTDATED` khi
`tour_translation(da).updated_at > tour_translation(vi).translated_at`. Cùng một
mô-típ với `GUARANTEED` của ngày khởi hành — trạng thái suy ra từ dữ liệu, không
có cờ để ai đó quên bật.

**Chỉ `PUBLISHED` mới hiện ra ngoài.** `OUTDATED` vẫn hiện — nội dung cũ tốt hơn
là trang trống — nhưng trang quản trị phải cảnh báo, và có danh sách việc cần
dịch lại.

### 6.4. Truy vấn

Join tường minh, **không dùng Hibernate `@Filter`**:

```sql
SELECT t.id, tt.title, tt.slug, tmp.price_from
FROM tour t
JOIN tour_translation tt
  ON tt.tour_id = t.id
 AND tt.locale = :locale
 AND tt.status = 'PUBLISHED'
JOIN tour_market_price tmp
  ON tmp.tour_id = t.id
 AND tmp.market = :market
ORDER BY tt.title COLLATE "da-DK-x-icu";
```

`INNER JOIN` chính là chính sách "không fallback" ở mục 4.1 — bản ghi thiếu bản
dịch tự rơi khỏi kết quả, không cần điều kiện `IF` ở tầng ứng dụng. Đây là lý do
chọn cấu trúc này.

`@Filter` làm được điều tương tự nhưng hoạt động âm thầm: người đọc code không
thấy điều kiện locale ở đâu, và quên bật filter ở một service là rò rỉ nội dung
sai ngôn ngữ. Chỗ này cần nhìn thấy được.

---

## 7. Truyền market và locale qua API

| | Nằm ở đâu | Ví dụ |
|---|---|---|
| Market | Đoạn đường dẫn | `/api/v1/dk/tours` |
| Locale | Header | `Accept-Language: da` |

Market vào đường dẫn vì nó đổi **tài nguyên** — `/dk/tours` và `/vn/tours` là hai
tập khác nhau, giá khác nhau, nên phải là hai URL khác nhau để cache được riêng.

Locale vào header vì nó chỉ đổi **cách trình bày** cùng một tài nguyên. Phản hồi
kèm `Content-Language` và `Vary: Accept-Language`.

**Quy tắc bắt buộc: API không trả câu tiếng người.** Lỗi trả về mã cộng tham số:

```json
{
  "code": "DEPARTURE_SOLD_OUT",
  "params": { "departureId": "…", "departDate": "2027-03-14" }
}
```

Frontend dịch mã sang câu. Nếu backend trả `"Afgangen er udsolgt"` thì bản dịch
tồn tại ở hai nơi và sẽ lệch nhau — và mã lỗi còn dùng được cho log, cho thống
kê, cho test, còn câu chữ thì không.

`MessageSource` của Spring chỉ dùng cho thứ backend thật sự sở hữu và gửi thẳng
tới khách: email xác nhận đặt tour, PDF chương trình, hoá đơn. Danh sách đầy đủ
mã lỗi nằm ở `13`.

---

## 8. Quy trình dịch

```
Người viết nội dung          Người dịch              Người duyệt
        │                        │                       │
   viết bản da               dịch da → vi            duyệt bản vi
   status=DRAFT              status=DRAFT           status=PUBLISHED
        │                        │                       │
   duyệt bản da  ───────────►  vào hàng đợi  ──────►  lên website
   status=PUBLISHED            "cần dịch"
        │
   sửa bản da đã publish
        │
        └──► bản vi tự thành OUTDATED, quay lại hàng đợi
```

Trang quản trị cần đúng ba màn hình cho việc này (chi tiết ở `22`):

1. **Hàng đợi dịch** — bản ghi có `da` là `PUBLISHED` nhưng `vi` thiếu hoặc
   `OUTDATED`, sắp theo mức ưu tiên.
2. **Màn hình dịch song song** — bản `da` bên trái, ô nhập `vi` bên phải, từng
   trường một.
3. **Bảng độ phủ** — mỗi loại thực thể đã dịch bao nhiêu phần trăm.

**Không dùng dịch máy tự động cho nội dung bán hàng.** Nội dung tour là văn bản
thuyết phục có chi tiết địa danh và món ăn; dịch máy ra thứ đọc được nhưng không
bán được. Chuỗi giao diện thì dịch máy rồi người rà lại là chấp nhận được.

---

## 9. Tiền tệ, ngày, số

| | `DK` / `da` | `VN` / `vi` |
|---|---|---|
| Giá | `24.990 kr.` — chấm ngăn nghìn | `18.900.000 ₫` |
| Số chữ số thập phân | 2 (thường không hiện) | 0 |
| Ngày | `14. marts 2027` | `14/03/2027` |
| Giờ | `18:30` (24 giờ) | `18:30` (24 giờ) |
| Ngăn nghìn | `.` | `.` |
| Ngăn thập phân | `,` | `,` |

Backend **không định dạng tiền**. Nó trả số và mã tiền tệ:

```json
{ "amount": "24990.00", "currency": "DKK" }
```

Frontend định dạng bằng `Intl.NumberFormat`. Lý do: định dạng là việc hiển thị,
và cùng một số có thể phải hiện khác nhau ở nhiều chỗ trên cùng một trang.

`amount` là **chuỗi**, không phải số JSON — số thực dấu phẩy động của JavaScript
làm hỏng tiền. Java dùng `BigDecimal`, tuần tự hoá thành chuỗi.

**Giá luôn đi kèm chữ "từ" và disclaimer** ở cả hai locale. Đây là yêu cầu pháp
lý của ngành, không phải lựa chọn thiết kế — chi tiết ở `32`.

---

## 10. Sắp xếp và tìm kiếm

**Sắp xếp ở tầng CSDL, không ở tầng Java.** `String.compareTo()` của Java sắp
theo mã ký tự Unicode, ra kết quả sai cho cả hai ngôn ngữ.

| Locale | Collation | Điều dễ sai |
|---|---|---|
| `da` | `da-DK-x-icu` | `æ ø å` đứng **sau** `z`, không phải cạnh `a` và `o` |
| `vi` | `vi-VN-x-icu` | Thứ tự dấu thanh, và `đ` là chữ cái riêng đứng sau `d` |

```sql
ORDER BY tt.title COLLATE "da-DK-x-icu"
```

Tìm kiếm không dấu dùng `unaccent` cộng `pg_trgm`. Khách gõ `hoi an` phải ra
`Hội An`; khách gõ `halong` phải ra `Halong-bugten`.

Cả hai đều **không mô phỏng được bằng H2**. Test phải chạy trên Postgres thật qua
Testcontainers — xem `33`.

---

## 11. Cái gì KHÔNG dịch

Ghi rõ để không ai dịch nhầm:

| Không dịch | Vì sao |
|---|---|
| Tên địa danh Việt Nam | Viết **không dấu, kiểu quốc tế** ở cả hai locale: `Hanoi`, `Hoi An`, `Halong-bugten`, `Mekongdeltaet`. Bản `vi` được viết có dấu trong văn xuôi nhưng slug thì không |
| Tên khách sạn, tên tàu | Tên riêng |
| Mã tour, mã đặt chỗ | Định danh |
| Mã lỗi | Xem mục 7 |
| Tên nhân viên tư vấn | Tên riêng |
| Đơn vị đo | Giữ hệ mét ở cả hai |

---

## 12. Thêm một ngôn ngữ mới cần làm gì

Danh sách này là phép thử kiến trúc. Nếu thêm `en` cần sửa lược đồ CSDL hoặc sửa
code backend thì thiết kế đã sai.

- [ ] Thêm một dòng vào bảng `locale`
- [ ] Thêm file message catalog trong `web/`
- [ ] Thêm cột ánh xạ đoạn đường dẫn cho locale mới
- [ ] Chọn collation ICU tương ứng
- [ ] Dịch nội dung; bản ghi chưa dịch tự ẩn theo mục 4.1
- [ ] Thêm `sitemap-en.xml` và khai báo hreflang
- [ ] Kiểm font có đủ ký tự — xem `21`

Không có mục nào là "sửa lược đồ" hoặc "sửa backend". Đó là tiêu chí nghiệm thu
của tài liệu này.

---

## 13. Việc còn để ngỏ

| Việc | Chặn cái gì | Ai quyết |
|---|---|---|
| Tỷ lệ đặt cọc và phí xử lý cho market `VN` | `14`, `23`, `30` | Nghiệp vụ |
| Catalog `VN` gồm những tour nào | `01`, dữ liệu mồi | Nghiệp vụ |
| Có bán tour cho khách quốc tế bằng `en` ở v2 không | Mức ưu tiên của mục 12 | Nghiệp vụ |
| Tên miền: một tên miền cho cả hai locale, hay hai | `34` | Kỹ thuật + nghiệp vụ |
