# Lược đồ cơ sở dữ liệu

```
Trạng thái: Nháp
Cập nhật: 01/09/2026
Nguồn sự thật về: DDL, ràng buộc, index, quy ước migration, dữ liệu mồi,
                  bộ kiểm tính nhất quán dữ liệu.
Không nói về: vì sao mô hình như vậy (11), công thức tính (14), API (13).
```

PostgreSQL 16+. Migration bằng **Flyway**, SQL thuần.

---

## 1. Chuẩn bị cơ sở dữ liệu

```sql
-- V1__extensions.sql
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

Collation ICU dùng trực tiếp trong truy vấn và index: `"da-DK-x-icu"`,
`"vi-VN-x-icu"`. Không đặt collation mặc định cho cả cơ sở dữ liệu — mỗi locale
một collation, đặt mặc định là chọn sai cho một trong hai.

### 1.1. `unaccent` không index được nếu gọi thẳng

`unaccent()` được đánh dấu `STABLE`, không phải `IMMUTABLE`, nên
`CREATE INDEX ... (unaccent(title))` **bị từ chối**. Phải bọc lại:

```sql
CREATE FUNCTION f_unaccent(text) RETURNS text
  LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS
$$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;
```

Đây là cạm bẫy tốn nửa ngày nếu không biết trước. Mọi index tìm kiếm không dấu
dùng `f_unaccent`, không dùng `unaccent`.

---

## 2. Quy ước

| Mục | Quy ước |
|---|---|
| Tên bảng | `snake_case`, **số ít**: `product`, `booking` |
| Bảng nối | `<a>_<b>`: `product_theme`, `staff_user_role` |
| Bảng dịch | `<entity>_translation` |
| Khoá chính | `id UUID`, sinh ở tầng ứng dụng (UUID v7) |
| Khoá ngoại | `<entity>_id` |
| Index | `ix_<bảng>_<cột>`; duy nhất: `ux_`; ràng buộc: `ck_` |
| Tiền | `NUMERIC(12,2)` + `currency VARCHAR(3)`, luôn đi cặp |
| Thời điểm | `TIMESTAMPTZ`, lưu UTC |
| Ngày khởi hành | `DATE` |
| Enum đóng | `VARCHAR(n)` + `CHECK` — không dùng kiểu `ENUM` của Postgres |
| Enum cấu hình được | Bảng tra: `market`, `locale`, `pax_type` |
| Cột kiểm toán | `created_at` · `created_by` · `last_modified_at` · `last_modified_by` — mục 2.1 |
| Xoá mềm | `soft_delete BOOLEAN NOT NULL DEFAULT FALSE` — mục 2.1 và 2.2 |

**Vì sao không dùng kiểu `ENUM` của Postgres:** thêm một giá trị phải
`ALTER TYPE`, không chạy trong transaction cùng các thay đổi khác ở phiên bản
Postgres cũ, và không xoá giá trị được. `VARCHAR` + `CHECK` sửa bằng một câu
`ALTER TABLE ... DROP CONSTRAINT / ADD CONSTRAINT` bình thường.

### 2.1. Năm cột kiểm toán và xoá mềm

```sql
created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
created_by        UUID        REFERENCES staff_user (id),
last_modified_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
last_modified_by  UUID        REFERENCES staff_user (id),
soft_delete       BOOLEAN     NOT NULL DEFAULT FALSE
```

Bảng nào nhận cột nào, và **vì sao ba nhóm bảng cố tình không nhận**: `11` mục
11.2. Tóm tắt: bảng thực thể nhận đủ năm cột; `market` và `locale` nhận bốn cột
đầu nhưng không có `soft_delete` vì đã có `is_active`; bảng con 1-1, bảng dòng
chi tiết và `booking_event` không nhận cột nào.

`last_modified_at` do **trigger CSDL** đặt, không phải ứng dụng:

```sql
CREATE FUNCTION trg_set_last_modified() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  NEW.last_modified_at := now();
  RETURN NEW;
END $$;
```

Gắn `BEFORE UPDATE ... FOR EACH ROW` cho mọi bảng có cột đó. Danh sách bảng nằm
trong một khối `DO` duy nhất ở cuối `V1` — thêm bảng mới thì thêm tên vào đúng
một chỗ, và bảng nào quên sẽ lộ ra ngay ở quy tắc kiểm 17 (mục 9).

`last_modified_by` thì ứng dụng ghi, vì CSDL không biết ai đang thao tác.

> **Các khối DDL bên dưới lược bỏ năm cột này cho dễ đọc.** DDL đầy đủ là
> `api/infrastructure/src/main/resources/db/migration/V1__khoi_tao.sql`; ở đó
> năm cột được thêm bằng một vòng lặp `ALTER TABLE` chứ không chép tay 17 lần.

### 2.2. Xoá mềm phá khoá duy nhất — đọc trước khi thêm bảng

Một dòng đã `soft_delete = TRUE` vẫn nằm trong bảng, nên nó vẫn **chiếm chỗ**
trong mọi khoá duy nhất. Hệ quả cụ thể: biên tập viên xoá một tour rồi tạo lại
tour khác với cùng slug sẽ bị từ chối, và thông báo lỗi không nói gì về nguyên
nhân thật.

Vì vậy mọi ràng buộc duy nhất trên bảng có `soft_delete` phải là **index bộ
phận**, không phải `CONSTRAINT ... UNIQUE`:

```sql
-- SAI: dòng đã xoá vẫn giữ slug vĩnh viễn
CONSTRAINT ux_product_translation_slug UNIQUE (locale, slug)

-- ĐÚNG
CREATE UNIQUE INDEX ux_product_translation_slug
  ON product_translation (locale, slug)
  WHERE NOT soft_delete;
