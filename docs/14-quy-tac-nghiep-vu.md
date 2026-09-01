# Quy tắc nghiệp vụ

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: engine tính giá, làm tròn, giải trạng thái ngày khởi hành,
                  tồn kho và giữ chỗ, vòng đời báo giá, danh sách test bắt buộc.
Không nói về: loại sản phẩm khác nhau ở đâu (04), lược đồ (12), API (13),
              luồng màn hình đặt tour (23).
```

Mọi thứ trong tài liệu này nằm ở module **`domain`**, không phụ thuộc Spring,
test bằng JUnit thuần.

---

## 1. Ranh giới của `domain`

| Quy tắc | Vì sao |
|---|---|
| Không phụ thuộc Spring, JPA, Jackson | Test chạy trong vài mili giây, không dựng context |
| **Không đọc đồng hồ hệ thống** | Hàm cần "hôm nay" nhận `LocalDate` làm tham số. Test đọc đồng hồ thật sẽ đỏ vào một ngày nào đó trong tương lai mà không ai hiểu vì sao |
| **Nhận số, không nhận id của lựa chọn** | Engine không cần biết "bảo hiểm trọn gói" là gì, chỉ cần biết nó bao nhiêu tiền một người. Đổi bảng giá tuỳ chọn không phải sửa engine, và test không bám vào bảng giá |
| Không truy vấn CSDL | Tầng `application` nạp dữ liệu rồi truyền vào |

Ràng buộc thứ nhất kiểm bằng ArchUnit. Ba ràng buộc còn lại kiểm bằng review.

---

## 2. Engine tính giá

### 2.1. Thứ tự cộng dồn — cố định, không được đổi

```
  giá cơ bản        Σ (số khách theo loại × đơn giá theo loại và loại phòng)
+ phụ thu phòng đơn (giá phòng đơn − giá phòng đôi) × số khách ở một mình
+ nâng hạng cabin   chênh giá hạng đã chọn so với hạng thấp nhất   [CRUISE]
+ phụ thu điểm khởi hành                                            [DK]
+ bảo hiểm          đơn giá × số khách
+ đêm khách sạn trước bay   đơn giá phòng × số phòng × số đêm       [DK]
− giảm đặt sớm      mức giảm mỗi người × số khách
+ phí xử lý         cố định một lần mỗi đơn
─────────────────────────────────────────────────────────────────
= tổng cộng
  đặt cọc = tổng × tỷ lệ đặt cọc của thị trường
  còn lại = tổng − đặt cọc
