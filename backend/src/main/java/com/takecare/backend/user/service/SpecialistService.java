package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.specialities.model.Speciality;
import com.takecare.backend.user.dto.SpecialistDetailResponseDto;
import com.takecare.backend.user.dto.SpecialistListItemResponseDto;
import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class SpecialistService extends UserService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistService.class);
    private static final String OFFICE_UBI_DELIMITER = "|||TC|||";

    private final SpecialistRepository specialistRepository;

    public SpecialistService(SpecialistRepository specialistRepository,
                             BCryptPasswordEncoder passwordEncoder) {
        super(passwordEncoder);
        this.specialistRepository = specialistRepository;
    }

    public Specialist registerSpecialistFromDTO(SpecialistRegisterDTO dto) {
        logger.info("Registering new specialist with email: {}", dto.getEmail());
        Specialist specialist = new Specialist();
        specialist.setNames(dto.getNames());
        specialist.setFirstLastname(dto.getFirstLastname());
        specialist.setSecondLastname(dto.getSecondLastname());
        specialist.setBirthDate(dto.getBirthDate());
        specialist.setCiNumber(dto.getCiNumber());
        specialist.setEmail(dto.getEmail());
        specialist.setPasswordHash(dto.getPassword());
        specialist.setBiography(dto.getBiography());
        specialist.setCiDocumentImg(dto.getCiDocumentImg());
        specialist.setCertificationImg(dto.getCertificationImg());
        specialist.setSessionCost(dto.getSessionCost());

        prepareUser(specialist, 2);

        Specialist saved = specialistRepository.save(specialist);
        logger.info("Specialist registered successfully with id: {}", saved.getId());
        return saved;
    }

    public Specialist registerSpecialist(Specialist specialist) {
        logger.info("Registering specialist entity directly");
        Specialist saved = specialistRepository.save(prepareUser(specialist, 2));
        logger.info("Specialist registered with id: {}", saved.getId());
        return saved;
    }

    public List<Specialist> getAllSpecialists() {
        logger.info("Fetching all specialists from repository");
        List<Specialist> specialists = specialistRepository.findAll();
        logger.info("Found {} specialists", specialists.size());
        return specialists;
    }

    public List<SpecialistListItemResponseDto> getAllSpecialists(String search) {
        try {
            String normalizedSearch = normalizeNullable(search);
            logger.info("Fetching visible specialists with search: {}", normalizedSearch);

            List<Specialist> specialists = normalizedSearch == null
                    ? specialistRepository.findVisibleSpecialists()
                    : specialistRepository.searchVisibleSpecialists(normalizedSearch);

            List<SpecialistListItemResponseDto> response = specialists.stream()
                    .map(this::toListItemDto)
                    .toList();

            logger.info("Found {} visible specialists", response.size());
            return response;
        } catch (RuntimeException e) {
            logger.error("Error fetching visible specialists", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching visible specialists", e);
            throw new RuntimeException("Error al obtener especialistas");
        }
    }

    public Optional<SpecialistDetailResponseDto> getSpecialistById(Integer id) {
        try {
            logger.info("Fetching visible specialist detail with id: {}", id);

            Optional<SpecialistDetailResponseDto> specialist = specialistRepository
                    .findVisibleSpecialistById(id)
                    .map(this::toDetailDto);

            if (specialist.isPresent()) {
                logger.info("Visible specialist detail found with id: {}", id);
            } else {
                logger.warn("No visible specialist found with id: {}", id);
            }

            return specialist;
        } catch (RuntimeException e) {
            logger.error("Error fetching visible specialist detail with id: {}", id, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error fetching visible specialist detail with id: {}", id, e);
            throw new RuntimeException("Error al obtener especialista");
        }
    }

    public Optional<Specialist> updateSpecialist(Integer id, Specialist specialistDetails) {
        logger.info("Attempting to update specialist with id: {}", id);
        return specialistRepository.findById(id)
            .map(specialist -> {
                specialist.setNames(specialistDetails.getNames());
                specialist.setFirstLastname(specialistDetails.getFirstLastname());
                specialist.setSecondLastname(specialistDetails.getSecondLastname());
                specialist.setBirthDate(specialistDetails.getBirthDate());
                specialist.setCiNumber(specialistDetails.getCiNumber());
                specialist.setEmail(specialistDetails.getEmail());
                specialist.setBiography(specialistDetails.getBiography());
                specialist.setCertificationImg(specialistDetails.getCertificationImg());
                specialist.setSessionCost(specialistDetails.getSessionCost());
                Specialist updated = specialistRepository.save(specialist);
                logger.info("Specialist with id: {} updated successfully", id);
                return updated;
            });
    }

    public boolean deleteSpecialist(Integer id) {
        logger.info("Attempting logical delete for specialist with id: {}", id);
        return specialistRepository.findById(id)
            .map(specialist -> {
                specialist.setStatus(0);
                specialist.setLastUpdate(LocalDateTime.now());
                specialistRepository.save(specialist);
                logger.info("Specialist with id: {} marked as inactive successfully", id);
                return true;
            }).orElseGet(() -> {
                logger.warn("Cannot delete - no specialist found with id: {}", id);
                return false;
            });
    }

    public Optional<Specialist> validateSpecialist(Integer id, boolean approved) {
        int verificationStatus = approved ? ACCOUNT_VERIFIED_APPROVED : ACCOUNT_VERIFIED_REJECTED;

        return specialistRepository.findById(id)
            .map(specialist -> {
                specialist.setAccountVerified(verificationStatus);
                specialist.setLastUpdate(LocalDateTime.now());
                return specialistRepository.save(specialist);
            });
    }

    private SpecialistListItemResponseDto toListItemDto(Specialist specialist) {
        SpecialistListItemResponseDto dto = new SpecialistListItemResponseDto();
        dto.setId(specialist.getId());
        dto.setFullName(buildFullName(specialist));
        dto.setBiography(normalizeNullable(specialist.getBiography()));
        dto.setOfficeUbi(formatOfficeUbiForPublicView(specialist.getOfficeUbi()));
        dto.setSpecialties(mapSpecialties(specialist));
        dto.setReputationAverage(specialist.getReputationAverage());
        dto.setSessionCost(specialist.getSessionCost());
        dto.setCertificationImg(normalizeNullable(specialist.getCertificationImg()));
        return dto;
    }

    private SpecialistDetailResponseDto toDetailDto(Specialist specialist) {
        SpecialistDetailResponseDto dto = new SpecialistDetailResponseDto();
        dto.setId(specialist.getId());
        dto.setFullName(buildFullName(specialist));
        dto.setEmail(normalizeNullable(specialist.getEmail()));
        dto.setBirthDate(specialist.getBirthDate());
        dto.setBiography(normalizeNullable(specialist.getBiography()));
        dto.setOfficeUbi(formatOfficeUbiForPublicView(specialist.getOfficeUbi()));
        dto.setSpecialties(mapSpecialties(specialist));
        dto.setReputationAverage(specialist.getReputationAverage());
        dto.setSessionCost(specialist.getSessionCost());
        dto.setCertificationImg(normalizeNullable(specialist.getCertificationImg()));
        return dto;
    }

    private List<String> mapSpecialties(Specialist specialist) {
        if (specialist.getSpecialities() == null || specialist.getSpecialities().isEmpty()) {
            return List.of();
        }

        return specialist.getSpecialities().stream()
                .map(Speciality::getName)
                .filter(Objects::nonNull)
                .map(this::normalizeOptional)
                .filter(name -> !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
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

    private String formatOfficeUbiForPublicView(String officeUbi) {
        String normalizedOfficeUbi = normalizeNullable(officeUbi);
        if (normalizedOfficeUbi == null) {
            return null;
        }

        if (!normalizedOfficeUbi.contains(OFFICE_UBI_DELIMITER)) {
            return normalizedOfficeUbi;
        }

        String formattedOfficeUbi = Arrays.stream(
                    normalizedOfficeUbi.split(Pattern.quote(OFFICE_UBI_DELIMITER), -1)
                )
                .map(this::normalizeOptional)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));

        return formattedOfficeUbi.isBlank() ? null : formattedOfficeUbi;
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
}
