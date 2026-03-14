package com.takecare.backend.specialities.Service;

import com.takecare.backend.specialities.model.Specialist;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpecialistService {

    private List<Specialist> specialists = new ArrayList<>();

    public Specialist registerSpecialist(Specialist specialist) {
        specialists.add(specialist);
        return specialist;
    }

    public List<Specialist> obtainSpecialists(){
        return specialists;
    }
}