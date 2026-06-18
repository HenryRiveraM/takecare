package com.takecare.backend.preventivealert.model;

import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "preventive_alerts")
public class PreventiveAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "care_plan_item_id")
    private Long carePlanItemId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType; // CRITICAL_STATE, ABANDONMENT, TASK_DUE_SOON, TASK_OVERDUE

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN"; // OPEN, REVIEWED

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "reviewed_date")
    private LocalDateTime reviewedDate;
}
