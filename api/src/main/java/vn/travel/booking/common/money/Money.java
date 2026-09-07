package vn.travel.booking.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Một khoản tiền kèm mã tiền tệ.
 *
 * <p>Luôn {@code BigDecimal}, không bao giờ {@code double} hay {@code float}.
 * Số chữ số thập phân lấy từ cấu hình thị trường ({@code market.fraction_digits}),
 * không hardcode: VND có 0, DKK có 2.
 *
 * <p>Lớp này thuộc module {@code domain} nên không biết Spring, không biết JSON,
 * và <b>không định dạng tiền</b>. Định dạng là việc của frontend.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Mã tiền tệ phải đúng 3 ký tự: " + currency);
        }
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    /**
     * Làm tròn HALF_UP theo số chữ số thập phân của thị trường.
     *
     * <p>Làm tròn ở <b>từng dòng</b> rồi mới cộng — docs/14 mục 3 quy tắc 3.
     * Cộng số chưa tròn rồi tròn một lần cho tổng đẹp hơn về mặt toán học, nhưng
     * khi đó bảng phân rã cộng lại không bằng tổng, và khách nhìn thấy ngay.
     */
    public Money round(int fractionDigits) {
        return new Money(amount.setScale(fractionDigits, RoundingMode.HALF_UP), currency);
    }

    /**
     * Đặt cọc làm tròn <b>xuống</b> — docs/14 mục 3 quy tắc 4.
     * Phần còn lại luôn tính bằng {@code tổng − đặt cọc}, không tính riêng, để
     * {@code deposit + balance = total} đúng tuyệt đối.
     */
    public Money depositAt(BigDecimal rate, int fractionDigits) {
        return new Money(amount.multiply(rate).setScale(fractionDigits, RoundingMode.FLOOR), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Không cộng trừ được hai loại tiền khác nhau: " + currency + " và " + other.currency);
        }
    }
}
