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
  ('e0000000-0000-4000-8000-000000000003', 'vi', 'hoi-an',       'Hội An'),
  ('e0000000-0000-4000-8000-000000000004', 'da', 'mekong-deltaet','Mekong-deltaet'),
  ('e0000000-0000-4000-8000-000000000004', 'vi', 'dong-bang-song-cuu-long', 'Đồng bằng sông Cửu Long')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- sản phẩm

-- P1 — GROUP_TOUR, dịch đủ hai ngôn ngữ, bán ở cả hai thị trường.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, map_image, rating, review_count,
                     consultant_id, created_by, last_modified_by) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'GROUP_TOUR',
   'e0000000-0000-4000-8000-000000000001', 16, '/img/tour/p01-bac-nam.jpg',
   NULL, 4.6, 87,
   'c0000000-0000-4000-8000-000000000011',
   'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                tour_leader_language, fitness_level) VALUES
  ('f0000000-0000-4000-8000-000000000001', 10, 22, 10, 'da', 2)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'da', 'vietnam-fra-nord-til-syd',
   'Vietnam fra nord til syd', 'Seksten dage fra Hanoi til Mekong, med dansk rejseleder',
   ARRAY['Landet er halvandet tusind kilometer langt, og de to ender ligner ikke hinanden. Nord er køligt og bjergrigt, syd er fladt og varmt hele året.',
         'Vi tager begge dele på seksten dage, med nattog i midten og fire dage i deltaet til sidst.',
         'Gruppen er på højst 22, og rejselederen er dansk hele vejen.'],
   ARRAY['Dansk rejseleder hele vejen','Højst 22 rejsende','Overnatning i Halong-bugten','Alle måltider undtagen tre aftener','Nattog i stedet for indenrigsfly'],
   'Kalkstensøer i Halong-bugten set fra luften', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000001', 'vi', 'viet-nam-tu-bac-vao-nam',
   'Việt Nam từ Bắc vào Nam', 'Mười sáu ngày từ Hà Nội xuống Cửu Long, có trưởng đoàn',
   ARRAY['Đất nước dài một nghìn rưỡi cây số, và hai đầu không giống nhau. Phía bắc mát và nhiều núi, phía nam phẳng và nóng quanh năm.',
         'Chúng tôi đi cả hai trong mười sáu ngày, giữa chặng là tàu đêm, cuối chặng là bốn ngày ở đồng bằng.',
         'Đoàn tối đa 22 người, trưởng đoàn theo suốt hành trình.'],
   ARRAY['Trưởng đoàn theo suốt hành trình','Tối đa 22 khách','Ngủ đêm trên vịnh Hạ Long','Bao bữa, trừ ba buổi tối','Đi tàu đêm thay vì bay nội địa'],
   'Đảo đá vôi trên vịnh Hạ Long nhìn từ trên cao', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

-- price_from KHÔNG đặt tay từ migration V5: nó là cột vật chất hoá do trigger
-- sở hữu, tính từ departure_price của khách ADULT ở phòng đôi. Đặt tay ở đây thì
-- trigger ghi đè ngay lúc INSERT, và dữ liệu mồi sẽ nói dối về cách hệ thống chạy.
INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000001', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- P2 — CRUISE, CHỈ có bản dịch `da`. Phải BIẾN MẤT khỏi mọi thứ của locale vi:
-- listing, tìm kiếm, sitemap; URL trả 404. Không hiện bản da thay thế.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, rating, review_count) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'CRUISE',
   'e0000000-0000-4000-8000-000000000002', 3, '/img/tour/p02-du-thuyen-ha-long.jpg',
   4.8, 34)
ON CONFLICT DO NOTHING;

INSERT INTO product_cruise (product_id, ship_name, port_count) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'Emeraude Classic', 4)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'da', 'krydstogt-halong-bugten',
   'Krydstogt i Halong-bugten', 'To nætter om bord mellem kalkstensklipperne',
   ARRAY['Bugten har halvandet tusind klippeøer, og de fleste af dem har ingen på sig.',
         'Skibet ligger stille om natten. Man vågner til vand og tåge og ingenting andet.'],
   ARRAY['Fire kabinekategorier','Alle måltider ombord','Kajak i en lukket bugt','Højst 30 gæster'],
   'Skib i Halong-bugten', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000002', 'DK', TRUE, now())
ON CONFLICT DO NOTHING;

-- P3 — INDIVIDUAL_PACKAGE, dịch đủ nhưng KHÔNG có dòng product_market cho VN.
-- Cổng chặn thị trường: sản phẩm này không tồn tại ở VN dù đã dịch xong.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, rating, review_count) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'INDIVIDUAL_PACKAGE',
   'e0000000-0000-4000-8000-000000000003', 9, '/img/tour/p03-hoi-an.jpg',
   4.2, 19)
ON CONFLICT DO NOTHING;

INSERT INTO product_individual (product_id, min_party_size, flexible_date_window_days) VALUES
  ('f0000000-0000-4000-8000-000000000003', 2, 14)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'da', 'hoi-an-paa-egen-haand',
   'Hoi An på egen hånd', 'Ni dage uden gruppe, uden fast program',
   ARRAY['Hoi An er lille nok til at man kan gå overalt, og gammel nok til at det er værd at gå langsomt.',
         'Vi står for hotel, transport og de aftaler I vil have. Resten af tiden er jeres.'],
   ARRAY['Egen tidsplan','Privat chauffør på de dage I vil','Hotel midt i den gamle bydel','Fri afrejsedato'],
   'Lanterner i Hoi An', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000003', 'vi', 'hoi-an-tu-tuc',
   'Hội An tự túc', 'Chín ngày không đoàn, không chương trình cố định',
   ARRAY['Hội An đủ nhỏ để đi bộ khắp nơi, và đủ cũ để đáng đi chậm.',
         'Chúng tôi lo khách sạn, xe cộ và những buổi hẹn bạn muốn có. Thời gian còn lại là của bạn.'],
   ARRAY['Tự chọn lịch trình','Xe riêng vào những ngày bạn cần','Khách sạn giữa phố cổ','Ngày khởi hành tự chọn'],
   'Đèn lồng Hội An', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

-- P3 KHÔNG có ngày khởi hành nào, nên nó cũng không có price_from và website
-- hiện "Liên hệ". Đó không phải thiếu sót của dữ liệu mồi mà là một câu hỏi thật:
-- loại không có lịch khởi hành thì "giá từ" lấy ở đâu. Ghi ở docs/12 mục 10.
INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'DK', TRUE, now())
ON CONFLICT DO NOTHING;

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
   'VN', DATE '2027-03-14', DATE '2027-03-29', 16, 'OPEN',      30,  4, NULL)
ON CONFLICT DO NOTHING;

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
   'DK', DATE '2027-03-20', DATE '2027-03-22', 3, 'AQUA',    'SOLD_OUT', 2, 2)
ON CONFLICT DO NOTHING;

-- Trường hợp rìa thứ tư: một điểm đến ĐÃ XOÁ MỀM. Nó vẫn nằm trong bảng và vẫn
-- giữ chỗ trong mọi khoá duy nhất không bộ phận — dùng để phát hiện truy vấn nào
-- quên lọc `AND NOT soft_delete`.
INSERT INTO destination (id, region_id, code, sort_order, soft_delete, last_modified_by) VALUES
  ('e0000000-0000-4000-8000-000000000099', 'd0000000-0000-4000-8000-000000000003',
   'DA_XOA', 99, TRUE, 'c0000000-0000-4000-8000-000000000001')
-- KHÔNG nhắc lại index bộ phận ở đây: dòng này `soft_delete = TRUE` nên index ấy
-- không phủ nó, và xung đột rơi vào khoá chính. Nhắc điều kiện là chạy lần hai đứt.
ON CONFLICT DO NOTHING;

