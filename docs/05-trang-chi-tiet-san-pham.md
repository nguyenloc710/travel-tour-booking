# Trang chi tiết — khách thấy gì với từng loại sản phẩm

```
Trạng thái: Nháp
Cập nhật: 31/08/2026
Nguồn sự thật về: thành phần trang chi tiết theo từng loại sản phẩm — đầu trang,
                  tab nào có, thứ tự khối, nút chính, thanh dính đáy, trạng thái rìa.
Không nói về: nghiệp vụ và ràng buộc dữ liệu từng loại (04), bố cục pixel và
              breakpoint (20), luồng đặt sau khi bấm nút (23).
```

Tài liệu này trả lời đúng một câu: **bấm vào một sản phẩm thì thấy gì.**

Ranh giới với `20-frontend-web`: ở đây là **có khối gì, theo thứ tự nào, cho loại
nào**. `20` là **route, breakpoint, và ba trạng thái màn hình**.

---

## 1. Khung chung

Bốn loại tour dài (`GROUP_TOUR`, `INDIVIDUAL_PACKAGE`, `PRIVATE_TOUR`, `CRUISE`)
dùng chung một khung. `COMBO` và `DAY_TOUR` **không** dùng khung này — xem mục 7
và 8.

```
┌──────────────────────────────────────────────────────────┐
│ Header (điện thoại, giờ mở cửa, bộ chọn ngôn ngữ)        │
├──────────────────────────────────────────────────────────┤
│ Breadcrumb                                               │
│ H1 — tên sản phẩm                                        │
│ Dòng phụ — KHÁC NHAU THEO LOẠI (mục 3)                   │
│ ★ rating · số đánh giá        ← chỉ hiện khi có          │
├──────────────────────────────────────────────────────────┤
│ Ảnh hero + bộ ảnh                                        │
├──────────────────────────────────────────────────────────┤
│ Thanh tab — SỐ TAB KHÁC NHAU THEO LOẠI (mục 2)           │
├──────────────────────────────────────────────────────────┤
│ Nội dung tab                                             │
├──────────────────────────────────────────────────────────┤
│ Sản phẩm liên quan (cùng điểm đến, cùng loại)            │
│ Form yêu cầu tư vấn cuối trang                           │
├──────────────────────────────────────────────────────────┤
│ Thanh dính đáy — NÚT KHÁC NHAU THEO LOẠI (mục 4)         │
└──────────────────────────────────────────────────────────┘
```

Tab nằm trên **route con**, không phải state trong component: đổi tab thì URL
đổi, F5 giữ nguyên tab, dán link ra đúng tab đó.

---

## 2. Tab nào có, theo loại

| Tab | `da` | `vi` | `GROUP` | `INDIVIDUAL` | `PRIVATE` | `CRUISE` |
|---|---|---|:--:|:--:|:--:|:--:|
| Tổng quan | `oversigt` | `tong-quan` | ✔ | ✔ | ✔ | ✔ |
| Lịch trình | `dagsprogram` | `lich-trinh` | ✔ | ✔ | ✔ **mẫu** | ✔ |
| Khách sạn | `hoteller` | `khach-san` | ✔ | ✔ | ✔ | ✘ |
| Tàu và cabin | `skib-og-kabiner` | `tau-va-cabin` | ✘ | ✘ | ✘ | **✔** |
| Giá và ngày | `priser-og-datoer` | `gia-va-ngay` | ✔ | ✔ | ✘ | ✔ |
| Bảng giá theo nhóm | `gruppepriser` | `bang-gia-nhom` | ✘ | ✘ | **✔** | ✘ |
| Trưởng đoàn | `rejseleder` | `truong-doan` | ✔ | ✘ | ✔ | tuỳ tour |
| Thời tiết | `vejr` | `thoi-tiet` | ✔ | ✔ | ✔ | ✔ |
| Thông tin thực tế | `praktisk-info` | `thong-tin-thuc-te` | ✔ | ✔ | ✔ | ✔ |
| **Tổng số tab** | | | **7** | **6** | **6** | **6 hoặc 7** |

