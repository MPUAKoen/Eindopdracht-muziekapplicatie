package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public class TeacherAssignmentRequest {
    @NotNull(message = "teacherId is required")
    private Long teacherId;

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}
