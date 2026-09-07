import type { NextConfig } from 'next';

const API = process.env.API_URL ?? 'http://localhost:8080';

const config: NextConfig = {
  transpilePackages: ['@travel/api-client', '@travel/i18n', '@travel/ui'],
  typedRoutes: true,

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
   */
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${API}/api/:path*` }];
  },
};

export default config;
