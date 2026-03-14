package com.takecare.backend.specialities.model;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialists")
public class Specialist {
    @Id
    private int id;

    private String biography;
    private String certification_img;
    private String office_ubi;
    private BigDecimal session_cost;
    private BigDecimal reputation_average;

    public Specialist(){}

    public Specialist(int id, String biography, String certification_img, String office_ubi, BigDecimal session_cost, BigDecimal reputation_average) {
        this.id = id;
        this.biography = biography;
        this.certification_img = certification_img;
        this.office_ubi = office_ubi;
        this.session_cost = session_cost;
        this.reputation_average = reputation_average;
    }

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public String getBiography(){
        return biography;
    }

    public void setBiography(String biography){
        this.biography=biography;
    }

    public String getCertification_img(){
        return certification_img;
    }

    public void setCertification_img(String certification_img){
        this.certification_img = certification_img;
    }

    public String getOffice_ubi(){
        return office_ubi;
    }

    public void setOffice_ubi(String office_ubi){
        this.office_ubi = office_ubi;
    }

    public BigDecimal getSession_cost(){
        return session_cost;
    }

    public void setSession_cost(BigDecimal session_cost){
        this.session_cost=session_cost;
    }

    public BigDecimal getReputation_average(){
        return reputation_average;
    }

    public void setReputation_average(BigDecimal reputation_average){
        this.reputation_average=reputation_average;
    }
}