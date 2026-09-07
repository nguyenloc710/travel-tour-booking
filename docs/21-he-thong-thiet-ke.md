# Hệ thống thiết kế

```
Trạng thái: Nháp
Cập nhật: 05/09/2026
Nguồn sự thật về: token màu và chữ, thang khoảng cách, điểm ngắt, danh mục
                  component dùng chung, tiêu chuẩn tiếp cận, quy tắc định dạng
                  số và tiền ở giao diện.
Không nói về: route và trạng thái màn hình (20), khối nội dung từng loại sản
              phẩm (05), giọng văn và quy trình chọn ảnh (24), chính sách ngôn
              ngữ (02).
```

Tài liệu này trả lời: **trang trông thế nào, và vì sao lại phải như vậy.**

Điểm khác biệt lớn nhất so với một design system thông thường: nhóm khách không
phải "người dùng web nói chung". Mọi con số ở đây rơi ra từ đúng một ràng buộc ở
mục 1 — bỏ ràng buộc đó thì bộ token này không còn lý do tồn tại.

---

## 1. Khách là ai, và điều đó ràng buộc cái gì

Khách chính: **người Đan Mạch lớn tuổi**, đi tour trọn gói, quyết định mua sau
khi gọi điện. Nhóm thứ hai: người Việt sống ở Đan Mạch và khách trong nước.

| Đặc điểm | Ràng buộc thiết kế |
|---|---|
| Thị lực giảm dần theo tuổi | Cỡ chữ thân bài **tối thiểu 16px**, dòng giãn **1.6** |
| Vận động tinh kém chính xác hơn | Vùng chạm **tối thiểu 44×44px** |
| Nhiều người dùng máy tính bảng, chuột rời | **Không dùng hover** làm điều kiện duy nhất để lộ thông tin |
| Ít quen với ký hiệu quy ước của web | Nhãn bằng **chữ**, không bằng biểu tượng đứng một mình |
| Gọi điện nhiều hơn điền form | Số điện thoại và giờ mở cửa **thường trực trên header** |
| Ngại nội dung tự chuyển động | Không tự chạy, không tự đổi slide |

Đây không phải danh sách mong muốn. Vi phạm một dòng là mất một phần khách hàng
thật, và mất im lặng — họ không báo lỗi, họ gọi cho đối thủ.

---

## 2. Màu

Bảng màu hiện tại là **bảng tạm**, đủ để dựng giao diện và đạt chuẩn tương phản.
Bảng thương hiệu thật chưa có — xem mục 9.

| Token | Giá trị | Dùng cho | Tương phản trên nền |
|---|---|---|---|
| `--mau-chu` | `#1c1a17` | Chữ thân bài | 15.9:1 |
| `--mau-nen` | `#fbf9f6` | Nền trang | — |
| `--mau-nen-diu` | `#f2ece3` | Dải nền của mục phụ | — |
| `--mau-vien` | `#e0d8cc` | Đường kẻ, viền ô nhập | — |
| `--mau-chu-phu` | `#5c564d` | Chữ phụ, chú thích | 6.9:1 |
| `--mau-nhan-manh` | `#0f4c46` | Liên kết, dải lời hứa, chân trang | 9.4:1 |
| `--mau-keu-goi` | `#a8451f` | **Chỉ** nút chính và gạch chân mục đang xem | 5.7:1 |
| `--mau-tren-toi` | `#f4efe7` | Chữ đặt trên hai màu đậm ở trên | — |
| `--mau-canh-bao-nen` | `#fbf3e4` | Nền băng thông báo | — |
| `--mau-canh-bao-vien` | `#b8860b` | Viền băng thông báo, sao đánh giá | — |

Nền **không phải màu trắng tinh**, và chữ không phải đen tuyền: cả hai ngả sang
ấm. Trắng `#ffffff` với đen `#000000` là cặp mặc định của trình duyệt, nên một
trang dùng đúng cặp ấy trông như chưa ai chọn màu cho nó.

**Hai màu đậm làm hai việc khác nhau, và đó là chủ ý.** Trước đây chỉ có một màu
nhấn dùng cho cả liên kết lẫn nút, nên nút "Đặt tour" trông hệt một liên kết
bình thường — thứ quan trọng nhất trên trang không nổi hơn thứ ít quan trọng
nhất. Nay `--mau-keu-goi` chỉ xuất hiện ở nút chính và ở gạch chân của mục đang
xem; thấy nó ở chỗ khác là dùng sai.

