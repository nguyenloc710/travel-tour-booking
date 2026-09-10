# Từ vựng nghiệp vụ

```
Trạng thái: Nháp
Cập nhật: 09/09/2026
Nguồn sự thật về: thuật ngữ nghiệp vụ và bản đối chiếu Việt – Đan – định danh code.
Không nói về: quy tắc tính toán đằng sau mỗi thuật ngữ (14),
              chuỗi giao diện thật (message catalog trong web/).
```

Đội ngũ nói tiếng Việt, khách đọc tiếng Đan, code viết tiếng Anh. Ba lớp ngôn ngữ
chồng lên nhau là nguồn bug âm thầm: người này nói "điểm đến" nghĩa là
`Destination`, người kia hiểu là `primaryDestination`, và bộ lọc đếm sai.

**Cách dùng bảng này:** cột **Định danh** là tên duy nhất được dùng trong code —
tên bảng, tên lớp, tên trường, tên endpoint. Cột **Đan Mạch** là chữ khách nhìn
thấy; nó là khoá tra cứu khi đọc website mẫu, **không phải** chuỗi giao diện
chính thức. Chuỗi chính thức nằm trong message catalog của `web/`.

---

## 1. Địa lý

| Việt | Đan Mạch | Định danh | Nghĩa |
|---|---|---|---|
| Miền | Landsdel | `Region` | Miền Bắc / Trung / Nam. Cấp trên cùng của mega menu và bộ lọc. Đúng ba bản ghi |
| Điểm đến | Destination | `Destination` | Một nơi trong nước: Hanoi, Sapa, Halong-bugten. **Không phải quốc gia** |
| Điểm đến chính | — | `Tour.primaryDestinationId` | Nơi đứng tên tour trong breadcrumb. Một tour ghé nhiều nơi nhưng chỉ có một điểm đến chính |
| Thông tin cấp quốc gia | — | `SiteInfo` | Visa, mùa, tiền tệ, lệch giờ. Chỉ bán một nước nên gom về một bản ghi |

> **Bẫy đếm số.** Số tour của một miền đếm theo **tour**, không cộng dồn từng
> điểm đến. Một tour ghé Hanoi, Halong và Ninh Binh chỉ được tính **một lần** cho
> miền Bắc. Cộng dồn ra con số to hơn thực tế và khách phát hiện ngay khi bấm vào.

---

## 2. Sản phẩm

| Việt | Đan Mạch | Định danh | Nghĩa |
|---|---|---|---|
| Tour | Rejse | `Tour` | Một hành trình bán được, chưa gắn ngày cụ thể |
| Tour đoàn có trưởng đoàn | Rundrejse med dansk rejseleder | `ProductType.GROUP_TOUR` | Khách lạ đi chung, ngày khởi hành cố định, 10–25 khách |
| Tour cá nhân | Individuel rejse | `ProductType.INDIVIDUAL_PACKAGE` | Lộ trình dựng sẵn, khách đi riêng nhóm mình, tra được giá ngay |
| Tour riêng theo yêu cầu | Privat rejse | `ProductType.PRIVATE_TOUR` | **Khách đề xuất ngày**, giá theo bậc số khách, phải qua bước báo giá |
| Du thuyền | Krydstogt | `ProductType.CRUISE` | Giá theo hạng cabin |
| Combo bay + khách sạn | — | `ProductType.COMBO` | Bó thành phần, **không có lịch trình từng ngày**. v1.5 |
| Tour trong ngày | Udflugt | `ProductType.DAY_TOUR` | Bán độc lập, có khung giờ và tồn kho riêng. v2 |
| Bậc giá theo số khách | — | `priceTiers` | Chỉ `PRIVATE_TOUR`. 2 · 3–4 · 5–8 · 9–15 · 16+ |
| Báo giá | Tilbud | `Quote` | Thực thể riêng cho `PRIVATE_TOUR`, **không** là trường trên `Booking` |
| Loại khách | Rejsendetype | `PaxType` | Người lớn / trẻ em / trẻ nhỏ / em bé. **Dữ liệu của `Market`**, không hardcode |
| Điểm khởi hành | Afrejsested | `departureOrigin` | Ở `DK` là **phụ thu**; ở `VN` là **bộ lọc và biến thể sản phẩm** |

