package vn.travel.booking.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.travel.booking.media.entity.MediaAssetTranslationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetTranslationRepository
        extends JpaRepository<MediaAssetTranslationEntity, MediaAssetTranslationEntity.CompositeId> {

    List<MediaAssetTranslationEntity> findByAssetIdOrderByLocale(UUID assetId);

    Optional<MediaAssetTranslationEntity> findByAssetIdAndLocale(UUID assetId, String locale);
}