Ba quy tắc:

1. **Tương phản đạt WCAG AA**: 4.5:1 cho chữ thường, 3:1 cho chữ lớn và cho viền
   của thành phần tương tác. Cột cuối bảng trên đo trên `--mau-nen`; chữ
   `--mau-tren-toi` trên `--mau-keu-goi` đạt 5.9:1. Chọn đậm hơn mức tối thiểu
   là cố ý, vì màn hình của khách thường cũ và đặt ở phòng sáng.
2. **Màu không bao giờ là phương tiện duy nhất truyền đạt thông tin.** Ngày hết
   chỗ phải có chữ "hết chỗ", không chỉ đổi sang màu xám.
3. **Không thêm màu ngoài bảng này vào component.** Cần một sắc thái mới thì
   thêm token, để đổi thương hiệu là đổi mười dòng chứ không phải đi tìm khắp nơi.

---

## 3. Chữ

### 3.1. Font phải phục vụ cả hai ngôn ngữ

**Ràng buộc cứng: mọi font dùng cho tiêu đề và thân bài phải dựng sẵn cả `æ ø å`
lẫn dấu tiếng Việt.** Thử bằng đúng hai dòng này trước khi chốt, và thử lại mỗi
lần đổi font:

```
Strand og øer på tværs
Điểm cuối · Hội An · Vịnh Hạ Long
```

Bản demo đã dính đúng bẫy này một lần: file SVG bản đồ dùng Georgia, chữ "Điểm
cuối" hiện thành "Điểm cuố i" vì font thiếu tổ hợp dấu và trình duyệt phải ghép
từ font thay thế.

**Ràng buộc áp dụng cho cả file SVG và ảnh có chữ**, không chỉ cho CSS. Đó là
chỗ bẫy này ẩn được lâu nhất: trang thì đúng, ảnh thì sai, và không ai kiểm ảnh.

**Và nó không chỉ là chuyện chữ cái.** Đợt 05/09 vấp lại đúng bẫy này ở một ký
tự tiền tệ: Playfair Display có đủ `ố`, `ø`, `æ`, nhưng **không có `₫`**, nên giá
ở thị trường VN hiện `690.000 ₫` với ký tự cuối nhảy sang một kiểu chữ khác. Vì
thế `.price-from__amount` dùng chữ không chân, dù mọi con số lớn khác trên trang
đều có chân.

Cách thử không cần mở mắt nhìn — đo bề rộng ký tự bằng canvas ở hai font, font
cần thử và font thay thế; **trùng nhau nghĩa là font không có ký tự đó**:

```js
const c = document.createElement('canvas').getContext('2d');
const rong = (f, k) => { c.font = '40px ' + f; return c.measureText(k).width; };
rong('"Playfair Display"', '₫') === rong('serif', '₫')   // true → thiếu
```

Danh sách ký tự phải thử, tối thiểu: `æ ø å Æ Ø Å` · `ố ạ ề ữ ơ ư đ Đ` · `₫ kr.`

**Hai họ chữ**, gán ở `apps/site/src/app/[locale]/layout.tsx` bằng `next/font`:

| Vai trò | Font | Vì sao |
|---|---|---|
| Tiêu đề | Playfair Display | Chữ có chân, chỉ dùng từ cỡ tiêu đề thẻ trở lên |
| Thân bài | Be Vietnam Pro | Dựng riêng cho tiếng Việt nên dấu không cụt ở cỡ nhỏ; cũng đủ `æ ø å` |

Tương phản giữa **một họ có chân và một họ không chân** là thứ mắt đọc ra trước
cả khi đọc chữ, và là khác biệt lớn nhất giữa trang này với một trang chỉ dùng
font hệ thống. Chữ có chân **không** được dùng cho thân bài: nó khó đọc hơn trên
màn hình cũ, và đó đúng là màn hình của khách (mục 1).

