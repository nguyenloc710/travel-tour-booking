# Đặc tả sản phẩm

```
Trạng thái: Nháp
Cập nhật: 31/08/2026
Nguồn sự thật về: bán gì cho ai, phạm vi v1, danh sách không làm, tiêu chí ra mắt.
Không nói về: thị trường và ngôn ngữ (02), kiến trúc (10),
              quy tắc tính giá (14), màn hình cụ thể (20, 22).
```

---

## 1. Sản phẩm là gì

Website của một công ty lữ hành **chuyên tour Việt Nam**, bán cho hai thị trường:
khách Đan Mạch và khách Việt. Khách xem tour, xem lịch trình từng ngày, chọn ngày
khởi hành, và đặt tour trực tuyến.

Cấp phân loại địa lý là **miền → điểm đến**, không phải châu lục → quốc gia — chỉ
bán một nước, nên "quốc gia" không còn là cấp phân loại có ích.

Gồm ba phần:

| Phần | Ai dùng | Nằm ở |
|---|---|---|
| Website khách | Khách mua tour | `web/` |
| API | Hai phần kia | `api/` |
| Trang quản trị | Nhân viên công ty | `web/`, đường dẫn riêng |

---

## 2. Ai dùng

### 2.1. Khách Đan Mạch — nhóm chính

Trung niên và cao tuổi ở Bắc Âu, đi tour trọn gói có trưởng đoàn nói tiếng Đan.
Đặc điểm ảnh hưởng trực tiếp tới thiết kế, không phải mô tả cho vui:

- Cỡ chữ thân bài tối thiểu 16px, dòng giãn 1.6
- Vùng chạm tối thiểu 44×44px
- Tương phản đạt WCAG AA
- **Không dùng hover làm điều kiện duy nhất để lộ thông tin**
- Ngôn từ rõ ràng, không viết tắt
- Nhiều người gọi điện để chốt thay vì đặt online — số điện thoại và giờ mở cửa
  phải hiện thường trực trên header

### 2.2. Khách Việt

Hai nhóm nhỏ, chưa tách được ở v1:

- Người Việt ở Việt Nam mua tour nội địa
- Người Việt sống ở Đan Mạch — mua ở thị trường `DK` nhưng đọc tiếng Việt. Chính
  nhóm này là lý do `Market` và `Locale` phải tách rời (`02` mục 1)

### 2.3. Nhân viên công ty

| Vai trò | Làm gì |
|---|---|
| Nhân viên tư vấn | Xem yêu cầu tư vấn, xem đơn đặt, trả lời khách |
| Người viết nội dung | Viết và sửa tour, điểm đến, bài viết — bằng tiếng Đan |
| Người dịch | Dịch `da` → `vi`, xử lý hàng đợi dịch |
| Quản trị viên | Quản lý ngày khởi hành, giá, số chỗ, người dùng |

Bốn vai trò, không phải một. Phân quyền chi tiết ở `22`.

---

## 3. Phạm vi v1

### 3.1. Bắt buộc có

**Website khách**

- [ ] Trang chủ, kiêm vai trò trang giới thiệu Việt Nam
- [ ] Tìm tour, bộ lọc phân cấp miền → điểm đến, **trạng thái lọc nằm trong URL**
- [ ] Trang điểm đến
- [ ] **Bốn loại sản phẩm**: tour đoàn, tour cá nhân, tour riêng, du thuyền.
      Mỗi loại có nghiệp vụ, thẻ, bộ lọc và luồng đặt riêng — `04`
- [ ] Trang chi tiết sản phẩm, **số tab khác nhau theo loại** (6–7 tab) — `05`.
      Du thuyền đổi tab Khách sạn thành Tàu và cabin; tour riêng đổi tab Giá và
      ngày thành Bảng giá theo nhóm
- [ ] Đặt tour bốn bước: chọn ngày khởi hành → số khách → tuỳ chọn thêm → thông
      tin và thanh toán
