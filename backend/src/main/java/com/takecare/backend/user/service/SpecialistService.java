package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class SpecialistService extends UserService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialistService.class);

    private final SpecialistRepository specialistRepository;

    public SpecialistService(SpecialistRepository specialistRepository, 
                             BCryptPasswordEncoder passwordEncoder) {
        super(passwordEncoder);
        this.specialistRepository = specialistRepository;
    }

    public Specialist registerSpecialistFromDTO(SpecialistRegisterDTO dto) {
        logger.info("Registering new specialist with email: {}", dto.getEmail());
        Specialist specialist = new Specialist();
        specialist.setNames(dto.getNames());
        specialist.setFirstLastname(dto.getFirstLastname());
        specialist.setSecondLastname(dto.getSecondLastname());
        specialist.setBirthDate(dto.getBirthDate());
        specialist.setCiNumber(dto.getCiNumber());
        specialist.setEmail(dto.getEmail());
        specialist.setPasswordHash(dto.getPassword());
        specialist.setBiography(dto.getBiography());
        specialist.setCertificationImg(dto.getCertificationImg());
        specialist.setSessionCost(dto.getSessionCost());

        prepareUser(specialist, 2);

        Specialist saved = specialistRepository.save(specialist);
        logger.info("Specialist registered successfully with id: {}", saved.getId());
        return saved;
    }

    public Specialist registerSpecialist(Specialist specialist) {
        logger.info("Registering specialist entity directly");
        Specialist saved = specialistRepository.save(prepareUser(specialist, 2));
        logger.info("Specialist registered with id: {}", saved.getId());
        return saved;
    }

    public List<Specialist> getAllSpecialists() {
        logger.info("Fetching all specialists from repository");
        List<Specialist> specialists = specialistRepository.findAll();
        logger.info("Found {} specialists", specialists.size());
        return specialists;
    }

    public Optional<Specialist> getSpecialistById(Integer id) {
        logger.info("Fetching specialist with id: {}", id);
        Optional<Specialist> specialist = specialistRepository.findById(id);
        if (specialist.isPresent()) {
            logger.info("Specialist found with id: {}", id);
        } else {
            logger.warn("No specialist found with id: {}", id);
        }
        return specialist;
    }

    public Optional<Specialist> updateSpecialist(Integer id, Specialist specialistDetails) {
        logger.info("Attempting to update specialist with id: {}", id);
        return specialistRepository.findById(id)
            .map(specialist -> {
                specialist.setNames(specialistDetails.getNames());
                specialist.setFirstLastname(specialistDetails.getFirstLastname());
                specialist.setSecondLastname(specialistDetails.getSecondLastname());
                specialist.setBirthDate(specialistDetails.getBirthDate());
                specialist.setCiNumber(specialistDetails.getCiNumber());
                specialist.setEmail(specialistDetails.getEmail());
                specialist.setBiography(specialistDetails.getBiography());
                specialist.setCertificationImg(specialistDetails.getCertificationImg());
                specialist.setSessionCost(specialistDetails.getSessionCost());
                Specialist updated = specialistRepository.save(specialist);
                logger.info("Specialist with id: {} updated successfully", id);
                return updated;
            });
    }

    public boolean deleteSpecialist(Integer id) {
        logger.info("Attempting logical delete for specialist with id: {}", id);
        return specialistRepository.findById(id)
            .map(specialist -> {
                specialist.setStatus(0);
                specialist.setLastUpdate(LocalDateTime.now());
                specialistRepository.save(specialist);
                logger.info("Specialist with id: {} marked as inactive successfully", id);
                return true;
            }).orElseGet(() -> {
                logger.warn("Cannot delete - no specialist found with id: {}", id);
                return false;
            });
    }

    public Optional<Specialist> validateSpecialist(Integer id, boolean approved) {
    int verificationStatus = approved ? ACCOUNT_VERIFIED_APPROVED : ACCOUNT_VERIFIED_REJECTED;

    return specialistRepository.findById(id)
        .map(specialist -> {
            specialist.setAccountVerified(verificationStatus);
            specialist.setLastUpdate(LocalDateTime.now());
            return specialistRepository.save(specialist);
        });
    }
}