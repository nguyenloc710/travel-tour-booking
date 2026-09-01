# Kế hoạch thực hiện theo giai đoạn

```
Trạng thái: Đã duyệt
Cập nhật: 01/09/2026
Phiên bản: 1.0
Chủ sở hữu: Chủ sản phẩm
Người duyệt: Chủ sản phẩm + Kiến trúc sư
Nguồn sự thật về: bảy giai đoạn, cổng nghiệm thu, tiêu chí vào/ra, vai trò RACI,
                  mã truy vết yêu cầu, quy trình kiểm soát thay đổi.
Không nói về: nội dung nghiệp vụ (01–05), kỹ thuật (10–14),
              vòng đời tài liệu (42), trạng thái hiện tại (41).
```

Tài liệu này trả lời **ba câu hỏi điều hành**: đang ở giai đoạn nào, giai đoạn
này xong khi nào, và ai có quyền tuyên bố là xong.

Nó **không** trả lời "làm cái gì trước cái gì trong tuần này" — đó là việc của
sổ trạng thái `41`.

---

## 1. Nguyên tắc

**Cổng nghiệm thu là điểm không quay lại.** Qua cổng nghĩa là những quyết định
của giai đoạn trước được coi là đã chốt; muốn đổi thì mở phiếu thay đổi (mục 7),
không sửa lặng lẽ.

**Tiêu chí ra phải đo được.** "Tài liệu đã đầy đủ" không phải tiêu chí. "Chạy
`python scripts/docs_check.py` trả về 0 lỗi" mới là tiêu chí. Mọi dòng ở cột
*Tiêu chí ra* dưới đây đều kiểm được bằng một lệnh hoặc một thao tác quan sát
được, không cần ai phán xét.

**Tài liệu là sản phẩm bàn giao của giai đoạn, không phải phụ lục.** Giai đoạn
chưa có tài liệu ở trạng thái `Đã duyệt` thì chưa qua cổng, kể cả khi code đã
chạy.

**Mỗi giai đoạn mở khoá đúng một năng lực.** Không viết cả 25 tài liệu rồi mới
code — tài liệu đi trước code quá xa thì thành phỏng đoán, và phỏng đoán sai thì
phải viết lại.

---

## 2. Bảy giai đoạn — tổng quan

| Cổng | Giai đoạn | Mở khoá năng lực | Bàn giao chính | Trạng thái |
|---|---|---|---|---|
| **G0** | Định hướng | Bắt đầu viết code mà không sợ phải đập | `00`–`05`, ADR-001…004, `CLAUDE.md` ×3 | ✔ Xong 31/08/2026 |
| **G1** | Nền dữ liệu và hợp đồng | Dựng CSDL và API đọc | `10`–`14`, ADR-005, ADR-006 | ✔ Xong 31/08/2026 |
| **G2** | Dựng khung kỹ thuật | Lệnh trong `CLAUDE.md` chạy được thật | `api/`, `web/`, `contracts/`, CI, `34` nháp, `40`–`42` | ▶ **Đang làm** |
| **G3** | Lõi danh mục | Khách xem được tour đúng thị trường, đúng ngôn ngữ | `20`, `21`, API đọc, site khách | Chưa bắt đầu |
| **G4** | Đặt tour và quản trị | Nhận được đơn thật | `22`, `23`, tồn kho, giữ chỗ, trang quản trị | Chưa bắt đầu |
| **G5** | Sẵn sàng vận hành | Nhận được **tiền** thật | `24`, `30`–`35`, ADR-007…009 | Chưa bắt đầu |
| **G6** | Ra mắt và hypercare | Khách thật dùng | Biên bản ra mắt, 30 ngày trực sự cố | Chưa bắt đầu |

> **G5 là cổng đắt nhất và hay bị coi nhẹ nhất.** Nó chứa việc phải chờ người
> ngoài đội: luật sư duyệt `32`, cổng thanh toán cấp tài khoản thật, kiểm thử
> phục hồi sao lưu. Ba việc này đặt hàng **từ đầu G4**, không đợi tới G5 mới bắt
> đầu — nếu không, đội ngồi chờ.

