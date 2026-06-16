package com.takecare.backend.careplan.scheduler;

import com.takecare.backend.careplan.model.CarePlanItem;
import com.takecare.backend.careplan.model.CarePlanItemStatus;
import com.takecare.backend.careplan.repository.CarePlanItemRepository;
import com.takecare.backend.notification.repository.NotificationRepository;
import com.takecare.backend.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class CarePlanItemReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CarePlanItemReminderScheduler.class);

    private final CarePlanItemRepository carePlanItemRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public CarePlanItemReminderScheduler(
            CarePlanItemRepository carePlanItemRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService
    ) {
        this.carePlanItemRepository = carePlanItemRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendDueDateReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        logger.info("ItemReminderScheduler: running. Searching PENDING items due on {}", tomorrow);

        List<CarePlanItem> itemsDueTomorrow = carePlanItemRepository
                .findPendingItemsDueInRange(CarePlanItemStatus.PENDING, tomorrow, tomorrow);

        if (itemsDueTomorrow.isEmpty()) {
            logger.info("ItemReminderScheduler: no pending items due tomorrow. Nothing to notify.");
            return;
        }

        logger.info("ItemReminderScheduler: found {} item(s) due tomorrow. Processing reminders.", itemsDueTomorrow.size());

        int sent = 0;
        int skipped = 0;

        for (CarePlanItem item : itemsDueTomorrow) {
            if (reminderAlreadySent(item.getId())) {
                logger.debug("ItemReminderScheduler: reminder already sent for itemId={}. Skipping.", item.getId());
                skipped++;
                continue;
            }

            try {
                Integer patientId = item.getCarePlan().getPatient().getId();
                Long carePlanId = item.getCarePlan().getId();
                String description = buildReminderMessage(item);

                notificationService.createItemReminder(patientId, carePlanId, item.getId(), description);

                logger.info("ItemReminderScheduler: reminder sent. itemId={}, patientId={}, carePlanId={}, dueDate={}",
                        item.getId(), patientId, carePlanId, item.getDueDate());
                sent++;
            } catch (RuntimeException e) {
                logger.error("ItemReminderScheduler: failed to send reminder for itemId={}. Error: {}",
                        item.getId(), e.getMessage(), e);
            }
        }

        logger.info("ItemReminderScheduler: finished. sent={}, skipped={}, total={}", sent, skipped, itemsDueTomorrow.size());
    }

    private boolean reminderAlreadySent(Long itemId) {
        return !notificationRepository.findUnreadRemindersByItemId(itemId).isEmpty();
    }

    private String buildReminderMessage(CarePlanItem item) {
        String title = item.getTitle() != null ? item.getTitle() : "Actividad";
        return "Recordatorio: \"" + title + "\" vence manana";
    }
}
