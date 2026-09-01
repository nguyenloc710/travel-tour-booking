# Mô hình dữ liệu

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: các thực thể, quan hệ giữa chúng, tầng dịch, cách mô hình hoá
                  sáu loại sản phẩm, vòng đời bản ghi, những gì KHÔNG lưu.
Không nói về: DDL và index cụ thể (12), công thức tính (14), API (13).
```

Tài liệu này nói **có những thực thể nào và vì sao**. `12` nói **viết chúng ra
SQL thế nào**.

---

## 1. Bảy quyết định mô hình hoá

Đọc bảy điều này trước khi xem ERD; chúng giải thích vì sao ERD trông như vậy.

| # | Quyết định | Vì sao |
|---|---|---|
| 1 | Mọi chuỗi khách đọc nằm ở bảng `*_translation` | ADR-003. `INNER JOIN` chính là chính sách không-fallback |
| 2 | Mỗi loại sản phẩm có **bảng con riêng**, không dùng một bảng rộng | ADR-005, mục 3 |
| 3 | `departure` thuộc về **một thị trường**, không dùng chung | Mục 4 |
| 4 | `product_market` là **cổng chặn**: chưa gán thì không bán được | Mục 5 |
| 5 | Đơn đặt **chụp lại** bảng giá lúc đặt, không tính lại về sau | Mục 7 |
| 6 | Bảy giá trị là **tính ra, không lưu** | Mục 8 |
| 7 | Khoá chính là **UUID v7**, không phải `BIGSERIAL` | Mục 9 |

---

## 2. Sơ đồ tổng thể

```
            CẤU HÌNH                        ĐỊA LÝ
  ┌──────────────────────┐        ┌─────────────────────────┐
  │ market               │        │ region ──< destination  │
  │ locale               │        │   │           │         │
  │ pax_type    (market) │        │   └ translation ┘       │
  │ departure_origin     │        │ hotel ──┐ excursion ──┐ │
  └──────────┬───────────┘        └─────────┼────────────┼──┘
             │                              │            │
             │            SẢN PHẨM          │            │
             │  ┌───────────────────────────┴────────────┴──┐
             │  │ product ──< product_translation           │
             │  │    │                                      │
             │  │    ├──< product_group_tour     ┐          │
             │  │    ├──< product_individual     │ đúng MỘT │
             │  │    ├──< product_private        │ bảng con │
             │  │    ├──< product_cruise         │ theo     │
             │  │    ├──< product_combo          │ product_ │
             │  │    └──< product_day_tour       ┘ type     │
             │  │    │                                      │
             │  │    ├──< route_leg ──< translation         │
             │  │    ├──< itinerary_day ──< translation     │
             │  │    ├──< product_hotel_stay ──> hotel      │
             │  │    ├──< product_destination               │
             │  │    └──< product_theme ──> theme           │
             │  └───────────────┬──────────────────────────┘
             │                  │
             └──────────┬───────┘
                        │
        ┌───────────────▼─────────────────────────────┐
        │ product_market   (product × market)         │  ← CỔNG CHẶN
        │   is_published · price_from                 │
        └───────────────┬─────────────────────────────┘
                        │
        ┌───────────────▼──────────────┐   ┌────────────────────┐
        │ departure  (thuộc 1 market)  │   │ price_tier         │
        │   ──< departure_price        │   │  (PRIVATE_TOUR)    │
        │   ──< seat_hold              │   └────────────────────┘
        └───────────────┬──────────────┘
                        │
        ┌───────────────▼──────────────────────────────┐
        │ booking ──< booking_passenger                │
        │         ──< booking_line   ← CHỤP LẠI GIÁ    │
        │ quote   ──< quote_line     (PRIVATE_TOUR)    │
        │ lead · newsletter_subscription               │
        └──────────────────────────────────────────────┘

            NỘI DUNG                     NGƯỜI DÙNG NỘI BỘ
  ┌──────────────────────────┐     ┌──────────────────────┐
  │ post · review            │     │ staff_user           │
  │ consultant · lecture     │     │ role                 │
  │ site_info                │     │ staff_user_role      │
  │ (đều có bảng translation)│     └──────────────────────┘
  └──────────────────────────┘