---

## 3. Cơ chế cổng nghiệm thu

Mỗi cổng chạy đúng bốn bước:

```
1. Đối chiếu tiêu chí ra   → từng dòng, có bằng chứng (lệnh + kết quả)
2. Đối chiếu tài liệu      → mọi file trong cột "Đã duyệt" đạt trạng thái đó
3. Ghi biên bản            → thêm mục vào docs/41-tinh-trang.md
4. Người có thẩm quyền ký  → xem mục 5
```

**Cổng có thể qua với điều kiện treo.** Ghi rõ điều kiện, người chịu trách
nhiệm, hạn chót — vào `41`. Quá ba điều kiện treo thì không phải "qua có điều
kiện" mà là chưa qua.

**Không có cổng lùi.** Phát hiện quyết định cũ sai thì viết ADR mới thay thế ADR
cũ (`42` mục 5), không mở lại cổng đã đóng.

Chạy cổng bằng: `/cong G2` — xem `.claude/skills/cong-nghiem-thu/`.

---

## 4. Chi tiết từng giai đoạn

### G0 — Định hướng ✔ xong 31/08/2026

| | |
|---|---|
| **Mục tiêu** | Chốt mô hình thị trường/ngôn ngữ trước khi có dòng code đầu tiên |
| **Đầu vào** | Demo tại `D:\CODE\travel`, khảo sát website mẫu |
| **Bàn giao** | `CLAUDE.md` ×3 · `00` · `01` · `02` · `03` · `04` · `05` · ADR-001…004 |
| **Tài liệu phải `Đã duyệt`** | `02`, `04` — hai file quyết định lược đồ CSDL |
| **Tiêu chí ra** | Trả lời được "một tour hiển thị cho khách Đan và khách Việt khác nhau ở những trường nào, giá lấy từ đâu" mà không phải suy đoán |
| **Rủi ro chính** | Chốt sai mô hình đa ngôn ngữ → phát hiện sau khi có dữ liệu thật, đắt nhất dự án |
| **Đã treo lại** | `02` và `04` vẫn ở trạng thái `Nháp` — cần duyệt hình thức. Xem `41` |

### G1 — Nền dữ liệu và hợp đồng ✔ xong 31/08/2026

| | |
|---|---|
| **Mục tiêu** | Lược đồ CSDL và hợp đồng API đủ chi tiết để hai bên code song song |
| **Đầu vào** | G0 đã qua |
| **Bàn giao** | `10` · `11` · `12` · `13` · `14` · ADR-005 · ADR-006 |
| **Tiêu chí ra** | ERD đầy đủ; DDL viết ra được; quy tắc tồn kho trả lời được "hai khách cùng bấm đặt chỗ cuối cùng thì chuyện gì xảy ra" |
| **Rủi ro chính** | Lược đồ chưa từng chạy qua Postgres lần nào — lỗi cú pháp và lỗi ràng buộc chỉ lộ ra ở G2 |

### G2 — Dựng khung kỹ thuật ▶ đang làm

