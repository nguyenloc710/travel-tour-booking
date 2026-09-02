# Thanh toán

```
Trạng thái: Nháp
Cập nhật: 02/09/2026
Nguồn sự thật về: thu tiền làm mấy lần và vào lúc nào, phương thức thanh toán của
                  từng thị trường, webhook và cách chống trùng, hoàn tiền và bậc
                  huỷ, đối soát.
Không nói về: máy trạng thái đơn đặt (23 mục 4), tám bước cộng dồn giá và quy tắc
              làm tròn (14), hình dạng tích hợp cổng thanh toán (ADR-007), lược
              đồ (12), danh mục endpoint và mã lỗi (13), nghĩa vụ pháp lý với
              tiền của khách (32), bí mật và triển khai (34).
```

Tài liệu này trả lời: **tiền đi từ khách vào tài khoản công ty bằng đường nào, và
đi ngược lại bằng đường nào.**

> **Cảnh báo tồn tại.** `00` mục 2 ghi thẳng: trả lời **Q-3** là "không" thì bỏ
> được hẳn tài liệu này. Mục 1 chia rõ phần nào biến mất cùng nó và phần nào
> không — vì kể cả khi v1 chỉ nhận đặt chỗ rồi gọi điện chốt, tiền vẫn chuyển
> khoản, vẫn phải hoàn, và vẫn phải đối soát.

---

## 1. Q-3 chia tài liệu này làm hai

| Mục | Sống sót nếu Q-3 = "không có thanh toán online" |
|---|---|
| 2 — Thu bao nhiêu, thu khi nào | **Có.** Đặt cọc và phần còn lại vẫn tồn tại, chỉ đổi đường thu |
| 3 — Phương thức từng thị trường | Thu hẹp còn **một dòng**: chuyển khoản ngân hàng |
| 4 — Webhook | **Không.** Không có cổng thì không có webhook |
| 5 — Hoàn tiền và bậc huỷ | **Có.** Hoàn tiền là nghĩa vụ với khách, không phải tính năng của cổng |
| 6 — Đối soát | **Có.** Vẫn phải khớp sổ với sao kê ngân hàng |

Nghĩa là **ba trong năm mục không phụ thuộc Q-3**. Đó là lý do viết tài liệu này
trước khi Q-3 có câu trả lời không phải việc làm thừa — nhưng cũng là lý do mục 3
và mục 4 cố tình viết mỏng, chờ câu trả lời rồi mới dày lên.

---

## 2. Thu bao nhiêu, thu khi nào

**Hai lần thu trên một đơn**, không phải một:

| Lần | Thu gì | Khi nào |
|---|---|---|
| 1 | **Đặt cọc** | Lúc đặt |
| 2 | **Phần còn lại** | Trước ngày khởi hành — mốc cụ thể chưa chốt, mục 8 |

Số tiền của từng lần **không định nghĩa ở đây**: tỷ lệ đặt cọc là cấu hình từng
thị trường và `deposit + balance = total` phải đúng tuyệt đối — cả hai thuộc `14`
mục 2.4 và mục 3. Chỗ này chỉ nói về **việc thu**.

Ba ràng buộc:

- **Tiền tệ là tiền tệ của thị trường.** Không quy đổi, không thu một tiền tệ rồi
  ghi nhận sang tiền tệ kia (`CLAUDE.md` điều 4).
- **Mỗi lần thu là một bản ghi riêng.** Không cộng dồn vào một trường "đã trả" —
  lý do ở ADR-007 mục 5.
- **Không thu tiền trước khi có chỗ.** Giữ chỗ đi trước, thanh toán đi sau
  (`23` mục 2). Ngược lại là thu tiền cho một chỗ có thể đã hết.

---

## 3. Phương thức của từng thị trường

> **Mục này cố tình chưa liệt kê nhà cung cấp**, và đó là chỗ dễ bịa nhất của cả
> tài liệu. Chọn phương thức thanh toán là quyết định nghiệp vụ có số liệu đi
> kèm — tỷ lệ bỏ giỏ, phí giao dịch, thói quen của đúng nhóm khách này — chứ
> không phải chuyện đoán từ xa. Dưới đây là **tiêu chí chọn**, không phải kết quả.