```

`CONSTRAINT ... UNIQUE` **không** viết được dạng bộ phận — bắt buộc phải chuyển
sang `CREATE UNIQUE INDEX`.

**Một ngoại lệ, và nó không thương lượng được:**

```sql
CONSTRAINT ux_product_id_type UNIQUE (id, product_type)   -- giữ nguyên, KHÔNG bộ phận
```

Khoá ngoại kép của sáu bảng con trỏ tới đúng ràng buộc này, mà **khoá ngoại
không tham chiếu được index bộ phận**. Chuyển nó sang dạng `WHERE NOT soft_delete`
sẽ làm toàn bộ migration đỏ ở bước tạo bảng con. Ghi ra đây vì trông nó giống
hệt các khoá duy nhất khác và sẽ có người "sửa cho đồng bộ".

Hệ quả thứ hai, âm thầm hơn: **mọi truy vấn đọc phải tự lọc `AND NOT
soft_delete`.** Xoá mềm có đúng tính chất mà ADR-003 đã bác bỏ khi từ chối
Hibernate `@Filter` — nó hoạt động ngầm, và quên một chỗ là rò dữ liệu đã xoá ra
khách mà không có lỗi nào nổ. Index listing vì thế cũng kèm `AND NOT soft_delete`,
để chúng chỉ chứa những dòng thật sự được truy vấn.

---

## 3. Cấu hình

```sql
CREATE TABLE market (
  code              VARCHAR(2)   PRIMARY KEY,          -- 'DK', 'VN'
  currency          VARCHAR(3)   NOT NULL,
  fraction_digits   SMALLINT     NOT NULL,             -- DKK 2, VND 0
  default_locale    VARCHAR(8)   NOT NULL,
  deposit_rate      NUMERIC(5,4) NOT NULL,             -- DK 0.2500
  processing_fee    NUMERIC(12,2) NOT NULL,
  is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT ck_market_code   CHECK (code = UPPER(code)),
  CONSTRAINT ck_market_digits CHECK (fraction_digits BETWEEN 0 AND 4),
  CONSTRAINT ck_market_rate   CHECK (deposit_rate BETWEEN 0 AND 1)
);

CREATE TABLE locale (
  code       VARCHAR(8)  PRIMARY KEY,                  -- 'da', 'vi'
  -- KHÔNG đặt tên cột là `collation`: đó là từ khoá dành riêng của Postgres,
  -- CREATE TABLE đỏ ngay với "syntax error at or near collation".
  collation_name TEXT     NOT NULL,                    -- 'da-DK-x-icu'
  is_source  BOOLEAN     NOT NULL DEFAULT FALSE,
  is_active  BOOLEAN     NOT NULL DEFAULT TRUE
);

-- Đúng MỘT ngôn ngữ nguồn. ADR-004.
CREATE UNIQUE INDEX ux_locale_single_source
  ON locale ((TRUE)) WHERE is_source;

ALTER TABLE market
  ADD CONSTRAINT fk_market_locale
  FOREIGN KEY (default_locale) REFERENCES locale (code);

CREATE TABLE pax_type (
  id            UUID         PRIMARY KEY,
  market        VARCHAR(2)   NOT NULL REFERENCES market (code),
  code          VARCHAR(16)  NOT NULL,                 -- 'ADULT', 'CHILD_5_11'
  min_age       SMALLINT,
  max_age       SMALLINT,
  discount_rate NUMERIC(5,4) NOT NULL DEFAULT 0,       -- 0.25 = giảm 25%
  sort_order    SMALLINT     NOT NULL,
  CONSTRAINT ck_pax_type_age  CHECK (min_age IS NULL OR max_age IS NULL
                                     OR min_age <= max_age)
);

-- Index bộ phận, không phải CONSTRAINT UNIQUE — mục 2.2.
CREATE UNIQUE INDEX ux_pax_type ON pax_type (market, code)
  WHERE NOT soft_delete;

CREATE TABLE departure_origin (
  id         UUID          PRIMARY KEY,
  market     VARCHAR(2)    NOT NULL REFERENCES market (code),
  city       VARCHAR(64)   NOT NULL,
  iata_code  VARCHAR(3),
  surcharge  NUMERIC(12,2) NOT NULL DEFAULT 0,         -- dùng ở DK
  is_default BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX ux_departure_origin
  ON departure_origin (market, city) WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_departure_origin_default
  ON departure_origin (market) WHERE is_default AND NOT soft_delete;
```

`departure_origin` phục vụ **cả hai vai trò** của `04` mục 6: cột `surcharge`
dùng ở `DK`, và bảng được đánh index để lọc ở `VN`.

### 3.1. Vai trò nhân viên

Bốn vai trò của `13` mục 10, ma trận quyền ở `22` mục 2.1. Bảng `staff_user` đã
có từ `V1`.

```sql
CREATE TABLE role (
  code       VARCHAR(16) PRIMARY KEY,      -- 'CONSULTANT','EDITOR','TRANSLATOR','ADMIN'
  sort_order SMALLINT    NOT NULL,
  CONSTRAINT ck_role_code CHECK (code = UPPER(code))
);

-- Bảng nối, nhưng có khoá chính THAY THẾ chứ không phải khoá kép
-- (staff_user_id, role_code). Lý do ở ngay dưới.
CREATE TABLE staff_user_role (
  id            UUID        PRIMARY KEY,
  staff_user_id UUID        NOT NULL REFERENCES staff_user (id) ON DELETE CASCADE,
  role_code     VARCHAR(16) NOT NULL REFERENCES role (code)
);

CREATE UNIQUE INDEX ux_staff_user_role
  ON staff_user_role (staff_user_id, role_code)
  WHERE NOT soft_delete;
```

`role` thuộc **nhóm B** (`11` mục 11.2): bốn cột kiểm toán, không có
`soft_delete`. Danh sách vai trò do migration quản, không ai thêm bớt từ giao
diện — `13` mục 10 và `22` mục 2.1 đều liệt kê đúng bốn giá trị này, nên thêm giá
trị thứ năm là việc phải đi qua tài liệu chứ không qua một dòng `INSERT`.

`staff_user_role` **cố ý lệch khỏi nhóm C** dù nó là bảng nối: nó nhận đủ năm
cột. Cấp và thu quyền là **sự kiện an ninh** — câu hỏi "ai cho người này quyền
`ADMIN`, lúc nào" phải trả lời được, và xoá cứng một dòng là xoá mất bằng chứng.
Thu quyền là `soft_delete = TRUE`, và khi đó `last_modified_by` chính là người
đã thu.

Đây cũng là ví dụ sạch nhất cho mục 2.2: nếu khoá chính là cặp
`(staff_user_id, role_code)` thì **cấp lại một vai trò đã thu là không thể** —
dòng cũ vẫn chiếm khoá. Khoá thay thế cộng index bộ phận giải đúng chuyện đó.

---

## 4. Sản phẩm và nội dung

### 4.1. Bảng gốc

```sql
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
  -- + năm cột kiểm toán, mục 2.1

  CONSTRAINT ck_product_type CHECK (product_type IN
    ('GROUP_TOUR','INDIVIDUAL_PACKAGE','PRIVATE_TOUR',
     'CRUISE','COMBO','DAY_TOUR')),
  CONSTRAINT ck_product_duration CHECK (
    (product_type = 'DAY_TOUR' AND duration_days IS NULL) OR
    (product_type <> 'DAY_TOUR' AND duration_days BETWEEN 1 AND 60)),
  CONSTRAINT ck_product_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),

  -- Cần cho khoá ngoại kép của bảng con. KHÔNG chuyển thành index bộ phận:
  -- khoá ngoại không tham chiếu được index bộ phận — mục 2.2.
  CONSTRAINT ux_product_id_type UNIQUE (id, product_type)
);
```

### 4.2. Bảng con — cưỡng chế đúng loại

Khoá ngoại kép làm cho **không thể** gắn một dòng `product_group_tour` vào một
sản phẩm loại `CRUISE`:

```sql
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
```

`min_pax BETWEEN 10 AND 25` viết được **sạch, không kèm `OR IS NULL`** — đây
chính là lợi ích của bảng con mà bảng rộng không có (`11` mục 3).

Các bảng con còn lại theo đúng khuôn mẫu:

| Bảng | Ràng buộc đáng chú ý |
|---|---|
| `product_individual` | `min_party_size >= 1`; `flexible_date_window_days BETWEEN 0 AND 30` |
| `product_private` | `lead_time_days BETWEEN 1 AND 90`; `quote_valid_days BETWEEN 1 AND 30` |
| `product_cruise` | `port_count >= 1` |
| `product_combo` | `nights BETWEEN 1 AND 14`; `valid_to >= valid_from` |
| `product_day_tour` | `duration_hours BETWEEN 1 AND 24`; `cutoff_hours >= 0` |

> **Ràng buộc "phải có đúng một dòng bảng con" không diễn đạt được bằng
> `CHECK`.** Khoá ngoại kép chặn được dòng *sai loại*, nhưng không chặn được
> *thiếu dòng*. Xử lý bằng constraint trigger hoãn tới cuối transaction, cộng một
> quy tắc trong bộ kiểm dữ liệu (mục 9). Ghi rõ ở đây để không ai tưởng lược đồ
> đã kín.

### 4.3. Bản dịch

```sql
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
  translated_at     TIMESTAMPTZ,
  translated_by     UUID         REFERENCES staff_user (id),
  -- + năm cột kiểm toán, mục 2.1

  PRIMARY KEY (product_id, locale),
  CONSTRAINT ck_pt_status CHECK (status IN ('DRAFT','TRANSLATED','PUBLISHED')),
  CONSTRAINT ck_pt_slug   CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  CONSTRAINT ck_pt_why    CHECK (array_length(why_choose_this, 1) BETWEEN 3 AND 7),
  CONSTRAINT ck_pt_long   CHECK (array_length(long_description, 1) >= 2)
);

