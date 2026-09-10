-- V9 — video cho điểm đến, và `media_asset` mở ra cho loại tệp thứ hai.
--
-- Tới V8, mọi thứ trong `media_asset` là ảnh, và bảng nối `destination_image`
-- nói đúng điều đó. Yêu cầu mới là mỗi điểm đến có **ảnh và video**, nên hai
-- việc phải làm: `media_asset` phải biết mình đang giữ loại gì, và bảng nối phải
-- có cái tên không nói dối.
--
-- ADR-013 chốt kho là MinIO tự dựng, và video **tự lưu** chứ không nhúng nhà
-- cung cấp ngoài. Cái giá đã biết trước: băng thông đổ lên VPS và chưa có CDN —
-- ADR-011 mục 5 đã ghi đây là chỗ phải theo dõi.

-- ------------------------------------------------- 1. media_asset biết loại

-- `DEFAULT 'IMAGE'` để mọi dòng đang có hợp lệ ngay, không phải backfill.
ALTER TABLE media_asset
  ADD COLUMN kind             VARCHAR(8)  NOT NULL DEFAULT 'IMAGE',
  ADD COLUMN content_type     VARCHAR(64),
  ADD COLUMN duration_seconds INTEGER,
  ADD COLUMN poster_asset_id  UUID REFERENCES media_asset (id);

ALTER TABLE media_asset
  ADD CONSTRAINT ck_media_kind CHECK (kind IN ('IMAGE', 'VIDEO'));

-- Video BẮT BUỘC có thời lượng, kiểu tệp và ảnh bìa; ảnh thì không được có cái
-- nào trong ba thứ đó.
--
-- Vì sao ảnh bìa là bắt buộc chứ không tuỳ chọn: thiếu nó, trình duyệt phải tải
-- những megabyte đầu của video chỉ để vẽ một khung hình tĩnh. Trên một VPS không
-- CDN thì đó là băng thông trả cho thứ khách chưa bấm xem — và `24` mục 7 đã đặt
-- ngân sách byte cho trang chi tiết.
--
-- "Ảnh bìa phải là một ẢNH, không phải video khác" thì CHECK không nói được —
-- nó là điều kiện trên dòng khác. Luật đó sống ở tầng nghiệp vụ (MediaService).
ALTER TABLE media_asset
  ADD CONSTRAINT ck_media_video CHECK (
    (kind = 'IMAGE' AND duration_seconds IS NULL AND poster_asset_id IS NULL)
    OR
    (kind = 'VIDEO' AND duration_seconds > 0
                    AND poster_asset_id IS NOT NULL
                    AND content_type IS NOT NULL));

COMMENT ON COLUMN media_asset.kind IS
  'IMAGE hoặc VIDEO. Ảnh bìa của video cũng là một dòng kind = IMAGE.';
COMMENT ON COLUMN media_asset.poster_asset_id IS
  'Ảnh bìa của video. Bắt buộc với VIDEO — xem ck_media_video và docs/24 mục 7.';

-- ------------------------------- 2. destination_image → destination_media
--
-- Đổi tên chứ không thêm bảng thứ hai. Ba lựa chọn đã cân:
--
--   a) Giữ tên `destination_image` và nhét video vào đó — cái tên nói dối, và
--      người đọc lược đồ sau này sẽ tin nó.
--   b) Thêm `destination_video` riêng — hai bảng cùng hình dạng, và bộ ảnh của
--      một điểm đến không sắp xen kẽ được: video luôn phải nằm riêng một khối.
--   c) Đổi tên. Rẻ ngay lúc này — V8 mới bốn ngày, chưa có đường ghi nào, và
--      đúng một truy vấn đọc trỏ vào nó.
--
-- `product_image` **cố tình không đổi**: bộ ảnh sản phẩm hôm nay chỉ có ảnh, và
-- đổi tên nó kéo theo mười hai chỗ tham chiếu gồm cả `kiem-nhat-quan.sql` và
-- `MigrationV2IT` — trả giá đó cho một cái tên chưa cần thiết. Ngày nào bộ ảnh
-- sản phẩm cần video thì đổi nó theo đúng cách này.
ALTER TABLE destination_image RENAME TO destination_media;
ALTER INDEX ix_destination_image RENAME TO ix_destination_media;

COMMENT ON TABLE destination_media IS
  'Ảnh VÀ video minh hoạ của điểm đến, một danh sách có thứ tự. Cùng khuôn bảng '
  'nối với product_image — xem docs/12 mục 4.6 và V9.';
