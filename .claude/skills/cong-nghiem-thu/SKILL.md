---
name: cong-nghiem-thu
description: Chạy cổng nghiệm thu một giai đoạn G0–G6 theo docs/40 — đối chiếu từng tiêu chí ra bằng bằng chứng thật, kiểm trạng thái tài liệu bắt buộc, ghi biên bản vào docs/41. Dùng khi được hỏi "xong giai đoạn chưa", "chốt G2", hoặc trước khi chuyển sang giai đoạn sau.
---

# Chạy cổng nghiệm thu

Tiêu chí ra của từng cổng nằm ở `docs/40-ke-hoach-thuc-hien.md` mục 4. Skill này
là cách chạy, không phải bản sao tiêu chí — **luôn đọc `40` để lấy tiêu chí, đừng
nhớ theo trí nhớ.**

## Nguyên tắc trên hết

**Bằng chứng, không phải ý kiến.** Mỗi tiêu chí phải kết thúc bằng một lệnh đã
chạy và kết quả của nó, hoặc một quan sát cụ thể. Không có bằng chứng thì tiêu
chí đó **chưa đạt**, kể cả khi trông có vẻ đúng.

**Không tự hạ tiêu chí.** Tiêu chí khó quá thì báo lại, đề xuất sửa `40` qua
phiếu thay đổi. Tuyệt đối không diễn giải lỏng ra để cổng qua.

## Thứ tự

### Bước 1 — Lấy tiêu chí

Đọc `docs/40` mục 4, phần của cổng đang chạy. Chép nguyên từng tiêu chí thành
một danh sách kiểm. Không thêm, không bớt.

### Bước 2 — Đối chiếu từng tiêu chí

Với mỗi tiêu chí, ghi lại đúng bốn thứ:

```
Tiêu chí:    <chép nguyên văn>
Bằng chứng:  <lệnh đã chạy + kết quả, hoặc file + dòng>
Kết luận:    ĐẠT | CHƯA ĐẠT | KHÔNG ÁP DỤNG (kèm lý do)
```

Tiêu chí nào chạy được bằng lệnh thì **chạy thật**, đừng suy luận:

| Loại tiêu chí | Lệnh |
|---|---|
| Tài liệu | `python scripts/docs_check.py` |
| Truy vết yêu cầu | `python scripts/docs_check.py --truy-vet` |
| Backend | `./gradlew build` · `./gradlew archTest` |
| Frontend | `pnpm build` · `pnpm typecheck` · `pnpm i18n:check` |
| Hợp đồng API | `pnpm contracts:generate` rồi biên dịch lại cả hai bên |
| CSDL | `./gradlew flywayMigrate` trên CSDL sạch |

### Bước 3 — Kiểm trạng thái tài liệu

`40` ghi rõ file nào phải ở `Đã duyệt` tại mỗi cổng. File còn `Nháp` là **điều
kiện treo**, không phải tiểu tiết bỏ qua được.

### Bước 4 — Kết luận

Đúng một trong ba:

| Kết luận | Điều kiện |
|---|---|
| **Qua** | Mọi tiêu chí ĐẠT, không có điều kiện treo |
| **Qua có điều kiện** | Tối đa **ba** điều kiện treo, mỗi cái có người chịu trách nhiệm và hạn chót |
| **Chưa qua** | Còn tiêu chí CHƯA ĐẠT thuộc phần lõi, hoặc quá ba điều kiện treo |

### Bước 5 — Ghi biên bản

Thêm vào `docs/41-tinh-trang.md`:

- Mục 7 *Nhật ký cổng*: ngày, cổng, kết quả, điều kiện treo
- Mục 1 *Đang ở đâu*: cập nhật giai đoạn hiện tại nếu cổng qua
- Mục 5 *Việc kế tiếp*: viết lại theo giai đoạn mới

Rồi báo lại người dùng: kết luận, số tiêu chí đạt/tổng, danh sách điều kiện treo,
và **ai cần ký** theo `40` mục 5.

## Việc skill này không làm

Không tự ký cổng. Không tự đánh dấu tài liệu thành `Đã duyệt`. Cả hai là thẩm
quyền của người, ghi ở `40` mục 5 — skill chỉ chuẩn bị đủ bằng chứng để người ký
mất ít thời gian.
