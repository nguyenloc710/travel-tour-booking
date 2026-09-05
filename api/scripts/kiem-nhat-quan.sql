-- ==========================================================================
--  Bộ kiểm tính nhất quán dữ liệu — `docs/12` mục 9
-- ==========================================================================
--
--  Chạy tay:
--    psql postgresql://travel:travel@localhost:5432/travel -f api/scripts/kiem-nhat-quan.sql
--
--  Chạy trong CI: `KiemNhatQuanIT` nạp migration, nạp `seed-dev.sql`, chạy
--  đúng tệp này và bắt lỗi ở mức `LOI`. Đó là cổng thật; chạy tay chỉ để nhìn.
--
--  KHÔNG có kết quả nghĩa là sạch.
--
--  Vì sao là MỘT câu lệnh SQL và không có lệnh meta nào của psql: tệp này chạy
--  ở hai chỗ — `psql` khi làm tay, và JDBC trong bài test. Lệnh meta của psql
--  (`\set`, `\gset`, `\i`) thì JDBC không hiểu, còn một tệp nhiều câu lệnh thì
--  phải tách tay và mỗi lần tách là một chỗ sai. Một câu lệnh chạy được ở cả
--  hai chỗ, không sửa gì.
--
--  Hai mức:
--    LOI       — dữ liệu sai, phải sửa. CI đỏ.
--    CANH_BAO  — đúng nhưng sắp thành vấn đề, hoặc còn chờ quyết định. CI xanh.
--
--  Thêm quy tắc thì thêm cả ở đây LẪN bảng của `docs/12` mục 9, cùng số. Bảng
--  ấy là nguồn sự thật về "có quy tắc nào"; tệp này là nguồn sự thật về "quy
--  tắc ấy thực sự đo cái gì".
--
--  Ba quy tắc quét MỌI bảng thay vì liệt kê tên: 16, 17 và 18 là loại lỗi chỉ
--  xảy ra khi ai đó thêm bảng mới và quên một bước, nên một danh sách tên bảng
--  viết tay sẽ bỏ sót đúng cái bảng gây lỗi. Quy tắc 16 và 18 phải ĐẾM dòng
--  trong từng bảng, nên chúng chạy truy vấn động qua `query_to_xml` — cách duy
--  nhất làm được việc đó mà không cần khối `DO` (xem lý do một-câu-lệnh ở
--  trên). Quy tắc 17 chỉ cần tên bảng nên không cần mẹo ấy.

WITH

-- Bảng thật (không tính view) và các cột của chúng — nền cho quy tắc 16–18.
bang AS (
  SELECT t.table_name::text AS ten,
         array_agg(c.column_name::text) AS cot
  FROM information_schema.tables t
  JOIN information_schema.columns c
    ON c.table_schema = t.table_schema AND c.table_name = t.table_name
  WHERE t.table_schema = 'public'
    AND t.table_type = 'BASE TABLE'
    AND t.table_name <> 'flyway_schema_history'
  GROUP BY t.table_name
),

-- Đếm dòng vi phạm bằng truy vấn động. `dieu_kien` là mệnh đề WHERE.
dem AS (
  SELECT b.ten, d.dieu_kien, d.quy_tac,
         (xpath('/row/c/text()',
                query_to_xml(format('SELECT count(*) AS c FROM public.%I WHERE %s',
                                    b.ten, d.dieu_kien),
                             false, true, '')))[1]::text::bigint AS so
  FROM bang b
  CROSS JOIN LATERAL (VALUES
    (16, 'last_modified_at < created_at',
        ARRAY['last_modified_at', 'created_at']),
    (18, 'soft_delete AND last_modified_by IS NULL',
        ARRAY['soft_delete', 'last_modified_by'])
  ) AS d(quy_tac, dieu_kien, can_cot)
  WHERE b.cot @> d.can_cot
),