```

**Thứ tự quyết định con số cuối**, vì giảm đặt sớm trừ **trước** phí xử lý. Đảo
hai dòng cuối ra kết quả khác. Đây là lý do thứ tự được ghi ra thành tài liệu chứ
không để mỗi người tự suy.

`deposit + balance` **luôn** bằng `total`. Ràng buộc này có test riêng, vì làm
tròn dễ làm lệch một đơn vị.

### 2.2. Khác biệt theo loại sản phẩm

| Dòng | `GROUP` | `INDIVIDUAL` | `PRIVATE` | `CRUISE` | `COMBO` | `DAY_TOUR` |
|---|:--:|:--:|:--:|:--:|:--:|:--:|
| Giá cơ bản theo ngày khởi hành | ✔ | ✔ | ✘ | ✔ | ✘ | ✘ |
| Giá cơ bản theo **bậc số khách** | ✘ | ✘ | **✔** | ✘ | ✘ | ✘ |
| Giá cơ bản theo **thành phần** | ✘ | ✘ | ✘ | ✘ | **✔** | ✘ |
| Giá cơ bản theo **loại khách × khung giờ** | ✘ | ✘ | ✘ | ✘ | ✘ | **✔** |
| Phụ thu phòng đơn | ✔ | ✔ | ✘ | ✔ | ✘ | ✘ |
| Nâng hạng cabin | ✘ | ✘ | ✘ | ✔ | ✘ | ✘ |
| Phụ thu điểm khởi hành | ✔ | ✔ | ✘ | ✔ | ✘¹ | ✘ |
| Giảm đặt sớm | ✔ | ✔ | ✘ | ✔ | ✘ | ✘ |
| Phí xử lý | ✔ | ✔ | ✔ | ✔ | ✔ | ✘ |

¹ Với `COMBO`, điểm khởi hành là **biến thể sản phẩm**, đã nằm trong giá cơ bản —
`04` mục 6.

Engine là **một hàm** với các dòng tuỳ chọn, không phải sáu hàm. Dòng không áp
dụng thì không xuất hiện trong bảng phân rã — không hiện dòng `0 kr.`

### 2.3. Bảng giá theo bậc — `PRIVATE_TOUR`

```
số khách trong nhóm → tra bậc → đơn giá mỗi người → × số khách
```

- Bậc chọn theo **tổng số khách**, kể cả trẻ em.
- Giảm theo loại khách áp **sau** khi đã chọn bậc: trẻ em vẫn tính vào số khách
  để chọn bậc, nhưng trả theo tỷ lệ của loại mình.
- Số khách ngoài mọi bậc → `PARTY_SIZE_OUT_OF_RANGE`, không tự lấy bậc gần nhất.

### 2.4. Hằng số

| Hằng số | `DK` | `VN` |
|---|---|---|
| Tỷ lệ đặt cọc | 25% | **chưa chốt** |
| Phí xử lý | 295 kr / đơn | **chưa chốt** |
| Giảm đặt sớm ≥ 6 tháng | 1.000 kr / người | không áp dụng |
| Giảm đặt sớm ≥ 3 tháng | 500 kr / người | không áp dụng |
| Ngưỡng "còn ít chỗ" | ≤ 3 chỗ | ≤ 3 chỗ |
| Hạn giữ chỗ | 20 phút | 20 phút |
| Hạn xác nhận `INDIVIDUAL_PACKAGE` | 24 giờ | 24 giờ |

Ba ô **chưa chốt** đang chặn phần tính giá của thị trường `VN` — `04` mục 13.
Engine viết được ngay vì nó **nhận hằng số làm tham số**, không đọc hằng số từ
đâu cả; chỉ dữ liệu cấu hình là thiếu.

Giảm đặt sớm hai bậc là **đề xuất**, cần chốt cùng ba ô trên.

---

## 3. Làm tròn — chỗ dễ sai nhất

VND có **0 chữ số thập phân**, DKK có **2**. Quy tắc:

1. Số chữ số lấy từ `market.fraction_digits`, **không hardcode**.
2. Làm tròn `HALF_UP`.
3. **Làm tròn ở từng dòng**, rồi mới cộng. Không cộng số chưa tròn rồi tròn một lần.
4. Đặt cọc làm tròn **xuống** (`FLOOR`); phần còn lại = `tổng − đặt cọc`.

Quy tắc 3 là chỗ hay bị làm ngược. Cộng trước rồi tròn cho tổng "đẹp" hơn về mặt
toán học, nhưng khi đó **bảng phân rã cộng lại không bằng tổng** — khách nhìn
thấy các dòng cộng ra 18.900.001 ₫ trong khi tổng ghi 18.900.000 ₫, và mất lòng
tin ngay tại bước thanh toán.

Quy tắc 4 để `deposit + balance = total` luôn đúng: làm tròn xuống đặt cọc rồi
lấy phần bù, thay vì làm tròn cả hai rồi hy vọng chúng khớp.

Ví dụ VND: giảm 25% cho trẻ em trên đơn giá 4.290.000 ₫ ra 3.217.500 ₫ →
làm tròn thành **3.218.000 ₫**? Không — `fraction_digits = 0` nghĩa là làm tròn
tới **đồng**, ra `3.217.500`. Nếu nghiệp vụ muốn làm tròn tới nghìn đồng thì đó
là một quy tắc khác, phải khai báo riêng ở `market`. **Chưa chốt.**

---

## 4. Ví dụ tính đủ một đơn

`GROUP_TOUR` · thị trường `DK` · 2 người lớn, phòng đôi · khởi hành từ Billund ·
có bảo hiểm · thêm 1 đêm khách sạn trước bay · đặt trước 7 tháng.

| Dòng | Số lượng | Đơn giá | Thành tiền |
|---|---:|---:|---:|
| Giá cơ bản | 2 | 24.990 | 49.980 |
| Phụ thu phòng đơn | 0 | — | 0 |
| Phụ thu điểm khởi hành (Billund) | 2 | 800 | 1.600 |
| Bảo hiểm | 2 | 895 | 1.790 |
| Đêm khách sạn trước bay | 1 phòng | 1.095 | 1.095 |
| **Giảm đặt sớm** (≥ 6 tháng) | 2 | −1.000 | **−2.000** |
| Phí xử lý | 1 | 295 | 295 |
| **Tổng cộng** | | | **52.760** |
| Đặt cọc 25% | | | 13.190 |
| Còn lại | | | 39.570 |

Dòng phụ thu phòng đơn bằng 0 nên **không xuất hiện** trên giao diện; nó có mặt ở
đây chỉ để cho thấy vị trí trong thứ tự.

---

## 5. Giải trạng thái ngày khởi hành

```
resolveStatus(baseStatus, seatsBooked, seatsAvailable, guaranteedThreshold, fewSeatsThreshold)
```

Thứ tự xét, dừng ở điều kiện đầu tiên đúng:

```
1. baseStatus = PENDING            → PENDING
2. baseStatus = SOLD_OUT           → SOLD_OUT        (nhân viên đóng bán tay)
3. seatsAvailable <= 0             → SOLD_OUT
4. seatsBooked >= guaranteedThreshold → GUARANTEED
5. seatsAvailable <= fewSeatsThreshold → FEW_SEATS
6. baseStatus = FEW_SEATS          → FEW_SEATS       (ghi đè tay)
7.                                 → OPEN
```

Ba điều đáng nhớ:

- **`GUARANTEED` xét trước `FEW_SEATS`.** Một chuyến vừa đảm bảo khởi hành vừa
  còn ít chỗ thì hiện "Đảm bảo khởi hành" — đó là thông tin có giá trị bán hàng
  cao hơn, và cũng là thứ trang mẫu làm.
- **Nhân viên ghi đè được về phía nghiêm ngặt hơn** (bước 2 và 6), nhưng
  **không ghi đè được `GUARANTEED`** — không có nhánh nào cho phép.
- `seatsAvailable` đã **trừ chỗ đang giữ** (mục 6.1).

### 5.1. Du thuyền

Áp dụng cho **từng dòng `departure`**, tức từng hạng cabin. Một ngày hiển thị là
`SOLD_OUT` chỉ khi **cả bốn** hạng đều `SOLD_OUT`. Việc gộp là của tầng hiển thị.

### 5.2. Buổi thuyết trình

Dùng chung mô-típ: `chỗ còn = seats − seatsTaken`. Kín chỗ thì hiện mờ và khoá
nút đăng ký, y như `SOLD_OUT`. **Không có cờ "đã đầy" trong CSDL.**

---

## 6. Tồn kho và giữ chỗ

Áp dụng cho `GROUP_TOUR`, `CRUISE`, `COMBO`, `DAY_TOUR`.
**Không áp dụng cho `INDIVIDUAL_PACKAGE` và `PRIVATE_TOUR`** — hai loại này không
có tồn kho chung (`04` mục 3).

### 6.1. Công thức

```
seatsAvailable = capacity − seatsBooked − Σ(seat_hold còn hạn, chưa giải phóng)
```

Quên vế thứ ba là để hai khách cùng mua được chỗ cuối cùng. Đây là bug hạng nhất
của mọi hệ thống đặt chỗ.

### 6.2. Tạo giữ chỗ — khoá bi quan

```sql
BEGIN;