```

---

## 3. Sáu loại sản phẩm — bảng con, không phải bảng rộng

`04` cho thấy sáu loại khác nhau ở đúng năm chiều, và **trường riêng của chúng
gần như không giao nhau**: `min_pax` chỉ có nghĩa với tour đoàn, `price_tiers`
chỉ với tour riêng, `components` chỉ với combo.

Ba cách mô hình hoá:

| | Một bảng rộng, cột nullable | **Bảng con theo loại** | Cột JSONB |
|---|---|---|---|
| Ràng buộc `NOT NULL` cho trường bắt buộc của loại | **Không được** — `min_pax` phải nullable vì tour riêng không có | **Được** | Không |
| `CHECK (min_pax BETWEEN 10 AND 25)` | Phải kèm `OR min_pax IS NULL` — mất tác dụng | **Sạch** | Không |
| Đọc lược đồ hiểu được loại nào có gì | Không — 40 cột lẫn lộn | **Có** | Không |
| Thêm loại mới | Thêm cột vào bảng đang chạy | **Thêm bảng** | Không đổi |
| Truy vấn listing chung | 1 bảng | 1 bảng `product` là đủ¹ | 1 bảng |

¹ Điểm mấu chốt: **truy vấn listing không cần bảng con.** Thẻ sản phẩm lấy dữ
liệu từ `product` + `product_translation` + `product_market`; trường riêng của
loại chỉ cần khi vào trang chi tiết, khi đó đã biết loại nên join đúng một bảng.

Chọn **bảng con** (ADR-005). Ràng buộc: mỗi `product` có **đúng một** dòng ở
đúng bảng con khớp `product_type` — cưỡng chế bằng khoá ngoại kép, xem `12`.

| `product_type` | Bảng con | Trường riêng |
|---|---|---|
| `GROUP_TOUR` | `product_group_tour` | `min_pax`, `max_pax`, `guaranteed_threshold`, `tour_leader_language`, `fitness_level` |
| `INDIVIDUAL_PACKAGE` | `product_individual` | `min_party_size`, `flexible_date_window_days` |
| `PRIVATE_TOUR` | `product_private` | `is_customisable`, `lead_time_days`, `quote_valid_days`, `min_party_size` |
| `CRUISE` | `product_cruise` | `ship_name`, `port_count` |
| `COMBO` | `product_combo` | `nights`, `origin_city`, `valid_from`, `valid_to` |
| `DAY_TOUR` | `product_day_tour` | `duration_hours`, `meeting_point`, `instant_confirm`, `cutoff_hours` |

Bảng phụ đi kèm: `product_cruise_cabin`, `product_combo_component`,
`product_combo_hotel_option`, `product_combo_blackout`, `product_day_tour_slot`,
`price_tier`.

**`product.duration_days` để `NULL` với `DAY_TOUR`** — sản phẩm bốn giờ không có
số ngày. Đây là cột duy nhất của bảng gốc được phép rỗng theo loại.

---

## 4. `departure` thuộc về một thị trường

Cân nhắc hai cách:

| | `departure` dùng chung, giá theo market | **`departure` thuộc một market** |
|---|---|---|
| Cùng ngày 14/03 bán ở cả hai thị trường | Một dòng | Hai dòng |
| Đoàn Đan và đoàn Việt là cùng một đoàn? | Ngầm hiểu là có | **Không, và đúng là không** |
| Số chỗ | Phải chia số chỗ giữa hai thị trường — không mô hình hoá được | Mỗi đoàn số chỗ riêng |
| Trưởng đoàn | Một người nói hai thứ tiếng? | Mỗi đoàn một trưởng đoàn |

Chọn cách thứ hai. Lý do thực tế: **đoàn khách Đan bay từ Copenhagen có trưởng
đoàn nói tiếng Đan; đoàn khách Việt khởi hành từ Hà Nội có hướng dẫn viên tiếng
Việt. Đó là hai chuyến đi khác nhau, chỉ trùng lộ trình.** Ép chúng vào một bản
ghi rồi chia số chỗ là mô hình sai với thực tế.

Hệ quả: `price_from` nằm ở `product_market`, tính từ `departure` của đúng thị
trường đó — hai thị trường hai giá "từ" khác nhau, đúng như `04` mục 4.1 yêu cầu.

### 4.1. Du thuyền — nhiều dòng một ngày

`CRUISE` có **bốn dòng `departure` cho cùng một ngày**, mỗi hạng cabin một dòng.
Khoá duy nhất là `(product_id, market, depart_date, cabin_category)`; với các loại
khác `cabin_category` là `NULL` và khoá rút về `(product_id, market, depart_date)`.

Tồn kho, trạng thái và `SOLD_OUT` tính **trên từng dòng**, tức từng hạng cabin.
Giao diện gộp lại theo ngày — việc gộp là của tầng hiển thị, không phải của dữ liệu.

---

## 5. `product_market` là cổng chặn

```
product_market (product_id, market)
  is_published   BOOLEAN
  price_from     NUMERIC   ← tính từ departure rẻ nhất của market này
  published_at   TIMESTAMPTZ
