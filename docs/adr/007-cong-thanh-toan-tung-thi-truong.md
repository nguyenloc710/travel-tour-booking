# ADR-007 — Cổng thanh toán từng thị trường

```
Trạng thái: Đề xuất
Ngày: 02/09/2026
```

> **Chưa chốt.** `40` mục 5 giao trách nhiệm **A** của mảng thanh toán (`30`) cho
> Chủ sản phẩm, với Pháp chế là **C**. Tài liệu này quyết **hình dạng tích hợp** —
> phần thuộc kiến trúc — và cố ý không quyết hai thứ không thuộc về nó: có thanh
> toán online ở v1 hay không (**Q-3**), và chọn nhà cung cấp nào. Mục 7 liệt kê
> đúng những gì còn thiếu.

## Bối cảnh

**Q-3 có thể xoá cả một tài liệu.** `00` mục 2 ghi thẳng: nếu v1 chỉ nhận đặt chỗ
rồi gọi điện chốt thì bỏ được hẳn `30`, bộ tài liệu còn 24 file. Câu hỏi đó chưa
ai trả lời từ 31/08/2026.

Nhưng "cổng thanh toán **từng thị trường**" là câu hỏi về **hình dạng**, và nó trả
lời được ngay — thậm chí nên trả lời trước. Nếu chọn sai hình dạng, ngày Q-3 nói
"có" thì phải sửa cả đường đặt tour, chứ không phải thêm một module.

## 1. Cái đã bị chốt sẵn ở nơi khác

Sáu ràng buộc dưới đây **không** do ADR này đặt ra. Chúng đã nằm trong tài liệu đã
viết, và chúng thu hẹp lựa chọn nhiều hơn người ta tưởng:

| Ràng buộc | Ở đâu | Nó loại bỏ cái gì |
|---|---|---|
| **Webhook là nguồn sự thật, không phải URL khách quay về** — URL đó khách gõ tay được | `23` mục 5.3 | Cổng không có webhook ký và gửi lại đáng tin |
| **Thu hai lần trên một đơn**: đặt cọc trước, phần còn lại trước ngày đi | `23` mục 5.1, `14` mục 2.4 | Cổng chỉ làm được một lần thanh toán một lần dứt điểm |
| `deposit_rate` và `processing_fee` là **cấu hình từng thị trường** | `12` bảng `market` | Một tỷ lệ đặt cọc dùng chung |
| **Không quy đổi tỷ giá ở bất kỳ đâu** | `CLAUDE.md` điều 4 | Thu một tiền tệ rồi quy ra tiền tệ kia |
| Đường tạo đơn đã **bất biến khi gọi lại** | `13` mục 7, `V4` | Phải nghĩ lại chống trùng từ đầu |
| API trả **mã lỗi**, không trả câu tiếng người | `13` mục 5 | Đẩy thẳng thông báo của nhà cung cấp ra khách |

Dòng đầu là dòng nặng nhất, và nó đã được quyết đúng: tiền vào hay không là việc
giữa máy chủ và cổng, không phải việc trình duyệt khách kể lại.

## 2. Quyết định

**Một hợp đồng nội bộ, một adapter cho mỗi thị trường.** Không giả định ở bất kỳ
đâu rằng một cổng phục vụ được cả hai thị trường.

Ba lý do, và không lý do nào là kỹ thuật:

1. **Hai tập phương thức thanh toán mà khách thật sự dùng.** Khách Đan quen
   MobilePay và Dankort; khách Việt quen thẻ nội địa và ví điện tử. Một cổng toàn
   cầu phục vụ tốt một bên và kém bên kia — và "kém" ở đây nghĩa là khách bỏ giỏ.
2. **Hai chế độ pháp lý.** Du lịch trọn gói bán ở Đan Mạch dính
   **Rejsegarantifonden**; Việt Nam có Luật Du lịch 2017 với bộ quy tắc riêng
   (`32`). Tiền của khách ở hai thị trường chịu hai ràng buộc khác nhau, kể cả khi
   cùng một cổng xử lý được cả hai.
3. **Không có tỷ giá trong hệ thống này.** Mỗi thị trường là một dòng tiền riêng,
   một lần đối soát riêng, một tài khoản nhận riêng. Đó là hệ quả trực tiếp của
   điều 4 `CLAUDE.md`, không phải một lựa chọn mới.

Đây cùng một mô-típ với ADR-006 (`departure` thuộc về **một** thị trường): thứ gì
dính tới tiền hoặc tồn kho thì thuộc về một thị trường, không dùng chung.

## 3. Hợp đồng nội bộ phải làm được năm việc

