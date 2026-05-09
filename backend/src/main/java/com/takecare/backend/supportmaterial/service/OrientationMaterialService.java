package com.takecare.backend.supportmaterial.service;

import com.takecare.backend.supportmaterial.dto.OrientationMaterialDTO;
import com.takecare.backend.supportmaterial.model.OrientationMaterial;
import com.takecare.backend.supportmaterial.repository.OrientationMaterialRepository;
import com.takecare.backend.user.repository.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrientationMaterialService {

    private static final Logger logger = LoggerFactory.getLogger(OrientationMaterialService.class);

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".doc", ".docx");

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final OrientationMaterialRepository materialRepository;
    private final SpecialistRepository specialistRepository;
    private final Path storageRoot;

    public OrientationMaterialService(
            OrientationMaterialRepository materialRepository,
            SpecialistRepository specialistRepository,
            @Value("${orientation.materials.storage-path:./uploads/orientation-materials}") String storagePath) {
        this.materialRepository = materialRepository;
        this.specialistRepository = specialistRepository;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageRoot);
            logger.info("Orientation materials storage ready at: {}", this.storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize orientation-materials storage directory", e);
        }
    }

    /**
     * Sube y persiste un nuevo material de orientación.
     *
     * @param specialistId ID del especialista dueño del material
     * @param title        Título del material (obligatorio)
     * @param description  Descripción del material (obligatorio)
     * @param file         Archivo a subir (obligatorio, máx 10 MB, PDF/DOC/DOCX)
     * @return DTO del material guardado
     * @throws IllegalArgumentException si alguna validación falla
     * @throws IOException              si ocurre un error al guardar el archivo
     */
    public OrientationMaterialDTO storeMaterial(Integer specialistId,
                                                String title,
                                                String description,
                                                MultipartFile file) throws IOException {

        specialistRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException("Specialist not found: " + specialistId));

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = "." + originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File format not allowed. Only PDF, DOC and DOCX are accepted");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("File exceeds 10 MB limit (%.1f MB)", file.getSize() / (1024.0 * 1024)));
        }

        String safeFileName = sanitizeFileName(originalName);
        String relativePath = specialistId + "/" + System.currentTimeMillis() + "_" + safeFileName;
        Path targetPath = storageRoot.resolve(relativePath);

        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Stored orientation material for specialist {}: {}", specialistId, targetPath);

        OrientationMaterial material = new OrientationMaterial();
        material.setSpecialistId(specialistId);
        material.setTitle(title.trim());
        material.setDescription(description.trim());
        material.setFileName(safeFileName);
        material.setFileUrl(relativePath);
        material.setContentType(contentType);
        material.setFileSize(file.getSize());
        material.setCreatedDate(LocalDateTime.now());
        material.setStatus((byte) 1);

        OrientationMaterial saved = materialRepository.save(material);
        logger.info("OrientationMaterial persisted with id: {}", saved.getId());

        return toDTO(saved);
    }

    /**
     * @param specialistId 
     * @return 
     */
    public List<OrientationMaterialDTO> getMaterialsBySpecialist(Integer specialistId) {
        return materialRepository.findActiveBySpecialistId(specialistId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * @param specialistId 
     * @param materialId 
     * @return
     * @throws SecurityException 
     */
    public boolean deleteMaterial(Integer specialistId, Long materialId) {
        return materialRepository.findActiveByIdAndSpecialistId(materialId, specialistId)
                .map(material -> {
                    material.setStatus((byte) 0);
                    materialRepository.save(material);
                    logger.info("OrientationMaterial {} soft-deleted by specialist {}", materialId, specialistId);
                    return true;
                })
                .orElseGet(() -> {
                    logger.warn("Delete failed: material {} not found for specialist {}", materialId, specialistId);
                    return false;
                });
    }

    private OrientationMaterialDTO toDTO(OrientationMaterial material) {
        return new OrientationMaterialDTO(
                material.getId(),
                material.getSpecialistId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSize(),
                material.getCreatedDate()
        );
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "file";
        return Paths.get(originalName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
