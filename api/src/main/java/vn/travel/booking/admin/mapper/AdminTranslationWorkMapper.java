package vn.travel.booking.admin.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.admin.dto.CoverageRow;
import vn.travel.booking.admin.dto.QueueItem;
import vn.travel.booking.web.generated.model.TranslationCoverageRow;
import vn.travel.booking.web.generated.model.TranslationEntityType;
import vn.travel.booking.web.generated.model.TranslationGap;
import vn.travel.booking.web.generated.model.TranslationQueueItem;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminTranslationWorkMapper {

    TranslationQueueItem toQueueItem(QueueItem q);

    List<TranslationQueueItem> toQueueItemList(List<QueueItem> items);

    TranslationCoverageRow toCoverageRow(CoverageRow c);

    List<TranslationCoverageRow> toCoverageRowList(List<CoverageRow> rows);

    default TranslationEntityType toEntityType(String entityType) {
        return entityType == null ? null : TranslationEntityType.fromValue(entityType);
    }

    default TranslationGap toGap(String gap) {
        return gap == null ? null : TranslationGap.fromValue(gap);
    }
}
