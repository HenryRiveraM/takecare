package com.takecare.backend.careplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLogbookNoteRequestDTO {

    @NotNull(message = "El id del autor es obligatorio")
    private Integer authorId;

    @NotBlank(message = "El rol del autor es obligatorio")
    private String authorRole;

    @NotBlank(message = "El nombre del autor es obligatorio")
    private String authorName;

    @NotBlank(message = "El contenido de la nota es obligatorio")
    private String content;

    private Integer sessionId;
}
