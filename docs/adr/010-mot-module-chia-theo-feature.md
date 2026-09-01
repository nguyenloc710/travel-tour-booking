# ADR-010 — Một module Gradle, chia theo feature

```
Trạng thái: Đã chốt
Ngày: 01/09/2026
```

## Bối cảnh

Backend dựng ban đầu theo bốn module Gradle — `domain`, `application`,
`infrastructure`, `web` — với chiều phụ thuộc một chiều và **ArchUnit** canh
ranh giới (`10` mục 3). Cách đó có ba cái được thật: `domain` không biết Spring
tồn tại nên test được bằng JUnit thuần trong vài mili giây; sai ranh giới là
build đỏ chứ không phải tranh luận; và `infrastructure` đổi được mà không đụng
tầng trên.

Cùng người bảo trì đang chạy một dự án khác, `comic-social-network-be`, theo
khuôn khác: **một module Gradle, chia theo feature ở tầng package**, mỗi feature
có `controller/ · dto/ · entity/ · mapper/ · repository/ · service/`. Dự án đó
đã chạy thật trên máy chủ.

Đọc hai dự án cùng lúc theo hai khuôn khác nhau là chi phí có thật, và nó rơi
vào đúng người phải đọc cả hai.

## Quyết định

Gộp bốn module thành **một**, chia theo feature ở tầng package:

```
vn.travel.booking.
  <feature>/{controller, dto, entity, mapper, repository, service}
  common/{config, dto, entity, exception, mapper, money, repository, util}
```

Feature hiện có: `admin` · `auth` · `booking` · `departure` · `destination` ·
`lecture` · `market` · `post` · `pricing` · `product` · `region` · `theme`.

Bỏ luôn tầng interface cổng: service gọi thẳng repository. Trước đây mỗi truy vấn
có một `*QueryPort` ở `application` và một `Jdbc*Adapter` ở `infrastructure`;
nay chỉ còn `*Repository`. Với một module thì cặp interface–implement đó không
mua được gì: nó tồn tại để đảo chiều phụ thuộc giữa hai module, mà nay không còn
hai module.

## Cái mất, nói thẳng

1. **Không còn `archTest`.** Ranh giới giữa lõi nghiệp vụ và hạ tầng nay dựa vào
   kỷ luật, không dựa vào trình biên dịch. Không có gì chặn một `@Repository`
   được tiêm thẳng vào `PricingEngine`, và ngày đó engine giá hết test được bằng
   JUnit thuần.
2. **Sáu bài test biến mất** — đúng sáu luật: `domain` không dính Spring, không
   dính JPA, không đọc đồng hồ hệ thống, không dùng `double`; `application` không
   dính JPA và Spring Web; `web` không chạm JPA.
3. Ranh giới nay là **quy ước đọc được**, không phải ràng buộc cưỡng chế được.

## Cái giữ

Ba thứ **không** đổi theo quyết định này, và mỗi thứ có ADR riêng đứng sau:

- **Spec-first** (ADR-002): `contracts/openapi.yaml` vẫn là nguồn sự thật,
  controller vẫn `implements` interface sinh ra.
- **Không dùng cơ chế ẩn cho locale và xoá mềm** (ADR-003): điều kiện vẫn viết
  tường minh trong truy vấn.
- **Lõi tính toán vẫn là hàm thuần**: `pricing/service/PricingEngine`,
  `departure/service/DepartureStatuses`, `booking/service/BookingStatuses` không
  nhận dependency nào và vẫn test bằng JUnit thuần. Chúng chỉ mất **hàng rào**,
  không mất **tính chất** — và giữ tính chất đó là việc của người rà soát mã.

## Phương án đã cân nhắc và bị loại

| Phương án | Bỏ vì |
|---|---|
| Giữ bốn module | Hai dự án cùng người bảo trì đọc theo hai khuôn khác nhau |
| Một module nhưng giữ `archTest` với luật theo package | ArchUnit làm được, nhưng luật theo package yếu hơn hẳn luật theo classpath: lỡ tay `import` vẫn biên dịch được, chỉ đỏ ở test. Nếu muốn hàng rào thì bốn module là hàng rào thật |
| Một module, chia theo tầng (`controller/`, `service/`, `repository/` ở gốc) | Mở `service/` ra thấy 12 lớp không liên quan gì nhau. Chia theo feature giữ được thứ đi cùng nhau ở cạnh nhau |

## Hệ quả

- `10` mục 3 và `api/CLAUDE.md` mục 1 viết lại theo cấu trúc mới.
- `15` mục 3 bỏ dòng "một module Gradle" khỏi danh sách **không mượn**.
- Thêm feature mới thì tạo package mới theo đúng sáu thư mục con, không nhét vào
  feature sẵn có.
- Nếu về sau ranh giới lõi nghiệp vụ bị phá tới mức engine giá không test thuần
  được nữa, đó là lúc viết ADR mới thay thế ADR này — **không** sửa ADR này.
