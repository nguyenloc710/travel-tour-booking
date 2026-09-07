# ADR-011 — MinIO tự dựng, cho cả dev và bản chạy thật

```
Trạng thái: Đã chốt
Ngày: 06/09/2026
Thay thế ADR-008
```

> **Q-6 đã chốt.** ADR-008 bày ba phương án và đề xuất kho đối tượng có quản đặt
> ở EU. Người quyết chọn phương án còn lại: **MinIO tự dựng**. ADR này ghi lại
> quyết định đó, phần nào của ADR-008 vẫn còn hiệu lực, và cái giá phải trả —
> viết ra để sáu tháng sau không ai phải đoán.

## Bối cảnh

`12` (migration `V2`) đã có `media_asset`, `media_asset_translation` và
`product_image` từ lâu, nhưng chưa dòng ảnh nào đi qua hệ thống: chưa có endpoint
tải lên, và `media_asset.path` chưa biết trỏ vào đâu. Việc này chặn `05` (bộ ảnh
ở trang chi tiết), `20` (chưa cấu hình được `next/image` vì chưa biết host nào
được phép) và `24` mục 7.

ADR-008 đã thu hẹp câu hỏi xuống còn hai: **kho S3 nào**, và **có CDN ở v1
không**. Nó đề xuất kho có quản, với lý do nặng nhất là dòng "máy chủ chết thì
ảnh mất cùng máy".

## 1. Quyết định

**MinIO tự dựng, dùng chung một cấu hình cho cả môi trường dev và bản chạy thật.
Không CDN ở v1.**

Ở dev, MinIO chạy trong `compose.yaml` cạnh Postgres. Ở bản chạy thật, MinIO chạy
trên VPS cùng cụm với API.

Lý do của người quyết, theo thứ tự:

1. **Không phát sinh chi phí thuê ngoài.** Kho có quản tính tiền theo dung lượng
   và băng thông ra; MinIO không tính gì.
2. **Dữ liệu nằm trên máy của mình.** ADR-008 mục 3 viết rằng MinIO *"là phương
   án đúng khi có ràng buộc bắt dữ liệu phải nằm trên máy của mình; ràng buộc đó
   chưa ai nêu"*. Nay đã nêu.

## 2. Phần của ADR-008 vẫn nguyên hiệu lực

Đây là phần quan trọng nhất của ADR này: **đổi nhà cung cấp không đổi thiết kế.**
MinIO nói giao thức S3, nên bốn quy ước dưới đây giữ nguyên từng chữ.

| Quy ước | Vì sao vẫn đúng |
|---|---|
| Tải ảnh bằng **presigned URL**, backend không proxy byte | `15` mục 2 — đã chạy thật ở dự án trước. MinIO ký presigned URL y như S3 |
| **Hai endpoint tách biệt**: một nội bộ cho backend gọi, một công khai để ký | SigV4 ký cả host. Ký bằng host nội bộ thì trình duyệt nhận `403` mà không nói vì sao — `15` mục 2 gọi đây là loại lỗi đọc tài liệu SDK không ra |
| **`media_asset.path` là đường dẫn tương đối** | `12` mục 4.6. Địa chỉ gốc nằm ở cấu hình, không nằm trong dữ liệu. Thêm CDN về sau là đổi một biến môi trường, không phải một migration |
| **Vùng EU** | `media_asset` có cột `person_consent`: hệ thống đã biết trước ảnh chứa người nhận diện được, và thị trường `DK` nằm trong EU nên đó là dữ liệu cá nhân |

Dòng cuối đổi hình thức chứ không đổi nội dung. Tự dựng nghĩa là **vị trí VPS
chính là vị trí dữ liệu**: `34` mục 1 ghi `prod` là "chưa dựng", nên khi dựng thì
VPS phải đặt ở EU. Trước đây đó là một ô chọn khi đăng ký dịch vụ; nay nó là một
ràng buộc phải nhớ.

## 3. Cái giá phải trả

