package vn.travel.booking.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.media.entity.MediaAssetEntity;

import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, UUID> {

    boolean existsByPath(String path);
}
