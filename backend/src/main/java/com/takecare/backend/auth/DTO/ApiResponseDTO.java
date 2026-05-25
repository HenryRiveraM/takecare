package com.takecare.backend.auth.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponseDTO<T> {

    private boolean success;
    private T data;
    private String error;

    public ApiResponseDTO(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.error = null;
    }
}
