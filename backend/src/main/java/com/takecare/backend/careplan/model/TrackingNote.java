package com.takecare.backend.careplan.model;

import com.takecare.backend.session.model.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import com.takecare.backend.careplan.converter.EncryptionConverter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tracking_notes")
public class TrackingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id", nullable = false)
    private CarePlan carePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(name = "author_role", nullable = false, length = 20)
    private String authorRole;

    @Column(name = "author_name", nullable = false, length = 150)
    private String authorName;

    @Column(name = "note_type", nullable = false, length = 30)
    private String noteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String note;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
}
