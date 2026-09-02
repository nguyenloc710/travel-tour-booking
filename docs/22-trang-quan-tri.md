# Trang quản trị

```
Trạng thái: Nháp
Cập nhật: 02/09/2026
Nguồn sự thật về: danh mục màn hình quản trị, bốn vai trò và ma trận quyền, ba
                  màn hình dịch thuật, checklist mở bán một sản phẩm.
Không nói về: máy trạng thái đơn đặt và luồng huỷ hoàn (23), quy tắc tính giá và
              tồn kho (14), lược đồ (12), endpoint (13), quy trình dịch nhìn từ
              nghiệp vụ (02 mục 8), token và component (21).
```

Tài liệu này trả lời: **nhân viên làm được gì, ai được làm cái gì, và màn hình
nào phục vụ việc đó.**

Nguyên tắc bao trùm: **nhân viên phải mở bán được một sản phẩm mới, đủ hai ngôn
ngữ, mà không cần lập trình viên.** Đó là tiêu chí ra số 7 của cổng G4 (`40`), và
là thước đo duy nhất đáng tin cho trang quản trị.

---

## 1. Trang quản trị khác website khách ở đâu

| | Website khách | Trang quản trị |
|---|---|---|
| Locale trong URL | Có | **Không** |
| Market | Suy từ locale, ghi đè bằng cookie | **Không có** — nhân viên làm việc xuyên thị trường |
| Dữ liệu trả về | Đúng **một** bản dịch | **Tất cả** bản dịch của một bản ghi |
| Nội dung chưa xuất bản | Không bao giờ | Có |
| Cache | Có | `no-store` |
| Xác thực | Không | Phiên đăng nhập, bốn vai trò |

Dòng thứ nhất là chỗ dễ cài sai nhất. Ở trang quản trị, locale là **thuộc tính
của dữ liệu đang sửa**, không phải của người đang sửa: màn hình dịch song song mở
bản `da` và bản `vi` cạnh nhau trên cùng một trang. Giao diện của chính trang
quản trị dùng một ngôn ngữ — xem mục 8.

Dòng thứ hai cũng vậy: một biên tập viên nhập giá cho `DK` và `VN` trong cùng một
màn hình. Nhét market vào đường dẫn quản trị là bắt họ chuyển qua chuyển lại.

---

## 2. Bốn vai trò

| Vai trò | Việc chính |
|---|---|
| `CONSULTANT` | Tư vấn viên: xem yêu cầu tư vấn, xem đơn, dựng báo giá |
| `EDITOR` | Người viết nội dung bản `da` |
| `TRANSLATOR` | Người dịch `da` → `vi` |
| `ADMIN` | Toàn quyền, gồm giá và người dùng |

Một người có thể mang nhiều vai trò. Ở công ty quy mô này, một người thường vừa
viết vừa dịch — mô hình phải cho phép, nhưng **quyền vẫn tính theo vai trò**, để
lúc đội lớn lên thì không phải viết lại.

### 2.1. Ma trận quyền

`R` đọc · `W` ghi · `–` không thấy màn hình.

| Tài nguyên | CONSULTANT | EDITOR | TRANSLATOR | ADMIN |
|---|---|---|---|---|
| Sản phẩm — bản `da` | R | **W** | R | W |
| Sản phẩm — bản `vi` | R | R | **W** | W |
| Xuất bản bản dịch | – | W | – | W |
| Gán sản phẩm vào thị trường | – | – | – | **W** |
| Giá: `departure_price`, `price_tier`, `price_from` | R | – | – | **W** |
| Ngày khởi hành: tạo, sửa, đóng bán | R | – | – | W |
| Nội dung khác: điểm đến, bài viết, sự kiện | R | W | W (bản `vi`) | W |
| Đơn đặt: xem | R | – | – | R |
| Đơn đặt: đổi trạng thái, huỷ, hoàn | **W** | – | – | W |
| Báo giá: dựng, gửi | **W** | – | – | W |
| Yêu cầu tư vấn | W | – | – | W |
| Người dùng và vai trò | – | – | – | **W** |
| Hàng đợi dịch, bảng độ phủ | R | R | R | R |

Ba điều cố ý:

1. **Chỉ `ADMIN` gán được sản phẩm vào thị trường.** Đây là công tắc "bắt đầu
   bán được" (`01` mục 4.4): dịch xong mà chưa gán thì sản phẩm vẫn không tồn tại
   với khách. Để nó ở tay người chịu trách nhiệm doanh thu, không ở tay người
   viết nội dung.
