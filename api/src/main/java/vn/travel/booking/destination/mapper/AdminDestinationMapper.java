package vn.travel.booking.destination.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.admin.dto.DestinationDetailView;
import vn.travel.booking.admin.dto.DestinationOption;
import vn.travel.booking.admin.dto.DestinationTranslationView;
import vn.travel.booking.web.generated.model.AdminDestination;
import vn.travel.booking.web.generated.model.AdminDestinationDetail;
import vn.travel.booking.web.generated.model.AdminDestinationTranslation;

/**
 * Điểm đến sang DTO của hợp đồng — phần điểm đến của M13 ({@code docs/22}).
 */
@Mapper(componentModel = "spring")
public interface AdminDestinationMapper {

    /**
     * Miền chưa có bản dịch ở ngôn ngữ nguồn thì trả chuỗi rỗng thay vì
     * {@code null}: đó là dữ liệu thiếu, không phải lỗi lập trình, và màn hình
     * vẫn phải mở được để người ta vào sửa.
     */
    @Mapping(target = "regionName", source = "regionName", defaultValue = "")
    AdminDestinationDetail toDetail(DestinationDetailView view);

    AdminDestinationTranslation toTranslation(DestinationTranslationView view);

    AdminDestination toOption(DestinationOption option);
}
