package vn.travel.booking.application.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.application.market.Markets;
import vn.travel.booking.application.shared.NotFoundException;

@Service
public class GetProductUseCase {

    private final ProductQueryPort products;
    private final Markets markets;

    public GetProductUseCase(ProductQueryPort products, Markets markets) {
        this.products = products;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public ProductDetail execute(String market, String locale, String slug) {
        markets.requireActive(market);
        return products.findProduct(market, locale, slug)
                .orElseThrow(() -> new NotFoundException(
                        "product slug=" + slug + " market=" + market + " locale=" + locale));
    }
}
