package com.takecare.controller;
import com.takecare.model.Especialista;
import com.takecare.service.EspecialistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/specialities")
public class EspecialistaController {

    @Autowired
    private EspecialistaService especialistaService;

    @PostMapping("/registro")
    public Especialista registrar(@RequestBody Especialista especialista){
        return especialistaService.registrarEspecialista(especialista);
    }

    @GetMapping
    public List<Especialista> listar(){
        return especialistaService.obtenerEspecialistas();
    }
}