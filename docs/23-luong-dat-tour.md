# Luồng đặt tour

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: bốn bước màn hình đặt tour, máy trạng thái đơn đặt, giữ chỗ
                  nhìn từ luồng, bàn giao sang thanh toán, xác nhận, huỷ và hoàn,
                  danh mục email gửi khách.
Không nói về: công thức tính giá, làm tròn, tồn kho và vòng đời báo giá (14),
              lược đồ (12), endpoint và mã lỗi (13), cổng thanh toán (30),
              route và trạng thái màn hình (20), màn hình quản trị (22).
```

Tài liệu này trả lời: **từ lúc khách bấm "Đặt" tới lúc tiền vào và email đi, có
những bước nào, và mỗi bước hỏng thì hệ thống làm gì.**

Ranh giới hay nhầm nhất với `14`: ở đó là **quy tắc** (công thức tính giá, công
thức chỗ còn lại, khoá bi quan). Ở đây là **luồng** (khách thấy gì, gọi gì, khi
nào, và trạng thái đơn đi đâu). Con số và công thức thì trỏ sang `14`, không chép.

---

## 1. Không phải loại nào cũng đặt được như nhau

| Loại | Có tồn kho | Giữ chỗ | Đặt trực tiếp | Trạng thái sau khi trả tiền |
|---|---|---|---|---|
| `GROUP_TOUR` | Có | Có | Có | `CONFIRMED` |
| `CRUISE` | Có | Có | Có | `CONFIRMED` |
| `DAY_TOUR` | Có | Có | Có | `CONFIRMED` |
| `COMBO` | Có | Có | Có | `CONFIRMED` |
| `INDIVIDUAL_PACKAGE` | **Không** | Không | Có | **`PENDING_CONFIRMATION`** |
| `PRIVATE_TOUR` | Không | Không | **Không** | — |

Hai dòng cuối là hai ngoại lệ có thật, không phải thiếu sót:

- `INDIVIDUAL_PACKAGE` không có tồn kho chung, nên trả tiền xong vẫn **chưa chắc
  chắn**: nhân viên phải xác nhận trong 24 giờ (`14` mục 8). Màn hình xác nhận
  phải nói rõ điều đó với khách, không được viết "Đã xác nhận".
- `PRIVATE_TOUR` **không đặt trực tiếp được**. CTA là "Yêu cầu báo giá" và luồng
  đi theo mục 7. Nút "Đặt ngay" ở đây là hứa một thứ hệ thống không làm được, và
  khách sẽ tin lời hứa đó.

---

## 2. Bốn bước

```
Bước 1  Chọn ngày khởi hành      → tạo giữ chỗ, đồng hồ bắt đầu chạy
Bước 2  Số khách và loại khách   → tính giá lại mỗi lần đổi
Bước 3  Tuỳ chọn thêm            → phòng đơn, nâng hạng, điểm khởi hành
Bước 4  Thông tin và thanh toán  → tạo đơn, chuyển sang cổng thanh toán
```

Bốn bước là **bốn URL** (`20` mục 2, R7), không phải bốn tab ẩn hiện bằng
JavaScript: khách bấm Back là chuyện chắc chắn xảy ra, và mất hết dữ liệu vì Back
là cách nhanh nhất để mất một đơn.

### 2.1. Mỗi bước hiện gì

| Bước | Hiện | Kiểm ở frontend | Kiểm lại ở backend |
|---|---|---|---|
| 1 | Lịch ngày khởi hành, trạng thái và chỗ còn lại từng ngày | Ngày đã đóng bán thì không chọn được | `DEPARTURE_CLOSED`, `DEPARTURE_SOLD_OUT` |
| 2 | Bộ đếm theo loại khách, bảng phân rã giá cập nhật ngay | Số khách trong khoảng cho phép | `PARTY_SIZE_OUT_OF_RANGE` |
| 3 | Tuỳ chọn kèm **chênh giá của từng tuỳ chọn** | – | Tính lại toàn bộ |
| 4 | Bảng phân rã đầy đủ, tổng, tiền đặt cọc, phần còn lại | Định dạng email và điện thoại | `VALIDATION_FAILED` |

**Frontend kiểm để khách khỏi mất công, backend kiểm để hệ thống khỏi sai.** Bỏ
vế thứ hai là mở cửa cho một yêu cầu gửi thẳng vào API; bỏ vế thứ nhất là bắt
khách điền xong bốn bước rồi mới báo hỏng.

### 2.2. Giá hiện ở mọi bước, và luôn là giá tính lại

Bước 2 và 3 gọi endpoint tính giá xem trước — **không lưu gì, không tác dụng
phụ** (`13` mục 9.2). Mỗi lần khách đổi lựa chọn là một lần gọi lại.

Frontend **không tự cộng trừ tiền**. Cám dỗ rất lớn ở bước 3: cộng thêm phụ thu
phòng đơn vào tổng cũ trông có vẻ vô hại. Nhưng thứ tự cộng dồn tám bước và quy
tắc làm tròn nằm ở `14`, và khi frontend tính khác backend thì con số ở bước 3
lệch con số ở bước 4 — khách thấy giá nhảy đúng vào lúc chuẩn bị trả tiền.

`amount` từ API là **chuỗi**; frontend chỉ định dạng, không `parseFloat`.

---

## 3. Giữ chỗ nhìn từ luồng

Quy tắc, công thức và khoá bi quan ở `14` mục 6. Ở đây là ba câu hỏi của luồng.

### 3.1. Tạo lúc nào

**Ngay khi khách chọn xong ngày khởi hành ở bước 1**, không phải ở bước 4.

Ba phương án đã cân nhắc:

| Phương án | Bỏ vì |
|---|---|
| Giữ chỗ ở bước 4, ngay trước khi chuyển sang cổng thanh toán | Khách điền xong bốn bước rồi mới biết hết chỗ — mất công vô ích, và đây là lúc họ bực nhất |
| Không giữ chỗ, chỉ kiểm lúc tạo đơn | Hai khách cùng mua được chỗ cuối; một người sẽ phải nhận cuộc gọi xin lỗi |
| **Giữ ngay ở bước 1** ✔ | Chọn cái này |

Đánh đổi đã biết: khách bỏ dở làm chỗ bị treo tới 20 phút. Chấp nhận được vì hạn
ngắn và chỗ tự về kho khi hết hạn (`14` mục 6.4).

### 3.2. Khách nhìn thấy gì

- Một dòng **chữ** nói còn bao nhiêu thời gian: *"Chúng tôi giữ chỗ cho bạn tới
  14:32"*. Không dùng đồng hồ đếm ngược nhấp nháy — nhóm khách này phản ứng tệ
  với sức ép, và một cái đồng hồ chạy giật làm họ bỏ ngang.
- Còn dưới 5 phút thì nhắc một lần, kèm cách **gia hạn**: quay lại bước 1 và chọn
  lại ngày đó.

### 3.3. Hết hạn giữa chừng

| Khách đang ở | Xử lý |
|---|---|
| Bước 2 hoặc 3 | Báo hết hạn, đưa về bước 1, **giữ nguyên** số khách và tuỳ chọn đã chọn |
| Bước 4, trước khi bấm trả tiền | Như trên |
| Đã sang cổng thanh toán | Xem mục 5.3 |

Giữ nguyên lựa chọn cũ là điều nhỏ nhưng quan trọng: bắt khách nhập lại từ đầu vì
một cái hạn kỹ thuật là lý do bỏ giỏ hàng phổ biến nhất.

---

## 4. Máy trạng thái đơn đặt

```
                 ┌──────────────► EXPIRED
                 │              (không trả tiền trong hạn)
   DRAFT ──► PENDING_PAYMENT ──► PENDING_CONFIRMATION ──► CONFIRMED ──► COMPLETED
                 │                        │                   │
                 └────────────────────────┴───────────────────┴──► CANCELLED ──► REFUNDED
