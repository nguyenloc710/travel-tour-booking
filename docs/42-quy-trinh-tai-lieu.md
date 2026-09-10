# Quy trình tài liệu

```
Trạng thái: Đã duyệt
Cập nhật: 10/09/2026
Phiên bản: 1.0
Chủ sở hữu: Kiến trúc sư
Người duyệt: Chủ sản phẩm + Kiến trúc sư
Nguồn sự thật về: vòng đời tài liệu, khối trạng thái, đánh số, quyền sở hữu,
                  quy trình sửa, bộ kiểm tự động, rà soát định kỳ.
Không nói về: bản đồ tài liệu (00), giai đoạn và cổng (40),
              trạng thái hiện tại (41).
```

Bộ tài liệu 25 file sẽ lỗi thời trong ba tháng nếu không có cơ chế giữ. Tài liệu
này là cơ chế đó.

Bài học lấy từ bản demo: **tài liệu chết vì trùng lặp và vì không ai biết file
nào còn đúng.** Demo có bản đồ URL nằm ở hai file; đổi đường dẫn một lần là một
trong hai chỗ lạc hậu vĩnh viễn, và không ai biết chỗ nào.

---

## 1. Vòng đời trạng thái

```
   Nháp ──────► Đang duyệt ──────► Đã duyệt
     ▲               │                 │
     └───── trả lại ─┘                 │
                                       ▼
                              Lỗi thời  hoặc  Bị thay thế bởi <file>
```

| Trạng thái | Nghĩa | Ai được dựa vào để code |
|---|---|---|
| `Nháp` | Đang viết, có thể sai, có thể đổi lớn | Đọc tham khảo. **Không** dựa vào để chốt lược đồ |
| `Đang duyệt` | Nội dung xong, chờ người có thẩm quyền đọc | Bắt đầu code được, chấp nhận rủi ro sửa nhỏ |
| `Đã duyệt` | Đã ký. Đổi phải qua phiếu thay đổi (`40` mục 7) | **Có** |
| `Lỗi thời` | Nội dung sai nhưng còn giữ để tra lịch sử | Không |
| `Bị thay thế bởi <file>` | Đã có file khác thay | Không — đọc file thay thế |

**Không xoá file tài liệu.** Chuyển sang `Lỗi thời` và ghi rõ file nào thay nó.
Xoá làm mọi link cũ và mọi tham chiếu trong lịch sử git trỏ vào hư không.

---

## 2. Khối trạng thái

Mọi file trong `docs/` mở đầu bằng khối này, đặt ngay sau tiêu đề `#`, trong
hàng rào ba dấu backtick:

```
Trạng thái: Nháp | Đang duyệt | Đã duyệt | Lỗi thời | Bị thay thế bởi <file>
Cập nhật: dd/mm/yyyy
Phiên bản: <major.minor>
Chủ sở hữu: <vai trò, không phải tên người>
Người duyệt: <vai trò>
Nguồn sự thật về: <một câu — file này chịu trách nhiệm về điều gì>
Không nói về: <trỏ sang file khác>
```

| Trường | Bắt buộc | Ghi chú |
|---|---|---|
| `Trạng thái` | ✔ luôn | Phải thuộc tập giá trị ở mục 1 |
| `Cập nhật` | ✔ luôn | `dd/mm/yyyy`. Đổi nội dung là phải đổi ngày |
| `Nguồn sự thật về` | ✔ luôn | Một câu. Hai file trùng nhau ở trường này là lỗi thiết kế |
| `Không nói về` | ✔ luôn | Trỏ sang file khác. Đây là thứ chống trùng lặp |
| `Phiên bản` | ✔ khi `Đã duyệt` | Tăng `minor` khi làm rõ, `major` khi đổi quyết định |
| `Chủ sở hữu` | ✔ khi `Đã duyệt` | **Vai trò**, không phải tên người — người rời dự án, vai trò thì không |
| `Người duyệt` | ✔ khi `Đã duyệt` | Lấy từ RACI ở `40` mục 5 |

> **`Chủ sở hữu` ghi vai trò chứ không ghi tên** là quyết định có chủ đích. Tài
> liệu ghi tên người sẽ mồ côi ngay khi người đó nghỉ, và không ai dám sửa file
> mồ côi.

---

## 3. Đánh số và đặt tên

| Dải | Tầng | Nội dung |
|---|---|---|
| `00`–`09` | 0 — Nền tảng | Bản đồ, đặc tả sản phẩm, thị trường/ngôn ngữ, từ vựng, sản phẩm |
| `10`–`19` | 1 — Kiến trúc và dữ liệu | Kiến trúc, mô hình dữ liệu, lược đồ, hợp đồng API, quy tắc nghiệp vụ |
| `20`–`29` | 2 — Ứng dụng | Frontend, thiết kế, quản trị, luồng đặt tour, nội dung |
| `30`–`39` | 3 — Vận hành và tuân thủ | Thanh toán, bảo mật, pháp lý, test, CI/CD, vận hành |
| `40`–`49` | 4 — Điều hành | Kế hoạch, trạng thái, quy trình tài liệu |
| `adr/NNN` | — | Một file một quyết định, số tăng dần, không tái sử dụng |