Đặc tả đầy đủ sáu loại — trường riêng, thẻ sản phẩm, luồng đặt, bộ lọc — nằm ở
`04-phan-loai-san-pham.md`. Bảng trên chỉ để tra tên.

> **`TourType` là tên cũ của bản demo.** Dự án này dùng `ProductType` vì `COMBO`
> và `DAY_TOUR` không phải tour. Thấy `TourType` ở đâu là code chép từ demo mà
> chưa đổi.
| Chủ đề | Rejsetype / tema | `Theme` | Trekking vùng cao, du thuyền sông, làng nghề, biển đảo… |
| Lộ trình | Rute | `Tour.route` | Danh sách chặng: nơi đến, số đêm, điểm nhấn |
| Ngày khởi hành | Afgang | `Departure` | **Một ngày cụ thể** của một tour, có giá riêng. Đây là thứ khách thật sự mua |
| Hạng cabin | Kabinekategori | `CabinCategory` | `INSIDE`, `OUTSIDE`, `BALCONY`, `AQUA`. Chỉ tour du thuyền |
| Chặng khách sạn | Hotelophold | `TourHotelStay` | Một tour ngủ bao nhiêu đêm ở khách sạn nào |
| Tham quan tuỳ chọn | Udflugt | `Excursion` | Buổi đi thêm, khách mua tại chỗ. **Không** nằm trong giá tour |
| Lịch trình | Dagsprogram | `ItineraryDay` | Chương trình từng ngày |
| Gia hạn | Forlængelse | `Departure.extensionVariant` | Biến thể kéo dài của một ngày khởi hành |

> **Bẫy `productType`.** Mỗi loại hiển thị **metadata khác nhau** trên thẻ, không
> phải cùng một bố cục: `GROUP_TOUR` hiện quy mô đoàn, `INDIVIDUAL_PACKAGE` hiện
> lộ trình theo đêm, `CRUISE` hiện tên tàu và số cảng, `PRIVATE_TOUR` hiện bậc số
> khách, `COMBO` hiện thành phần và điểm khởi hành. Đừng gộp làm một.

> **Bẫy `PRIVATE_TOUR`.** Loại duy nhất **không chốt được tự động**. Nút chính
> không phải "Đặt tour" mà là "Yêu cầu báo giá". Nó cũng không có tồn kho, nên
> không giữ chỗ và không bao giờ `SOLD_OUT`.

> **Bẫy du thuyền.** Một ngày khởi hành du thuyền có **nhiều bản ghi
> `Departure`** — mỗi hạng cabin một bản ghi. Bảng ngày khởi hành phải gộp theo
> ngày rồi hiện bốn dòng con.

---

## 3. Trạng thái ngày khởi hành

| Việt | Đan Mạch | Định danh | Nghĩa |
|---|---|---|---|
| Đảm bảo khởi hành | Garanteret afgang | `GUARANTEED` | Đã đủ số khách tối thiểu → chắc chắn chạy |
| Còn ít chỗ | Få pladser | `FEW_SEATS` | |
| Hết chỗ | Udsolgt | `SOLD_OUT` | Hiện mờ, không click được, không có nút đặt |
| Chờ chương trình | — | `PENDING` | Chưa có chương trình hoặc giá |
| Bình thường | — | `OPEN` | Không hiện badge |

> **`GUARANTEED` là trạng thái TÍNH RA, không lưu.** Nó bằng
> `seatsBooked >= minPax`. Kiểu dữ liệu lưu trong CSDL phải **loại trừ**
> `GUARANTEED` để không ai nhập tay được. Cùng mô-típ với `OUTDATED` của bản dịch
> (`02` mục 6.3) và số chỗ còn lại của buổi thuyết trình.

---

## 4. Giá và đặt tour

