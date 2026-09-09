package vn.travel.booking.quote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.common.exception.QuoteErrors;
import vn.travel.booking.market.service.MarketService;
import vn.travel.booking.quote.dto.QuoteProduct;
import vn.travel.booking.quote.dto.QuoteReceiptView;
import vn.travel.booking.quote.dto.QuoteRequestCommand;
import vn.travel.booking.quote.repository.QuoteRepository;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Khách gửi yêu cầu báo giá — docs/23 mục 7.
 *
 * <p><b>Không giữ chỗ ở bất kỳ bước nào</b> (điều 1 của mục đó): tour riêng
 * không dùng tồn kho chung, nên không có gì để giữ và không có gì hết hạn. Đó là
 * khác biệt lớn nhất so với {@code BookingService}, và là lý do lớp này ngắn
 * đến vậy.
 */
@Service
public class QuoteService {

    /** Chỉ loại này đi qua luồng báo giá — docs/04. */
    private static final String QUOTABLE_PRODUCT_TYPE = "PRIVATE_TOUR";

    private final QuoteRepository quote;
    private final MarketService markets;
    private final Clock clock;

    public QuoteService(QuoteRepository quote, MarketService markets, Clock clock) {
        this.quote = quote;
        this.markets = markets;
        this.clock = clock;
    }

    /**
     * Nhận một yêu cầu và sinh {@code quote} ở {@code DRAFT}.
     *
     * <p>Ba lần từ chối, theo đúng thứ tự này:
     *
     * <ol>
     *   <li><b>404</b> khi không tra ra sản phẩm. Gộp ba tình huống như mọi chỗ
     *       khác: không có, chưa dịch cho locale này, chưa gán thị trường này.
     *   <li><b>422 {@code PRODUCT_NOT_QUOTABLE}</b> khi loại sản phẩm đặt thẳng
     *       được. Sau bước 1 chứ không trước: trả "loại này không hỏi giá" cho
     *       một tour chưa mở bán là xác nhận rằng tour đó tồn tại.
     *   <li><b>422 {@code LEAD_TIME_NOT_MET}</b> khi ngày yêu cầu quá gần —
     *       docs/14 mục 7 quy tắc 1.
     * </ol>
     */
    @Transactional
    public QuoteReceiptView submitRequest(String market, String locale, QuoteRequestCommand command) {
        markets.requireActive(market);

        QuoteProduct product = quote.findProduct(market, locale, command.productSlug())
                .orElseThrow(() -> new NotFoundException(
                        "product slug=" + command.productSlug() + " locale=" + locale));

        if (!QUOTABLE_PRODUCT_TYPE.equals(product.productType())) {
            throw new QuoteErrors.ProductNotQuotable(
                    "loại " + product.productType() + " đặt thẳng được, không đi qua báo giá");
        }

        validateDate(product, command.requestedDate());

        return quote.createRequest(market, locale, product.id(), command);
    }

    /**
     * Ngày yêu cầu phải cách hôm nay ít nhất {@code leadTimeDays}.
     *
     * <p>Frontend đã chặn ở lịch chọn, và <b>vẫn kiểm lại ở đây</b>: chặn ở một
     * phía là chặn được đúng những người dùng trình duyệt.
     *
     * <p>Khách chưa chốt ngày thì không có gì để kiểm — bắt điền một ngày giả để
     * qua form là cách chắc chắn nhất để có dữ liệu sai.
     */
    private void validateDate(QuoteProduct product, LocalDate requestedAt) {
        if (requestedAt == null) {
            return;
        }
        LocalDate earliestDate = LocalDate.now(clock).plusDays(product.leadTimeDays());
        if (requestedAt.isBefore(earliestDate)) {
            throw new QuoteErrors.LeadTimeNotMet(product.leadTimeDays(), earliestDate);
        }
    }
}
