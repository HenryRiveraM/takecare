package com.takecare.backend.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNotificationStatusRequestDto {

    private Integer specialistId;
    private Boolean read;
}
