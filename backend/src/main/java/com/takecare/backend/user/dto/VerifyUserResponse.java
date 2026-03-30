package com.takecare.backend.user.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyUserResponse {

    private Integer userId;
    private String names;
    private String firstLastname;
    private String email;
    private Boolean accountVerified;
    private LocalDateTime lastUpdate;
    private String message;

    public VerifyUserResponse(Integer userId, String names, String firstLastname,
                               String email, Boolean accountVerified,
                               LocalDateTime lastUpdate, String message) {
        this.userId = userId;
        this.names = names;
        this.firstLastname = firstLastname;
        this.email = email;
        this.accountVerified = accountVerified;
        this.lastUpdate = lastUpdate;
        this.message = message;
    }
}