| Việc | Ghi chú |
|---|---|
| Khởi tạo **một lần thu** — đặt cọc hoặc phần còn lại | Không phải "thanh toán đơn hàng": một đơn có nhiều lần thu |
| **Xác minh chữ ký** webhook | Trước khi đọc bất cứ trường nào trong thân yêu cầu |
| **Chống trùng webhook** | Giao hàng ít nhất một lần là chuẩn của mọi cổng. Cùng một sự kiện tới hai lần **không được** cộng tiền hai lần |
| Hoàn tiền **một phần** | Bậc huỷ và tỷ lệ hoàn ở `14`, `23` mục 8 — chưa chốt (**Q-2**) |
| Trả **mã lỗi của hệ thống này** | Không phải mã của nhà cung cấp |

Dòng thứ ba đã có sẵn hình dạng để mượn: bảng `idempotency_key` của `V4` giải đúng
bài toán "cùng một thứ tới hai lần" cho đường tạo đơn. Webhook cần cơ chế cùng
loại, khoá theo **mã sự kiện của cổng**, không theo mã đơn — một đơn có nhiều sự
kiện hợp lệ.

## 4. Cái **không được** rò ra khỏi adapter

- Kiểu dữ liệu, tên trường và mã lỗi của nhà cung cấp
- **Dữ liệu thẻ.** Hệ thống này không bao giờ nhận, không bao giờ lưu, không bao
  giờ ghi log số thẻ. Khách nhập thẻ ở trang của cổng — chuyển hướng hoặc khung
  nhúng. Đây là ranh giới không thương lượng: chạm vào dữ liệu thẻ là kéo toàn bộ
  hệ thống vào phạm vi kiểm định PCI
- Giả định "chỉ có một cổng" trong máy trạng thái đơn (`23` mục 4) và trong bất kỳ
  truy vấn báo cáo nào

## 5. Lược đồ còn thiếu một bảng, và đây là chỗ phải nói thẳng

**Không có bảng nào ghi một lần thu tiền.** `booking` có `total`, `deposit`,
`currency` — tức là biết *phải trả bao nhiêu*, không biết *đã trả gì*. Thiếu bảng
đó thì ba việc không làm được:

- **Đối soát** — `00` giao cho `30` sở hữu việc này, mà không có bản ghi giao dịch
  thì không có gì để đối chiếu với sao kê của cổng
- **Chống trùng webhook** — không có chỗ ghi "sự kiện này xử lý rồi"
- Trả lời "khách đã trả bao nhiêu" mà không phải suy diễn từ trạng thái đơn

Bảng đó cần tối thiểu: mã giao dịch của cổng, mã sự kiện đã xử lý, thị trường,
loại thu (đặt cọc / phần còn lại / hoàn), số tiền kèm tiền tệ, trạng thái, thời
điểm. Và nó phải là bảng **chỉ ghi thêm**, cùng nhóm D với `booking_event` (`11`
mục 11.2): sửa được lịch sử tiền là bỏ luôn giá trị của nó.

Đây là **migration bắt buộc trước dòng tiền thật đầu tiên**, không phải việc dọn
sau. Ghi vào `12` mục 10.

## 6. Cái này **không** quyết

- **Q-3** — v1 có thanh toán online thật hay không. Thuộc Chủ sản phẩm, và câu trả
  lời "không" làm ADR này đúng nhưng chưa dùng tới
- Nhà cung cấp cụ thể của từng thị trường — có phần chi phí và phần hợp đồng
- Bậc huỷ, tỷ lệ hoàn, tỷ lệ đặt cọc của `VN` — **Q-2**
- Nghĩa vụ Rejsegarantifonden và cách chứng minh — `32`, cần người có chuyên môn
  pháp lý ký (`40` mục 4)

## 7. Cần gì để chuyển sang `Đã chốt`

| # | Việc | Ai |
|---|---|---|
| 1 | **Q-3** — trả lời trước mọi thứ khác; nó quyết `30` có tồn tại không | CSH |
| 2 | Viết `30` | CSH là **A**, BE là **R** |
| 3 | `32` — nghĩa vụ pháp lý với tiền của khách ở cả hai thị trường | Pháp chế |
| 4 | Chọn hai nhà cung cấp, kiểm đủ năm việc ở mục 3 | KTS đề xuất, CSH duyệt |
| 5 | Migration cho bảng giao dịch ở mục 5 | KTS + BE |

Chốt xong thì sửa `10` mục 10, `41` mục 4, và đổi **chỉ khối trạng thái** ở đầu
file này. Thân ADR giữ nguyên kể cả phần về sau hoá ra sai — đó là hồ sơ lịch sử
(`42` mục 5.3).
