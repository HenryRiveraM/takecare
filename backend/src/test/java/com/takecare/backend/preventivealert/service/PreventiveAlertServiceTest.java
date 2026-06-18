package com.takecare.backend.preventivealert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;

@ExtendWith(MockitoExtension.class)
class PreventiveAlertServiceTest {

    @Mock
    private PreventiveAlertRepository preventiveAlertRepository;

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private EmotionalRecordRepository emotionalRecordRepository;

    @Mock
    private CarePlanItemRepository carePlanItemRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private NotificationService notificationService;

    private PreventiveAlertService preventiveAlertService;

    private Patient patient;
    private Specialist specialist;
    private CarePlan carePlan;

    @BeforeEach
    void setUp() {
        preventiveAlertService = new PreventiveAlertService(
                preventiveAlertRepository,
                carePlanRepository,
                emotionalRecordRepository,
                carePlanItemRepository,
                sessionRepository,
                notificationService
        );

        patient = new Patient();
        patient.setId(1);
        patient.setNames("John");
        patient.setFirstLastname("Doe");

        specialist = new Specialist();
        specialist.setId(2);

        carePlan = new CarePlan();
        carePlan.setId(10L);
        carePlan.setPatient(patient);
        carePlan.setSpecialist(specialist);
        carePlan.setStatus(CarePlanStatus.ACTIVE);
        carePlan.setCreatedDate(LocalDateTime.now().minusDays(10));
    }