```

Một sản phẩm hiện ra ở một thị trường khi **cả ba** đúng:

```
product_market.is_published = true
AND tồn tại product_translation cho locale đang xem, status = 'PUBLISHED'
AND (loại có departure) → tồn tại departure của market đó
```

Ba điều kiện độc lập, và đó là chủ ý: **dịch xong nhưng chưa gán thị trường thì
vẫn không bán được**. Nhân viên nội dung và nhân viên kinh doanh làm hai việc
khác nhau, không ai vô tình mở bán thay người kia.

---

## 6. Tầng dịch

Khuôn mẫu áp dụng cho: `region`, `destination`, `product`, `route_leg`,
`itinerary_day`, `hotel`, `excursion`, `theme`, `post`, `lecture`, `consultant`,
`site_info`.

```
<entity>              dữ liệu KHÔNG phụ thuộc ngôn ngữ
<entity>_translation  (entity_id, locale) → mọi chuỗi khách đọc
                      + slug, status, translated_at, last_modified_at
```

Quy tắc:

- **Bắt buộc có dòng `locale = 'da'`** — ADR-004, cưỡng chế ở `12`.
- `slug` duy nhất theo `(locale, slug)`, **không** duy nhất toàn cục.
- `status ∈ {DRAFT, TRANSLATED, PUBLISHED}`. **`OUTDATED` không lưu** — nó là
  giá trị tính ra, xem mục 8.
- Ảnh không nằm trong bảng dịch: cùng một ảnh cho mọi ngôn ngữ. Nhưng **`alt`
  text thì nằm trong bảng dịch** — nó là chữ khách đọc, và là yêu cầu tiếp cận.

### 6.1. Cái gì không có bảng dịch

| Thực thể | Vì sao |
|---|---|
| `departure` | Không có chuỗi nào ngoài `extension_variant` — đưa chuỗi này vào `departure_translation` riêng |
| `booking`, `quote`, `lead` | Dữ liệu giao dịch. Lưu `locale` khách dùng lúc đặt để gửi email đúng tiếng |
| `market`, `pax_type`, `departure_origin` | Nhãn hiển thị là **khoá chuỗi giao diện** trong `web/`, không phải nội dung CSDL |
| `review` | **Có** bảng dịch, nhưng bản gốc không dịch — xem mục 6.2 |

### 6.2. Đánh giá của khách — không dịch máy

`review` có `original_locale`. Đánh giá viết tiếng Đan **hiện nguyên văn tiếng
Đan** cho khách Việt, kèm nhãn ghi rõ ngôn ngữ gốc. Dịch lời khách là sửa lời
người khác; và đánh giá dịch máy đọc ra giả ngay.

Đây là **ngoại lệ có chủ ý** của chính sách không-fallback ở `02` mục 4.1: đánh
giá thiếu bản dịch thì vẫn hiện, không ẩn.

---

## 7. Đơn đặt chụp lại giá

`booking_line` là **bản chụp** bảng phân rã giá tại thời điểm đặt:

```
booking_line (booking_id, seq)
  line_key        'BASE' | 'SINGLE_SUPPLEMENT' | 'CABIN_UPGRADE'
                | 'ORIGIN_SURCHARGE' | 'INSURANCE' | 'PRE_FLIGHT_HOTEL'
                | 'EARLY_BIRD' | 'PROCESSING_FEE'
  label_key       khoá chuỗi giao diện, KHÔNG phải câu tiếng người
  quantity        số khách hoặc số đêm
  unit_amount     NUMERIC
  amount          NUMERIC — âm là dòng giảm trừ
