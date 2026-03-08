package com.takecare.supportmaterial.repository;

import com.takecare.supportmaterial.model.SupportMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMaterialRepository 
        extends JpaRepository<SupportMaterial, Long> {

}