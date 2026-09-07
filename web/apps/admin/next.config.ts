import type { NextConfig } from 'next';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Gốc của workspace pnpm: web/. Hai cấp lên từ web/apps/admin/.
const GOC_WORKSPACE = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

const API = process.env.API_URL ?? 'http://localhost:8080';

const config: NextConfig = {
  transpilePackages: ['@travel/api-client', '@travel/i18n', '@travel/ui'],
  typedRoutes: true,

  // Xem chú thích cùng tên ở apps/site/next.config.ts — docs/34 mục 5.1.
  output: 'standalone',
  outputFileTracingRoot: GOC_WORKSPACE,

  // Trang quản trị KHÔNG có locale trong URL: nhân viên dùng một ngôn ngữ làm
  // việc, và bảng điều khiển dịch thuật hiện cả hai bản cạnh nhau trong cùng
  // một màn hình. Locale ở đây là thuộc tính của DỮ LIỆU đang sửa, không phải
  // của người dùng.

  /**
   * Trình duyệt CHỈ nói chuyện với chính máy chủ Next; Next chuyển tiếp sang API.
   *
   * Không phải để tiện — để giữ đúng chính sách cookie của docs/22 mục 9. Nếu
   * trình duyệt gọi thẳng cổng 8080 thì đó là yêu cầu KHÁC ORIGIN, và cookie
   * phiên buộc phải đổi sang `SameSite=None; Secure` mới đi kèm được. Mà
   * `SameSite=Lax` chính là thứ chặn phần lớn tấn công giả mạo yêu cầu, và
   * docs/22 mục 9 đã chốt nó.
   *
   * Đi qua cùng một origin thì cookie `HttpOnly` + `SameSite=Lax` chạy đúng như
   * đã thiết kế, và không phải bật CORS ở phía API — một thứ nữa để cấu hình
   * sai.
   *
   * **`API_URL` bị nướng vào ảnh lúc build, không đọc lúc chạy.** Bản standalone
   * đóng băng cấu hình đã giải trị vào `.next/required-server-files.json`, nên
   * đặt biến này trong `compose.prod.yaml` là vô tác dụng — nó phải là tham số
   * `--build-arg` của `docker build`. Chi tiết: docs/34 mục 5.3.
   */
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${API}/api/:path*` }];
  },
};

export default config;