| Việt | Đan Mạch | Định danh | Nghĩa |
|---|---|---|---|
| Giá từ | Fra-pris | `priceFrom` | Giá thấp nhất, cho 1 người khi 2 người ở phòng đôi. **Luôn kèm chữ "từ"** — yêu cầu pháp lý |
| Phụ thu phòng đơn | Enkeltværelsestillæg | `singleSupplement` | Chênh lệch khi khách ở một mình |
| Đặt sớm | Tidlig booking | `earlyBird` | Đặt ≥ 6 tháng trước ngày khởi hành → giảm tối đa 1.000 kr/người |
| Sân bay khởi hành | Afrejselufthavn | `departureAirport` | Copenhagen mặc định; Billund và Aalborg có phụ thu. **Chỉ market `DK`** |
| Phí xử lý | Ekspeditionsgebyr | `processingFee` | Cố định một lần mỗi đơn |
| Đặt cọc | Depositum | `deposit` | 25% tổng ở market `DK` |
| Đơn đặt | Booking | `Booking` | Đơn đã chốt, có mã tra cứu |
| Bản nháp đặt tour | — | `BookingDraft` | Trạng thái tạm khi khách đang qua bốn bước, chưa chốt |
| Giữ chỗ | Pladsreservation | `SeatHold` | Khoá tạm số chỗ trong lúc khách thanh toán, **có hạn** |
| Yêu cầu tư vấn | Forespørgsel | `Lead` | Khách để lại thông tin, chưa đặt |

---

## 5. Nội dung và con người

| Việt | Đan Mạch | Định danh | Nghĩa |
|---|---|---|---|
| Nhân viên tư vấn | Rejsekonsulent | `Consultant` | Người phụ trách một miền, hiện trên trang tour và trang liên hệ |
| Trưởng đoàn | Rejseleder | — | Người dẫn đoàn tour `GROUP`. Ở v1 là nội dung tĩnh, chưa là thực thể |
| Buổi thuyết trình | Foredrag | `Lecture` | Buổi giới thiệu miễn phí, khách đăng ký chỗ |
| Bài viết | Blogindlæg | `Post` | |
| Đánh giá | Udtalelse | `Review` | |
| Bản tin | Nyhedsbrev | `Newsletter` | |

> **Buổi thuyết trình dùng chung luật với ngày khởi hành.** Số chỗ còn lại tính
> bằng `seats − seatsTaken`, không có cờ "đã đầy" trong CSDL. Buổi kín chỗ hiện
> mờ và khoá nút đăng ký, y như `SOLD_OUT`. Trang sự kiện chỉ hiện buổi **chưa
> diễn ra** — lọc ở tầng truy vấn, không lọc ở component.

---

## 6. Đa ngôn ngữ

| Việt | Định danh | Nghĩa |
|---|---|---|
| Thị trường | `Market` | Khách **mua** gì, giá bao nhiêu, theo luật nào. `DK`, `VN` |
| Ngôn ngữ | `Locale` | Khách **đọc** bằng tiếng gì. `da`, `vi` |
| Ngôn ngữ nguồn | `sourceLocale` | `da`. Mọi nội dung viết bằng tiếng Đan trước |
| Bản dịch | `*Translation` | Bảng riêng, khoá `(entityId, locale)` |
| Bản dịch lỗi thời | `OUTDATED` | Bản `da` đã sửa sau khi bản này được dịch. **Tính ra, không lưu** |
| Độ phủ bản dịch | `translationCoverage` | Phần trăm bản ghi đã dịch của một loại thực thể |
| Chuỗi giao diện | `messageKey` | Nhãn nút, tiêu đề cột. Nằm trong `web/`, **không** trong CSDL |
| Mã lỗi | `errorCode` | Backend trả mã, frontend dịch. `DEPARTURE_SOLD_OUT` |

> **Đừng nói "ngôn ngữ" khi ý là "thị trường".** Đây là nhầm lẫn tốn kém nhất
> của dự án này. Xem `02` mục 1.

---

## 7. Quy ước đặt tên trong code

