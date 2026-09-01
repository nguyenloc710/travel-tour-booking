# Phân loại sản phẩm và nghiệp vụ từng loại

```
Trạng thái: Nháp
Cập nhật: 31/08/2026
Nguồn sự thật về: danh sách loại sản phẩm, năm chiều phân biệt, nghiệp vụ và
                  ràng buộc dữ liệu của từng loại, thẻ sản phẩm, bộ lọc.
Không nói về: trang chi tiết hiển thị gì (05), công thức tính giá chi tiết (14),
              lược đồ CSDL (12), bố cục màn hình (20), phân quyền (22).
```

Khảo sát ngày **31/08/2026**. Nguồn:

| Nguồn | Vai trò |
|---|---|
| stjernegaard-rejser.dk/vietnam/rundrejser-vietnam | Mô hình nhà điều hành Bắc Âu — thị trường `DK` |
| ivivu.com/du-lich | Mô hình OTA Việt Nam — thị trường `VN` |
| travel.com.vn — tour trọn gói và combo bay + khách sạn | Combo và tour nội địa |
| Intrepid, G Adventures | Chuẩn quốc tế cho tour đoàn nhỏ |
| Klook, GetYourGuide | Chuẩn quốc tế cho tour trong ngày và hoạt động |

---

## 1. Phát hiện chính: hai triết lý sản phẩm khác nhau

Các trang tham khảo không cùng một mô hình, và khác biệt của chúng **trùng khớp
với hai thị trường của dự án này**:

| | Nhà điều hành Bắc Âu (Stjernegaard) | OTA Việt Nam (iVivu, Vietravel) |
|---|---|---|
| Bán cái gì | **Catalog hành trình do mình soạn** | **Chợ ghép các thành phần** |
| Sản phẩm | Rundrejse, individuel rejse, krydstogt | Tour ghép, tour riêng, combo, vé tham quan, vé bay, khách sạn |
| Đặt trước bao lâu | 6–18 tháng | 2–8 tuần |
| Giá trị đơn | 20.000–40.000 kr. | Vài triệu tới vài chục triệu đồng |
| Hiển thị giá | "15 dage fra 24.990 kr." — một giá "từ" | Giá người lớn / trẻ em riêng, giá gốc gạch ngang, giá giảm |
| Điểm khởi hành | Copenhagen mặc định, Billund và Aalborg **phụ thu** | Hà Nội / TP.HCM / Đà Nẵng — là **bộ lọc và biến thể sản phẩm riêng** |
| Chốt đơn | Đặt cọc rồi nhân viên liên hệ | Đặt online, thanh toán ngay |

**Kết luận:** loại sản phẩm nào bán ở thị trường nào là **thuộc tính của
`Market`**, không phải hằng số toàn hệ thống. Quan hệ `product_type ↔ market` là
dữ liệu cấu hình, không phải `if` trong code.

---

## 2. Sáu loại sản phẩm

| Mã | Việt | Đan Mạch | Thị trường | Giai đoạn |
|---|---|---|---|---|
| `GROUP_TOUR` | Tour đoàn có trưởng đoàn | Rundrejse med dansk rejseleder | `DK` `VN` | **v1** |
| `INDIVIDUAL_PACKAGE` | Tour cá nhân lộ trình dựng sẵn | Individuel rejse | `DK` `VN` | **v1** |
| `PRIVATE_TOUR` | Tour riêng theo yêu cầu | Privat rejse | `VN` `DK` | **v1** |
| `CRUISE` | Du thuyền | Krydstogt | `DK` `VN` | **v1** |
| `COMBO` | Combo bay + khách sạn | — | `VN` | v1.5 — xem mục 10 |
| `DAY_TOUR` | Tour trong ngày / hoạt động | Udflugt | `VN` `DK` | v2 — xem mục 11 |

> **`DAY_TOUR` khác `Excursion` đang có.** `Excursion` là tham quan tuỳ chọn
> **gắn vào một tour dài**, khách mua thêm tại chỗ, không đặt riêng được.
> `DAY_TOUR` là sản phẩm **bán độc lập**, có lịch và tồn kho riêng. Đừng gộp hai
> thứ này — xem mục 11.

---

## 3. Năm chiều phân biệt

Sáu loại sản phẩm khác nhau ở **đúng năm chiều**; mọi khác biệt còn lại đều suy
ra được từ chúng.

