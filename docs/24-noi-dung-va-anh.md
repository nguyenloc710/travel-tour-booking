# Nội dung và ảnh

```
Trạng thái: Nháp
Cập nhật: 05/09/2026
Nguồn sự thật về: giọng văn hai ngôn ngữ, cách viết từng trường nội dung, quy tắc
                  dịch, tiêu chuẩn ảnh, cách viết alt text, bản quyền và giấy phép.
Không nói về: quy trình dịch và trạng thái bản dịch (02 mục 8), màn hình dịch của
              nhân viên (22 mục 4), khối nội dung của trang chi tiết (05), token
              và component (21), lược đồ (12).
```

Tài liệu này trả lời: **viết gì vào từng ô, viết như thế nào, và ảnh nào được
phép dùng.**

Người đọc chính không phải lập trình viên mà là **người viết nội dung và người
dịch**. Vì vậy tài liệu này nói bằng ví dụ nhiều hơn bằng quy tắc trừu tượng.

---

## 1. Hai nhóm khách, hai giọng khác nhau

| | Khách Đan Mạch (`da`) | Khách Việt (`vi`) |
|---|---|---|
| Ai | Người lớn tuổi, đi tour trọn gói, thường đi cùng vợ/chồng hoặc bạn bè | Người Việt ở Đan Mạch, và khách trong nước |
| Điều họ lo | Đi lại có mệt không, ăn uống thế nào, có người nói tiếng Đan không | Lịch trình có hợp lý không, giá gồm những gì |
| Thứ thuyết phục | Sự yên tâm: rõ ràng, cụ thể, có người lo | Sự cụ thể: đi đâu, ở đâu, ăn gì |
| Thứ làm họ bỏ đi | Từ ngữ hoa mỹ, chữ viết tắt, câu dài | Mô tả chung chung không có địa danh |

**Giọng chung: bình tĩnh, cụ thể, không quảng cáo quá lời.** Đây là sản phẩm giá
cao mà khách quyết định sau khi gọi điện — chữ trên web có nhiệm vụ làm họ đủ tin
để nhấc máy, không phải để chốt đơn ngay.

### 1.1. Ba thứ luôn tránh

| Tránh | Vì sao | Thay bằng |
|---|---|---|
| Tính từ cực cấp: "tuyệt vời nhất", "không thể bỏ lỡ" | Không nói thêm thông tin nào, và nhóm khách này đọc nó như quảng cáo | Chi tiết cụ thể: "bốn đêm ở Hội An, khách sạn cách phố cổ 300 mét" |
| Dấu chấm than | Cùng lý do trên | Câu khẳng định bình thường |
| Viết tắt: "min. 10 pax", "kh.sạn 4*" | `21` mục 1 — nhóm khách này không quen ký hiệu quy ước | "Tối thiểu 10 khách", "khách sạn bốn sao" |

### 1.2. Câu và đoạn

- Câu **trung bình dưới 20 từ**. Câu dài là chỗ người lớn tuổi đọc lại từ đầu.
- Đoạn **tối đa 4 câu**, mỗi đoạn một ý.
- **Không dùng chữ in hoa toàn bộ** để nhấn mạnh — trình đọc màn hình đọc từng
  chữ cái, và mắt người khó đọc hơn hẳn.
- Con số viết bằng chữ số: "14 ngày", không "mười bốn ngày".

---

## 2. `da` là ngôn ngữ nguồn

Nội dung viết bằng `da` trước, dịch sang `vi` sau. **Không có chiều ngược lại**
(ADR-004). Đội ngũ nói tiếng Việt nên đây là điều làm ngược trực giác, và là chỗ
quy trình hay bị đi tắt nhất.

Viết bản `vi` trước rồi dịch ngược sang `da` thì bản `da` — bản mà khách chính
đọc — trở thành bản dịch, và văn bản thuyết phục dịch ra hiếm khi còn thuyết
phục. Sản phẩm dành cho thị trường Việt về sau vẫn phải có bản `da` làm nguồn;
nếu thấy điều đó vô lý cho một sản phẩm chỉ bán ở `VN` thì đó là câu hỏi cho
ADR-004, không phải chỗ để lách.

---

## 3. Viết từng trường

Ràng buộc độ dài và số phần tử nằm ở `12` (bảng `product_translation`). Ở đây là
**mỗi trường để làm gì và viết thế nào**.

