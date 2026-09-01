package vn.travel.booking.application.product;

/**
 * Một chặng nghỉ khách sạn của sản phẩm.
 *
 * <p>{@code name} không dịch; {@code description} thì có, và là {@code null}
 * khi chưa có bản dịch cho locale đang xem. Đây là chỗ ranh giới "cái gì là nội
 * dung" đi qua giữa hai trường của cùng một thực thể.
 */
public record HotelStay(
        String name,
        Integer stars,
        int nights,
        NamedRef destination,
        String description,
        String image) {
}
