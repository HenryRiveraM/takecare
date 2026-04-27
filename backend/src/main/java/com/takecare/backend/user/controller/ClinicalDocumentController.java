package com.takecare.backend.user.controller;

import com.takecare.backend.user.dto.ClinicalDocumentDTO;
import com.takecare.backend.user.model.User;
import com.takecare.backend.user.service.ClinicalDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

/**
 * Endpoints de documentos clínicos.
 *
 * Todos los endpoints bajo /api/v1/patients/clinical-docs/{patient_id}
 * pasan por ClinicalDocAccessInterceptor antes de llegar aquí.
 *
 * Endpoints:
 *  GET    /api/v1/patients/clinical-docs/{patient_id}                → listar docs
 *  POST   /api/v1/patients/clinical-docs/{patient_id}                → subir doc
 *  GET    /api/v1/patients/clinical-docs/{patient_id}/{document_id}  → descargar doc
 *  DELETE /api/v1/patients/clinical-docs/{patient_id}/{document_id}  → eliminar doc
 */
@RestController
@RequestMapping("/api/v1/patients/clinical-docs")
public class ClinicalDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalDocumentController.class);

    private final ClinicalDocumentService documentService;

    public ClinicalDocumentController(ClinicalDocumentService documentService) {
        this.documentService = documentService;
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/patients/clinical-docs/{patient_id}
    // Lista los documentos activos del paciente según el rol del solicitante
    // -----------------------------------------------------------------------
    @GetMapping("/{patient_id}")
    public ResponseEntity<List<ClinicalDocumentDTO>> listDocuments(
            @PathVariable("patient_id") Integer patientId,
            @AuthenticationPrincipal User requestingUser) {

        logger.info("List clinical docs for patient {} requested by user {}",
                patientId, requestingUser.getId());

        List<ClinicalDocumentDTO> docs = documentService.getDocumentsForPatient(requestingUser, patientId);
        return ResponseEntity.ok(docs);
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/patients/clinical-docs/{patient_id}
    // Sube un nuevo documento clínico
    // -----------------------------------------------------------------------
    @PostMapping(value = "/{patient_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @PathVariable("patient_id") Integer patientId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User requestingUser) {

        // Solo el propio paciente puede subir documentos
        if (!requestingUser.getId().equals(patientId)) {
            logger.warn("User {} tried to upload doc for patient {}", requestingUser.getId(), patientId);
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

    // -----------------------------------------------------------------------
    // GET /api/v1/patients/clinical-docs/{patient_id}/{document_id}
    // Descarga un documento (el interceptor ya validó acceso)
    // -----------------------------------------------------------------------
    @GetMapping("/{patient_id}/{document_id}")
    public ResponseEntity<?> downloadDocument(
            @PathVariable("patient_id") Integer patientId,
            @PathVariable("document_id") Long documentId,
            @AuthenticationPrincipal User requestingUser) {

        logger.info("Download doc {} of patient {} requested by user {}",
                documentId, patientId, requestingUser.getId());

        try {
            Optional<Resource> resourceOpt = documentService.downloadDocument(requestingUser, patientId, documentId);

            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Document not found or access denied");
            }

            Resource resource = resourceOpt.get();
            String contentDisposition = "attachment; filename=\"" + resource.getFilename() + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (MalformedURLException e) {
            logger.error("Malformed URL for doc {} of patient {}", documentId, patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not serve the file");
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/patients/clinical-docs/{patient_id}/{document_id}
    // Eliminación lógica — solo el propio paciente
    // -----------------------------------------------------------------------
    @DeleteMapping("/{patient_id}/{document_id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable("patient_id") Integer patientId,
            @PathVariable("document_id") Long documentId,
            @AuthenticationPrincipal User requestingUser) {

        // Doble check: el interceptor protege el path, pero el delete es solo del dueño
        if (!requestingUser.getId().equals(patientId)) {
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
}
