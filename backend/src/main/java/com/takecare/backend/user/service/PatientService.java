package com.takecare.backend.user.service;

import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Integer id) {
        return patientRepository.findById(id);
    }

    public Patient registerPatient(Patient patient) {
        patient.setCreatedDate(LocalDateTime.now());
        patient.setLastUpdate(LocalDateTime.now());
        patient.setStatus(true);
        patient.setStrikes(0);
        patient.setAccountVerified(false);

        return patientRepository.save(patient);
    }

    public Patient registerPatientFromDTO(PatientRegisterDTO dto) {
        Patient patient = new Patient();
        patient.setNames(dto.getNames());
        patient.setFirstLastname(dto.getFirstLastname());
        patient.setSecondLastname(dto.getSecondLastname());
        patient.setBirthDate(dto.getBirthDate());
        patient.setCiNumber(dto.getCiNumber());
        patient.setEmail(dto.getEmail());
        patient.setPasswordHash(dto.getPassword()); // TODO: Use PasswordEncoder when security is implemented
        patient.setSelfieVerification(dto.getSelfieVerification());
        patient.setClinicalHistory(dto.getClinicalHistory());
        patient.setRole(1); // 1 for Patient
        patient.setCiDocumentImg(null); // Or set if needed

        return registerPatient(patient);
    }

    public Optional<Patient> updatePatient(Integer id, Patient patientDetails) {
        return patientRepository.findById(id).map(patient -> {
            patient.setNames(patientDetails.getNames());
            patient.setFirstLastname(patientDetails.getFirstLastname());
            patient.setSecondLastname(patientDetails.getSecondLastname());
            patient.setBirthDate(patientDetails.getBirthDate());
            patient.setEmail(patientDetails.getEmail());
            patient.setCiNumber(patientDetails.getCiNumber());
            patient.setClinicalHistory(patientDetails.getClinicalHistory());
            patient.setSelfieVerification(patientDetails.getSelfieVerification());
            patient.setLastUpdate(LocalDateTime.now());
            return patientRepository.save(patient);
        });
    }

    public boolean deletePatient(Integer id) {
        return patientRepository.findById(id).map(patient -> {
            patientRepository.delete(patient);
            return true;
        }).orElse(false);
    }
}