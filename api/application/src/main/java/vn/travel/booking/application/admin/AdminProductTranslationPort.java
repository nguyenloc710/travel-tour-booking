package vn.travel.booking.application.admin;

import java.util.List;
import java.util.UUID;

public interface AdminProductTranslationPort {

    boolean productExists(UUID productId);

    List<ProductTranslationView> findAll(UUID productId);

    ProductTranslationView save(UUID productId, String locale, ProductTranslationInput input);
}
