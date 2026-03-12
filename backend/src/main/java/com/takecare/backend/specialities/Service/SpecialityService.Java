package com.takecare.service;

import com.takecare.model.Especialista;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EspecialistaService {

    private List<Especialista> especialistas = new ArrayList<>();

    public Especialista registrarEspecialista(Especialista especialista) {
        especialistas.add(especialista);
        return especialista;
    }

    public List<Especialista> obtenerEspecialistas(){
        return especialistas;
    }
}