package vn.travel.booking.itinerary.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.travel.booking.common.exception.AdminErrors;
import vn.travel.booking.common.exception.ForbiddenException;
import vn.travel.booking.common.exception.NotFoundException;
import vn.travel.booking.itinerary.dto.ItineraryViews;
import vn.travel.booking.itinerary.entity.ItineraryDayEntity;
import vn.travel.booking.itinerary.entity.ItineraryDayTranslationEntity;
import vn.travel.booking.itinerary.repository.ItineraryDayRepository;
import vn.travel.booking.itinerary.repository.ItineraryDayTranslationRepository;
import vn.travel.booking.itinerary.repository.ItineraryRefRepository;
import vn.travel.booking.product.entity.ProductEntity;
import vn.travel.booking.product.repository.ProductWriteRepository;
import vn.travel.booking.web.generated.model.FieldRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lịch trình từng ngày — đường ghi của trang quản trị (docs/22 M3).
 *
 * <p><b>Hai loại sản phẩm không có lịch trình theo ngày</b>: {@code COMBO} là bộ
 * cố định không có lộ trình (ADR-009), {@code DAY_TOUR} chỉ có một ngày và mô tả
 * của nó nằm ngay trong bản dịch sản phẩm. Với hai loại đó, tài nguyên này
 * không tồn tại — {@code 404}, cùng mã với bề mặt công khai.
 *
 * <p><b>Vì sao thay cả mảng chứ không sửa từng ngày.</b> Lịch trình là một mảng
 * có ràng buộc giữa các phần tử: số ngày phải bằng {@code durationDays}
 * (quy tắc 3 của docs/12 mục 9), và {@code dayNumber} phải liền mạch từ 1. Cả
 * hai luật chỉ kiểm được khi nhìn cả mảng. Sửa từng ngày nghĩa là hệ thống có
 * những khoảnh khắc hợp lệ dở dang mà không ai định nghĩa.
 */
@Service
public class ItineraryService {

    /** Hai loại không có lộ trình theo ngày. */
    private static final Set<String> KHONG_CO_LICH_TRINH = Set.of("COMBO", "DAY_TOUR");

    private final ProductWriteRepository products;
    private final ItineraryDayRepository days;
    private final ItineraryDayTranslationRepository texts;
    private final ItineraryRefRepository refs;

    public ItineraryService(ProductWriteRepository products, ItineraryDayRepository days,
                            ItineraryDayTranslationRepository texts, ItineraryRefRepository refs) {
        this.products = products;
        this.days = days;
        this.texts = texts;
        this.refs = refs;
    }

    @Transactional(readOnly = true)
    public ItineraryViews.Itinerary get(UUID productId, String sourceLocale) {
        requireProductWithItinerary(productId);
        return view(productId, sourceLocale);
    }

    /**
     * Thay toàn bộ lịch trình, kèm bản ngôn ngữ nguồn.
     *
     * <p>Ngày còn trong mảng thì <b>giữ nguyên {@code id}</b> — bản dịch treo vào
     * id đó, và tạo lại id mỗi lần lưu là mỗi lần lưu xoá sạch công của người
     * dịch. Ngày biến mất khỏi mảng thì xoá mềm.
     */
    @Transactional
    public ItineraryViews.Itinerary save(UUID productId, String sourceLocale, List<ItineraryViews.DayInput> input) {
        ProductEntity product = requireProductWithItinerary(productId);
        validate(product, input);

        Map<Short, ItineraryDayEntity> dangCo = new HashMap<>();
        for (ItineraryDayEntity e : days.findByProductIdAndSoftDeleteFalseOrderByDayNumber(productId)) {
            dangCo.put(e.getDayNumber(), e);
        }

        for (ItineraryViews.DayInput d : input) {
            short so = (short) d.dayNumber();
            ItineraryDayEntity e = dangCo.remove(so);
            if (e == null) {
                e = new ItineraryDayEntity(UUID.randomUUID(), productId, so);
            }
            e.setDestinationId(d.destinationId());
            e.setHotelId(d.hotelId());
            days.save(e);

            luuChu(e.getId(), sourceLocale, d.title(), d.description());
        }

        // Còn sót trong `dangCo` nghĩa là ngày đó không còn trong mảng gửi lên.
        dangCo.values().forEach(e -> {
            e.setSoftDelete(true);
            days.save(e);
        });

        days.flush();
        texts.flush();
        return view(productId, sourceLocale);
    }

