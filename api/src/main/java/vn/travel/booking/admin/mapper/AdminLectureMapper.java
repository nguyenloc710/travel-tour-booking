package vn.travel.booking.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.LectureDetailView;
import vn.travel.booking.admin.dto.LectureRow;
import vn.travel.booking.admin.dto.LectureTranslationView;
import vn.travel.booking.common.dto.PagedResult;
import vn.travel.booking.web.generated.model.AdminLectureDetail;
import vn.travel.booking.web.generated.model.AdminLecturePage;
import vn.travel.booking.web.generated.model.AdminLectureSummary;
import vn.travel.booking.web.generated.model.AdminLectureTranslation;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(componentModel = "spring", uses = {ContentLocaleStateMapper.class})
public interface AdminLectureMapper {

    @Mapping(target = "locales", source = "locales")
    @Mapping(target = "startTime", expression = "java(timeText(l.startTime()))")
    AdminLectureSummary toSummary(LectureRow l);

    List<AdminLectureSummary> toSummaryList(List<LectureRow> lectures);

    @Mapping(target = "startTime", expression = "java(timeText(l.startTime()))")
    AdminLectureDetail toDetail(LectureDetailView l);

    AdminLectureTranslation toTranslation(LectureTranslationView t);

    List<AdminLectureTranslation> toTranslationList(List<LectureTranslationView> translations);

    default AdminLecturePage toPage(PagedResult<LectureRow> result) {
        if (result == null) {
            return null;
        }
        return new AdminLecturePage(
                toSummaryList(result.items()),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages());
    }

    default String timeText(LocalTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.MINUTES).toString();
    }

    default AdminLectureSummary.MarketEnum toSummaryMarket(String market) {
        return market == null ? null : AdminLectureSummary.MarketEnum.fromValue(market);
    }

    default AdminLectureDetail.MarketEnum toDetailMarket(String market) {
        return market == null ? null : AdminLectureDetail.MarketEnum.fromValue(market);
    }
}
