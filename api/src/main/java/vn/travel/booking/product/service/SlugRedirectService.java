package vn.travel.booking.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.product.repository.SlugRedirectRepository;

import java.util.Optional;

/**
 * Giải một slug cũ thành slug hiện tại.
 *
 * <p>Không tìm thấy thì ném {@link NotFoundException} — cùng một {@code 404} với
 * mọi tình huống khác của bề mặt công khai. Đây là chỗ dễ bị cám dỗ trả một mã
 * riêng "slug này không phải slug cũ", và không nên: phân biệt ra ngoài là cho
 * phép dò xem slug nào từng tồn tại.
 */
@Service
public class SlugRedirectService {

    private final SlugRedirectRepository slugCu;

    public SlugRedirectService(SlugRedirectRepository slugCu) {
        this.slugCu = slugCu;
    }

    @Transactional(readOnly = true)
    public String giai(String market, String locale, String loai, String slug) {
        Optional<String> moi = switch (loai) {
            case "PRODUCT" -> slugCu.product(market, locale, slug);
            case "DESTINATION" -> slugCu.destination(market, locale, slug);
            default -> throw new IllegalArgumentException("loại lạ: " + loai);
        };

        return moi.orElseThrow(() -> new NotFoundException(
                "slug cũ " + loai + "/" + slug + " ở " + market + "/" + locale));
    }
}
