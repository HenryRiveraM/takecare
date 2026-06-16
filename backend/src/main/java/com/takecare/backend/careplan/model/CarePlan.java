package com.takecare.backend.careplan.model;

import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.session.model.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "care_plans")
public class CarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "therapeutic_objectives", nullable = false, columnDefinition = "TEXT")
    private String therapeuticObjectives;

    @Column(name = "general_recommendations", nullable = false, columnDefinition = "TEXT")
    private String generalRecommendations;

    @Column(name = "professional_observations", columnDefinition = "TEXT")
    private String professionalObservations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CarePlanStatus status;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_session_id")
    private Session reviewSession;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "archived_by_specialist", nullable = false)
    private Boolean archivedBySpecialist = false;

    @Column(name = "archived_date")
    private LocalDateTime archivedDate;
}
