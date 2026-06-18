package com.takecare.backend.preventivealert.service;

import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanItem;
import com.takecare.backend.careplan.model.CarePlanItemStatus;
import com.takecare.backend.careplan.model.CarePlanStatus;
import com.takecare.backend.careplan.model.EmotionalRecord;
import com.takecare.backend.careplan.repository.CarePlanItemRepository;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.careplan.repository.EmotionalRecordRepository;
import com.takecare.backend.preventivealert.dto.PreventiveAlertResponseDTO;
import com.takecare.backend.preventivealert.model.PreventiveAlert;
import com.takecare.backend.preventivealert.repository.PreventiveAlertRepository;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PreventiveAlertService {

    private static final Logger logger = LoggerFactory.getLogger(PreventiveAlertService.class);

    private final PreventiveAlertRepository preventiveAlertRepository;
    private final CarePlanRepository carePlanRepository;
    private final EmotionalRecordRepository emotionalRecordRepository;
    private final CarePlanItemRepository carePlanItemRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;

    public PreventiveAlertService(
            PreventiveAlertRepository preventiveAlertRepository,
            CarePlanRepository carePlanRepository,
            EmotionalRecordRepository emotionalRecordRepository,
            CarePlanItemRepository carePlanItemRepository,
            SessionRepository sessionRepository,
            NotificationService notificationService
    ) {
        this.preventiveAlertRepository = preventiveAlertRepository;
        this.carePlanRepository = carePlanRepository;
        this.emotionalRecordRepository = emotionalRecordRepository;
        this.carePlanItemRepository = carePlanItemRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void evaluateCriticalStateRule(Integer patientId) {
        logger.info("Evaluating Critical State Rule for patientId={}", patientId);

        Optional<CarePlan> activePlanOpt = carePlanRepository
                .findFirstByPatientIdAndStatusOrderByCreatedDateDesc(patientId, CarePlanStatus.ACTIVE);

        if (activePlanOpt.isEmpty()) {
            logger.debug("No active care plan found for patientId={}. Skipping critical state rule.", patientId);
            return;
        }

        CarePlan plan = activePlanOpt.get();
        List<EmotionalRecord> records = emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(patientId);

        if (records.size() < 3) {
            logger.debug("Patient patientId={} has fewer than 3 emotional records. Skipping rule.", patientId);
            return;
        }

        // Rule: 3 records of mood <= 2, anxiety >= 4 or stress >= 4
        boolean isCritical = true;
        for (int i = 0; i < 3; i++) {
            EmotionalRecord r = records.get(i);
            int mood = r.getMoodLevel() != null ? r.getMoodLevel() : 3;
            int anxiety = r.getAnxietyLevel() != null ? r.getAnxietyLevel() : 1;
            int stress = r.getStressLevel() != null ? r.getStressLevel() : 1;

            if (mood > 2 && anxiety < 4 && stress < 4) {
                isCritical = false;
                break;
            }
        }

        if (isCritical) {
            boolean exists = preventiveAlertRepository.existsByPatientIdAndAlertTypeAndStatus(
                    patientId, "CRITICAL_STATE", "OPEN"
            );
            if (!exists) {
                PreventiveAlert alert = new PreventiveAlert();
                alert.setSpecialist(plan.getSpecialist());
                alert.setPatient(plan.getPatient());
                alert.setTitle("Estado de ánimo crítico");
                alert.setMessage("El paciente ha reportado niveles de ánimo bajos o niveles altos de ansiedad/estrés en sus últimos 3 registros. Se sugiere revisión y acompañamiento.");
                alert.setPriority("HIGH");
                alert.setAlertType("CRITICAL_STATE");
                alert.setStatus("OPEN");
                alert.setCreatedDate(LocalDateTime.now());

                preventiveAlertRepository.save(alert);
                logger.warn("ALERTA PREVENTIVA CRÍTICA DISPARADA: El paciente id={} asignado al especialista id={} ha entrado en un estado crítico emocional en sus últimos 3 registros.",
                        patientId, plan.getSpecialist().getId());

                sendSpecialistAlertNotification(plan.getSpecialist().getId(), patientId, plan.getId(),
                        "Alerta Crítica: El paciente " + getPatientFullName(plan.getPatient()) + " ha entrado en un estado de ánimo crítico.");
            }
        }
    }

    @Transactional
    public void evaluateAbandonmentRule(CarePlan carePlan) {
        Integer patientId = carePlan.getPatient().getId();
        logger.info("Evaluating Abandonment Rule for patientId={} on carePlanId={}", patientId, carePlan.getId());

        List<EmotionalRecord> records = emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(patientId);
        LocalDateTime baseTime = carePlan.getCreatedDate();

        if (!records.isEmpty()) {
            baseTime = records.get(0).getCreatedDate();
        }

        long days = ChronoUnit.DAYS.between(baseTime.toLocalDate(), LocalDate.now());
        String targetPriority = null;
        String targetMessage = null;

        if (days >= 7) {
            targetPriority = "HIGH";
            targetMessage = "El paciente no ha registrado su estado emocional durante los últimos 7 días.";
        } else if (days >= 4) {
            targetPriority = "MEDIUM";
            targetMessage = "El paciente no ha registrado su estado emocional durante los últimos 4 días.";
        } else if (days >= 2) {
            targetPriority = "LOW";
            targetMessage = "El paciente no ha registrado su estado emocional durante los últimos 2 días.";
        }

        if (targetPriority != null) {
            Optional<PreventiveAlert> existingOpt = preventiveAlertRepository
                    .findByPatientIdAndAlertTypeAndStatus(patientId, "ABANDONMENT", "OPEN");

            if (existingOpt.isPresent()) {
                PreventiveAlert alert = existingOpt.get();
                if (!alert.getPriority().equals(targetPriority)) {
                    alert.setPriority(targetPriority);
                    alert.setMessage(targetMessage);
                    alert.setCreatedDate(LocalDateTime.now());
                    preventiveAlertRepository.save(alert);
                    if ("HIGH".equals(targetPriority)) {
                        logger.warn("ALERTA PREVENTIVA DE ABANDONO ESCALADA A ALTA para el paciente id={} asignado al especialista id={}",
                                patientId, carePlan.getSpecialist().getId());
                    }
                    sendSpecialistAlertNotification(carePlan.getSpecialist().getId(), patientId, carePlan.getId(),
                            "Alerta Escalada: El paciente " + getPatientFullName(carePlan.getPatient()) + " " + targetMessage.toLowerCase());
                }
            } else {
                PreventiveAlert alert = new PreventiveAlert();
                alert.setSpecialist(carePlan.getSpecialist());
                alert.setPatient(carePlan.getPatient());
                alert.setTitle("Ausencia de registros");
                alert.setMessage(targetMessage);
                alert.setPriority(targetPriority);
                alert.setAlertType("ABANDONMENT");
                alert.setStatus("OPEN");
                alert.setCreatedDate(LocalDateTime.now());

                preventiveAlertRepository.save(alert);
                if ("HIGH".equals(targetPriority)) {
                    logger.warn("ALERTA PREVENTIVA DE ABANDONO INICIAL ALTA para el paciente id={} asignado al especialista id={}",
                            patientId, carePlan.getSpecialist().getId());
                }
                sendSpecialistAlertNotification(carePlan.getSpecialist().getId(), patientId, carePlan.getId(),
                        "Alerta: El paciente " + getPatientFullName(carePlan.getPatient()) + " " + targetMessage.toLowerCase());
            }
        }
    }

    @Transactional
    public void evaluateTaskRules(CarePlan carePlan) {
        logger.info("Evaluating Task Rules for carePlanId={}", carePlan.getId());

        List<CarePlanItem> items = carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(carePlan.getId());
        LocalDate today = LocalDate.now();

        for (CarePlanItem item : items) {
            if (item.getStatus() != CarePlanItemStatus.PENDING || item.getDueDate() == null) {
                continue;
            }

            LocalDate dueDate = item.getDueDate();
            String targetPriority = null;
            String targetAlertType = null;
            String targetTitle = null;
            String targetMessage = null;

            if (dueDate.isBefore(today)) {
                targetPriority = "MEDIUM";
                targetAlertType = "TASK_OVERDUE";
                targetTitle = "Actividad vencida";
                targetMessage = "La actividad '" + item.getTitle() + "' ha superado su fecha de vencimiento sin completarse.";
            } else if (!dueDate.isBefore(today) && dueDate.isBefore(today.plusDays(3))) {
                targetPriority = "LOW";
                targetAlertType = "TASK_DUE_SOON";
                targetTitle = "Actividad próxima a vencer";
                targetMessage = "La actividad '" + item.getTitle() + "' está próxima a vencer (menos de 3 días) y sigue pendiente.";
            }

            if (targetPriority != null) {
                Optional<PreventiveAlert> existingOpt = preventiveAlertRepository
                        .findByCarePlanItemIdAndStatus(item.getId(), "OPEN");

                if (existingOpt.isPresent()) {
                    PreventiveAlert alert = existingOpt.get();
                    // Escalate from TASK_DUE_SOON to TASK_OVERDUE
                    if ("TASK_DUE_SOON".equals(alert.getAlertType()) && "TASK_OVERDUE".equals(targetAlertType)) {
                        alert.setPriority("MEDIUM");
                        alert.setAlertType("TASK_OVERDUE");
                        alert.setTitle("Actividad vencida");
                        alert.setMessage(targetMessage);
                        alert.setCreatedDate(LocalDateTime.now());
                        preventiveAlertRepository.save(alert);

                        sendSpecialistAlertNotification(carePlan.getSpecialist().getId(), carePlan.getPatient().getId(), carePlan.getId(),
                                "Alerta Escalada: El paciente " + getPatientFullName(carePlan.getPatient()) + " tiene la actividad '" + item.getTitle() + "' vencida.");
                    }
                } else {
                    PreventiveAlert alert = new PreventiveAlert();
                    alert.setSpecialist(carePlan.getSpecialist());
                    alert.setPatient(carePlan.getPatient());
                    alert.setCarePlanItemId(item.getId());
                    alert.setTitle(targetTitle);
                    alert.setMessage(targetMessage);
                    alert.setPriority(targetPriority);
                    alert.setAlertType(targetAlertType);
                    alert.setStatus("OPEN");
                    alert.setCreatedDate(LocalDateTime.now());

                    preventiveAlertRepository.save(alert);

                    String prefix = "TASK_OVERDUE".equals(targetAlertType) ? "Alerta: El paciente " : "Aviso: El paciente ";
                    String suffix = "TASK_OVERDUE".equals(targetAlertType) ? " tiene la actividad '" + item.getTitle() + "' vencida." 
                            : " tiene la actividad '" + item.getTitle() + "' próxima a vencer.";

                    sendSpecialistAlertNotification(carePlan.getSpecialist().getId(), carePlan.getPatient().getId(), carePlan.getId(),
                            prefix + getPatientFullName(carePlan.getPatient()) + suffix);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PreventiveAlertResponseDTO> getAlertsBySpecialist(Integer specialistId) {
        logger.info("Fetching alerts for specialistId={}", specialistId);
        return preventiveAlertRepository.findBySpecialistIdOrderByStatusAndCreatedDate(specialistId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public PreventiveAlertResponseDTO markAsReviewed(Long alertId, Integer specialistId) {
        logger.info("Marking alertId={} as reviewed by specialistId={}", alertId, specialistId);

        PreventiveAlert alert = preventiveAlertRepository.findByIdAndSpecialistId(alertId, specialistId)
                .orElseThrow(() -> new NoSuchElementException("Alerta no encontrada para este especialista"));

        alert.setStatus("REVIEWED");
        alert.setReviewedDate(LocalDateTime.now());

        PreventiveAlert saved = preventiveAlertRepository.save(alert);
        return toResponseDTO(saved);
    }

    private PreventiveAlertResponseDTO toResponseDTO(PreventiveAlert alert) {
        PreventiveAlertResponseDTO dto = new PreventiveAlertResponseDTO();
        dto.setId(alert.getId());
        dto.setPatientId(alert.getPatient() != null ? alert.getPatient().getId() : null);

        String patientName = "Paciente";
        if (alert.getPatient() != null) {
            String names = alert.getPatient().getNames() != null ? alert.getPatient().getNames() : "";
            String lastName = alert.getPatient().getFirstLastname() != null ? alert.getPatient().getFirstLastname() : "";
            patientName = (names + " " + lastName).trim();
            if (patientName.isEmpty()) {
                patientName = "Paciente";
            }
        }
        dto.setPatientName(patientName);
        dto.setPriority(alert.getPriority());
        dto.setTitle(alert.getTitle());
        dto.setMessage(alert.getMessage());
        dto.setAlertType(alert.getAlertType());
        dto.setStatus(alert.getStatus());
        dto.setCreatedDate(alert.getCreatedDate());
        dto.setDetectedAt(alert.getCreatedDate());
        dto.setReviewed("REVIEWED".equals(alert.getStatus()));
        dto.setReviewedAt(alert.getReviewedDate());
        return dto;
    }

    private String getPatientFullName(com.takecare.backend.user.model.Patient p) {
        if (p == null) return "Paciente";
        String names = p.getNames() != null ? p.getNames() : "";
        String lastName = p.getFirstLastname() != null ? p.getFirstLastname() : "";
        String fullName = (names + " " + lastName).trim();
        return fullName.isEmpty() ? "Paciente" : fullName;
    }

    private void sendSpecialistAlertNotification(Integer specialistId, Integer patientId, Long carePlanId, String message) {
        try {
            Session notifySession = null;
            
            // Try to find a session between this specialist and patient
            List<Session> sessions = sessionRepository.findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(
                    specialistId,
                    patientId
            );
            if (!sessions.isEmpty()) {
                notifySession = sessions.get(0);
            }

            // Fallback to any session of the specialist
            if (notifySession == null) {
                List<Session> specialistSessions = sessionRepository.findBySpecialistIdOrderByCreatedDateDesc(
                        specialistId
                );
                if (!specialistSessions.isEmpty()) {
                    notifySession = specialistSessions.get(0);
                }
            }

            if (notifySession != null) {
                notificationService.createForSpecialistCarePlan(
                        notifySession,
                        carePlanId,
                        message
                );
                logger.info("Alert notification sent successfully to specialistId={} for patientId={}", specialistId, patientId);
            } else {
                logger.warn("Could not send alert notification to specialistId={} because no session could be resolved", specialistId);
            }
        } catch (Exception e) {
            logger.error("Error sending alert notification to specialist: {}", e.getMessage(), e);
        }
    }
}
