package vn.travel.booking.application.market;

public interface MarketQueryPort {

    /** Thị trường có tồn tại và đang bật bán hay không. */
    boolean isActive(String market);
}