| | |
|---|---|
| **Mục tiêu** | Mọi lệnh trong ba file `CLAUDE.md` chạy được thật, không còn là lệnh "sẽ có" |
| **Đầu vào** | G1 đã qua; `12` có DDL; `13` có danh mục endpoint |
| **Bàn giao** | `api/` bốn module Gradle · `web/` pnpm workspace · `contracts/openapi.yaml` v0.1 · `compose.yaml` · CI hai pipeline code + bộ kiểm tài liệu, lọc đường dẫn · migration `V1__khoi_tao.sql` · dữ liệu mồi · `34` bản nháp · `40` `41` `42` |
| **Tài liệu phải `Đã duyệt`** | `40`, `42` |
| **Tiêu chí ra** | 1. `docker compose up -d` cho Postgres có sẵn `unaccent` và `pg_trgm`<br>2. `./gradlew build` xanh<br>3. `pnpm build` và `pnpm typecheck` xanh cả `site` lẫn `admin`<br>4. `pnpm contracts:generate` sinh được **cả** interface Java **và** TS client; controller rỗng `implements` interface đó và biên dịch sạch<br>5. Trên một CSDL **trắng**, Flyway dựng xong lược đồ của `12` và dữ liệu mồi nạp được. Migration chạy lúc **ứng dụng khởi động** (`./gradlew bootRun`) — dự án không áp plugin Flyway của Gradle, nên không có task `flywayMigrate`<br>6. `python scripts/docs_check.py` → 0 lỗi<br>7. CI chạy trên một PR thật: sửa `web/` **không** kích hoạt build Gradle |
| **Rủi ro chính** | DDL ở `12` sai khi gặp Postgres thật — **dự kiến sẽ có sai**, và đó chính là giá trị của giai đoạn này. Sai thì sửa `12` trước, sửa migration sau |
| **Không làm ở đây** | Logic nghiệp vụ. G2 chỉ dựng khung — một endpoint trả dữ liệu mồi là đủ |

> **Bẫy thứ tự.** Viết migration trước khi `openapi.yaml` tồn tại là được. Viết
> controller trước khi `contracts:generate` chạy là **sai quy trình** (ADR-002) —
> và sẽ phải viết lại khi interface sinh ra không khớp.

### G3 — Lõi danh mục

| | |
|---|---|
| **Mục tiêu** | Khách xem được tour đúng thị trường, đúng ngôn ngữ, đúng giá |
| **Đầu vào** | G2 đã qua; nội dung mẫu của ít nhất **3 tour đủ hai ngôn ngữ** |
| **Bàn giao** | `20-frontend-web` · `21-he-thong-thiet-ke` · API đọc (listing, chi tiết, bộ lọc, tìm kiếm) · site khách |
| **Tài liệu phải `Đã duyệt`** | `20`, `21`, và `05` nâng từ `Nháp` lên |
| **Tiêu chí ra** | 1. Một tour thật hiển thị đủ ở 4 tổ hợp `(DK,da)` `(DK,vi)` `(VN,vi)` `(VN,da)` với giá và tiền tệ đúng từng thị trường<br>2. Tour thiếu bản dịch `vi`: **biến mất** khỏi listing `vi`, khỏi tìm kiếm `vi`, khỏi sitemap `vi`, URL trả **404** — không hiện bản `da`<br>3. Chuỗi giao diện thiếu khoá `vi`: fallback `da`, ghi log, `pnpm i18n:check` **báo lỗi**<br>4. Sắp xếp tên tour đúng thứ tự `da-DK-x-icu` (æ ø å sau z) và `vi-VN-x-icu`<br>5. Gõ "hoi an" không dấu ra "Hội An"<br>6. Mọi màn hình có dữ liệu đủ **ba trạng thái**: skeleton, rỗng kèm hướng dẫn, lỗi<br>7. Không còn số liệu hiển thị nào hardcode — "Xem tất cả 92 tour" đếm từ dữ liệu trong phạm vi `(market, locale)`<br>8. Font hiển thị đúng cả `Strand og øer på tværs` lẫn `Điểm cuối · Hội An · Vịnh Hạ Long` |
| **Rủi ro chính** | Nội dung tiếng Đan không có người viết → hệ thống xong mà không có gì để bán. Xem `41` mục quyết định đang chờ |

### G4 — Đặt tour và quản trị

