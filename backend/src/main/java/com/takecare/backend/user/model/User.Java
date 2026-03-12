package com.takecare.backend.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String names;

    @Column(name = "first_lastname")
    private String firstLastname;

    @Column(name = "second_lastname")
    private String secondLastname;

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
    private Integer role;

    @Column(columnDefinition = "tinyint")
    private Integer strikes;

    @Column(columnDefinition = "tinyint")
    private Boolean status;

    @Column(name = "account_verified", columnDefinition = "tinyint")
    private Boolean accountVerified;
}