CREATE UNIQUE INDEX ux_product_translation_slug
  ON product_translation (locale, slug)
  WHERE NOT soft_delete;
```

`ck_pt_slug` cưỡng chế quy tắc **slug không dấu** của `02` mục 5.1 ngay ở tầng
CSDL: biểu thức chỉ nhận `a-z`, `0-9` và dấu nối. `bekræftelse` và
`việt-nam` đều bị từ chối.

**`OUTDATED` không có trong `ck_pt_status`** — nó là giá trị tính ra
(`11` mục 8), lộ ra qua khung nhìn:

```sql
CREATE VIEW v_product_translation_state AS
SELECT t.product_id, t.locale, t.status,
       (src.last_modified_at > COALESCE(t.translated_at, 'epoch'::timestamptz))
         AS is_outdated
FROM product_translation t
JOIN locale l ON l.code = t.locale AND NOT l.is_source
JOIN product_translation src ON src.product_id = t.product_id
JOIN locale sl ON sl.code = src.locale AND sl.is_source
WHERE NOT t.soft_delete AND NOT src.soft_delete;
```

### 4.4. Bắt buộc có bản dịch ngôn ngữ nguồn

Không diễn đạt được bằng khoá ngoại. Dùng constraint trigger hoãn:

```sql
CREATE FUNCTION trg_require_source_translation() RETURNS trigger
LANGUAGE plpgsql AS $$
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

  -- Sản phẩm đã xoá mềm thì thôi — xoá mềm cả chùm là thao tác hợp lệ.
  IF NOT EXISTS (
    SELECT 1 FROM product p WHERE p.id = ma_san_pham AND NOT p.soft_delete
  ) THEN
    RETURN NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM product_translation pt
    JOIN locale l ON l.code = pt.locale AND l.is_source
    WHERE pt.product_id = ma_san_pham AND NOT pt.soft_delete
  ) THEN
    RAISE EXCEPTION 'product % thiếu bản dịch ngôn ngữ nguồn', ma_san_pham;
  END IF;

  RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER ct_product_source_translation
  AFTER INSERT ON product
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION trg_require_source_translation();

-- Chiều ngược lại: xoá hoặc xoá mềm bản dịch nguồn của sản phẩm còn sống.
CREATE CONSTRAINT TRIGGER ct_product_translation_source_kept
  AFTER DELETE OR UPDATE ON product_translation
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION trg_require_source_translation();
```

Hoãn tới cuối transaction để tạo `product` và `product_translation` trong cùng
một transaction vẫn chạy được.

> **Một hàm dùng cho hai bảng thì phải rẽ nhánh theo `TG_TABLE_NAME`.** Bản đầu
> của tài liệu này viết `COALESCE(NEW.id, OLD.product_id)` cho gọn; Postgres từ
> chối ngay ở `INSERT` đầu tiên với `record "old" has no field "product_id"` vì
> `OLD` của trigger trên `product` không có cấu trúc của `product_translation`.
> Nhánh nào không đi vào thì plpgsql không đụng tới, nên rẽ nhánh là cách duy
> nhất viết chung một hàm cho hai bảng.

### 4.5. Cổng chặn thị trường

```sql
CREATE TABLE product_market (
  product_id   UUID          NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  market       VARCHAR(2)    NOT NULL REFERENCES market (code),
  is_published BOOLEAN       NOT NULL DEFAULT FALSE,
  price_from   NUMERIC(12,2),                          -- vật chất hoá, trigger
  published_at TIMESTAMPTZ,
  PRIMARY KEY (product_id, market)
);

