// package com.takecare.backend.user.service;

// import com.takecare.backend.user.dto.PatientProfileDTO;
// import com.takecare.backend.user.dto.UpdatePatientProfileDTO;
// import com.takecare.backend.user.model.Patient;
// import com.takecare.backend.user.repository.PatientRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDate;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class PatientProfileServiceTest {

//     @Mock
//     private PatientRepository patientRepository;

//     @InjectMocks
//     private PatientProfileService patientProfileService;

//     private Patient mockPatient;

//     @BeforeEach
//     void setUp() {
//         mockPatient = new Patient();
//         mockPatient.setId(2);
//         mockPatient.setNames("Alex");
//         mockPatient.setFirstLastname("Ipore");
//         mockPatient.setSecondLastname(null);
//         mockPatient.setBirthDate(LocalDate.of(1995, 3, 10));
//         mockPatient.setCiNumber("98765432");
//         mockPatient.setEmail("alex@example.com");
//         mockPatient.setClinicalHistory("Sin antecedentes");
//         mockPatient.setAccountVerified(0);
//         mockPatient.setRole(1);
//     }

//     @Test
//     @DisplayName("HU08 – GET perfil devuelve datos correctos sin password")
//     void getProfile_success() {
//         when(patientRepository.findById(2)).thenReturn(Optional.of(mockPatient));

//         PatientProfileDTO result = patientProfileService.getProfile(2);

//         assertThat(result.getId()).isEqualTo(2);
//         assertThat(result.getNames()).isEqualTo("Alex");
//         assertThat(result.getEmail()).isEqualTo("alex@example.com");
//         assertThat(result.getCiNumber()).isEqualTo("98765432");
//     }

//     @Test
//     @DisplayName("HU08 – GET perfil lanza excepción si paciente no existe")
//     void getProfile_notFound() {
//         when(patientRepository.findById(99)).thenReturn(Optional.empty());

//         assertThatThrownBy(() -> patientProfileService.getProfile(99))
//                 .isInstanceOf(RuntimeException.class)
//                 .hasMessageContaining("99");
//     }

//     @Test
//     @DisplayName("HU08 – PUT perfil actualiza solo los campos enviados")
//     void updateProfile_partialUpdate() {
//         when(patientRepository.findById(2)).thenReturn(Optional.of(mockPatient));
//         when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

//         UpdatePatientProfileDTO dto = new UpdatePatientProfileDTO();
//         dto.setNames("Alexander");

//         PatientProfileDTO result = patientProfileService.updateProfile(2, dto);

//         assertThat(result.getNames()).isEqualTo("Alexander");
//         assertThat(result.getFirstLastname()).isEqualTo("Ipore");
//     }

//     @Test
//     @DisplayName("HU08 – PUT perfil no modifica campos sensibles")
//     void updateProfile_doesNotTouchSensitiveFields() {
//         when(patientRepository.findById(2)).thenReturn(Optional.of(mockPatient));
//         when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

//         UpdatePatientProfileDTO dto = new UpdatePatientProfileDTO();
//         dto.setNames("Alexander");

//         patientProfileService.updateProfile(2, dto);

//         assertThat(mockPatient.getRole()).isEqualTo(1);
//         assertThat(mockPatient.getAccountVerified()).isFalse();
//     }

//     @Test
//     @DisplayName("HU08 – PUT perfil lanza excepción si paciente no existe")
//     void updateProfile_notFound() {
//         when(patientRepository.findById(99)).thenReturn(Optional.empty());

//         UpdatePatientProfileDTO dto = new UpdatePatientProfileDTO();
//         dto.setNames("Alexander");

//         assertThatThrownBy(() -> patientProfileService.updateProfile(99, dto))
//                 .isInstanceOf(RuntimeException.class)
//                 .hasMessageContaining("99");

//         verify(patientRepository, never()).save(any());
//     }
// }
