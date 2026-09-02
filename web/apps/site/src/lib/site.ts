/**
 * Địa chỉ gốc của website khách.
 *
 * Sitemap và `robots.txt` bắt buộc dùng **URL tuyệt đối** — công cụ tìm kiếm đọc
 * chúng ngoài ngữ cảnh trang, nên đường dẫn tương đối không có nghĩa gì.
 *
 * Chưa có tên miền thật (`docs/34` mục 1: môi trường `prod` chưa dựng), nên mặc
 * định là địa chỉ máy dev. Đặt `NEXT_PUBLIC_SITE_URL` lúc triển khai.
 *
 * **Đây là thứ phải sửa trước lần triển khai thật đầu tiên.** Sitemap trỏ vào
 * `localhost` thì công cụ tìm kiếm bỏ qua toàn bộ, và không có lỗi nào nổ.
 */
export const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000').replace(
  /\/$/,
  '',
);
