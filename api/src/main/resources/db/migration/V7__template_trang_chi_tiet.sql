-- V7 — template trang chi tiết: biên tập viên chọn bộ khung cho từng sản phẩm.
--
-- Tới nay trang chi tiết chỉ có MỘT khung, khác nhau bằng công tắc bật/tắt theo
-- `product_type` nằm trong mã frontend. Thêm một biến thể là thêm một nhánh `if`
-- vào một component, và không ai ngoài lập trình viên đổi được. Cột này chuyển
-- lựa chọn đó sang cho người biên tập, giống cách WordPress cho chọn template
-- của một trang.
--
-- VÌ SAO LÀ TEXT TỰ DO CHỨ KHÔNG PHẢI ENUM HAY BẢNG THAM CHIẾU.
--
-- Danh mục template là thứ của frontend: thêm một template là thêm một component
-- và một dòng trong bảng đăng ký ở `web/`. Nếu CSDL cũng giữ danh mục ấy thì mỗi
-- template mới cần một migration, và hai danh mục sẽ lệch nhau vào đúng ngày ai
-- đó quên. Cùng lý do đã khiến `code` của `ErrorResponse` là chuỗi tự do.
--
-- Cái giá phải nhận: CSDL không cưỡng chế được giá trị hợp lệ. Nhận có ý thức —
-- frontend rơi về template mặc định của loại khi gặp giá trị lạ hoặc NULL. Đó là
-- hành vi bắt buộc chứ không phải phòng thủ thừa: gỡ một template đi mà sản phẩm
-- cũ còn trỏ tới là chuyện chắc chắn xảy ra, và nó không được phép làm trắng một
-- trang đang bán hàng.
--
-- KHÔNG đặt mặc định. NULL nghĩa là "chưa ai chọn", và nó phải khác với "đã chọn
-- đúng cái đang là mặc định": ngày đổi template mặc định của một loại, nhóm thứ
-- nhất phải đi theo, nhóm thứ hai phải đứng yên. Một giá trị mặc định ở tầng CSDL
-- xoá mất phân biệt đó ngay lúc chèn dòng.

ALTER TABLE product
  ADD COLUMN layout VARCHAR(40);

COMMENT ON COLUMN product.layout IS
  'Template trang chi tiết do biên tập viên chọn. NULL = chưa chọn, dùng mặc định của loại. Danh mục do frontend giữ — xem docs/05.';

-- Không index: cột này chỉ được đọc kèm chính dòng sản phẩm đó, không bao giờ
-- là điều kiện lọc. Index ở đây là chi phí ghi không đổi lại được gì.
