# ADR-009 — `COMBO` là bó cố định do nhân viên soạn

```
Trạng thái: Đề xuất
Ngày: 02/09/2026
```

> **Chưa chốt.** Q-7 giao cho Chủ sản phẩm (`41` mục 4). ADR này gom lại phần đã
> phân tích xong ở `04` mục 10, biến đề xuất ở đó thành một quyết định ghi được,
> và nêu thêm ba thứ `04` chưa nói: Q-7 hỏi sai tiền đề, lược đồ còn thiếu hai
> thứ, và "không khoá cửa" chỉ đúng nếu giữ được bốn tính chất ở mục 5.

## Bối cảnh

`04` mục 10 đã làm gần hết việc: nó chỉ ra rằng `COMBO` không phải "một loại tour
nữa" mà là **sản phẩm bán hàng tồn kho của người khác**, liệt kê bốn khoản chi phí
thật, kết luận rằng khối lượng **ngang cả phần còn lại của dự án cộng lại**, và đề
xuất thu hẹp thành bó cố định. Nó kết thúc bằng đúng một câu: *"Cần một ADR khi
bắt đầu làm."*

Đây là ADR đó.

## 1. Q-7 hỏi sai tiền đề, và điều đó gỡ nó khỏi đường găng

Q-7 viết: *"`COMBO` **v1** làm bó cố định do nhân viên soạn, hay tồn kho thời gian
thực?"* — nhưng **`COMBO` không nằm trong v1**:

| Nguồn | Nói gì |
|---|---|
| `04` mục 2 | `COMBO` · thị trường `VN` · **v1.5** |
| `01` mục 3.2 | "Combo bay + khách sạn (`COMBO`) — **v1.5**" |
| `40` — tám tiêu chí ra của G4 | **Không nhắc `COMBO` một lần nào** |

`41` mục 4 ghi Q-7 chặn **G4**. Không đúng: không tiêu chí nào của G4 chạm tới
`COMBO`, nên câu hỏi này chặn **v1.5**, không chặn cổng nào của v1.

Đây không phải chuyện chữ nghĩa. Một câu hỏi treo bị gắn nhầm vào cổng gần nhất
làm hai việc cùng lúc: nó tạo áp lực giả lên cổng đó, và nó **giấu mất** việc thật
là chưa ai lên lịch cho v1.5.

## 2. Quyết định

**`COMBO` ở v1.5 là bó cố định do nhân viên soạn sẵn. Không nối tồn kho thời gian
thực.**

Cụ thể, theo `04` mục 10:

- Nhân viên nhập sẵn tuyến bay, giờ bay, danh sách khách sạn chọn được, giá trọn
  gói theo từng khoảng ngày, và **số suất còn**
- Khách chọn từ những gì đã soạn, không tra cứu thời gian thực
- Tồn kho là số suất nhân viên nhập tay

## 3. Vì sao

Bảng chi phí ở `04` mục 10 đã đủ, nên ở đây chỉ nêu điều nó chưa nói thành lời:
**ba trong bốn khoản chi phí đó không phải chi phí lập trình.** Nối GDS cần hợp
đồng; nối channel manager cần hợp đồng; luật vé đổi/huỷ của từng hãng cần người
đọc và giữ cho khớp. Tăng người viết mã không rút ngắn được chúng.

Khoản thứ tư thì là kỹ thuật, và là khoản nguy hiểm nhất: **giữ chỗ hai nhà cung
cấp cùng lúc** là giao dịch phân tán. Giữ được vé mà hết phòng thì phải hoàn vé —
và đó là một đường mã chỉ chạy khi có sự cố, tức là đường ít được thử nhất trong
hệ thống, lại là đường đụng tới tiền thật của khách.

Bó cố định giữ được phần lớn giá trị bán hàng với một phần nhỏ khối lượng.

## 4. Cái đã dựng sẵn, và cố ý dừng ở đó

`COMBO` **không phải chưa có gì**. Vỏ đã thật:

| Đã có | Ở đâu |
|---|---|
| `ProductType.COMBO`, ràng buộc `ck_product_type` | `12` mục 4.1 |
| Bảng con `product_combo` — `nights`, `valid_from`, `valid_to` | `12` mục 4.2 |
| `ComboDetail` ở hợp đồng API, `ComboFields` ở bề mặt quản trị | `contracts/openapi.yaml` |
| Tạo và sửa một sản phẩm `COMBO` qua trang quản trị | Đợt 5b |
| `04` mục 8: thẻ sản phẩm riêng · `05` mục 7: trang chi tiết **không dùng khung tab** | Đã đặc tả |

