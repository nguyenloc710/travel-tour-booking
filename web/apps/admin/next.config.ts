import type { NextConfig } from 'next';

const config: NextConfig = {
  transpilePackages: ['@travel/api-client', '@travel/i18n', '@travel/ui'],
  typedRoutes: true,

  // Trang quản trị KHÔNG có locale trong URL: nhân viên dùng một ngôn ngữ làm
  // việc, và bảng điều khiển dịch thuật hiện cả hai bản cạnh nhau trong cùng
  // một màn hình. Locale ở đây là thuộc tính của DỮ LIỆU đang sửa, không phải
  // của người dùng.
};

export default config;
