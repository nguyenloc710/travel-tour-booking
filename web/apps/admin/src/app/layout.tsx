import type { Metadata } from 'next';
import './globals.css';
import { ThanhTren } from '@/components/ThanhTren';

/**
 * Không trang quản trị nào được dựng sẵn lúc build.
 *
 * docs/22 mục 1: mọi phản hồi của bề mặt quản trị là `no-store` — nội dung chưa
 * xuất bản không được nằm trong bất kỳ cache nào. Dựng sẵn một trang lúc build
 * là đúng thứ đó, chỉ khác tên gọi.
 */
export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Quản trị — Vietnamrejser',
  // Nội dung chưa xuất bản không được nằm trong bất kỳ cache nào, kể cả chỉ mục
  // của công cụ tìm kiếm (docs/22 mục 1).
  robots: { index: false, follow: false },
};

/**
 * Khung của trang quản trị.
 *
 * Giao diện dùng **một ngôn ngữ** — tiếng Việt — tách hẳn khỏi locale của dữ
 * liệu đang sửa (docs/22 mục 8). Chuỗi giao diện viết thẳng trong component chứ
 * không nằm trong catalog của website khách: trộn vào là bộ kiểm độ phủ đòi dịch
 * cả những nhãn mà khách không bao giờ nhìn thấy.
 */
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>
        <ThanhTren />
        {children}
      </body>
    </html>
  );
}