vi_pham AS (

-- 1 ----------------------------------------------------------------------
-- Đúng một khối riêng, và khối đó khớp `product_type`. Khoá ngoại đã cưỡng
-- chế "khớp loại" (ux_product_id_type), nên cái còn lọt được là THIẾU khối:
-- sản phẩm không có khối thì mọi truy vấn JOIN vào nó trả rỗng, và sản phẩm
-- biến mất khỏi trang chi tiết mà không có lỗi nào.
SELECT 1 AS quy_tac, 'LOI' AS muc_do,
       'product ' || p.id AS doi_tuong,
       'có ' || k.so || ' khối riêng, cần đúng 1 khớp ' || p.product_type AS chi_tiet
FROM product p
CROSS JOIN LATERAL (
  SELECT (SELECT count(*) FROM product_group_tour x WHERE x.product_id = p.id)
       + (SELECT count(*) FROM product_individual  x WHERE x.product_id = p.id)
       + (SELECT count(*) FROM product_private     x WHERE x.product_id = p.id)
       + (SELECT count(*) FROM product_cruise      x WHERE x.product_id = p.id)
       + (SELECT count(*) FROM product_combo       x WHERE x.product_id = p.id)
       + (SELECT count(*) FROM product_day_tour    x WHERE x.product_id = p.id) AS so
) k
WHERE NOT p.soft_delete AND k.so <> 1

-- 2 ----------------------------------------------------------------------
-- Bật bán mà chưa có nội dung ngôn ngữ nguồn đã xuất bản. Không hỏng gì:
-- chính sách không-fallback làm đúng việc của nó và sản phẩm lặng lẽ vắng mặt
-- khỏi listing của cả hai locale.
UNION ALL
SELECT 2, 'LOI',
       'product ' || pm.product_id || ' @' || pm.market,
       'đã bật bán nhưng không có bản dịch ' || l.code || ' PUBLISHED'
FROM product_market pm
JOIN product p ON p.id = pm.product_id AND NOT p.soft_delete
CROSS JOIN (SELECT code FROM locale WHERE is_source AND is_active) l
WHERE pm.is_published
  AND NOT EXISTS (
    SELECT 1 FROM product_translation t
    WHERE t.product_id = pm.product_id AND t.locale = l.code
      AND t.status = 'PUBLISHED' AND NOT t.soft_delete)

-- 3 ----------------------------------------------------------------------
-- Lịch trình thủng ngày. Bốn loại tour dài: `COMBO` bán theo đêm khách sạn
-- chứ không theo ngày lịch trình, `DAY_TOUR` không có `duration_days`.
UNION ALL
SELECT 3, 'LOI',
       'product ' || p.id,
       'lịch trình ' || n.so || ' ngày, duration_days ' || p.duration_days
FROM product p
CROSS JOIN LATERAL (
  SELECT count(*) AS so FROM itinerary_day i
  WHERE i.product_id = p.id AND NOT i.soft_delete
) n
WHERE NOT p.soft_delete
  AND p.product_type IN ('GROUP_TOUR', 'INDIVIDUAL_PACKAGE', 'PRIVATE_TOUR', 'CRUISE')
  AND n.so <> p.duration_days

-- 4 ----------------------------------------------------------------------
-- Số đêm bán ≠ số đêm thật trong lịch trình, THEO TỪNG khách sạn. Ghi 3 đêm
-- Hội An mà lịch trình chỉ có 2 là bán một đêm không tồn tại.
UNION ALL
SELECT 4, 'LOI',
       'product ' || s.product_id || ' / hotel ' || s.hotel_id,
       'hotel_stay ghi ' || s.nights || ' đêm, lịch trình có ' || d.so
FROM product_hotel_stay s
CROSS JOIN LATERAL (
  SELECT count(*) AS so FROM itinerary_day i
  WHERE i.product_id = s.product_id AND i.hotel_id = s.hotel_id AND NOT i.soft_delete
) d
WHERE s.nights <> d.so

-- 5 ----------------------------------------------------------------------
-- `price_from` là cột vật chất hoá do trigger của V5 sở hữu. Lệch nghĩa là
-- trigger không chạy, hoặc có ai đó đặt tay — và giá hiện trên listing khác
-- giá tính ra khi khách bấm vào.
UNION ALL
SELECT 5, 'LOI',
       'product ' || pm.product_id || ' @' || pm.market,
       'price_from ' || COALESCE(pm.price_from::text, 'NULL')
         || ', tính lại ra ' || COALESCE(t.gia::text, 'NULL')
FROM product_market pm
JOIN product p ON p.id = pm.product_id AND NOT p.soft_delete
CROSS JOIN LATERAL (
  SELECT min(dp.amount) AS gia
  FROM departure d
  JOIN departure_price dp ON dp.departure_id = d.id AND dp.occupancy = 'DOUBLE'
  JOIN pax_type x ON x.id = dp.pax_type_id AND x.code = 'ADULT'
  WHERE d.product_id = pm.product_id AND d.market = pm.market AND NOT d.soft_delete
) t
WHERE pm.price_from IS DISTINCT FROM t.gia

-- 6 ----------------------------------------------------------------------
-- Giá phải dao động giữa các ngày khởi hành. Một cột giá phẳng không sai về
-- kỹ thuật, nó chỉ là dấu hiệu của dữ liệu chép dán — `04` mục 4.1.
UNION ALL
SELECT 6, 'LOI',
       'product ' || d.product_id || ' @' || d.market,
       count(*) || ' ngày khởi hành cùng đúng một giá ' || min(dp.amount)
FROM departure d
JOIN departure_price dp ON dp.departure_id = d.id AND dp.occupancy = 'DOUBLE'
JOIN pax_type x ON x.id = dp.pax_type_id AND x.code = 'ADULT'
JOIN product p ON p.id = d.product_id AND p.product_type <> 'CRUISE'
WHERE NOT d.soft_delete
GROUP BY d.product_id, d.market
HAVING count(*) > 1 AND count(DISTINCT dp.amount) = 1

-- 7a ---------------------------------------------------------------------
-- `CRUISE`: mỗi ngày khởi hành phải đủ bốn hạng cabin. Đếm THEO NGÀY, vì mỗi
-- hạng là một dòng `departure` riêng — thiếu một hạng là thiếu lặng lẽ.
UNION ALL
SELECT 7, 'LOI',
       'product ' || d.product_id || ' @' || d.market || ' ' || d.depart_date,
       'có ' || count(DISTINCT d.cabin_category) || ' hạng cabin, cần 4'
FROM departure d
JOIN product p ON p.id = d.product_id AND p.product_type = 'CRUISE'
WHERE NOT d.soft_delete
GROUP BY d.product_id, d.market, d.depart_date
HAVING count(DISTINCT d.cabin_category) <> 4

-- 7b ---------------------------------------------------------------------
-- ...và chênh giá giữa hai hạng liền nhau nằm trong 20–45%. Dưới 20% thì khách
-- không có lý do gì để chọn hạng thấp; trên 45% thì hạng cao không bán được.
UNION ALL
SELECT 7, 'LOI',
       'product ' || t.product_id || ' @' || t.market || ' ' || t.depart_date,
       t.cabin_truoc || ' → ' || t.cabin_category || ' chênh '
         || round((t.amount - t.truoc) / t.truoc * 100, 1) || '%, ngoài 20–45%'
FROM (
  SELECT d.product_id, d.market, d.depart_date, d.cabin_category, dp.amount,
         lag(dp.amount)         OVER w AS truoc,
         lag(d.cabin_category)  OVER w AS cabin_truoc
  FROM departure d
  JOIN product p ON p.id = d.product_id AND p.product_type = 'CRUISE'
  JOIN departure_price dp ON dp.departure_id = d.id AND dp.occupancy = 'DOUBLE'
  JOIN pax_type x ON x.id = dp.pax_type_id AND x.code = 'ADULT'
  WHERE NOT d.soft_delete
  WINDOW w AS (PARTITION BY d.product_id, d.market, d.depart_date ORDER BY dp.amount)
) t
WHERE t.truoc IS NOT NULL
  AND ((t.amount - t.truoc) / t.truoc * 100 < 20
    OR (t.amount - t.truoc) / t.truoc * 100 > 45)

-- 8 ----------------------------------------------------------------------
-- `PRIVATE_TOUR`: bậc giá liền mạch, không chồng, giá mỗi người giảm dần. Một
-- khoảng hở là một số khách mà hệ thống không tính ra giá — và không ai biết
-- cho tới khi đúng nhóm khách đó hỏi.
UNION ALL
SELECT 8, 'LOI',
       'product ' || t.product_id || ' @' || t.market || ' bậc từ ' || t.min_pax,
       CASE
         WHEN t.min_sau IS NULL AND t.max_pax IS NOT NULL
           THEN 'bậc cuối vẫn có trần ' || t.max_pax || ' — số khách lớn hơn không có giá'
         WHEN t.max_pax IS NULL
           THEN 'bậc không trần nhưng còn bậc sau bắt đầu ở ' || t.min_sau
         WHEN t.min_sau <> t.max_pax + 1
           THEN 'trần ' || t.max_pax || ' nhưng bậc sau bắt đầu ở ' || t.min_sau
         ELSE 'giá ' || t.price_per_person || ' không giảm ở bậc sau (' || t.gia_sau || ')'
       END
FROM (
  SELECT pt.product_id, pt.market, pt.min_pax, pt.max_pax, pt.price_per_person,
         lead(pt.min_pax)          OVER w AS min_sau,
         lead(pt.price_per_person) OVER w AS gia_sau
  FROM price_tier pt
  WHERE NOT pt.soft_delete
  WINDOW w AS (PARTITION BY pt.product_id, pt.market ORDER BY pt.min_pax)
) t
WHERE (t.min_sau IS NULL AND t.max_pax IS NOT NULL)
   OR (t.min_sau IS NOT NULL AND t.max_pax IS NULL)
   OR (t.min_sau IS NOT NULL AND t.min_sau <> t.max_pax + 1)
   OR (t.gia_sau IS NOT NULL AND t.gia_sau >= t.price_per_person)

-- 9 ----------------------------------------------------------------------
-- `PRIVATE_TOUR` không có lịch khởi hành: nó chạy theo ngày khách chọn, và
-- giá đi qua báo giá chứ không qua bảng giá ngày.
UNION ALL
SELECT 9, 'LOI',
       'product ' || d.product_id,
       'PRIVATE_TOUR nhưng có ' || count(*) || ' dòng departure'
FROM departure d
JOIN product p ON p.id = d.product_id AND p.product_type = 'PRIVATE_TOUR'
WHERE NOT d.soft_delete
GROUP BY d.product_id

-- 10 ---------------------------------------------------------------------
-- Tham chiếu khách sạn chết. Khoá ngoại chỉ bảo đảm dòng TỒN TẠI, không bảo
-- đảm nó còn sống: xoá mềm khách sạn thì lịch trình vẫn trỏ tới nó.
UNION ALL
SELECT 10, 'LOI',
       'itinerary_day ' || i.id,
       'trỏ tới khách sạn không còn sống: ' || i.hotel_id
FROM itinerary_day i
WHERE NOT i.soft_delete AND i.hotel_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hotel h WHERE h.id = i.hotel_id AND NOT h.soft_delete)