-- `last_modified_by` bắt buộc ở dòng xoá mềm — quy tắc kiểm 18. Xoá mà không
-- biết ai xoá thì cột xoá mềm chỉ trả lời được nửa câu hỏi nó sinh ra để trả lời.
INSERT INTO destination_translation (destination_id, locale, slug, name, soft_delete,
                                     last_modified_by) VALUES
  ('e0000000-0000-4000-8000-000000000099', 'da', 'slug-da-xoa', 'Slettet destination', TRUE,
   'c0000000-0000-4000-8000-000000000001'),
  ('e0000000-0000-4000-8000-000000000099', 'vi', 'slug-da-xoa-vi', 'Điểm đến đã xoá', TRUE,
   'c0000000-0000-4000-8000-000000000001')
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

  -- Ngày 09/05 trước đây KHÔNG có dòng giá nào. Nó `SOLD_OUT` nên không ai đặt
  -- được, nhưng bảng ngày khởi hành ở trang chi tiết vẫn hiện nó — với ô giá
  -- trống. Một ngày không có giá là dữ liệu thiếu, không phải trạng thái bán.
  ('11110000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 26990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001',
   'SINGLE', 32390.00, 'DKK'),

  -- P1 thị trường VN: SỐ KHÁC HẲN, không phải 24990 nhân tỷ giá. Tour bán cho
  -- khách Đan gồm vé bay quốc tế, bán cho khách Việt thì không — CLAUDE.md điều 4.
  ('11110000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000004',
   'DOUBLE', 18900000, 'VND'),

  -- P2 CRUISE: mỗi hạng cabin một dòng departure riêng, nên giá cũng theo dòng đó.
  ('11110000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 6490.00, 'DKK'),
  -- Chênh giữa hai hạng liền nhau phải nằm trong 20–45% (quy tắc kiểm 7).
  -- Bậc đầu trước đây chênh 15.4%, dưới ngưỡng — sửa lên 7990.
  ('11110000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 7990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 9990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000001',
   'DOUBLE', 12990.00, 'DKK')
ON CONFLICT DO NOTHING;

-- --------------------------------------------------- bài viết, thẻ, sự kiện
--
-- Đủ để R9, R10 và sitemap có gì mà hiện. Vẫn giữ đúng trường hợp rìa quan
-- trọng nhất của bộ dữ liệu này: **B3 chỉ có bản `da`**, nên nó biến mất khỏi
-- blog `vi`, khỏi sitemap `vi`, và URL `vi` của nó trả 404 — đúng chính sách
-- không-fallback, và là thứ chỉ lộ ra khi có dữ liệu để thử.

INSERT INTO tag (id, code, sort_order) VALUES
  ('f1000000-0000-4000-8000-000000000001', 'FOOD', 1),
  ('f1000000-0000-4000-8000-000000000002', 'CULTURE', 2),
  ('f1000000-0000-4000-8000-000000000003', 'PRACTICAL', 3)
ON CONFLICT DO NOTHING;

INSERT INTO tag_translation (tag_id, locale, slug, name) VALUES
  ('f1000000-0000-4000-8000-000000000001', 'da', 'mad', 'Mad'),
  ('f1000000-0000-4000-8000-000000000001', 'vi', 'am-thuc', 'Ẩm thực'),
  ('f1000000-0000-4000-8000-000000000002', 'da', 'kultur', 'Kultur'),
  ('f1000000-0000-4000-8000-000000000002', 'vi', 'van-hoa', 'Văn hoá'),
  ('f1000000-0000-4000-8000-000000000003', 'da', 'praktisk', 'Praktisk'),
  ('f1000000-0000-4000-8000-000000000003', 'vi', 'kinh-nghiem', 'Kinh nghiệm')
ON CONFLICT DO NOTHING;

INSERT INTO post (id, hero_image, published_at) VALUES
  ('f2000000-0000-4000-8000-000000000001', '/img/blog/pho.jpg',    '2026-06-02T08:00:00Z'),
  ('f2000000-0000-4000-8000-000000000002', '/img/blog/hoi-an.jpg', '2026-07-14T08:00:00Z'),
  ('f2000000-0000-4000-8000-000000000003', '/img/blog/visum.jpg',  '2026-08-20T08:00:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO post_translation (post_id, locale, slug, title, excerpt, body, status) VALUES
  ('f2000000-0000-4000-8000-000000000001', 'da', 'morgenmad-i-hanoi',
   'Morgenmad i Hanoi', 'Pho til morgenmad lyder mærkeligt indtil man har prøvet det.',
   ARRAY['Klokken er seks om morgenen på et gadehjørne i Hanoi.',
         'Suppen koger fra klokken fire. Det smager man.'], 'PUBLISHED'),
  ('f2000000-0000-4000-8000-000000000001', 'vi', 'bua-sang-o-ha-noi',
   'Bữa sáng ở Hà Nội', 'Ăn phở buổi sáng nghe lạ với khách Bắc Âu, tới khi họ thử.',
   ARRAY['Sáu giờ sáng, một góc phố Hà Nội.',
         'Nồi nước dùng đun từ bốn giờ. Ăn là biết.'], 'PUBLISHED'),

  ('f2000000-0000-4000-8000-000000000002', 'da', 'lanternerne-i-hoi-an',
   'Lanternerne i Hoi An', 'Den fjortende dag i månemåneden slukker byen lyset.',
   ARRAY['En gang om måneden slukker Hoi An gadebelysningen.',
         'Kun lanternerne er tændt, og floden bliver til et spejl.'], 'PUBLISHED'),
  ('f2000000-0000-4000-8000-000000000002', 'vi', 'den-long-hoi-an',
   'Đèn lồng Hội An', 'Ngày rằm, cả phố cổ tắt đèn điện.',
   ARRAY['Mỗi tháng một lần, Hội An tắt đèn đường.',
         'Chỉ còn đèn lồng, và mặt sông thành một tấm gương.'], 'PUBLISHED'),

  -- B3: CHỈ có bản `da`. Nó phải biến mất hoàn toàn khỏi locale `vi`.
  ('f2000000-0000-4000-8000-000000000003', 'da', 'visum-til-vietnam',
   'Visum til Vietnam', 'Reglerne skifter. Her er hvad der gælder i år.',
   ARRAY['Danske statsborgere kan søge e-visum online.',
         'Ansøg mindst tre uger før afrejse.'], 'PUBLISHED')
ON CONFLICT DO NOTHING;

INSERT INTO post_tag (post_id, tag_id) VALUES
  ('f2000000-0000-4000-8000-000000000001', 'f1000000-0000-4000-8000-000000000001'),
  ('f2000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000002'),
  ('f2000000-0000-4000-8000-000000000003', 'f1000000-0000-4000-8000-000000000003')
ON CONFLICT DO NOTHING;

-- Buổi thuyết trình. Ngày đặt xa trong tương lai có chủ ý: endpoint chỉ trả
-- buổi CHƯA diễn ra, nên một ngày gần sẽ làm bộ dữ liệu mồi tự rỗng đi theo
-- thời gian và không ai hiểu vì sao trang sự kiện trống.
INSERT INTO lecture (id, market, event_date, start_time, city, venue, seats, seats_taken) VALUES
  ('f3000000-0000-4000-8000-000000000001', 'DK', '2027-02-11', '19:00',
   'København', 'Kulturhuset Islands Brygge', 60, 41),
  ('f3000000-0000-4000-8000-000000000002', 'DK', '2027-03-04', '19:00',
   'Aarhus', 'Dokk1', 45, 45),
  ('f3000000-0000-4000-8000-000000000003', 'VN', '2027-03-20', '18:30',
   'Hà Nội', 'Trung tâm Văn hoá Pháp', 80, 12)
ON CONFLICT DO NOTHING;

INSERT INTO lecture_translation (lecture_id, locale, title, description) VALUES
  ('f3000000-0000-4000-8000-000000000001', 'da', 'Vietnam fra nord til syd',
   'En aften om landet, maden og menneskene. Vores rejseleder fortæller.'),
  ('f3000000-0000-4000-8000-000000000001', 'vi', 'Việt Nam từ bắc vào nam',
   'Một buổi tối về đất nước, món ăn và con người. Trưởng đoàn của chúng tôi kể.'),
  ('f3000000-0000-4000-8000-000000000002', 'da', 'Mekongdeltaet',
   'Livet på floden — markeder, både og rismarker.'),
  ('f3000000-0000-4000-8000-000000000002', 'vi', 'Đồng bằng sông Cửu Long',
   'Cuộc sống trên sông — chợ nổi, ghe thuyền và ruộng lúa.'),
  ('f3000000-0000-4000-8000-000000000003', 'da', 'Rejs til Danmark',
   'For vietnamesere der vil se Skandinavien.'),
  ('f3000000-0000-4000-8000-000000000003', 'vi', 'Du lịch Bắc Âu',
   'Dành cho khách Việt muốn đi Scandinavia.')
ON CONFLICT DO NOTHING;

-- ==========================================================================
--                    TẦNG NỘI DUNG — chủ đề, khách sạn, tham quan, lịch trình
-- ==========================================================================
--
-- Ba bảng của V3 (`hotel`, `excursion`, `itinerary_day`) đứng rỗng từ ngày
-- migration chạy. Hậu quả không phải "thiếu dữ liệu" mà là **hai tab của trang
-- chi tiết không bao giờ có gì để hiện** — `docs/05` mục 3 mô tả chúng, giao
-- diện dựng chúng, và người mở trang chỉ thấy trạng thái rỗng mãi mãi.
--
-- Bốn quy tắc kiểm của `docs/12` mục 9 áp cho phần dưới đây, và dữ liệu này
-- tuân thủ có chủ ý chứ không tình cờ:
--
--   quy tắc 3   số ngày lịch trình = duration_days
--   quy tắc 4   tổng product_hotel_stay.nights = số đêm CÓ khách sạn trong lịch
--               trình, tính theo TỪNG khách sạn
--   quy tắc 12  mỗi điểm đến có ít nhất một khách sạn và một tham quan
--
-- Đêm bay và đêm trên tàu hoả là những đêm KHÔNG có khách sạn. Đó là lý do quy
-- tắc 4 nói "số đêm có khách sạn" chứ không nói "duration_days - 1": tour 16
-- ngày ở dưới có 15 đêm nhưng chỉ 13 đêm khách sạn.

-- ---------------------------------------------------------------- điểm đến mới
-- Sáu điểm đến nữa để lưới sản phẩm có chiều sâu và bộ lọc theo miền có gì mà
-- lọc. Slug KHÔNG dấu ở cả hai ngôn ngữ.
--
-- Khoá duy nhất của slug là (locale, slug) chứ không phải slug toàn cục, nên
-- 'hoi-an' dùng được ở CẢ HAI locale — dòng 'hoi-an-vi' cũ ở trên đã sửa lại
-- cho khỏi làm người đọc tưởng slug phải duy nhất xuyên ngôn ngữ.

INSERT INTO destination (id, region_id, code, sort_order) VALUES
  ('e0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000001', 'SAPA',       3),
  ('e0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000001', 'NINH_BINH',  4),
  ('e0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000002', 'HUE',        2),
  ('e0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000002', 'DA_NANG',    3),
  ('e0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000003', 'HCMC',       2),
  ('e0000000-0000-4000-8000-000000000010', 'd0000000-0000-4000-8000-000000000003', 'PHU_QUOC',   3)
ON CONFLICT (code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO destination_translation (destination_id, locale, slug, name) VALUES
  ('e0000000-0000-4000-8000-000000000005', 'da', 'sapa',      'Sapa'),
  ('e0000000-0000-4000-8000-000000000005', 'vi', 'sa-pa',     'Sa Pa'),
  ('e0000000-0000-4000-8000-000000000006', 'da', 'ninh-binh', 'Ninh Binh'),
  ('e0000000-0000-4000-8000-000000000006', 'vi', 'ninh-binh', 'Ninh Bình'),
  ('e0000000-0000-4000-8000-000000000007', 'da', 'hue',       'Hue'),
  ('e0000000-0000-4000-8000-000000000007', 'vi', 'hue',       'Huế'),
  ('e0000000-0000-4000-8000-000000000008', 'da', 'da-nang',   'Da Nang'),
  ('e0000000-0000-4000-8000-000000000008', 'vi', 'da-nang',   'Đà Nẵng'),
  ('e0000000-0000-4000-8000-000000000009', 'da', 'saigon',    'Saigon'),
  ('e0000000-0000-4000-8000-000000000009', 'vi', 'sai-gon',   'Sài Gòn'),
  ('e0000000-0000-4000-8000-000000000010', 'da', 'phu-quoc',  'Phu Quoc'),
  ('e0000000-0000-4000-8000-000000000010', 'vi', 'phu-quoc',  'Phú Quốc')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- tư vấn viên
-- Trang chi tiết hiện ảnh và tên người phụ trách tour. Một tư vấn viên duy nhất
-- cho cả catalog làm màn hình trông như dữ liệu giả — vì nó đúng là thế.

INSERT INTO consultant (id, market, full_name, email) VALUES
  ('c0000000-0000-4000-8000-000000000012', 'DK', 'Lars Bech',        'lars@example.test'),
  ('c0000000-0000-4000-8000-000000000013', 'VN', 'Nguyễn Thu Hà',    'ha@example.test')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- chủ đề

INSERT INTO theme (id, code, sort_order) VALUES
  ('f4000000-0000-4000-8000-000000000001', 'TREKKING',     1),
  ('f4000000-0000-4000-8000-000000000002', 'RIVER_CRUISE', 2),
  ('f4000000-0000-4000-8000-000000000003', 'BEACH',        3),
  ('f4000000-0000-4000-8000-000000000004', 'FOOD',         4),
  ('f4000000-0000-4000-8000-000000000005', 'CULTURE',      5),
  ('f4000000-0000-4000-8000-000000000006', 'FAMILY',       6)
ON CONFLICT (code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO theme_translation (theme_id, locale, slug, name) VALUES
  ('f4000000-0000-4000-8000-000000000001', 'da', 'vandring',      'Vandring'),
  ('f4000000-0000-4000-8000-000000000001', 'vi', 'di-bo-duong-dai','Đi bộ đường dài'),
  ('f4000000-0000-4000-8000-000000000002', 'da', 'flodkrydstogt', 'Flodkrydstogt'),
  ('f4000000-0000-4000-8000-000000000002', 'vi', 'du-thuyen-song','Du thuyền sông'),
  ('f4000000-0000-4000-8000-000000000003', 'da', 'strand',        'Strand'),
  ('f4000000-0000-4000-8000-000000000003', 'vi', 'bien',          'Biển'),
  ('f4000000-0000-4000-8000-000000000004', 'da', 'mad-og-marked', 'Mad og marked'),
  ('f4000000-0000-4000-8000-000000000004', 'vi', 'am-thuc',       'Ẩm thực'),
  ('f4000000-0000-4000-8000-000000000005', 'da', 'kultur-og-historie', 'Kultur og historie'),
  ('f4000000-0000-4000-8000-000000000005', 'vi', 'van-hoa-lich-su','Văn hoá và lịch sử'),
  ('f4000000-0000-4000-8000-000000000006', 'da', 'familierejser', 'Familierejser'),
  ('f4000000-0000-4000-8000-000000000006', 'vi', 'gia-dinh',      'Gia đình')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- khách sạn
--
-- `name` KHÔNG nằm trong bảng dịch — tên riêng không dịch (`docs/24` mục 5).
-- Chỉ `description` mới là nội dung phải dịch, và nó nằm ở hotel_translation.
--
-- Mỗi điểm đến đúng một khách sạn: quy tắc kiểm 12 đòi ÍT NHẤT một, và một là
-- đủ để quy tắc xanh mà không phải bịa thêm chín cái tên nữa.

INSERT INTO hotel (id, destination_id, name, stars, image) VALUES
  ('a1000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000001',
   'Sofitel Legend Metropole Hanoi', 5, '/img/khach-san/metropole.jpg'),
  ('a1000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002',
   'Paradise Elegance', 4, '/img/khach-san/paradise-ha-long.jpg'),
  ('a1000000-0000-4000-8000-000000000003', 'e0000000-0000-4000-8000-000000000007',
   'Azerai La Residence', 5, '/img/khach-san/azerai-hue.jpg'),
  ('a1000000-0000-4000-8000-000000000004', 'e0000000-0000-4000-8000-000000000003',
   'Almanity Hoi An', 4, '/img/khach-san/almanity-hoi-an.jpg'),
  ('a1000000-0000-4000-8000-000000000005', 'e0000000-0000-4000-8000-000000000005',
   'Topas Ecolodge', 4, '/img/khach-san/topas-sa-pa.jpg'),
  ('a1000000-0000-4000-8000-000000000006', 'e0000000-0000-4000-8000-000000000004',
   'Victoria Can Tho', 4, '/img/khach-san/victoria-can-tho.jpg'),
  ('a1000000-0000-4000-8000-000000000007', 'e0000000-0000-4000-8000-000000000010',
   'Salinda Resort', 5, '/img/khach-san/salinda-phu-quoc.jpg'),
  ('a1000000-0000-4000-8000-000000000008', 'e0000000-0000-4000-8000-000000000006',
   'Tam Coc Garden', 4, '/img/khach-san/tam-coc.jpg'),
  ('a1000000-0000-4000-8000-000000000009', 'e0000000-0000-4000-8000-000000000008',
   'Fusion Maia', 5, '/img/khach-san/fusion-da-nang.jpg'),
  ('a1000000-0000-4000-8000-000000000010', 'e0000000-0000-4000-8000-000000000009',
   'Hotel des Arts', 5, '/img/khach-san/des-arts-sai-gon.jpg')
ON CONFLICT (destination_id, name) WHERE NOT soft_delete DO NOTHING;

INSERT INTO hotel_translation (hotel_id, locale, description) VALUES
  ('a1000000-0000-4000-8000-000000000001', 'da', 'Fransk kolonihotel fra 1901 midt i Hanoi, ti minutters gang fra den gamle bydel.'),
  ('a1000000-0000-4000-8000-000000000001', 'vi', 'Khách sạn kiểu Pháp từ năm 1901 giữa Hà Nội, đi bộ mười phút tới phố cổ.'),
  ('a1000000-0000-4000-8000-000000000002', 'da', 'Skib med tredive kabiner. Alle har vindue mod bugten.'),
  ('a1000000-0000-4000-8000-000000000002', 'vi', 'Tàu ba mươi cabin. Cabin nào cũng có cửa sổ nhìn ra vịnh.'),
  ('a1000000-0000-4000-8000-000000000003', 'da', 'Tidligere fransk residens ved Parfumefloden, med have og pool.'),
  ('a1000000-0000-4000-8000-000000000003', 'vi', 'Dinh thự Pháp cũ bên sông Hương, có vườn và bể bơi.'),
  ('a1000000-0000-4000-8000-000000000004', 'da', 'Lille hotel i gåafstand fra den gamle bydel, med spa i stueetagen.'),
  ('a1000000-0000-4000-8000-000000000004', 'vi', 'Khách sạn nhỏ, đi bộ ra phố cổ, có spa ở tầng trệt.'),
  ('a1000000-0000-4000-8000-000000000005', 'da', 'Bungalows på en bakketop uden nabo. Ingen aircondition — der er ikke brug for det.'),
  ('a1000000-0000-4000-8000-000000000005', 'vi', 'Bungalow trên đỉnh đồi, không có nhà nào bên cạnh. Không điều hoà — không cần tới.'),
  ('a1000000-0000-4000-8000-000000000006', 'da', 'Kolonibygning ved floden i Can Tho, tæt på bådene til det flydende marked.'),
  ('a1000000-0000-4000-8000-000000000006', 'vi', 'Toà nhà kiểu thuộc địa bên sông ở Cần Thơ, gần bến ghe đi chợ nổi.'),
  ('a1000000-0000-4000-8000-000000000007', 'da', 'Resort ved Long Beach med egen strandstrækning.'),
  ('a1000000-0000-4000-8000-000000000007', 'vi', 'Khu nghỉ ở Bãi Trường, có bãi biển riêng.'),
  ('a1000000-0000-4000-8000-000000000008', 'da', 'Have og rismarker hele vejen rundt. Cykler er gratis.'),
  ('a1000000-0000-4000-8000-000000000008', 'vi', 'Xung quanh là vườn và ruộng lúa. Xe đạp miễn phí.'),
  ('a1000000-0000-4000-8000-000000000009', 'da', 'Villaer med privat pool ved My Khe-stranden.'),
  ('a1000000-0000-4000-8000-000000000009', 'vi', 'Villa có bể bơi riêng bên bãi biển Mỹ Khê.'),
  ('a1000000-0000-4000-8000-000000000010', 'da', 'Art deco-hotel med tagbar og udsigt over floden.'),
  ('a1000000-0000-4000-8000-000000000010', 'vi', 'Khách sạn art deco có bar sân thượng nhìn ra sông.')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- tham quan
--
-- Tham quan thuộc về ĐIỂM ĐẾN chứ không thuộc sản phẩm: cùng một chuyến thăm
-- Văn Miếu xuất hiện trong nhiều tour. Chưa có endpoint công khai nào đọc bảng
-- này — nó có mặt ở đây vì quy tắc kiểm 12 đếm theo điểm đến, và vì màn hình
-- quản trị nội dung sẽ cần tới nó trước khi ai kịp nhập tay mười dòng.

INSERT INTO excursion (id, destination_id, code, duration_hours, image) VALUES
  ('a2000000-0000-4000-8000-000000000001', 'e0000000-0000-4000-8000-000000000001', 'VAN_MIEU',   3, NULL),
  ('a2000000-0000-4000-8000-000000000002', 'e0000000-0000-4000-8000-000000000002', 'SUNG_SOT',   2, NULL),
  ('a2000000-0000-4000-8000-000000000003', 'e0000000-0000-4000-8000-000000000003', 'DEN_LONG',   2, NULL),
  ('a2000000-0000-4000-8000-000000000004', 'e0000000-0000-4000-8000-000000000004', 'CAI_RANG',   4, NULL),
  ('a2000000-0000-4000-8000-000000000005', 'e0000000-0000-4000-8000-000000000005', 'CAT_CAT',    5, NULL),
  ('a2000000-0000-4000-8000-000000000006', 'e0000000-0000-4000-8000-000000000006', 'TAM_COC',    4, NULL),
  ('a2000000-0000-4000-8000-000000000007', 'e0000000-0000-4000-8000-000000000007', 'DAI_NOI',    3, NULL),
  ('a2000000-0000-4000-8000-000000000008', 'e0000000-0000-4000-8000-000000000008', 'NGU_HANH_SON', 3, NULL),
  ('a2000000-0000-4000-8000-000000000009', 'e0000000-0000-4000-8000-000000000009', 'CU_CHI',     5, NULL),
  ('a2000000-0000-4000-8000-000000000010', 'e0000000-0000-4000-8000-000000000010', 'HON_THOM',   6, NULL)
ON CONFLICT (destination_id, code) WHERE NOT soft_delete DO NOTHING;

INSERT INTO excursion_translation (excursion_id, locale, name, description) VALUES
  ('a2000000-0000-4000-8000-000000000001', 'da', 'Litteraturtemplet', 'Vietnams første universitet, grundlagt i 1070.'),
  ('a2000000-0000-4000-8000-000000000001', 'vi', 'Văn Miếu',          'Trường đại học đầu tiên của Việt Nam, dựng năm 1070.'),
  ('a2000000-0000-4000-8000-000000000002', 'da', 'Sung Sot-grotten',  'Den største grotte i bugten, med trapper og belysning.'),
  ('a2000000-0000-4000-8000-000000000002', 'vi', 'Hang Sửng Sốt',     'Hang lớn nhất vịnh, có bậc thang và đèn.'),
  ('a2000000-0000-4000-8000-000000000003', 'da', 'Lanternerne',       'Aftentur gennem den gamle bydel når gadelyset slukkes.'),
  ('a2000000-0000-4000-8000-000000000003', 'vi', 'Đèn lồng phố cổ',   'Đi bộ buổi tối trong phố cổ lúc đèn đường tắt.'),
  ('a2000000-0000-4000-8000-000000000004', 'da', 'Cai Rang-markedet', 'Flydende marked. Bådene lægger til klokken fem om morgenen.'),
  ('a2000000-0000-4000-8000-000000000004', 'vi', 'Chợ nổi Cái Răng',  'Chợ trên sông. Ghe tụ về từ năm giờ sáng.'),
  ('a2000000-0000-4000-8000-000000000005', 'da', 'Cat Cat-landsbyen', 'Vandring ned gennem risterrasserne til hmong-landsbyen.'),
  ('a2000000-0000-4000-8000-000000000005', 'vi', 'Bản Cát Cát',       'Đi bộ xuống qua ruộng bậc thang tới bản người Mông.'),
  ('a2000000-0000-4000-8000-000000000006', 'da', 'Tam Coc med robåd', 'To timer på floden mellem kalkstensklipperne.'),
  ('a2000000-0000-4000-8000-000000000006', 'vi', 'Tam Cốc bằng đò',   'Hai tiếng trên sông giữa những khối núi đá vôi.'),
  ('a2000000-0000-4000-8000-000000000007', 'da', 'Citadellet i Hue',  'Kejserbyen bag voldgraven, med Den Forbudte Purpurby indenfor.'),
  ('a2000000-0000-4000-8000-000000000007', 'vi', 'Đại Nội Huế',       'Kinh thành sau hào nước, bên trong là Tử Cấm Thành.'),
  ('a2000000-0000-4000-8000-000000000008', 'da', 'Marmorbjergene',    'Fem kalkstensbakker med grotter og pagoder, elevator til toppen.'),
  ('a2000000-0000-4000-8000-000000000008', 'vi', 'Ngũ Hành Sơn',      'Năm ngọn núi đá có hang và chùa, lên đỉnh bằng thang máy.'),
  ('a2000000-0000-4000-8000-000000000009', 'da', 'Cu Chi-tunnelerne', 'Tunnelsystemet nordvest for Saigon.'),
  ('a2000000-0000-4000-8000-000000000009', 'vi', 'Địa đạo Củ Chi',    'Hệ thống địa đạo phía tây bắc Sài Gòn.'),
  ('a2000000-0000-4000-8000-000000000010', 'da', 'Hon Thom med kabelbane', 'Verdens længste kabelbane over havet, og en dag på øen.'),
  ('a2000000-0000-4000-8000-000000000010', 'vi', 'Hòn Thơm và cáp treo',   'Cáp treo vượt biển dài nhất thế giới, và một ngày trên đảo.')
ON CONFLICT DO NOTHING;

-- ==========================================================================
--                                                       SẢN PHẨM P4 … P12
-- ==========================================================================
--
-- Ba sản phẩm đầu phủ ba loại. Sáu loại của `docs/04` cần sáu, và một lưới ba
-- thẻ không cho ai thấy phân trang, sắp xếp hay bộ lọc có chạy hay không —
-- `totalItems` bằng 3 thì con số nào cũng đúng.
--
-- Chín sản phẩm dưới đây phủ nốt PRIVATE_TOUR, COMBO, DAY_TOUR, và đủ số để
-- trang 2 của listing tồn tại.

-- --------------------------------------------------------------- P4 GROUP_TOUR
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, map_image, is_new, rating, review_count, consultant_id) VALUES
  ('f0000000-0000-4000-8000-000000000004', 'GROUP_TOUR',
   'e0000000-0000-4000-8000-000000000005', 10, '/img/tour/p04-sa-pa.jpg',
   NULL, FALSE, 4.7, 63, 'c0000000-0000-4000-8000-000000000011')
ON CONFLICT DO NOTHING;

INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                tour_leader_language, fitness_level) VALUES
  ('f0000000-0000-4000-8000-000000000004', 10, 18, 10, 'da', 3)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000004', 'da', 'nordvietnam-og-sapa',
   'Nordvietnam og Sapa', 'Ti dage i bjergene og ved bugten, med dansk rejseleder',
   ARRAY['Nordvietnam er det Vietnam de fleste har set på billeder: kalkstensklipper i vandet, risterrasser i tåge, og en hovedstad hvor fortovet er køkken.',
         'Vi tager nattoget op til Lao Cai og bliver to nætter i bjergene. Der er tid til at gå, og der er tid til at lade være.',
         'Turen slutter, hvor den begyndte, med en fri dag i Hanoi inden hjemrejsen.'],
   ARRAY['Dansk rejseleder hele vejen','To nætter i bjergene ved Sapa','Overnatning ombord i Halong-bugten','Maksimalt 18 rejsende'],
   'Risterrasser i bjergene ved Sapa', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000004', 'vi', 'mien-bac-va-sa-pa',
   'Miền Bắc và Sa Pa', 'Mười ngày giữa núi và vịnh, có trưởng đoàn đi cùng',
   ARRAY['Miền Bắc là Việt Nam mà phần lớn người ta đã thấy qua ảnh: núi đá dựng giữa nước, ruộng bậc thang trong sương, và một thủ đô lấy vỉa hè làm bếp.',
         'Chúng tôi đi tàu đêm lên Lào Cai và ở lại hai đêm trên núi. Có thời gian để đi bộ, và cũng có thời gian để không làm gì.',
         'Chuyến đi kết thúc ở nơi nó bắt đầu, với một ngày tự do tại Hà Nội trước khi bay về.'],
   ARRAY['Trưởng đoàn theo suốt hành trình','Hai đêm trên núi Sa Pa','Ngủ đêm trên tàu ở vịnh Hạ Long','Tối đa 18 khách'],
   'Ruộng bậc thang trên núi ở Sa Pa', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000004', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000004', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------- P5 GROUP_TOUR
-- is_new = TRUE: thẻ phải mọc thêm một nhãn, và nhãn đó chưa từng hiện ra vì
-- chưa sản phẩm nào bật cờ này.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, map_image, is_new, rating, review_count, consultant_id) VALUES
  ('f0000000-0000-4000-8000-000000000005', 'GROUP_TOUR',
   'e0000000-0000-4000-8000-000000000007', 8, '/img/tour/p05-mien-trung.jpg',
   NULL, TRUE, 4.5, 41, 'c0000000-0000-4000-8000-000000000012')
ON CONFLICT DO NOTHING;

INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                tour_leader_language, fitness_level) VALUES
  ('f0000000-0000-4000-8000-000000000005', 10, 20, 10, 'da', 2)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000005', 'da', 'det-centrale-vietnam',
   'Det centrale Vietnam', 'Otte dage mellem kejsergrave og strand',
   ARRAY['Midten af landet er den smalle del: fra kysten til grænsen er der halvtreds kilometer, og alt ligger tæt.',
         'Vi bor to steder — Hue og Hoi An — og kører den korte strækning imellem over Hai Van-passet.'],
   ARRAY['Kun to hoteller på otte dage','Kejserbyen og My Son på samme rejse','Fri dag ved kysten','Lavt tempo'],
   'Pagode i Hue under blå himmel', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000005', 'vi', 'mien-trung-viet-nam',
   'Miền Trung Việt Nam', 'Tám ngày giữa lăng tẩm và biển',
   ARRAY['Khúc giữa đất nước là khúc hẹp nhất: từ bờ biển tới biên giới chỉ năm mươi cây số, cái gì cũng gần.',
         'Chúng tôi ở hai nơi — Huế và Hội An — và đi đoạn ngắn giữa hai nơi đó qua đèo Hải Vân.'],
   ARRAY['Chỉ hai khách sạn trong tám ngày','Kinh thành và Mỹ Sơn trong cùng chuyến','Một ngày tự do bên biển','Nhịp đi chậm'],
   'Ngôi chùa ở Huế dưới trời xanh', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000005', 'DK', TRUE, now())
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------- P6 PRIVATE_TOUR
-- KHÔNG có ngày khởi hành và KHÔNG có giá từ departure_price: loại này bán qua
-- báo giá, và giá tham khảo nằm ở price_tier theo cỡ đoàn. Đây là sản phẩm duy
-- nhất trong bộ dữ liệu mà nút CTA dẫn sang form báo giá thay vì sang đặt tour.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, review_count, consultant_id) VALUES
  ('f0000000-0000-4000-8000-000000000006', 'PRIVATE_TOUR',
   'e0000000-0000-4000-8000-000000000001', 12, '/img/tour/p06-gia-dinh.jpg',
   TRUE, 0, 'c0000000-0000-4000-8000-000000000011')
