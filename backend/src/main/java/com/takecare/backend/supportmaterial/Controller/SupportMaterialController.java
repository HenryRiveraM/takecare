package com.takecare.backend.supportmaterial.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.takecare.backend.supportmaterial.dto.SupportMaterialListResponseDto;
import com.takecare.backend.supportmaterial.service.OrientationMaterialService;

@RestController
@RequestMapping("/api/v1")
public class SupportMaterialController {

    private static final Logger logger = LoggerFactory.getLogger(SupportMaterialController.class);

    private final OrientationMaterialService materialService;

    public SupportMaterialController(OrientationMaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/support-materials")
    public ResponseEntity<?> listSupportMaterials(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {
        try {
            logger.info("GET /api/v1/support-materials | search={} | type={}", search, type);
            SupportMaterialListResponseDto response = materialService.getSupportMaterials(search, type);
            logger.info("GET /api/v1/support-materials | total={}", response.totalDocuments());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("GET /api/v1/support-materials | validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("GET /api/v1/support-materials | error fetching materials", e);
            throw e;
        } catch (Exception e) {
            logger.error("GET /api/v1/support-materials | unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener materiales"));
        }
    }

    @GetMapping("/specialists/{specialistId}/support-materials")
    public ResponseEntity<?> listSupportMaterialsBySpecialist(@PathVariable Integer specialistId) {
        try {
            logger.info("GET /api/v1/specialists/{}/support-materials", specialistId);
            SupportMaterialListResponseDto response = materialService.getSupportMaterialsBySpecialist(specialistId);
            logger.info("GET /api/v1/specialists/{}/support-materials | total={}", specialistId, response.totalDocuments());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            logger.warn("GET /api/v1/specialists/{}/support-materials | specialist not found", specialistId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Specialist not found"));
        } catch (RuntimeException e) {
            logger.error("GET /api/v1/specialists/{}/support-materials | error fetching materials", specialistId, e);
            throw e;
        } catch (Exception e) {
            logger.error("GET /api/v1/specialists/{}/support-materials | unexpected error", specialistId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener materiales del especialista"));
        }
    }
}