| Trường | Nhiệm vụ | Hướng dẫn |
|---|---|---|
| `title` | Tên sản phẩm, xuất hiện ở thẻ, trang chi tiết, kết quả tìm kiếm, email | Nói **đi đâu** và nếu được thì **kiểu đi**. Không nhồi từ khoá |
| `slug` | Địa chỉ trang | Mục 4 |
| `shortDescription` | Một câu trên thẻ sản phẩm | Một câu, khoảng 12–18 từ, nói điều **khác biệt** của chuyến này |
| `longDescription` | Mô tả chính, từng đoạn văn | Ít nhất 2 đoạn. Đoạn đầu trả lời "chuyến này dành cho ai" |
| `whyChooseThis` | 3–7 gạch đầu dòng | Mục 3.1 |
| `heroImageAlt` | Chữ thay cho ảnh đầu trang | Mục 6 |

### 3.1. `whyChooseThis` — chỗ dễ viết sai nhất

Ba tới bảy gạch đầu dòng, **mỗi dòng một lý do cụ thể**, không phải khẩu hiệu.

| Sai | Đúng |
|---|---|
| "Trải nghiệm khó quên" | "Trưởng đoàn nói tiếng Đan đi cùng suốt chuyến" |
| "Dịch vụ chất lượng cao" | "Đoàn tối đa 22 khách" |
| "Giá tốt nhất thị trường" | "Đã gồm toàn bộ bữa sáng và 9 bữa tối" |

Phép thử: **nếu một dòng dán sang tour khác vẫn đúng thì dòng đó vô nghĩa** — nó
không nói gì về chuyến này.

Ba dòng đầu quan trọng nhất: chúng xuất hiện trước khi khách phải cuộn.

**Mỗi dòng phải là sự thật kiểm chứng được**, vì đây là những dòng khách trích ra
khi gọi điện khiếu nại. "Đã gồm bữa tối" mà thực tế chỉ gồm 9 trên 13 bữa là một
tranh chấp, không phải một lỗi chính tả.

---

## 4. Slug

Quy tắc kỹ thuật ở `02` mục 5.1: **không dấu ở cả hai ngôn ngữ**. Ở đây là cách
đặt.

- Lấy từ `title`, bỏ dấu, bỏ từ nối: `Vietnam fra nord til syd` →
  `vietnam-fra-nord-til-syd`.
- Tiếng Đan: `æ → ae`, `ø → oe`, `å → aa`. `bekræftelse` → `bekraeftelse`.
- Tiếng Việt bỏ toàn bộ dấu: `Việt Nam từ Bắc vào Nam` → `viet-nam-tu-bac-vao-nam`.
- **Không nhét năm vào slug** (`tour-viet-nam-2027`): sang mùa sau slug thành sai,
  mà đổi slug thì mất liên kết.

**Slug đã xuất bản thì không đổi.** Nó nằm trong liên kết khách đã lưu, trong
email đã gửi, trong kết quả tìm kiếm. Buộc phải đổi thì phải có chuyển hướng 301
từ slug cũ — mà hệ thống hiện chưa có chỗ lưu slug cũ, xem mục 9.

---

## 5. Dịch — không phải viết lại, cũng không phải dịch từng chữ

| Được phép đổi | Không được đổi |
|---|---|
| Cách diễn đạt, thứ tự vế trong câu | Sự thật: số ngày, số bữa ăn, số sao khách sạn |
| Ví dụ so sánh mang tính địa phương | Lời hứa và cam kết |
| Độ dài câu | Giá, và những gì giá đã gồm |
| Cách gọi tên món ăn, địa danh theo thói quen của ngôn ngữ đó | Tên riêng của khách sạn, tàu, hãng bay |

**Bản dịch không được thêm một lời hứa nào mà bản nguồn không có.** Đây là ràng
buộc pháp lý, không phải sở thích biên tập: hai bản là hai lời chào bán của cùng
một công ty, và khách khiếu nại theo bản họ đã đọc.

Ngược lại, **bản dịch không được bỏ bớt điều kiện**. Câu "giá chưa gồm vé tham
quan" mà bản `vi` bỏ đi là một tranh chấp chờ sẵn.

### 5.1. Không dịch máy nội dung bán hàng

`02` mục 8 đã quy định. Lý do nhắc lại ở đây vì nó là quyết định về **nội dung**,
không phải về công cụ: mô tả tour là văn bản thuyết phục có chi tiết địa danh và
món ăn. Dịch máy ra thứ đọc được nhưng không bán được, và cái sai đó không lộ ra
khi rà soát nhanh.

Chuỗi giao diện thì khác: dịch máy rồi người rà lại là chấp nhận được.

### 5.2. Sửa bản nguồn thì bản dịch trôi

Sửa bản `da` đã xuất bản làm bản `vi` thành `OUTDATED` và quay lại hàng đợi
(`02` mục 8). Hệ quả cho người viết: **sửa chính tả và sửa nội dung là hai việc
khác nhau về hậu quả**. Một lần sửa dấu phẩy cũng đẩy bản dịch vào hàng đợi.