Ba khác biệt đáng nhớ:

- **`CRUISE` đổi tab Khách sạn thành Tàu và cabin.** Khách ngủ trên tàu, không có
  khách sạn để liệt kê. Giữ tab Khách sạn rỗng là lỗi.
- **`PRIVATE_TOUR` đổi tab Giá và ngày thành Bảng giá theo nhóm.** Không có ngày
  khởi hành để bày, có bậc giá để bày.
- **`INDIVIDUAL_PACKAGE` không có tab Trưởng đoàn.** Không có người đi suốt tuyến.

> **Không hiện tab rỗng.** Thà sáu tab đầy còn hơn bảy tab có một cái viết
> "Đang cập nhật". Tab được dựng từ dữ liệu, không viết cứng danh sách.

---

## 3. Dòng phụ dưới H1 — chỗ khách nhận ra đang xem loại gì

Đây là dòng làm việc nặng nhất trên trang. Ba giây đầu khách phải hiểu sản phẩm
này vận hành thế nào.

| Loại | Dòng phụ |
|---|---|
| `GROUP_TOUR` | `Tour đoàn có trưởng đoàn · 15 ngày · từ 24.990 kr.`<br>`10–25 khách · 7 ngày khởi hành · mức vận động 2/4` |
| `INDIVIDUAL_PACKAGE` | `Tour cá nhân · 14 ngày · từ 19.895 kr.`<br>`Hanoi (4 đêm) · Hoi An (3) · Halong-bugten (2)` |
| `PRIVATE_TOUR` | `Tour riêng · 5 ngày · từ 4.290.000 ₫/khách khi đi 2 người`<br>`Khởi hành theo yêu cầu · báo trước tối thiểu 7 ngày` |
| `CRUISE` | `Du thuyền · 17 ngày · từ 36.990 kr.`<br>`Tàu Mekong Princess · 8 cảng ghé · 4 hạng cabin` |

**`PRIVATE_TOUR` bắt buộc ghi bậc số khách ngay cạnh giá.** "từ 4.290.000 ₫" trần
trụi là hiểu nhầm có hệ thống: khách đi một mình sẽ thấy giá gấp đôi ở bước sau
rồi bỏ đi.

Mọi giá ở đây đều đi qua component giá dùng chung, nên **luôn có chữ "từ" và
disclaimer** — yêu cầu pháp lý, không phải lựa chọn thiết kế.

---

## 4. Thanh dính đáy và nút chính

| Loại | Thanh dính đáy | Nút chính dẫn tới |
|---|---|---|
| `GROUP_TOUR` | Tên tour · `từ 24.990 kr.` · [Tải chương trình] · **[Đặt tour]** | Tab Giá và ngày |
| `INDIVIDUAL_PACKAGE` | Như trên | Tab Giá và ngày |
| `CRUISE` | Như trên | Tab Giá và ngày |
| `PRIVATE_TOUR` | Tên tour · `từ … khi đi 2 người` · **[Yêu cầu báo giá]** | **Mở form báo giá**, không sang tab nào |

> **`PRIVATE_TOUR` không có nút "Đặt tour" ở bất kỳ đâu trên trang.** Loại này
> không chốt tự động được. Đặt nhầm nút là lỗi nghiệp vụ, không phải lỗi chữ.

Thanh dính đáy **cao tối đa 88px ở màn hình hẹp**. Bài học đo được từ bản demo:
header 80 + thanh tab 72 + thanh đặt tour 204 = 356px khung cố định, chiếm 53%
màn hình cao 667px. Ba thanh cộng lại không được vượt 30% chiều cao khung nhìn.

---

## 5. Nội dung từng tab

### 5.1. Tổng quan — thứ tự khối

Thứ tự này lấy từ trang mẫu và giữ nguyên, vì nó đi từ cảm hứng tới chi tiết tới
hành động:

```
1  Đoạn mở đầu — nhấn mùa đẹp nhất trong năm
2  "Vì sao chọn tour này" — 5 gạch đầu dòng
3  Mô tả dài, nhiều đoạn
4  Bản đồ lộ trình + "Bấm xem bản đồ lớn" (lightbox)
5  Bảng lộ trình: Nơi đến | Số đêm | Điểm nhấn
6  Form liên hệ + hồ sơ nhân viên tư vấn phụ trách
7  Trích dẫn khách + rating
8  Bộ ảnh
9  Sản phẩm liên quan cùng điểm đến
10 Form yêu cầu tư vấn cuối trang
```

Khác biệt theo loại:

| Loại | Khác gì |
|---|---|
| `GROUP_TOUR` | Thêm khối "Nhóm nhỏ 10–25 khách" sau khối 2 — đây là điểm bán chính |
| `INDIVIDUAL_PACKAGE` | Khối 5 đổi thành lộ trình theo đêm; thêm dòng "Bạn đi riêng nhóm mình, không ghép khách lạ" |
| `PRIVATE_TOUR` | Thêm khối **"Chuyến đi này điều chỉnh được"** ngay sau khối 1: nói rõ đổi được ngày, số khách, và lộ trình |
| `CRUISE` | Khối 4 là bản đồ hải trình có đánh dấu cảng ghé |

### 5.2. Lịch trình

Mỗi ngày: số ngày · tiêu đề gắn địa danh · mô tả 2–4 câu · **bữa ăn được bao
gồm** · khách sạn ngủ đêm.

- Đêm trên tàu hoả hoặc máy bay: không hiện khách sạn, hiện nhãn "ngủ trên tàu".
- **`PRIVATE_TOUR`**: đầu tab có băng thông báo thường trực —
  *"Đây là lộ trình mẫu. Chúng tôi điều chỉnh theo yêu cầu của bạn."* Thiếu băng
  này là hứa sai với khách.
- Số mục lịch trình **luôn bằng** số ngày của tour. Không có ngoại lệ, và điều
  này được kiểm ở tầng dữ liệu chứ không phải ở giao diện.

### 5.3. Khách sạn — bốn loại tour trừ `CRUISE`

Danh sách chặng nghỉ: tên khách sạn · số sao · số đêm · điểm đến · ảnh · mô tả.
Tổng số đêm hiện ở đầu tab và **phải khớp** số đêm của tour.

### 5.4. Tàu và cabin — chỉ `CRUISE`

```
Thông tin tàu: tên · năm đóng · số cabin · số khách tối đa · tiện ích
Bốn hạng cabin, mỗi hạng: ảnh · diện tích · vị trí trên tàu ·
  có cửa sổ / ban công không · chênh giá so với hạng thấp nhất
Sơ đồ tàu (ảnh tĩnh + lightbox)
```

Chênh giá hiện dạng **phần trăm so với hạng thấp nhất**, không phải số tuyệt đối
— khách so sánh dễ hơn, và số tuyệt đối thay đổi theo ngày khởi hành.

### 5.5. Giá và ngày — `GROUP_TOUR`, `INDIVIDUAL_PACKAGE`, `CRUISE`

Bảng bảy cột: **Ngày đi | Ngày về | Số ngày | Giá phòng đôi | Giá phòng đơn |
Trạng thái | Đặt**.

Đầu bảng là ghi chú giá, nguyên văn ý: *giá "từ", mỗi người, hai người ở phòng
đôi, khởi hành từ Copenhagen, có thể khởi hành từ Billund với phụ thu.* Đây là
yêu cầu pháp lý.

| Loại | Bảng trông thế nào |
|---|---|
| `GROUP_TOUR` | Một dòng một ngày khởi hành. Badge trạng thái ở cột 6 |
| `INDIVIDUAL_PACKAGE` | Như trên, nhưng trạng thái chủ yếu là `PENDING` — kèm chú thích "chúng tôi xác nhận trong 24 giờ" |
| `CRUISE` | **Gộp theo ngày**, mỗi ngày mở ra bốn dòng con theo hạng cabin |