| | Ai quyết ngày | Đơn vị giá | Tồn kho chung | Cần báo giá | Có trưởng đoàn |
|---|---|---|---|---|---|
| `GROUP_TOUR` | Nhà điều hành | Mỗi người, theo **ngày khởi hành** | **Chỗ** | Không | **Có** |
| `INDIVIDUAL_PACKAGE` | Nhà điều hành (nhiều ngày để chọn) | Mỗi người, theo ngày khởi hành + **loại phòng** | Không¹ | Không | Không² |
| `PRIVATE_TOUR` | **Khách đề xuất** | Mỗi người, theo **bậc số khách** | Không | **Có** | Có, riêng cho đoàn |
| `CRUISE` | Nhà điều hành | Mỗi người, theo **hạng cabin** | **Cabin, theo từng hạng** | Không | Tuỳ tour |
| `COMBO` | **Khách chọn khoảng ngày** | Thành phần: vé bay + đêm phòng | **Từ nhà cung cấp** | Không | Không |
| `DAY_TOUR` | Khách chọn ngày + **khung giờ** | Mỗi người, theo **loại khách** | **Chỗ theo khung giờ** | Không | Có, tại chỗ |

¹ Không có tồn kho chỗ, nhưng có thể chặn bởi phòng khách sạn — xem 4.2.
² Có hướng dẫn viên tại từng điểm, không có người đi suốt tuyến.

**Cách dùng bảng này:** khi phân vân "loại X có cần chức năng Y không", tra năm
chiều thay vì đoán. Ví dụ: `PRIVATE_TOUR` không có tồn kho chung, nên **không cần
giữ chỗ có hạn** — nút của nó dẫn sang luồng yêu cầu báo giá, không dẫn sang
thanh toán.

---

## 4. Nghiệp vụ từng loại

Mỗi loại trình bày theo cùng bảy mục: định nghĩa · trường riêng · ràng buộc dữ
liệu · nghiệp vụ giá · nghiệp vụ tồn kho · huỷ và đổi · cạm bẫy.

---

### 4.1. `GROUP_TOUR` — Tour đoàn có trưởng đoàn

**Định nghĩa.** Khách lạ đi chung một đoàn theo lịch trình và ngày do nhà điều
hành ấn định. Chia sẻ chi phí xe, hướng dẫn viên, một số bữa ăn. Sản phẩm chủ lực
của thị trường `DK`.

**Trường riêng**

| Trường | Ràng buộc |
|---|---|
| `minPax` | 10–25. Dưới ngưỡng này tour có thể bị huỷ |
| `maxPax` | 10–25, `>= minPax` |
| `tourLeaderLanguage` | `da` cho `DK`, `vi` cho `VN` |
| `fitnessLevel` | 1–4. Khách lớn tuổi cần biết trước |
| `guaranteedThreshold` | Mặc định `= minPax`. Tách riêng vì có tour cam kết chạy sớm hơn |

**Ràng buộc dữ liệu — chặn ở tầng CSDL và ở kiểm dữ liệu**

1. `itinerary.length === durationDays`. Không ngoại lệ.
2. Tổng số đêm trong `hotelStays` khớp **chính xác** số đêm có khách sạn đếm được
   trong lịch trình, theo từng khách sạn. Đêm trên tàu hoả hoặc máy bay để trống
   `hotelId` và không tính vào `hotelStays`.
3. Mỗi tour có **ít nhất một** ngày khởi hành ở mỗi thị trường được gán.
4. `priceFrom` khớp ngày khởi hành rẻ nhất **của thị trường đó**. Hai thị trường
   có hai `priceFrom` khác nhau.
5. Giá **phải dao động** giữa các ngày khởi hành của cùng một tour. Mọi ngày cùng
   một giá là dấu hiệu dữ liệu nhập ẩu — cao điểm phải đắt hơn.
6. `returnDate = departDate + days − 1`.
7. Mô tả mỗi ngày lịch trình tối thiểu 80 ký tự — chặn nội dung lấp chỗ trống.

**Nghiệp vụ giá.** Thứ tự cộng dồn cố định, không được đổi (chi tiết ở `14`):

```
giá cơ bản (số khách × giá theo loại khách × loại phòng)
+ phụ thu phòng đơn
+ phụ thu điểm khởi hành khác          ← chỉ DK
+ bảo hiểm đã chọn
+ đêm khách sạn trước ngày gặp đoàn
− giảm đặt sớm (≥6 tháng, tối đa 1.000 kr/người)
+ phí xử lý (cố định một lần mỗi đơn)
= tổng cộng;  đặt cọc = 25% tổng
```

