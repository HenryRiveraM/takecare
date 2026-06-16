package com.takecare.backend.careplan.service;

import com.takecare.backend.careplan.dto.CarePlanItemRequestDTO;
import com.takecare.backend.careplan.dto.CarePlanItemResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanActivityListResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanActivityProgressResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanActivityRequestDTO;
import com.takecare.backend.careplan.dto.CarePlanListResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanResponseDTO;
import com.takecare.backend.careplan.dto.CarePlanSummaryDTO;
import com.takecare.backend.careplan.dto.CreateCarePlanRequestDTO;
import com.takecare.backend.careplan.dto.UpdateCarePlanItemRequestDTO;
import com.takecare.backend.careplan.dto.UpdateCarePlanRequestDTO;
import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanItem;
import com.takecare.backend.careplan.model.CarePlanItemStatus;
import com.takecare.backend.careplan.model.CarePlanItemType;
import com.takecare.backend.careplan.model.CarePlanStatus;
import com.takecare.backend.careplan.repository.CarePlanItemRepository;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.notification.service.NotificationService;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.PatientRepository;
import com.takecare.backend.user.repository.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class CarePlanService {

    private static final Logger logger = LoggerFactory.getLogger(CarePlanService.class);

    private static final Integer SESSION_ACCEPTED = 2;
    private static final Integer SESSION_FINISHED = 4;
    private static final Integer SESSION_PENDING = 1;
    private static final Integer SESSION_CANCELLED = 5;
    private static final Integer SESSION_TYPE_PRESENTIAL = 2;
    private static final Byte SCHEDULE_AVAILABLE = 0;
    private static final Byte SCHEDULE_UNAVAILABLE = 1;
    private static final int OBJECTIVES_MAX_LENGTH = 1000;
    private static final int RECOMMENDATIONS_MAX_LENGTH = 1000;
    private static final int OBSERVATIONS_MAX_LENGTH = 700;
    private static final int ITEM_DESCRIPTION_MAX_LENGTH = 500;
    private static final String COMPLETED_PLAN_LOCK_MESSAGE = "Este plan ya fue completado y no puede modificarse.";
    private static final String COMPLETED_PLAN_REACTIVATE_MESSAGE = "Un plan completado no puede reactivarse ni modificarse.";
    private static final String COMPLETED_ACTIVITY_LOCK_MESSAGE =
            "No se puede modificar una actividad que ya fue completada por el paciente.";

    private final CarePlanRepository carePlanRepository;
    private final CarePlanItemRepository carePlanItemRepository;
    private final SpecialistRepository specialistRepository;
    private final PatientRepository patientRepository;
    private final SessionRepository sessionRepository;
    private final SpecialistScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    public CarePlanService(
            CarePlanRepository carePlanRepository,
            CarePlanItemRepository carePlanItemRepository,
            SpecialistRepository specialistRepository,
            PatientRepository patientRepository,
            SessionRepository sessionRepository,
            SpecialistScheduleRepository scheduleRepository,
            NotificationService notificationService
    ) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanItemRepository = carePlanItemRepository;
        this.specialistRepository = specialistRepository;
        this.patientRepository = patientRepository;
        this.sessionRepository = sessionRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CarePlanResponseDTO createCarePlan(
            Integer specialistId,
            Integer patientId,
            CreateCarePlanRequestDTO request
    ) {
        logger.info("Creating care plan. specialistId={}, patientId={}", specialistId, patientId);

        Specialist specialist = specialistRepository.findById(specialistId)
                .orElseThrow(() -> new NoSuchElementException("Especialista no encontrado"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Paciente no encontrado"));

        validateCreateRequest(request);
        validateSpecialistPatientRelationship(specialistId, patientId);
        validateReviewTimeAvailability(
                specialistId,
                request.getReviewDate(),
                request.getReviewStartTime(),
                request.getReviewEndTime()
        );

        SpecialistSchedule reviewSchedule = createReviewSchedule(specialist, request);
        Session reviewSession = createReviewSession(patient, reviewSchedule);

        CarePlan carePlan = new CarePlan();
        carePlan.setSpecialist(specialist);
        carePlan.setPatient(patient);
        carePlan.setTitle(request.getTitle().trim());
        carePlan.setTherapeuticObjectives(request.getTherapeuticObjectives().trim());
        carePlan.setGeneralRecommendations(request.getGeneralRecommendations().trim());
        carePlan.setProfessionalObservations(cleanNullableText(request.getProfessionalObservations()));
        carePlan.setReviewDate(reviewSchedule.getScheduleDate());
        carePlan.setReviewSession(reviewSession);
        carePlan.setStatus(CarePlanStatus.ACTIVE);
        carePlan.setProgressPercentage(0);
        carePlan.setArchivedBySpecialist(false);
        carePlan.setArchivedDate(null);
        carePlan.setCreatedDate(LocalDateTime.now());

        CarePlan saved = carePlanRepository.save(carePlan);
        logger.info("Care plan saved. carePlanId={}, reviewSessionId={}",
                saved.getId(), reviewSession.getId());

        List<CarePlanItem> items = request.getItems().stream()
                .map(itemRequest -> buildItem(saved, itemRequest))
                .toList();
        carePlanItemRepository.saveAll(items);
        logger.info("Care plan items created. carePlanId={}, totalItems={}", saved.getId(), items.size());

        recalculateProgress(saved);
        carePlanRepository.save(saved);

        notificationService.createForCarePlan(
                reviewSession,
                saved.getId(),
                "Se agendo una cita de seguimiento para tu plan de atencion el "
                        + reviewSchedule.getScheduleDate()
                        + " de "
                        + reviewSchedule.getStartTime()
                        + " a "
                        + reviewSchedule.getEndTime()
        );
        logger.info("Patient notified for care plan review. carePlanId={}, patientId={}, sessionId={}",
                saved.getId(), patientId, reviewSession.getId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CarePlanListResponseDTO listCarePlans(Integer specialistId, Integer patientId) {
        logger.info("Listing care plans. specialistId={}, patientId={}", specialistId, patientId);

        if (!specialistRepository.existsById(specialistId)) {
            throw new NoSuchElementException("Especialista no encontrado");
        }

        if (!patientRepository.existsById(patientId)) {
            throw new NoSuchElementException("Paciente no encontrado");
        }

        validateSpecialistPatientRelationship(specialistId, patientId);

        return buildListResponse(carePlanRepository
                .findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(specialistId, patientId)
                .stream()
                .filter(plan -> !Boolean.TRUE.equals(plan.getArchivedBySpecialist()))
                .toList());
    }

    @Transactional(readOnly = true)
    public CarePlanListResponseDTO listCarePlansBySpecialist(Integer specialistId) {
        logger.info("Listing care plans for specialist. specialistId={}", specialistId);

        if (!specialistRepository.existsById(specialistId)) {
            throw new NoSuchElementException("Especialista no encontrado");
        }

        return buildListResponse(carePlanRepository.findBySpecialistIdOrderByCreatedDateDesc(specialistId)
                .stream()
                .filter(plan -> !Boolean.TRUE.equals(plan.getArchivedBySpecialist()))
                .toList());
    }

    @Transactional(readOnly = true)
    public CarePlanListResponseDTO listCarePlansByPatient(Integer patientId) {
        logger.info("Listing care plans for patient. patientId={}", patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new NoSuchElementException("Paciente no encontrado");
        }

        return buildListResponse(carePlanRepository.findByPatientIdOrderByCreatedDateDesc(patientId));
    }

    @Transactional(readOnly = true)
    public CarePlanResponseDTO getCarePlan(Long planId, Integer specialistId, Integer patientId) {
        logger.info("Getting care plan detail. carePlanId={}, specialistId={}, patientId={}",
                planId, specialistId, patientId);

        CarePlan carePlan = findPlan(planId);
        validateViewPermission(carePlan, specialistId, patientId);
        return toResponse(carePlan);
    }

    @Transactional
    public CarePlanResponseDTO updateCarePlan(
            Long planId,
            Integer specialistId,
            UpdateCarePlanRequestDTO request
    ) {
        logger.info("Updating care plan. carePlanId={}, specialistId={}", planId, specialistId);

        CarePlan carePlan = findPlan(planId);
        validateSpecialistOwner(carePlan, specialistId);
        validateCompletedPlanAllowsUpdate(carePlan, request);

        CarePlanSnapshot snapshot = CarePlanSnapshot.from(carePlan);
        applyUpdates(carePlan, request);
        carePlan.setUpdatedDate(LocalDateTime.now());

        CarePlan saved = carePlanRepository.save(carePlan);
        notifyPatientForCarePlanChanges(saved, snapshot);
        logger.info("Care plan updated successfully. carePlanId={}, status={}, progress={}",
                saved.getId(), saved.getStatus(), saved.getProgressPercentage());

        return toResponse(saved);
    }

    @Transactional
    public void deleteCarePlan(Long planId, Integer specialistId) {
        logger.info("Deleting care plan. carePlanId={}, specialistId={}", planId, specialistId);

        CarePlan carePlan = findPlan(planId);
        validateSpecialistOwner(carePlan, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        cancelReviewSessionForDeletedPlan(carePlan);

        carePlanItemRepository.deleteByCarePlanId(planId);
        carePlanRepository.delete(carePlan);

        logger.info("Care plan deleted successfully. carePlanId={}, specialistId={}", planId, specialistId);
    }

    @Transactional
    public void archiveCarePlan(Long planId, Integer specialistId) {
        logger.info("Archiving completed care plan. carePlanId={}, specialistId={}", planId, specialistId);

        CarePlan carePlan = findPlan(planId);
        validateSpecialistOwner(carePlan, specialistId);

        if (carePlan.getStatus() != CarePlanStatus.COMPLETED) {
            logger.warn("Care plan archive blocked because plan is not completed. carePlanId={}, status={}",
                    carePlan.getId(), carePlan.getStatus());
            throw new IllegalStateException("Solo se pueden archivar planes completados.");
        }

        carePlan.setArchivedBySpecialist(true);
        carePlan.setArchivedDate(LocalDateTime.now());
        carePlan.setUpdatedDate(LocalDateTime.now());
        carePlanRepository.save(carePlan);

        logger.info("Completed care plan archived. carePlanId={}, specialistId={}", planId, specialistId);
    }

    @Transactional
    public CarePlanItemResponseDTO addItem(Long planId, Integer specialistId, CarePlanItemRequestDTO request) {
        CarePlan carePlan = findPlan(planId);
        validateSpecialistOwner(carePlan, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validateItemRequest(request);

        CarePlanItem item = buildItem(carePlan, request);
        CarePlanItem saved = carePlanItemRepository.save(item);
        recalculateAndSave(carePlan);

        logger.info("Care plan item added. carePlanId={}, itemId={}", planId, saved.getId());
        return toItemResponse(saved);
    }

    @Transactional
    public CarePlanItemResponseDTO createActivity(
            Long planId,
            Integer specialistId,
            CarePlanActivityRequestDTO request
    ) {
        logger.info("Creating care plan activity. carePlanId={}, specialistId={}", planId, specialistId);

        CarePlan carePlan = findPlan(planId);
        validateSpecialistOwner(carePlan, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validatePlanAllowsActivities(carePlan);
        validateActivityRequest(request);

        CarePlanItem activity = new CarePlanItem();
        activity.setCarePlan(carePlan);
        activity.setTitle(request.getTitle().trim());
        activity.setDescription(cleanNullableText(request.getDescription()));
        activity.setItemType(CarePlanItemType.ACTIVITY);
        activity.setStatus(CarePlanItemStatus.PENDING);
        activity.setDueDate(request.getDueDate());
        activity.setCompletedDate(null);
        activity.setCreatedDate(LocalDateTime.now());

        CarePlanItem saved = carePlanItemRepository.save(activity);
        recalculateAndSave(carePlan);

        logger.info("Care plan activity created. carePlanId={}, activityId={}", planId, saved.getId());
        return toItemResponse(saved);
    }

    @Transactional(readOnly = true)
    public CarePlanActivityListResponseDTO listActivities(Long planId, Integer specialistId, Integer patientId) {
        logger.info("Listing care plan activities. carePlanId={}, specialistId={}, patientId={}",
                planId, specialistId, patientId);

        CarePlan carePlan = findPlan(planId);
        validateViewPermission(carePlan, specialistId, patientId);

        List<CarePlanItemResponseDTO> activities = carePlanItemRepository
                .findByCarePlanIdOrderByCreatedDateAsc(planId)
                .stream()
                .map(this::toItemResponse)
                .toList();

        CarePlanActivityListResponseDTO response = new CarePlanActivityListResponseDTO();
        response.setTotalActivities(activities.size());
        response.setActivities(activities);
        return response;
    }

    @Transactional
    public CarePlanItemResponseDTO updateItem(Long itemId, Integer specialistId, UpdateCarePlanItemRequestDTO request) {
        CarePlanItem item = findItem(itemId);
        CarePlan carePlan = item.getCarePlan();
        validateSpecialistOwner(carePlan, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validateActivityNotCompletedForSpecialistMutation(item, request);

        applyItemUpdates(item, request);
        item.setUpdatedDate(LocalDateTime.now());
        CarePlanItem saved = carePlanItemRepository.save(item);
        recalculateAndSave(carePlan);

        logger.info("Care plan item updated. itemId={}, status={}", itemId, saved.getStatus());
        return toItemResponse(saved);
    }

    @Transactional
    public CarePlanItemResponseDTO updateActivity(
            Long activityId,
            Integer specialistId,
            UpdateCarePlanItemRequestDTO request
    ) {
        logger.info("Updating care plan activity. activityId={}, specialistId={}", activityId, specialistId);

        CarePlanItem item = findItem(activityId);
        CarePlan carePlan = item.getCarePlan();
        validateSpecialistOwner(carePlan, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validatePlanAllowsActivities(carePlan);
        validateActivityNotCompletedForSpecialistMutation(item, request);

        if (request.getStatus() != null) {
            CarePlanItemStatus status = parseItemStatus(request.getStatus());
            if (status == CarePlanItemStatus.CANCELLED) {
                logger.info("Cancelling care plan activity. activityId={}, carePlanId={}", activityId, carePlan.getId());
            } else if (item.getStatus() == CarePlanItemStatus.CANCELLED && status == CarePlanItemStatus.PENDING) {
                logger.info("Restoring care plan activity. activityId={}, carePlanId={}", activityId, carePlan.getId());
            }
        }

        applyItemUpdates(item, request);
        item.setUpdatedDate(LocalDateTime.now());
        CarePlanItem saved = carePlanItemRepository.save(item);
        recalculateAndSave(carePlan);

        logger.info("Care plan activity updated. activityId={}, status={}", activityId, saved.getStatus());
        return toItemResponse(saved);
    }

    @Transactional
    public CarePlanResponseDTO completeItem(Long itemId, Integer patientId, Integer specialistId) {
        CarePlanItem item = findItem(itemId);
        CarePlan carePlan = item.getCarePlan();
        validateItemActionPermission(carePlan, patientId, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validatePlanActiveForActivityAction(carePlan);

        item.setStatus(CarePlanItemStatus.COMPLETED);
        item.setCompletedDate(LocalDateTime.now());
        item.setUpdatedDate(LocalDateTime.now());
        carePlanItemRepository.save(item);

        logger.info("Care plan item completed. itemId={}, carePlanId={}", itemId, carePlan.getId());
        recalculateAndSave(carePlan);
        return toResponse(carePlan);
    }

    @Transactional
    public CarePlanActivityProgressResponseDTO completeActivity(
            Long activityId,
            Integer patientId,
            Integer specialistId
    ) {
        CarePlanItem item = findItem(activityId);
        CarePlan carePlan = item.getCarePlan();
        validateItemActionPermission(carePlan, patientId, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validatePlanActiveForActivityAction(carePlan);

        item.setStatus(CarePlanItemStatus.COMPLETED);
        if (item.getCompletedDate() == null) {
            item.setCompletedDate(LocalDateTime.now());
        }
        item.setUpdatedDate(LocalDateTime.now());
        CarePlanItem saved = carePlanItemRepository.save(item);

        logger.info("Care plan activity completed. activityId={}, carePlanId={}", activityId, carePlan.getId());
        recalculateAndSave(carePlan);
        return toProgressResponse(saved);
    }

    @Transactional
    public CarePlanResponseDTO markItemPending(Long itemId, Integer patientId, Integer specialistId) {
        CarePlanItem item = findItem(itemId);
        CarePlan carePlan = item.getCarePlan();
        validateItemActionPermission(carePlan, patientId, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validatePlanActiveForActivityAction(carePlan);

        item.setStatus(CarePlanItemStatus.PENDING);
        item.setCompletedDate(null);
        item.setUpdatedDate(LocalDateTime.now());
        carePlanItemRepository.save(item);

        logger.info("Care plan item marked pending. itemId={}, carePlanId={}", itemId, carePlan.getId());
        recalculateAndSave(carePlan);
        return toResponse(carePlan);
    }

    @Transactional
    public CarePlanActivityProgressResponseDTO markActivityPending(
            Long activityId,
            Integer patientId,
            Integer specialistId
    ) {
        CarePlanItem item = findItem(activityId);
        CarePlan carePlan = item.getCarePlan();
        validateItemActionPermission(carePlan, patientId, specialistId);
        validatePlanNotCompletedForMutation(carePlan);
        validatePlanActiveForActivityAction(carePlan);

        item.setStatus(CarePlanItemStatus.PENDING);
        item.setCompletedDate(null);
        item.setUpdatedDate(LocalDateTime.now());
        CarePlanItem saved = carePlanItemRepository.save(item);

        logger.info("Care plan activity marked pending. activityId={}, carePlanId={}", activityId, carePlan.getId());
        recalculateAndSave(carePlan);
        return toProgressResponse(saved);
    }

    private CarePlanListResponseDTO buildListResponse(List<CarePlan> carePlans) {
        List<CarePlanSummaryDTO> plans = carePlans.stream()
                .sorted(this::compareCarePlansForList)
                .map(this::toSummary)
                .toList();

        CarePlanListResponseDTO response = new CarePlanListResponseDTO();
        response.setTotalCarePlans(plans.size());
        response.setCarePlans(plans);
        return response;
    }

    private int compareCarePlansForList(CarePlan first, CarePlan second) {
        int statusComparison = Integer.compare(statusOrder(first.getStatus()), statusOrder(second.getStatus()));
        if (statusComparison != 0) {
            return statusComparison;
        }

        LocalDateTime firstDate = first.getUpdatedDate() != null ? first.getUpdatedDate() : first.getCreatedDate();
        LocalDateTime secondDate = second.getUpdatedDate() != null ? second.getUpdatedDate() : second.getCreatedDate();

        if (firstDate == null && secondDate == null) {
            return 0;
        }
        if (firstDate == null) {
            return 1;
        }
        if (secondDate == null) {
            return -1;
        }
        return secondDate.compareTo(firstDate);
    }

    private int statusOrder(CarePlanStatus status) {
        if (status == null) {
            return 99;
        }
        return switch (status) {
            case ACTIVE -> 0;
            case PAUSED -> 1;
            case CANCELLED -> 2;
            case COMPLETED -> 3;
        };
    }

    private CarePlan findPlan(Long planId) {
        return carePlanRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan de cuidado no encontrado"));
    }

    private CarePlanItem findItem(Long itemId) {
        return carePlanItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Actividad del plan no encontrada"));
    }

    private Session createReviewSession(Patient patient, SpecialistSchedule schedule) {
        logger.info("Creating automatic review session. patientId={}, scheduleId={}",
                patient.getId(), schedule.getId());

        Session session = new Session();
        session.setPatient(patient);
        session.setSchedule(schedule);
        session.setStatus(SESSION_ACCEPTED);
        session.setTypeOfSession(SESSION_TYPE_PRESENTIAL);
        session.setCreatedDate(LocalDateTime.now());
        session.setDescription("Cita de seguimiento generada por plan de atencion");

        Session savedSession = sessionRepository.save(session);
        schedule.setStatus(SCHEDULE_UNAVAILABLE);
        scheduleRepository.save(schedule);

        logger.info("Automatic review session created. sessionId={}, scheduleId={}",
                savedSession.getId(), schedule.getId());
        return savedSession;
    }

    private SpecialistSchedule createReviewSchedule(Specialist specialist, CreateCarePlanRequestDTO request) {
        SpecialistSchedule schedule = new SpecialistSchedule();
        schedule.setSpecialist(specialist);
        schedule.setScheduleDate(request.getReviewDate());
        schedule.setDayOfWeek((byte) request.getReviewDate().getDayOfWeek().getValue());
        schedule.setStartTime(request.getReviewStartTime());
        schedule.setEndTime(request.getReviewEndTime());
        schedule.setStatus(SCHEDULE_UNAVAILABLE);
        schedule.setActivo((byte) 1);

        SpecialistSchedule saved = scheduleRepository.save(schedule);
        logger.info("Review schedule created for care plan. scheduleId={}, specialistId={}, date={}, start={}, end={}",
                saved.getId(), specialist.getId(), saved.getScheduleDate(), saved.getStartTime(), saved.getEndTime());
        return saved;
    }

    private void validateReviewTimeAvailability(
            Integer specialistId,
            LocalDate reviewDate,
            LocalTime reviewStartTime,
            LocalTime reviewEndTime
    ) {
        logger.info("Validating review time availability. specialistId={}, date={}, start={}, end={}",
                specialistId, reviewDate, reviewStartTime, reviewEndTime);

        if (reviewDate == null) {
            throw new IllegalArgumentException("La fecha de control es obligatoria");
        }

        if (reviewStartTime == null) {
            throw new IllegalArgumentException("La hora de inicio de control es obligatoria");
        }

        if (reviewEndTime == null) {
            throw new IllegalArgumentException("La hora de fin de control es obligatoria");
        }

        if (!reviewEndTime.isAfter(reviewStartTime)) {
            throw new IllegalArgumentException("La hora de fin debe ser mayor que la hora de inicio");
        }

        LocalDateTime reviewStart = LocalDateTime.of(reviewDate, reviewStartTime);
        if (reviewStart.isBefore(LocalDateTime.now().plusHours(24))) {
            logger.warn("Review date rejected because it is less than 24 hours away. date={}, start={}",
                    reviewDate, reviewStartTime);
            throw new IllegalArgumentException("La cita de control debe agendarse con al menos 24 horas de anticipacion.");
        }

        boolean hasConflict = sessionRepository.existsOverlappingSessionForSpecialist(
                specialistId,
                reviewDate,
                reviewStartTime,
                reviewEndTime,
                List.of(SESSION_PENDING, SESSION_ACCEPTED, SESSION_FINISHED)
        );

        if (hasConflict) {
            logger.warn("Review session schedule conflict. specialistId={}, date={}, start={}, end={}",
                    specialistId, reviewDate, reviewStartTime, reviewEndTime);
            throw new IllegalStateException("El especialista ya tiene una cita registrada en ese horario.");
        }
    }

    private void validateReviewTimeAvailabilityForUpdate(
            CarePlan carePlan,
            LocalDate reviewDate,
            LocalTime reviewStartTime,
            LocalTime reviewEndTime
    ) {
        Integer specialistId = carePlan.getSpecialist() != null ? carePlan.getSpecialist().getId() : null;
        Integer excludedSessionId = carePlan.getReviewSession() != null ? carePlan.getReviewSession().getId() : null;

        logger.info("Validating review time update. carePlanId={}, specialistId={}, excludedSessionId={}, date={}, start={}, end={}",
                carePlan.getId(), specialistId, excludedSessionId, reviewDate, reviewStartTime, reviewEndTime);

        if (reviewDate == null) {
            throw new IllegalArgumentException("La fecha de control es obligatoria");
        }
        if (reviewStartTime == null) {
            throw new IllegalArgumentException("La hora de inicio de control es obligatoria");
        }
        if (reviewEndTime == null) {
            throw new IllegalArgumentException("La hora de fin de control es obligatoria");
        }
        if (!reviewEndTime.isAfter(reviewStartTime)) {
            throw new IllegalArgumentException("La hora de fin debe ser mayor que la hora de inicio");
        }

        validateNotPastDate(reviewDate, "La fecha de control no puede ser anterior a la fecha actual.");

        boolean hasConflict = excludedSessionId == null
                ? sessionRepository.existsOverlappingSessionForSpecialist(
                        specialistId,
                        reviewDate,
                        reviewStartTime,
                        reviewEndTime,
                        List.of(SESSION_PENDING, SESSION_ACCEPTED, SESSION_FINISHED)
                )
                : sessionRepository.existsOverlappingSessionForSpecialistExcludingSession(
                        specialistId,
                        reviewDate,
                        reviewStartTime,
                        reviewEndTime,
                        List.of(SESSION_PENDING, SESSION_ACCEPTED, SESSION_FINISHED),
                        excludedSessionId
                );

        if (hasConflict) {
            logger.warn("Review session update conflict. carePlanId={}, specialistId={}, date={}, start={}, end={}",
                    carePlan.getId(), specialistId, reviewDate, reviewStartTime, reviewEndTime);
            throw new IllegalStateException("El especialista ya tiene una cita registrada en ese horario.");
        }
    }

    private void validateReviewScheduleConflict(
            CarePlan carePlan,
            LocalDate reviewDate,
            LocalTime reviewStartTime,
            LocalTime reviewEndTime
    ) {
        validateReviewTimeAvailabilityForUpdate(carePlan, reviewDate, reviewStartTime, reviewEndTime);
    }

    private void replaceOrCreateReviewSession(CarePlan carePlan, SpecialistSchedule newSchedule) {
        logger.info("Replacing review session schedule. carePlanId={}, scheduleId={}",
                carePlan.getId(), newSchedule.getId());

        newSchedule.setStatus(SCHEDULE_UNAVAILABLE);
        scheduleRepository.save(newSchedule);

        Session currentSession = carePlan.getReviewSession();
        if (currentSession == null) {
            Session reviewSession = createReviewSession(carePlan.getPatient(), newSchedule);
            carePlan.setReviewSession(reviewSession);
        } else {
            SpecialistSchedule previousSchedule = currentSession.getSchedule();
            if (previousSchedule != null && !previousSchedule.getId().equals(newSchedule.getId())) {
                previousSchedule.setStatus(SCHEDULE_AVAILABLE);
                scheduleRepository.save(previousSchedule);
            }
            currentSession.setSchedule(newSchedule);
            currentSession.setStatus(SESSION_ACCEPTED);
            currentSession.setDescription("Cita de seguimiento reprogramada por plan de atencion");
            carePlan.setReviewSession(sessionRepository.save(currentSession));
        }

        carePlan.setReviewDate(newSchedule.getScheduleDate());
    }

    private void cancelReviewSessionForDeletedPlan(CarePlan carePlan) {
        Session reviewSession = carePlan.getReviewSession();
        if (reviewSession == null) {
            logger.info("Care plan has no review session to cancel. carePlanId={}", carePlan.getId());
            return;
        }

        logger.info("Cancelling review session for deleted care plan. carePlanId={}, sessionId={}",
                carePlan.getId(), reviewSession.getId());

        reviewSession.setStatus(SESSION_CANCELLED);
        reviewSession.setDescription("Cita de seguimiento cancelada por eliminacion de plan de atencion");
        sessionRepository.save(reviewSession);

        if (reviewSession.getSchedule() != null) {
            SpecialistSchedule schedule = reviewSession.getSchedule();
            schedule.setStatus(SCHEDULE_AVAILABLE);
            scheduleRepository.save(schedule);
            logger.info("Review session schedule released. carePlanId={}, scheduleId={}",
                    carePlan.getId(), schedule.getId());
        }

        notificationService.createForCarePlan(
                reviewSession,
                carePlan.getId(),
                "El plan de atencion '" + carePlan.getTitle()
                        + "' fue cancelado y la cita de seguimiento asociada fue anulada."
        );
        logger.info("Patient notified about care plan deletion. carePlanId={}, patientId={}, sessionId={}",
                carePlan.getId(),
                carePlan.getPatient() != null ? carePlan.getPatient().getId() : null,
                reviewSession.getId());
    }

    private void notifyReviewSessionUpdated(CarePlan carePlan, SpecialistSchedule schedule) {
        Session reviewSession = carePlan.getReviewSession();
        if (reviewSession == null) {
            logger.warn("Skipping review session update notification because session is null. carePlanId={}",
                    carePlan.getId());
            return;
        }

        notificationService.createForCarePlan(
                reviewSession,
                carePlan.getId(),
                "La cita de seguimiento de tu plan '" + carePlan.getTitle()
                        + "' fue actualizada para el " + schedule.getScheduleDate()
                        + " de " + schedule.getStartTime()
                        + " a " + schedule.getEndTime() + "."
        );
        logger.info("Patient notified about review session update. carePlanId={}, patientId={}, sessionId={}",
                carePlan.getId(),
                carePlan.getPatient() != null ? carePlan.getPatient().getId() : null,
                reviewSession.getId());
    }

    private void notifyPatientForCarePlanChanges(CarePlan carePlan, CarePlanSnapshot snapshot) {
        if (carePlan.getPatient() == null || snapshot == null) {
            return;
        }

        boolean statusChanged = !Objects.equals(snapshot.status(), carePlan.getStatus());
        boolean contentChanged = !Objects.equals(snapshot.title(), carePlan.getTitle())
                || !Objects.equals(snapshot.therapeuticObjectives(), carePlan.getTherapeuticObjectives())
                || !Objects.equals(snapshot.generalRecommendations(), carePlan.getGeneralRecommendations())
                || !Objects.equals(snapshot.professionalObservations(), carePlan.getProfessionalObservations());

        if (statusChanged) {
            notifyPatientForCarePlan(carePlan, buildStatusNotificationMessage(carePlan));
            logger.info("Patient notified about care plan status change. carePlanId={}, patientId={}, status={}",
                    carePlan.getId(), carePlan.getPatient().getId(), carePlan.getStatus());
        }

        if (contentChanged) {
            notifyPatientForCarePlan(
                    carePlan,
                    "Tu plan de atencion '" + carePlan.getTitle() + "' fue actualizado por tu especialista."
            );
            logger.info("Patient notified about care plan content update. carePlanId={}, patientId={}",
                    carePlan.getId(), carePlan.getPatient().getId());
        }
    }

    private void notifyPatientForCarePlan(CarePlan carePlan, String message) {
        Session reviewSession = carePlan.getReviewSession();
        if (reviewSession == null) {
            logger.warn("Skipping care plan notification because review session is null. carePlanId={}, patientId={}",
                    carePlan.getId(),
                    carePlan.getPatient() != null ? carePlan.getPatient().getId() : null);
            return;
        }

        notificationService.createForCarePlan(reviewSession, carePlan.getId(), message);
    }

    private String buildStatusNotificationMessage(CarePlan carePlan) {
        String title = carePlan.getTitle();
        return switch (carePlan.getStatus()) {
            case ACTIVE -> "Tu plan de atencion '" + title + "' fue activado nuevamente.";
            case PAUSED -> "Tu plan de atencion '" + title + "' fue pausado temporalmente.";
            case COMPLETED -> "Tu plan de atencion '" + title + "' fue marcado como completado por tu especialista.";
            case CANCELLED -> "Tu plan de atencion '" + title + "' fue cancelado.";
        };
    }

    private void validateSpecialistPatientRelationship(Integer specialistId, Integer patientId) {
        logger.info("Validating specialist-patient relationship. specialistId={}, patientId={}",
                specialistId, patientId);

        boolean existsRelationship = sessionRepository.existsRelationshipBySpecialistAndPatientAndStatuses(
                specialistId,
                patientId,
                List.of(SESSION_ACCEPTED, SESSION_FINISHED)
        );

        if (!existsRelationship) {
            logger.warn("Specialist-patient relationship denied. specialistId={}, patientId={}",
                    specialistId, patientId);
            throw new SecurityException("El especialista no tiene una cita aceptada o finalizada con este paciente");
        }
    }

    private void validateViewPermission(CarePlan carePlan, Integer specialistId, Integer patientId) {
        if (specialistId == null && patientId == null) {
            return;
        }

        boolean specialistAllowed = specialistId != null
                && carePlan.getSpecialist() != null
                && specialistId.equals(carePlan.getSpecialist().getId());
        boolean patientAllowed = patientId != null
                && carePlan.getPatient() != null
                && patientId.equals(carePlan.getPatient().getId());

        if (!specialistAllowed && !patientAllowed) {
            throw new SecurityException("No tiene permiso para ver este plan");
        }
    }

    private void validateSpecialistOwner(CarePlan carePlan, Integer specialistId) {
        if (specialistId == null) {
            throw new SecurityException("Debe indicar el especialista que modifica el plan");
        }

        if (carePlan.getSpecialist() == null || !specialistId.equals(carePlan.getSpecialist().getId())) {
            throw new SecurityException("Solo el especialista dueno del plan puede modificarlo");
        }
    }

    private void validateItemActionPermission(CarePlan carePlan, Integer patientId, Integer specialistId) {
        boolean patientAllowed = patientId != null
                && carePlan.getPatient() != null
                && patientId.equals(carePlan.getPatient().getId());
        boolean specialistAllowed = specialistId != null
                && carePlan.getSpecialist() != null
                && specialistId.equals(carePlan.getSpecialist().getId());

        if (!patientAllowed && !specialistAllowed) {
            throw new SecurityException("No tiene permiso para modificar esta actividad");
        }
    }

    private void validatePlanActiveForActivityAction(CarePlan carePlan) {
        if (carePlan.getStatus() != CarePlanStatus.ACTIVE) {
            logger.warn("Care plan activity action blocked because plan is not active. carePlanId={}, status={}",
                    carePlan.getId(), carePlan.getStatus());
            throw new SecurityException("No puedes modificar actividades porque el plan no esta activo.");
        }
    }

    private void validateCompletedPlanAllowsUpdate(CarePlan carePlan, UpdateCarePlanRequestDTO request) {
        if (carePlan.getStatus() != CarePlanStatus.COMPLETED) {
            return;
        }

        if (request != null && request.getStatus() != null) {
            CarePlanStatus requestedStatus = parseStatus(request.getStatus());
            if (requestedStatus != CarePlanStatus.COMPLETED) {
                logger.warn("Care plan reactivation blocked because plan is completed. carePlanId={}, requestedStatus={}",
                        carePlan.getId(), requestedStatus);
                throw new SecurityException(COMPLETED_PLAN_REACTIVATE_MESSAGE);
            }
        }

        validatePlanNotCompletedForMutation(carePlan);
    }

    private void validatePlanNotCompletedForMutation(CarePlan carePlan) {
        if (carePlan.getStatus() == CarePlanStatus.COMPLETED) {
            logger.warn("Care plan mutation blocked because plan is completed. carePlanId={}", carePlan.getId());
            throw new SecurityException(COMPLETED_PLAN_LOCK_MESSAGE);
        }
    }

    private void validateActivityNotCompletedForSpecialistMutation(
            CarePlanItem item,
            UpdateCarePlanItemRequestDTO request
    ) {
        if (item.getStatus() != CarePlanItemStatus.COMPLETED) {
            return;
        }

        CarePlanItemStatus requestedStatus = request != null && request.getStatus() != null
                ? parseItemStatus(request.getStatus())
                : null;
        if (requestedStatus == CarePlanItemStatus.CANCELLED) {
            logger.warn("Care plan activity cancel blocked because activity is completed. activityId={}, carePlanId={}",
                    item.getId(), item.getCarePlan() != null ? item.getCarePlan().getId() : null);
        } else {
            logger.warn("Care plan activity edit blocked because activity is completed. activityId={}, carePlanId={}, requestedStatus={}",
                    item.getId(),
                    item.getCarePlan() != null ? item.getCarePlan().getId() : null,
                    requestedStatus);
        }

        throw new SecurityException(COMPLETED_ACTIVITY_LOCK_MESSAGE);
    }

    private void validateCreateRequest(CreateCarePlanRequestDTO request) {
        if (isBlank(request.getTitle())) {
            throw new IllegalArgumentException("El titulo del plan es obligatorio");
        }
        if (request.getTitle().trim().length() > 150) {
            throw new IllegalArgumentException("El titulo no puede exceder 150 caracteres");
        }
        if (isBlank(request.getTherapeuticObjectives())) {
            throw new IllegalArgumentException("Los objetivos terapeuticos son obligatorios");
        }
        validateMaxLength(request.getTherapeuticObjectives(), OBJECTIVES_MAX_LENGTH,
                "Los objetivos terapeuticos no pueden exceder 1000 caracteres");
        if (isBlank(request.getGeneralRecommendations())) {
            throw new IllegalArgumentException("Las recomendaciones generales son obligatorias");
        }
        validateMaxLength(request.getGeneralRecommendations(), RECOMMENDATIONS_MAX_LENGTH,
                "Las recomendaciones generales no pueden exceder 1000 caracteres");
        validateMaxLength(request.getProfessionalObservations(), OBSERVATIONS_MAX_LENGTH,
                "Las observaciones profesionales no pueden exceder 700 caracteres");
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos una actividad al plan");
        }
        request.getItems().forEach(this::validateItemRequest);
    }

    private void validateItemRequest(CarePlanItemRequestDTO request) {
        if (isBlank(request.getTitle())) {
            throw new IllegalArgumentException("El titulo de la actividad es obligatorio");
        }
        if (request.getTitle().trim().length() > 150) {
            throw new IllegalArgumentException("El titulo de la actividad no puede exceder 150 caracteres");
        }
        validateMaxLength(request.getDescription(), ITEM_DESCRIPTION_MAX_LENGTH,
                "La descripcion de la actividad no puede exceder 500 caracteres");
        parseItemType(request.getItemType());
        if (request.getDueDate() != null) {
            validateNotPastDate(request.getDueDate(), "La fecha limite de la actividad no puede ser anterior a la fecha actual.");
        }
    }

    private void validateActivityRequest(CarePlanActivityRequestDTO request) {
        if (isBlank(request.getTitle())) {
            throw new IllegalArgumentException("El titulo de la actividad es obligatorio");
        }
        if (request.getTitle().trim().length() > 150) {
            throw new IllegalArgumentException("El titulo de la actividad no puede exceder 150 caracteres");
        }
        validateMaxLength(request.getDescription(), ITEM_DESCRIPTION_MAX_LENGTH,
                "La descripcion de la actividad no puede exceder 500 caracteres");
        if (request.getDueDate() != null) {
            validateNotPastDate(request.getDueDate(), "La fecha limite de la actividad no puede ser anterior a la fecha actual.");
        }
    }

    private void validatePlanAllowsActivities(CarePlan carePlan) {
        if (carePlan.getStatus() == CarePlanStatus.CANCELLED) {
            throw new IllegalStateException("No se pueden agregar actividades a un plan cancelado");
        }
    }

    private CarePlanItem buildItem(CarePlan carePlan, CarePlanItemRequestDTO request) {
        CarePlanItem item = new CarePlanItem();
        item.setCarePlan(carePlan);
        item.setTitle(request.getTitle().trim());
        item.setDescription(cleanNullableText(request.getDescription()));
        item.setItemType(parseItemType(request.getItemType()));
        item.setStatus(CarePlanItemStatus.PENDING);
        item.setDueDate(request.getDueDate());
        item.setCompletedDate(null);
        item.setCreatedDate(LocalDateTime.now());
        return item;
    }

    private void applyUpdates(CarePlan carePlan, UpdateCarePlanRequestDTO request) {
        if (request.getTitle() != null) {
            if (isBlank(request.getTitle())) {
                throw new IllegalArgumentException("El titulo del plan es obligatorio");
            }
            if (request.getTitle().trim().length() > 150) {
                throw new IllegalArgumentException("El titulo no puede exceder 150 caracteres");
            }
            carePlan.setTitle(request.getTitle().trim());
        }

        if (request.getTherapeuticObjectives() != null) {
            if (isBlank(request.getTherapeuticObjectives())) {
                throw new IllegalArgumentException("Los objetivos terapeuticos son obligatorios");
            }
            validateMaxLength(request.getTherapeuticObjectives(), OBJECTIVES_MAX_LENGTH,
                    "Los objetivos terapeuticos no pueden exceder 1000 caracteres");
            carePlan.setTherapeuticObjectives(request.getTherapeuticObjectives().trim());
        }

        if (request.getGeneralRecommendations() != null) {
            if (isBlank(request.getGeneralRecommendations())) {
                throw new IllegalArgumentException("Las recomendaciones generales son obligatorias");
            }
            validateMaxLength(request.getGeneralRecommendations(), RECOMMENDATIONS_MAX_LENGTH,
                    "Las recomendaciones generales no pueden exceder 1000 caracteres");
            carePlan.setGeneralRecommendations(request.getGeneralRecommendations().trim());
        }

        if (request.getProfessionalObservations() != null) {
            validateMaxLength(request.getProfessionalObservations(), OBSERVATIONS_MAX_LENGTH,
                    "Las observaciones profesionales no pueden exceder 700 caracteres");
            carePlan.setProfessionalObservations(cleanNullableText(request.getProfessionalObservations()));
        }

        if (request.getStatus() != null) {
            CarePlanStatus status = parseStatus(request.getStatus());
            carePlan.setStatus(status);
            logger.info("Care plan status updated. carePlanId={}, status={}",
                    carePlan.getId(), carePlan.getStatus());
            if (status == CarePlanStatus.COMPLETED) {
                logger.info("Care plan marked as completed. carePlanId={}, specialistId={}",
                        carePlan.getId(),
                        carePlan.getSpecialist() != null ? carePlan.getSpecialist().getId() : null);
            }
            if (status == CarePlanStatus.CANCELLED) {
                cancelReviewSessionForDeletedPlan(carePlan);
            }
        }

        if (request.getProgressPercentage() != null) {
            throw new IllegalArgumentException("El progreso se calcula automaticamente segun las actividades del plan");
        }

        applyReviewSessionUpdates(carePlan, request);
    }

    private void applyReviewSessionUpdates(CarePlan carePlan, UpdateCarePlanRequestDTO request) {
        boolean hasReviewTimeChange = request.getReviewScheduleId() != null
                || request.getReviewDate() != null
                || request.getReviewStartTime() != null
                || request.getReviewEndTime() != null;

        if (!hasReviewTimeChange) {
            return;
        }

        if (carePlan.getStatus() == CarePlanStatus.CANCELLED) {
            throw new IllegalStateException("No se puede reprogramar la cita de seguimiento de un plan cancelado");
        }

        if (request.getReviewScheduleId() != null) {
            SpecialistSchedule schedule = scheduleRepository.findById(request.getReviewScheduleId())
                    .orElseThrow(() -> new NoSuchElementException("Horario de control no encontrado"));
            validateReviewScheduleConflict(carePlan, schedule.getScheduleDate(), schedule.getStartTime(), schedule.getEndTime());
            replaceOrCreateReviewSession(carePlan, schedule);
            notifyReviewSessionUpdated(carePlan, schedule);
            return;
        }

        Session currentSession = carePlan.getReviewSession();
        SpecialistSchedule currentSchedule = currentSession != null ? currentSession.getSchedule() : null;

        LocalDate reviewDate = request.getReviewDate() != null
                ? request.getReviewDate()
                : currentSchedule != null ? currentSchedule.getScheduleDate() : carePlan.getReviewDate();
        LocalTime startTime = request.getReviewStartTime() != null
                ? request.getReviewStartTime()
                : currentSchedule != null ? currentSchedule.getStartTime() : null;
        LocalTime endTime = request.getReviewEndTime() != null
                ? request.getReviewEndTime()
                : currentSchedule != null ? currentSchedule.getEndTime() : null;

        validateReviewTimeAvailabilityForUpdate(carePlan, reviewDate, startTime, endTime);

        Session reviewSession = currentSession;
        SpecialistSchedule schedule = currentSchedule;

        if (schedule == null) {
            schedule = new SpecialistSchedule();
            schedule.setSpecialist(carePlan.getSpecialist());
            schedule.setActivo((byte) 1);
        }

        schedule.setScheduleDate(reviewDate);
        schedule.setDayOfWeek((byte) reviewDate.getDayOfWeek().getValue());
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setStatus(SCHEDULE_UNAVAILABLE);
        SpecialistSchedule savedSchedule = scheduleRepository.save(schedule);

        if (reviewSession == null) {
            reviewSession = createReviewSession(carePlan.getPatient(), savedSchedule);
        } else {
            reviewSession.setSchedule(savedSchedule);
            reviewSession.setStatus(SESSION_ACCEPTED);
            reviewSession.setDescription("Cita de seguimiento reprogramada por plan de atencion");
            reviewSession = sessionRepository.save(reviewSession);
        }

        carePlan.setReviewSession(reviewSession);
        carePlan.setReviewDate(savedSchedule.getScheduleDate());
        notifyReviewSessionUpdated(carePlan, savedSchedule);
    }

    private void applyItemUpdates(CarePlanItem item, UpdateCarePlanItemRequestDTO request) {
        if (request.getTitle() != null) {
            if (isBlank(request.getTitle())) {
                throw new IllegalArgumentException("El titulo de la actividad es obligatorio");
            }
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            validateMaxLength(request.getDescription(), ITEM_DESCRIPTION_MAX_LENGTH,
                    "La descripcion de la actividad no puede exceder 500 caracteres");
            item.setDescription(cleanNullableText(request.getDescription()));
        }
        if (request.getItemType() != null) {
            item.setItemType(parseItemType(request.getItemType()));
        }
        if (request.getStatus() != null) {
            CarePlanItemStatus status = parseItemStatus(request.getStatus());
            item.setStatus(status);
            item.setCompletedDate(status == CarePlanItemStatus.COMPLETED ? LocalDateTime.now() : null);
        }
        if (request.getDueDate() != null) {
            validateNotPastDate(request.getDueDate(), "La fecha limite de la actividad no puede ser anterior a la fecha actual.");
            item.setDueDate(request.getDueDate());
        }
    }

    private void validateNotPastDate(LocalDate date, String message) {
        logger.info("Validating care plan date. date={}", date);
        if (date.isBefore(LocalDate.now())) {
            logger.warn("Care plan date rejected because it is in the past. date={}", date);
            throw new IllegalArgumentException(message);
        }
    }

    private void validateMaxLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private void recalculateAndSave(CarePlan carePlan) {
        recalculateProgress(carePlan);
        carePlan.setUpdatedDate(LocalDateTime.now());
        carePlanRepository.save(carePlan);
    }

    private void recalculateProgress(CarePlan carePlan) {
        List<CarePlanItem> items = carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(carePlan.getId());
        long total = items.stream()
                .filter(item -> item.getStatus() == CarePlanItemStatus.PENDING
                        || item.getStatus() == CarePlanItemStatus.COMPLETED)
                .count();
        long completed = items.stream()
                .filter(item -> item.getStatus() == CarePlanItemStatus.COMPLETED)
                .count();

        int progress = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
        carePlan.setProgressPercentage(progress);
        logger.info("Care plan progress recalculated. carePlanId={}, completed={}, total={}, progress={}",
                carePlan.getId(), completed, total, progress);
    }

    private CarePlanStatus parseStatus(String value) {
        try {
            return CarePlanStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            logger.warn("Care plan validation failed: invalid status={}", value);
            throw new IllegalArgumentException("Estado de plan invalido");
        }
    }

    private CarePlanItemType parseItemType(String value) {
        try {
            return CarePlanItemType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            logger.warn("Care plan item validation failed: invalid itemType={}", value);
            throw new IllegalArgumentException("Tipo de actividad invalido");
        }
    }

    private CarePlanItemStatus parseItemStatus(String value) {
        try {
            return CarePlanItemStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            logger.warn("Care plan item validation failed: invalid status={}", value);
            throw new IllegalArgumentException("Estado de actividad invalido");
        }
    }

    private CarePlanResponseDTO toResponse(CarePlan carePlan) {
        CarePlanResponseDTO dto = new CarePlanResponseDTO();
        dto.setId(carePlan.getId());
        dto.setSpecialistId(carePlan.getSpecialist() != null ? carePlan.getSpecialist().getId() : null);
        dto.setPatientId(carePlan.getPatient() != null ? carePlan.getPatient().getId() : null);
        dto.setPatientName(buildPatientName(carePlan.getPatient()));
        dto.setTitle(carePlan.getTitle());
        dto.setTherapeuticObjectives(carePlan.getTherapeuticObjectives());
        dto.setGeneralRecommendations(carePlan.getGeneralRecommendations());
        dto.setProfessionalObservations(carePlan.getProfessionalObservations());
        dto.setStatus(carePlan.getStatus() != null ? carePlan.getStatus().name() : null);
        dto.setProgressPercentage(carePlan.getProgressPercentage());
        dto.setReviewDate(carePlan.getReviewDate());
        dto.setCreatedDate(carePlan.getCreatedDate());
        dto.setUpdatedDate(carePlan.getUpdatedDate());
        dto.setArchivedBySpecialist(Boolean.TRUE.equals(carePlan.getArchivedBySpecialist()));
        dto.setArchivedDate(carePlan.getArchivedDate());

        if (carePlan.getReviewSession() != null) {
            Session session = carePlan.getReviewSession();
            dto.setReviewSessionId(session.getId());
            if (session.getSchedule() != null) {
                dto.setReviewDate(session.getSchedule().getScheduleDate());
                dto.setReviewStartTime(session.getSchedule().getStartTime());
                dto.setReviewEndTime(session.getSchedule().getEndTime());
            }
        }

        dto.setItems(carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(carePlan.getId())
                .stream()
                .map(this::toItemResponse)
                .toList());

        return dto;
    }

    private CarePlanSummaryDTO toSummary(CarePlan carePlan) {
        CarePlanSummaryDTO dto = new CarePlanSummaryDTO();
        dto.setId(carePlan.getId());
        dto.setSpecialistId(carePlan.getSpecialist() != null ? carePlan.getSpecialist().getId() : null);
        dto.setPatientId(carePlan.getPatient() != null ? carePlan.getPatient().getId() : null);
        dto.setPatientName(buildPatientName(carePlan.getPatient()));
        dto.setTitle(carePlan.getTitle());
        dto.setStatus(carePlan.getStatus() != null ? carePlan.getStatus().name() : null);
        dto.setProgressPercentage(carePlan.getProgressPercentage());
        dto.setReviewDate(carePlan.getReviewDate());
        dto.setReviewSessionId(carePlan.getReviewSession() != null ? carePlan.getReviewSession().getId() : null);
        if (carePlan.getReviewSession() != null && carePlan.getReviewSession().getSchedule() != null) {
            dto.setReviewDate(carePlan.getReviewSession().getSchedule().getScheduleDate());
            dto.setReviewStartTime(carePlan.getReviewSession().getSchedule().getStartTime());
            dto.setReviewEndTime(carePlan.getReviewSession().getSchedule().getEndTime());
        }
        dto.setCreatedDate(carePlan.getCreatedDate());
        dto.setArchivedBySpecialist(Boolean.TRUE.equals(carePlan.getArchivedBySpecialist()));
        dto.setArchivedDate(carePlan.getArchivedDate());
        return dto;
    }

    private CarePlanItemResponseDTO toItemResponse(CarePlanItem item) {
        CarePlanItemResponseDTO dto = new CarePlanItemResponseDTO();
        dto.setId(item.getId());
        if (item.getCarePlan() != null) {
            dto.setPlanId(item.getCarePlan().getId());
            dto.setPlanProgressPercentage(item.getCarePlan().getProgressPercentage());
        }
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
        dto.setStatus(item.getStatus() != null ? item.getStatus().name() : null);
        dto.setDueDate(item.getDueDate());
        dto.setCompletedDate(item.getCompletedDate());
        dto.setCreatedDate(item.getCreatedDate());
        dto.setUpdatedDate(item.getUpdatedDate());
        return dto;
    }

    private CarePlanActivityProgressResponseDTO toProgressResponse(CarePlanItem item) {
        CarePlanActivityProgressResponseDTO dto = new CarePlanActivityProgressResponseDTO();
        dto.setActivityId(item.getId());
        dto.setStatus(item.getStatus() != null ? item.getStatus().name() : null);
        dto.setCompletedDate(item.getCompletedDate());
        dto.setPlanProgressPercentage(item.getCarePlan() != null ? item.getCarePlan().getProgressPercentage() : 0);
        return dto;
    }

    private String cleanNullableText(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private String buildPatientName(Patient patient) {
        if (patient == null) {
            return null;
        }

        String names = patient.getNames() != null ? patient.getNames().trim() : "";
        String firstLastname = patient.getFirstLastname() != null ? patient.getFirstLastname().trim() : "";
        String secondLastname = patient.getSecondLastname() != null ? patient.getSecondLastname().trim() : "";
        String fullName = (names + " " + firstLastname + " " + secondLastname).trim().replaceAll("\\s+", " ");
        return fullName.isBlank() ? null : fullName;
    }

    private record CarePlanSnapshot(
            String title,
            String therapeuticObjectives,
            String generalRecommendations,
            String professionalObservations,
            CarePlanStatus status
    ) {
        static CarePlanSnapshot from(CarePlan carePlan) {
            return new CarePlanSnapshot(
                    carePlan.getTitle(),
                    carePlan.getTherapeuticObjectives(),
                    carePlan.getGeneralRecommendations(),
                    carePlan.getProfessionalObservations(),
                    carePlan.getStatus()
            );
        }
    }
}
