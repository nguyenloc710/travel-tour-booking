package vn.travel.booking.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Mô tả hiển thị trên Swagger UI.
 *
 * <p><b>Spec ở đây KHÔNG phải {@code contracts/openapi.yaml}.</b> springdoc đọc
 * annotation trên controller rồi sinh ra một spec riêng ở {@code /v3/api-docs}.
 * Từ khi controller thôi {@code implements} interface sinh ra, hai bản mô tả API
 * cùng tồn tại và <b>không có gì bắt chúng khớp nhau</b>:
 *
 * <ul>
 *   <li>{@code contracts/openapi.yaml} — vẫn là thứ sinh model Java và TS client,
 *       vẫn là thứ {@code pnpm contracts:check} kiểm tương thích ngược.</li>
 *   <li>{@code /v3/api-docs} — ảnh chụp của code đang chạy.</li>
 * </ul>
 *
 * <p>Lệch nhau thì bản đúng là bản ở {@code contracts/}, vì frontend sinh client
 * từ đó. Đổi endpoint mà quên cập nhật file spec là lỗi phải bắt bằng mắt lúc rà
 * soát mã — trước đây trình biên dịch bắt hộ.
 */
@Configuration
public class OpenApiConfig {

    private final String version;

    public OpenApiConfig(@Value("${spring.application.version:0.1.0-SNAPSHOT}") String version) {
        this.version = version;
    }

    @Bean
    OpenAPI travelOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Tour Booking API")
                        .version(version)
                        .description("""
                                API bán tour du lịch Việt Nam cho hai thị trường.

                                **Market và Locale là hai thứ khác nhau.** `market` nằm trong \
                                đường dẫn và quyết định khách *mua* gì — catalog, giá, tiền tệ. \
                                `Accept-Language` quyết định khách *đọc* bằng tiếng gì. Một khách \
                                Việt sống ở Đan Mạch mua ở `dk` nhưng đọc `vi`.

                                **Tiền trả về dạng chuỗi**, kèm mã tiền tệ: \
                                `{"amount":"24990.00","currency":"DKK"}`. Không định dạng sẵn.

                                **Lỗi trả mã, không trả câu tiếng người** — frontend dịch.

                                Nội dung thiếu bản dịch cho locale đang xem thì **404**, \
                                không lùi về ngôn ngữ nguồn.""")
                        .license(new License().name("Sở hữu nội bộ")))
                .servers(List.of(
                        new Server().url("/").description("Máy chủ đang phục vụ trang này")));
    }
}
