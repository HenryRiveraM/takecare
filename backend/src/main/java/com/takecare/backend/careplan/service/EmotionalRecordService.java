package com.takecare.backend.careplan.service;

import com.takecare.backend.careplan.dto.EmotionalRecordRequestDTO;
import com.takecare.backend.careplan.dto.EmotionalRecordResponseDTO;
import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanStatus;
import com.takecare.backend.careplan.model.EmotionalRecord;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.careplan.repository.EmotionalRecordRepository;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;
import com.takecare.backend.user.repository.SpecialistRepository;
import com.takecare.backend.preventivealert.service.PreventiveAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EmotionalRecordService {

    private static final Logger logger = LoggerFactory.getLogger(EmotionalRecordService.class);

    private static final Integer SESSION_ACCEPTED = 2;
    private static final Integer SESSION_FINISHED = 4;

    private final EmotionalRecordRepository emotionalRecordRepository;
    private final PatientRepository patientRepository;
    private final CarePlanRepository carePlanRepository;
    private final SessionRepository sessionRepository;
    private final SpecialistRepository specialistRepository;
    private final PreventiveAlertService preventiveAlertService;

    public EmotionalRecordService(
            EmotionalRecordRepository emotionalRecordRepository,
            PatientRepository patientRepository,
            CarePlanRepository carePlanRepository,
            SessionRepository sessionRepository,
            SpecialistRepository specialistRepository,
            PreventiveAlertService preventiveAlertService
    ) {
        this.emotionalRecordRepository = emotionalRecordRepository;
        this.patientRepository = patientRepository;
        this.carePlanRepository = carePlanRepository;
        this.sessionRepository = sessionRepository;
        this.specialistRepository = specialistRepository;
        this.preventiveAlertService = preventiveAlertService;
    }

    @Transactional
    public EmotionalRecordResponseDTO createRecord(Integer patientId, EmotionalRecordRequestDTO request) {
        logger.info("Creating emotional record for patientId={}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado"));

        EmotionalRecord record = new EmotionalRecord();
        record.setPatient(patient);
        record.setMoodLevel(request.getMoodLevel());
        record.setAnxietyLevel(request.getAnxietyLevel());
        record.setStressLevel(request.getStressLevel());
        record.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        record.setCreatedDate(LocalDateTime.now());
        record.setMoodState(mapMoodLevelToState(request.getMoodLevel()));

        // Automatically associate the active care plan of the patient if it exists
        carePlanRepository.findFirstByPatientIdAndStatusOrderByCreatedDateDesc(patientId, CarePlanStatus.ACTIVE)
                .ifPresent(record::setCarePlan);

        EmotionalRecord saved = emotionalRecordRepository.save(record);
        logger.info("Emotional record created with id={} for patientId={}", saved.getId(), patientId);

        try {
            preventiveAlertService.evaluateCriticalStateRule(patientId);
        } catch (Exception e) {
            logger.error("Error evaluating critical state rule for patientId={}: {}", patientId, e.getMessage(), e);
        }

        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<EmotionalRecordResponseDTO> getRecords(Integer patientId) {
        logger.info("Fetching emotional records for patientId={}", patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new NoSuchElementException("Paciente no encontrado");
        }

        return emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(patientId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmotionalRecordResponseDTO> getRecordsForSpecialist(Integer specialistId, Integer patientId) {
        logger.info("Fetching emotional records for specialistId={} of patientId={}", specialistId, patientId);

        if (!specialistRepository.existsById(specialistId)) {
            throw new NoSuchElementException("Especialista no encontrado");
        }

        if (!patientRepository.existsById(patientId)) {
            throw new NoSuchElementException("Paciente no encontrado");
        }

        // Validate specialist-patient relationship
        boolean hasRelationship = sessionRepository.existsRelationshipBySpecialistAndPatientAndStatuses(
                specialistId,
                patientId,
                List.of(SESSION_ACCEPTED, SESSION_FINISHED)
        );

        if (!hasRelationship) {
            logger.warn("Access denied. Specialist specialistId={} does not have relationship with patientId={}", specialistId, patientId);
            throw new SecurityException("El especialista no tiene una cita aceptada o finalizada con este paciente");
        }

        return emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(patientId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private String mapMoodLevelToState(Integer level) {
        if (level == null) return "NEUTRAL";
        return switch (level) {
            case 5 -> "EXCELLENT";
            case 4 -> "GOOD";
            case 3 -> "NEUTRAL";
            case 2 -> "LOW";
            case 1 -> "VERY_LOW";
            default -> "NEUTRAL";
        };
    }

    private EmotionalRecordResponseDTO toResponseDTO(EmotionalRecord record) {
        EmotionalRecordResponseDTO dto = new EmotionalRecordResponseDTO();
        dto.setId(record.getId());
        dto.setPatientId(record.getPatient() != null ? record.getPatient().getId() : null);
        dto.setCarePlanId(record.getCarePlan() != null ? record.getCarePlan().getId() : null);
        dto.setMoodLevel(record.getMoodLevel());
        dto.setAnxietyLevel(record.getAnxietyLevel());
        dto.setStressLevel(record.getStressLevel());
        dto.setMoodState(record.getMoodState());
        dto.setNotes(record.getNotes());
        dto.setCreatedDate(record.getCreatedDate());
        return dto;
    }
}
