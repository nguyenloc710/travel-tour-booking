package vn.travel.booking.domain.pricing;

import vn.travel.booking.domain.shared.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Đầu vào của engine giá.
 *
 * <p><b>Mọi hằng số là tham số.</b> Tỷ lệ đặt cọc, phí xử lý, số chữ số thập
 * phân, mức giảm đặt sớm — không cái nào engine tự biết. Đó là lý do engine viết
 * được ngay cả khi sáu con số nghiệp vụ của thị trường {@code VN} còn chưa ai
 * quyết (docs/14 mục 2.4): thiếu <b>dữ liệu cấu hình</b>, không thiếu logic.
 *
 * <p>Dòng nào không áp dụng cho loại sản phẩm đang tính thì để trống — nó sẽ
 * không xuất hiện trong bảng phân rã, chứ không hiện thành một dòng {@code 0}.
 */
public final class PricingInput {

    private final List<PaxLine> pax;
    private final int fractionDigits;
    private final BigDecimal depositRate;

    private int singleTravellers;
    private Money singleSupplementPerPerson;
    private Money cabinUpgradePerPerson;
    private Money departureOriginSurchargePerPerson;
    private Money insurancePerPerson;
    private int preTourHotelRooms;
    private int preTourHotelNights;
    private Money preTourHotelPricePerRoomNight;
    private Money earlyBirdPerPerson;
    private Money processingFee;

    private PricingInput(List<PaxLine> pax, int fractionDigits, BigDecimal depositRate) {
        if (pax == null || pax.isEmpty()) {
            throw new IllegalArgumentException("Đơn phải có ít nhất một loại khách");
        }
        this.pax = List.copyOf(pax);
        this.fractionDigits = fractionDigits;
        this.depositRate = depositRate;
    }

    /**
     * @param fractionDigits lấy từ {@code market.fraction_digits} — DKK 2, VND 0.
     *                       Không hardcode ở bất kỳ đâu (docs/14 mục 3 quy tắc 1)
     * @param depositRate    lấy từ {@code market.deposit_rate}
     */
    public static PricingInput cua(List<PaxLine> pax, int fractionDigits, BigDecimal depositRate) {
        return new PricingInput(pax, fractionDigits, depositRate);
    }

    /** Phụ thu phòng đơn = (giá phòng đơn − giá phòng đôi) × số khách ở một mình. */
    public PricingInput phongDon(int soKhachOMotMinh, Money chenhLechMoiNguoi) {
        this.singleTravellers = soKhachOMotMinh;
        this.singleSupplementPerPerson = chenhLechMoiNguoi;
        return this;
    }

    /** Chỉ {@code CRUISE}: chênh giá hạng đã chọn so với hạng thấp nhất của đúng ngày đó. */
    public PricingInput nangHangCabin(Money chenhMoiNguoi) {
        this.cabinUpgradePerPerson = chenhMoiNguoi;
        return this;
    }

    /** Chỉ thị trường có nhiều điểm khởi hành. */
    public PricingInput phuThuDiemKhoiHanh(Money moiNguoi) {
        this.departureOriginSurchargePerPerson = moiNguoi;
        return this;
    }

    public PricingInput baoHiem(Money moiNguoi) {
        this.insurancePerPerson = moiNguoi;
        return this;
    }

    public PricingInput demKhachSanTruocBay(int soPhong, int soDem, Money giaMoiPhongMoiDem) {
        this.preTourHotelRooms = soPhong;
        this.preTourHotelNights = soDem;
        this.preTourHotelPricePerRoomNight = giaMoiPhongMoiDem;
        return this;
    }

    /** Mức giảm mỗi người; tính bằng {@link EarlyBird}. */
    public PricingInput giamDatSom(Money moiNguoi) {
        this.earlyBirdPerPerson = moiNguoi;
        return this;
    }

    /** Cố định một lần mỗi đơn, không nhân theo số khách. */
    public PricingInput phiXuLy(Money moiDon) {
        this.processingFee = moiDon;
        return this;
    }

    // ------------------------------------------------------------ đọc

    List<PaxLine> pax() {
        return pax;
    }

    int fractionDigits() {
        return fractionDigits;
    }

    BigDecimal depositRate() {
        return depositRate;
    }

    int tongSoKhach() {
        return pax.stream().mapToInt(PaxLine::count).sum();
    }

    String currency() {
        return pax.get(0).unitPrice().currency();
    }

    List<PaxLine> paxCopy() {
        return new ArrayList<>(pax);
    }

    int singleTravellers() {
        return singleTravellers;
    }

    Money singleSupplementPerPerson() {
        return singleSupplementPerPerson;
    }

    Money cabinUpgradePerPerson() {
        return cabinUpgradePerPerson;
    }

    Money departureOriginSurchargePerPerson() {
        return departureOriginSurchargePerPerson;
    }

    Money insurancePerPerson() {
        return insurancePerPerson;
    }

    int preTourHotelRooms() {
        return preTourHotelRooms;
    }

    int preTourHotelNights() {
        return preTourHotelNights;
    }

    Money preTourHotelPricePerRoomNight() {
        return preTourHotelPricePerRoomNight;
    }

    Money earlyBirdPerPerson() {
        return earlyBirdPerPerson;
    }

    Money processingFee() {
        return processingFee;
    }
}