```

**Không bao giờ tính lại giá của đơn cũ.** Bảng giá đổi, tỷ lệ đặt cọc đổi, phí
xử lý đổi — đơn đã đặt phải giữ nguyên con số khách đã nhìn thấy và đã đồng ý.
Tính lại là sai về mặt pháp lý, không chỉ sai về mặt kỹ thuật.

Đơn cũng chụp lại: `market`, `locale`, `currency`, `product_title` (tên sản phẩm
lúc đặt — sản phẩm có thể đổi tên sau).

`quote_line` dùng đúng khuôn mẫu này.

---

## 8. Bảy giá trị tính ra, không lưu

Không có cột nào trong CSDL cho những thứ này. Có cột là có ai đó quên cập nhật.

| Giá trị | Công thức |
|---|---|
| `GUARANTEED` | `seats_booked >= guaranteed_threshold` |
| `FEW_SEATS` | `seats_available <= 3` |
| `SOLD_OUT` | `seats_available <= 0` |
| `seats_available` | `capacity − seats_booked − Σ seat_hold còn hạn` |
| Số chỗ còn của buổi thuyết trình | `seats − seats_taken` |
| `OUTDATED` của bản dịch | `translation(da).last_modified_at > translation(vi).translated_at` |
| `price_from` của một thị trường | `MIN(departure_price.amount)` của thị trường đó, **chỉ** dòng `occupancy = 'DOUBLE'` của loại khách `ADULT`, và chỉ trên `departure` chưa xoá mềm |

**`price_from` là ngoại lệ được vật chất hoá**: nó nằm ở `product_market` như một
cột thật, vì mọi trang listing đều cần và tính lại mỗi lần thì quá đắt. Nhưng nó
được **cập nhật bằng trigger** khi `departure_price` đổi, không cập nhật bằng tay
ở tầng ứng dụng. Kiểm dữ liệu phải có một quy tắc đối chiếu lại cột này với thực tế.

Hai điều kiện thu hẹp trong công thức là **định nghĩa của `03`**, không phải tối
ưu: "Giá từ" là giá thấp nhất *cho 1 người khi 2 người ở phòng đôi*. Bỏ
`occupancy = 'DOUBLE'` thì phụ thu phòng đơn lọt vào; bỏ điều kiện loại khách thì
`min()` vớ phải **giá trẻ em**, và website quảng cáo giá trẻ em như giá tour —
chuyện pháp lý (`32`), không phải chuyện hiển thị. Loại khách nào là loại tính giá
niêm yết thì chưa tài liệu nào chốt; hiện khoá cứng `ADULT`, ghi ở `12` mục 10.

Kiểu enum **lưu** trong CSDL loại trừ `GUARANTEED`:

```
departure_base_status ∈ {OPEN, FEW_SEATS, SOLD_OUT, PENDING}
```

`FEW_SEATS` và `SOLD_OUT` có mặt trong kiểu lưu vì nhân viên **ghi đè tay** được
(ví dụ đóng bán sớm dù còn chỗ). Trạng thái hiển thị = giá trị nghiêm ngặt hơn
giữa giá trị lưu và giá trị tính. `GUARANTEED` thì không ghi đè tay được.

---

## 9. Khoá chính, thời gian, tiền

**Khoá chính: `UUID v7`.** Sinh ở tầng ứng dụng.

| | `BIGSERIAL` | `UUID v4` | **`UUID v7`** |
|---|---|---|---|
| Đoán được id đơn hàng kế tiếp | **Có — rò rỉ số liệu kinh doanh** | Không | Không |
| Sinh id trước khi ghi CSDL | Không | Có | Có |
| Cục bộ theo thời gian trong index | Có | **Không — index phân mảnh** | Có |

**Mã tra cứu đơn** là chuỗi riêng, không phải id: `VN-2026-8F3K2P` — khách đọc
được qua điện thoại, và không lộ id nội bộ.

**Thời gian**: `TIMESTAMPTZ` cho mọi mốc thời gian hệ thống, lưu UTC.
**`DATE` cho ngày khởi hành** — ngày 14/03 là ngày 14/03 ở mọi múi giờ, không
phải một mốc thời gian.

**Tiền**: `NUMERIC(12,2)` + cột `currency VARCHAR(3)` luôn đi cặp. Không bao giờ
`FLOAT`. VND 0 chữ số thập phân và DKK 2 — số chữ số lấy từ `market`, không
hardcode; cột vẫn để scale 2 cho cả hai để tránh phải đổi kiểu khi thêm thị trường.

---

## 10. Vòng đời

### 10.1. Bản ghi nội dung

```
DRAFT ──► PUBLISHED ──► ARCHIVED
   ▲          │
   └──────────┘  (gỡ xuống sửa)
