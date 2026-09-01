package vn.travel.booking.application.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;
import vn.travel.booking.application.shared.PagedResult;

@Service
public class ListProductsUseCase {

    private final ProductQueryPort products;
    private final Markets markets;

    public ListProductsUseCase(ProductQueryPort products, Markets markets) {
        this.products = products;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public PagedResult<ProductSummary> execute(ProductQuery query) {
        markets.requireActive(query.market());
        return products.findProducts(query);
    }
}
