package com.takecare.backend.calification.model;

import jakarta.persistence.*;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.user.model.Specialist;
import com.takecare.backend.session.model.Session;
import java.time.LocalDateTime;

@Entity
@Table(name = "califications")
public class Calification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "specialist_id")
    private Specialist specialist;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(columnDefinition = "tinyint")
    private Integer rating;

    private String comment;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}