- [ ] **Luồng yêu cầu báo giá** cho tour riêng: khách gửi yêu cầu → nhân viên
      dựng báo giá → khách chấp nhận → đặt cọc. `Quote` là thực thể riêng
- [ ] Trang xác nhận, có mã tra cứu
- [ ] Blog, lọc theo tag, trạng thái lọc nằm trong URL
- [ ] Liên hệ, có form yêu cầu tư vấn
- [ ] Sự kiện thuyết trình, đăng ký giữ chỗ
- [ ] Đăng ký bản tin
- [ ] Bộ chọn ngôn ngữ và bộ chọn thị trường
- [ ] Cả hai locale: `da` và `vi`

**API**

- [ ] Đọc: tour, ngày khởi hành, điểm đến, khách sạn, tham quan, nội dung
- [ ] Tính giá, trả bảng phân rã từng dòng
- [ ] Giữ chỗ có hạn
- [ ] Tạo đơn đặt, xác nhận thanh toán
- [ ] Nhận yêu cầu tư vấn, đăng ký bản tin, đăng ký thuyết trình
- [ ] Đọc và ghi nội dung cho trang quản trị
- [ ] Xác thực cho nhân viên

**Trang quản trị**

- [ ] Đăng nhập, bốn vai trò
- [ ] Quản lý tour, ngày khởi hành, giá theo từng thị trường
- [ ] Ba màn hình dịch thuật: hàng đợi, dịch song song, bảng độ phủ (`02` mục 8)
- [ ] Xem đơn đặt và yêu cầu tư vấn
- [ ] Quản lý nội dung: điểm đến, khách sạn, tham quan, bài viết, sự kiện

### 3.2. Để v2

- **Combo bay + khách sạn** (`COMBO`) — v1.5. Khối lượng lớn hơn vẻ ngoài rất
  nhiều; lý do và đề xuất thu hẹp ở `04` mục 9
- **Tour trong ngày bán độc lập** (`DAY_TOUR`) — v2. Khác `Excursion` đang có ở
  v1, đừng gộp: `04` mục 10
- Cổng khách hàng (khách đăng nhập xem đơn của mình)
- Tiếng Anh
- Tour liên quốc gia (Việt Nam + Campuchia)
- Quản lý chuyến bay như một thực thể riêng
- Bản đồ tương tác
- Trò chuyện trực tiếp
- Ứng dụng di động
- Đánh giá do khách tự gửi
- Danh mục PDF sinh tự động

### 3.3. Không làm, kể cả v2

Ghi rõ để không ai đề xuất lại:

| Không làm | Vì sao |
|---|---|
| Đặt vé máy bay lẻ | Không phải nghiệp vụ của công ty |
| So sánh giá với đối thủ | Không phải nghiệp vụ |
| Mạng xã hội trong sản phẩm | Không phục vụ nhóm khách này |
| Cá nhân hoá bằng học máy | Quy mô dữ liệu không đủ, và nhóm khách lớn tuổi phản ứng tệ với nội dung nhảy loạn |
| Quy đổi tỷ giá tự động | Giá mỗi thị trường là giá nhập tay — `02` mục 3 |

---

## 4. Bốn luồng chính

### 4.1. Khách tìm và đặt tour

```
Trang chủ / Tìm tour
   → lọc theo miền, điểm đến, loại tour, chủ đề, thời lượng, tháng
   → Trang tour, bảy tab
   → Tab "Giá và ngày", chọn một ngày khởi hành
   → Đặt tour: ngày → số khách → tuỳ chọn → thông tin và thanh toán
   → Xác nhận, có mã tra cứu, kèm email
```

Chỗ dễ hỏng nằm ở bước thanh toán: **giữ chỗ có hạn** phải khoá chỗ trước khi
khách rời sang cổng thanh toán, và trả chỗ về kho nếu khách bỏ dở. Chi tiết ở
`14` và `23`.

### 4.2. Khách chưa sẵn sàng đặt

```
Trang tour → form yêu cầu tư vấn → nhân viên tư vấn gọi lại
```