ON CONFLICT DO NOTHING;

INSERT INTO product_private (product_id, lead_time_days, quote_valid_days) VALUES
  ('f0000000-0000-4000-8000-000000000006', 30, 14)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000006', 'da', 'vietnam-med-boern',
   'Vietnam med børn', 'Privat rejse, jeres datoer, jeres tempo',
   ARRAY['En rejse med børn er ikke den samme rejse med færre timer i bussen. Det er en anden rute.',
         'Vi lægger den sammen med jer: kortere køreture, hoteller med pool, og et par dage hvor der ikke står noget i programmet.',
         'Forslaget herunder er et udgangspunkt på tolv dage. Alt kan laves om.'],
   ARRAY['Egen guide og chauffør','Ruten lægges sammen med jer','Hoteller valgt efter pool og plads','Ingen faste afrejsedatoer'],
   'Flod gennem en grøn dal i Ninh Binh', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000006', 'vi', 'viet-nam-cung-tre-nho',
   'Việt Nam cùng trẻ nhỏ', 'Tour riêng, ngày của bạn, nhịp của bạn',
   ARRAY['Đi cùng trẻ con không phải là chuyến đi cũ bớt vài giờ ngồi xe. Đó là một lộ trình khác.',
         'Chúng tôi dựng lộ trình cùng bạn: chặng xe ngắn hơn, khách sạn có bể bơi, và vài ngày không ghi gì trong chương trình.',
         'Gợi ý dưới đây là một khung mười hai ngày. Sửa được hết.'],
   ARRAY['Hướng dẫn viên và lái xe riêng','Lộ trình dựng cùng bạn','Khách sạn chọn theo bể bơi và chỗ chơi','Không có ngày khởi hành cố định'],
   'Dòng sông giữa thung lũng xanh ở Ninh Bình', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000006', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000006', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- Giá theo cỡ đoàn. Bậc cuối bỏ trần (max_pax NULL) — đúng cách bảng này được
-- thiết kế để dùng. Số của VN KHÔNG phải số của DK nhân tỷ giá.
INSERT INTO price_tier (id, product_id, market, min_pax, max_pax, price_per_person, currency) VALUES
  ('a4000000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000006', 'DK', 2, 3,  32900.00, 'DKK'),
  ('a4000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000006', 'DK', 4, 5,  28900.00, 'DKK'),
  ('a4000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000006', 'DK', 6, NULL, 24900.00, 'DKK'),
  ('a4000000-0000-4000-8000-000000000004', 'f0000000-0000-4000-8000-000000000006', 'VN', 2, 3,  24500000, 'VND'),
  ('a4000000-0000-4000-8000-000000000005', 'f0000000-0000-4000-8000-000000000006', 'VN', 4, NULL, 19800000, 'VND')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------ P7 CRUISE
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, rating, review_count, consultant_id) VALUES
  ('f0000000-0000-4000-8000-000000000007', 'CRUISE',
   'e0000000-0000-4000-8000-000000000004', 5, '/img/tour/p07-du-thuyen-mekong.jpg',
   FALSE, 4.8, 27, 'c0000000-0000-4000-8000-000000000012')
ON CONFLICT DO NOTHING;

INSERT INTO product_cruise (product_id, ship_name, port_count) VALUES
  ('f0000000-0000-4000-8000-000000000007', 'Jahan', 6)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000007', 'da', 'krydstogt-paa-mekong',
   'Krydstogt på Mekong', 'Fem dage fra Saigon til Can Tho',
   ARRAY['Deltaet ses bedst fra vandet. Vejene går udenom, floden går igennem.',
         'Skibet lægger til seks steder på fem dage, og der er en udflugt i land ved hver.'],
   ARRAY['Seks anløb på fem dage','Alle måltider ombord','Fire kabinekategorier','Udflugt i land hver dag'],
   'Både på floden i Mekong-deltaet', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000007', 'vi', 'du-thuyen-song-cuu-long',
   'Du thuyền sông Cửu Long', 'Năm ngày từ Sài Gòn xuống Cần Thơ',
   ARRAY['Đồng bằng nhìn từ mặt nước là rõ nhất. Đường bộ đi vòng, còn sông thì đi xuyên qua.',
         'Tàu cập sáu điểm trong năm ngày, và mỗi điểm đều có một chuyến lên bờ.'],
   ARRAY['Sáu điểm dừng trong năm ngày','Bao trọn bữa ăn trên tàu','Bốn hạng cabin','Ngày nào cũng có chuyến lên bờ'],
   'Ghe thuyền trên sông ở đồng bằng sông Cửu Long', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000007', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000007', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------- P8 COMBO
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, rating, review_count) VALUES
  ('f0000000-0000-4000-8000-000000000008', 'COMBO',
   'e0000000-0000-4000-8000-000000000010', 10, '/img/tour/p08-hoi-an-phu-quoc.jpg',
   FALSE, 4.4, 18)
ON CONFLICT DO NOTHING;

INSERT INTO product_combo (product_id, nights, valid_from, valid_to) VALUES
  ('f0000000-0000-4000-8000-000000000008', 9, DATE '2027-01-06', DATE '2027-11-30')
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000008', 'da', 'hoi-an-og-phu-quoc',
   'Hoi An og Phu Quoc', 'Fire nætter i den gamle bydel, fem ved stranden',
   ARRAY['To steder, én pris, ingen planlægning. Vi har sat flyet imellem dem ind i pakken.',
         'Første halvdel er by og mad, anden halvdel er strand og ingenting.'],
   ARRAY['Indenrigsfly indregnet','Ni nætter, to hoteller','Fri afbestilling indtil 60 dage før','Ingen faste afrejsedage'],
   'Strand på Phu Quoc', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000008', 'vi', 'hoi-an-va-phu-quoc',
   'Hội An và Phú Quốc', 'Bốn đêm phố cổ, năm đêm bên biển',
   ARRAY['Hai nơi, một mức giá, không phải sắp gì. Chặng bay nội địa giữa hai nơi đã nằm trong gói.',
         'Nửa đầu là phố và ăn, nửa sau là biển và không làm gì.'],
   ARRAY['Đã gồm vé bay nội địa','Chín đêm, hai khách sạn','Huỷ miễn phí tới trước 60 ngày','Không cố định ngày khởi hành'],
   'Bãi biển Phú Quốc', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000008', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000008', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- P9 DAY_TOUR
-- duration_days phải là NULL — ck_product_duration từ chối mọi giá trị khác cho
-- loại này, và trên website trường `durationDays` bị BỎ HẲN khỏi JSON.
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, rating, review_count) VALUES
  ('f0000000-0000-4000-8000-000000000009', 'DAY_TOUR',
   'e0000000-0000-4000-8000-000000000001', NULL, '/img/tour/p09-am-thuc-ha-noi.jpg',
   FALSE, 4.9, 112)
ON CONFLICT DO NOTHING;

INSERT INTO product_day_tour (product_id, duration_hours, cutoff_hours) VALUES
  ('f0000000-0000-4000-8000-000000000009', 4, 24)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000009', 'da', 'gadekoekken-i-hanoi',
   'Gadekøkken i Hanoi', 'Fire timer, otte stop, én mave',
   ARRAY['Vi går, vi spiser, vi går videre. Ingen restauranter — kun de steder hvor der står plastikstole på fortovet.',
         'Turen slutter med kaffe med æg, som lyder værre end det er.'],
   ARRAY['Otte smagsstop','Maksimalt otte deltagere','Vegetarisk rute muligt','Bestilles indtil dagen før'],
   'En skål pho med kød og grøntsager', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000009', 'vi', 'am-thuc-duong-pho-ha-noi',
   'Ẩm thực đường phố Hà Nội', 'Bốn tiếng, tám điểm, một cái bụng',
   ARRAY['Đi bộ, ăn, rồi đi tiếp. Không vào nhà hàng — chỉ những chỗ kê ghế nhựa ra vỉa hè.',
         'Kết thúc bằng cà phê trứng, nghe thì lạ mà uống thì không.'],
   ARRAY['Tám điểm nếm','Tối đa tám khách','Có lộ trình chay','Đặt tới hôm trước'],
   'Một bát phở có thịt và rau', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000009', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000009', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- ------------------------------------------------- P10 INDIVIDUAL_PACKAGE
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, rating, review_count) VALUES
  ('f0000000-0000-4000-8000-000000000010', 'INDIVIDUAL_PACKAGE',
   'e0000000-0000-4000-8000-000000000010', 7, '/img/tour/p10-phu-quoc.jpg',
   FALSE, 4.3, 22)
ON CONFLICT DO NOTHING;

INSERT INTO product_individual (product_id, min_party_size, flexible_date_window_days) VALUES
  ('f0000000-0000-4000-8000-000000000010', 2, 21)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000010', 'da', 'phu-quoc-paa-egen-haand',
   'Phu Quoc på egen hånd', 'Syv dage ved stranden, uden program',
   ARRAY['Der er ikke noget program. Der er et hotel, en lufthavnstransfer og et telefonnummer der virker.',
         'Vil I ud af solsengen, sætter vi en bådtur eller en kabelbane på. Vil I ikke, gør vi ikke.'],
   ARRAY['Fleksibel afrejse inden for tre uger','Transfer begge veje','Udflugter kan tilkøbes','Fra to personer'],
   'Sandstrand med palmer på Phu Quoc', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000010', 'vi', 'phu-quoc-tu-tuc',
   'Phú Quốc tự túc', 'Bảy ngày bên biển, không chương trình',
   ARRAY['Không có chương trình nào cả. Có một khách sạn, một chuyến đón sân bay và một số điện thoại gọi được.',
         'Muốn rời ghế tắm nắng thì chúng tôi gắn thêm chuyến tàu hoặc cáp treo. Không muốn thì thôi.'],
   ARRAY['Chọn ngày linh hoạt trong ba tuần','Đón tiễn sân bay hai chiều','Thêm được các chuyến tham quan','Từ hai khách'],
   'Bãi cát và hàng dừa ở Phú Quốc', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000010', 'DK', TRUE, now())
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------- P11 GROUP_TOUR
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, rating, review_count, consultant_id) VALUES
  ('f0000000-0000-4000-8000-000000000011', 'GROUP_TOUR',
   'e0000000-0000-4000-8000-000000000009', 12, '/img/tour/p11-mien-nam.jpg',
   FALSE, 4.6, 55, 'c0000000-0000-4000-8000-000000000013')
ON CONFLICT DO NOTHING;

INSERT INTO product_group_tour (product_id, min_pax, max_pax, guaranteed_threshold,
                                tour_leader_language, fitness_level) VALUES
  ('f0000000-0000-4000-8000-000000000011', 12, 22, 12, 'da', 2)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000011', 'da', 'sydvietnam-og-mekong',
   'Sydvietnam og Mekong', 'Tolv dage i deltaet og ved kysten',
   ARRAY['Syden er varmere, fladere og hurtigere end resten af landet. Maden er sødere, og folk taler højere.',
         'Vi bruger fire dage i deltaet, hvor der er flere kanaler end veje.'],
   ARRAY['Fire dage i Mekong-deltaet','Overnatning hos en familie ved floden','Cu Chi og Saigon','Dansk rejseleder'],
   'Både på en kanal i Mekong-deltaet', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000011', 'vi', 'mien-nam-va-song-cuu-long',
   'Miền Nam và sông Cửu Long', 'Mười hai ngày ở đồng bằng và ven biển',
   ARRAY['Miền Nam nóng hơn, phẳng hơn và nhanh hơn phần còn lại. Đồ ăn ngọt hơn, và người ta nói to hơn.',
         'Chúng tôi dành bốn ngày ở đồng bằng, nơi kênh rạch nhiều hơn đường.'],
   ARRAY['Bốn ngày ở đồng bằng sông Cửu Long','Một đêm ở nhà dân bên sông','Củ Chi và Sài Gòn','Có trưởng đoàn đi cùng'],
   'Ghe thuyền trên kênh miền Tây', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000011', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000011', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------- P12 DAY_TOUR
INSERT INTO product (id, product_type, primary_destination_id, duration_days,
                     hero_image, is_new, rating, review_count) VALUES
  ('f0000000-0000-4000-8000-000000000012', 'DAY_TOUR',
   'e0000000-0000-4000-8000-000000000006', NULL, '/img/tour/p12-ninh-binh.jpg',
   TRUE, 4.7, 64)
ON CONFLICT DO NOTHING;

INSERT INTO product_day_tour (product_id, duration_hours, cutoff_hours) VALUES
  ('f0000000-0000-4000-8000-000000000012', 12, 48)
ON CONFLICT DO NOTHING;

INSERT INTO product_translation (product_id, locale, slug, title, short_description,
                                 long_description, why_choose_this, hero_image_alt,
                                 status, translated_at) VALUES
  ('f0000000-0000-4000-8000-000000000012', 'da', 'ninh-binh-paa-en-dag',
   'Ninh Binh på en dag', 'Halong-bugten uden vand, to timer fra Hanoi',
   ARRAY['De samme kalkstensklipper som ude i bugten, men med rismarker imellem i stedet for hav.',
         'Vi henter klokken otte og er tilbage i Hanoi til aftensmad.'],
   ARRAY['Afhentning på hotellet i Hanoi','Robåd gennem grotterne','Frokost undervejs','Hjemme igen til aften'],
   'Pagode omgivet af vand og bjerge i Ninh Binh', 'PUBLISHED', now()),
  ('f0000000-0000-4000-8000-000000000012', 'vi', 'ninh-binh-mot-ngay',
   'Ninh Bình một ngày', 'Hạ Long trên cạn, cách Hà Nội hai tiếng',
   ARRAY['Vẫn những khối đá vôi như ngoài vịnh, chỉ khác là giữa chúng là ruộng lúa chứ không phải biển.',
         'Đón lúc tám giờ, về tới Hà Nội kịp bữa tối.'],
   ARRAY['Đón tại khách sạn ở Hà Nội','Đi đò xuyên hang','Có bữa trưa','Về trong ngày'],
   'Ngôi chùa giữa sông nước và núi đá ở Ninh Bình', 'PUBLISHED', now())
ON CONFLICT DO NOTHING;

INSERT INTO product_market (product_id, market, is_published, published_at) VALUES
  ('f0000000-0000-4000-8000-000000000012', 'DK', TRUE, now()),
  ('f0000000-0000-4000-8000-000000000012', 'VN', TRUE, now())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- chủ đề gán

INSERT INTO product_theme (product_id, theme_id) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'f4000000-0000-4000-8000-000000000005'),
  ('f0000000-0000-4000-8000-000000000002', 'f4000000-0000-4000-8000-000000000002'),
  ('f0000000-0000-4000-8000-000000000003', 'f4000000-0000-4000-8000-000000000005'),
  ('f0000000-0000-4000-8000-000000000004', 'f4000000-0000-4000-8000-000000000001'),
  ('f0000000-0000-4000-8000-000000000004', 'f4000000-0000-4000-8000-000000000005'),
  ('f0000000-0000-4000-8000-000000000005', 'f4000000-0000-4000-8000-000000000005'),
  ('f0000000-0000-4000-8000-000000000006', 'f4000000-0000-4000-8000-000000000006'),
  ('f0000000-0000-4000-8000-000000000007', 'f4000000-0000-4000-8000-000000000002'),
  ('f0000000-0000-4000-8000-000000000008', 'f4000000-0000-4000-8000-000000000003'),
  ('f0000000-0000-4000-8000-000000000009', 'f4000000-0000-4000-8000-000000000004'),
  ('f0000000-0000-4000-8000-000000000010', 'f4000000-0000-4000-8000-000000000003'),
  ('f0000000-0000-4000-8000-000000000011', 'f4000000-0000-4000-8000-000000000002'),
  ('f0000000-0000-4000-8000-000000000012', 'f4000000-0000-4000-8000-000000000005')
ON CONFLICT DO NOTHING;

-- ==========================================================================
--                                          LỊCH TRÌNH TỪNG NGÀY và KHÁCH SẠN
-- ==========================================================================
--
-- Sáu sản phẩm có lịch trình đầy đủ: P1, P2, P3, P4, P5, P7. Sáu sản phẩm còn
-- lại không có, và cả sáu đều là chủ ý chứ không phải bỏ dở:
--
--   · P8 (COMBO), P9 và P12 (DAY_TOUR) — `tabCoMat` trong
--     `web/apps/site/src/lib/tabs.ts` bỏ hẳn tab lịch trình với hai loại này.
--     Chúng không có "ngày thứ bảy" để mà kể.
--
-- P6, P10 và P11 CÓ lịch trình. Bản nháp đầu để trống cả ba cho "trạng thái
-- rỗng có thứ chứng minh", và **quy tắc kiểm 3 bác điều đó**: nó đòi số ngày
-- lịch trình khớp `duration_days` cho cả bốn loại tour dài, nên không ngày nào
-- là một cột đỏ chứ không phải một lựa chọn thiết kế.
--
-- Trạng thái rỗng vẫn có chỗ chứng minh, chỉ là ở tab khác: **P6 không có dòng
-- `product_hotel_stay` nào** và `hotel_id` của mọi ngày đều NULL. Với một tour
-- riêng thì đó là sự thật nghiệp vụ chứ không phải dữ liệu thiếu — lộ trình
-- mẫu chưa chốt khách sạn, vì khách sạn chọn cùng khách. Và quy tắc kiểm 4
-- xanh: không có đêm nào gắn khách sạn thì không có gì để lệch.
--
-- Số ngày khớp `duration_days` ở cả sáu — quy tắc kiểm 3 của `docs/12` mục 9.
-- Ngày bay và đêm trên tàu hoả để `destination_id` và `hotel_id` NULL: đó là
-- "Nat undervejs" trên giao diện, không phải dữ liệu thiếu.

