package vn.travel.booking.admin.dto;

import java.time.LocalDate;

/**
 * Một hành khách của đơn (docs/22 M7).
 *
 * <p>Hộ chiếu và quốc tịch có mặt ở đây vì đó là thứ nhân viên cần khi làm visa.
 * Chúng <b>không</b> có ở bề mặt công khai tra đơn — khách đã biết hộ chiếu của
 * mình, còn ai dò được mã đơn thì không nên đọc được số hộ chiếu người khác.
 */
public record BookingPassengerRow(
        int seq,
        String paxTypeCode,
        String fullName,
        LocalDate dateOfBirth,
        String passportNo,
        LocalDate passportExpiry,
        String nationality) {
}
