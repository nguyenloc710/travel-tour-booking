-- Bốn nhóm bảng mà các tài liệu khác đã gọi tên nhưng lược đồ chưa có.
-- DDL đầy đủ và lý do từng quyết định: docs/12 mục 3.1, 4.6, 4.7, 6.1.
--
--   1. role, staff_user_role   — ma trận quyền của docs/22 mục 2.1
--   2. media_asset + bộ ảnh    — docs/05 đòi bộ ảnh, docs/24 mục 8 đòi giấy phép
--   3. slug_history            — chuyển hướng 301, docs/24 mục 4
--   4. booking_passenger       — bước 4 của docs/23, được docs/14 mục 6.3 chèn vào
--
-- Cột kiểm toán và trigger last_modified nằm ở khối DO cuối file, cùng khuôn
-- mẫu với V1 — thêm bảng mới thì thêm tên vào đúng một chỗ.

-- ------------------------------------------------------------- 1. Vai trò

CREATE TABLE role (
  code       VARCHAR(16) PRIMARY KEY,      -- 'CONSULTANT','EDITOR','TRANSLATOR','ADMIN'
  sort_order SMALLINT    NOT NULL,
  CONSTRAINT ck_role_code CHECK (code = UPPER(code))
);

-- Khoá chính THAY THẾ, không phải khoá kép (staff_user_id, role_code).
-- Với khoá kép thì cấp lại một vai trò đã thu là không thể: dòng đã
-- soft_delete vẫn chiếm khoá. docs/12 mục 2.2.
CREATE TABLE staff_user_role (
  id            UUID        PRIMARY KEY,
  staff_user_id UUID        NOT NULL REFERENCES staff_user (id) ON DELETE CASCADE,
  role_code     VARCHAR(16) NOT NULL REFERENCES role (code)
);

-- ------------------------------------------------------------- 2. Ảnh

CREATE TABLE media_asset (
  id             UUID        PRIMARY KEY,
  path           TEXT        NOT NULL,
  width          INTEGER     NOT NULL,
  height         INTEGER     NOT NULL,
  byte_size      INTEGER     NOT NULL,
  source         VARCHAR(16) NOT NULL,
  licence_ref    VARCHAR(200),
  licence_scope  VARCHAR(200),
  licence_until  DATE,
  person_consent BOOLEAN     NOT NULL DEFAULT FALSE,

  CONSTRAINT ck_media_source CHECK (source IN ('SELF','PARTNER','PURCHASED','CUSTOMER')),
  CONSTRAINT ck_media_size   CHECK (width > 0 AND height > 0 AND byte_size > 0),
  -- Ảnh không phải tự chụp thì BẮT BUỘC có chứng từ — docs/24 mục 8.
  -- Cưỡng chế ở tầng CSDL: ảnh tạm là thứ ở lại lâu nhất.
  CONSTRAINT ck_media_licence CHECK (source = 'SELF' OR licence_ref IS NOT NULL)
);

-- Ảnh dùng chung cho mọi ngôn ngữ; alt thì KHÔNG — docs/11 mục 6.
CREATE TABLE media_asset_translation (
  asset_id UUID         NOT NULL REFERENCES media_asset (id) ON DELETE CASCADE,
  locale   VARCHAR(8)   NOT NULL REFERENCES locale (code),
  alt      VARCHAR(300) NOT NULL,
  PRIMARY KEY (asset_id, locale)
);

-- Nhóm C: vòng đời theo product. KHÔNG có ON DELETE CASCADE ở asset_id —
-- gỡ ảnh khỏi bộ ảnh không được làm mất chứng từ giấy phép của ảnh đó.
CREATE TABLE product_image (
  product_id UUID     NOT NULL REFERENCES product (id) ON DELETE CASCADE,
  asset_id   UUID     NOT NULL REFERENCES media_asset (id),
  sort_order SMALLINT NOT NULL,
  PRIMARY KEY (product_id, asset_id)
);

-- ------------------------------------------------------------- 3. Slug cũ

