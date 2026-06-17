package com.takecare.backend.careplan.model;

import com.takecare.backend.user.model.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "emotional_records")
public class EmotionalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id")
    private CarePlan carePlan;

    @Column(name = "mood_state", nullable = false, length = 30)
    private String moodState;

    @Column(name = "mood_level", nullable = false)
    private Integer moodLevel;

    @Column(name = "anxiety_level", nullable = false)
    private Integer anxietyLevel;

    @Column(name = "stress_level", nullable = false)
    private Integer stressLevel;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
