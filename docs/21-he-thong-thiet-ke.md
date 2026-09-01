# Hệ thống thiết kế

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
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

| Token | Giá trị | Dùng cho |
|---|---|---|
| `--mau-chu` | `#1a1a1a` | Chữ thân bài |
| `--mau-nen` | `#ffffff` | Nền trang |
| `--mau-vien` | `#d4d4d4` | Đường kẻ, viền ô nhập |
| `--mau-nhan-manh` | `#1f3a66` | Liên kết, nút chính |
| `--mau-canh-bao-nen` | `#fbf3e4` | Nền băng thông báo |
| `--mau-canh-bao-vien` | `#b8860b` | Viền băng thông báo |

Ba quy tắc:

1. **Tương phản đạt WCAG AA**: 4.5:1 cho chữ thường, 3:1 cho chữ lớn và cho viền
   của thành phần tương tác. `#1f3a66` trên nền trắng đạt 10.4:1 — chọn đậm hơn
   mức tối thiểu là cố ý, vì màn hình của khách thường cũ và đặt ở phòng sáng.
2. **Màu không bao giờ là phương tiện duy nhất truyền đạt thông tin.** Ngày hết
   chỗ phải có chữ "hết chỗ", không chỉ đổi sang màu xám.
3. **Không thêm màu ngoài bảng này vào component.** Cần một sắc thái mới thì
   thêm token, để đổi thương hiệu là đổi sáu dòng chứ không phải đi tìm khắp nơi.

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

Bộ font hiện tại là ngăn xếp font hệ thống có `Noto Sans` làm lớp đỡ — đủ dấu cả
hai ngôn ngữ trên mọi nền tảng phổ biến, và không tốn một lượt tải mạng nào.
Chọn font thương hiệu là việc còn để ngỏ ở mục 9; ai chọn cũng phải qua hai dòng
thử ở trên.

### 3.2. Thang chữ

| Vai trò | Cỡ | Ghi chú |
|---|---|---|
| Thân bài | `1rem` = 16px | **Sàn tuyệt đối**, không có ngoại lệ |
| Chú thích, disclaimer | `0.875rem` | Chỉ cho chữ phụ, không cho nội dung khách cần đọc |
| Tiêu đề thẻ | `1.25rem` | |
| Tiêu đề mục | `1.5rem` | |
| Tiêu đề trang | `2rem` | Một `h1` mỗi trang |

Dòng giãn 1.6 cho thân bài. Bề rộng cột chữ tối đa khoảng **46rem** — dài hơn
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

### 5.1. Định dạng theo locale

| | `da` / DKK | `vi` / VND |
|---|---|---|
| Giá | `24.990,00 kr.` | `18.900.000 ₫` |
| Chữ số thập phân | 2 | 0 |
| Ngày | `14. marts 2027` | `14/03/2027` |

Số chữ số thập phân **không hardcode**: nó suy ra từ mã tiền tệ. `amount` từ API
là **chuỗi** và phải đi thẳng vào hàm định dạng — không `parseFloat`, không phép
tính nào ở frontend.

Ngày luôn đọc ở múi giờ UTC. Ngày khởi hành là một ngày trên tờ lịch, không phải
một thời điểm: để trình duyệt áp múi giờ địa phương vào thì khách ở Copenhagen
và khách ở Hà Nội thấy hai ngày khác nhau cho cùng một chuyến.

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
