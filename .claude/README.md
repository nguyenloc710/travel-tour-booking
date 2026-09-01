# `.claude/` — quy trình làm việc cho Claude Code

Thư mục này biến quy ước trong `docs/40` và `docs/42` thành thứ **chạy được**,
thay vì thứ phải nhớ.

```
.claude/
  settings.json      quyền, hook
  hooks/             kiểm tự động, chặn sửa file sinh ra
  skills/            sáu quy trình dài, gọi bằng tên
  commands/          lệnh gạch chéo gọn cho việc hay làm
  agents/            tác nhân rà soát chuyên trách
```

---

## Lệnh

| Lệnh | Làm gì |
|---|---|
| `/tinh-trang` | Đọc `docs/41`, đối chiếu git, tóm tắt đang ở đâu và việc kế tiếp |
| `/kiem-tai-lieu` | Chạy bộ kiểm, sửa lỗi tìm được |
| `/cong G2` | Chạy cổng nghiệm thu một giai đoạn theo `docs/40` |
| `/tai-lieu 20-frontend-web` | Viết một tài liệu mới đúng chuẩn |
| `/ket-phien` | Kết thúc phiên: chạy kiểm, cập nhật `41`, đề xuất commit |

## Skill

Gọi bằng tên khi việc khớp, không cần lệnh:

| Skill | Khi nào |
|---|---|
| `tai-lieu-moi` | Viết một tài liệu mới trong `docs/` |
| `ra-soat-tai-lieu` | Rà soát định kỳ, sửa tài liệu lỗi thời |
| `cong-nghiem-thu` | Chốt một giai đoạn G0–G6 |
| `doi-hop-dong-api` | Bất cứ thay đổi nào chạm tới `contracts/openapi.yaml` |
| `them-thi-truong-ngon-ngu` | Thêm một `Market` hoặc một `Locale` |
| `ban-giao-khach` | Sinh lại bản Word gửi khách hàng |

## Hook

| Hook | Khi nào | Làm gì |
|---|---|---|
| `chan-file-sinh-ra.py` | Trước mọi `Write`/`Edit` | **Chặn** sửa tay `web/packages/api-client/` và code sinh từ spec |
| `kiem-tai-lieu.py` | Sau mọi `Write`/`Edit` | Nếu file vừa sửa nằm trong `docs/`, chạy bộ kiểm và trả lỗi ngược lại ngay |

Hai hook gọi `python` bằng **đường dẫn tương đối** từ gốc repo. Máy nào có
`python` trỏ tới Python 2 hoặc chưa có trong PATH thì đổi thành đường dẫn tuyệt
đối trong `settings.json`.

## Tác nhân

`ra-soat-nghiep-vu` — đọc chéo toàn bộ tài liệu và code tìm vi phạm bốn quy tắc
cốt lõi (Market ≠ Locale, `da` là nguồn, không fallback nội dung bán hàng, không
quy đổi tỷ giá). Chỉ chạy khi được yêu cầu — nó đọc nhiều file và tốn.

---

## Nguyên tắc

**Skill mô tả quy trình, không mô tả nội dung.** Nội dung nằm ở `docs/`. Skill
nào bắt đầu giải thích Market khác Locale ra sao là đang chép lại `docs/02`, và
sẽ lệch. Skill chỉ được **trỏ**.

**Hook không được cản trở người làm.** `kiem-tai-lieu.py` không chặn thao tác,
chỉ báo lỗi. Duy nhất `chan-file-sinh-ra.py` chặn thật, vì sửa file sinh ra là
việc luôn luôn sai.
