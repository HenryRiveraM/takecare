package com.takecare.backend.specialities.service;

import com.takecare.backend.specialities.dto.SpecialistFilterResponseDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpecialistSearchService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistSearchService.class);

    private static final Byte STATUS_AVAILABLE = 0;

    private final SpecialistRepository specialistRepository;
    private final SpecialistScheduleRepository scheduleRepository;

    public SpecialistSearchService(SpecialistRepository specialistRepository,
                                   SpecialistScheduleRepository scheduleRepository) {
        this.specialistRepository = specialistRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<SpecialistFilterResponseDTO> searchSpecialists(String search, String name, String category, String city, String availability) {
        logger.info("Buscando especialistas | search={} | name={} | category={} | city={} | availability={}", search, name, category, city, availability);

        Byte dayOfWeek = parseDay(availability);

        String normalizedSearch = normalizeNullable(search);
        String normalizedName = normalizeNullable(name);
        String normalizedCategory = normalizeNullable(category);
        String normalizedCity = normalizeNullable(city);

        List<Specialist> specialists;

        if (normalizedSearch != null && normalizedName == null && normalizedCategory == null && normalizedCity == null) {
            specialists = specialistRepository.findBySearchTerm(normalizedSearch, dayOfWeek);
        } else {
            specialists = specialistRepository.findByFilters(
                    normalizedName,
                    normalizedCategory,
                    normalizedCity,
                    dayOfWeek
            );
        }

        logger.info("Filtro aplicado | resultados={}", specialists.size());

        return specialists.stream()
                .map(s -> toDTO(s, dayOfWeek))
                .collect(Collectors.toList());
    }

    private Byte parseDay(String availability) {
        if (availability == null || availability.isBlank()) {
            return null;
        }

        String value = availability.trim().toUpperCase();

        return switch (value) {
            case "MONDAY", "LUNES", "1" -> 1;
            case "TUESDAY", "MARTES", "2" -> 2;
            case "WEDNESDAY", "MIERCOLES", "MIÉRCOLES", "3" -> 3;
            case "THURSDAY", "JUEVES", "4" -> 4;
            case "FRIDAY", "VIERNES", "5" -> 5;
            case "SATURDAY", "SABADO", "SÁBADO", "6" -> 6;
            case "SUNDAY", "DOMINGO", "7" -> 7;
            default -> {
                logger.warn("Valor de availability inválido: {}", availability);
                yield null;
            }
        };
    }

    private SpecialistFilterResponseDTO toDTO(Specialist s, Byte dayOfWeek) {
        List<SpecialistSchedule> schedules = dayOfWeek != null
                ? scheduleRepository.findBySpecialistIdAndDayOfWeekAndStatus(
                        s.getId(),
                        dayOfWeek,
                        STATUS_AVAILABLE
                )
                : scheduleRepository.findBySpecialistIdAndStatus(
                        s.getId(),
                        STATUS_AVAILABLE
                );

        List<SpecialistFilterResponseDTO.ScheduleDTO> scheduleDTOs = schedules.stream()
                .map(sc -> SpecialistFilterResponseDTO.ScheduleDTO.builder()
                        .dayOfWeek(sc.getDayOfWeek())
                        .startTime(sc.getStartTime())
                        .endTime(sc.getEndTime())
                        .build())
                .collect(Collectors.toList());

        List<String> specialityNames = s.getSpecialties() == null ? List.of() :
                s.getSpecialties().stream()
                        .map(sp -> sp.getName())
                        .collect(Collectors.toList());

        return SpecialistFilterResponseDTO.builder()
                .id(s.getId())
                .names(s.getNames())
                .firstLastname(s.getFirstLastname())
                .secondLastname(s.getSecondLastname())
                .email(s.getEmail())
                .biography(s.getBiography())
                .officeUbi(s.getOfficeUbi())
                .sessionCost(s.getSessionCost())
                .reputationAverage(s.getReputationAverage())
                .specialities(specialityNames)
                .availableSchedules(scheduleDTOs)
                .build();
    }
    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}