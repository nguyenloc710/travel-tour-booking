package vn.travel.booking.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.market.service.MarketService;
import vn.travel.booking.product.dto.ProductDetail;
import vn.travel.booking.product.dto.ProductQuery;
import vn.travel.booking.product.dto.ProductSummary;
import vn.travel.booking.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository products;
    private final MarketService markets;

    public ProductService(ProductRepository products, MarketService markets) {
        this.products = products;
        this.markets = markets;
    }

    @Transactional(readOnly = true)
    public PagedResult<ProductSummary> list(ProductQuery query) {
        markets.requireActive(query.market());
        return products.findProducts(query);
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(String market, String locale, String slug) {
        markets.requireActive(market);
        return products.findProduct(market, locale, slug)
                .orElseThrow(() -> new NotFoundException(
                        "product slug=" + slug + " market=" + market + " locale=" + locale));
    }
}