Trạng thái hiển thị: `Đảm bảo khởi hành` · `Còn ít chỗ` · `Hết chỗ` ·
`Chờ chương trình` · không nhãn.

> **Dòng `SOLD_OUT` hiện mờ, không bấm được, không có nút đặt.** Với `CRUISE`,
> `SOLD_OUT` áp cho **một hạng cabin**, không cho cả ngày — ba hạng còn lại vẫn
> đặt được bình thường.

Bảng cuộn ngang **trong khung riêng**, không đẩy cả trang cuộn ngang.

### 5.6. Bảng giá theo nhóm — chỉ `PRIVATE_TOUR`

Thay cho tab Giá và ngày.

```
Số khách        Giá mỗi người        Tổng
2 người         4.290.000 ₫          8.580.000 ₫
3–4 người       3.690.000 ₫          từ 11.070.000 ₫
5–8 người       3.190.000 ₫          từ 15.950.000 ₫
9–15 người      2.890.000 ₫          từ 26.010.000 ₫
16 người trở lên  liên hệ            —
```

Kèm ba thông tin bắt buộc:

- **Giá tham khảo** — giá chính thức nằm trong báo giá gửi riêng
- Báo trước tối thiểu `leadTimeDays` ngày
- Báo giá có hiệu lực `quoteValidDays` ngày kể từ khi gửi

Dưới bảng là **form yêu cầu báo giá**: ngày mong muốn · số khách theo từng loại ·
ghi chú. Ngày sớm hơn `leadTimeDays` bị **chặn ngay trong lịch chọn**, kèm giải
thích — không để khách gửi rồi mới bị từ chối.

### 5.7. Trưởng đoàn — `GROUP_TOUR`, `PRIVATE_TOUR`, một số `CRUISE`

Hồ sơ người dẫn đoàn: ảnh · tên · số năm kinh nghiệm · ngôn ngữ · giới thiệu
ngắn. Với `PRIVATE_TOUR` là mô tả chung về đội ngũ, không phải một người cụ thể —
chưa biết ai dẫn cho tới khi chốt ngày.

### 5.8. Thời tiết

Biểu đồ cột theo tháng: nhiệt độ cao/thấp, lượng mưa. Kèm câu kết luận về mùa
đẹp nhất. Biểu đồ cuộn ngang trong khung riêng.

### 5.9. Thông tin thực tế

Dạng gập mở: thị thực · tiêm chủng · tiền tệ · lệch giờ · điện · trang phục ·
mẹo đi lại. Nội dung cấp quốc gia dùng chung, phần riêng của tour thì ghi đè.

---

## 6. Ba màn hình mẫu

### 6.1. `GROUP_TOUR`

```
Trang chủ › Tour Việt Nam › Việt Nam từ Bắc vào Nam

Việt Nam từ Bắc vào Nam
Tour đoàn có trưởng đoàn · 15 ngày · từ 24.990 kr.
10–25 khách · 7 ngày khởi hành · mức vận động 2/4
★ 4,8 · 380 đánh giá

[ảnh hero]  [bộ ảnh]

[Tổng quan] [Lịch trình] [Khách sạn] [Giá và ngày]
[Trưởng đoàn] [Thời tiết] [Thông tin thực tế]

Tháng 11 tới tháng 3 là mùa đẹp nhất…
Vì sao chọn tour này  •••••
Nhóm nhỏ 10–25 khách
[bản đồ lộ trình]   Nơi đến | Số đêm | Điểm nhấn
[form tư vấn + hồ sơ chuyên viên]  [đánh giá]  [bộ ảnh]
[9 tour liên quan]  [form cuối trang]

┌ Việt Nam từ Bắc vào Nam · từ 24.990 kr. [Tải chương trình] [Đặt tour] ┐
```

### 6.2. `PRIVATE_TOUR`

