package com.takecare.backend.user.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;

@Service
public class PatientService extends UserService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository, 
                          BCryptPasswordEncoder passwordEncoder) {
        super(passwordEncoder);
        this.patientRepository = patientRepository;
    }

    public Patient registerPatientFromDTO(PatientRegisterDTO dto) {
        Patient patient = new Patient();
        patient.setNames(dto.getNames());
        patient.setFirstLastname(dto.getFirstLastname());
        patient.setSecondLastname(dto.getSecondLastname());
        patient.setBirthDate(dto.getBirthDate());
        patient.setCiNumber(dto.getCiNumber());
        patient.setEmail(dto.getEmail());
        patient.setPasswordHash(dto.getPassword()); 
        patient.setSelfieVerification(dto.getSelfieVerification());
        patient.setClinicalHistory(dto.getClinicalHistory());

        prepareUser(patient, 1);

        return patientRepository.save(patient);
    }

    public Patient registerPatient(Patient patient) {
        return patientRepository.save(prepareUser(patient, 1));
    }
}