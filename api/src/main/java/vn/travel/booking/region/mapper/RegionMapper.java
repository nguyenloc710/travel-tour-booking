package vn.travel.booking.region.mapper;

import org.mapstruct.Mapper;
import vn.travel.booking.region.dto.RegionSummary;
import vn.travel.booking.web.generated.model.Region;

@Mapper(componentModel = "spring")
public interface RegionMapper {

    Region toView(RegionSummary summary);
}