Cả hai khai `subsets: ['latin', 'latin-ext', 'vietnamese']`. Thiếu một trong ba
thì trình duyệt lấy font thay thế cho đúng những ký tự ấy, và trang hiện hai
kiểu chữ lẫn lộn ở đúng những từ quan trọng nhất — chính là bẫy ở trên, chỉ
khác chỗ xảy ra.

Hai hệ quả phải biết:

- **CI cần mạng khi build.** `next/font` tải chữ về lúc dựng.
- **Không có lượt gọi nào sang Google lúc khách xem trang.** `next/font` tự phục
  vụ tệp chữ từ máy chủ của mình. Nhúng thẳng `fonts.googleapis.com` là gửi địa
  chỉ IP của khách sang bên thứ ba — việc đã bị phạt ở châu Âu, và khách của
  trang này ở Đan Mạch (`31`).

Đây vẫn chưa phải font thương hiệu; chọn font thương hiệu còn để ngỏ ở mục 9, và
ai chọn cũng phải qua hai dòng thử ở trên.

### 3.2. Thang chữ

| Vai trò | Cỡ | Ghi chú |
|---|---|---|
| Thân bài | `1.0625rem` = 17px | **16px là sàn tuyệt đối**; đặt trên sàn một nấc vì 17px đọc dễ hơn đúng ở nhóm khách này |
| Chú thích, disclaimer | `0.9375rem` | Chỉ cho chữ phụ, không cho nội dung khách cần đọc |
| Nhãn nhỏ in hoa | `0.8125rem` | Giãn chữ `0.12em`. **Tối đa ba từ** — chữ in hoa đọc chậm hơn chữ thường |
| Tiêu đề thẻ | `1.375rem` | |
| Tiêu đề mục | `clamp(1.75rem, 3.4vw, 2.5rem)` | |
| Tiêu đề trang | `clamp(2.25rem, 5.5vw, 3.75rem)` | Một `h1` mỗi trang |

Hai cỡ lớn nhất **co giãn theo bề rộng**: một con số cố định thì hoặc quá nhỏ
trên màn hình rộng, hoặc tràn dòng trên điện thoại.

**Thang phải có khoảng cách lớn giữa hai đầu.** Tiêu đề trang gấp hơn ba lần
thân bài, và giữa chúng gần như không có cỡ trung gian nào. Một thang chữ phẳng
— mọi thứ trong khoảng 16px tới 24px — là dấu hiệu rõ nhất của một trang không
ai thiết kế, kể cả khi từng con số đều hợp lệ.

Dòng giãn 1.65 cho thân bài, 1.08–1.3 cho tiêu đề: chữ càng lớn thì dòng giãn
càng phải nhỏ, nếu không các dòng của một tiêu đề trông như rời nhau. Bề rộng cột chữ tối đa khoảng **46rem** — dài hơn
thì mắt lạc dòng khi xuống dòng, và đó là vấn đề nặng hơn với người lớn tuổi.

Cấp tiêu đề đi **liên tục**, không nhảy cấp để lấy cỡ chữ. Cỡ chữ là việc của
CSS; cấp tiêu đề là cấu trúc mà trình đọc màn hình dùng để điều hướng.

---

## 4. Khoảng cách và điểm ngắt

Thang khoảng cách dựa trên `0.25rem`: `0.25` · `0.5` · `0.75` · `1` · `1.5` ·
`2` · `3rem`. Không dùng giá trị ngoài thang.

| Điểm ngắt | Từ | Bố cục |
|---|---|---|
| Hẹp | 0 | Một cột, bộ lọc xếp dọc |
| Vừa | 48rem | Lưới thẻ tự xuống dòng, tối thiểu 18rem mỗi thẻ |
| Rộng | 72rem | Nội dung giới hạn bề rộng, không tràn hết màn hình |

Lưới thẻ dùng cơ chế tự xuống dòng theo bề rộng tối thiểu, **không đếm số cột
theo điểm ngắt**. Đếm cột là cách chắc chắn để có một cấu hình màn hình nào đó
hiện ra một thẻ đơn độc ở dòng cuối.

---

## 5. Component dùng chung

Nằm ở `web/packages/ui`. Cái gì có mặt ở đây thì **không được viết lại trong
ứng dụng**.

