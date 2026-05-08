package com.takecare.backend.supportmaterial.repository;

import com.takecare.backend.supportmaterial.model.SupportMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportMaterialRepository 
        extends JpaRepository<SupportMaterial, Integer> {

}