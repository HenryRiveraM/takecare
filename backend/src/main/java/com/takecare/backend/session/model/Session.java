package com.takecare.backend.session.model;

import jakarta.persistence.*;
import com.takecare.backend.user.model.Patient;
import com.takecare.backend.specialistschedule.model.SpecialistSchedule;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private SpecialistSchedule schedule;

    @Column(columnDefinition = "tinyint")
    private Integer status; //1 pendiente, 2 aceptada, 3 rechazada, 4 cancelada, 5 finalizada

    @Column(name = "type_of_session", columnDefinition = "tinyint")
    private Integer typeOfSession; //1 virtual, 2 presencial

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}