package vn.travel.booking.common.exception;

/**
 * Số khách không rơi vào bậc giá nào.
 *
 * <p>Ném lỗi chứ <b>không lấy bậc gần nhất</b>: bảng bậc là thứ nhân viên nhập,
 * và một khoảng trống trong đó là lỗi dữ liệu cần thấy, không phải chuyện để
 * đoán giùm. Đoán giùm nghĩa là bán một mức giá không ai từng duyệt.
 *
 * <p>Ánh xạ ra mã lỗi {@code PARTY_SIZE_OUT_OF_RANGE} của docs/13 mục 5.1.
 */
public class PartySizeOutOfRangeException extends RuntimeException {

    private final int partySize;

    public PartySizeOutOfRangeException(int partySize) {
        super("Số khách ngoài mọi bậc giá: " + partySize);
        this.partySize = partySize;
    }

    public int partySize() {
        return partySize;
    }
}
