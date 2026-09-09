package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.AdminProductQuery;
import vn.travel.booking.admin.dto.AdminProductRow;
import vn.travel.booking.admin.dto.DestinationOption;
import vn.travel.booking.admin.repository.AdminCatalogRepository;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.repository.LocaleRepository;

import java.util.List;

/**
 * Danh sách sản phẩm cho trang quản trị (docs/22 M2).
 */
@Service
public class AdminCatalogService {

    private final AdminCatalogRepository catalog;
    private final LocaleRepository locale;

    public AdminCatalogService(AdminCatalogRepository catalog, LocaleRepository locale) {
        this.catalog = catalog;
        this.locale = locale;
    }

    @Transactional(readOnly = true)
    public PagedResult<AdminProductRow> list(AdminProductQuery query) {
        return catalog.findProducts(locale.sourceLocale(), query);
    }

    @Transactional(readOnly = true)
    public List<DestinationOption> destination() {
        return catalog.findDestinations(locale.sourceLocale());
    }
}
