package com.takecare.backend.user.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.PatientProfileDTO;
import com.takecare.backend.user.dto.UpdatePatientProfileDTO;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;

@Service
public class PatientProfileService {

    private static final Logger logger = LoggerFactory.getLogger(PatientProfileService.class);

    private final PatientRepository patientRepository;

    public PatientProfileService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public PatientProfileDTO getProfile(Integer id) {
        logger.info("Fetching profile for patient id: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Patient not found with id: {}", id);
                    return new RuntimeException("Paciente no encontrado con ID: " + id);
                });

        return toDTO(patient);
    }

    public PatientProfileDTO updateProfile(Integer id, UpdatePatientProfileDTO dto) {
        logger.info("Updating profile for patient id: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Patient not found with id: {}", id);
                    return new RuntimeException("Paciente no encontrado con ID: " + id);
                });

        if (dto.getNames() != null && !dto.getNames().isBlank()) {
            patient.setNames(dto.getNames());
        }
        if (dto.getFirstLastname() != null && !dto.getFirstLastname().isBlank()) {
            patient.setFirstLastname(dto.getFirstLastname());
        }
        if (dto.getSecondLastname() != null) {
            patient.setSecondLastname(dto.getSecondLastname());
        }
        if (dto.getBirthDate() != null) {
            patient.setBirthDate(dto.getBirthDate());
        }

        patient.setLastUpdate(LocalDateTime.now());
        Patient updated = patientRepository.save(patient);

        logger.info("Profile updated successfully for patient id: {}", id);
        return toDTO(updated);
    }

    private PatientProfileDTO toDTO(Patient patient) {
        PatientProfileDTO dto = new PatientProfileDTO();
        dto.setId(patient.getId());
        dto.setNames(patient.getNames());
        dto.setFirstLastname(patient.getFirstLastname());
        dto.setSecondLastname(patient.getSecondLastname());
        dto.setBirthDate(patient.getBirthDate());
        dto.setCiNumber(patient.getCiNumber());
        dto.setEmail(patient.getEmail());
        dto.setClinicalHistory(patient.getClinicalHistory());
        dto.setAccountVerified(patient.getAccountVerified());
        return dto;
    }
}