2. **`EDITOR` không sửa được bản `vi` và `TRANSLATOR` không sửa được bản `da`.**
   Người dịch sửa bản nguồn là làm hai bản lệch nhau mà không ai biết bản nào
   đúng — và `da` là ngôn ngữ nguồn (ADR-004).
3. **Giá tách khỏi nội dung.** Người viết mô tả tour không nên chạm được vào con
   số khách phải trả.

---

## 3. Bản đồ màn hình

Nhóm theo **tài nguyên**, không nhóm theo màn hình của từng vai trò — vai trò chỉ
quyết định thấy hay không thấy. Nhóm theo vai trò là cách chắc chắn để cùng một
danh sách tồn tại ở ba chỗ và lệch nhau dần.

| # | Màn hình | Ghi chú |
|---|---|---|
| M1 | Bảng điều khiển | Việc cần làm hôm nay: đơn chờ xác nhận quá 24 giờ, báo giá sắp hết hạn, ngày khởi hành sắp tới hạn chốt mà thiếu khách, hàng đợi dịch |
| M2 | Danh sách sản phẩm | Lọc theo loại, thị trường, trạng thái dịch |
| M3 | Sửa sản phẩm | Tab: thông tin chung · bản `da` · bản `vi` · thị trường và giá · ngày khởi hành |
| M4 | Ngày khởi hành | Theo sản phẩm; hiện `capacity`, `seats_booked`, chỗ còn lại, trạng thái |
| M5 | Giá | `departure_price` theo loại khách và kiểu phòng; `price_tier` cho `PRIVATE_TOUR` |
| M6 | Danh sách đơn | Lọc theo trạng thái, thị trường, khoảng ngày |
| M7 | Chi tiết đơn | Bảng phân rã giá, hành khách, **toàn bộ nhật ký `booking_event`** |
| M8 | Báo giá | Dựng, gửi, theo dõi trạng thái |
| M9 | Yêu cầu tư vấn | Gán cho tư vấn viên, đánh dấu đã xử lý |
| M10 | Hàng đợi dịch | Mục 4.1 |
| M11 | Dịch song song | Mục 4.2 |
| M12 | Bảng độ phủ dịch | Mục 4.3 |
| M13 | Nội dung khác | Điểm đến, khách sạn, tham quan, bài viết, sự kiện |
| M14 | Người dùng và vai trò | Chỉ `ADMIN` |

M1 không phải trang trang trí. Ba việc trên đó — đơn quá hạn xác nhận, báo giá
sắp hết hạn, chuyến thiếu khách sắp tới hạn chốt — là ba việc mà **không ai phát
hiện nếu không có màn hình nhắc**, và cả ba đều mất tiền thật.

---

## 4. Ba màn hình dịch thuật

Quy trình nghiệp vụ ở `02` mục 8. Ở đây là màn hình.

### 4.1. Hàng đợi dịch (M10)

Bản ghi có bản `da` là `PUBLISHED` nhưng bản `vi` **thiếu** hoặc **`OUTDATED`**.

| Cột | Ghi chú |
|---|---|
| Loại thực thể | Sản phẩm, điểm đến, bài viết… |
| Tiêu đề bản `da` | Bấm vào mở M11 |
| Tình trạng | `THIẾU` hoặc `OUTDATED` |
| Sửa bản `da` lần cuối | Với `OUTDATED`, đây là lý do nó quay lại hàng đợi |
| Mức ưu tiên | Mục 4.1.1 |

**`OUTDATED` là giá trị tính ra, không lưu**: `translation(da).lastModifiedAt >
translation(vi).translatedAt`. Thêm một cột cờ trong CSDL là tạo ra thứ có thể
sai — và nó sẽ sai, vào đúng lúc ai đó cập nhật bản `da` bằng một câu SQL.

#### 4.1.1. Thứ tự ưu tiên

Sắp theo tác động tới doanh thu, không sắp theo ngày:

1. Sản phẩm **đã gán thị trường** và đang bán — chưa dịch là mất doanh thu ngay
2. Sản phẩm đã xuất bản bản `da`, chưa gán thị trường
3. Điểm đến — ảnh hưởng gián tiếp qua listing
4. Bài viết, sự kiện

Sắp theo ngày sửa là để một dòng chữ trong bài blog chen lên trước một tour đang
bán.

### 4.2. Dịch song song (M11)

Bản `da` **bên trái, chỉ đọc**; ô nhập `vi` bên phải; **từng trường một**, cạnh
nhau theo hàng — không phải hai khối văn bản lớn.

