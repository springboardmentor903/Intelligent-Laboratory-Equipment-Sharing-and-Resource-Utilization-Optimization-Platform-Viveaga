
package com.labresource.platform.repository;

import com.labresource.platform.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByEquipment_Id(Long equipmentId);
}
