package vn.travel.booking.admin.dto;

/**
 * @param created số ngày khởi hành mới — <b>chưa có giá</b>
 * @param skipped số ngày đã tồn tại ở thị trường đích nên bỏ qua
 */
public record CopyResult(int created, int skipped) {
}
