package com.takecare.backend.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.user.service.PatientService;
import com.takecare.backend.user.service.SpecialistService;
import java.util.List;



@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final PatientService patientService;
    private final SpecialistService specialistService;

    public AdminController(PatientService patientService,
         SpecialistService specialistService) {
        this.patientService = patientService;
        this.specialistService = specialistService;
    }

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Integer id) {

        boolean deleted = patientService.deletePatient(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


    @GetMapping("/specialists")
    public ResponseEntity<List<Specialist>> getAllSpecialists() {
        return ResponseEntity.ok(specialistService.getAllSpecialists());
    }

    @DeleteMapping("/specialists/{id}")
    public ResponseEntity<Void> deleteSpecialist(@PathVariable Integer id) {

        boolean deleted = specialistService.deleteSpecialist(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/specialists/{id}/validate/approve")
    public ResponseEntity<Specialist> approveSpecialist(@PathVariable Integer id) {
        return specialistService.validateSpecialist(id, true)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/specialists/{id}/validate/reject")    
    public ResponseEntity<Specialist> rejectSpecialist(@PathVariable Integer id) {
        return specialistService.validateSpecialist(id, false)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
