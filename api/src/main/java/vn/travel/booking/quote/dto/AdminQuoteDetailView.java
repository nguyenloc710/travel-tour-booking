package vn.travel.booking.quote.dto;

import java.util.List;

/**
 * Một báo giá đầy đủ: dòng danh sách, cộng thứ chỉ màn hình chi tiết cần.
 *
 * <p>Gộp bằng cách <b>chứa</b> {@link AdminQuoteRow} chứ không chép lại mười
 * bốn trường của nó: hai bản sao của cùng một tập trường là hai chỗ phải nhớ
 * sửa cùng lúc, và chỗ thứ hai luôn là chỗ bị quên.
 *
 * @param leadTimeDays   của sản phẩm — màn hình cần nó để giải thích ngày bị từ chối
 * @param quoteValidDays số ngày báo giá còn hiệu lực kể từ lúc gửi
 * @param lines          rỗng khi chưa ai dựng bảng giá
 */
public record AdminQuoteDetailView(
        AdminQuoteRow summary,
        String contactPhone,
        String message,
        int leadTimeDays,
        int quoteValidDays,
        List<QuoteLineRow> lines) {
}