Với nhóm khách chính, đây là luồng **quan trọng ngang luồng đặt online**, không
phải luồng phụ. Form phải có ở trang tour, trang điểm đến, trang liên hệ và trang
chủ.

### 4.3. Người viết và người dịch

```
Viết bản da → duyệt → vào hàng đợi dịch → dịch vi → duyệt → lên website
```

Sửa bản `da` đã xuất bản thì bản `vi` tự thành `OUTDATED` và quay lại hàng đợi.
Chi tiết ở `02` mục 8.

### 4.4. Quản trị viên mở bán một tour

```
Tạo tour (bản da) → viết lịch trình từng ngày → gán khách sạn
   → gán vào thị trường và nhập giá cho từng thị trường
   → tạo ngày khởi hành → xuất bản
```

Tour phải được **gán vào thị trường** mới hiện ra. Đã dịch xong nhưng chưa gán
thì vẫn không bán được — cố ý như vậy.

---

## 5. Quy tắc sản phẩm bắt buộc

Những điều này không được vi phạm ở bất kỳ màn hình nào. Chúng là kết luận đã trả
giá của bản demo, không phải sở thích.

1. **Không hardcode số liệu hiển thị.** "Xem tất cả 92 tour" phải đếm từ dữ liệu,
   và đếm trong phạm vi `(market, locale)` đang xem.
2. **Trạng thái bộ lọc nằm trong URL.** F5 phải giữ nguyên bộ lọc; dán link phải
   ra đúng kết quả đó.
3. **Ba trạng thái cho mọi màn hình có dữ liệu**: đang tải (skeleton, không phải
   vòng xoay), rỗng (kèm hướng dẫn hành động), lỗi.
4. **Giá luôn kèm chữ "từ" và disclaimer.** Yêu cầu pháp lý, không phải lựa chọn
   thiết kế.
5. **Không fallback ngôn ngữ cho nội dung bán hàng.** `02` mục 4.
6. **Ngày hết chỗ hiển thị mờ, không click được, không có nút đặt.**
7. **Lịch trình phải kín ngày.** Số mục lịch trình bằng đúng số ngày của tour,
   không ngoại lệ.
8. **Không tự chạy.** Dải cuộn ngang không auto-play, không dùng chấm tròn — dùng
   chữ đọc được (`Đang xem 1–3 trên 8 chuyến đi`).

---

## 6. Tiêu chí ra mắt

Ra mắt được khi tất cả đúng:

- [ ] Cả hai locale hiển thị đầy đủ, không có chuỗi lọt ngôn ngữ sai
- [ ] Đặt tour thành công đầu-cuối ở cả hai thị trường, tiền vào tài khoản thật
- [ ] Giữ chỗ chịu được hai khách bấm đặt chỗ cuối cùng cùng lúc — có test
- [ ] Kiểm giao diện thật ở 375 / 768 / 1440px
- [ ] Đạt WCAG AA
- [ ] Tài liệu pháp lý (`32`) được người có chuyên môn duyệt
- [ ] Có runbook cho "cổng thanh toán lỗi sau khi đã trừ tiền khách"
- [ ] Sao lưu CSDL đã chạy và **đã thử phục hồi một lần**

---

## 7. Việc còn để ngỏ

| Việc | Chặn cái gì | Ai quyết |
|---|---|---|
| Catalog thị trường `VN` gồm những tour nào | Dữ liệu mồi, `02` mục 13 | Nghiệp vụ |
| Tỷ lệ đặt cọc và phí xử lý ở `VN` | `14`, `23`, `30` | Nghiệp vụ |
| Có nhập nội dung từ hệ thống cũ không | `12` dữ liệu mồi, khối lượng đợt 2 | Nghiệp vụ |
| Ai viết nội dung tiếng Đan | Toàn bộ tiến độ nội dung | Nghiệp vụ |
| Quy mô dự kiến: bao nhiêu tour, bao nhiêu đơn/tháng | `10` chọn hạ tầng | Nghiệp vụ |
