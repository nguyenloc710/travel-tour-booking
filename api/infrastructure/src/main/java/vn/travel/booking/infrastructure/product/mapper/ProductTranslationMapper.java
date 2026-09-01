package vn.travel.booking.infrastructure.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.travel.booking.application.admin.ProductTranslationInput;
import vn.travel.booking.application.admin.ProductTranslationView;
import vn.travel.booking.infrastructure.product.entity.ProductTranslationEntity;

import java.util.List;

/**
 * Entity sang bản ghi của tầng application, và ngược lại — bằng MapStruct, không
 * map tay trong service (khuôn mượn của dự án trước, docs/15 mục 2).
 *
 * <p>Lý do không map tay: thêm một trường vào entity rồi quên thêm vào mapper
 * thì trường đó lặng lẽ không bao giờ được ghi. MapStruct sinh mã lúc biên dịch
 * và <b>báo cảnh báo cho mọi trường không được ánh xạ</b>, nên "quên" trở thành
 * thứ nhìn thấy được.
 *
 * <p>Hai trường phải chỉ định tay vì chúng không có ở nguồn:
 * {@code isSource} và {@code outdated} là giá trị <b>tính ra</b> theo cấu hình
 * ngôn ngữ nguồn, không phải cột — nên chúng được điền ở adapter.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductTranslationMapper {

    @Mapping(target = "isSource", ignore = true)
    @Mapping(target = "outdated", ignore = true)
    ProductTranslationView sangView(ProductTranslationEntity entity);

    /**
     * Ghi dữ liệu vào entity đang có. Cố tình <b>không</b> chạm tới khoá chính,
     * cột kiểm toán và cặp {@code translated_*}: khoá chính là danh tính, cột
     * kiểm toán do JPA auditing và trigger lo, còn "đã dịch lúc nào" là một sự
     * kiện riêng chứ không phải một lần lưu bất kỳ.
     */
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "locale", ignore = true)
    @Mapping(target = "translatedAt", ignore = true)
    @Mapping(target = "translatedBy", ignore = true)
    @Mapping(target = "softDelete", ignore = true)
    void ghiVao(ProductTranslationInput input, @MappingTarget ProductTranslationEntity entity);

    default String[] sangMang(List<String> danhSach) {
        return danhSach == null ? null : danhSach.toArray(String[]::new);
    }

    default List<String> sangDanhSach(String[] mang) {
        return mang == null ? List.of() : List.of(mang);
    }
}
