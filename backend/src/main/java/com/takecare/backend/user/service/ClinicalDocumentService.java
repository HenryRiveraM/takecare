package com.takecare.backend.user.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.takecare.backend.user.dto.ClinicalDocumentDTO;
import com.takecare.backend.user.model.ClinicalDocument;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.repository.ClinicalDocumentRepository;
import com.takecare.backend.user.repository.PatientRepository;

@Service
public class ClinicalDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalDocumentService.class);

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg"
    );

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private static final byte ROLE_PATIENT    = 1;
    private static final byte ROLE_SPECIALIST = 2;

    private final ClinicalDocumentRepository documentRepository;
    private final PatientRepository patientRepository;

    private final Path storageRoot;

    public ClinicalDocumentService(
            ClinicalDocumentRepository documentRepository,
            PatientRepository patientRepository,
            @Value("${clinical.docs.storage-path:./uploads/clinical-docs}") String storagePath) {
        this.documentRepository = documentRepository;
        this.patientRepository  = patientRepository;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageRoot);
            logger.info("Clinical docs storage ready at: {}", this.storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize clinical-docs storage directory", e);
        }
    }

    /**
     @param patientId  
     @param file       
     @return 
     @throws IllegalArgumentException 
     * @throws IOException          
     */
    public ClinicalDocumentDTO storeDocument(Integer patientId, MultipartFile file) throws IOException {

        patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("File exceeds 5 MB limit (%.1f MB)", file.getSize() / (1024.0 * 1024)));
        }

        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        String relativePath = patientId + "/" + System.currentTimeMillis() + "_" + safeFileName;
        Path targetPath = storageRoot.resolve(relativePath);

        Files.createDirectories(targetPath.getParent());

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Stored clinical doc for patient {}: {}", patientId, targetPath);

        ClinicalDocument doc = new ClinicalDocument();
        doc.setPatientId(patientId);
        doc.setFileName(safeFileName);
        doc.setFilePath(relativePath);
        doc.setContentType(contentType);
        doc.setFileSize(file.getSize());
        doc.setUploadedAt(LocalDateTime.now());
        doc.setStatus((byte) 1);

        ClinicalDocument saved = documentRepository.save(doc);
        logger.info("ClinicalDocument persisted with id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * @param requestingUser 
     * @param patientId  
     */
    public List<ClinicalDocumentDTO> getDocumentsForPatient(User requestingUser, Integer patientId) {
        byte role = requestingUser.getRole();

        if (role == ROLE_PATIENT) {
            if (!requestingUser.getId().equals(patientId)) {
                logger.warn("Patient {} tried to access docs of patient {}", requestingUser.getId(), patientId);
                return List.of();
            }
        } else if (role == ROLE_SPECIALIST) {
            logger.info("Specialist {} accessing docs of patient {}", requestingUser.getId(), patientId);
        } else {
            logger.warn("Role {} tried to access clinical docs", role);
            return List.of();
        }

        return documentRepository.findActiveByPatientId(patientId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    
    /**
     * @param requestingUser 
     * @param patientId    
     * @param documentId   
     * @return
     */
    public Optional<Resource> downloadDocument(User requestingUser, Integer patientId, Long documentId)
            throws MalformedURLException {

        byte role = requestingUser.getRole();
        
        if (role == ROLE_PATIENT && !requestingUser.getId().equals(patientId)) {
            logger.warn("Patient {} tried to download doc of patient {}", requestingUser.getId(), patientId);
            return Optional.empty();
        }
        
        Optional<ClinicalDocument> docOpt = documentRepository.findActiveByIdAndPatientId(documentId, patientId);
        if (docOpt.isEmpty()) {
            logger.warn("Document {} not found for patient {}", documentId, patientId);
            return Optional.empty();
        }

        Path filePath = storageRoot.resolve(docOpt.get().getFilePath()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            logger.error("File not readable: {}", filePath);
            return Optional.empty();
        }

        return Optional.of(resource);
    }

  
    /**
     * @return 
     */
    public boolean deleteDocument(Integer requestingPatientId, Long documentId) {
        return documentRepository.findActiveByIdAndPatientId(documentId, requestingPatientId)
                .map(doc -> {
                    doc.setStatus((byte) 0);
                    documentRepository.save(doc);
                    logger.info("ClinicalDocument {} soft-deleted by patient {}", documentId, requestingPatientId);
                    return true;
                }).orElseGet(() -> {
                    logger.warn("Delete failed: doc {} not found for patient {}", documentId, requestingPatientId);
                    return false;
                });
    }

    
    private ClinicalDocumentDTO toDTO(ClinicalDocument doc) {
        return new ClinicalDocumentDTO(
                doc.getId(),
                doc.getPatientId(),
                doc.getFileName(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getUploadedAt()
        );
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "file";
        return Paths.get(originalName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
