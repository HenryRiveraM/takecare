package com.takecare.backend.supportmaterial.service;
 
import com.takecare.backend.supportmaterial.dto.OrientationMaterialDTO;
import com.takecare.backend.supportmaterial.dto.SupportMaterialItemDto;
import com.takecare.backend.supportmaterial.dto.SupportMaterialListResponseDto;
import com.takecare.backend.supportmaterial.model.OrientationMaterial;
import com.takecare.backend.supportmaterial.repository.OrientationMaterialRepository;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
 
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrientationMaterialService {
 
    private static final Logger logger = LoggerFactory.getLogger(OrientationMaterialService.class);
 
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
 
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".doc", ".docx");
 
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
 
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
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("File without extension is not allowed");
        }

        String extension = originalName.substring(dotIndex).toLowerCase();
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
 
    public List<OrientationMaterialDTO> getMaterialsBySpecialist(Integer specialistId) {
        return materialRepository.findActiveBySpecialistId(specialistId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
 
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
 
    /**
     * Devuelve el archivo físico para servirlo como descarga o vista previa.
     */
    public Optional<Resource> getFile(Integer specialistId, Long materialId) throws MalformedURLException {
        Optional<OrientationMaterial> materialOpt =
                materialRepository.findActiveByIdAndSpecialistId(materialId, specialistId);
 
        if (materialOpt.isEmpty()) {
            logger.warn("File not found: material {} for specialist {}", materialId, specialistId);
            return Optional.empty();
        }
 
        Path filePath = storageRoot.resolve(materialOpt.get().getFileUrl()).normalize();
        Resource resource = new UrlResource(filePath.toUri());
 
        if (!resource.exists() || !resource.isReadable()) {
            logger.error("File not readable: {}", filePath);
            return Optional.empty();
        }
 
        return Optional.of(resource);
    }


    public SupportMaterialListResponseDto getSupportMaterials(String search, String type) {
        try {
            String normalizedSearch = normalizeNullable(search);
            String normalizedType = normalizeType(type);

            validateType(normalizedType);

            if (normalizedSearch == null && normalizedType == null) {
                logger.info("Fetching full support materials library");
            } else {
                logger.info("Fetching support materials with filters | search={} | type={}", normalizedSearch, normalizedType);
            }

            List<OrientationMaterial> materials = materialRepository
                    .findActiveWithFilters(normalizedSearch, normalizedType);

            if (materials.isEmpty()) {
                logger.info("No support materials found | search={} | type={}", normalizedSearch, normalizedType);
            }

            Map<Integer, String> specialistNames = resolveSpecialistNames(materials);

            List<SupportMaterialItemDto> items = materials.stream()
                    .map(material -> toSupportMaterialItemDto(material, specialistNames.get(material.getSpecialistId())))
                    .toList();

            return new SupportMaterialListResponseDto(items.size(), items);
        } catch (RuntimeException e) {
            logger.error("Error fetching support materials", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching support materials", e);
            throw new RuntimeException("Error al obtener materiales");
        }
    }

    public SupportMaterialListResponseDto getSupportMaterialsBySpecialist(Integer specialistId) {
        try {
            Specialist specialist = specialistRepository.findById(specialistId)
                    .orElseThrow(() -> new NoSuchElementException("Specialist not found"));

            logger.info("Fetching support materials by specialist {}", specialistId);

            List<OrientationMaterial> materials = materialRepository.findActiveBySpecialistId(specialistId);

            if (materials.isEmpty()) {
                logger.info("No support materials found for specialist {}", specialistId);
            }

            String specialistName = buildFullName(specialist);
            List<SupportMaterialItemDto> items = materials.stream()
                    .map(material -> toSupportMaterialItemDto(material, specialistName))
                    .toList();

            return new SupportMaterialListResponseDto(items.size(), items);
        } catch (RuntimeException e) {
            logger.error("Error fetching support materials for specialist {}", specialistId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching support materials for specialist {}", specialistId, e);
            throw new RuntimeException("Error al obtener materiales del especialista");
        }
    }

    private OrientationMaterialDTO toDTO(OrientationMaterial material) {
        String fileUrl = "/api/v1/specialists/" + material.getSpecialistId()
                + "/orientation-materials/" + material.getId() + "/file";
 
        return new OrientationMaterialDTO(
                material.getId(),
                material.getSpecialistId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSize(),
                material.getCreatedDate(),
                fileUrl
        );
    }
 

    private SupportMaterialItemDto toSupportMaterialItemDto(OrientationMaterial material, String specialistName) {
        String fileType = resolveFileType(material.getContentType(), material.getFileName());
        // TODO: Add preview/download endpoints (e.g., /api/v1/support-materials/{id}/file, /download) once storage is defined.
        String previewUrl = null; // TODO: define preview URL when storage strategy is decided.
        String downloadUrl = null; // TODO: define download URL when storage strategy is decided.

        return new SupportMaterialItemDto(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileName(),
                material.getContentType(),
                fileType,
                material.getFileSize(),
                material.getFileUrl(),
                previewUrl,
                downloadUrl,
                material.getCreatedDate(),
                material.getSpecialistId(),
                specialistName
        );
    }

    private Map<Integer, String> resolveSpecialistNames(List<OrientationMaterial> materials) {
        Set<Integer> specialistIds = materials.stream()
                .map(OrientationMaterial::getSpecialistId)
                .collect(Collectors.toSet());

        if (specialistIds.isEmpty()) {
            return Map.of();
        }

        return specialistRepository.findAllById(specialistIds).stream()
                .collect(Collectors.toMap(Specialist::getId, this::buildFullName));
    }

    private String buildFullName(Specialist specialist) {
        String fullName = List.of(
                normalizeOptional(specialist.getNames()),
                normalizeOptional(specialist.getFirstLastname()),
                normalizeOptional(specialist.getSecondLastname())
        ).stream()
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));

        return fullName.isBlank() ? null : fullName;
    }

    private String resolveFileType(String contentType, String fileName) {
        String normalizedContentType = normalizeNullable(contentType);
        String normalizedFileName = normalizeNullable(fileName);

        if (normalizedContentType != null) {
            String lowerContentType = normalizedContentType.toLowerCase(Locale.ROOT);
            if (lowerContentType.contains("pdf")) {
                return "PDF";
            }
            if (lowerContentType.contains("wordprocessingml") || lowerContentType.contains("msword")) {
                return "DOCX";
            }
        }

        if (normalizedFileName != null) {
            String lowerFileName = normalizedFileName.toLowerCase(Locale.ROOT);
            if (lowerFileName.endsWith(".pdf")) {
                return "PDF";
            }
            if (lowerFileName.endsWith(".docx") || lowerFileName.endsWith(".doc")) {
                return "DOCX";
            }
        }

        return null;
    }

    private void validateType(String type) {
        if (type == null) {
            return;
        }

        if (!"PDF".equals(type) && !"DOCX".equals(type)) {
            throw new IllegalArgumentException("Tipo de archivo invalido. Solo PDF o DOCX");
        }
    }

    private String normalizeType(String type) {
        String normalized = normalizeNullable(type);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeOptional(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? "" : normalized;
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "file";
        return Paths.get(originalName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}