Giảm đặt sớm trừ **trước** phí xử lý — thứ tự quyết định con số cuối.

**Nghiệp vụ tồn kho**

| Sự kiện | Tác động |
|---|---|
| Khách vào bước thanh toán | Tạo `SeatHold`, có hạn (đề xuất 20 phút) |
| Thanh toán thành công | `SeatHold` → `Booking`, `seatsBooked += n` |
| Hết hạn giữ | Job quét trả chỗ về kho |
| Khách huỷ | `seatsBooked -= n`, chỗ về kho ngay |
| `seatsBooked >= guaranteedThreshold` | Trạng thái thành `GUARANTEED` — **tính ra, không lưu** |
| `seatsBooked >= maxPax` | `SOLD_OUT` |
| Còn ≤ 3 chỗ | `FEW_SEATS` |

Chỗ khả dụng = `maxPax − seatsBooked − số chỗ đang giữ chưa hết hạn`. **Phải trừ
cả chỗ đang giữ**, nếu không hai khách cùng mua được chỗ cuối.

**Huỷ và đổi**

- Nhà điều hành huỷ khi tới hạn chốt mà `seatsBooked < minPax` → hoàn 100%, và
  phải báo khách. Đây là nghiệp vụ có thật, không phải trường hợp hiếm.
- Khách huỷ: theo bậc thời gian, chi tiết ở `23`.
- Đổi sang ngày khởi hành khác cùng tour: v1 xử lý thủ công qua nhân viên.

**Cạm bẫy**

- Quên trừ chỗ đang giữ khi tính chỗ còn.
- Lưu cờ `GUARANTEED` trong CSDL thay vì tính ra.
- Dùng chung `priceFrom` cho cả hai thị trường.

---

### 4.2. `INDIVIDUAL_PACKAGE` — Tour cá nhân lộ trình dựng sẵn

**Định nghĩa.** Lộ trình do nhà điều hành soạn sẵn, nhưng khách **đi riêng nhóm
mình**, không ghép khách lạ. Stjernegaard mô tả là *"skræddersyet til dig og dit
behov"*. Khác `PRIVATE_TOUR` ở chỗ: lộ trình **cố định, có sẵn trên kệ**, khách
chọn ngày trong danh sách nhà điều hành mở, và **tra được giá ngay** không cần hỏi.

**Trường riêng**

| Trường | Ghi chú |
|---|---|
| `routeNights` | Lộ trình theo đêm — thứ hiển thị thay cho quy mô đoàn |
| `minPartySize` | Thường 2. Đi một mình thì phụ thu phòng đơn |
| `flexibleDateWindow` | Số ngày xê dịch được quanh ngày công bố |

**Không có `minPax` / `maxPax`** — không có đoàn để giới hạn. Đây là lỗi hay gặp:
gán quy mô đoàn cho tour cá nhân rồi hiển thị "10–25 khách" trên thẻ.

**Ràng buộc dữ liệu.** Giống `GROUP_TOUR` mục 1, 2, 3, 4, 6, 7. **Không áp dụng**
quy tắc 5 (giá dao động) — tour cá nhân thường có bảng giá theo mùa, không theo
từng ngày.

**Nghiệp vụ giá.** Giống `GROUP_TOUR` nhưng:

- Không có giảm theo quy mô đoàn.
- **Có giảm theo số khách trong nhóm**: 2 người một giá, 4 người rẻ hơn mỗi người.
- Đi một mình luôn có phụ thu phòng đơn, không có tuỳ chọn ghép phòng.

**Nghiệp vụ tồn kho.** Không đếm chỗ. Nhưng **có thể bị chặn bởi phòng khách sạn**
ở mùa cao điểm.

> **Quy tắc v1:** ngày khởi hành mà nhà điều hành chưa chắc có phòng thì để trạng
> thái `PENDING` (chờ xác nhận), **không** để `SOLD_OUT`. Khách vẫn đặt được;
> đơn ở trạng thái "chờ xác nhận" và nhân viên phải trả lời trong **24 giờ**.
> Đây là khác biệt nghiệp vụ quan trọng nhất so với `GROUP_TOUR`: đơn không chốt
> ngay khi thanh toán xong.

**Huỷ và đổi.** Nhà điều hành không huỷ vì thiếu khách — không có ngưỡng số khách.
Nhưng có thể không xác nhận được ngày, khi đó hoàn 100% hoặc đề xuất ngày khác.

**Cạm bẫy**

