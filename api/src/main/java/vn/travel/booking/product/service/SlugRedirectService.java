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

    private final SlugRedirectRepository slugRedirectRepository;

    public SlugRedirectService(SlugRedirectRepository slugRedirectRepository) {
        this.slugRedirectRepository = slugRedirectRepository;
    }

    @Transactional(readOnly = true)
    public String resolve(String market, String locale, String type, String slug) {
        Optional<String> newSlug = switch (type) {
            case "PRODUCT" -> slugRedirectRepository.product(market, locale, slug);
            case "DESTINATION" -> slugRedirectRepository.destination(market, locale, slug);
            default -> throw new IllegalArgumentException("loại lạ: " + type);
        };

        return newSlug.orElseThrow(() -> new NotFoundException(
                "slug cũ " + type + "/" + slug + " ở " + market + "/" + locale));
    }
}