```

| Trạng thái | Nghĩa là gì |
|---|---|
| `DRAFT` | Đã tạo, chưa gửi đi thanh toán. Sống rất ngắn |
| `PENDING_PAYMENT` | Đang ở cổng thanh toán, hoặc đang chờ kết quả |
| `PENDING_CONFIRMATION` | Đã trả tiền, chờ nhân viên xác nhận — chỉ `INDIVIDUAL_PACKAGE` |
| `CONFIRMED` | Chắc chắn đi |
| `COMPLETED` | Đã đi xong |
| `CANCELLED` | Huỷ, chỗ đã về kho |
| `REFUNDED` | Đã hoàn tiền xong |
| `EXPIRED` | Không trả tiền trong hạn, tự động |

Bốn quy tắc:

1. **Mọi lần đổi trạng thái ghi một `booking_event`** — từ trạng thái nào, sang
   trạng thái nào, ai làm (`CUSTOMER` / `STAFF` / `SYSTEM`), lúc nào. Không có
   ngoại lệ cho thao tác của hệ thống: chính những lần đổi tự động mới là thứ khó
   truy nhất về sau.
2. **Không có bước lùi.** Huỷ nhầm thì tạo đơn mới, không đưa `CANCELLED` trở lại
   `CONFIRMED` — nhật ký phải đọc được như một câu chuyện có thật.
3. **Đơn không bao giờ bị xoá**, kể cả xoá mềm. Đơn hỏng thì `CANCELLED` (`11`
   mục 11.3).
4. `CANCELLED` **trả chỗ về kho ngay**, không chờ hoàn tiền xong (`14` mục 6.5).

### 4.1. Đơn chụp lại mọi thứ

Đơn giữ bản sao của những gì đã bán: `product_title`, từng dòng `booking_line`,
`total`, `deposit`, `currency`.

Đổi bảng giá **không** làm đổi đơn cũ. Nếu đơn tham chiếu bảng giá hiện hành thì
một lần cập nhật giá mùa sau làm đổi số tiền của đơn đã ký — đó là sai sót không
sửa được sau khi khách đã trả tiền.

`booking_line.label_key` là **khoá chuỗi**, không phải câu tiếng người: hoá đơn
in ra được ở cả hai ngôn ngữ, và câu chữ sửa được mà không đụng vào đơn cũ.

---

## 5. Thanh toán

### 5.1. Số tiền phải trả bây giờ

Mặc định là **tiền đặt cọc**, phần còn lại thu trước ngày khởi hành. Tỷ lệ đặt
cọc lấy từ cấu hình thị trường, không hardcode (`14` mục 2.4).

`deposit + balance = total` phải đúng **tuyệt đối**: phần còn lại luôn tính bằng
`tổng − đặt cọc`, không tính riêng bằng một phép nhân thứ hai (`14` mục 3).

### 5.2. Tính bất biến khi gọi lại

Endpoint tạo đơn, tạo giữ chỗ và tạo yêu cầu báo giá đều đòi header
`Idempotency-Key` (`13` mục 7). Ba tình huống có thật mà nó chống:

- Khách bấm nút hai lần vì trang phản hồi chậm
- Mạng di động gửi lại yêu cầu sau khi máy chủ đã xử lý xong nhưng phản hồi chưa về
- Khách bấm Back rồi bấm lại nút trả tiền

Khoá do **client sinh một lần cho một lần đặt**, không sinh lại ở mỗi lần thử —
sinh lại là mất toàn bộ tác dụng.

### 5.3. Khi kết quả thanh toán về muộn hoặc không về

Đơn ở `PENDING_PAYMENT` cho tới khi có kết quả chắc chắn. Ba đường có thể xảy ra:

| Tình huống | Xử lý |
|---|---|
| Khách quay lại trang thành công, webhook chưa tới | Hiện *"đang xác nhận thanh toán"*, **không** hiện đã xác nhận. Trang tự cập nhật khi có kết quả |
| Webhook tới, khách đóng trình duyệt | Đơn vẫn chuyển trạng thái và email vẫn gửi. Kết quả không phụ thuộc việc khách còn mở trang hay không |
| Không có kết quả trong hạn | `EXPIRED`, chỗ đã tự về kho khi giữ chỗ hết hạn |

**Nguồn sự thật của việc trả tiền là webhook, không phải URL khách quay về.** URL
đó khách gõ tay được.

Nếu tiền vào mà giữ chỗ đã hết hạn và chỗ đã bị người khác lấy: đơn về
`PENDING_CONFIRMATION` và **báo cho nhân viên xử lý tay** — không tự huỷ và tự
hoàn. Đây là trường hợp hiếm nhưng có thật, và tự động hoá nó sai một lần là mất
một khách vĩnh viễn.

Chi tiết cổng thanh toán, chữ ký webhook và đường dẫn trả về ở `30`.

---

## 6. Xác nhận và tra cứu

Trang xác nhận hiện: mã tra cứu, tóm tắt chuyến đi, số tiền đã trả, số tiền còn
lại kèm hạn, và **việc tiếp theo là gì**.

Không có đăng nhập cho khách ở v1 (`01` mục 3.2). Tra cứu đơn bằng **mã tra cứu +
email khớp**, giới hạn 10 lượt/giờ mỗi IP — thiếu giới hạn thì đây là kênh dò mã
đơn (`13` mục 11).

### 6.1. Email

Email dùng **`booking.locale`** — ngôn ngữ khách đã đọc lúc đặt — và **tiền tệ
của `booking.market`**. Hai thứ độc lập: một khách đọc `vi` mua ở `DK` nhận email
tiếng Việt ghi giá bằng DKK. Lấy ngôn ngữ email theo thị trường là lỗi kinh điển
của việc gộp market với locale.

| Email | Khi nào |
|---|---|
| Xác nhận đã nhận đơn | Ngay khi thanh toán thành công |
| Xác nhận chắc chắn đi | Khi sang `CONFIRMED` |
| Nhắc trả phần còn lại | Trước hạn; số ngày ở `14` |
| Thông tin trước khi đi | Trước ngày khởi hành |
| Huỷ | Khi sang `CANCELLED`, kèm số tiền hoàn nếu có |
| Chuyến bị huỷ vì thiếu khách | `14` mục 6.6, gửi cho từng khách của chuyến đó |

Nội dung email là thứ backend gửi thẳng tới khách, nên đây là **ngoại lệ duy
nhất** cho quy tắc "API không trả câu tiếng người": email dịch ở backend bằng
`MessageSource`.

---

## 7. Luồng báo giá — `PRIVATE_TOUR`

```
Khách gửi yêu cầu ──► Tư vấn viên dựng báo giá ──► Gửi khách
                                                      │
                          Khách chấp nhận ◄───────────┘
                                │
                          Đặt cọc ──► Booking
