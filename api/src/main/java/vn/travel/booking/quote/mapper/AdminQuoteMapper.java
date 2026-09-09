package vn.travel.booking.quote.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.common.mapper.RefMapper;
import vn.travel.booking.common.money.Money;
import vn.travel.booking.quote.dto.AdminQuoteDetailView;
import vn.travel.booking.quote.dto.AdminQuoteRow;
import vn.travel.booking.quote.dto.QuoteLineRow;
import vn.travel.booking.web.generated.model.AdminQuoteDetail;
import vn.travel.booking.web.generated.model.AdminQuoteSummary;
import vn.travel.booking.web.generated.model.QuoteLine;
import vn.travel.booking.web.generated.model.QuoteStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Báo giá sang DTO của hợp đồng — M8 của {@code docs/22}.
 *
 * <p><b>Chi tiết = tóm tắt + bốn trường.</b> Lớp sinh ra làm phẳng {@code allOf}
 * thành một lớp duy nhất, nên mười một trường của tóm tắt phải có mặt lần nữa
 * trong chi tiết. Bản map tay chép chúng bằng tay; ở đây
 * {@code @Mapping(target = ".", source = "tomTat")} nói đúng ý đó một lần, và
 * thêm một trường vào {@link AdminQuoteRow} thì cả hai đích cùng nhận được.
 */
@Mapper(componentModel = "spring")
public interface AdminQuoteMapper {

    AdminQuoteSummary toSummary(AdminQuoteRow row);

    @Mapping(target = ".", source = "summary")
    AdminQuoteDetail toDetail(AdminQuoteDetailView view);

    QuoteLine toLine(QuoteLineRow row);

    vn.travel.booking.quote.dto.QuoteLineDraft toDraft(vn.travel.booking.web.generated.model.QuoteLineInput line);

    List<vn.travel.booking.quote.dto.QuoteLineDraft> toDraftList(List<vn.travel.booking.web.generated.model.QuoteLineInput> lines);

    default QuoteStatus toStatus(vn.travel.booking.quote.dto.QuoteStatus status) {
        return status == null ? null : QuoteStatus.fromValue(status.name());
    }

    default AdminQuoteSummary.MarketEnum toSummaryMarket(String market) {
        return market == null ? null : AdminQuoteSummary.MarketEnum.fromValue(market);
    }

    default AdminQuoteDetail.MarketEnum toDetailMarket(String market) {
        return market == null ? null : AdminQuoteDetail.MarketEnum.fromValue(market);
    }

    default vn.travel.booking.web.generated.model.Money toMoney(Money money) {
        return RefMapper.toMoney(money);
    }

    /**
     * Số lượng đi ra JSON dạng <b>chuỗi</b>, cùng lý do với tiền: số thập phân
     * qua JSON là nơi chữ số cuối biến mất mà không ai thấy
     * ({@code CLAUDE.md} quy tắc 6).
     */
    default String toDecimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    default BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
