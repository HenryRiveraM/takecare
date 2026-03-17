package com.takecare.backend.user.service;

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
}