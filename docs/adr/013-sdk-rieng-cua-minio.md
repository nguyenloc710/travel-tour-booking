# ADR-013 — Gọi kho ảnh bằng SDK riêng của MinIO

```
Trạng thái: Đã chốt
Ngày: 10/09/2026
```

Sửa một phần **ADR-011**. ADR-011 vẫn đúng ở mọi chỗ khác: kho ảnh là **MinIO tự
dựng**, tải lên bằng **presigned URL**, backend **không proxy byte**, và **hai
địa chỉ tách biệt** — nội bộ để backend gọi, công khai để ký. Cái bị đổi là
đúng một dòng: dùng thư viện nào để nói chuyện với nó.

## Bối cảnh

ADR-011 mục 4 viết:

> Vì thế client phải là thư viện S3 tổng quát, **không** phải SDK riêng của
> MinIO.

Lý do khi đó là tính chuyển đổi: MinIO và mọi kho có quản đều nói giao thức S3,
nên viết theo chuẩn S3 thì ngày đổi sang nhà cung cấp có quản chỉ phải đổi cấu
hình chứ không phải viết lại.

Lập luận đó không sai. Nó chỉ trả giá cho một ngày **chưa chắc tới**, và trả
bằng tiền mặt hôm nay: `software.amazon.awssdk:s3` kéo theo vài megabyte phụ
thuộc và một tầng cấu hình (region, credentials provider chain, path-style
access) mà dự án này không dùng tới cái nào — MinIO tự dựng không có region, và
khoá thì nằm sẵn trong biến môi trường.

## Quyết định

Dùng **`io.minio:minio`**, SDK riêng của MinIO.

Người quyết: chủ dự án, với lý do nêu thẳng — quy mô nhỏ, một máy chủ, và chưa
có nhu cầu đẩy lên kho có quản nào.

## Hệ quả

**Được:**

- Ít phụ thuộc hơn, và API ký URL ngắn hơn hẳn: một lời gọi
  `getPresignedObjectUrl` thay cho một `S3Presigner` cộng một `PutObjectRequest`.
- Không phải cấu hình những thứ MinIO tự dựng không có (region, path-style).

**Mất:**

- Ngày đổi sang kho có quản (S3, R2, Spaces) sẽ phải **viết lại lớp gọi kho**,
  không chỉ đổi cấu hình như ADR-011 hứa.
- Cái mất đó **được khoanh lại có chủ ý**: mọi lời gọi kho nằm trong đúng một
  lớp — `media/service/MediaStorage`. Ngày phải đổi thì đổi một tệp, và bài
  `MediaUploadIT` đã có sẵn để chứng minh bản mới còn chạy.

**Không đổi:** hai địa chỉ vẫn tách biệt, backend vẫn không nhận byte, đường dẫn
trong `media_asset.path` vẫn là đường dẫn tương đối (ADR-011 mục 2).

## Vì sao khoanh vào một lớp là đủ

Cách rẻ nhất để giữ tính chuyển đổi không phải là chọn thư viện tổng quát nhất —
nó là **giữ bề mặt tiếp xúc nhỏ**. `MediaStorage` có đúng ba phương thức công
khai: đặt tên tệp, ký URL, tra tệp. Không lớp nào khác trong `api/` biết MinIO
tồn tại, và không kiểu dữ liệu nào của MinIO rò ra ngoài nó — trừ
`StatObjectResponse` ở một chỗ, thứ sẽ đổi cùng lúc với phần còn lại.
