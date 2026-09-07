# syntax=docker/dockerfile:1

# Ảnh website khách, hai giai đoạn — docs/34 mục 5.1.
#
# BỐI CẢNH BUILD LÀ GỐC REPO:
#
#     docker build -f web/apps/site/Dockerfile -t travel-site .
#
# Cần `contracts/openapi.yaml` để sinh TS client (ADR-002), mà file đó nằm ngoài
# web/. Cùng lý do với `api/Dockerfile`.

# ---------------------------------------------------------------- giai đoạn 1
FROM node:22-alpine AS build
WORKDIR /src

# `openapi-generator-cli` là công cụ Java. Đây chính là lý do `web.yml` phải cài
# JDK 21 dù nó là pipeline frontend — thiếu Java thì bước sinh TS client chết,
# và nó chết với một stack trace của Node không nhắc gì tới Java.
RUN apk add --no-cache openjdk21-jre-headless
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Phiên bản pnpm lấy từ trường `packageManager` của web/package.json — một nguồn
# sự thật, giống cách `web.yml` làm.
RUN corepack enable pnpm

# Khai báo phụ thuộc trước phần mã nguồn: sửa một file .tsx không làm mất lớp
# cache của bước cài đặt, thứ tốn nhiều thời gian nhất.
COPY web/package.json web/pnpm-lock.yaml web/pnpm-workspace.yaml ./web/
COPY web/apps/site/package.json ./web/apps/site/
COPY web/apps/admin/package.json ./web/apps/admin/
COPY web/packages/api-client/package.json ./web/packages/api-client/
COPY web/packages/i18n/package.json ./web/packages/i18n/
COPY web/packages/ui/package.json ./web/packages/ui/

WORKDIR /src/web
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

COPY contracts /src/contracts
COPY web /src/web

# Sinh TS client TRƯỚC khi build, cùng thứ tự với `web.yml`: sinh sai thì lỗi
# hiện ngay ở đây, không hiện thành một đống lỗi kiểu khó hiểu ở bước sau.
RUN pnpm run contracts:generate:ts

# ---------------------------------------------------------------------------
# Bốn biến này bị NƯỚNG VÀO MÃ JAVASCRIPT lúc build, không đọc lúc chạy — đó là
# cách `NEXT_PUBLIC_*` hoạt động. Hệ quả phải biết trước:
#
#   * Đặt chúng trong `compose.prod.yaml` là VÔ TÁC DỤNG với phần chạy ở trình
#     duyệt. Chúng phải là `--build-arg`.
#   * Ảnh gắn liền với một môi trường. Không thể build một ảnh rồi đem cùng ảnh
#     đó chạy ở staging và prod — ngày có staging (Q-9) thì phải build hai lần.
#
# `NEXT_PUBLIC_API_URL` là địa chỉ trình duyệt gọi thẳng: `bookingApi()` chạy ở
# phía khách chứ không ở máy chủ Next (xem src/lib/api.ts). Nên nó phải là tên
# miền công khai, và tên miền đó phải nằm trong `CORS_ALLOWED_ORIGINS` của API.
#
# `NEXT_PUBLIC_SITE_URL` đi vào sitemap và robots.txt dưới dạng URL tuyệt đối.
# Để nguyên `localhost` thì công cụ tìm kiếm bỏ qua TOÀN BỘ site và không có lỗi
# nào nổ ra — docs/34 mục 5.2.
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_SITE_URL
ARG NEXT_PUBLIC_STORAGE_PROTOCOL=https
ARG NEXT_PUBLIC_STORAGE_HOST
ARG NEXT_PUBLIC_STORAGE_PORT=443
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL \
    NEXT_PUBLIC_STORAGE_PROTOCOL=$NEXT_PUBLIC_STORAGE_PROTOCOL \
    NEXT_PUBLIC_STORAGE_HOST=$NEXT_PUBLIC_STORAGE_HOST \
    NEXT_PUBLIC_STORAGE_PORT=$NEXT_PUBLIC_STORAGE_PORT

RUN pnpm --filter site build

# ---------------------------------------------------------------- giai đoạn 2
FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
RUN addgroup -S travel && adduser -S travel -G travel

# Ba dòng chép, không phải một. Bản standalone KHÔNG tự gói `static/` và
# `public/` — Next để chúng ngoài vì bình thường chúng do CDN phục vụ. Ở đây
# không có CDN, nên thiếu hai dòng dưới là site chạy được mà không có CSS và
# không có ảnh, một kiểu hỏng trông như lỗi giao diện chứ không như lỗi triển khai.
#
# Đường dẫn có `apps/site/` vì `outputFileTracingRoot` trỏ gốc workspace: cây
# thư mục trong bản standalone giữ nguyên hình dạng của workspace.
COPY --from=build --chown=travel:travel /src/web/apps/site/.next/standalone ./
COPY --from=build --chown=travel:travel /src/web/apps/site/.next/static ./apps/site/.next/static
COPY --from=build --chown=travel:travel /src/web/apps/site/public ./apps/site/public

USER travel
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0

CMD ["node", "apps/site/server.js"]
