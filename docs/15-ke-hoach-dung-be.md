# Kế hoạch dựng backend

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: thứ tự dựng backend theo đợt, và những quy ước mượn từ dự án
                  comic-social-network-be cùng lý do nhận hay loại từng cái.
Không nói về: kiến trúc và ranh giới module (10), lược đồ (12), hợp đồng API (13),
              quy tắc nghiệp vụ (14), giai đoạn và cổng nghiệm thu (40).
```

Tài liệu này trả lời: **dựng backend theo thứ tự nào, và ở đâu thì học lại dự án
trước thay vì nghĩ lại từ đầu.**

Không đặt ra giai đoạn mới. Năm đợt dưới đây nằm gọn trong G3, G4, G5 của `40`.

---

## 1. Dự án đối chiếu

`github.com/nguyenloc710/comic-social-network-be` — cùng người viết, đã chạy thật
trên VPS. Stack gần: Java 21, Spring Boot 3.4.4, PostgreSQL 16, Gradle Kotlin
DSL, Flyway với `ddl-auto: validate`, **một module Gradle chia theo feature**.
Dự án này ban đầu dùng bốn module; ADR-010 đã đổi sang cùng khuôn.

Giá trị lớn nhất của nó không phải là code mà là **những cái bẫy đã trả giá để
biết**. Mục 2 nhận, mục 3 loại.

---

## 2. Mượn nguyên — đã chạy thật, không phải phỏng đoán

| Quy ước | Áp dụng ở đâu |
|---|---|
| **Khoá bi quan + sổ cái chỉ ghi thêm** cho mọi thay đổi số dư; trigger CSDL cấm `UPDATE`/`DELETE` trên bảng nhật ký | Tồn kho và giữ chỗ (`14` mục 6), `booking_event`. Khuôn mẫu: `DonationService` |
| **Flyway sở hữu lược đồ, JPA `ddl-auto: validate`** — không để Hibernate tạo hay sửa bảng | Đợt 2 trở đi, khi có entity đầu tiên |
| **MapStruct cho entity → DTO**, không map tay trong service | Đợt 2. Phía đọc dùng SQL thuần trả record thì không cần |
| **`@EnableJpaAuditing` chỉ bật `@CreatedBy` và `@LastModifiedBy`** | `api/CLAUDE.md` mục 7b — `last_modified_at` là việc của trigger |
| **Tải ảnh bằng presigned URL**, backend không proxy file | ADR-008, đợt 5 |
| **Hai endpoint lưu trữ tách biệt**: một nội bộ cho backend gọi, một công khai để ký URL | ADR-008. SigV4 ký cả host: ký bằng host nội bộ thì trình duyệt không tải lên được |
| **Chuẩn hoá slug khử dấu, trùng thì thêm hậu tố `-2`, `-3`** | `24` mục 4 |
| **CI/CD: đẩy `main` → test → build image → registry → SSH `docker compose pull/up`** | `34`, đợt 5 |

Hai cái đáng tiền nhất là dòng đầu và dòng thứ sáu. Dòng đầu vì đặt chỗ trùng là
lỗi hạng nhất của mọi hệ thống có tồn kho. Dòng thứ sáu vì nó là loại lỗi mà đọc
tài liệu SDK không ra, chỉ gặp mới biết.

---

## 3. Không mượn — dự án này đã chốt ngược lại

| Quy ước bên kia | Vì sao không dùng ở đây |
|---|---|
| Xoá mềm bằng `@SQLRestriction("deleted_date IS NULL")` | **ADR-003 bác bỏ đúng cơ chế này.** Nó hoạt động ngầm; quên một chỗ là rò dữ liệu đã xoá ra khách mà không có lỗi nào nổ. Ở đây điều kiện `AND NOT soft_delete` phải nhìn thấy được trong câu truy vấn |
| Bọc mọi phản hồi trong `BaseResponse<T>` | Hợp đồng là `openapi.yaml` (ADR-002). Thêm lớp bọc thì phải khai nó trong spec và mọi schema đội thêm một tầng, đổi lại không được gì |
| Trả câu tiếng người từ `messages.properties` | `13` mục 5: API trả **mã lỗi kèm tham số**, frontend dịch. Backend dịch thì bản dịch tồn tại hai nơi và lệch nhau. `MessageSource` chỉ dùng cho email và PDF — thứ backend gửi thẳng tới khách |
| JWT stateless trong `localStorage` | v1 không có đăng nhập cho khách. Nhân viên dùng phiên đăng nhập với cookie `HttpOnly` + `SameSite=Lax` (`13` mục 10); `22` mục 9 cấm token trong `localStorage` |
| Test cần PostgreSQL cài sẵn ở `localhost:5432` | Ở đây dùng Testcontainers: test chạy được trên máy sạch và trên runner CI mà không cần ai cài gì trước |
| ~~Một module Gradle chia theo feature~~ | **Đã đổi ý ngày 01/09/2026 — nay mượn luôn.** ADR-010 gộp bốn module thành một và chia theo feature đúng khuôn bên kia. Cái mất, gồm cả `archTest`, ghi trong ADR đó |

Ba dòng đầu không phải chuyện khẩu vị: mỗi dòng có một ADR hoặc một mục tài liệu
đã duyệt đứng sau. Muốn đổi thì viết **ADR mới thay thế ADR cũ**, không sửa ADR
cũ và không lách trong code (`40` mục 7).

---

## 4. Năm đợt

Mỗi đợt có đầu ra **đo được bằng một lệnh**, không phải bằng cảm giác xong.

### Đợt 1 — hoàn tất phần đọc của G3

| | |
|---|---|
| **Làm** | `GET /destinations` · `/destinations/{slug}` · lọc sản phẩm theo điểm đến · `/products/{slug}/departures` · `/posts` · `/site-info` · chuyển hướng 301 đọc `slug_history` · sitemap theo locale |
| **Chặn** | `12` chưa có bảng cho bài viết, lịch trình, khách sạn, buổi thuyết trình và thông tin điểm đến — xem mục 5 |
| **Xong khi** | Mọi endpoint đọc ở `13` mục 9.1 có thật; test tích hợp phủ đủ bốn tổ hợp `(market, locale)` |

### Đợt 2 — nền ghi và phân quyền

| | |
|---|---|
| **Làm** | Spring Security, phiên đăng nhập cookie; `role` và `staff_user_role` (đã có ở `V2`); `@PreAuthorize` theo ma trận `22` mục 2.1; MapStruct; `AuditorAware` lấy người đang đăng nhập |
| **Ghi chú** | **Entity JPA đầu tiên xuất hiện ở đây**, nằm ở `<feature>/entity` |
| **Xong khi** | Một tài khoản `TRANSLATOR` sửa được bản `vi` và **bị từ chối** khi sửa bản `da`, có test |

### Đợt 3 — engine giá

| | |
|---|---|
| **Làm** | Tám bước cộng dồn của `14` mục 2, làm tròn từng dòng, đặt cọc làm tròn xuống |
| **Ở đâu** | `pricing/service`, JUnit thuần, không dựng context, không chạm CSDL |
| **Chặn** | **Q-2** — sáu con số nghiệp vụ thị trường `VN` |
| **Xong khi** | `deposit + balance = total` đúng tuyệt đối trên 20 đơn mẫu của **cả hai** thị trường; ≥ 24 test cho riêng engine giá |

### Đợt 4 — tồn kho, giữ chỗ, đặt tour

| | |
|---|---|
| **Làm** | Khoá bi quan `FOR UPDATE`; `Idempotency-Key`; máy trạng thái `23` mục 4; mọi lần đổi trạng thái ghi `booking_event`; job quét hạn |
| **Ghi chú** | **Đặt ShedLock ngay từ job đầu tiên** (`14` mục 6.4): job này bất biến khi lặp, nhưng cùng cơ chế còn dùng cho gửi email nhắc — thứ không bất biến |
| **Xong khi** | Test hai khách đồng thời giành chỗ cuối: đúng **một** người thành công, người kia nhận `DEPARTURE_SOLD_OUT` |

### Đợt 5 — quản trị và vận hành

| | |
|---|---|
| **Làm** | CRUD quản trị theo `22`; ba màn hình dịch thuật; media qua presigned URL; `30` và `34` |
| **Xong khi** | Nhân viên nhập được một tour mới đủ hai ngôn ngữ **không cần lập trình viên** — tiêu chí ra số 7 của G4 |

---

## 5. Chưa chốt

| Việc | Chặn | Ghi ở |
|---|---|---|
| **`12` thiếu bảng cho `itinerary_day`, `product_hotel_stay`, `hotel`, `excursion`, bài viết, buổi thuyết trình, thông tin điểm đến** — quy tắc kiểm 3, 4, 10, 12 ở `12` mục 9 đã tham chiếu tới chúng | Phần lớn đợt 1 | Cần bổ sung `12` rồi một migration `V3` |
| Xoá mềm: giữ lọc tường minh hay đổi sang cơ chế ẩn của Hibernate | Toàn bộ đường đọc | Đổi thì cần **ADR mới thay ADR-003** |
| Nơi lưu ảnh và cách ký URL | Đợt 5 | **Q-6** ở `41` mục 4 → ADR-008 |
| Sáu con số nghiệp vụ `VN` | Đợt 3 | **Q-2** ở `41` mục 4 |
| Có cần Redis ở v1 không | Chưa. Giữ chỗ dùng khoá CSDL; đệm và chống trùng lượt xem là bài toán của quy mô chưa tới | Xem lại khi có **Q-4** |
