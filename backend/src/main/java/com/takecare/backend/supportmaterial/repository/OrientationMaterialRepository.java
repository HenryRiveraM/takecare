package com.takecare.backend.supportmaterial.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.takecare.backend.supportmaterial.model.OrientationMaterial;

@Repository
public interface OrientationMaterialRepository extends JpaRepository<OrientationMaterial, Long> {

    @Query("SELECT m FROM OrientationMaterial m WHERE m.specialistId = :specialistId AND m.status = 1 ORDER BY m.createdDate DESC")
    List<OrientationMaterial> findActiveBySpecialistId(@Param("specialistId") Integer specialistId);

    @Query("SELECT m FROM OrientationMaterial m WHERE m.id = :id AND m.specialistId = :specialistId AND m.status = 1")
    Optional<OrientationMaterial> findActiveByIdAndSpecialistId(@Param("id") Long id,
                                                                @Param("specialistId") Integer specialistId);

    @Query("""
        SELECT m FROM OrientationMaterial m
        JOIN com.takecare.backend.user.model.Specialist s ON s.id = m.specialistId
        WHERE m.status = 1
          AND (
                :search IS NULL
                OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(
                        COALESCE(s.names, ''), ' ',
                        COALESCE(s.firstLastname, ''), ' ',
                        COALESCE(s.secondLastname, '')
                )) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(m.contentType) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(m.fileName) LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (
                :type IS NULL
                OR (
                    :type = 'PDF'
                    AND (
                        LOWER(m.contentType) LIKE '%pdf%'
                        OR LOWER(m.fileName) LIKE '%.pdf'
                    )
                )
                OR (
                    :type = 'DOCX'
                    AND (
                        LOWER(m.contentType) LIKE '%wordprocessingml%'
                        OR LOWER(m.contentType) LIKE '%msword%'
                        OR LOWER(m.fileName) LIKE '%.docx'
                        OR LOWER(m.fileName) LIKE '%.doc'
                    )
                )
          )
        ORDER BY m.createdDate DESC
    """)
    List<OrientationMaterial> findActiveWithFilters(@Param("search") String search,
                                                    @Param("type") String type);
}