| | |
|---|---|
| **Mục tiêu** | Nhận được đơn thật, nhân viên vận hành được bằng trang quản trị |
| **Đầu vào** | G3 đã qua; **sáu con số nghiệp vụ thị trường VN** đã có (`14` mục 10) |
| **Bàn giao** | `22-trang-quan-tri` · `23-luong-dat-tour` · tồn kho + giữ chỗ · luồng báo giá `PRIVATE_TOUR` · trang quản trị đủ 4 vai trò · bảng điều khiển dịch thuật |
| **Tài liệu phải `Đã duyệt`** | `22`, `23`, `14` |
| **Tiêu chí ra** | 1. Đặt một đơn đầu-cuối trên staging: chọn ngày → giữ chỗ → nhập khách → xác nhận<br>2. Test hai khách đồng thời giành chỗ cuối cùng: đúng **một** người thành công, người kia nhận `DEPARTURE_SOLD_OUT`<br>3. Giữ chỗ hết hạn 20 phút trả chỗ về kho — kiểm bằng đồng hồ giả, không chờ thật<br>4. `deposit + balance = total` đúng tuyệt đối trên 20 đơn mẫu của **cả hai** thị trường<br>5. Đơn đã đặt **chụp lại** giá: đổi bảng giá không làm đổi đơn cũ<br>6. `PRIVATE_TOUR` không tự chốt được đơn — CTA là "Yêu cầu báo giá", sinh `Quote`<br>7. Nhân viên nhập được một tour mới đủ hai ngôn ngữ **không cần lập trình viên**<br>8. Mọi thao tác đổi trạng thái đơn ghi vào `booking_event` |
| **Rủi ro chính** | Thiếu sáu con số VN → engine giá chạy nhưng không bán được ở `VN`. Chặn từ đầu giai đoạn, không phát hiện ở cuối |
| **Bắt đầu song song** | Đặt lịch luật sư cho `32`; mở tài khoản sandbox cổng thanh toán hai thị trường |

### G5 — Sẵn sàng vận hành

| | |
|---|---|
| **Mục tiêu** | Nhận được tiền thật của khách thật mà không vi phạm luật hai nước |
| **Đầu vào** | G4 đã qua; **GĐ-6 đã trả lời** (v1 có thanh toán online thật hay không) |
| **Bàn giao** | `24` · `30`–`35` · ADR-007 cổng thanh toán · ADR-008 lưu ảnh · ADR-009 phạm vi COMBO |
| **Tài liệu phải `Đã duyệt`** | `30`–`35`, và `32` phải có **chữ ký người có chuyên môn pháp lý** |
| **Tiêu chí ra** | 1. `32` được luật sư hoặc chuyên viên pháp lý duyệt — **không phải lập trình viên tự duyệt**<br>2. Thanh toán sandbox chạy đủ: thành công · thất bại · timeout · hoàn tiền một phần<br>3. Có runbook cho "cổng thanh toán trả lỗi **sau khi** đã trừ tiền khách"<br>4. Phục hồi sao lưu thử **thật** trên môi trường sạch, đo được thời gian<br>5. Cookie consent và thời hạn lưu dữ liệu cá nhân đúng GDPR **và** NĐ 13/2023<br>6. Giá hiển thị đúng quy tắc pháp lý từng thị trường: chữ "từ" + disclaimer<br>7. Giám sát có cảnh báo cho: lỗi thanh toán, job quét hạn giữ chỗ chết, tỷ lệ 5xx |
| **Rủi ro chính** | Yêu cầu pháp lý hai nước mâu thuẫn → phát hiện muộn thì phải sửa cả giao diện lẫn quy trình |

### G6 — Ra mắt và hypercare

| | |
|---|---|
| **Mục tiêu** | Đưa vào chạy thật và sống sót 30 ngày đầu |
| **Đầu vào** | G5 đã qua; tiêu chí ra mắt ở `01` đạt đủ |
| **Bàn giao** | Biên bản ra mắt · lịch trực · báo cáo hậu kiểm 30 ngày |
| **Tiêu chí ra** | 1. Toàn bộ tiêu chí ra mắt của `01` đạt — **không tự chế tiêu chí mới ở đây**<br>2. Đơn thật đầu tiên của **mỗi** thị trường hoàn tất trọn vòng, đối soát khớp<br>3. 30 ngày không có sự cố mức nghiêm trọng chưa có runbook<br>4. Báo cáo hậu kiểm: cái gì tài liệu nói sai so với thực tế → sửa tài liệu |

---

## 5. Vai trò và thẩm quyền

