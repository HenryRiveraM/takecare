package com.takecare.backend.supportmaterial.repository;

import com.takecare.backend.supportmaterial.model.OrientationMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrientationMaterialRepository extends JpaRepository<OrientationMaterial, Long> {

    /**
     * Devuelve todos los materiales activos de un especialista.
     * status = 1 → no eliminados lógicamente.
     */
    @Query("SELECT m FROM OrientationMaterial m WHERE m.specialistId = :specialistId AND m.status = 1 ORDER BY m.createdDate DESC")
    List<OrientationMaterial> findActiveBySpecialistId(@Param("specialistId") Integer specialistId);

    /**
     * Busca un material activo por su id y su dueño.
     * Útil para verificar propiedad antes de eliminar.
     */
    @Query("SELECT m FROM OrientationMaterial m WHERE m.id = :id AND m.specialistId = :specialistId AND m.status = 1")
    Optional<OrientationMaterial> findActiveByIdAndSpecialistId(@Param("id") Long id,
                                                                @Param("specialistId") Integer specialistId);
}
