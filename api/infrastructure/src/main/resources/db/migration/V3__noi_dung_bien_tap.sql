-- Tầng nội dung biên tập: chủ đề, khách sạn, tham quan, lịch trình từng ngày,
-- bài viết và buổi thuyết trình.
--
-- DDL đầy đủ và lý do từng quyết định: docs/12 mục 4.8 tới 4.11.
--
-- Bốn quy tắc kiểm nhất quán ở docs/12 mục 9 đã tham chiếu tới những bảng này
-- từ trước khi chúng tồn tại — quy tắc 3 (số ngày lịch trình khớp duration_days),
-- 4 (số đêm khách sạn khớp lịch trình), 10 (hotel_id không chết), 12 (mỗi điểm
-- đến có ít nhất một khách sạn và một tham quan).
--
-- KHÔNG có trong migration này: site_info. docs/13 mục 9.1 mới nói đúng bốn chữ
-- "thị thực, mùa, tiền tệ, lệch giờ" — chưa đủ để dựng bảng, và bịa lược đồ cho
-- một thứ chưa ai đặc tả là cách chắc chắn để phải sửa lại. Ghi ở docs/12 mục 10.

-- ------------------------------------------------------------- 1. Chủ đề

CREATE TABLE theme (
  id         UUID        PRIMARY KEY,
  code       VARCHAR(32) NOT NULL,          -- 'TREKKING', 'RIVER_CRUISE'
  sort_order SMALLINT    NOT NULL DEFAULT 0
);

CREATE TABLE theme_translation (
  theme_id UUID         NOT NULL REFERENCES theme (id) ON DELETE CASCADE,
  locale   VARCHAR(8)   NOT NULL REFERENCES locale (code),
  slug     VARCHAR(160) NOT NULL,
  name     VARCHAR(120) NOT NULL,
  PRIMARY KEY (theme_id, locale),
  CONSTRAINT ck_tht_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

-- Nhóm C: vòng đời theo product.
CREATE TABLE product_theme (
  product_id UUID NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  theme_id   UUID NOT NULL REFERENCES theme (id),
  PRIMARY KEY (product_id, theme_id)
);

-- ------------------------------------------- 2. Khách sạn và tham quan

-- name KHÔNG nằm trong bảng dịch: tên riêng của khách sạn không dịch
-- (docs/24 mục 5). Chỉ phần mô tả mới là nội dung phải dịch.
CREATE TABLE hotel (
  id             UUID         PRIMARY KEY,
  destination_id UUID         NOT NULL REFERENCES destination (id),
  name           VARCHAR(160) NOT NULL,
  stars          SMALLINT,
  image          TEXT,
  CONSTRAINT ck_hotel_stars CHECK (stars IS NULL OR stars BETWEEN 1 AND 5)
);

CREATE TABLE hotel_translation (
  hotel_id    UUID       NOT NULL REFERENCES hotel (id) ON DELETE CASCADE,
  locale      VARCHAR(8) NOT NULL REFERENCES locale (code),
  description TEXT       NOT NULL,
  PRIMARY KEY (hotel_id, locale)
);

-- Tham quan thuộc về ĐIỂM ĐẾN, không thuộc về sản phẩm: cùng một chuyến thăm
-- Văn Miếu xuất hiện trong nhiều tour. Quy tắc kiểm 12 đếm theo điểm đến.
CREATE TABLE excursion (
  id             UUID        PRIMARY KEY,
  destination_id UUID        NOT NULL REFERENCES destination (id),
  code           VARCHAR(48) NOT NULL,
  duration_hours SMALLINT,
  image          TEXT,
  CONSTRAINT ck_exc_duration CHECK (duration_hours IS NULL OR duration_hours BETWEEN 1 AND 24)
);

CREATE TABLE excursion_translation (
  excursion_id UUID         NOT NULL REFERENCES excursion (id) ON DELETE CASCADE,
  locale       VARCHAR(8)   NOT NULL REFERENCES locale (code),
  name         VARCHAR(160) NOT NULL,
  description  TEXT         NOT NULL,
  PRIMARY KEY (excursion_id, locale)
);

-- Nhóm C: vòng đời theo product. Quy tắc kiểm 4 đối chiếu tổng nights ở đây
-- với số đêm có khách sạn trong lịch trình, THEO TỪNG khách sạn.
CREATE TABLE product_hotel_stay (
  product_id UUID     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  hotel_id   UUID     NOT NULL REFERENCES hotel (id),
  nights     SMALLINT NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id, hotel_id),
  CONSTRAINT ck_phs_nights CHECK (nights >= 1)
);

