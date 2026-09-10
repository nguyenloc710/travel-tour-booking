package vn.travel.booking.product.dto;

import java.util.List;

/**
 * Một chặng dừng của lộ trình — khối "bản đồ lộ trình" ở tab Tổng quan
 * (docs/05 mục 6.1).
 *
 * <p><b>Dựng từ lịch trình, không nhập riêng.</b> Mỗi điểm đến xuất hiện một lần,
 * số đêm bằng số ngày lịch trình ngủ ở đó. Cho biên tập viên nhập lại danh sách
 * chặng là mời hai nguồn sự thật về cùng một lộ trình.
 */
public record ProductStopView(
        NamedRef destination,
        int nights,
        List<Integer> dayNumbers,
        List<MediaItemView> media) {

    /**
     * Một tệp của điểm đến, nhìn từ bề mặt khách.
     *
     * <p>{@code alt} lấy theo locale đang đọc, và tệp <b>không có {@code alt} ở
     * locale đó thì không xuất hiện</b> — luật không fallback, cưỡng chế bằng
     * {@code INNER JOIN} chứ không bằng {@code if} ở tầng ứng dụng (ADR-003).
     */
    public record MediaItemView(
            String kind,
            String url,
            String alt,
            int width,
            int height,
            String contentType,
            Integer durationSeconds,
            String posterUrl) {
    }
}
