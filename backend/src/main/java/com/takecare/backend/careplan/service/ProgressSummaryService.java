package com.takecare.backend.careplan.service;

import com.takecare.backend.calification.repository.CalificationRepository;
import com.takecare.backend.careplan.dto.ProgressSummaryDTO;
import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanItem;
import com.takecare.backend.careplan.model.CarePlanItemStatus;
import com.takecare.backend.careplan.repository.CarePlanItemRepository;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class ProgressSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(ProgressSummaryService.class);

    private static final int EMOTIONAL_WINDOW_DAYS = 30;

    private final CarePlanRepository carePlanRepository;
    private final CarePlanItemRepository carePlanItemRepository;
    private final CalificationRepository calificationRepository;

    public ProgressSummaryService(
            CarePlanRepository carePlanRepository,
            CarePlanItemRepository carePlanItemRepository,
            CalificationRepository calificationRepository
    ) {
        this.carePlanRepository = carePlanRepository;
        this.carePlanItemRepository = carePlanItemRepository;
        this.calificationRepository = calificationRepository;
    }

    public ProgressSummaryDTO getSummary(Long planId, Integer requestingUserId, String role) {
        logger.info("Progress summary requested. planId={}, requestingUserId={}, role={}", planId, requestingUserId, role);

        CarePlan plan = carePlanRepository.findById(planId)
                .orElseThrow(() -> {
                    logger.warn("Progress summary - plan not found. planId={}", planId);
                    return new NoSuchElementException("Plan de cuidado no encontrado");
                });

        validateAccess(plan, requestingUserId, role);

        List<CarePlanItem> items = carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(planId);
        logger.info("Progress summary - computing task stats. planId={}, totalItems={}", planId, items.size());

        ProgressSummaryDTO summary = buildTaskStats(planId, items);
        summary.setPlanId(planId);

        enrichWithEmotionalAverage(summary, plan);

        logger.info("Progress summary computed. planId={}, total={}, completed={}, overdue={}, avgRating={}",
                planId, summary.getTotalTasks(), summary.getCompletedTasks(),
                summary.getOverdueTasks(), summary.getAverageEmotionalRatingLast30Days());

        return summary;
    }

    private void validateAccess(CarePlan plan, Integer requestingUserId, String role) {
        boolean isPatient = "PATIENT".equalsIgnoreCase(role)
                && plan.getPatient() != null
                && requestingUserId.equals(plan.getPatient().getId());

        boolean isSpecialist = "SPECIALIST".equalsIgnoreCase(role)
                && plan.getSpecialist() != null
                && requestingUserId.equals(plan.getSpecialist().getId());

        if (!isPatient && !isSpecialist) {
            logger.warn("Progress summary - unauthorized access. planId={}, requestingUserId={}, role={}",
                    plan.getId(), requestingUserId, role);
            throw new SecurityException("No tiene permiso para consultar el resumen de este plan de cuidado");
        }
    }

    private ProgressSummaryDTO buildTaskStats(Long planId, List<CarePlanItem> items) {
        LocalDate today = LocalDate.now();

        int total = items.size();
        int completed = 0;
        int overdue = 0;
        int pending = 0;

        for (CarePlanItem item : items) {
            if (item.getStatus() == CarePlanItemStatus.COMPLETED) {
                completed++;
            } else if (item.getStatus() == CarePlanItemStatus.PENDING) {
                if (item.getDueDate() != null && item.getDueDate().isBefore(today)) {
                    overdue++;
                } else {
                    pending++;
                }
            }
        }

        double completionRate = total == 0 ? 0.0 : Math.round((completed * 100.0 / total) * 10.0) / 10.0;

        ProgressSummaryDTO dto = new ProgressSummaryDTO();
        dto.setTotalTasks(total);
        dto.setCompletedTasks(completed);
        dto.setOverdueTasks(overdue);
        dto.setPendingTasks(pending);
        dto.setCompletionRate(completionRate);
        return dto;
    }

    private void enrichWithEmotionalAverage(ProgressSummaryDTO dto, CarePlan plan) {
        if (plan.getPatient() == null) {
            dto.setAverageEmotionalRatingLast30Days(null);
            dto.setEmotionalRatingsCount(0);
            return;
        }

        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(EMOTIONAL_WINDOW_DAYS);
        Integer patientId = plan.getPatient().getId();

        logger.info("Progress summary - fetching emotional ratings. planId={}, patientId={}, from={}, to={}",
                plan.getId(), patientId, from, to);

        var ratings = calificationRepository.findPatientRatingsInRange(patientId, from, to);

        if (ratings.isEmpty()) {
            logger.info("Progress summary - no emotional ratings found. planId={}, patientId={}", plan.getId(), patientId);
            dto.setAverageEmotionalRatingLast30Days(null);
            dto.setEmotionalRatingsCount(0);
            return;
        }

        double avg = ratings.stream()
                .filter(c -> c.getRating() != null)
                .mapToInt(c -> c.getRating())
                .average()
                .orElse(0.0);

        double rounded = Math.round(avg * 10.0) / 10.0;
        dto.setAverageEmotionalRatingLast30Days(rounded);
        dto.setEmotionalRatingsCount(ratings.size());

        logger.info("Progress summary - emotional average computed. planId={}, count={}, avg={}",
                plan.getId(), ratings.size(), rounded);
    }
}
