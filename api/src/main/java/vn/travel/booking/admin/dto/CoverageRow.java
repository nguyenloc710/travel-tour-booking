package vn.travel.booking.admin.dto;

/**
 * Một dòng của bảng độ phủ dịch (docs/22 M12).
 *
 * <p>Hai con số vì chúng nói hai chuyện khác nhau: 100% đã dịch nhưng 60% còn
 * hạn nghĩa là nội dung đang trôi, và đó là dấu hiệu sớm hơn nhiều so với việc
 * khách phàn nàn.
 *
 * @param total     mẫu số: bản ghi có bản nguồn {@code PUBLISHED}. Bản nguồn còn
 *                  là nháp thì chưa đến lượt dịch
 * @param upToDate  đã dịch <b>và</b> chưa quá hạn. Luôn {@code <= translated}
 */
public record CoverageRow(String entityType, String locale, int total, int translated, int upToDate) {
}