Ghi ra đây vì đây là thứ mà một ADR tồn tại để ghi. Không cái nào là lý do đảo
quyết định — chúng là việc phải làm để quyết định này không hoá thành tai nạn.

| Rủi ro | Vì sao nó thật | Phải làm gì |
|---|---|---|
| **Máy chủ chết thì ảnh đi cùng máy** | Ảnh tour không sinh lại được: một bộ ảnh chụp đúng mùa (`24` mục 7.2) mất đi là mất một chuyến đi chụp lại | Sao lưu ảnh ra **ngoài** VPS, và kiểm thử khôi phục |
| **Sao lưu Postgres không đụng tới kho đối tượng** | Khôi phục CSDL xong mà ảnh không còn thì mọi `media_asset.path` trỏ vào hư không, và **không truy vấn nào báo lỗi** | `35` phải có mục sao lưu ảnh riêng — `35` hiện chưa viết |
| **Thêm một dịch vụ có trạng thái phải vá và giám sát** | MinIO là phần mềm máy chủ, có phiên bản và có lỗ hổng | Vào danh mục vá và giám sát cùng Postgres |
| **Xoá mềm `media_asset` không xoá tệp trong kho** | Tệp mồ côi tích lại vô hạn | Quy tắc dọn tệp mồ côi, và nó phải **chậm, có thời gian chờ**: xoá ngay là biến một thao tác hoàn tác được thành không hoàn tác được |

Một điểm cộng đáng ghi: so với đĩa thường của VPS, MinIO **giải được Q-5**. Đĩa
cục bộ không chia sẻ giữa nhiều instance, nên chọn đĩa là chọn luôn "mãi mãi một
instance". MinIO không khoá cửa đó.

## 4. Cửa thoát

Quyết định này **không khó đảo ngược**, và đó là lý do nó chấp nhận được dù đi
ngược đề xuất của ADR-008.

MinIO và mọi kho có quản đều nói cùng giao thức S3. Client trong `api/` viết theo
giao thức, không theo nhà cung cấp. Ngày muốn đổi:

- không sửa một dòng code nào,
- không migration nào — vì `path` là tương đối,
- chỉ đổi hai biến endpoint, khoá truy cập, khoá bí mật và tên bucket.

Vì thế client phải là thư viện S3 tổng quát, **không** phải SDK riêng của MinIO.
Dùng SDK riêng là tự đóng cửa thoát này mà không mua được gì.

## 5. Cái này không quyết

- Định dạng phục vụ (WebP/AVIF) và tầng đổi kích thước ảnh — `24` mục 7.1 hỏi,
  nhưng đó là quyết định của frontend, đo được bằng thực nghiệm.
- **Q-4** (quy mô danh mục và lưu lượng) vẫn treo. Nó không còn chặn việc chọn
  nhà cung cấp nữa, nhưng vẫn cần để biết khi nào phải thêm CDN.
- Ai chụp ảnh, dùng ảnh đối tác hay thuê nhiếp ảnh gia — `24` mục 9.
- Chính sách lưu ảnh có người nhận diện được: thời hạn lưu, cơ sở pháp lý, quyền
  xoá — thuộc `31`.

## 6. Hệ quả phải làm

| # | Việc | Ở đâu |
|---|---|---|
| 1 | MinIO vào `compose.yaml`, kèm bucket khởi tạo sẵn | `compose.yaml` |
| 2 | Hai biến endpoint (nội bộ và công khai), khoá truy cập, khoá bí mật, tên bucket | `34` mục 3.1 |
| 3 | Endpoint ký presigned URL và endpoint ghi `media_asset` | `contracts/openapi.yaml` trước, theo ADR-002 |
| 4 | Host công khai vào danh sách cho phép của `next/image` | `20` |
| 5 | Mục sao lưu ảnh riêng | `35` — chưa viết |
| 6 | Quy tắc dọn tệp mồ côi, có thời gian chờ | `14` |
| 7 | VPS `prod` đặt ở EU | `34` mục 1 |

Mục 5 là chỗ dễ quên nhất, và cũng là chỗ đắt nhất nếu quên.