UNION ALL
SELECT 10, 'LOI',
       'product_hotel_stay ' || s.product_id,
       'trỏ tới khách sạn không còn sống: ' || s.hotel_id
FROM product_hotel_stay s
WHERE NOT EXISTS (SELECT 1 FROM hotel h WHERE h.id = s.hotel_id AND NOT h.soft_delete)

-- 11 ---------------------------------------------------------------------
-- Nội dung lấp chỗ trống. Quy tắc này không bắt lỗi kỹ thuật — nó bắt những
-- ngày lịch trình chỉ ghi "Biển." Đọc code không thấy, mở trang cũng dễ bỏ qua.
UNION ALL
SELECT 11, 'LOI',
       'itinerary_day_translation ' || i.day_number || '/' || t.locale
         || ' của product ' || i.product_id,
       'mô tả ' || length(t.description) || ' ký tự, cần ≥ 80'
FROM itinerary_day_translation t
JOIN itinerary_day i ON i.id = t.itinerary_day_id AND NOT i.soft_delete
WHERE NOT t.soft_delete AND length(t.description) < 80

-- 12 ---------------------------------------------------------------------
-- Trang điểm đến rỗng. Điểm đến không có khách sạn lẫn tham quan vẫn hiện
-- trong danh sách, vẫn có URL, và mở ra là một trang trắng.
UNION ALL
SELECT 12, 'LOI',
       'destination ' || d.code,
       'có ' || k.so_ks || ' khách sạn và ' || k.so_tq || ' điểm tham quan'
