-- Lược đồ khởi tạo. Nguồn sự thật: docs/12-luoc-do-csdl.md
--
-- Quy ước: tên bảng snake_case số ít · khoá chính UUID sinh ở tầng ứng dụng
-- (UUID v7) · tiền NUMERIC(12,2) luôn đi kèm cột currency · thời điểm TIMESTAMPTZ
-- lưu UTC · enum đóng dùng VARCHAR + CHECK, không dùng kiểu ENUM của Postgres.
--
-- Thứ tự các mục KHÔNG tuỳ tiện:
--   1–10  bảng
--   11    năm cột kiểm toán và xoá mềm, thêm bằng vòng lặp
--   12    index — phải sau mục 11 vì phần lớn là index BỘ PHẬN theo soft_delete
--   13    khung nhìn — cũng lọc soft_delete
--   14    trigger nghiệp vụ
--
-- KHÔNG sửa file này sau khi đã chạy ở bất kỳ môi trường nào. Đổi lược đồ thì
-- viết migration mới — docs/12 mục 8.

-- ---------------------------------------------------------------- 1. Chuẩn bị

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() được đánh dấu STABLE nên CREATE INDEX (unaccent(title)) bị từ chối.
-- Bọc lại thành hàm IMMUTABLE. Mọi index tìm kiếm không dấu dùng f_unaccent,
-- không dùng unaccent — docs/12 mục 1.1.
CREATE FUNCTION f_unaccent(text) RETURNS text
  LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS
$$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

-- ---------------------------------------------------------------- 2. Cấu hình

