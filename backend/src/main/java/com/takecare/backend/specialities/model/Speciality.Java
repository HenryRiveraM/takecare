package com.takecare.backend.specialities.model;

import java.io.Serializable;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialities")
public class Speciality implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToMany(mappedBy = "specialities")
    private Set<com.takecare.backend.user.model.Specialist> specialists;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<com.takecare.backend.user.model.Specialist> getSpecialists() { return specialists; }
    public void setSpecialists(Set<com.takecare.backend.user.model.Specialist> specialists) { this.specialists = specialists; }
}