FROM destination d
CROSS JOIN LATERAL (
  SELECT (SELECT count(*) FROM hotel h
           WHERE h.destination_id = d.id AND NOT h.soft_delete) AS so_ks,
         (SELECT count(*) FROM excursion e
           WHERE e.destination_id = d.id AND NOT e.soft_delete) AS so_tq
) k
WHERE NOT d.soft_delete AND (k.so_ks = 0 OR k.so_tq = 0)

-- 13 ---------------------------------------------------------------------
-- CẢNH BÁO có chủ ý, không phải LOI: đang đỏ và còn đỏ cho tới khi Q-2 có câu
-- trả lời. Sáu con số nghiệp vụ của thị trường VN chưa chốt, nên chưa ai biết
-- giá trẻ em và em bé phải là bao nhiêu. Nâng lên LOI khi Q-2 xong.
UNION ALL
SELECT 13, 'CANH_BAO',
       'departure ' || d.id || ' @' || d.market,
       'thiếu giá loại khách: ' || string_agg(x.code, ', ' ORDER BY x.sort_order)
FROM departure d
JOIN pax_type x ON x.market = d.market AND NOT x.soft_delete
WHERE NOT d.soft_delete
  AND NOT EXISTS (
    SELECT 1 FROM departure_price dp
    WHERE dp.departure_id = d.id AND dp.pax_type_id = x.id AND dp.occupancy = 'DOUBLE')