- Hiển thị "10–25 khách" trên thẻ.
- Cho trạng thái `SOLD_OUT` khi thật ra chỉ là chưa xác nhận được phòng.
- Quên bước xác nhận 24 giờ, gửi thẳng email "đặt tour thành công".

---

### 4.3. `PRIVATE_TOUR` — Tour riêng theo yêu cầu

**Định nghĩa.** Khách **đề xuất ngày**, lộ trình điều chỉnh được, giá theo **bậc
số khách**. Sản phẩm phổ biến nhất của thị trường Việt Nam mà mô hình Bắc Âu
không có.

**Trường riêng**

| Trường | Ghi chú |
|---|---|
| `priceTiers` | Bậc giá theo số khách: 2 · 3–4 · 5–8 · 9–15 · 16+. Càng đông càng rẻ mỗi người |
| `isCustomisable` | Lộ trình sửa được, hay chỉ đổi ngày |
| `leadTimeDays` | Số ngày báo trước tối thiểu. Thường 7–14 |
| `quoteValidDays` | Báo giá có hạn bao lâu. Thường 7 |

**Ràng buộc dữ liệu**

1. Lịch trình vẫn phải kín ngày — nó là **lộ trình mẫu**, và phải ghi rõ trên
   giao diện là mẫu, có thể điều chỉnh.
2. `priceTiers` phải **liên tục và không chồng lấn**: hết bậc này tới bậc kia,
   không có khoảng trống ở giữa. Bậc đầu bắt đầu từ `minPartySize`.
3. Giá mỗi người phải **giảm dần** khi bậc tăng. Bậc 5–8 đắt hơn bậc 3–4 là dấu
   hiệu nhập sai.
4. Không có bản ghi `Departure` nào. Có `Departure` cho `PRIVATE_TOUR` là dữ liệu
   sai.

**Nghiệp vụ giá và luồng báo giá.** Đây là loại **duy nhất không chốt được tự
động**.

```
Khách gửi yêu cầu                    Quote.DRAFT
  (ngày mong muốn, số khách,
   loại khách, ghi chú)
        │
Nhân viên dựng báo giá               Quote.SENT     ← hạn quoteValidDays
  (chốt lộ trình, chốt giá,
   có thể khác giá niêm yết)
        │
Khách chấp nhận                      Quote.ACCEPTED
        │
Đặt cọc                              → Booking
```

Quy tắc:

- `Quote` là **thực thể riêng**, không phải trường trên `Booking`.
- Giá niêm yết trên trang là **giá tham khảo theo bậc**; giá thật nằm trong báo
  giá. Trang phải nói rõ điều này.
- Báo giá quá hạn thì tự thành `EXPIRED`, khách phải yêu cầu lại.
- Yêu cầu có ngày sớm hơn `leadTimeDays` thì **chặn ngay ở form**, kèm giải thích,
  không để khách gửi rồi mới bị từ chối.

**Nghiệp vụ tồn kho.** Không có. Không giữ chỗ, không `SOLD_OUT`, không
`GUARANTEED`. Toàn bộ máy tồn kho không chạm tới loại này.

**Huỷ và đổi.** Trước khi có `Booking` thì không có gì để huỷ — khách chỉ cần bỏ
báo giá. Sau khi đặt cọc thì theo chính sách như `GROUP_TOUR`.

**Cạm bẫy**

- Đặt nút "Đặt tour" thay vì "Yêu cầu báo giá".
- Hiện giá không kèm bậc số khách — khách đi một mình thấy giá gấp đôi ở bước
  sau và bỏ đi. Đây là hiểu nhầm có hệ thống, không phải trường hợp hiếm.
- Nhét `Quote` vào bảng `Booking` với một cột trạng thái. Hai vòng đời khác nhau.
- Cho `PRIVATE_TOUR` đi qua máy giữ chỗ vì "cho đồng nhất".

---

### 4.4. `CRUISE` — Du thuyền

**Định nghĩa.** Tour trên tàu, giá theo hạng cabin.

**Trường riêng**

| Trường | Ghi chú |
|---|---|
| `shipName` | |
| `portCount` | Số cảng ghé — hiển thị thay cho quy mô đoàn |
| `cabinCategory` | Nằm trên `Departure`, **không** trên `Tour` |

**Ràng buộc dữ liệu**

1. Một ngày khởi hành có **nhiều bản ghi `Departure`**, mỗi hạng cabin một bản
   ghi: `INSIDE`, `OUTSIDE`, `BALCONY`, `AQUA`.
