package vn.travel.booking.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.admin.dto.CoverageRow;
import vn.travel.booking.admin.dto.QueueItem;
import vn.travel.booking.admin.repository.TranslationWorkRepository;
import vn.travel.booking.common.repository.LocaleRepository;

import java.util.List;

/**
 * Hàng đợi dịch và bảng độ phủ (docs/22 M10, M12).
 *
 * <p>Hai màn hình này là thứ trả lời câu hỏi <i>"còn phải làm gì"</i> mà không
 * ai phát hiện nếu không có màn hình nhắc — cùng loại với ba việc trên bảng điều
 * khiển M1, và cả ba đều mất tiền thật khi bị bỏ sót.
 */
@Service
public class TranslationWorkService {

    private final TranslationWorkRepository translationWorkRepository;
    private final LocaleRepository locale;

    public TranslationWorkService(TranslationWorkRepository translationWorkRepository, LocaleRepository locale) {
        this.translationWorkRepository = translationWorkRepository;
        this.locale = locale;
    }

    @Transactional(readOnly = true)
    public List<QueueItem> queue(String entityType, int limit) {
        return translationWorkRepository.queue(locale.sourceLocale(), entityType, limit);
    }

    /**
     * @param locale locale đích, hoặc {@code null} để lấy mọi locale phải dịch
     */
    @Transactional(readOnly = true)
    public List<CoverageRow> coverage(String locale) {
        return translationWorkRepository.coverage(this.locale.sourceLocale(), locale);
    }
}