Hai thị trường là **hai bộ phương thức**, không phải một bộ dùng chung — ADR-007.

### 3.1. Tiêu chí bắt buộc, áp cho mọi phương thức

| Tiêu chí | Vì sao |
|---|---|
| Có **webhook ký được xác minh**, gửi lại khi thất bại | `23` mục 5.3: webhook là nguồn sự thật |
| Thu được **hai lần cách nhau nhiều tháng** trên cùng một đơn | Mục 2 |
| **Hoàn một phần** được | Mục 5 |
| Khách **không nhập thẻ trên trang của mình** | ADR-007 mục 4 — chạm dữ liệu thẻ là kéo cả hệ thống vào phạm vi PCI |
| Thu được đúng tiền tệ của thị trường | Không có tỷ giá trong hệ thống này |

Tiêu chí đầu loại nhiều hơn người ta tưởng: một cổng chỉ có "trang thành công" mà
không có webhook đáng tin thì không dùng được, dù tích hợp dễ tới đâu.

### 3.2. Câu hỏi phải trả lời cho từng thị trường

1. Khách của thị trường này thật sự trả bằng gì? — số liệu, không phải cảm nhận
2. Phí giao dịch bao nhiêu, ai chịu? Phí xử lý `295 kr` ở `DK` (`14` mục 2.4) có
   phải để bù khoản này không?
3. Tiền về tài khoản sau bao lâu? Nó quyết định mục 6 chạy theo nhịp nào
4. Hoàn tiền mất bao lâu và có mất phí không?

---

## 4. Webhook

**Nguồn sự thật của việc trả tiền là webhook, không phải URL khách quay về** —
`23` mục 5.3 đã chốt, không nhắc lại lý do ở đây.

Bốn quy tắc xử lý, theo đúng thứ tự:

| # | Quy tắc | Bỏ qua thì sao |
|---|---|---|
| 1 | **Xác minh chữ ký trước khi đọc bất kỳ trường nào** trong thân yêu cầu | Ai cũng gửi được một yêu cầu nói "đơn này đã trả tiền" |
| 2 | **Chống trùng theo mã sự kiện của cổng**, không theo mã đơn | Mọi cổng gửi ít nhất một lần. Một đơn có nhiều sự kiện hợp lệ, nên khoá theo mã đơn là chặn nhầm |
| 3 | **Ghi sự kiện trước, xử lý sau** | Sập giữa chừng thì mất hẳn một lần trả tiền, và không có dấu vết nào để dò lại |
| 4 | **Không giả định thứ tự tới** | Sự kiện "hoàn tiền" tới trước "thu tiền" là chuyện có thật khi có lần gửi lại |

Quy tắc 2 có sẵn khuôn để mượn: bảng `idempotency_key` (`12`, migration `V4`) giải
đúng bài toán này cho đường tạo đơn.

**Đường dẫn webhook phải công khai và ổn định từ trước khi tích hợp** — nó là địa
chỉ khai báo ở phía cổng, không đổi được dễ. `34` mục 9 đã ghi đây là việc chặn.

---

## 5. Hoàn tiền và bậc huỷ

### 5.1. Quy tắc không phụ thuộc con số

- **Hoàn về đúng phương thức đã thu.** Không hoàn sang tài khoản khác vì khách
  nhờ — đó là đường đi của gian lận, và nó cũng làm mất dấu đối soát.
- **Hoàn một phần phải làm được**, vì bậc huỷ hiếm khi là 0% hoặc 100%.
- **Hoàn tiền và trả chỗ về kho là hai việc độc lập** — `23` mục 8.
- **Nhà điều hành huỷ chuyến vì thiếu khách thì hoàn 100%**, không áp bậc nào —
  `14` mục 6.6.
- Mỗi lần hoàn là **một bản ghi riêng**, cùng lý do với mỗi lần thu.

### 5.2. Bậc huỷ — hình dạng có, số chưa có

Bậc huỷ là bảng theo **thị trường** và theo **khoảng thời gian còn lại tới ngày
khởi hành**:

