package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.SpecialistLocationResponseDto;
import com.takecare.backend.user.dto.SpecialistProfileDTO;
import com.takecare.backend.user.dto.UpdateSpecialistLocationRequestDto;
import com.takecare.backend.user.dto.UpdateSpecialistProfileDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class SpecialistProfileService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistProfileService.class);
    private static final int ROLE_SPECIALIST = 2;
    private static final String OFFICE_UBI_DELIMITER = "|||TC|||";
    private static final int ADDRESS_LINE_MAX_LENGTH = 120;
    private static final int CITY_MAX_LENGTH = 60;
    private static final int NEIGHBORHOOD_MAX_LENGTH = 60;
    private static final int REFERENCE_MAX_LENGTH = 120;
    private static final int OFFICE_UBI_MAX_LENGTH = 255;

    private final SpecialistRepository specialistRepository;

    public SpecialistProfileService(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public Optional<SpecialistProfileDTO> getProfile(Integer id) {
        try {
            logger.info("Fetching profile for specialist id: {}", id);

            Optional<SpecialistProfileDTO> profile = specialistRepository.findById(id)
                    .map(this::toDTO);

            if (profile.isPresent()) {
                logger.info("Profile found for specialist id: {}", id);
            } else {
                logger.warn("Specialist not found with id: {}", id);
            }

            return profile;
        } catch (RuntimeException e) {
            logger.error("Error fetching profile for specialist id: {}", id, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching profile for specialist id: {}", id, e);
            throw new RuntimeException("Error al obtener perfil del especialista");
        }
    }

    public Optional<SpecialistProfileDTO> updateProfile(Integer id, UpdateSpecialistProfileDTO dto) {
        try {
            logger.info("Updating profile for specialist id: {}", id);

            Optional<SpecialistProfileDTO> updatedProfile = specialistRepository.findById(id)
                    .map(specialist -> {
                        specialist.setNames(normalize(dto.getNames()));
                        specialist.setFirstLastname(normalize(dto.getFirstLastname()));
                        specialist.setSecondLastname(normalizeNullable(dto.getSecondLastname()));
                        specialist.setEmail(normalize(dto.getEmail()));
                        specialist.setBiography(normalizeNullable(dto.getBiography()));
                        specialist.setOfficeUbi(normalizeNullable(dto.getOfficeUbi()));
                        specialist.setSessionCost(dto.getSessionCost());
                        specialist.setLastUpdate(LocalDateTime.now());

                        Specialist updated = specialistRepository.save(specialist);
                        logger.info("Profile updated successfully for specialist id: {}", id);
                        return toDTO(updated);
                    });

            if (updatedProfile.isEmpty()) {
                logger.warn("Specialist not found with id: {}", id);
            }

            return updatedProfile;
        } catch (RuntimeException e) {
            logger.error("Error updating profile for specialist id: {}", id, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating profile for specialist id: {}", id, e);
            throw new RuntimeException("Error al actualizar perfil del especialista");
        }
    }

    public SpecialistLocationResponseDto updateSpecialistLocation(
            Integer specialistId,
            Integer authenticatedUserId,
            UpdateSpecialistLocationRequestDto dto) {
        try {
            logger.info("Updating location for specialist id: {}", specialistId);

            Specialist specialist = specialistRepository.findById(specialistId)
                    .orElseThrow(() -> {
                        logger.warn("Specialist not found with id: {}", specialistId);
                        return new NoSuchElementException("Especialista no encontrado con ID: " + specialistId);
                    });

            validateSpecialistOwnership(specialistId, authenticatedUserId);

            String addressLine = normalizeRequired(dto.getAddressLine(), "addressLine");
            String city = normalizeRequired(dto.getCity(), "city");
            String neighborhood = normalizeOptional(dto.getNeighborhood());
            String reference = normalizeOptional(dto.getReference());
            String visibility = normalizeVisibility(dto.getVisibility());

            validateMaxLength("addressLine", addressLine, ADDRESS_LINE_MAX_LENGTH);
            validateMaxLength("city", city, CITY_MAX_LENGTH);
            validateMaxLength("neighborhood", neighborhood, NEIGHBORHOOD_MAX_LENGTH);
            validateMaxLength("reference", reference, REFERENCE_MAX_LENGTH);

            validateArtificialText("addressLine", addressLine);
            validateArtificialText("city", city);
            validateArtificialText("neighborhood", neighborhood);
            validateArtificialText("reference", reference);

            String officeUbi = buildCompositeOfficeUbi(addressLine, city, neighborhood, reference);
            validateMaxLength("officeUbi", officeUbi, OFFICE_UBI_MAX_LENGTH);

            specialist.setOfficeUbi(officeUbi);
            specialist.setLastUpdate(LocalDateTime.now());
            Specialist updatedSpecialist = specialistRepository.save(specialist);

            SpecialistLocationParts parts = splitCompositeOfficeUbi(updatedSpecialist.getOfficeUbi());
            SpecialistLocationResponseDto response = toLocationResponseDto(
                    updatedSpecialist.getId(),
                    parts,
                    updatedSpecialist.getOfficeUbi(),
                    visibility
            );

            logger.info("Location updated successfully for specialist id: {}", specialistId);
            return response;
        } catch (RuntimeException e) {
            logger.error("Error updating location for specialist id: {}", specialistId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating location for specialist id: {}", specialistId, e);
            throw new RuntimeException("Error al actualizar ubicación del especialista");
        }
    }

    private SpecialistProfileDTO toDTO(Specialist specialist) {
        SpecialistProfileDTO dto = new SpecialistProfileDTO();
        dto.setId(specialist.getId());
        dto.setNames(specialist.getNames());
        dto.setFirstLastname(specialist.getFirstLastname());
        dto.setSecondLastname(specialist.getSecondLastname());
        dto.setEmail(specialist.getEmail());
        dto.setBiography(specialist.getBiography());
        dto.setOfficeUbi(specialist.getOfficeUbi());
        dto.setSessionCost(specialist.getSessionCost());
        return dto;
    }

    private SpecialistLocationResponseDto toLocationResponseDto(
            Integer specialistId,
            SpecialistLocationParts parts,
            String officeUbi,
            String visibility) {
        SpecialistLocationResponseDto dto = new SpecialistLocationResponseDto();
        dto.setSpecialistId(specialistId);
        dto.setAddressLine(parts.addressLine);
        dto.setCity(parts.city);
        dto.setNeighborhood(parts.neighborhood);
        dto.setReference(parts.reference);
        dto.setOfficeUbi(officeUbi);
        // visibility is returned for UI compatibility but is not persisted yet.
        dto.setVisibility(visibility);
        dto.setVisibilityPersisted(Boolean.FALSE);
        return dto;
    }

    String buildCompositeOfficeUbi(String addressLine, String city, String neighborhood, String reference) {
        return String.join(
                OFFICE_UBI_DELIMITER,
                normalizeOptional(addressLine),
                normalizeOptional(city),
                normalizeOptional(neighborhood),
                normalizeOptional(reference)
        );
    }

    SpecialistLocationParts splitCompositeOfficeUbi(String officeUbi) {
        if (officeUbi == null || officeUbi.isBlank()) {
            return new SpecialistLocationParts("", "", "", "");
        }

        String[] parts = officeUbi.split(Pattern.quote(OFFICE_UBI_DELIMITER), -1);
        if (parts.length == 4) {
            return new SpecialistLocationParts(
                    normalizeOptional(parts[0]),
                    normalizeOptional(parts[1]),
                    normalizeOptional(parts[2]),
                    normalizeOptional(parts[3])
            );
        }

        return new SpecialistLocationParts(normalizeOptional(officeUbi), "", "", "");
    }

    private void validateSpecialistOwnership(Integer specialistId, Integer authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new SecurityException("Acceso denegado: usuario autenticado no identificado");
        }

        Specialist authenticatedSpecialist = specialistRepository.findById(authenticatedUserId)
                .orElseThrow(() -> {
                    logger.warn("Authenticated user id: {} is not a specialist", authenticatedUserId);
                    return new SecurityException("Acceso denegado: solo especialistas pueden modificar ubicación");
                });

        if (authenticatedSpecialist.getRole() == null || authenticatedSpecialist.getRole() != ROLE_SPECIALIST) {
            throw new SecurityException("Acceso denegado: solo especialistas pueden modificar ubicación");
        }

        if (!authenticatedSpecialist.getId().equals(specialistId)) {
            throw new SecurityException("Acceso denegado: solo puedes modificar tu propia ubicación");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        return normalized;
    }

    private String normalizeVisibility(String visibility) {
        String normalized = normalizeNullable(visibility);
        if (normalized == null) {
            return null;
        }

        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (!"public".equals(lowered) && !"private".equals(lowered)) {
            throw new IllegalArgumentException("visibility solo permite valores public o private");
        }
        return lowered;
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " no puede superar " + maxLength + " caracteres"
            );
        }
    }

    private void validateArtificialText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String[] words = value.toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length < 4) {
            return;
        }

        int consecutiveRepeats = 1;
        for (int i = 1; i < words.length; i++) {
            if (words[i].equals(words[i - 1])) {
                consecutiveRepeats++;
                if (consecutiveRepeats >= 4) {
                    throw new IllegalArgumentException(
                            "El campo " + fieldName + " contiene repetición excesiva de palabras"
                    );
                }
            } else {
                consecutiveRepeats = 1;
            }
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    static class SpecialistLocationParts {
        private final String addressLine;
        private final String city;
        private final String neighborhood;
        private final String reference;

        SpecialistLocationParts(String addressLine, String city, String neighborhood, String reference) {
            this.addressLine = addressLine;
            this.city = city;
            this.neighborhood = neighborhood;
            this.reference = reference;
        }
    }
}