package vn.travel.booking.destination.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.travel.booking.destination.dto.DestinationSummary;
import vn.travel.booking.product.dto.NamedRef;
import vn.travel.booking.web.generated.model.Destination;
import vn.travel.booking.web.generated.model.GalleryImage;
import vn.travel.booking.web.generated.model.Ref;

@Mapper(componentModel = "spring")
public interface DestinationMapper {

    @Mapping(target = "region", source = "region")
    @Mapping(target = "image", source = "image")
    Destination toView(DestinationSummary summary);

    Ref toRef(NamedRef ref);

    GalleryImage toImage(vn.travel.booking.product.dto.GalleryImage image);
}