CREATE TABLE locale (
  code       VARCHAR(8)  PRIMARY KEY,                  -- 'da', 'vi'
  -- KHÔNG đặt tên cột là `collation`: đó là từ khoá dành riêng của Postgres,
  -- CREATE TABLE đỏ ngay với "syntax error at or near collation".
  collation_name TEXT     NOT NULL,                    -- 'da-DK-x-icu'
  is_source  BOOLEAN     NOT NULL DEFAULT FALSE,
  is_active  BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE market (
  code              VARCHAR(2)    PRIMARY KEY,         -- 'DK', 'VN'
  currency          VARCHAR(3)    NOT NULL,
  fraction_digits   SMALLINT      NOT NULL,            -- DKK 2, VND 0
  default_locale    VARCHAR(8)    NOT NULL,
  deposit_rate      NUMERIC(5,4)  NOT NULL,
  processing_fee    NUMERIC(12,2) NOT NULL,
  is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
  CONSTRAINT ck_market_code   CHECK (code = UPPER(code)),
  CONSTRAINT ck_market_digits CHECK (fraction_digits BETWEEN 0 AND 4),
  CONSTRAINT ck_market_rate   CHECK (deposit_rate BETWEEN 0 AND 1)
);

ALTER TABLE market
  ADD CONSTRAINT fk_market_locale
  FOREIGN KEY (default_locale) REFERENCES locale (code);

CREATE TABLE pax_type (
  id            UUID         PRIMARY KEY,
  market        VARCHAR(2)   NOT NULL REFERENCES market (code),
  code          VARCHAR(16)  NOT NULL,                 -- 'ADULT', 'CHILD_5_11'
  min_age       SMALLINT,
  max_age       SMALLINT,
  discount_rate NUMERIC(5,4) NOT NULL DEFAULT 0,
  sort_order    SMALLINT     NOT NULL,
  CONSTRAINT ck_pax_type_age CHECK (min_age IS NULL OR max_age IS NULL
                                    OR min_age <= max_age)
);

CREATE TABLE departure_origin (
  id         UUID          PRIMARY KEY,
  market     VARCHAR(2)    NOT NULL REFERENCES market (code),
  city       VARCHAR(64)   NOT NULL,
  iata_code  VARCHAR(3),
  surcharge  NUMERIC(12,2) NOT NULL DEFAULT 0,         -- dùng ở DK
  is_default BOOLEAN       NOT NULL DEFAULT FALSE
);

-- ---------------------------------------------------------------- 3. Người dùng

CREATE TABLE staff_user (
  id            UUID         PRIMARY KEY,
  email         VARCHAR(320) NOT NULL,
  display_name  VARCHAR(120) NOT NULL,
  password_hash TEXT         NOT NULL,
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE consultant (
  id         UUID         PRIMARY KEY,
  market     VARCHAR(2)   NOT NULL REFERENCES market (code),
  full_name  VARCHAR(120) NOT NULL,
  photo      TEXT,
  email      VARCHAR(320),
  phone      VARCHAR(32),
  is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------- 4. Địa lý
--
-- Cấp phân loại là miền → điểm đến, KHÔNG phải châu lục → quốc gia: chỉ bán
-- một nước. Bảng `region` đúng ba dòng.

CREATE TABLE region (
  id         UUID        PRIMARY KEY,
  code       VARCHAR(16) NOT NULL,                     -- 'NORTH','CENTRAL','SOUTH'
  sort_order SMALLINT    NOT NULL
);

CREATE TABLE region_translation (
  region_id UUID         NOT NULL REFERENCES region (id) ON DELETE CASCADE,
  locale    VARCHAR(8)   NOT NULL REFERENCES locale (code),
  slug      VARCHAR(160) NOT NULL,
  name      VARCHAR(120) NOT NULL,
  PRIMARY KEY (region_id, locale),
  CONSTRAINT ck_rt_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE TABLE destination (
  id         UUID        PRIMARY KEY,
  region_id  UUID        NOT NULL REFERENCES region (id),
  code       VARCHAR(32) NOT NULL,
  sort_order SMALLINT    NOT NULL DEFAULT 0
);

CREATE TABLE destination_translation (
  destination_id UUID         NOT NULL REFERENCES destination (id) ON DELETE CASCADE,
  locale         VARCHAR(8)   NOT NULL REFERENCES locale (code),
  slug           VARCHAR(160) NOT NULL,
  name           VARCHAR(120) NOT NULL,
  summary        TEXT,
  PRIMARY KEY (destination_id, locale),
  CONSTRAINT ck_dt_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

-- ---------------------------------------------------------------- 5. Sản phẩm

CREATE TABLE product (
  id                     UUID        PRIMARY KEY,
  product_type           VARCHAR(24) NOT NULL,
  primary_destination_id UUID        NOT NULL REFERENCES destination (id),
  duration_days          SMALLINT,                     -- NULL với DAY_TOUR
  hero_image             TEXT        NOT NULL,
  map_image              TEXT,
  is_new                 BOOLEAN     NOT NULL DEFAULT FALSE,
  rating                 NUMERIC(2,1),
  review_count           INTEGER     NOT NULL DEFAULT 0,
  consultant_id          UUID        REFERENCES consultant (id),

  CONSTRAINT ck_product_type CHECK (product_type IN
    ('GROUP_TOUR','INDIVIDUAL_PACKAGE','PRIVATE_TOUR',
     'CRUISE','COMBO','DAY_TOUR')),
  CONSTRAINT ck_product_duration CHECK (
    (product_type = 'DAY_TOUR' AND duration_days IS NULL) OR
    (product_type <> 'DAY_TOUR' AND duration_days BETWEEN 1 AND 60)),
  CONSTRAINT ck_product_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),

  -- Khoá ngoại kép của sáu bảng con trỏ tới đúng ràng buộc này.
  -- KHÔNG chuyển thành index bộ phận WHERE NOT soft_delete: khoá ngoại không
  -- tham chiếu được index bộ phận, migration sẽ đỏ ngay ở bảng con đầu tiên.
  -- Đây là ngoại lệ duy nhất của quy tắc ở docs/12 mục 2.2.
  CONSTRAINT ux_product_id_type UNIQUE (id, product_type)
);

-- Bảng con theo loại — ADR-005. Khoá ngoại kép (id, product_type) làm cho KHÔNG
-- THỂ gắn một dòng product_group_tour vào sản phẩm loại CRUISE.
--
-- Nhóm C của docs/11 mục 11.2: KHÔNG có cột kiểm toán, KHÔNG có soft_delete.
-- Vòng đời trùng khít với product và đã có ON DELETE CASCADE; thêm
-- last_modified_by vào đây tạo ra câu trả lời thứ hai cho "ai sửa sản phẩm này".

CREATE TABLE product_group_tour (
  product_id           UUID        PRIMARY KEY,
  product_type         VARCHAR(24) NOT NULL DEFAULT 'GROUP_TOUR',
  min_pax              SMALLINT    NOT NULL,
  max_pax              SMALLINT    NOT NULL,
  guaranteed_threshold SMALLINT    NOT NULL,
  tour_leader_language VARCHAR(8)  NOT NULL REFERENCES locale (code),
  fitness_level        SMALLINT    NOT NULL,
  CONSTRAINT ck_pgt_type    CHECK (product_type = 'GROUP_TOUR'),
  CONSTRAINT ck_pgt_pax     CHECK (min_pax BETWEEN 10 AND 25
                                   AND max_pax BETWEEN 10 AND 25
                                   AND min_pax <= max_pax),
  CONSTRAINT ck_pgt_guar    CHECK (guaranteed_threshold BETWEEN 1 AND min_pax),
  CONSTRAINT ck_pgt_fitness CHECK (fitness_level BETWEEN 1 AND 4),
  CONSTRAINT fk_pgt_product FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);

CREATE TABLE product_individual (
  product_id                UUID        PRIMARY KEY,
  product_type              VARCHAR(24) NOT NULL DEFAULT 'INDIVIDUAL_PACKAGE',
  min_party_size            SMALLINT    NOT NULL,
  flexible_date_window_days SMALLINT    NOT NULL DEFAULT 0,
  CONSTRAINT ck_pi_type   CHECK (product_type = 'INDIVIDUAL_PACKAGE'),
  CONSTRAINT ck_pi_party  CHECK (min_party_size >= 1),
  CONSTRAINT ck_pi_window CHECK (flexible_date_window_days BETWEEN 0 AND 30),
  CONSTRAINT fk_pi_product FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);

CREATE TABLE product_private (
  product_id       UUID        PRIMARY KEY,
  product_type     VARCHAR(24) NOT NULL DEFAULT 'PRIVATE_TOUR',
  lead_time_days   SMALLINT    NOT NULL,
  quote_valid_days SMALLINT    NOT NULL,
  CONSTRAINT ck_pp_type  CHECK (product_type = 'PRIVATE_TOUR'),
  CONSTRAINT ck_pp_lead  CHECK (lead_time_days BETWEEN 1 AND 90),
  CONSTRAINT ck_pp_quote CHECK (quote_valid_days BETWEEN 1 AND 30),
  CONSTRAINT fk_pp_product FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);

CREATE TABLE product_cruise (
  product_id   UUID         PRIMARY KEY,
  product_type VARCHAR(24)  NOT NULL DEFAULT 'CRUISE',
  ship_name    VARCHAR(120) NOT NULL,
  port_count   SMALLINT     NOT NULL,
  CONSTRAINT ck_pc_type CHECK (product_type = 'CRUISE'),
  CONSTRAINT ck_pc_port CHECK (port_count >= 1),
  CONSTRAINT fk_pc_product FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);

CREATE TABLE product_combo (
  product_id   UUID        PRIMARY KEY,
  product_type VARCHAR(24) NOT NULL DEFAULT 'COMBO',
  nights       SMALLINT    NOT NULL,
  valid_from   DATE        NOT NULL,
  valid_to     DATE        NOT NULL,
  CONSTRAINT ck_pcb_type   CHECK (product_type = 'COMBO'),
  CONSTRAINT ck_pcb_nights CHECK (nights BETWEEN 1 AND 14),
  CONSTRAINT ck_pcb_valid  CHECK (valid_to >= valid_from),
  CONSTRAINT fk_pcb_product FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);

CREATE TABLE product_day_tour (
  product_id     UUID        PRIMARY KEY,
  product_type   VARCHAR(24) NOT NULL DEFAULT 'DAY_TOUR',
  duration_hours SMALLINT    NOT NULL,
  cutoff_hours   SMALLINT    NOT NULL DEFAULT 0,
  CONSTRAINT ck_pdt_type     CHECK (product_type = 'DAY_TOUR'),
  CONSTRAINT ck_pdt_duration CHECK (duration_hours BETWEEN 1 AND 24),
  CONSTRAINT ck_pdt_cutoff   CHECK (cutoff_hours >= 0),
  CONSTRAINT fk_pdt_product FOREIGN KEY (product_id, product_type)
    REFERENCES product (id, product_type) ON DELETE CASCADE
);

-- ---------------------------------------------------------------- 6. Bản dịch

CREATE TABLE product_translation (
  product_id        UUID         NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  locale            VARCHAR(8)   NOT NULL REFERENCES locale (code),
  slug              VARCHAR(160) NOT NULL,
  title             VARCHAR(200) NOT NULL,
  short_description TEXT         NOT NULL,
  long_description  TEXT[]       NOT NULL,
  why_choose_this   TEXT[]       NOT NULL,
  hero_image_alt    VARCHAR(300) NOT NULL,
  status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
  -- translated_* KHÁC last_modified_*: dịch xong khác với sửa chính tả.
  translated_at     TIMESTAMPTZ,
  translated_by     UUID         REFERENCES staff_user (id),

  PRIMARY KEY (product_id, locale),
  -- OUTDATED KHÔNG có ở đây: nó là giá trị tính ra, không ai nhập tay được.
  CONSTRAINT ck_pt_status CHECK (status IN ('DRAFT','TRANSLATED','PUBLISHED')),
  -- Slug không dấu ở cả hai ngôn ngữ, cưỡng chế ngay tầng CSDL:
  -- 'bekræftelse' và 'việt-nam' đều bị từ chối.
  CONSTRAINT ck_pt_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  CONSTRAINT ck_pt_why  CHECK (array_length(why_choose_this, 1) BETWEEN 3 AND 7),
  CONSTRAINT ck_pt_long CHECK (array_length(long_description, 1) >= 2)
);

-- Cổng chặn thị trường: sản phẩm chỉ tồn tại ở thị trường có dòng ở đây.
-- Nhóm C: không cột kiểm toán, không soft_delete — đã có is_published.
CREATE TABLE product_market (
  product_id   UUID          NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  market       VARCHAR(2)    NOT NULL REFERENCES market (code),
  is_published BOOLEAN       NOT NULL DEFAULT FALSE,
  price_from   NUMERIC(12,2),                          -- vật chất hoá, trigger cập nhật
  published_at TIMESTAMPTZ,
  PRIMARY KEY (product_id, market)
);

-- ------------------------------------------------- 7. Ngày khởi hành và giá
--
-- Một departure thuộc về ĐÚNG MỘT thị trường — ADR-006. Đoàn khách Đan bay từ
-- Copenhagen với trưởng đoàn nói tiếng Đan, và đoàn khách Việt khởi hành từ Hà
-- Nội với hướng dẫn viên tiếng Việt, là hai chuyến đi khác nhau.

CREATE TABLE departure (
  id                  UUID        PRIMARY KEY,
  product_id          UUID        NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  market              VARCHAR(2)  NOT NULL REFERENCES market (code),
  depart_date         DATE        NOT NULL,
  return_date         DATE        NOT NULL,
  days                SMALLINT    NOT NULL,
  cabin_category      VARCHAR(16),                     -- chỉ CRUISE
  base_status         VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  capacity            SMALLINT    NOT NULL,
  seats_booked        SMALLINT    NOT NULL DEFAULT 0,
  departure_origin_id UUID        REFERENCES departure_origin (id),

  -- KHÔNG có GUARANTEED: đó là giá trị tính ra (seats_booked >= min_pax),
  -- cưỡng chế ở tầng kiểu dữ liệu để không ai nhập tay được.
  CONSTRAINT ck_dep_status CHECK (base_status IN
    ('OPEN','FEW_SEATS','SOLD_OUT','PENDING')),
  CONSTRAINT ck_dep_dates  CHECK (return_date = depart_date + (days - 1)),
  CONSTRAINT ck_dep_cabin  CHECK (cabin_category IS NULL OR cabin_category IN
    ('INSIDE','OUTSIDE','BALCONY','AQUA')),
  CONSTRAINT ck_dep_seats  CHECK (seats_booked BETWEEN 0 AND capacity)
);

-- Nhóm C: bảng dòng giá, vòng đời theo departure.
CREATE TABLE departure_price (
  departure_id UUID          NOT NULL REFERENCES departure (id) ON DELETE CASCADE,
  pax_type_id  UUID          NOT NULL REFERENCES pax_type (id),
  occupancy    VARCHAR(8)    NOT NULL,
  amount       NUMERIC(12,2) NOT NULL,
  currency     VARCHAR(3)    NOT NULL,
  PRIMARY KEY (departure_id, pax_type_id, occupancy),
  CONSTRAINT ck_dp_occupancy CHECK (occupancy IN ('DOUBLE','SINGLE')),
  CONSTRAINT ck_dp_amount    CHECK (amount >= 0)
);

CREATE TABLE price_tier (                              -- chỉ PRIVATE_TOUR
  id               UUID          PRIMARY KEY,
  product_id       UUID          NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  market           VARCHAR(2)    NOT NULL REFERENCES market (code),
  min_pax          SMALLINT      NOT NULL,
  max_pax          SMALLINT,                           -- NULL = bậc cuối, không trần
  price_per_person NUMERIC(12,2) NOT NULL,
  currency         VARCHAR(3)    NOT NULL,
  CONSTRAINT ck_tier_range CHECK (max_pax IS NULL OR max_pax >= min_pax)
);

-- ---------------------------------------------------------------- 8. Báo giá

CREATE TABLE quote (
  id          UUID        PRIMARY KEY,
  reference   VARCHAR(20) NOT NULL,
  product_id  UUID        NOT NULL REFERENCES product (id),
  market      VARCHAR(2)  NOT NULL REFERENCES market (code),
  locale      VARCHAR(8)  NOT NULL REFERENCES locale (code),
  status      VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  party_size  SMALLINT    NOT NULL,
  total       NUMERIC(12,2),
  currency    VARCHAR(3),
  valid_until DATE,
  CONSTRAINT ck_quote_status CHECK (status IN
    ('DRAFT','SENT','ACCEPTED','REJECTED','EXPIRED')),
  CONSTRAINT ck_quote_party CHECK (party_size >= 1)
);

CREATE TABLE quote_line (
  quote_id    UUID          NOT NULL REFERENCES quote (id) ON DELETE CASCADE,
  seq         SMALLINT      NOT NULL,
  label_key   VARCHAR(64)   NOT NULL,                  -- KHOÁ CHUỖI, không phải câu
  quantity    NUMERIC(10,2),
  unit_amount NUMERIC(12,2),
  amount      NUMERIC(12,2) NOT NULL,
  PRIMARY KEY (quote_id, seq)
);

-- ---------------------------------------------------------------- 9. Đơn đặt

CREATE TABLE booking (
  id             UUID          PRIMARY KEY,
  reference      VARCHAR(20)   NOT NULL,               -- 'VN-2026-8F3K2P'
  market         VARCHAR(2)    NOT NULL REFERENCES market (code),
  locale         VARCHAR(8)    NOT NULL REFERENCES locale (code),
  product_id     UUID          NOT NULL REFERENCES product (id),
  departure_id   UUID          REFERENCES departure (id),  -- NULL với PRIVATE/COMBO
  quote_id       UUID          REFERENCES quote (id),
  status         VARCHAR(24)   NOT NULL,
  product_title  VARCHAR(200)  NOT NULL,               -- CHỤP LẠI lúc đặt
  total          NUMERIC(12,2) NOT NULL,
  deposit        NUMERIC(12,2) NOT NULL,
  currency       VARCHAR(3)    NOT NULL,
  contact_email  VARCHAR(320)  NOT NULL,
  contact_phone  VARCHAR(32)   NOT NULL,

  CONSTRAINT ck_booking_status CHECK (status IN
    ('DRAFT','PENDING_PAYMENT','PENDING_CONFIRMATION','CONFIRMED',
     'COMPLETED','CANCELLED','REFUNDED','EXPIRED')),
  CONSTRAINT ck_booking_deposit CHECK (deposit BETWEEN 0 AND total)
);

-- Nhóm C.
CREATE TABLE booking_line (
  booking_id  UUID          NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
  seq         SMALLINT      NOT NULL,
  line_key    VARCHAR(32)   NOT NULL,
  label_key   VARCHAR(64)   NOT NULL,                  -- KHOÁ CHUỖI, không phải câu
  quantity    NUMERIC(10,2),
  unit_amount NUMERIC(12,2),
  amount      NUMERIC(12,2) NOT NULL,                  -- âm = giảm trừ
  PRIMARY KEY (booking_id, seq)
);

-- Nhóm D — nhật ký chỉ ghi thêm. KHÔNG có last_modified_*, KHÔNG có soft_delete:
-- thêm hai thứ đó vào một nhật ký kiểm toán là cho phép sửa và giấu lịch sử,
-- phá đúng cái tính chất khiến bảng này có giá trị. docs/11 mục 11.2.
-- UPDATE và DELETE bị thu hồi ở tầng vai trò CSDL, không chỉ trông vào kỷ luật code.
CREATE TABLE booking_event (
  id          UUID        PRIMARY KEY,
  booking_id  UUID        NOT NULL REFERENCES booking (id),
  from_status VARCHAR(24),
  to_status   VARCHAR(24) NOT NULL,
  actor_type  VARCHAR(16) NOT NULL,                    -- 'CUSTOMER','STAFF','SYSTEM'
  actor_id    UUID,
  note        TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_be_actor CHECK (actor_type IN ('CUSTOMER','STAFF','SYSTEM'))
);

-- ---------------------------------------------------------------- 10. Giữ chỗ

CREATE TABLE seat_hold (
  id           UUID        PRIMARY KEY,
  departure_id UUID        NOT NULL REFERENCES departure (id) ON DELETE CASCADE,
  seats        SMALLINT    NOT NULL CHECK (seats > 0),
  session_ref  VARCHAR(64) NOT NULL,
  expires_at   TIMESTAMPTZ NOT NULL,
  released_at  TIMESTAMPTZ,
  booking_id   UUID        REFERENCES booking (id)
);

-- ------------------------------------- 11. Cột kiểm toán và xoá mềm
--
-- docs/11 mục 11 và docs/12 mục 2.1. Thêm bằng vòng lặp thay vì chép tay 17
-- lần: thêm bảng mới thì thêm đúng một dòng tên vào mảng bên dưới, và bảng nào
-- bị bỏ sót sẽ lộ ra ở quy tắc kiểm 17 của docs/12 mục 9.
--
-- Ba nhóm bảng CỐ TÌNH không có mặt ở đây:
--   · bảng con 1-1 và bảng dòng chi tiết — vòng đời trùng bảng cha
--   · booking_event — nhật ký chỉ ghi thêm
--   · market, locale không có soft_delete — đã có is_active, hai cột cùng nghĩa
--     "không dùng được nữa" chắc chắn sẽ lệch nhau

-- last_modified_at do TRIGGER đặt, không phải ứng dụng: nó luôn đúng và không
-- ai quên được. last_modified_by thì ứng dụng ghi, vì CSDL không biết ai đang
-- thao tác.
CREATE FUNCTION trg_set_last_modified() RETURNS trigger
LANGUAGE plpgsql AS
$$
BEGIN
  NEW.last_modified_at := now();
  RETURN NEW;
END
$$;

DO
$$
DECLARE
  bang            text;
  co_xoa_mem      text[] := ARRAY[
    'pax_type', 'departure_origin', 'staff_user', 'consultant',
    'region', 'region_translation', 'destination', 'destination_translation',
    'product', 'product_translation', 'departure', 'price_tier',
    'seat_hold', 'quote', 'booking'];
  chi_kiem_toan   text[] := ARRAY['market', 'locale'];
BEGIN
  FOREACH bang IN ARRAY (co_xoa_mem || chi_kiem_toan) LOOP
    EXECUTE format(
      'ALTER TABLE %1$I'
      ' ADD COLUMN created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),'
      ' ADD COLUMN created_by       UUID REFERENCES staff_user (id),'
      ' ADD COLUMN last_modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),'
      ' ADD COLUMN last_modified_by UUID REFERENCES staff_user (id)', bang);

    EXECUTE format(
      'CREATE TRIGGER tg_%1$s_last_modified BEFORE UPDATE ON %1$I'
      ' FOR EACH ROW EXECUTE FUNCTION trg_set_last_modified()', bang);
  END LOOP;

  FOREACH bang IN ARRAY co_xoa_mem LOOP
    EXECUTE format(
      'ALTER TABLE %1$I ADD COLUMN soft_delete BOOLEAN NOT NULL DEFAULT FALSE',
      bang);
  END LOOP;
END
$$;

-- ------------------------------------------------------- 12. Index
--
-- Phải nằm sau mục 11: phần lớn là index BỘ PHẬN theo soft_delete.
--
-- Vì sao bộ phận: một dòng đã xoá mềm vẫn nằm trong bảng nên vẫn chiếm chỗ
-- trong khoá duy nhất. Không có WHERE NOT soft_delete thì slug của một tour đã
-- xoá chiếm chỗ vĩnh viễn, và biên tập viên tạo lại tour cùng slug sẽ bị từ chối
-- mà thông báo lỗi không nói gì về nguyên nhân thật — docs/12 mục 2.2.

-- Đúng MỘT ngôn ngữ nguồn. ADR-004.
CREATE UNIQUE INDEX ux_locale_single_source
  ON locale ((TRUE)) WHERE is_source;

CREATE UNIQUE INDEX ux_pax_type
  ON pax_type (market, code) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_departure_origin
  ON departure_origin (market, city) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_departure_origin_default
  ON departure_origin (market) WHERE is_default AND NOT soft_delete;

CREATE UNIQUE INDEX ux_staff_user_email
  ON staff_user (email) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_region_code
  ON region (code) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_region_translation_slug
  ON region_translation (locale, slug) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_destination_code
  ON destination (code) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_destination_translation_slug
  ON destination_translation (locale, slug) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_product_translation_slug
  ON product_translation (locale, slug) WHERE NOT soft_delete;

-- Một ngày một dòng; CRUISE bốn dòng một ngày, phân biệt bằng hạng cabin.
CREATE UNIQUE INDEX ux_departure_slot
  ON departure (product_id, market, depart_date, COALESCE(cabin_category, ''))
  WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_quote_reference
  ON quote (reference) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_booking_reference
  ON booking (reference) WHERE NOT soft_delete;

-- Index tra cứu. Cũng bộ phận theo soft_delete để chỉ chứa những dòng thật sự
-- được truy vấn.

-- Website khách không bao giờ đọc dòng chưa xuất bản.
CREATE INDEX ix_product_market_published
  ON product_market (market, price_from)
  WHERE is_published;

CREATE INDEX ix_departure_finder
  ON departure (market, depart_date)
  INCLUDE (product_id)
  WHERE base_status <> 'SOLD_OUT' AND NOT soft_delete;

-- Job quét hạn chỉ đọc những dòng còn sống.
CREATE INDEX ix_seat_hold_active
  ON seat_hold (expires_at)
  WHERE released_at IS NULL AND NOT soft_delete;

CREATE INDEX ix_seat_hold_departure
  ON seat_hold (departure_id)
  WHERE released_at IS NULL AND NOT soft_delete;

CREATE INDEX ix_booking_event_booking
  ON booking_event (booking_id, created_at);

-- Sắp theo collation của từng locale. Câu truy vấn PHẢI ghi cùng collation,
-- nếu không Postgres bỏ qua index và sắp lại toàn bộ.
CREATE INDEX ix_pt_title_da ON product_translation (title COLLATE "da-DK-x-icu")
  WHERE locale = 'da' AND NOT soft_delete;
CREATE INDEX ix_pt_title_vi ON product_translation (title COLLATE "vi-VN-x-icu")
  WHERE locale = 'vi' AND NOT soft_delete;

-- Tìm kiếm không dấu: "hoi an" phải ra "Hội An".
CREATE INDEX ix_pt_search
  ON product_translation USING gin (f_unaccent(title) gin_trgm_ops);

-- Listing lọc theo locale + trạng thái là truy vấn nóng nhất của cả hệ thống.
CREATE INDEX ix_pt_listing
  ON product_translation (locale, status)
  INCLUDE (product_id, slug, title)
  WHERE status = 'PUBLISHED' AND NOT soft_delete;

CREATE INDEX ix_destination_region ON destination (region_id);
CREATE INDEX ix_product_destination ON product (primary_destination_id);

-- ---------------------------------------------------------- 13. Khung nhìn

-- OUTDATED là giá trị tính ra, không lưu.
CREATE VIEW v_product_translation_state AS
SELECT t.product_id, t.locale, t.status,
       (src.last_modified_at > COALESCE(t.translated_at, 'epoch'::timestamptz))
         AS is_outdated
FROM product_translation t
JOIN locale l ON l.code = t.locale AND NOT l.is_source
JOIN product_translation src ON src.product_id = t.product_id
JOIN locale sl ON sl.code = src.locale AND sl.is_source
WHERE NOT t.soft_delete AND NOT src.soft_delete;

-- Chỗ khả dụng tính bằng khung nhìn, KHÔNG bằng cột. Trừ cả chỗ đang giữ.
CREATE VIEW v_departure_availability AS
SELECT d.id AS departure_id,
       d.capacity - d.seats_booked - COALESCE(h.held, 0) AS seats_available
FROM departure d
LEFT JOIN LATERAL (
  SELECT SUM(seats) AS held
  FROM seat_hold
  WHERE departure_id = d.id
    AND released_at IS NULL
    AND NOT soft_delete
    AND expires_at > now()
) h ON TRUE
WHERE NOT d.soft_delete;

-- ------------------------------------------------- 14. Trigger nghiệp vụ

-- Bắt buộc có bản dịch ngôn ngữ nguồn — không diễn đạt được bằng khoá ngoại.
-- Hoãn tới cuối transaction để tạo product và product_translation trong cùng
-- một transaction vẫn chạy được.
-- COALESCE(NEW.id, OLD.product_id) KHÔNG chạy được: hai bảng có cấu trúc khác
-- nhau, và plpgsql báo 'record "old" has no field "product_id"' ngay ở INSERT
-- đầu tiên. Phải rẽ nhánh theo TG_TABLE_NAME và TG_OP — nhánh không đi vào thì
-- plpgsql không đụng tới, nên tham chiếu NEW/OLD sai chỗ mới không nổ.
CREATE FUNCTION trg_require_source_translation() RETURNS trigger
LANGUAGE plpgsql AS
$$
DECLARE
  ma_san_pham UUID;
BEGIN
  IF TG_TABLE_NAME = 'product' THEN
    ma_san_pham := NEW.id;
  ELSIF TG_OP = 'DELETE' THEN
    ma_san_pham := OLD.product_id;
  ELSE
    ma_san_pham := NEW.product_id;
  END IF;

  -- Sản phẩm đã xoá mềm thì thôi: xoá mềm cả chùm sản phẩm là thao tác hợp lệ,
  -- và bản dịch nguồn của nó bị xoá mềm theo.
  IF NOT EXISTS (
    SELECT 1 FROM product p WHERE p.id = ma_san_pham AND NOT p.soft_delete
  ) THEN
    RETURN NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM product_translation pt
    JOIN locale l ON l.code = pt.locale AND l.is_source
    WHERE pt.product_id = ma_san_pham
      AND NOT pt.soft_delete
  ) THEN
    RAISE EXCEPTION 'product % thiếu bản dịch ngôn ngữ nguồn', ma_san_pham;
  END IF;

  RETURN NULL;
END
$$;

CREATE CONSTRAINT TRIGGER ct_product_source_translation
  AFTER INSERT ON product
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION trg_require_source_translation();

-- Chiều ngược lại: xoá hoặc xoá mềm bản dịch nguồn của một sản phẩm còn sống.
CREATE CONSTRAINT TRIGGER ct_product_translation_source_kept
  AFTER DELETE OR UPDATE ON product_translation
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION trg_require_source_translation();
