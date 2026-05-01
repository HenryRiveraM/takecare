package com.takecare.backend.user.repository;

import com.takecare.backend.user.model.ClinicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, Long> {

    /**
     * Devuelve todos los documentos activos de un paciente.
     * status = 1 → no eliminados lógicamente.
     */
    @Query("SELECT d FROM ClinicalDocument d WHERE d.patientId = :patientId AND d.status = 1")
    List<ClinicalDocument> findActiveByPatientId(@Param("patientId") Integer patientId);

    /**
     * Busca un documento activo por su id y su dueño.
     * Útil para verificar propiedad antes de descargar o eliminar.
     */
    @Query("SELECT d FROM ClinicalDocument d WHERE d.id = :id AND d.patientId = :patientId AND d.status = 1")
    Optional<ClinicalDocument> findActiveByIdAndPatientId(@Param("id") Long id,
                                                          @Param("patientId") Integer patientId);
}