-- ------------------------------------------------------- 3. Lịch trình

-- Là thực thể chứ không phải bảng dòng chi tiết: biên tập viên sửa từng ngày
-- một, xoá một ngày rồi thêm lại ngày khác cùng số thứ tự, và mỗi ngày có nội
-- dung phải dịch riêng. Vì thế nhận đủ năm cột, và khoá duy nhất phải bộ phận.
CREATE TABLE itinerary_day (
  id             UUID     PRIMARY KEY,
  product_id     UUID     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  day_number     SMALLINT NOT NULL,
  -- Ngủ đêm ở đâu. NULL với ngày bay hoặc ngày trên tàu.
  destination_id UUID     REFERENCES destination (id),
  hotel_id       UUID     REFERENCES hotel (id),
  CONSTRAINT ck_itd_day CHECK (day_number >= 1)
);

CREATE TABLE itinerary_day_translation (
  itinerary_day_id UUID         NOT NULL REFERENCES itinerary_day (id) ON DELETE CASCADE,
  locale           VARCHAR(8)   NOT NULL REFERENCES locale (code),
  title            VARCHAR(200) NOT NULL,
  description      TEXT         NOT NULL,
  PRIMARY KEY (itinerary_day_id, locale)
);

-- --------------------------------------- 4. Bài viết, thẻ, thuyết trình

CREATE TABLE tag (
  id         UUID        PRIMARY KEY,
  code       VARCHAR(32) NOT NULL,
  sort_order SMALLINT    NOT NULL DEFAULT 0
);

