package com.takecare.backend.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.takecare.backend.user.dto.SpecialistRegisterDTO;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.repository.SpecialistRepository;

@Service
public class SpecialistService extends UserService {

    private final SpecialistRepository specialistRepository;

    public SpecialistService(SpecialistRepository specialistRepository, 
                             BCryptPasswordEncoder passwordEncoder) {
        super(passwordEncoder);
        this.specialistRepository = specialistRepository;
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

        prepareUser(specialist, 2); 

        return specialistRepository.save(specialist);
    }

    public Specialist registerSpecialist(Specialist specialist) {
        return specialistRepository.save(prepareUser(specialist, 2));
    }

    public List<Specialist> getAllSpecialists() {
        return specialistRepository.findAll();
    }

    public Optional<Specialist> getSpecialistById(Integer id) {
        return specialistRepository.findById(id);
    }

    public Optional<Specialist> updateSpecialist(Integer id, Specialist specialistDetails) {
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
                return specialistRepository.save(specialist);
            });
    }

    public boolean deleteSpecialist(Integer id) {
        return specialistRepository.findById(id)
            .map(specialist -> {
                specialistRepository.delete(specialist);
                return true;
            }).orElse(false);
    }
}