Gom các sửa đổi nhỏ lại thành một lần thay vì sửa rải rác mười lần — hàng đợi
dịch là thời gian của người thật.

---

## 6. Alt text

**Alt text là nội dung phải dịch**, nằm trong bảng dịch (`11` mục 6). Cùng một
ảnh dùng cho mọi ngôn ngữ, nhưng chữ mô tả nó thì mỗi ngôn ngữ một bản.

| Quy tắc | Ví dụ |
|---|---|
| Mô tả **cái quan trọng trong ảnh**, không mô tả từng chi tiết | "Ruộng bậc thang ở Sa Pa lúc sáng sớm" |
| Không bắt đầu bằng "Ảnh chụp…" | Trình đọc màn hình đã nói đó là ảnh |
| Dài khoảng 5–15 từ | Dài hơn thì người nghe mất mạch |
| Ảnh **có chữ** thì alt phải chứa chữ đó | Bản đồ, sơ đồ tàu, ảnh có chú thích |
| Ảnh thuần trang trí thì `alt=""` | Nhưng ảnh trong nội dung tour gần như không bao giờ thuần trang trí |

**Ảnh bản đồ cần một bản thay thế bằng chữ, không chỉ một dòng alt.** Lộ trình
"Hà Nội → Hạ Long → Huế → Hội An → TP.HCM" là thông tin thật của sản phẩm; nhét
nó vào một dòng alt 15 từ là mất. Cách đúng: lộ trình cũng có mặt dưới dạng danh
sách chặng trong nội dung.

---

## 7. Ảnh

### 7.1. Tiêu chuẩn kỹ thuật

| | Yêu cầu |
|---|---|
| Ảnh hero | Tỷ lệ 3:2, cạnh dài tối thiểu 1600px |
| Ảnh trong bộ ảnh | Tỷ lệ 3:2, cạnh dài tối thiểu 1200px |
| Ảnh chân dung (tư vấn viên, trưởng đoàn) | Vuông, tối thiểu 600px |
| Định dạng nguồn | JPEG hoặc PNG chất lượng cao; nơi lưu và định dạng phục vụ xem mục 9 |
| Dung lượng sau xử lý | Dưới 250KB cho ảnh hero |
| Tên file | Không dấu, có nội dung: `sapa-ruong-bac-thang-01.jpg`, không `IMG_4821.jpg` |

Ảnh dọc **không** dùng làm hero: nó vỡ bố cục ở mọi bề rộng màn hình.

### 7.1b. Ảnh mẫu cho môi trường dev

`scripts/tai-anh-mau.py` tải 29 tấm về `web/apps/site/public/img/`, và sinh
`NGUON.md` cạnh chúng — sổ ghi **nguồn · giấy phép · phạm vi · hạn dùng** mà mục
8 đòi phải biết cho mỗi ảnh.

Vì sao cần: dữ liệu mồi trỏ tới `/img/…` từ ngày đầu nhưng chưa bao giờ có tệp
thật nào ở đó, nên mọi thẻ đều hiện khung vỡ. Ảnh vỡ còn **che mất lỗi bố cục**:
một thẻ có ảnh cao 320px xếp khác hẳn một thẻ có ảnh cao 0px.

Ba điều đáng ghi lại:

- **Nguồn là Unsplash, giấy phép cho phép dùng thương mại và không bắt ghi
  công.** Mục 8 cấm "ảnh tìm trên mạng", và ý của lệnh cấm nằm ở câu đầu mục:
  không dùng ảnh **chưa rõ nguồn**. Ảnh có giấy phép tra được thì thuộc dòng
  "ảnh mua có giấy phép" của bảng, với điều kiện lưu lại chứng từ — và `NGUON.md`
  chính là chỗ lưu.
- **Ảnh TẢI VỀ, không nhúng thẳng từ CDN.** Nhúng thẳng thì mỗi lượt xem gửi địa
  chỉ IP của khách sang một CDN nước ngoài mà khách không hề đồng ý (`31`). Tải
  về còn bỏ được một phụ thuộc lúc chạy.
- **Đây vẫn là ảnh mẫu, không phải ảnh của sản phẩm.** Không tấm nào chụp đúng
  khách sạn hay đúng chuyến đi nó đang minh hoạ, nên chúng **không** dùng được
  cho bản chạy thật. Có ảnh thật thì xoá cả thư mục lẫn `NGUON.md`.

Chữ `alt` phải mô tả **tấm ảnh đang hiện**. Đổi ảnh mà giữ nguyên chữ `alt` cũ là
nói sai với đúng nhóm người phụ thuộc vào nó nhất — xem mục 6.

### 7.2. Ảnh chụp cái gì

