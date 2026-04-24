package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.PatientRegisterDTO;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.repository.PatientRepository;

@Service
public class PatientService extends UserService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PatientService(PatientRepository patientRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          SimpMessagingTemplate messagingTemplate) { 
        super(passwordEncoder);
        this.patientRepository = patientRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Patient registerPatientFromDTO(PatientRegisterDTO dto) {
        logger.info("Registering new patient with email: {}", dto.getEmail());
        Patient patient = new Patient();
        patient.setNames(dto.getNames());
        patient.setFirstLastname(dto.getFirstLastname());
        patient.setSecondLastname(dto.getSecondLastname());
        patient.setBirthDate(dto.getBirthDate());
        patient.setCiNumber(dto.getCiNumber());
        patient.setEmail(dto.getEmail());
        patient.setCiDocumentImg(dto.getCiDocumentImg());
        patient.setPasswordHash(dto.getPassword());
        patient.setSelfieVerification(dto.getSelfieVerification());
        patient.setClinicalHistory(dto.getClinicalHistory());

        prepareUser(patient, 1);

        Patient saved = patientRepository.save(patient);
        logger.info("Patient registered successfully with id: {}", saved.getId());

        messagingTemplate.convertAndSend("/topic/patients", "NUEVO_PACIENTE_REGISTRADO");
        
        return saved;
    }

    public List<Patient> getAllPatients() {
        logger.info("Fetching all patients from repository");
        List<Patient> patients = patientRepository.findAll();
        logger.info("Found {} patients", patients.size());
        return patients;
    }

    public Optional<Patient> getPatientById(Integer id) {
        logger.info("Fetching patient with id: {}", id);
        Optional<Patient> patient = patientRepository.findById(id);
        if (patient.isPresent()) {
            logger.info("Patient found with id: {}", id);
        } else {
            logger.warn("No patient found with id: {}", id);
        }
        return patient;
    }

    public Optional<Patient> updatePatient(Integer id, Patient patientDetails) {
        logger.info("Attempting to update patient with id: {}", id);
        return patientRepository.findById(id)
            .map(patient -> {
                patient.setNames(patientDetails.getNames());
                patient.setFirstLastname(patientDetails.getFirstLastname());
                patient.setSecondLastname(patientDetails.getSecondLastname());
                patient.setBirthDate(patientDetails.getBirthDate());
                patient.setCiNumber(patientDetails.getCiNumber());
                patient.setEmail(patientDetails.getEmail());
                patient.setClinicalHistory(patientDetails.getClinicalHistory());
                Patient updated = patientRepository.save(patient);
                logger.info("Patient with id: {} updated successfully", id);
                
                messagingTemplate.convertAndSend("/topic/patients", "PACIENTE_ACTUALIZADO");
                
                return updated;
            });
    }

    public boolean deletePatient(Integer id) {
        logger.info("Attempting logical delete for patient with id: {}", id);
        return patientRepository.findById(id)
            .map(patient -> {
                patient.setStatus((byte) 0);
                patient.setLastUpdate(LocalDateTime.now());
                patientRepository.save(patient);
                logger.info("Patient with id: {} marked as inactive successfully", id);
                
                messagingTemplate.convertAndSend("/topic/patients", "PACIENTE_ELIMINADO");
                
                return true;
            }).orElseGet(() -> {
                logger.warn("Cannot delete - no patient found with id: {}", id);
                return false;
            });
    }

    public Optional<Patient> validatePatient(Integer id, boolean approved) {
    return patientRepository.findById(id).map(patient -> {
        patient.setAccountVerified(approved ? (byte) 1 : (byte) 0);
        patient.setLastUpdate(LocalDateTime.now());
        return patientRepository.save(patient);
    });
}
}