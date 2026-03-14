package com.takecare.backend.specialities.controller;
import com.takecare.backend.specialities.model.Specialist;
import com.takecare.backend.specialities.Service.SpecialistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/specialists")
public class SpecialistController {

    @Autowired
    private SpecialistService specialistService;

    @PostMapping("/registro")
    public Specialist register(@RequestBody Specialist specialist){
        return specialistService.registerSpecialist(specialist);
    }

    @GetMapping
    public List<Specialist> list(){
        return specialistService.obtainSpecialists();
    }
}