Tên file: `NN-ten-khong-dau.md`. **Không dấu tiếng Việt trong tên file** — cùng
lý do với quy tắc slug ở `CLAUDE.md`: công cụ, URL và một số hệ thống tệp xử lý
dấu không nhất quán.

Số hiệu **không tái sử dụng**. File `25` bị bỏ thì số `25` chết theo.

---

## 4. Ai sở hữu cái gì

| Tầng | Chủ sở hữu mặc định | Người duyệt |
|---|---|---|
| 0 — Nền tảng | Chủ sản phẩm | Chủ sản phẩm + Kiến trúc sư |
| 1 — Kiến trúc | Kiến trúc sư | Kiến trúc sư |
| 2 — Ứng dụng | Lập trình viên frontend | Chủ sản phẩm + Kiến trúc sư |
| 3 — Vận hành | Vận hành | Kiến trúc sư; riêng `32` là **Pháp chế** |
| 4 — Điều hành | Chủ sản phẩm | Chủ sản phẩm |
| `adr/` | Người đề xuất | Kiến trúc sư |

Chủ sở hữu chịu trách nhiệm: file còn đúng, ngày `Cập nhật` còn tươi, và không
lấn sang nguồn sự thật của file khác.

---

## 5. Quy trình sửa

### 5.1. Sửa nhỏ — làm rõ, sửa lỗi, thêm ví dụ

Sửa thẳng. Đổi `Cập nhật`. Không cần duyệt, không cần phiếu thay đổi.

### 5.2. Sửa nội dung của file `Đã duyệt`

1. Mở phiếu thay đổi theo `40` mục 7
2. Sửa file, tăng `Phiên bản`
3. Rà **file bị ảnh hưởng** — dùng `grep` tìm mọi chỗ trỏ tới phần vừa sửa
4. Chạy `python scripts/docs_check.py`
5. Cập nhật `41`

### 5.3. Khi nào phải viết ADR thay vì sửa tài liệu

Viết ADR khi thay đổi **khó đảo ngược** và người sáu tháng sau sẽ hỏi "vì sao lại
thế":

- Đổi công nghệ, thư viện, hạ tầng
- Đổi cách mô hình hoá một khái niệm nghiệp vụ
- Đổi cách hai hệ thống nói chuyện với nhau
- Bác bỏ một phương án mà người khác sẽ nghĩ tới

Không viết ADR cho: đổi tên biến, đổi bố cục màn hình, thêm một trường.

**ADR cũ không bao giờ bị sửa.** Đổi ý thì viết ADR mới, ghi `Thay thế ADR-00X`,
và sửa ADR cũ **chỉ một dòng**: thêm `Bị thay thế bởi ADR-00Y` vào khối trạng
thái. Nội dung phần thân giữ nguyên kể cả khi đã sai — đó là hồ sơ lịch sử.

---

## 6. Một sự thật, một chỗ

Quy tắc quan trọng nhất của tài liệu này.

| Sự thật | Chỉ được nằm ở |
|---|---|
| Bản đồ URL và route | `20` |
| Thứ tự cộng dồn giá tám bước | `14` |
| DDL và ràng buộc | `12` |
| Danh mục endpoint và mã lỗi | `13` |
| Chính sách fallback ngôn ngữ | `02` |
| Nghiệp vụ từng loại sản phẩm | `04` |
| Tiêu chí ra của từng cổng | `40` |
| Vòng đời và quy trình tài liệu | `42` — file này |

Chỗ khác cần nhắc tới thì **trỏ link**, không chép lại.

**Ba ngoại lệ được phép tóm tắt:**

1. Ba file `CLAUDE.md` — tóm tắt để nạp vào ngữ cảnh làm việc, nhưng phải ghi rõ
   nguồn, và khi lệch thì `docs/` đúng
2. `00` — bản đồ, nên phải nhắc tên mọi file
3. `41` — sổ trạng thái, nhắc lại tiêu chí đang dở

Ngoài ba chỗ đó, chép nội dung sang file khác là nợ kỹ thuật của tài liệu.

---

## 7. Bộ kiểm tự động

```bash
python scripts/docs_check.py            # kiểm toàn bộ
python scripts/docs_check.py --sua      # sửa được cái gì thì sửa (chỉ ngày tháng)
python scripts/docs_check.py --truy-vet # ma trận YC / QT / RB
python scripts/docs_check.py docs/02-thi-truong-va-da-ngon-ngu.md   # một file
```