-- Nhóm D — nhật ký chỉ ghi thêm, như booking_event.
-- Cố ý KHÔNG có khoá ngoại tới thực thể: bảng phục vụ bốn loại, và bốn bảng
-- lịch sử gần giống nhau là bốn chỗ để quên một chỗ. docs/12 mục 4.7.
CREATE TABLE slug_history (
  id          UUID         PRIMARY KEY,
  entity_type VARCHAR(16)  NOT NULL,
  entity_id   UUID         NOT NULL,
  locale      VARCHAR(8)   NOT NULL REFERENCES locale (code),
  old_slug    VARCHAR(160) NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  UUID         REFERENCES staff_user (id),

  CONSTRAINT ck_slug_history_type CHECK (entity_type IN
    ('PRODUCT','DESTINATION','REGION','POST')),
  CONSTRAINT ck_slug_history_slug CHECK (old_slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

-- ------------------------------------------------------- 4. Hành khách của đơn

-- Nhóm C: vòng đời theo booking. Ba trường hộ chiếu cho phép NULL vì khách đặt
-- trước cả năm thường chưa có hộ chiếu mới; ck_bp_passport chỉ đòi rằng đã có
-- số thì phải có hạn — số hộ chiếu không kèm ngày hết hạn là thứ vô dụng.
CREATE TABLE booking_passenger (
  booking_id      UUID         NOT NULL REFERENCES booking (id) ON DELETE CASCADE,
  seq             SMALLINT     NOT NULL,
  pax_type_id     UUID         NOT NULL REFERENCES pax_type (id),
  full_name       VARCHAR(160) NOT NULL,
  date_of_birth   DATE,
  passport_no     VARCHAR(32),
  passport_expiry DATE,
  nationality     VARCHAR(2),

  PRIMARY KEY (booking_id, seq),
  CONSTRAINT ck_bp_seq         CHECK (seq >= 1),
  CONSTRAINT ck_bp_passport    CHECK (passport_no IS NULL OR passport_expiry IS NOT NULL),
  CONSTRAINT ck_bp_nationality CHECK (nationality IS NULL OR nationality = UPPER(nationality))
);

-- ------------------------------------------- 5. Cột kiểm toán và xoá mềm
--
-- Cùng khuôn mẫu với V1. Bảng nào nhận cột nào: docs/11 mục 11.2.
--   · media_asset, media_asset_translation, staff_user_role → nhóm A, đủ năm cột
--   · role                                                  → nhóm B, bốn cột
--   · product_image, booking_passenger                      → nhóm C, không cột nào
--   · slug_history                                          → nhóm D, chỉ created_*

DO $$
DECLARE
  bang text;
  co_xoa_mem   text[] := ARRAY['media_asset','media_asset_translation','staff_user_role'];
  chi_kiem_toan text[] := ARRAY['role'];
BEGIN
  FOREACH bang IN ARRAY (co_xoa_mem || chi_kiem_toan) LOOP
    EXECUTE format(
      'ALTER TABLE %1$I
         ADD COLUMN created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
         ADD COLUMN created_by       UUID REFERENCES staff_user (id),
         ADD COLUMN last_modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
         ADD COLUMN last_modified_by UUID REFERENCES staff_user (id)', bang);

    EXECUTE format(
      'CREATE TRIGGER tg_%1$s_last_modified BEFORE UPDATE ON %1$I
         FOR EACH ROW EXECUTE FUNCTION trg_set_last_modified()', bang);
  END LOOP;

  FOREACH bang IN ARRAY co_xoa_mem LOOP
    EXECUTE format(
      'ALTER TABLE %1$I ADD COLUMN soft_delete BOOLEAN NOT NULL DEFAULT FALSE', bang);
  END LOOP;
END $$;

-- ------------------------------------------------------------- 6. Index
--
-- Bảng có soft_delete thì khoá duy nhất phải là index BỘ PHẬN — docs/12 mục 2.2.

CREATE UNIQUE INDEX ux_staff_user_role
  ON staff_user_role (staff_user_id, role_code)
  WHERE NOT soft_delete;

CREATE UNIQUE INDEX ux_media_asset_path
  ON media_asset (path)
  WHERE NOT soft_delete;

CREATE INDEX ix_product_image ON product_image (product_id, sort_order);

-- slug_history không có soft_delete nên đây là index duy nhất bình thường.
CREATE UNIQUE INDEX ux_slug_history
  ON slug_history (entity_type, locale, old_slug);

CREATE INDEX ix_slug_history_entity ON slug_history (entity_type, entity_id);

-- --------------------------------------------------- 7. Trigger lưu slug cũ
--
-- Ghi bằng trigger, không bằng code ứng dụng: đổi slug xảy ra ở nhiều màn hình,
-- và chỗ nào quên gọi thì lỗi không hiện ra cho tới khi có người báo link chết.

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

CREATE TRIGGER tg_product_translation_slug_cu
  AFTER UPDATE OF slug ON product_translation
  FOR EACH ROW EXECUTE FUNCTION trg_luu_slug_cu('PRODUCT', 'product_id');

CREATE TRIGGER tg_destination_translation_slug_cu
  AFTER UPDATE OF slug ON destination_translation
  FOR EACH ROW EXECUTE FUNCTION trg_luu_slug_cu('DESTINATION', 'destination_id');

CREATE TRIGGER tg_region_translation_slug_cu
  AFTER UPDATE OF slug ON region_translation
  FOR EACH ROW EXECUTE FUNCTION trg_luu_slug_cu('REGION', 'region_id');
