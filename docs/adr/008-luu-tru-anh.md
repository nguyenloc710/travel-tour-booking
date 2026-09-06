# ADR-008 — Lưu trữ ảnh

```
Trạng thái: Đề xuất
Ngày: 02/09/2026
Bị thay thế bởi ADR-011
```

> **Chưa chốt, và cố ý chưa chốt.** Q-6 giao cho Kiến trúc sư (`41` mục 4). Tài
> liệu này gom lại những gì đã bị ràng buộc sẵn, bày ba phương án, và nêu một đề
> xuất kèm lý do — để người có quyền quyết định đọc rồi chốt, không phải để thay
> họ chốt. Mục 6 liệt kê đúng những gì còn thiếu.

## Bối cảnh

`12` (migration `V2`) đã có `media_asset`, `media_asset_translation` và
`product_image`. Lược đồ xong, nhưng **chưa dòng ảnh nào đi qua hệ thống**: chưa
có endpoint tải lên, và `media_asset.path` chưa biết trỏ vào đâu.

Đây là phần cuối còn thiếu của đợt 5 (`15` mục 4), và nó chặn thật: `24` mục 7 đã
tả xong tiêu chuẩn ảnh, `05` đã yêu cầu bộ ảnh ở trang chi tiết, `20` chưa cấu
hình được `next/image` vì chưa biết host nào được phép.

## 1. Q-6 hỏi chưa đúng câu

Q-6 viết: *"Lưu ảnh ở đâu: VPS, S3, hay CDN?"* Ba thứ đó **không loại trừ nhau**,
và một phần câu trả lời đã bị chốt ở chỗ khác rồi.

**Hai quy ước đã mượn nguyên từ `comic-social-network-be`** (`15` mục 2, đã chạy
thật trên máy chủ):

| Quy ước | Hệ quả |
|---|---|
| Tải ảnh bằng **presigned URL**, backend không proxy file | Trình duyệt gửi thẳng byte tới kho, backend chỉ ký |
| **Hai endpoint lưu trữ tách biệt**: một nội bộ cho backend gọi, một công khai để ký URL | SigV4 ký cả host — ký bằng host nội bộ thì trình duyệt không tải lên được |

Cả hai chỉ có nghĩa với **kho đối tượng nói giao thức S3**. Đĩa thường không có
khái niệm presigned URL: muốn có thì phải tự viết một cơ chế ký, tự đặt hạn, tự
chống phát lại — tức là tự viết lại phần khó nhất của S3.

Nên câu hỏi thật còn lại hẹp hơn nhiều:

1. **Kho S3 nào** — tự dựng trên VPS, hay dịch vụ có quản?
2. **Có CDN ở v1 không?**

Và câu 2 gần như không phải quyết định khó đảo ngược, vì
`media_asset.path` là **đường dẫn tương đối** (`12` mục 4.6): địa chỉ gốc nằm ở
cấu hình, không nằm trong dữ liệu. Thêm CDN về sau là đổi một biến môi trường,
không phải một migration. Cột đó được thiết kế tương đối chính vì lý do này.

> Nếu bỏ hai quy ước ở trên thì đĩa VPS quay lại bàn — nhưng bỏ chúng cần một ADR
> khác thay thế đoạn tương ứng của `15`, không phải một dòng trong ADR này.

## 2. Phương án đã cân nhắc

| | Đĩa của VPS | MinIO tự dựng trên VPS | Kho đối tượng có quản |
|---|---|---|---|
| Presigned URL | Phải tự viết | Có sẵn | Có sẵn |
| Hai endpoint nội bộ/công khai | Không có khái niệm | Tự cấu hình được | Có sẵn |
| Nhiều instance cùng đọc ghi (**Q-5**) | **Không** — đĩa cục bộ không chia sẻ | Được | Được |
| Nằm trong bản sao lưu nào | Phải tự thêm vào kịch bản sao lưu | Phải tự thêm | Của nhà cung cấp, vẫn **phải tự kiểm** |
| Máy chủ chết thì ảnh | **Mất cùng máy** | Mất cùng máy | Còn |
| Vị trí dữ liệu chọn được | Theo VPS | Theo VPS | Chọn vùng |
| Việc vận hành thêm | Ít nhất | **Một dịch vụ nữa phải vá và giám sát** | Không |
| Chi phí ở quy mô này | 0 | 0 | Nhỏ, nhưng khác 0 |
| Đổi sang thứ khác về sau | Khó — không cùng giao thức | Dễ — cùng giao thức S3 | Dễ |

Dòng **"máy chủ chết thì ảnh"** là dòng nặng nhất. Ảnh tour không phải dữ liệu
sinh ra lại được: một bộ ảnh chụp đúng mùa (`24` mục 7.2) mất đi là mất một chuyến
đi chụp lại, không phải mất một lần khôi phục.

