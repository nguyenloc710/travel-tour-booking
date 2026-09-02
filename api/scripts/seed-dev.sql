-- Dữ liệu mồi cho môi trường DEV. KHÔNG phải migration — docs/12 mục 8.2.
--
--     psql "$DB_URL" -f api/scripts/seed-dev.sql
--
-- Mọi con số ở đây là DỮ LIỆU GIẢ để màn hình có gì mà hiện. Không dùng làm
-- căn cứ nghiệp vụ, không chép sang production.
--
-- Điều quan trọng nhất của tệp này không phải là số lượng bản ghi mà là các
-- TRƯỜNG HỢP RÌA — nếu dữ liệu mồi không chứa sẵn, sẽ không ai gặp chúng cho
-- tới khi lên production:
--
--   · P2 có bản dịch `da` nhưng KHÔNG có `vi`  → thử chính sách không-fallback
--   · P3 đã dịch đủ nhưng KHÔNG bán ở VN        → thử cổng chặn product_market
--   · Ngày khởi hành đủ OPEN / FEW_SEATS / SOLD_OUT / PENDING
--   · Một điểm đến ĐÃ XOÁ MỀM             → bắt truy vấn quên lọc soft_delete
--
-- Chạy trong MỘT transaction: trigger ct_product_source_translation là
-- DEFERRABLE INITIALLY DEFERRED, nên product và bản dịch nguồn của nó phải nằm
-- trong cùng một transaction. Chạy từng câu ở chế độ autocommit sẽ đỏ ngay ở
-- câu INSERT INTO product đầu tiên.

BEGIN;

-- ---------------------------------------------------------------- thị trường
-- Dữ liệu tra cứu (R__) để VN ở is_active = FALSE vì sáu con số nghiệp vụ của
-- thị trường đó chưa ai quyết (docs/41 mục 4, Q-2). API trả 404 cho thị trường
-- đang tắt, nên trang tiếng Việt ở máy dev sẽ trắng nếu không bật lên.
--
-- Bật ở ĐÂY chứ không ở R__: đây là dữ liệu dev, còn R__ chạy cả trên
-- production. Mở bán VN thật là một quyết định nghiệp vụ, không phải một dòng
-- SQL lẫn vào migration.
UPDATE market SET is_active = TRUE WHERE code = 'VN';

-- ------------------------------------------------- loại khách, điểm khởi hành
-- Tỷ lệ giảm để 0: mức giảm theo loại khách chưa chốt (docs/41 Q-2).

INSERT INTO pax_type (id, market, code, min_age, max_age, discount_rate, sort_order) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'DK', 'ADULT',      12, NULL, 0, 1),
  ('a0000000-0000-4000-8000-000000000002', 'DK', 'CHILD',       2,   11, 0, 2),
  ('a0000000-0000-4000-8000-000000000003', 'DK', 'INFANT',      0,    1, 0, 3),
  -- Thị trường VN có bộ loại khách RIÊNG, không dùng chung với DK: khoá duy nhất
  -- là (market, code), và mức giảm của từng thị trường do người của thị trường
  -- đó quyết định. Con số dưới đây là giả — sáu con số thật của VN chặn ở Q-2.
  ('a0000000-0000-4000-8000-000000000004', 'VN', 'ADULT',      12, NULL, 0, 1),
  ('a0000000-0000-4000-8000-000000000005', 'VN', 'CHILD',       2,   11, 0, 2)
