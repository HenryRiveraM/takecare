package com.takecare.backend.specialities.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.takecare.backend.specialities.model.Specialist;
import com.takecare.backend.specialities.repository.SpecialistRepository;

@Service
public class SpecialistService {

    @Autowired
    private SpecialistRepository specialistRepository;

    public Specialist registerSpecialist(Specialist specialist) {
        return specialistRepository.save(specialist);
    }

    public List<Specialist> obtainSpecialists(){
        return specialistRepository.findAll();
    }
}