-- ------------------------------------------------ P1, 16 ngày, Bắc vào Nam
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000001010000', 'f0000000-0000-4000-8000-000000000001',  1, NULL, NULL),
  ('a3000000-0000-4000-8000-000001020000', 'f0000000-0000-4000-8000-000000000001',  2, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000001030000', 'f0000000-0000-4000-8000-000000000001',  3, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000001040000', 'f0000000-0000-4000-8000-000000000001',  4, 'e0000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000002'),
  ('a3000000-0000-4000-8000-000001050000', 'f0000000-0000-4000-8000-000000000001',  5, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000001060000', 'f0000000-0000-4000-8000-000000000001',  6, NULL, NULL),
  ('a3000000-0000-4000-8000-000001070000', 'f0000000-0000-4000-8000-000000000001',  7, 'e0000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003'),
  ('a3000000-0000-4000-8000-000001080000', 'f0000000-0000-4000-8000-000000000001',  8, 'e0000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003'),
  ('a3000000-0000-4000-8000-000001090000', 'f0000000-0000-4000-8000-000000000001',  9, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000001100000', 'f0000000-0000-4000-8000-000000000001', 10, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000001110000', 'f0000000-0000-4000-8000-000000000001', 11, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000001120000', 'f0000000-0000-4000-8000-000000000001', 12, 'e0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000006'),
  ('a3000000-0000-4000-8000-000001130000', 'f0000000-0000-4000-8000-000000000001', 13, 'e0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000006'),
  ('a3000000-0000-4000-8000-000001140000', 'f0000000-0000-4000-8000-000000000001', 14, 'e0000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000010'),
  ('a3000000-0000-4000-8000-000001150000', 'f0000000-0000-4000-8000-000000000001', 15, 'e0000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000010'),
  ('a3000000-0000-4000-8000-000001160000', 'f0000000-0000-4000-8000-000000000001', 16, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000001010000','da','Afrejse fra København','Vi flyver fra København om aftenen med ét skift undervejs. Måltiderne er med i billetten, og vi lander i Hanoi ved middagstid dagen efter.'),
  ('a3000000-0000-4000-8000-000001010000','vi','Khởi hành từ Copenhagen','Bay từ Copenhagen vào buổi tối, quá cảnh một chặng. Bữa ăn đã gồm trong vé, và tới Hà Nội vào khoảng giữa trưa hôm sau.'),
  ('a3000000-0000-4000-8000-000001020000','da','Ankomst til Hanoi','Rejselederen tager imod inde i ankomsthallen og følger med til hotellet. Resten af dagen er fri, så kroppen kan nå at finde det nye tidspunkt.'),
  ('a3000000-0000-4000-8000-000001020000','vi','Tới Hà Nội','Trưởng đoàn đón ngay trong sảnh đến và đi cùng về khách sạn. Thời gian còn lại để tự do, cho cơ thể kịp quen múi giờ mới.'),
  ('a3000000-0000-4000-8000-000001030000','da','Hanoi til fods','Til fods gennem den gamle bydel om formiddagen, med et stop ved Litteraturtemplet. Frokosten spiser vi et sted vi har brugt i femten år.'),
  ('a3000000-0000-4000-8000-000001030000','vi','Hà Nội đi bộ','Buổi sáng đi bộ trong phố cổ, ghé Văn Miếu. Bữa trưa ăn ở một quán chúng tôi đã lui tới suốt mười lăm năm.'),
  ('a3000000-0000-4000-8000-000001040000','da','Halong-bugten','Vi kører ud til bugten om morgenen og går ombord ved middagstid. Kajak om eftermiddagen, og om natten ligger skibet stille mellem klipperne.'),
  ('a3000000-0000-4000-8000-000001040000','vi','Vịnh Hạ Long','Sáng đi xe ra vịnh, trưa lên tàu. Chiều chèo kayak, và ban đêm tàu nằm yên giữa những khối đá vôi.'),
  ('a3000000-0000-4000-8000-000001050000','da','Tilbage til Hanoi','Sen morgenmad mens skibet sejler ind mod havnen. Turen tilbage til Hanoi går gennem rismarker, og vi er fremme i god tid før aftensmaden.'),
  ('a3000000-0000-4000-8000-000001050000','vi','Về lại Hà Nội','Ăn sáng muộn trong lúc tàu vào bờ. Đường về Hà Nội đi qua những cánh đồng lúa, và tới nơi trước bữa tối khá lâu.'),
  ('a3000000-0000-4000-8000-000001060000','da','Nattog mod syd','Toget mod syd kører klokken ti om aftenen, og vi har hele kupeer med fire køjer. Man vågner et helt andet sted i landet.'),
  ('a3000000-0000-4000-8000-000001060000','vi','Tàu đêm vào Nam','Tàu vào Nam chạy lúc mười giờ tối, và chúng tôi đặt trọn khoang bốn giường. Sáng dậy đã ở một vùng hoàn toàn khác.'),
  ('a3000000-0000-4000-8000-000001070000','da','Hue og kejserbyen','Citadellet og Den Forbudte Purpurby om formiddagen, mens der stadig er skygge. Om eftermiddagen sejler vi på Parfumefloden ud til Thien Mu.'),
  ('a3000000-0000-4000-8000-000001070000','vi','Huế và kinh thành','Sáng đi Đại Nội và Tử Cấm Thành, lúc còn bóng râm. Chiều đi thuyền trên sông Hương lên chùa Thiên Mụ.'),
  ('a3000000-0000-4000-8000-000001080000','da','Kejsergravene','To kejsergrave der ligner hinanden så lidt som muligt: den ene stram og militær, den anden groet helt til. Frokost ved floden imellem dem.'),
  ('a3000000-0000-4000-8000-000001080000','vi','Lăng tẩm','Hai lăng vua khác nhau tới mức khó tin: một cái nghiêm và vuông vức, một cái để cây mọc trùm lên. Giữa hai lăng là bữa trưa bên sông.'),
  ('a3000000-0000-4000-8000-000001090000','da','Over Hai Van til Hoi An','Vi kører over Hai Van-passet og holder på toppen, hvor man ser kysten begge veje. Ankomst til Hoi An sidst på eftermiddagen.'),
  ('a3000000-0000-4000-8000-000001090000','vi','Qua đèo Hải Vân tới Hội An','Đi xe qua đèo Hải Vân, dừng trên đỉnh nhìn được cả hai phía bờ biển. Tới Hội An vào cuối buổi chiều.'),
  ('a3000000-0000-4000-8000-000001100000','da','Hoi An','Den gamle bydel er lukket for biler, så byen går man i. Skrædderne tager mål om formiddagen og syr færdigt inden vi rejser videre.'),
  ('a3000000-0000-4000-8000-000001100000','vi','Hội An','Phố cổ cấm xe nên chỉ có đi bộ. Thợ may đo vào buổi sáng và may xong trước khi đoàn rời đi.'),
  ('a3000000-0000-4000-8000-000001110000','da','Fri dag i Hoi An','Der står ikke noget i programmet i dag. Stranden ligger fire kilometer væk, cyklerne er gratis, og der er madlavningskursus for dem der vil.'),
  ('a3000000-0000-4000-8000-000001110000','vi','Ngày tự do ở Hội An','Hôm nay chương trình không ghi gì. Biển cách bốn cây số, xe đạp miễn phí, và ai muốn thì có lớp nấu ăn.'),
  ('a3000000-0000-4000-8000-000001120000','da','Ned til Mekong','Morgenfly til Saigon og videre ud i deltaet med bil samme dag. Landskabet skifter fra storby til kanaler på under to timer.'),
  ('a3000000-0000-4000-8000-000001120000','vi','Xuống đồng bằng','Bay sáng vào Sài Gòn rồi đi xe tiếp về miền Tây trong ngày. Chưa tới hai tiếng, cảnh đã đổi từ đô thị sang kênh rạch.'),
  ('a3000000-0000-4000-8000-000001130000','da','Det flydende marked','Cai Rang åbner klokken fem, så vi står tidligt op. Vi er ude på vandet før turistbådene og hjemme igen til en sen morgenmad.'),
  ('a3000000-0000-4000-8000-000001130000','vi','Chợ nổi','Chợ Cái Răng họp từ năm giờ nên phải dậy sớm. Ra tới nơi trước các tàu du lịch, và về kịp bữa sáng muộn.'),
  ('a3000000-0000-4000-8000-000001140000','da','Saigon','Tilbage til Saigon ad motorvejen. Eftermiddagen er fri i byen, og aftenen slutter på en tagterrasse med udsigt ud over floden.'),
  ('a3000000-0000-4000-8000-000001140000','vi','Sài Gòn','Về Sài Gòn theo đường cao tốc. Chiều tự do trong thành phố, buổi tối kết thúc trên một sân thượng nhìn ra sông.'),
  ('a3000000-0000-4000-8000-000001150000','da','Cu Chi og afsked','Cu Chi-tunnelerne om formiddagen, hvor man kan gå ned i et stykke af dem. Om aftenen spiser hele gruppen sammen for sidste gang.'),
  ('a3000000-0000-4000-8000-000001150000','vi','Củ Chi và bữa chia tay','Sáng đi địa đạo Củ Chi, xuống được một đoạn thật. Buổi tối cả đoàn ăn với nhau bữa cuối cùng.'),
  ('a3000000-0000-4000-8000-000001160000','da','Hjemrejse','Afgang fra Saigon om formiddagen og hjemkomst til København samme aften. Rejselederen følger med hele vejen til check-in.'),
  ('a3000000-0000-4000-8000-000001160000','vi','Về nhà','Rời Sài Gòn buổi sáng, tối cùng ngày về tới Copenhagen. Trưởng đoàn đi cùng tới tận quầy làm thủ tục.')
ON CONFLICT DO NOTHING;

INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 3, 1),
  ('f0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000002', 1, 2),
  ('f0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000003', 2, 3),
  ('f0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000004', 3, 4),
  ('f0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000006', 2, 5),
  ('f0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000010', 2, 6)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------ P2, 3 ngày, du thuyền
-- Không có khách sạn nào: ba ngày này ở trên tàu. `tabCoMat` cũng bỏ tab khách
-- sạn với CRUISE, nên bảng product_hotel_stay để trống là đúng chứ không thiếu.
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000002010000', 'f0000000-0000-4000-8000-000000000002', 1, 'e0000000-0000-4000-8000-000000000002', NULL),
  ('a3000000-0000-4000-8000-000002020000', 'f0000000-0000-4000-8000-000000000002', 2, 'e0000000-0000-4000-8000-000000000002', NULL),
  ('a3000000-0000-4000-8000-000002030000', 'f0000000-0000-4000-8000-000000000002', 3, 'e0000000-0000-4000-8000-000000000002', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000002010000','da','Ombordstigning','Bussen fra Hanoi ankommer til havnen ved middagstid, og frokosten serveres mens vi sejler ud. Kabinerne er klar når vi når de første klipper.'),
  ('a3000000-0000-4000-8000-000002010000','vi','Lên tàu','Xe từ Hà Nội tới bến vào giữa trưa, bữa trưa dọn ngay lúc tàu rời bến. Cabin sẵn sàng khi tàu ra tới những khối đá đầu tiên.'),
  ('a3000000-0000-4000-8000-000002020000','da','Grotter og kajak','Sung Sot-grotten om morgenen inden de store både kommer. Om eftermiddagen kajak i en lukket bugt, hvor der ikke er motorstøj.'),
  ('a3000000-0000-4000-8000-000002020000','vi','Hang động và kayak','Sáng vào hang Sửng Sốt, trước khi các tàu lớn tới. Chiều chèo kayak trong một vụng kín, không có tiếng máy nổ.'),
  ('a3000000-0000-4000-8000-000002030000','da','I land igen','Brunch ombord mens skibet sejler mod havnen igen. Bussen mod Hanoi kører klokken tolv og er i byen midt på eftermiddagen.'),
  ('a3000000-0000-4000-8000-000002030000','vi','Trở lại bờ','Ăn nhẹ trên tàu trong lúc tàu quay về bến. Xe đi Hà Nội chạy lúc mười hai giờ, giữa chiều thì tới nơi.')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------- P3, 9 ngày, Hội An tự túc
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000003010000', 'f0000000-0000-4000-8000-000000000003', 1, NULL, NULL),
  ('a3000000-0000-4000-8000-000003020000', 'f0000000-0000-4000-8000-000000000003', 2, 'e0000000-0000-4000-8000-000000000008', 'a1000000-0000-4000-8000-000000000009'),
  ('a3000000-0000-4000-8000-000003030000', 'f0000000-0000-4000-8000-000000000003', 3, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000003040000', 'f0000000-0000-4000-8000-000000000003', 4, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000003050000', 'f0000000-0000-4000-8000-000000000003', 5, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000003060000', 'f0000000-0000-4000-8000-000000000003', 6, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000003070000', 'f0000000-0000-4000-8000-000000000003', 7, 'e0000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003'),
  ('a3000000-0000-4000-8000-000003080000', 'f0000000-0000-4000-8000-000000000003', 8, 'e0000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003'),
  ('a3000000-0000-4000-8000-000003090000', 'f0000000-0000-4000-8000-000000000003', 9, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000003010000','da','Afrejse fra København','Aftenafgang fra København med ét skift undervejs. Vi flyver direkte til Da Nang og undgår dermed en ekstra indenrigsstrækning.'),
  ('a3000000-0000-4000-8000-000003010000','vi','Khởi hành từ Copenhagen','Bay tối từ Copenhagen, quá cảnh một chặng. Bay thẳng tới Đà Nẵng nên không phải nối thêm một chặng nội địa.'),
  ('a3000000-0000-4000-8000-000003020000','da','Ankomst til Da Nang','Chaufføren venter i ankomsthallen med navneskilt. Første nat bor I ved stranden, tyve minutter fra lufthavnen.'),
  ('a3000000-0000-4000-8000-000003020000','vi','Tới Đà Nẵng','Lái xe chờ ở sảnh đến, có bảng tên. Đêm đầu ở bên biển, cách sân bay hai mươi phút.'),
  ('a3000000-0000-4000-8000-000003030000','da','Ind til Hoi An','En halv times kørsel ind til Hoi An, og indtjekning fra klokken fjorten. Resten af dagen bestemmer I selv over.'),
  ('a3000000-0000-4000-8000-000003030000','vi','Vào Hội An','Nửa tiếng xe vào Hội An, nhận phòng từ hai giờ chiều. Thời gian còn lại trong ngày là của bạn.'),
  ('a3000000-0000-4000-8000-000003040000','da','Skrædder og gammel bydel','Målene tages om formiddagen hos en skrædder vi bruger fast, og der prøves af to dage senere. Bagefter er den gamle bydel til fods.'),
  ('a3000000-0000-4000-8000-000003040000','vi','Thợ may và phố cổ','Buổi sáng đo ở một tiệm may chúng tôi hay dùng, hai hôm sau thì thử. Sau đó đi bộ trong phố cổ.'),
  ('a3000000-0000-4000-8000-000003050000','da','Fri dag','Cyklerne står i receptionen og koster ikke noget. Stranden er fire kilometer væk ad en flad vej mellem rismarker.'),
  ('a3000000-0000-4000-8000-000003050000','vi','Ngày tự do','Xe đạp để ở quầy lễ tân, không mất tiền. Biển cách bốn cây số, đường bằng phẳng chạy giữa ruộng lúa.'),
  ('a3000000-0000-4000-8000-000003060000','da','Marked og madlavning','Vi køber ind på markedet klokken syv, mens der stadig kommer varer ind. Madlavningen begynder klokken elleve, og man spiser det man har lavet.'),
  ('a3000000-0000-4000-8000-000003060000','vi','Đi chợ và nấu ăn','Bảy giờ đi chợ, lúc hàng còn đang về. Mười một giờ vào bếp, và bữa trưa là chính món mình nấu.'),
  ('a3000000-0000-4000-8000-000003070000','da','Videre til Hue','Kørsel op over Hai Van-passet med stop ved udsigten. Vi er i Hue til frokost, så eftermiddagen er fri ved floden.'),
  ('a3000000-0000-4000-8000-000003070000','vi','Ra Huế','Đi xe lên qua đèo Hải Vân, dừng ở chỗ nhìn ra biển. Tới Huế kịp bữa trưa nên buổi chiều rảnh bên sông.'),
  ('a3000000-0000-4000-8000-000003080000','da','Kejsergravene','To kejsergrave om formiddagen med egen chauffør, så I bestemmer hvor længe. Eftermiddagen går med en bådtur på Parfumefloden.'),
  ('a3000000-0000-4000-8000-000003080000','vi','Lăng tẩm','Sáng đi hai lăng vua, có xe riêng nên dừng bao lâu là tuỳ bạn. Chiều đi thuyền trên sông Hương.'),
  ('a3000000-0000-4000-8000-000003090000','da','Hjemrejse','Transfer til lufthavnen i Da Nang, halvanden time før afgang. Chaufføren henter på hotellet efter morgenmaden.'),
  ('a3000000-0000-4000-8000-000003090000','vi','Về nhà','Xe đưa ra sân bay Đà Nẵng, tới trước giờ bay một tiếng rưỡi. Lái xe đón tại khách sạn sau bữa sáng.')
ON CONFLICT DO NOTHING;

INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000009', 1, 1),
  ('f0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004', 4, 2),
  ('f0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000003', 2, 3)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------- P4, 10 ngày, Sa Pa
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000004010000', 'f0000000-0000-4000-8000-000000000004',  1, NULL, NULL),
  ('a3000000-0000-4000-8000-000004020000', 'f0000000-0000-4000-8000-000000000004',  2, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000004030000', 'f0000000-0000-4000-8000-000000000004',  3, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000004040000', 'f0000000-0000-4000-8000-000000000004',  4, NULL, NULL),
  ('a3000000-0000-4000-8000-000004050000', 'f0000000-0000-4000-8000-000000000004',  5, 'e0000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000005'),
  ('a3000000-0000-4000-8000-000004060000', 'f0000000-0000-4000-8000-000000000004',  6, 'e0000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000005'),
  ('a3000000-0000-4000-8000-000004070000', 'f0000000-0000-4000-8000-000000000004',  7, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000004080000', 'f0000000-0000-4000-8000-000000000004',  8, 'e0000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000002'),
  ('a3000000-0000-4000-8000-000004090000', 'f0000000-0000-4000-8000-000000000004',  9, 'e0000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001'),
  ('a3000000-0000-4000-8000-000004100000', 'f0000000-0000-4000-8000-000000000004', 10, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000004010000','da','Afrejse fra København','Aftenafgang fra København med ét skift. Vi lander i Hanoi næste formiddag, så den første dag i landet ikke går til spilde.'),
  ('a3000000-0000-4000-8000-000004010000','vi','Khởi hành từ Copenhagen','Bay tối từ Copenhagen, quá cảnh một chặng. Sáng hôm sau tới Hà Nội, nên ngày đầu ở Việt Nam không bị mất.'),
  ('a3000000-0000-4000-8000-000004020000','da','Ankomst til Hanoi','Indkvartering midt i byen og en kort tur rundt i kvarteret inden aftensmaden. Ikke mere end det — der er ni dage tilbage.'),
  ('a3000000-0000-4000-8000-000004020000','vi','Tới Hà Nội','Nhận phòng ở giữa thành phố và đi một vòng quanh khu phố trước bữa tối. Chỉ vậy thôi — còn chín ngày nữa.'),
  ('a3000000-0000-4000-8000-000004030000','da','Hanoi','Litteraturtemplet om formiddagen og søen bagefter, alt sammen til fods. Om aftenen er der vandmarionetteater for dem der vil med.'),
  ('a3000000-0000-4000-8000-000004030000','vi','Hà Nội','Sáng đi Văn Miếu rồi ra hồ, tất cả đều đi bộ. Buổi tối ai muốn thì đi xem múa rối nước.'),
  ('a3000000-0000-4000-8000-000004040000','da','Nattog til Lao Cai','Toget mod Lao Cai kører klokken ti om aftenen fra Hanoi. Vi har hele kupeer, så ingen deler køje med fremmede.'),
  ('a3000000-0000-4000-8000-000004040000','vi','Tàu đêm lên Lào Cai','Tàu đi Lào Cai rời Hà Nội lúc mười giờ tối. Chúng tôi đặt trọn khoang nên không ai phải nằm chung với người lạ.'),
  ('a3000000-0000-4000-8000-000004050000','da','Op i bjergene','En times kørsel op fra stationen, og så er vi over skyerne. Resten af dagen er kort vandring og udsigt over dalen.'),
  ('a3000000-0000-4000-8000-000004050000','vi','Lên núi','Từ ga đi xe thêm một tiếng là đã ở trên mây. Còn lại trong ngày là đi bộ ngắn và ngắm thung lũng.'),
  ('a3000000-0000-4000-8000-000004060000','da','Vandring til landsbyerne','Fem timer ned gennem terrasserne til to landsbyer, med frokost undervejs hos en familie. Turen er nedad hele vejen, men stien er ujævn.'),
  ('a3000000-0000-4000-8000-000004060000','vi','Đi bộ xuống bản','Năm tiếng đi xuống qua ruộng bậc thang, ghé hai bản, ăn trưa ở nhà dân. Đường toàn xuống dốc nhưng lối đi gồ ghề.'),
  ('a3000000-0000-4000-8000-000004070000','da','Retur til Hanoi','Ned ad motorvejen til Hanoi, fem timer med et stop undervejs. Aftenen er fri, og bagagen står allerede på hotellet.'),
  ('a3000000-0000-4000-8000-000004070000','vi','Về Hà Nội','Về Hà Nội theo cao tốc, năm tiếng, có một chặng nghỉ. Buổi tối tự do, và hành lý đã sẵn ở khách sạn.'),
  ('a3000000-0000-4000-8000-000004080000','da','Halong-bugten','Ombordstigning i Halong ved middagstid og sejlads ud mellem klipperne. Aftensmaden spises på dækket hvis vejret tillader det.'),
  ('a3000000-0000-4000-8000-000004080000','vi','Vịnh Hạ Long','Trưa lên tàu ở Hạ Long rồi ra giữa những khối đá. Trời thuận thì bữa tối dọn trên boong.'),
  ('a3000000-0000-4000-8000-000004090000','da','Hanoi på egen hånd','Tilbage i Hanoi ved frokosttid. Resten af dagen er jeres, og rejselederen kan anbefale steder inden for gåafstand.'),
  ('a3000000-0000-4000-8000-000004090000','vi','Hà Nội tự do','Về tới Hà Nội lúc trưa. Thời gian còn lại là của bạn, và trưởng đoàn gợi ý được vài chỗ đi bộ tới được.'),
  ('a3000000-0000-4000-8000-000004100000','da','Hjemrejse','Transfer til lufthavnen og hjemkomst til København samme aften. Værelserne kan beholdes til klokken tolv.'),
  ('a3000000-0000-4000-8000-000004100000','vi','Về nhà','Xe ra sân bay, tối cùng ngày về tới Copenhagen. Phòng giữ được tới mười hai giờ trưa.')
ON CONFLICT DO NOTHING;

INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000001', 4, 1),
  ('f0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000005', 2, 2),
  ('f0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000002', 1, 3)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------- P5, 8 ngày, miền Trung
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000005010000', 'f0000000-0000-4000-8000-000000000005', 1, NULL, NULL),
  ('a3000000-0000-4000-8000-000005020000', 'f0000000-0000-4000-8000-000000000005', 2, 'e0000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003'),
  ('a3000000-0000-4000-8000-000005030000', 'f0000000-0000-4000-8000-000000000005', 3, 'e0000000-0000-4000-8000-000000000007', 'a1000000-0000-4000-8000-000000000003'),
  ('a3000000-0000-4000-8000-000005040000', 'f0000000-0000-4000-8000-000000000005', 4, 'e0000000-0000-4000-8000-000000000008', 'a1000000-0000-4000-8000-000000000009'),
  ('a3000000-0000-4000-8000-000005050000', 'f0000000-0000-4000-8000-000000000005', 5, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000005060000', 'f0000000-0000-4000-8000-000000000005', 6, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000005070000', 'f0000000-0000-4000-8000-000000000005', 7, 'e0000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000004'),
  ('a3000000-0000-4000-8000-000005080000', 'f0000000-0000-4000-8000-000000000005', 8, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000005010000','da','Afrejse fra København','Aftenafgang fra København med ét skift. Vi flyver ind til Hue via Da Nang og er fremme først på eftermiddagen dagen efter.'),
  ('a3000000-0000-4000-8000-000005010000','vi','Khởi hành từ Copenhagen','Bay tối từ Copenhagen, quá cảnh một chặng. Vào Huế qua ngả Đà Nẵng, đầu giờ chiều hôm sau là tới.'),
  ('a3000000-0000-4000-8000-000005020000','da','Ankomst til Hue','Indkvartering på et gammelt fransk hus ved floden. Første aften er der ikke andet program end en gåtur langs vandet.'),
  ('a3000000-0000-4000-8000-000005020000','vi','Tới Huế','Nhận phòng ở một ngôi nhà Pháp cũ bên sông. Tối đầu tiên không có chương trình nào ngoài đi bộ dọc bờ nước.'),
  ('a3000000-0000-4000-8000-000005030000','da','Kejserbyen','Citadellet og Den Forbudte Purpurby om formiddagen, hvor der er langt mellem skyggerne. Om eftermiddagen en dragebåd op ad floden.'),
  ('a3000000-0000-4000-8000-000005030000','vi','Kinh thành','Sáng đi Đại Nội và Tử Cấm Thành, chỗ này ít bóng râm. Chiều đi thuyền rồng ngược sông.'),
  ('a3000000-0000-4000-8000-000005040000','da','Over passet til Da Nang','Over Hai Van-passet med stop på toppen, hvor man ser kysten begge veje. Marmorbjergene inden vi tjekker ind ved stranden.'),
  ('a3000000-0000-4000-8000-000005040000','vi','Qua đèo về Đà Nẵng','Qua đèo Hải Vân, dừng trên đỉnh nhìn được cả hai phía bờ biển. Ghé Ngũ Hành Sơn trước khi về nhận phòng bên biển.'),
  ('a3000000-0000-4000-8000-000005050000','da','Videre til Hoi An','En halv times kørsel ind til Hoi An. Eftermiddagen går i den gamle bydel, som er lukket for biler hele døgnet.'),
  ('a3000000-0000-4000-8000-000005050000','vi','Vào Hội An','Nửa tiếng xe vào Hội An. Chiều đi trong phố cổ, nơi cấm xe suốt cả ngày lẫn đêm.'),
  ('a3000000-0000-4000-8000-000005060000','da','My Son','Chamtårnene i My Son om morgenen, inden det bliver for varmt til at gå rundt. Tilbage i Hoi An til frokost.'),
  ('a3000000-0000-4000-8000-000005060000','vi','Mỹ Sơn','Sáng đi tháp Chăm ở Mỹ Sơn, trước khi trời nắng tới mức không đi nổi. Về Hội An kịp bữa trưa.'),
  ('a3000000-0000-4000-8000-000005070000','da','Fri dag ved kysten','Fri dag. Stranden er fire kilometer væk, cyklerne er gratis, og skrædderne kan nå at sy en skjorte færdig.'),
  ('a3000000-0000-4000-8000-000005070000','vi','Ngày tự do bên biển','Ngày tự do. Biển cách bốn cây số, xe đạp miễn phí, và thợ may vẫn kịp xong một cái áo.'),
  ('a3000000-0000-4000-8000-000005080000','da','Hjemrejse','Transfer til Da Nang lufthavn, fyrre minutters kørsel. Vi flyver hjem via samme rute som på udturen.'),
  ('a3000000-0000-4000-8000-000005080000','vi','Về nhà','Xe ra sân bay Đà Nẵng, đi bốn mươi phút. Bay về theo đúng đường lúc đi, quá cảnh một chặng.')
ON CONFLICT DO NOTHING;

INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000003', 2, 1),
  ('f0000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000009', 1, 2),
  ('f0000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000004', 3, 3)
ON CONFLICT DO NOTHING;

-- ------------------------------------------ P7, 5 ngày, du thuyền Cửu Long
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000007010000', 'f0000000-0000-4000-8000-000000000007', 1, 'e0000000-0000-4000-8000-000000000009', NULL),
  ('a3000000-0000-4000-8000-000007020000', 'f0000000-0000-4000-8000-000000000007', 2, 'e0000000-0000-4000-8000-000000000004', NULL),
  ('a3000000-0000-4000-8000-000007030000', 'f0000000-0000-4000-8000-000000000007', 3, 'e0000000-0000-4000-8000-000000000004', NULL),
  ('a3000000-0000-4000-8000-000007040000', 'f0000000-0000-4000-8000-000000000007', 4, 'e0000000-0000-4000-8000-000000000004', NULL),
  ('a3000000-0000-4000-8000-000007050000', 'f0000000-0000-4000-8000-000000000007', 5, 'e0000000-0000-4000-8000-000000000004', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000007010000','da','Ombordstigning i Saigon','Vi går ombord i Saigon sidst på eftermiddagen og sejler ud mens byen stadig er lys. Aftensmaden spises mens husene bliver færre.'),
  ('a3000000-0000-4000-8000-000007010000','vi','Lên tàu ở Sài Gòn','Lên tàu ở Sài Gòn vào cuối chiều, tàu rời bến lúc trời còn sáng. Ăn tối trong lúc nhà cửa hai bên thưa dần.'),
  ('a3000000-0000-4000-8000-000007020000','da','Cai Be','Frugthaverne ved Cai Be om formiddagen, og bagefter et værksted hvor riskiks stadig laves i hånden. Vi sejler videre efter frokost.'),
  ('a3000000-0000-4000-8000-000007020000','vi','Cái Bè','Sáng vào vườn trái cây ở Cái Bè, sau đó ghé một lò tráng bánh vẫn làm bằng tay. Ăn trưa xong tàu đi tiếp.'),
  ('a3000000-0000-4000-8000-000007030000','da','Sa Dec','Sa Dec er blomsternes by, og markerne ligger helt ned til floden. Vi ser også huset fra romanen, som stadig står.'),
  ('a3000000-0000-4000-8000-000007030000','vi','Sa Đéc','Sa Đéc là xứ hoa, và những vườn hoa chạy xuống tới sát mé sông. Ghé cả ngôi nhà trong tiểu thuyết, nay vẫn còn.'),
  ('a3000000-0000-4000-8000-000007040000','da','Chau Doc','Flydende fiskefarme om morgenen, hvor der bor familier oven på dammene. Om eftermiddagen op på Sam-bjerget med udsigt til Cambodja.'),
  ('a3000000-0000-4000-8000-000007040000','vi','Châu Đốc','Sáng đi bè cá trên sông, nơi cả gia đình sống ngay trên mặt ao. Chiều lên núi Sam, từ đó nhìn sang được Campuchia.'),
  ('a3000000-0000-4000-8000-000007050000','da','I land i Can Tho','Cai Rang-markedet ved daggry, hvor handlen foregår fra båd til båd. Vi går i land i Can Tho efter morgenmaden.'),
  ('a3000000-0000-4000-8000-000007050000','vi','Lên bờ ở Cần Thơ','Rạng sáng ra chợ nổi Cái Răng, mua bán diễn ra từ ghe này sang ghe khác. Ăn sáng xong thì lên bờ ở Cần Thơ.')
ON CONFLICT DO NOTHING;


-- ------------------------------------- P6, 12 ngày, tour riêng cho gia đình
--
-- `hotel_id` NULL ở MỌI ngày, và không có dòng `product_hotel_stay` nào. Đó là
-- sự thật nghiệp vụ của loại này chứ không phải dữ liệu thiếu: lộ trình mẫu
-- chưa chốt khách sạn, vì khách sạn chọn cùng khách sau khi họ nói muốn gì.
-- Nhờ vậy tab khách sạn có một **trạng thái rỗng thật** để nhìn.
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000006010000', 'f0000000-0000-4000-8000-000000000006',  1, NULL, NULL),
  ('a3000000-0000-4000-8000-000006020000', 'f0000000-0000-4000-8000-000000000006',  2, 'e0000000-0000-4000-8000-000000000001', NULL),
  ('a3000000-0000-4000-8000-000006030000', 'f0000000-0000-4000-8000-000000000006',  3, 'e0000000-0000-4000-8000-000000000001', NULL),
  ('a3000000-0000-4000-8000-000006040000', 'f0000000-0000-4000-8000-000000000006',  4, 'e0000000-0000-4000-8000-000000000006', NULL),
  ('a3000000-0000-4000-8000-000006050000', 'f0000000-0000-4000-8000-000000000006',  5, 'e0000000-0000-4000-8000-000000000006', NULL),
  ('a3000000-0000-4000-8000-000006060000', 'f0000000-0000-4000-8000-000000000006',  6, 'e0000000-0000-4000-8000-000000000003', NULL),
  ('a3000000-0000-4000-8000-000006070000', 'f0000000-0000-4000-8000-000000000006',  7, 'e0000000-0000-4000-8000-000000000003', NULL),
  ('a3000000-0000-4000-8000-000006080000', 'f0000000-0000-4000-8000-000000000006',  8, 'e0000000-0000-4000-8000-000000000003', NULL),
  ('a3000000-0000-4000-8000-000006090000', 'f0000000-0000-4000-8000-000000000006',  9, 'e0000000-0000-4000-8000-000000000010', NULL),
  ('a3000000-0000-4000-8000-000006100000', 'f0000000-0000-4000-8000-000000000006', 10, 'e0000000-0000-4000-8000-000000000010', NULL),
  ('a3000000-0000-4000-8000-000006110000', 'f0000000-0000-4000-8000-000000000006', 11, 'e0000000-0000-4000-8000-000000000010', NULL),
  ('a3000000-0000-4000-8000-000006120000', 'f0000000-0000-4000-8000-000000000006', 12, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000006010000','da','Afrejse','Aftenafgang, hvilket er med vilje: børn sover som regel det meste af strækningen. Vi lander i Hanoi ved middagstid.'),
  ('a3000000-0000-4000-8000-000006010000','vi','Khởi hành','Bay buổi tối, và đó là chủ ý: trẻ con thường ngủ gần hết chặng. Tới Hà Nội vào khoảng giữa trưa.'),
  ('a3000000-0000-4000-8000-000006020000','da','Ankomst til Hanoi','Der er ikke noget program første dag. Hotellet har pool, og vi anbefaler tidlig aftensmad frem for at holde nogen vågen.'),
  ('a3000000-0000-4000-8000-000006020000','vi','Tới Hà Nội','Ngày đầu không có chương trình nào. Khách sạn có bể bơi, và nên ăn tối sớm thay vì cố giữ ai đó thức.'),
  ('a3000000-0000-4000-8000-000006030000','da','Vandmarionetter og søen','En kort formiddag i den gamle bydel — ikke mere end børnene kan holde til. Om eftermiddagen vandmarionetteater, som varer knap en time.'),
  ('a3000000-0000-4000-8000-000006030000','vi','Múa rối nước và hồ','Buổi sáng ngắn trong phố cổ, không dài hơn sức của bọn trẻ. Chiều đi xem múa rối nước, chưa tới một tiếng.'),
  ('a3000000-0000-4000-8000-000006040000','da','Ud til Ninh Binh','To timers kørsel ud til Ninh Binh, med et stop undervejs. Resten af dagen er cykler, geder på klipperne og ingen faste tider.'),
  ('a3000000-0000-4000-8000-000006040000','vi','Về Ninh Bình','Hai tiếng xe về Ninh Bình, dọc đường có một chặng nghỉ. Còn lại trong ngày là xe đạp, dê trên núi đá và không giờ giấc gì.'),
  ('a3000000-0000-4000-8000-000006050000','da','Robåd gennem grotterne','To timer på floden i robåd gennem tre gennemskårne grotter. Roersken bruger fødderne til årerne, og det er dagens største hit.'),
  ('a3000000-0000-4000-8000-000006050000','vi','Đi đò xuyên hang','Hai tiếng trên sông bằng đò, chui qua ba cái hang xuyên núi. Người chèo dùng chân để đẩy mái chèo, và bọn trẻ nhớ nhất chuyện đó.'),
  ('a3000000-0000-4000-8000-000006060000','da','Fly til Hoi An','Morgenfly ned til Da Nang og en halv times kørsel ind til Hoi An. Eftermiddagen er ved stranden, og der er ikke mere program.'),
  ('a3000000-0000-4000-8000-000006060000','vi','Bay vào Hội An','Bay sáng vào Đà Nẵng rồi nửa tiếng xe tới Hội An. Chiều ra biển, và không có chương trình nào nữa.'),
  ('a3000000-0000-4000-8000-000006070000','da','Lanterner og skrædder','Der laves papirlanterner om formiddagen, og alle får deres egen med hjem. Skrædderne kan sy børnetøj færdigt på to dage.'),
  ('a3000000-0000-4000-8000-000006070000','vi','Đèn lồng và thợ may','Buổi sáng có lớp làm đèn lồng giấy, ai cũng mang cái của mình về. Thợ may làm xong đồ trẻ con trong hai ngày.'),
  ('a3000000-0000-4000-8000-000006080000','da','Fri dag','Fri dag uden aftaler. Cykel til stranden er det de fleste vælger, og guiden tager telefonen hvis der bliver brug for noget.'),
  ('a3000000-0000-4000-8000-000006080000','vi','Ngày tự do','Ngày tự do, không hẹn gì. Phần lớn chọn đạp xe ra biển, và hướng dẫn viên vẫn nghe máy nếu cần gì.'),
  ('a3000000-0000-4000-8000-000006090000','da','Videre til Phu Quoc','Sidste flyvetur, halvanden time ned til øen. Herfra står der ikke mere i programmet, og det er sådan det er tænkt.'),
  ('a3000000-0000-4000-8000-000006090000','vi','Ra Phú Quốc','Chặng bay cuối, một tiếng rưỡi ra đảo. Từ đây chương trình không ghi gì nữa, và đó là chủ ý.'),
  ('a3000000-0000-4000-8000-000006100000','da','Strand','Strand. Hotellet har egen strandstrækning, og der er skygge nok til at man kan blive liggende hele dagen.'),
  ('a3000000-0000-4000-8000-000006100000','vi','Biển','Biển. Khách sạn có bãi riêng, đủ bóng râm để nằm cả ngày, và bữa trưa dọn ngay tại bãi.'),
  ('a3000000-0000-4000-8000-000006110000','da','Strand igen','Strand igen, eller kabelbanen ud til Hon Thom hvis nogen bliver rastløs. Turen tager tyve minutter hver vej.'),
  ('a3000000-0000-4000-8000-000006110000','vi','Lại biển','Lại biển, hoặc đi cáp treo ra Hòn Thơm nếu có ai bắt đầu chán. Mỗi chiều đi mất hai mươi phút.'),
  ('a3000000-0000-4000-8000-000006120000','da','Hjemrejse','Transfer til lufthavnen. Vi flyver hjem via Saigon, og der er tid nok imellem de to fly til at spise i ro.'),
  ('a3000000-0000-4000-8000-000006120000','vi','Về nhà','Xe ra sân bay. Bay về qua Sài Gòn, và giữa hai chặng đủ thời gian để ăn uống thong thả.')
ON CONFLICT DO NOTHING;

-- ------------------------------------------- P10, 7 ngày, Phú Quốc tự túc
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000010010000', 'f0000000-0000-4000-8000-000000000010', 1, NULL, NULL),
  ('a3000000-0000-4000-8000-000010020000', 'f0000000-0000-4000-8000-000000000010', 2, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000010030000', 'f0000000-0000-4000-8000-000000000010', 3, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000010040000', 'f0000000-0000-4000-8000-000000000010', 4, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000010050000', 'f0000000-0000-4000-8000-000000000010', 5, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000010060000', 'f0000000-0000-4000-8000-000000000010', 6, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000010070000', 'f0000000-0000-4000-8000-000000000010', 7, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000010010000','da','Afrejse fra København','Aftenafgang fra København med ét skift og en indenrigsstrækning til sidst. Hele rejsen tager omkring seksten timer.'),
  ('a3000000-0000-4000-8000-000010010000','vi','Khởi hành từ Copenhagen','Bay tối từ Copenhagen, quá cảnh một chặng rồi thêm một chặng nội địa. Cả hành trình mất khoảng mười sáu tiếng.'),
  ('a3000000-0000-4000-8000-000010020000','da','Ankomst','Chaufføren venter i ankomsthallen, og der er tyve minutter til hotellet. Indtjekning fra klokken fjorten, bagagen kan afleveres før.'),
  ('a3000000-0000-4000-8000-000010020000','vi','Tới nơi','Lái xe chờ ở sảnh đến, về khách sạn mất hai mươi phút. Nhận phòng từ hai giờ chiều, gửi hành lý trước cũng được.'),
  ('a3000000-0000-4000-8000-000010030000','da','Fri dag','Der står ikke noget i programmet, og det er meningen. Hotellet har egen strandstrækning, og solsengene koster ikke ekstra.'),
  ('a3000000-0000-4000-8000-000010030000','vi','Ngày tự do','Chương trình không ghi gì, và đó là chủ ý. Khách sạn có bãi biển riêng, ghế tắm nắng không tính thêm tiền.'),
  ('a3000000-0000-4000-8000-000010040000','da','Fri dag','Fri dag. Bådtur til de sydlige øer kan bestilles i receptionen dagen før og tager omkring seks timer med frokost.'),
  ('a3000000-0000-4000-8000-000010040000','vi','Ngày tự do','Ngày tự do. Muốn đi tàu ra các đảo phía nam thì đặt ở quầy lễ tân từ hôm trước, đi khoảng sáu tiếng, có bữa trưa.'),
  ('a3000000-0000-4000-8000-000010050000','da','Fri dag','Fri dag. Nattemarkedet i Duong Dong åbner klokken sytten, og taxaen ind til byen tager under et kvarter.'),
  ('a3000000-0000-4000-8000-000010050000','vi','Ngày tự do','Ngày tự do. Chợ đêm Dương Đông mở từ năm giờ chiều, đi taxi vào thị trấn chưa tới mười lăm phút.'),
  ('a3000000-0000-4000-8000-000010060000','da','Sidste dag','Udtjekning er klokken tolv, men stranden er der stadig, og bagagen kan stå i receptionen indtil afhentning.'),
  ('a3000000-0000-4000-8000-000010060000','vi','Ngày cuối','Trả phòng lúc mười hai giờ trưa, nhưng biển thì vẫn còn đó, và hành lý gửi được ở quầy tới lúc xe đón.'),
  ('a3000000-0000-4000-8000-000010070000','da','Hjemrejse','Transfer til lufthavnen, tyve minutters kørsel. Hjemrejsen går via Saigon med god tid mellem de to fly.'),
  ('a3000000-0000-4000-8000-000010070000','vi','Về nhà','Xe ra sân bay, đi hai mươi phút. Về theo ngả Sài Gòn, giữa hai chặng bay có nhiều thời gian.')
ON CONFLICT DO NOTHING;

INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007', 5, 1)
ON CONFLICT DO NOTHING;

-- ------------------------------------ P11, 12 ngày, miền Nam và Cửu Long
-- Ngày 5 ngủ nhà dân bên sông: `destination_id` có, `hotel_id` NULL. Nhà dân
-- KHÔNG phải khách sạn, nên nó không có dòng trong `hotel` và cũng không được
-- tính vào `product_hotel_stay` — đó chính là lý do quy tắc kiểm 4 đếm "số đêm
-- CÓ khách sạn" chứ không đếm `duration_days - 1`.
INSERT INTO itinerary_day (id, product_id, day_number, destination_id, hotel_id) VALUES
  ('a3000000-0000-4000-8000-000011010000', 'f0000000-0000-4000-8000-000000000011',  1, NULL, NULL),
  ('a3000000-0000-4000-8000-000011020000', 'f0000000-0000-4000-8000-000000000011',  2, 'e0000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000010'),
  ('a3000000-0000-4000-8000-000011030000', 'f0000000-0000-4000-8000-000000000011',  3, 'e0000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000010'),
  ('a3000000-0000-4000-8000-000011040000', 'f0000000-0000-4000-8000-000000000011',  4, 'e0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000006'),
  ('a3000000-0000-4000-8000-000011050000', 'f0000000-0000-4000-8000-000000000011',  5, 'e0000000-0000-4000-8000-000000000004', NULL),
  ('a3000000-0000-4000-8000-000011060000', 'f0000000-0000-4000-8000-000000000011',  6, 'e0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000006'),
  ('a3000000-0000-4000-8000-000011070000', 'f0000000-0000-4000-8000-000000000011',  7, 'e0000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000006'),
  ('a3000000-0000-4000-8000-000011080000', 'f0000000-0000-4000-8000-000000000011',  8, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000011090000', 'f0000000-0000-4000-8000-000000000011',  9, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000011100000', 'f0000000-0000-4000-8000-000000000011', 10, 'e0000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000007'),
  ('a3000000-0000-4000-8000-000011110000', 'f0000000-0000-4000-8000-000000000011', 11, 'e0000000-0000-4000-8000-000000000009', 'a1000000-0000-4000-8000-000000000010'),
  ('a3000000-0000-4000-8000-000011120000', 'f0000000-0000-4000-8000-000000000011', 12, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_day_translation (itinerary_day_id, locale, title, description) VALUES
  ('a3000000-0000-4000-8000-000011010000','da','Afrejse fra København','Aftenafgang fra København mod syd med ét skift undervejs. Vi lander i Saigon om formiddagen dagen efter.'),
  ('a3000000-0000-4000-8000-000011010000','vi','Khởi hành từ Copenhagen','Bay tối từ Copenhagen xuống phía nam, quá cảnh một chặng. Sáng hôm sau tới Sài Gòn.'),
  ('a3000000-0000-4000-8000-000011020000','da','Ankomst til Saigon','Indkvartering midt i byen og ikke mere end en gåtur på ti minutter. Første dag i troperne skal ikke bruges på program.'),
  ('a3000000-0000-4000-8000-000011020000','vi','Tới Sài Gòn','Nhận phòng ở giữa thành phố, rồi chỉ đi bộ chừng mười phút. Ngày đầu ở xứ nóng không nên dùng vào chương trình.'),
  ('a3000000-0000-4000-8000-000011030000','da','Cu Chi og byen','Cu Chi-tunnelerne om formiddagen, hvor man kan gå ned i et stykke af dem. Om eftermiddagen Ben Thanh-markedet og byen til fods.'),
  ('a3000000-0000-4000-8000-000011030000','vi','Củ Chi và thành phố','Sáng đi địa đạo Củ Chi, xuống được một đoạn thật. Chiều đi chợ Bến Thành và đi bộ trong thành phố.'),
  ('a3000000-0000-4000-8000-000011040000','da','Ned i deltaet','Tre timer i bil ned i deltaet, og landskabet bliver fladt og grønt undervejs. Vi bor ved floden i Can Tho.'),
  ('a3000000-0000-4000-8000-000011040000','vi','Xuống miền Tây','Ba tiếng xe xuống miền Tây, dọc đường cảnh vật thành phẳng và xanh. Ở lại bên sông tại Cần Thơ.'),
  ('a3000000-0000-4000-8000-000011050000','da','Nat hos en familie ved floden','Vi sover i et hus på pæle hos en familie ved floden. Der er myggenet og ingen aircondition, og maden laves i det køkken der er.'),
  ('a3000000-0000-4000-8000-000011050000','vi','Đêm ở nhà dân bên sông','Ngủ trong một căn nhà sàn của một gia đình bên sông. Có màn, không có điều hoà, và bữa ăn nấu bằng đúng cái bếp nhà có.'),
  ('a3000000-0000-4000-8000-000011060000','da','Cai Rang','Op klokken fire og ude på Cai Rang klokken fem, mens markedet er på sit største. Tilbage til morgenmad omkring klokken otte.'),
  ('a3000000-0000-4000-8000-000011060000','vi','Cái Răng','Bốn giờ dậy, năm giờ đã ra tới Cái Răng, lúc chợ đông nhất. Khoảng tám giờ về ăn sáng.'),
  ('a3000000-0000-4000-8000-000011070000','da','Kanaler og frugthaver','Smalle både gennem kanaler der er for små til motor, så det eneste man hører er årerne. Frokost i en frugthave undervejs.'),
  ('a3000000-0000-4000-8000-000011070000','vi','Kênh rạch và vườn cây','Xuồng nhỏ đi trong những con rạch hẹp không chạy máy được, nên chỉ nghe tiếng mái chèo. Ăn trưa trong một vườn cây dọc đường.'),
  ('a3000000-0000-4000-8000-000011080000','da','Fly til Phu Quoc','Fyrre minutter i luften ud til øen, og derefter er der ikke flere lange køreture på turen. Eftermiddagen er ved stranden.'),
  ('a3000000-0000-4000-8000-000011080000','vi','Bay ra Phú Quốc','Bốn mươi phút bay ra đảo, và từ đó không còn chặng xe dài nào nữa. Buổi chiều ở bên biển.'),
  ('a3000000-0000-4000-8000-000011090000','da','Fri dag','Fri dag uden program. Bådtur til de sydlige øer kan tilkøbes, men de fleste bliver liggende efter fire dage i deltaet.'),
  ('a3000000-0000-4000-8000-000011090000','vi','Ngày tự do','Ngày tự do, không chương trình. Muốn đi tàu ra đảo phía nam thì đặt thêm, nhưng sau bốn ngày ở miền Tây thì phần lớn chọn nằm nghỉ.'),
  ('a3000000-0000-4000-8000-000011100000','da','Fri dag','Sidste hele dag ved vandet. Hotellet har egen strandstrækning, og solnedgangen ses fra samme sted man har ligget hele dagen.'),
  ('a3000000-0000-4000-8000-000011100000','vi','Ngày tự do','Ngày trọn vẹn cuối cùng bên biển. Khách sạn có bãi riêng, và hoàng hôn nhìn được từ đúng chỗ đã nằm cả ngày.'),
  ('a3000000-0000-4000-8000-000011110000','da','Tilbage til Saigon','Eftermiddagsfly tilbage til Saigon, fyrre minutter. Om aftenen spiser hele gruppen sammen for sidste gang.'),
  ('a3000000-0000-4000-8000-000011110000','vi','Về lại Sài Gòn','Chiều bay về Sài Gòn, bốn mươi phút. Buổi tối cả đoàn ăn với nhau bữa cuối cùng.'),
  ('a3000000-0000-4000-8000-000011120000','da','Hjemrejse','Afgang om formiddagen og hjemkomst til København samme aften. Rejselederen følger med til check-in i lufthavnen.'),
  ('a3000000-0000-4000-8000-000011120000','vi','Về nhà','Bay buổi sáng, tối cùng ngày về tới Copenhagen. Trưởng đoàn đi cùng tới quầy làm thủ tục.')
ON CONFLICT DO NOTHING;

INSERT INTO product_hotel_stay (product_id, hotel_id, nights, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000011', 'a1000000-0000-4000-8000-000000000010', 3, 1),
  ('f0000000-0000-4000-8000-000000000011', 'a1000000-0000-4000-8000-000000000006', 3, 2),
  ('f0000000-0000-4000-8000-000000000011', 'a1000000-0000-4000-8000-000000000007', 3, 3)
ON CONFLICT DO NOTHING;

-- ==========================================================================
--                                          NGÀY KHỞI HÀNH và GIÁ cho P4 … P12
-- ==========================================================================
--
-- Không có dòng nào ở đây thì `price_from` là NULL, và cả chín sản phẩm mới sẽ
-- hiện "Liên hệ" — đúng về mặt hành vi nhưng vô dụng để nhìn bố cục: cột giá
-- trong lưới trông khác hẳn khi có số.
--
-- Ngày rải khắp năm 2027 và có **cả mùa cao lẫn mùa thấp với giá khác nhau**,
-- vì `price_from` lấy min() — một bộ dữ liệu mà mọi ngày cùng giá không bao giờ
-- chứng minh được trigger của V5 chọn đúng.
--
-- P6 CỐ TÌNH không có ngày nào: PRIVATE_TOUR bán qua báo giá, giá tham khảo
-- nằm ở `price_tier`. Đó là sản phẩm duy nhất mà nút CTA dẫn sang form báo giá.

INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                       base_status, capacity, seats_booked, departure_origin_id) VALUES
  -- P4, 10 ngày
  ('11110000-0000-4000-8000-000000000401', 'f0000000-0000-4000-8000-000000000004', 'DK', DATE '2027-02-06', DATE '2027-02-15', 10, 'OPEN',      18,  7, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000402', 'f0000000-0000-4000-8000-000000000004', 'DK', DATE '2027-04-03', DATE '2027-04-12', 10, 'FEW_SEATS', 18, 16, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000403', 'f0000000-0000-4000-8000-000000000004', 'DK', DATE '2027-09-11', DATE '2027-09-20', 10, 'OPEN',      18,  3, 'b0000000-0000-4000-8000-000000000002'),
  ('11110000-0000-4000-8000-000000000404', 'f0000000-0000-4000-8000-000000000004', 'DK', DATE '2027-10-16', DATE '2027-10-25', 10, 'PENDING',   18,  1, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000405', 'f0000000-0000-4000-8000-000000000004', 'VN', DATE '2027-04-03', DATE '2027-04-12', 10, 'OPEN',      24,  5, NULL),

  -- P5, 8 ngày
  ('11110000-0000-4000-8000-000000000501', 'f0000000-0000-4000-8000-000000000005', 'DK', DATE '2027-03-06', DATE '2027-03-13',  8, 'OPEN',      20,  9, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000502', 'f0000000-0000-4000-8000-000000000005', 'DK', DATE '2027-05-15', DATE '2027-05-22',  8, 'OPEN',      20,  4, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000503', 'f0000000-0000-4000-8000-000000000005', 'DK', DATE '2027-11-06', DATE '2027-11-13',  8, 'PENDING',   20,  2, 'b0000000-0000-4000-8000-000000000001'),

  -- P8 COMBO, 10 ngày
  ('11110000-0000-4000-8000-000000000801', 'f0000000-0000-4000-8000-000000000008', 'DK', DATE '2027-01-16', DATE '2027-01-25', 10, 'OPEN',      20,  6, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000802', 'f0000000-0000-4000-8000-000000000008', 'DK', DATE '2027-03-13', DATE '2027-03-22', 10, 'OPEN',      20, 11, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000803', 'f0000000-0000-4000-8000-000000000008', 'DK', DATE '2027-10-09', DATE '2027-10-18', 10, 'FEW_SEATS', 20, 18, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000000804', 'f0000000-0000-4000-8000-000000000008', 'VN', DATE '2027-03-13', DATE '2027-03-22', 10, 'OPEN',      20,  3, NULL),

  -- P9 DAY_TOUR: days = 1, nên return_date TRÙNG depart_date. ck_dep_dates
  -- (return = depart + days - 1) từ chối mọi cách viết khác.
  ('11110000-0000-4000-8000-000000000901', 'f0000000-0000-4000-8000-000000000009', 'DK', DATE '2027-02-10', DATE '2027-02-10',  1, 'OPEN',       8,  3, NULL),
  ('11110000-0000-4000-8000-000000000902', 'f0000000-0000-4000-8000-000000000009', 'DK', DATE '2027-02-17', DATE '2027-02-17',  1, 'SOLD_OUT',   8,  8, NULL),
  ('11110000-0000-4000-8000-000000000903', 'f0000000-0000-4000-8000-000000000009', 'DK', DATE '2027-03-03', DATE '2027-03-03',  1, 'OPEN',       8,  1, NULL),
  ('11110000-0000-4000-8000-000000000904', 'f0000000-0000-4000-8000-000000000009', 'VN', DATE '2027-02-10', DATE '2027-02-10',  1, 'OPEN',      12,  2, NULL),

  -- P10, 7 ngày
  ('11110000-0000-4000-8000-000000001001', 'f0000000-0000-4000-8000-000000000010', 'DK', DATE '2027-02-13', DATE '2027-02-19',  7, 'OPEN',      12,  4, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000001002', 'f0000000-0000-4000-8000-000000000010', 'DK', DATE '2027-04-17', DATE '2027-04-23',  7, 'OPEN',      12,  2, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000001003', 'f0000000-0000-4000-8000-000000000010', 'DK', DATE '2027-11-20', DATE '2027-11-26',  7, 'OPEN',      12,  0, 'b0000000-0000-4000-8000-000000000001'),

  -- P11, 12 ngày
  ('11110000-0000-4000-8000-000000001101', 'f0000000-0000-4000-8000-000000000011', 'DK', DATE '2027-01-23', DATE '2027-02-03', 12, 'OPEN',      22, 14, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000001102', 'f0000000-0000-4000-8000-000000000011', 'DK', DATE '2027-03-20', DATE '2027-03-31', 12, 'FEW_SEATS', 22, 20, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000001103', 'f0000000-0000-4000-8000-000000000011', 'DK', DATE '2027-10-02', DATE '2027-10-13', 12, 'OPEN',      22,  5, 'b0000000-0000-4000-8000-000000000001'),
  ('11110000-0000-4000-8000-000000001104', 'f0000000-0000-4000-8000-000000000011', 'VN', DATE '2027-03-20', DATE '2027-03-31', 12, 'OPEN',      25,  8, NULL),

  -- P12 DAY_TOUR
  ('11110000-0000-4000-8000-000000001201', 'f0000000-0000-4000-8000-000000000012', 'DK', DATE '2027-02-12', DATE '2027-02-12',  1, 'OPEN',      16,  6, NULL),
  ('11110000-0000-4000-8000-000000001202', 'f0000000-0000-4000-8000-000000000012', 'DK', DATE '2027-03-05', DATE '2027-03-05',  1, 'FEW_SEATS', 16, 14, NULL),
  ('11110000-0000-4000-8000-000000001203', 'f0000000-0000-4000-8000-000000000012', 'DK', DATE '2027-04-09', DATE '2027-04-09',  1, 'OPEN',      16,  2, NULL),
  ('11110000-0000-4000-8000-000000001204', 'f0000000-0000-4000-8000-000000000012', 'VN', DATE '2027-03-05', DATE '2027-03-05',  1, 'OPEN',      20,  4, NULL)
ON CONFLICT DO NOTHING;

-- P7 CRUISE: mỗi hạng cabin là một dòng departure RIÊNG trong cùng một ngày —
-- khoá duy nhất ux_departure_slot đã tính cabin_category vào, và đó là lý do
-- nó dùng COALESCE(cabin_category, '') chứ không dùng thẳng cột.
INSERT INTO departure (id, product_id, market, depart_date, return_date, days,
                       cabin_category, base_status, capacity, seats_booked) VALUES
  -- BỐN hạng cabin cho MỖI ngày khởi hành, ở CẢ HAI thị trường: quy tắc kiểm 7
  -- đếm theo ngày, nên một ngày thiếu hạng là một ngày đỏ.
  ('11110000-0000-4000-8000-000000000701', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-02-20', DATE '2027-02-24', 5, 'INSIDE',  'OPEN',      12,  4),
  ('11110000-0000-4000-8000-000000000702', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-02-20', DATE '2027-02-24', 5, 'OUTSIDE', 'OPEN',      10,  6),
  ('11110000-0000-4000-8000-000000000703', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-02-20', DATE '2027-02-24', 5, 'BALCONY', 'FEW_SEATS',  6,  5),
  ('11110000-0000-4000-8000-00000000070a', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-02-20', DATE '2027-02-24', 5, 'AQUA',    'SOLD_OUT',   4,  4),
  ('11110000-0000-4000-8000-000000000704', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-11-13', DATE '2027-11-17', 5, 'INSIDE',  'OPEN',      12,  1),
  ('11110000-0000-4000-8000-000000000705', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-11-13', DATE '2027-11-17', 5, 'OUTSIDE', 'OPEN',      10,  0),
  ('11110000-0000-4000-8000-00000000070b', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-11-13', DATE '2027-11-17', 5, 'BALCONY', 'OPEN',       6,  2),
  ('11110000-0000-4000-8000-00000000070c', 'f0000000-0000-4000-8000-000000000007', 'DK', DATE '2027-11-13', DATE '2027-11-17', 5, 'AQUA',    'OPEN',       4,  1),
  ('11110000-0000-4000-8000-000000000706', 'f0000000-0000-4000-8000-000000000007', 'VN', DATE '2027-02-20', DATE '2027-02-24', 5, 'INSIDE',  'OPEN',      12,  2),
  ('11110000-0000-4000-8000-00000000070d', 'f0000000-0000-4000-8000-000000000007', 'VN', DATE '2027-02-20', DATE '2027-02-24', 5, 'OUTSIDE', 'OPEN',      10,  3),
  ('11110000-0000-4000-8000-00000000070e', 'f0000000-0000-4000-8000-000000000007', 'VN', DATE '2027-02-20', DATE '2027-02-24', 5, 'BALCONY', 'OPEN',       6,  1),
  ('11110000-0000-4000-8000-00000000070f', 'f0000000-0000-4000-8000-000000000007', 'VN', DATE '2027-02-20', DATE '2027-02-24', 5, 'AQUA',    'OPEN',       4,  0)
ON CONFLICT DO NOTHING;

-- Giá. `price_from` KHÔNG đặt tay — trigger của V5 tính lại sau mỗi dòng dưới
-- đây, lấy min() của khách ADULT ở phòng đôi.
--
-- Dòng SINGLE của khối này chỉ có ở vài ngày, và phần còn lại được bù ở khối
-- "giá phòng đơn" cuối mục — đọc chú thích ở đó trước khi thêm ngày mới.
INSERT INTO departure_price (departure_id, pax_type_id, occupancy, amount, currency) VALUES
  ('11110000-0000-4000-8000-000000000401', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 21990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000401', 'a0000000-0000-4000-8000-000000000001', 'SINGLE', 25990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000401', 'a0000000-0000-4000-8000-000000000002', 'DOUBLE', 18990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000402', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 23490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000403', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 22490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000404', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 20990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000405', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE', 14500000, 'VND'),

  ('11110000-0000-4000-8000-000000000501', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 17990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000501', 'a0000000-0000-4000-8000-000000000001', 'SINGLE', 21490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000502', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 18990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000503', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 16990.00, 'DKK'),

  ('11110000-0000-4000-8000-000000000701', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',  9490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000702', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 11490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000703', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 14290.00, 'DKK'),
  ('11110000-0000-4000-8000-00000000070a', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 18990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000704', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',  8990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000705', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 10990.00, 'DKK'),
  ('11110000-0000-4000-8000-00000000070b', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 13490.00, 'DKK'),
  ('11110000-0000-4000-8000-00000000070c', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 17490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000706', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE',  9800000, 'VND'),
  ('11110000-0000-4000-8000-00000000070d', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE', 11900000, 'VND'),
  ('11110000-0000-4000-8000-00000000070e', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE', 14800000, 'VND'),
  ('11110000-0000-4000-8000-00000000070f', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE', 19500000, 'VND'),

  ('11110000-0000-4000-8000-000000000801', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 15990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000802', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 16990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000803', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 14990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000804', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE', 11200000, 'VND'),

  -- Ba ngày ba giá, không phải ba lần cùng một con số: quy tắc kiểm 6 đòi giá
  -- dao động giữa các ngày, vì một cột giá phẳng là dấu hiệu nhập ẩu.
  ('11110000-0000-4000-8000-000000000901', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',   375.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000902', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',   395.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000903', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',   445.00, 'DKK'),
  ('11110000-0000-4000-8000-000000000904', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE',   690000, 'VND'),

  ('11110000-0000-4000-8000-000000001001', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 13990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001002', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 14990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001003', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 12990.00, 'DKK'),

  ('11110000-0000-4000-8000-000000001101', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 26990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001101', 'a0000000-0000-4000-8000-000000000001', 'SINGLE', 32490.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001102', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 27990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001103', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE', 25990.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001104', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE', 16900000, 'VND'),

  ('11110000-0000-4000-8000-000000001201', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',   645.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001202', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',   690.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001203', 'a0000000-0000-4000-8000-000000000001', 'DOUBLE',   745.00, 'DKK'),
  ('11110000-0000-4000-8000-000000001204', 'a0000000-0000-4000-8000-000000000004', 'DOUBLE',  1190000, 'VND')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------- giá phòng đơn (occupancy SINGLE)
--
-- **Mọi ngày khởi hành của sản phẩm có lưu trú qua đêm phải có dòng này** — quy
-- tắc kiểm 23 của `12` mục 9, và `api/scripts/kiem-nhat-quan.sql` bắt được nếu
-- thiếu.
--
-- Vì sao nó thành một quy tắc: thiếu dòng SINGLE thì không có gì hỏng và không
-- có lỗi nào ghi ra. Máy tính giá lấy `giá phòng đơn − giá phòng đôi`, không
-- tìm thấy dòng nào thì phụ thu bằng 0, và khách đi MỘT MÌNH đặt được nguyên
-- chuyến ở giá chia đôi phòng. Chênh lệch chỉ lộ ra khi kế toán đối soát với
-- khách sạn, tức là sau khi khách đã đi.
--
-- Trước đợt này chỉ 4/40 ngày có dòng SINGLE. Chú thích cũ biện hộ rằng để
-- thưa thì mới phát hiện được truy vấn nào quên lọc `occupancy` — lý do ấy
-- **sai theo cả hai chiều**:
--
--   * Nó không bảo vệ được gì: `price_from` lấy `MIN()`, mà giá phòng đơn luôn
--     cao hơn giá phòng đôi, nên bỏ bộ lọc `occupancy` ra thì `MIN()` vẫn rơi
--     đúng vào dòng DOUBLE. Quên lọc ở đó là quên lặng lẽ, có hay không có dòng
--     SINGLE cũng vậy.
--   * Nó lại che mất chỗ mà bộ lọc thực sự quan trọng:
--     `BookingPricingRepository.giaTheoLoaiKhach` dựng bảng giá theo loại
--     khách. Bỏ bộ lọc ở đó là hai dòng cùng đè lên một khoá và giá cơ bản nhảy
--     lên mức phòng đơn. Nay mỗi ngày có đủ hai dòng nên lỗi ấy làm sai tổng
--     tiền của gần như mọi bài test — tức là lộ ra ngay.
--
-- `DAY_TOUR` **không** có dòng SINGLE, và đó không phải thiếu sót: tour trong
-- ngày không có đêm nào để ở phòng, nên phụ thu phòng đơn không có nghĩa. Quy
-- tắc 23 loại chúng ra bằng `product_type`, và đó cũng là chỗ giữ lại trường
-- hợp rìa "sản phẩm không có giá phòng đơn" cho đường đọc.
--
-- Phụ thu +20% cho tour và gói lẻ, +55% cho du thuyền: một mình một cabin là
-- mất nguyên chỗ thứ hai chứ không chỉ mất phần chia đôi tiền phòng.
INSERT INTO departure_price (departure_id, pax_type_id, occupancy, amount, currency) VALUES
  -- COMBO DK
  ('11110000-0000-4000-8000-000000000801', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    19190.00, 'DKK'),  -- 16/01/2027 15990.00 -> 19190 (+20%)
  ('11110000-0000-4000-8000-000000000802', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    20390.00, 'DKK'),  -- 13/03/2027 16990.00 -> 20390 (+20%)
  ('11110000-0000-4000-8000-000000000803', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    17990.00, 'DKK'),  -- 09/10/2027 14990.00 -> 17990 (+20%)

  -- COMBO VN
  ('11110000-0000-4000-8000-000000000804', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    13400000, 'VND'),  -- 13/03/2027 11200000.00 -> 13400000 (+20%)

  -- CRUISE DK
  ('11110000-0000-4000-8000-000000000701', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    14690.00, 'DKK'),  -- 20/02/2027 INSIDE 9490.00 -> 14690 (+55%)
  ('11110000-0000-4000-8000-000000000702', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    17790.00, 'DKK'),  -- 20/02/2027 OUTSIDE 11490.00 -> 17790 (+55%)
  ('11110000-0000-4000-8000-000000000703', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    22090.00, 'DKK'),  -- 20/02/2027 BALCONY 14290.00 -> 22090 (+55%)
  ('11110000-0000-4000-8000-00000000070a', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    29390.00, 'DKK'),  -- 20/02/2027 AQUA 18990.00 -> 29390 (+55%)
  ('11110000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    10090.00, 'DKK'),  -- 20/03/2027 INSIDE 6490.00 -> 10090 (+55%)
  ('11110000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    12390.00, 'DKK'),  -- 20/03/2027 OUTSIDE 7990.00 -> 12390 (+55%)
  ('11110000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    15490.00, 'DKK'),  -- 20/03/2027 BALCONY 9990.00 -> 15490 (+55%)
  ('11110000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    20090.00, 'DKK'),  -- 20/03/2027 AQUA 12990.00 -> 20090 (+55%)
  ('11110000-0000-4000-8000-000000000704', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    13890.00, 'DKK'),  -- 13/11/2027 INSIDE 8990.00 -> 13890 (+55%)
  ('11110000-0000-4000-8000-000000000705', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    16990.00, 'DKK'),  -- 13/11/2027 OUTSIDE 10990.00 -> 16990 (+55%)
  ('11110000-0000-4000-8000-00000000070b', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    20890.00, 'DKK'),  -- 13/11/2027 BALCONY 13490.00 -> 20890 (+55%)
  ('11110000-0000-4000-8000-00000000070c', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    27090.00, 'DKK'),  -- 13/11/2027 AQUA 17490.00 -> 27090 (+55%)

  -- CRUISE VN
  ('11110000-0000-4000-8000-000000000706', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    15200000, 'VND'),  -- 20/02/2027 INSIDE 9800000.00 -> 15200000 (+55%)
  ('11110000-0000-4000-8000-00000000070d', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    18400000, 'VND'),  -- 20/02/2027 OUTSIDE 11900000.00 -> 18400000 (+55%)
  ('11110000-0000-4000-8000-00000000070e', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    22900000, 'VND'),  -- 20/02/2027 BALCONY 14800000.00 -> 22900000 (+55%)
  ('11110000-0000-4000-8000-00000000070f', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    30200000, 'VND'),  -- 20/02/2027 AQUA 19500000.00 -> 30200000 (+55%)

  -- GROUP_TOUR DK
  ('11110000-0000-4000-8000-000000001102', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    33590.00, 'DKK'),  -- 20/03/2027 27990.00 -> 33590 (+20%)
  ('11110000-0000-4000-8000-000000000402', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    28190.00, 'DKK'),  -- 03/04/2027 23490.00 -> 28190 (+20%)
  ('11110000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    31190.00, 'DKK'),  -- 11/04/2027 25990.00 -> 31190 (+20%)
  ('11110000-0000-4000-8000-000000000502', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    22790.00, 'DKK'),  -- 15/05/2027 18990.00 -> 22790 (+20%)
  ('11110000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    31190.00, 'DKK'),  -- 06/06/2027 25990.00 -> 31190 (+20%)
  ('11110000-0000-4000-8000-000000000403', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    26990.00, 'DKK'),  -- 11/09/2027 22490.00 -> 26990 (+20%)
  ('11110000-0000-4000-8000-000000001103', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    31190.00, 'DKK'),  -- 02/10/2027 25990.00 -> 31190 (+20%)
  ('11110000-0000-4000-8000-000000000404', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    25190.00, 'DKK'),  -- 16/10/2027 20990.00 -> 25190 (+20%)
  ('11110000-0000-4000-8000-000000000503', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    20390.00, 'DKK'),  -- 06/11/2027 16990.00 -> 20390 (+20%)

  -- GROUP_TOUR VN
  ('11110000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    22700000, 'VND'),  -- 14/03/2027 18900000.00 -> 22700000 (+20%)
  ('11110000-0000-4000-8000-000000001104', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    20300000, 'VND'),  -- 20/03/2027 16900000.00 -> 20300000 (+20%)
  ('11110000-0000-4000-8000-000000000405', 'a0000000-0000-4000-8000-000000000004', 'SINGLE',    17400000, 'VND'),  -- 03/04/2027 14500000.00 -> 17400000 (+20%)

  -- INDIVIDUAL_PACKAGE DK
  ('11110000-0000-4000-8000-000000001001', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    16790.00, 'DKK'),  -- 13/02/2027 13990.00 -> 16790 (+20%)
  ('11110000-0000-4000-8000-000000001002', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    17990.00, 'DKK'),  -- 17/04/2027 14990.00 -> 17990 (+20%)
  ('11110000-0000-4000-8000-000000001003', 'a0000000-0000-4000-8000-000000000001', 'SINGLE',    15590.00, 'DKK')   -- 20/11/2027 12990.00 -> 15590 (+20%)
ON CONFLICT DO NOTHING;

-- ==========================================================================
--                                                 BÀI VIẾT và BUỔI THUYẾT TRÌNH
-- ==========================================================================

INSERT INTO tag (id, code, sort_order) VALUES
  ('f1000000-0000-4000-8000-000000000004', 'NATURE',    4),
  ('f1000000-0000-4000-8000-000000000005', 'TRANSPORT', 5),
  ('f1000000-0000-4000-8000-000000000006', 'FAMILY',    6)
ON CONFLICT DO NOTHING;

INSERT INTO tag_translation (tag_id, locale, slug, name) VALUES
  ('f1000000-0000-4000-8000-000000000004', 'da', 'natur',     'Natur'),
  ('f1000000-0000-4000-8000-000000000004', 'vi', 'thien-nhien','Thiên nhiên'),
  ('f1000000-0000-4000-8000-000000000005', 'da', 'transport', 'Transport'),
  ('f1000000-0000-4000-8000-000000000005', 'vi', 'di-lai',    'Đi lại'),
  ('f1000000-0000-4000-8000-000000000006', 'da', 'med-boern', 'Med børn'),
  ('f1000000-0000-4000-8000-000000000006', 'vi', 'di-cung-tre','Đi cùng trẻ')
ON CONFLICT DO NOTHING;

INSERT INTO post (id, hero_image, published_at) VALUES
  ('f2000000-0000-4000-8000-000000000004', '/img/blog/mua-mua.jpg', '2026-05-11T08:00:00Z'),
  ('f2000000-0000-4000-8000-000000000005', '/img/blog/tau-hoa.jpg', '2026-08-04T08:00:00Z'),
  ('f2000000-0000-4000-8000-000000000006', '/img/blog/cho-noi.jpg', '2026-09-01T08:00:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO post_translation (post_id, locale, slug, title, excerpt, body, status) VALUES
  ('f2000000-0000-4000-8000-000000000004', 'da', 'hvornaar-er-regntiden',
   'Hvornår er regntiden?', 'Svaret afhænger af hvor i landet man står, og det overrasker de fleste.',
   ARRAY['Vietnam er halvandet tusind kilometer langt. Der er ikke én regntid, der er tre.',
         'I nord regner det fra maj til september. I midten kommer regnen sent, oktober og november.',
         'I syd står regnen i en time om eftermiddagen og holder så op igen. Man kan planlægge efter den.'], 'PUBLISHED'),
  ('f2000000-0000-4000-8000-000000000004', 'vi', 'mua-mua-la-khi-nao',
   'Mùa mưa là khi nào?', 'Câu trả lời tuỳ vào bạn đang đứng ở đâu, và điều đó làm phần lớn người ta bất ngờ.',
   ARRAY['Việt Nam dài một nghìn rưỡi cây số. Không có một mùa mưa, có ba.',
         'Miền Bắc mưa từ tháng năm tới tháng chín. Miền Trung mưa muộn, tháng mười và mười một.',
         'Miền Nam mưa một tiếng buổi chiều rồi tạnh. Cái đó thì sắp lịch được.'], 'PUBLISHED'),

  ('f2000000-0000-4000-8000-000000000005', 'da', 'nattoget-til-lao-cai',
   'Nattoget til Lao Cai', 'Otte timer i køje, og man vågner i bjergene.',
   ARRAY['Toget kører fra Hanoi klokken ti om aftenen og er fremme lidt over seks.',
         'Der er fire køjer i en kupé. Vi booker hele kupeen, så man ikke deler med fremmede.',
         'Det rykker og det larmer. Man sover alligevel.'], 'PUBLISHED'),
  ('f2000000-0000-4000-8000-000000000005', 'vi', 'tau-dem-di-lao-cai',
   'Tàu đêm đi Lào Cai', 'Tám tiếng trên giường nằm, tỉnh dậy đã ở trên núi.',
   ARRAY['Tàu rời Hà Nội lúc mười giờ tối, hơn sáu giờ sáng tới nơi.',
         'Một khoang bốn giường. Chúng tôi đặt trọn khoang để không phải ở chung với người lạ.',
         'Tàu xóc và ồn. Rồi vẫn ngủ được.'], 'PUBLISHED'),

  ('f2000000-0000-4000-8000-000000000006', 'da', 'markedet-der-flyder',
   'Markedet der flyder', 'Cai Rang lukker klokken ni om formiddagen. Det er derfor man står op klokken fire.',
   ARRAY['Handlen foregår mellem både. Sælgeren hejser en stang op med sin vare på toppen.',
         'Klokken ni er der ikke mere. Bådene sejler hjem, og floden ser ud som alle andre floder.'], 'PUBLISHED'),
  ('f2000000-0000-4000-8000-000000000006', 'vi', 'cai-cho-troi-tren-song',
   'Cái chợ trôi trên sông', 'Cái Răng tan lúc chín giờ sáng. Đó là lý do người ta dậy từ bốn giờ.',
   ARRAY['Mua bán diễn ra giữa các ghe. Người bán treo hàng lên một cây sào cho người mua nhìn thấy.',
         'Chín giờ là hết. Ghe về nhà, và khúc sông trông như mọi khúc sông khác.'], 'PUBLISHED')
ON CONFLICT DO NOTHING;

INSERT INTO post_tag (post_id, tag_id) VALUES
  ('f2000000-0000-4000-8000-000000000004', 'f1000000-0000-4000-8000-000000000003'),
  ('f2000000-0000-4000-8000-000000000004', 'f1000000-0000-4000-8000-000000000004'),
  ('f2000000-0000-4000-8000-000000000005', 'f1000000-0000-4000-8000-000000000005'),
  ('f2000000-0000-4000-8000-000000000005', 'f1000000-0000-4000-8000-000000000003'),
  ('f2000000-0000-4000-8000-000000000006', 'f1000000-0000-4000-8000-000000000001'),
  ('f2000000-0000-4000-8000-000000000006', 'f1000000-0000-4000-8000-000000000002')
ON CONFLICT DO NOTHING;

-- Thêm hai thẻ nữa cho bài cũ, để bộ lọc thẻ ở R9 có bài trả về nhiều hơn một.
INSERT INTO post_tag (post_id, tag_id) VALUES
  ('f2000000-0000-4000-8000-000000000001', 'f1000000-0000-4000-8000-000000000002'),
  ('f2000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO lecture (id, market, event_date, start_time, city, venue, seats, seats_taken) VALUES
  ('f3000000-0000-4000-8000-000000000004', 'DK', '2027-01-28', '19:00',
   'Odense', 'Odense Bibliotek', 50, 22),
  ('f3000000-0000-4000-8000-000000000005', 'DK', '2027-04-15', '18:30',
   'Aalborg', 'Nordkraft', 40, 9),
  ('f3000000-0000-4000-8000-000000000006', 'VN', '2027-05-08', '18:30',
   'TP. Hồ Chí Minh', 'Đường sách Nguyễn Văn Bình', 60, 31)
ON CONFLICT DO NOTHING;

INSERT INTO lecture_translation (lecture_id, locale, title, description) VALUES
  ('f3000000-0000-4000-8000-000000000004', 'da', 'Nordvietnam og bjergene',
   'Om Sapa, nattoget og hvad man skal have med når det bliver koldt i troperne.'),
  ('f3000000-0000-4000-8000-000000000004', 'vi', 'Miền Bắc và những ngọn núi',
   'Về Sa Pa, tàu đêm, và mang gì theo khi vùng nhiệt đới trở lạnh.'),
  ('f3000000-0000-4000-8000-000000000005', 'da', 'Vietnam med børn',
   'Hvad virker, hvad virker ikke, og hvor lang en køretur et barn kan holde til.'),
  ('f3000000-0000-4000-8000-000000000005', 'vi', 'Việt Nam cùng trẻ nhỏ',
   'Cái gì được, cái gì không, và một đứa trẻ chịu được chặng xe dài bao nhiêu.'),
  ('f3000000-0000-4000-8000-000000000006', 'da', 'Norden om vinteren',
   'For vietnamesere der overvejer nordlys og sne.'),
  ('f3000000-0000-4000-8000-000000000006', 'vi', 'Bắc Âu mùa đông',
   'Dành cho khách Việt đang tính chuyện đi xem cực quang và tuyết.')
ON CONFLICT DO NOTHING;

-- ================================================================ BỘ ẢNH
--
-- Ảnh của trang chi tiết, ba bảng: `media_asset` (tệp và giấy phép),
-- `media_asset_translation` (chữ alt theo locale), `product_image` (tấm nào
-- thuộc sản phẩm nào, thứ tự nào).
--
-- **`path` là đường dẫn TƯƠNG ĐỐI trong bucket, không phải URL.** ADR-011 mục 2
-- và `12` mục 4.6: địa chỉ gốc nằm ở cấu hình (`travel.storage.public-base-url`),
-- không nằm trong dữ liệu. Lưu URL đầy đủ là đóng cứng nhà cung cấp vào từng
-- dòng, và ngày đổi kho sẽ cần một migration thay vì một biến môi trường.
--
-- Tệp phải có mặt trong kho thì ảnh mới hiện. Nạp bằng:
--   python scripts/nap-anh-len-kho.py
--
-- **Đây là ảnh mẫu dùng lại, không phải ảnh của từng sản phẩm.** Cùng một tấm
-- xuất hiện ở nhiều tour, và không tấm nào chụp đúng chuyến đi nó minh hoạ —
-- `24` mục 7.1b đã nói rõ giới hạn đó. Chúng đủ để dựng và xem bố cục thật;
-- ảnh thật thay vào thì xoá cả khối này.
--
-- `source = PURCHASED` chứ không `SELF`: ràng buộc `ck_media_licence` đòi
-- `licence_ref` khác NULL với mọi nguồn trừ `SELF`, và ảnh Unsplash thì có giấy
-- phép tra được — đúng dòng "ảnh mua có giấy phép" của `24` mục 8.
-- `licence_until` để NULL vì giấy phép này không có hạn; quy tắc kiểm 20 chỉ bắt
-- ảnh CÓ hạn mà đã quá hạn.

INSERT INTO media_asset (id, path, width, height, byte_size, source,
                         licence_ref, licence_scope, created_by, last_modified_by) VALUES
  ('33330000-0000-4000-8000-000000000001', 'hero.jpg', 2000, 1573, 697829, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000002', 'tour/p01-bac-nam.jpg', 1400, 835, 324478, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000003', 'tour/p02-du-thuyen-ha-long.jpg', 1400, 933, 182829, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000004', 'tour/p03-hoi-an.jpg', 1400, 1050, 376581, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000005', 'tour/p04-sa-pa.jpg', 1400, 934, 191119, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000006', 'tour/p05-mien-trung.jpg', 1400, 933, 224476, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000007', 'tour/p06-gia-dinh.jpg', 1400, 933, 376205, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000008', 'tour/p07-du-thuyen-mekong.jpg', 1400, 1083, 559925, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000009', 'tour/p08-hoi-an-phu-quoc.jpg', 1400, 955, 335654, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000010', 'tour/p09-am-thuc-ha-noi.jpg', 1400, 933, 284834, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000011', 'tour/p10-phu-quoc.jpg', 1400, 2035, 654250, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000012', 'tour/p11-mien-nam.jpg', 1400, 908, 472246, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000013', 'tour/p12-ninh-binh.jpg', 1400, 933, 298021, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000014', 'khach-san/metropole.jpg', 900, 1418, 359959, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000015', 'khach-san/paradise-ha-long.jpg', 900, 600, 95728, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000016', 'khach-san/azerai-hue.jpg', 900, 600, 113026, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000017', 'khach-san/almanity-hoi-an.jpg', 900, 1350, 419615, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000018', 'khach-san/topas-sa-pa.jpg', 900, 600, 202965, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000019', 'khach-san/victoria-can-tho.jpg', 900, 660, 118979, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000020', 'khach-san/salinda-phu-quoc.jpg', 900, 600, 150672, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000021', 'khach-san/tam-coc.jpg', 900, 1200, 278776, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000022', 'khach-san/fusion-da-nang.jpg', 900, 600, 125014, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000023', 'khach-san/des-arts-sai-gon.jpg', 900, 600, 101247, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000024', 'blog/pho.jpg', 1100, 1648, 188850, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000025', 'blog/hoi-an.jpg', 1100, 733, 186910, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000026', 'blog/visum.jpg', 1100, 2094, 577398, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000027', 'blog/mua-mua.jpg', 1100, 733, 305056, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000028', 'blog/tau-hoa.jpg', 1100, 746, 274479, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000029', 'blog/cho-noi.jpg', 1100, 685, 184987, 'PURCHASED',
   'Unsplash License', 'du lieu mau moi truong dev', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;

-- Chữ `alt` mô tả TẤM ẢNH ĐANG HIỆN, không mô tả sản phẩm — `24` mục 6. Đổi ảnh
-- mà giữ nguyên `alt` cũ là nói sai với đúng nhóm người phụ thuộc vào nó nhất.
--
-- Cả hai locale đều phải có: tầng đọc dùng JOIN chứ không LEFT JOIN, nên ảnh
-- thiếu `alt` ở locale nào thì biến mất khỏi locale đó — luật không fallback cho
-- nội dung bán hàng, `CLAUDE.md` quy tắc 3.
INSERT INTO media_asset_translation (asset_id, locale, alt, created_by, last_modified_by) VALUES
  ('33330000-0000-4000-8000-000000000001', 'da', 'Kalkstensklipper rejser sig af turkisblåt vand', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000001', 'vi', 'Những khối núi đá vôi nhô lên từ mặt nước xanh ngọc', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000002', 'da', 'Luftfoto af øer i en bugt', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000002', 'vi', 'Ảnh từ trên cao của những hòn đảo trong vịnh', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000003', 'da', 'Både samlet i Halong-bugten', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000003', 'vi', 'Những chiếc thuyền tụ lại trong vịnh Hạ Long', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000004', 'da', 'Mennesker går gennem en gade under papirlanterner', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000004', 'vi', 'Người đi bộ trên phố dưới những chiếc đèn lồng giấy', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000005', 'da', 'Rismarker i terrasser på en bjergskråning', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000005', 'vi', 'Ruộng bậc thang trên sườn núi', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000006', 'da', 'Pagode i brune og grønne farver under blå himmel', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000006', 'vi', 'Ngôi chùa nâu và xanh dưới bầu trời trong', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000007', 'da', 'En flod løber gennem en grøn dal', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000007', 'vi', 'Dòng sông chảy qua thung lũng xanh', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000008', 'da', 'Mennesker i båd på floden midt på dagen', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000008', 'vi', 'Người trên thuyền giữa dòng sông lúc ban ngày', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000009', 'da', 'Strand med palmer og en båd i vandet', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000009', 'vi', 'Bãi biển có hàng dừa và một chiếc thuyền', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000010', 'da', 'Skål med hvide nudler, kød og grøntsager', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000010', 'vi', 'Bát bún với thịt và rau', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000011', 'da', 'Kokospalmer langs stranden', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000011', 'vi', 'Hàng dừa dọc bãi biển', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000012', 'da', 'En gruppe mennesker i en smal træbåd', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000012', 'vi', 'Một nhóm người trên chiếc xuồng gỗ', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000013', 'da', 'Pagode omgivet af vand og bjerge', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000013', 'vi', 'Ngôi chùa giữa sông nước và núi đá', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000014', 'da', 'Hvid og brun bygning i kolonistil', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000014', 'vi', 'Toà nhà trắng nâu kiểu thuộc địa', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000015', 'da', 'Stort skib på roligt vand', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000015', 'vi', 'Con tàu lớn trên mặt nước lặng', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000016', 'da', 'Grønne palmer ved en swimmingpool', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000016', 'vi', 'Hàng cọ xanh bên bể bơi', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000017', 'da', 'Swimmingpool omkranset af palmer', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000017', 'vi', 'Bể bơi giữa những hàng cọ', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000018', 'da', 'Landsby i en dal omgivet af bjerge', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000018', 'vi', 'Bản làng trong thung lũng giữa những dãy núi', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000019', 'da', 'Træbro ud i vandet midt på dagen', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000019', 'vi', 'Cầu gỗ vươn ra mặt nước lúc ban ngày', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000020', 'da', 'Række af parasoller på en sandstrand', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000020', 'vi', 'Dãy ô trên bãi cát', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000021', 'da', 'Mennesker ror i både i dagslys', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000021', 'vi', 'Người chèo thuyền dưới ánh ngày', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000022', 'da', 'Stor swimmingpool omgivet af palmer', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000022', 'vi', 'Bể bơi lớn giữa những hàng cọ', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000023', 'da', 'Bybilledet om natten', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000023', 'vi', 'Đường chân trời thành phố về đêm', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000024', 'da', 'Suppe i en hvid keramikskål', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000024', 'vi', 'Bát phở trong tô sứ trắng', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000025', 'da', 'Kinesiske lanterner i mange farver om natten', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000025', 'vi', 'Đèn lồng nhiều màu trong đêm', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000026', 'da', 'Mennesker står på gaden ved en bygning', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000026', 'vi', 'Người đứng trên phố cạnh một toà nhà', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000027', 'da', 'Rismarker i terrasser i en bjergdal', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000027', 'vi', 'Ruộng bậc thang trong thung lũng', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000028', 'da', 'Tog kører forbi en menneskemængde', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000028', 'vi', 'Đoàn tàu chạy qua đám đông', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000029', 'da', 'Kvinde i en båd fyldt med varer', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001'),
  ('33330000-0000-4000-8000-000000000029', 'vi', 'Người phụ nữ trên chiếc thuyền chở đầy hàng', 'c0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;

-- `sort_order` là thứ tự biên tập viên sắp, và tầng đọc ORDER BY theo nó. Tấm
-- đầu của mỗi bộ trùng với ảnh hero của sản phẩm: template ảnh lớn mở đầu bằng
-- chính tấm khách đã thấy ở trang danh sách, nên nhấn vào không bị hẫng.
INSERT INTO product_image (product_id, asset_id, sort_order) VALUES
  ('f0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000002', 1),
  ('f0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000005', 2),
  ('f0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000006', 3),
  ('f0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000004', 4),
  ('f0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000012', 5),
  ('f0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000028', 6),
  ('f0000000-0000-4000-8000-000000000002', '33330000-0000-4000-8000-000000000003', 1),
  ('f0000000-0000-4000-8000-000000000002', '33330000-0000-4000-8000-000000000015', 2),
  ('f0000000-0000-4000-8000-000000000002', '33330000-0000-4000-8000-000000000001', 3),
  ('f0000000-0000-4000-8000-000000000002', '33330000-0000-4000-8000-000000000013', 4),
  ('f0000000-0000-4000-8000-000000000002', '33330000-0000-4000-8000-000000000027', 5),
  ('f0000000-0000-4000-8000-000000000003', '33330000-0000-4000-8000-000000000004', 1),
  ('f0000000-0000-4000-8000-000000000003', '33330000-0000-4000-8000-000000000017', 2),
  ('f0000000-0000-4000-8000-000000000003', '33330000-0000-4000-8000-000000000025', 3),
  ('f0000000-0000-4000-8000-000000000003', '33330000-0000-4000-8000-000000000022', 4),
  ('f0000000-0000-4000-8000-000000000003', '33330000-0000-4000-8000-000000000006', 5),
  ('f0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000005', 1),
  ('f0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000018', 2),
  ('f0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000014', 3),
  ('f0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000027', 4),
  ('f0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000013', 5),
  ('f0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000024', 6),
  ('f0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000006', 1),
  ('f0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000016', 2),
  ('f0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000004', 3),
  ('f0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000022', 4),
  ('f0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000025', 5),
  ('f0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000017', 6),
  ('f0000000-0000-4000-8000-000000000006', '33330000-0000-4000-8000-000000000007', 1),
  ('f0000000-0000-4000-8000-000000000006', '33330000-0000-4000-8000-000000000013', 2),
  ('f0000000-0000-4000-8000-000000000006', '33330000-0000-4000-8000-000000000021', 3),
  ('f0000000-0000-4000-8000-000000000006', '33330000-0000-4000-8000-000000000011', 4),
  ('f0000000-0000-4000-8000-000000000006', '33330000-0000-4000-8000-000000000029', 5),
  ('f0000000-0000-4000-8000-000000000007', '33330000-0000-4000-8000-000000000008', 1),
  ('f0000000-0000-4000-8000-000000000007', '33330000-0000-4000-8000-000000000019', 2),
  ('f0000000-0000-4000-8000-000000000007', '33330000-0000-4000-8000-000000000029', 3),
  ('f0000000-0000-4000-8000-000000000007', '33330000-0000-4000-8000-000000000012', 4),
  ('f0000000-0000-4000-8000-000000000007', '33330000-0000-4000-8000-000000000023', 5),
  ('f0000000-0000-4000-8000-000000000008', '33330000-0000-4000-8000-000000000009', 1),
  ('f0000000-0000-4000-8000-000000000008', '33330000-0000-4000-8000-000000000017', 2),
  ('f0000000-0000-4000-8000-000000000008', '33330000-0000-4000-8000-000000000020', 3),
  ('f0000000-0000-4000-8000-000000000008', '33330000-0000-4000-8000-000000000011', 4),
  ('f0000000-0000-4000-8000-000000000008', '33330000-0000-4000-8000-000000000025', 5),
  ('f0000000-0000-4000-8000-000000000009', '33330000-0000-4000-8000-000000000010', 1),
  ('f0000000-0000-4000-8000-000000000009', '33330000-0000-4000-8000-000000000024', 2),
  ('f0000000-0000-4000-8000-000000000009', '33330000-0000-4000-8000-000000000014', 3),
  ('f0000000-0000-4000-8000-000000000009', '33330000-0000-4000-8000-000000000029', 4),
  ('f0000000-0000-4000-8000-000000000010', '33330000-0000-4000-8000-000000000011', 1),
  ('f0000000-0000-4000-8000-000000000010', '33330000-0000-4000-8000-000000000020', 2),
  ('f0000000-0000-4000-8000-000000000010', '33330000-0000-4000-8000-000000000009', 3),
  ('f0000000-0000-4000-8000-000000000010', '33330000-0000-4000-8000-000000000027', 4),
  ('f0000000-0000-4000-8000-000000000011', '33330000-0000-4000-8000-000000000012', 1),
  ('f0000000-0000-4000-8000-000000000011', '33330000-0000-4000-8000-000000000023', 2),
  ('f0000000-0000-4000-8000-000000000011', '33330000-0000-4000-8000-000000000008', 3),
  ('f0000000-0000-4000-8000-000000000011', '33330000-0000-4000-8000-000000000019', 4),
  ('f0000000-0000-4000-8000-000000000011', '33330000-0000-4000-8000-000000000029', 5),
  ('f0000000-0000-4000-8000-000000000011', '33330000-0000-4000-8000-000000000007', 6),
  ('f0000000-0000-4000-8000-000000000012', '33330000-0000-4000-8000-000000000013', 1),
  ('f0000000-0000-4000-8000-000000000012', '33330000-0000-4000-8000-000000000021', 2),
  ('f0000000-0000-4000-8000-000000000012', '33330000-0000-4000-8000-000000000003', 3),
  ('f0000000-0000-4000-8000-000000000012', '33330000-0000-4000-8000-000000000027', 4)
ON CONFLICT DO NOTHING;


-- ======================================================= TEMPLATE TRANG CHI TIẾT
--
-- `product.layout` — biên tập viên chọn khung cho từng sản phẩm (`V7`, `12` mục
-- 4.1). Danh mục nằm ở `web/apps/site/src/lib/templates.ts`, không ở CSDL.
--
-- Đặt sẵn hai giá trị để mở trang là thấy được cả ba khung mà không phải vào
-- trang quản trị. Hai sản phẩm còn lại để NULL có chủ ý: NULL là trạng thái của
-- mọi sản phẩm chưa ai chạm tới, và nó phải dựng ra được một trang tử tế.
--
-- UPDATE chứ không INSERT nên tự nó chạy lại được.
UPDATE product SET layout = 'tap-chi'
 WHERE id = 'f0000000-0000-4000-8000-000000000004';   -- Nordvietnam og Sapa
-- Sản phẩm 11 chứ không phải 05: 05 chỉ bán ở thị trường DK, nên ở locale `vi`
-- nó trả 404 — đúng luật, nhưng làm ví dụ này không mở được bằng tiếng Việt.
UPDATE product SET layout = 'ke-chuyen'
 WHERE id = 'f0000000-0000-4000-8000-000000000011';   -- Sydvietnam og Mekong

-- ======================================================= ẢNH CỦA ĐIỂM ĐẾN
--
-- `V8`. Trang điểm đến là trang cho khách **chưa biết mình muốn gì** (`01` mục
-- 3.1) — người đang chọn vùng đất chứ chưa chọn chuyến đi. Một danh sách tên
-- địa danh không nói được với người đó rằng Sa Pa khác Phú Quốc ở chỗ nào.
--
-- Đi qua `media_asset` chứ không thêm một cột chữ: `12` mục 10 đã ghi cột ảnh
-- dạng chữ vào danh sách nợ vì chúng không có chứng từ giấy phép.
--
-- Dùng lại chính 29 tấm mẫu đã nạp ở khối trên, nên không tốn thêm tệp nào và
-- giấy phép đã có sẵn. Vài tấm vừa minh hoạ một tour vừa minh hoạ điểm đến —
-- đúng lý do `media_asset` là thực thể riêng chứ không phải cột trên bảng cha
-- (`12` mục 4.6): một ảnh, một giấy phép, nhiều chỗ dùng.
--
-- Mỗi điểm đến một tấm. Bảng chịu được nhiều hơn — `sort_order` có sẵn — và
-- tầng đọc lấy tấm đầu.
INSERT INTO destination_image (destination_id, asset_id, sort_order) VALUES
  ('e0000000-0000-4000-8000-000000000001', '33330000-0000-4000-8000-000000000014', 1),  -- HANOI      -> toà nhà kiểu thuộc địa
  ('e0000000-0000-4000-8000-000000000002', '33330000-0000-4000-8000-000000000003', 1),  -- HALONG     -> thuyền trong vịnh Hạ Long
  ('e0000000-0000-4000-8000-000000000005', '33330000-0000-4000-8000-000000000005', 1),  -- SAPA       -> ruộng bậc thang
  ('e0000000-0000-4000-8000-000000000006', '33330000-0000-4000-8000-000000000013', 1),  -- NINH_BINH  -> chùa giữa sông nước và núi đá
  ('e0000000-0000-4000-8000-000000000003', '33330000-0000-4000-8000-000000000004', 1),  -- HOI_AN     -> phố đèn lồng
  ('e0000000-0000-4000-8000-000000000007', '33330000-0000-4000-8000-000000000006', 1),  -- HUE        -> chùa dưới trời trong
  ('e0000000-0000-4000-8000-000000000008', '33330000-0000-4000-8000-000000000022', 1),  -- DA_NANG    -> bể bơi giữa hàng cọ
  ('e0000000-0000-4000-8000-000000000004', '33330000-0000-4000-8000-000000000029', 1),  -- MEKONG     -> thuyền chở hàng trên chợ nổi
  ('e0000000-0000-4000-8000-000000000009', '33330000-0000-4000-8000-000000000023', 1),  -- HCMC       -> đường chân trời thành phố về đêm
  ('e0000000-0000-4000-8000-000000000010', '33330000-0000-4000-8000-000000000011', 1)   -- PHU_QUOC   -> hàng dừa dọc bãi biển
ON CONFLICT DO NOTHING;

COMMIT;