-- Khoá duy nhất là index BỘ PHẬN, nên ON CONFLICT phải nhắc lại điều kiện của
-- index thì Postgres mới nhận ra nó — docs/12 mục 2.2.
ON CONFLICT (market, code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO departure_origin (id, market, city, iata_code, surcharge, is_default) VALUES
  ('b0000000-0000-4000-8000-000000000001', 'DK', 'København', 'CPH',    0, TRUE),
  ('b0000000-0000-4000-8000-000000000002', 'DK', 'Billund',   'BLL', 1200, FALSE)
ON CONFLICT (market, city) WHERE NOT soft_delete DO NOTHING;

-- ---------------------------------------------------------------- nhân sự

-- Sáu tài khoản, MẬT KHẨU CỦA CẢ SÁU LÀ  password
--
-- Trước đây cột này là chuỗi 'x', nên không tài khoản mồi nào đăng nhập được và
-- toàn bộ bề mặt quản trị không thử tay được trên máy dev.
--
-- Băm bằng bcrypt, không lưu mật khẩu thô — đúng cơ chế thật (docs/22 mục 9).
-- Cả sáu dùng chung một chuỗi băm vì đây là dữ liệu giả cho localhost.
--
--   !!! KHÔNG BAO GIỜ chạy tệp này trên production. Sáu tài khoản dưới đây có
--   !!! mật khẩu ai đọc repo cũng biết, và một trong số đó là ADMIN.
--
-- Bốn tài khoản đầu phủ đúng bốn vai trò của docs/22 mục 2.1, để ma trận quyền
-- thử được bằng tay chứ không chỉ bằng test. Hai tài khoản cuối là TRƯỜNG HỢP
-- RÌA, cùng tinh thần với P2 và P3 ở trên:
--
--   · ca-hai@  mang HAI vai trò — docs/22 mục 2 nói rõ một người vừa viết vừa
--     dịch là chuyện thường ở công ty quy mô này, và quyền vẫn tính theo vai trò
--   · da-nghi@ có is_active = FALSE — vô hiệu hoá người dùng là đặt cờ, KHÔNG
--     xoá bản ghi, vì người đó còn nằm trong created_by của hàng trăm dòng khác
--
INSERT INTO staff_user (id, email, display_name, password_hash, is_active) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'bien-tap@example.test',  'Biên tập viên',      '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE),
  ('c0000000-0000-4000-8000-000000000002', 'tu-van@example.test',    'Tư vấn viên',        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE),
  ('c0000000-0000-4000-8000-000000000003', 'bien-dich@example.test', 'Biên dịch viên',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE),
  ('c0000000-0000-4000-8000-000000000004', 'quan-tri@example.test',  'Quản trị viên',      '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE),
  ('c0000000-0000-4000-8000-000000000005', 'ca-hai@example.test',    'Vừa viết vừa dịch',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE),
  ('c0000000-0000-4000-8000-000000000006', 'da-nghi@example.test',   'Đã nghỉ việc',       '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', FALSE)
-- DO UPDATE chứ không DO NOTHING: chạy lại tệp này phải SỬA ĐƯỢC chuỗi băm cũ.
-- Với DO NOTHING thì một CSDL dev đã có sẵn dòng 'x' sẽ giữ nguyên nó mãi mãi,
-- và người chạy lại seed vẫn không đăng nhập được — đúng cái bẫy vừa gặp.
ON CONFLICT (email) WHERE NOT soft_delete DO UPDATE SET
  display_name  = EXCLUDED.display_name,
  password_hash = EXCLUDED.password_hash,
  is_active     = EXCLUDED.is_active;

INSERT INTO staff_user_role (id, staff_user_id, role_code) VALUES
  ('c0000000-0000-4000-8000-0000000000e1', 'c0000000-0000-4000-8000-000000000001', 'EDITOR'),
  ('c0000000-0000-4000-8000-0000000000e2', 'c0000000-0000-4000-8000-000000000002', 'CONSULTANT'),
  ('c0000000-0000-4000-8000-0000000000e3', 'c0000000-0000-4000-8000-000000000003', 'TRANSLATOR'),
  ('c0000000-0000-4000-8000-0000000000e4', 'c0000000-0000-4000-8000-000000000004', 'ADMIN'),
  ('c0000000-0000-4000-8000-0000000000e5', 'c0000000-0000-4000-8000-000000000005', 'EDITOR'),
  ('c0000000-0000-4000-8000-0000000000e6', 'c0000000-0000-4000-8000-000000000005', 'TRANSLATOR'),
  ('c0000000-0000-4000-8000-0000000000e7', 'c0000000-0000-4000-8000-000000000006', 'ADMIN')
ON CONFLICT (staff_user_id, role_code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO consultant (id, market, full_name, email) VALUES
  ('c0000000-0000-4000-8000-000000000011', 'DK', 'Mette Sørensen', 'mette@example.test')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- địa lý

INSERT INTO region (id, code, sort_order) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'NORTH',   1),
  ('d0000000-0000-4000-8000-000000000002', 'CENTRAL', 2),
  ('d0000000-0000-4000-8000-000000000003', 'SOUTH',   3)
ON CONFLICT (code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO region_translation (region_id, locale, slug, name) VALUES
  ('d0000000-0000-4000-8000-000000000001', 'da', 'nordvietnam',   'Nordvietnam'),
  ('d0000000-0000-4000-8000-000000000001', 'vi', 'mien-bac',      'Miền Bắc'),
  ('d0000000-0000-4000-8000-000000000002', 'da', 'centralvietnam','Det centrale Vietnam'),
  ('d0000000-0000-4000-8000-000000000002', 'vi', 'mien-trung',    'Miền Trung'),
  ('d0000000-0000-4000-8000-000000000003', 'da', 'sydvietnam',    'Sydvietnam'),
  ('d0000000-0000-4000-8000-000000000003', 'vi', 'mien-nam',      'Miền Nam')
ON CONFLICT DO NOTHING;

INSERT INTO destination (id, region_id, code, sort_order) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'HANOI',    1),
  ('e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', 'HALONG',   2),
  ('e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002', 'HOI_AN',   1),
  ('e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000003', 'MEKONG',   1)
ON CONFLICT (code) WHERE NOT soft_delete DO NOTHING;

-- Slug không dấu ở CẢ HAI ngôn ngữ: 'hoi-an' chứ không 'hội-an'.
INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'da', 'hanoi',        'Hanoi'),
  ('e0000000-0000-4000-8000-000000000001', 'vi', 'ha-noi',       'Hà Nội'),
  ('e0000000-0000-4000-8000-000000000002', 'da', 'halong-bugten','Halong-bugten'),
  ('e0000000-0000-4000-8000-000000000002', 'vi', 'vinh-ha-long', 'Vịnh Hạ Long'),
  ('e0000000-0000-4000-8000-000000000003', 'da', 'hoi-an',       'Hoi An'),
  ('e0000000-0000-4000-8000-000000000003', 'vi', 'hoi-an-vi',    'Hội An'),
  ('e0000000-0000-4000-8000-000000000004', 'da', 'mekong-deltaet','Mekong-deltaet'),
  ('e0000000-0000-4000-8000-000000000004', 'vi', 'dong-bang-song-cuu-long', 'Đồng bằng sông Cửu Long')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- sản phẩm

-- P1 — GROUP_TOUR, dịch đủ hai ngôn ngữ, bán ở cả hai thị trường.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, consultant_id, created_by, last_modified_by) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'GROUP_TOUR',
   'e0000000-0000-4000-8000-000000000001', 16, '/img/p1.jpg',
   'c0000000-0000-4000-8000-000000000011',
   'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001');

INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                tour_leader_language, fitness_level) VALUES
  ('f0000000-0000-4000-8000-000000000001', 10, 22, 10, 'da', 2);

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'da', 'vietnam-fra-nord-til-syd',
   'Vietnam fra nord til syd', 'Rundrejse med dansk rejseleder',
   ARRAY['Første afsnit på dansk.','Andet afsnit på dansk.'],
   ARRAY['Dansk rejseleder','Små grupper','Alle måltider'],
   'Solnedgang over Halong-bugten', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000001', 'vi', 'viet-nam-tu-bac-vao-nam',
   'Việt Nam từ Bắc vào Nam', 'Tour đoàn có hướng dẫn viên',
   ARRAY['Đoạn thứ nhất tiếng Việt.','Đoạn thứ hai tiếng Việt.'],
   ARRAY['Hướng dẫn viên tiếng Việt','Đoàn nhỏ','Bao trọn bữa ăn'],
   'Hoàng hôn trên Vịnh Hạ Long', 'PUBLISHED', now());

-- price_from KHÔNG đặt tay từ migration V5: nó là cột vật chất hoá do trigger
-- sở hữu, tính từ departure_price của khách ADULT ở phòng đôi. Đặt tay ở đây thì
-- trigger ghi đè ngay lúc INSERT, và dữ liệu mồi sẽ nói dối về cách hệ thống chạy.
INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000001', 'VN', TRUE, now());

