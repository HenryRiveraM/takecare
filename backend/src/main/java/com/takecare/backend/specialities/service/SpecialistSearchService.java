package com.takecare.backend.specialities.service;

import com.takecare.backend.specialities.dto.SpecialistFilterResponseDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpecialistSearchService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistSearchService.class);

    private final SpecialistRepository specialistRepository;
    private final SpecialistScheduleRepository scheduleRepository;

    public SpecialistSearchService(SpecialistRepository specialistRepository,
                                   SpecialistScheduleRepository scheduleRepository) {
        this.specialistRepository = specialistRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<SpecialistFilterResponseDTO> searchSpecialists(String category, String availability) {
        logger.info("Buscando especialistas | category={} | availability={}", category, availability);

        DayOfWeek dayOfWeek = parseDay(availability);
        List<Specialist> specialists;

        boolean hasCategory = category != null && !category.isBlank();
        boolean hasDay = dayOfWeek != null;

        if (hasCategory && hasDay) {
            specialists = specialistRepository.findBySpecialityNameAndAvailability(category, dayOfWeek);
            logger.info("Filtro combinado | resultados={}", specialists.size());
        } else if (hasCategory) {
            specialists = specialistRepository.findBySpecialityName(category);
            logger.info("Filtro por categoría | resultados={}", specialists.size());
        } else if (hasDay) {
            specialists = specialistRepository.findByAvailability(dayOfWeek);
            logger.info("Filtro por disponibilidad | resultados={}", specialists.size());
        } else {
            specialists = specialistRepository.findAll();
            logger.info("Sin filtros, devolviendo todos | resultados={}", specialists.size());
        }

        return specialists.stream()
                .map(s -> toDTO(s, dayOfWeek))
                .collect(Collectors.toList());
    }

    private DayOfWeek parseDay(String availability) {
        if (availability == null || availability.isBlank()) return null;
        try {
            return DayOfWeek.valueOf(availability.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Valor de availability inválido: {}", availability);
            return null;
        }
    }

    private SpecialistFilterResponseDTO toDTO(Specialist s, DayOfWeek dayOfWeek) {
        List<SpecialistSchedule> schedules = dayOfWeek != null
                ? scheduleRepository.findBySpecialistIdAndDayOfWeekAndAvailableTrue(s.getId(), dayOfWeek)
                : scheduleRepository.findBySpecialistIdAndAvailableTrue(s.getId());

        List<SpecialistFilterResponseDTO.ScheduleDTO> scheduleDTOs = schedules.stream()
                .map(sc -> SpecialistFilterResponseDTO.ScheduleDTO.builder()
                        .dayOfWeek(sc.getDayOfWeek())
                        .startTime(sc.getStartTime())
                        .endTime(sc.getEndTime())
                        .build())
                .collect(Collectors.toList());

        List<String> specialityNames = s.getSpecialities() == null ? List.of() :
                s.getSpecialities().stream()
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
}