GROUP BY d.id, d.market

-- 14 ---------------------------------------------------------------------
-- CẢNH BÁO: trang sự kiện SẼ rỗng, chưa rỗng. Báo trước là toàn bộ giá trị
-- của quy tắc này — biết sau khi trang đã trắng thì đã muộn.
UNION ALL
SELECT 14, 'CANH_BAO',
       'thị trường ' || m.code,
       'không còn buổi thuyết trình nào trong tương lai'
FROM market m
WHERE m.is_active
  AND NOT EXISTS (
    SELECT 1 FROM lecture le
    WHERE le.market = m.code AND NOT le.soft_delete AND le.event_date >= current_date)

-- 15 ---------------------------------------------------------------------
-- Giữ chỗ quá hạn mà chưa trả lại. Job quét hạn chết là kiểu hỏng đắt nhất
-- của luồng đặt tour: chỗ bị giam, `seatsAvailable` tụt dần, và ngày khởi hành
-- báo hết chỗ trong khi không ai đặt.
UNION ALL
SELECT 15, 'LOI',
       'seat_hold ' || sh.id,
       'hết hạn ' || sh.expires_at || ' mà released_at còn NULL'
FROM seat_hold sh
WHERE NOT sh.soft_delete AND sh.released_at IS NULL
  AND sh.booking_id IS NULL AND sh.expires_at < now()

-- 16 ---------------------------------------------------------------------
-- `last_modified_at` chạy lùi so với `created_at`. Nghĩa là ứng dụng ghi tay
-- giá trị đó thay vì để trigger đặt.
UNION ALL
SELECT 16, 'LOI',
       'bảng ' || dem.ten,
       dem.so || ' dòng có last_modified_at < created_at'
FROM dem WHERE dem.quy_tac = 16 AND dem.so > 0

-- 17 ---------------------------------------------------------------------
-- Bảng có `last_modified_at` nhưng không có trigger đặt nó. Thêm bảng mới mà
-- quên gắn trigger thì cột "sửa lần cuối" đứng yên vĩnh viễn, và không có gì
-- báo — nó vẫn có giá trị, chỉ là giá trị của lúc tạo.
UNION ALL
SELECT 17, 'LOI',
       'bảng ' || b.ten,
       'có last_modified_at nhưng thiếu trigger tg_' || b.ten || '_last_modified'
FROM bang b
WHERE b.cot @> ARRAY['last_modified_at']
  AND NOT EXISTS (
    SELECT 1 FROM pg_trigger tg
    WHERE NOT tg.tgisinternal AND tg.tgname = 'tg_' || b.ten || '_last_modified')

-- 18 ---------------------------------------------------------------------
-- Xoá mềm mà không biết ai xoá. Cột xoá mềm sinh ra để trả lời "ai, lúc nào",
-- và một nửa câu trả lời thì không dùng được vào việc gì.
UNION ALL
SELECT 18, 'LOI',
       'bảng ' || dem.ten,
       dem.so || ' dòng soft_delete = TRUE mà last_modified_by là NULL'
FROM dem WHERE dem.quy_tac = 18 AND dem.so > 0