| Chỗ | Quy ước | Ví dụ |
|---|---|---|
| Bảng CSDL | `snake_case`, số ít | `tour`, `tour_translation`, `seat_hold` |
| Cột | `snake_case` | `duration_days`, `price_from` |
| Lớp Java | `PascalCase` | `Tour`, `TourTranslation`, `SeatHold` |
| Trường Java | `camelCase` | `durationDays` |
| Endpoint | `kebab-case`, số nhiều | `/api/v1/dk/tours`, `/api/v1/dk/departures` |
| Trường JSON | `camelCase` | `durationDays` |
| Enum | `SCREAMING_SNAKE` | `GROUP`, `SOLD_OUT`, `DEPARTURE_SOLD_OUT` |
| Mã locale | ISO 639-1 chữ thường | `da`, `vi` |
| Mã market | ISO 3166-1 alpha-2 **chữ hoa** trong code, **chữ thường** trong URL | `DK` ↔ `/api/v1/dk/…` |
| Ngày | ISO 8601 | `2027-03-14` |

Slug **không dùng ký tự có dấu** ở cả hai ngôn ngữ: `bekraeftelse` chứ không
`bekræftelse`, `viet-nam-tu-bac-vao-nam` chứ không `việt-nam-từ-bắc-vào-nam`.

### 7.1. Ngôn ngữ của định danh

**Tên lớp và tên hàm viết bằng tiếng Anh** — cả `api/` lẫn `web/`, cả hàm thường
lẫn component React.

| Chỗ | Ngôn ngữ | Ví dụ |
|---|---|---|
| Lớp, interface, enum, record, component | **Tiếng Anh** | `ProductTranslation`, `ProductForm` |
| Hàm, phương thức, hook | **Tiếng Anh** | `listProducts()`, `translateError()` |
| `operationId` trong `openapi.yaml` | **Tiếng Anh** | `createProduct`, không `taoSanPham` |
| Trường, hằng số xuất ra ngoài tệp | **Tiếng Anh** | `durationDays`, `FIELDS_BY_TYPE` |
| Biến cục bộ trong một hàm | Tiếng Anh, tiếng Việt không phải lỗi | |
| Chú thích | **Tiếng Việt** | |
| Chuỗi khách nhìn thấy | `da` và `vi`, trong message catalog | |

`operationId` nằm trong bảng vì nó **không phải chuyện của riêng hợp đồng**: bộ
sinh mã lấy thẳng nó làm tên phương thức ở cả hai phía, nên một `operationId`
tiếng Việt đẻ ra một tên hàm tiếng Việt trong Java lẫn TypeScript, và không ai
sửa được nó ở phía dưới.

Vì sao tên thì Anh mà chú thích thì Việt: định danh là thứ **người ngoài đội cũng
đọc** — nó hiện trong stack trace, trong JSON, trong URL, trong tên tệp, trong ô
tìm kiếm của IDE và trong công cụ không gõ được dấu. Chú thích thì chỉ đội ngũ
đọc, và giải thích "vì sao" bằng tiếng mẹ đẻ luôn rõ hơn.

**Tên tiếng Việt đang có là di sản, đổi dần.** Quy tắc này áp cho code viết từ
nay; file nào sửa vì lý do khác thì đổi tên trong file đó luôn. Không mở một PR
chỉ để đổi tên hàng loạt: nó đụng tới gần như mọi tệp, và một xung đột trộn
nhánh ở giữa đợt đó tốn hơn nhiều so với cái nó sửa.

---

## 8. Thuật ngữ bị cấm

Dùng những từ này là dấu hiệu đang nghĩ theo mô hình cũ hoặc sai:

| Cấm | Dùng thay |
|---|---|
| `Country`, "quốc gia" (làm cấp phân loại) | `Destination`, "điểm đến". Chỉ bán một nước |
| `Continent`, "châu lục" | `Region`, "miền" |
| "Ngôn ngữ" khi ý là thị trường | `Market`, "thị trường" |
| "Tỷ giá", `exchangeRate` | Không tồn tại. Giá nhập riêng cho từng market — xem `02` mục 3 |
| "Chuyến bay" như một thực thể | Ở v1 vé bay là một mục **bao gồm trong giá**, không phải thực thể quản lý được |
| `language` làm tên cột | `locale` |