SELECT capacity, seats_booked
FROM departure
WHERE id = :departureId
FOR UPDATE;                          -- ← khoá dòng, các phiên khác xếp hàng

SELECT COALESCE(SUM(seats), 0)
FROM seat_hold
WHERE departure_id = :departureId
  AND released_at IS NULL
  AND expires_at > now();

-- nếu capacity − seats_booked − held < requested → huỷ bỏ, trả DEPARTURE_SOLD_OUT

INSERT INTO seat_hold (id, departure_id, seats, session_ref, expires_at)
VALUES (:id, :departureId, :seats, :sessionRef, now() + interval '20 minutes');

COMMIT;
```

**Khoá bi quan, không phải lạc quan.** Với khoá lạc quan (`@Version`), khách thứ
hai chỉ biết mình trượt **sau khi** đã điền xong thông tin và bấm thanh toán.
Khoá bi quan cho câu trả lời ngay ở bước chọn ngày, và transaction này ngắn —
chỉ vài mili giây, không giữ khoá qua thao tác của người dùng.

**Không bao giờ khoá ở tầng ứng dụng** (`synchronized`, khoá trong bộ nhớ). Chạy
hai instance là hỏng ngay, và không có gì báo.

### 6.3. Chuyển giữ chỗ thành đơn

Trong **một** transaction:

```
1. SELECT ... FROM seat_hold WHERE id = ? FOR UPDATE
2. Kiểm released_at IS NULL AND expires_at > now()
      → sai thì SEAT_HOLD_EXPIRED
