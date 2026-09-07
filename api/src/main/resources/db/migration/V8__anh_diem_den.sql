-- V8 — ảnh minh hoạ cho điểm đến.
--
-- Trang điểm đến tới nay chỉ có chữ: tên, một câu tóm tắt, và số tour. Nó là
-- trang dành cho khách CHƯA BIẾT MÌNH MUỐN GÌ (`01` mục 3.1) — người đang chọn
-- vùng đất chứ chưa chọn chuyến đi. Một danh sách tên địa danh không nói được
-- với người đó rằng Sa Pa khác Phú Quốc ở chỗ nào.
--
-- VÌ SAO KHÔNG THÊM MỘT CỘT `image TEXT` NHƯ `hotel` VÀ `excursion`.
--
-- Đó là khuôn đang có trong lược đồ, nên nó là lựa chọn hiển nhiên — và `12`
-- mục 10 đã ghi chính khuôn ấy vào danh sách nợ: "Chuyển `product.hero_image`
-- và `map_image` sang tham chiếu `media_asset` — ảnh đầu trang và ảnh bản đồ
-- hiện không có chứng từ giấy phép." Thêm cột chữ thứ ba là làm khoản nợ đó to
-- thêm, và làm ngay sau khi vừa dựng xong hạ tầng để trả nó.
--
-- Đi qua `media_asset` thì mỗi tấm có sẵn: nguồn, giấy phép, phạm vi, hạn dùng
-- (`24` mục 8 đòi cả bốn), `person_consent`, và chữ `alt` theo từng locale.
--
-- BẢNG NỐI CHỨ KHÔNG PHẢI KHOÁ NGOẠI ĐƠN. Hôm nay mỗi điểm đến chỉ cần một tấm,
-- và một cột `image_asset_id` sẽ đủ. Nhưng lược đồ này đã có đúng một khuôn cho
-- quan hệ ảnh–thực thể là `product_image`, và thêm khuôn thứ ba cho cùng một
-- loại quan hệ đắt hơn cái tiết kiệm được: người đọc lược đồ sau này phải nhớ ba
-- cách thay vì một. Trang chi tiết điểm đến muốn có bộ ảnh về sau thì cũng không
-- cần migration nữa.
--
-- Ba điều cố ý giữ nguyên từ `product_image` (`12` mục 4.6):
--
--   · `asset_id` KHÔNG có ON DELETE CASCADE — gỡ ảnh khỏi điểm đến là xoá dòng
--     nối; bản thân ảnh và chứng từ giấy phép của nó ở lại.
--   · `destination_id` CÓ cascade — ảnh gắn vào một điểm đến đã xoá thì dòng
--     nối không còn nghĩa.
--   · Index theo (destination_id, sort_order) vì đó đúng là cách đọc.

CREATE TABLE destination_image (
  destination_id UUID     NOT NULL REFERENCES destination (id) ON DELETE CASCADE,
  asset_id       UUID     NOT NULL REFERENCES media_asset (id),
  sort_order     SMALLINT NOT NULL,
  PRIMARY KEY (destination_id, asset_id)
);

CREATE INDEX ix_destination_image ON destination_image (destination_id, sort_order);

COMMENT ON TABLE destination_image IS
  'Ảnh minh hoạ của điểm đến. Cùng khuôn với product_image — xem docs/12 mục 4.6.';