```
Trang chủ › Tour Việt Nam › Khám phá Hà Giang theo cách của bạn

Khám phá Hà Giang theo cách của bạn
Tour riêng · 5 ngày · từ 4.290.000 ₫/khách khi đi 2 người
Khởi hành theo yêu cầu · báo trước tối thiểu 7 ngày

[Tổng quan] [Lịch trình] [Khách sạn] [Bảng giá theo nhóm]
[Trưởng đoàn] [Thời tiết] [Thông tin thực tế]
                                    ↑ KHÔNG có tab Giá và ngày

Chuyến đi này điều chỉnh được         ← khối riêng của loại này
  Đổi ngày · đổi số khách · thêm bớt điểm dừng

⚠ Đây là lộ trình mẫu. Chúng tôi điều chỉnh theo yêu cầu của bạn.
  Ngày 1 …

┌ Khám phá Hà Giang · từ 4.290.000 ₫ khi đi 2 người [Yêu cầu báo giá] ┐
                                                     ↑ KHÔNG phải "Đặt tour"
```

### 6.3. `CRUISE`

```
Du thuyền sông Mekong
Du thuyền · 17 ngày · từ 36.990 kr.
Tàu Mekong Princess · 8 cảng ghé · 4 hạng cabin

[Tổng quan] [Lịch trình] [Tàu và cabin] [Giá và ngày]
[Thời tiết] [Thông tin thực tế]
                ↑ Tàu và cabin THAY CHO Khách sạn

Tab Giá và ngày:
  ▾ 14/03/2027 – 30/03/2027            Đảm bảo khởi hành
      Cabin trong            36.990 kr.   [Đặt]
      Cabin ngoài            41.990 kr.   [Đặt]
      Có ban công            48.990 kr.   Hết chỗ  ← chỉ hạng này
      Hạng Aqua              53.990 kr.   [Đặt]
```

---

## 7. `COMBO` — không dùng khung tab

Combo không có lịch trình từng ngày, nên bảy tab không có gì để chứa. Dùng **một
trang cuộn, có khung chọn bám bên phải**:

```
Combo Đà Nẵng 3 ngày 2 đêm            ┌────────────────────┐
Combo bay + khách sạn                 │ Khởi hành từ       │
Khởi hành từ Hà Nội                   │  [Hà Nội      ▾]   │
                                      │ Ngày đi  [       ] │
Combo gồm                             │ Ngày về  [       ] │
  ✔ Vé khứ hồi Hà Nội – Đà Nẵng       │ Số khách [2      ] │
  ✔ 2 đêm khách sạn 4★                │                    │
  ✔ Đưa đón sân bay                   │ Tạm tính           │
  ✘ Bữa ăn ngoài bữa sáng             │ 3.990.000 ₫/khách  │
  ✘ Vé tham quan                      │                    │
                                      │ [Chọn chuyến bay]  │
Chọn khách sạn                        └────────────────────┘
  ○ Khách sạn A 4★   +0 ₫
  ○ Khách sạn B 4★   +350.000 ₫
  ○ Khách sạn C 5★   +1.200.000 ₫

Chính sách huỷ                         ← TÁCH THEO THÀNH PHẦN
  Vé bay: không hoàn
  Khách sạn: hoàn 100% trước 3 ngày
```

Ba điều bắt buộc:

1. **Danh sách gồm gì và không gồm gì đặt ngang nhau**, cùng một khối. Combo bán
   được là nhờ khách tin mình biết mình mua gì.
2. **Giá cập nhật ngay khi đổi khách sạn hoặc đổi ngày**, không chờ sang bước sau.
3. **Chính sách huỷ tách theo thành phần.** Một tỷ lệ chung cho cả vé bay lẫn
   phòng là sai, và là loại sai dẫn tới khiếu nại.

---

## 8. `DAY_TOUR` — một trang, không tab

Sản phẩm bốn giờ không chịu nổi bảy tab. Theo chuẩn Klook và GetYourGuide: một
trang cuộn, khung đặt bám bên phải, chọn ngày và khung giờ **ngay trên trang**.