3. UPDATE departure SET seats_booked = seats_booked + n WHERE id = ?
4. UPDATE seat_hold SET released_at = now(), booking_id = ?
5. INSERT booking, booking_line, booking_passenger
6. INSERT booking_event (to_status = 'PENDING_PAYMENT')
```

Bước 2 **phải kiểm lại** dù đã kiểm ở tầng trên: thời gian trôi giữa hai lần gọi,
và giữ chỗ có thể vừa hết hạn.

### 6.4. Quét hạn

Job chạy mỗi phút:

```sql
UPDATE seat_hold SET released_at = now()
WHERE released_at IS NULL AND expires_at <= now();
```

> **Chạy nhiều instance thì job này phải có ShedLock**, nếu không mỗi instance
> quét một lần. Ở đây hệ quả nhẹ (lệnh `UPDATE` bất biến khi lặp lại), nhưng
> cùng cơ chế còn dùng cho gửi email nhắc và cho hết hạn báo giá — hai việc
> **không** bất biến. Đặt ShedLock ngay từ job đầu tiên.

Chỗ được trả về kho **ngay khi hết hạn**, không chờ job: công thức 6.1 dùng
`expires_at > now()`, nên một giữ chỗ quá hạn không còn được tính dù cột
`released_at` chưa được cập nhật. Job chỉ dọn dẹp.

### 6.5. Huỷ đơn

`CONFIRMED → CANCELLED`: `seats_booked -= n` **ngay**, chỗ về kho lập tức. Không
chờ hoàn tiền xong — hoàn tiền và trả chỗ là hai việc độc lập, và giữ chỗ lại
trong lúc chờ ngân hàng là mất doanh thu vô ích.

### 6.6. Nhà điều hành huỷ vì thiếu khách

Chỉ `GROUP_TOUR` và `CRUISE`. Tới hạn chốt mà `seatsBooked < minPax`:

```
departure → CANCELLED
mọi booking của departure đó → CANCELLED, hoàn 100%
gửi email cho từng khách
```

Đây là nghiệp vụ có thật, không phải trường hợp hiếm — phải có màn hình quản trị
cho nó, không xử lý bằng cách sửa CSDL tay.

---

## 7. Vòng đời báo giá — `PRIVATE_TOUR`

```
DRAFT ──► SENT ──► ACCEPTED ──► sinh Booking
            │
            ├──► EXPIRED    (quá valid_until, job tự động)
            └──► REJECTED