| Component | Bắt buộc dùng khi | Vì sao là component chứ không phải quy ước |
|---|---|---|
| `PriceFrom` | **Mọi chỗ hiện giá** | Chữ "từ" và disclaimer là yêu cầu pháp lý; đóng vào component thì không ai hiện được số trần |
| Hàm định dạng tiền | Mọi chỗ hiện tiền | Định dạng rải rác là cách chắc chắn để hai màn hình hiện hai kiểu |
| Hàm định dạng ngày | Mọi chỗ hiện ngày | Như trên |
| Hàm định dạng số | Mọi chỗ hiện số lượng | Như trên |
| Thẻ sản phẩm | Listing, danh sách liên quan | Giữ thứ tự thông tin giống nhau ở mọi màn hình |
| Ba trạng thái màn hình | Mọi khối có dữ liệu | Quy tắc ở `20` mục 4 |
| Băng thị trường | Khi market khác mặc định của locale | `02` mục 5.3 |

### 5.0. Bốn khuôn hình lặp lại ở site khách

Không nằm ở `packages/ui` vì chúng là **bố cục**, không phải hàm — nhưng cả bốn
lặp lại ở nhiều màn hình, nên chúng cũng là hợp đồng chứ không phải tuỳ hứng.

**a. Cả thẻ bấm được, nhưng chỉ MỘT liên kết.** Thẻ tour và ô hình thức đi đều
bấm được toàn bộ diện tích. Cách làm là đặt liên kết ở tiêu đề rồi cho `::after`
của nó trải kín thẻ. Bọc cả thẻ trong `<a>` là HTML không hợp lệ khi bên trong
còn liên kết khác; đặt hai `<a>` (một trên ảnh, một trên tiêu đề) thì trình đọc
màn hình đọc hai lần cùng một đích.

**b. Chữ đè lên ảnh luôn có lớp phủ tối, khai ở CSS.** Ảnh mẫu hiện có sẵn vùng
tối ở đáy, nhưng ảnh thật do biên tập viên tải lên thì không. Lớp phủ nằm ở CSS
là thứ duy nhất còn lại khi ảnh đổi — và tương phản chữ trắng trên ảnh là chỗ
không công cụ đo tự động nào bắt được.

**c. Nhãn đè lên ảnh có nền ĐỤC.** Nền trong suốt đọc được trên tấm ảnh này và
biến mất trên tấm sau. Không ai thử lại khi thay ảnh.

**d. Sao đánh giá đi kèm CHỮ, luôn luôn.** Dãy sao mang `aria-hidden`, và con số
"4,6 af 5 · 87 anmeldelser" mới là thứ trình đọc màn hình đọc. Bốn sao rưỡi nhìn
bằng mắt là bốn ô vàng rưỡi — mục 2 quy tắc 2 cấm để màu và hình làm phương tiện
duy nhất, và đây là chỗ vi phạm dễ nhất.

Cùng lý do ấy: **viên trạng thái ngày khởi hành** đổi màu theo trạng thái nhưng
chữ trong viên đã nói đủ nghĩa. Gỡ hết màu đi thì bảng vẫn dùng được.

### 5.1. Định dạng theo locale

| | `da` / DKK | `vi` / VND |
|---|---|---|
| Giá | `24.990,00 kr.` | `18.900.000 ₫` |
| Chữ số thập phân | 2 | 0 |
| Ngày | `14. marts 2027` | `14/03/2027` |

Số chữ số thập phân **không hardcode**: nó suy ra từ mã tiền tệ. `amount` từ API
là **chuỗi** và phải đi thẳng vào hàm định dạng — không `parseFloat`, không phép
tính nào ở frontend.

Ngày khởi hành là một ngày **trên tờ lịch**, không phải một thời điểm: khách ở
Copenhagen và khách ở Hà Nội phải thấy cùng một ngày cho cùng một chuyến.

Cách đạt được điều đó **không phải** là ép định dạng về UTC. Client sinh từ
`openapi.yaml` dựng trường `format: date` thành **nửa đêm giờ địa phương** — xem
`parseDate` trong runtime của nó — và mốc đó đã biểu diễn đúng ngày trên tờ lịch
ở mọi múi giờ rồi. Định dạng lại bằng `timeZone: 'UTC'` là đổi hệ quy chiếu giữa
chừng: ở phía đông UTC, nửa đêm địa phương rơi vào **hôm trước** theo UTC.