| Yêu cầu | Vì sao |
|---|---|
| Đối chiếu theo từng trường | Dịch cả khối là bỏ sót trường, và không ai thấy |
| Hiện rõ trường nào bắt buộc | `slug`, `title`, `shortDescription`, `heroImageAlt` không được rỗng |
| Cảnh báo slug có dấu ngay khi gõ | CSDL sẽ từ chối; báo sớm hơn thì đỡ mất công |
| Lưu nháp được, không bắt xong mới lưu | Một tour có nhiều trường; mất việc giữa chừng là mất buổi làm |
| Nút "đánh dấu đã dịch" tách khỏi nút "xuất bản" | Dịch xong khác với được lên website |

**Không có nút dịch máy cho nội dung bán hàng** (`02` mục 8). Ô nhập không được
điền sẵn bằng bản `da`: người dịch sẽ sửa qua loa và để sót nguyên câu tiếng Đan.

`heroImageAlt` là **nội dung phải dịch**, không phải siêu dữ liệu kỹ thuật — nó
là thứ người khiếm thị đọc thay cho ảnh.

### 4.3. Bảng độ phủ (M12)

Mỗi loại thực thể đã dịch bao nhiêu phần trăm. Hai con số cho mỗi dòng: **đã
dịch** và **còn hạn** (không `OUTDATED`).

Tách hai con số vì chúng nói hai chuyện khác nhau: 100% đã dịch nhưng 60% còn hạn
nghĩa là nội dung đang trôi, và đó là dấu hiệu sớm hơn nhiều so với việc khách
phàn nàn.

---

## 5. Mở bán một sản phẩm — checklist

Trình tự ở `01` mục 4.4. Trang quản trị phải **hiện được đang thiếu bước nào**,
chứ không để nhân viên đoán vì sao tour chưa lên website.

| # | Bước | Ai | Cổng chặn |
|---|---|---|---|
| 1 | Tạo sản phẩm, chọn loại | EDITOR | Đổi loại sau khi tạo: xem mục 7 |
| 2 | Viết bản `da` đủ trường bắt buộc | EDITOR | Thiếu bản dịch nguồn thì CSDL từ chối |
| 3 | Điền phần riêng của loại | EDITOR | Mỗi loại một bộ trường (`04`) |
| 4 | Xuất bản bản `da` | EDITOR | Vào hàng đợi dịch |
| 5 | Dịch và xuất bản bản `vi` | TRANSLATOR | Chưa dịch thì **ẩn hoàn toàn** khỏi locale `vi` |
| 6 | Gán vào thị trường, nhập giá từng thị trường | ADMIN | **Chưa gán thì không bán được, dù đã dịch xong** |
| 7 | Tạo ngày khởi hành | ADMIN | Loại có tồn kho mới cần |

Màn hình M3 nên có một dải trạng thái hiện đúng bảy bước này cho từng thị trường
và từng locale. Câu hỏi *"vì sao tour này chưa hiện trên web"* phải trả lời được
bằng cách nhìn, không bằng cách hỏi lập trình viên.

**Giá của mỗi thị trường là số người nhập.** Không có ô nào tự tính giá thị
trường này từ giá thị trường kia — hai thị trường là hai sản phẩm khác nhau
(`02` mục 3).

---

## 6. Vận hành đơn và báo giá

| Màn hình | Quy tắc |
|---|---|
| M6 danh sách đơn | Mặc định lọc "cần xử lý", không phải "tất cả" |
| M7 chi tiết đơn | Hiện **toàn bộ** `booking_event`: từ trạng thái nào sang trạng thái nào, ai làm, lúc nào |
| Đổi trạng thái | Mọi thao tác đổi trạng thái **ghi một `booking_event`**, kể cả thao tác của nhân viên. Máy trạng thái ở `23` |
| Huỷ chuyến vì thiếu khách | Phải có màn hình riêng (`14` mục 6.6) — không xử lý bằng cách sửa CSDL tay |
| M8 báo giá | Chỉ `PRIVATE_TOUR`. Vòng đời ở `14` mục 7 |

Nhật ký `booking_event` là bảng **chỉ ghi thêm**: không sửa, không xoá, không xoá
mềm (`11` mục 11.2). Giao diện quản trị vì thế không được có nút sửa hay xoá một
dòng nhật ký — không phải vì khó làm, mà vì có nút đó thì nhật ký hết giá trị.

---

## 7. Quy tắc chung của giao diện quản trị