CREATE TABLE tag_translation (
  tag_id UUID         NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
  locale VARCHAR(8)   NOT NULL REFERENCES locale (code),
  slug   VARCHAR(160) NOT NULL,
  name   VARCHAR(120) NOT NULL,
  PRIMARY KEY (tag_id, locale),
  CONSTRAINT ck_tgt_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

-- Bài viết KHÔNG có cổng chặn thị trường ở v1: market trong đường dẫn chỉ là
-- quy ước chung của API công khai. Cần bài riêng cho từng thị trường thì thêm
-- post_market đúng khuôn product_market — docs/12 mục 4.11.
CREATE TABLE post (
  id           UUID PRIMARY KEY,
  hero_image   TEXT,
  published_at TIMESTAMPTZ,
  author_id    UUID REFERENCES staff_user (id)
);

-- Cùng khuôn product_translation: status ba giá trị, OUTDATED không lưu.
CREATE TABLE post_translation (
  post_id       UUID         NOT NULL REFERENCES post (id) ON DELETE CASCADE,
  locale        VARCHAR(8)   NOT NULL REFERENCES locale (code),
  slug          VARCHAR(160) NOT NULL,
  title         VARCHAR(200) NOT NULL,
  excerpt       TEXT         NOT NULL,
  body          TEXT[]       NOT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
  translated_at TIMESTAMPTZ,
  translated_by UUID         REFERENCES staff_user (id),

  PRIMARY KEY (post_id, locale),
  CONSTRAINT ck_pot_status CHECK (status IN ('DRAFT','TRANSLATED','PUBLISHED')),
  CONSTRAINT ck_pot_slug   CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  CONSTRAINT ck_pot_body   CHECK (array_length(body, 1) >= 1)
);

-- Nhóm C: vòng đời theo post.
CREATE TABLE post_tag (
  post_id UUID NOT NULL REFERENCES post (id) ON DELETE CASCADE,
  tag_id  UUID NOT NULL REFERENCES tag (id),
  PRIMARY KEY (post_id, tag_id)
);

-- Buổi thuyết trình thuộc về MỘT thị trường: buổi ở Odense phục vụ khách Đan,
-- buổi ở Hà Nội phục vụ khách Việt — hai sự kiện khác nhau.
--
-- Cột tên là event_date, KHÔNG phải date: `date` là tên kiểu dữ liệu của
-- Postgres, đặt làm tên cột thì mọi câu truy vấn phải trích dẫn nó. Cùng loại
-- bẫy với cột `collation` ở V1.
CREATE TABLE lecture (
  id          UUID         PRIMARY KEY,
  market      VARCHAR(2)   NOT NULL REFERENCES market (code),
  event_date  DATE         NOT NULL,
  start_time  TIME,
  city        VARCHAR(64)  NOT NULL,
  venue       VARCHAR(160),
  seats       SMALLINT     NOT NULL,
  seats_taken SMALLINT     NOT NULL DEFAULT 0,

  CONSTRAINT ck_lec_seats CHECK (seats > 0 AND seats_taken BETWEEN 0 AND seats)
);

CREATE TABLE lecture_translation (
  lecture_id  UUID         NOT NULL REFERENCES lecture (id) ON DELETE CASCADE,
  locale      VARCHAR(8)   NOT NULL REFERENCES locale (code),
  title       VARCHAR(200) NOT NULL,
  description TEXT         NOT NULL,
  PRIMARY KEY (lecture_id, locale)
);

-- ------------------------------------------- 5. Cột kiểm toán và xoá mềm
--
-- Cùng khuôn mẫu với V1 và V2. Bảng nào nhận cột nào: docs/11 mục 11.2.
-- Ba bảng nối product_theme, product_hotel_stay, post_tag thuộc nhóm C nên
-- không có mặt ở đây.

DO $$
DECLARE
  bang text;
  co_xoa_mem text[] := ARRAY[
    'theme','theme_translation',
    'hotel','hotel_translation',
    'excursion','excursion_translation',
    'itinerary_day','itinerary_day_translation',
    'tag','tag_translation',
    'post','post_translation',
    'lecture','lecture_translation'];
BEGIN
  FOREACH bang IN ARRAY co_xoa_mem LOOP
    EXECUTE format(
      'ALTER TABLE %1$I
         ADD COLUMN created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
         ADD COLUMN created_by       UUID REFERENCES staff_user (id),
         ADD COLUMN last_modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
         ADD COLUMN last_modified_by UUID REFERENCES staff_user (id),
         ADD COLUMN soft_delete      BOOLEAN NOT NULL DEFAULT FALSE', bang);

    EXECUTE format(
      'CREATE TRIGGER tg_%1$s_last_modified BEFORE UPDATE ON %1$I
         FOR EACH ROW EXECUTE FUNCTION trg_set_last_modified()', bang);
  END LOOP;
END $$;

-- ------------------------------------------------------------- 6. Index
--
-- Bảng có soft_delete thì khoá duy nhất phải là index BỘ PHẬN — docs/12 mục 2.2.

CREATE UNIQUE INDEX ux_theme_code ON theme (code) WHERE NOT soft_delete;
CREATE UNIQUE INDEX ux_theme_translation_slug
  ON theme_translation (locale, slug) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_tag_code ON tag (code) WHERE NOT soft_delete;
CREATE UNIQUE INDEX ux_tag_translation_slug
  ON tag_translation (locale, slug) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_hotel ON hotel (destination_id, name) WHERE NOT soft_delete;
CREATE UNIQUE INDEX ux_excursion ON excursion (destination_id, code) WHERE NOT soft_delete;

-- Một sản phẩm không có hai ngày cùng số thứ tự. Xoá mềm một ngày rồi thêm
-- lại ngày khác cùng số là thao tác biên tập bình thường, nên index phải bộ phận.
CREATE UNIQUE INDEX ux_itinerary_day
  ON itinerary_day (product_id, day_number) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_post_translation_slug
  ON post_translation (locale, slug) WHERE NOT soft_delete;

CREATE INDEX ix_itinerary_day_product ON itinerary_day (product_id, day_number);
CREATE INDEX ix_hotel_destination     ON hotel (destination_id);
CREATE INDEX ix_excursion_destination ON excursion (destination_id);
CREATE INDEX ix_product_hotel_stay    ON product_hotel_stay (product_id, sort_order);
CREATE INDEX ix_post_tag_tag          ON post_tag (tag_id);

-- Listing bài viết: lọc theo locale và trạng thái, sắp theo ngày xuất bản.
CREATE INDEX ix_post_translation_listing
  ON post_translation (locale, status)
  INCLUDE (post_id, slug, title)
  WHERE status = 'PUBLISHED' AND NOT soft_delete;

-- Trang sự kiện chỉ hiện buổi CHƯA diễn ra — docs/13 mục 9.1.
CREATE INDEX ix_lecture_upcoming
  ON lecture (market, event_date) WHERE NOT soft_delete;

-- ------------------------------------------------- 7. Trigger lưu slug cũ
--
-- 'POST' đã nằm sẵn trong ck_slug_history_type của V2.

CREATE TRIGGER tg_post_translation_slug_cu
  AFTER UPDATE OF slug ON post_translation
  FOR EACH ROW EXECUTE FUNCTION trg_luu_slug_cu('POST', 'post_id');
