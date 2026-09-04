-- V6 — yêu cầu báo giá của khách, gắn thẳng vào bảng `quote`.
--
-- docs/14 mục 7 mô tả vòng đời báo giá bắt đầu ở DRAFT, và docs/23 mục 7 vẽ
-- luồng bắt đầu bằng "khách gửi yêu cầu". Bảng `quote` của V1 đã có đủ phần
-- SAU của luồng đó — trạng thái, tổng tiền, hạn hiệu lực, và `quote_line` chụp
-- lại bảng giá — nhưng không có chỗ nào giữ phần ĐẦU: khách là ai, muốn đi
-- ngày nào, yêu cầu riêng là gì.
--
-- VÌ SAO KHÔNG LÀM BẢNG `quote_request` RIÊNG. Một bảng riêng thì mỗi yêu cầu
-- sinh ra hai dòng ở hai bảng, nối bằng khoá ngoại một-một, và mọi truy vấn của
-- màn hình M8 phải JOIN chúng lại. Cái được duy nhất là "yêu cầu chưa dựng giá"
-- và "báo giá đã dựng" nằm riêng — mà đó đúng là thứ cột `status` đã nói: DRAFT
-- là yêu cầu chưa ai chạm tới. **Yêu cầu của khách CHÍNH LÀ bản nháp báo giá**,
-- không phải một thực thể khác.
--
-- Hệ quả phải nhận: một `quote` ở DRAFT có `total`, `currency` và `valid_until`
-- đều NULL. Cả ba đã NULL được từ V1 nên không phải nới ràng buộc nào — V1 đã
-- lường trước tình huống này, chỉ chưa có đường ghi.

-- ---------------------------------------------------------- 1. Người yêu cầu
--
-- Ba cột liên hệ theo đúng khuôn `booking`: cùng kiểu, cùng độ dài. Khác một
-- chỗ — `booking` không lưu tên người liên hệ vì tên nằm ở `booking_passenger`,
-- còn báo giá thì chưa có hành khách nào, nên tên phải nằm ở đây.
--
-- NOT NULL đặt qua hai bước với DEFAULT tạm rồi bỏ DEFAULT: bảng có thể đã có
-- dòng ở môi trường nào đó, và để DEFAULT lại thì dòng mới thiếu liên hệ sẽ đi
-- lọt vào CSDL dưới dạng chuỗi rỗng thay vì bị từ chối.
ALTER TABLE quote
  ADD COLUMN contact_name  VARCHAR(160) NOT NULL DEFAULT '',
  ADD COLUMN contact_email VARCHAR(320) NOT NULL DEFAULT '',
  ADD COLUMN contact_phone VARCHAR(32)  NOT NULL DEFAULT '';

ALTER TABLE quote
  ALTER COLUMN contact_name  DROP DEFAULT,
  ALTER COLUMN contact_email DROP DEFAULT,
  ALTER COLUMN contact_phone DROP DEFAULT;

-- ------------------------------------------------------------ 2. Nội dung yêu cầu
--
-- `requested_date` để NULL được: khách chưa chốt ngày vẫn hỏi giá được, và bắt
-- điền một ngày giả để qua form là cách chắc chắn nhất để có dữ liệu sai.
--
-- `message` là TEXT không giới hạn ở CSDL; giới hạn 2000 ký tự nằm ở spec, nơi
-- nó là quy tắc giao diện chứ không phải quy tắc lưu trữ.
ALTER TABLE quote
  ADD COLUMN requested_date DATE,
  ADD COLUMN message        TEXT,
  -- Lúc gửi cho khách. `valid_until` suy ra từ đây cộng `quote_valid_days`
  -- (docs/14 mục 7 quy tắc 2), nhưng vẫn lưu cả hai: quy tắc 4 cấm gia hạn, nên
  -- ngày hết hạn phải là một sự thật đã chốt, không phải một phép tính đọc lại
  -- `product_private` — sửa `quote_valid_days` hôm nay không được làm đổi hạn
  -- của báo giá đã gửi tuần trước.
  ADD COLUMN sent_at        TIMESTAMPTZ;

-- Gửi rồi thì phải có hạn, và chưa gửi thì không được có. Ràng buộc này là thứ
-- giữ cho quy tắc 2 đúng ở tầng CSDL chứ không chỉ trong một hàm Java.
ALTER TABLE quote
  ADD CONSTRAINT ck_quote_sent CHECK (
    (status = 'DRAFT' AND sent_at IS NULL AND valid_until IS NULL)
    OR (status <> 'DRAFT' AND sent_at IS NOT NULL AND valid_until IS NOT NULL)
  );

-- ------------------------------------------------------------------ 3. Index
--
-- Hàng đợi việc của M8: lọc theo trạng thái, sắp cũ nhất trước. Bộ phận theo
-- soft_delete cùng lý do với mọi index khác ở docs/12 mục 2.2.
CREATE INDEX ix_quote_hang_doi
  ON quote (status, created_at) WHERE NOT soft_delete;

-- Tìm theo email khách — tổng đài tra "khách này đã hỏi giá bao giờ chưa".
CREATE INDEX ix_quote_contact_email
  ON quote (lower(contact_email)) WHERE NOT soft_delete;
