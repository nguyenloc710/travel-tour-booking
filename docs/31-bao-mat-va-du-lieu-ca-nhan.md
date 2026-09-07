# Bảo mật và dữ liệu cá nhân

```
Trạng thái: Nháp
Cập nhật: 02/09/2026
Nguồn sự thật về: hệ thống giữ dữ liệu cá nhân nào, thời hạn lưu và cách xoá,
                  quyền của chủ thể dữ liệu chạm vào đâu trong hệ thống, cookie
                  và đồng ý, phiên đăng nhập nhân viên, cái gì không được ghi log.
Không nói về: nghĩa vụ ngành du lịch — Rejsegarantifonden, chỉ thị EU về gói du
              lịch, Luật Du lịch 2017 (32), thu tiền và hoàn tiền (30), lược đồ
              (12), ma trận quyền theo vai trò (22 mục 2.1), sao lưu và vận hành
              (35), bí mật triển khai (34).
```

Tài liệu này trả lời: **hệ thống giữ dữ liệu gì của người thật, giữ bao lâu, ai
chạm được, và bỏ đi thế nào.**

> **Cần người có chuyên môn pháp lý duyệt.** `40` mục 4 chỉ bắt `32` phải có chữ
> ký đó, nhưng tài liệu này cũng chạm luật — hai bộ luật, hai thị trường. Bản
> nháp gom việc lại và chỉ ra chỗ phải hỏi; nó **không thay lời tư vấn pháp lý**,
> và mọi ô ghi "cần xác nhận" là ô thật sự chưa có câu trả lời.

---

## 1. Không suy được luật nào áp cho một khách từ `market` hay `locale`

Đây là điều quan trọng nhất của tài liệu này, và nó là hệ quả trực tiếp của quyết
định nền tảng nhất của dự án.

`02` tách `Market` khỏi `Locale` bằng đúng một ví dụ: **một khách Việt sống ở Đan
Mạch mua ở `DK` nhưng đọc `vi`**. Ví dụ đó vốn để giải thích chuyện catalog và
tiền tệ — nhưng nó cũng nói một điều nữa, ít người để ý:

**Cả hai trường đó đều không cho biết người này ở đâu.** `market` là nơi khách
*mua*, `locale` là ngôn ngữ khách *đọc*. Chỗ ở thực tế — thứ quyết định GDPR hay
NĐ 13/2023 áp cho họ — hệ thống **không hỏi và không lưu**.

Hai cách xử lý:

| | |
|---|---|
| Đoán từ `market` | **Sai.** Đúng ví dụ mở đầu của `02` đã là phản ví dụ |
| **Áp mức bảo vệ cao hơn cho mọi người, không phân biệt** | Chọn cái này |

Áp mức cao hơn cho tất cả **đắt hơn về nghĩa vụ nhưng rẻ hơn về mã**: một đường
xử lý thay vì hai, không có nhánh nào chỉ chạy cho một nhóm khách và vì thế không
bao giờ được thử.

> **Cần xác nhận pháp lý:** "mức cao hơn" cụ thể là gì ở từng quyền. Bản nháp này
> giả định GDPR nghiêm hơn ở phần lớn các trục, nhưng đó là giả định của kỹ thuật,
> không phải kết luận của luật sư.

---

## 2. Hệ thống giữ dữ liệu cá nhân ở đâu

`11` mục 12 đã lập bảng cho phần nghiệp vụ — không chép lại. Ba chỗ **`11` chưa
nhắc**, và cả ba đều chứa dữ liệu người thật:

| Chỗ | Chứa gì | Ghi chú |
|---|---|---|
| `staff_user` | Email, tên hiển thị, băm mật khẩu | Nhân viên cũng là chủ thể dữ liệu |
| `media_asset` + `product_image` | **Ảnh có mặt người** — cột `person_consent` đã có sẵn từ `V2` | ADR-008 mục 3 dựa vào chỗ này để chọn vùng lưu trữ |
| `booking_event`, cột kiểm toán `created_by` / `last_modified_by` | Ai làm gì lúc nào | Nhóm D — **không xoá được**, xem mục 4.3 |

### 2.1. Hai bảng `11` liệt kê mà lược đồ chưa có