```
Tour Hội An về đêm                    ┌────────────────────┐
Tour trong ngày · 4 giờ · từ 590.000 ₫│ [lịch chọn ngày]   │
Xác nhận ngay · tiếng Việt, tiếng Anh │ Khung giờ          │
                                      │  ○ 16:00 còn 8 chỗ │
Điểm hẹn                              │  ○ 17:30 hết chỗ   │
  Chùa Cầu, 16:00                     │  ● 18:00 còn 3 chỗ │
                                      │ Người lớn  [2]     │
Bạn sẽ trải nghiệm                    │ Trẻ em 5–11 [1]    │
  •••                                 │                    │
                                      │ Tổng 1.475.000 ₫   │
Gồm / không gồm                       │ [Đặt ngay]         │
Chính sách huỷ                        └────────────────────┘
  Miễn phí trước 24 giờ
```

Khác biệt cốt lõi so với tour dài: **tồn kho hiện theo từng khung giờ**. 17:30
hết chỗ không làm 18:00 hết chỗ. Hiện "hết chỗ" cho cả ngày là sai dữ liệu.

---

## 9. Trạng thái rìa — phải thiết kế, không được để mặc

| Tình huống | Trang hiện gì |
|---|---|
| Mọi ngày khởi hành đều `SOLD_OUT` | Trang vẫn mở đầy đủ. Tab Giá và ngày hiện thông báo "Tất cả ngày khởi hành đã kín chỗ", kèm **form nhận thông báo khi mở ngày mới** và 3 tour tương tự. **Không** trả 404 |
| Sản phẩm chưa có bản dịch cho locale đang xem | **404.** Không hiện bản tiếng Đan cho trang tiếng Việt — `02` mục 4.1 |
| Sản phẩm không được gán vào thị trường đang xem | **404** ở thị trường đó. Dịch xong nhưng chưa gán thì vẫn không bán được — cố ý |
| Chưa có ngày khởi hành nào | Trạng thái `PENDING`: "Chương trình đang được hoàn thiện", nút chuyển thành **Yêu cầu tư vấn** |
| Chưa có đánh giá | Ẩn hẳn dòng rating. Không hiện `★ 0` hay "chưa có đánh giá" |
| `PRIVATE_TOUR` mà mọi ngày gần đều dưới `leadTimeDays` | Lịch chọn vô hiệu hoá những ngày đó, kèm giải thích tại chỗ |
| Ảnh hỏng hoặc thiếu | Ảnh dự phòng theo điểm đến, không phải ô xám |

Ba trạng thái chung của mọi màn hình có dữ liệu — đang tải bằng skeleton, rỗng
kèm hướng dẫn, lỗi — vẫn áp dụng đầy đủ. Chi tiết ở `20`.

---

## 10. Quy tắc bắt buộc rút ra

1. **Tab dựng từ dữ liệu, không viết cứng.** Loại nào có tab nấy; không hiện tab
   rỗng.
2. **`CRUISE` đổi tab Khách sạn thành Tàu và cabin.**
3. **`PRIVATE_TOUR` đổi tab Giá và ngày thành Bảng giá theo nhóm**, và **không có
   nút "Đặt tour" ở bất kỳ đâu trên trang**.
4. **Dòng phụ dưới H1 khác nhau theo loại** — đây là chỗ khách nhận ra sản phẩm
   vận hành thế nào.
5. **Giá `PRIVATE_TOUR` luôn kèm bậc số khách**, kể cả trên thanh dính đáy.
6. **Tab lịch trình của `PRIVATE_TOUR` phải có băng "đây là lộ trình mẫu".**
7. **`SOLD_OUT` của `CRUISE` áp cho một hạng cabin, không cho cả ngày.**
8. **`COMBO` và `DAY_TOUR` không dùng khung tab.**
9. **Chính sách huỷ của `COMBO` tách theo thành phần.**
10. **Tồn kho `DAY_TOUR` hiện theo từng khung giờ.**
11. **Ba thanh cố định — header, tab, thanh đáy — cộng lại không quá 30% chiều
    cao khung nhìn.**
12. **Hết chỗ không phải 404.** Trang vẫn mở, và chuyển sang thu yêu cầu tư vấn.
