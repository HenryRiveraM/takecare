package com.takecare.backend.auth.DTO;

public class ForgotPasswordRequestDTO{
    private String email;
    private String frontendUrl;

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getFrontendUrl(){
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl){
        this.frontendUrl = frontendUrl;
    }
}