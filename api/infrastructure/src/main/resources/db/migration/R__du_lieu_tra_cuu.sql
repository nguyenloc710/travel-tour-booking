-- Dữ liệu tra cứu — migration lặp lại được (docs/12 mục 8.1).
--
-- Đây là dữ liệu mà CODE PHỤ THUỘC VÀO, nên nó thuộc về migration chứ không
-- phải nhập tay. Chạy lại được nhiều lần: mọi câu đều ON CONFLICT DO UPDATE.
--
-- Cố tình CHƯA nạp `pax_type` và `departure_origin` ở đây: loại khách và điểm
-- khởi hành của từng thị trường là dữ liệu nghiệp vụ chưa chốt (docs/41 Q-2).
-- Nạp số bịa vào migration là biến phỏng đoán thành sự thật của hệ thống.
-- Tạm thời chúng nằm trong api/scripts/seed-dev.sql, nơi đã ghi rõ là dữ liệu giả.

-- ---------------------------------------------------------------- locale

INSERT INTO locale (code, collation_name, is_source, is_active) VALUES
  ('da', 'da-DK-x-icu', TRUE,  TRUE),   -- ngôn ngữ NGUỒN — ADR-004
  ('vi', 'vi-VN-x-icu', FALSE, TRUE)
ON CONFLICT (code) DO UPDATE SET
  collation_name = EXCLUDED.collation_name,
  is_source = EXCLUDED.is_source,
  is_active = EXCLUDED.is_active;

-- ---------------------------------------------------------------- market
--
-- DK: tỷ lệ đặt cọc 25% và phí xử lý 295 kr theo docs/14 mục 2.4.
-- VN: ba ô "chưa chốt" ở cùng bảng đó đang chặn phần tính giá của thị trường
--     này (docs/41 Q-2). Để is_active = FALSE cho tới khi có số thật — giá trị 0
--     bên dưới là chỗ trống, KHÔNG phải quyết định nghiệp vụ.

INSERT INTO market (code, currency, fraction_digits, default_locale,
                    deposit_rate, processing_fee, is_active) VALUES
  ('DK', 'DKK', 2, 'da', 0.2500, 295.00, TRUE),
  ('VN', 'VND', 0, 'vi', 0.0000,   0.00, FALSE)
ON CONFLICT (code) DO UPDATE SET
  currency        = EXCLUDED.currency,
  fraction_digits = EXCLUDED.fraction_digits,
  default_locale  = EXCLUDED.default_locale,
  deposit_rate    = EXCLUDED.deposit_rate,
  processing_fee  = EXCLUDED.processing_fee,
  is_active       = EXCLUDED.is_active;

-- ---------------------------------------------------------------- role
--
-- Bốn vai trò của docs/13 mục 10, ma trận quyền ở docs/22 mục 2.1. Đây là dữ
-- liệu mà code phụ thuộc vào — thêm vai trò thứ năm là việc đi qua tài liệu
-- rồi qua migration, không phải một câu INSERT trên production.

INSERT INTO role (code, sort_order) VALUES
  ('CONSULTANT', 1),
  ('EDITOR',     2),
  ('TRANSLATOR', 3),
  ('ADMIN',      4)
ON CONFLICT (code) DO UPDATE SET sort_order = EXCLUDED.sort_order;