```

**Không xoá cứng nội dung.** `ARCHIVED` ẩn khỏi mọi truy vấn của website khách
nhưng giữ lại, vì đơn đặt cũ còn trỏ tới.

> **`ARCHIVED` và `soft_delete` là hai thứ khác nhau, đừng dùng lẫn.**
>
> | | Nghĩa | Ai đặt | Đưa lại được không |
> |---|---|---|---|
> | `status = 'ARCHIVED'` | Nội dung còn đó, **có chủ đích** ngừng bán | Biên tập viên | Có, là thao tác bình thường |
> | `soft_delete = TRUE` | Nhân viên đã **xoá**; dòng chỉ còn để giữ toàn vẹn tham chiếu | Người có quyền xoá | Được, nhưng là thao tác khôi phục, không phải quy trình |
>
> Hai cơ chế cùng ẩn nội dung là chỗ dễ sinh bug: một bản ghi `ARCHIVED` chưa
> xoá vẫn phải hiện trong trang quản trị, còn bản ghi `soft_delete` thì không.

### 10.2. Đơn đặt

```
DRAFT ──► PENDING_PAYMENT ──► CONFIRMED ──► COMPLETED
   │             │                │
   │             ▼                ▼
   └────────► EXPIRED         CANCELLED ──► REFUNDED
```

`PENDING_CONFIRMATION` chèn giữa `PENDING_PAYMENT` và `CONFIRMED` cho
`INDIVIDUAL_PACKAGE` — đơn đã trả tiền nhưng chờ nhân viên xác nhận trong 24 giờ
(`04` mục 4.2). Đây là lý do vòng đời không thể là một chuỗi thẳng dùng chung.

**Đơn đặt không bao giờ bị xoá.** Kể cả đơn `EXPIRED` — chúng là dữ liệu phân tích
tỷ lệ bỏ giỏ.

### 10.3. Báo giá — chỉ `PRIVATE_TOUR`

```
DRAFT ──► SENT ──► ACCEPTED ──► (sinh ra booking)
            │
            ├──► EXPIRED   (quá quote_valid_days, tự động)
            └──► REJECTED
```

`quote` và `booking` là **hai bảng riêng**, không phải một bảng có cột trạng thái.
Hai vòng đời khác nhau, hai bộ trường bắt buộc khác nhau, và một báo giá có thể
không bao giờ thành đơn.

---

## 11. Vết kiểm toán và xoá mềm

### 11.1. Năm cột chuẩn

Mỗi bảng thuộc nhóm A và B ở mục 11.2 mang đúng năm cột này, tên giống hệt nhau
ở mọi bảng:

| Cột | Kiểu | Ai ghi |
|---|---|---|
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | CSDL |
| `created_by` | `UUID NULL → staff_user` | Ứng dụng |
| `last_modified_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | **Trigger CSDL** |
| `last_modified_by` | `UUID NULL → staff_user` | Ứng dụng |
| `soft_delete` | `BOOLEAN NOT NULL DEFAULT FALSE` | Ứng dụng |

