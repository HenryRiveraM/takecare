package com.takecare.backend.careplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takecare.backend.careplan.dto.CreateLogbookNoteRequestDTO;
import com.takecare.backend.careplan.dto.LogbookNoteResponseDTO;
import com.takecare.backend.careplan.model.CarePlan;
import com.takecare.backend.careplan.model.TrackingNote;
import com.takecare.backend.careplan.repository.CarePlanRepository;
import com.takecare.backend.careplan.repository.TrackingNoteRepository;
import com.takecare.backend.notification.service.NotificationService;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;

@ExtendWith(MockitoExtension.class)
class TrackingNoteServiceTest {

    @Mock
    private TrackingNoteRepository trackingNoteRepository;

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private NotificationService notificationService;

    private TrackingNoteService trackingNoteService;

    @BeforeEach
    void setUp() {
        trackingNoteService = new TrackingNoteService(
                trackingNoteRepository,
                carePlanRepository,
                sessionRepository,
                notificationService
        );
    }

    @Test
    void whenSpecialistAddsNote_thenPatientIsNotified() {
        // Arrange
        Long planId = 1L;
        CarePlan plan = new CarePlan();
        plan.setId(planId);
        plan.setTitle("Plan de Ansiedad");
        
        Patient patient = new Patient();
        patient.setId(10);
        plan.setPatient(patient);

        CreateLogbookNoteRequestDTO request = new CreateLogbookNoteRequestDTO();
        request.setAuthorId(5);
        request.setAuthorRole("SPECIALIST");
        request.setAuthorName("Dr. House");
        request.setContent("Recomiendo ejercicios diarios");

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        
        TrackingNote savedNote = new TrackingNote();
        savedNote.setId(100L);
        savedNote.setCarePlan(plan);
        savedNote.setAuthorId(5);
        savedNote.setAuthorRole("SPECIALIST");
        savedNote.setAuthorName("Dr. House");
        savedNote.setNote("Recomiendo ejercicios diarios");
        savedNote.setCreatedDate(LocalDateTime.now());
        
        when(trackingNoteRepository.save(any(TrackingNote.class))).thenReturn(savedNote);

        // Act
        LogbookNoteResponseDTO response = trackingNoteService.addNote(planId, request);

        // Assert
        assertThat(response.getId()).isEqualTo(100L);
        verify(notificationService, times(1)).createForCarePlan(
                eq(10),
                eq(planId),
                eq("Nueva nota de tu especialista en la bitácora de: Plan de Ansiedad")
        );
    }

    @Test
    void whenPatientAddsNoteAndReviewSessionExists_thenSpecialistIsNotifiedUsingReviewSession() {
        // Arrange
        Long planId = 1L;
        CarePlan plan = new CarePlan();
        plan.setId(planId);
        plan.setTitle("Plan de Depresion");
        
        Patient patient = new Patient();
        patient.setId(10);
        patient.setNames("Juan");
        plan.setPatient(patient);

        Session reviewSession = new Session();
        reviewSession.setId(50);
        plan.setReviewSession(reviewSession);

        CreateLogbookNoteRequestDTO request = new CreateLogbookNoteRequestDTO();
        request.setAuthorId(10);
        request.setAuthorRole("PATIENT");
        request.setAuthorName("Juan Perez");
        request.setContent("Me he sentido mejor");

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        
        TrackingNote savedNote = new TrackingNote();
        savedNote.setId(101L);
        savedNote.setCarePlan(plan);
        savedNote.setAuthorId(10);
        savedNote.setAuthorRole("PATIENT");
        savedNote.setAuthorName("Juan Perez");
        savedNote.setNote("Me he sentido mejor");
        savedNote.setCreatedDate(LocalDateTime.now());
        
        when(trackingNoteRepository.save(any(TrackingNote.class))).thenReturn(savedNote);

        // Act
        LogbookNoteResponseDTO response = trackingNoteService.addNote(planId, request);

        // Assert
        assertThat(response.getId()).isEqualTo(101L);
        verify(notificationService, times(1)).createForSpecialistCarePlan(
                eq(reviewSession),
                eq(planId),
                eq("Nuevo comentario de Juan en la bitácora de: Plan de Depresion")
        );
    }

    @Test
    void whenPatientAddsNoteAndReviewSessionIsNull_thenSpecialistIsNotifiedUsingFallbackSession() {
        // Arrange
        Long planId = 1L;
        CarePlan plan = new CarePlan();
        plan.setId(planId);
        plan.setTitle("Plan de Depresion");
        
        Patient patient = new Patient();
        patient.setId(10);
        patient.setNames("Juan");
        plan.setPatient(patient);

        Specialist specialist = new Specialist();
        specialist.setId(8);
        plan.setSpecialist(specialist);
        
        plan.setReviewSession(null); // null review session

        CreateLogbookNoteRequestDTO request = new CreateLogbookNoteRequestDTO();
        request.setAuthorId(10);
        request.setAuthorRole("PATIENT");
        request.setAuthorName("Juan Perez");
        request.setContent("Me he sentido mejor");

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        
        Session fallbackSession = new Session();
        fallbackSession.setId(99);
        
        when(sessionRepository.findBySpecialistIdAndPatientIdOrderByCreatedDateDesc(8, 10))
                .thenReturn(List.of(fallbackSession));

        TrackingNote savedNote = new TrackingNote();
        savedNote.setId(102L);
        savedNote.setCarePlan(plan);
        savedNote.setAuthorId(10);
        savedNote.setAuthorRole("PATIENT");
        savedNote.setAuthorName("Juan Perez");
        savedNote.setNote("Me he sentido mejor");
        savedNote.setCreatedDate(LocalDateTime.now());
        
        when(trackingNoteRepository.save(any(TrackingNote.class))).thenReturn(savedNote);

        // Act
        LogbookNoteResponseDTO response = trackingNoteService.addNote(planId, request);

        // Assert
        assertThat(response.getId()).isEqualTo(102L);
        verify(notificationService, times(1)).createForSpecialistCarePlan(
                eq(fallbackSession),
                eq(planId),
                eq("Nuevo comentario de Juan en la bitácora de: Plan de Depresion")
        );
    }
}
