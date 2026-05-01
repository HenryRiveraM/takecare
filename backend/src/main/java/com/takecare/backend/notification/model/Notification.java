package com.takecare.backend.notification.model;

import java.time.LocalDateTime;

import com.takecare.backend.session.model.Session;

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

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "type", columnDefinition = "tinyint", nullable = false)
    private Byte type = 1;

    @Column(name = "status", columnDefinition = "tinyint", nullable = false)
    private Byte status = 0;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "read_date")
    private LocalDateTime readDate;
}