Nghĩa là hôm nay nhân viên tạo được một sản phẩm `COMBO`, viết mô tả, dịch, gán
thị trường. Cái chưa có là **thành phần của bó** — chuyến bay, đêm phòng, số suất
— và đó chính là ranh giới v1/v1.5.

## 5. "Không khoá cửa" chỉ đúng nếu giữ được bốn thứ

`04` mục 10 hứa rằng nâng lên tồn kho thời gian thực về sau "chỉ thay tầng nhà
cung cấp, giao diện và luồng đặt giữ nguyên". Lời hứa đó **không tự đúng**. Nó
đúng khi và chỉ khi v1.5 giữ được:

1. **Số suất đọc qua cùng một khái niệm "còn bao nhiêu chỗ"** như `departure`
   (`14` mục 6). Nếu bó cố định tự đẻ ra một cách đếm chỗ riêng thì ngày nối nhà
   cung cấp sẽ có hai cách đếm, và chúng sẽ lệch.
2. **Giữ chỗ đi qua đúng khoá bi quan đã có** (`14` mục 6, `V1` bảng `seat_hold`),
   không phải một cơ chế thứ hai. Tồn kho thời gian thực sẽ thêm bước gọi ra
   ngoài, không thay cơ chế.
3. **Chính sách huỷ tách theo thành phần ngay từ v1.5** (`14` mục 8, `05` mục 7
   quy tắc 9). Làm một tỷ lệ chung cho cả đơn ở v1.5 rồi tách sau là phải sửa cả
   đơn cũ — mà đơn đã đặt thì **chụp lại** điều kiện lúc đặt và không sửa được.
4. **Giá của bó là giá người nhập**, theo từng khoảng ngày và từng thị trường —
   không phải tổng cộng máy tính ra từ giá thành phần. Cùng lý do với điều 4 của
   `CLAUDE.md`.

Điều 3 là điều đắt nhất nếu làm sai, vì nó hỏng **về sau** chứ không hỏng ngay.

## 6. Lược đồ còn thiếu hai thứ

Cả hai chỉ cần khi bắt đầu v1.5, nhưng phải ghi ra trước khi quên:

**Điểm khởi hành của `COMBO` không có chỗ lưu.** `11` mục 4 liệt kê `product_combo`
gồm `nights`, **`origin_city`**, `valid_from`, `valid_to` — nhưng DDL thật ở `12`
chỉ có ba cột, không có `origin_city`. Trong khi đó `04` mục 6 xếp `COMBO` vào
nhóm có điểm khởi hành, `04` mục 8 nói thẻ sản phẩm hiện *"Khởi hành từ Hà Nội"*,
và `14` mục 2 nói với `COMBO` thì điểm khởi hành là **biến thể sản phẩm, đã nằm
trong giá cơ bản**. Ba tài liệu cần cột đó, một tài liệu tưởng nó đã có, lược đồ
thì không có.

**Không có bảng nào cho thành phần của bó** — chuyến bay, danh sách khách sạn chọn
được, số suất theo khoảng ngày. Đây là phần chính của v1.5.

Ghi ở `12` mục 10.

## 7. Hệ quả đã thấy ngay ở v1

**`COMBO` không có `departure`, nên nó không có `price_from`.** Trigger của `V5`
tính giá từ từ `departure_price`, mà `COMBO` không có lịch khởi hành — nên listing
hiện "Liên hệ" cho mọi sản phẩm `COMBO`, mãi mãi, cho tới khi v1.5 định nghĩa giá
của bó nằm ở đâu. `INDIVIDUAL_PACKAGE` cũng dính, và đó đã là một dòng ở `12`
mục 10.

Không phải lỗi. Nhưng nếu ai đó tạo một sản phẩm `COMBO` hôm nay và ngạc nhiên vì
nó không hiện giá, đây là lý do.

## 8. Cái này **không** quyết

- **Bao giờ làm v1.5** — thuộc Chủ sản phẩm, và hiện chưa có trong `40`
- Nhà cung cấp vé bay và phòng khi nâng lên thời gian thực
- `DAY_TOUR` (v2) — khác `Excursion` đang có ở v1, `04` mục 11 đã nói rõ đừng gộp

## 9. Cần gì để chuyển sang `Đã chốt`

| # | Việc | Ai |
|---|---|---|
| 1 | Xác nhận Q-7 với tiền đề đã sửa: câu hỏi là về **v1.5**, không phải v1 | CSH |
| 2 | Đưa v1.5 vào `40` — hiện kế hoạch dừng ở G6 và không có v1.5 | CSH + KTS |
| 3 | Bổ sung `12`: cột điểm khởi hành và bảng thành phần bó | KTS |

Chốt xong thì sửa `10` mục 10, `41` mục 4, `04` mục 13, và đổi **chỉ khối trạng
thái** ở đầu file này (`42` mục 5.3).