    /**
     * Dịch lịch trình sang một ngôn ngữ — <b>chỉ đụng tới chữ</b>.
     *
     * <p>Luật quyền phụ thuộc locale, chép đúng ma trận docs/22 mục 2.1. Chép có
     * chủ ý, cùng lý do đã ghi ở {@code AdminContentService}: hai bề mặt, hai
     * dòng trong ma trận, hai chỗ kiểm.
     */
    @Transactional
    public ItineraryViews.Itinerary saveTranslation(UUID productId, String locale, String sourceLocale,
                                                    Set<String> roles, List<ItineraryViews.TextInput> input) {
        boolean isSource = sourceLocale.equals(locale);
        boolean allowed = roles.contains("ADMIN")
                || (isSource ? roles.contains("EDITOR") : roles.contains("TRANSLATOR"));
        if (!allowed) {
            throw new ForbiddenException(isSource
                    ? "chỉ EDITOR hoặc ADMIN sửa được bản ngôn ngữ nguồn"
                    : "chỉ TRANSLATOR hoặc ADMIN sửa được bản dịch");
        }

        requireProductWithItinerary(productId);
        List<ItineraryDayEntity> hienCo = days.findByProductIdAndSoftDeleteFalseOrderByDayNumber(productId);

        Map<Short, UUID> idTheoNgay = new HashMap<>();
        hienCo.forEach(e -> idTheoNgay.put(e.getDayNumber(), e.getId()));

        // Tập ngày gửi lên phải khớp ĐÚNG tập ngày đang có. Dịch nửa chừng rồi
        // lưu sẽ để lại một lịch trình mà locale này thấy thủng ngày.
        Set<Short> guiLen = new LinkedHashSet<>();
        List<AdminErrors.FieldRulesViolated.Issue> issues = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            short so = (short) input.get(i).dayNumber();
            if (!idTheoNgay.containsKey(so) || !guiLen.add(so)) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "days[%d].dayNumber".formatted(i), FieldRule.INVALID, Map.of("dayNumber", so)));
            }
        }
        if (guiLen.size() != idTheoNgay.size()) {
            issues.add(new AdminErrors.FieldRulesViolated.Issue(
                    "days", FieldRule.SIZE, Map.of("min", idTheoNgay.size(), "max", idTheoNgay.size())));
        }
        AdminErrors.FieldRulesViolated.throwIfAny(issues);

        for (ItineraryViews.TextInput t : input) {
            luuChu(idTheoNgay.get((short) t.dayNumber()), locale, t.title(), t.description());
        }
        texts.flush();

        return view(productId, sourceLocale);
    }

    // ------------------------------------------------------------ nội bộ

    /**
     * Ba luật liên trường, gom rồi ném <b>một lần</b> — ném ở luật đầu tiên là
     * bắt người nhập sửa từng cái một và gửi lại từng lần.
     */
    private void validate(ProductEntity product, List<ItineraryViews.DayInput> input) {
        List<AdminErrors.FieldRulesViolated.Issue> issues = new ArrayList<>();

        Short duration = product.getDurationDays();
        if (duration != null && input.size() != duration) {
            // Quy tắc 3 của docs/12 mục 9, và bộ kiểm nhất quán dữ liệu cũng bắt
            // đúng luật này — bắt ở đây để nó thành 400 chỉ đúng ô thay vì một
            // dòng đỏ trong báo cáo hằng đêm.
            issues.add(new AdminErrors.FieldRulesViolated.Issue(
                    "days", FieldRule.SIZE, Map.of("min", duration, "max", duration)));
        }

        Set<Integer> daGap = new LinkedHashSet<>();
        for (int i = 0; i < input.size(); i++) {
            ItineraryViews.DayInput d = input.get(i);

            // Liền mạch từ 1: ngày thứ i (đếm từ 0) phải mang số i+1. Thủng một
            // ngày nghĩa là trang khách hiện "Ngày 3" ngay sau "Ngày 1".
            if (d.dayNumber() != i + 1 || !daGap.add(d.dayNumber())) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "days[%d].dayNumber".formatted(i), FieldRule.INVALID, Map.of("expected", i + 1)));
            }
            if (d.destinationId() != null && !refs.destinationExists(d.destinationId())) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "days[%d].destinationId".formatted(i), FieldRule.INVALID, Map.of()));
            }
            if (d.hotelId() != null && !refs.hotelExists(d.hotelId())) {
                issues.add(new AdminErrors.FieldRulesViolated.Issue(
                        "days[%d].hotelId".formatted(i), FieldRule.INVALID, Map.of()));
            }
        }

        AdminErrors.FieldRulesViolated.throwIfAny(issues);
    }

    private ProductEntity requireProductWithItinerary(UUID productId) {
        ProductEntity product = products.findByIdAndSoftDeleteFalse(productId)
                .orElseThrow(() -> new NotFoundException("product id=" + productId));

        if (KHONG_CO_LICH_TRINH.contains(product.getProductType())) {
            throw new NotFoundException(
                    "loại " + product.getProductType() + " không có lịch trình theo ngày");
        }
        return product;
    }

    /**
     * Ghi đè chữ của một ngày ở một ngôn ngữ.
     *
     * <p>Tra cả dòng đã xoá mềm chứ không chỉ dòng đang sống: khoá chính là
     * {@code (itinerary_day_id, locale)}, nên một dòng cũ đã xoá mềm vẫn chặn
     * {@code INSERT}. Gặp nó thì hồi sinh và ghi đè — đó đúng là ý người dùng
     * khi họ dịch lại một ngày từng bị bỏ.
     */
    private void luuChu(UUID dayId, String locale, String title, String description) {
        texts.findByItineraryDayIdAndLocale(dayId, locale)
                .ifPresentOrElse(t -> {
                    t.setTitle(title);
                    t.setDescription(description);
                    t.setSoftDelete(false);
                    texts.save(t);
                }, () -> texts.save(
                        new ItineraryDayTranslationEntity(dayId, locale, title, description)));
    }

    private ItineraryViews.Itinerary view(UUID productId, String sourceLocale) {
        List<ItineraryDayEntity> hienCo = days.findByProductIdAndSoftDeleteFalseOrderByDayNumber(productId);
        if (hienCo.isEmpty()) {
            return new ItineraryViews.Itinerary(List.of());
        }

        List<UUID> ids = hienCo.stream().map(ItineraryDayEntity::getId).toList();
        Map<UUID, List<ItineraryViews.Text>> chuTheoNgay = new HashMap<>();
        for (ItineraryDayTranslationEntity t : texts.findByItineraryDayIdInAndSoftDeleteFalse(ids)) {
            chuTheoNgay.computeIfAbsent(t.getItineraryDayId(), k -> new ArrayList<>())
                    .add(new ItineraryViews.Text(t.getLocale(), t.getTitle(), t.getDescription(),
                            sourceLocale.equals(t.getLocale())));
        }

        Map<UUID, String> tenDiemDen = refs.destinationNames(sourceLocale);
        Map<UUID, String> tenKhachSan = refs.hotelNames();

        List<ItineraryViews.Day> ra = hienCo.stream()
                .map(e -> new ItineraryViews.Day(
                        e.getDayNumber(),
                        e.getDestinationId(),
                        e.getDestinationId() == null ? null : tenDiemDen.get(e.getDestinationId()),
                        e.getHotelId(),
                        e.getHotelId() == null ? null : tenKhachSan.get(e.getHotelId()),
                        chuTheoNgay.getOrDefault(e.getId(), List.of()).stream()
                                .sorted((a, b) -> a.locale().compareTo(b.locale()))
                                .toList()))
                .toList();

        return new ItineraryViews.Itinerary(ra);
    }
}
