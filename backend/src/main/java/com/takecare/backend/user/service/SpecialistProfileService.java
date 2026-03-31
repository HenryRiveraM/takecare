package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
                        NameParts nameParts = splitFullName(normalize(dto.getFullName()));

                        specialist.setNames(nameParts.names);
                        specialist.setFirstLastname(nameParts.firstLastname);
                        specialist.setSecondLastname(nameParts.secondLastname);
                        specialist.setEmail(normalize(dto.getEmail()));
                        specialist.setBiography(normalizeNullable(dto.getBiography()));
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
        dto.setFullName(buildFullName(specialist.getNames(), specialist.getFirstLastname(), specialist.getSecondLastname()));
        dto.setEmail(specialist.getEmail());
        dto.setBiography(specialist.getBiography());
        return dto;
    }

    private String buildFullName(String names, String firstLastname, String secondLastname) {
        List<String> parts = new ArrayList<>();

        String normalizedNames = normalizeNullable(names);
        String normalizedFirstLastname = normalizeNullable(firstLastname);
        String normalizedSecondLastname = normalizeNullable(secondLastname);

        if (normalizedNames != null) {
            parts.add(normalizedNames);
        }
        if (normalizedFirstLastname != null) {
            parts.add(normalizedFirstLastname);
        }
        if (normalizedSecondLastname != null) {
            parts.add(normalizedSecondLastname);
        }

        return String.join(" ", parts);
    }

    private NameParts splitFullName(String fullName) {
        String[] tokens = fullName.split(" ");

        if (tokens.length == 1) {
            return new NameParts(tokens[0], "", null);
        }

        if (tokens.length == 2) {
            return new NameParts(tokens[0], tokens[1], null);
        }

        StringBuilder namesBuilder = new StringBuilder();
        for (int i = 0; i < tokens.length - 2; i++) {
            if (i > 0) {
                namesBuilder.append(" ");
            }
            namesBuilder.append(tokens[i]);
        }

        String names = namesBuilder.toString();
        String firstLastname = tokens[tokens.length - 2];
        String secondLastname = tokens[tokens.length - 1];

        return new NameParts(names, firstLastname, secondLastname);
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

    private static class NameParts {
        private final String names;
        private final String firstLastname;
        private final String secondLastname;

        private NameParts(String names, String firstLastname, String secondLastname) {
            this.names = names;
            this.firstLastname = firstLastname;
            this.secondLastname = secondLastname;
        }
    }
}
