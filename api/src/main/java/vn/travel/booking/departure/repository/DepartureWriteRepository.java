package vn.travel.booking.departure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.departure.entity.DepartureEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Đường <b>ghi</b> của ngày khởi hành — JPA, docs/10 mục 6. */
public interface DepartureWriteRepository extends JpaRepository<DepartureEntity, UUID> {

    Optional<DepartureEntity> findByIdAndSoftDeleteFalse(UUID id);

    List<DepartureEntity> findByProductIdAndSoftDeleteFalseOrderByMarketAscDepartDateAsc(
            UUID productId);

    List<DepartureEntity> findByProductIdAndMarketAndSoftDeleteFalseOrderByDepartDateAsc(
            UUID productId, String market);

    /**
     * Dùng cho nhân bản lịch: ngày đã có ở thị trường đích thì bỏ qua, không ghi
     * đè. Gọi lại lần hai vì thế không tạo bản sao thứ hai và không xoá giá vừa
     * nhập.
     */
    boolean existsByProductIdAndMarketAndDepartDateAndSoftDeleteFalse(
            UUID productId, String market, LocalDate departDate);
}