2. Mỗi ngày khởi hành phải có **đủ bốn hạng**, hoặc ghi rõ hạng nào không mở bán.
3. Chênh giá từ hạng trong ra hạng Aqua trong khoảng **20–45%**. Ngoài khoảng này
   là dấu hiệu nhập sai.
4. Lịch trình vẫn kín ngày. Đêm trên tàu để trống `hotelId`.

**Nghiệp vụ giá.** Giống `GROUP_TOUR`, thêm một dòng: **nâng hạng cabin**, cộng
sau phụ thu phòng đơn và trước phụ thu điểm khởi hành.

**Nghiệp vụ tồn kho.** Đếm theo cabin, **riêng từng hạng**.

> Hết `BALCONY` **không** có nghĩa là hết `INSIDE`. Đây là chỗ dễ sai nhất của
> loại này. Trạng thái `SOLD_OUT` áp cho **một hạng**, không cho cả ngày. Cả ngày
> chỉ `SOLD_OUT` khi bốn hạng đều hết.

**Huỷ và đổi.** Như `GROUP_TOUR`. Thêm: đổi hạng cabin trong cùng ngày khởi hành
là nghiệp vụ có thật, xử lý thủ công ở v1.

**Cạm bẫy**

- Gộp bốn hạng thành một dòng rồi mất thông tin hạng nào còn.
- Tính `SOLD_OUT` theo ngày thay vì theo hạng.
- Hiển thị bảng ngày khởi hành phẳng, không gộp theo ngày — khách thấy bốn dòng
  cùng ngày và tưởng bốn chuyến khác nhau.

---

### 4.5. `COMBO` — Combo bay + khách sạn

Xem mục 10 về phạm vi. Ở đây là nghiệp vụ.

**Định nghĩa.** Bó thành phần, **không phải hành trình**:

```
COMBO = vé bay khứ hồi + N đêm khách sạn (+ xe đưa đón sân bay) (+ vé tham quan)
```

**Trường riêng**

| Trường | Ghi chú |
|---|---|
| `components[]` | Mỗi thành phần: loại, nhà cung cấp, bắt buộc hay tuỳ chọn |
| `nights` | Thường 2–4 |
| `originCity` | **Biến thể sản phẩm riêng**, không phải phụ thu |
| `hotelOptions[]` | Khách sạn chọn được, mỗi cái một mức giá |
| `validFrom` / `validTo` | Khoảng ngày combo còn hiệu lực |
| `blackoutDates[]` | Ngày lễ không áp dụng |

**Ràng buộc dữ liệu**

1. **Không có `ItineraryDay`.** Ràng buộc "lịch trình kín ngày" **không áp dụng**.
2. Phải có ít nhất một thành phần bắt buộc là vé bay và một là khách sạn.
3. `validTo >= validFrom`; `blackoutDates` nằm trong khoảng hiệu lực.
4. Mỗi `hotelOption` phải có giá cho mọi khoảng ngày trong thời gian hiệu lực.

**Nghiệp vụ giá.** Khác hẳn tour: **giá là tổng thành phần**, thay đổi theo ngày
đi và theo khách sạn khách chọn.

```
giá vé bay (theo ngày, theo tuyến)
+ giá phòng × số đêm (theo khách sạn và hạng phòng khách chọn)
+ dịch vụ thêm đã chọn
= tổng
```

Không có giảm đặt sớm, không có phụ thu điểm khởi hành — điểm khởi hành đã là
sản phẩm khác.

**Nghiệp vụ tồn kho.** Ở v1 rút gọn (mục 10): số suất do nhân viên nhập tay cho
từng khoảng ngày. Hết suất thì đóng khoảng ngày đó, không đóng cả combo.

**Huỷ và đổi.** Đây là chỗ combo khác tour nhiều nhất: **vé bay thường không hoàn
được**, phòng thì hoàn được tới sát ngày. Chính sách huỷ phải tách theo thành
phần, không có một tỷ lệ chung cho cả đơn.

**Cạm bẫy**

- Bắt combo phải có lịch trình từng ngày cho "đồng nhất với tour".
- Coi điểm khởi hành là phụ thu như thị trường `DK`.
- Áp một chính sách huỷ chung cho cả vé bay lẫn phòng.

---

### 4.6. `DAY_TOUR` — Tour trong ngày và hoạt động

Xem mục 11 về phạm vi.

**Định nghĩa.** Sản phẩm bán độc lập, kéo dài vài giờ tới một ngày, có khung giờ.

