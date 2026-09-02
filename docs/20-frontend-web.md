# Frontend website khách

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: danh mục route và tham số, cách chọn chế độ render, ba trạng
                  thái màn hình, trạng thái bộ lọc trong URL, SEO kỹ thuật.
Không nói về: bảng dịch đoạn đường dẫn và chính sách hreflang (02 mục 5), khối
              nội dung của từng loại sản phẩm (05), token và component (21),
              endpoint và mã lỗi (13), trang quản trị (22), luồng đặt tour (23).
```

Tài liệu này trả lời: **có những trang nào, mỗi trang render kiểu gì, và khi dữ
liệu chưa có hoặc hỏng thì màn hình trông ra sao.**

Ranh giới hay nhầm nhất: `02` mục 5.1 giữ **bảng dịch đoạn đường dẫn** (`rejser`
↔ `tour`) vì nó thuộc chính sách ngôn ngữ. Ở đây là **danh mục route**: tham số,
chế độ render, mã trạng thái HTTP, và ba trạng thái màn hình.

---

## 1. Ứng dụng và ranh giới

| Ứng dụng | Đường dẫn | Locale trong URL | Tài liệu |
|---|---|---|---|
| Website khách | `web/apps/site` | **Có** — đoạn đầu | File này |
| Trang quản trị | `web/apps/admin` | **Không** | `22` |

Trang quản trị không có locale trong URL vì ở đó locale là **thuộc tính của dữ
liệu đang sửa**, không phải của người đang sửa: một biên tập viên mở bản `da` và
bản `vi` cạnh nhau trên cùng một màn hình.

---

## 2. Danh mục route

Đoạn `{products}`, `{destinations}`… là **khoá** trong bảng ánh xạ ở `02` mục
5.1, không phải chuỗi cố định: `da` ra `rejser`, `vi` ra `tour`.

| # | Route | Tham số | Render | Nguồn dữ liệu |
|---|---|---|---|---|
| R1 | `/{locale}` | – | Động | `/site-info`, `/regions` |
| R2 | `/{locale}/{tourFinder}` | bộ lọc ở truy vấn | Động | `/products` |
| R3 | `/{locale}/{products}` | bộ lọc ở truy vấn | Động | `/products` |
| R4 | `/{locale}/{products}/{slug}` | `slug` | Động | `/products/{slug}` |
| R5 | `/{locale}/{destinations}` | – | Động | `/destinations` |
| R6 | `/{locale}/{destinations}/{slug}` | `slug` | Động | `/destinations/{slug}` |
| R7 | `/{locale}/{booking}/{slug}` | `slug`, bước ở truy vấn | Động, `no-store` | `23` |
| R8 | `/{locale}/{confirmation}/{reference}` | `reference`, `?email=` | Động, `no-store` | `23` |
| R9 | `/{locale}/{blog}` · `/{blog}/{slug}` | `slug`, `?tag=` lặp lại | Động | `/posts` |
| R10 | `/{locale}/{events}` | – | Động | `/lectures` |
| R11 | `/{locale}/{contact}` | – | Tĩnh | Chuỗi giao diện |

**R2 và R3 là hai trang khác nhau, không phải một.** `{tourFinder}` là bộ lọc
phân cấp miền → điểm đến dành cho khách chưa biết mình muốn gì (`01` mục 3.1);
`{products}` là danh sách phẳng có bộ lọc, dành cho khách đã biết. Hai trang gọi
cùng một endpoint nhưng khác nhau ở cách dẫn dắt.

### 2.1. Cây thư mục App Router

```
app/
  [locale]/
    layout.tsx            header, băng thị trường, khai báo ngôn ngữ
    page.tsx              R1
    [section]/
      page.tsx            R2, R3, R5, R9, R10, R11 — phân nhánh theo đoạn
      [slug]/page.tsx     R4, R6, R9 chi tiết