**Chia việc giữa CSDL và ứng dụng là có chủ đích.** `last_modified_at` do trigger
đặt vì nó *luôn* đúng và không ai quên được; `last_modified_by` thì CSDL không
biết ai đang thao tác nên ứng dụng phải ghi. Đừng để ứng dụng đặt
`last_modified_at` — sẽ có chỗ quên, và cột "lần sửa cuối" sai còn tệ hơn không có.

**`created_by` và `last_modified_by` cho phép NULL**, và NULL có ba nghĩa hợp lệ:
migration tạo ra, job nền tạo ra, hoặc **khách tự tạo** — v1 không có tài khoản
khách nên đơn đặt do khách tạo không có `staff_user` nào đứng tên. Ai làm gì với
đơn thì đọc `booking_event`, không đọc `created_by`.

### 11.2. Bảng nào nhận cột nào

| Nhóm | Bảng | 4 cột thời gian/người | `soft_delete` |
|---|---|---|---|
| **A** — thực thể nghiệp vụ | `region` · `destination` · hai bảng dịch của chúng · `staff_user` · `consultant` · `product` · `product_translation` · `departure` · `price_tier` · `seat_hold` · `quote` · `booking` · `pax_type` · `departure_origin` · `media_asset` · `media_asset_translation` · **`staff_user_role`** | ✔ | ✔ |
| **B** — cấu hình do migration quản | `market` · `locale` · `role` | ✔ | ✗ |
| **C** — bảng con 1-1 và bảng dòng chi tiết | `product_group_tour` … `product_day_tour` · `product_market` · `departure_price` · `booking_line` · `quote_line` · `booking_passenger` · `product_image` | ✗ | ✗ |
| **D** — nhật ký chỉ ghi thêm | `booking_event` · `slug_history` | ✗ | ✗ |

Lý do ba nhóm bị loại — mỗi lý do là một cái bẫy thật, không phải sự cầu kỳ:

**B không có `soft_delete`** vì đã có `is_active`. Hai cột cùng nghĩa "không dùng
được nữa" là chỗ chắc chắn sẽ lệch nhau: ai đó tắt cột này, truy vấn lọc theo cột
kia. Một khái niệm một cột.

**C không có cột nào.** Vòng đời của chúng trùng khít với bảng cha và đã có
`ON DELETE CASCADE`. Thêm `last_modified_by` vào `product_group_tour` tạo ra hai
câu trả lời cho câu hỏi "ai sửa sản phẩm này", và chúng sẽ khác nhau. Sửa gì trên
sản phẩm thì `product.last_modified_by` là câu trả lời duy nhất.

**`staff_user_role` nằm ở nhóm A dù nó là bảng nối** — ngoại lệ duy nhất của
quy tắc "bảng nối thuộc nhóm C". Cấp và thu quyền là sự kiện an ninh: câu hỏi "ai
cho người này quyền `ADMIN`, lúc nào" phải trả lời được, và xoá cứng một dòng là
xoá bằng chứng. Thu quyền vì thế là `soft_delete = TRUE`, không phải `DELETE`.

**D không có cột nào, và đây là điều quan trọng nhất.** `booking_event` là nhật
ký kiểm toán. Thêm `last_modified_by` vào một nhật ký kiểm toán là cho phép sửa
nó — phá đúng cái tính chất khiến nó có giá trị. Thêm `soft_delete` còn tệ hơn:
cho phép **giấu lịch sử**. Khi khách khiếu nại "tôi không hề huỷ", đây là chỗ duy
nhất trả lời được, nên nó phải là bảng không sửa được, không xoá được. Bảng này
đã có sẵn `created_at` cùng `actor_type` và `actor_id`.

`slug_history` cùng nhóm vì cùng tính chất: nó là lời hứa rằng một URL cũ vẫn dẫn
tới đúng chỗ. Sửa được nó là làm được chuyện tệ hơn link chết — dẫn khách tới sai
sản phẩm.

### 11.3. Xoá mềm ẩn lỗi rất giỏi — ba hệ quả bắt buộc