**Trường riêng**

| Trường | Ghi chú |
|---|---|
| `durationHours` | |
| `timeSlots[]` | Khung giờ trong ngày, **mỗi khung một tồn kho riêng** |
| `paxTypes[]` | Người lớn / trẻ em / em bé, mỗi loại một giá |
| `meetingPoint` | Điểm hẹn, kèm toạ độ |
| `languages[]` | Ngôn ngữ hướng dẫn — Klook và GetYourGuide đều cho lọc theo cái này |
| `instantConfirm` | Xác nhận ngay hay chờ nhà cung cấp |
| `cutoffHours` | Đóng bán trước giờ khởi hành bao lâu |

**Ràng buộc dữ liệu**

1. Không có `ItineraryDay`, không có `hotelStays`.
2. Ít nhất một khung giờ.
3. Ít nhất một loại khách có giá.

**Nghiệp vụ giá.** Đơn giản nhất trong sáu loại: `Σ (số lượng × đơn giá theo loại
khách)`. Không phụ thu phòng đơn, không giảm đặt sớm, không phí xử lý.

**Nghiệp vụ tồn kho.** Theo **từng khung giờ**, không theo ngày. Tour 9:00 hết chỗ
không có nghĩa là tour 14:00 hết. Đóng bán khi còn ít hơn `cutoffHours` tới giờ
khởi hành.

**Huỷ và đổi.** Thường cho huỷ miễn phí tới 24 giờ trước — đây là chuẩn của thị
trường hoạt động, khác hẳn tour dài ngày.

**Cạm bẫy**

- Áp tồn kho theo ngày thay vì theo khung giờ.
- Dùng lại `Excursion` cho loại này — xem mục 11.
- Ép bố cục bảy tab lên một sản phẩm bốn giờ. Xem `05`.

---

## 5. Loại khách và giá theo loại khách

Hai thị trường chia loại khách khác nhau. **Không hardcode.** `PaxType` là dữ
liệu cấu hình của `Market`.

| Thị trường | Loại khách | Cách tính |
|---|---|---|
| `DK` | Người lớn (voksen) | Giá đầy đủ |
| | Trẻ em (barn) | Theo tuổi, giảm theo bậc |
| | Ở phòng đơn | **Phụ thu**, không phải loại khách riêng |
| `VN` | Người lớn | Giá đầy đủ |
| | Trẻ em 5–11 | Thường 75% |
| | Trẻ nhỏ 2–4 | Thường 50% |
| | Em bé dưới 2 | Thường 10–15% hoặc miễn |

Ngưỡng tuổi cũng là dữ liệu, không phải hằng số — mỗi tour có thể khác.

**Hệ quả:** engine giá nhận **danh sách (loại khách, số lượng, đơn giá)**, không
nhận `numAdults` và `numChildren`. Giữ đúng nguyên tắc của bản demo: engine nhận
số, không nhận id của lựa chọn, nên đổi bảng loại khách không phải sửa engine.

---

## 6. Điểm khởi hành — hai vai trò khác nhau

| | `DK` | `VN` |
|---|---|---|
| Vai trò | **Phụ thu giá** | **Biến thể sản phẩm và bộ lọc** |
| Giá trị | Copenhagen (0 kr.), Billund (+800), Aalborg (+800) | Hà Nội, TP.HCM, Đà Nẵng |
| Chọn ở đâu | Bước đặt tour | **Bộ lọc trên trang tìm kiếm** |
| Ảnh hưởng | Cộng vào tổng | Đổi cả danh sách sản phẩm hiển thị |

Mô hình dữ liệu phải đỡ cả hai: `departure_origin` có cột `surcharge` (dùng ở
`DK`) **và** được đánh index để lọc (dùng ở `VN`). Không tách làm hai khái niệm.

---

## 7. Bộ lọc — chung và riêng

**Luôn có:** miền → điểm đến (phân cấp) · loại sản phẩm · thời lượng · khoảng giá
· tháng khởi hành · chủ đề.

**Chỉ hiện với loại tương ứng**

| Bộ lọc | Loại |
|---|---|
| Đảm bảo khởi hành | `GROUP_TOUR` |
| Quy mô đoàn | `GROUP_TOUR` |
| Mức vận động | `GROUP_TOUR`, `PRIVATE_TOUR` |
| Hạng cabin | `CRUISE` |
| Điểm khởi hành | `COMBO`, và `GROUP_TOUR` ở thị trường `VN` |
| Hạng sao khách sạn | `COMBO` |
| Khung giờ | `DAY_TOUR` |
| Ngôn ngữ hướng dẫn | `DAY_TOUR`, `GROUP_TOUR` |
| Xác nhận ngay | `DAY_TOUR` |