Đây là lỗi đã xảy ra thật, không phải giả định: buổi thuyết trình ngày 20/03
hiện thành 19/03 trên máy ở Việt Nam. Nó nằm cùng lúc ở `formatDate` của
`packages/ui` và ở hai hàm `ngay()` của trang quản trị, nên danh sách đơn cũng
lệch một ngày so với vé. Hai test ở `packages/ui` nay canh cho nó không quay lại.

---

## 6. Tiếp cận

Mức mục tiêu: **WCAG 2.1 AA**.

| Yêu cầu | Cụ thể |
|---|---|
| Bàn phím | Mọi thao tác làm được bằng bàn phím, thứ tự tab theo thứ tự đọc |
| Viền focus | **Luôn nhìn thấy**, không `outline: none` |
| Liên kết bỏ qua | "Đi tới nội dung" là phần tử focus được đầu tiên |
| Nhãn form | Mọi ô nhập có `<label>` gắn với `id`, không chỉ có placeholder |
| Ảnh | Có `alt`; alt là **nội dung phải dịch**, quy tắc viết ở `24` |
| Vùng động | Kết quả lọc và trạng thái phân trang có `aria-live` |
| Ngôn ngữ | Thẻ `html` có `lang` đúng locale đang xem |
| Chuyển động | Tôn trọng thiết lập "giảm chuyển động" của hệ điều hành |

Placeholder không thay được nhãn: nó biến mất ngay khi khách bắt đầu gõ, và
người quay lại kiểm tra form không còn biết ô đó là gì.

---

## 7. Viết chữ trên giao diện

Giọng văn đầy đủ ở `24`. Ở đây chỉ ba quy tắc thuộc về giao diện:

1. **Nhãn nút nói hành động sắp xảy ra**: "Xem kết quả", không phải "OK".
2. **Không viết tắt.** "Tối thiểu 10 khách", không phải "Min. 10 pax".
3. **Số ít và số nhiều là hai khoá dịch riêng.** Tiếng Đan viết *"1 rejse"* chứ
   không *"1 rejser"*; tiếng Việt không phân biệt nên hai khoá trùng nội dung.
   Ghép chuỗi kiểu `count + " " + nhãn` là cách hỏng ở đúng một ngôn ngữ.

Chuỗi giao diện **được phép** fallback về `da` kèm ghi log — thiếu một nhãn nút
không phá vỡ lòng tin của khách. Nội dung bán hàng thì ngược lại: ẩn hoàn toàn
(`02` mục 4). Hai luật ngược nhau này là chỗ dễ cài sai nhất, vì thư viện i18n
nào cũng bật fallback sẵn cho cả hai.

---

## 8. Cấm

- Cỡ chữ thân bài dưới 16px
- Vùng chạm dưới 44×44px
- `outline: none` mà không có chỉ báo focus thay thế
- Hover là cách duy nhất để lộ thông tin
- Màu là cách duy nhất truyền đạt trạng thái
- Biểu tượng đứng một mình làm nút, không có nhãn chữ
- Nội dung tự chuyển động, tự đổi slide
- Chấm tròn làm chỉ báo vị trí trong dải cuộn
- Màu hoặc khoảng cách nằm ngoài thang ở mục 2 và mục 4
- Định dạng tiền, ngày, số ở ngoài `packages/ui`
- Font chưa qua hai dòng thử ở mục 3.1

---

## 9. Chưa chốt

| Việc | Chặn | Ai quyết |
|---|---|---|
| Bảng màu thương hiệu, logo, tên công ty | Bảng ở mục 2 đang là bảng tạm; bản Word gửi khách còn ba chỗ để trống tên công ty | Chủ sản phẩm |
| Font thương hiệu cho tiêu đề | Mục 3.1 — bất kỳ lựa chọn nào cũng phải qua hai dòng thử | Chủ sản phẩm + thiết kế |
| Chế độ nền tối | Chưa quyết có làm ở v1 không; nhóm khách này ít dùng | Chủ sản phẩm |
| Ảnh minh hoạ và phong cách nhiếp ảnh | `24` chưa viết | `24` |