| Nên | Không nên |
|---|---|
| Cảnh và người trong cùng khung: khách đang đi, đang ăn, đang xem | Cảnh trống không có ai — không cho khách hình dung mình ở trong đó |
| Người ở độ tuổi gần với khách thật | Chỉ toàn người trẻ đi phượt |
| Ánh sáng tự nhiên, màu gần thật | Màu chỉnh đậm tới mức khách tới nơi thấy khác hẳn |
| Đúng mùa của chuyến đi | Ảnh mùa khô cho tour mùa mưa |

Dòng cuối là ràng buộc thật: ảnh đẹp nhưng sai mùa là hứa hẹn một thứ khách sẽ
không thấy.

### 7.3. Ảnh có chữ

**Mọi font dùng trong ảnh và file SVG phải dựng sẵn cả `æ ø å` lẫn dấu tiếng
Việt** (`21` mục 3.1). Thử bằng đúng hai dòng ở đó, **trước khi xuất file**.

Bản demo đã dính bẫy này: bản đồ dùng Georgia, "Điểm cuối" hiện thành "Điểm cuố
i". Đây là chỗ bẫy ẩn lâu nhất — trang thì đúng, ảnh thì sai, và không ai kiểm
ảnh khi rà soát nội dung.

Ảnh có chữ phải có **hai bản, mỗi ngôn ngữ một bản**, vì chữ trong ảnh cũng là
nội dung. Bản đồ tiếng Đan không dùng được cho trang tiếng Việt.

---

## 8. Bản quyền

**Không dùng ảnh chưa rõ nguồn.** Không có ngoại lệ, kể cả cho bản nháp: ảnh tạm
là thứ ở lại lâu nhất.

| Nguồn | Điều kiện |
|---|---|
| Công ty tự chụp | Ưu tiên. Cần giấy đồng ý của người nhận ra được mặt |
| Ảnh mua có giấy phép | Lưu chứng từ giấy phép, ghi rõ phạm vi dùng |
| Ảnh của đối tác (khách sạn, hãng tàu) | Xin bằng văn bản, ghi rõ được dùng trên web và trong quảng cáo hay không |
| Ảnh khách gửi | Cần đồng ý bằng văn bản, ghi rõ có nêu tên hay không |
| Ảnh tìm trên mạng | **Không dùng** |

Với mỗi ảnh phải biết: **nguồn · giấy phép · phạm vi · hạn dùng nếu có**. Ảnh
người nhận ra được mặt cần thêm giấy đồng ý của chính người đó — yêu cầu về dữ
liệu cá nhân, không phải chỉ về bản quyền; chi tiết ở `32`.

Hệ thống hiện **chưa có chỗ lưu bốn thông tin đó** — xem mục 9. Cho tới khi có,
giữ chúng trong một sổ ngoài hệ thống, và ghi vào tên thư mục chứa ảnh gốc.

---

## 9. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| **Ai viết nội dung tour tiếng Đan Mạch** | Toàn bộ tài liệu này. Không có người viết bản nguồn thì không có gì để dịch và không có gì để bán | **Q-1** ở `41` mục 4 |
| Ảnh lưu ở đâu, có tầng tối ưu và định dạng phục vụ nào | Mục 7.1 | Q-6 ở `41` mục 4 → ADR-008 |
| **`12` chưa có bảng bộ ảnh** — chỉ có `hero_image` và `map_image` là hai cột chữ, trong khi `05` yêu cầu bộ ảnh ở trang chi tiết | Bộ ảnh, thứ tự ảnh, alt cho từng ảnh | Cần bổ sung vào `12` |
| **Chưa có chỗ lưu nguồn và giấy phép ảnh** | Mục 8 | Cần bổ sung vào `12` |
| Chưa có chỗ lưu slug cũ để chuyển hướng 301 | Mục 4 | Cần bổ sung vào `12` |
| Có thuê nhiếp ảnh gia cho bộ ảnh riêng không, hay dùng ảnh đối tác | Mục 7.2 | Chủ sản phẩm |

---

## 10. Cấm

- Viết bản `vi` trước rồi dịch ngược sang `da`
- Dịch máy nội dung bán hàng
- Bản dịch thêm lời hứa, hoặc bỏ bớt điều kiện, so với bản nguồn
- Tính từ cực cấp, dấu chấm than, chữ viết tắt trong nội dung khách đọc
- Gạch đầu dòng `whyChooseThis` dán sang tour khác vẫn đúng
- Đổi slug đã xuất bản mà không có chuyển hướng
- Alt text bắt đầu bằng "Ảnh chụp"
- Ảnh có chữ dùng chung cho cả hai ngôn ngữ
- Font trong ảnh hoặc SVG chưa qua hai dòng thử ở `21` mục 3.1
- Ảnh sai mùa so với chuyến đi
- Ảnh chưa rõ nguồn, kể cả ở bản nháp