**Toàn bộ trạng thái lọc nằm trong URL.** Đổi loại sản phẩm thì bộ lọc riêng của
loại cũ phải **bị xoá khỏi URL**, không để lại tham số mồ côi.

---

## 8. Thẻ sản phẩm — mỗi loại một bố cục

| Loại | Dòng đặc trưng trên thẻ |
|---|---|
| `GROUP_TOUR` | `10–25 khách · 7 ngày khởi hành` |
| `INDIVIDUAL_PACKAGE` | `Hanoi (4 đêm) · Hoi An (3) · Halong-bugten (2)` |
| `PRIVATE_TOUR` | `Từ 2 khách · báo trước 7 ngày` + giá **kèm bậc số khách** |
| `CRUISE` | `Tàu Mekong Princess · 8 cảng ghé` |
| `COMBO` | `Khởi hành từ Hà Nội` + danh sách thành phần |
| `DAY_TOUR` | `4 giờ · xác nhận ngay` + giá theo loại khách |

Chung cho mọi loại: ảnh (có nút lật sang bản đồ với bốn loại tour dài), badge
`MỚI`, tiêu đề, nhãn loại viết đầy đủ, rating khi có, và giá dạng
`{n} ngày từ {giá}`.

**Đây không phải cùng một bố cục có vài trường khác nhau. Đừng gộp.**

---

## 9. Bảng đối chiếu chức năng

| Chức năng | `GROUP` | `INDIVIDUAL` | `PRIVATE` | `CRUISE` | `COMBO` | `DAY_TOUR` |
|---|:--:|:--:|:--:|:--:|:--:|:--:|
| Lịch trình từng ngày | ✔ | ✔ | ✔ mẫu | ✔ | ✘ | ✘ |
| Ràng buộc lịch trình kín ngày | ✔ | ✔ | ✔ | ✔ | — | — |
| Bảng ngày khởi hành | ✔ | ✔ | ✘ | ✔ gộp theo ngày | ✘ | ✘ |
| Lịch chọn ngày | ✘ | ✔ | ✔ | ✘ | ✔ | ✔ |
| Trạng thái `GUARANTEED` | ✔ | ✘ | ✘ | ✔ | ✘ | ✘ |
| Trạng thái `SOLD_OUT` | ✔ | ✘ | ✘ | ✔ theo hạng | ✔ | ✔ theo khung giờ |
| Trạng thái `PENDING` | ✔ | **✔ chủ đạo** | ✘ | ✔ | ✘ | ✘ |
| Giữ chỗ có hạn | ✔ | ✘ | ✘ | ✔ | ✔ | ✔ |
| Phụ thu phòng đơn | ✔ | ✔ | ✘ | ✔ | ✘ | ✘ |
| Giá theo bậc số khách | ✘ | ✔ nhẹ | **✔** | ✘ | ✘ | ✘ |
| Giá theo loại khách | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| Giảm đặt sớm | ✔ | ✔ | ✘ | ✔ | ✘ | ✘ |
| Phí xử lý | ✔ | ✔ | ✔ | ✔ | ✔ | ✘ |
| Bước báo giá | ✘ | ✘ | **✔** | ✘ | ✘ | ✘ |
| Đặt và thanh toán online | ✔ | ✔ | ✘ | ✔ | ✔ | ✔ |
| Xác nhận ngay | ✔ | ✘ 24 giờ | — | ✔ | ✔ | tuỳ |
| Nhà điều hành huỷ vì thiếu khách | ✔ | ✘ | ✘ | ✔ | ✘ | ✘ |

`—` nghĩa là ràng buộc không áp dụng; `✘` nghĩa là có khái niệm nhưng không dùng.

---

## 10. Cảnh báo phạm vi: `COMBO` đắt hơn vẻ ngoài

Combo trông như "một loại tour nữa" nhưng **không phải**. Nó là sản phẩm bán hàng
tồn kho của người khác:

| Cần gì | Chi phí thật |
|---|---|
| Giá và chỗ trống chuyến bay theo thời gian thực | Nối GDS hoặc đại lý gộp vé. Hợp đồng, phí giao dịch, môi trường thử |
| Giá và phòng trống khách sạn | Nối channel manager hoặc kho phòng riêng |
| Giữ chỗ hai nhà cung cấp cùng lúc | Giao dịch phân tán. Giữ được vé mà hết phòng thì phải hoàn vé |
| Chính sách vé đổi/huỷ của từng hãng | Mỗi hãng một luật |