```
thị trường · số ngày còn lại tối thiểu · tỷ lệ hoàn
```

**Không con số nào trong bảng đó đã được chốt**, cho cả hai thị trường — `14`
mục 10 và `23` mục 10 đều ghi việc này còn treo, và với `VN` thì nó nằm trong sáu
con số của **Q-2**.

Vì thế `23` mục 8 đã quyết: **khách không tự huỷ được ở v1**. Tự động hoá một
chính sách chưa chốt là cách chắc chắn để hoàn sai tiền.

`COMBO` không dùng bảng này: chính sách huỷ của nó **tách theo thành phần**
(`14` mục 8) — và đó là một trong bốn tính chất ADR-009 bắt v1.5 phải giữ.

---

## 6. Đối soát

**Đối soát là so sổ của mình với sao kê của bên giữ tiền.** Nó không phải việc kế
toán làm cuối tháng — nó là việc chạy hằng ngày, vì ba loại lệch dưới đây càng
phát hiện muộn càng khó dò:

| Loại lệch | Nghĩa là | Nguy hiểm vì |
|---|---|---|
| Cổng có, hệ thống không | Tiền vào mà đơn không chuyển trạng thái | Khách đã trả tiền và không được đi |
| Hệ thống có, cổng không | Đơn tưởng đã trả | Cho đi mà không thu được tiền |
| Cả hai có, số khác nhau | Sai số tiền, hoặc phí bị trừ vào gốc | Âm thầm, và cộng dồn |

Ba việc bắt buộc:

1. **Phải có bảng ghi từng lần thu và hoàn.** Không có nó thì không có gì để đối
   chiếu — ADR-007 mục 5 ghi đây là migration bắt buộc trước dòng tiền thật đầu
   tiên, và lược đồ hiện **chưa có** bảng đó.
2. **Phí của cổng không trừ vào số tiền ghi nhận.** Khách trả 24.990, cổng chuyển
   về 24.700 — số của khách vẫn là 24.990. Trừ phí vào gốc là làm mọi đối chiếu
   với đơn hàng lệch đúng bằng phí.
3. **Đối soát theo từng thị trường riêng.** Hai thị trường, hai tiền tệ, hai
   tài khoản nhận — cộng chung là mất khả năng phát hiện lệch ở một bên.

---

## 7. Cấm

- Nhận, lưu hoặc ghi log dữ liệu thẻ ở bất kỳ đâu trong hệ thống này
- Tin URL khách quay về thay cho webhook
- Xử lý thân webhook trước khi xác minh chữ ký
- Chống trùng webhook theo mã đơn thay vì mã sự kiện
- Hoàn tiền sang phương thức khác phương thức đã thu
- Trừ phí cổng vào số tiền ghi nhận của đơn
- Cộng dồn nhiều lần thu vào một trường "đã trả"
- Quy đổi tỷ giá ở bất kỳ bước nào — hệ thống này **không có** tỷ giá
- Tự động huỷ và tự động hoàn khi tiền vào muộn hơn hạn giữ chỗ — `23` mục 5.3
  bắt chuyển cho nhân viên xử lý tay

---

## 8. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| **Q-3 — v1 có thanh toán online thật không** | Mục 3 và mục 4 của chính tài liệu này | `41` mục 4 |
| Nhà cung cấp của từng thị trường | Mục 3 | ADR-007 mục 7 |
| **Bậc huỷ và tỷ lệ hoàn**, cả hai thị trường | Mục 5.2, và việc khách tự huỷ ở `23` | `14` mục 10, **Q-2** |
| Tỷ lệ đặt cọc và phí xử lý của `VN` | Mục 2 | **Q-2** |
| Mốc thu phần còn lại, và ai kích hoạt: khách tự trả hay nhân viên gửi yêu cầu | Mục 2 | `23` mục 10 |
| Bảng ghi giao dịch — chưa có trong lược đồ | Mục 6 | `12` mục 10, ADR-007 mục 5 |
| Nghĩa vụ pháp lý với tiền của khách ở `DK` (Rejsegarantifonden) | Có được giữ tiền khách không, và giữ thế nào | `32` |
