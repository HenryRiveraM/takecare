package com.takecare.backend.supportmaterial.controller;
 
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.takecare.backend.supportmaterial.dto.OrientationMaterialDTO;
import com.takecare.backend.supportmaterial.service.OrientationMaterialService;
 
@RestController
@RequestMapping("/api/v1/specialists")
public class OrientationMaterialController {
 
    private static final Logger logger = LoggerFactory.getLogger(OrientationMaterialController.class);
 
    private final OrientationMaterialService materialService;
 
    public OrientationMaterialController(OrientationMaterialService materialService) {
        this.materialService = materialService;
    }
 
    @GetMapping("/{specialistId}/orientation-materials")
    public ResponseEntity<List<OrientationMaterialDTO>> listMaterials(
            @PathVariable Integer specialistId) {
 
        logger.info("GET /api/v1/specialists/{}/orientation-materials", specialistId);
        List<OrientationMaterialDTO> materials = materialService.getMaterialsBySpecialist(specialistId);
        logger.info("GET orientation-materials | specialistId={} | total={}", specialistId, materials.size());
        return ResponseEntity.ok(materials);
    }
 
    @PostMapping(value = "/{specialistId}/orientation-materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(
            @PathVariable Integer specialistId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file) {
 
        logger.info("POST /api/v1/specialists/{}/orientation-materials | file={}", specialistId, file.getOriginalFilename());
 
        try {
            OrientationMaterialDTO saved = materialService.storeMaterial(specialistId, title, description, file);
            logger.info("POST orientation-materials | saved id={}", saved.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
 
        } catch (IllegalArgumentException e) {
            logger.warn("POST orientation-materials | validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
 
        } catch (IOException e) {
            logger.error("POST orientation-materials | IO error for specialist {}", specialistId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not store file. Please try again.");
        }
    }
 
    @DeleteMapping("/{specialistId}/orientation-materials/{id}")
    public ResponseEntity<?> deleteMaterial(
            @PathVariable Integer specialistId,
            @PathVariable Long id) {
 
        logger.info("DELETE /api/v1/specialists/{}/orientation-materials/{}", specialistId, id);
 
        boolean deleted = materialService.deleteMaterial(specialistId, id);
 
        if (deleted) {
            logger.info("DELETE orientation-materials | id={} deleted successfully", id);
            return ResponseEntity.ok("Material deleted successfully");
        } else {
            logger.warn("DELETE orientation-materials | id={} not found for specialist {}", id, specialistId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Material not found or you do not have permission to delete it");
        }
    }
 
    /**
     * Sirve el archivo físico para vista previa o descarga.
     * GET /api/v1/specialists/{specialistId}/orientation-materials/{id}/file
     */
    @GetMapping("/{specialistId}/orientation-materials/{id}/file")
    public ResponseEntity<?> getFile(
            @PathVariable Integer specialistId,
            @PathVariable Long id) {
 
        logger.info("GET /api/v1/specialists/{}/orientation-materials/{}/file", specialistId, id);
 
        try {
            Optional<Resource> resourceOpt = materialService.getFile(specialistId, id);
 
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found");
            }
 
            Resource resource = resourceOpt.get();
            String filename = resource.getFilename() != null ? resource.getFilename() : "file";
            String contentDisposition = "inline; filename=\"" + filename + "\"";
 
            // Detectar content type para que el navegador pueda mostrarlo inline
            String contentType = "application/octet-stream";
            if (filename.endsWith(".pdf")) contentType = "application/pdf";
            else if (filename.endsWith(".doc"))  contentType = "application/msword";
            else if (filename.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
 
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
 
        } catch (MalformedURLException e) {
            logger.error("GET file | malformed URL for material {} specialist {}", id, specialistId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not serve the file");
        }
    }
}