-- 19 ---------------------------------------------------------------------
-- Xoá mềm không lan xuống dưới — `11` mục 11.3.b. Đây là quy tắc đắt nhất
-- bảng vì phải quét chéo, và cũng là quy tắc bắt được kiểu hỏng khó truy nhất:
-- không có gì gãy, sản phẩm chỉ lặng lẽ biến mất khỏi listing vì truy vấn của
-- nó `INNER JOIN` sang một dòng đã chết.
UNION ALL
SELECT 19, 'LOI', 'product ' || p.id,
       'primary_destination_id trỏ tới điểm đến đã xoá mềm'
FROM product p
WHERE NOT p.soft_delete
  AND NOT EXISTS (SELECT 1 FROM destination d
                  WHERE d.id = p.primary_destination_id AND NOT d.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'itinerary_day ' || i.id,
       'destination_id trỏ tới điểm đến đã xoá mềm'
FROM itinerary_day i
WHERE NOT i.soft_delete AND i.destination_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM destination d
                  WHERE d.id = i.destination_id AND NOT d.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'hotel ' || h.id,
       'destination_id trỏ tới điểm đến đã xoá mềm'
FROM hotel h
WHERE NOT h.soft_delete
  AND NOT EXISTS (SELECT 1 FROM destination d
                  WHERE d.id = h.destination_id AND NOT d.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'excursion ' || e.id,
       'destination_id trỏ tới điểm đến đã xoá mềm'
FROM excursion e
WHERE NOT e.soft_delete
  AND NOT EXISTS (SELECT 1 FROM destination d
                  WHERE d.id = e.destination_id AND NOT d.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'destination ' || d.code,
       'region_id trỏ tới miền đã xoá mềm'
FROM destination d
WHERE NOT d.soft_delete
  AND NOT EXISTS (SELECT 1 FROM region r WHERE r.id = d.region_id AND NOT r.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'product_theme ' || pt.product_id,
       'theme_id trỏ tới chủ đề đã xoá mềm'
FROM product_theme pt
JOIN product p ON p.id = pt.product_id AND NOT p.soft_delete
WHERE NOT EXISTS (SELECT 1 FROM theme th WHERE th.id = pt.theme_id AND NOT th.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'post_tag ' || pt.post_id,
       'tag_id trỏ tới thẻ đã xoá mềm'
FROM post_tag pt
JOIN post po ON po.id = pt.post_id AND NOT po.soft_delete
WHERE NOT EXISTS (SELECT 1 FROM tag t WHERE t.id = pt.tag_id AND NOT t.soft_delete)

UNION ALL
SELECT 19, 'LOI', 'departure ' || d.id,
       'departure_origin_id trỏ tới điểm khởi hành đã xoá mềm'
FROM departure d
WHERE NOT d.soft_delete AND d.departure_origin_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM departure_origin o
                  WHERE o.id = d.departure_origin_id AND NOT o.soft_delete)

-- 20 ---------------------------------------------------------------------
-- Giấy phép ảnh hết hạn mà ảnh vẫn nằm trên web — `24` mục 8. Hết hạn không
-- làm ảnh biến mất, nên không có gì báo cho tới khi bên cấp phép gửi hoá đơn.
UNION ALL
SELECT 20, 'LOI',
       'media_asset ' || a.path,
       'giấy phép hết hạn ' || a.licence_until || ' mà ảnh còn dùng ở '
         || (SELECT count(*) FROM product_image pi WHERE pi.asset_id = a.id) || ' sản phẩm'
FROM media_asset a
WHERE NOT a.soft_delete
  AND a.licence_until IS NOT NULL AND a.licence_until < current_date
  AND EXISTS (SELECT 1 FROM product_image pi WHERE pi.asset_id = a.id)

-- 21 ---------------------------------------------------------------------
-- Vòng lặp chuyển hướng 301: một slug vừa là slug đang dùng, vừa là slug cũ
-- được chuyển hướng đi chỗ khác. Trình duyệt đi vòng cho tới khi bỏ cuộc.
UNION ALL
SELECT 21, 'LOI',
       'slug_history ' || sh.entity_type || '/' || sh.locale || '/' || sh.old_slug,
       'trùng slug đang dùng của cùng loại và locale'
FROM slug_history sh
WHERE EXISTS (
        SELECT 1 FROM product_translation t
        WHERE sh.entity_type = 'PRODUCT' AND t.locale = sh.locale
          AND t.slug = sh.old_slug AND NOT t.soft_delete)
   OR EXISTS (
        SELECT 1 FROM destination_translation t
        WHERE sh.entity_type = 'DESTINATION' AND t.locale = sh.locale
          AND t.slug = sh.old_slug AND NOT t.soft_delete)
   OR EXISTS (
        SELECT 1 FROM region_translation t
        WHERE sh.entity_type = 'REGION' AND t.locale = sh.locale
          AND t.slug = sh.old_slug AND NOT t.soft_delete)
   OR EXISTS (
        SELECT 1 FROM post_translation t
        WHERE sh.entity_type = 'POST' AND t.locale = sh.locale
          AND t.slug = sh.old_slug AND NOT t.soft_delete)

-- 22 ---------------------------------------------------------------------
-- Đơn đã xác nhận mà thiếu tên người đi. Số khách lấy từ các dòng giá cơ bản,
-- vì đó là thứ khách đã trả tiền cho.
UNION ALL
SELECT 22, 'LOI',
       'booking ' || b.reference,
       'CONFIRMED với ' || round(k.so_khach) || ' khách nhưng có ' || k.so_ten || ' dòng hành khách'
FROM booking b
CROSS JOIN LATERAL (
  SELECT COALESCE((SELECT sum(bl.quantity) FROM booking_line bl
                   WHERE bl.booking_id = b.id AND bl.line_key = 'BASE'), 0) AS so_khach,
         (SELECT count(*) FROM booking_passenger bp WHERE bp.booking_id = b.id) AS so_ten
) k
WHERE NOT b.soft_delete AND b.status = 'CONFIRMED' AND k.so_khach <> k.so_ten

-- 23 ---------------------------------------------------------------------
-- Ngày khởi hành có lưu trú qua đêm mà không có giá phòng đơn.
--
-- Thiếu nó thì KHÔNG có gì hỏng, và đó là toàn bộ vấn đề: máy tính giá lấy
-- `giá phòng đơn − giá phòng đôi`, không thấy dòng nào thì phụ thu bằng 0, và
-- khách đi một mình đặt được nguyên chuyến ở giá chia đôi phòng. Chênh lệch
-- chỉ lộ ra khi kế toán đối soát với khách sạn — sau khi khách đã đi.
--
-- `DAY_TOUR` loại ra vì tour trong ngày không có đêm nào để ở phòng.
UNION ALL
SELECT 23, 'LOI',
       'departure ' || d.id || ' @' || d.market || ' ' || d.depart_date,
       p.product_type || ' có lưu trú nhưng không có dòng giá occupancy = SINGLE'
FROM departure d
JOIN product p ON p.id = d.product_id AND NOT p.soft_delete
WHERE NOT d.soft_delete
  AND p.product_type <> 'DAY_TOUR'
  AND NOT EXISTS (
    SELECT 1 FROM departure_price dp
    WHERE dp.departure_id = d.id AND dp.occupancy = 'SINGLE')

-- 23b --------------------------------------------------------------------
-- ...và phụ thu phải DƯƠNG. Giá phòng đơn bằng hoặc thấp hơn giá phòng đôi là
-- một dòng có mặt cho đủ, không phải một mức giá — hệ quả giống hệt việc thiếu
-- hẳn dòng ấy, chỉ khác là quy tắc trên không bắt được.
UNION ALL
SELECT 23, 'LOI',
       'departure ' || d.id || ' @' || d.market || ' ' || d.depart_date,
       'giá phòng đơn ' || don.amount || ' không cao hơn giá phòng đôi ' || doi.amount
         || ' (loại khách ' || x.code || ')'
FROM departure d
JOIN departure_price don ON don.departure_id = d.id AND don.occupancy = 'SINGLE'
JOIN departure_price doi ON doi.departure_id = d.id AND doi.occupancy = 'DOUBLE'
                        AND doi.pax_type_id = don.pax_type_id
JOIN pax_type x ON x.id = don.pax_type_id
WHERE NOT d.soft_delete AND don.amount <= doi.amount

)

SELECT quy_tac, muc_do, doi_tuong, chi_tiet
FROM vi_pham
ORDER BY (muc_do = 'CANH_BAO'), quy_tac, doi_tuong;
