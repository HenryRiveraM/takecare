package com.takecare.backend.auth.model;

import java.time.LocalDateTime;

import com.takecare.backend.user.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private LocalDateTime expiration;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public String getToken(){
        return token;
    }

    public void setToken(String token){
        this.token = token;
    }

    public LocalDateTime getExpiration(){
        return expiration;
    }

    public void setExpiration(LocalDateTime expiration){
        this.expiration = expiration;
    }

    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }
}