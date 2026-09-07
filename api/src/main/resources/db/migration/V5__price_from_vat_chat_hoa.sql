-- V5 — cột vật chất hoá `product_market.price_from`, và trigger giữ nó đúng.
--
-- docs/12 mục 6.1 ghi cột này là "vật chất hoá, trigger cập nhật" và docs/12
-- mục 9 quy tắc kiểm 5 đòi nó khớp thực tế, nhưng trigger thì chưa ai viết.
-- Cột vẫn NULL suốt từ V1, nên mọi sản phẩm hiện "Liên hệ" thay vì hiện giá.
--
-- VÌ SAO VẬT CHẤT HOÁ, khi cả docs/11 mục 12 nói trạng thái phải tính ra chứ
-- đừng lưu: đây là ngoại lệ đã ghi rõ ở docs/11 mục 12 — listing sắp xếp theo
-- giá, và tính min() cho từng dòng của mỗi trang là truy vấn không index được.
-- Ngoại lệ thì phải có thứ canh nó khỏi lệch, và thứ đó là trigger dưới đây
-- cộng với quy tắc kiểm 5.
--
-- ĐỊNH NGHĨA "giá từ" lấy nguyên văn docs/03: giá thấp nhất *cho 1 người khi 2
-- người ở phòng đôi*. Hai vế, và cả hai đều phải vào truy vấn:
--
--   occupancy = 'DOUBLE'      -- phụ thu phòng đơn không thuộc giá từ
--   pt.code = 'ADULT'         -- "1 NGƯỜI" ở đây là một người lớn
--
-- Vế thứ hai dễ bỏ sót và hỏng theo hướng tệ nhất: thiếu nó thì min() vớ phải
-- giá trẻ em, và website quảng cáo giá trẻ em như giá tour. Đó là chuyện pháp lý
-- (docs/32), không phải chuyện hiển thị.
--
-- ĐÃ THỬ discount_rate = 0 và BỎ: nghe có vẻ dữ liệu hơn ("giảm 0% tức nguyên
-- giá"), nhưng chính api/scripts/seed-dev.sql đặt discount_rate = 0 cho cả CHILD
-- lẫn INFANT, nên điều kiện đó không loại được gì. Một quy ước mà dữ liệu của
-- chính dự án đã vi phạm thì không phải quy ước.
--
-- 'ADULT' là chuỗi khoá cứng, và đó là điều CẦN GHI RA chứ không cần giấu: tài
-- liệu chưa chỗ nào nói loại khách nào là loại tính giá niêm yết. Ghi ở docs/12
-- mục 10, chờ Q-2. Thị trường không có loại khách 'ADULT' thì price_from để
-- NULL và website hiện "Liên hệ" — hành vi an toàn đã có sẵn.

CREATE FUNCTION f_refresh_price_from(p_product_id UUID, p_market VARCHAR)
RETURNS void
LANGUAGE plpgsql AS
$$
BEGIN
  UPDATE product_market pm
     SET price_from = (
           SELECT min(dp.amount)
           FROM departure d
           JOIN departure_price dp ON dp.departure_id = d.id
           JOIN pax_type pt        ON pt.id = dp.pax_type_id
                                  AND NOT pt.soft_delete
           WHERE d.product_id = p_product_id
             AND d.market     = p_market
             AND NOT d.soft_delete
             AND dp.occupancy = 'DOUBLE'
             AND pt.code = 'ADULT')
   WHERE pm.product_id = p_product_id
     AND pm.market     = p_market;
END
$$;

-- Đổi giá của một ngày khởi hành.
CREATE FUNCTION trg_price_from_tu_gia() RETURNS trigger
LANGUAGE plpgsql AS
$$
DECLARE
  d departure%ROWTYPE;
BEGIN
  SELECT * INTO d FROM departure
   WHERE id = COALESCE(NEW.departure_id, OLD.departure_id);
  IF FOUND THEN
    PERFORM f_refresh_price_from(d.product_id, d.market);
  END IF;
  RETURN NULL;
END
$$;

-- Thêm, sửa, xoá hoặc xoá mềm một ngày khởi hành. Xoá mềm cũng phải tính lại:
-- ngày rẻ nhất bị gỡ khỏi lịch thì giá từ phải tăng lên, nếu không website
-- quảng cáo một mức giá không còn đặt được — và đó là chuyện pháp lý, không
-- phải chuyện hiển thị.
CREATE FUNCTION trg_price_from_tu_ngay() RETURNS trigger
LANGUAGE plpgsql AS
$$
BEGIN
  IF TG_OP <> 'INSERT' THEN
    PERFORM f_refresh_price_from(OLD.product_id, OLD.market);
  END IF;
  IF TG_OP <> 'DELETE' THEN
    PERFORM f_refresh_price_from(NEW.product_id, NEW.market);
  END IF;
  RETURN NULL;
END
$$;

-- Gán sản phẩm vào một thị trường: dòng mới phải nhận giá ngay, vì lịch khởi
-- hành có thể đã nhập từ trước.
CREATE FUNCTION trg_price_from_khi_gan() RETURNS trigger
LANGUAGE plpgsql AS
$$
BEGIN
  PERFORM f_refresh_price_from(NEW.product_id, NEW.market);
  RETURN NULL;
END
$$;

CREATE TRIGGER tg_departure_price_price_from
  AFTER INSERT OR UPDATE OR DELETE ON departure_price
  FOR EACH ROW EXECUTE FUNCTION trg_price_from_tu_gia();

CREATE TRIGGER tg_departure_price_from
  AFTER INSERT OR UPDATE OR DELETE ON departure
  FOR EACH ROW EXECUTE FUNCTION trg_price_from_tu_ngay();

-- CHỈ INSERT, không UPDATE: hàm ở trên UPDATE chính bảng này, và bắt UPDATE
-- ở đây là đệ quy vô hạn.
CREATE TRIGGER tg_product_market_price_from
  AFTER INSERT ON product_market
  FOR EACH ROW EXECUTE FUNCTION trg_price_from_khi_gan();

-- Dữ liệu đã có từ trước migration này.
UPDATE product_market pm
   SET price_from = (
         SELECT min(dp.amount)
         FROM departure d
         JOIN departure_price dp ON dp.departure_id = d.id
         JOIN pax_type pt        ON pt.id = dp.pax_type_id
                                AND NOT pt.soft_delete
         WHERE d.product_id = pm.product_id
           AND d.market     = pm.market
           AND NOT d.soft_delete
           AND dp.occupancy = 'DOUBLE'
           AND pt.code = 'ADULT');