```

Quy tắc:

| # | Quy tắc |
|---|---|
| 1 | Yêu cầu có ngày sớm hơn `leadTimeDays` → `LEAD_TIME_NOT_MET`. Frontend phải chặn trước ở lịch chọn, backend vẫn kiểm lại |
| 2 | `valid_until = ngày gửi + quote_valid_days` |
| 3 | Chỉ `ACCEPTED` mới sinh được `Booking`; trạng thái khác → `QUOTE_NOT_ACCEPTABLE` |
| 4 | Báo giá quá hạn → `QUOTE_EXPIRED`, khách phải yêu cầu lại. **Không tự gia hạn** |
| 5 | Giá trong báo giá **có thể khác** giá niêm yết theo bậc — đó là mục đích của bước báo giá |
| 6 | `quote_line` chụp lại bảng giá, đúng khuôn mẫu `booking_line` |
| 7 | Không có giữ chỗ ở bất kỳ bước nào |

Quy tắc 5 là lý do `PRIVATE_TOUR` tồn tại: nhân viên chốt được giá theo thực tế
mùa vụ, số khách, yêu cầu riêng — thứ bảng giá tĩnh không diễn đạt được.

---

## 8. Quy tắc riêng còn lại

| Loại | Quy tắc |
|---|---|
| `INDIVIDUAL_PACKAGE` | Thanh toán xong → `PENDING_CONFIRMATION`, **không** phải `CONFIRMED`. Nhân viên xác nhận trong 24 giờ. Quá hạn chưa xác nhận → cảnh báo trên bảng điều khiển quản trị |
| `CRUISE` | Nâng hạng cabin tính bằng chênh giá so với **hạng thấp nhất của đúng ngày khởi hành đó**, không phải chênh giá cố định |
| `COMBO` | Chính sách huỷ **tách theo thành phần**. Không có một tỷ lệ chung cho cả đơn |
| `DAY_TOUR` | Đóng bán khi còn ít hơn `cutoffHours` tới giờ khởi hành. Không phí xử lý |

---

## 9. Test bắt buộc

Bản demo có 24 test cho riêng engine giá. Giữ mức đó, và thêm phần tồn kho.

### 9.1. Engine giá — `domain`, JUnit thuần

- [x] Hai người phòng đôi, không tuỳ chọn nào
- [x] Một người ở phòng đơn → có dòng phụ thu
- [x] Giảm đặt sớm đúng ngưỡng 6 tháng, và **đúng một ngày trước ngưỡng** (không được giảm)
- [x] Giảm đặt sớm bậc 3 tháng
- [x] Thứ tự: giảm đặt sớm trừ **trước** phí xử lý
- [x] `deposit + balance = total`, cho cả DKK và VND
- [x] Làm tròn từng dòng: các dòng cộng lại **bằng đúng** tổng
- [x] VND `fraction_digits = 0`: không có phần thập phân ở bất kỳ dòng nào
- [x] Bậc giá `PRIVATE_TOUR`: đúng bậc cho 2, 4, 8, 15, 20 khách
- [x] Bậc giá: số khách ngoài mọi bậc → ném lỗi, không lấy bậc gần nhất
- [x] Trẻ em tính vào số khách để chọn bậc, nhưng trả theo tỷ lệ của mình
- [x] Nâng hạng cabin theo đúng ngày khởi hành
- [x] Dòng bằng 0 không xuất hiện trong bảng phân rã

### 9.2. Giải trạng thái — `domain`

- [x] `GUARANTEED` thắng `FEW_SEATS` khi cả hai điều kiện đúng
- [x] Nhân viên đặt `SOLD_OUT` tay khi vẫn còn chỗ → hiện `SOLD_OUT`
- [x] Không có đường nào cho ra `GUARANTEED` từ ghi đè tay
- [x] `PENDING` thắng tất cả
- [ ] Du thuyền: một hạng hết chỗ không làm ngày đó hết chỗ

### 9.3. Tồn kho — `infrastructure`, Testcontainers với Postgres thật

- [ ] **Hai luồng cùng giữ chỗ cuối cùng: đúng một luồng thành công**
- [ ] Chỗ khả dụng trừ cả giữ chỗ còn hạn
- [ ] Giữ chỗ quá hạn không còn được tính, **kể cả trước khi job quét chạy**
- [ ] Chuyển giữ chỗ đã hết hạn thành đơn → `SEAT_HOLD_EXPIRED`
- [ ] Huỷ đơn trả chỗ về kho ngay
- [ ] Job quét chạy hai lần cho cùng kết quả

Test đầu của 9.3 là test quan trọng nhất của cả dự án. Nó phải chạy **hai
transaction thật song song**, không phải mô phỏng.

---

## 10. Việc còn để ngỏ

| Việc | Chặn cái gì | Ai quyết |
|---|---|---|
| Tỷ lệ đặt cọc và phí xử lý ở `VN` | Bảng hằng số mục 2.4 | Nghiệp vụ |
| Giảm đặt sớm hai bậc có đúng không | Mục 2.4 | Nghiệp vụ |
| `VN` có làm tròn tới nghìn đồng không | Mục 3 | Nghiệp vụ |
| Hạn giữ chỗ 20 phút có hợp lý không | Mục 2.4 | Nghiệp vụ |
| Bậc thời gian huỷ và tỷ lệ hoàn từng bậc | `23`, `30` | Nghiệp vụ + pháp lý |
| Hạn chốt để huỷ tour vì thiếu khách | Mục 6.6 | Nghiệp vụ |
