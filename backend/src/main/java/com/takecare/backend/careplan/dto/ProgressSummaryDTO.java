package com.takecare.backend.careplan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgressSummaryDTO {

    private Long planId;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer overdueTasks;
    private Integer pendingTasks;
    private Double completionRate;
    private Double averageEmotionalRatingLast30Days;
    private Integer emotionalRatingsCount;
}