-- P2 — CRUISE, CHỈ có bản dịch `da`. Phải BIẾN MẤT khỏi mọi thứ của locale vi:
-- listing, tìm kiếm, sitemap; URL trả 404. Không hiện bản da thay thế.
INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'CRUISE',
   'e0000000-0000-4000-8000-000000000002', 3, '/img/p2.jpg');

INSERT INTO product_cruise (product_id, ship_name, port_count) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'Emeraude Classic', 4);

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'da', 'krydstogt-halong-bugten',
   'Krydstogt i Halong-bugten', 'To nætter om bord',
   ARRAY['Første afsnit.','Andet afsnit.'],
   ARRAY['Fire kabinekategorier','Alle måltider','Kajak'],
   'Skib i Halong-bugten', 'PUBLISHED', now());

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'DK', TRUE, now());

-- P3 — INDIVIDUAL_PACKAGE, dịch đủ nhưng KHÔNG có dòng product_market cho VN.
-- Cổng chặn thị trường: sản phẩm này không tồn tại ở VN dù đã dịch xong.
INSERT INTO product (id, product_type, primary_destination_id, duration_days, hero_image) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'INDIVIDUAL_PACKAGE',
   'e0000000-0000-4000-8000-000000000003', 9, '/img/p3.jpg');

INSERT INTO product_individual (product_id, min_party_size, flexible_date_window_days) VALUES
  ('f0000000-0000-4000-8000-000000000003', 2, 14);

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'da', 'hoi-an-paa-egen-haand',
   'Hoi An på egen hånd', 'Individuel rejse',
   ARRAY['Første afsnit.','Andet afsnit.'],
   ARRAY['Egen tidsplan','Privat chauffør','Central bolig'],
   'Lanterner i Hoi An', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000003', 'vi', 'hoi-an-tu-tuc',
   'Hội An tự túc', 'Tour cá nhân',
   ARRAY['Đoạn thứ nhất.','Đoạn thứ hai.'],
   ARRAY['Tự chọn lịch trình','Xe riêng','Ở trung tâm'],
   'Đèn lồng Hội An', 'PUBLISHED', now());

-- P3 KHÔNG có ngày khởi hành nào, nên nó cũng không có price_from và website
-- hiện "Liên hệ". Đó không phải thiếu sót của dữ liệu mồi mà là một câu hỏi thật:
-- loại không có lịch khởi hành thì "giá từ" lấy ở đâu. Ghi ở docs/12 mục 10.
INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'DK', TRUE, now());

-- ------------------------------------------------- ngày khởi hành, đủ trạng thái
-- GUARANTEED không có ở đây và không bao giờ có: nó tính từ seats_booked >= min_pax.

INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                       base_status, capacity, seats_booked, departure_origin_id) VALUES
  ('11110000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000001',
   'DK', DATE '2027-03-14', DATE '2027-03-29', 16, 'OPEN',      22,  6,
   'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000001',
   'DK', DATE '2027-04-11', DATE '2027-04-26', 16, 'FEW_SEATS', 22, 20,
   'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000001',
   'DK', DATE '2027-05-09', DATE '2027-05-24', 16, 'SOLD_OUT',  22, 22,
   'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000004', 'f0000000-0000-4000-8000-000000000001',
   'DK', DATE '2027-06-06', DATE '2027-06-21', 16, 'PENDING',   22,  2,
   'b0000000-0000-4000-8000-000000000001'),
  -- Cùng lộ trình, thị trường VN: đoàn KHÁC, khởi hành từ nơi khác. ADR-006.
  ('11110000-0000-4000-8000-000000000005', 'f0000000-0000-4000-8000-000000000001',
   'VN', DATE '2027-03-14', DATE '2027-03-29', 16, 'OPEN',      30,  4, NULL);

-- CRUISE: bốn hạng cabin trong CÙNG một ngày — bốn dòng, phân biệt bằng cabin.
INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                       cabin_category, base_status, capacity, seats_booked) VALUES
  ('11110000-0000-4000-8000-000000000011', 'f0000000-0000-4000-8000-000000000002',
   'DK', DATE '2027-03-20', DATE '2027-03-22', 3, 'INSIDE',  'OPEN', 8, 2),
  ('11110000-0000-4000-8000-000000000012', 'f0000000-0000-4000-8000-000000000002',
   'DK', DATE '2027-03-20', DATE '2027-03-22', 3, 'OUTSIDE', 'OPEN', 8, 1),
  ('11110000-0000-4000-8000-000000000013', 'f0000000-0000-4000-8000-000000000002',
   'DK', DATE '2027-03-20', DATE '2027-03-22', 3, 'BALCONY', 'FEW_SEATS', 4, 3),
  ('11110000-0000-4000-8000-000000000014', 'f0000000-0000-4000-8000-000000000002',
   'DK', DATE '2027-03-20', DATE '2027-03-22', 3, 'AQUA',    'SOLD_OUT', 2, 2);

-- Trường hợp rìa thứ tư: một điểm đến ĐÃ XOÁ MỀM. Nó vẫn nằm trong bảng và vẫn
-- giữ chỗ trong mọi khoá duy nhất không bộ phận — dùng để phát hiện truy vấn nào
-- quên lọc `AND NOT soft_delete`.
INSERT INTO destination (id, region_id, code, sort_order, soft_delete, last_modified_by) VALUES
  ('e0000000-0000-4000-8000-000000000099', 'd0000000-0000-4000-8000-000000000003',
   'DA_XOA', 99, TRUE, 'c0000000-0000-4000-8000-000000000001')
ON CONFLICT (code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO destination_translation (destination_id, locale, slug, name, soft_delete) VALUES
  ('e0000000-0000-4000-8000-000000000099', 'da', 'slug-da-xoa', 'Slettet destination', TRUE),
  ('e0000000-0000-4000-8000-000000000099', 'vi', 'slug-da-xoa-vi', 'Điểm đến đã xoá', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO departure_price (departure_id, pax_type_id, occupancy, amount, currency) VALUES
  -- P1 thị trường DK. Bốn ngày cùng giá cho gọn; thực tế mỗi mùa một giá.
  ('11110000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 24990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
   'SINGLE', 29990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 25990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 25990.00, 'DKK'),

  -- P1 thị trường VN: SỐ KHÁC HẲN, không phải 24990 nhân tỷ giá. Tour bán cho
  -- khách Đan gồm vé bay quốc tế, bán cho khách Việt thì không — CLAUDE.md điều 4.
  ('11110000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000004',
   'DOUBLE', 18900000, 'VND'),

  -- P2 CRUISE: mỗi hạng cabin một dòng departure riêng, nên giá cũng theo dòng đó.
  ('11110000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 6490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 7490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 8990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 11990.00, 'DKK');

COMMIT;
