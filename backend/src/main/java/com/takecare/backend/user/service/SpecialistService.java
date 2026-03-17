package com.takecare.backend.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class SpecialistService {

    private final SpecialistRepository specialistRepository;

    public SpecialistService(SpecialistRepository specialistRepository) {
        this.specialistRepository = specialistRepository;
    }

    public List<Specialist> getAllSpecialists() {
        return specialistRepository.findAll();
    }

    public Optional<Specialist> getSpecialistById(Integer id) {
        return specialistRepository.findById(id);
    }

    public Specialist registerSpecialist(Specialist specialist) {
        specialist.setCreatedDate(LocalDateTime.now());
        specialist.setLastUpdate(LocalDateTime.now());
        specialist.setStatus(true);
        specialist.setStrikes(0);
        specialist.setAccountVerified(false);

        return specialistRepository.save(specialist);
    }

    public Specialist registerSpecialistFromDTO(SpecialistRegisterDTO dto) {
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
        specialist.setRole(2); // 2 for Specialist

        return registerSpecialist(specialist);
    }

    public Optional<Specialist> updateSpecialist(Integer id, Specialist specialistDetails) {
        return specialistRepository.findById(id).map(specialist -> {
            specialist.setNames(specialistDetails.getNames());
            specialist.setFirstLastname(specialistDetails.getFirstLastname());
            specialist.setSecondLastname(specialistDetails.getSecondLastname());
            specialist.setEmail(specialistDetails.getEmail());
            specialist.setBirthDate(specialistDetails.getBirthDate());
            specialist.setCiNumber(specialistDetails.getCiNumber());
            specialist.setBiography(specialistDetails.getBiography());
            specialist.setCertificationImg(specialistDetails.getCertificationImg());
            specialist.setOfficeUbi(specialistDetails.getOfficeUbi());
            specialist.setSessionCost(specialistDetails.getSessionCost());
            specialist.setReputationAverage(specialistDetails.getReputationAverage());
            specialist.setSpecialties(specialistDetails.getSpecialties());

            specialist.setLastUpdate(LocalDateTime.now());

            return specialistRepository.save(specialist);
        });
    }

    public boolean deleteSpecialist(Integer id) {
        return specialistRepository.findById(id).map(specialist -> {
            specialistRepository.delete(specialist);
            return true;
        }).orElse(false);
    }
}