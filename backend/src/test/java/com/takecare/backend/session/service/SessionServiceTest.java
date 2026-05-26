package com.takecare.backend.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takecare.backend.notification.service.NotificationService;
import com.takecare.backend.session.dto.SpecialistPatientDTO;
import com.takecare.backend.session.dto.SpecialistPatientsResponseDTO;
import com.takecare.backend.session.model.Session;
import com.takecare.backend.session.repository.SessionRepository;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import com.takecare.backend.specialistschedule.repository.SpecialistScheduleRepository;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private SpecialistScheduleRepository scheduleRepository;

    @Mock
    private NotificationService notificationService;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                sessionRepository,
                patientRepository,
                scheduleRepository,
                notificationService
        );
    }

    @Test
    void listsEachAssociatedPatientOnlyOnceWithSessionDates() {
        Patient patient = patient(10, "Ana", "Perez", "ana@example.com");
        Patient secondPatient = patient(11, "Luis", "Rojas", "luis@example.com");
        LocalDate pastDate = LocalDate.now().minusDays(5);
        LocalDate futureDate = LocalDate.now().plusDays(3);

        when(sessionRepository.findBySpecialistIdOrderByCreatedDateDesc(7))
                .thenReturn(List.of(
                        session(patient, futureDate),
                        session(patient, pastDate),
                        session(secondPatient, pastDate)
                ));

        SpecialistPatientsResponseDTO response = sessionService.listPatientsBySpecialist(7);

        assertThat(response.getTotalPatients()).isEqualTo(2);
        assertThat(response.getPatients()).hasSize(2);

        SpecialistPatientDTO ana = response.getPatients().stream()
                .filter(item -> item.getPatientId().equals(10))
                .findFirst()
                .orElseThrow();

        assertThat(ana.getFullName()).isEqualTo("Ana Perez");
        assertThat(ana.getEmail()).isEqualTo("ana@example.com");
        assertThat(ana.getLastSessionDate()).isEqualTo(pastDate);
        assertThat(ana.getNextSessionDate()).isEqualTo(futureDate);
    }

    @Test
    void returnsEmptyResponseWhenSpecialistHasNoSessions() {
        when(sessionRepository.findBySpecialistIdOrderByCreatedDateDesc(7)).thenReturn(List.of());

        SpecialistPatientsResponseDTO response = sessionService.listPatientsBySpecialist(7);

        assertThat(response.getTotalPatients()).isZero();
        assertThat(response.getPatients()).isEmpty();
    }

    private Patient patient(Integer id, String names, String lastname, String email) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setNames(names);
        patient.setFirstLastname(lastname);
        patient.setEmail(email);
        return patient;
    }

    private Session session(Patient patient, LocalDate date) {
        SpecialistSchedule schedule = new SpecialistSchedule();
        schedule.setScheduleDate(date);
        schedule.setStartTime(LocalTime.of(10, 0));

        Session session = new Session();
        session.setPatient(patient);
        session.setSchedule(schedule);
        return session;
    }
}
