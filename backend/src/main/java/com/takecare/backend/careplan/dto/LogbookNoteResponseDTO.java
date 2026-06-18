package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LogbookNoteResponseDTO {
    private Long id;
    private Long planId;
    private Integer authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private LocalDateTime createdDate;
}
