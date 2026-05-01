package com.takecare.backend.user.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String names;

    @Column(name = "first_lastname")
    private String firstLastname;

    @Column(name = "second_lastname")
    private String secondLastname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "ci_number")
    private String ciNumber;

    @Column(name = "ci_document_img")
    private String ciDocumentImg;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(columnDefinition = "tinyint")
    private Byte role;

    @Column(columnDefinition = "tinyint")
    private Byte strikes;

    @Column(columnDefinition = "tinyint")
    private Byte status;

    @Column(name = "account_verified", columnDefinition = "tinyint default 2")
    private Byte accountVerified = 0;
}