Đây là **khối lượng ngang cả phần còn lại của dự án cộng lại**, không phải một
sprint.

**Đề xuất cho v1:** `COMBO` là **bó cố định do nhân viên soạn sẵn**:

- Nhân viên nhập sẵn tuyến bay, giờ bay, danh sách khách sạn chọn được, giá trọn
  gói theo từng khoảng ngày, số suất còn
- Khách chọn từ những gì đã soạn, không tra cứu thời gian thực
- Tồn kho là số suất nhân viên nhập tay

Giữ được phần lớn giá trị bán hàng với một phần nhỏ khối lượng, và **không khoá
cửa**: nâng lên tồn kho thời gian thực về sau chỉ thay tầng nhà cung cấp, giao
diện và luồng đặt giữ nguyên. Cần một ADR khi bắt đầu làm.

---

## 11. `DAY_TOUR` và `Excursion` — đừng gộp

| | `Excursion` (v1) | `DAY_TOUR` (v2) |
|---|---|---|
| Bán độc lập | Không | **Có** |
| Gắn với tour dài | Có, qua `destinationId` | Không |
| Có lịch riêng | Không | **Có, theo khung giờ** |
| Có tồn kho | Không | **Có** |
| Khách mua khi nào | Tại chỗ, trong chuyến | Trước chuyến, online |
| Nằm trong giá tour | **Không** | Không áp dụng |

Cám dỗ là dùng lại `Excursion` vì trông giống nhau. Đừng — thêm lịch và tồn kho
vào `Excursion` sẽ làm hỏng phần đang chạy đúng.

---

## 12. Quy tắc bắt buộc rút ra

1. **Thẻ sản phẩm hiển thị metadata khác nhau theo loại.** Không phải cùng một
   bố cục. Đừng gộp.
2. **`PRIVATE_TOUR` không có nút "Đặt tour"** — nút là "Yêu cầu báo giá", và
   `Quote` là thực thể riêng.
3. **Giá `PRIVATE_TOUR` luôn ghi kèm bậc số khách.**
4. **`CRUISE` gộp bốn hạng cabin vào một dòng ngày**, tồn kho và `SOLD_OUT` tính
   riêng từng hạng.
5. **`INDIVIDUAL_PACKAGE` dùng `PENDING`, không dùng `SOLD_OUT`**, và có bước xác
   nhận 24 giờ.
6. **`COMBO` không có lịch trình từng ngày**; chính sách huỷ tách theo thành phần.
7. **`DAY_TOUR` tính tồn kho theo khung giờ**, không theo ngày.
8. **Chỗ khả dụng phải trừ cả chỗ đang giữ chưa hết hạn.**
9. **Loại sản phẩm nào bán ở thị trường nào là dữ liệu**, không phải `if`.
10. **`PaxType` và ngưỡng tuổi là dữ liệu của `Market`**, không hardcode.
11. **Điểm khởi hành vừa là phụ thu (`DK`) vừa là bộ lọc (`VN`).**
12. **Đổi loại sản phẩm thì xoá tham số lọc riêng của loại cũ khỏi URL.**

---

## 13. Việc còn để ngỏ

| Việc | Chặn cái gì | Ai quyết |
|---|---|---|
| `PRIVATE_TOUR` có bán ở thị trường `DK` không | Bộ lọc, catalog `DK` | Nghiệp vụ |
| Bậc giá `PRIVATE_TOUR` chia thế nào | `14`, dữ liệu mồi | Nghiệp vụ |
| Ngưỡng tuổi trẻ em từng thị trường | `14`, `12` | Nghiệp vụ |
| Hạn giữ chỗ bao nhiêu phút | `14`, `23` | Nghiệp vụ + kỹ thuật |
| Hạn xác nhận `INDIVIDUAL_PACKAGE` có đúng 24 giờ không | `23` | Nghiệp vụ |
| `COMBO` vào v1 hay v1.5 | Khối lượng đợt 2 và 3 | Nghiệp vụ + kỹ thuật |
| Có bán `DAY_TOUR` không, hay giữ nguyên `Excursion` | Phạm vi v2 | Nghiệp vụ |
| Tour liên quốc gia là loại riêng hay thuộc tính | `11` | Nghiệp vụ |
