package com.takecare.backend.specialities.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "specialties")
public class Speciality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToMany(mappedBy = "specialties")
    private Set<com.takecare.backend.user.model.Specialist> specialists;
}