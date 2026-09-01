package vn.travel.booking.application.product;

/**
 * Cách sắp xếp listing.
 *
 * <p>{@code TITLE_*} sắp theo <b>collation của locale đang xem</b>, ở tầng CSDL
 * chứ không ở Java: {@code String.compareTo()} xếp {@code øst} sau {@code zoo}
 * theo mã Unicode một cách tình cờ đúng, nhưng xếp {@code Ørsted} trước
 * {@code Aarhus} sai hoàn toàn với người Đan Mạch.
 */
public enum ProductSort {
    TITLE_ASC,
    TITLE_DESC,
    PRICE_FROM_ASC,
    PRICE_FROM_DESC,
    DURATION_DAYS_ASC,
    DURATION_DAYS_DESC
}