| Ký hiệu | Vai trò | Có quyền ký cổng nào |
|---|---|---|
| CSH | Chủ sản phẩm | Mọi cổng — bắt buộc có |
| KTS | Kiến trúc sư | G1, G2, G3, G4 |
| BE | Lập trình viên backend | — |
| FE | Lập trình viên frontend | — |
| ND | Biên tập nội dung tiếng Đan | G3 (phần nội dung) |
| BD | Biên dịch `da`→`vi` | G3 (phần bản dịch) |
| QA | Kiểm thử | G4, G6 |
| PC | Pháp chế (thuê ngoài) | **G5 — bắt buộc, không uỷ quyền được** |
| VH | Vận hành | G5, G6 |

### RACI theo nhóm bàn giao

`R` làm · `A` chịu trách nhiệm cuối · `C` hỏi ý kiến · `I` thông báo

| Bàn giao | CSH | KTS | BE | FE | ND | BD | QA | PC | VH |
|---|---|---|---|---|---|---|---|---|---|
| Tài liệu nghiệp vụ `01`–`05` | **A/R** | C | I | I | C | C | I | – | – |
| Tài liệu kỹ thuật `10`–`14` | C | **A/R** | R | C | – | – | C | – | I |
| `contracts/openapi.yaml` | I | **A** | R | R | – | – | C | – | – |
| Lược đồ CSDL và migration | I | **A** | R | I | – | – | I | – | C |
| Giao diện khách `20`, `21` | C | C | I | **A/R** | C | I | C | – | – |
| Trang quản trị `22` | **A** | C | R | R | C | C | C | – | C |
| Nội dung tour tiếng `da` | C | – | – | – | **A/R** | I | – | – | – |
| Bản dịch `vi` | C | – | – | – | C | **A/R** | – | – | – |
| Thanh toán `30` | **A** | C | R | C | – | – | C | **C** | C |
| Pháp lý `32` | C | I | I | I | C | C | – | **A/R** | I |
| CI/CD, vận hành `34`, `35` | I | C | R | I | – | – | I | – | **A/R** |

> **Một `A` cho mỗi dòng.** Hai người cùng chịu trách nhiệm cuối nghĩa là không
> ai chịu. Dòng nào thấy hai `A` là lỗi của tài liệu này, sửa ngay.

---

## 6. Truy vết yêu cầu

Ba loại mã, đặt trong ngoặc vuông ngay tại chỗ định nghĩa:

| Tiền tố | Nghĩa | Nơi định nghĩa |
|---|---|---|
| `YC-###` | Một yêu cầu chức năng | `01`, `04`, `05`, `22`, `23` |
| `QT-###` | Một quy tắc nghiệp vụ tính toán được | `14` |
| `RB-###` | Một ràng buộc dữ liệu | `12` |

Mỗi mã phải đi hết chuỗi:

```
YC-042  định nghĩa ở 04 mục 5.2
   → endpoint  GET /api/v1/{market}/products  (13 mục 8)
   → bảng      product_market                 (12 mục 4)
   → test      ProductListingTest#anTourThieuBanDich
```

**Mã chỉ được cấp một lần, không tái sử dụng.** Yêu cầu bị bỏ thì đánh dấu
`YC-0xx — đã huỷ, xem PTĐ-0yy`, giữ nguyên số. Tái dùng số cũ làm mọi tham chiếu
lịch sử trỏ sai chỗ.

Ví dụ minh hoạ trong tài liệu phải đặt trong hàng rào code — bộ kiểm bỏ qua nội
dung trong hàng rào, nên ví dụ không lẫn vào ma trận truy vết thật.

Ma trận truy vết đầy đủ sinh tự động: `python scripts/docs_check.py --truy-vet`.
Yêu cầu không có test tương ứng bị báo là **lỗ hổng truy vết** — cảnh báo, không
chặn, cho tới G4; từ G4 trở đi là lỗi.

---

## 7. Kiểm soát thay đổi

