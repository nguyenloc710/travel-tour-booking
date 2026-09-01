package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.AdminProductQuery;
import vn.travel.booking.admin.dto.AdminProductRow;
import vn.travel.booking.admin.repository.AdminCatalogRepository;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.repository.LocaleRepository;

/**
 * Danh sách sản phẩm cho trang quản trị (docs/22 M2).
 */
@Service
public class AdminCatalogService {

    private final AdminCatalogRepository danhMuc;
    private final LocaleRepository locale;

    public AdminCatalogService(AdminCatalogRepository danhMuc, LocaleRepository locale) {
        this.danhMuc = danhMuc;
        this.locale = locale;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminProductRow> danhSach(AdminProductQuery query) {
        return danhMuc.findProducts(locale.localeNguon(), query);
    }
}