CREATE INDEX ix_product_market_published
  ON product_market (market, price_from)
  WHERE is_published;
```

Index bộ phận `WHERE is_published` — website khách không bao giờ đọc dòng chưa
xuất bản, nên index chỉ chứa những dòng thật sự được truy vấn.

### 4.6. Ảnh và giấy phép

`05` yêu cầu bộ ảnh ở trang chi tiết, và `24` mục 8 đòi biết **nguồn · giấy phép ·
phạm vi · hạn dùng** của từng ảnh. Hai cột chữ `hero_image` và `map_image` trên
`product` không mang được thông tin đó.

```sql
CREATE TABLE media_asset (
  id             UUID        PRIMARY KEY,
  path           TEXT        NOT NULL,     -- đường dẫn tương đối; nơi lưu chốt ở ADR-008
  width          INTEGER     NOT NULL,
  height         INTEGER     NOT NULL,
  byte_size      INTEGER     NOT NULL,
  source         VARCHAR(16) NOT NULL,     -- SELF, PARTNER, PURCHASED, CUSTOMER
  licence_ref    VARCHAR(200),             -- số hoá đơn, mã giấy phép, tên file thư đồng ý
  licence_scope  VARCHAR(200),             -- 'web', 'web + in ấn', ...
  licence_until  DATE,                     -- NULL = không hạn
  person_consent BOOLEAN     NOT NULL DEFAULT FALSE,

  CONSTRAINT ck_media_source CHECK (source IN ('SELF','PARTNER','PURCHASED','CUSTOMER')),
  CONSTRAINT ck_media_size   CHECK (width > 0 AND height > 0 AND byte_size > 0),
  -- Ảnh không phải tự chụp thì BẮT BUỘC có chứng từ — 24 mục 8. Ràng buộc ở
  -- tầng CSDL chứ không ở tầng giao diện: ảnh tạm là thứ ở lại lâu nhất.
  CONSTRAINT ck_media_licence CHECK (source = 'SELF' OR licence_ref IS NOT NULL)
);

CREATE UNIQUE INDEX ux_media_asset_path
  ON media_asset (path) WHERE NOT soft_delete;

-- Ảnh dùng chung cho mọi ngôn ngữ, nhưng alt thì KHÔNG — 11 mục 6.
CREATE TABLE media_asset_translation (
  asset_id UUID         NOT NULL REFERENCES media_asset (id) ON DELETE CASCADE,
  locale   VARCHAR(8)   NOT NULL REFERENCES locale (code),
  alt      VARCHAR(300) NOT NULL,
  PRIMARY KEY (asset_id, locale)
);

-- Bộ ảnh của một sản phẩm. Nhóm C: vòng đời theo product.
CREATE TABLE product_image (
  product_id UUID     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  asset_id   UUID     NOT NULL REFERENCES media_asset (id),
  sort_order SMALLINT NOT NULL,
  PRIMARY KEY (product_id, asset_id)
);