Sau khi một cổng đã đóng, đổi thứ thuộc phạm vi cổng đó phải qua **phiếu thay
đổi** (PTĐ).

| Loại thay đổi | Cần gì |
|---|---|
| Sửa lỗi, làm rõ câu chữ, thêm ví dụ | Không cần PTĐ. Sửa và cập nhật ngày |
| Đổi quy tắc nghiệp vụ đã `Đã duyệt` | PTĐ + `A` của dòng RACI tương ứng duyệt |
| Đổi lược đồ CSDL sau khi có dữ liệu thật | PTĐ + KTS + kế hoạch migration + kế hoạch lùi |
| Đổi quyết định trong ADR | **ADR mới thay thế ADR cũ.** Không sửa ADR cũ |
| Thêm/bớt phạm vi v1 | PTĐ + CSH + cập nhật `01` mục "không làm" |
| Thêm thị trường hoặc ngôn ngữ | PTĐ + toàn bộ checklist ở `02` mục 12 |

Mẫu PTĐ ghi vào `41`:

```
PTĐ-007  · 12/09/2026 · Người đề xuất: … · Duyệt: …
Đổi gì:      …
Vì sao:      …
Ảnh hưởng:   tài liệu … · bảng … · endpoint … · test …
Phương án đã cân nhắc và bị loại: …
```

> **Không có PTĐ khẩn.** Việc gấp thì làm trước, nhưng PTĐ vẫn phải viết trong
> 24 giờ. Thay đổi không có dấu vết là thứ làm tài liệu chết.

---

## 8. Nhịp làm việc

| Nhịp | Việc | Ai |
|---|---|---|
| Mỗi phiên làm việc | Đọc `41` trước khi bắt đầu, cập nhật `41` trước khi kết thúc | Người làm |
| Mỗi tuần | Rà việc đang treo, số ngày treo của từng quyết định đang chờ | CSH |
| Mỗi cổng | Chạy cổng nghiệm thu, ghi biên bản, ký | Xem mục 5 |
| Mỗi 90 ngày | Rà tài liệu quá hạn — `42` mục 8 | Chủ sở hữu từng file |

---

## 9. Định nghĩa Sẵn sàng và Hoàn thành

**Một việc Sẵn sàng để làm khi:**

- Tài liệu nguồn sự thật cho việc đó đã ở trạng thái `Đã duyệt`, hoặc phần liên
  quan đã chốt và ghi rõ
- Không phụ thuộc con số nghiệp vụ nào đang chờ trả lời
- Có tiêu chí nghiệm thu viết trước khi bắt đầu, không viết sau

**Một việc Hoàn thành khi:**

- Code chạy, test của phần đó xanh, và **bộ kiểm liên quan tới thứ đã sửa** đã
  chạy — sửa `contracts/` thì chạy **cả hai** bên
- Tài liệu là nguồn sự thật của phần đó đã cập nhật, ngày `Cập nhật` đổi
- `python scripts/docs_check.py` → 0 lỗi
- Nếu có mã `YC`/`QT`/`RB` mới: đã có test tương ứng
- `41` đã cập nhật

---

## 10. Việc còn để ngỏ

| Việc | Chặn cổng nào | Ghi chú |
|---|---|---|
| GĐ-6 — v1 có thanh toán online thật không | G5 | Nếu không thì bỏ hẳn `30`, bộ tài liệu còn 24 file |
| Sáu con số nghiệp vụ thị trường `VN` | **G4** | `14` mục 10. Engine giá đã xong, chỉ thiếu cấu hình |
| Ai viết nội dung tour tiếng Đan | **G3** | Ràng buộc nhân sự, không phải kỹ thuật. Hệ thống xong mà không có nội dung thì không ra mắt được |
| Quy mô dự kiến (số tour, số đơn/tháng) | G2 | Quyết định có cần cache và chạy mấy instance |
| Ngày mong muốn ra mắt | Cả kế hoạch | Chưa có mốc thời gian nào trong tài liệu này là có chủ đích — mốc đặt ra khi biết quy mô đội |
