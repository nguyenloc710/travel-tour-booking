package vn.travel.booking.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Ranh giới phụ thuộc giữa bốn module — docs/10 mục 3.
 *
 * <p>Đây là ràng buộc quan trọng nhất của tầng backend, và nó được kiểm tự động
 * chứ không dựa vào kỷ luật cá nhân. Chạy bằng {@code ./gradlew archTest}.
 */
@AnalyzeClasses(
        packages = "vn.travel.booking",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule domain_khong_biet_spring_ton_tai = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "javax..",
                    "com.fasterxml.jackson..",
                    "org.hibernate..")
            .because("""
                    Engine tính giá và quy tắc nghiệp vụ phải test được bằng JUnit thuần \
                    trong vài mili giây — không dựng context, không CSDL. Thêm một \
                    annotation vào domain là mất tính chất đó.""");

    @ArchTest
    static final ArchRule domain_khong_phu_thuoc_module_khac = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "vn.travel.booking.application..",
                    "vn.travel.booking.infrastructure..",
                    "vn.travel.booking.web..")
            .because("domain nằm ở đáy chuỗi phụ thuộc, không biết gì về các tầng trên");

    @ArchTest
    static final ArchRule application_khong_dung_jpa_va_spring_web = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.web..",
                    "org.springframework.data.jpa..",
                    "vn.travel.booking.infrastructure..",
                    "vn.travel.booking.web..")
            .because("""
                    application khai báo cổng, infrastructure hiện thực. \
                    Biết JPA hay HTTP là đã lộn ngược chiều phụ thuộc.""");

    @ArchTest
    static final ArchRule web_khong_cham_jpa_truc_tiep = noClasses()
            .that().resideInAPackage("vn.travel.booking.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.jdbc..",
                    "vn.travel.booking.infrastructure..")
            .because("controller gọi use case, không gọi thẳng CSDL");

    /**
     * Hàm nào cần "hôm nay" thì <b>nhận {@code LocalDate} làm tham số</b>; tầng
     * application truyền vào từ {@code Clock} được tiêm.
     *
     * <p>Giảm giá đặt sớm phụ thuộc khoảng cách tới ngày khởi hành. Test đọc
     * đồng hồ thật sẽ đỏ vào một ngày nào đó trong tương lai mà không ai hiểu
     * vì sao — docs/10 mục 3.1.
     */
    @ArchTest
    static final ArchRule domain_khong_doc_dong_ho_he_thong = noClasses()
            .that().resideInAPackage("..domain..")
            .should().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .orShould().callMethod(Clock.class, "systemDefaultZone")
            .because("domain không đọc đồng hồ hệ thống — nhận ngày làm tham số");

    /**
     * Tiền luôn {@code BigDecimal}. {@code double} và {@code float} làm sai số
     * tiền tệ, và sai số tiền là loại bug không ai phát hiện cho tới lúc đối soát.
     */
    @ArchTest
    static final ArchRule domain_khong_dung_so_dau_phay_dong = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveRawType(double.class)
            .orShould().haveRawType(float.class)
            .because("mọi phép tính tiền dùng BigDecimal, không bao giờ double hay float");
}