Xoá mềm có đúng cái tính chất mà dự án này đã bác bỏ một lần rồi khi từ chối
Hibernate `@Filter` (ADR-003): **nó hoạt động âm thầm**. Quên lọc ở một chỗ là rò
dữ liệu đã xoá ra khách, và không có lỗi nào nổ.

**a) Mọi khoá duy nhất phải thành index bộ phận.** `ux_product_translation_slug`
mà không kèm `WHERE NOT soft_delete` thì slug của một tour đã xoá **chiếm chỗ
vĩnh viễn** — biên tập viên xoá tour rồi tạo lại với cùng slug sẽ bị từ chối mà
không hiểu vì sao. Chi tiết và ngoại lệ: `12` mục 2.2.

**b) Xoá mềm KHÔNG lan xuống dưới.** `ON DELETE CASCADE` chỉ chạy khi xoá cứng.
Đặt `product.soft_delete = TRUE` không đụng gì tới `product_translation` hay
`departure` của nó. Hoặc use case phải xoá mềm cả chùm trong một transaction,
hoặc truy vấn phải join lên bảng cha để lọc. Chọn cách nào cũng được, nhưng phải
chọn một và ghi rõ — để lửng là chỗ dữ liệu ma xuất hiện.

**c) `booking` xoá mềm chỉ dành cho sai sót thật.** Đơn của khách không bị "xoá",
nó chuyển sang `CANCELLED` — mục 10.2. `soft_delete` trên `booking` chỉ dùng cho
đơn trùng hoặc đơn thử. Dùng nó để giấu một giao dịch có thật là hành vi phải
ngăn ở tầng quyền, không phải ở tầng lược đồ.

### 11.4. Vết kiểm toán ngoài năm cột

Năm cột trên trả lời "ai sửa lần cuối". Chúng **không** trả lời "đã sửa những gì".

| Loại bảng | Cần thêm |
|---|---|
| Bản dịch | `translated_at`, `translated_by` — khác `last_modified_*`: dịch xong khác với sửa chính tả |
| Đơn đặt, báo giá | **Bảng `booking_event`** ghi mọi lần đổi trạng thái: ai, khi nào, từ đâu sang đâu |
| Giá | Đổi giá của `departure` đã có đơn thì ghi vết. Đơn cũ không đổi theo — mục 7 |

Cần xem lại được **bản cũ** của một nội dung thì phải có bảng `*_revision` —
chưa làm ở v1, xem mục 13.

---

## 12. Dữ liệu cá nhân

Đánh dấu ngay từ mô hình, đừng để tới `31`:

| Bảng | Chứa dữ liệu cá nhân | Thời hạn lưu |
|---|---|---|
| `booking_passenger` | Tên, ngày sinh, số hộ chiếu | Theo yêu cầu kế toán, sau đó ẩn danh |
| `booking` | Email, điện thoại, địa chỉ | Như trên |
| `lead` | Tên, email, điện thoại | Xoá sau N tháng nếu không thành đơn |
| `newsletter_subscription` | Email | Tới khi huỷ đăng ký |
| `quote` | Như `lead` | Theo `booking` nếu thành đơn |

**Số hộ chiếu là dữ liệu nhạy cảm** ở cả GDPR lẫn NĐ 13/2023. Đề xuất: chỉ thu
thập khi thật sự cần (tour quốc tế), mã hoá ở tầng cột, và **không bao giờ** trả
về qua API đọc — chỉ ghi vào, không đọc ra.

---

## 13. Việc còn để ngỏ

| Việc | Chặn cái gì | Ghi chú |
|---|---|---|
| Có nhập dữ liệu từ hệ thống cũ không | Khối lượng `12`, dữ liệu mồi | `01` mục 7 |
| Thời hạn lưu dữ liệu cá nhân từng loại | `31`, cột `retention_until` | Cần người có chuyên môn pháp lý |
| Có cần lịch sử phiên bản nội dung (xem lại bản cũ) không | Thêm bảng `*_revision` | Nghiêng về **không** ở v1 |
| Tour liên quốc gia mô hình thế nào | `product.primary_destination_id` hiện giả định một nước | `04` mục 13 |