`11` mục 12 xếp `lead` và `newsletter_subscription` vào danh sách bảng chứa dữ
liệu cá nhân, nhưng **cả hai chưa tồn tại trong `12`**. Yêu cầu tư vấn và đăng ký
nhận tin đều nằm trong phạm vi v1 (`01` mục 3.1), nên đây là hai bảng sẽ phải
thêm — và khi thêm thì thời hạn lưu phải quyết cùng lúc, không để lại sau.

---

## 3. Số hộ chiếu

`11` mục 12 đã gọi tên: **nhạy cảm ở cả GDPR lẫn NĐ 13/2023**. Ba quy tắc, và
quy tắc thứ hai là quy tắc dễ vi phạm nhất mà không ai nhận ra:

1. **Chỉ thu khi thật sự cần.** Cột `passport_no` của `booking_passenger` là
   `NULL`-able có chủ ý — tour trong nước không hỏi.
2. **Chỉ ghi vào, không đọc ra.** Không endpoint đọc nào trả `passport_no`, kể cả
   bề mặt quản trị, kể cả cho `ADMIN`. Nhân viên cần đối chiếu hộ chiếu thì làm ở
   quầy với giấy tờ thật, không làm bằng cách nhìn màn hình.
3. **Không bao giờ vào log, vào thông báo lỗi, vào email.** Xem mục 7.

Ràng buộc `ck_bp_passport` đã bắt: có số hộ chiếu thì phải có ngày hết hạn. Điều
đó nghĩa là dữ liệu này **có hạn dùng tự nhiên** — hộ chiếu hết hạn thì số cũ
không còn giá trị gì, và đó là mốc xoá rõ ràng hơn mọi mốc khác trong hệ thống.

**Chưa quyết:** mã hoá ở tầng cột hay không (`12` mục 10).

---

## 4. Thời hạn lưu và cách bỏ đi

### 4.1. Cột `retention_until` chưa tồn tại

`11` mục 13 đã ghi việc này. Không có cột đó thì không có job xoá nào chạy được,
và "thời hạn lưu" chỉ là một câu trong tài liệu.

### 4.2. Ẩn danh khác xoá, và phần lớn trường hợp là ẩn danh

| Cách | Nghĩa là | Dùng khi |
|---|---|---|
| **Xoá** | Dòng biến mất | Dữ liệu không có nghĩa vụ giữ lại |
| **Ẩn danh** | Dòng ở lại, trường nhận diện được thay bằng giá trị vô nghĩa | Có nghĩa vụ kế toán, hoặc số liệu tổng hợp cần dòng đó |

Đơn đặt là ví dụ rõ nhất: nghĩa vụ kế toán bắt giữ chứng từ nhiều năm, nhưng
không bắt giữ **tên và email** của khách suốt thời gian đó. Ẩn danh giữ được con
số, bỏ được con người.

Ẩn danh phải **không đảo ngược được**. Thay email bằng một mã băm mà vẫn tra
ngược ra được thì đó không phải ẩn danh, chỉ là làm khó đọc.

### 4.3. Mâu thuẫn phải giải: quyền được xoá gặp nhật ký không xoá được

`11` mục 11.2 xếp `booking_event` và `slug_history` vào **nhóm D — chỉ ghi thêm**,
với lý do rất mạnh: *"khi khách khiếu nại 'tôi không hề huỷ', đây là chỗ duy nhất
trả lời được"*. Không sửa, không xoá, không xoá mềm.

Nhưng nhật ký đó có `actor_id`, và đơn có tên khách.

Hai nguyên tắc đúng, va vào nhau. Đây **không phải** thứ giải được bằng cách chọn
một bên trong lúc viết mã — và nó cũng không phải thứ hiếm gặp: mọi hệ thống có
nhật ký kiểm toán đều gặp.

> **Cần xác nhận pháp lý.** Hướng thường dùng là giữ nhật ký nhưng ẩn danh phần
> nhận diện trong đó khi chủ thể yêu cầu xoá — nhật ký vẫn đọc được như một chuỗi
> sự kiện, chỉ không còn chỉ vào một người. Nhưng "thường dùng" không phải "đúng
> luật ở đây", và câu này cần người có chuyên môn xác nhận trước khi cài.

---

## 5. Cookie và đồng ý

### 5.1. Trang đang đặt cookie gì

