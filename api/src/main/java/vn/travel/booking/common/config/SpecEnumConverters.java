package vn.travel.booking.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import vn.travel.booking.web.generated.model.ProductSort;

/**
 * Dạy Spring đọc những enum của spec có <b>giá trị khác tên hằng</b>.
 *
 * <p>{@code ProductSort} mang giá trị {@code "title,asc"} nhưng tên hằng là
 * {@code TITLE_ASC}. Mặc định Spring chuyển tham số truy vấn sang enum
 * <b>bằng tên hằng</b>, nên mọi yêu cầu — kể cả yêu cầu không truyền {@code sort},
 * vì spec có giá trị mặc định {@code title,asc} — đều đỏ với 400 trước khi vào
 * tới controller. Triệu chứng đánh lừa: mọi thứ trả {@code VALIDATION_FAILED} mà
 * tham số nhìn thì đúng hết.
 *
 * <p>{@code ProductType} không cần bộ chuyển: giá trị và tên hằng trùng nhau.
 */
@Configuration
public class SpecEnumConverters {

    @Bean
    public Converter<String, ProductSort> productSortConverter() {
        return ProductSort::fromValue;
    }
}
