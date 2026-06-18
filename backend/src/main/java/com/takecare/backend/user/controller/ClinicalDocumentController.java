package com.takecare.backend.user.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.takecare.backend.user.dto.ClinicalDocumentDTO;
import com.takecare.backend.user.model.ClinicalDocument;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.service.ClinicalDocumentService;

@RestController
@RequestMapping("/api/v1/patients/clinical-docs")
public class ClinicalDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalDocumentController.class);

    private final ClinicalDocumentService documentService;

    public ClinicalDocumentController(ClinicalDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{patient_id}")
    public ResponseEntity<List<ClinicalDocumentDTO>> listDocuments(
            @PathVariable("patient_id") Integer patientId,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole) {

        logger.info("List clinical docs for patient {} requested by userId={}, role={}",
                patientId, sessionUserId, sessionUserRole);

        User requestingUser = resolveRequestingUser(sessionUserId, sessionUserRole, patientId);

        List<ClinicalDocumentDTO> docs = documentService.getDocumentsForPatient(requestingUser, patientId);
        return ResponseEntity.ok(docs);
    }

    @PostMapping(value = "/{patient_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @PathVariable("patient_id") Integer patientId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole) {

        if (sessionUserId != null && !sessionUserId.equals(patientId)) {
            logger.warn("User {} tried to upload doc for patient {}", sessionUserId, patientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only upload documents to your own profile");
        }

        try {
            ClinicalDocumentDTO saved = documentService.storeDocument(patientId, file);
            logger.info("Document uploaded successfully for patient {}: {}", patientId, saved.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            logger.warn("Upload rejected for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            logger.error("IO error storing document for patient {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not store file. Please try again.");
        }
    }

    @GetMapping("/{patient_id}/{document_id}")
    public ResponseEntity<?> downloadDocument(
            @PathVariable("patient_id") Integer patientId,
            @PathVariable("document_id") Long documentId,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole) {

        logger.info("Download doc {} of patient {} requested by userId={}, role={}",
                documentId, patientId, sessionUserId, sessionUserRole);

        User requestingUser = resolveRequestingUser(sessionUserId, sessionUserRole, patientId);

        try {
            Optional<ClinicalDocument> docOpt =
                    documentService.findActiveDoc(patientId, documentId);

            Optional<Resource> resourceOpt = documentService.downloadDocument(requestingUser, patientId, documentId);

            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Document not found or access denied");
            }

            Resource resource = resourceOpt.get();

            // Resolve actual content type from stored metadata
            String contentTypeStr = docOpt.map(d -> d.getContentType())
                    .filter(ct -> ct != null && !ct.isBlank())
                    .orElse("application/octet-stream");

            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(contentTypeStr);
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            // Use inline for images and PDFs so browsers can render them directly
            boolean inlinePreview = contentTypeStr.startsWith("image/") ||
                                    "application/pdf".equals(contentTypeStr);
            String contentDisposition = (inlinePreview ? "inline" : "attachment") +
                    "; filename=\"" + resource.getFilename() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(mediaType)
                    .body(resource);

        } catch (MalformedURLException e) {
            logger.error("Malformed URL for doc {} of patient {}", documentId, patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not serve the file");
        }
    }

    @DeleteMapping("/{patient_id}/{document_id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable("patient_id") Integer patientId,
            @PathVariable("document_id") Long documentId,
            @RequestHeader(value = "X-User-Id", required = false) Integer sessionUserId,
            @RequestHeader(value = "X-User-Role", required = false) String sessionUserRole) {

        if (sessionUserId != null && !sessionUserId.equals(patientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only delete your own documents");
        }

        boolean deleted = documentService.deleteDocument(patientId, documentId);

        if (deleted) {
            return ResponseEntity.ok("Document deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Document not found");
        }
    }

    private User resolveRequestingUser(Integer sessionUserId, String sessionUserRole, Integer patientId) {
        User temp = new User();
        if (sessionUserId != null) {
            temp.setId(sessionUserId);
            if ("SPECIALIST".equalsIgnoreCase(sessionUserRole)) {
                temp.setRole((byte) 2);
            } else {
                temp.setRole((byte) 1);
            }
        } else {
            temp.setId(patientId);
            temp.setRole((byte) 1);
        }
        return temp;
    }
}