| Cookie | Việc gì | Cần đồng ý không |
|---|---|---|
| Ghi đè thị trường | Khách chọn thị trường khác mặc định (`02` mục 5.3) | **Không** — khách chủ động đặt nó bằng một thao tác của chính mình |
| Phiên đăng nhập quản trị | `HttpOnly` + `SameSite=Lax` (`22` mục 9) | **Không** — không có nó thì không đăng nhập được |
| `XSRF-TOKEN` | Chống giả mạo yêu cầu | **Không** — cùng lý do |
| Đo lường | Chưa gắn gì | **Có** — và phải là đồng ý **trước**, không phải "tiếp tục dùng là đồng ý" |

Ba dòng đầu là cookie **bắt buộc kỹ thuật**. Dòng cuối chưa tồn tại, nên hôm nay
website **chưa cần bảng đồng ý** — và đó là trạng thái nên giữ càng lâu càng tốt.

### 5.2. Ngày gắn công cụ đo lường thì phải gắn cả bảng đồng ý cùng lúc

Không phải gắn sau. Gắn công cụ đo trước rồi bổ sung bảng đồng ý sau là thu thập
dữ liệu không có cơ sở, trong đúng khoảng thời gian đó.

> `20` mục 9 trỏ việc "đo lường và bảng đồng ý cookie" sang `32`. Trỏ nhầm: `32`
> lo nghĩa vụ ngành du lịch, còn đồng ý cookie là chuyện dữ liệu cá nhân — `00`
> giao cho tài liệu này. Đã sửa `20`.

---

## 6. Phiên đăng nhập nhân viên

`22` mục 11 hỏi hai câu và ghi "xem `31`". Trả lời ở đây.

### 6.1. Cái đã có

| | Trạng thái |
|---|---|
| Cookie `HttpOnly` + `SameSite=Lax`, không token trong `localStorage` | ✔ chạy thật |
| Đổi id phiên ngay sau khi đăng nhập — chống cố định phiên | ✔ chạy thật |
| Một câu trả lời cho cả sai email lẫn sai mật khẩu | ✔ — phân biệt hai cái là cho phép dò xem địa chỉ nào có trong hệ thống |
| Vô hiệu hoá người dùng bằng `is_active`, không xoá bản ghi | ✔ chạy thật |
| Mật khẩu băm bcrypt | ✔ chạy thật |

### 6.2. Cái thiếu, và cái thứ hai là thiếu thật

| Việc | Trạng thái |
|---|---|
| Cookie phiên thiếu cờ `Secure` | Đã nằm trong danh sách chặn trước lần triển khai `prod` đầu tiên — `34` mục 5.2 |
| **Giới hạn 10 lượt đăng nhập / 15 phút mỗi tài khoản** | `22` mục 9 nêu như một quy tắc đang có, nhưng **chưa có gì cài nó**. Hôm nay đăng nhập sai bao nhiêu lần cũng được |
| ~~Bộ lọc CSRF miễn nhầm cho `/api/v1/admin/products/**`~~ | **Đã vá** 02/09/2026 — xem mục 6.4 |

Dòng thứ hai là loại lệch nguy hiểm hơn một việc chưa làm: tài liệu mô tả nó ở
thì hiện tại, nên người đọc tin rằng nó đã có.

### 6.4. Một lỗ hổng CSRF đã có thật, và vì sao nó sống được lâu

Danh sách miễn CSRF của bề mặt công khai viết bằng ký tự đại diện một đoạn:

```
"/api/v1/*/products/**"
```

Ý định là `dk` và `vn`. Nhưng `*` khớp **mọi** đoạn đường dẫn, kể cả `admin` — nên
dòng đó miễn CSRF luôn cho `/api/v1/admin/products/**`: tạo sản phẩm, sửa, xoá,
gán thị trường, lưu bản dịch, lưu bậc giá. Sáu đường ghi, tất cả xác thực bằng
cookie, tất cả không đòi thẻ. Đúng điều kiện để một trang khác lừa trình duyệt
của nhân viên gửi yêu cầu thay họ.

`PATCH /api/v1/admin/departures/{id}` thì **có** bị chặn — nó không nằm dưới
`/products/`. Chính sự khác nhau đó là thứ làm lỗi khó thấy: thử một endpoint bất
kỳ thì thấy CSRF "đang chạy".

**Vá** bằng cách ràng buộc đoạn thị trường đúng hai ký tự —
`/api/v1/{market:[a-z]{2}}/products/**` — vì `market.code` là `VARCHAR(2)` còn
`admin` dài năm ký tự.