Dòng **Q-5** cũng đáng chú ý vì nó khoá tương lai: chọn đĩa cục bộ là chọn luôn
"mãi mãi một instance", và câu hỏi số instance thì chưa ai trả lời.

## 3. Đề xuất

**Kho đối tượng có quản, nói giao thức S3, đặt ở vùng EU. Không CDN ở v1.**

Ba lý do, theo thứ tự quan trọng:

1. **Ảnh sống lâu hơn máy chủ.** Xem dòng nặng nhất ở trên.
2. **Vùng EU vì ảnh có người trong đó.** `media_asset` có sẵn cột
   `person_consent` — nghĩa là hệ thống đã biết trước rằng ảnh chứa người nhận
   diện được. Thị trường `DK` nằm trong EU, nên đó là dữ liệu cá nhân. Để dữ liệu
   trong EU là tránh hẳn câu hỏi về cơ chế chuyển dữ liệu ra ngoài — câu hỏi mà
   `31` chưa viết nên chưa ai trả lời được.
3. **Không CDN ở v1 vì chưa có gì để tối ưu.** Quy mô chưa biết (**Q-4**), và
   thêm CDN là thêm bài toán vô hiệu hoá cache vào một hệ thống mà nhân viên sửa
   ảnh hằng ngày. Thêm về sau chỉ tốn một biến môi trường.

**MinIO tự dựng bị loại**, dù nó miễn phí và cùng giao thức: nó đặt thêm một dịch
vụ có trạng thái lên đúng cái VPS mà nếu chết thì mất ảnh — tức là trả tiền bằng
việc vận hành mà không mua được thứ đắt nhất trong bảng. Nó là phương án đúng khi
có ràng buộc bắt dữ liệu phải nằm trên máy của mình; ràng buộc đó chưa ai nêu.

## 4. Hệ quả nếu chốt như đề xuất

- **Hai biến cấu hình cho hai endpoint**, không phải một. Ký bằng host nội bộ rồi
  đưa URL đó cho trình duyệt là lỗi đã có người trả giá (`15` mục 2) — SigV4 ký
  cả host, nên chữ ký không khớp và trình duyệt nhận `403` mà không nói vì sao.
- **Ba bí mật ở `34` mục 3.1 thành cụ thể**: khoá truy cập, khoá bí mật, tên bucket.
- **`20` khai được `next/image`**: host công khai vào danh sách cho phép.
- **`35` phải có mục sao lưu riêng cho ảnh.** Sao lưu Postgres **không** đụng tới
  kho đối tượng. Đây là chỗ dễ quên nhất của quyết định này: khôi phục CSDL xong
  mà ảnh không còn thì mọi `media_asset.path` trỏ vào hư không, và không truy vấn
  nào báo lỗi.
- **Xoá mềm một `media_asset` không xoá tệp trong kho.** Cần một quy tắc dọn tệp
  mồ côi — và nó phải chậm, có thời gian chờ: xoá tệp ngay là biến một thao tác
  hoàn tác được thành không hoàn tác được.
- **`path` giữ nguyên là tương đối.** Không bao giờ lưu URL đầy đủ vào CSDL: làm
  thế là đóng cứng nhà cung cấp vào từng dòng dữ liệu, và ngày đổi CDN sẽ cần một
  migration thay vì một biến môi trường.

## 5. Cái này **không** quyết

- Định dạng phục vụ (WebP/AVIF) và tầng đổi kích thước ảnh — `24` mục 7.1 hỏi,
  nhưng nó là quyết định của frontend và đo được bằng thực nghiệm, không phải
  quyết định hạ tầng khó đảo ngược.
- Ai chụp ảnh, dùng ảnh đối tác hay thuê nhiếp ảnh gia — `24` mục 9, thuộc Chủ
  sản phẩm.
- Cách lưu giấy phép và chứng từ — đã xong ở `12` (`ck_media_licence`).

## 6. Cần gì để chuyển sang `Đã chốt`

| # | Việc | Ai |
|---|---|---|
| 1 | Chọn nhà cung cấp và vùng cụ thể — có phần chi phí, nên không thuần kỹ thuật | KTS đề xuất, CSH duyệt chi |
| 2 | **Q-4** — quy mô danh mục và lưu lượng, để biết CDN là "chưa cần" hay "cần ngay" | CSH |
| 3 | **`31`** — lập trường về ảnh có người nhận diện được: thời hạn lưu, cơ sở pháp lý, quyền xoá | Cần viết `31` |
| 4 | Xác nhận VPS đặt ở đâu (`34` mục 1 ghi `prod` là "chưa dựng") | KTS |

Chốt xong thì sửa `10` mục 10, `41` mục 4, và đổi khối trạng thái ở đầu file này
— **chỉ khối trạng thái**. Thân ADR giữ nguyên kể cả phần về sau hoá ra sai; đó
là hồ sơ lịch sử (`42` mục 5.3).
