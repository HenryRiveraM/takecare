package com.takecare.backend.report.model;

import jakarta.persistence.*;
import com.takecare.backend.user.model.User;
import com.takecare.backend.session.model.Session;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "reporter_user_id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "reported_user_id")
    private User reported;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(length = 100, nullable = false)
    private String reason;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(length = 500)
    private String description;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}