    @Test
    void evaluateCriticalStateRule_whenCriticalConditionMet_createsAlert() {
        // Arrange
        when(carePlanRepository.findFirstByPatientIdAndStatusOrderByCreatedDateDesc(eq(1), eq(CarePlanStatus.ACTIVE)))
                .thenReturn(Optional.of(carePlan));

        EmotionalRecord r1 = new EmotionalRecord();
        r1.setMoodLevel(2); // <= 2
        r1.setAnxietyLevel(5); // >= 4
        r1.setStressLevel(5); // >= 4

        EmotionalRecord r2 = new EmotionalRecord();
        r2.setMoodLevel(1); // <= 2
        r2.setAnxietyLevel(4);

        EmotionalRecord r3 = new EmotionalRecord();
        r3.setMoodLevel(2);
        r3.setStressLevel(4);

        when(emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(eq(1)))
                .thenReturn(List.of(r1, r2, r3));

        when(preventiveAlertRepository.existsByPatientIdAndAlertTypeAndStatus(eq(1), eq("CRITICAL_STATE"), eq("OPEN")))
                .thenReturn(false);

        // Act
        preventiveAlertService.evaluateCriticalStateRule(1);

        // Assert
        ArgumentCaptor<PreventiveAlert> alertCaptor = ArgumentCaptor.forClass(PreventiveAlert.class);
        verify(preventiveAlertRepository, times(1)).save(alertCaptor.capture());
        PreventiveAlert alert = alertCaptor.getValue();
        assertThat(alert.getPriority()).isEqualTo("HIGH");
        assertThat(alert.getAlertType()).isEqualTo("CRITICAL_STATE");
        assertThat(alert.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void evaluateCriticalStateRule_whenCriticalConditionNotMet_doesNotCreateAlert() {
        // Arrange
        when(carePlanRepository.findFirstByPatientIdAndStatusOrderByCreatedDateDesc(eq(1), eq(CarePlanStatus.ACTIVE)))
                .thenReturn(Optional.of(carePlan));

        EmotionalRecord r1 = new EmotionalRecord();
        r1.setMoodLevel(4); // > 2, anxiety < 4, stress < 4
        r1.setAnxietyLevel(2);
        r1.setStressLevel(2);

        EmotionalRecord r2 = new EmotionalRecord();
        r2.setMoodLevel(1);

        EmotionalRecord r3 = new EmotionalRecord();
        r3.setMoodLevel(2);

        when(emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(eq(1)))
                .thenReturn(List.of(r1, r2, r3));

        // Act
        preventiveAlertService.evaluateCriticalStateRule(1);

        // Assert
        verify(preventiveAlertRepository, never()).save(any(PreventiveAlert.class));
    }

    @Test
    void evaluateAbandonmentRule_whenInactivityIs2Days_createsLowAlert() {
        // Arrange
        EmotionalRecord r = new EmotionalRecord();
        r.setCreatedDate(LocalDateTime.now().minusDays(2));
        when(emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(eq(1)))
                .thenReturn(List.of(r));

        when(preventiveAlertRepository.findByPatientIdAndAlertTypeAndStatus(eq(1), eq("ABANDONMENT"), eq("OPEN")))
                .thenReturn(Optional.empty());

        // Act
        preventiveAlertService.evaluateAbandonmentRule(carePlan);

        // Assert
        ArgumentCaptor<PreventiveAlert> alertCaptor = ArgumentCaptor.forClass(PreventiveAlert.class);
        verify(preventiveAlertRepository, times(1)).save(alertCaptor.capture());
        PreventiveAlert alert = alertCaptor.getValue();
        assertThat(alert.getPriority()).isEqualTo("LOW");
        assertThat(alert.getAlertType()).isEqualTo("ABANDONMENT");
    }

    @Test
    void evaluateAbandonmentRule_whenInactivityIs7Days_createsHighAlert() {
        // Arrange
        EmotionalRecord r = new EmotionalRecord();
        r.setCreatedDate(LocalDateTime.now().minusDays(7));
        when(emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(eq(1)))
                .thenReturn(List.of(r));

        when(preventiveAlertRepository.findByPatientIdAndAlertTypeAndStatus(eq(1), eq("ABANDONMENT"), eq("OPEN")))
                .thenReturn(Optional.empty());

        // Act
        preventiveAlertService.evaluateAbandonmentRule(carePlan);

        // Assert
        ArgumentCaptor<PreventiveAlert> alertCaptor = ArgumentCaptor.forClass(PreventiveAlert.class);
        verify(preventiveAlertRepository, times(1)).save(alertCaptor.capture());
        PreventiveAlert alert = alertCaptor.getValue();
        assertThat(alert.getPriority()).isEqualTo("HIGH");
        assertThat(alert.getAlertType()).isEqualTo("ABANDONMENT");
    }

    @Test
    void evaluateAbandonmentRule_whenAlertExistsAndEscalates_updatesAlert() {
        // Arrange
        EmotionalRecord r = new EmotionalRecord();
        r.setCreatedDate(LocalDateTime.now().minusDays(5)); // Medium alert condition
        when(emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(eq(1)))
                .thenReturn(List.of(r));

        PreventiveAlert existingAlert = new PreventiveAlert();
        existingAlert.setPatient(patient);
        existingAlert.setAlertType("ABANDONMENT");
        existingAlert.setPriority("LOW"); // Escalation from LOW to MEDIUM
        existingAlert.setStatus("OPEN");

        when(preventiveAlertRepository.findByPatientIdAndAlertTypeAndStatus(eq(1), eq("ABANDONMENT"), eq("OPEN")))
                .thenReturn(Optional.of(existingAlert));

        // Act
        preventiveAlertService.evaluateAbandonmentRule(carePlan);

        // Assert
        verify(preventiveAlertRepository, times(1)).save(existingAlert);
        assertThat(existingAlert.getPriority()).isEqualTo("MEDIUM");
    }

    @Test
    void evaluateTaskRules_whenTaskDueSoon_createsLowAlert() {
        // Arrange
        CarePlanItem item = new CarePlanItem();
        item.setId(50L);
        item.setTitle("Task 1");
        item.setStatus(CarePlanItemStatus.PENDING);
        item.setDueDate(LocalDate.now().plusDays(1)); // Due soon (tomorrow)

        when(carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(eq(10L)))
                .thenReturn(List.of(item));

        when(preventiveAlertRepository.findByCarePlanItemIdAndStatus(eq(50L), eq("OPEN")))
                .thenReturn(Optional.empty());

        // Act
        preventiveAlertService.evaluateTaskRules(carePlan);

        // Assert
        ArgumentCaptor<PreventiveAlert> alertCaptor = ArgumentCaptor.forClass(PreventiveAlert.class);
        verify(preventiveAlertRepository, times(1)).save(alertCaptor.capture());
        PreventiveAlert alert = alertCaptor.getValue();
        assertThat(alert.getPriority()).isEqualTo("LOW");
        assertThat(alert.getAlertType()).isEqualTo("TASK_DUE_SOON");
    }

    @Test
    void evaluateTaskRules_whenTaskOverdue_createsMediumAlert() {
        // Arrange
        CarePlanItem item = new CarePlanItem();
        item.setId(50L);
        item.setTitle("Task 1");
        item.setStatus(CarePlanItemStatus.PENDING);
        item.setDueDate(LocalDate.now().minusDays(1)); // Overdue

        when(carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(eq(10L)))
                .thenReturn(List.of(item));

        when(preventiveAlertRepository.findByCarePlanItemIdAndStatus(eq(50L), eq("OPEN")))
                .thenReturn(Optional.empty());

        // Act
        preventiveAlertService.evaluateTaskRules(carePlan);

        // Assert
        ArgumentCaptor<PreventiveAlert> alertCaptor = ArgumentCaptor.forClass(PreventiveAlert.class);
        verify(preventiveAlertRepository, times(1)).save(alertCaptor.capture());
        PreventiveAlert alert = alertCaptor.getValue();
        assertThat(alert.getPriority()).isEqualTo("MEDIUM");
        assertThat(alert.getAlertType()).isEqualTo("TASK_OVERDUE");
    }

    @Test
    void evaluateTaskRules_whenTaskAlertExistsAndEscalates_updatesAlert() {
        // Arrange
        CarePlanItem item = new CarePlanItem();
        item.setId(50L);
        item.setTitle("Task 1");
        item.setStatus(CarePlanItemStatus.PENDING);
        item.setDueDate(LocalDate.now().minusDays(1)); // Overdue

        when(carePlanItemRepository.findByCarePlanIdOrderByCreatedDateAsc(eq(10L)))
                .thenReturn(List.of(item));

        PreventiveAlert existingAlert = new PreventiveAlert();
        existingAlert.setCarePlanItemId(50L);
        existingAlert.setAlertType("TASK_DUE_SOON");
        existingAlert.setPriority("LOW");
        existingAlert.setStatus("OPEN");

        when(preventiveAlertRepository.findByCarePlanItemIdAndStatus(eq(50L), eq("OPEN")))
                .thenReturn(Optional.of(existingAlert));

        // Act
        preventiveAlertService.evaluateTaskRules(carePlan);

        // Assert
        verify(preventiveAlertRepository, times(1)).save(existingAlert);
        assertThat(existingAlert.getPriority()).isEqualTo("MEDIUM");
        assertThat(existingAlert.getAlertType()).isEqualTo("TASK_OVERDUE");
    }

    @Test
    void markAsReviewed_whenValid_updatesStatus() {
        // Arrange
        PreventiveAlert alert = new PreventiveAlert();
        alert.setId(100L);
        alert.setSpecialist(specialist);
        alert.setPatient(patient);
        alert.setStatus("OPEN");

        when(preventiveAlertRepository.findByIdAndSpecialistId(eq(100L), eq(2)))
                .thenReturn(Optional.of(alert));
        when(preventiveAlertRepository.save(any(PreventiveAlert.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PreventiveAlertResponseDTO response = preventiveAlertService.markAsReviewed(100L, 2);

        // Assert
        assertThat(response.getStatus()).isEqualTo("REVIEWED");
        assertThat(response.isReviewed()).isTrue();
        assertThat(response.getReviewedAt()).isNotNull();
    }

    @Test
    void evaluateCriticalStateRule_whenCriticalConditionMet_sendsNotificationToSpecialist() {
        // Arrange
        when(carePlanRepository.findFirstByPatientIdAndStatusOrderByCreatedDateDesc(eq(1), eq(CarePlanStatus.ACTIVE)))
                .thenReturn(Optional.of(carePlan));

        EmotionalRecord r1 = new EmotionalRecord();
        r1.setMoodLevel(2);
        r1.setAnxietyLevel(5);
        r1.setStressLevel(5);

        EmotionalRecord r2 = new EmotionalRecord();
        r2.setMoodLevel(1);

        EmotionalRecord r3 = new EmotionalRecord();
        r3.setMoodLevel(2);

        when(emotionalRecordRepository.findByPatientIdOrderByCreatedDateDesc(eq(1)))
                .thenReturn(List.of(r1, r2, r3));

        when(preventiveAlertRepository.existsByPatientIdAndAlertTypeAndStatus(eq(1), eq("CRITICAL_STATE"), eq("OPEN")))
                .thenReturn(false);

        Session session = new Session();
        session.setId(5);
        when(sessionRepository.findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(eq(2), eq(1)))
                .thenReturn(List.of(session));

        // Act
        preventiveAlertService.evaluateCriticalStateRule(1);

        // Assert
        verify(notificationService, times(1)).createForSpecialistCarePlan(
                eq(session),
                eq(10L),
                any(String.class)
        );
    }
}
