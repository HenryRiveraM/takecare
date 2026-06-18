package com.takecare.backend.preventivealert.scheduler;

import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.CarePlanStatus;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.preventivealert.service.PreventiveAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PreventiveAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PreventiveAlertScheduler.class);

    private final CarePlanRepository carePlanRepository;
    private final PreventiveAlertService preventiveAlertService;

    public PreventiveAlertScheduler(
            CarePlanRepository carePlanRepository,
            PreventiveAlertService preventiveAlertService
    ) {
        this.carePlanRepository = carePlanRepository;
        this.preventiveAlertService = preventiveAlertService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void evaluatePreventiveRules() {
        logger.info("PreventiveAlertScheduler: Starting daily evaluation of preventive rules...");
        try {
            List<CarePlan> activePlans = carePlanRepository.findByStatus(CarePlanStatus.ACTIVE);
            logger.info("PreventiveAlertScheduler: Found {} active care plans to evaluate", activePlans.size());

            for (CarePlan plan : activePlans) {
                try {
                    preventiveAlertService.evaluateAbandonmentRule(plan);
                } catch (Exception e) {
                    logger.error("Error evaluating abandonment rule for carePlanId={}: {}", plan.getId(), e.getMessage(), e);
                }

                try {
                    preventiveAlertService.evaluateTaskRules(plan);
                } catch (Exception e) {
                    logger.error("Error evaluating task rules for carePlanId={}: {}", plan.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to execute preventive alert scheduler evaluation: {}", e.getMessage(), e);
        }
        logger.info("PreventiveAlertScheduler: Finished daily evaluation of preventive rules.");
    }
}
