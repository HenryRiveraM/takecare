package com.takecare.backend.supportmaterial.Controller;

import com.takecare.backend.supportmaterial.DTO.OrientationMaterialDTO;
import com.takecare.backend.supportmaterial.service.OrientationMaterialService;
import com.takecare.backend.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/specialists")
public class OrientationMaterialController {

    private static final Logger logger = LoggerFactory.getLogger(OrientationMaterialController.class);

    private final OrientationMaterialService materialService;

    public OrientationMaterialController(OrientationMaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/orientation-materials")
    public ResponseEntity<List<OrientationMaterialDTO>> listMaterials(
            @AuthenticationPrincipal User requestingUser) {

        Integer specialistId = requestingUser.getId();
        logger.info("GET /api/v1/specialists/orientation-materials | specialistId={}", specialistId);

        List<OrientationMaterialDTO> materials = materialService.getMaterialsBySpecialist(specialistId);

        logger.info("GET /api/v1/specialists/orientation-materials | total={}", materials.size());
        return ResponseEntity.ok(materials);
    }

    @PostMapping(value = "/orientation-materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User requestingUser) {

        Integer specialistId = requestingUser.getId();
        logger.info("POST /api/v1/specialists/orientation-materials | specialistId={} | file={}",
                specialistId, file.getOriginalFilename());

        try {
            OrientationMaterialDTO saved = materialService.storeMaterial(specialistId, title, description, file);
            logger.info("POST /api/v1/specialists/orientation-materials | saved id={}", saved.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            logger.warn("POST /api/v1/specialists/orientation-materials | validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IOException e) {
            logger.error("POST /api/v1/specialists/orientation-materials | IO error for specialist {}", specialistId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not store file. Please try again.");
        }
    }

    @DeleteMapping("/orientation-materials/{id}")
    public ResponseEntity<?> deleteMaterial(
            @PathVariable Long id,
            @AuthenticationPrincipal User requestingUser) {

        Integer specialistId = requestingUser.getId();
        logger.info("DELETE /api/v1/specialists/orientation-materials/{} | specialistId={}", id, specialistId);

        boolean deleted = materialService.deleteMaterial(specialistId, id);

        if (deleted) {
            logger.info("DELETE /api/v1/specialists/orientation-materials/{} | deleted successfully", id);
            return ResponseEntity.ok("Material deleted successfully");
        } else {
            logger.warn("DELETE /api/v1/specialists/orientation-materials/{} | not found or not owner", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Material not found or you do not have permission to delete it");
        }
    }
}
