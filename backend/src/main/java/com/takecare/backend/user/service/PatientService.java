package com.takecare.backend.user.service;

import java.util.List;
import java.util.Optional;

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

    // Registro desde DTO
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

        prepareUser(patient, 1); // ✅ ahora funciona

        return patientRepository.save(patient);
    }

    // Listado de todos los pacientes
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // Obtener paciente por ID
    public Optional<Patient> getPatientById(Integer id) {
        return patientRepository.findById(id);
    }

    // Actualizar paciente
    public Optional<Patient> updatePatient(Integer id, Patient patientDetails) {
        return patientRepository.findById(id)
            .map(patient -> {
                patient.setNames(patientDetails.getNames());
                patient.setFirstLastname(patientDetails.getFirstLastname());
                patient.setSecondLastname(patientDetails.getSecondLastname());
                patient.setBirthDate(patientDetails.getBirthDate());
                patient.setCiNumber(patientDetails.getCiNumber());
                patient.setEmail(patientDetails.getEmail());
                patient.setSelfieVerification(patientDetails.getSelfieVerification());
                patient.setClinicalHistory(patientDetails.getClinicalHistory());
                return patientRepository.save(patient);
            });
    }

    // Eliminar paciente
    public boolean deletePatient(Integer id) {
        return patientRepository.findById(id)
            .map(patient -> {
                patientRepository.delete(patient);
                return true;
            }).orElse(false);
    }
}