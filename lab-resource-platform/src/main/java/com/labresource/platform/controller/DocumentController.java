
package com.labresource.platform.controller;

import com.labresource.platform.entity.Document;
import com.labresource.platform.entity.Equipment;
import com.labresource.platform.repository.DocumentRepository;
import com.labresource.platform.repository.EquipmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/{equipmentId}/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final EquipmentRepository equipmentRepository;

    public DocumentController(
            DocumentRepository documentRepository,
            EquipmentRepository equipmentRepository) {
        this.documentRepository = documentRepository;
        this.equipmentRepository = equipmentRepository;
    }

    // GET all documents for equipment
    @GetMapping
    public List<Document> getDocuments(
            @PathVariable Long equipmentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new RuntimeException(
                    "Equipment not found with ID: " + equipmentId);
        }

        return documentRepository.findByEquipment_Id(equipmentId);
    }

    // POST a document
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(
            @PathVariable Long equipmentId,
            @RequestBody Document document) {

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException(
                        "Equipment not found with ID: " + equipmentId));

        if (document.getName() == null
                || document.getName().isBlank()
                || document.getFileUrl() == null
                || document.getFileUrl().isBlank()) {
            throw new IllegalArgumentException(
                    "Document name and file URL are required");
        }

        document.setId(null);
        document.setEquipment(equipment);

        return documentRepository.save(document);
    }

    // DELETE a document
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Long equipmentId,
            @PathVariable Long documentId) {

        if (!equipmentRepository.existsById(equipmentId)) {
            throw new RuntimeException(
                    "Equipment not found with ID: " + equipmentId);
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found with ID: " + documentId));

        if (!document.getEquipment().getId().equals(equipmentId)) {
            throw new IllegalArgumentException(
                    "Document does not belong to this equipment");
        }

        documentRepository.delete(document);
    }
}
