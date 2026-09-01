-- Hai bảng hạ tầng cho đường ghi. DDL và lý do: docs/12 mục 6.2 và 6.3.

-- --------------------------------------------- 1. Tính bất biến khi gọi lại
--
-- docs/13 mục 7: gọi lại cùng Idempotency-Key trong 24 giờ trả về CÙNG kết quả
-- cũ, không tạo bản ghi thứ hai. Muốn vậy phải nhớ kết quả đã trả.
--
-- Nhóm D — chỉ ghi thêm rồi hết hạn. Không sửa: một khoá đã trả kết quả nào thì
-- vĩnh viễn trả kết quả đó, nếu không thì lần gọi lại thứ hai và thứ ba cho ra
-- hai câu trả lời khác nhau, đúng cái mà cơ chế này sinh ra để ngăn.
CREATE TABLE idempotency_key (
  key          UUID         PRIMARY KEY,
  market       VARCHAR(2)   NOT NULL REFERENCES market (code),
  endpoint     VARCHAR(64)  NOT NULL,
  -- Vân tay của thân yêu cầu. Cùng khoá nhưng thân KHÁC là lỗi phía client,
  -- không phải một lần gọi lại — trả 409 chứ không trả kết quả cũ.
  request_hash CHAR(64)     NOT NULL,
  status_code  SMALLINT     NOT NULL,
  response     JSONB        NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at   TIMESTAMPTZ  NOT NULL,

  CONSTRAINT ck_idem_status CHECK (status_code BETWEEN 100 AND 599)
);

CREATE INDEX ix_idempotency_expiry ON idempotency_key (expires_at);

-- ------------------------------------------------------- 2. Khoá cho job nền
--
-- docs/14 mục 6.4: chạy nhiều instance thì job quét hạn phải có khoá, nếu không
-- mỗi instance quét một lần. Ở job đó hệ quả nhẹ (UPDATE bất biến khi lặp),
-- nhưng CÙNG cơ chế còn dùng cho gửi email nhắc và cho hết hạn báo giá — hai
-- việc KHÔNG bất biến. Vì thế đặt khoá ngay từ job đầu tiên.
--
-- Lược đồ theo đúng yêu cầu của ShedLock; tên cột không đổi được.
CREATE TABLE shedlock (
  name       VARCHAR(64)  PRIMARY KEY,
  lock_until TIMESTAMPTZ  NOT NULL,
  locked_at  TIMESTAMPTZ  NOT NULL,
  locked_by  VARCHAR(255) NOT NULL
);