```

Vòng đời và bảy quy tắc của `Quote` ở `14` mục 7. Ba điều thuộc về luồng:

1. **Không có giữ chỗ ở bất kỳ bước nào** — tour riêng không dùng tồn kho chung.
2. Ngày khách yêu cầu sớm hơn `leadTimeDays` thì **lịch chặn trước ở frontend**,
   backend vẫn kiểm lại và trả `LEAD_TIME_NOT_MET`.
3. Báo giá quá hạn thì khách phải yêu cầu lại, **không tự gia hạn**. Trang báo giá
   hết hạn phải có nút gửi yêu cầu mới, không phải một ngõ cụt.

Màn hình dựng báo giá của nhân viên ở `22` mục 6.

---

## 8. Huỷ và hoàn tiền

| Ai huỷ | Được không | Hệ quả |
|---|---|---|
| Khách, trước khi trả tiền | Có, tự làm | Giữ chỗ giải phóng, đơn `EXPIRED` hoặc `CANCELLED` |
| Khách, sau khi trả tiền | **Không tự làm ở v1** | Gọi điện hoặc email; nhân viên xử lý ở `22` |
| Nhân viên | Có | `CANCELLED`, chỗ về kho ngay, hoàn tiền theo bậc |
| Nhà điều hành, vì thiếu khách | Có | Cả chuyến `CANCELLED`, hoàn **100%** (`14` mục 6.6) |

Khách không tự huỷ được ở v1 là quyết định có chủ ý: bậc huỷ và tỷ lệ hoàn chưa
chốt (mục 10), và tự động hoá một chính sách chưa chốt là cách chắc chắn để hoàn
sai tiền. Cuộc gọi cũng là cơ hội giữ khách bằng cách đổi ngày.

**Hoàn tiền và trả chỗ về kho là hai việc độc lập.** Chỗ về kho ngay lúc huỷ;
tiền về tài khoản khách theo nhịp của ngân hàng. Buộc hai việc vào nhau là giữ
chỗ trống trong lúc chờ ngân hàng — mất doanh thu vô ích.

---

## 9. Cấm

- Frontend tự cộng trừ tiền, hoặc `parseFloat` trường tiền
- Tạo giữ chỗ ở bước 4 thay vì bước 1
- Đồng hồ đếm ngược nhấp nháy tạo sức ép
- Bốn bước dùng chung một URL
- Coi URL khách quay về từ cổng thanh toán là bằng chứng đã trả tiền
- Hiện "đã xác nhận" khi chưa có kết quả chắc chắn
- Đơn tham chiếu bảng giá hiện hành thay vì chụp lại giá
- Xoá đơn, hoặc xoá mềm đơn
- Đưa `CANCELLED` trở lại `CONFIRMED`
- Đổi trạng thái mà không ghi `booking_event`
- Lấy ngôn ngữ email theo thị trường thay vì theo `booking.locale`
- Nút "Đặt ngay" trên `PRIVATE_TOUR`

---

## 10. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| Bậc thời gian huỷ và tỷ lệ hoàn từng bậc | Mục 8; không có bảng này thì không tự động hoá được việc huỷ | Q-2 ở `41` mục 4 |
| v1 có thanh toán online thật, hay nhận đặt chỗ rồi gọi điện chốt | Toàn bộ mục 5 | **Q-3** ở `41` mục 4 |
| **`12` chưa định nghĩa bảng `booking_passenger`** dù `14` mục 6.3 chèn vào nó | Bước 4 thu thông tin từng hành khách | Cần bổ sung vào `12` |
| Định dạng mã tra cứu | Mục 6 | `13` mục 12 |
| Thu phần còn lại bằng cách nào: khách tự trả online hay nhân viên gửi yêu cầu | Mục 5.1, email nhắc ở mục 6.1 | Nghiệp vụ + `30` |
| `COMBO` huỷ tách theo thành phần thì hoàn thế nào | Mục 8 | Q-7 ở `41` mục 4 |
