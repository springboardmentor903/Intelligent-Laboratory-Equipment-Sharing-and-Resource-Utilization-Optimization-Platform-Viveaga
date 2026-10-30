package com.labresource.platform.repository;

import com.labresource.platform.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface EquipmentRepository
        extends JpaRepository<Equipment, Long>,
        JpaSpecificationExecutor<Equipment> {

    Optional<Equipment> findByAssetTag(String assetTag);

    boolean existsByAssetTag(String assetTag);
}