CREATE INDEX ix_product_image ON product_image (product_id, sort_order);
```

Ba điều cố ý:

1. **`media_asset` là thực thể riêng, không phải cột trên `product`.** Cùng một
   ảnh khách sạn dùng cho ba tour; chép giấy phép ba lần là ba chỗ để lệch.
2. **`product_image.asset_id` không có `ON DELETE CASCADE`.** Gỡ ảnh khỏi bộ ảnh
   là xoá dòng nối; bản thân ảnh và chứng từ giấy phép của nó **ở lại**. Xoá cứng
   một `media_asset` còn được dùng sẽ bị khoá ngoại chặn — đúng như mong muốn.
3. **`alt` là `NOT NULL`.** Ảnh không có chữ thay thế là ảnh không dùng được cho
   người khiếm thị, và `24` mục 6 coi alt là nội dung phải dịch.

`product.hero_image` và `map_image` **vẫn là cột chữ** ở phiên bản này. Chuyển
chúng sang tham chiếu `media_asset` là đổi phá vỡ tương thích, nên phải tách làm
hai lần triển khai theo đúng quy tắc ở mục 8 — ghi vào mục 10.

### 4.7. Slug cũ và chuyển hướng 301

`24` mục 4: slug đã xuất bản thì không đổi, và nếu buộc phải đổi thì URL cũ phải
chuyển hướng 301 chứ không trả 404. Muốn vậy phải nhớ slug cũ.

```sql
-- Nhóm D — nhật ký chỉ ghi thêm, như booking_event.
CREATE TABLE slug_history (
  id          UUID         PRIMARY KEY,
  entity_type VARCHAR(16)  NOT NULL,      -- PRODUCT, DESTINATION, REGION, POST
  entity_id   UUID         NOT NULL,
  locale      VARCHAR(8)   NOT NULL REFERENCES locale (code),
  old_slug    VARCHAR(160) NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID         REFERENCES staff_user (id),

  CONSTRAINT ck_slug_history_type CHECK (entity_type IN
    ('PRODUCT','DESTINATION','REGION','POST')),
  CONSTRAINT ck_slug_history_slug CHECK (old_slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE UNIQUE INDEX ux_slug_history
  ON slug_history (entity_type, locale, old_slug);
```

**Bảng này cố ý không có khoá ngoại tới thực thể.** Nó phục vụ bốn loại thực thể,
và bốn bảng lịch sử gần giống nhau là bốn chỗ để quên một chỗ. Cái giá phải trả:
`entity_id` có thể trỏ tới bản ghi đã biến mất — vô hại, vì khi đó chuyển hướng
dẫn tới một trang trả 404, đúng bằng thứ sẽ xảy ra nếu không có dòng đó.

Ghi bằng **trigger**, không bằng code ứng dụng: đổi slug xảy ra ở nhiều màn hình,
và chỗ nào quên gọi thì lỗi không hiện ra cho tới khi có người báo link chết.

```sql
CREATE FUNCTION trg_luu_slug_cu() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  loai   text := TG_ARGV[0];
  cot_id text := TG_ARGV[1];
  ma     uuid;
BEGIN
  IF NEW.slug IS NOT DISTINCT FROM OLD.slug THEN
    RETURN NULL;
  END IF;

  ma := (to_jsonb(NEW) ->> cot_id)::uuid;

  -- Slug mới có thể chính là một slug cũ đang được chuyển hướng (A → B → A).
  -- Không dọn dòng đó thì sinh ra vòng lặp chuyển hướng.
  DELETE FROM slug_history
   WHERE entity_type = loai AND locale = NEW.locale AND old_slug = NEW.slug;

  INSERT INTO slug_history (id, entity_type, entity_id, locale, old_slug, created_by)
  VALUES (gen_random_uuid(), loai, ma, NEW.locale, OLD.slug, NEW.last_modified_by)
  ON CONFLICT (entity_type, locale, old_slug)
  DO UPDATE SET entity_id = EXCLUDED.entity_id, created_at = now();

  RETURN NULL;
END $$;
```

Gắn `AFTER UPDATE OF slug ... FOR EACH ROW` cho từng bảng dịch, truyền loại thực
thể và tên cột khoá ngoại làm tham số. `gen_random_uuid()` dùng ở đây thay cho
UUID v7 sinh ở tầng ứng dụng vì không có tầng ứng dụng nào tham gia — đây là dòng
do CSDL tự sinh.

### 4.8. Chủ đề

`13` mục 6 cho phép lọc `?theme=` lặp lại. Chủ đề là **phân loại ngang**, cắt qua
miền và loại sản phẩm: một tour trekking miền Bắc và một tour trekking miền Trung
cùng chủ đề nhưng khác mọi thứ khác.

```sql
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

CREATE TABLE product_theme (                -- nhóm C
  product_id UUID NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  theme_id   UUID NOT NULL REFERENCES theme (id),
  PRIMARY KEY (product_id, theme_id)
);
```

### 4.9. Khách sạn và tham quan

```sql
-- name KHÔNG nằm trong bảng dịch: tên riêng của khách sạn không dịch
-- (24 mục 5). Chỉ phần mô tả mới là nội dung phải dịch.
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

CREATE TABLE product_hotel_stay (           -- nhóm C
  product_id UUID     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  hotel_id   UUID     NOT NULL REFERENCES hotel (id),
  nights     SMALLINT NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id, hotel_id),
  CONSTRAINT ck_phs_nights CHECK (nights >= 1)
);
```

**Tham quan thuộc về điểm đến, không thuộc về sản phẩm.** Cùng một chuyến thăm
Văn Miếu xuất hiện trong nhiều tour; gắn nó vào sản phẩm là chép mô tả ra nhiều
bản rồi để chúng lệch nhau. Quy tắc kiểm 12 ở mục 9 đếm theo điểm đến vì vậy.

**Tên khách sạn không dịch, mô tả thì có.** Đây là chỗ ranh giới "cái gì là nội
dung" đi qua giữa hai cột của cùng một thực thể — `24` mục 5 liệt kê tên riêng
của khách sạn, tàu và hãng bay vào nhóm không được đổi khi dịch.

### 4.10. Lịch trình từng ngày

```sql
CREATE TABLE itinerary_day (
  id             UUID     PRIMARY KEY,
  product_id     UUID     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  day_number     SMALLINT NOT NULL,
  destination_id UUID     REFERENCES destination (id),   -- NULL với ngày bay
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

CREATE UNIQUE INDEX ux_itinerary_day
  ON itinerary_day (product_id, day_number) WHERE NOT soft_delete;
```

`itinerary_day` **là thực thể, không phải bảng dòng chi tiết** — nó lệch khỏi
nhóm C dù trông giống. Biên tập viên sửa từng ngày một, xoá một ngày rồi thêm
lại ngày khác cùng số thứ tự, và mỗi ngày có nội dung phải dịch riêng. Vì thế nó
nhận đủ năm cột, và khoá duy nhất phải là index bộ phận.

**Độ dài mô tả không cưỡng chế bằng `CHECK`.** Quy tắc kiểm 11 ở mục 9 đòi mô tả
mỗi ngày ≥ 80 ký tự, nhưng đó là kiểm **chất lượng nội dung**, không phải ràng
buộc toàn vẹn: biến nó thành `CHECK` là chặn biên tập viên lưu bản nháp giữa
chừng, và họ sẽ đối phó bằng cách gõ 80 ký tự rác.

### 4.11. Bài viết, thẻ, buổi thuyết trình

```sql
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

CREATE TABLE post (
  id           UUID PRIMARY KEY,
  hero_image   TEXT,
  published_at TIMESTAMPTZ,
  author_id    UUID REFERENCES staff_user (id)
);

CREATE TABLE post_translation (             -- cùng khuôn product_translation
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

CREATE TABLE post_tag (                     -- nhóm C
  post_id UUID NOT NULL REFERENCES post (id) ON DELETE CASCADE,
  tag_id  UUID NOT NULL REFERENCES tag (id),
  PRIMARY KEY (post_id, tag_id)
);

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
```

Ba điều cố ý:

1. **Bài viết không có cổng chặn thị trường ở v1.** `market` trong đường dẫn
   `/{market}/posts` chỉ là quy ước chung của API công khai. Cần bài riêng cho
   từng thị trường thì thêm `post_market` đúng khuôn `product_market` — đừng nhét
   một cột `market` vào `post`, vì một bài có thể chạy ở cả hai.
2. **Buổi thuyết trình thì ngược lại: thuộc về đúng một thị trường.** Buổi ở
   Odense phục vụ khách Đan, buổi ở Hà Nội phục vụ khách Việt — hai sự kiện khác
   nhau, cùng lý do với `departure` ở ADR-006.
3. **Cột tên là `event_date`, không phải `date`.** `date` là tên kiểu dữ liệu của
   Postgres; đặt làm tên cột thì mọi câu truy vấn phải trích dẫn nó. Cùng loại bẫy
   với cột `collation` ở mục 3. Quy tắc kiểm 14 ở mục 9 vì thế đọc `lecture.event_date`.

**Số chỗ còn lại của buổi thuyết trình là giá trị tính ra** — `seats − seats_taken`
— không lưu thành cột thứ ba (`api/CLAUDE.md` mục 7).

---

## 5. Ngày khởi hành, giá, giữ chỗ

```sql
CREATE TABLE departure (
  id                UUID        PRIMARY KEY,
  product_id        UUID        NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  market            VARCHAR(2)  NOT NULL REFERENCES market (code),
  depart_date       DATE        NOT NULL,
  return_date       DATE        NOT NULL,
  days              SMALLINT    NOT NULL,
  cabin_category    VARCHAR(16),                       -- chỉ CRUISE
  base_status       VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  capacity          SMALLINT    NOT NULL,
  seats_booked      SMALLINT    NOT NULL DEFAULT 0,
  departure_origin_id UUID      REFERENCES departure_origin (id),

  CONSTRAINT ck_dep_status CHECK (base_status IN
    ('OPEN','FEW_SEATS','SOLD_OUT','PENDING')),        -- KHÔNG có GUARANTEED
  CONSTRAINT ck_dep_dates  CHECK (return_date = depart_date + (days - 1)),
  CONSTRAINT ck_dep_cabin  CHECK (cabin_category IS NULL OR cabin_category IN
    ('INSIDE','OUTSIDE','BALCONY','AQUA')),
  CONSTRAINT ck_dep_seats  CHECK (seats_booked BETWEEN 0 AND capacity)
);

-- Một ngày một dòng; CRUISE bốn dòng một ngày, phân biệt bằng hạng cabin.
CREATE UNIQUE INDEX ux_departure_slot
  ON departure (product_id, market, depart_date,
                COALESCE(cabin_category, ''))
  WHERE NOT soft_delete;

CREATE INDEX ix_departure_finder
  ON departure (market, depart_date)
  INCLUDE (product_id)
  WHERE base_status <> 'SOLD_OUT' AND NOT soft_delete;
```

`ck_dep_status` **không chứa `GUARANTEED`** — cưỡng chế ở tầng kiểu dữ liệu điều
mà `03` và `11` đã quy định: không ai nhập tay được trạng thái tính ra.

```sql
CREATE TABLE departure_price (
  departure_id UUID          NOT NULL REFERENCES departure (id) ON DELETE CASCADE,
  pax_type_id  UUID          NOT NULL REFERENCES pax_type (id),
  occupancy    VARCHAR(8)    NOT NULL,                 -- 'DOUBLE' | 'SINGLE'
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

CREATE TABLE seat_hold (
  id           UUID        PRIMARY KEY,
  departure_id UUID        NOT NULL REFERENCES departure (id) ON DELETE CASCADE,
  seats        SMALLINT    NOT NULL CHECK (seats > 0),
  session_ref  VARCHAR(64) NOT NULL,
  expires_at   TIMESTAMPTZ NOT NULL,
  released_at  TIMESTAMPTZ,
  booking_id   UUID        REFERENCES booking (id)
);

-- Job quét hạn chỉ đọc những dòng còn sống.
CREATE INDEX ix_seat_hold_active
  ON seat_hold (expires_at)
  WHERE released_at IS NULL AND NOT soft_delete;

CREATE INDEX ix_seat_hold_departure
  ON seat_hold (departure_id)
  WHERE released_at IS NULL AND NOT soft_delete;
```

Chỗ khả dụng tính bằng khung nhìn, **không** bằng cột:

```sql
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
```

Trừ cả chỗ đang giữ — quy tắc 8 của `04` mục 12.

---

## 6. Đơn đặt và báo giá

```sql
CREATE TABLE booking (
  id             UUID          PRIMARY KEY,
  reference      VARCHAR(20)   NOT NULL,               -- 'VN-2026-8F3K2P'
  market         VARCHAR(2)    NOT NULL REFERENCES market (code),
  locale         VARCHAR(8)    NOT NULL REFERENCES locale (code),
  product_id     UUID          NOT NULL REFERENCES product (id),
  departure_id   UUID          REFERENCES departure (id),   -- NULL với PRIVATE/COMBO
  quote_id       UUID          REFERENCES quote (id),
  status         VARCHAR(24)   NOT NULL,
  product_title  VARCHAR(200)  NOT NULL,               -- CHỤP LẠI
  total          NUMERIC(12,2) NOT NULL,
  deposit        NUMERIC(12,2) NOT NULL,
  currency       VARCHAR(3)    NOT NULL,
  contact_email  VARCHAR(320)  NOT NULL,
  contact_phone  VARCHAR(32)   NOT NULL,
  -- + năm cột kiểm toán, mục 2.1. `created_by` để NULL khi khách tự đặt:
  -- v1 không có tài khoản khách — `11` mục 11.1.

  CONSTRAINT ck_booking_status CHECK (status IN
    ('DRAFT','PENDING_PAYMENT','PENDING_CONFIRMATION','CONFIRMED',
     'COMPLETED','CANCELLED','REFUNDED','EXPIRED')),
  CONSTRAINT ck_booking_deposit CHECK (deposit BETWEEN 0 AND total)
);

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

CREATE TABLE booking_event (                           -- chỉ ghi thêm
  id          UUID        PRIMARY KEY,
  booking_id  UUID        NOT NULL REFERENCES booking (id),
  from_status VARCHAR(24),
  to_status   VARCHAR(24) NOT NULL,
  actor_type  VARCHAR(16) NOT NULL,                    -- 'CUSTOMER','STAFF','SYSTEM'
  actor_id    UUID,
  note        TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

```sql
CREATE UNIQUE INDEX ux_booking_reference ON booking (reference)
  WHERE NOT soft_delete;
```

`booking_event` **không có `UPDATE` và `DELETE`** — thu hồi quyền hai lệnh này ở
tầng vai trò CSDL, không chỉ trông vào kỷ luật code.

`quote` và `quote_line` theo đúng khuôn mẫu này, với
`status ∈ (DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED)` và cột `valid_until`.

### 6.1. Hành khách của đơn

Bước 4 của luồng đặt tour thu thông tin từng người đi (`23` mục 2.1). `14` mục
6.3 chèn vào bảng này trong cùng transaction với `booking`.

```sql
-- Nhóm C: vòng đời theo booking, không cột kiểm toán, không soft_delete.
CREATE TABLE booking_passenger (
  booking_id      UUID         NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
  seq             SMALLINT     NOT NULL,
  pax_type_id     UUID         NOT NULL REFERENCES pax_type (id),
  full_name       VARCHAR(160) NOT NULL,
  date_of_birth   DATE,
  passport_no     VARCHAR(32),
  passport_expiry DATE,
  nationality     VARCHAR(2),                 -- ISO 3166-1 alpha-2

  PRIMARY KEY (booking_id, seq),
  CONSTRAINT ck_bp_seq        CHECK (seq >= 1),
  CONSTRAINT ck_bp_passport   CHECK (passport_no IS NULL OR passport_expiry IS NOT NULL),
  CONSTRAINT ck_bp_nationality CHECK (nationality IS NULL OR nationality = UPPER(nationality))
);
```

Ba trường hộ chiếu **cho phép NULL** vì chúng không thu ở bước đặt: khách nhiều
khi chưa có hộ chiếu mới lúc đặt tour trước cả năm. Nhân viên bổ sung sau, và
`ck_bp_passport` chỉ đòi rằng đã có số thì phải có hạn — số hộ chiếu không kèm
ngày hết hạn là thứ vô dụng khi làm thị thực.

Không xoá dòng hành khách khỏi một đơn đã xác nhận: đơn hỏng thì `CANCELLED`
(`23` mục 4). Đây là **dữ liệu cá nhân** — thời hạn lưu và việc mã hoá số hộ
chiếu còn để ngỏ ở mục 10, và `32` chịu trách nhiệm phần pháp lý.

### 6.2. Tính bất biến khi gọi lại

`13` mục 7: gọi lại cùng `Idempotency-Key` trong 24 giờ trả về **cùng kết quả
cũ**. Muốn vậy phải nhớ kết quả đã trả.

```sql
CREATE TABLE idempotency_key (
  key          UUID         PRIMARY KEY,
  market       VARCHAR(2)   NOT NULL REFERENCES market (code),
  endpoint     VARCHAR(64)  NOT NULL,
  request_hash CHAR(64)     NOT NULL,     -- vân tay thân yêu cầu
  status_code  SMALLINT     NOT NULL,
  response     JSONB        NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at   TIMESTAMPTZ  NOT NULL,
  CONSTRAINT ck_idem_status CHECK (status_code BETWEEN 100 AND 599)
);
```

Nhóm D — chỉ ghi thêm rồi hết hạn. **Không sửa**: một khoá đã trả kết quả nào
thì vĩnh viễn trả kết quả đó, nếu không thì lần gọi lại thứ hai và thứ ba cho ra
hai câu trả lời khác nhau — đúng cái mà cơ chế này sinh ra để ngăn.

**`request_hash` là chỗ dễ bỏ sót.** Cùng khoá nhưng thân yêu cầu **khác** không
phải là một lần gọi lại, mà là lỗi phía client dùng lại khoá cho việc khác. Trả
kết quả cũ trong tình huống đó là im lặng nuốt mất một đơn hàng thật.

### 6.3. Khoá cho job nền

`14` mục 6.4 đòi khoá ngay từ job đầu tiên. Lược đồ theo đúng yêu cầu của
ShedLock, tên cột không đổi được:

```sql
CREATE TABLE shedlock (
  name       VARCHAR(64)  PRIMARY KEY,
  lock_until TIMESTAMPTZ  NOT NULL,
  locked_at  TIMESTAMPTZ  NOT NULL,
  locked_by  VARCHAR(255) NOT NULL
);
```

Job quét hạn giữ chỗ là `UPDATE` bất biến khi lặp, nên chạy hai lần không hại gì.
Nhưng **cùng cơ chế** còn dùng cho gửi email nhắc và cho hết hạn báo giá — hai
việc không bất biến. Đặt khoá từ job đầu tiên rẻ hơn nhiều so với nhớ ra ở job
thứ ba.

---

## 7. Index cho tìm kiếm và sắp xếp

```sql
-- Sắp theo collation của từng locale, index bộ phận theo locale
CREATE INDEX ix_pt_title_da ON product_translation (title COLLATE "da-DK-x-icu")
  WHERE locale = 'da' AND NOT soft_delete;
CREATE INDEX ix_pt_title_vi ON product_translation (title COLLATE "vi-VN-x-icu")
  WHERE locale = 'vi' AND NOT soft_delete;

-- Tìm kiếm không dấu
CREATE INDEX ix_pt_search
  ON product_translation USING gin (f_unaccent(title) gin_trgm_ops);

-- Listing: lọc theo locale + trạng thái là truy vấn nóng nhất của cả hệ thống
CREATE INDEX ix_pt_listing
  ON product_translation (locale, status)
  INCLUDE (product_id, slug, title)
  WHERE status = 'PUBLISHED' AND NOT soft_delete;
```

Câu truy vấn sắp xếp **phải ghi cùng collation** với index, nếu không Postgres bỏ
qua index và quay về sắp toàn bộ:

```sql
ORDER BY tt.title COLLATE "da-DK-x-icu"
```

---

## 8. Migration

| Mục | Quy ước |
|---|---|
| Công cụ | Flyway, SQL thuần |
| Tên file | `V<số>__<mô_tả_không_dấu>.sql` — `V12__them_bang_seat_hold.sql` |
| Số phiên bản | Tăng dần, **không tái sử dụng**, không sửa file đã chạy |
| Dữ liệu mồi | `R__seed_<tên>.sql` — migration lặp lại được, chỉ dùng cho dữ liệu tra cứu |
| Nơi chạy | Lúc triển khai, **trước** khi container ứng dụng nhận traffic |
| Rollback | Viết migration mới, **không** rollback ngược |

**Quy tắc đổi phá vỡ tương thích:** tách làm hai lần triển khai. Thêm cột mới →
ghi cả hai chỗ → chuyển dữ liệu → đọc từ chỗ mới → lần triển khai sau mới xoá cột
cũ. Đổi tên cột trong một migration là làm sập ứng dụng bản cũ đang còn chạy.

### 8.1. Dữ liệu tra cứu

Nạp bằng `R__` migration, không nhập tay: `market`, `locale`, `pax_type`,
`departure_origin`, `role`. Đây là dữ liệu mà **code phụ thuộc vào**, nên nó
thuộc về migration.

### 8.2. Dữ liệu mồi cho môi trường dev

Kịch bản riêng, không phải migration. Tối thiểu để mọi màn hình có gì để hiện:

```
2 thị trường · 2 ngôn ngữ · 3 miền · 15 điểm đến · 12 chủ đề
6 sản phẩm — mỗi loại một cái, để test cả sáu bố cục
  · GROUP_TOUR có ≥ 5 ngày khởi hành, đủ mọi trạng thái
  · CRUISE có ≥ 2 ngày, mỗi ngày đủ 4 hạng cabin
  · PRIVATE_TOUR có ≥ 4 bậc giá
1 sản phẩm CHƯA dịch sang vi — để test chính sách không-fallback
1 sản phẩm dịch rồi nhưng CHƯA gán thị trường VN — để test cổng chặn
1 ngày khởi hành SOLD_OUT, 1 FEW_SEATS, 1 PENDING
37 khách sạn · 33 tham quan · 8 bài viết · 21 đánh giá · 4 chuyên viên
```

Hai dòng in đậm ở giữa là quan trọng nhất: **dữ liệu mồi phải chứa sẵn các
trường hợp rìa**, nếu không sẽ không ai gặp chúng cho tới khi lên production.

---

## 9. Bộ kiểm tính nhất quán dữ liệu

Tương đương `npm run validate-data` của bản demo — thứ mà tài liệu gọi là "hàng
rào chính". Chạy trong CI và chạy được tay trên bất kỳ môi trường nào.

| # | Quy tắc | Bắt được gì |
|---|---|---|
| 1 | Mọi `product` có đúng một dòng bảng con khớp `product_type` | Lỗ hổng đã nêu ở 4.2 |
| 2 | Mọi `product` có bản dịch `da` với `status = 'PUBLISHED'` khi `product_market.is_published` | Xuất bản sản phẩm chưa có nội dung |
| 3 | `COUNT(itinerary_day) = product.duration_days` cho bốn loại tour dài | Lịch trình thủng ngày |
| 4 | Tổng `product_hotel_stay.nights` khớp số đêm có khách sạn trong lịch trình, **theo từng khách sạn** | Ghi 3 đêm Hội An mà lịch trình có 2 |
| 5 | `product_market.price_from` khớp `MIN(departure_price)` của thị trường đó | Cột vật chất hoá lệch thực tế |
| 6 | Giá **dao động** giữa các ngày khởi hành của cùng sản phẩm và thị trường | Dữ liệu nhập ẩu — `04` mục 4.1 |
| 7 | `CRUISE`: mỗi ngày khởi hành đủ bốn hạng cabin, chênh giá 20–45% | `04` mục 4.4 |
| 8 | `PRIVATE_TOUR`: bậc giá liên tục, không chồng lấn, giá giảm dần theo bậc | `04` mục 4.3 |
| 9 | `PRIVATE_TOUR` **không có** dòng `departure` nào | Dữ liệu sai loại |
| 10 | Mọi `hotel_id` trong lịch trình và `hotel_stay` tồn tại | Tham chiếu chết |
| 11 | Mô tả mỗi ngày lịch trình ≥ 80 ký tự | Nội dung lấp chỗ trống |
| 12 | Mỗi điểm đến có ≥ 1 khách sạn và ≥ 1 tham quan | Trang điểm đến rỗng |
| 13 | Mọi `pax_type` của một thị trường có giá ở mọi `departure` của thị trường đó | Thiếu giá trẻ em |
| 14 | `lecture.event_date` còn ngày trong tương lai | Trang sự kiện sẽ rỗng — cảnh báo trước |
| 15 | Không có `seat_hold` quá hạn mà chưa `released_at` | Job quét hạn chết |
| 16 | `last_modified_at >= created_at` ở mọi bảng có hai cột đó | Ứng dụng ghi đè sai, hoặc thiếu trigger |
| 17 | Mọi bảng có `last_modified_at` đều có trigger `tg_*_last_modified` | Thêm bảng mới mà quên gắn trigger — cột "sửa lần cuối" đứng yên vĩnh viễn |
| 18 | Dòng `soft_delete = TRUE` phải có `last_modified_by` khác NULL | Xoá mà không biết ai xoá thì cột xoá mềm vô nghĩa |
| 19 | Không dòng còn sống nào trỏ tới bản ghi đã xoá mềm trên đường đọc của khách | Xoá mềm không lan xuống dưới — `11` mục 11.3.b |
| 20 | Không `media_asset` nào đang được dùng mà `licence_until` đã qua | Giấy phép ảnh hết hạn mà ảnh vẫn nằm trên web — `24` mục 8 |
| 21 | Không `slug_history.old_slug` nào trùng slug đang dùng của cùng `(entity_type, locale)` | Vòng lặp chuyển hướng 301 |
| 22 | Mọi `booking` đã `CONFIRMED` có số dòng `booking_passenger` bằng số khách của đơn | Đơn xác nhận mà thiếu tên người đi |

Quy tắc 5, 6, 7, 8 và 15 là loại mà `tsc` và ràng buộc CSDL **không** bắt được —
đúng như bài học của bản demo.

Quy tắc 17 và 19 sinh ra vì xoá mềm: cả hai lỗi đều **không** làm gãy gì, chỉ làm
dữ liệu sai âm thầm. Quy tắc 19 đắt nhất bảng vì phải quét chéo nhiều bảng — chạy
hằng đêm trong CI, không chạy ở mỗi lần build.

---

## 10. Việc còn để ngỏ

| Việc | Chặn cái gì |
|---|---|
| Chuyển `product.hero_image` và `map_image` sang tham chiếu `media_asset` | Ảnh đầu trang và ảnh bản đồ hiện không có chứng từ giấy phép. Đổi phá vỡ tương thích: phải tách hai lần triển khai theo mục 8 |
| Ai được phép cấp vai trò `ADMIN`, và có cần hai người duyệt không | `22` mục 9 |
| **Chưa có bảng `site_info`** — `13` mục 9.1 mới nói đúng bốn chữ "thị thực, mùa, tiền tệ, lệch giờ". Chưa đủ để dựng bảng; cần đặc tả nội dung trước | `GET /{market}/site-info` |
| **`destination_translation` và `lecture_translation` không có `status` lẫn `translated_at`** — nên với hai thứ này thì "bản nguồn đã xuất bản chưa" và "dịch từ lúc nào" đều không trả lời được | Hàng đợi dịch (`22` mục 4.1) hiện chỉ phủ sản phẩm và bài viết. Thêm hai cột là định nghĩa một vòng đời xuất bản cho điểm đến — thứ chưa tài liệu nào mô tả, và nó đụng thẳng vào chính sách không-fallback của `02` |
| Số chỗ mặc định `capacity` của `GROUP_TOUR` lấy từ `max_pax` hay nhập riêng | DDL `departure` |
| Có mã hoá cột số hộ chiếu ở v1 không | `booking_passenger`, `31` |
| Thời hạn lưu dữ liệu cá nhân | Cột `retention_until`, job xoá |
| Có cần bảng `*_revision` cho lịch sử nội dung không | `11` mục 13 |