| Quy tắc | Vì sao |
|---|---|
| **Không xoá cứng.** Nút "Xoá" đặt `soft_delete` | Xoá nhầm một tour đang bán là sự cố không hoàn tác được |
| Hành động khó đảo phải xác nhận, và ô xác nhận nói rõ **hậu quả** | "Huỷ chuyến này và 14 đơn của nó" khác hẳn "Bạn có chắc không?" |
| Hiện "sửa lần cuối bởi ai, lúc nào" trên mọi bản ghi | Năm cột kiểm toán đã có sẵn (`11` mục 11) — không hiện ra thì thu thập làm gì |
| Trạng thái bộ lọc trong URL | Nhân viên gửi link màn hình đã lọc cho nhau; đây là thao tác hằng ngày |
| Không đổi loại sản phẩm sau khi tạo | Mỗi loại một bảng con, đổi loại là mất dữ liệu riêng của loại cũ. Cần đổi thì tạo mới |
| Bảng dữ liệu phải phân trang phía máy chủ | Danh sách đơn lớn dần theo thời gian và không bao giờ nhỏ lại |
| Mọi phản hồi `no-store` | Nội dung chưa xuất bản không được nằm trong bất kỳ cache nào |

---

## 8. Ngôn ngữ của chính trang quản trị

Giao diện quản trị dùng **một** ngôn ngữ cho toàn đội, tách hẳn khỏi locale của
dữ liệu. Đội ngũ nói tiếng Việt, nên chuỗi giao diện quản trị viết bằng `vi`.

Không đầu tư song ngữ cho trang quản trị ở v1: người dùng là mười mấy nhân viên
đã biết dùng, còn chi phí giữ hai catalog đồng bộ là có thật. Nếu về sau thuê
biên tập viên người Đan thì thêm — cấu trúc catalog đã sẵn sàng cho việc đó.

**Chuỗi giao diện quản trị nằm trong catalog riêng**, không trộn với catalog của
website khách. Trộn vào là bộ kiểm độ phủ đòi dịch cả những nhãn mà khách không
bao giờ nhìn thấy.

---

## 9. Đăng nhập và an toàn

| | |
|---|---|
| Cơ chế | Phiên đăng nhập, cookie `HttpOnly` + `SameSite=Lax` (`13` mục 10) |
| Giới hạn đăng nhập | 10 lượt / 15 phút mỗi tài khoản — **chưa có gì cài**, `31` mục 6.2 |
| Hết phiên | Đăng xuất sau thời gian không hoạt động; giá trị cụ thể ở `31` |
| Mật khẩu | Băm, không bao giờ ghi log, không gửi qua email |
| Vô hiệu hoá người dùng | `is_active = false`, **không xoá bản ghi** — người đó còn nằm trong `created_by` của hàng trăm bản ghi |

Không dùng token lưu trong `localStorage`: kịch bản tấn công qua chèn mã đọc được
`localStorage`, không đọc được cookie `HttpOnly`. Chi tiết ở `31`.

---

## 10. Cấm

- Xoá cứng bất kỳ bản ghi nghiệp vụ nào từ giao diện
- Nút sửa hoặc xoá một dòng `booking_event`
- Người dịch sửa được bản `da`; người viết sửa được bản `vi`
- Dịch máy tự động cho nội dung bán hàng
- Điền sẵn ô nhập `vi` bằng nội dung bản `da`
- Ô nhập tự tính giá thị trường này từ giá thị trường kia
- Đổi loại sản phẩm sau khi tạo
- Bất kỳ màn hình quản trị nào được cache
- Trộn chuỗi giao diện quản trị vào catalog của website khách

---

## 11. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| ~~`12` chưa định nghĩa bảng `role` và `staff_user_role`~~ | — | **Xong** 01/09/2026: `12` mục 3.1 và migration `V2` |
| **Hàng đợi mục 4.1 chưa phủ được điểm đến và buổi thuyết trình** — bảng dịch của chúng không có `status` lẫn `translated_at`, nên `OUTDATED` không tính ra được | Bậc ưu tiên 3 của mục 4.1.1 | `12` mục 10 |
| Có ghi nhật ký thao tác cho thay đổi **nội dung** không, hay chỉ cho đơn đặt | Truy vết sửa nội dung; năm cột kiểm toán chỉ giữ lần sửa **cuối** | Quyết định kiến trúc → cân nhắc ADR |
| Xác thực hai lớp cho `ADMIN` | Mục 9 | `31` mục 6.3 — **đã có đề xuất**, chờ chốt |
| Thời gian hết phiên | Mục 9 | `31` mục 6.3 — **đã có đề xuất**, chờ chốt |
| Nhập hàng loạt ngày khởi hành theo lịch lặp | M4; nhập tay 30 ngày khởi hành một mùa là việc thật | Nghiệp vụ |
