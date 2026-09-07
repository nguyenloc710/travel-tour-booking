# syntax=docker/dockerfile:1

# Ảnh trang quản trị, hai giai đoạn — docs/34 mục 5.1.
#
# BỐI CẢNH BUILD LÀ GỐC REPO:
#
#     docker build -f web/apps/admin/Dockerfile -t travel-admin .
#
# Khuôn giống hệt `web/apps/site/Dockerfile`; hai khác biệt được đánh dấu bên dưới.

# ---------------------------------------------------------------- giai đoạn 1
FROM node:22-alpine AS build
WORKDIR /src

RUN apk add --no-cache openjdk21-jre-headless
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
RUN corepack enable pnpm

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

RUN pnpm run contracts:generate:ts

# KHÁC BIỆT 1 — trang quản trị không có biến `NEXT_PUBLIC_*` nào.
#
# Trình duyệt của nhân viên chỉ nói chuyện với chính máy chủ Next, và Next
# chuyển tiếp `/api/*` sang API (xem `rewrites()` trong next.config.ts). Nên
# không có địa chỉ API nào rò ra phía khách, và cũng không cần CORS.
#
# Nhưng `API_URL` vẫn là tham số BUILD chứ không phải biến chạy: bản standalone
# đóng băng cấu hình đã giải trị vào `.next/required-server-files.json`, và
# `rewrites()` nằm trong đó. Đặt `API_URL` trong compose thì Next không đọc lại.
#
# Đây là địa chỉ NỘI BỘ giữa hai container — `http://api:8080` trong mạng của
# compose — chứ không phải tên miền công khai. Lượt gọi này không đi qua Internet.
ARG API_URL=http://api:8080
ENV API_URL=$API_URL

RUN pnpm --filter admin build

# ---------------------------------------------------------------- giai đoạn 2
FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
RUN addgroup -S travel && adduser -S travel -G travel

# KHÁC BIỆT 2 — chỉ hai dòng chép, không ba: trang quản trị không có thư mục
# `public/`. Thêm dòng chép nó vào là build đỏ ngay ở đây.
COPY --from=build --chown=travel:travel /src/web/apps/admin/.next/standalone ./
COPY --from=build --chown=travel:travel /src/web/apps/admin/.next/static ./apps/admin/.next/static

USER travel
EXPOSE 3001
ENV PORT=3001 HOSTNAME=0.0.0.0

CMD ["node", "apps/admin/server.js"]
