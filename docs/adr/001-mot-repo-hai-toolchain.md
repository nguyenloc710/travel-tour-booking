# ADR-001 — Một repo hai toolchain, backend Java Spring Boot

```
Trạng thái: Đã chốt
Ngày: 31/08/2026
```

## Bối cảnh

Backend viết bằng Java Spring Boot (ràng buộc từ đội ngũ, không phải lựa chọn kỹ
thuật mở). Frontend là Next.js. Gradle và pnpm không sống chung trong một
workspace được, nên phải quyết cách chia repo.

## Phương án đã cân nhắc

| | Ưu | Nhược |
|---|---|---|
| **A. Một repo, hai toolchain** | `docs/` và `contracts/` dùng chung, đổi hợp đồng API sửa được cả hai bên trong một pull request | CI phức tạp hơn, phải lọc theo đường dẫn |
| B. Ba repo riêng | Mỗi repo một pipeline sạch | Hợp đồng API lệch mà không ai phát hiện. Tài liệu phải chọn một repo để ở, bên còn lại không đọc |
| C. Monorepo có công cụ hợp nhất (Bazel, Nx) | Một cách build duy nhất | Chi phí học và bảo trì quá lớn cho đội ngũ ở quy mô này |

## Quyết định

Chọn **A**.

```
api/         Gradle multi-module — Spring Boot
web/         pnpm workspace — Next.js site + admin
contracts/   openapi.yaml
docs/        bộ tài liệu
```

Kèm theo:

- Java **21 LTS**; Gradle Kotlin DSL; PostgreSQL 16+
- CI hai pipeline, lọc theo đường dẫn. Sửa `contracts/` kích hoạt **cả hai**
- `api/` chia bốn module: `domain`, `application`, `infrastructure`, `web`;
  `domain` không phụ thuộc Spring, kiểm bằng ArchUnit

## Hệ quả

- Một pull request sửa được cả hai bên khi đổi hợp đồng API — đây là lợi ích chính
- Người mới phải cài cả JDK lẫn Node
- Lịch sử git lẫn hai loại thay đổi; dùng tiền tố commit `api:` / `web:` /
  `contracts:` / `docs:` để đọc lại được
- Tách repo về sau vẫn làm được, chi phí thấp — quyết định này không khoá cửa