**Vì sao nó sống được lâu:** không bài test nào gọi một đường ghi quản trị mà
**cố tình bỏ** thẻ CSRF. Mọi test đều gửi thẻ, nên chúng chứng minh "gửi thẻ thì
được", không chứng minh "không gửi thì bị chặn". Nay có hai bài phủ cả hai chiều.

### 6.3. Hai câu `22` hỏi

| Câu | Đề xuất | Ai chốt |
|---|---|---|
| Thời gian hết phiên vì không hoạt động | **8 giờ** — đủ một ca làm việc, nên nhân viên không bị đá ra giữa lúc nhập một tour; và hết hạn sau khi họ về | Kiến trúc sư |
| Xác thực hai lớp cho `ADMIN` | **Có, nhưng không ở v1.** `ADMIN` là vai trò gán được thị trường và đổi được giá — mất tài khoản đó là mất quyền kiểm soát doanh thu | Chủ sản phẩm quyết thời điểm |

Cả hai là **đề xuất**, không phải quyết định đã chốt. Con số 8 giờ đến từ nhịp làm
việc chứ không từ một khảo sát; ai có căn cứ tốt hơn thì đổi.

---

## 7. Cái không bao giờ được ghi log

| Không bao giờ | Vì sao |
|---|---|
| Mật khẩu, kể cả sai | Người ta gõ nhầm mật khẩu của hệ thống khác vào đây |
| Băm mật khẩu | Ghi ra là mở đường dò offline |
| **Số hộ chiếu** | Mục 3 |
| Số thẻ, mã bảo mật thẻ | Hệ thống này không bao giờ chạm tới — `30` mục 7 |
| Cookie phiên, thẻ CSRF | Ghi ra là ghi ra chính chìa khoá |
| Toàn bộ thân yêu cầu của endpoint đặt tour | Nó chứa tên và ngày sinh hành khách |

`application.yml` hiện để `logging.level.vn.travel.booking: DEBUG`. `34` mục 5.2
đã bắt hạ xuống `INFO` trước lần triển khai `prod` đầu tiên — và mục này là lý do
thứ hai để làm việc đó.

Email và điện thoại **được** ghi ở mức `INFO` khi cần dò lỗi một đơn cụ thể, vì
không dò được đơn thì không hỗ trợ được khách. Nhưng chúng cũng là dữ liệu cá
nhân, nên thời hạn giữ log là một câu hỏi thật — `35`.

---

## 8. Cấm

- Suy chỗ ở của khách từ `market` hoặc `locale` để chọn mức bảo vệ
- Trả `passport_no` qua bất kỳ endpoint đọc nào
- Ghi bất cứ thứ gì ở mục 7 vào log
- Gắn công cụ đo lường trước khi có bảng đồng ý
- "Tiếp tục dùng trang là đồng ý" — đồng ý phải là một hành động
- Xoá cứng một dòng `booking_event` hoặc `slug_history` để đáp ứng yêu cầu xoá.
  Cách xử lý ở mục 4.3, và nó cần xác nhận pháp lý trước
- Ẩn danh bằng một phép biến đổi tra ngược lại được
- Gửi dữ liệu cá nhân sang bất kỳ dịch vụ bên thứ ba nào chưa có trong tài liệu này

---

## 9. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| **Thời hạn lưu từng loại dữ liệu**, và cột `retention_until` | Mục 4, job xoá | `11` mục 13 — cần người có chuyên môn pháp lý |
| **Cách đáp ứng yêu cầu xoá khi dữ liệu nằm trong nhật ký chỉ ghi thêm** | Mục 4.3 | Cần xác nhận pháp lý trước khi cài |
| Mã hoá cột `passport_no` | Mục 3 | `12` mục 10 |
| Thời gian hết phiên, và 2FA cho `ADMIN` | Mục 6.3 | Đề xuất sẵn, chờ chốt |
| Giới hạn lượt đăng nhập — cài ở đâu, đếm theo gì | Mục 6.2 | Chưa có gì cài |
| Hai bảng `lead` và `newsletter_subscription` | Mục 2.1 | `12` |
| Thời hạn giữ log ứng dụng | Mục 7 | `35` |
| Chỗ lưu ảnh có mặt người, và vùng đặt dữ liệu | Mục 2 | **ADR-008** đang chờ chính tài liệu này |
