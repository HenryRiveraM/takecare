package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.SpecialistProfileDTO;
import com.takecare.backend.user.dto.UpdateSpecialistProfileDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class SpecialistProfileService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistProfileService.class);

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
}