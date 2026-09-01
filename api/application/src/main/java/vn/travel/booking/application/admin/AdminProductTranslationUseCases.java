package vn.travel.booking.application.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.shared.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sửa bản dịch sản phẩm — đường ghi đầu tiên của hệ thống.
 *
 * <p><b>Quyền ở đây phụ thuộc cả vai trò lẫn locale đang sửa</b>, nên nó không
 * diễn đạt hết được bằng {@code @PreAuthorize} ở controller. Hai tầng, hai việc
 * khác nhau:
 *
 * <ul>
 *   <li>{@code @PreAuthorize} ở controller: <b>ai được vào cửa</b> — phải mang
 *       một trong ba vai trò
 *   <li>Lớp này: <b>vào rồi thì được sửa cái gì</b> — người dịch không sửa bản
 *       nguồn, người viết không sửa bản dịch
 * </ul>
 *
 * <p>Đặt luật thứ hai ở controller là để nó nằm ngoài tầm test của tầng nghiệp
 * vụ; đặt luật thứ nhất ở đây là để mọi endpoint quản trị phải tự nhớ kiểm.
 */
@Service
public class AdminProductTranslationUseCases {

    private final AdminProductTranslationPort banDich;

    public AdminProductTranslationUseCases(AdminProductTranslationPort banDich) {
        this.banDich = banDich;
    }

    @Transactional(readOnly = true)
    public List<ProductTranslationView> danhSach(UUID productId) {
        phaiTonTai(productId);
        return banDich.findAll(productId);
    }

    /**
     * @param sourceLocale ngôn ngữ nguồn của hệ thống — ADR-004, hiện là {@code da}
     * @param roles        vai trò của người đang đăng nhập
     */
    @Transactional
    public ProductTranslationView luu(UUID productId, String locale, String sourceLocale,
                                      Set<String> roles, ProductTranslationInput input) {
        phaiTonTai(productId);

        boolean laNguon = sourceLocale.equals(locale);
        boolean duocPhep = roles.contains("ADMIN")
                || (laNguon ? roles.contains("EDITOR") : roles.contains("TRANSLATOR"));

        if (!duocPhep) {
            throw new ForbiddenException(laNguon
                    ? "chỉ EDITOR hoặc ADMIN sửa được bản ngôn ngữ nguồn"
                    : "chỉ TRANSLATOR hoặc ADMIN sửa được bản dịch");
        }

        return banDich.save(productId, locale, input);
    }

    private void phaiTonTai(UUID productId) {
        if (!banDich.productExists(productId)) {
            throw new NotFoundException("product id=" + productId);
        }
    }
}