| # | Phép kiểm | Mức | Vì sao cần |
|---|---|---|---|
| 1 | Khối trạng thái tồn tại và đủ trường bắt buộc | **Lỗi** | Không có khối này thì không ai biết file còn đúng không |
| 2 | `Trạng thái` thuộc tập giá trị hợp lệ | **Lỗi** | Trạng thái tự chế làm quy trình vô nghĩa |
| 3 | `Cập nhật` đúng định dạng `dd/mm/yyyy` | **Lỗi** | Không so sánh được thì không rà được hạn |
| 4 | File `Đã duyệt` có đủ `Phiên bản`, `Chủ sở hữu`, `Người duyệt` | **Lỗi** | Đã duyệt mà không biết ai duyệt là chưa duyệt |
| 5 | Link nội bộ trỏ tới file có thật | **Lỗi** | Link chết là dấu hiệu file đã bị đổi tên mà chỗ khác chưa biết |
| 6 | Thuật ngữ bị cấm (`03` mục 8) | **Lỗi** | `Country`, `exchangeRate`, `TourType` xuất hiện nghĩa là ai đó đang nghĩ theo mô hình cũ |
| 7 | `Cập nhật` cũ hơn 90 ngày | Cảnh báo | Đến hạn rà, chưa chắc đã sai |
| 8 | File có trong bản đồ `00` mà chưa tồn tại | Ghi chú | Là việc chưa làm, không phải lỗi |
| 9 | Mã `YC`/`QT`/`RB` không có test tương ứng | Cảnh báo → **Lỗi từ G4** | Yêu cầu không có test là yêu cầu không ai kiểm |
| 10 | Danh mục mã lỗi ở `13` mục 5.1 khớp enum `ErrorCode` của `contracts/openapi.yaml` | **Lỗi** | Hai bên nói cùng một danh mục cho hai loại người đọc: enum bắt máy xử lý đủ, bảng nói cho người *khi nào* mã đó xảy ra. Lệch nhau thì một trong hai đang nói dối |

Bộ kiểm này chạy ở ba chỗ: tay, hook sau mỗi lần sửa file trong `docs/`, và CI.

> **Phép kiểm số 6 là thứ rẻ nhất và có giá trị nhất.** Nó không bắt được lỗi
> logic, nhưng bắt được **lỗi tư duy** — và lỗi tư duy trong dự án hai thị trường
> là thứ đắt nhất.

---

## 8. Rà soát định kỳ

Mỗi 90 ngày, chủ sở hữu rà file của mình và làm đúng một trong ba việc:

| Kết luận | Việc |
|---|---|
| Còn đúng | Đổi `Cập nhật` thành hôm nay. Hết |
| Đúng nhưng thiếu | Bổ sung, tăng `minor`, đổi ngày |
| Không còn đúng | Chuyển `Lỗi thời` hoặc viết lại, tăng `major` |

**Không được để quá hạn mà không làm gì.** File quá hạn 90 ngày mà chủ sở hữu
không đụng tới là tín hiệu: hoặc file không ai cần, hoặc không ai thật sự sở hữu.
Cả hai đều phải xử lý, không phải bỏ qua.

---

## 9. Định nghĩa Hoàn thành cho một tài liệu

- [ ] Khối trạng thái đủ trường, đúng định dạng
- [ ] `Nguồn sự thật về` không trùng với bất kỳ file nào khác
- [ ] `Không nói về` trỏ đúng sang file chịu trách nhiệm phần đó
- [ ] Mọi khẳng định có thể kiểm được đều **kiểm được**, không phải ý kiến
- [ ] Không chép nội dung của file khác — chỉ trỏ link
- [ ] Mọi quyết định khó đảo ngược đã có ADR
- [ ] Đã cấp mã `YC`/`QT`/`RB` cho những gì cần truy vết
- [ ] `python scripts/docs_check.py` → 0 lỗi
- [ ] Đã đăng ký vào bản đồ `00`
- [ ] Đã ghi vào `41`

---

## 10. Quy ước viết

Kế thừa giọng của bộ tài liệu hiện có, không phải quy tắc trang trí:

- **Tiếng Việt.** Định danh code giữ nguyên tiếng Anh, đặt trong dấu backtick
- **Bảng thay cho đoạn văn** khi nội dung có cấu trúc. Bảng đọc lướt được
- **Nêu cái bẫy, không chỉ nêu quy tắc.** Câu "đây là chỗ lập trình viên mặc định
  làm ngược" có giá trị hơn ba đoạn giải thích đúng
- **Ghi cả phương án bị loại và lý do loại.** Người sáu tháng sau sẽ nghĩ tới
  đúng phương án đó
- **Con số phải có nguồn.** "20 phút" phải nói rõ đó là quyết định hay là khảo sát
- Không dùng emoji trong `docs/`. Dấu `✔` `▶` trong bảng trạng thái là ngoại lệ