```

Đoạn thứ hai là **tham số động** (`[section]`) chứ không phải thư mục cố định,
vì tên đoạn khác nhau theo locale — không thể có cả `rejser/` lẫn `tour/` trong
cây mà vẫn giữ một bản cài đặt. Trang tự đối chiếu đoạn nhận được với bảng ánh
xạ của locale đang xem.

**Hệ quả bắt buộc: đoạn của locale khác phải trả 404.** `/da/tour` không được mở
ra trang tiếng Đan. Hai URL cùng một nội dung là nội dung trùng lặp với công cụ
tìm kiếm, và là hai đường dẫn để chỉ số SEO chia đôi.

### 2.2. Khi nào trả 404

| Tình huống | Mã |
|---|---|
| `locale` không thuộc `da`, `vi` | 404 |
| Đoạn đường dẫn không thuộc locale đó | 404 |
| Slug không tồn tại | 404 |
| Slug tồn tại nhưng **chưa dịch** sang locale đang xem | **404** |
| Slug tồn tại nhưng **chưa bán** ở thị trường đang xem | **404** |
| API trả 5xx hoặc không gọi được | **Không phải 404** — xem mục 4.3 |

Ba dòng in đậm là cùng một câu trả lời cho khách và ba câu khác nhau trong log.
Backend đã gộp sẵn (`13` mục 5.1); frontend **không** được đi tìm bản `da` để
lấp chỗ trống.

Dòng cuối là chỗ dễ sai nhất: biến một sự cố backend thành trang "không tồn tại"
là tự tay đuổi khách đi, và biến nó thành `410 Gone` thì công cụ tìm kiếm gỡ
trang khỏi chỉ mục ngay.

---

## 3. Chế độ render

### 3.1. Vì sao gần như mọi trang đều động

Market suy từ locale nhưng **ghi đè bằng cookie** (`02` mục 5.3). Cookie làm cho
mọi trang có giá trở thành cá nhân hoá theo người xem: hai khách mở cùng một URL
có thể thấy hai bảng giá khác nhau.

Ba phương án đã cân nhắc:

| Phương án | Bỏ vì |
|---|---|
| Tĩnh hoá toàn trang theo locale | Sai giá cho khách đã đổi thị trường — lỗi nghiêm trọng nhất mà site này có thể mắc |
| Tĩnh hoá phần nội dung, nạp giá phía client | Giá nhảy sau khi trang hiện; nhóm khách lớn tuổi phản ứng tệ với nội dung nhảy loạn (`01` mục 3.3) |
| **Render phía máy chủ, đọc cookie** ✔ | Chọn cái này |

Đánh đổi đã biết: mất CDN cache toàn trang. Chấp nhận ở v1 vì quy mô danh mục là
hàng chục tới hàng trăm sản phẩm, không phải hàng chục nghìn. **Xem lại khi có
câu trả lời cho Q-4** (quy mô dự kiến, `41` mục 4).

### 3.2. Tuổi thọ cache

Trang không tự đặt thời gian sống cho dữ liệu — nó **tôn trọng header của API**
(`13` mục 8). Nhắc lại đúng một điều vì nó là điều dễ chết người nhất: **ngày
khởi hành và số chỗ không bao giờ được cache.** Hiện số chỗ cũ là dẫn khách vào
một giao dịch chắc chắn thất bại ở bước cuối.

Trang tĩnh duy nhất là R11 (liên hệ) vì nó không đọc gì từ API.

---

## 4. Ba trạng thái màn hình

Mọi màn hình có dữ liệu phải có đủ ba. Thiếu một cái là thiếu, không phải là
"chưa làm tới".

### 4.1. Đang tải — skeleton

Khối xám đúng hình dạng nội dung sắp hiện, **không phải vòng xoay**. Vòng xoay
nói "hãy chờ"; skeleton nói "sắp có gì ở đây" — với kết nối chậm, cái thứ hai
giữ khách lại.

Ranh giới tải nằm ở **khối dữ liệu**, không ở cả trang: bộ lọc và tiêu đề hiện
ngay, chỉ danh sách mới chờ. Đổi bộ lọc thì skeleton phải hiện lại — nếu không,
khách nhìn kết quả cũ và tưởng bộ lọc không ăn.

### 4.2. Rỗng — kèm hướng dẫn hành động

Một dòng "không có kết quả" là ngõ cụt. Trạng thái rỗng phải nói được **làm gì
tiếp**: bỏ bớt một bộ lọc, hoặc xem toàn bộ danh sách.

Rỗng vì bộ lọc và rỗng vì **chưa có bản dịch** trông giống hệt nhau với khách, và
đó là chủ ý — nhưng hãy nhớ nguyên nhân thứ hai tồn tại khi đọc log: cùng một
thị trường, hai locale ra hai con số khác nhau là bình thường (`02` mục 4).

### 4.3. Lỗi — có số điện thoại

Hiện câu chung, **không hiện mã lỗi ra khách**, ghi log kèm mã để bổ sung bản
dịch. Kèm số điện thoại: nhóm khách này gọi điện nhiều hơn điền form, và một sự
cố kỹ thuật không nên làm mất một cuộc gọi.

Mã lỗi tra trong message catalog theo khoá `error.<MÃ>`; mã chưa có bản dịch thì
lùi về câu chung. Danh mục mã ở `13` mục 5.1.

---

## 5. Trạng thái bộ lọc nằm trong URL

Không dùng `useState` cho bộ lọc. F5 phải giữ nguyên, nút Back phải hoạt động,
và dán link cho người khác thì họ thấy đúng kết quả đó.

| Tham số | Giá trị | Mặc định |
|---|---|---|
| `region` | slug miền **của locale đang xem** | không lọc |
| `productType` | một giá trị của `ProductType` | không lọc |
| `q` | chuỗi tìm, không phân biệt dấu | không tìm |
| `sort` | `title,asc` · `priceFrom,asc` · … | `title,asc` |
| `page` | số nguyên từ 0 | 0 |
| `tag` | **lặp lại được**, cho R9 | không lọc |

Bốn quy tắc:

1. **Giá trị mặc định không xuất hiện trong URL.** `?sort=title,asc` và không có
   tham số nào phải ra cùng một URL, nếu không mỗi trang có hai địa chỉ.
2. **Giá trị rác bị bỏ qua, không làm hỏng trang.** Người ta sửa tay thanh địa
   chỉ; trang trắng vì `?productType=abc` là phản ứng quá đáng.
3. **Bộ lọc nhiều giá trị dùng tham số lặp lại**, không dùng dấu phẩy (`13` mục
   6). Dấu phẩy vỡ khi giá trị có chứa dấu phẩy.
4. **Đổi bộ lọc thì `page` về 0.** Giữ nguyên số trang cũ là cách chắc chắn để
   khách rơi vào một trang rỗng ngay sau khi lọc.

### 5.1. Bộ lọc phải chạy được khi không có JavaScript

Bộ lọc là một `<form method="get">`. Trình duyệt tự dựng URL có đúng các tham số,
nên trạng thái **luôn** nằm trong URL mà không cần một dòng code đồng bộ nào.

Đây không phải chủ nghĩa khổ hạnh: nhóm khách này dùng máy cũ, trình duyệt cũ, và
mạng chập chờn ở nơi script tải hỏng mà HTML thì vẫn tới. JavaScript được phép
làm trải nghiệm tốt hơn, không được phép là điều kiện để lọc.

### 5.2. Phân trang

Phân trang theo offset (`13` mục 6). Hai nút là thẻ `<a>` thật, giữ nguyên mọi
bộ lọc đang bật.

Trạng thái phân trang viết bằng **chữ đọc được** — *"Đang xem 1–12 trên 47"* —
không dùng dãy chấm tròn. Chữ nói đủ ba điều mà chấm tròn không nói: đang ở đâu,
còn bao nhiêu, tổng là bao nhiêu.

**Mọi con số hiển thị đếm từ dữ liệu API trả về**, trong phạm vi `(market,
locale)` đang xem. Không hardcode, kể cả ở nhãn nút "Xem tất cả N tour".

---

## 6. SEO kỹ thuật

| Việc | Quy tắc |
|---|---|
| `hreflang` | `02` mục 5.2 — trang chưa có bản dịch thì **không** khai báo |
| Canonical | Trỏ về URL **không có tham số bộ lọc** |
| Trang có bộ lọc | `noindex, follow` |
| Sitemap | Một file cho mỗi locale; trang chưa dịch không có mặt |
| Slug cũ | Chuyển hướng **vĩnh viễn** sang slug mới, đọc `slug_history`. Next phát `308` chứ không `301` — cả hai là "chuyển vĩnh viễn" và công cụ tìm kiếm xử lý như nhau; `308` chặt hơn ở chỗ cấm đổi phương thức HTTP. Muốn đúng `301` thì phải chuyển việc này xuống `proxy.ts`, và khi đó **mọi** request trả giá cho một trường hợp hiếm |
| Tiêu đề trang | `{tên trang} · {tên site}`, tên trang lấy từ nội dung đã dịch |
| Mô tả | Lấy `shortDescription` của bản dịch, không tự sinh |

`noindex` cho trang có bộ lọc là cố ý: mỗi tổ hợp bộ lọc là một URL, và để công
cụ tìm kiếm thu thập hết thì ngân sách thu thập tiêu vào các biến thể gần giống
nhau thay vì tiêu vào trang sản phẩm thật.

**Trang chi tiết chưa khai báo được `hreflang`** vì slug phụ thuộc locale và
trang không biết bản dịch bên kia có tồn tại hay không. Khai bừa một URL trả 404
còn tệ hơn không khai. Cách xử lý đúng — API trả kèm slug của các locale đã dịch
— cần đổi hợp đồng, ghi ở mục 9.

---

## 7. Gọi API

| Quy tắc | Vì sao |
|---|---|
| Mọi truy vấn đi qua client sinh từ `openapi.yaml` | Đổi spec mà quên sửa chỗ gọi là lỗi biên dịch, không phải bug lúc chạy |
| Không viết `fetch` tay tới API | Cùng lý do trên |
| Market vào **đường dẫn**, locale vào **header** | Hai thứ độc lập; gộp là nhầm lẫn tốn kém nhất của dự án |
| Gọi từ **máy chủ**, không từ trình duyệt | Cookie thị trường đã có ở máy chủ; gọi từ trình duyệt thêm một vòng mạng và lộ cấu trúc API |
| Định dạng tiền và ngày bằng hàm dùng chung ở `21` | Định dạng rải rác là cách chắc chắn để hai màn hình hiện hai kiểu |

Tiền từ API có `amount` là **chuỗi**. Đừng `parseFloat` rồi tính — số dấu phẩy
động của JavaScript làm hỏng tiền. Chỉ định dạng.

---

## 8. Cấm

- Vòng xoay thay cho skeleton
- Trạng thái bộ lọc trong `useState`
- Lấp nội dung thiếu bản dịch bằng bản `da`
- Hiện mã lỗi ra khách
- Hiện giá không kèm chữ "từ" và disclaimer
- Hardcode số liệu hiển thị
- Dải cuộn ngang tự chạy, hoặc dùng chấm tròn làm chỉ báo vị trí
- Dùng hover làm điều kiện duy nhất để lộ thông tin
- Sửa file trong `packages/api-client`

---

## 9. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| Ảnh lưu ở đâu, có tầng tối ưu ảnh không | Cấu hình `next/image`, đo hiệu năng thật | Q-6 ở `41` mục 4 → ADR-008 |
| Quy mô danh mục dự kiến | Xem lại quyết định "gần như mọi trang đều động" ở mục 3.1 | Q-4 ở `41` mục 4 |
| API trả kèm slug các locale đã dịch, để trang chi tiết khai `hreflang` | Mục 6 | Đổi hợp đồng, `13` |
| Bộ lọc theo chủ đề ở R2, R3 | `12` chưa có bảng chủ đề | `13` mục 12 |
| Đo lường và bảng đồng ý cookie | Trang chưa gắn gì | `31` mục 5 — `32` lo nghĩa vụ ngành du lịch, đồng ý cookie là chuyện dữ liệu cá nhân |
